import { post, get, USER_API } from './client'
import type { User } from '../types'

export function login(email: string, password: string) {
  return post<{ user: User }>(`${USER_API}/api/auth/login`, { email, password })
}

export function register(email: string, password: string, name: string) {
  return post<{ message: string }>(`${USER_API}/api/auth/register`, { email, password, name })
}

export function verifyEmail(token: string) {
  return get<{ message: string; user: User }>(`${USER_API}/api/auth/verify?token=${encodeURIComponent(token)}`)
}
