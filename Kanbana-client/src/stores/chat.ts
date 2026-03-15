import { defineStore } from 'pinia'
import { ref } from 'vue'
import { io, type Socket } from 'socket.io-client'
import { CHAT_API } from '../api/client'
import type { Chat, Message } from '../types'

export const useChatStore = defineStore('chat', () => {
  const socket = ref<Socket | null>(null)
  const chats = ref<Chat[]>([])
  const messages = ref<Message[]>([])
  const activeChatId = ref<number | null>(null)
  // userId -> Set of chatIds where that user is typing
  const typingUsers = ref<Map<number, Set<number>>>(new Map())

  function connect() {
    if (socket.value?.connected) return

    socket.value = io(CHAT_API, {
      withCredentials: true,
    })

    socket.value.on('message_received', (msg: Message & { chatId: number }) => {
      // If the message is for the currently open chat, add it
      if (msg.chatId === activeChatId.value) {
        messages.value.unshift(msg)
      }
      // Update latest message in chat list
      const chat = chats.value.find((c) => c.id === msg.chatId)
      if (chat) {
        chat.messages = [msg]
        // Move to top
        chats.value = [chat, ...chats.value.filter((c) => c.id !== msg.chatId)]
      }
    })

    socket.value.on('message_sent', (msg: Message & { chatId: number }) => {
      if (msg.chatId === activeChatId.value) {
        messages.value.unshift(msg)
      }
      const chat = chats.value.find((c) => c.id === msg.chatId)
      if (chat) {
        chat.messages = [msg]
        chats.value = [chat, ...chats.value.filter((c) => c.id !== msg.chatId)]
      }
    })

    socket.value.on('user_typing', ({ chatId, userId }: { chatId: number; userId: number }) => {
      if (!typingUsers.value.has(userId)) {
        typingUsers.value.set(userId, new Set())
      }
      typingUsers.value.get(userId)!.add(chatId)
    })

    socket.value.on('user_stop_typing', ({ chatId, userId }: { chatId: number; userId: number }) => {
      typingUsers.value.get(userId)?.delete(chatId)
    })
  }

  function disconnect() {
    socket.value?.disconnect()
    socket.value = null
  }

  function sendMessage(chatId: number, content: string) {
    socket.value?.emit('send_message', { chatId, content })
  }

  function emitTyping(chatId: number) {
    socket.value?.emit('typing', { chatId })
  }

  function emitStopTyping(chatId: number) {
    socket.value?.emit('stop_typing', { chatId })
  }

  function getTypingUsersForChat(chatId: number): number[] {
    const result: number[] = []
    typingUsers.value.forEach((chatSet, userId) => {
      if (chatSet.has(chatId)) result.push(userId)
    })
    return result
  }

  return {
    socket,
    chats,
    messages,
    activeChatId,
    typingUsers,
    connect,
    disconnect,
    sendMessage,
    emitTyping,
    emitStopTyping,
    getTypingUsersForChat,
  }
})
