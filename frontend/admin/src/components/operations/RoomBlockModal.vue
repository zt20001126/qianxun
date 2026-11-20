<script setup lang="ts">
/** 页内弹窗：新增管理员封房 / 调整封房区间与解除封房。 */
import { computed, reactive, ref, watch } from 'vue'
import { roomBlockApi } from '../../api/room-blocks'
import type { RoomBlock, RoomBlockSaveRequest } from '../../api/room-blocks'
import type { Room } from '../../types/catalog'
import { addDays, shanghaiDate } from '../../utils/operations'

interface Props {
  open: boolean
  rooms: Room[]
  block?: RoomBlock | null
  presetRoomId?: number | null
  presetStartDate?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  saved: [message: string]
}>()

const form = reactive({
  room_id: '',
  start_date: '',
  end_date: '',
  reason: '',
})
const saving = ref(false)
const removing = ref(false)
const errorMessage = ref('')

const isEdit = computed(() => props.block != null)

watch(
  () => [props.open, props.block] as const,
  ([isOpen, block]) => {
    if (!isOpen) return
    errorMessage.value = ''
    if (block) {
      form.room_id = String(block.room_id)
      form.start_date = block.start_date
      form.end_date = block.end_date
      form.reason = block.reason || ''
    } else {
      const start = props.presetStartDate || shanghaiDate()
      form.room_id = String(props.presetRoomId ?? props.rooms[0]?.id ?? '')
      form.start_date = start
      form.end_date = addDays(start, 2)
      form.reason = ''
    }
  },
)

function handleClose(): void {
  if (!saving.value && !removing.value) emit('close')
}

async function submit(): Promise<void> {
  errorMessage.value = ''
  if (!form.room_id || !form.start_date || !form.end_date) {
    errorMessage.value = '请填写房间、开始日期与结束日期'
    return
  }
  if (form.end_date <= form.start_date) {
    errorMessage.value = '结束日期必须晚于开始日期'
    return
  }

  const request: RoomBlockSaveRequest = {
    room_id: Number(form.room_id),
    start_date: form.start_date,
    end_date: form.end_date,
    reason: form.reason.trim() || null,
  }

  saving.value = true
  try {
    if (props.block) {
      await roomBlockApi.update(props.block.id, request)
      emit('saved', '封房区间已调整')
    } else {
      await roomBlockApi.create(request)
      emit('saved', '封房记录已生效')
    }
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '封房保存失败'
  } finally {
    saving.value = false
  }
}

async function removeBlock(): Promise<void> {
  if (!props.block) return
  if (!window.confirm(`确定整段解除 ${props.block.start_date} ~ ${props.block.end_date} 的封房吗？`)) return
  removing.value = true
  errorMessage.value = ''
  try {
    await roomBlockApi.remove(props.block.id)
    emit('saved', '已解除封房，对应间夜恢复可订')
  } catch (cause) {
    errorMessage.value = cause instanceof Error ? cause.message : '解除封房失败'
  } finally {
    removing.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="qx-modal-backdrop" @click.self="handleClose">
      <div class="qx-modal-panel qx-modal-md" role="dialog" aria-modal="true" aria-labelledby="room-block-title">
        <div class="qx-modal-header">
          <div>
            <h3 id="room-block-title">{{ isEdit ? '调整封房区间 / 解除封房' : '新增管理员封房' }}</h3>
            <p>仅用于管理员主动设置不可订间夜（维修、自用等），结束日期不含当晚</p>
          </div>
          <button type="button" class="qx-modal-close" aria-label="关闭" @click="handleClose">
            <svg viewBox="0 0 24 24" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          </button>
        </div>

        <p v-if="errorMessage" class="notice notice-error qx-modal-alert" role="alert">{{ errorMessage }}</p>

        <form class="qx-modal-form" @submit.prevent="submit">
          <label class="qx-field">
            <span>选择房间</span>
            <select v-model="form.room_id" required>
              <option v-for="room in rooms" :key="room.id" :value="String(room.id)">
                {{ room.number }} · {{ room.name }}
              </option>
            </select>
          </label>

          <div class="qx-form-grid-2">
            <label class="qx-field">
              <span>开始日期（首个不可订晚）</span>
              <input v-model="form.start_date" type="date" required />
            </label>

            <label class="qx-field">
              <span>结束日期（不含当晚）</span>
              <input v-model="form.end_date" type="date" required />
            </label>
          </div>

          <label class="qx-field">
            <span>封房原因（如维修、主理人自用）</span>
            <input v-model="form.reason" type="text" placeholder="如：设备检修维护" />
          </label>

          <div class="qx-modal-footer">
            <div class="qx-footer-left">
              <button
                v-if="isEdit"
                type="button"
                class="qx-btn-danger-outline"
                :disabled="removing || saving"
                @click="removeBlock"
              >
                {{ removing ? '解除中…' : '整段解除封房' }}
              </button>
            </div>
            <div class="qx-footer-right">
              <button type="button" class="qx-btn-ghost" :disabled="saving || removing" @click="handleClose">取消</button>
              <button type="submit" class="qx-btn-primary" :disabled="saving || removing">
                {{ saving ? '保存中…' : isEdit ? '保存区间' : '确认封房' }}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>
