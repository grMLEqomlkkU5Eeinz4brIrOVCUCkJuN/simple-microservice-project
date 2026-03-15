<script setup lang="ts">
import { ref } from 'vue'
import KanbanColumn from './KanbanColumn.vue'
import AppSpinner from '../AppSpinner.vue'
import { createBucket } from '../../api/buckets'
import { useToastStore } from '../../stores/toast'
import type { Bucket, BucketStats, Task } from '../../types'

export interface ColumnData {
  bucket: Bucket
  stats: BucketStats
  tasks: Task[]
}

const props = defineProps<{
  columns: ColumnData[]
  canEdit: boolean
  isOwner: boolean
  projectId: number
}>()

const emit = defineEmits<{
  'task-created': [task: Task, bucketId: number]
  'task-updated': [task: Task]
  'task-deleted': [taskId: number, bucketId: number]
  'bucket-updated': [bucket: Bucket]
  'bucket-deleted': [bucketId: number]
  'bucket-created': [bucket: Bucket]
}>()

const toast = useToastStore()

// Add column
const showAdd = ref(false)
const newColName = ref('')
const addingCol = ref(false)

async function submitColumn() {
  if (!newColName.value.trim()) return
  addingCol.value = true
  try {
    const bucket = await createBucket(props.projectId, newColName.value.trim())
    emit('bucket-created', bucket)
    newColName.value = ''
    showAdd.value = false
  } catch (err: unknown) {
    toast.error(err instanceof Error ? err.message : 'Failed to create column')
  } finally {
    addingCol.value = false
  }
}

function onColKey(e: KeyboardEvent) {
  if (e.key === 'Enter') submitColumn()
  if (e.key === 'Escape') { showAdd.value = false; newColName.value = '' }
}
</script>

<template>
  <div class="board">
    <KanbanColumn
      v-for="col in columns"
      :key="col.bucket.id"
      :bucket="col.bucket"
      :stats="col.stats"
      :tasks="col.tasks"
      :can-edit="canEdit"
      :is-owner="isOwner"
      @task-created="(t) => emit('task-created', t, col.bucket.id)"
      @task-updated="(t) => emit('task-updated', t)"
      @task-deleted="(id) => emit('task-deleted', id, col.bucket.id)"
      @bucket-updated="(b) => emit('bucket-updated', b)"
      @bucket-deleted="(id) => emit('bucket-deleted', id)"
    />

    <!-- Add column button -->
    <div v-if="canEdit" class="add-column">
      <template v-if="showAdd">
        <div class="add-column-form">
          <input
            v-model="newColName"
            class="input"
            placeholder="Column name..."
            @keydown="onColKey"
            v-focus
          />
          <div style="display:flex;gap:6px;margin-top:8px">
            <button class="btn btn-primary btn-sm" @click="submitColumn" :disabled="addingCol">
              <AppSpinner v-if="addingCol" size="12px" />
              Add Column
            </button>
            <button class="btn btn-ghost btn-sm" @click="showAdd = false; newColName = ''">Cancel</button>
          </div>
        </div>
      </template>
      <button v-else class="add-column-btn" @click="showAdd = true">
        + Add Column
      </button>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  directives: {
    focus: { mounted(el: HTMLElement) { el.focus() } }
  }
}
</script>

<style scoped>
.board {
  display: flex;
  gap: 16px;
  padding: 20px 24px;
  overflow-x: auto;
  flex: 1;
  align-items: flex-start;
  min-height: 0;
}

.add-column {
  flex-shrink: 0;
  width: 280px;
}

.add-column-form {
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 12px;
}

.add-column-btn {
  width: 100%;
  height: 48px;
  background: transparent;
  border: 2px dashed var(--border);
  border-radius: var(--radius-lg);
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: color var(--transition), border-color var(--transition), background var(--transition);
  font-family: inherit;
}

.add-column-btn:hover {
  border-color: var(--primary);
  color: var(--primary-hover);
  background: var(--primary-dim);
}
</style>
