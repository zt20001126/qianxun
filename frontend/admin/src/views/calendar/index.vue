<script setup lang="ts">
/** K01 后台月历：按房间和住宿日展示订单、封房及空房状态。 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { calendarApi } from '../../api/calendar'
import type { CalendarDay, CalendarDayStatus, RoomCalendar } from '../../api/calendar'

const router = useRouter()
const month = ref(currentMonth())
const calendar = ref<RoomCalendar | null>(null)
const loading = ref(false)
const errorMessage = ref('')
let latestRequest = 0

const dates = computed(() => {
  const [year, monthNumber] = month.value.split('-').map(Number)
  if (!year || !monthNumber) return []
  const dayCount = new Date(year, monthNumber, 0).getDate()
  return Array.from({ length: dayCount }, (_, index) =>
    `${month.value}-${String(index + 1).padStart(2, '0')}`,
  )
})

const monthLabel = computed(() => {
  const [year, monthNumber] = month.value.split('-')
  return `${year} 年 ${Number(monthNumber)} 月`
})

const statusLabels: Record<CalendarDayStatus, string> = {
  AVAILABLE: '空房',
  BLOCKED: '不可预订',
  PENDING_CONFIRM: '已预订',
  PENDING_CHECKIN: '待入住',
  CHECKED_IN: '已入住',
  COMPLETED: '已退房',
  OCCUPIED: '占用',
}

onMounted(() => loadCalendar())

function currentMonth(): string {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`
}

function changeMonth(offset: number): void {
  const [year, monthNumber] = month.value.split('-').map(Number)
  if (!year || !monthNumber) return
  const nextMonth = new Date(year, monthNumber - 1 + offset, 1)
  month.value = `${nextMonth.getFullYear()}-${String(nextMonth.getMonth() + 1).padStart(2, '0')}`
  void loadCalendar()
}

function goToCurrentMonth(): void {
  month.value = currentMonth()
  void loadCalendar()
}

async function loadCalendar(): Promise<void> {
  const requestNumber = ++latestRequest
  loading.value = true
  errorMessage.value = ''
  calendar.value = null
  try {
    const result = await calendarApi.month(month.value)
    if (requestNumber === latestRequest) calendar.value = result
  } catch (error) {
    if (requestNumber === latestRequest) {
      errorMessage.value = error instanceof Error ? error.message : '房态日历读取失败'
      calendar.value = null
    }
  } finally {
    if (requestNumber === latestRequest) loading.value = false
  }
}

function weekday(date: string): string {
  const [year, monthNumber, day] = date.split('-').map(Number)
  return new Date(year, monthNumber - 1, day).toLocaleDateString('zh-CN', { weekday: 'short' })
}

function statusLabel(status: CalendarDayStatus): string {
  return statusLabels[status]
}

function openCell(roomId: number, day: CalendarDay): void {
  if (day.status === 'AVAILABLE') {
    void router.push({
      path: '/orders',
      query: {
        calendar_action: 'create',
        room_id: String(roomId),
        check_in: day.date,
        check_out: nextDate(day.date),
      },
    })
    return
  }
  if (day.status === 'BLOCKED') {
    void router.push({
      path: '/room-blocks',
      query: { calendar_action: 'block', room_id: String(roomId), month: month.value },
    })
    return
  }
  if (day.order_id != null) {
    void router.push({
      path: '/orders',
      query: { calendar_action: 'detail', order_id: String(day.order_id) },
    })
  }
}

function nextDate(date: string): string {
  const [year, monthNumber, day] = date.split('-').map(Number)
  const next = new Date(year, monthNumber - 1, day + 1)
  return `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}-${String(next.getDate()).padStart(2, '0')}`
}

function cellClass(status: CalendarDayStatus): string {
  return `calendar-cell-${status.toLowerCase()}`
}
</script>

<template>
  <section class="calendar-page">
    <div class="page-heading-row calendar-heading">
      <div>
        <p class="page-eyebrow">ROOM CALENDAR</p>
        <h1>房态日历</h1>
        <p class="page-description">查看客房每日订单和封房情况；待支付临时锁房不在此日历显示。</p>
      </div>
      <div class="calendar-month-controls">
        <button class="button button-outline" type="button" :disabled="loading" aria-label="上一个月" @click="changeMonth(-1)">‹</button>
        <input v-model="month" type="month" aria-label="选择月份" @change="loadCalendar">
        <button class="button button-outline" type="button" :disabled="loading" aria-label="下一个月" @click="changeMonth(1)">›</button>
        <button class="button button-quiet" type="button" :disabled="loading" @click="goToCurrentMonth">本月</button>
      </div>
    </div>

    <div class="calendar-summary-row">
      <strong>{{ monthLabel }}</strong>
      <div class="calendar-legend" aria-label="房态说明">
        <span><i class="legend-dot legend-available" />空房</span>
        <span><i class="legend-dot legend-booked" />已预订 / 待入住</span>
        <span><i class="legend-dot legend-checked-in" />已入住 / 占用</span>
        <span><i class="legend-dot legend-completed" />已退房</span>
        <span><i class="legend-dot legend-blocked" />不可预订</span>
      </div>
    </div>

    <p v-if="errorMessage" class="notice notice-error" role="alert">{{ errorMessage }}</p>

    <div class="calendar-card">
      <div v-if="loading && !calendar" class="calendar-state">正在读取房态日历…</div>
      <div v-else-if="!calendar || calendar.rooms.length === 0" class="calendar-state">
        <strong>没有客房日历数据</strong>
        <span>请检查客房资料或重试。</span>
        <button class="button button-outline" type="button" :disabled="loading" @click="loadCalendar">重新加载</button>
      </div>
      <div v-else class="calendar-scroll" :aria-busy="loading">
        <table class="room-calendar-table">
          <thead>
            <tr>
              <th class="calendar-room-heading">客房</th>
              <th v-for="date in dates" :key="date" class="calendar-date-heading">
                <span>{{ Number(date.slice(-2)) }} 日</span>
                <small>{{ weekday(date) }}</small>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="room in calendar.rooms" :key="room.room_id">
              <th class="calendar-room-cell" scope="row">
                <strong>{{ room.room_number }}</strong>
                <small>客房</small>
              </th>
              <td v-for="day in room.days" :key="day.date" class="calendar-day-cell">
                <button
                  type="button"
                  class="calendar-day-button"
                  :class="cellClass(day.status)"
                  :aria-label="`${room.room_number}，${day.date}，${statusLabel(day.status)}`"
                  :title="`${day.date} · ${statusLabel(day.status)}`"
                  @click="openCell(room.room_id, day)"
                >
                  <span>{{ statusLabel(day.status) }}</span>
                  <small v-if="day.order_id != null">订单详情</small>
                  <small v-else-if="day.block_id != null">封房管理</small>
                  <small v-else-if="day.status === 'AVAILABLE'">新增订单</small>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.calendar-page { display: grid; gap: 18px; min-width: 0; }
.calendar-heading { align-items: flex-end; }
.calendar-month-controls { display: flex; align-items: center; gap: 8px; }
.calendar-month-controls input { min-height: 40px; padding: 0 10px; border: 1px solid #ded5cc; border-radius: 9px; background: #fffdfb; color: #292522; font: inherit; }
.calendar-summary-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.calendar-summary-row > strong { color: #512a18; font-size: 18px; }
.calendar-legend { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; color: #665b52; font-size: 12px; }
.calendar-legend span { display: inline-flex; align-items: center; gap: 6px; }
.legend-dot { width: 9px; height: 9px; border-radius: 50%; }
.legend-available { background: #63856b; }
.legend-booked { background: #a56a2a; }
.legend-checked-in { background: #98501f; }
.legend-completed { background: #718395; }
.legend-blocked { background: #a34c42; }
.calendar-card { overflow: hidden; border: 1px solid #e8dfd7; border-radius: 14px; background: #fffcf9; box-shadow: 0 8px 24px rgb(61 42 27 / 5%); }
.calendar-scroll { max-width: 100%; overflow: auto; }
.room-calendar-table { width: max-content; min-width: 100%; border-collapse: separate; border-spacing: 0; table-layout: fixed; }
.room-calendar-table th, .room-calendar-table td { border-right: 1px solid #eee7e0; border-bottom: 1px solid #eee7e0; }
.room-calendar-table thead th { position: sticky; top: 0; z-index: 2; height: 58px; background: #f8f1ea; color: #65584e; }
.calendar-room-heading, .calendar-room-cell { position: sticky; left: 0; z-index: 3; width: 118px; min-width: 118px; text-align: left; }
.calendar-room-heading { padding: 0 14px; }
.calendar-date-heading { width: 82px; min-width: 82px; text-align: center; font-size: 12px; }
.calendar-date-heading span, .calendar-date-heading small { display: block; }
.calendar-date-heading small { margin-top: 4px; color: #93877d; font-weight: 400; }
.calendar-room-cell { padding: 10px 14px; background: #fffdfb; color: #512a18; }
.calendar-room-cell strong, .calendar-room-cell small { display: block; }
.calendar-room-cell small { margin-top: 3px; color: #93877d; font-size: 11px; font-weight: 400; }
.calendar-day-cell { padding: 4px; background: #fffdfb; }
.calendar-day-button { display: flex; width: 100%; min-height: 54px; flex-direction: column; align-items: center; justify-content: center; gap: 3px; padding: 4px 2px; border: 1px solid transparent; border-radius: 8px; color: #51473f; cursor: pointer; font: inherit; font-size: 11px; transition: border-color .15s ease, filter .15s ease; }
.calendar-day-button:hover { border-color: #c9b5a3; filter: brightness(.98); }
.calendar-day-button small { color: inherit; opacity: .72; font-size: 9px; }
.calendar-cell-available { background: #f0f6ef; color: #42634a; }
.calendar-cell-blocked { background: #fbefec; color: #8e4038; }
.calendar-cell-pending_confirm, .calendar-cell-pending_checkin { background: #fbf2e6; color: #87531e; }
.calendar-cell-checked_in, .calendar-cell-occupied { background: #f4e8dc; color: #713c1e; }
.calendar-cell-completed { background: #edf1f4; color: #536b7c; }
.calendar-state { display: flex; min-height: 220px; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: #74685e; }
.calendar-state strong { color: #512a18; }
@media (max-width: 760px) {
  .calendar-heading { align-items: flex-start; flex-direction: column; }
  .calendar-month-controls { width: 100%; }
  .calendar-month-controls input { flex: 1; }
  .calendar-legend { gap: 9px; }
  .calendar-room-heading, .calendar-room-cell { width: 92px; min-width: 92px; }
  .calendar-date-heading { width: 72px; min-width: 72px; }
}
</style>
