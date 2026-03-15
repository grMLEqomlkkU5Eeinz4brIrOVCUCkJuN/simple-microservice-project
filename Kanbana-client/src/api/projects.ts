import { get, post, put, del, TASK_API } from './client'
import type { Project, ProjectWithStats } from '../types'

export function listProjects() {
  return get<{ projects: Project[]; count: number }>(`${TASK_API}/api/projects`)
}

export function listPublicProjects() {
  return get<{ projects: Project[]; count: number }>(`${TASK_API}/api/projects/public`)
}

export function searchProjects(query: string) {
  return get<{ projects: Project[]; count: number }>(
    `${TASK_API}/api/projects/search?query=${encodeURIComponent(query)}`
  )
}

export function getProject(id: number) {
  return get<ProjectWithStats>(`${TASK_API}/api/projects/${id}`)
}

export interface CreateProjectBody {
  projectName: string
  isPublic?: boolean
  isShared?: boolean
  viewPassword?: string
  editPassword?: string
}

export function createProject(body: CreateProjectBody) {
  return post<Project>(`${TASK_API}/api/projects`, body)
}

export function updateProject(id: number, body: Partial<CreateProjectBody>) {
  return put<Project>(`${TASK_API}/api/projects/${id}`, body)
}

export function deleteProject(id: number) {
  return del<{ message: string }>(`${TASK_API}/api/projects/${id}`)
}
