<script setup lang="ts">
/** 商城商品管理页面：维护商品资料、展示状态，并以弹窗完成新建和编辑。 */
import { computed, onMounted, reactive, ref } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { MALL_PAGE_SIZE, mallApi, type MallProduct, type MallProductInput } from '../../api/mall'

interface ProductForm {
  name: string
  description: string
  price: string
  images: string[]
}

const AMOUNT_PATTERN = /^\d{1,10}(?:\.\d{1,2})?$/

const products = ref<MallProduct[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const imageInput = ref<HTMLInputElement | null>(null)
const errorMessage = ref('')
const successMessage = ref('')
const editingProduct = ref<MallProduct | null>(null)
const dialogOpen = ref(false)
const busyProductIds = ref<number[]>([])
const form = reactive<ProductForm>(emptyForm())

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / MALL_PAGE_SIZE)))

onMounted(() => { void loadProducts() })

function emptyForm(): ProductForm {
  return { name: '', description: '', price: '', images: [] }
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
    images: [...product.images],
  })
  errorMessage.value = ''
  dialogOpen.value = true
}

function closeDialog(): void {
  if (!saving.value && !uploading.value) dialogOpen.value = false
}

async function uploadImages(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return
  uploading.value = true
  errorMessage.value = ''
  try {
    for (const file of files) form.images.push(await mallApi.uploadImage(file))
  } catch (error) {
    errorMessage.value = messageOf(error, '图片上传失败')
  } finally {
    uploading.value = false
  }
}

function moveImage(index: number, offset: number): void {
  const target = index + offset
  if (target < 0 || target >= form.images.length) return
  const [image] = form.images.splice(index, 1)
  form.images.splice(target, 0, image)
}

function removeImage(index: number): void {
  form.images.splice(index, 1)
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
    images: [...form.images],
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
              <div class="form-span image-field">
                <label>商品图片</label>
                <input ref="imageInput" class="image-file-input" type="file" accept="image/jpeg,image/png,image/gif,image/bmp,image/webp" multiple @change="uploadImages" />
                <div class="image-toolbar">
                  <button class="button button-outline" type="button" :disabled="uploading || saving" @click="imageInput?.click()">
                    {{ uploading ? '上传中…' : '选择并上传图片' }}
                  </button>
                  <small>支持 JPG、PNG、GIF、BMP、WebP；可用上下移调整顺序，第一张为封面。</small>
                </div>
                <ol v-if="form.images.length" class="product-image-list">
                  <li v-for="(image, index) in form.images" :key="image + index">
                    <img :src="image" :alt="`商品图片 ${index + 1}`" />
                    <span>{{ index === 0 ? '封面' : `图片 ${index + 1}` }}</span>
                    <button class="button button-quiet" type="button" :disabled="index === 0 || uploading" @click="moveImage(index, -1)">上移</button>
                    <button class="button button-quiet" type="button" :disabled="index === form.images.length - 1 || uploading" @click="moveImage(index, 1)">下移</button>
                    <button class="button button-quiet" type="button" :disabled="uploading" @click="removeImage(index)">移除</button>
                  </li>
                </ol>
                <small v-else>尚未添加商品图片。</small>
              </div>
            </div>
            <footer class="dialog-actions">
              <button class="button button-outline" type="button" :disabled="saving || uploading" @click="closeDialog">取消</button>
              <button class="button button-primary" type="submit" :disabled="saving || uploading">{{ saving ? '保存中…' : '保存商品' }}</button>
            </footer>
          </form>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.mall-products-table { min-width: 620px; }
.image-file-input { display: none; }
.image-field { display: grid; gap: 10px; }
.image-field > label { font-weight: 600; }
.image-toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.product-image-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 10px; margin: 0; padding: 0; list-style: none; }
.product-image-list li { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; padding: 8px; border: 1px solid #e7e0da; border-radius: 10px; }
.product-image-list img { width: 56px; height: 56px; object-fit: cover; border-radius: 6px; }
.product-image-list span { flex: 1; min-width: 42px; }
</style>
