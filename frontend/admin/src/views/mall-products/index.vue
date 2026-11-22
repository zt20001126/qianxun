<script setup lang="ts">
/** 管理商城商品资料、图片地址及上下架状态。 */
import { onMounted, reactive, ref } from 'vue'
import { mallApi, type MallProduct } from '../../api/mall'

const products = ref<MallProduct[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const editing = ref<number | null>(null)
const showForm = ref(false)
const form = reactive({ name: '', description: '', price: '', imagesText: '' })

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try { const result = await mallApi.list(page.value); products.value = result.list; total.value = result.total }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '加载商品失败' }
  finally { loading.value = false }
}
function edit(product?: MallProduct): void {
  editing.value = product?.id ?? null
  form.name = product?.name ?? ''
  form.description = product?.description ?? ''
  form.price = product?.price ?? ''
  form.imagesText = product?.images.join('\n') ?? ''
  showForm.value = true
}
async function save(): Promise<void> {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try {
    const input = { name: form.name, description: form.description, price: form.price,
      images: form.imagesText.split('\n').map((item) => item.trim()).filter(Boolean) }
    if (editing.value === null) await mallApi.create(input)
    else await mallApi.update(editing.value, input)
    showForm.value = false
    await load()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '保存商品失败' }
  finally { saving.value = false }
}
async function toggle(product: MallProduct): Promise<void> {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try {
    await mallApi.setStatus(product.id, product.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF')
    await load()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '修改状态失败' }
  finally { saving.value = false }
}
onMounted(load)
async function changePage(next: number): Promise<void> { page.value = next; await load() }
</script>

<template>
  <section class="mall-page">
    <div class="mall-heading"><h1>商品管理</h1><button class="button button-primary" @click="edit()">新建商品</button></div>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading">正在加载商品…</p>
    <p v-else-if="!products.length">暂无商品</p>
    <table v-else class="mall-table">
      <thead><tr><th>商品名称</th><th>价格</th><th>状态</th><th>操作</th></tr></thead>
      <tbody><tr v-for="product in products" :key="product.id">
        <td>{{ product.name }}</td><td>¥{{ product.price }}</td>
        <td>{{ product.status === 'ON_SHELF' ? '已上架' : '已下架' }}</td>
        <td><button class="text-button" @click="edit(product)">修改</button>
          <button class="text-button" :disabled="saving" @click="toggle(product)">{{ product.status === 'ON_SHELF' ? '下架' : '上架' }}</button></td>
      </tr></tbody>
    </table>
    <div class="mall-pager"><button :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page }} 页 · 共 {{ total }} 条</span><button :disabled="page * 20 >= total || loading" @click="changePage(page + 1)">下一页</button></div>
    <form v-if="showForm" class="mall-form" @submit.prevent="save">
      <h2>{{ editing === null ? '新建商品' : '修改商品' }}</h2>
      <label>商品名称<input v-model.trim="form.name" required maxlength="120" /></label>
      <label>商品描述<textarea v-model.trim="form.description" required maxlength="10000" rows="4" /></label>
      <label>价格（元）<input v-model="form.price" required type="number" min="0" step="0.01" /></label>
      <label>图片地址（每行一个）<textarea v-model="form.imagesText" rows="3" /></label>
      <div><button class="button button-primary" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
        <button class="button" type="button" @click="showForm = false">取消</button></div>
    </form>
  </section>
</template>

<style scoped>
.mall-page { background: white; border-radius: 12px; padding: 24px; }
.mall-heading { display: flex; justify-content: space-between; align-items: center; }
.mall-table { width: 100%; border-collapse: collapse; text-align: left; }
.mall-table th, .mall-table td { padding: 14px; border-bottom: 1px solid #eee; }
.mall-form { display: grid; gap: 12px; max-width: 600px; margin-top: 24px; }
.mall-form label { display: grid; gap: 6px; }
.mall-form input, .mall-form textarea { padding: 8px; border: 1px solid #ccc; border-radius: 6px; }
.mall-pager { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
</style>
