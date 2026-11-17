import { apiRequest } from "../../../shared/api/httpClient";
import type { JobTitle, UserRole } from "../../auth/types";
import type { CreateUserInput, ManagedUser, UserDeletionImpact, UserWorkHistoryEntry, UserWorkTrackingEntry, UserWorkTrackingScope } from "../types";
import type { PageResponse, TaskStatus } from "../../projects/types";

const basePath = "/api/v1/admin/users";

export const usersApi = {
  list: (deleted = false) => apiRequest<ManagedUser[]>(`${basePath}?deleted=${deleted}`),

  workHistory: (id: number, page = 0) =>
    apiRequest<PageResponse<UserWorkHistoryEntry>>(`${basePath}/${id}/work-history?page=${page}&size=20`),

  workTracking: (id: number, filters: { page: number; scope: UserWorkTrackingScope; status: TaskStatus | ""; query: string }) => {
    const parameters = new URLSearchParams({ page: String(filters.page), size: "20", scope: filters.scope });
    if (filters.status) parameters.set("status", filters.status);
    if (filters.query) parameters.set("query", filters.query);
    return apiRequest<PageResponse<UserWorkTrackingEntry>>(`${basePath}/${id}/work-tracking?${parameters}`);
  },

  create: (input: CreateUserInput) =>
    apiRequest<ManagedUser>(basePath, {
      method: "POST",
      body: JSON.stringify(input),
    }),

  update: (id: number, input: { name: string; email: string; role: UserRole; jobTitle: JobTitle; managerId?: number | null; teamIds?: number[] }) =>
    apiRequest<ManagedUser>(`${basePath}/${id}`, {
      method: "PATCH",
      body: JSON.stringify(input),
    }),

  changeStatus: (id: number, active: boolean) =>
    apiRequest<ManagedUser>(`${basePath}/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ active }),
    }),

  resetPassword: (id: number, temporaryPassword: string) =>
    apiRequest<ManagedUser>(`${basePath}/${id}/reset-password`, {
      method: "POST",
      body: JSON.stringify({ temporaryPassword }),
    }),

  deleteUser: (id: number) =>
    apiRequest<void>(`${basePath}/${id}`, { method: "DELETE" }),

  restore: (id: number, temporaryPassword: string) =>
    apiRequest<ManagedUser>(`${basePath}/${id}/restore`, { method: "POST", body: JSON.stringify({ temporaryPassword }) }),

  purge: (id: number) => apiRequest<void>(`${basePath}/${id}/purge`, { method: "DELETE" }),

  deletionImpact: (id: number) =>
    apiRequest<UserDeletionImpact>(`${basePath}/${id}/deletion-impact`),
};
