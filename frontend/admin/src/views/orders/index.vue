<script setup lang="ts">
/** O04-O12 订单管理页面：录入第三方订单、维护资料并执行状态流转。 */
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { channelApi } from '../../api/channels'
import { creatorApi } from '../../api/creators'
import { orderApi } from '../../api/orders'
import { roomApi } from '../../api/rooms'
import type { Channel, Creator, Room } from '../../types/catalog'
import type {
  CreateExternalOrderRequest,
  OrderDetail,
  OrderFilters,
  OrderListItem,
  OrderStatus,
  UpdateOrderRequest,
} from '../../types/orders'
import { orderStatusLabels } from '../../types/orders'

interface FilterForm {
  channel_id: string
  room_id: string
  creator_id: string
  status: '' | OrderStatus
  check_in_from: string
  check_in_to: string
  guest_name: string
  phone: string
}

interface OrderForm {
  channel_id: string
  room_id: string
  creator_id: string
  platform_order_no: string
  guest_name: string
  phone: string
  guest_count: string
  check_in: string
  check_out: string
  status: Exclude<OrderStatus, 'CANCELLED'>
  remark: string
  paid_amount: string
  platform_fee: string
  platform_commission: string
  expected_amount: string
}

const channels = ref<Channel[]>([])
const route = useRoute()
const router = useRouter()
const rooms = ref<Room[]>([])
const creators = ref<Creator[]>([])
const creatorCatalog = ref<Creator[]>([])
const rows = ref<OrderListItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const dialogError = ref('')
const dialogOpen = ref(false)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<OrderDetail | null>(null)
const editingOrder = ref<OrderDetail | null>(null)
const busyOrderIds = ref<number[]>([])
const filters = reactive<FilterForm>(emptyFilters())
const form = reactive<OrderForm>(emptyOrderForm())

const externalChannels = () => channels.value.filter((channel) => channel.id !== 1 && channel.enabled)
const editableDates = () => editingOrder.value?.status === 'PENDING_CONFIRM'
  || editingOrder.value?.status === 'PENDING_CHECKIN'
const statusOptions: { value: Exclude<OrderStatus, 'CANCELLED'>; label: string }[] = [
  { value: 'PENDING_CONFIRM', label: orderStatusLabels.PENDING_CONFIRM },
  { value: 'PENDING_CHECKIN', label: orderStatusLabels.PENDING_CHECKIN },
  { value: 'CHECKED_IN', label: orderStatusLabels.CHECKED_IN },
  { value: 'COMPLETED', label: orderStatusLabels.COMPLETED },
]

onMounted(async () => {
  await Promise.all([loadCatalogs(), loadOrders()])
  await applyCalendarIntent()
})

watch(() => form.channel_id, (channelId) => {
  form.creator_id = ''
  creators.value = creatorCatalog.value.filter((creator) => creator.channel_id === Number(channelId)
    && creator.enabled)
})

watch(() => filters.channel_id, () => {
  filters.creator_id = ''
})

function emptyFilters(): FilterForm {
  return {
    channel_id: '', room_id: '', creator_id: '', status: '',
    check_in_from: '', check_in_to: '', guest_name: '', phone: '',
  }
}

function emptyOrderForm(): OrderForm {
  return {
    channel_id: '', room_id: '', creator_id: '', platform_order_no: '', guest_name: '', phone: '',
    guest_count: '', check_in: '', check_out: '', status: 'PENDING_CONFIRM', remark: '',
    paid_amount: '', platform_fee: '0.00', platform_commission: '0.00', expected_amount: '',
  }
}

async function loadCatalogs(): Promise<void> {
  try {
    const [channelList, roomList, creatorList] = await Promise.all([
      channelApi.list(), roomApi.list(), creatorApi.list({}),
    ])
    channels.value = channelList
    rooms.value = roomList
    creatorCatalog.value = creatorList
  } catch (error) {
    errorMessage.value = messageOf(error, '订单筛选资料读取失败')
  }
}

function queryFilters(): OrderFilters {
  return {
    channel_id: filters.channel_id ? Number(filters.channel_id) : undefined,
    room_id: filters.room_id ? Number(filters.room_id) : undefined,
    creator_id: filters.creator_id ? Number(filters.creator_id) : undefined,
    status: filters.status || undefined,
    check_in_from: filters.check_in_from || undefined,
    check_in_to: filters.check_in_to || undefined,
    guest_name: filters.guest_name.trim() || undefined,
    phone: filters.phone.trim() || undefined,
    page: page.value,
    size: pageSize.value,
  }
}

