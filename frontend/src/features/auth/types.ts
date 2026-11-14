export type UserRole = "ADMIN" | "EMPLOYEE";
export type JobTitle =
  | "SOFTWARE_DEVELOPER"
  | "SOFTWARE_ENGINEER"
  | "SENIOR_SOFTWARE_ENGINEER"
  | "TEAM_LEADER"
  | "PROJECT_MANAGER"
  | "PROJECT_OWNER";

export type CurrentUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  jobTitle: JobTitle;
  mustChangePassword: boolean;
};

export type LoginInput = {
  email: string;
  password: string;
};

export type ChangePasswordInput = {
  currentPassword: string;
  newPassword: string;
};

export type AuthStatus =
  | "loading"
  | "unauthenticated"
  | "authenticated"
  | "password-change-required";
