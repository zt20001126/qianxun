<script setup lang="ts">
/** 管理后台统一提示弹窗：管理员确认后才关闭提示。 */
import { computed, nextTick, ref, useId, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    message: string
    kind?: 'error' | 'success' | 'info'
  }>(),
  { kind: 'error' },
)

const emit = defineEmits<{
  'update:message': [message: string]
}>()

const confirmButton = ref<HTMLButtonElement | null>(null)
const messageId = useId()
const heading = computed(() => {
  if (props.kind === 'success') return '操作成功'
  if (props.kind === 'info') return '提示'
  return '操作未完成'
})

async function focusConfirmButton(): Promise<void> {
  await nextTick()
  confirmButton.value?.focus()
}

function confirm(): void {
  emit('update:message', '')
}

watch(() => props.message, focusConfirmButton, { immediate: true })
</script>

<template>
  <Teleport to="body">
    <div v-if="message" class="admin-notice-backdrop">
      <section
        class="admin-notice-dialog"
        :class="`is-${kind}`"
        role="alertdialog"
        aria-modal="true"
        :aria-label="heading"
        :aria-describedby="messageId"
        @keydown.esc.prevent.stop
        @keydown.tab.prevent="focusConfirmButton"
      >
        <span class="admin-notice-icon" aria-hidden="true">{{ kind === 'success' ? '✓' : kind === 'info' ? 'i' : '!' }}</span>
        <h2>{{ heading }}</h2>
        <p :id="messageId">{{ message }}</p>
        <div class="admin-notice-actions">
          <button ref="confirmButton" class="button button-primary" type="button" @click="confirm">确认</button>
        </div>
      </section>
    </div>
  </Teleport>
</template>
