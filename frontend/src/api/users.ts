import { api } from './client'
import type { Role, UserResponse } from './types'

export interface CreateUserInput {
  username: string
  password: string
  displayName: string
  role: Role
}

export interface UpdateUserInput {
  displayName: string
  role: Role
  active: boolean
  password: string | null
}

export function fetchUsers(): Promise<UserResponse[]> {
  return api<UserResponse[]>('/api/users')
}

export function createUser(input: CreateUserInput): Promise<UserResponse> {
  return api<UserResponse>('/api/users', { method: 'POST', body: input })
}

export function updateUser(id: number, input: UpdateUserInput): Promise<UserResponse> {
  return api<UserResponse>(`/api/users/${id}`, { method: 'PUT', body: input })
}
