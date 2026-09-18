// Typen passend zu den Records des Backends.

export type Role = 'ADMIN' | 'PROJECT_MANAGER' | 'EMPLOYEE'
export type ProjectStatus = 'ACTIVE' | 'ARCHIVED'
export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE'

export interface Me {
  id: number
  username: string
  displayName: string
  role: Role
}

export interface UserSummary {
  id: number
  displayName: string
}

export interface UserResponse {
  id: number
  username: string
  displayName: string
  role: Role
  active: boolean
}

export interface TaskCounts {
  total: number
  open: number
  inProgress: number
  done: number
}

export interface ProjectSummary {
  id: number
  name: string
  status: ProjectStatus
  manager: UserSummary
  counts: TaskCounts
  progressPercent: number
  createdAt: string
}

export interface TaskResponse {
  id: number
  title: string
  description: string | null
  status: TaskStatus
  assignee: UserSummary | null
  dueDate: string | null
  createdAt: string
  updatedAt: string
}

export interface ProjectDetail {
  id: number
  name: string
  description: string | null
  status: ProjectStatus
  manager: UserSummary
  members: UserSummary[]
  counts: TaskCounts
  progressPercent: number
  tasks: TaskResponse[]
  createdAt: string
  canManage: boolean
  canEditTasks: boolean
}

export interface FieldError {
  field: string
  message: string
}

export interface ErrorResponse {
  error: string
  message?: string
  errors?: FieldError[]
}

export const roleLabels: Record<Role, string> = {
  ADMIN: 'Administration',
  PROJECT_MANAGER: 'Projektleitung',
  EMPLOYEE: 'Mitarbeitende',
}

export const taskStatusLabels: Record<TaskStatus, string> = {
  OPEN: 'offen',
  IN_PROGRESS: 'in Bearbeitung',
  DONE: 'erledigt',
}

export const projectStatusLabels: Record<ProjectStatus, string> = {
  ACTIVE: 'aktiv',
  ARCHIVED: 'archiviert',
}
