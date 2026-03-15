<script setup lang="ts">
import { ref, watch } from 'vue'
import AppModal from './AppModal.vue'
import AppSpinner from './AppSpinner.vue'
import { createProject } from '../api/projects'
import { useToastStore } from '../stores/toast'
import type { Project } from '../types'

const props = defineProps<{ show: boolean }>()
const emit = defineEmits<{ close: []; created: [project: Project] }>()

const toast = useToastStore()

const name = ref('')
const isPublic = ref(false)
const isShared = ref(false)
const viewPassword = ref('')
const editPassword = ref('')
const loading = ref(false)
const error = ref('')

watch(() => props.show, (v) => {
  if (!v) return
  name.value = ''
  isPublic.value = false
  isShared.value = false
  viewPassword.value = ''
  editPassword.value = ''
  error.value = ''
})

async function submit() {
  if (!name.value.trim()) { error.value = 'Project name is required'; return }
  loading.value = true
  error.value = ''
  try {
    const project = await createProject({
      projectName: name.value.trim(),
      isPublic: isPublic.value,
      isShared: isShared.value,
      viewPassword: viewPassword.value || undefined,
      editPassword: editPassword.value || undefined,
    })
    toast.success('Project created!')
    emit('created', project)
    emit('close')
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Failed to create project'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AppModal title="New Project" :show="show" @close="emit('close')">
    <form @submit.prevent="submit" style="display:flex;flex-direction:column;gap:16px">
      <div class="input-group">
        <label>Project Name</label>
        <input v-model="name" class="input" placeholder="My awesome project" />
      </div>

      <label class="toggle">
        <input type="checkbox" v-model="isPublic" />
        <span>Make public (anyone can view)</span>
      </label>

      <label class="toggle">
        <input type="checkbox" v-model="isShared" />
        <span>Enable password sharing</span>
      </label>

      <template v-if="isShared">
        <div class="input-group">
          <label>View Password (optional)</label>
          <input v-model="viewPassword" type="password" class="input" placeholder="Leave blank for no password" />
        </div>
        <div class="input-group">
          <label>Edit Password (optional)</label>
          <input v-model="editPassword" type="password" class="input" placeholder="Leave blank for no password" />
        </div>
      </template>

      <div v-if="error" class="form-error">{{ error }}</div>

      <div style="display:flex;gap:8px;justify-content:flex-end">
        <button type="button" class="btn btn-ghost" @click="emit('close')">Cancel</button>
        <button type="submit" class="btn btn-primary" :disabled="loading">
          <AppSpinner v-if="loading" size="14px" />
          Create
        </button>
      </div>
    </form>
  </AppModal>
</template>
