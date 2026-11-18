<script setup lang="ts">
/** M03–M05 会员管理：手机号检索、预建会员、查看关联订单和发放积分。 */
import { onMounted, reactive, ref } from 'vue'
import { memberApi } from '../../api/members'
import { pointsApi } from '../../api/points'
import type { MemberDetail, MemberListItem, PointsGrantRequest, PointsLog, PointsLogType } from '../../types/members'
import { orderStatusLabels } from '../../types/orders'

const rows = ref<MemberListItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const phoneFilter = ref('')
const createOpen = ref(false)
const createPhone = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<MemberDetail | null>(null)
const detailLogs = ref<PointsLog[]>([])
const detailLogTotal = ref(0)
const detailLogPage = ref(1)
const detailLogsLoadingMore = ref(false)
const grantOpen = ref(false)
const grantTarget = ref<MemberListItem | MemberDetail | null>(null)
const grantForm = reactive({ type: '' as '' | 'HISTORY_REWARD' | 'GIFT', points: '', order_id: '', remark: '', request_no: '' })
const retryingOrderIds = ref<number[]>([])

onMounted(() => { void loadMembers() })

async function loadMembers(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await memberApi.list({ phone: phoneFilter.value.trim() || undefined, page: page.value, size: pageSize })
    rows.value = result.list
    total.value = result.total
  } catch (error) {
    errorMessage.value = messageOf(error, '会员列表读取失败')
  } finally {
    loading.value = false
  }
}

async function search(): Promise<void> {
  page.value = 1
  await loadMembers()
}

function beginCreate(): void {
  createPhone.value = ''
  errorMessage.value = ''
  createOpen.value = true
}

async function createMember(): Promise<void> {
  const phone = createPhone.value.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    errorMessage.value = '请输入有效的中国大陆手机号'
    return
  }
  saving.value = true
  errorMessage.value = ''
  try {
    const created = await memberApi.create(phone)
    createOpen.value = false
    successMessage.value = '会员已创建。初始积分请通过积分流水登记。'
    page.value = 1
    await loadMembers()
    beginGrant({ id: created.id, phone: created.phone, nickname: phone.slice(-4), growth_points: 0, points: 0, level: '普通', created_at: '' })
  } catch (error) {
    errorMessage.value = messageOf(error, '会员创建失败')
  } finally {
    saving.value = false
  }
}

async function showDetail(member: MemberListItem): Promise<void> {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  detailLogs.value = []
  detailLogPage.value = 1
  errorMessage.value = ''
  try {
    const profile = await memberApi.detail(member.id)
    detail.value = profile
    detailLogs.value = profile.points_logs
    detailLogTotal.value = profile.points_log_total
  } catch (error) {
    errorMessage.value = messageOf(error, '会员详情读取失败')
  } finally {
    detailLoading.value = false
  }
}

async function loadMoreLogs(): Promise<void> {
  if (detailLogsLoadingMore.value || !detail.value || detailLogs.value.length >= detailLogTotal.value) return
  const nextPage = detailLogPage.value + 1
  detailLogsLoadingMore.value = true
  try {
    const result = await pointsApi.list({ member_id: detail.value.id, page: nextPage, size: 20 })
    detailLogs.value = detailLogs.value.concat(result.list)
    detailLogPage.value = nextPage
  } catch (error) {
    errorMessage.value = messageOf(error, '积分流水读取失败')
  } finally {
    detailLogsLoadingMore.value = false
  }
}

function beginGrant(member: MemberListItem | MemberDetail): void {
  detailOpen.value = false
  grantTarget.value = member
  grantForm.type = ''
  grantForm.points = ''
  grantForm.order_id = ''
  grantForm.remark = ''
  grantForm.request_no = newRequestNo()
  errorMessage.value = ''
  grantOpen.value = true
}

