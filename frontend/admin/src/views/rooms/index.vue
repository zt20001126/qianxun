<script setup lang="ts">
/** R05-R09 客房资料维护。具体日期占用由房态模块统一校验。 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminNoticeDialog from '../../components/AdminNoticeDialog.vue'
import { roomApi } from '../../api/rooms'
import type { Room, RoomSaveRequest } from '../../types/catalog'

const ROOM_NUMBERS = ['101', '102', '103', '201', '202', '203']

interface RoomForm {
  number: string
  name: string
  floor: number
  price: string
  capacity: string
  bed_info: string
  area: string
  facilities_text: string
  images_text: string
  description: string
}

const rooms = ref<Room[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const dialogOpen = ref(false)
const editingRoom = ref<Room | null>(null)
const busyRoomIds = ref<number[]>([])
const form = reactive<RoomForm>(emptyForm())

const availableNumbers = computed(() => ROOM_NUMBERS.filter((number) =>
  !rooms.value.some((room) => room.number === number && room.id !== editingRoom.value?.id),
))

watch(() => form.number, (number) => {
  if (number) form.floor = number.startsWith('1') ? 1 : 2
})

onMounted(() => { void loadRooms() })

function emptyForm(): RoomForm {
  return {
    number: '', name: '', floor: 1, price: '', capacity: '', bed_info: '', area: '',
    facilities_text: '', images_text: '', description: '',
  }
}

async function loadRooms(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    rooms.value = await roomApi.list()
  } catch (error) {
    errorMessage.value = messageOf(error, '客房资料读取失败')
  } finally {
    loading.value = false
  }
}

function beginAdd(): void {
  editingRoom.value = null
  Object.assign(form, emptyForm(), { number: availableNumbers.value[0] || '' })
  errorMessage.value = ''
  successMessage.value = ''
  dialogOpen.value = true
}

function beginEdit(room: Room): void {
  editingRoom.value = room
  Object.assign(form, {
    number: room.number,
    name: room.name,
    floor: room.floor,
    price: room.price,
    capacity: String(room.capacity),
    bed_info: room.bed_info || '',
    area: room.area === null ? '' : String(room.area),
    facilities_text: (room.facilities || []).join('\n'),
    images_text: (room.images || []).join('\n'),
    description: room.description || '',
  })
  errorMessage.value = ''
  successMessage.value = ''
  dialogOpen.value = true
}

function closeDialog(): void {
  if (!saving.value) dialogOpen.value = false
}

async function saveRoom(): Promise<void> {
  const price = form.price.trim()
  const capacity = Number(form.capacity)
  if (!form.number || !form.name.trim()) {
    errorMessage.value = '请填写房号和房间名称'
    return
  }
  if (!/^\d{1,10}(?:\.\d{1,2})?$/.test(price)) {
    errorMessage.value = '房价须为不超过两位小数的非负金额'
    return
  }
  if (!Number.isInteger(capacity) || capacity < 1 || capacity > 65535) {
    errorMessage.value = '入住人数须为正整数'
    return
  }
  if (form.area && !/^\d{1,4}(?:\.\d{1,2})?$/.test(form.area.trim())) {
    errorMessage.value = '面积须为不超过两位小数的非负数字'
    return
  }

  const request: RoomSaveRequest = {
    number: form.number,
    name: form.name.trim(),
    floor: form.floor,
    price,
    capacity,
    bed_info: form.bed_info.trim() || null,
    area: form.area ? Number(form.area) : null,
    facilities: splitLines(form.facilities_text),
    images: splitLines(form.images_text),
    description: form.description.trim() || null,
  }

  saving.value = true
  errorMessage.value = ''
  try {
    if (editingRoom.value) await roomApi.update(editingRoom.value.id, request)
    else await roomApi.create(request)
    dialogOpen.value = false
    successMessage.value = editingRoom.value ? '客房资料已更新' : '客房已创建'
    await loadRooms()
  } catch (error) {
    errorMessage.value = messageOf(error, '客房保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleRoom(room: Room): Promise<void> {
  busyRoomIds.value = [...busyRoomIds.value, room.id]
  errorMessage.value = ''
  try {
    await roomApi.setEnabled(room.id, !room.enabled)
    successMessage.value = room.enabled ? `${room.number} 已下架` : `${room.number} 已上架`
    await loadRooms()
  } catch (error) {
    errorMessage.value = messageOf(error, '客房状态更新失败')
  } finally {
    busyRoomIds.value = busyRoomIds.value.filter((id) => id !== room.id)
  }
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
        <p class="page-eyebrow">ROOMS</p>
        <h1>客房管理</h1>
        <p class="page-description">维护房型资料、基础夜价和展示状态。指定日期房态将在后续阶段接入。</p>
      </div>
      <button class="button button-primary" type="button" :disabled="availableNumbers.length === 0" @click="beginAdd">
        新增客房
      </button>
    </div>

    <div class="summary-strip">
      <span>客房总数 <strong>{{ rooms.length }}</strong> / 6</span>
      <span>当前上架 <strong>{{ rooms.filter((room) => room.enabled).length }}</strong></span>
    </div>

    <AdminNoticeDialog v-model:message="successMessage" kind="success" />
    <AdminNoticeDialog v-model:message="errorMessage" />

    <div class="table-card">
      <div v-if="loading" class="table-state">正在读取客房资料…</div>
      <div v-else-if="rooms.length === 0" class="table-state">
        <strong>还没有客房资料</strong>
        <span>添加 101、102、103、201、202、203 房间后，小程序即可展示房型。</span>
      </div>
      <div v-else class="table-scroll">
        <table class="data-table rooms-table">
          <thead>
            <tr><th>房间</th><th>房型与床型</th><th>楼层</th><th>可住人数</th><th>每晚价格</th><th>设施 / 图片</th><th>状态</th><th class="actions-column">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="room in rooms" :key="room.id">
              <td><strong class="room-number">{{ room.number }}</strong></td>
              <td><strong>{{ room.name }}</strong><small>{{ room.bed_info || '未填写床型' }}</small></td>
              <td>{{ room.floor }} 楼</td>
              <td>{{ room.capacity }} 人</td>
              <td class="price-cell">¥{{ room.price }} <small>/ 晚</small></td>
              <td>{{ room.facilities.length }} 项设施 · {{ room.images.length }} 张图片</td>
              <td><span class="status-pill" :class="room.enabled ? 'is-enabled' : 'is-disabled'">{{ room.enabled ? '已上架' : '已下架' }}</span></td>
              <td class="row-actions">
                <button class="button button-quiet" type="button" @click="beginEdit(room)">编辑</button>
                <button class="button button-quiet" type="button" :disabled="busyRoomIds.includes(room.id)" @click="toggleRoom(room)">
                  {{ busyRoomIds.includes(room.id) ? '处理中…' : room.enabled ? '下架' : '上架' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="dialogOpen" class="dialog-backdrop" @click.self="closeDialog">
      <section class="form-dialog" role="dialog" aria-modal="true" :aria-label="editingRoom ? '编辑客房' : '新增客房'">
        <header class="dialog-header">
          <div><p class="page-eyebrow">ROOM PROFILE</p><h2>{{ editingRoom ? '编辑客房' : '新增客房' }}</h2></div>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeDialog">×</button>
        </header>
        <form class="catalog-form" @submit.prevent="saveRoom">
          <div class="form-grid">
            <label>房号
              <select v-model="form.number" required>
                <option value="" disabled>选择房号</option>
                <option v-for="number in availableNumbers" :key="number" :value="number">{{ number }}</option>
              </select>
            </label>
            <label>楼层
              <input v-model.number="form.floor" type="number" min="1" max="2" required readonly />
            </label>
            <label class="form-span">房间名称
              <input v-model="form.name" maxlength="80" placeholder="例如：智能大床房" required />
            </label>
            <label>每晚价格（元）
              <input v-model="form.price" class="money-value" inputmode="decimal" maxlength="13" placeholder="如 198.00" required />
            </label>
            <label>可入住人数
              <input v-model="form.capacity" type="number" min="1" max="65535" placeholder="人数" required />
            </label>
            <label>床型与数量
              <input v-model="form.bed_info" maxlength="120" placeholder="如：大床 1.8m" />
            </label>
            <label>面积（平方米）
              <input v-model="form.area" inputmode="decimal" placeholder="可选" />
            </label>
            <label class="form-span">设施（每行一项）
              <textarea v-model="form.facilities_text" rows="3" placeholder="全屋智能&#10;独立卫浴" />
            </label>
            <label class="form-span">图片地址（每行一项）
              <textarea v-model="form.images_text" rows="3" placeholder="请输入 HTTPS 图片地址" />
              <small>后台保存图片地址，请填写可访问的图片链接。</small>
            </label>
            <label class="form-span">房间介绍
              <textarea v-model="form.description" rows="4" placeholder="填写房间介绍" />
            </label>
          </div>
          <footer class="dialog-actions">
            <button class="button button-outline" type="button" :disabled="saving" @click="closeDialog">取消</button>
            <button class="button button-primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存客房' }}</button>
          </footer>
        </form>
      </section>
    </div>
  </section>
</template>
