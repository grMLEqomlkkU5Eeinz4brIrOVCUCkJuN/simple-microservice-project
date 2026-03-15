<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import AppSpinner from '../components/AppSpinner.vue'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!email.value || !password.value) {
    error.value = 'Please fill in all fields'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.login(email.value, password.value)
    toast.success('Welcome back!')
    router.push('/projects')
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-title">Welcome back</div>
      <div class="auth-subtitle">Sign in to your Kanbana account</div>

      <form class="auth-form" @submit.prevent="submit">
        <div class="input-group">
          <label>Email</label>
          <input
            v-model="email"
            type="email"
            class="input"
            placeholder="you@example.com"
            autocomplete="email"
          />
        </div>
        <div class="input-group">
          <label>Password</label>
          <input
            v-model="password"
            type="password"
            class="input"
            placeholder="••••••••"
            autocomplete="current-password"
          />
        </div>
        <div v-if="error" class="form-error">{{ error }}</div>
        <button type="submit" class="btn btn-primary" :disabled="loading">
          <AppSpinner v-if="loading" size="14px" />
          Sign in
        </button>
      </form>

      <div class="auth-footer">
        Don't have an account?
        <RouterLink to="/register">Create one</RouterLink>
      </div>
    </div>
  </div>
</template>
