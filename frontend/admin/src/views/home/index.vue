<script setup lang="ts">
/** S01 经营总览：按日查看房态与分摊金额，按月查看经营汇总，支持页内快捷建单、详情编辑与封房调整。 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { channelApi } from '../../api/channels'
import { creatorApi } from '../../api/creators'
import { orderApi } from '../../api/orders'
import { reportApi } from '../../api/reports'
import { roomBlockApi } from '../../api/room-blocks'
import type { RoomBlock } from '../../api/room-blocks'
import { roomApi } from '../../api/rooms'
import ExternalOrderCreateModal from '../../components/operations/ExternalOrderCreateModal.vue'
import OrderDetailModal from '../../components/operations/OrderDetailModal.vue'
import RoomBlockModal from '../../components/operations/RoomBlockModal.vue'
import type { Channel, Creator, Room } from '../../types/catalog'
import type { Dashboard, DashboardRoom, ReportOverview } from '../../types/operations'
import type { OrderDetail } from '../../types/orders'
import {
  addAmounts,
  addDays,
  allocateNight,
  centsToAmount,
  countBlockedNightsInMonth,
  dayDifference,
  monthDayCount,
  roomStatusLabel,
  shanghaiDate,
  shiftMonthValue,
} from '../../utils/operations'
import '../../styles/operations.css'

type DashboardMode = 'day' | 'month'

interface DailyFinance {
  paid: string
  platformFee: string
  platformCommission: string
  extraCommission: string
  netAmount: string
}

interface SummaryCardItem {
  key: string
  label: string
  value: string
  icon: 'coins' | 'percent' | 'user-check' | 'award' | 'wallet'
  valueTone: 'cinnabar' | 'charcoal' | 'teal'
  highlight: boolean
  note: string
}

interface DashboardRoomView {
  room: DashboardRoom
  stateType: 'ORDER' | 'BLOCKED' | 'AVAILABLE'
  displayStatus: string
  statusLabel: string
  cardTone: string
  badgeTone: string
  order: OrderDetail | null
  block: RoomBlock | null
  nightsCount: number
  imageUrl: string
}

const router = useRouter()
const today = shanghaiDate()

const dashboard = ref<Dashboard | null>(null)
const monthOverview = ref<ReportOverview | null>(null)
const rooms = ref<Room[]>([])
const channels = ref<Channel[]>([])
const creators = ref<Creator[]>([])
const dateBlocks = ref<RoomBlock[]>([])
const monthBlocks = ref<RoomBlock[]>([])
const orderDetails = ref<Record<number, OrderDetail>>({})
const failedImageIds = ref<number[]>([])

const date = ref(today)
const month = ref(today.slice(0, 7))
const mode = ref<DashboardMode>('day')
const dailyFinance = ref<DailyFinance | null>(null)
const dailyFinanceError = ref('')
const loading = ref(false)
const error = ref('')
let loadSequence = 0

// 页内模态框状态
const createModalOpen = ref(false)
const createPresetRoomId = ref<number | null>(null)
const createPresetCheckIn = ref(today)

const detailModalOpen = ref(false)
const activeOrderId = ref<number | null>(null)

const blockModalOpen = ref(false)
const editingBlock = ref<RoomBlock | null>(null)
const blockPresetRoomId = ref<number | null>(null)

// Toast 提示
const toastMessage = ref('')
let toastTimer: number | null = null

const isToday = computed(() => date.value === today)
const statLabelPrefix = computed(() => {
  if (mode.value === 'month') return '本月'
  return isToday.value ? '今日' : '当日'
})

const roomViews = computed((): DashboardRoomView[] => {
  if (!dashboard.value) return []
  return dashboard.value.rooms.map((room) => {
    const order = room.order_id != null ? orderDetails.value[room.order_id] ?? null : null
    const block =
      room.status === 'BLOCKED'
        ? dateBlocks.value.find(
            (item) => item.room_id === room.room_id && item.start_date <= date.value && item.end_date > date.value,
          ) ?? null
        : null

    let stateType: 'ORDER' | 'BLOCKED' | 'AVAILABLE' = 'AVAILABLE'
    let displayStatus = room.status

    if (room.status === 'BLOCKED') {
      stateType = 'BLOCKED'
      displayStatus = 'BLOCKED'
    } else if (room.order_id != null) {
      stateType = 'ORDER'
      displayStatus = order?.status || room.status
    }

    const nightsCount =
      order?.nights
      ?? (room.check_in && room.check_out ? Math.max(1, dayDifference(room.check_in, room.check_out)) : 0)

    const imageUrl = failedImageIds.value.includes(room.room_id)
      ? ''
      : rooms.value.find((item) => item.id === room.room_id)?.images?.[0] || ''

    return {
      room,
      stateType,
      displayStatus,
      statusLabel: roomStatusLabel(displayStatus),
      cardTone: statusCardClass(displayStatus),
      badgeTone: statusBadgeClass(displayStatus),
      order,
      block,
      nightsCount,
      imageUrl,
    }
  })
})

const bookedRoomsCount = computed(() => roomViews.value.filter((item) => item.stateType === 'ORDER').length)
const blockedRoomsCount = computed(() => roomViews.value.filter((item) => item.stateType === 'BLOCKED').length)
const occupiedRoomsCount = computed(() => bookedRoomsCount.value + blockedRoomsCount.value)
const availableRoomsCount = computed(() =>
  Math.max(0, (dashboard.value?.total_rooms ?? 0) - occupiedRoomsCount.value),
)

const monthNightStats = computed(() => {
  const totalRooms = dashboard.value?.total_rooms ?? rooms.value.length
  const totalCalendarNights = totalRooms * monthDayCount(month.value)
  const nightCount = monthOverview.value?.night_count ?? 0
  const blockedNights = countBlockedNightsInMonth(monthBlocks.value, month.value)
  const freeNights = Math.max(0, totalCalendarNights - nightCount - blockedNights)
  return { nightCount, freeNights }
})

const occupancyRateDisplay = computed(() => {
  if (!dashboard.value) return '—'
  if (mode.value === 'month') {
    const rate = dashboard.value.month_summary.occupancy_rate
    return rate == null ? '—' : `${(rate * 100).toFixed(1)}%`
  }
  const sellableRooms = dashboard.value.total_rooms - blockedRoomsCount.value
  return sellableRooms > 0 ? `${((bookedRoomsCount.value / sellableRooms) * 100).toFixed(1)}%` : '0.0%'
})

const summaryCards = computed((): SummaryCardItem[] => {
  const prefix = statLabelPrefix.value
  const summary = dashboard.value?.month_summary
  const day = dailyFinance.value
  const isMonth = mode.value === 'month'

  const paid = isMonth
    ? monthOverview.value?.paid_amount
      ?? (summary ? addAmounts(summary.unoccupied_order_revenue, summary.checked_in_order_revenue) : '—')
    : day?.paid ?? '—'

  return [
    {
      key: 'paid',
      label: `${prefix}营业额`,
      value: paid,
      icon: 'coins',
      valueTone: 'cinnabar',
      highlight: false,
      note: '有效订单实付总额',
    },
    {
      key: 'fee',
      label: `${prefix}平台手续费`,
      value: isMonth ? summary?.platform_fee ?? '—' : day?.platformFee ?? '—',
      icon: 'percent',
      valueTone: 'charcoal',
      highlight: false,
      note: '渠道平台扣点费用',
    },
    {
      key: 'pcomm',
      label: `${prefix}平台达人提成`,
      value: isMonth ? summary?.platform_commission ?? '—' : day?.platformCommission ?? '—',
      icon: 'user-check',
      valueTone: 'charcoal',
      highlight: false,
      note: '平台结算达人佣金',
    },
    {
      key: 'ecomm',
      label: `${prefix}达人额外提成`,
      value: isMonth ? summary?.extra_commission ?? '—' : day?.extraCommission ?? '—',
      icon: 'award',
      valueTone: 'charcoal',
      highlight: false,
      note: '差额自动核算佣金',
    },
    {
      key: 'net',
      label: `${prefix}商家到手金额`,
      value: isMonth ? summary?.net_amount ?? '—' : day?.netAmount ?? '—',
      icon: 'wallet',
      valueTone: 'teal',
      highlight: true,
      note: '扣除手续费与提成后净入',
    },
  ]
})

async function loadCatalogs(): Promise<void> {
  const [channelList, creatorList] = await Promise.all([
    channelApi.list().catch(() => []),
    creatorApi.list({}).catch(() => []),
  ])
  channels.value = channelList
  creators.value = creatorList
}

async function load(): Promise<void> {
  if (!date.value || !month.value) return
  const sequence = ++loadSequence
  const selectedDate = date.value
  const selectedDateMonth = selectedDate.slice(0, 7)
  const selectedMonth = month.value

  loading.value = true
  error.value = ''
  dailyFinanceError.value = ''

  try {
    const needSeparateMonthBlocks = selectedMonth !== selectedDateMonth
    const [result, roomResult, overviewResult, dateBlockList, monthBlockList] = await Promise.all([
      reportApi.dashboard({ date: selectedDate, month: selectedMonth }),
      roomApi.list().catch(() => rooms.value),
      reportApi.overview({ month: selectedMonth, scope: 'ALL' }).catch(() => null),
      roomBlockApi.list(undefined, selectedDateMonth).catch(() => []),
      needSeparateMonthBlocks
        ? roomBlockApi.list(undefined, selectedMonth).catch(() => [])
        : Promise.resolve<RoomBlock[] | null>(null),
    ])
    if (sequence !== loadSequence) return

    dashboard.value = result
    rooms.value = roomResult
    monthOverview.value = overviewResult
    dateBlocks.value = dateBlockList
    monthBlocks.value = monthBlockList ?? dateBlockList
    failedImageIds.value = []

    const orderIds = [
      ...new Set(result.rooms.map((room) => room.order_id).filter((id): id is number => id != null)),
    ]
    const detailResults = await Promise.allSettled(orderIds.map((id) => orderApi.detail(id)))
    if (sequence !== loadSequence) return

    const details: Record<number, OrderDetail> = {}
    detailResults.forEach((entry, index) => {
      if (entry.status === 'fulfilled') details[orderIds[index]] = entry.value
    })
    orderDetails.value = details

    if (detailResults.some((entry) => entry.status === 'rejected')) {
      dailyFinance.value = null
      dailyFinanceError.value = '部分订单金额暂不可用，请稍后刷新。'
    } else {
      dailyFinance.value = calculateDailyFinance(Object.values(details), selectedDate)
    }
  } catch (cause) {
    if (sequence !== loadSequence) return
    error.value = cause instanceof Error ? cause.message : '经营总览加载失败'
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function calculateDailyFinance(orders: OrderDetail[], targetDate: string): DailyFinance {
  const totals = {
    paid: 0,
    platformFee: 0,
    platformCommission: 0,
    extraCommission: 0,
    netAmount: 0,
  }

  for (const order of orders) {
    if (order.status === 'CANCELLED') continue
    const nights = dayDifference(order.check_in, order.check_out)
    const nightIndex = dayDifference(order.check_in, targetDate)
    if (nights <= 0 || nightIndex < 0 || nightIndex >= nights) continue

    totals.paid += allocateNight(order.paid_amount, nightIndex, nights)
    totals.platformFee += allocateNight(order.platform_fee, nightIndex, nights)
    totals.platformCommission += allocateNight(order.platform_commission, nightIndex, nights)
    totals.extraCommission += allocateNight(order.extra_commission, nightIndex, nights)
    totals.netAmount += allocateNight(order.net_amount, nightIndex, nights)
  }

  return {
    paid: centsToAmount(totals.paid),
    platformFee: centsToAmount(totals.platformFee),
    platformCommission: centsToAmount(totals.platformCommission),
    extraCommission: centsToAmount(totals.extraCommission),
    netAmount: centsToAmount(totals.netAmount),
  }
}

function setMode(nextMode: DashboardMode): void {
  mode.value = nextMode
  void load()
}

function setDate(nextDate: string): void {
  if (!nextDate) return
  mode.value = 'day'
  date.value = nextDate
  month.value = nextDate.slice(0, 7)
  void load()
}

function shiftDate(delta: number): void {
  setDate(addDays(date.value, delta))
}

function onDateChange(event: Event): void {
  const target = event.target
  if (target instanceof HTMLInputElement && target.value) setDate(target.value)
}

function onMonthChange(event: Event): void {
  const target = event.target
  if (!(target instanceof HTMLInputElement) || !target.value) return
  mode.value = 'month'
  month.value = target.value
  void load()
}

function shiftMonth(delta: number): void {
  mode.value = 'month'
  month.value = shiftMonthValue(month.value, delta)
  void load()
}

function markImageFailed(roomId: number): void {
  if (!failedImageIds.value.includes(roomId)) {
    failedImageIds.value = [...failedImageIds.value, roomId]
  }
}

function statusCardClass(status: string): string {
  const map: Record<string, string> = {
    AVAILABLE: 'is-available',
    BLOCKED: 'is-blocked',
    PENDING_CONFIRM: 'is-reserved',
    PENDING_CHECKIN: 'is-pending',
    CHECKED_IN: 'is-checked-in',
    OCCUPIED: 'is-checked-in',
    COMPLETED: 'is-completed',
  }
  return map[status] || 'is-available'
}

function statusBadgeClass(status: string): string {
  const map: Record<string, string> = {
    AVAILABLE: 'is-available',
    BLOCKED: 'is-blocked',
    PENDING_CONFIRM: 'is-reserved',
    PENDING_CHECKIN: 'is-pending',
    CHECKED_IN: 'is-checked-in',
    OCCUPIED: 'is-checked-in',
    COMPLETED: 'is-completed',
  }
  return map[status] || 'is-available'
}

function openRoomCard(view: DashboardRoomView): void {
  if (view.stateType === 'ORDER' && view.room.order_id != null) {
    activeOrderId.value = view.room.order_id
    detailModalOpen.value = true
    return
  }
  if (view.stateType === 'BLOCKED') {
    if (view.block) {
      editingBlock.value = view.block
      blockPresetRoomId.value = view.room.room_id
      blockModalOpen.value = true
    } else {
      void router.push('/room-blocks')
    }
    return
  }
  createPresetRoomId.value = view.room.room_id
  createPresetCheckIn.value = date.value
  createModalOpen.value = true
}

function goToCalendar(): void {
  void router.push('/room-calendar')
}

function showToast(message: string): void {
  toastMessage.value = message
  if (toastTimer != null) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2600)
}

function handleOrderCreated(message: string): void {
  createModalOpen.value = false
  showToast(message)
  void load()
}

function handleOrderUpdated(message: string): void {
  detailModalOpen.value = false
  showToast(message)
  void load()
}

function handleBlockSaved(message: string): void {
  blockModalOpen.value = false
  showToast(message)
  void load()
}

onMounted(() => {
  void loadCatalogs()
  void load()
})

onBeforeUnmount(() => {
  if (toastTimer != null) window.clearTimeout(toastTimer)
})
</script>

<template>
  <section class="operation-page dashboard-page">
    <!-- 顶部日期与按月统计切换栏 -->
    <section class="dashboard-control-card" aria-label="经营看板日期和统计方式">
      <div class="dashboard-control-copy">
        <div class="dashboard-section-title">
          <span class="dashboard-title-mark"></span>
          <h1>经营实时看板</h1>
          <span v-if="mode === 'day' && isToday" class="status-pill-tag is-today">今日</span>
          <button
            v-else-if="mode === 'day'"
            type="button"
            class="status-pill-btn"
            :disabled="loading"
            @click="setDate(today)"
          >
            返回今日
          </button>
          <span v-else class="status-pill-tag is-month">按月统计视图</span>
        </div>
        <p>支持左右滑动切换日期查看每日数据，或切换至按月统计查看整月经营汇总</p>
      </div>

      <div class="dashboard-control-actions">
        <div v-if="mode === 'day'" class="dashboard-date-picker">
          <button type="button" aria-label="前一天" title="前一天" :disabled="loading" @click="shiftDate(-1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="15 18 9 12 15 6" /></svg>
          </button>
          <input :value="date" type="date" aria-label="选择房态日期" @change="onDateChange" />
          <button type="button" aria-label="后一天" title="后一天" :disabled="loading" @click="shiftDate(1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="9 18 15 12 9 6" /></svg>
          </button>
        </div>

        <div v-else class="dashboard-date-picker">
          <button type="button" aria-label="上一月" title="上一月" :disabled="loading" @click="shiftMonth(-1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="15 18 9 12 15 6" /></svg>
          </button>
          <input :value="month" type="month" aria-label="选择统计月份" @change="onMonthChange" />
          <button type="button" aria-label="下一月" title="下一月" :disabled="loading" @click="shiftMonth(1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="9 18 15 12 9 6" /></svg>
          </button>
        </div>

        <div class="dashboard-mode-switch" role="group" aria-label="统计方式">
          <button
            type="button"
            :class="{ 'is-active-day': mode === 'day' }"
            @click="setMode('day')"
          >
            按日查看
          </button>
          <button
            type="button"
            :class="{ 'is-active-month': mode === 'month' }"
            @click="setMode('month')"
          >
            按月统计
          </button>
        </div>
      </div>
    </section>

    <p v-if="error" class="notice notice-error" role="alert">{{ error }}</p>
    <div v-else-if="loading && !dashboard" class="operation-card operation-empty">正在加载经营总览…</div>

    <template v-else-if="dashboard">
      <p v-if="dailyFinanceError && mode === 'day'" class="notice notice-error" role="alert">{{ dailyFinanceError }}</p>

      <!-- 核心数据指标看板（6 项） -->
      <section class="dashboard-kpis" aria-label="经营核心指标">
        <!-- 1. 房间统计 -->
        <article class="dashboard-kpi room-kpi">
          <div class="dashboard-kpi-heading">
            <span>房间统计</span>
            <span class="kpi-svg is-teal">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M2 20v-8a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v8" />
                <path d="M4 10V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v4" />
                <path d="M12 4v6" />
                <path d="M2 18h20" />
              </svg>
            </span>
          </div>

          <div class="dashboard-kpi-body">
            <div v-if="mode === 'day'" class="room-count-line">
              <strong>占用 <b class="is-teal">{{ occupiedRoomsCount }}</b> 间</strong>
              <span class="room-count-sep">|</span>
              <strong>空房 <b class="is-celadon">{{ availableRoomsCount }}</b> 间</strong>
            </div>
            <div v-else class="room-count-line is-month-nights">
              <strong>已占 <b class="is-teal">{{ monthNightStats.nightCount }}</b> 晚</strong>
              <span class="room-count-sep">|</span>
              <strong>空闲 <b class="is-celadon">{{ monthNightStats.freeNights }}</b> 晚</strong>
            </div>
            <div class="room-rate-line">
              <span>入住率</span>
              <strong>{{ occupancyRateDisplay }}</strong>
            </div>
          </div>
        </article>

        <!-- 2 ~ 6. 营业额、手续费、双提成、商家到手金额 -->
        <article
          v-for="card in summaryCards"
          :key="card.key"
          class="dashboard-kpi"
          :class="{ 'is-highlight': card.highlight }"
        >
          <div class="dashboard-kpi-heading">
            <span>{{ card.label }}</span>
            <span class="kpi-svg" :class="`is-${card.valueTone}`">
              <svg v-if="card.icon === 'coins'" viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="8" cy="8" r="6" />
                <path d="M18.09 10.37A6 6 0 1 1 10.34 18" />
                <path d="M7 6h1v4" />
                <path d="m16.71 13.88.7.71-2.82 2.82" />
              </svg>
              <svg v-else-if="card.icon === 'percent'" viewBox="0 0 24 24" aria-hidden="true">
                <line x1="19" y1="5" x2="5" y2="19" />
                <circle cx="6.5" cy="6.5" r="2.5" />
                <circle cx="17.5" cy="17.5" r="2.5" />
              </svg>
              <svg v-else-if="card.icon === 'user-check'" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <polyline points="16 11 18 13 22 9" />
              </svg>
              <svg v-else-if="card.icon === 'award'" viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="12" cy="8" r="7" />
                <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88" />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M21 12V7H5a2 2 0 0 1 0-4h14v4" />
                <path d="M3 5v14a2 2 0 0 0 2 2h16v-5" />
                <path d="M18 12a2 2 0 0 0 0 4h4v-4Z" />
              </svg>
            </span>
          </div>

          <div class="dashboard-kpi-body">
            <strong class="dashboard-kpi-value" :class="`is-${card.valueTone}`">
              {{ card.value === '—' ? '—' : `¥${card.value}` }}
            </strong>
            <p class="dashboard-kpi-note">{{ card.note }}</p>
          </div>
        </article>
      </section>

      <!-- 全店 6 间客房实时状态矩阵 -->
      <section class="dashboard-rooms-card">
        <div class="dashboard-room-heading">
          <div class="dashboard-section-title">
            <span class="dashboard-title-mark is-sm"></span>
            <h2>客房实时状态</h2>
          </div>
          <button type="button" class="dashboard-calendar-link" @click="goToCalendar">
            <span>进入房态日历</span>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </button>
        </div>

        <div v-if="roomViews.length" class="dashboard-room-grid">
          <button
            v-for="view in roomViews"
            :key="view.room.room_id"
            class="dashboard-room"
            :class="view.cardTone"
            type="button"
            @click="openRoomCard(view)"
          >
            <span class="dashboard-room-header">
              <span class="dashboard-room-identity">
                <img
                  v-if="view.imageUrl"
                  :src="view.imageUrl"
                  :alt="`${view.room.room_number} ${view.room.room_name}`"
                  class="dashboard-room-image"
                  @error="markImageFailed(view.room.room_id)"
                />
                <span v-else class="dashboard-room-image-placeholder">{{ view.room.room_number }}</span>
                <span class="dashboard-room-name-wrap">
                  <span class="dashboard-room-number">{{ view.room.room_number }}</span>
                  <strong class="dashboard-room-name">{{ view.room.room_name }}</strong>
                </span>
              </span>
              <span class="proto-status-badge" :class="view.badgeTone">{{ view.statusLabel }}</span>
            </span>

            <!-- 订单占房 -->
            <span v-if="view.stateType === 'ORDER'" class="proto-room-inner is-order">
              <span class="proto-order-top">
                <span class="proto-guest-wrap">
                  <strong class="proto-guest-name">{{ view.room.guest_name || view.order?.guest_name || '未填姓名' }}</strong>
                  <span class="proto-guest-count">· {{ view.room.guest_count ?? view.order?.guest_count ?? 1 }}人</span>
                </span>
                <span class="proto-channel-tag">{{ view.room.channel_name || view.order?.channel_name || '未知来源' }}</span>
              </span>

              <span class="proto-order-dates">
                <span class="is-mono">
                  {{ view.room.check_in || view.order?.check_in || '—' }} 至 {{ view.room.check_out || view.order?.check_out || '—' }}
                </span>
                <span class="proto-nights">共 {{ view.nightsCount || '—' }} 晚</span>
              </span>

              <span class="proto-order-footer">
                <span class="proto-order-footer-label">实付金额</span>
                <strong class="proto-order-price is-mono">
                  {{ view.order ? `¥${view.order.paid_amount}` : '—' }}
                </strong>
              </span>
            </span>

            <!-- 管理员封房 -->
            <span v-else-if="view.stateType === 'BLOCKED'" class="proto-room-inner is-block">
              <span class="is-mono proto-block-range">
                封房区间：{{ view.block ? `${view.block.start_date} ~ ${view.block.end_date}` : `${date} 起` }}
              </span>
              <span class="proto-block-reason">
                原因：{{ view.block?.reason || '管理员主动封房' }}
              </span>
            </span>

            <!-- 空房引导 -->
            <span v-else class="proto-room-inner is-available">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="16" />
                <line x1="8" y1="12" x2="16" y2="12" />
              </svg>
              <span>点击创建第三方平台订单</span>
            </span>
          </button>
        </div>

        <div v-else class="operation-empty">当前没有客房数据。</div>
      </section>
    </template>

    <!-- 页内弹窗 -->
    <ExternalOrderCreateModal
      :open="createModalOpen"
      :channels="channels"
      :creators="creators"
      :rooms="rooms"
      :preset-room-id="createPresetRoomId"
      :preset-check-in="createPresetCheckIn"
      @close="createModalOpen = false"
      @created="handleOrderCreated"
    />

    <OrderDetailModal
      :open="detailModalOpen"
      :order-id="activeOrderId"
      :rooms="rooms"
      :creators="creators"
      @close="detailModalOpen = false"
      @updated="handleOrderUpdated"
    />

    <RoomBlockModal
      :open="blockModalOpen"
      :rooms="rooms"
      :block="editingBlock"
      :preset-room-id="blockPresetRoomId"
      :preset-start-date="date"
      @close="blockModalOpen = false"
      @saved="handleBlockSaved"
    />

    <!-- 全局轻量 Toast -->
    <div class="qx-toast-wrap" aria-live="polite">
      <div class="qx-toast-box" :class="{ 'is-visible': Boolean(toastMessage) }">
        <span class="qx-toast-dot"></span>
        <span>{{ toastMessage }}</span>
      </div>
    </div>
  </section>
</template>
