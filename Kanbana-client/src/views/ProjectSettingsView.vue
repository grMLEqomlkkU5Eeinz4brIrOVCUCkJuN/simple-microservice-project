<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppNav from '../components/AppNav.vue'
import AppModal from '../components/AppModal.vue'
import AppSpinner from '../components/AppSpinner.vue'
import { getProject, updateProject, deleteProject } from '../api/projects'
import { listPermissions, addPermission, updatePermission, revokePermission } from '../api/permissions'
import { searchUsersByName } from '../api/users'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import type { Project, Permission, UserSearchResult, PermissionLevel } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const projectId = computed(() => Number(route.params.id))
const project = ref<Project | null>(null)
const permissions = ref<Permission[]>([])
const loading = ref(true)
const activeTab = ref<'general' | 'team' | 'danger'>('general')

// General form
const formName = ref('')
const formPublic = ref(false)
const formShared = ref(false)
const formViewPass = ref('')
const formEditPass = ref('')
const saving = ref(false)

// Team / invite
const showInvite = ref(false)
const inviteQuery = ref('')
const inviteResults = ref<UserSearchResult[]>([])
const inviteUserId = ref<number | null>(null)
const inviteLevel = ref<PermissionLevel>('READONLY')
const inviting = ref(false)
let searchTimer: ReturnType<typeof setTimeout>

function onInviteSearch() {
  clearTimeout(searchTimer)
  if (!inviteQuery.value.trim()) { inviteResults.value = []; return }
  searchTimer = setTimeout(async () => {
    const res = await searchUsersByName(inviteQuery.value)
    inviteResults.value = res.results.filter((u) => u.id !== auth.user?.id)
  }, 300)
}

function selectUser(u: UserSearchResult) {
  inviteUserId.value = u.id
  inviteQuery.value = u.name
  inviteResults.value = []
}

async function submitInvite() {
  if (!inviteUserId.value) return
  inviting.value = true
  try {
    const perm = await addPermission(projectId.value, inviteUserId.value, inviteLevel.value)
    permissions.value.push(perm)
    toast.success('User invited!')
    showInvite.value = false
    inviteQuery.value = ''
    inviteUserId.value = null
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : 'Failed to invite')
  } finally {
    inviting.value = false
  }
}

async function changePermLevel(perm: Permission, level: PermissionLevel) {
  try {
    const updated = await updatePermission(projectId.value, perm.userId, level)
    const idx = permissions.value.findIndex((p) => p.id === perm.id)
    if (idx !== -1) permissions.value[idx] = updated
    toast.success('Permission updated')
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : 'Failed to update')
  }
}

async function revoke(perm: Permission) {
  if (!confirm('Revoke access for this user?')) return
  try {
    await revokePermission(projectId.value, perm.userId)
    permissions.value = permissions.value.filter((p) => p.id !== perm.id)
    toast.success('Access revoked')
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : 'Failed to revoke')
  }
}

// Save general settings
async function saveGeneral() {
  if (!formName.value.trim()) return
  saving.value = true
  try {
    const updated = await updateProject(projectId.value, {
      projectName: formName.value.trim(),
      isPublic: formPublic.value,
      isShared: formShared.value,
      viewPassword: formViewPass.value || undefined,
      editPassword: formEditPass.value || undefined,
    })
    project.value = updated
    toast.success('Settings saved')
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : 'Failed to save')
  } finally {
    saving.value = false
  }
}

