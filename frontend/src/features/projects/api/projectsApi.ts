import { apiRequest } from "../../../shared/api/httpClient";
import type { PageResponse, Project, ProjectMember, ProjectMemberCandidate, ProjectTeam, Task, TaskInput, TaskPriority, TaskStatus, TaskBucket, TaskProgress, TaskHandoff } from "../types";

export type TaskQuery = {
  query?: string;
  status?: TaskStatus | "";
  priority?: TaskPriority | "";
  assigneeId?: number | "";
  unassigned?: boolean;
  bucket?: TaskBucket;
  page?: number;
  size?: number;
};

function taskQueryString(filters: TaskQuery) {
  const params = new URLSearchParams();
  if (filters.query) params.set("query", filters.query);
  if (filters.status) params.set("status", filters.status);
  if (filters.priority) params.set("priority", filters.priority);
  if (filters.assigneeId) params.set("assigneeId", String(filters.assigneeId));
  if (filters.unassigned) params.set("unassigned", "true");
  if (filters.bucket) params.set("bucket", filters.bucket);
  params.set("page", String(filters.page ?? 0));
  params.set("size", String(filters.size ?? 20));
  return params.toString();
}

export const projectsApi = {
  list: (query = "", archived = false, page = 0) => apiRequest<PageResponse<Project>>(`/api/v1/projects?query=${encodeURIComponent(query)}&archived=${archived}&page=${page}`),
  get: (id: number) => apiRequest<Project>(`/api/v1/projects/${id}`),
  create: (input: { name: string; description: string; memberIds?: number[] }) => apiRequest<Project>("/api/v1/projects", { method: "POST", body: JSON.stringify(input) }),
  creationCandidates: () => apiRequest<ProjectMemberCandidate[]>("/api/v1/projects/member-candidates"),
  update: (id: number, input: { name: string; description: string }) => apiRequest<Project>(`/api/v1/projects/${id}`, { method: "PATCH", body: JSON.stringify(input) }),
  archive: (id: number) => apiRequest<Project>(`/api/v1/projects/${id}/archive`, { method: "POST" }),
  restore: (id: number) => apiRequest<Project>(`/api/v1/projects/${id}/restore`, { method: "POST" }),
  deleteProject: (id: number) => apiRequest<void>(`/api/v1/projects/${id}`, { method: "DELETE" }),
  members: (id: number) => apiRequest<ProjectMember[]>(`/api/v1/projects/${id}/members`),
  memberCandidates: (id: number) => apiRequest<ProjectMemberCandidate[]>(`/api/v1/projects/${id}/member-candidates`),
  addMembers: (id: number, userIds: number[]) => apiRequest<ProjectMember[]>(`/api/v1/projects/${id}/members`, { method: "POST", body: JSON.stringify({ userIds }) }),
  removeMember: (id: number, userId: number) => apiRequest<void>(`/api/v1/projects/${id}/members/${userId}`, { method: "DELETE" }),
  teams: (id: number) => apiRequest<ProjectTeam[]>(`/api/v1/projects/${id}/teams`),
  createTeam: (id: number, input: { name: string; userIds: number[] }) => apiRequest<ProjectTeam>(`/api/v1/projects/${id}/teams`, { method: "POST", body: JSON.stringify(input) }),
  updateTeam: (projectId: number, teamId: number, input: { name: string; userIds: number[] }) => apiRequest<ProjectTeam>(`/api/v1/projects/${projectId}/teams/${teamId}`, { method: "PATCH", body: JSON.stringify(input) }),
  deleteTeam: (projectId: number, teamId: number) => apiRequest<void>(`/api/v1/projects/${projectId}/teams/${teamId}`, { method: "DELETE" }),
  tasks: (id: number, filters: TaskQuery = {}) => apiRequest<PageResponse<Task>>(`/api/v1/projects/${id}/tasks?${taskQueryString(filters)}`),
  createTask: (id: number, input: TaskInput) => apiRequest<Task>(`/api/v1/projects/${id}/tasks`, { method: "POST", body: JSON.stringify(input) }),
  updateTask: (id: number, input: TaskInput) => apiRequest<Task>(`/api/v1/tasks/${id}`, { method: "PATCH", body: JSON.stringify(input) }),
  status: (id: number, status: TaskStatus, note?: string, expectedStatus?: TaskStatus) => apiRequest<Task>(`/api/v1/tasks/${id}/status`, { method: "PATCH", body: JSON.stringify({ status, note, expectedStatus }) }),
  progress: (id: number) => apiRequest<TaskProgress[]>(`/api/v1/tasks/${id}/progress`),
  successors: (id: number) => apiRequest<Task[]>(`/api/v1/tasks/${id}/successors`),
  handoffHistory: (id: number) => apiRequest<TaskHandoff[]>(`/api/v1/tasks/${id}/handoff-history`),
  handoff: (id: number, input: TaskInput) => apiRequest<Task>(`/api/v1/tasks/${id}/handoff`, { method: "POST", body: JSON.stringify(input) }),
  deleteTask: (id: number) => apiRequest<void>(`/api/v1/tasks/${id}`, { method: "DELETE" }),
  mine: (filters: TaskQuery = {}) => apiRequest<PageResponse<Task>>(`/api/v1/tasks/my?${taskQueryString(filters)}`),
};
