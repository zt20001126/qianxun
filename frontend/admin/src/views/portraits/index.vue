<script setup lang="ts">
/** 顾客画像总表：按预订人汇总资料，并展开查看所有已完成订单消费。 */
import { onMounted, onBeforeUnmount, ref, watch, nextTick } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import BookerPortraitEditModal from '../../components/operations/BookerPortraitEditModal.vue'
import { portraitApi } from '../../api/portraits'
import type { BookerPortraitListItem, BookerPartyType } from '../../types/portraits'
import { bookerPartyTypeLabels } from '../../types/portraits'

const rows = ref<BookerPortraitListItem[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const guestName = ref('')
const phone = ref('')
const tableViewport = ref<HTMLDivElement | null>(null)
const bottomScrollbar = ref<HTMLDivElement | null>(null)
const scrollWidth = ref(0)
let resizeObserver: ResizeObserver | undefined

function syncScroll(source: 'table' | 'bottom'): void {
  const from = source === 'table' ? tableViewport.value : bottomScrollbar.value
  const to = source === 'table' ? bottomScrollbar.value : tableViewport.value
  if (from && to && to.scrollLeft !== from.scrollLeft) to.scrollLeft = from.scrollLeft
}

async function measureTable(): Promise<void> {
  await nextTick()
  resizeObserver?.disconnect()
  const viewport = tableViewport.value
  if (!viewport) return
  const measure = () => { scrollWidth.value = viewport.scrollWidth }
  measure()
  resizeObserver = new ResizeObserver(measure)
  resizeObserver.observe(viewport)
  if (viewport.firstElementChild) resizeObserver.observe(viewport.firstElementChild)
}

onBeforeUnmount(() => resizeObserver?.disconnect())
const editingProfile = ref<BookerPortraitListItem | null>(null)
const editOpen = ref(false)
const columns = [
  { key: 'name', label: '预订人姓名' }, { key: 'phone', label: '预订人手机号' },
  { key: 'age', label: '年龄' }, { key: 'party', label: '用户群体' },
  { key: 'region', label: '地区' }, { key: 'member', label: '是否是会员' },
  { key: 'tags', label: '画像标签' }, { key: 'count', label: '累计订单数量' },
  { key: 'amount', label: '累计消费金额' }, { key: 'orders', label: '已完成订单及实付金额' },
  { key: 'notes', label: '用户下单备注汇总' }, { key: 'admin', label: '管理员画像备注' },
] as const
const visibleColumns = ref<string[]>(['phone', 'age', 'party', 'region', 'member', 'admin'])


onMounted(() => { void loadProfiles() })
watch([rows, visibleColumns], () => { void measureTable() }, { deep: true })

async function loadProfiles(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await portraitApi.list({
      guest_name: guestName.value.trim() || undefined,
      phone: phone.value.trim() || undefined,
      page: page.value,
      size,
    })
    rows.value = result.list
    total.value = result.total
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '顾客画像读取失败'
  } finally {
    loading.value = false
  }
}

async function search(): Promise<void> {
  page.value = 1
  await loadProfiles()
}

async function resetFilters(): Promise<void> {
  guestName.value = ''
  phone.value = ''
  await search()
}

async function changePage(nextPage: number): Promise<void> {
  if (nextPage < 1 || nextPage > Math.ceil(total.value / size) || loading.value) return
  page.value = nextPage
  await loadProfiles()
  tableViewport.value?.scrollTo({ left: 0 })
}

function editProfile(profile: BookerPortraitListItem): void {
  editingProfile.value = profile
  editOpen.value = true
}

async function afterProfileSaved(): Promise<void> {
  editOpen.value = false
  successMessage.value = '顾客画像已保存'
  await loadProfiles()
}

function partyTypeLabel(value: BookerPartyType | null): string {
  return value ? bookerPartyTypeLabels[value] : '未填写'
}

function money(value: string): string {
  return `¥${value}`
}

function combinedUserRemarks(profile: BookerPortraitListItem): string {
  return profile.orders
    .filter((order) => order.remark)
    .map((order) => `【${order.order_no}】${order.remark}`)
    .join('\n')
}
</script>

