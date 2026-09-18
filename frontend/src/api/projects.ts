import { api } from './client'
import type { ProjectDetail, ProjectStatus, ProjectSummary, UserSummary } from './types'

export function fetchProjects(status?: ProjectStatus): Promise<ProjectSummary[]> {
  const query = status ? `?status=${status}` : ''
  return api<ProjectSummary[]>(`/api/projects${query}`)
}

export function fetchProject(id: number): Promise<ProjectDetail> {
  return api<ProjectDetail>(`/api/projects/${id}`)
}

export function createProject(name: string, description: string): Promise<ProjectDetail> {
  return api<ProjectDetail>('/api/projects', { method: 'POST', body: { name, description } })
}

export function updateProject(id: number, name: string, description: string): Promise<ProjectDetail> {
  return api<ProjectDetail>(`/api/projects/${id}`, { method: 'PUT', body: { name, description } })
}

export function changeProjectStatus(id: number, status: ProjectStatus): Promise<ProjectDetail> {
  return api<ProjectDetail>(`/api/projects/${id}/status`, { method: 'PATCH', body: { status } })
}

export function setProjectMembers(id: number, userIds: number[]): Promise<ProjectDetail> {
  return api<ProjectDetail>(`/api/projects/${id}/members`, { method: 'PUT', body: { userIds } })
}

export function fetchMemberCandidates(id: number): Promise<UserSummary[]> {
  return api<UserSummary[]>(`/api/projects/${id}/member-candidates`)
}
