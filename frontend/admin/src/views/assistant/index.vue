<script setup lang="ts">
/** 管理员智能助手：会话历史、自由文本提问与对话。 */
import { computed, nextTick, onMounted, ref } from 'vue'
import { agentApi } from '../../api/agent'
import { renderAssistantMarkdown } from '../../utils/assistantMarkdown'
import type { AgentConversation, AgentMessage, AgentPart, AgentQuestion, AgentChatResult } from '../../types/agent'

const props = withDefaults(defineProps<{ floating?: boolean }>(), { floating: false })
const historyOpen = ref(false)
const conversations = ref<AgentConversation[]>([])
const conversationPage = ref(0)
const conversationTotal = ref(0)
const activeId = ref<number | null>(null)
const messages = ref<AgentMessage[]>([])
const messagePage = ref(1)
const draft = ref('')
const listLoading = ref(false)
const messagesLoading = ref(false)
const sending = ref(false)
const listError = ref('')
const messageError = ref('')
const sendError = ref('')
const questionAnswers = ref<Record<number, Record<string, string>>>({})
const messageArea = ref<HTMLElement | null>(null)
let selectionVersion = 0
let temporaryId = -1

const activeTitle = computed(() => conversations.value.find((item) => item.id === activeId.value)?.title || (activeId.value ? '当前会话' : '新对话'))
const hasMoreConversations = computed(() => conversations.value.length < conversationTotal.value)
const canSend = computed(() => !!draft.value.trim() && !sending.value && !messagesLoading.value && !messageError.value)

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
async function scrollToBottom(): Promise<void> {
  await nextTick()
  if (messageArea.value) messageArea.value.scrollTop = messageArea.value.scrollHeight
}
function restoreAnswers(items: AgentMessage[]): void {
  for (const item of items) for (const part of item.content.parts) {
    if (part.type === 'question_answers') questionAnswers.value[part.question_message_id] = part.answers
  }
}
async function loadConversations(reset = false): Promise<void> {
  if (listLoading.value) return
  listLoading.value = true
  listError.value = ''
  try {
    const page = reset ? 1 : conversationPage.value + 1
    const result = await agentApi.conversations(page)
    conversations.value = reset ? result.list : [...conversations.value, ...result.list.filter((item) => !conversations.value.some((old) => old.id === item.id))]
    conversationPage.value = page
    conversationTotal.value = result.total
  } catch (error) {
    listError.value = errorMessage(error, '会话列表加载失败，请重试。')
  } finally {
    listLoading.value = false
  }
}
async function openConversation(id: number): Promise<void> {
  if (sending.value) return
  const version = ++selectionVersion
  activeId.value = id
  messages.value = []
  messagePage.value = 1
  messageError.value = ''
  sendError.value = ''
  messagesLoading.value = true
  try {
    // 接口按时间正序分页；先取得总数，再展示最近一页。
    const count = await agentApi.messages(id, 1, 1)
    const lastPage = Math.max(1, Math.ceil(count.total / 100))
    const result = await agentApi.messages(id, lastPage)
    if (version !== selectionVersion) return
    messages.value = result.list
    restoreAnswers(result.list)
    messagePage.value = lastPage
    await scrollToBottom()
  } catch (error) {
    if (version === selectionVersion) messageError.value = errorMessage(error, '消息加载失败，请重试。')
  } finally {
    if (version === selectionVersion) messagesLoading.value = false
  }
}
async function loadEarlier(): Promise<void> {
  if (activeId.value === null || messagePage.value <= 1 || messagesLoading.value) return
  const id = activeId.value
  const version = selectionVersion
  messagesLoading.value = true
  messageError.value = ''
  try {
    const result = await agentApi.messages(id, messagePage.value - 1)
    if (version !== selectionVersion) return
    messages.value = [...result.list, ...messages.value]
    restoreAnswers(result.list)
    messagePage.value--
  } catch (error) {
    if (version === selectionVersion) messageError.value = errorMessage(error, '更早的消息加载失败，请重试。')
  } finally {
    if (version === selectionVersion) messagesLoading.value = false
  }
}
function newConversation(): void {
  if (sending.value) return
  selectionVersion++
  activeId.value = null
  messages.value = []
  messagesLoading.value = false
  messageError.value = ''
  sendError.value = ''
  messagePage.value = 1
  historyOpen.value = false
}
function localMessage(role: AgentMessage['role'], parts: AgentPart[]): AgentMessage {
  return { id: temporaryId--, role, content: { parts }, created_at: new Date().toISOString() }
}
function resultMessage(result: AgentChatResult): AgentMessage {
  const parts: AgentPart[] = result.status === 'waiting'
    ? [{ type: 'questions', questions: result.questions || [] }]
    : [{ type: 'text', text: result.answer || '' }]
  const message = localMessage('assistant', parts)
  if (result.status === 'waiting' && result.question_message_id) message.id = result.question_message_id
  return message
}
function isPendingQuestion(message: AgentMessage): boolean {
  return message.id > 0 && messages.value.at(-1)?.id === message.id && !sending.value && !messagesLoading.value
}
function setAnswer(messageId: number, questionId: string, value: string): void {
  questionAnswers.value[messageId] = { ...questionAnswers.value[messageId], [questionId]: value }
}
function readyToAnswer(message: AgentMessage, questions: AgentQuestion[]): boolean {
  return isPendingQuestion(message) && questions.every((q) => !!questionAnswers.value[message.id]?.[q.id]?.trim())
}
async function submitAnswers(message: AgentMessage, questions: AgentQuestion[]): Promise<void> {
  if (activeId.value === null || !readyToAnswer(message, questions)) return
  const answers = Object.fromEntries(questions.map((q) => [q.id, questionAnswers.value[message.id]![q.id]!.trim()]))
  sending.value = true
  sendError.value = ''
  try {
    const result = await agentApi.answer(activeId.value, message.id, answers)
    const summary = questions.map((q) => `${q.title}：${answers[q.id]}`).join('\n')
    messages.value.push(localMessage('user', [{ type: 'text', text: summary }, { type: 'question_answers', question_message_id: message.id, answers }]), resultMessage(result))
    await scrollToBottom()
    void loadConversations(true)
  } catch (error) {
    sendError.value = `${errorMessage(error, '提交失败')}。请检查会话后再重试。`
  } finally {
    sending.value = false
  }
}
async function send(): Promise<void> {
  if (!canSend.value) return
  const text = draft.value.trim()
  const id = activeId.value
  sending.value = true
  sendError.value = ''
  try {
    const result = await agentApi.chat(text, id)
    activeId.value = result.conversation_id
    messages.value.push(localMessage('user', [{ type: 'text', text }]), resultMessage(result))
    draft.value = ''
    await scrollToBottom()
    void loadConversations(true)
  } catch (error) {
    sendError.value = `${errorMessage(error, '发送失败')}。请检查会话后再重试。`
  } finally {
    sending.value = false
  }
}
function onComposerKeydown(event: KeyboardEvent): void {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    void send()
  }
}
onMounted(() => { void loadConversations(true) })
</script>