<template>
  <section class="operation-page portrait-page">
    <AdminNoticeDialog v-model:message="successMessage" kind="success" />
    <AdminNoticeDialog v-model:message="errorMessage" />

    <section class="table-card portrait-table-card" aria-label="顾客画像列表">
    <form class="portrait-filter" @submit.prevent="search">
      <label>预订人姓名<input v-model="guestName" maxlength="80" placeholder="输入姓名或部分姓名" /></label>
      <label>预订人手机号<input v-model="phone" maxlength="20" inputmode="tel" placeholder="输入完整或部分手机号" /></label>
      <button class="button button-outline" type="submit" :disabled="loading">查询</button>
      <button class="button button-quiet" type="button" :disabled="loading" @click="resetFilters">重置</button>
    </form>
      <div class="portrait-table-toolbar">
        <span>共 {{ total }} 位预订人</span>
        <div class="portrait-toolbar-actions">
          <details class="portrait-column-picker">
            <summary>显示字段</summary>
            <div class="portrait-column-options">
              <label v-for="column in columns" :key="column.key"><input v-model="visibleColumns" type="checkbox" :value="column.key" />{{ column.label }}</label>
            </div>
          </details>

        </div>
      </div>

      <div v-if="loading" class="table-state">正在读取顾客画像…</div>
      <div v-else-if="rows.length === 0" class="table-state">
        <strong>没有匹配的顾客画像</strong>
        <span>调整姓名或手机号条件后重试。</span>
      </div>
      <div v-else ref="tableViewport" class="portrait-table-scroll" tabindex="0" @scroll="syncScroll('table')" aria-label="可横向滚动的顾客画像表格">
        <table class="data-table portrait-table">
          <thead>
            <tr>
              <th v-if="visibleColumns.includes('name')">预订人姓名</th>
              <th v-if="visibleColumns.includes('phone')">预订人手机号</th>
              <th v-if="visibleColumns.includes('age')">年龄</th>
              <th v-if="visibleColumns.includes('party')">用户群体</th>
              <th v-if="visibleColumns.includes('region')">地区</th>
              <th v-if="visibleColumns.includes('member')">会员</th>
              <th v-if="visibleColumns.includes('tags')">画像标签</th>
              <th v-if="visibleColumns.includes('count')">累计订单数量</th>
              <th v-if="visibleColumns.includes('amount')">累计消费金额</th>
              <th v-if="visibleColumns.includes('orders')">已完成订单及实付金额</th>
              <th v-if="visibleColumns.includes('notes')">用户下单备注汇总</th>
              <th v-if="visibleColumns.includes('admin')">画像管理员备注</th>
              <th class="portrait-action-column">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="profile in rows" :key="profile.id">
              <td v-if="visibleColumns.includes('name')" class="portrait-identity"><strong>{{ profile.guest_name || '未填写' }}</strong><small>画像 #{{ profile.id }}</small></td>
              <td v-if="visibleColumns.includes('phone')" class="portrait-phone">{{ profile.phone || '未填写' }}</td>
              <td v-if="visibleColumns.includes('age')">{{ profile.age == null ? '未填写' : `${profile.age} 岁` }}</td>
              <td v-if="visibleColumns.includes('party')">{{ partyTypeLabel(profile.party_type) }}</td>
              <td v-if="visibleColumns.includes('region')">{{ profile.inferred_region || '未推断' }}</td>
              <td v-if="visibleColumns.includes('member')"><span class="portrait-member-pill" :class="{ 'is-member': profile.member }">{{ profile.member ? '是' : '否' }}</span></td>
              <td v-if="visibleColumns.includes('tags')" class="portrait-tags-cell">
                <template v-if="profile.tags.length"><span v-for="tag in profile.tags" :key="tag" class="portrait-tag-pill">{{ tag }}</span></template>
                <span v-else class="portrait-muted">未添加</span>
              </td>
              <td v-if="visibleColumns.includes('count')" class="portrait-number">{{ profile.completed_order_count }}</td>
              <td v-if="visibleColumns.includes('amount')" class="portrait-money">{{ money(profile.cumulative_paid_amount) }}</td>
              <td v-if="visibleColumns.includes('orders')" class="portrait-orders-cell">
                <div v-if="profile.completed_orders.length" class="portrait-order-list">
                  <article v-for="order in profile.completed_orders" :key="order.order_id" class="portrait-order-card">
                    <div><strong>{{ order.order_no }}</strong><span>{{ order.check_in }} 入住</span><b>{{ money(order.paid_amount) }}</b></div>
                    <small>订单号 #{{ order.order_id }}</small>
                  </article>
                </div>
                <span v-else class="portrait-muted">暂无已完成订单</span>
              </td>
              <td v-if="visibleColumns.includes('notes')" class="portrait-remarks-cell">
                <span v-if="combinedUserRemarks(profile)" class="portrait-combined-user-remark">{{ combinedUserRemarks(profile) }}</span>
                <span v-else class="portrait-muted">暂无备注</span>
              </td>
              <td v-if="visibleColumns.includes('admin')" class="portrait-profile-admin-remark">{{ profile.admin_remark || '暂无备注' }}</td>
              <td class="portrait-row-actions"><button class="button button-quiet" type="button" @click="editProfile(profile)">编辑画像</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="portrait-pagination">
        <span>每页 {{ size }} 位 · {{ total ? (page - 1) * size + 1 : 0 }}–{{ Math.min(page * size, total) }} / {{ total }}</span>
        <div>
          <button class="button button-outline" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <span>第 {{ page }} / {{ Math.max(1, Math.ceil(total / size)) }} 页</span>
          <button class="button button-outline" type="button" :disabled="page * size >= total || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </footer>
      <div v-if="rows.length" ref="bottomScrollbar" class="portrait-bottom-scroll" aria-label="横向滚动画像表格" tabindex="0" @scroll="syncScroll('bottom')">
        <div :style="{ width: `${scrollWidth}px` }" />
      </div>
    </section>

    <BookerPortraitEditModal :open="editOpen" :profile="editingProfile" @close="editOpen = false" @saved="afterProfileSaved" />

  </section>
