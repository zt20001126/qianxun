<script setup lang="ts">
/** 页内弹窗：订单详情、资料编辑、状态流转（确认/入住/退房）与第三方订单取消。 */
import { computed, reactive, ref, watch } from 'vue'
import AdminNoticeDialog from '../AdminNoticeDialog.vue'
import { orderApi } from '../../api/orders'
import type { Creator, Room } from '../../types/catalog'
import type { OrderDetail, UpdateOrderRequest } from '../../types/orders'
import { orderStatusLabels } from '../../types/orders'
import { dayDifference } from '../../utils/operations'

interface Props {
  open: boolean
  orderId: number | null
  rooms: Room[]
  creators: Creator[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  updated: [message: string]
}>()

const loading = ref(false)
const saving = ref(false)
const actionBusy = ref(false)
const errorMessage = ref('')
const detail = ref<OrderDetail | null>(null)
const cancelConfirmOpen = ref(false)
const cancelReason = ref('')

const form = reactive({
  room_id: '',
  guest_count: '1',
  check_in: '',
  check_out: '',
  guest_name: '',
  phone: '',
  remark: '',
})

const canEditRoomDate = computed(() =>
  detail.value != null && ['PENDING_CONFIRM', 'PENDING_CHECKIN'].includes(detail.value.status),
)
const canCancelExternal = computed(() =>
  detail.value != null
  && detail.value.channel_id !== 1
  && ['PENDING_CONFIRM', 'PENDING_CHECKIN'].includes(detail.value.status),
)
const creatorName = computed(() => {
  if (!detail.value?.creator_id) return ''
  return props.creators.find((cr) => cr.id === detail.value?.creator_id)?.name || ''
})

const nextStatusAction = computed((): { action: 'confirm' | 'check-in' | 'check-out'; label: string; tone: string } | null => {
  if (!detail.value) return null
  if (detail.value.status === 'PENDING_CONFIRM') {
    return { action: 'confirm', label: '确认订单', tone: 'is-teal' }
  }
  if (detail.value.status === 'PENDING_CHECKIN') {
    return { action: 'check-in', label: '办理入住', tone: 'is-celadon' }
  }
  if (detail.value.status === 'CHECKED_IN') {
    return { action: 'check-out', label: '办理退房', tone: 'is-charcoal' }
  }
  return null
})

watch(
  () => [props.open, props.orderId] as const,
  async ([isOpen, id]) => {
    if (!isOpen || !id) return
    await fetchDetail(id)
  },
)

async function fetchDetail(id: number): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  cancelConfirmOpen.value = false
  cancelReason.value = ''
  detail.value = null
  try {
    const order = await orderApi.detail(id)
    detail.value = order
    Object.assign(form, {
      room_id: String(order.room_id),
      guest_count: order.guest_count == null ? '' : String(order.guest_count),
      check_in: order.check_in,
      check_out: order.check_out,
      guest_name: order.guest_name || '',
      phone: order.phone || '',
      remark: order.remark || '',
    })
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '订单详情读取失败'
  } finally {
    loading.value = false
  }
}

function handleClose(): void {
  if (!saving.value && !actionBusy.value) emit('close')
}

