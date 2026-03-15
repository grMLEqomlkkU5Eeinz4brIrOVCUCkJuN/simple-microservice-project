<script setup lang="ts">
defineProps<{ size?: string }>()
</script>

<template>
  <div class="spinner" :style="{ width: size || '20px', height: size || '20px' }" />
</template>

<style scoped>
.spinner {
  border: 2px solid var(--bg-4);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
