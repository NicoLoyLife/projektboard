import { api, refreshCsrfToken } from './client'
import type { Me } from './types'

export async function fetchMe(): Promise<Me> {
  return api<Me>('/api/auth/me', { ignoreUnauthorized: true })
}

export async function login(username: string, password: string): Promise<Me> {
  const me = await api<Me>('/api/auth/login', {
    method: 'POST',
    body: { username, password },
    ignoreUnauthorized: true,
  })
  // Das Backend erneuert das CSRF-Token nach der Anmeldung.
  await refreshCsrfToken()
  return me
}

export async function logout(): Promise<void> {
  await api<void>('/api/auth/logout', { method: 'POST' })
  await refreshCsrfToken()
}
