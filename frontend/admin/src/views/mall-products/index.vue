<script setup lang="ts">
/** 商城商品管理页面：维护商品资料、展示状态，并以弹窗完成新建和编辑。 */
import { computed, onMounted, reactive, ref } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { MALL_PAGE_SIZE, mallApi, type MallProduct, type MallProductInput } from '../../api/mall'

interface ProductForm {
  name: string
  description: string
  price: string
  imagesText: string
}

const AMOUNT_PATTERN = /^\d{1,10}(?:\.\d{1,2})?$/

const products = ref<MallProduct[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const editingProduct = ref<MallProduct | null>(null)
const dialogOpen = ref(false)
const busyProductIds = ref<number[]>([])
const form = reactive<ProductForm>(emptyForm())

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / MALL_PAGE_SIZE)))

onMounted(() => { void loadProducts() })

function emptyForm(): ProductForm {
  return { name: '', description: '', price: '', imagesText: '' }
}

async function loadProducts(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await mallApi.list(page.value, MALL_PAGE_SIZE)
    products.value = result.list
    total.value = result.total
  } catch (error) {
    errorMessage.value = messageOf(error, '商品列表读取失败')
  } finally {
    loading.value = false
  }
}

function beginAdd(): void {
  editingProduct.value = null
  Object.assign(form, emptyForm())
  errorMessage.value = ''
  dialogOpen.value = true
}

function beginEdit(product: MallProduct): void {
  editingProduct.value = product
  Object.assign(form, {
    name: product.name,
    description: product.description,
    price: product.price,
    imagesText: product.images.join('\n'),
  })
  errorMessage.value = ''
  dialogOpen.value = true
}

function closeDialog(): void {
  if (!saving.value) dialogOpen.value = false
}

