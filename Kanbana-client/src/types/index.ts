export interface User {
  id: number
  email: string
  name: string
}

export interface Project {
  id: number
  projectName: string
  isPublic: boolean
  isShared: boolean
  ownerId: number
  hasViewPassword: boolean
  hasEditPassword: boolean
  createdAt: string
  updatedAt: string
}

export interface ProjectStats {
  buckets: number
  tasks: number
}

export interface ProjectWithStats {
  project: Project
  stats: ProjectStats
}

export interface Bucket {
  id: number
  bucketName: string
  projectId: number
  isDoneBucket: boolean
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface BucketStats {
  totalTasks: number
  completedTasks: number
  completionPercentage: number
}

export interface BucketWithStats {
  bucket: Bucket
  stats: BucketStats
}

export interface Task {
  id: number
  bucketId: number
  taskName: string
  taskDesc?: string
  isTaskDone: boolean
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface Permission {
  id: number
  projectId: number
  userId: number
  permissionLevel: 'READONLY' | 'EDIT'
  createdAt: string
}

export interface ChatParticipant {
  userId: number
  name: string | null
  email: string | null
}

export interface Message {
  id: number
  fromUserId: number
  chatId: number
  chatContent: string
  createdAt: string
  updatedAt: string
}

export interface Chat {
  id: number
  participants: ChatParticipant[]
  messages?: Message[]
  createdAt: string
  updatedAt: string
}

export interface UserSearchResult {
  id: number
  name: string
}

export type PermissionLevel = 'READONLY' | 'EDIT'
export type ToastType = 'success' | 'error' | 'info'
