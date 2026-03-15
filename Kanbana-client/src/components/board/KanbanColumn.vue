<script setup lang="ts">
import { ref, computed } from 'vue'
import TaskCard from './TaskCard.vue'
import AppSpinner from '../AppSpinner.vue'
import { updateBucket, deleteBucket } from '../../api/buckets'
import { createTask } from '../../api/tasks'
import { useToastStore } from '../../stores/toast'
import type { Bucket, BucketStats, Task } from '../../types'

const props = defineProps<{
  bucket: Bucket
  stats: BucketStats
  tasks: Task[]
  canEdit: boolean
  isOwner: boolean
}>()

const emit = defineEmits<{
  'task-created': [task: Task]
  'task-updated': [task: Task]
  'task-deleted': [taskId: number]
  'bucket-updated': [bucket: Bucket]
  'bucket-deleted': [bucketId: number]
}>()

const toast = useToastStore()

// Bucket name editing
const editingName = ref(false)
const nameInput = ref('')
const savingName = ref(false)

function startEditName() {
  if (!props.canEdit) return
  nameInput.value = props.bucket.bucketName
  editingName.value = true
}

async function saveName() {
  if (!nameInput.value.trim() || nameInput.value === props.bucket.bucketName) {
    editingName.value = false
    return
  }
  savingName.value = true
  try {
    const updated = await updateBucket(props.bucket.id, { bucketName: nameInput.value.trim() })
    emit('bucket-updated', updated)
    editingName.value = false
  } catch (err: unknown) {
    toast.error(err instanceof Error ? err.message : 'Failed to rename column')
  } finally {
    savingName.value = false
  }
}

function onNameKey(e: KeyboardEvent) {
  if (e.key === 'Enter') saveName()
  if (e.key === 'Escape') editingName.value = false
}

// Toggle done bucket
const togglingDone = ref(false)
async function toggleDone() {
  if (!props.isOwner) return
  togglingDone.value = true
  try {
    const updated = await updateBucket(props.bucket.id, { isDoneBucket: !props.bucket.isDoneBucket })
    emit('bucket-updated', updated)
  } catch (err: unknown) {
    toast.error(err instanceof Error ? err.message : 'Failed to update column')
  } finally {
    togglingDone.value = false
  }
}

// Delete bucket
const deleting = ref(false)
async function remove() {
  if (!confirm(`Delete column "${props.bucket.bucketName}" and all its tasks?`)) return
  deleting.value = true
  try {
    await deleteBucket(props.bucket.id)
    emit('bucket-deleted', props.bucket.id)
  } catch (err: unknown) {
    toast.error(err instanceof Error ? err.message : 'Failed to delete column')
    deleting.value = false
  }
}

// Add task
const newTaskName = ref('')
const addingTask = ref(false)
const showAddTask = ref(false)

async function submitTask() {
  if (!newTaskName.value.trim()) return
  addingTask.value = true
  try {
    const task = await createTask(props.bucket.id, newTaskName.value.trim())
    emit('task-created', task)
    newTaskName.value = ''
    showAddTask.value = false
  } catch (err: unknown) {
    toast.error(err instanceof Error ? err.message : 'Failed to create task')
  } finally {
    addingTask.value = false
  }
}

function onAddTaskKey(e: KeyboardEvent) {
  if (e.key === 'Enter') submitTask()
  if (e.key === 'Escape') { showAddTask.value = false; newTaskName.value = '' }
}

const pct = computed(() => props.stats.completionPercentage)
const isFullyDone = computed(() => props.stats.totalTasks > 0 && pct.value === 100)
</script>

