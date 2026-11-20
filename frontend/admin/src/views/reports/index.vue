<script setup lang="ts">
/** S02–S05 经营报表页面，统一提供状态范围和四维统计切换。 */
import { onMounted, ref } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { channelApi } from '../../api/channels'
import { creatorApi } from '../../api/creators'
import { reportApi } from '../../api/reports'
import { roomApi } from '../../api/rooms'
import type { Channel, Creator, Room } from '../../types/catalog'
import type {
  ReportDimension,
  ReportDimensionItem,
  ReportDimensionResult,
  ReportOverview,
  ReportScope,
} from '../../types/operations'
import '../../styles/operations.css'

const month = ref(shanghaiMonth())
const scope = ref<ReportScope>('ALL')
const dimension = ref<ReportDimension>('overview')
const selectedChannelId = ref<number | undefined>()
const selectedCreatorId = ref<number | undefined>()
const selectedRoomId = ref<number | undefined>()
const channels = ref<Channel[]>([])
const creators = ref<Creator[]>([])
const rooms = ref<Room[]>([])
const overview = ref<ReportOverview | null>(null)
const result = ref<ReportDimensionResult | null>(null)
const loading = ref(false)
const error = ref('')

const dimensionTabs: { value: ReportDimension; label: string }[] = [
  { value: 'overview', label: '经营总览' },
  { value: 'channels', label: '按来源' },
  { value: 'creators', label: '按主播' },
  { value: 'rooms', label: '按房间' },
]

async function loadOptions(): Promise<void> {
  try {
    const [channelRows, creatorRows, roomRows] = await Promise.all([
      channelApi.list(),
      creatorApi.list({}),
      roomApi.list(),
    ])
    channels.value = channelRows
    creators.value = creatorRows
    rooms.value = roomRows
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '报表筛选项加载失败'
  }
}

async function load(): Promise<void> {
  if (!month.value) return
  loading.value = true
  error.value = ''
  try {
    const filters = { month: month.value, scope: scope.value }
    const overviewRequest = reportApi.overview(filters)
    if (dimension.value === 'overview') {
      overview.value = await overviewRequest
      result.value = null
      return
    }
    const dimensionRequest = dimension.value === 'channels'
      ? reportApi.channels({ ...filters, channel_id: selectedChannelId.value })
      : dimension.value === 'creators'
        ? reportApi.creators({ ...filters, creator_id: selectedCreatorId.value })
        : reportApi.rooms({ ...filters, room_id: selectedRoomId.value })
    const [overviewData, dimensionData] = await Promise.all([overviewRequest, dimensionRequest])
    overview.value = overviewData
    result.value = dimensionData
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '经营报表加载失败'
  } finally {
    loading.value = false
  }
}

function selectDimension(next: ReportDimension): void {
  dimension.value = next
  void load()
}

function selectFilterChanged(): void {
  void load()
}

function formatPercent(rate: number | null | undefined): string {
  return rate == null ? '—' : (rate * 100).toFixed(1) + '%'
}

function rowsFor(resultValue: ReportDimensionResult | null): ReportDimensionItem[] {
  return resultValue?.list || []
}

function shanghaiDate(): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  const part = (type: string) => parts.find((item) => item.type === type)?.value || ''
  return part('year') + '-' + part('month') + '-' + part('day')
}

function shanghaiMonth(): string {
  return shanghaiDate().slice(0, 7)
}

onMounted(async () => {
  await Promise.all([loadOptions(), load()])
})
</script>

