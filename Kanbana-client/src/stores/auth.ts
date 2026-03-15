import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User } from '../types'
import * as authApi from '../api/auth'

const STORAGE_KEY = 'kanbana_user'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)

  function loadFromStorage() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) user.value = JSON.parse(raw)
    } catch {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  async function login(email: string, password: string) {
    const res = await authApi.login(email, password)
    user.value = res.user
    localStorage.setItem(STORAGE_KEY, JSON.stringify(res.user))
  }

  function logout() {
    user.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  function setUser(u: User) {
    user.value = u
    localStorage.setItem(STORAGE_KEY, JSON.stringify(u))
  }

  return { user, loadFromStorage, login, logout, setUser }
})
