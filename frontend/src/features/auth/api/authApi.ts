import {
  apiRequest,
  clearCsrfToken,
  refreshCsrfToken,
} from "../../../shared/api/httpClient";
import type {
  ChangePasswordInput,
  CurrentUser,
  LoginInput,
} from "../types";

export const authApi = {
  currentUser() {
    return apiRequest<CurrentUser>("/api/v1/auth/me");
  },

  async login(input: LoginInput) {
    const user = await apiRequest<CurrentUser>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(input),
    });

    // Login rotates the session and invalidates the pre-login CSRF token.
    clearCsrfToken();
    await refreshCsrfToken();
    return user;
  },

  changePassword(input: ChangePasswordInput) {
    return apiRequest<CurrentUser>("/api/v1/auth/change-password", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async logout() {
    try {
      await apiRequest<void>("/api/v1/auth/logout", { method: "POST" });
    } finally {
      clearCsrfToken();
    }
  },
};
