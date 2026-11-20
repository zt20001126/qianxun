<script setup lang="ts">
/** E01–E06 经营支出页面；支出创建后只允许查询，不开放编辑或删除。 */
import { computed, onMounted, reactive, ref } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { expenseApi } from '../../api/expenses'
import type { Expense, ExpensePage, ExpenseSummary } from '../../types/operations'
import '../../styles/operations.css'

const month = ref(shanghaiMonth())
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const result = ref<ExpensePage | null>(null)
const summary = ref<ExpenseSummary | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')
const dialogOpen = ref(false)
const formError = ref('')
const form = reactive({ content: '', amount: '', date: shanghaiDate() })

const pageCount = computed(() => Math.max(1, Math.ceil((result.value?.total || 0) / size.value)))
const dateRange = computed(() => monthRange(month.value))

async function load(): Promise<void> {
  if (!month.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const [pageResult, summaryResult] = await Promise.all([
      expenseApi.list({
        start_date: dateRange.value.start,
        end_date: dateRange.value.end,
        keyword: keyword.value.trim() || undefined,
        page: page.value,
        size: size.value,
      }),
      expenseApi.summary(dateRange.value.start, dateRange.value.end),
    ])
    result.value = pageResult
    summary.value = summaryResult
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '经营支出加载失败'
  } finally {
    loading.value = false
  }
}

function showCreateDialog(): void {
  form.content = ''
  form.amount = ''
  form.date = shanghaiDate()
  formError.value = ''
  dialogOpen.value = true
}

async function createExpense(): Promise<void> {
  formError.value = ''
  if (!form.content.trim()) {
    formError.value = '请填写支出内容。'
    return
  }
  if (!/^\d{1,10}(?:\.\d{1,2})?$/.test(form.amount.trim()) || !/[1-9]/.test(form.amount)) {
    formError.value = '支出金额需大于 0，最多保留两位小数。'
    return
  }
  if (!form.date) {
    formError.value = '请选择实际支出日期。'
    return
  }
  saving.value = true
  try {
    const created: Expense = await expenseApi.create({
      content: form.content.trim(),
      amount: form.amount.trim(),
      date: form.date,
    })
    dialogOpen.value = false
    if (created.date.slice(0, 7) === month.value) page.value = 1
    await load()
    success.value = '经营支出已新增。'
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : '新增经营支出失败'
  } finally {
    saving.value = false
  }
}

function changePage(nextPage: number): void {
  if (nextPage < 1 || nextPage > pageCount.value || loading.value) return
  page.value = nextPage
  void load()
}

function monthRange(value: string): { start: string; end: string } {
  const [year, monthValue] = value.split('-').map(Number)
  const lastDay = new Date(Date.UTC(year, monthValue, 0)).getUTCDate()
  return { start: value + '-01', end: value + '-' + String(lastDay).padStart(2, '0') }
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

function formatDateTime(value: string): string {
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(() => void load())
</script>

<template>
  <section class="operation-page">
    <div class="page-heading-row">
      <div>
        <p class="page-eyebrow">OPERATING EXPENSES</p>
        <h1>经营支出</h1>
        <p class="page-description">记录实际支出日期与金额；提交后记录不可修改或删除。</p>
      </div>
      <button class="button button-primary" type="button" @click="showCreateDialog">新增支出</button>
    </div>

    <AdminNoticeDialog v-model:message="success" kind="success" />
    <AdminNoticeDialog v-model:message="error" />
    <AdminNoticeDialog v-model:message="formError" />

    <div class="operation-toolbar">
      <div class="filter-card">
        <label>统计月份<input v-model="month" type="month" @change="page = 1; load()" /></label>
        <label class="filter-name">支出内容<input v-model="keyword" type="search" placeholder="按内容搜索" @keyup.enter="page = 1; load()" /></label>
        <button class="button button-primary" type="button" @click="page = 1; load()" :disabled="loading">查询</button>
      </div>
    </div>

    <section v-if="summary" class="expense-summary-row">
      <article class="operation-kpi is-highlight">
        <small>{{ month }} 实际经营支出</small>
        <strong class="money-value">¥ {{ summary.total_amount }}</strong>
      </article>
      <article class="operation-kpi">
        <small>本月支出笔数</small>
        <strong>{{ summary.count }} 笔</strong>
      </article>
    </section>

    <section class="table-card">
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>支出日期</th>
              <th>支出内容</th>
              <th>支出金额</th>
              <th>创建时间</th>
              <th>更新时间</th>
            </tr>
          </thead>
          <tbody v-if="result?.list.length">
            <tr v-for="expense in result.list" :key="expense.id">
              <td>{{ expense.date }}</td>
              <td>{{ expense.content }}</td>
              <td class="price-cell">¥ {{ expense.amount }}</td>
              <td>{{ formatDateTime(expense.created_at) }}</td>
              <td>{{ formatDateTime(expense.updated_at) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="loading" class="table-state">正在加载支出记录…</div>
        <div v-else-if="!result?.list.length" class="table-state">
          <strong>没有支出记录</strong>
          <span>可以新增一笔支出，或调整月份和关键词。</span>
        </div>
      </div>
      <div class="member-pagination">
        <span>共 {{ result?.total || 0 }} 笔</span>
        <div>
          <label>每页
            <select v-model.number="size" @change="page = 1; load()">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
          </label>
          <button class="button button-outline" type="button" @click="changePage(page - 1)" :disabled="page <= 1 || loading">上一页</button>
          <span>{{ page }} / {{ pageCount }}</span>
          <button class="button button-outline" type="button" @click="changePage(page + 1)" :disabled="page >= pageCount || loading">下一页</button>
        </div>
      </div>
    </section>

    <div v-if="dialogOpen" class="dialog-backdrop" @click.self="dialogOpen = false">
      <section class="form-dialog compact-dialog" role="dialog" aria-modal="true" aria-labelledby="expense-dialog-title">
        <header class="dialog-header">
          <div><p class="page-eyebrow">NEW EXPENSE</p><h2 id="expense-dialog-title">新增经营支出</h2></div>
          <button class="icon-button" type="button" aria-label="关闭" @click="dialogOpen = false">×</button>
        </header>
        <form class="expense-form-grid" @submit.prevent="createExpense">
          <label>支出内容
            <input v-model="form.content" maxlength="300" required placeholder="例如：采购洗漱用品" />
          </label>
          <label>支出金额（元）
            <input v-model="form.amount" class="money-value" inputmode="decimal" required placeholder="0.00" />
          </label>
          <label>实际支出日期
            <input v-model="form.date" type="date" required />
          </label>
          <div class="dialog-actions">
            <button class="button button-outline" type="button" @click="dialogOpen = false">返回</button>
            <button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '提交中…' : '确认新增' }}</button>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>
