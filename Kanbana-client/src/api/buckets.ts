import { get, post, put, del, TASK_API } from './client'
import type { Bucket, BucketWithStats } from '../types'

export function listBuckets(projectId: number) {
  return get<{ buckets: BucketWithStats[]; count: number }>(
    `${TASK_API}/api/projects/${projectId}/buckets`
  )
}

export function createBucket(projectId: number, bucketName: string, isDoneBucket = false) {
  return post<Bucket>(`${TASK_API}/api/projects/${projectId}/buckets`, {
    bucketName,
    isDoneBucket,
  })
}

export function updateBucket(id: number, body: { bucketName?: string; isDoneBucket?: boolean }) {
  return put<Bucket>(`${TASK_API}/api/buckets/${id}`, body)
}

export function deleteBucket(id: number) {
  return del<{ message: string }>(`${TASK_API}/api/buckets/${id}`)
}