<template>
  <section class="assistant-page" :class="{ 'assistant-page-floating': props.floating, 'assistant-history-open': historyOpen }" aria-label="智能助手">
    <div v-if="props.floating" class="floating-actions">
      <button type="button" class="floating-action" :disabled="sending" @click="newConversation">＋ 新对话</button>
      <button type="button" class="floating-action" :class="{ active: historyOpen }" :aria-expanded="historyOpen" @click="historyOpen = !historyOpen">{{ historyOpen ? '返回对话' : '历史会话' }}</button>
    </div>
    <div class="assistant-workspace">
      <aside class="conversation-panel" aria-label="会话列表">
        <div class="conversation-heading"><h2>会话</h2><button type="button" class="new-chat" :disabled="sending" @click="newConversation"><span aria-hidden="true">＋</span> 新对话</button></div>
        <p class="conversation-hint">选择一段对话，继续交流</p>
        <div class="conversation-list">
          <p v-if="listLoading && !conversations.length" class="list-note" role="status">正在加载会话…</p>
          <p v-else-if="!conversations.length && !listError" class="list-note">还没有会话，发送消息开始吧。</p>
          <button v-for="item in conversations" :key="item.id" type="button" class="conversation-item" :class="{ selected: activeId === item.id }" :disabled="sending" :aria-current="activeId === item.id ? 'true' : undefined" @click="openConversation(item.id); historyOpen = false">
            <span class="conversation-title">{{ item.title || '未命名会话' }}</span>
            <span class="conversation-date">{{ new Date(item.updated_at).toLocaleDateString('zh-CN', { timeZone: 'Asia/Shanghai' }) }}</span>
          </button>
          <p v-if="listError" class="inline-error" role="alert">{{ listError }} <button type="button" @click="loadConversations(conversationPage === 0)">重试</button></p>
          <button v-if="hasMoreConversations && !listError" class="more-button" type="button" :disabled="listLoading" @click="loadConversations()">{{ listLoading ? '加载中…' : '加载更多会话' }}</button>
        </div>
      </aside>
      <div class="chat-panel">
        <div class="chat-header"><div><h2>{{ activeTitle }}</h2><p>智能助手可以帮助整理信息与撰写内容</p></div></div>
        <div ref="messageArea" class="message-area" aria-live="polite" aria-label="对话消息">
          <div v-if="!activeId && !messages.length && !sending" class="chat-empty">
            <div class="assistant-symbol" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v9a2.5 2.5 0 0 1-2.5 2.5H10l-4.5 4v-4H6.5A2.5 2.5 0 0 1 4 14.5z"/><path d="M8 9h8M8 12.5h5"/></svg></div>
            <h2>你好，今天想聊什么？</h2><p>输入你的问题，开启一段对话。</p>
          </div>
          <div v-else class="message-list">
            <button v-if="messagePage > 1 && !messageError" class="more-button earlier" type="button" :disabled="messagesLoading || sending" @click="loadEarlier">{{ messagesLoading ? '加载中…' : '查看更早消息' }}</button>
            <p v-if="messageError" class="inline-error" role="alert">{{ messageError }} <button type="button" @click="activeId && openConversation(activeId)">重新加载</button></p>
            <p v-if="messagesLoading && !messages.length" class="list-note" role="status">正在加载消息…</p>
            <article v-for="message in messages" :key="message.id" class="message-row" :class="{ 'message-user': message.role === 'user' }">
              <div class="message-avatar" aria-hidden="true">{{ message.role === 'user' ? '我' : '芊' }}</div>
              <div class="message-body"><div class="message-label">{{ message.role === 'user' ? '你' : '智能助手' }}</div>
                <div class="message-bubble"><template v-for="(part, index) in message.content.parts" :key="index"><template v-if="part.type === 'text'"><p v-if="message.role === 'user'">{{ part.text }}</p><div v-else class="assistant-markdown" v-html="renderAssistantMarkdown(part.text)"></div></template><div v-else-if="part.type === 'questions'" class="question-list">
                  <fieldset v-for="question in part.questions" :key="question.id" class="question-field" :disabled="!isPendingQuestion(message)">
                    <legend>{{ question.title }}</legend>
                    <template v-if="question.type === 'single_choice'">
                      <p v-if="question.context" class="question-context">{{ question.context }}</p>
                      <label v-for="option in question.options" :key="option.label" class="choice-option" :class="{ selected: questionAnswers[message.id]?.[question.id] === option.label }">
                        <input type="radio" :name="`${props.floating ? 'float' : 'page'}-${message.id}-${question.id}`" :checked="questionAnswers[message.id]?.[question.id] === option.label" @change="setAnswer(message.id, question.id, option.label)" />
                        <span><strong>{{ option.label }}</strong><small>{{ option.description }}</small></span>
                      </label>
                    </template>
                    <input v-else class="question-text" type="text" maxlength="100" placeholder="请输入答案" :value="questionAnswers[message.id]?.[question.id] || ''" @input="setAnswer(message.id, question.id, ($event.target as HTMLInputElement).value)" />
                  </fieldset>
                  <button v-if="isPendingQuestion(message)" type="button" class="send-button question-submit" :disabled="!readyToAnswer(message, part.questions)" @click="submitAnswers(message, part.questions)">提交答案</button>
                </div></template></div>
              </div>
            </article>
            <div v-if="sending" class="message-row"><div class="message-avatar" aria-hidden="true">芊</div><div class="message-body"><div class="message-label">智能助手</div><div class="message-bubble thinking" role="status">正在思考，请稍候…</div></div></div>
          </div>
        </div>
        <form class="composer" @submit.prevent="send">
          <p v-if="sendError" class="inline-error send-error" role="alert">{{ sendError }}</p>
          <label class="sr-only" :for="props.floating ? 'floating-assistant-message' : 'assistant-message'">输入消息</label>
          <textarea :id="props.floating ? 'floating-assistant-message' : 'assistant-message'" v-model="draft" maxlength="10000" rows="3" placeholder="输入你的问题，Enter 发送，Shift + Enter 换行" :disabled="sending" @keydown="onComposerKeydown" />
          <div class="composer-footer"><span>内容由 AI 生成，请注意核实。</span><button type="submit" class="send-button" :disabled="!canSend">{{ sending ? '发送中…' : '发送消息' }}</button></div>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.assistant-page { display: flex; min-width: 0; min-height: 0; flex: 1; flex-direction: column; }
