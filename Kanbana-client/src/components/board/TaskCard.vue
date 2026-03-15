<script setup lang="ts">
import { ref } from 'vue'
import AppModal from '../AppModal.vue'
import AppSpinner from '../AppSpinner.vue'
import { updateTask, markTaskDone, deleteTask } from '../../api/tasks'
import { useToastStore } from '../../stores/toast'
import type { Task } from '../../types'

const props = defineProps<{
  task: Task
  canEdit: boolean
  isOwner: boolean
}>()

const emit = defineEmits<{
  updated: [task: Task]
  deleted: [id: number]
}>()

const toast = useToastStore()

const showDetail = ref(false)
const togglingDone = ref(false)
const deleting = ref(false)

// Edit form state
const editName = ref('')
const editDesc = ref('')
const saving = ref(false)

function openDetail() {
  editName.value = props.task.taskName
  editDesc.value = props.task.taskDesc || ''
  showDetail.value = true
}

async function toggleDone(e: Event) {
  e.stopPropagation()
  if (!props.canEdit || togglingDone.value) return
  togglingDone.value = true
  try {
    let updated: Task
    if (props.task.isTaskDone) {
      updated = await updateTask(props.task.id, { isTaskDone: false })
    } else {
      updated = await markTaskDone(props.task.id)
    }
    emit('updated', updated)
  } catch (err: unknown) {
    toast.error(err instanceof Error ? err.message : 'Failed to update task')
  } finally {
    togglingDone.value = false
  }
}

async function saveEdit() {
  if (!editName.value.trim()) return
  saving.value = true
  try {
    const updated = await updateTask(props.task.id, {
      taskName: editName.value.trim(),
      taskDesc: editDesc.value.trim() || undefined,
    })
    emit('updated', updated)
    toast.success('Task updated')
    showDetail.value = false
  } catch (err: unknown) {
    toast.error(err instanceof Error ? err.message : 'Failed to save')
  } finally {
    saving.value = false
  }
}

async function remove(e: Event) {
  e.stopPropagation()
  if (!confirm('Delete this task?')) return
  deleting.value = true
  try {
    await deleteTask(props.task.id)
    emit('deleted', props.task.id)
    showDetail.value = false
  } catch (err: unknown) {
    toast.error(err instanceof Error ? err.message : 'Failed to delete')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="task-card" :class="{ done: task.isTaskDone }" @click="openDetail">
    <div class="task-card-row">
      <button
        class="task-checkbox"
        :class="{ checked: task.isTaskDone, loading: togglingDone }"
        :disabled="!canEdit || togglingDone"
        @click="toggleDone"
        :title="task.isTaskDone ? 'Mark incomplete' : 'Mark done'"
      >
        <span v-if="!togglingDone">{{ task.isTaskDone ? '✓' : '' }}</span>
        <AppSpinner v-else size="10px" />
      </button>
      <div class="task-content">
        <div class="task-name">{{ task.taskName }}</div>
        <div v-if="task.taskDesc" class="task-desc">{{ task.taskDesc }}</div>
      </div>
      <button
        v-if="isOwner"
        class="btn-icon danger task-delete"
        title="Delete task"
        @click="remove"
        :disabled="deleting"
      >✕</button>
    </div>
  </div>

  <!-- Task detail modal -->
  <AppModal title="Task Details" :show="showDetail" @close="showDetail = false">
    <div style="display:flex;flex-direction:column;gap:16px">
      <div class="input-group">
        <label>Task Name</label>
        <input
          v-model="editName"
          class="input"
          :disabled="!canEdit"
          placeholder="Task name"
        />
      </div>
      <div class="input-group">
        <label>Description</label>
        <textarea
          v-model="editDesc"
          class="input"
          :disabled="!canEdit"
          placeholder="Optional description..."
          rows="4"
        />
      </div>

      <div style="display:flex;align-items:center;gap:8px">
        <span class="badge" :class="task.isTaskDone ? 'badge-success' : ''">
          {{ task.isTaskDone ? 'Completed' : 'In progress' }}
        </span>
        <span class="text-muted text-sm" style="margin-left:auto">
          Created {{ new Date(task.createdAt).toLocaleDateString() }}
        </span>
      </div>

      <div style="display:flex;gap:8px;justify-content:space-between">
        <button v-if="isOwner" class="btn btn-danger btn-sm" @click="remove" :disabled="deleting">
          <AppSpinner v-if="deleting" size="12px" />
          Delete
        </button>
        <div style="display:flex;gap:8px;margin-left:auto">
          <button class="btn btn-ghost" @click="showDetail = false">Close</button>
          <button v-if="canEdit" class="btn btn-primary" @click="saveEdit" :disabled="saving">
            <AppSpinner v-if="saving" size="14px" />
            Save
          </button>
        </div>
      </div>
    </div>
  </AppModal>
</template>

<style scoped>
.task-card {
  background: var(--bg-3);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 10px 12px;
  cursor: pointer;
  transition: border-color var(--transition), background var(--transition);
}

.task-card:hover {
  border-color: var(--primary);
  background: var(--bg-4);
}

.task-card:hover .task-delete {
  opacity: 1;
}

.task-card.done {
  opacity: 0.6;
}

.task-card-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.task-checkbox {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 2px solid var(--border);
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: var(--success);
  flex-shrink: 0;
  margin-top: 1px;
  transition: border-color var(--transition), background var(--transition);
}

.task-checkbox.checked {
  border-color: var(--success);
  background: var(--success-dim);
}

.task-checkbox:hover:not(:disabled) {
  border-color: var(--primary);
}

.task-checkbox:disabled {
  cursor: default;
}

.task-content {
  flex: 1;
  min-width: 0;
}

.task-name {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  word-break: break-word;
}

.done .task-name {
  text-decoration: line-through;
  color: var(--text-muted);
}

.task-desc {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-delete {
  opacity: 0;
  transition: opacity var(--transition);
  font-size: 11px;
  flex-shrink: 0;
}
</style>