// Delete project
const deleting = ref(false)
async function remove() {
  if (!confirm(`Delete project "${project.value?.projectName}"? This cannot be undone.`)) return
  deleting.value = true
  try {
    await deleteProject(projectId.value)
    toast.success('Project deleted')
    router.push('/projects')
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : 'Failed to delete')
    deleting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const [projRes, permRes] = await Promise.all([
      getProject(projectId.value),
      listPermissions(projectId.value),
    ])
    project.value = projRes.project
    permissions.value = permRes.permissions
    formName.value = projRes.project.projectName
    formPublic.value = projRes.project.isPublic
    formShared.value = projRes.project.isShared
  } catch (e: unknown) {
    toast.error(e instanceof Error ? e.message : 'Failed to load settings')
    router.push('/projects')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <AppNav />

    <div v-if="loading" class="center-spinner" style="padding:64px 0">
      <AppSpinner size="32px" />
    </div>

    <div v-else class="container settings-wrap">
      <div class="settings-header">
        <button class="btn btn-ghost btn-sm" @click="router.push(`/projects/${projectId}`)">
          ← Board
        </button>
        <h1 class="settings-title">{{ project?.projectName }} — Settings</h1>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button
          v-for="tab in ['general', 'team', 'danger'] as const"
          :key="tab"
          class="tab"
          :class="{ active: activeTab === tab }"
          @click="activeTab = tab"
        >{{ tab.charAt(0).toUpperCase() + tab.slice(1) }}</button>
      </div>

      <!-- General tab -->
      <div v-if="activeTab === 'general'" class="settings-section">
        <form @submit.prevent="saveGeneral" style="display:flex;flex-direction:column;gap:16px;max-width:480px">
          <div class="input-group">
            <label>Project Name</label>
            <input v-model="formName" class="input" placeholder="Project name" />
          </div>
          <label class="toggle">
            <input type="checkbox" v-model="formPublic" />
            <span>Make public (anyone can view without auth)</span>
          </label>
          <label class="toggle">
            <input type="checkbox" v-model="formShared" />
            <span>Enable password-protected sharing</span>
          </label>
          <template v-if="formShared">
            <div class="input-group">
              <label>View Password (leave blank to keep existing)</label>
              <input v-model="formViewPass" type="password" class="input" placeholder="••••••••" />
            </div>
            <div class="input-group">
              <label>Edit Password (leave blank to keep existing)</label>
              <input v-model="formEditPass" type="password" class="input" placeholder="••••••••" />
            </div>
          </template>
          <div>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              <AppSpinner v-if="saving" size="14px" />
              Save Changes
            </button>
          </div>
        </form>
      </div>

      <!-- Team tab -->
      <div v-else-if="activeTab === 'team'" class="settings-section">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <h2 class="section-subtitle">Team Access</h2>
          <button class="btn btn-primary btn-sm" @click="showInvite = true">+ Invite User</button>
        </div>

        <div v-if="permissions.length === 0" class="empty-state" style="padding:24px 0">
          <div class="text-muted text-sm">No users invited yet</div>
        </div>

        <div v-else class="perm-list">
          <div v-for="perm in permissions" :key="perm.id" class="perm-row">
            <span class="perm-user">User #{{ perm.userId }}</span>
            <select
              class="input perm-select"
              :value="perm.permissionLevel"
              @change="changePermLevel(perm, ($event.target as HTMLSelectElement).value as PermissionLevel)"
            >
              <option value="READONLY">Read Only</option>
              <option value="EDIT">Edit</option>
            </select>
            <button class="btn btn-danger btn-sm" @click="revoke(perm)">Revoke</button>
          </div>
        </div>
      </div>

      <!-- Danger tab -->
      <div v-else-if="activeTab === 'danger'" class="settings-section">
        <div class="danger-zone">
          <h2 class="section-subtitle" style="color:var(--danger)">Danger Zone</h2>
          <p class="text-muted text-sm" style="margin:8px 0 16px">
            This will permanently delete the project and all its buckets and tasks.
          </p>
          <button class="btn btn-danger" @click="remove" :disabled="deleting">
            <AppSpinner v-if="deleting" size="14px" />
            Delete Project
          </button>
        </div>
      </div>
    </div>

    <!-- Invite modal -->
    <AppModal title="Invite User" :show="showInvite" @close="showInvite = false">
      <div style="display:flex;flex-direction:column;gap:16px">
        <div class="input-group" style="position:relative">
          <label>Search User by Name</label>
          <input
            v-model="inviteQuery"
            class="input"
            placeholder="Type a name..."
            @input="onInviteSearch"
            autocomplete="off"
          />
          <div v-if="inviteResults.length" class="search-dropdown">
            <button
              v-for="u in inviteResults"
              :key="u.id"
              class="search-item"
              @click="selectUser(u)"
              type="button"
            >{{ u.name }}</button>
          </div>
        </div>
        <div class="input-group">
          <label>Permission Level</label>
          <select v-model="inviteLevel" class="input">
            <option value="READONLY">Read Only</option>
            <option value="EDIT">Edit</option>
          </select>
        </div>
        <div style="display:flex;gap:8px;justify-content:flex-end">
          <button class="btn btn-ghost" @click="showInvite = false">Cancel</button>
          <button class="btn btn-primary" @click="submitInvite" :disabled="!inviteUserId || inviting">
            <AppSpinner v-if="inviting" size="14px" />
            Invite
          </button>
        </div>
      </div>
    </AppModal>
  </div>
</template>

<style scoped>
.settings-wrap {
  padding-top: 32px;
  padding-bottom: 48px;
}

.settings-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.settings-title {
  font-size: 18px;
  font-weight: 700;
}

.tabs {
  display: flex;
  gap: 2px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 24px;
}

.tab {
  padding: 8px 16px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  margin-bottom: -1px;
  transition: color var(--transition), border-color var(--transition);
  font-family: inherit;
}

.tab.active {
  color: var(--primary-hover);
  border-bottom-color: var(--primary);
}

.tab:hover:not(.active) {
  color: var(--text);
}

.settings-section {
  max-width: 720px;
}

.section-subtitle {
  font-size: 15px;
  font-weight: 600;
}

.perm-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.perm-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}

.perm-user {
  flex: 1;
  font-size: 13px;
}

.perm-select {
  width: 130px;
}

.danger-zone {
  padding: 20px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: var(--radius-lg);
  background: var(--danger-dim);
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--bg-3);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  margin-top: 2px;
  z-index: 100;
  overflow: hidden;
}

.search-item {
  width: 100%;
  text-align: left;
  padding: 9px 12px;
  background: none;
  border: none;
  color: var(--text);
  font-size: 13px;
  cursor: pointer;
  transition: background var(--transition);
  font-family: inherit;
}

.search-item:hover {
  background: var(--bg-4);
}
</style>
