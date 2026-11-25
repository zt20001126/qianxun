<script setup lang="ts">
/** 管理员核对邀请订单并记录线下发放状态。 */
import { nextTick, onMounted, ref } from 'vue'
import { invitationApi } from '../../api/invitations'
import type { InvitationRecord, InvitationRewardFilter } from '../../types/invitations'

const rows = ref<InvitationRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const savingId = ref<number | null>(null)
const error = ref('')
const notice = ref('')
const confirmationTarget = ref<InvitationRecord | null>(null)
const confirmationText = ref('')
const confirmationError = ref('')
const confirmationInput = ref<HTMLInputElement | null>(null)
const requiredConfirmation = '已发放'
const phoneFilter = ref('')
const rewardStatusFilter = ref<InvitationRewardFilter>('ALL')
const appliedPhone = ref('')
const appliedRewardStatus = ref<InvitationRewardFilter>('ALL')
onMounted(() => { void load() })

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const result = await invitationApi.list(page.value, size, appliedPhone.value, appliedRewardStatus.value)
    rows.value = result.list
    total.value = result.total
  } catch (err) {
    error.value = err instanceof Error ? err.message : '邀请记录加载失败'
  } finally { loading.value = false }
}
/** 使用后台筛选并从第一页开始，避免仅筛选当前页。 */
async function search(): Promise<void> {
  const phone = phoneFilter.value.trim()
  if (phone && !/^[0-9]{1,11}$/.test(phone)) {
    error.value = '请输入不超过 11 位的手机号数字'
    return
  }
  appliedPhone.value = phone
  appliedRewardStatus.value = rewardStatusFilter.value
  page.value = 1
  await load()
}

async function resetFilters(): Promise<void> {
  phoneFilter.value = ''
  rewardStatusFilter.value = 'ALL'
  await search()
}

function rewardStatus(row: InvitationRecord): Exclude<InvitationRewardFilter, 'ALL'> {
  return !row.order_no ? 'NO_REWARD' : row.reward_issued ? 'ISSUED' : 'UNISSUED'
}

async function mark(row: InvitationRecord): Promise<void> {
  if (savingId.value !== null || !row.order_no || row.reward_issued) return
  confirmationTarget.value = row
  confirmationText.value = ''
  confirmationError.value = ''
  await nextTick()
  confirmationInput.value?.focus()
}

/** 只有输入完整确认文字后才登记系统外发放。 */
async function confirmIssued(): Promise<void> {
  if (!confirmationTarget.value || confirmationText.value !== requiredConfirmation || savingId.value !== null) return
  await saveIssued(confirmationTarget.value)
}

function closeConfirmation(): void {
  if (savingId.value !== null) return
  confirmationTarget.value = null
  confirmationText.value = ''
  confirmationError.value = ''
}

async function saveIssued(row: InvitationRecord): Promise<void> {
  savingId.value = row.id
  error.value = ''
  notice.value = ''
  confirmationError.value = ''
  try {
    await invitationApi.setIssued(row.id)
    confirmationTarget.value = null
    confirmationText.value = ''
    notice.value = '已登记为已发放'
    await load()
  } catch (err) {
    const message = err instanceof Error ? err.message : '状态保存失败，请重试'
    confirmationError.value = message
  } finally { savingId.value = null }
}
</script>