async function submitGrant(): Promise<void> {
  if (!grantTarget.value) return
  const points = Number(grantForm.points)
  const orderId = grantForm.order_id.trim() ? Number(grantForm.order_id) : undefined
  if (!grantForm.type || !Number.isSafeInteger(points) || points <= 0
      || (orderId !== undefined && (!Number.isSafeInteger(orderId) || orderId <= 0))) {
    errorMessage.value = '请选择发放类型，并填写正整数积分和有效的关联订单编号'
    return
  }
  const payload: PointsGrantRequest = {
    type: grantForm.type,
    points,
    ...(orderId === undefined ? {} : { order_id: orderId }),
    ...(grantForm.remark.trim() ? { remark: grantForm.remark.trim() } : {}),
    request_no: grantForm.request_no,
  }
  saving.value = true
  errorMessage.value = ''
  try {
    const result = await memberApi.grant(grantTarget.value.id, payload)
    grantOpen.value = false
    successMessage.value = `已发放 ${result.points} 积分，当前可用余额 ${result.balance_after}`
    await loadMembers()
    if (detail.value?.id === grantTarget.value.id) await showDetail(detail.value)
  } catch (error) {
    errorMessage.value = messageOf(error, '积分发放失败；可使用相同请求内容安全重试')
  } finally {
    saving.value = false
  }
}

async function retryReward(orderId: number): Promise<void> {
  if (retryingOrderIds.value.includes(orderId)) return
  retryingOrderIds.value = [...retryingOrderIds.value, orderId]
  errorMessage.value = ''
  try {
    const result = await pointsApi.retryReward(orderId)
    successMessage.value = result.rewarded ? `订单奖励流水已确认（流水 #${result.log_id}）` : '该订单暂不符合奖励条件，未生成积分流水'
    if (detail.value) await showDetail(detail.value)
  } catch (error) {
    errorMessage.value = messageOf(error, '订单奖励重试失败')
  } finally {
    retryingOrderIds.value = retryingOrderIds.value.filter((id) => id !== orderId)
  }
}

function closeDialog(): void {
  if (!saving.value) {
    createOpen.value = false
    grantOpen.value = false
    detailOpen.value = false
  }
}

function typeLabel(type: PointsLogType): string {
  const labels: Record<PointsLogType, string> = {
    ORDER_REWARD: '订单奖励', HISTORY_REWARD: '历史消费补发', GIFT: '赠送/补偿',
    REDEEM: '积分抵扣', RETURN: '退款返还', ADJUST: '积分调整',
  }
  return labels[type]
}

function statusLabel(status: MemberDetail['orders'][number]['status']): string {
  return orderStatusLabels[status]
}