async function saveEdit(): Promise<void> {
  const order = detail.value
  if (!order) return
  errorMessage.value = ''

  if (canEditRoomDate.value) {
    if (!form.check_in || !form.check_out || form.check_out <= form.check_in) {
      errorMessage.value = '退房日期必须晚于入住日期'
      return
    }
    const oldNights = dayDifference(order.check_in, order.check_out)
    const newNights = dayDifference(form.check_in, form.check_out)
    if (newNights !== oldNights) {
      errorMessage.value = `校验失败：修改后晚数（${newNights}晚）必须与原订单（${oldNights}晚）一致`
      return
    }
  }
  if (form.guest_count && (!Number.isInteger(Number(form.guest_count)) || Number(form.guest_count) < 1)) {
    errorMessage.value = '入住人数须为正整数'
    return
  }

  const request: UpdateOrderRequest = {
    guest_name: form.guest_name.trim(),
    phone: form.phone.trim(),
    guest_count: form.guest_count ? Number(form.guest_count) : null,
    remark: form.remark.trim(),
  }
  if (canEditRoomDate.value) {
    request.room_id = Number(form.room_id)
    request.check_in = form.check_in
    request.check_out = form.check_out
  }

  saving.value = true
  try {
    await orderApi.update(order.id, request)
    emit('updated', `订单 ${order.order_no} 资料已更新`)
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '订单资料更新失败'
  } finally {
    saving.value = false
  }
}

async function runStatusAction(action: 'confirm' | 'check-in' | 'check-out'): Promise<void> {
  const order = detail.value
  if (!order) return
  actionBusy.value = true
  errorMessage.value = ''
  try {
    if (action === 'confirm') {
      await orderApi.confirm(order.id)
      emit('updated', `订单 ${order.order_no} 已确认，进入待入住`)
    } else if (action === 'check-in') {
      await orderApi.checkIn(order.id)
      emit('updated', `订单 ${order.order_no} 已办理入住`)
    } else {
      await orderApi.checkOut(order.id)
      emit('updated', `订单 ${order.order_no} 已办理退房完成`)
    }
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '状态更新失败'
  } finally {
    actionBusy.value = false
  }
}

