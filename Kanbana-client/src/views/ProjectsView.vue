<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppNav from '../components/AppNav.vue'
import ProjectCard from '../components/ProjectCard.vue'
import CreateProjectModal from '../components/CreateProjectModal.vue'
import AppSpinner from '../components/AppSpinner.vue'
import { listProjects, searchProjects, listPublicProjects } from '../api/projects'
import type { Project } from '../types'

const projects = ref<Project[]>([])
const publicProjects = ref<Project[]>([])
const loading = ref(true)
const error = ref('')
const showCreate = ref(false)
const searchQuery = ref('')
const searchResults = ref<Project[]>([])
const searching = ref(false)
const searchMode = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [mine, pub] = await Promise.all([
      listProjects(),
      listPublicProjects(),
    ])
    projects.value = mine.projects
    // Filter public projects to exclude already listed ones
    const myIds = new Set(mine.projects.map((p) => p.id))
    publicProjects.value = pub.projects.filter((p) => !myIds.has(p.id))
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Failed to load projects'
  } finally {
    loading.value = false
  }
}

let searchTimer: ReturnType<typeof setTimeout>
function onSearchInput() {
  clearTimeout(searchTimer)
  if (!searchQuery.value.trim()) {
    searchMode.value = false
    searchResults.value = []
    return
  }
  searching.value = true
  searchMode.value = true
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchProjects(searchQuery.value)
      searchResults.value = res.projects
    } finally {
      searching.value = false
    }
  }, 350)
}

function onProjectCreated(project: Project) {
  projects.value.unshift(project)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <AppNav />

    <main class="main-content">
      <div class="container">
        <!-- Header -->
        <div class="page-header">
          <h1 class="page-title">Projects</h1>
          <button class="btn btn-primary" @click="showCreate = true">+ New Project</button>
        </div>

        <!-- Search -->
        <div class="search-bar">
          <input
            v-model="searchQuery"
            class="input"
            placeholder="Search projects..."
            @input="onSearchInput"
          />
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

        <!-- Search results -->
        <template v-else-if="searchMode">
          <div class="section-title">
            <span>Search results</span>
            <span v-if="searching" class="text-muted text-sm">Searching...</span>
          </div>
          <div v-if="searchResults.length" class="project-grid">
            <ProjectCard v-for="p in searchResults" :key="p.id" :project="p" />
          </div>
          <div v-else class="empty-state">
            <div class="empty-state-icon">🔍</div>
            <div class="empty-state-title">No projects found</div>
          </div>
        </template>

        <!-- Normal view -->
        <template v-else>
          <!-- My projects -->
          <div class="section-title">My Projects</div>
          <div v-if="projects.length" class="project-grid">
            <ProjectCard v-for="p in projects" :key="p.id" :project="p" @click="() => {}" />
          </div>
          <div v-else class="empty-state">
            <div class="empty-state-icon">📋</div>
            <div class="empty-state-title">No projects yet</div>
            <p class="text-muted text-sm">Create your first project to get started</p>
            <button class="btn btn-primary" @click="showCreate = true">New Project</button>
          </div>

          <!-- Public projects -->
          <template v-if="publicProjects.length">
            <div class="section-title" style="margin-top:32px">Public Projects</div>
            <div class="project-grid">
              <ProjectCard v-for="p in publicProjects" :key="p.id" :project="p" />
            </div>
          </template>
        </template>
      </div>
    </main>

    <CreateProjectModal
      :show="showCreate"
      @close="showCreate = false"
      @created="onProjectCreated"
    />
  </div>
</template>

<style scoped>
.main-content {
  flex: 1;
  padding: 32px 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
}

.search-bar {
  margin-bottom: 24px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.center-spinner {
  display: flex;
  justify-content: center;
  padding: 64px 0;
}
</style>
