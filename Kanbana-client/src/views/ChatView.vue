<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppNav from '../components/AppNav.vue'
import AppSpinner from '../components/AppSpinner.vue'
import ChatSidebar from '../components/chat/ChatSidebar.vue'
import ChatWindow from '../components/chat/ChatWindow.vue'
import { listChats } from '../api/chats'
import { useChatStore } from '../stores/chat'
import type { Chat } from '../types'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()

const chats = ref<Chat[]>([])
const activeChat = ref<Chat | null>(null)
const loading = ref(true)

const activeChatId = computed(() => activeChat.value?.id ?? null)

async function load() {
  loading.value = true
  try {
    const res = await listChats()
    chats.value = res
  } finally {
    loading.value = false
  }
}

function selectChat(chatId: number) {
  const chat = chats.value.find((c) => c.id === chatId)
  if (chat) {
    activeChat.value = chat
    router.replace(`/chat/${chatId}`)
  }
}

function onChatCreated(chat: Chat) {
  if (!chats.value.find((c) => c.id === chat.id)) {
    chats.value.unshift(chat)
  }
}

// Connect socket
onMounted(async () => {
  chatStore.connect()
  await load()

  // Open chat from URL param
  const paramId = Number(route.params.chatId)
  if (paramId) selectChat(paramId)
})

onUnmounted(() => {
  // Don't disconnect — user might navigate back; disconnect on logout instead
})

watch(
  () => route.params.chatId,
  (id) => {
    if (id) selectChat(Number(id))
  }
)

// Sync chat list from store updates
watch(
  () => chatStore.chats,
  (updated) => {
    if (updated.length) chats.value = updated
  },
  { deep: true }
)

onMounted(() => {
  chatStore.chats = chats.value
})

watch(chats, (c) => { chatStore.chats = c }, { deep: true })
</script>

<template>
  <div class="page chat-page">
    <AppNav />

    <div class="chat-layout">
      <div v-if="loading" class="center-spinner">
        <AppSpinner size="28px" />
      </div>

      <template v-else>
        <ChatSidebar
          :chats="chats"
          :active-chat-id="activeChatId"
          @select-chat="selectChat"
          @chat-created="onChatCreated"
        />

        <div v-if="activeChat" class="chat-main">
          <ChatWindow :key="activeChat.id" :chat="activeChat" />
        </div>

        <div v-else class="chat-empty">
          <div class="empty-state">
            <div class="empty-state-icon">💬</div>
            <div class="empty-state-title">Select a conversation</div>
            <p class="text-muted text-sm">or start a new one</p>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  height: 100vh;
  overflow: hidden;
}

.chat-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  height: calc(100vh - 52px);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.center-spinner {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
