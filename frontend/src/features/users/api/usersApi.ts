import { apiRequest } from "../../../shared/api/httpClient";
import type { UserRole } from "../../auth/types";
import type { CreateUserInput, ManagedUser } from "../types";

const basePath = "/api/v1/admin/users";

export const usersApi = {
  list: () => apiRequest<ManagedUser[]>(basePath),

  create: (input: CreateUserInput) =>
    apiRequest<ManagedUser>(basePath, {
      method: "POST",
      body: JSON.stringify(input),
    }),

  update: (id: number, input: { name: string; role: UserRole }) =>
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
};
