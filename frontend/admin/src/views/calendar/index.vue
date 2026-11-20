<script setup lang="ts">
/** K01 后台房态日历：支持按日 / 按周 / 按月三维度筛选、订单详情/入离与封房页内操作。 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { calendarApi } from '../../api/calendar'
import type { CalendarDay } from '../../api/calendar'
import { channelApi } from '../../api/channels'
import { creatorApi } from '../../api/creators'
import { orderApi } from '../../api/orders'
import { roomBlockApi } from '../../api/room-blocks'
import type { RoomBlock } from '../../api/room-blocks'
import { roomApi } from '../../api/rooms'
import ExternalOrderCreateModal from '../../components/operations/ExternalOrderCreateModal.vue'
import OrderDetailModal from '../../components/operations/OrderDetailModal.vue'
import RoomBlockModal from '../../components/operations/RoomBlockModal.vue'
import type { Channel, Creator, Room } from '../../types/catalog'
import type { OrderDetail } from '../../types/orders'
import {
  addDays,
  dayDifference,
  isWeekendDate,
  monthDateList,
  roomStatusLabel,
  shanghaiDate,
  shiftMonthValue,
  weekdayShort,
} from '../../utils/operations'
import '../../styles/operations.css'

type CalendarMode = 'day' | 'week' | 'month'

interface CalendarRowRoom {
  id: number
  number: string
  name: string
}

interface CellRenderInfo {
  type: 'AVAILABLE' | 'BLOCKED' | 'ORDER'
  statusCode: string
  label: string
  isFirstNight: boolean
  guestName: string
  channelName: string
  order: OrderDetail | null
  orderId: number | null
  block: RoomBlock | null
  blockReason: string
  nights: number
  guestCount: number
  paidAmount: string
}

const today = shanghaiDate()
const mode = ref<CalendarMode>('week')
const anchorDate = ref(today)
const month = ref(today.slice(0, 7))

const loading = ref(false)
const errorMessage = ref('')
let loadSequence = 0

const rooms = ref<Room[]>([])
const channels = ref<Channel[]>([])
const creators = ref<Creator[]>([])
const calendarRoomList = ref<CalendarRowRoom[]>([])
const dayMapByRoom = ref<Record<number, Record<string, CalendarDay>>>({})
const blockById = ref<Record<number, RoomBlock>>({})
const orderDetails = ref<Record<number, OrderDetail>>({})

// 页内弹窗状态
const createModalOpen = ref(false)
const createPresetRoomId = ref<number | null>(null)
const createPresetCheckIn = ref(today)

const detailModalOpen = ref(false)
const activeOrderId = ref<number | null>(null)

const blockModalOpen = ref(false)
const editingBlock = ref<RoomBlock | null>(null)
const blockPresetRoomId = ref<number | null>(null)
const blockPresetStartDate = ref(today)

// Toast
const toastMessage = ref('')
let toastTimer: number | null = null

const dateCols = computed((): string[] => {
  if (mode.value === 'day') return [anchorDate.value]
  if (mode.value === 'week') return Array.from({ length: 7 }, (_, i) => addDays(anchorDate.value, i))
  return monthDateList(month.value)
})

const weekEndShort = computed(() => {
  const cols = dateCols.value
  return cols.length > 0 ? cols[cols.length - 1].slice(5) : ''
})

const isCurrentPeriod = computed(() => {
  if (mode.value === 'day') return anchorDate.value === today
  if (mode.value === 'week') return dateCols.value.includes(today)
  return month.value === today.slice(0, 7)
})

const currentPeriodBadge = computed(() => {
  if (mode.value === 'day') return '今日'
  if (mode.value === 'week') return '本周'
  return '本月'
})

const resetBtnLabel = computed(() => {
  if (mode.value === 'day') return '回到今日'
  if (mode.value === 'week') return '回到本周'
  return '回到本月'
})

async function loadCatalogs(): Promise<void> {
  const [channelList, creatorList] = await Promise.all([
    channelApi.list().catch(() => []),
    creatorApi.list({}).catch(() => []),
  ])
  channels.value = channelList
  creators.value = creatorList
}

async function loadCalendar(): Promise<void> {
  const cols = dateCols.value
  if (cols.length === 0) return

  const sequence = ++loadSequence
  const involvedMonths = [...new Set(cols.map((d) => d.slice(0, 7)))]
  loading.value = true
  errorMessage.value = ''

  try {
    const [monthResults, blockLists, roomList] = await Promise.all([
      Promise.all(involvedMonths.map((m) => calendarApi.month(m))),
      Promise.all(involvedMonths.map((m) => roomBlockApi.list(undefined, m).catch(() => []))),
      roomApi.list().catch(() => rooms.value),
    ])
    if (sequence !== loadSequence) return

    rooms.value = roomList
    const roomNameById = new Map(roomList.map((r) => [r.id, r.name]))

    const mergedRoomOrder: CalendarRowRoom[] = []
    const seenRoomIds = new Set<number>()
    const mergedDays: Record<number, Record<string, CalendarDay>> = {}

    for (const cal of monthResults) {
      for (const rm of cal.rooms) {
        if (!seenRoomIds.has(rm.room_id)) {
          seenRoomIds.add(rm.room_id)
          mergedRoomOrder.push({
            id: rm.room_id,
            number: rm.room_number,
            name: roomNameById.get(rm.room_id) || '客房',
          })
        }
        if (!mergedDays[rm.room_id]) mergedDays[rm.room_id] = {}
        for (const day of rm.days) {
          mergedDays[rm.room_id][day.date] = day
        }
      }
    }

    const mergedBlocks: Record<number, RoomBlock> = {}
    for (const list of blockLists) {
      for (const blk of list) {
        mergedBlocks[blk.id] = blk
      }
    }

    calendarRoomList.value = mergedRoomOrder
    dayMapByRoom.value = mergedDays
    blockById.value = mergedBlocks

    // 仅拉取当前可见日期列中出现的订单详情（去重并行）
    const orderIdSet = new Set<number>()
    for (const rm of mergedRoomOrder) {
      const days = mergedDays[rm.id]
      if (!days) continue
      for (const d of cols) {
        const cell = days[d]
        if (cell?.order_id != null) orderIdSet.add(cell.order_id)
      }
    }

    const orderIds = [...orderIdSet]
    const detailResults = await Promise.allSettled(orderIds.map((id) => orderApi.detail(id)))
    if (sequence !== loadSequence) return

    const details: Record<number, OrderDetail> = {}
    detailResults.forEach((entry, index) => {
      if (entry.status === 'fulfilled') details[orderIds[index]] = entry.value
    })
    orderDetails.value = details
  } catch (cause) {
    if (sequence !== loadSequence) return
    errorMessage.value = cause instanceof Error ? cause.message : '房态日历读取失败'
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function getCellInfo(roomId: number, dateStr: string): CellRenderInfo {
  const day = dayMapByRoom.value[roomId]?.[dateStr]
  if (!day || day.status === 'AVAILABLE') {
    return {
      type: 'AVAILABLE',
      statusCode: 'AVAILABLE',
      label: '空房',
      isFirstNight: true,
      guestName: '',
      channelName: '',
      order: null,
      orderId: null,
      block: null,
      blockReason: '',
      nights: 0,
      guestCount: 0,
      paidAmount: '0.00',
    }
  }

  if (day.status === 'BLOCKED') {
    const block = day.block_id != null ? blockById.value[day.block_id] ?? null : null
    return {
      type: 'BLOCKED',
      statusCode: 'BLOCKED',
      label: '不可预订',
      isFirstNight: true,
      guestName: '',
      channelName: '',
      order: null,
      orderId: null,
      block,
      blockReason: block?.reason || '管理员封房',
      nights: 0,
      guestCount: 0,
      paidAmount: '0.00',
    }
  }

  const order = day.order_id != null ? orderDetails.value[day.order_id] ?? null : null
  const statusCode = order?.status || day.status
  const isFirstNight = order ? order.check_in === dateStr : day.status !== 'OCCUPIED'
  const nights = order ? order.nights || Math.max(1, dayDifference(order.check_in, order.check_out)) : 1

  return {
    type: 'ORDER',
    statusCode,
    label: roomStatusLabel(statusCode),
    isFirstNight,
    guestName: order?.guest_name || '散客',
    channelName: order?.channel_name || '外部',
    order,
    orderId: day.order_id,
    block: null,
    blockReason: '',
    nights,
    guestCount: order?.guest_count ?? 1,
    paidAmount: order?.paid_amount ?? '0.00',
  }
}

function orderToneClass(statusCode: string): string {
  const map: Record<string, string> = {
    PENDING_CONFIRM: 'is-reserved',
    PENDING_CHECKIN: 'is-pending',
    CHECKED_IN: 'is-checked-in',
    OCCUPIED: 'is-checked-in',
    COMPLETED: 'is-completed',
  }
  return map[statusCode] || 'is-checked-in'
}

function formatHeaderDate(dateStr: string): string {
  return mode.value === 'month' ? `${dateStr.slice(8)}日` : dateStr.slice(5).replace('-', '/')
}

function setMode(nextMode: CalendarMode): void {
  mode.value = nextMode
  if (nextMode === 'month') {
    month.value = anchorDate.value.slice(0, 7)
  }
  void loadCalendar()
}

function setAnchorDate(nextDate: string): void {
  if (!nextDate) return
  anchorDate.value = nextDate
  month.value = nextDate.slice(0, 7)
  void loadCalendar()
}

function onAnchorDateInput(event: Event): void {
  const target = event.target
  if (target instanceof HTMLInputElement && target.value) setAnchorDate(target.value)
}

function onMonthInput(event: Event): void {
  const target = event.target
  if (!(target instanceof HTMLInputElement) || !target.value) return
  month.value = target.value
  anchorDate.value = `${target.value}-01`
  void loadCalendar()
}

function shiftPeriod(delta: number): void {
  if (mode.value === 'day') {
    anchorDate.value = addDays(anchorDate.value, delta)
    month.value = anchorDate.value.slice(0, 7)
  } else if (mode.value === 'week') {
    anchorDate.value = addDays(anchorDate.value, delta * 7)
    month.value = anchorDate.value.slice(0, 7)
  } else {
    month.value = shiftMonthValue(month.value, delta)
    anchorDate.value = `${month.value}-01`
  }
  void loadCalendar()
}

function resetToToday(): void {
  anchorDate.value = today
  month.value = today.slice(0, 7)
  void loadCalendar()
}

function handleCellClick(roomId: number, dateStr: string, info: CellRenderInfo): void {
  if (info.type === 'ORDER' && info.orderId != null) {
    activeOrderId.value = info.orderId
    detailModalOpen.value = true
    return
  }
  if (info.type === 'BLOCKED') {
    editingBlock.value = info.block
    blockPresetRoomId.value = roomId
    blockPresetStartDate.value = dateStr
    blockModalOpen.value = true
    return
  }
  createPresetRoomId.value = roomId
  createPresetCheckIn.value = dateStr
  createModalOpen.value = true
}

function openNewBlockModal(): void {
  editingBlock.value = null
  blockPresetRoomId.value = rooms.value[0]?.id ?? calendarRoomList.value[0]?.id ?? null
  blockPresetStartDate.value = anchorDate.value
  blockModalOpen.value = true
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
  void loadCalendar()
}

function handleOrderUpdated(message: string): void {
  detailModalOpen.value = false
  showToast(message)
  void loadCalendar()
}

function handleBlockSaved(message: string): void {
  blockModalOpen.value = false
  showToast(message)
  void loadCalendar()
}

onMounted(() => {
  void loadCatalogs()
  void loadCalendar()
})

onBeforeUnmount(() => {
  if (toastTimer != null) window.clearTimeout(toastTimer)
})
</script>

<template>
  <section class="cal-proto-page">
    <!-- 顶部日/周/月三维度筛选 + 时间步进导航 + 新增封房 -->
    <section class="cal-toolbar-card" aria-label="房态日历筛选与导航">
      <div class="cal-toolbar-left">
        <div class="cal-mode-switch" role="group" aria-label="日历时间维度">
          <button type="button" :class="{ 'is-active': mode === 'day' }" @click="setMode('day')">按日</button>
          <button type="button" :class="{ 'is-active': mode === 'week' }" @click="setMode('week')">按周</button>
          <button type="button" :class="{ 'is-active': mode === 'month' }" @click="setMode('month')">按月</button>
        </div>

        <!-- 按日时间导航 -->
        <div v-if="mode === 'day'" class="dashboard-date-picker">
          <button type="button" aria-label="前一天" title="前一天" :disabled="loading" @click="shiftPeriod(-1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="15 18 9 12 15 6" /></svg>
          </button>
          <input :value="anchorDate" type="date" aria-label="选择房态日期" @change="onAnchorDateInput" />
          <button type="button" aria-label="后一天" title="后一天" :disabled="loading" @click="shiftPeriod(1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="9 18 15 12 9 6" /></svg>
          </button>
        </div>

        <!-- 按周时间导航 -->
        <div v-else-if="mode === 'week'" class="dashboard-date-picker cal-week-picker">
          <button type="button" aria-label="上一周" title="上一周" :disabled="loading" @click="shiftPeriod(-1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="15 18 9 12 15 6" /></svg>
          </button>
          <div class="cal-week-range">
            <input :value="anchorDate" type="date" aria-label="选择周起始日期" @change="onAnchorDateInput" />
            <span class="is-mono">至 {{ weekEndShort }}</span>
          </div>
          <button type="button" aria-label="下一周" title="下一周" :disabled="loading" @click="shiftPeriod(1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="9 18 15 12 9 6" /></svg>
          </button>
        </div>

        <!-- 按月时间导航 -->
        <div v-else class="dashboard-date-picker">
          <button type="button" aria-label="上一月" title="上一月" :disabled="loading" @click="shiftPeriod(-1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="15 18 9 12 15 6" /></svg>
          </button>
          <input :value="month" type="month" aria-label="选择月份" @change="onMonthInput" />
          <button type="button" aria-label="下一月" title="下一月" :disabled="loading" @click="shiftPeriod(1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="9 18 15 12 9 6" /></svg>
          </button>
        </div>

        <span v-if="isCurrentPeriod" class="status-pill-tag is-today">{{ currentPeriodBadge }}</span>
        <button v-else type="button" class="status-pill-btn is-rect" :disabled="loading" @click="resetToToday">
          {{ resetBtnLabel }}
        </button>
      </div>

      <div class="cal-toolbar-right">
        <button type="button" class="cal-add-block-btn" @click="openNewBlockModal">+ 新增封房</button>
      </div>
    </section>

    <!-- 房态颜色图例条 -->
    <section class="cal-legend-bar" aria-label="房态图例">
      <div class="cal-legend-items">
        <span class="cal-legend-title">房态图例：</span>
        <span class="cal-legend-pill is-available">空房</span>
        <span class="cal-legend-pill is-reserved">已预订(待确认)</span>
        <span class="cal-legend-pill is-pending">待入住</span>
        <span class="cal-legend-pill is-checked-in">已入住</span>
        <span class="cal-legend-pill is-completed">已退房</span>
        <span class="cal-legend-pill is-blocked">不可预订(封房)</span>
      </div>
      <span class="cal-legend-hint">点击「空房」直接建单，点击已占日期查看详情或办理入离</span>
    </section>

    <p v-if="errorMessage" class="notice notice-error" role="alert">{{ errorMessage }}</p>

    <!-- 房态矩阵表格 -->
    <section class="cal-matrix-card">
      <div v-if="loading && calendarRoomList.length === 0" class="operation-empty">正在读取房态日历…</div>
      <div v-else-if="calendarRoomList.length === 0" class="operation-empty">
        <div>
          <strong>没有客房日历数据</strong>
          <p class="cal-empty-sub">请检查客房资料或稍后重试。</p>
          <button type="button" class="qx-btn-ghost" :disabled="loading" @click="loadCalendar">重新加载</button>
        </div>
      </div>

      <div v-else class="cal-table-scroll" :class="{ 'is-loading': loading }" :aria-busy="loading">
        <table class="cal-matrix-table">
          <thead>
            <tr>
              <th class="cal-room-th">房间号 / 名称</th>
              <th
                v-for="d in dateCols"
                :key="d"
                class="cal-date-th"
                :class="[`is-mode-${mode}`, { 'is-today': d === today }]"
              >
                <div class="cal-date-th-inner">
                  <span class="cal-date-text is-mono">{{ formatHeaderDate(d) }}</span>
                  <span
                    class="cal-week-text"
                    :class="{ 'is-today': d === today, 'is-weekend': d !== today && isWeekendDate(d) }"
                  >
                    周{{ weekdayShort(d) }}
                  </span>
                  <span v-if="d === today" class="cal-today-chip">今日</span>
                </div>
              </th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="room in calendarRoomList" :key="room.id">
              <!-- 左侧精简客房信息：仅房间号 + 房间名称 -->
              <td class="cal-room-td">
                <div class="cal-room-identity">
                  <span class="dashboard-room-number">{{ room.number }}</span>
                  <strong class="cal-room-name" :title="room.name">{{ room.name }}</strong>
                </div>
              </td>

              <td v-for="d in dateCols" :key="d" class="cal-cell-td">
                <template v-for="info in [getCellInfo(room.id, d)]" :key="`${room.id}-${d}`">
                  <!-- 空房单元格 -->
                  <button
                    v-if="info.type === 'AVAILABLE'"
                    type="button"
                    class="cal-cell-card is-available"
                    :class="`is-mode-${mode}`"
                    :aria-label="`${room.number} ${room.name}，${d}，空房`"
                    @click="handleCellClick(room.id, d, info)"
                  >
                    <span class="cal-avail-label">空房</span>
                    <span v-if="mode === 'day'" class="cal-avail-sub">+ 点击创建第三方订单</span>
                  </button>

                  <!-- 封房单元格 -->
                  <button
                    v-else-if="info.type === 'BLOCKED'"
                    type="button"
                    class="cal-cell-card is-blocked"
                    :class="`is-mode-${mode}`"
                    :aria-label="`${room.number} ${room.name}，${d}，不可预订`"
                    @click="handleCellClick(room.id, d, info)"
                  >
                    <span class="cal-card-top">
                      <span class="cal-mini-badge is-blocked">{{ info.label }}</span>
                      <span v-if="mode === 'day' && info.block" class="cal-block-dates is-mono">
                        {{ info.block.start_date }} ~ {{ info.block.end_date }}
                      </span>
                    </span>
                    <span class="cal-block-reason-line" :title="info.blockReason">{{ info.blockReason }}</span>
                  </button>

                  <!-- 订单占房：按日宽幅横向卡片 -->
                  <button
                    v-else-if="mode === 'day'"
                    type="button"
                    class="cal-cell-card is-order-day"
                    :class="orderToneClass(info.statusCode)"
                    :aria-label="`${room.number} ${room.name}，${d}，${info.label}，${info.guestName}`"
                    @click="handleCellClick(room.id, d, info)"
                  >
                    <span class="cal-day-left">
                      <span class="cal-day-row-1">
                        <span class="cal-mini-badge" :class="orderToneClass(info.statusCode)">{{ info.label }}</span>
                        <strong class="cal-day-guest">{{ info.guestName }}</strong>
                        <span class="cal-source-chip" :class="orderToneClass(info.statusCode)">{{ info.channelName }}</span>
                        <span v-if="!info.isFirstNight" class="cal-continue-inline">· 续住</span>
                      </span>
                      <span v-if="info.order" class="cal-day-row-2 is-mono">
                        {{ info.order.check_in }} 至 {{ info.order.check_out }}（共 {{ info.nights }} 晚 · {{ info.guestCount }}人）
                      </span>
                    </span>
                    <span class="cal-day-right">
                      <small>实付金额</small>
                      <strong class="is-mono">¥{{ info.paidAmount }}</strong>
                    </span>
                  </button>

                  <!-- 订单占房：按周 / 按月垂直三要素卡片 -->
                  <button
                    v-else
                    type="button"
                    class="cal-cell-card is-order-grid"
                    :class="orderToneClass(info.statusCode)"
                    :aria-label="`${room.number} ${room.name}，${d}，${info.label}，${info.guestName}`"
                    @click="handleCellClick(room.id, d, info)"
                  >
                    <span class="cal-card-top">
                      <span class="cal-mini-badge" :class="orderToneClass(info.statusCode)">{{ info.label }}</span>
                      <span v-if="!info.isFirstNight" class="cal-continue-tag">续住</span>
                    </span>
                    <strong class="cal-grid-guest" :title="info.guestName">{{ info.guestName }}</strong>
                    <span class="cal-grid-source-row">
                      <span class="cal-source-chip" :class="orderToneClass(info.statusCode)">{{ info.channelName }}</span>
                    </span>
                  </button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

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
      :preset-start-date="blockPresetStartDate"
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
