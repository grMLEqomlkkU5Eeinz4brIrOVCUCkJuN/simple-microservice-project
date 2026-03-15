<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { verifyEmail } from '../api/auth'

const route = useRoute()
const status = ref<'loading' | 'success' | 'error'>('loading')
const message = ref('')

onMounted(async () => {
  const token = route.query.token as string
  if (!token) {
    status.value = 'error'
    message.value = 'No verification token found in URL.'
    return
  }
  try {
    const res = await verifyEmail(token)
    status.value = 'success'
    message.value = res.message || 'Email verified successfully!'
  } catch (e: unknown) {
    status.value = 'error'
    message.value = e instanceof Error ? e.message : 'Verification failed.'
  }
})
</script>

<template>
  <div class="auth-page">
    <div class="auth-card" style="text-align:center">
      <template v-if="status === 'loading'">
        <div class="auth-title">Verifying...</div>
        <div class="auth-subtitle">Please wait while we verify your email.</div>
      </template>
      <template v-else-if="status === 'success'">
        <div style="font-size:40px;margin-bottom:12px">✓</div>
        <div class="auth-title">Email verified!</div>
        <div class="auth-subtitle">{{ message }}</div>
        <RouterLink to="/login" class="btn btn-primary" style="margin-top:16px;justify-content:center">
          Sign in
        </RouterLink>
      </template>
      <template v-else>
        <div style="font-size:40px;margin-bottom:12px;color:var(--danger)">✕</div>
        <div class="auth-title">Verification failed</div>
        <div class="auth-subtitle">{{ message }}</div>
        <RouterLink to="/login" class="btn btn-ghost" style="margin-top:16px;justify-content:center">
          Back to sign in
        </RouterLink>
      </template>
    </div>
  </div>
</template>
