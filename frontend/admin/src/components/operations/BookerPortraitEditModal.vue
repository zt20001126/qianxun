<script setup lang="ts">
/** 管理端编辑顾客画像资料；订单金额和订单状态不在此表单内。 */
import { ref, watch } from 'vue'
import BookerPortraitFields from './BookerPortraitFields.vue'
import { portraitApi } from '../../api/portraits'
import { orderStatusLabels } from '../../types/orders'
import '../../styles/operations.css'
import type {
  BookerPortraitEditableOrder,
  BookerPortraitInput,
  BookerPortraitListItem,
} from '../../types/portraits'

type OrderRemarkDraft = Omit<BookerPortraitEditableOrder, 'remark' | 'admin_remark'> & {
  remark: string
  admin_remark: string
}

const props = defineProps<{
  open: boolean
  profile: BookerPortraitListItem | null
}>()
const emit = defineEmits<{
  close: []
  saved: []
}>()

const saving = ref(false)
const error = ref('')
const form = ref<BookerPortraitInput>(emptyPortrait())
const adminRemark = ref('')
const orderRemarks = ref<OrderRemarkDraft[]>([])

watch(
  () => [props.open, props.profile] as const,
  ([open, profile]) => {
    if (!open || !profile) return
    error.value = ''
    form.value = {
      age: profile.age,
      party_type: profile.party_type,
      inferred_region: profile.inferred_region,
      tags: [...profile.tags],
    }
    adminRemark.value = profile.admin_remark || ''
    orderRemarks.value = profile.orders.map((order) => ({
      ...order,
      remark: order.remark || '',
      admin_remark: order.admin_remark || '',
    }))
  },
  { immediate: true },
)

async function save(): Promise<void> {
  const profile = props.profile
  if (!profile) return
  saving.value = true
  error.value = ''
  try {
    await portraitApi.update(profile.id, {
      age: form.value.age,
      party_type: form.value.party_type,
      inferred_region: form.value.inferred_region,
      tags: form.value.tags,
      admin_remark: adminRemark.value.trim() || null,
      order_remarks: orderRemarks.value.map((order) => ({
        order_id: order.order_id,
        remark: order.remark.trim() || null,
        admin_remark: order.admin_remark.trim() || null,
      })),
    })
    emit('saved')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '画像保存失败'
  } finally {
    saving.value = false
  }
}

function emptyPortrait(): BookerPortraitInput {
  return { age: null, party_type: null, inferred_region: null, tags: [] }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open && profile" class="qx-modal-backdrop" @click.self="!saving && emit('close')">
      <div class="qx-modal-panel qx-modal-lg portrait-edit-modal" role="dialog" aria-modal="true" aria-labelledby="portrait-edit-title">
        <div class="qx-modal-header">
          <div>
            <h3 id="portrait-edit-title">编辑顾客画像 · {{ profile.guest_name || '未填写姓名' }}</h3>
            <p>{{ profile.phone || '手机号未填写' }} · 完成订单仍可修改画像资料</p>
          </div>
          <button type="button" class="qx-modal-close" aria-label="关闭" @click="emit('close')">×</button>
        </div>
        <p v-if="error" class="portrait-edit-error" role="alert">{{ error }}</p>
        <BookerPortraitFields v-model="form" />
        <label class="portrait-profile-admin-remark">
          管理员画像备注
          <textarea v-model="adminRemark" maxlength="2000" rows="3" placeholder="持续记录该手机号顾客的管理备注" />
        </label>
        <section class="portrait-order-notes" aria-labelledby="portrait-order-notes-title">
          <div class="portrait-order-notes-heading">
            <h4 id="portrait-order-notes-title">订单备注</h4>
            <span>用户备注和管理员备注均可持续编辑，不受订单状态限制。</span>
          </div>
          <p v-if="!orderRemarks.length" class="portrait-order-notes-empty">该手机号暂无关联订单。</p>
          <div v-else class="portrait-order-notes-list">
            <article v-for="order in orderRemarks" :key="order.order_id" class="portrait-order-note-card">
              <header>
                <strong>{{ order.order_no }}</strong>
                <span>{{ order.check_in }} · {{ orderStatusLabels[order.status as keyof typeof orderStatusLabels] || order.status }}</span>
              </header>
              <label>
                用户备注
                <textarea v-model="order.remark" maxlength="500" rows="2" placeholder="编辑用户下单备注" />
              </label>
              <label>
                管理员备注
                <textarea v-model="order.admin_remark" maxlength="500" rows="2" placeholder="编辑管理员备注" />
              </label>
            </article>
          </div>
        </section>
        <div class="qx-modal-footer qx-footer-end">
          <button class="qx-btn-ghost" type="button" :disabled="saving" @click="emit('close')">取消</button>
          <button class="qx-btn-primary" type="button" :disabled="saving" @click="save">
            {{ saving ? '保存中…' : '保存画像' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.portrait-edit-modal { display: grid; gap: 14px; max-height: min(90vh, 900px); overflow-y: auto; }
.portrait-profile-admin-remark { display: grid; gap: 5px; color: #5f6a6d; font-size: 11px; }
.portrait-profile-admin-remark textarea { width: 100%; box-sizing: border-box; padding: 8px 9px; border: 1px solid #dce3e3; border-radius: 6px; color: #202b2d; background: #fff; font: inherit; resize: vertical; }
.portrait-edit-error { margin: 0; color: #b83b2a; font-size: 12px; }
.portrait-order-notes { padding: 15px; border: 1px solid #e4e8e8; border-radius: 10px; background: #fbfcfc; }
.portrait-order-notes-heading { display: flex; flex-wrap: wrap; align-items: baseline; gap: 8px; margin-bottom: 10px; }
.portrait-order-notes-heading h4 { margin: 0; color: #1f292b; font-size: 14px; }
.portrait-order-notes-heading span, .portrait-order-notes-empty { color: #6e777a; font-size: 11px; }
.portrait-order-notes-empty { margin: 0; }
.portrait-order-notes-list { display: grid; max-height: 340px; gap: 9px; overflow-y: auto; }
.portrait-order-note-card { display: grid; gap: 8px; padding: 10px; border: 1px solid #e4e8e8; border-radius: 8px; background: #fff; }
.portrait-order-note-card header { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px; color: #6e777a; font-size: 11px; }
.portrait-order-note-card header strong { color: #263234; }
.portrait-order-note-card label { display: grid; gap: 4px; color: #5f6a6d; font-size: 11px; }
.portrait-order-note-card textarea { width: 100%; box-sizing: border-box; padding: 7px 9px; border: 1px solid #dce3e3; border-radius: 6px; color: #202b2d; background: #fff; font: inherit; resize: vertical; }
</style>
