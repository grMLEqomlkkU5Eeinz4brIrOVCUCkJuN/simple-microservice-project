<script setup lang="ts">
import { ref } from 'vue'
import AppModal from '../AppModal.vue'
import AppSpinner from '../AppSpinner.vue'
import { searchUsers } from '../../api/users'
import { createOrGetChat } from '../../api/chats'
import { useAuthStore } from '../../stores/auth'
import { useToastStore } from '../../stores/toast'
import type { Chat, UserSearchResult } from '../../types'

const props = defineProps<{
  chats: Chat[]
  activeChatId: number | null
}>()

const emit = defineEmits<{
  'select-chat': [chatId: number]
  'chat-created': [chat: Chat]
}>()

const auth = useAuthStore()
const toast = useToastStore()

const showNewChat = ref(false)
const searchQuery = ref('')
const searchResults = ref<UserSearchResult[]>([])
const creating = ref(false)
let timer: ReturnType<typeof setTimeout>

function onSearch() {
  clearTimeout(timer)
  if (!searchQuery.value.trim()) { searchResults.value = []; return }
  timer = setTimeout(async () => {
    const res = await searchUsers(searchQuery.value)
    searchResults.value = res.results.filter((u) => u.id !== auth.user?.id)
  }, 300)
}

async function startChat(user: UserSearchResult) {
  creating.value = true
  try {
    const chat = await createOrGetChat(user.id)
    emit('chat-created', chat)
    emit('select-chat', chat.id)
    showNewChat.value = false
    searchQuery.value = ''
    searchResults.value = []
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : 'Failed to create chat')
  } finally {
    creating.value = false
  }
}

function getChatName(chat: Chat): string {
  const other = chat.participants.find((p) => p.userId !== auth.user?.id)
  return other?.name || `Chat #${chat.id}`
}

function getLatestMessage(chat: Chat): string {
  const msg = chat.messages?.[0]
  if (!msg) return 'No messages yet'
  const prefix = msg.fromUserId === auth.user?.id ? 'You: ' : ''
  return prefix + (msg.chatContent.length > 40 ? msg.chatContent.slice(0, 40) + '…' : msg.chatContent)
}
</script>

<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <h2 class="sidebar-title">Messages</h2>
      <button class="btn btn-primary btn-sm" @click="showNewChat = true">+ New</button>
    </div>

    <div class="chat-list">
      <div
        v-for="chat in chats"
        :key="chat.id"
        class="chat-item"
        :class="{ active: chat.id === activeChatId }"
        @click="emit('select-chat', chat.id)"
      >
        <div class="chat-avatar">{{ getChatName(chat).charAt(0).toUpperCase() }}</div>
        <div class="chat-info">
          <div class="chat-name">{{ getChatName(chat) }}</div>
          <div class="chat-preview">{{ getLatestMessage(chat) }}</div>
        </div>
      </div>

      <div v-if="chats.length === 0" class="empty-state" style="padding:24px 16px">
        <div class="text-muted text-sm">No conversations yet</div>
      </div>
    </div>

    <!-- New chat modal -->
    <AppModal title="New Conversation" :show="showNewChat" @close="showNewChat = false">
      <div style="display:flex;flex-direction:column;gap:12px">
        <div class="input-group">
          <label>Search user</label>
          <input
            v-model="searchQuery"
            class="input"
            placeholder="Search by name or email..."
            @input="onSearch"
            autocomplete="off"
          />
        </div>

        <div v-if="creating" style="text-align:center;padding:12px">
          <AppSpinner size="20px" />
        </div>

        <div v-else-if="searchResults.length" class="user-results">
          <button
            v-for="u in searchResults"
            :key="u.id"
            class="user-result-item"
            @click="startChat(u)"
            type="button"
          >
            <div class="chat-avatar sm">{{ u.name.charAt(0).toUpperCase() }}</div>
            <span>{{ u.name }}</span>
          </button>
        </div>

        <div v-else-if="searchQuery" class="text-muted text-sm" style="text-align:center;padding:8px">
          No users found
        </div>
      </div>
    </AppModal>
  </div>
</template>

<style scoped>
.sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  background: var(--bg-2);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background var(--transition);
  border-bottom: 1px solid rgba(255,255,255,0.04);
}

.chat-item:hover { background: var(--bg-3); }
.chat-item.active { background: var(--primary-dim); }

.chat-avatar {
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
  flex-shrink: 0;
}

.chat-avatar.sm {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.chat-info { flex: 1; min-width: 0; }

.chat-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-preview {
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.user-results {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 240px;
  overflow-y: auto;
}

.user-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: var(--bg-3);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  transition: background var(--transition), border-color var(--transition);
  font-size: 13px;
  color: var(--text);
  font-family: inherit;
}

.user-result-item:hover {
  background: var(--bg-4);
  border-color: var(--primary);
}
</style>
