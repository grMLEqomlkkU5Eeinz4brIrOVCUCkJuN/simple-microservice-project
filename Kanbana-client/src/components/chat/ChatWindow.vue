<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted, computed } from 'vue'
import AppSpinner from '../AppSpinner.vue'
import { getMessages } from '../../api/chats'
import { useChatStore } from '../../stores/chat'
import { useAuthStore } from '../../stores/auth'
import type { Chat, Message } from '../../types'

const props = defineProps<{ chat: Chat }>()

const auth = useAuthStore()
const chatStore = useChatStore()

const messages = ref<Message[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const nextCursor = ref<number | null>(null)
const messageContent = ref('')
const sending = ref(false)
const messagesEl = ref<HTMLElement | null>(null)

// Typing indicator
let typingTimer: ReturnType<typeof setTimeout>
const typingUserIds = computed(() => chatStore.getTypingUsersForChat(props.chat.id))

function getChatName(): string {
  const other = props.chat.participants.find((p) => p.userId !== auth.user?.id)
  return other?.name || `Chat #${props.chat.id}`
}

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessages(props.chat.id)
    messages.value = [...res.messages].reverse() // oldest at top
    nextCursor.value = res.nextCursor
    await nextTick()
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!nextCursor.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const res = await getMessages(props.chat.id, nextCursor.value)
    messages.value = [...res.messages.reverse(), ...messages.value]
    nextCursor.value = res.nextCursor
  } finally {
    loadingMore.value = false
  }
}

function scrollToBottom() {
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

function onInput() {
  chatStore.emitTyping(props.chat.id)
  clearTimeout(typingTimer)
  typingTimer = setTimeout(() => chatStore.emitStopTyping(props.chat.id), 2000)
}

function send() {
  const content = messageContent.value.trim()
  if (!content || sending.value) return
  chatStore.sendMessage(props.chat.id, content)
  messageContent.value = ''
  chatStore.emitStopTyping(props.chat.id)
  clearTimeout(typingTimer)
  nextTick(scrollToBottom)
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

// Watch for new messages in store
watch(
  () => chatStore.messages,
  async (msgs) => {
    // chatStore.messages is sorted newest-first for active chat
    if (chatStore.activeChatId === props.chat.id) {
      messages.value = [...msgs].reverse()
      await nextTick()
      scrollToBottom()
    }
  },
  { deep: true }
)

// Sync store's activeChatId and messages
watch(
  () => props.chat.id,
  () => {
    chatStore.activeChatId = props.chat.id
    chatStore.messages = []
    loadMessages()
  },
  { immediate: true }
)

onUnmounted(() => {
  chatStore.emitStopTyping(props.chat.id)
  clearTimeout(typingTimer)
})

function formatTime(ts: string): string {
  const d = new Date(ts)
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function formatDate(ts: string): string {
  return new Date(ts).toLocaleDateString()
}

function isDifferentDay(a: Message, b: Message): boolean {
  return new Date(a.createdAt).toDateString() !== new Date(b.createdAt).toDateString()
}
</script>

<template>
  <div class="chat-window">
    <!-- Header -->
    <div class="chat-header">
      <div class="chat-avatar-wrap">
        <div class="chat-avatar-letter">{{ getChatName().charAt(0).toUpperCase() }}</div>
      </div>
      <div>
        <div class="chat-header-name">{{ getChatName() }}</div>
        <div v-if="typingUserIds.length" class="typing-indicator">typing…</div>
      </div>
    </div>

    <!-- Messages -->
    <div class="messages-wrap" ref="messagesEl" @scroll.passive="loadMore">
      <div v-if="loading" style="text-align:center;padding:24px">
        <AppSpinner size="24px" />
      </div>
      <template v-else>
        <div v-if="nextCursor" style="text-align:center;padding:8px">
          <button class="btn btn-ghost btn-sm" @click="loadMore" :disabled="loadingMore">
            <AppSpinner v-if="loadingMore" size="12px" />
            Load older messages
          </button>
        </div>

        <template v-for="(msg, idx) in messages" :key="msg.id">
          <!-- Date separator -->
          <div
            v-if="idx === 0 || isDifferentDay(messages[idx - 1]!, msg)"
            class="date-sep"
          >{{ formatDate(msg.createdAt) }}</div>

          <div
            class="msg-row"
            :class="{ own: msg.fromUserId === auth.user?.id }"
          >
            <div class="bubble">
              <div class="bubble-text">{{ msg.chatContent }}</div>
              <div class="bubble-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </template>

        <div v-if="messages.length === 0" class="empty-state" style="flex:1">
          <div class="empty-state-icon" style="font-size:32px">💬</div>
          <div class="text-muted text-sm">Say hello!</div>
        </div>
      </template>
    </div>

    <!-- Input -->
    <div class="input-area">
      <textarea
        v-model="messageContent"
        class="input message-input"
        placeholder="Type a message... (Enter to send, Shift+Enter for newline)"
        rows="1"
        @keydown="onKey"
        @input="onInput"
      />
      <button class="btn btn-primary" @click="send" :disabled="!messageContent.trim()">Send</button>
    </div>
  </div>
</template>

<style scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  background: var(--bg-2);
}

.chat-avatar-letter {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary-dim);
  color: var(--primary-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.chat-header-name {
  font-size: 14px;
  font-weight: 600;
}

.typing-indicator {
  font-size: 11px;
  color: var(--text-muted);
  font-style: italic;
}

.messages-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.date-sep {
  text-align: center;
  font-size: 11px;
  color: var(--text-dim);
  margin: 12px 0 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-sep::before, .date-sep::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}

.msg-row {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 2px;
}

.msg-row.own {
  justify-content: flex-end;
}

.bubble {
  max-width: 70%;
  background: var(--bg-3);
  border: 1px solid var(--border);
  border-radius: 12px 12px 12px 4px;
  padding: 8px 12px;
}

.msg-row.own .bubble {
  background: var(--primary-dim);
  border-color: rgba(99, 102, 241, 0.3);
  border-radius: 12px 12px 4px 12px;
}

.bubble-text {
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-time {
  font-size: 10px;
  color: var(--text-dim);
  margin-top: 4px;
  text-align: right;
}

.input-area {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
  background: var(--bg-2);
}

.message-input {
  flex: 1;
  min-height: 40px;
  max-height: 120px;
  resize: none;
  padding: 9px 12px;
}
</style>
