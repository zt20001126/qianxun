<script setup lang="ts">
/** O04-O12 订单管理页面：多维度筛选、关键词实时搜索、8列明细展示、状态逐级流转及页内弹窗编辑。 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { useRoute, useRouter } from 'vue-router'
import { channelApi } from '../../api/channels'
import { creatorApi } from '../../api/creators'
import { orderApi } from '../../api/orders'
import { roomApi } from '../../api/rooms'
import ExternalOrderCreateModal from '../../components/operations/ExternalOrderCreateModal.vue'
import OrderDetailModal from '../../components/operations/OrderDetailModal.vue'
import type { Channel, Creator, Room } from '../../types/catalog'
import type { OrderDetail, OrderFilters, OrderListItem, OrderStatus } from '../../types/orders'
import { amountToCents } from '../../utils/operations'
import '../../styles/operations.css'

interface OrderFilterState {
  channel_id: string
  creator_id: string
  status: '' | OrderStatus
  keyword: string
  room_id: string
  check_in: string
}

const route = useRoute()
const router = useRouter()

const channels = ref<Channel[]>([])
const rooms = ref<Room[]>([])
const creators = ref<Creator[]>([])
const rows = ref<OrderListItem[]>([])
const detailCache = ref<Record<number, OrderDetail>>({})

const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const errorMessage = ref('')
const busyOrderIds = ref<number[]>([])
let loadSequence = 0

const filters = reactive<OrderFilterState>({
  channel_id: '',
  creator_id: '',
  status: '',
  keyword: '',
  room_id: '',
  check_in: '',
})

// 页内弹窗状态
const createModalOpen = ref(false)
const createPresetRoomId = ref<number | null>(null)
const createPresetCheckIn = ref('')

const detailModalOpen = ref(false)
const activeOrderId = ref<number | null>(null)

// 操作成功弹窗
const successMessage = ref('')

const displayRows = computed(() => rows.value)
const filteredCount = computed(() => total.value)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const statusBadgeMap: Record<OrderStatus, { label: string; cls: string }> = {
  PENDING_CONFIRM: { label: '已预订(待确认)', cls: 'is-reserved' },
  PENDING_CHECKIN: { label: '待入住', cls: 'is-pending' },
  CHECKED_IN: { label: '已入住', cls: 'is-checked-in' },
  COMPLETED: { label: '已退房(已完成)', cls: 'is-completed' },
  CANCELLED: { label: '已取消', cls: 'is-cancelled' },
}

async function loadCatalogs(): Promise<void> {
  try {
    const [channelList, roomList, creatorList] = await Promise.all([
      channelApi.list(),
      roomApi.list(),
      creatorApi.list({}),
    ])
    channels.value = channelList
    rooms.value = roomList
    creators.value = creatorList
  } catch (error) {
    errorMessage.value = messageOf(error, '订单筛选基础资料读取失败')
  }
}

function buildBaseFilters(reqPage: number, reqSize: number): OrderFilters {
  const numericCreatorId =
    filters.creator_id && filters.creator_id !== 'NONE' ? Number(filters.creator_id) : undefined
  return {
    channel_id: filters.channel_id ? Number(filters.channel_id) : undefined,
    creator_id: numericCreatorId,
    status: filters.status || undefined,
    room_id: filters.room_id ? Number(filters.room_id) : undefined,
    check_in_from: filters.check_in || undefined,
    check_in_to: filters.check_in || undefined,
    keyword: filters.keyword.trim() || undefined,
    without_creator: filters.creator_id === 'NONE',
    page: reqPage,
    size: reqSize,
  }
}

async function loadOrders(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await orderApi.list(buildBaseFilters(page.value, pageSize.value))
    if (sequence !== loadSequence) return
    const list = result.list
    rows.value = list
    total.value = result.total

    // 并行补齐未缓存的订单详情（获取平台订单号与入住人数）
    const missingIds = list.map((item) => item.id).filter((id) => !detailCache.value[id])
    if (missingIds.length > 0) {
      const settled = await Promise.allSettled(missingIds.map((id) => orderApi.detail(id)))
      if (sequence !== loadSequence) return
      const nextCache = { ...detailCache.value }
      settled.forEach((entry, index) => {
        if (entry.status === 'fulfilled') nextCache[missingIds[index]] = entry.value
      })
      detailCache.value = nextCache
    }
  } catch (error) {
    if (sequence !== loadSequence) return
    errorMessage.value = messageOf(error, '订单列表读取失败')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function onSelectFilterChange(): void {
  page.value = 1
  void loadOrders()
}

function clearKeyword(): void {
  filters.keyword = ''
  page.value = 1
  void loadOrders()
}

function resetFilters(): void {
  filters.channel_id = ''
  filters.creator_id = ''
  filters.status = ''
  filters.keyword = ''
  filters.room_id = ''
  filters.check_in = ''
  page.value = 1
  void loadOrders()
}

function changePage(nextPage: number): void {
  page.value = nextPage
  void loadOrders()
}

function onPageSizeChange(): void {
  page.value = 1
  void loadOrders()
}

// 关键词变化后稍作等待，避免连续输入产生过多请求。
watch(() => filters.keyword, (_value, _oldValue, onCleanup) => {
  const timer = setTimeout(() => { page.value = 1; void loadOrders() }, 300)
  onCleanup(() => clearTimeout(timer))
})

function openCreateModal(prefill?: { roomId?: number | null; checkIn?: string }): void {
  createPresetRoomId.value = prefill?.roomId ?? null
  createPresetCheckIn.value = prefill?.checkIn || ''
  createModalOpen.value = true
}

function openDetailModal(orderId: number): void {
  activeOrderId.value = orderId
  detailModalOpen.value = true
}

async function advanceStatus(row: OrderListItem, action: 'confirm' | 'check-in' | 'check-out'): Promise<void> {
  busyOrderIds.value = [...busyOrderIds.value, row.id]
  errorMessage.value = ''
  try {
    if (action === 'confirm') {
      await orderApi.confirm(row.id)
      showSuccessNotice(`订单 ${row.order_no} 已确认，进入待入住`)
    } else if (action === 'check-in') {
      await orderApi.checkIn(row.id)
      showSuccessNotice(`订单 ${row.order_no} 已办理入住`)
    } else {
      await orderApi.checkOut(row.id)
      showSuccessNotice(`订单 ${row.order_no} 已办理退房完成`)
    }
    const nextCache = { ...detailCache.value }
    delete nextCache[row.id]
    detailCache.value = nextCache
    await loadOrders()
  } catch (error) {
    errorMessage.value = messageOf(error, '订单状态更新失败')
  } finally {
    busyOrderIds.value = busyOrderIds.value.filter((id) => id !== row.id)
  }
}

function handleOrderCreated(message: string): void {
  createModalOpen.value = false
  showSuccessNotice(message)
  page.value = 1
  void loadOrders()
}

function handleOrderUpdated(message: string): void {
  const updatedId = activeOrderId.value
  detailModalOpen.value = false
  showSuccessNotice(message)
  if (updatedId != null) {
    const nextCache = { ...detailCache.value }
    delete nextCache[updatedId]
    detailCache.value = nextCache
  }
  void loadOrders()
}

function channelName(channelId: number): string {
  return channels.value.find((ch) => ch.id === channelId)?.name || '未知来源'
}

function creatorName(creatorId: number | null): string {
  if (creatorId == null) return ''
  return creators.value.find((cr) => cr.id === creatorId)?.name || ''
}

function roomName(roomId: number): string {
  return rooms.value.find((rm) => rm.id === roomId)?.name || ''
}

function platformOrderNo(orderId: number): string | null {
  return detailCache.value[orderId]?.platform_order_no ?? null
}

function guestCount(orderId: number): number | null {
  return detailCache.value[orderId]?.guest_count ?? null
}

function hasExtraCommission(value: string): boolean {
  return amountToCents(value) > 0
}

function showSuccessNotice(message: string): void {
  successMessage.value = message
}

function queryValue(key: string): string | undefined {
  const value = route.query[key]
  return typeof value === 'string' ? value : undefined
}

async function applyRouteIntent(): Promise<void> {
  const action = queryValue('calendar_action')
  if (action === 'create') {
    const requestedRoomId = Number(queryValue('room_id'))
    const roomId =
      Number.isSafeInteger(requestedRoomId) && rooms.value.some((r) => r.id === requestedRoomId)
        ? requestedRoomId
        : null
    openCreateModal({ roomId, checkIn: queryValue('check_in') || '' })
  } else if (action === 'detail') {
    const orderId = Number(queryValue('order_id'))
    if (Number.isSafeInteger(orderId) && orderId > 0) openDetailModal(orderId)
  }
  if (action === 'create' || action === 'detail') {
    await router.replace({ path: route.path, query: {} })
  }
}

watch(
  () => route.query.calendar_action,
  (action) => {
    if (action === 'create' || action === 'detail') void applyRouteIntent()
  },
)

function messageOf(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}

onMounted(async () => {
  await Promise.all([loadCatalogs(), loadOrders()])
  await applyRouteIntent()
})

</script>

<template>
  <section class="orders-proto-page">
    <!-- 1. 顶部多维度筛选与关键词搜索栏 -->
    <section class="orders-filter-card" aria-label="订单筛选与搜索">
      <div class="orders-filter-controls">
        <!-- ① 订单来源筛选 -->
        <select
          v-model="filters.channel_id"
          aria-label="订单来源筛选"
          class="orders-filter-select"
          @change="onSelectFilterChange"
        >
          <option value="">全部订单来源</option>
          <option v-for="ch in channels" :key="ch.id" :value="String(ch.id)">
            来源：{{ ch.name }}
          </option>
        </select>

        <!-- ② 合作达人筛选 -->
        <select
          v-model="filters.creator_id"
          aria-label="合作达人筛选"
          class="orders-filter-select"
          @change="onSelectFilterChange"
        >
          <option value="">全部达人/主播</option>
          <option value="NONE">无达人（自然流量）</option>
          <option v-for="cr in creators" :key="cr.id" :value="String(cr.id)">
            达人：{{ cr.name }}
          </option>
        </select>

        <!-- ③ 订单状态筛选 -->
        <select
          v-model="filters.status"
          aria-label="订单状态筛选"
          class="orders-filter-select"
          @change="onSelectFilterChange"
        >
          <option value="">全部订单状态</option>
          <option value="PENDING_CONFIRM">已预订（待确认）</option>
          <option value="PENDING_CHECKIN">待入住</option>
          <option value="CHECKED_IN">已入住</option>
          <option value="COMPLETED">已退房（已完成）</option>
          <option value="CANCELLED">已取消</option>
        </select>

        <select v-model="filters.room_id" aria-label="房间筛选" class="orders-filter-select" @change="onSelectFilterChange">
          <option value="">全部房间</option>
          <option v-for="room in rooms" :key="room.id" :value="String(room.id)">{{ room.number }} · {{ room.name }}</option>
        </select>
        <input v-model="filters.check_in" type="date" aria-label="入住日期筛选" class="orders-filter-select" @change="onSelectFilterChange" />
        <!-- ④ 关键词搜索输入框 -->
        <div class="orders-search-box">
          <input
            v-model="filters.keyword"
            type="text"
            aria-label="搜索订单号、预订人或手机号"
            placeholder="搜索订单号 / 预订人 / 手机号"
            class="orders-search-input"
          />
          <button
            v-if="filters.keyword"
            type="button"
            class="orders-search-clear"
            title="清空搜索"
            aria-label="清空搜索"
            @click="clearKeyword"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <!-- ⑤ 重置筛选按钮 -->
        <button type="button" class="orders-reset-btn" :disabled="loading" @click="resetFilters">
          重置筛选
        </button>
      </div>

      <div class="orders-filter-summary">
        共筛选出 <span class="is-mono">{{ filteredCount }}</span> 笔订单
      </div>
    </section>

    <AdminNoticeDialog v-model:message="errorMessage" />
    <AdminNoticeDialog v-model:message="successMessage" kind="success" />

    <!-- 2. 订单明细列表（8 列完整字段结构） -->
    <section class="orders-table-card">
      <div class="orders-table-scroll" :class="{ 'is-loading': loading }" :aria-busy="loading">
        <table class="orders-proto-table">
          <thead>
            <tr>
              <th class="col-left">订单编号与来源（含达人）</th>
              <th class="col-left">房间与入离日期</th>
              <th class="col-left">预订人与手机（含人数）</th>
              <th class="col-right">实付金额</th>
              <th class="col-right">手续费 / 达人提成 / 额外提成</th>
              <th class="col-right">商家到手</th>
              <th class="col-center">状态</th>
              <th class="col-right col-actions">操作</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="loading && displayRows.length === 0">
              <td colspan="8" class="orders-empty-td">正在读取订单数据…</td>
            </tr>
            <tr v-else-if="displayRows.length === 0">
              <td colspan="8" class="orders-empty-td">暂无符合当前筛选条件的订单记录</td>
            </tr>
            <tr v-for="row in displayRows" :key="row.id" class="orders-data-row">
              <!-- ① 订单编号与来源（含达人） -->
              <td class="td-pad-lg">
                <div class="order-no-text is-mono">{{ row.order_no }}</div>
                <div v-if="platformOrderNo(row.id)" class="order-plat-no is-mono">
                  平台单号：{{ platformOrderNo(row.id) }}
                </div>
                <div class="order-source-tags">
                  <span class="order-channel-badge">{{ channelName(row.channel_id) }}</span>
                  <span v-if="creatorName(row.creator_id)" class="order-creator-badge">
                    达人：{{ creatorName(row.creator_id) }}
                  </span>
                </div>
              </td>

              <!-- ② 房间与入离日期 -->
              <td>
                <div class="order-room-row">
                  <span class="order-room-num is-mono">{{ row.room_number }}</span>
                  <strong class="order-room-name">{{ roomName(row.room_id) }}</strong>
                </div>
                <div class="order-dates-row is-mono">
                  {{ row.check_in }} 至 {{ row.check_out }}（共 {{ row.nights }} 晚）
                </div>
              </td>

              <!-- ③ 预订人与手机（含人数） -->
              <td>
                <div class="order-guest-row">
                  <strong class="order-guest-name">{{ row.guest_name || '—' }}</strong>
                  <span v-if="guestCount(row.id) != null" class="order-guest-pill">
                    {{ guestCount(row.id) }}人入住
                  </span>
                </div>
                <div class="order-phone-row is-mono">{{ row.phone || '未填手机号' }}</div>
              </td>

              <!-- ④ 实付金额 -->
              <td class="col-right">
                <span class="order-paid-text money-value">¥{{ row.paid_amount }}</span>
              </td>

              <!-- ⑤ 手续费 / 达人提成 / 额外提成 -->
              <td class="col-right">
                <div class="order-fee-lines money-value">
                  <div>
                    平台手续费：<span class="fee-val">¥{{ row.platform_fee }}</span>
                  </div>
                  <div>
                    平台达人提成：<span class="fee-val">¥{{ row.platform_commission }}</span>
                  </div>
                  <div>
                    达人额外提成：<span
                      :class="hasExtraCommission(row.extra_commission) ? 'fee-val-amber' : 'fee-val'"
                    >¥{{ row.extra_commission }}</span>
                  </div>
                </div>
              </td>

              <!-- ⑥ 商家到手 -->
              <td class="col-right">
                <span class="order-net-text money-value">¥{{ row.net_amount }}</span>
              </td>

              <!-- ⑦ 状态 -->
              <td class="col-center">
                <span class="order-status-capsule" :class="statusBadgeMap[row.status].cls">
                  {{ statusBadgeMap[row.status].label }}
                </span>
              </td>

              <!-- ⑧ 操作 -->
              <td class="col-right td-pad-lg td-nowrap">
                <div class="order-action-group">
                  <button
                    v-if="row.status === 'PENDING_CONFIRM'"
                    type="button"
                    class="order-btn-action is-confirm"
                    :disabled="busyOrderIds.includes(row.id)"
                    @click="advanceStatus(row, 'confirm')"
                  >
                    确认订单
                  </button>
                  <button
                    v-else-if="row.status === 'PENDING_CHECKIN'"
                    type="button"
                    class="order-btn-action is-checkin"
                    :disabled="busyOrderIds.includes(row.id)"
                    @click="advanceStatus(row, 'check-in')"
                  >
                    办理入住
                  </button>
                  <button
                    v-else-if="row.status === 'CHECKED_IN'"
                    type="button"
                    class="order-btn-action is-checkout"
                    :disabled="busyOrderIds.includes(row.id)"
                    @click="advanceStatus(row, 'check-out')"
                  >
                    办理退房
                  </button>

                  <button
                    type="button"
                    class="order-btn-detail"
                    @click="openDetailModal(row.id)"
                  >
                    详情/编辑
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 订单分页 -->
      <footer v-if="total > 0" class="orders-pagination-bar">
        <span class="orders-page-info">共 {{ total }} 条记录</span>
        <div class="orders-page-controls">
          <label class="orders-page-size">
            每页
            <select v-model.number="pageSize" @change="onPageSizeChange">
              <option :value="10">10 条</option>
              <option :value="20">20 条</option>
              <option :value="50">50 条</option>
              <option :value="100">100 条</option>
            </select>
          </label>
          <button
            type="button"
            class="orders-page-btn"
            :disabled="loading || page <= 1"
            @click="changePage(page - 1)"
          >
            上一页
          </button>
          <span class="orders-page-num is-mono">{{ page }} / {{ totalPages }}</span>
          <button
            type="button"
            class="orders-page-btn"
            :disabled="loading || page >= totalPages"
            @click="changePage(page + 1)"
          >
            下一页
          </button>
        </div>
      </footer>
    </section>

    <!-- 页内弹窗：创建第三方平台订单 -->
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

    <!-- 页内弹窗：订单详情、资料编辑、状态流转与第三方取消 -->
    <OrderDetailModal
      :open="detailModalOpen"
      :order-id="activeOrderId"
      :rooms="rooms"
      :creators="creators"
      @close="detailModalOpen = false"
      @updated="handleOrderUpdated"
    />

  </section>
</template>