</template>

<style scoped>
.portrait-page { gap: 16px; }
.portrait-page-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.portrait-page-heading h1 { margin: 0; color: #1f2426; font-family: "Noto Serif SC", "Songti SC", serif; font-size: 23px; }
.portrait-page-heading .page-description { margin: 5px 0 0; }
.portrait-filter { padding: 16px 14px; border-bottom: 1px solid #e7ebeb; display: flex; flex-wrap: wrap; align-items: end; gap: 12px; }
.portrait-filter label { display: grid; min-width: 220px; gap: 5px; color: #536164; font-size: 12px; }
.portrait-filter input { min-height: 38px; padding: 8px 10px; border: 1px solid #dce3e3; border-radius: 7px; color: #202b2d; background: #fff; font: inherit; }
.portrait-table-card { min-width: 0; }
.portrait-table-toolbar { display: flex; min-height: 48px; align-items: center; justify-content: space-between; gap: 14px; padding: 8px 14px; border-bottom: 1px solid #e7ebeb; color: #596467; font-size: 12px; }
.portrait-toolbar-actions { display: flex; align-items: center; gap: 14px; }
.portrait-column-picker { position: relative; }
.portrait-column-picker summary { cursor: pointer; padding: 7px 12px; border: 1px solid #dce3e3; border-radius: 6px; color: #286b7b; background: #fff; }
.portrait-column-options { position: absolute; right: 0; z-index: 5; display: grid; gap: 9px; width: 210px; max-height: 320px; overflow-y: auto; padding: 12px; border: 1px solid #dce3e3; border-radius: 8px; background: #fff; box-shadow: 0 8px 24px #0002; }
.portrait-column-options label { display: flex; align-items: center; gap: 7px; white-space: nowrap; }
.portrait-scroll-controls { display: flex; align-items: center; gap: 7px; }
.portrait-scroll-controls span { margin-right: 5px; color: #778184; }
.portrait-scroll-controls button { width: 30px; height: 28px; border: 1px solid #dce3e3; border-radius: 6px; color: #337a8b; background: #fff; font-size: 17px; line-height: 1; }
.portrait-scroll-controls button:disabled { opacity: .45; }
.portrait-table-scroll { max-width: 100%; overflow-x: auto; scrollbar-width: none; overscroll-behavior-inline: contain; scrollbar-color: #aababc #f0f3f3; scrollbar-width: auto; }
.portrait-table-scroll::-webkit-scrollbar { display: none; }
.portrait-bottom-scroll { overflow-x: scroll; scrollbar-color: #aababc #f0f3f3; }
.portrait-bottom-scroll > div { height: 1px; }
.portrait-table { width: 100%; table-layout: auto; }
.portrait-table th { position: sticky; top: 0; z-index: 1; white-space: nowrap; background: #f7f9f9; }
.portrait-table td { vertical-align: top; white-space: normal; }
.portrait-identity { min-width: 130px; }
.portrait-identity strong, .portrait-identity small { display: block; }
.portrait-identity small { margin-top: 4px; color: #899295; font-size: 10px; }
.portrait-phone { min-width: 135px; color: #202b2d; font-variant-numeric: tabular-nums; white-space: nowrap !important; }
.portrait-member-pill, .portrait-tag-pill { display: inline-flex; align-items: center; margin: 2px 4px 2px 0; padding: 3px 8px; border-radius: 999px; color: #647174; background: #eef1f1; font-size: 11px; white-space: nowrap; }
.portrait-member-pill.is-member { color: #276a52; background: #e8f4ee; }
.portrait-tag-pill { color: #286b7b; background: #edf5f7; }
.portrait-tags-cell { min-width: 180px; max-width: 260px; }
.portrait-number { text-align: center; font-variant-numeric: tabular-nums; }
.portrait-money { color: #b54132; font-weight: 650; white-space: nowrap !important; font-variant-numeric: tabular-nums; }
.portrait-orders-cell { min-width: 320px; max-width: 420px; }
.portrait-order-list { display: grid; gap: 6px; }
.portrait-order-card { padding: 7px 8px; border: 1px solid #e6ebeb; border-radius: 7px; background: #fbfcfc; }
.portrait-order-card > div { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 12px; }
.portrait-order-card strong { color: #263234; font-size: 11px; }
.portrait-order-card span, .portrait-order-card small { color: #788386; font-size: 10px; }
.portrait-order-card b { margin-left: auto; color: #b54132; font-size: 11px; }
.portrait-order-card small { display: block; margin-top: 4px; }
.portrait-remarks-cell { min-width: 260px; max-width: 330px; }
.portrait-combined-user-remark { display: block; color: #485356; font-size: 11px; line-height: 1.5; white-space: pre-wrap; overflow-wrap: anywhere; }
.portrait-profile-admin-remark { min-width: 260px; max-width: 330px; white-space: pre-wrap !important; overflow-wrap: anywhere; }
.portrait-remark-list { display: grid; gap: 6px; }
.portrait-remark-list p { display: grid; gap: 3px; margin: 0; padding: 6px 8px; border-radius: 6px; color: #485356; background: #f7f8f8; font-size: 11px; line-height: 1.5; overflow-wrap: anywhere; }
.portrait-remark-list strong { color: #778184; font-size: 10px; font-weight: 500; }
.portrait-muted { color: #929b9d; font-size: 11px; }
.portrait-row-actions { min-width: 100px; white-space: nowrap !important; }
.portrait-pagination { display: flex; min-height: 56px; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 14px; border-top: 1px solid #e7ebeb; color: #677275; font-size: 12px; }
.portrait-pagination > div { display: flex; align-items: center; gap: 10px; }
@media (max-width: 700px) {
  .portrait-page-heading { align-items: flex-start; flex-direction: column; }
  .portrait-filter { align-items: stretch; flex-direction: column; }
  .portrait-filter label { min-width: 0; }
  .portrait-table-toolbar, .portrait-pagination { align-items: flex-start; flex-direction: column; }
  .portrait-pagination > div { align-self: stretch; justify-content: space-between; }
}
</style>
