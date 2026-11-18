<script setup lang="ts">
/** T04 后台积分流水查询：支持按会员、订单与变更类型筛选。 */
import { onMounted, reactive, ref } from 'vue'
import { pointsApi } from '../../api/points'
import type { PointsLog, PointsLogType } from '../../types/members'

const rows = ref<PointsLog[]>([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const errorMessage = ref('')
const filters = reactive({ member_id: '', order_id: '', type: '' as '' | PointsLogType })
const typeOptions: { value: PointsLogType; label: string }[] = [
  { value: 'ORDER_REWARD', label: '订单奖励' },
  { value: 'HISTORY_REWARD', label: '历史消费补发' },
  { value: 'GIFT', label: '赠送/补偿' },
  { value: 'REDEEM', label: '积分抵扣' },
  { value: 'RETURN', label: '退款返还' },
  { value: 'ADJUST', label: '积分调整' },
]

onMounted(() => { void loadLogs() })

async function loadLogs(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await pointsApi.list({
      member_id: filters.member_id ? Number(filters.member_id) : undefined,
      order_id: filters.order_id ? Number(filters.order_id) : undefined,
      type: filters.type || undefined,
      page: page.value,
      size,
    })
    rows.value = result.list
    total.value = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '积分流水读取失败'
  } finally {
    loading.value = false
  }
}

async function search(): Promise<void> {
  page.value = 1
  await loadLogs()
}

function typeLabel(type: PointsLogType): string {
  return typeOptions.find((item) => item.value === type)?.label || type
}
</script>

<template>
  <section class="catalog-page">
    <div class="page-heading-row"><div><p class="page-eyebrow">POINTS LEDGER</p><h1>积分流水</h1><p class="page-description">所有余额变化均可追溯到积分类型、来源及变动后余额。</p></div></div>
    <form class="filter-card" @submit.prevent="search">
      <label>会员 ID<input v-model="filters.member_id" type="number" min="1" placeholder="全部会员" /></label>
      <label>订单 ID<input v-model="filters.order_id" type="number" min="1" placeholder="全部订单" /></label>
      <label>流水类型<select v-model="filters.type"><option value="">全部类型</option><option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
      <button class="button button-outline" type="submit" :disabled="loading">查询</button>
      <button class="button button-quiet" type="button" @click="filters.member_id = ''; filters.order_id = ''; filters.type = ''; search()">重置</button>
    </form>
    <p v-if="errorMessage" class="notice notice-error" role="alert">{{ errorMessage }}</p>
    <div class="table-card">
      <div v-if="loading" class="table-state">正在读取积分流水…</div>
      <div v-else-if="rows.length === 0" class="table-state"><strong>没有匹配的流水</strong><span>调整筛选条件后重试。</span></div>
      <div v-else class="table-scroll">
        <table class="data-table points-table"><thead><tr><th>会员</th><th>类型</th><th>积分变动</th><th>成长积分</th><th>余额</th><th>关联订单</th><th>金额</th><th>抵扣金额</th><th>时间与说明</th></tr></thead>
          <tbody><tr v-for="log in rows" :key="log.id"><td><strong>#{{ log.member_id }}</strong></td><td>{{ typeLabel(log.type) }}</td><td :class="log.points >= 0 ? 'points-positive' : 'points-negative'">{{ log.points > 0 ? '+' : '' }}{{ log.points }}</td><td>{{ log.growth_points > 0 ? '+' : '' }}{{ log.growth_points }}</td><td>{{ log.balance_after }}</td><td>{{ log.order_id || '—' }}</td><td>{{ log.amount === null ? '—' : `¥${log.amount}` }}</td><td>{{ log.discount_amount === null ? '—' : `¥${log.discount_amount}` }}</td><td><strong>{{ log.created_at.replace('T', ' ').slice(0, 16) }}</strong><small>{{ log.remark || '—' }}</small></td></tr></tbody>
        </table>
      </div>
      <div class="member-pagination"><span>共 {{ total }} 条流水</span><div><button class="button button-outline" :disabled="page <= 1 || loading" @click="page--; loadLogs()">上一页</button><span>第 {{ page }} 页</span><button class="button button-outline" :disabled="page * size >= total || loading" @click="page++; loadLogs()">下一页</button></div></div>
    </div>
  </section>
</template>