<template>
  <section class="operation-page">
    <div class="page-heading-row">
      <div>
        <p class="page-eyebrow">OPERATING REPORTS</p>
        <h1>经营报表</h1>
        <p class="page-description">按住宿夜晚归属统计金额；经营净利润始终使用已入住到手金额减实际支出。</p>
      </div>
    </div>

    <AdminNoticeDialog v-model:message="error" />

    <section class="operation-card">
      <div class="filter-card order-filter-card">
        <label>统计月份<input v-model="month" type="month" @change="load" /></label>
        <label>订单范围
          <select v-model="scope" @change="load">
            <option value="ALL">全部有效订单</option>
            <option value="NOT_CHECKED_IN">未入住订单</option>
            <option value="CHECKED_IN">已入住订单</option>
          </select>
        </label>
        <label v-if="dimension === 'channels'">订单来源
          <select v-model.number="selectedChannelId" @change="selectFilterChanged">
            <option :value="undefined">全部来源</option>
            <option v-for="channel in channels" :key="channel.id" :value="channel.id">{{ channel.name }}</option>
          </select>
        </label>
        <label v-if="dimension === 'creators'">主播
          <select v-model.number="selectedCreatorId" @change="selectFilterChanged">
            <option :value="undefined">全部主播</option>
            <option v-for="creator in creators" :key="creator.id" :value="creator.id">{{ creator.name }}</option>
          </select>
        </label>
        <label v-if="dimension === 'rooms'">客房
          <select v-model.number="selectedRoomId" @change="selectFilterChanged">
            <option :value="undefined">全部房间</option>
            <option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.number }} {{ room.name }}</option>
          </select>
        </label>
        <button class="button button-primary" type="button" @click="load" :disabled="loading">查询</button>
      </div>
    </section>

    <section v-if="overview" class="operation-card">
      <div class="operation-card-heading">
        <h2>{{ overview.month }} · {{ scope === 'ALL' ? '全部有效订单' : scope === 'CHECKED_IN' ? '已入住订单' : '未入住订单' }}</h2>
        <span class="operation-muted">已入住入住率 {{ formatPercent(overview.occupancy_rate) }}</span>
      </div>
      <div class="operation-kpis">
        <article class="operation-kpi"><small>订单数</small><strong>{{ overview.order_count }} 笔</strong></article>
        <article class="operation-kpi"><small>间夜数</small><strong>{{ overview.night_count }} 间夜</strong></article>
        <article class="operation-kpi is-highlight"><small>顾客实付</small><strong class="money-value">¥ {{ overview.paid_amount }}</strong></article>
        <article class="operation-kpi"><small>商家到手</small><strong class="money-value">¥ {{ overview.net_amount }}</strong></article>
        <article class="operation-kpi"><small>平台手续费</small><strong class="money-value">¥ {{ overview.platform_fee }}</strong></article>
        <article class="operation-kpi"><small>平台达人提成</small><strong class="money-value">¥ {{ overview.platform_commission }}</strong></article>
        <article class="operation-kpi"><small>达人额外提成</small><strong class="money-value">¥ {{ overview.extra_commission }}</strong></article>
        <article class="operation-kpi"><small>经营支出</small><strong class="money-value">¥ {{ overview.expense_amount }}</strong></article>
        <article class="operation-kpi is-profit"><small>经营净利润（固定口径）</small><strong class="money-value">¥ {{ overview.operating_profit }}</strong></article>
      </div>
    </section>

    <nav class="dimension-tabs" aria-label="报表维度">
      <button
        v-for="tab in dimensionTabs"
        :key="tab.value"
        type="button"
        :class="{ 'is-active': dimension === tab.value }"
        @click="selectDimension(tab.value)"
      >{{ tab.label }}</button>
    </nav>

    <section v-if="dimension !== 'overview'" class="table-card">
      <div class="table-scroll">
        <table class="data-table report-table">
          <thead>
            <tr>
              <th>{{ dimension === 'channels' ? '订单来源' : dimension === 'creators' ? '主播' : '房间' }}</th>
              <th>订单数</th>
              <th>间夜数</th>
              <th>顾客实付</th>
              <th>平台手续费</th>
              <th>平台达人提成</th>
              <th>达人额外提成</th>
              <th>主播总提成</th>
              <th>商家到手</th>
              <th v-if="dimension === 'rooms'">房间利润</th>
              <th v-if="dimension === 'rooms'">入住率</th>
            </tr>
          </thead>
          <tbody v-if="rowsFor(result).length">
            <tr v-for="row in rowsFor(result)" :key="row.id == null ? 'no-creator' : row.id">
              <td>{{ row.name }}</td>
              <td>{{ row.order_count }}</td>
              <td>{{ row.night_count }}</td>
              <td class="price-cell money-value">¥ {{ row.paid_amount }}</td>
              <td class="money-value">¥ {{ row.platform_fee }}</td>
              <td class="money-value">¥ {{ row.platform_commission }}</td>
              <td class="money-value">¥ {{ row.extra_commission }}</td>
              <td class="money-value">¥ {{ row.total_commission }}</td>
              <td class="money-value">¥ {{ row.net_amount }}</td>
              <td v-if="dimension === 'rooms'" class="money-value">¥ {{ row.room_profit || '0.00' }}</td>
              <td v-if="dimension === 'rooms'" class="rate-cell">{{ formatPercent(row.occupancy_rate) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="loading" class="table-state">正在计算报表…</div>
        <div v-else-if="!rowsFor(result).length" class="table-state">
          <strong>暂无统计数据</strong>
          <span>当前月份和筛选条件下没有可展示的分组。</span>
        </div>
      </div>
    </section>
    <section v-else-if="!loading && !overview" class="operation-card operation-empty">没有可展示的报表数据。</section>
  </section>
</template>
