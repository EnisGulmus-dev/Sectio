import { apiRequest } from "../../../shared/api/httpClient";
import type { PageResponse, Project, ProjectMember, ProjectMemberCandidate, Task, TaskPriority, TaskStatus } from "../types";

export const projectsApi = {
  list: (q = "", archived = false, page = 0) => apiRequest<PageResponse<Project>>(`/api/v1/projects?query=${encodeURIComponent(q)}&archived=${archived}&page=${page}`),
  get: (id: number) => apiRequest<Project>(`/api/v1/projects/${id}`),
  create: (input: { name: string; description: string }) => apiRequest<Project>("/api/v1/projects", { method: "POST", body: JSON.stringify(input) }),
  update: (id: number, input: { name: string; description: string }) => apiRequest<Project>(`/api/v1/projects/${id}`, { method: "PATCH", body: JSON.stringify(input) }),
  archive: (id: number) => apiRequest<Project>(`/api/v1/projects/${id}/archive`, { method: "POST" }),
  members: (id: number) => apiRequest<ProjectMember[]>(`/api/v1/projects/${id}/members`),
  memberCandidates: (id: number) => apiRequest<ProjectMemberCandidate[]>(`/api/v1/projects/${id}/member-candidates`),
  addMember: (id: number, userId: number) => apiRequest<ProjectMember>(`/api/v1/projects/${id}/members`, { method: "POST", body: JSON.stringify({ userId }) }),
  removeMember: (id: number, userId: number) => apiRequest<void>(`/api/v1/projects/${id}/members/${userId}`, { method: "DELETE" }),
  tasks: (id: number) => apiRequest<PageResponse<Task>>(`/api/v1/projects/${id}/tasks`),
  createTask: (id: number, input: { title: string; description: string; assigneeId: number | null; priority: TaskPriority; dueAt: string | null }) => apiRequest<Task>(`/api/v1/projects/${id}/tasks`, { method: "POST", body: JSON.stringify(input) }),
  status: (id: number, status: TaskStatus) => apiRequest<Task>(`/api/v1/tasks/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
  deleteTask: (id: number) => apiRequest<void>(`/api/v1/tasks/${id}`, { method: "DELETE" }),
  mine: () => apiRequest<PageResponse<Task>>("/api/v1/tasks/my"),
};
