<script setup lang="ts">
/** 商城商品订单页面：按状态与关键词查询订单，并完成发货、完成订单等履约操作。 */
import { computed, onMounted, reactive, ref } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { MALL_PAGE_SIZE, mallApi, type MallOrder } from '../../api/mall'

interface OrderFilters {
  status: '' | MallOrder['status']
  keyword: string
}

interface ShippingForm {
  company: string
  tracking: string
}

const orders = ref<MallOrder[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const shippingOrder = ref<MallOrder | null>(null)
const busyOrderIds = ref<number[]>([])
const filters = reactive<OrderFilters>({ status: '', keyword: '' })
const shippingForm = reactive<ShippingForm>({ company: '', tracking: '' })
let loadSequence = 0

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / MALL_PAGE_SIZE)))

const statusMeta: Record<MallOrder['status'], { label: string; className: string }> = {
  PENDING_SHIPMENT: { label: '待发货', className: 'is-pending-shipment' },
  SHIPPED: { label: '已发货', className: 'is-shipped' },
  COMPLETED: { label: '已完成', className: 'is-completed' },
  REFUNDED: { label: '已退款', className: 'is-refunded' },
}

onMounted(() => { void loadOrders() })

async function loadOrders(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await mallApi.orders({
      page: page.value,
      size: MALL_PAGE_SIZE,
      status: filters.status || undefined,
      keyword: filters.keyword.trim() || undefined,
    })
    if (sequence !== loadSequence) return

    total.value = result.total
    const lastPage = Math.max(1, Math.ceil(result.total / MALL_PAGE_SIZE))
    if (page.value > lastPage) {
      page.value = lastPage
      await loadOrders()
      return
    }
    orders.value = result.list
  } catch (error) {
    if (sequence !== loadSequence) return
    errorMessage.value = messageOf(error, '商品订单读取失败')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function queryOrders(): void {
  page.value = 1
  void loadOrders()
}

function resetFilters(): void {
  filters.status = ''
  filters.keyword = ''
  queryOrders()
}

function changePage(nextPage: number): void {
  if (nextPage < 1 || nextPage > totalPages.value || loading.value) return
  page.value = nextPage
  void loadOrders()
}

function openShipping(order: MallOrder): void {
  shippingOrder.value = order
  shippingForm.company = ''
  shippingForm.tracking = ''
  errorMessage.value = ''
}

function closeShipping(): void {
  if (!saving.value) shippingOrder.value = null
}

async function ship(): Promise<void> {
  const order = shippingOrder.value
  if (saving.value || order === null) return
  const company = shippingForm.company.trim()
  const tracking = shippingForm.tracking.trim()
  if (!company || !tracking) {
    errorMessage.value = '请填写快递公司和快递单号'
    return
  }

  saving.value = true
  errorMessage.value = ''
  try {
    await mallApi.ship(order.id, company, tracking)
    successMessage.value = `订单 ${order.order_no} 已发货`
    shippingOrder.value = null
    await loadOrders()
  } catch (error) {
    errorMessage.value = messageOf(error, '商品订单发货失败')
  } finally {
    saving.value = false
  }
}

async function complete(order: MallOrder): Promise<void> {
  if (busyOrderIds.value.includes(order.id)) return
  busyOrderIds.value = [...busyOrderIds.value, order.id]
  errorMessage.value = ''
  try {
    await mallApi.complete(order.id)
    successMessage.value = `订单 ${order.order_no} 已完成`
    await loadOrders()
  } catch (error) {
    errorMessage.value = messageOf(error, '商品订单完成失败')
  } finally {
    busyOrderIds.value = busyOrderIds.value.filter((id) => id !== order.id)
  }
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
</script>

<template>
  <section class="catalog-page">
    <div class="page-heading-row">
      <div>
        <p class="page-eyebrow">MALL ORDERS</p>
        <h1>商品订单</h1>
        <p class="page-description">查看商城订单履约进度，按订单状态或关键词快速定位订单。</p>
      </div>
    </div>

    <form class="filter-card mall-order-filter" @submit.prevent="queryOrders">
      <label>订单状态
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="PENDING_SHIPMENT">待发货</option>
          <option value="SHIPPED">已发货</option>
          <option value="COMPLETED">已完成</option>
          <option value="REFUNDED">已退款</option>
        </select>
      </label>
      <label class="filter-name">关键词查询
        <input
          v-model="filters.keyword"
          maxlength="120"
          placeholder="订单号、商品名、收货人或手机号"
        />
      </label>
      <button class="button button-outline" type="submit" :disabled="loading">查询</button>
      <button class="button button-quiet" type="button" :disabled="loading" @click="resetFilters">重置</button>
    </form>

    <div class="summary-strip" aria-label="商品订单查询结果统计">
      <span>符合条件 <strong>{{ total }}</strong> 笔订单</span>
      <span>每页展示 <strong>{{ MALL_PAGE_SIZE }}</strong> 条</span>
    </div>

    <AdminNoticeDialog v-model:message="errorMessage" />
    <AdminNoticeDialog v-model:message="successMessage" kind="success" />

    <section class="table-card">
      <div v-if="loading" class="table-state">正在读取商品订单…</div>
      <div v-else-if="orders.length === 0" class="table-state">
        <strong>没有匹配的商品订单</strong>
        <span>可调整订单状态或关键词后重新查询。</span>
      </div>
      <div v-else class="table-scroll">
        <table class="data-table mall-orders-table">
          <thead>
            <tr><th>订单号</th><th>商品</th><th>实付金额</th><th>收货信息</th><th>状态</th><th>快递信息</th><th class="actions-column">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td class="mall-order-no"><strong>{{ order.order_no }}</strong></td>
              <td class="mall-product-cell"><strong>{{ order.product_name }}</strong><small>数量：{{ order.quantity }} 件</small></td>
              <td class="price-cell">¥{{ order.paid_amount }}</td>
              <td class="mall-recipient-cell">
                <strong>{{ order.recipient_name }}</strong>
                <small>{{ order.recipient_phone }}</small>
                <small class="mall-address">{{ order.shipping_address }}</small>
              </td>
              <td>
                <span class="status-pill mall-order-status" :class="statusMeta[order.status].className">
                  {{ statusMeta[order.status].label }}
                </span>
              </td>
              <td class="mall-shipping-cell">
                <template v-if="order.express_company || order.tracking_no">
                  <strong>{{ order.express_company || '快递信息待补充' }}</strong>
                  <small>{{ order.tracking_no || '未填写运单号' }}</small>
                </template>
                <span v-else>—</span>
              </td>
              <td class="row-actions">
                <button
                  v-if="order.status === 'PENDING_SHIPMENT'"
                  class="button button-quiet"
                  type="button"
                  :disabled="saving"
                  @click="openShipping(order)"
                >
                  发货
                </button>
                <button
                  v-else-if="order.status === 'SHIPPED'"
                  class="button button-quiet"
                  type="button"
                  :disabled="busyOrderIds.includes(order.id)"
                  @click="complete(order)"
                >
                  {{ busyOrderIds.includes(order.id) ? '处理中…' : '完成订单' }}
                </button>
                <span v-else>—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer v-if="total > 0" class="member-pagination">
        <span>共 {{ total }} 笔订单</span>
        <div>
          <button class="button button-outline" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <span>第 {{ page }} / {{ totalPages }} 页</span>
          <button class="button button-outline" type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </footer>
    </section>

    <Teleport to="body">
      <div v-if="shippingOrder" class="dialog-backdrop" @click.self="closeShipping">
        <section class="form-dialog compact-dialog" role="dialog" aria-modal="true" aria-label="填写快递信息" @keydown.esc="closeShipping">
          <header class="dialog-header">
            <div>
              <p class="page-eyebrow">SHIPMENT</p>
              <h2>填写快递信息</h2>
            </div>
            <button class="icon-button" type="button" aria-label="关闭" :disabled="saving" @click="closeShipping">×</button>
          </header>
          <form class="catalog-form" @submit.prevent="ship">
            <p class="form-help">订单号：{{ shippingOrder.order_no }}</p>
            <label>快递公司
              <input v-model="shippingForm.company" maxlength="100" placeholder="如：顺丰速运" required />
            </label>
            <label>快递单号
              <input v-model="shippingForm.tracking" maxlength="100" placeholder="请输入快递单号" required />
            </label>
            <footer class="dialog-actions">
              <button class="button button-outline" type="button" :disabled="saving" @click="closeShipping">取消</button>
              <button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '发货中…' : '确认发货' }}</button>
            </footer>
          </form>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.mall-order-filter { flex-wrap: wrap; }
.mall-order-filter label { flex: 1 1 190px; }
.mall-orders-table { min-width: 1100px; }
.mall-order-no strong { color: var(--qx-primary-dark); font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 11px; }
.mall-product-cell, .mall-recipient-cell, .mall-shipping-cell { white-space: normal !important; }
.mall-recipient-cell { min-width: 205px; }
.mall-shipping-cell { min-width: 180px; }
.mall-address { max-width: 230px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mall-order-status.is-pending-shipment { color: var(--qx-warning); background: var(--qx-warning-bg); }
.mall-order-status.is-shipped { color: var(--qx-info); background: var(--qx-info-bg); }
.mall-order-status.is-completed { color: var(--qx-success); background: var(--qx-success-bg); }
.mall-order-status.is-refunded { color: var(--qx-danger); background: var(--qx-danger-bg); }
</style>
