<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import type { Project } from '../types'

const props = defineProps<{ project: Project }>()
const router = useRouter()
const auth = useAuthStore()

const isOwner = props.project.ownerId === auth.user?.id

function openBoard() {
  router.push(`/projects/${props.project.id}`)
}

function openSettings(e: Event) {
  e.stopPropagation()
  router.push(`/projects/${props.project.id}/settings`)
}
</script>

<template>
  <div class="project-card" @click="openBoard">
    <div class="project-card-header">
      <h3 class="project-name">{{ project.projectName }}</h3>
      <button v-if="isOwner" class="btn-icon" title="Settings" @click="openSettings">⚙</button>
    </div>
    <div class="project-badges">
      <span v-if="isOwner" class="badge badge-primary">Owner</span>
      <span v-else class="badge badge-warning">Shared</span>
      <span v-if="project.isPublic" class="badge badge-success">Public</span>
      <span v-if="project.isShared" class="badge" style="background:var(--bg-4);color:var(--text-muted)">Shared link</span>
    </div>
  </div>
</template>

<style scoped>
.project-card {
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 16px;
  cursor: pointer;
  transition: border-color var(--transition), background var(--transition);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.project-card:hover {
  border-color: var(--primary);
  background: var(--bg-3);
}

.project-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.project-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  flex: 1;
}

.project-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
