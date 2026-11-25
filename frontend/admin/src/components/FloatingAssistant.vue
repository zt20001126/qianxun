<script setup lang="ts">
/** 后台公共悬浮入口；收起时保留对话组件及其未发送内容。 */
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AssistantView from '../views/assistant/index.vue'

const route = useRoute()
const opened = ref(false)
const initialized = ref(false)
const launcher = ref<HTMLButtonElement | null>(null)
const closeButton = ref<HTMLButtonElement | null>(null)
const visible = computed(() => route.path !== '/assistant')

async function open(): Promise<void> {
  initialized.value = true
  opened.value = true
  await nextTick()
  closeButton.value?.focus()
}
async function close(): Promise<void> {
  opened.value = false
  await nextTick()
  launcher.value?.focus()
}
// 在独立助手页面隐藏悬浮入口，返回其他页面后保留之前的会话状态。
watch(visible, (isVisible) => { if (!isVisible) opened.value = false })
</script>

<template>
  <div v-show="visible" class="assistant-float-root" @keydown.esc.stop="opened && close()">
    <section v-if="initialized" v-show="opened" id="assistant-floating-window" class="assistant-window" role="region" aria-label="智能助手悬浮窗口">
      <header class="window-header">
        <div class="window-title"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v9a2.5 2.5 0 0 1-2.5 2.5H10l-4.5 4v-4H6.5A2.5 2.5 0 0 1 4 14.5z"/><path d="M8 9h8M8 12.5h5"/></svg><h2>智能助手</h2></div>
        <button ref="closeButton" type="button" class="window-close" aria-label="收起智能助手" @click="close"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14"/></svg></button>
      </header>
      <AssistantView floating />
    </section>
    <button v-show="!opened" ref="launcher" type="button" class="assistant-launcher" aria-label="打开智能助手" :aria-expanded="opened" aria-controls="assistant-floating-window" @click="open">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v9a2.5 2.5 0 0 1-2.5 2.5H10l-4.5 4v-4H6.5A2.5 2.5 0 0 1 4 14.5z"/><path d="M8 9h8M8 12.5h5"/></svg>
      <span>智能助手</span>
    </button>
  </div>
</template>

<style scoped>
.assistant-float-root { position: fixed; right: 24px; bottom: 24px; z-index: 19; }
.assistant-window { display: flex; width: min(460px, calc(100vw - 48px)); height: min(700px, calc(100dvh - 48px)); min-height: 0; flex-direction: column; overflow: hidden; border: 1px solid #dbe7e8; border-radius: 14px; background: #fff; box-shadow: 0 16px 48px rgba(25, 55, 61, .2); }
.window-header { display: flex; min-height: 58px; align-items: center; justify-content: space-between; gap: 12px; padding: 0 16px 0 20px; border-bottom: 1px solid #e5eded; }
.window-title { display: flex; align-items: center; gap: 10px; color: #226b7b; }
.window-title h2 { margin: 0; color: #263c40; font-size: 16px; }
.window-title svg, .assistant-launcher svg, .window-close svg { width: 22px; height: 22px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.window-close { display: grid; width: 36px; height: 36px; place-items: center; border: 0; border-radius: 7px; color: #526a6e; background: transparent; }
.window-close:hover { background: #edf5f5; }
.assistant-launcher { display: flex; width: 72px; height: 72px; align-items: center; justify-content: center; flex-direction: column; gap: 4px; border: 0; border-radius: 50%; color: #fff; background: #286e7f; box-shadow: 0 8px 22px rgba(35, 92, 104, .25); font-size: 11px; font-weight: 600; }
.assistant-launcher:hover { background: #205868; }
button:focus-visible { outline: 3px solid #78b6c2; outline-offset: 3px; }
@media (max-width: 560px) { .assistant-float-root { right: 12px; bottom: 12px; } .assistant-window { width: calc(100vw - 24px); height: min(700px, calc(100dvh - 24px)); } .assistant-launcher { width: 60px; height: 60px; } }
</style>