async function confirmCancelExternal(): Promise<void> {
  const order = detail.value
  if (!order) return
  actionBusy.value = true
  errorMessage.value = ''
  try {
    await orderApi.cancel(order.id, cancelReason.value.trim())
    emit('updated', `第三方订单 ${order.order_no} 已取消，对应间夜已释放`)
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '订单取消失败'
  } finally {
    actionBusy.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="qx-modal-backdrop" @click.self="handleClose">
      <div class="qx-modal-panel qx-modal-lg" role="dialog" aria-modal="true" aria-labelledby="order-detail-title">
        <div class="qx-modal-header">
          <div>
            <div class="qx-modal-title-row">
              <h3 id="order-detail-title">
                订单详情<template v-if="detail"> · {{ detail.order_no }}</template>
              </h3>
              <span v-if="detail" class="qx-status-pill" :class="`is-${detail.status.toLowerCase()}`">
                {{ orderStatusLabels[detail.status] }}
              </span>
            </div>
            <p v-if="detail">
              来源：{{ detail.channel_name }}
              <template v-if="creatorName"> · 主播：{{ creatorName }}</template>
              · 住宿 {{ detail.nights }} 晚
              <template v-if="detail.platform_order_no"> · 外部单号：{{ detail.platform_order_no }}</template>
            </p>
          </div>
          <button type="button" class="qx-modal-close" aria-label="关闭" @click="handleClose">
            <svg viewBox="0 0 24 24" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          </button>
        </div>

        <AdminNoticeDialog v-model:message="errorMessage" />

        <div v-if="loading" class="qx-modal-loading">正在读取订单详情…</div>

        <form v-else-if="detail" class="qx-modal-form" @submit.prevent="saveEdit">
          <div class="qx-snapshot-box">
            <div class="qx-snapshot-head">
              <strong>订单金额与提成快照（创建后锁定不可改）</strong>
              <span>自动计算到手金额</span>
            </div>
            <div class="qx-snapshot-grid">
              <div class="qx-snapshot-item">
                <small>1. 顾客实付金额</small>
                <strong class="is-cinnabar money-value is-lg">¥{{ detail.paid_amount }}</strong>
              </div>
              <div class="qx-snapshot-item">
                <small>2. 平台手续费</small>
                <strong class="money-value">¥{{ detail.platform_fee }}</strong>
              </div>
              <div class="qx-snapshot-item">
                <small>3. 平台达人提成</small>
                <strong class="money-value">¥{{ detail.platform_commission }}</strong>
              </div>
              <div class="qx-snapshot-item">
                <small>4. 达人额外提成</small>
                <strong class="money-value">¥{{ detail.extra_commission }}</strong>
              </div>
              <div class="qx-snapshot-item">
                <small>5. 预期收入</small>
                <strong class="money-value">{{ detail.expected_amount != null ? `¥${detail.expected_amount}` : '—' }}</strong>
              </div>
              <div class="qx-snapshot-item is-net">
                <small>6. 商家到手金额</small>
                <strong class="is-celadon money-value is-lg">¥{{ detail.net_amount }}</strong>
              </div>
            </div>
          </div>

          <div class="qx-form-grid-2">
            <label class="qx-field">
              <span>房间 {{ canEditRoomDate ? '' : '(当前状态锁定)' }}</span>
              <select v-model="form.room_id" :disabled="!canEditRoomDate">
                <option v-for="room in rooms" :key="room.id" :value="String(room.id)">
                  {{ room.number }} · {{ room.name }}
                </option>
              </select>
            </label>

            <label class="qx-field">
              <span>入住人数</span>
              <input v-model="form.guest_count" type="number" min="1" max="10" />
            </label>

            <label class="qx-field">
              <span>入住日期 {{ canEditRoomDate ? `(须保持${detail.nights}晚)` : '(锁定)' }}</span>
              <input v-model="form.check_in" type="date" :disabled="!canEditRoomDate" />
            </label>

            <label class="qx-field">
              <span>退房日期 {{ canEditRoomDate ? `(须保持${detail.nights}晚)` : '(锁定)' }}</span>
              <input v-model="form.check_out" type="date" :disabled="!canEditRoomDate" />
            </label>

            <label class="qx-field">
              <span>预订人姓名</span>
              <input v-model="form.guest_name" type="text" />
            </label>

            <label class="qx-field">
              <span>预订手机号码</span>
              <input v-model="form.phone" type="text" />
            </label>

            <label class="qx-field qx-span-2">
              <span>订单备注</span>
              <input v-model="form.remark" type="text" />
            </label>
          </div>

          <div v-if="cancelConfirmOpen" class="qx-cancel-confirm">
            <label class="qx-field">
              <span>取消第三方订单 {{ detail.order_no }}（选填取消原因）</span>
              <input v-model="cancelReason" type="text" placeholder="如：客人改期 / 平台退单" />
            </label>
            <div class="qx-cancel-actions">
              <button type="button" class="qx-btn-ghost" :disabled="actionBusy" @click="cancelConfirmOpen = false">
                返回
              </button>
              <button type="button" class="qx-btn-danger-solid" :disabled="actionBusy" @click="confirmCancelExternal">
                {{ actionBusy ? '正在取消…' : '确认取消并释放房态' }}
              </button>
            </div>
          </div>

          <div class="qx-modal-footer">
            <div class="qx-footer-left">
              <button
                v-if="nextStatusAction"
                type="button"
                class="qx-btn-status"
                :class="nextStatusAction.tone"
                :disabled="actionBusy || saving"
                @click="runStatusAction(nextStatusAction.action)"
              >
                {{ actionBusy ? '处理中…' : nextStatusAction.label }}
              </button>
              <button
                v-if="canCancelExternal && !cancelConfirmOpen"
                type="button"
                class="qx-btn-danger-outline"
                :disabled="actionBusy || saving"
                @click="cancelConfirmOpen = true"
              >
                取消第三方订单并释放房间
              </button>
            </div>

            <div class="qx-footer-right">
              <button type="button" class="qx-btn-ghost" :disabled="saving || actionBusy" @click="handleClose">关闭</button>
              <button type="submit" class="qx-btn-primary" :disabled="saving || actionBusy">
                {{ saving ? '保存中…' : '保存修改' }}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