async function loadOrders(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await orderApi.list(queryFilters())
    rows.value = result.list
    total.value = result.total
  } catch (error) {
    errorMessage.value = messageOf(error, '订单列表读取失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void loadOrders()
}

function clearFilters(): void {
  Object.assign(filters, emptyFilters())
  page.value = 1
  void loadOrders()
}

function changePage(nextPage: number): void {
  page.value = nextPage
  void loadOrders()
}

function beginCreate(prefill?: { roomId: number | null; checkIn: string; checkOut: string }): void {
  detail.value = null
  editingOrder.value = null
  Object.assign(form, emptyOrderForm(), {
    channel_id: String(externalChannels()[0]?.id || ''),
    room_id: String(prefill?.roomId ?? rooms.value[0]?.id ?? ''),
    check_in: prefill?.checkIn || '',
    check_out: prefill?.checkOut || '',
  })
  dialogError.value = ''
  errorMessage.value = ''
  successMessage.value = ''
  dialogOpen.value = true
}

function beginCreateFromPage(): void {
  beginCreate()
}

async function applyCalendarIntent(): Promise<void> {
  const action = queryValue('calendar_action')
  if (action === 'create') {
    const requestedRoomId = Number(queryValue('room_id'))
    const roomId = Number.isSafeInteger(requestedRoomId)
      && rooms.value.some((room) => room.id === requestedRoomId)
      ? requestedRoomId
      : null
    beginCreate({
      roomId,
      checkIn: queryValue('check_in') || '',
      checkOut: queryValue('check_out') || '',
    })
  } else if (action === 'detail') {
    const orderId = Number(queryValue('order_id'))
    if (Number.isSafeInteger(orderId) && orderId > 0) await showDetail(orderId)
  }
  if (action === 'create' || action === 'detail') {
    await router.replace({ path: route.path, query: {} })
  }
}

function queryValue(key: string): string | undefined {
  const value = route.query[key]
  return typeof value === 'string' ? value : undefined
}

async function beginEdit(row: OrderListItem): Promise<void> {
  try {
    const order = await orderApi.detail(row.id)
    editingOrder.value = order
    Object.assign(form, emptyOrderForm(), {
      room_id: String(order.room_id),
      guest_name: order.guest_name || '',
      phone: order.phone || '',
      guest_count: order.guest_count == null ? '' : String(order.guest_count),
      check_in: order.check_in,
      check_out: order.check_out,
      remark: order.remark || '',
    })
    dialogError.value = ''
    errorMessage.value = ''
    dialogOpen.value = true
  } catch (error) {
    errorMessage.value = messageOf(error, '订单详情读取失败')
  }
}

async function showDetail(row: OrderListItem | number): Promise<void> {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await orderApi.detail(typeof row === 'number' ? row : row.id)
  } catch (error) {
    errorMessage.value = messageOf(error, '订单详情读取失败')
    detailOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

function closeDialog(): void {
  if (!saving.value) dialogOpen.value = false
}

async function saveOrder(): Promise<void> {
  dialogError.value = ''
  if (editingOrder.value) {
    await updateOrder()
    return
  }
  if (!form.channel_id || !form.room_id || !form.check_in || !form.check_out || !form.paid_amount) {
    dialogError.value = '请填写渠道、房间、入住退房日期和实付金额'
    return
  }
  if (form.check_out <= form.check_in) {
    dialogError.value = '退房日期必须晚于入住日期'
    return
  }
  if (![form.paid_amount, form.platform_fee, form.platform_commission, form.expected_amount]
    .filter(Boolean).every(isMoneyString)) {
    dialogError.value = '金额请输入非负数字，最多保留两位小数'
    return
  }
  if (form.creator_id && !form.expected_amount) {
    dialogError.value = '选择达人时必须填写预期收入'
    return
  }
  if (form.guest_count && (!Number.isInteger(Number(form.guest_count)) || Number(form.guest_count) < 1)) {
    dialogError.value = '入住人数须为正整数'
    return
  }

  const request: CreateExternalOrderRequest = {
    channel_id: Number(form.channel_id),
    room_id: Number(form.room_id),
    creator_id: form.creator_id ? Number(form.creator_id) : null,
    platform_order_no: form.platform_order_no.trim() || null,
    guest_name: form.guest_name.trim() || null,
    phone: form.phone.trim() || null,
    guest_count: form.guest_count ? Number(form.guest_count) : null,
    check_in: form.check_in,
    check_out: form.check_out,
    status: form.status,
    remark: form.remark.trim() || null,
    amount: {
      paid_amount: form.paid_amount.trim(),
      platform_fee: form.platform_fee.trim() || '0.00',
      platform_commission: form.platform_commission.trim() || '0.00',
      expected_amount: form.creator_id ? form.expected_amount.trim() : null,
    },
  }
  saving.value = true
  try {
    const created = await orderApi.create(request)
    dialogOpen.value = false
    successMessage.value = `订单 ${created.order_no} 已录入，到手金额 ¥${created.net_amount}`
    page.value = 1
    await loadOrders()
  } catch (error) {
    dialogError.value = messageOf(error, '订单录入失败')
  } finally {
    saving.value = false
  }
}

async function updateOrder(): Promise<void> {
  const order = editingOrder.value
  if (!order) return
  if (editableDates() && form.check_out <= form.check_in) {
    dialogError.value = '退房日期必须晚于入住日期'
    return
  }
  if (form.guest_count && (!Number.isInteger(Number(form.guest_count)) || Number(form.guest_count) < 1)) {
    dialogError.value = '入住人数须为正整数'
    return
  }
  const request: UpdateOrderRequest = {
    guest_name: form.guest_name.trim(),
    phone: form.phone.trim(),
    guest_count: form.guest_count ? Number(form.guest_count) : null,
    remark: form.remark.trim(),
  }
  if (editableDates()) {
    request.room_id = Number(form.room_id)
    request.check_in = form.check_in
    request.check_out = form.check_out
  }
  saving.value = true
  try {
    await orderApi.update(order.id, request)
    dialogOpen.value = false
    successMessage.value = '订单资料已更新'
    await loadOrders()
  } catch (error) {
    dialogError.value = messageOf(error, '订单资料更新失败')
  } finally {
    saving.value = false
  }
}

async function runAction(row: OrderListItem, action: 'confirm' | 'check-in' | 'check-out' | 'cancel'): Promise<void> {
  let reason = ''
  if (action === 'cancel') {
    const input = window.prompt(`取消第三方订单 ${row.order_no}。可填写取消原因：`, '')
    if (input === null) return
    reason = input.trim()
  }
  busyOrderIds.value = [...busyOrderIds.value, row.id]
  errorMessage.value = ''
  successMessage.value = ''
  try {
    if (action === 'confirm') await orderApi.confirm(row.id)
    if (action === 'check-in') await orderApi.checkIn(row.id)
    if (action === 'check-out') await orderApi.checkOut(row.id)
    if (action === 'cancel') await orderApi.cancel(row.id, reason)
    successMessage.value = action === 'cancel' ? '第三方订单已取消' : `订单已${actionLabel(action)}`
    await loadOrders()
  } catch (error) {
    errorMessage.value = messageOf(error, '订单状态更新失败')
  } finally {
    busyOrderIds.value = busyOrderIds.value.filter((id) => id !== row.id)
  }
}

function actionLabel(action: 'confirm' | 'check-in' | 'check-out' | 'cancel'): string {
  if (action === 'confirm') return '确认'
  if (action === 'check-in') return '办理入住'
  if (action === 'check-out') return '办理退房'
  return '取消'
}

function statusClass(status: OrderStatus): string {
  if (status === 'CANCELLED') return 'is-disabled'
  if (status === 'COMPLETED') return 'is-complete'
  if (status === 'CHECKED_IN') return 'is-occupied'
  return 'is-pending'
}

function channelName(channelId: number): string {
  return channels.value.find((channel) => channel.id === channelId)?.name || '未知渠道'
}

function roomName(roomId: number): string {
  const room = rooms.value.find((item) => item.id === roomId)
  return room ? `${room.number} · ${room.name}` : `房间 ${roomId}`
}

function filteredCreators(): Creator[] {
  return filters.channel_id
    ? creatorCatalog.value.filter((creator) => creator.channel_id === Number(filters.channel_id))
    : creatorCatalog.value
}

function isMoneyString(value: string): boolean {
  return /^\d{1,10}(\.\d{1,2})?$/.test(value.trim())
}

function money(value: string | null): string {
  return value == null ? '—' : `¥${value}`
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
</script>

<template>
  <section class="catalog-page">
    <div class="page-heading-row">
      <div>
        <p class="page-eyebrow">ORDER MANAGEMENT</p>
        <h1>订单管理</h1>
        <p class="page-description">统一查看七渠道订单；此处手工录入第三方订单，订单创建后金额快照不可修改。</p>
      </div>
      <button class="button button-primary" type="button" :disabled="rooms.length === 0 || externalChannels().length === 0" @click="beginCreateFromPage">
        新增第三方订单
      </button>
    </div>

    <div class="summary-strip">
      <span>当前筛选订单<strong>{{ total }}</strong></span>
      <span>页码<strong>{{ page }}</strong></span>
    </div>

    <form class="filter-card order-filter-card" @submit.prevent="search">
      <label>渠道
        <select v-model="filters.channel_id"><option value="">全部渠道</option><option v-for="item in channels" :key="item.id" :value="String(item.id)">{{ item.name }}</option></select>
      </label>
      <label>房间
        <select v-model="filters.room_id"><option value="">全部房间</option><option v-for="item in rooms" :key="item.id" :value="String(item.id)">{{ item.number }} · {{ item.name }}</option></select>
      </label>
      <label>订单状态
        <select v-model="filters.status"><option value="">全部状态</option><option v-for="item in Object.entries(orderStatusLabels)" :key="item[0]" :value="item[0]">{{ item[1] }}</option></select>
      </label>
      <label>达人
        <select v-model="filters.creator_id"><option value="">全部达人</option><option v-for="creator in filteredCreators()" :key="creator.id" :value="String(creator.id)">{{ creator.name }}{{ creator.enabled ? '' : '（停用）' }}</option></select>
      </label>
      <label>入住从<input v-model="filters.check_in_from" type="date"></label>
      <label>入住至<input v-model="filters.check_in_to" type="date"></label>
      <label class="filter-name">预订人<input v-model="filters.guest_name" maxlength="80" placeholder="按姓名搜索"></label>
      <label>手机号<input v-model="filters.phone" maxlength="20" placeholder="按手机号搜索"></label>
      <div class="order-filter-actions">
        <button class="button button-outline" type="submit" :disabled="loading">查询</button>
        <button class="button button-quiet" type="button" :disabled="loading" @click="clearFilters">清除</button>
      </div>
    </form>

    <p v-if="successMessage" class="notice notice-success" role="status">{{ successMessage }}</p>
    <p v-if="errorMessage" class="notice notice-error" role="alert">{{ errorMessage }}</p>

    <div class="table-card">
      <div v-if="loading" class="table-state">正在读取订单…</div>
      <div v-else-if="rows.length === 0" class="table-state"><strong>没有匹配的订单</strong><span>录入订单或调整筛选条件。</span></div>
      <div v-else class="table-scroll">
        <table class="data-table orders-table">
          <thead><tr>
            <th>订单与渠道</th><th>预订人</th><th>客房</th><th>住宿区间</th><th>状态</th><th>金额快照</th><th class="order-actions-heading">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td><strong>{{ row.order_no }}</strong><small>{{ channelName(row.channel_id) }}</small></td>
              <td><strong>{{ row.guest_name || '未填写' }}</strong><small>{{ row.phone || '无联系电话' }}</small></td>
              <td>{{ row.room_number }}</td>
              <td><strong>{{ row.check_in }} 至 {{ row.check_out }}</strong><small>{{ row.nights }} 晚</small></td>
              <td><span class="status-pill" :class="statusClass(row.status)">{{ orderStatusLabels[row.status] }}</span></td>
              <td><strong class="price-cell">{{ money(row.paid_amount) }}</strong><small>到手 {{ money(row.net_amount) }}</small></td>
              <td class="row-actions order-actions">
                <button class="button button-quiet" type="button" @click="showDetail(row)">详情</button>
                <button class="button button-quiet" type="button" @click="beginEdit(row)">编辑</button>
                <button v-if="row.status === 'PENDING_CONFIRM'" class="button button-quiet" type="button" :disabled="busyOrderIds.includes(row.id)" @click="runAction(row, 'confirm')">确认</button>
                <button v-if="row.status === 'PENDING_CHECKIN'" class="button button-quiet" type="button" :disabled="busyOrderIds.includes(row.id)" @click="runAction(row, 'check-in')">入住</button>
                <button v-if="row.status === 'CHECKED_IN'" class="button button-quiet" type="button" :disabled="busyOrderIds.includes(row.id)" @click="runAction(row, 'check-out')">退房</button>
                <button v-if="row.channel_id !== 1 && ['PENDING_CONFIRM', 'PENDING_CHECKIN'].includes(row.status)" class="button button-quiet danger-action" type="button" :disabled="busyOrderIds.includes(row.id)" @click="runAction(row, 'cancel')">取消</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer class="order-pagination">
        <span>共 {{ total }} 条</span>
        <label>每页
          <select v-model.number="pageSize" @change="page = 1; loadOrders()"><option :value="20">20 条</option><option :value="50">50 条</option><option :value="100">100 条</option></select>
        </label>
        <button class="button button-outline" type="button" :disabled="loading || page <= 1" @click="changePage(page - 1)">上一页</button>
        <span>第 {{ page }} / {{ Math.max(1, Math.ceil(total / pageSize)) }} 页</span>
        <button class="button button-outline" type="button" :disabled="loading || page >= Math.ceil(total / pageSize)" @click="changePage(page + 1)">下一页</button>
      </footer>
    </div>

    <div v-if="dialogOpen" class="dialog-backdrop" @click.self="closeDialog">
      <section class="form-dialog order-dialog" role="dialog" aria-modal="true" aria-labelledby="order-dialog-title">
        <header class="dialog-header">
          <div><p class="page-eyebrow">{{ editingOrder ? 'ORDER PROFILE' : 'EXTERNAL ORDER' }}</p><h2 id="order-dialog-title">{{ editingOrder ? '编辑订单资料' : '新增第三方订单' }}</h2></div>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeDialog">×</button>
        </header>
        <form class="catalog-form" @submit.prevent="saveOrder">
          <p v-if="dialogError" class="notice notice-error" role="alert">{{ dialogError }}</p>
          <div v-if="!editingOrder" class="form-grid">
            <label>订单渠道
              <select v-model="form.channel_id" required><option value="" disabled>选择外部渠道</option><option v-for="item in externalChannels()" :key="item.id" :value="String(item.id)">{{ item.name }}</option></select>
            </label>
            <label>初始状态
              <select v-model="form.status"><option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select>
            </label>
            <label>客房
              <select v-model="form.room_id" required><option value="" disabled>选择客房</option><option v-for="room in rooms" :key="room.id" :value="String(room.id)">{{ room.number }} · {{ room.name }}</option></select>
            </label>
            <label>达人（选填）
              <select v-model="form.creator_id"><option value="">无达人</option><option v-for="creator in creators" :key="creator.id" :value="String(creator.id)">{{ creator.name }}</option></select>
            </label>
            <label>平台订单号（选填）<input v-model="form.platform_order_no" maxlength="100" placeholder="平台原始订单号"></label>
            <label>入住人数（选填）<input v-model="form.guest_count" inputmode="numeric" placeholder="人数"></label>
            <label>入住日期<input v-model="form.check_in" type="date" required></label>
            <label>退房日期（不含）<input v-model="form.check_out" type="date" :min="form.check_in || undefined" required></label>
            <label>预订人（选填）<input v-model="form.guest_name" maxlength="80" placeholder="姓名"></label>
            <label>联系电话（选填）<input v-model="form.phone" maxlength="20" placeholder="手机号"></label>
          </div>
          <div v-else class="form-grid">
            <label>预订人<input v-model="form.guest_name" maxlength="80" placeholder="姓名"></label>
            <label>联系电话<input v-model="form.phone" maxlength="20" placeholder="手机号"></label>
            <label>入住人数<input v-model="form.guest_count" inputmode="numeric" placeholder="人数"></label>
            <label v-if="editableDates()">客房
              <select v-model="form.room_id"><option v-for="room in rooms" :key="room.id" :value="String(room.id)">{{ room.number }} · {{ room.name }}</option></select>
            </label>
            <label v-if="editableDates()">入住日期<input v-model="form.check_in" type="date"></label>
            <label v-if="editableDates()">退房日期（不含）<input v-model="form.check_out" type="date" :min="form.check_in || undefined"></label>
          </div>
          <div v-if="!editingOrder" class="amount-snapshot-block">
            <h3>金额快照</h3>
            <div class="form-grid">
              <label>顾客实付金额<input v-model="form.paid_amount" inputmode="decimal" placeholder="0.00" required></label>
              <label>平台手续费<input v-model="form.platform_fee" inputmode="decimal" placeholder="0.00"></label>
              <label>平台达人提成<input v-model="form.platform_commission" inputmode="decimal" placeholder="无达人时系统按 0 保存"></label>
              <label>预期收入<input v-model="form.expected_amount" inputmode="decimal" :disabled="!form.creator_id" :required="Boolean(form.creator_id)" placeholder="选择达人时必填"></label>
            </div>
            <p class="form-help">额外提成和商家到手金额由后端计算。订单创建后金额不可修改；录错时须按规则取消并重新录入。</p>
          </div>
          <label>备注<textarea v-model="form.remark" maxlength="500" rows="3" placeholder="选填"></textarea></label>
          <p v-if="editingOrder" class="form-help">金额快照不可修改。{{ editableDates() ? '房间或日期只能调整为相同住宿晚数，并由后端重新校验房态。' : '当前状态只允许编辑联系人、人数和备注。' }}</p>
          <footer class="dialog-actions">
            <button class="button button-outline" type="button" :disabled="saving" @click="closeDialog">取消</button>
            <button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : editingOrder ? '保存资料' : '创建订单' }}</button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="detailOpen" class="dialog-backdrop" @click.self="detailOpen = false">
      <section class="form-dialog order-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="order-detail-title">
        <header class="dialog-header">
          <div><p class="page-eyebrow">ORDER DETAIL</p><h2 id="order-detail-title">订单详情</h2></div>
          <button class="icon-button" type="button" aria-label="关闭" @click="detailOpen = false">×</button>
        </header>
        <div v-if="detailLoading" class="table-state">正在读取订单详情…</div>
        <template v-else-if="detail">
          <div class="order-detail-grid">
            <div><small>系统订单号</small><strong>{{ detail.order_no }}</strong></div>
            <div><small>平台订单号</small><strong>{{ detail.platform_order_no || '未填写' }}</strong></div>
            <div><small>渠道 / 客房</small><strong>{{ detail.channel_name }} · {{ roomName(detail.room_id) }}</strong></div>
            <div><small>订单状态</small><strong>{{ orderStatusLabels[detail.status] }}</strong></div>
            <div><small>预订人 / 联系电话</small><strong>{{ detail.guest_name || '未填写' }} · {{ detail.phone || '未填写' }}</strong></div>
            <div><small>入住人数 / 晚数</small><strong>{{ detail.guest_count ?? '未填写' }} 人 · {{ detail.nights }} 晚</strong></div>
            <div><small>入住日期</small><strong>{{ detail.check_in }}</strong></div>
            <div><small>退房日期（不含）</small><strong>{{ detail.check_out }}</strong></div>
          </div>
          <h3 class="detail-section-title">金额快照</h3>
          <div class="order-detail-grid money-detail-grid">
            <div><small>顾客实付金额</small><strong>{{ money(detail.paid_amount) }}</strong></div>
            <div><small>平台手续费</small><strong>{{ money(detail.platform_fee) }}</strong></div>
            <div><small>平台达人提成</small><strong>{{ money(detail.platform_commission) }}</strong></div>
            <div><small>达人额外提成</small><strong>{{ money(detail.extra_commission) }}</strong></div>
            <div><small>预期收入</small><strong>{{ money(detail.expected_amount) }}</strong></div>
            <div><small>商家到手金额</small><strong>{{ money(detail.net_amount) }}</strong></div>
          </div>
          <div class="order-detail-note"><small>备注</small><p>{{ detail.remark || '无' }}</p></div>
          <footer class="dialog-actions"><button class="button button-outline" type="button" @click="detailOpen = false">关闭</button></footer>
        </template>
      </section>
    </div>
  </section>
</template>