async function saveProduct(): Promise<void> {
  const name = form.name.trim()
  const description = form.description.trim()
  const price = form.price.trim()
  if (!name || !description) {
    errorMessage.value = '请填写商品名称和商品描述'
    return
  }
  if (!AMOUNT_PATTERN.test(price)) {
    errorMessage.value = '商品价格须为不超过两位小数的非负金额'
    return
  }

  const request: MallProductInput = {
    name,
    description,
    price,
    images: splitLines(form.imagesText),
  }
  const productId = editingProduct.value?.id
  const isCreating = productId === undefined
  saving.value = true
  errorMessage.value = ''
  try {
    if (isCreating) {
      await mallApi.create(request)
      page.value = 1
    } else {
      await mallApi.update(productId, request)
    }
    dialogOpen.value = false
    successMessage.value = isCreating ? '商品已创建，当前为下架状态' : '商品资料已更新'
    await loadProducts()
  } catch (error) {
    errorMessage.value = messageOf(error, '商品保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(product: MallProduct): Promise<void> {
  if (busyProductIds.value.includes(product.id)) return
  busyProductIds.value = [...busyProductIds.value, product.id]
  errorMessage.value = ''
  try {
    const nextStatus = product.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
    await mallApi.setStatus(product.id, nextStatus)
    successMessage.value = nextStatus === 'ON_SHELF' ? '商品已上架' : '商品已下架'
    await loadProducts()
  } catch (error) {
    errorMessage.value = messageOf(error, '商品状态修改失败')
  } finally {
    busyProductIds.value = busyProductIds.value.filter((id) => id !== product.id)
  }
}

function changePage(nextPage: number): void {
  if (nextPage < 1 || nextPage > totalPages.value || loading.value) return
  page.value = nextPage
  void loadProducts()
}

function splitLines(value: string): string[] {
  return value.split(/\r?\n/).map((item) => item.trim()).filter(Boolean)
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
</script>

<template>
  <section class="catalog-page">
    <div class="page-heading-row">
      <div>
        <p class="page-eyebrow">MALL PRODUCTS</p>
        <h1>商品管理</h1>
        <p class="page-description">维护民宿自营商品资料与上下架状态；无需在本页额外筛选或查询。</p>
      </div>
      <button class="button button-primary" type="button" @click="beginAdd">新建商品</button>
    </div>

    <div class="summary-strip" aria-label="商品列表统计">
      <span>商品总数 <strong>{{ total }}</strong></span>
      <span>每页展示 <strong>{{ MALL_PAGE_SIZE }}</strong> 条</span>
    </div>

    <AdminNoticeDialog v-model:message="errorMessage" />
    <AdminNoticeDialog v-model:message="successMessage" kind="success" />

    <section class="table-card">
      <div v-if="loading" class="table-state">正在读取商品资料…</div>
      <div v-else-if="products.length === 0" class="table-state">
        <strong>还没有商品</strong>
        <span>点击“新建商品”添加民宿自营商品。</span>
      </div>
      <div v-else class="table-scroll">
        <table class="data-table mall-products-table">
          <thead>
            <tr><th>商品名称</th><th>价格</th><th>状态</th><th class="actions-column">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="product in products" :key="product.id">
              <td><strong>{{ product.name }}</strong></td>
              <td class="price-cell">¥{{ product.price }}</td>
              <td>
                <span class="status-pill" :class="product.status === 'ON_SHELF' ? 'is-enabled' : 'is-disabled'">
                  {{ product.status === 'ON_SHELF' ? '已上架' : '已下架' }}
                </span>
              </td>
              <td class="row-actions">
                <button class="button button-quiet" type="button" @click="beginEdit(product)">编辑</button>
                <button
                  class="button button-quiet"
                  type="button"
                  :disabled="busyProductIds.includes(product.id)"
                  @click="toggleStatus(product)"
                >
                  {{ busyProductIds.includes(product.id) ? '处理中…' : product.status === 'ON_SHELF' ? '下架' : '上架' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer v-if="total > 0" class="member-pagination">
        <span>共 {{ total }} 件商品</span>
        <div>
          <button class="button button-outline" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
          <span>第 {{ page }} / {{ totalPages }} 页</span>
          <button class="button button-outline" type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
        </div>
      </footer>
    </section>

    <Teleport to="body">
      <div v-if="dialogOpen" class="dialog-backdrop" @click.self="closeDialog">
        <section
          class="form-dialog"
          role="dialog"
          aria-modal="true"
          :aria-label="editingProduct ? '编辑商品' : '新建商品'"
          @keydown.esc="closeDialog"
        >
          <header class="dialog-header">
            <div>
              <p class="page-eyebrow">PRODUCT PROFILE</p>
              <h2>{{ editingProduct ? '编辑商品' : '新建商品' }}</h2>
            </div>
            <button class="icon-button" type="button" aria-label="关闭" :disabled="saving" @click="closeDialog">×</button>
          </header>
          <form class="catalog-form" @submit.prevent="saveProduct">
            <div class="form-grid">
              <label class="form-span">商品名称
                <input v-model="form.name" maxlength="120" placeholder="请输入商品名称" required />
              </label>
              <label>价格（元）
                <input v-model="form.price" class="money-value" inputmode="decimal" maxlength="13" placeholder="如 199.00" required />
              </label>
              <label class="form-span">商品描述
                <textarea v-model="form.description" rows="4" maxlength="10000" placeholder="请填写商品介绍" required />
              </label>
              <label class="form-span">图片地址（每行一个）
                <textarea v-model="form.imagesText" rows="4" placeholder="请输入已有图片 URL；每行一条" />
                <small>当前仅支持填写已存在的图片地址，暂不提供图片上传。</small>
              </label>
            </div>
            <footer class="dialog-actions">
              <button class="button button-outline" type="button" :disabled="saving" @click="closeDialog">取消</button>
              <button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存商品' }}</button>
            </footer>
          </form>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.mall-products-table { min-width: 620px; }
</style>
