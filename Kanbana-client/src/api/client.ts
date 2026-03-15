const USER_API = 'http://localhost:8080'
const TASK_API = 'http://localhost:8081'
const CHAT_API = 'http://localhost:3001'

export { USER_API, TASK_API, CHAT_API }

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (res.status === 401) {
    localStorage.removeItem('kanbana_user')
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }

  const text = await res.text()
  let data: unknown
  try {
    data = text ? JSON.parse(text) : {}
  } catch {
    data = {}
  }

  if (!res.ok) {
    const err = data as { message?: string; error?: string }
    throw new Error(err.message || err.error || `Request failed: ${res.status}`)
  }

  return data as T
}

export function get<T>(url: string): Promise<T> {
  return request<T>(url)
}

export function post<T>(url: string, body?: unknown): Promise<T> {
  return request<T>(url, {
    method: 'POST',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
}

export function put<T>(url: string, body?: unknown): Promise<T> {
  return request<T>(url, {
    method: 'PUT',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
}

export function del<T>(url: string): Promise<T> {
  return request<T>(url, { method: 'DELETE' })
}
