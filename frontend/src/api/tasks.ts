import { api } from './client'
import type { TaskResponse, TaskStatus } from './types'

export interface TaskInput {
  title: string
  description: string
  assigneeId: number | null
  dueDate: string | null
}

export function createTask(projectId: number, input: TaskInput): Promise<TaskResponse> {
  return api<TaskResponse>(`/api/projects/${projectId}/tasks`, { method: 'POST', body: input })
}

export function updateTask(taskId: number, input: TaskInput): Promise<TaskResponse> {
  return api<TaskResponse>(`/api/tasks/${taskId}`, { method: 'PUT', body: input })
}

export function changeTaskStatus(taskId: number, status: TaskStatus): Promise<TaskResponse> {
  return api<TaskResponse>(`/api/tasks/${taskId}/status`, { method: 'PATCH', body: { status } })
}
