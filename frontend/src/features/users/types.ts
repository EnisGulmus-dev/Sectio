import type { JobTitle, UserRole } from "../auth/types";

export type ManagedUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  jobTitle: JobTitle;
  active: boolean;
  mustChangePassword: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateUserInput = {
  name: string;
  email: string;
  temporaryPassword: string;
  role: UserRole;
  jobTitle: JobTitle;
};
