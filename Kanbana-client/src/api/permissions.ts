import { get, post, put, del, TASK_API } from './client'
import type { Permission, PermissionLevel } from '../types'

export function listPermissions(projectId: number) {
  return get<{ permissions: Permission[]; count: number }>(
    `${TASK_API}/api/projects/${projectId}/permissions`
  )
}

export function addPermission(projectId: number, userId: number, permissionLevel: PermissionLevel) {
  return post<Permission>(`${TASK_API}/api/projects/${projectId}/permissions`, {
    userId: String(userId),
    permissionLevel,
  })
}

export function updatePermission(projectId: number, userId: number, permissionLevel: PermissionLevel) {
  return put<Permission>(`${TASK_API}/api/projects/${projectId}/permissions/${userId}`, {
    permissionLevel,
  })
}

export function revokePermission(projectId: number, userId: number) {
  return del<{ message: string }>(`${TASK_API}/api/projects/${projectId}/permissions/${userId}`)
}