<template>
  <div class="column" :class="{ 'column-done': bucket.isDoneBucket }">
    <!-- Column header -->
    <div class="column-header">
      <div class="column-title-row">
        <template v-if="editingName">
          <input
            v-model="nameInput"
            class="input column-name-input"
            @blur="saveName"
            @keydown="onNameKey"
            v-focus
          />
          <AppSpinner v-if="savingName" size="14px" />
        </template>
        <template v-else>
          <h3
            class="column-name"
            :class="{ editable: canEdit }"
            @click="startEditName"
            :title="canEdit ? 'Click to rename' : ''"
          >{{ bucket.bucketName }}</h3>
          <span class="column-count">{{ tasks.length }}</span>
        </template>
      </div>
      <div class="column-actions">
        <button
          v-if="isOwner"
          class="btn-icon"
          :title="bucket.isDoneBucket ? 'Unmark as done column' : 'Mark as done column'"
          @click="toggleDone"
        >{{ bucket.isDoneBucket ? '✓' : '○' }}</button>
        <button
          v-if="isOwner"
          class="btn-icon danger"
          title="Delete column"
          @click="remove"
          :disabled="deleting"
        >
          <AppSpinner v-if="deleting" size="12px" />
          <span v-else>✕</span>
        </button>
      </div>
    </div>

    <!-- Progress bar -->
    <div v-if="stats.totalTasks > 0" class="column-progress">
      <div class="progress">
        <div
          class="progress-bar"
          :class="{ done: isFullyDone }"
          :style="{ width: pct + '%' }"
        />
      </div>
      <span class="progress-label">{{ stats.completedTasks }}/{{ stats.totalTasks }}</span>
    </div>

    <!-- Task list -->
    <div class="task-list">
      <TaskCard
        v-for="task in tasks"
        :key="task.id"
        :task="task"
        :can-edit="canEdit"
        :is-owner="isOwner"
        @updated="(t) => emit('task-updated', t)"
        @deleted="(id) => emit('task-deleted', id)"
      />

      <div v-if="tasks.length === 0" class="column-empty">
        No tasks yet
      </div>
    </div>

    <!-- Add task -->
    <div class="add-task-area">
      <template v-if="showAddTask && canEdit">
        <input
          v-model="newTaskName"
          class="input"
          placeholder="Task name..."
          @keydown="onAddTaskKey"
          v-focus
        />
        <div style="display:flex;gap:6px;margin-top:6px">
          <button class="btn btn-primary btn-sm" @click="submitTask" :disabled="addingTask">
            <AppSpinner v-if="addingTask" size="12px" />
            Add
          </button>
          <button class="btn btn-ghost btn-sm" @click="showAddTask = false; newTaskName = ''">Cancel</button>
        </div>
      </template>
      <button
        v-else-if="canEdit"
        class="add-task-btn"
        @click="showAddTask = true"
      >
        + Add task
      </button>
    </div>
  </div>
</template>

<script lang="ts">
// v-focus directive registration
export default {
  directives: {
    focus: {
      mounted(el: HTMLElement) { el.focus() }
    }
  }
}
</script>

<style scoped>
.column {
  width: 280px;
  flex-shrink: 0;
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 120px);
}

.column-done {
  border-color: rgba(34, 197, 94, 0.25);
}

.column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.column-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.column-name {
  font-size: 13px;
  font-weight: 600;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.column-name.editable {
  cursor: pointer;
}

.column-name.editable:hover {
  color: var(--primary-hover);
}

.column-name-input {
  font-size: 13px;
  font-weight: 600;
  padding: 2px 6px;
  height: 28px;
}

.column-count {
  font-size: 11px;
  font-weight: 600;
  background: var(--bg-4);
  color: var(--text-muted);
  padding: 1px 6px;
  border-radius: 100px;
  flex-shrink: 0;
}

.column-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.column-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.column-progress .progress {
  flex: 1;
}

.progress-label {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.task-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.column-empty {
  text-align: center;
  color: var(--text-dim);
  font-size: 12px;
  padding: 16px 0;
}

.add-task-area {
  padding: 8px 10px 10px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.add-task-btn {
  width: 100%;
  padding: 8px;
  background: transparent;
  border: 1px dashed var(--border);
  border-radius: var(--radius);
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: color var(--transition), border-color var(--transition), background var(--transition);
  font-family: inherit;
}

.add-task-btn:hover {
  color: var(--text);
  border-color: var(--primary);
  background: var(--primary-dim);
}
</style>