.assistant-workspace { display: grid; grid-template-columns: 240px minmax(0, 1fr); min-height: 0; flex: 1; overflow: hidden; background: #fff; }
.conversation-panel { display: flex; min-height: 0; flex-direction: column; padding: 22px 15px; border-right: 1px solid #e5eded; background: #f8fbfb; }
.conversation-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.conversation-heading h2, .chat-header h2 { margin: 0; color: #263c40; font-size: 15px; font-weight: 700; }
.new-chat { min-height: 34px; padding: 0 10px; border: 1px solid #b9d4d9; border-radius: 7px; color: #175e70; background: #fff; font-size: 12px; font-weight: 600; }
.new-chat:hover:not(:disabled) { background: #e9f4f5; }
.new-chat span { font-size: 16px; }
.conversation-hint { margin: 10px 0 18px; color: #697a7b; font-size: 12px; }
.conversation-list { display: grid; align-content: start; gap: 5px; min-height: 0; overflow-y: auto; }
.conversation-item { display: flex; min-width: 0; flex-direction: column; gap: 6px; padding: 11px 12px; border: 1px solid transparent; border-radius: 8px; color: #3d5255; background: transparent; text-align: left; }
.conversation-item:hover, .conversation-item.selected { border-color: #cce0e3; background: #eaf4f5; }
.conversation-title { overflow: hidden; width: 100%; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.conversation-date { color: #697a7b; font-size: 11px; }
.chat-panel { display: flex; min-width: 0; min-height: 0; flex-direction: column; }
.chat-header { padding: 20px 28px; border-bottom: 1px solid #e8eeee; }
.chat-header p { margin: 5px 0 0; color: #6b7878; font-size: 12px; }
.message-area { flex: 1; min-height: 0; overflow-y: auto; padding: 28px; background: #fff; }
.chat-empty { display: flex; min-height: 100%; align-items: center; justify-content: center; flex-direction: column; text-align: center; }
.assistant-symbol { display: grid; width: 58px; height: 58px; place-items: center; border-radius: 16px; color: #276f7f; background: #e8f4f5; }
.assistant-symbol svg { width: 28px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.5; }
.chat-empty h2 { margin: 20px 0 0; color: #294a50; font-size: 20px; }
.chat-empty p { margin: 9px 0 0; color: #687778; font-size: 13px; }
.message-list { display: grid; align-content: start; gap: 24px; max-width: 760px; margin: 0 auto; }
.message-row { display: flex; gap: 11px; align-items: flex-start; }
.message-user { flex-direction: row-reverse; }
.message-avatar { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 10px; color: #276b79; background: #e7f2f3; font-size: 13px; font-weight: 700; }
.message-user .message-avatar { color: #fff; background: #467888; }
.message-body { min-width: 0; max-width: min(85%, 640px); }
.message-label { margin: 1px 0 7px; color: #5c6b6c; font-size: 12px; }
.message-user .message-label { text-align: right; }
.message-bubble { padding: 12px 15px; border-radius: 4px 12px 12px 12px; color: #283a3c; background: #f2f6f6; font-size: 14px; line-height: 1.75; overflow-wrap: anywhere; }
.message-user .message-bubble { border-radius: 12px 4px 12px 12px; color: #fff; background: #337a8b; }
.message-bubble p { margin: 0; white-space: pre-wrap; }
.assistant-markdown :deep(p) { margin: 0 0 0.65em; white-space: normal; }
.assistant-markdown :deep(ul), .assistant-markdown :deep(ol) { margin: 0 0 0.65em; padding-left: 1.5em; }
.assistant-markdown :deep(li p) { margin: 0; }
.assistant-markdown :deep(h1), .assistant-markdown :deep(h2), .assistant-markdown :deep(h3), .assistant-markdown :deep(h4), .assistant-markdown :deep(h5), .assistant-markdown :deep(h6) { margin: 0 0 0.5em; font-size: 1em; }
.assistant-markdown :deep(pre) { overflow-x: auto; white-space: pre; }
.assistant-markdown :deep(blockquote) { margin: 0 0 0.65em; padding-left: 0.8em; border-left: 3px solid #b9d4d9; }
.assistant-markdown :deep(a) { color: #175e70; text-decoration: underline; }
.assistant-markdown :deep(table) { display: block; max-width: 100%; overflow-x: auto; border-collapse: collapse; }
.assistant-markdown :deep(th), .assistant-markdown :deep(td) { padding: 4px 8px; border: 1px solid #cbdada; }
.assistant-markdown :deep(> :last-child) { margin-bottom: 0; }
.question-list { display: grid; gap: 10px; }
.question-field { min-width: 0; margin: 0; padding: 0; border: 0; }
.question-field legend { padding: 0; font-weight: 600; }
.question-context { margin-top: 4px !important; color: #536b6e; font-size: 12px; }
.choice-option { display: flex; gap: 9px; align-items: flex-start; margin-top: 8px; padding: 9px 10px; border: 1px solid #d3e2e3; border-radius: 7px; background: #fff; cursor: pointer; }
.choice-option.selected { border-color: #337a8b; background: #e7f4f5; }
.choice-option input { margin-top: 5px; accent-color: #286e7f; }
.choice-option strong, .choice-option small { display: block; }
.choice-option small { color: #536b6e; font-size: 12px; }
.question-text { width: 100%; margin-top: 8px; padding: 8px; border: 1px solid #c8dada; border-radius: 7px; font: inherit; }
.question-submit { justify-self: start; margin-top: 4px; }
.thinking { color: #536f73; }
.composer { padding: 16px 24px 20px; border-top: 1px solid #e8eeee; }
.composer textarea { display: block; width: 100%; min-height: 72px; resize: vertical; padding: 12px 14px; border: 1px solid #cedddd; border-radius: 9px; outline: none; color: #263b3d; background: #fff; font: inherit; font-size: 14px; line-height: 1.6; }
.composer textarea:focus { border-color: #337a8b; box-shadow: 0 0 0 3px rgba(51, 122, 139, .12); }
.composer textarea::placeholder { color: #778484; }
.composer-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 10px; color: #697777; font-size: 12px; }
.send-button { min-height: 36px; padding: 0 18px; border: 0; border-radius: 7px; color: #fff; background: #286e7f; font-weight: 600; }
.send-button:hover:not(:disabled) { background: #205868; }
button:disabled { cursor: not-allowed; opacity: .55; }
button:focus-visible { outline: 3px solid #78b6c2; outline-offset: 2px; }
.list-note { padding: 8px 4px; color: #617274; font-size: 12px; line-height: 1.6; }
.inline-error { color: #a53326; font-size: 12px; line-height: 1.6; }
.inline-error button, .more-button { border: 0; color: #21687a; background: transparent; font-size: 12px; text-decoration: underline; text-underline-offset: 3px; }
.more-button { padding: 10px 4px; }
.earlier { justify-self: center; }
.send-error { margin: 0 0 9px; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
.floating-actions { display: flex; gap: 8px; padding: 12px 16px; border-bottom: 1px solid #e8eeee; }
.floating-action { min-height: 36px; flex: 1; border: 1px solid #c6dcdf; border-radius: 7px; color: #205e6d; background: #f2f8f8; font-size: 13px; font-weight: 600; }
.floating-action:hover:not(:disabled), .floating-action.active { background: #dceff1; }
.assistant-page-floating .assistant-workspace { display: flex; min-height: 0; height: auto; flex: 1; }
.assistant-page-floating .conversation-panel { display: none; width: 100%; border-right: 0; }
.assistant-page-floating.assistant-history-open .conversation-panel { display: flex; }
.assistant-page-floating.assistant-history-open .chat-panel { display: none; }
.assistant-page-floating .conversation-list { display: grid; max-height: none; }
.assistant-page-floating .chat-panel { width: 100%; min-height: 0; flex: 1; }
.assistant-page-floating .chat-header { padding: 14px 18px; }
.assistant-page-floating .message-area { min-height: 0; padding: 18px; }
.assistant-page-floating .message-list { gap: 18px; }
.assistant-page-floating .composer { padding: 12px 16px 16px; }
.assistant-page-floating .composer textarea { min-height: 58px; resize: none; }
@media (max-width: 850px) { .assistant-workspace { grid-template-columns: 180px minmax(0, 1fr); height: 70vh; min-height: 520px; flex: none; } .message-area { padding: 20px; } .chat-header { padding: 17px 20px; } }
@media (max-width: 560px) { .assistant-workspace { display: flex; height: auto; min-height: 0; flex-direction: column; } .conversation-panel { padding: 14px; border-right: 0; border-bottom: 1px solid #e5eded; } .conversation-hint { margin-bottom: 8px; } .conversation-list { display: flex; max-height: 120px; overflow: auto; } .conversation-item { min-width: 140px; } .chat-panel { min-height: 520px; } .message-area { min-height: 300px; } .composer { padding: 14px; } }
</style>