function newRequestNo(): string {
  return `grant-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
</script>

<template>
  <section class="catalog-page">
    <div class="page-heading-row">
      <div><p class="page-eyebrow">MEMBERS</p><h1>会员管理</h1><p class="page-description">按手机号查找会员，积分变动统一登记到积分流水。</p></div>
      <button class="button button-primary" type="button" @click="beginCreate">新增会员</button>
    </div>

    <form class="filter-card" @submit.prevent="search">
      <label class="filter-name">手机号<input v-model="phoneFilter" maxlength="20" placeholder="输入完整或部分手机号" /></label>
      <button class="button button-outline" type="submit" :disabled="loading">查询</button>
      <button class="button button-quiet" type="button" :disabled="loading" @click="phoneFilter = ''; search()">重置</button>
    </form>

    <p v-if="successMessage" class="notice notice-success" role="status">{{ successMessage }}</p>
    <p v-if="errorMessage && !createOpen && !grantOpen" class="notice notice-error" role="alert">{{ errorMessage }}</p>

    <div class="table-card">
      <div v-if="loading" class="table-state">正在读取会员资料…</div>
      <div v-else-if="rows.length === 0" class="table-state"><strong>没有匹配的会员</strong><span>可以新增会员，或调整手机号条件。</span></div>
      <div v-else class="table-scroll">
        <table class="data-table members-table">
          <thead><tr><th>会员</th><th>手机号</th><th>等级</th><th>成长积分</th><th>可用积分</th><th>注册时间</th><th class="actions-column">操作</th></tr></thead>
          <tbody>
            <tr v-for="member in rows" :key="member.id">
              <td><strong>{{ member.nickname }}</strong><small>#{{ member.id }}</small></td>
              <td>{{ member.phone }}</td><td><span class="member-level-pill">{{ member.level }}</span></td>
              <td>{{ member.growth_points }}</td><td>{{ member.points }}</td><td>{{ member.created_at.replace('T', ' ').slice(0, 16) }}</td>
              <td class="row-actions"><button class="button button-quiet" type="button" @click="showDetail(member)">详情</button><button class="button button-quiet" type="button" @click="beginGrant(member)">发放积分</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="member-pagination"><span>共 {{ total }} 位会员</span><div><button class="button button-outline" :disabled="page <= 1 || loading" @click="page--; loadMembers()">上一页</button><span>第 {{ page }} 页</span><button class="button button-outline" :disabled="page * pageSize >= total || loading" @click="page++; loadMembers()">下一页</button></div></div>
    </div>

    <div v-if="createOpen" class="dialog-backdrop" @click.self="closeDialog">
      <section class="form-dialog compact-dialog" role="dialog" aria-modal="true" aria-label="新增会员">
        <header class="dialog-header"><div><p class="page-eyebrow">NEW MEMBER</p><h2>新增会员</h2></div><button class="icon-button" type="button" aria-label="关闭" @click="closeDialog">×</button></header>
        <form class="catalog-form" @submit.prevent="createMember">
          <label>会员手机号<input v-model="createPhone" maxlength="20" inputmode="numeric" placeholder="请输入中国大陆手机号" required /></label>
          <p class="form-help">昵称自动取手机号后四位，头像使用系统默认头像。初始积分需通过积分流水单独登记。</p>
          <p v-if="errorMessage" class="notice notice-error">{{ errorMessage }}</p>
          <div class="dialog-actions"><button class="button button-outline" type="button" :disabled="saving" @click="closeDialog">取消</button><button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '正在创建…' : '创建会员' }}</button></div>
        </form>
      </section>
    </div>

    <div v-if="grantOpen && grantTarget" class="dialog-backdrop" @click.self="closeDialog">
      <section class="form-dialog compact-dialog" role="dialog" aria-modal="true" aria-label="发放积分">
        <header class="dialog-header"><div><p class="page-eyebrow">POINTS GRANT</p><h2>发放积分</h2><p class="page-description">{{ grantTarget.nickname }} · {{ grantTarget.phone }}</p></div><button class="icon-button" type="button" aria-label="关闭" @click="closeDialog">×</button></header>
        <form class="catalog-form" @submit.prevent="submitGrant">
          <label>积分类型<select v-model="grantForm.type" required><option value="" disabled>请选择类型</option><option value="HISTORY_REWARD">历史消费补发（计入成长积分）</option><option value="GIFT">赠送/补偿（不计成长积分）</option></select></label>
          <label>发放积分<input v-model="grantForm.points" type="number" min="1" step="1" inputmode="numeric" placeholder="请输入正整数" required /></label>
          <label>关联订单编号（选填）<input v-model="grantForm.order_id" type="number" min="1" step="1" placeholder="订单 ID" /></label>
          <label>备注（选填）<textarea v-model="grantForm.remark" maxlength="300" placeholder="说明发放原因"></textarea></label>
          <p class="form-help">余额与流水会在同一事务写入。网络异常后请用相同内容重试，系统会按请求编号去重。</p>
          <p v-if="errorMessage" class="notice notice-error">{{ errorMessage }}</p>
          <div class="dialog-actions"><button class="button button-outline" type="button" :disabled="saving" @click="closeDialog">取消</button><button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '正在发放…' : '确认发放' }}</button></div>
        </form>
      </section>
    </div>

    <div v-if="detailOpen" class="dialog-backdrop" @click.self="closeDialog">
      <section class="form-dialog member-detail-dialog" role="dialog" aria-modal="true" aria-label="会员详情">
        <header class="dialog-header"><div><p class="page-eyebrow">MEMBER PROFILE</p><h2>会员详情</h2></div><button class="icon-button" type="button" aria-label="关闭" @click="closeDialog">×</button></header>
        <div v-if="detailLoading" class="table-state">正在读取会员详情…</div>
        <template v-else-if="detail">
          <div class="member-detail-summary"><div><small>手机号</small><strong>{{ detail.phone }}</strong></div><div><small>会员等级</small><strong>{{ detail.level }}</strong></div><div><small>成长积分</small><strong>{{ detail.growth_points }}</strong></div><div><small>可用积分</small><strong>{{ detail.points }}</strong></div></div>
          <div class="detail-title-row"><h3>关联消费订单</h3><span>{{ detail.orders.length }} 笔</span></div>
          <div v-if="detail.orders.length === 0" class="table-state compact-state">暂无关联订单</div>
          <div v-else class="table-scroll detail-table-wrap"><table class="data-table"><thead><tr><th>订单</th><th>客房</th><th>入住区间</th><th>状态</th><th>实付</th><th>奖励操作</th></tr></thead><tbody>
            <tr v-for="order in detail.orders" :key="order.id"><td><strong>{{ order.order_no }}</strong><small>#{{ order.id }}</small></td><td>{{ order.room_number }} 房</td><td>{{ order.check_in }} 至 {{ order.check_out }}</td><td>{{ statusLabel(order.status) }}</td><td class="price-cell">¥{{ order.paid_amount }}</td><td><button v-if="order.status === 'COMPLETED'" class="button button-quiet" type="button" :disabled="retryingOrderIds.includes(order.id)" @click="retryReward(order.id)">{{ retryingOrderIds.includes(order.id) ? '处理中…' : '重试奖励' }}</button><span v-else>—</span></td></tr>
          </tbody></table></div>
          <div class="detail-title-row"><h3>积分流水</h3><span>{{ detailLogTotal }} 条</span></div>
          <div v-if="detailLogs.length === 0" class="table-state compact-state">暂无积分流水</div>
          <div v-else class="table-scroll detail-table-wrap"><table class="data-table"><thead><tr><th>类型</th><th>变动积分</th><th>成长积分</th><th>余额</th><th>关联订单</th><th>时间/备注</th></tr></thead><tbody>
            <tr v-for="log in detailLogs" :key="log.id"><td>{{ typeLabel(log.type) }}</td><td :class="log.points >= 0 ? 'points-positive' : 'points-negative'">{{ log.points > 0 ? '+' : '' }}{{ log.points }}</td><td>{{ log.growth_points > 0 ? '+' : '' }}{{ log.growth_points }}</td><td>{{ log.balance_after }}</td><td>{{ log.order_id || '—' }}</td><td><strong>{{ log.created_at.replace('T', ' ').slice(0, 16) }}</strong><small>{{ log.remark || '—' }}</small></td></tr>
          </tbody></table></div>
          <button v-if="detailLogs.length < detailLogTotal" class="button button-outline load-more-button" type="button" :disabled="detailLogsLoadingMore" @click="loadMoreLogs">{{ detailLogsLoadingMore ? '正在加载…' : '加载更多流水' }}</button>
          <p v-if="errorMessage" class="notice notice-error">{{ errorMessage }}</p>
          <div class="dialog-actions"><button class="button button-outline" type="button" @click="beginGrant(detail)">发放积分</button><button class="button button-primary" type="button" @click="closeDialog">关闭</button></div>
        </template>
      </section>
    </div>
  </section>
</template>
