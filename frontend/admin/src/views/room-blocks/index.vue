<script setup lang="ts">
/** R10-R13 管理员封房记录维护与日期冲突提示。 */
import { onMounted, reactive, ref } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { useRoute, useRouter } from 'vue-router'
import { roomBlockApi } from '../../api/room-blocks'
import { roomApi } from '../../api/rooms'
import type { Room } from '../../types/catalog'
import type { RoomBlock, RoomBlockSaveRequest } from '../../api/room-blocks'

interface RoomBlockForm {
  room_id: number | null
  start_date: string
  end_date: string
  reason: string
}

const blocks = ref<RoomBlock[]>([])
const route = useRoute()
const router = useRouter()
const rooms = ref<Room[]>([])
const roomFilter = ref<number | ''>('')
const monthFilter = ref('')
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const dialogOpen = ref(false)
const editingBlock = ref<RoomBlock | null>(null)
const form = reactive<RoomBlockForm>(emptyForm())

onMounted(async () => {
  const action = route.query.calendar_action
  const roomId = Number(route.query.room_id)
  if (action === 'block' && Number.isSafeInteger(roomId) && roomId > 0) roomFilter.value = roomId
  if (action === 'block' && typeof route.query.month === 'string' && /^\d{4}-(0[1-9]|1[0-2])$/.test(route.query.month)) {
    monthFilter.value = route.query.month
  }
  await Promise.all([loadRooms(), loadBlocks()])
  if (action === 'block') await router.replace({ path: route.path, query: {} })
})

function emptyForm(): RoomBlockForm {
  return { room_id: null, start_date: '', end_date: '', reason: '' }
}

function roomName(roomId: number): string {
  const room = rooms.value.find((item) => item.id === roomId)
  return room ? `${room.number} · ${room.name}` : `房间 ${roomId}`
}

async function loadRooms(): Promise<void> {
  try {
    rooms.value = await roomApi.list()
  } catch (error) {
    errorMessage.value = messageOf(error, '客房列表读取失败')
  }
}

async function loadBlocks(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    blocks.value = await roomBlockApi.list(roomFilter.value || undefined, monthFilter.value || undefined)
  } catch (error) {
    errorMessage.value = messageOf(error, '封房记录读取失败')
  } finally {
    loading.value = false
  }
}

function beginAdd(): void {
  editingBlock.value = null
  Object.assign(form, emptyForm(), { room_id: rooms.value[0]?.id ?? null })
  errorMessage.value = ''
  successMessage.value = ''
  dialogOpen.value = true
}

function beginEdit(block: RoomBlock): void {
  editingBlock.value = block
  Object.assign(form, {
    room_id: block.room_id,
    start_date: block.start_date,
    end_date: block.end_date,
    reason: block.reason || '',
  })
  errorMessage.value = ''
  successMessage.value = ''
  dialogOpen.value = true
}

function closeDialog(): void {
  if (!saving.value) dialogOpen.value = false
}

async function saveBlock(): Promise<void> {
  if (!form.room_id || !form.start_date || !form.end_date) {
    errorMessage.value = '请填写房间、封房开始和结束日期'
    return
  }
  if (form.end_date <= form.start_date) {
    errorMessage.value = '结束日期必须晚于开始日期'
    return
  }
  const request: RoomBlockSaveRequest = {
    room_id: form.room_id,
    start_date: form.start_date,
    end_date: form.end_date,
    reason: form.reason.trim() || null,
  }
  saving.value = true
  errorMessage.value = ''
  try {
    if (editingBlock.value) await roomBlockApi.update(editingBlock.value.id, request)
    else await roomBlockApi.create(request)
    dialogOpen.value = false
    successMessage.value = editingBlock.value ? '封房区间已更新' : '封房记录已创建'
    await loadBlocks()
  } catch (error) {
    errorMessage.value = messageOf(error, '封房保存失败')
  } finally {
    saving.value = false
  }
}

