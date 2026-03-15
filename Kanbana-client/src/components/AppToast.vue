<script setup lang="ts">
import { useToastStore } from '../stores/toast'

const toast = useToastStore()
</script>

<template>
  <div class="toast-container">
    <TransitionGroup name="toast">
      <div
        v-for="t in toast.toasts"
        :key="t.id"
        class="toast"
        :class="`toast-${t.type}`"
        @click="toast.remove(t.id)"
      >
        <span class="toast-icon">
          {{ t.type === 'success' ? '✓' : t.type === 'error' ? '✕' : 'ℹ' }}
        </span>
        <span>{{ t.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 360px;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: var(--bg-2);
  cursor: pointer;
  font-size: 13px;
  box-shadow: var(--shadow-lg);
  transition: opacity var(--transition), transform var(--transition);
}

.toast-success { border-color: rgba(34, 197, 94, 0.3); }
.toast-error { border-color: rgba(239, 68, 68, 0.3); }
.toast-info { border-color: rgba(99, 102, 241, 0.3); }

.toast-icon {
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.toast-success .toast-icon { color: var(--success); }
.toast-error .toast-icon { color: var(--danger); }
.toast-info .toast-icon { color: var(--primary); }

.toast-enter-from { opacity: 0; transform: translateX(100%); }
.toast-leave-to { opacity: 0; transform: translateX(100%); }
.toast-enter-active, .toast-leave-active { transition: all 250ms ease; }
</style>
