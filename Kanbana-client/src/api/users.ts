import { get, USER_API } from './client'
import type { UserSearchResult } from '../types'

export function searchUsers(query: string) {
  return get<{ results: UserSearchResult[]; count: number }>(
    `${USER_API}/api/user/search?query=${encodeURIComponent(query)}`
  )
}

export function searchUsersByName(query: string) {
  return get<{ results: UserSearchResult[]; count: number }>(
    `${USER_API}/api/user/search/name?query=${encodeURIComponent(query)}`
  )
}
