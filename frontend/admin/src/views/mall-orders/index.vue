<script setup lang="ts">
/** 后台查看商品订单、填写快递信息并完成订单。 */
import { onMounted, reactive, ref } from 'vue'
import { mallApi, type MallOrder } from '../../api/mall'

const orders = ref<MallOrder[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const page = ref(1)
const total = ref(0)
const shippingId = ref<number | null>(null)
const form = reactive({ company: '', tracking: '' })
const labels: Record<MallOrder['status'], string> = {
  PENDING_SHIPMENT: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', REFUNDED: '已退款',
}
async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const data = await mallApi.orders(page.value)
    orders.value = data.list
    total.value = data.total
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '加载订单失败' }
  finally { loading.value = false }
}
function openShipping(id: number): void {
  shippingId.value = id
  form.company = ''
  form.tracking = ''
}
async function ship(): Promise<void> {
  if (saving.value || shippingId.value === null) return
  saving.value = true
  error.value = ''
  try {
    await mallApi.ship(shippingId.value, form.company, form.tracking)
    shippingId.value = null
    await load()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '发货失败' }
  finally { saving.value = false }
}
async function complete(id: number): Promise<void> {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try { await mallApi.complete(id); await load() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '完成订单失败' }
  finally { saving.value = false }
}
async function changePage(next: number): Promise<void> { page.value = next; await load() }
onMounted(load)
</script>

<template>
  <section class="mall-orders">
    <h1>商品订单</h1>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading">正在加载订单…</p>
    <p v-else-if="!orders.length">暂无商品订单</p>
    <div v-else class="table-wrap"><table>
      <thead><tr><th>订单号</th><th>商品</th><th>实付</th><th>收货信息</th><th>状态</th><th>快递</th><th>操作</th></tr></thead>
      <tbody><tr v-for="order in orders" :key="order.id">
        <td>{{ order.order_no }}</td><td>{{ order.product_name }} × {{ order.quantity }}</td><td>¥{{ order.paid_amount }}</td>
        <td>{{ order.recipient_name }} {{ order.recipient_phone }}<br />{{ order.shipping_address }}</td>
        <td>{{ labels[order.status] }}</td><td>{{ order.express_company || '—' }} {{ order.tracking_no || '' }}</td>
        <td><button v-if="order.status === 'PENDING_SHIPMENT'" class="text-button" :disabled="saving" @click="openShipping(order.id)">发货</button>
          <button v-if="order.status === 'SHIPPED'" class="text-button" :disabled="saving" @click="complete(order.id)">完成订单</button></td>
      </tr></tbody>
    </table></div>
    <div class="pager"><button :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} 页 · 共 {{ total }} 条</span><button :disabled="page * 20 >= total || loading" @click="changePage(page + 1)">下一页</button></div>
    <form v-if="shippingId !== null" class="shipping" @submit.prevent="ship">
      <h2>填写快递信息</h2>
      <label>快递公司<input v-model.trim="form.company" required maxlength="100" /></label>
      <label>快递单号<input v-model.trim="form.tracking" required maxlength="100" /></label>
      <div><button class="button button-primary" :disabled="saving">确认发货</button>
        <button class="button" type="button" @click="shippingId = null">取消</button></div>
    </form>
  </section>
</template>

<style scoped>
.mall-orders { padding: 24px; background: white; border-radius: 12px; }
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; text-align: left; }
th, td { padding: 12px; border-bottom: 1px solid #eee; vertical-align: top; }
.pager { display: flex; gap: 12px; margin-top: 16px; align-items: center; }
.shipping { display: grid; gap: 12px; max-width: 500px; margin-top: 20px; }
.shipping label { display: grid; gap: 6px; }
.shipping input { padding: 8px; border: 1px solid #ccc; border-radius: 6px; }
</style>
