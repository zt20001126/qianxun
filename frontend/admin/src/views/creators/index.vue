<script setup lang="ts">
/** D01-D05 达人维护，筛选与编辑均沿用固定渠道资料。 */
import { onMounted, reactive, ref } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { channelApi } from '../../api/channels'
import { creatorApi } from '../../api/creators'
import type { Channel, Creator, CreatorSaveRequest } from '../../types/catalog'

interface CreatorForm {
  name: string
  channel_id: string
}

const creators = ref<Creator[]>([])
const channels = ref<Channel[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const dialogOpen = ref(false)
const editingCreator = ref<Creator | null>(null)
const busyCreatorIds = ref<number[]>([])
const filters = reactive({ channel_id: '', enabled: '', name: '' })
const form = reactive<CreatorForm>({ name: '', channel_id: '' })

onMounted(async () => {
  await Promise.all([loadChannels(), loadCreators()])
})

async function loadChannels(): Promise<void> {
  try {
    channels.value = await channelApi.list()
  } catch (error) {
    errorMessage.value = messageOf(error, '渠道资料读取失败')
  }
}

async function loadCreators(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    creators.value = await creatorApi.list({
      channel_id: filters.channel_id ? Number(filters.channel_id) : undefined,
      enabled: filters.enabled === '' ? undefined : filters.enabled === 'true',
      name: filters.name.trim() || undefined,
    })
  } catch (error) {
    errorMessage.value = messageOf(error, '达人资料读取失败')
  } finally {
    loading.value = false
  }
}

function beginAdd(): void {
  editingCreator.value = null
  form.name = ''
  form.channel_id = String(channels.value[0]?.id || '')
  errorMessage.value = ''
  successMessage.value = ''
  dialogOpen.value = true
}

function beginEdit(creator: Creator): void {
  editingCreator.value = creator
  form.name = creator.name
  form.channel_id = String(creator.channel_id)
  errorMessage.value = ''
  successMessage.value = ''
  dialogOpen.value = true
}

function closeDialog(): void {
  if (!saving.value) dialogOpen.value = false
}

async function saveCreator(): Promise<void> {
  if (!form.name.trim() || !form.channel_id) {
    errorMessage.value = '请填写达人名称并选择所属渠道'
    return
  }
  const request: CreatorSaveRequest = { name: form.name.trim(), channel_id: Number(form.channel_id) }
  saving.value = true
  errorMessage.value = ''
  try {
    if (editingCreator.value) await creatorApi.update(editingCreator.value.id, request)
    else await creatorApi.create(request)
    dialogOpen.value = false
    successMessage.value = editingCreator.value ? '达人资料已更新' : '达人已添加'
    await loadCreators()
  } catch (error) {
    errorMessage.value = messageOf(error, '达人保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleCreator(creator: Creator): Promise<void> {
  busyCreatorIds.value = [...busyCreatorIds.value, creator.id]
  errorMessage.value = ''
  try {
    await creatorApi.setEnabled(creator.id, !creator.enabled)
    successMessage.value = creator.enabled ? `${creator.name} 已停用` : `${creator.name} 已启用`
    await loadCreators()
  } catch (error) {
    errorMessage.value = messageOf(error, '达人状态更新失败')
  } finally {
    busyCreatorIds.value = busyCreatorIds.value.filter((id) => id !== creator.id)
  }
}

function channelName(channelId: number): string {
  return channels.value.find((channel) => channel.id === channelId)?.name || '未知渠道'
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
</script>

<template>
  <section class="catalog-page">
    <div class="page-heading-row">
      <div>
        <p class="page-eyebrow">CREATORS</p>
        <h1>达人管理</h1>
        <p class="page-description">每位达人归属一个渠道。停用后保留资料和历史关联。</p>
      </div>
      <button class="button button-primary" type="button" :disabled="channels.length === 0" @click="beginAdd">新增达人</button>
    </div>

    <form class="filter-card" @submit.prevent="loadCreators">
      <label>所属渠道
        <select v-model="filters.channel_id"><option value="">全部渠道</option><option v-for="channel in channels" :key="channel.id" :value="String(channel.id)">{{ channel.name }}{{ channel.enabled ? '' : '（停用）' }}</option></select>
      </label>
      <label>启用状态
        <select v-model="filters.enabled"><option value="">全部状态</option><option value="true">已启用</option><option value="false">已停用</option></select>
      </label>
      <label class="filter-name">达人名称
        <input v-model="filters.name" maxlength="80" placeholder="搜索达人名称" />
      </label>
      <button class="button button-outline" type="submit" :disabled="loading">查询</button>
    </form>

    <AdminNoticeDialog v-model:message="successMessage" kind="success" />
    <AdminNoticeDialog v-model:message="errorMessage" />

    <div class="table-card">
      <div v-if="loading" class="table-state">正在读取达人资料…</div>
      <div v-else-if="creators.length === 0" class="table-state"><strong>没有匹配的达人</strong><span>可以新增达人，或调整筛选条件。</span></div>
      <div v-else class="table-scroll">
        <table class="data-table">
          <thead><tr><th>达人名称</th><th>所属渠道</th><th>当前状态</th><th class="actions-column">操作</th></tr></thead>
          <tbody>
            <tr v-for="creator in creators" :key="creator.id">
              <td><strong>{{ creator.name }}</strong></td>
              <td>{{ channelName(creator.channel_id) }}</td>
              <td><span class="status-pill" :class="creator.enabled ? 'is-enabled' : 'is-disabled'">{{ creator.enabled ? '已启用' : '已停用' }}</span></td>
              <td class="row-actions">
                <button class="button button-quiet" type="button" @click="beginEdit(creator)">编辑</button>
                <button class="button button-quiet" type="button" :disabled="busyCreatorIds.includes(creator.id)" @click="toggleCreator(creator)">
                  {{ busyCreatorIds.includes(creator.id) ? '处理中…' : creator.enabled ? '停用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="dialogOpen" class="dialog-backdrop" @click.self="closeDialog">
      <section class="form-dialog compact-dialog" role="dialog" aria-modal="true" :aria-label="editingCreator ? '编辑达人' : '新增达人'">
        <header class="dialog-header">
          <div><p class="page-eyebrow">CREATOR PROFILE</p><h2>{{ editingCreator ? '编辑达人' : '新增达人' }}</h2></div>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeDialog">×</button>
        </header>
        <form class="catalog-form" @submit.prevent="saveCreator">
          <label>达人名称
            <input v-model="form.name" maxlength="80" placeholder="请输入达人名称" required />
          </label>
          <label>所属渠道
            <select v-model="form.channel_id" required>
              <option value="" disabled>选择渠道</option>
              <option v-for="channel in channels" :key="channel.id" :value="String(channel.id)">{{ channel.name }}{{ channel.enabled ? '' : '（停用）' }}</option>
            </select>
          </label>
          <footer class="dialog-actions">
            <button class="button button-outline" type="button" :disabled="saving" @click="closeDialog">取消</button>
            <button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存达人' }}</button>
          </footer>
        </form>
      </section>
    </div>
  </section>
</template>
