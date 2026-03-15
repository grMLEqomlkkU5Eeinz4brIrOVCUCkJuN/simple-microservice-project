<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { register } from '../api/auth'
import AppSpinner from '../components/AppSpinner.vue'

const name = ref('')
const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const success = ref(false)

async function submit() {
  if (!name.value || !email.value || !password.value) {
    error.value = 'Please fill in all fields'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await register(email.value, password.value, name.value)
    success.value = true
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Registration failed'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <template v-if="success">
        <div class="auth-title">Check your inbox</div>
        <div class="auth-subtitle">
          We sent a verification link to <strong>{{ email }}</strong>.
          Please verify your email to continue.
        </div>
        <div class="auth-footer">
          <RouterLink to="/login">Back to sign in</RouterLink>
        </div>
      </template>

      <template v-else>
        <div class="auth-title">Create account</div>
        <div class="auth-subtitle">Get started with Kanbana</div>

        <form class="auth-form" @submit.prevent="submit">
          <div class="input-group">
            <label>Name</label>
            <input v-model="name" type="text" class="input" placeholder="Your name" autocomplete="name" />
          </div>
          <div class="input-group">
            <label>Email</label>
            <input v-model="email" type="email" class="input" placeholder="you@example.com" autocomplete="email" />
          </div>
          <div class="input-group">
            <label>Password</label>
            <input v-model="password" type="password" class="input" placeholder="••••••••" autocomplete="new-password" />
          </div>
          <div v-if="error" class="form-error">{{ error }}</div>
          <button type="submit" class="btn btn-primary" :disabled="loading">
            <AppSpinner v-if="loading" size="14px" />
            Create account
          </button>
        </form>

        <div class="auth-footer">
          Already have an account?
          <RouterLink to="/login">Sign in</RouterLink>
        </div>
      </template>
    </div>
  </div>
</template>
