<script setup lang="ts">
/** 页内弹窗：创建第三方平台订单（不含小程序，实时预览达人额外提成与商家到手金额）。 */
import { computed, reactive, ref, watch } from 'vue'
import { orderApi } from '../../api/orders'
import type { Channel, Creator, Room } from '../../types/catalog'
import type { CreateExternalOrderRequest, OrderStatus } from '../../types/orders'
import { addDays, amountToCents, centsToAmount, isMoneyString, shanghaiDate } from '../../utils/operations'

interface Props {
  open: boolean
  channels: Channel[]
  creators: Creator[]
  rooms: Room[]
  presetRoomId?: number | null
  presetCheckIn?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  created: [message: string]
}>()

interface ExternalOrderForm {
  channel_id: string
  room_id: string
  creator_id: string
  platform_order_no: string
  guest_name: string
  phone: string
  guest_count: string
  check_in: string
  check_out: string
  status: Exclude<OrderStatus, 'CANCELLED'>
  remark: string
  paid_amount: string
  platform_fee: string
  platform_commission: string
  expected_amount: string
}

const form = reactive<ExternalOrderForm>(emptyForm())
const saving = ref(false)
const errorMessage = ref('')

const externalChannels = computed(() => props.channels.filter((ch) => ch.id !== 1 && ch.enabled))
const availableCreators = computed(() =>
  props.creators.filter((cr) => cr.enabled && cr.channel_id === Number(form.channel_id)),
)
const hasCreator = computed(() => Boolean(form.creator_id))

const amountPreview = computed(() => {
  const paid = amountToCents(form.paid_amount)
  const fee = amountToCents(form.platform_fee)
  const pComm = hasCreator.value ? amountToCents(form.platform_commission) : 0
  const hasExpected = hasCreator.value && form.expected_amount.trim() !== ''
  const expected = hasExpected ? amountToCents(form.expected_amount) : 0
  const extra = hasExpected ? Math.max(0, paid - fee - pComm - expected) : 0
  const net = paid - fee - pComm - extra
  return {
    extra: centsToAmount(extra),
    net: centsToAmount(net),
  }
})

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return
    errorMessage.value = ''
    const checkIn = props.presetCheckIn || shanghaiDate()
    const checkOut = addDays(checkIn, 1)
    const targetRoom = props.rooms.find((r) => r.id === props.presetRoomId) || props.rooms[0]
    Object.assign(form, emptyForm(), {
      channel_id: String(externalChannels.value[0]?.id ?? ''),
      room_id: String(targetRoom?.id ?? ''),
      check_in: checkIn,
      check_out: checkOut,
      guest_count: '2',
      status: 'PENDING_CHECKIN' as Exclude<OrderStatus, 'CANCELLED'>,
      paid_amount: targetRoom?.price ?? '',
      platform_fee: '0.00',
      platform_commission: '0.00',
    })
  },
)

watch(
  () => form.channel_id,
  () => {
    if (form.creator_id && !availableCreators.value.some((cr) => String(cr.id) === form.creator_id)) {
      form.creator_id = ''
    }
  },
)

watch(
  () => form.creator_id,
  (creatorId) => {
    if (!creatorId) {
      form.platform_commission = '0.00'
      form.expected_amount = ''
    }
  },
)

function emptyForm(): ExternalOrderForm {
  return {
    channel_id: '',
    room_id: '',
    creator_id: '',
    platform_order_no: '',
    guest_name: '',
    phone: '',
    guest_count: '2',
    check_in: '',
    check_out: '',
    status: 'PENDING_CHECKIN',
    remark: '',
    paid_amount: '',
    platform_fee: '0.00',
    platform_commission: '0.00',
    expected_amount: '',
  }
}

function handleClose(): void {
  if (!saving.value) emit('close')
}

