import { get, post, del, CHAT_API } from './client'
import type { Chat, Message } from '../types'

export function listChats() {
  return get<Chat[]>(`${CHAT_API}/api/chats/`)
}

export function createOrGetChat(participantId: number) {
  return post<Chat>(`${CHAT_API}/api/chats/`, { participantId })
}

export function getChat(chatId: number) {
  return get<Chat>(`${CHAT_API}/api/chats/${chatId}`)
}

export function deleteChat(chatId: number) {
  return del<{ message: string }>(`${CHAT_API}/api/chats/${chatId}`)
}

export function getMessages(chatId: number, cursor?: number, limit = 50) {
  const params = new URLSearchParams({ limit: String(limit) })
  if (cursor) params.set('cursor', String(cursor))
  return get<{ messages: Message[]; nextCursor: number | null }>(
    `${CHAT_API}/api/chats/${chatId}/messages?${params}`
  )
}

export function sendMessageRest(chatId: number, content: string) {
  return post<Message>(`${CHAT_API}/api/chats/${chatId}/messages`, { content })
}
