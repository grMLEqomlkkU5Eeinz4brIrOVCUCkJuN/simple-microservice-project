import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ToastType } from '../types'

export interface Toast {
  id: number
  message: string
  type: ToastType
}

let nextId = 0

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])

  function add(message: string, type: ToastType = 'info', duration = 4000) {
    const id = ++nextId
    toasts.value.push({ id, message, type })
    setTimeout(() => remove(id), duration)
  }

  function remove(id: number) {
    const idx = toasts.value.findIndex((t) => t.id === id)
    if (idx !== -1) toasts.value.splice(idx, 1)
  }

  function success(message: string) { add(message, 'success') }
  function error(message: string) { add(message, 'error') }
  function info(message: string) { add(message, 'info') }

  return { toasts, add, remove, success, error, info }
})
