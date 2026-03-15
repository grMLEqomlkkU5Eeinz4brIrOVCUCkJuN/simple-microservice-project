import { get, post, put, del, TASK_API } from './client'
import type { Task } from '../types'

export function listTasks(bucketId: number) {
  return get<{ tasks: Task[]; count: number; completionPercentage: number }>(
    `${TASK_API}/api/buckets/${bucketId}/tasks`
  )
}

export function searchTasks(bucketId: number, query: string) {
  return get<{ tasks: Task[]; count: number }>(
    `${TASK_API}/api/buckets/${bucketId}/tasks/search?query=${encodeURIComponent(query)}`
  )
}

export function createTask(bucketId: number, taskName: string, taskDesc?: string) {
  return post<Task>(`${TASK_API}/api/buckets/${bucketId}/tasks`, { taskName, taskDesc })
}

export function updateTask(id: number, body: { taskName?: string; taskDesc?: string; isTaskDone?: boolean }) {
  return put<Task>(`${TASK_API}/api/tasks/${id}`, body)
}

export function markTaskDone(id: number) {
  return put<Task>(`${TASK_API}/api/tasks/${id}/done`)
}

export function deleteTask(id: number) {
  return del<{ message: string }>(`${TASK_API}/api/tasks/${id}`)
}
