<script setup lang="ts">
/** P05 后台同步模拟退款流水查询页。 */
import { onMounted, reactive, ref } from 'vue'
import { transactionApi } from '../../api/transactions'
import type { RefundListItem, RefundStatus, TransactionPage } from '../../types/transactions'

const rows = ref<RefundListItem[]>([])
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const filters = reactive({ status: '' as RefundStatus | '', start_date: '', end_date: '', page: 1, size: 20 })
const statusLabels: Record<RefundStatus, string> = { PENDING: '处理中', SUCCESS: '成功', FAILED: '失败' }

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const result: TransactionPage<RefundListItem> = await transactionApi.refunds({
      status: filters.status || undefined,
      start_date: filters.start_date || undefined,
      end_date: filters.end_date || undefined,
      page: filters.page,
      size: filters.size,
    })
    rows.value = result.list
    total.value = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '退款流水读取失败'
  } finally {
    loading.value = false
  }
}

function search(): void {
  filters.page = 1
  void load()
}

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}

onMounted(() => void load())
</script>

<template>
  <section class="catalog-page">
    <header class="page-heading-row">
      <div><p class="page-eyebrow">REFUNDS</p><h1>退款流水</h1><p class="page-description">查看顾客自助取消产生的同步模拟退款记录。</p></div>
    </header>
    <div class="filter-card">
      <label>退款状态
        <select v-model="filters.status" :disabled="loading"><option value="">全部状态</option><option value="PENDING">处理中</option><option value="SUCCESS">成功</option><option value="FAILED">失败</option></select>
      </label>
      <label>退款日期起<input v-model="filters.start_date" type="date" :disabled="loading"></label>
      <label>退款日期止<input v-model="filters.end_date" type="date" :disabled="loading"></label>
      <button class="button button-primary" type="button" :disabled="loading" @click="search">查询</button>
    </div>
    <p v-if="errorMessage" class="notice notice-error">{{ errorMessage }}</p>
    <div class="table-card">
      <div class="table-scroll">
        <table class="data-table">
          <thead><tr><th>退款单号</th><th>订单 ID</th><th>退款金额</th><th>状态</th><th>退款原因</th><th>退款完成时间</th></tr></thead>
          <tbody v-if="!loading && rows.length">
            <tr v-for="row in rows" :key="row.id">
              <td><strong>{{ row.refund_no }}</strong></td><td>{{ row.order_id }}</td><td class="price-cell">¥{{ row.amount }}</td>
              <td><span class="status-pill" :class="row.status === 'SUCCESS' ? 'is-enabled' : 'is-disabled'">{{ statusLabels[row.status] }}</span></td>
              <td>{{ row.reason || '未填写' }}</td><td>{{ formatTime(row.refunded_at) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="loading" class="table-state">正在读取退款流水…</div>
        <div v-else-if="!rows.length" class="table-state"><strong>暂无退款记录</strong><span>调整筛选条件后重新查询。</span></div>
      </div>
      <footer class="order-pagination">
        <span>共 {{ total }} 条</span>
        <button class="button button-quiet" type="button" :disabled="filters.page <= 1 || loading" @click="filters.page--; void load()">上一页</button>
        <span>第 {{ filters.page }} 页</span>
        <button class="button button-quiet" type="button" :disabled="filters.page * filters.size >= total || loading" @click="filters.page++; void load()">下一页</button>
        <label>每页<select v-model.number="filters.size" @change="search"><option :value="20">20</option><option :value="50">50</option><option :value="100">100</option></select></label>
      </footer>
    </div>
  </section>
</template>
