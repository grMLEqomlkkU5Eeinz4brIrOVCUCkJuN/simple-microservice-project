import { get, post, TASK_API } from './client'
import type { Project, ProjectStats } from '../types'

export function checkPublic(projectId: number) {
  return get<{ isPublic: boolean; requiresPassword: boolean; projectName: string }>(
    `${TASK_API}/api/projects/${projectId}/public`
  )
}

export function unlockProject(projectId: number, password: string) {
  return post<{ permission: 'READONLY' | 'EDIT'; expiresIn: number }>(
    `${TASK_API}/api/projects/${projectId}/unlock`,
    { password }
  )
}

export function getSharedProject(projectId: number) {
  return get<{ project: Project; permission: 'READONLY' | 'EDIT'; stats: ProjectStats }>(
    `${TASK_API}/api/projects/${projectId}/shared`
  )
}
