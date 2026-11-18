<script setup lang="ts">
/** C01/C02 固定七渠道管理。停用不影响历史订单，后续仅限制新订单选用。 */
import { onMounted, ref } from 'vue'
import { channelApi } from '../../api/channels'
import type { Channel } from '../../types/catalog'

const channels = ref<Channel[]>([])
const loading = ref(false)
const busyIds = ref<number[]>([])
const errorMessage = ref('')
const successMessage = ref('')

onMounted(() => { void loadChannels() })

async function loadChannels(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    channels.value = await channelApi.list()
  } catch (error) {
    errorMessage.value = messageOf(error, '渠道资料读取失败')
  } finally {
    loading.value = false
  }
}

async function toggleChannel(channel: Channel): Promise<void> {
  busyIds.value = [...busyIds.value, channel.id]
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await channelApi.setEnabled(channel.id, !channel.enabled)
    successMessage.value = channel.enabled ? `${channel.name} 已停用` : `${channel.name} 已启用`
    await loadChannels()
  } catch (error) {
    errorMessage.value = messageOf(error, '渠道状态更新失败')
  } finally {
    busyIds.value = busyIds.value.filter((id) => id !== channel.id)
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
        <p class="page-eyebrow">CHANNELS</p>
        <h1>渠道管理</h1>
        <p class="page-description">七个固定订单来源。停用仅影响新订单，不更改历史订单归属。</p>
      </div>
    </div>
    <p v-if="successMessage" class="notice notice-success" role="status">{{ successMessage }}</p>
    <p v-if="errorMessage" class="notice notice-error" role="alert">{{ errorMessage }}</p>

    <div class="table-card">
      <div v-if="loading" class="table-state">正在读取渠道资料…</div>
      <div v-else-if="channels.length === 0" class="table-state">渠道初始化数据暂不可用，请重新加载。</div>
      <div v-else class="table-scroll">
        <table class="data-table">
          <thead><tr><th>渠道名称</th><th>当前状态</th><th>说明</th><th class="actions-column">操作</th></tr></thead>
          <tbody>
            <tr v-for="channel in channels" :key="channel.id">
              <td><strong>{{ channel.name }}</strong></td>
              <td><span class="status-pill" :class="channel.enabled ? 'is-enabled' : 'is-disabled'">{{ channel.enabled ? '已启用' : '已停用' }}</span></td>
              <td>{{ channel.enabled ? '可用于新订单' : '保留历史记录，不可用于新订单' }}</td>
              <td class="row-actions">
                <button class="button button-quiet" type="button" :disabled="busyIds.includes(channel.id)" @click="toggleChannel(channel)">
                  {{ busyIds.includes(channel.id) ? '处理中…' : channel.enabled ? '停用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