<template>
  <section class="catalog-page">
    <div class="page-heading-row"><div><h1>邀请记录</h1><p class="page-description">核对首笔完成的房间订单及奖励；发放由管理员在系统外处理。</p></div></div>
    <form class="filter-card invitation-filters" @submit.prevent="search">
      <label class="filter-name">手机号<input v-model="phoneFilter" maxlength="11" inputmode="numeric" placeholder="邀请人或被邀请人手机号" /></label>
      <label>发放状态<select v-model="rewardStatusFilter"><option value="ALL">全部</option><option value="NO_REWARD">无奖励</option><option value="UNISSUED">未发放</option><option value="ISSUED">已发放</option></select></label>
      <button class="button button-outline" type="submit" :disabled="loading">查询</button>
      <button class="button button-quiet" type="button" :disabled="loading" @click="resetFilters">重置</button>
    </form>
    <p v-if="error" class="invitation-error" role="alert">{{ error }} <button type="button" class="button button-quiet" @click="load">重试</button></p>
    <p v-if="notice" class="invitation-notice" role="status">{{ notice }}</p>
    <div class="table-card">
      <div v-if="loading" class="table-state">正在读取邀请记录…</div>
      <div v-else-if="!rows.length" class="table-state">暂无邀请记录</div>
      <div v-else class="table-scroll"><table class="data-table">
        <thead><tr><th>邀请人</th><th>被邀请人</th><th>绑定时间</th><th>房间订单</th><th>实付金额</th><th>邀请奖励</th><th>发放状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="row in rows" :key="row.id" :class="`reward-row-${rewardStatus(row)}`">
          <td>{{ row.inviter_phone }}<small>会员 #{{ row.inviter_id }}</small></td>
          <td>{{ row.invitee_phone }}<small>会员 #{{ row.invitee_id }}</small></td>
          <td>{{ row.bound_at.replace('T', ' ').slice(0, 16) }}</td><td>{{ row.order_no || '—' }}</td>
          <td>{{ row.paid_amount == null ? '—' : `¥${row.paid_amount}` }}</td>
          <td>{{ row.reward_amount == null ? '—' : `¥${row.reward_amount}` }}</td>
          <td><span class="reward-status" :class="`reward-status-${rewardStatus(row)}`">{{ row.order_no ? (row.reward_issued ? '已发放' : '未发放') : '无奖励' }}</span></td>
          <td><button v-if="row.order_no && !row.reward_issued" type="button" class="button button-quiet" :disabled="savingId !== null" @click="mark(row)">{{ savingId === row.id ? '保存中…' : '标记已发放' }}</button><span v-else>—</span></td>
        </tr></tbody>
      </table></div>
      <div class="member-pagination"><span>共 {{ total }} 条</span><div><button class="button button-outline" :disabled="page <= 1 || loading" @click="page--; load()">上一页</button><span>第 {{ page }} 页</span><button class="button button-outline" :disabled="page * size >= total || loading" @click="page++; load()">下一页</button></div></div>
    </div>
    <Teleport to="body">
      <div v-if="confirmationTarget" class="dialog-backdrop" @click.self="closeConfirmation" @keydown.esc="closeConfirmation">
        <section class="form-dialog compact-dialog" role="dialog" aria-modal="true" aria-labelledby="invitation-confirm-title" aria-describedby="invitation-confirm-help">
          <header class="dialog-header">
            <div><h2 id="invitation-confirm-title">确认发放邀请奖励</h2></div>
            <button class="icon-button" type="button" aria-label="关闭" :disabled="savingId !== null" @click="closeConfirmation">×</button>
          </header>
          <form class="catalog-form" @submit.prevent="confirmIssued">
            <p id="invitation-confirm-help">请确认已在系统外向 {{ confirmationTarget.inviter_phone }} 发放 ¥{{ confirmationTarget.reward_amount }}。此操作只记录状态，不会转账。</p>
            <label>请输入“已发放”以确认<input ref="confirmationInput" v-model="confirmationText" autocomplete="off" :disabled="savingId !== null" /></label>
            <p v-if="confirmationError" class="invitation-error" role="alert">{{ confirmationError }}</p>
            <div class="dialog-actions">
              <button class="button button-outline" type="button" :disabled="savingId !== null" @click="closeConfirmation">取消</button>
              <button class="button button-primary" type="submit" :disabled="confirmationText !== requiredConfirmation || savingId !== null">{{ savingId !== null ? '保存中…' : '确认发放' }}</button>
            </div>
          </form>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.invitation-filters { margin-bottom: 16px; }
.invitation-filters label:not(.filter-name) { min-width: 140px; }
.reward-row-NO_REWARD { background: #f8f9f9; }
.reward-row-UNISSUED { background: #fff8ee; }
.reward-row-ISSUED { background: #f1f8f3; }
.reward-status { display: inline-block; padding: 4px 9px; border-radius: 6px; font-weight: 600; }
.reward-status-NO_REWARD { color: #555f60; background: #e9eded; }
.reward-status-UNISSUED { color: #8a4b0c; background: #ffead0; }
.reward-status-ISSUED { color: #276039; background: #def0e3; }
.invitation-error { color: #a42e24; }
.invitation-notice { color: #32664b; }
</style>