async function submit(): Promise<void> {
  errorMessage.value = ''
  if (!form.channel_id || !form.room_id || !form.check_in || !form.check_out || !form.paid_amount.trim()) {
    errorMessage.value = '请完整填写渠道、房间、入离日期与顾客实付金额'
    return
  }
  if (form.check_out <= form.check_in) {
    errorMessage.value = '退房日期必须晚于入住日期'
    return
  }
  if (
    ![form.paid_amount, form.platform_fee, form.platform_commission, form.expected_amount]
      .filter((v) => v.trim() !== '')
      .every(isMoneyString)
  ) {
    errorMessage.value = '金额须为非负数字，最多保留两位小数'
    return
  }
  if (form.creator_id && !form.expected_amount.trim()) {
    errorMessage.value = '选择达人时必须填写「预期收入」以计算额外提成'
    return
  }
  if (form.guest_count && (!Number.isInteger(Number(form.guest_count)) || Number(form.guest_count) < 1)) {
    errorMessage.value = '入住人数须为正整数'
    return
  }

  const request: CreateExternalOrderRequest = {
    channel_id: Number(form.channel_id),
    room_id: Number(form.room_id),
    creator_id: form.creator_id ? Number(form.creator_id) : null,
    platform_order_no: form.platform_order_no.trim() || null,
    guest_name: form.guest_name.trim() || null,
    phone: form.phone.trim() || null,
    guest_count: form.guest_count ? Number(form.guest_count) : null,
    check_in: form.check_in,
    check_out: form.check_out,
    status: form.status,
    remark: form.remark.trim() || null,
    amount: {
      paid_amount: form.paid_amount.trim(),
      platform_fee: form.platform_fee.trim() || '0.00',
      platform_commission: form.creator_id ? form.platform_commission.trim() || '0.00' : '0.00',
      expected_amount: form.creator_id ? form.expected_amount.trim() : null,
    },
  }

  saving.value = true
  try {
    const created = await orderApi.create(request)
    emit('created', `第三方订单 ${created.order_no} 创建成功，商家到手 ¥${created.net_amount}`)
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '第三方订单创建失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="qx-modal-backdrop" @click.self="handleClose">
      <div class="qx-modal-panel qx-modal-lg" role="dialog" aria-modal="true" aria-labelledby="ext-order-title">
        <div class="qx-modal-header">
          <div>
            <h3 id="ext-order-title">创建第三方平台订单</h3>
            <p>支持抖音、美团、携程、同程、飞猪、途家 6 大外部渠道（不含小程序）</p>
          </div>
          <button type="button" class="qx-modal-close" aria-label="关闭" @click="handleClose">
            <svg viewBox="0 0 24 24" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          </button>
        </div>

        <p v-if="errorMessage" class="notice notice-error qx-modal-alert" role="alert">{{ errorMessage }}</p>

        <form class="qx-modal-form" @submit.prevent="submit">
          <div class="qx-form-grid-2">
            <label class="qx-field">
              <span>第三方平台来源 <em class="is-req">*</em></span>
              <select v-model="form.channel_id" required>
                <option v-for="ch in externalChannels" :key="ch.id" :value="String(ch.id)">{{ ch.name }}</option>
              </select>
            </label>

            <label class="qx-field">
              <span>房间号 <em class="is-req">*</em></span>
              <select v-model="form.room_id" required>
                <option v-for="room in rooms" :key="room.id" :value="String(room.id)">
                  {{ room.number }} · {{ room.name }}
                </option>
              </select>
            </label>

            <label class="qx-field">
              <span>入住日期 <em class="is-req">*</em></span>
              <input v-model="form.check_in" type="date" required />
            </label>

            <label class="qx-field">
              <span>退房日期 <em class="is-req">*</em></span>
              <input v-model="form.check_out" type="date" required />
            </label>

            <label class="qx-field">
              <span>预订人姓名</span>
              <input v-model="form.guest_name" type="text" placeholder="如：王先生" />
            </label>

            <label class="qx-field">
              <span>入住人数</span>
              <input v-model="form.guest_count" type="number" min="1" max="10" />
            </label>

            <label class="qx-field">
              <span>订单状态 <em class="is-req">*</em></span>
              <select v-model="form.status">
                <option value="PENDING_CONFIRM">已预订（待确认）</option>
                <option value="PENDING_CHECKIN">待入住</option>
                <option value="CHECKED_IN">已入住</option>
                <option value="COMPLETED">已完成（历史订单）</option>
              </select>
            </label>

            <label class="qx-field">
              <span>关联达人/主播（选填）</span>
              <select v-model="form.creator_id">
                <option value="">无主播（普通自然流量）</option>
                <option v-for="cr in availableCreators" :key="cr.id" :value="String(cr.id)">{{ cr.name }}</option>
              </select>
            </label>
          </div>

          <div class="qx-amount-box">
            <div class="qx-amount-inputs">
              <label class="qx-field">
                <span class="is-cinnabar">顾客实付金额 *</span>
                <input v-model="form.paid_amount" type="text" inputmode="decimal" placeholder="0.00" class="is-mono" required />
              </label>

              <label class="qx-field">
                <span>平台手续费</span>
                <input v-model="form.platform_fee" type="text" inputmode="decimal" placeholder="0.00" class="is-mono" />
              </label>

              <label class="qx-field">
                <span>平台达人提成</span>
                <input
                  v-model="form.platform_commission"
                  type="text"
                  inputmode="decimal"
                  placeholder="0.00"
                  class="is-mono"
                  :disabled="!hasCreator"
                />
              </label>

              <label class="qx-field">
                <span>预期收入(有主播必填)</span>
                <input
                  v-model="form.expected_amount"
                  type="text"
                  inputmode="decimal"
                  placeholder="无主播留空"
                  class="is-mono"
                  :disabled="!hasCreator"
                />
              </label>
            </div>

            <div class="qx-amount-preview">
              <span>自动计算达人额外提成：<strong class="is-teal is-mono">¥{{ amountPreview.extra }}</strong></span>
              <span>自动计算商家到手金额：<strong class="is-celadon is-mono is-lg">¥{{ amountPreview.net }}</strong></span>
            </div>
          </div>

          <div class="qx-form-grid-2">
            <label class="qx-field">
              <span>预订手机号（完成后自动发积分）</span>
              <input v-model="form.phone" type="text" placeholder="11位手机号" />
            </label>

            <label class="qx-field">
              <span>外部平台订单号（防重）</span>
              <input v-model="form.platform_order_no" type="text" placeholder="如：DY99281723" />
            </label>

            <label class="qx-field qx-span-2">
              <span>订单备注</span>
              <input v-model="form.remark" type="text" placeholder="特殊需求说明" />
            </label>
          </div>

          <div class="qx-modal-footer qx-footer-end">
            <button type="button" class="qx-btn-ghost" :disabled="saving" @click="handleClose">取消</button>
            <button type="submit" class="qx-btn-primary" :disabled="saving">
              {{ saving ? '正在创建…' : '确认创建订单' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
