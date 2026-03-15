<script setup lang="ts">
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'

const auth = useAuthStore()
const chatStore = useChatStore()
const router = useRouter()

function logout() {
  chatStore.disconnect()
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <nav class="nav">
    <div class="nav-inner">
      <div class="nav-left">
        <RouterLink to="/projects" class="nav-brand">Kanbana</RouterLink>
        <RouterLink to="/projects" class="nav-link">Projects</RouterLink>
        <RouterLink to="/chat" class="nav-link">Chat</RouterLink>
      </div>
      <div class="nav-right">
        <span class="nav-user">{{ auth.user?.name }}</span>
        <button class="btn btn-ghost btn-sm" @click="logout">Logout</button>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.nav {
  background: var(--bg-2);
  border-bottom: 1px solid var(--border);
  height: 52px;
  flex-shrink: 0;
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 24px;
  max-width: 1600px;
  margin: 0 auto;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-brand {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin-right: 12px;
  letter-spacing: -0.02em;
}

.nav-brand:hover { color: var(--primary-hover); }

.nav-link {
  padding: 6px 10px;
  border-radius: var(--radius);
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  transition: color var(--transition), background var(--transition);
}

.nav-link:hover, .nav-link.router-link-active {
  color: var(--text);
  background: var(--bg-3);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-user {
  font-size: 13px;
  color: var(--text-muted);
}
</style>
