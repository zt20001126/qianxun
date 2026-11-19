<script setup lang="ts">
/** S01 经营总览页面，展示所选业务日期房态和月份经营指标。 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { reportApi } from '../../api/reports'
import type { Dashboard, DashboardRoom } from '../../types/operations'
import '../../styles/operations.css'

const router = useRouter()
const dashboard = ref<Dashboard | null>(null)
const date = ref(shanghaiDate())
const month = ref(shanghaiMonth())
const loading = ref(false)
const error = ref('')

const summaryCards = computed((): { label: string; value: string; highlight?: boolean; profit?: boolean }[] => {
  const summary = dashboard.value?.month_summary
  if (!summary) return []
  return [
    { label: '未入住订单营业额', value: summary.unoccupied_order_revenue },
    { label: '已入住订单营业额', value: summary.checked_in_order_revenue },
    { label: '平台手续费', value: summary.platform_fee },
    { label: '平台达人提成', value: summary.platform_commission },
    { label: '达人额外提成', value: summary.extra_commission },
    { label: '商家到手金额', value: summary.net_amount, highlight: true },
    { label: '本月经营支出', value: summary.expense_amount },
    { label: '经营净利润', value: summary.operating_profit, profit: true },
  ]
})

async function load(): Promise<void> {
  if (!date.value || !month.value) return
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await reportApi.dashboard({ date: date.value, month: month.value })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '经营总览加载失败'
  } finally {
    loading.value = false
  }
}

function roomStatusLabel(room: DashboardRoom): string {
  const labels: Record<string, string> = {
    AVAILABLE: '空房',
    BLOCKED: '不可预订',
    PENDING_CONFIRM: '待确认',
    PENDING_CHECKIN: '待入住',
    CHECKED_IN: '已入住',
    COMPLETED: '已退房',
    OCCUPIED: '连续住宿',
  }
  return labels[room.status] || room.status
}

function roomStatusClass(status: string): string {
  if (status === 'PENDING_CONFIRM' || status === 'PENDING_CHECKIN') return 'is-pending'
  if (status === 'OCCUPIED' || status === 'CHECKED_IN') return 'is-occupied'
  if (status === 'BLOCKED') return 'is-blocked'
  if (status === 'COMPLETED') return 'is-complete'
  return ''
}

function percentage(rate: number | null | undefined): string {
  if (rate == null) return '—'
  return (rate * 100).toFixed(1) + '%'
}

function openOrders(room: DashboardRoom): void {
  void router.push(room.order_id
    ? { path: '/orders', query: { calendar_action: 'detail', order_id: String(room.order_id) } }
    : '/orders')
}

function shanghaiDate(): string {
  return formatShanghaiDate(new Date())
}

function shanghaiMonth(): string {
  return shanghaiDate().slice(0, 7)
}

function formatShanghaiDate(value: Date): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(value)
  const part = (type: string) => parts.find((item) => item.type === type)?.value || ''
  return part('year') + '-' + part('month') + '-' + part('day')
}

onMounted(() => void load())
</script>

<template>
  <section class="operation-page">
    <div class="page-heading-row">
      <div>
        <p class="page-eyebrow">DAILY DASHBOARD</p>
        <h1>经营总览</h1>
        <p class="page-description">房态按指定日期展示，经营指标按所选住宿月份统计。</p>
      </div>
      <div class="operation-date-controls">
        <label>房态日期<input v-model="date" type="date" @change="load" /></label>
        <label>经营月份<input v-model="month" type="month" @change="load" /></label>
        <button class="button button-outline" type="button" @click="load" :disabled="loading">查询</button>
      </div>
    </div>

    <p v-if="error" class="notice notice-error">{{ error }}</p>

    <template v-if="dashboard">
      <section class="operation-card">
        <div class="operation-card-heading">
          <h2>{{ month }} 经营指标</h2>
          <span class="operation-muted">入住率 {{ percentage(dashboard.month_summary.occupancy_rate) }}</span>
        </div>
        <div class="operation-kpis">
          <article
            v-for="card in summaryCards"
            :key="card.label"
            class="operation-kpi"
            :class="{ 'is-highlight': card.highlight, 'is-profit': card.profit }"
          >
            <small>{{ card.label }}</small>
            <strong>¥ {{ card.value }}</strong>
          </article>
        </div>
      </section>

      <section class="operation-card">
        <div class="operation-card-heading">
          <h2>{{ date }} 房态</h2>
          <span class="operation-muted">占用 {{ dashboard.occupied_rooms }} / {{ dashboard.total_rooms }} 间</span>
        </div>
        <div v-if="loading" class="operation-empty">正在加载房态…</div>
        <div v-else-if="dashboard.rooms.length" class="dashboard-room-grid">
          <button
            v-for="room in dashboard.rooms"
            :key="room.room_id"
            class="dashboard-room"
            type="button"
            @click="openOrders(room)"
          >
            <span class="dashboard-room-top">
              <span><strong>{{ room.room_number }}</strong><span class="dashboard-room-name">{{ room.room_name }}</span></span>
              <span class="room-status" :class="roomStatusClass(room.status)">{{ roomStatusLabel(room) }}</span>
            </span>
            <span class="dashboard-room-detail">
              <span v-if="room.guest_name"><strong>{{ room.guest_name }}</strong> · 预订 {{ room.guest_count || 0 }} 位</span>
              <span v-if="room.check_in && room.check_out">{{ room.check_in }} 至 {{ room.check_out }}</span>
              <span v-if="room.channel_name">来源：{{ room.channel_name }}</span>
              <span v-if="!room.order_id && room.status === 'BLOCKED'">管理员封房</span>
            </span>
          </button>
        </div>
        <div v-else class="operation-empty">当前没有客房数据。</div>
      </section>
    </template>
    <div v-else-if="loading" class="operation-card operation-empty">正在加载经营总览…</div>
  </section>
</template>