async function removeBlock(block: RoomBlock): Promise<void> {
  if (!window.confirm(`确定解除 ${roomName(block.room_id)} ${block.start_date} 至 ${block.end_date} 的整段封房吗？`)) {
    return
  }
  errorMessage.value = ''
  try {
    await roomBlockApi.remove(block.id)
    successMessage.value = '整段封房已解除'
    await loadBlocks()
  } catch (error) {
    errorMessage.value = messageOf(error, '解除封房失败')
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
        <p class="page-eyebrow">INVENTORY CONTROL</p>
        <h1>封房管理</h1>
        <p class="page-description">仅管理主动封闭的住宿夜晚。结束日期不包含当晚，保存时会复核订单及有效锁房冲突。</p>
      </div>
      <button class="button button-primary" type="button" :disabled="rooms.length === 0" @click="beginAdd">新增封房</button>
    </div>

    <AdminNoticeDialog v-model:message="successMessage" kind="success" />
    <AdminNoticeDialog v-model:message="errorMessage" />

    <form class="filter-card" @submit.prevent="loadBlocks">
      <label>
        房间
        <select v-model="roomFilter">
          <option value="">全部房间</option>
          <option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.number }} · {{ room.name }}</option>
        </select>
      </label>
      <label>
        月份
        <input v-model="monthFilter" type="month">
      </label>
      <button class="button button-outline" type="submit" :disabled="loading">查询</button>
      <button class="button button-quiet" type="button" @click="roomFilter = ''; monthFilter = ''; loadBlocks()">清除筛选</button>
    </form>

    <div class="table-card">
      <div v-if="loading" class="table-state">正在读取封房记录…</div>
      <div v-else-if="blocks.length === 0" class="table-state">
        <strong>当前筛选范围没有封房记录</strong>
        <span>新建后会即时参与小程序可订查询和后台库存校验。</span>
      </div>
      <div v-else class="table-scroll">
        <table class="data-table blocks-table">
          <thead><tr><th>客房</th><th>封房区间</th><th>封房原因</th><th class="actions-column">操作</th></tr></thead>
          <tbody>
            <tr v-for="block in blocks" :key="block.id">
              <td><strong>{{ roomName(block.room_id) }}</strong></td>
              <td><strong>{{ block.start_date }} 至 {{ block.end_date }}</strong><small>住宿区间左闭右开，结束日期当晚可重新入住</small></td>
              <td>{{ block.reason || '未填写' }}</td>
              <td class="row-actions">
                <button class="button button-quiet" type="button" @click="beginEdit(block)">调整</button>
                <button class="button button-quiet danger-action" type="button" @click="removeBlock(block)">整段解封</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="dialogOpen" class="dialog-backdrop" @click.self="closeDialog">
      <section class="form-dialog compact-dialog" role="dialog" aria-modal="true" aria-labelledby="room-block-title">
        <div class="dialog-header">
          <div><p class="page-eyebrow">ROOM BLOCK</p><h2 id="room-block-title">{{ editingBlock ? '调整封房' : '新增封房' }}</h2></div>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeDialog">×</button>
        </div>
        <form class="catalog-form" @submit.prevent="saveBlock">
          <label>客房
            <select v-model.number="form.room_id" required>
              <option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.number }} · {{ room.name }}</option>
            </select>
          </label>
          <div class="form-grid">
            <label>开始日期<input v-model="form.start_date" type="date" required></label>
            <label>结束日期（不含）<input v-model="form.end_date" type="date" :min="form.start_date || undefined" required></label>
          </div>
          <label>封房原因（选填）<input v-model="form.reason" type="text" maxlength="200" placeholder="例如：维修、自用"></label>
          <p class="form-help">调整日期会重新检查订单、其他封房和未过期锁房。整段解封请使用列表中的“整段解封”。</p>
          <div class="dialog-actions">
            <button class="button button-outline" type="button" :disabled="saving" @click="closeDialog">取消</button>
            <button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>
