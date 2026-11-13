export type UserRole = "ADMIN" | "EMPLOYEE";

export type CurrentUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
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
