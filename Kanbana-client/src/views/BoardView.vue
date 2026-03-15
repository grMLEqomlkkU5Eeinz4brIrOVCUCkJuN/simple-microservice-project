<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppNav from '../components/AppNav.vue'
import AppSpinner from '../components/AppSpinner.vue'
import KanbanBoard from '../components/board/KanbanBoard.vue'
import type { ColumnData } from '../components/board/KanbanBoard.vue'
import { getProject } from '../api/projects'
import { listBuckets } from '../api/buckets'
import { listTasks } from '../api/tasks'
import { useAuthStore } from '../stores/auth'
import type { Project, Bucket, Task } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const projectId = computed(() => Number(route.params.id))

const project = ref<Project | null>(null)
const columns = ref<ColumnData[]>([])
const loading = ref(true)
const error = ref('')

const isOwner = computed(() => project.value?.ownerId === auth.user?.id)
const canEdit = computed(() => isOwner.value) // for now owner = can edit; shared edit access same

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [projRes, bucketsRes] = await Promise.all([
      getProject(projectId.value),
      listBuckets(projectId.value),
    ])

    project.value = projRes.project

    // Fetch tasks for all buckets in parallel
    const taskResults = await Promise.all(
      bucketsRes.buckets.map((bws) => listTasks(bws.bucket.id))
    )

    columns.value = bucketsRes.buckets.map((bws, i) => ({
      bucket: bws.bucket,
      stats: bws.stats,
      tasks: taskResults[i]?.tasks ?? [],
    }))
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Failed to load board'
  } finally {
    loading.value = false
  }
}

// Event handlers from board
function onTaskCreated(task: Task, bucketId: number) {
  const col = columns.value.find((c) => c.bucket.id === bucketId)
  if (col) {
    col.tasks.push(task)
    col.stats.totalTasks++
    recalcStats(col)
  }
}

function onTaskUpdated(task: Task) {
  const col = columns.value.find((c) => c.bucket.id === task.bucketId)
  if (col) {
    const idx = col.tasks.findIndex((t) => t.id === task.id)
    if (idx !== -1) {
      col.tasks[idx] = task
      recalcStats(col)
    }
  }
}

function onTaskDeleted(taskId: number, bucketId: number) {
  const col = columns.value.find((c) => c.bucket.id === bucketId)
  if (col) {
    col.tasks = col.tasks.filter((t) => t.id !== taskId)
    recalcStats(col)
  }
}

function recalcStats(col: ColumnData) {
  const done = col.tasks.filter((t) => t.isTaskDone).length
  col.stats.completedTasks = done
  col.stats.totalTasks = col.tasks.length
  col.stats.completionPercentage = col.tasks.length
    ? Math.round((done / col.tasks.length) * 100)
    : 0
}

function onBucketUpdated(bucket: Bucket) {
  const col = columns.value.find((c) => c.bucket.id === bucket.id)
  if (col) col.bucket = bucket
}

function onBucketDeleted(bucketId: number) {
  columns.value = columns.value.filter((c) => c.bucket.id !== bucketId)
}

function onBucketCreated(bucket: Bucket) {
  columns.value.push({
    bucket,
    stats: { totalTasks: 0, completedTasks: 0, completionPercentage: 0 },
    tasks: [],
  })
}

onMounted(load)
</script>

<template>
  <div class="board-page">
    <AppNav />

    <!-- Board header -->
    <div v-if="!loading" class="board-header">
      <button class="btn btn-ghost btn-sm" @click="router.push('/projects')">← Projects</button>
      <h1 class="board-title">{{ project?.projectName }}</h1>
      <div style="display:flex;gap:8px">
        <button
          v-if="isOwner"
          class="btn btn-ghost btn-sm"
          @click="router.push(`/projects/${projectId}/settings`)"
        >⚙ Settings</button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="center-spinner">
      <AppSpinner size="32px" />
    </div>

    <!-- Error -->
    <div v-else-if="error" class="empty-state">
      <div class="empty-state-icon">⚠</div>
      <div class="empty-state-title">{{ error }}</div>
      <button class="btn btn-ghost" @click="load">Retry</button>
    </div>

    <!-- Board -->
    <div v-else class="board-wrap">
      <KanbanBoard
        :columns="columns"
        :can-edit="canEdit"
        :is-owner="isOwner"
        :project-id="projectId"
        @task-created="onTaskCreated"
        @task-updated="onTaskUpdated"
        @task-deleted="onTaskDeleted"
        @bucket-updated="onBucketUpdated"
        @bucket-deleted="onBucketDeleted"
        @bucket-created="onBucketCreated"
      />
    </div>
  </div>
</template>

<style scoped>
.board-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.board-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  background: var(--bg-2);
}

.board-title {
  font-size: 16px;
  font-weight: 700;
  flex: 1;
}

.board-wrap {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.center-spinner {
  display: flex;
  justify-content: center;
  padding: 64px 0;
}
</style>
