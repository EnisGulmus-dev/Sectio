import {
  createContext,
  type ReactNode,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { isApiClientError } from "../../shared/api/apiError";
import { authApi } from "./api/authApi";
import type {
  AuthStatus,
  ChangePasswordInput,
  CurrentUser,
  LoginInput,
} from "./types";

type AuthContextValue = {
  status: AuthStatus;
  user: CurrentUser | null;
  startupError: string | null;
  login: (input: LoginInput) => Promise<CurrentUser>;
  changePassword: (input: ChangePasswordInput) => Promise<CurrentUser>;
  logout: () => Promise<void>;
  reloadSession: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | null>(null);

function statusFor(user: CurrentUser): AuthStatus {
  return user.mustChangePassword
    ? "password-change-required"
    : "authenticated";
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [startupError, setStartupError] = useState<string | null>(null);

  const reloadSession = useCallback(async () => {
    setStatus("loading");
    setStartupError(null);

    try {
      const currentUser = await authApi.currentUser();
      setUser(currentUser);
      setStatus(statusFor(currentUser));
    } catch (error) {
      setUser(null);
      if (isApiClientError(error) && error.status === 401) {
        setStatus("unauthenticated");
        return;
      }

      setStartupError(
        "Oturum bilgisi alınamadı. Backend bağlantısını kontrol edip tekrar deneyin.",
      );
      setStatus("unauthenticated");
    }
  }, []);

  useEffect(() => {
    void reloadSession();
  }, [reloadSession]);

  const login = useCallback(async (input: LoginInput) => {
    const currentUser = await authApi.login(input);
    setUser(currentUser);
    setStatus(statusFor(currentUser));
    return currentUser;
  }, []);

  const changePassword = useCallback(async (input: ChangePasswordInput) => {
    const currentUser = await authApi.changePassword(input);
    setUser(currentUser);
    setStatus(statusFor(currentUser));
    return currentUser;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
      setStatus("unauthenticated");
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      startupError,
      login,
      changePassword,
      logout,
      reloadSession,
    }),
    [
      status,
      user,
      startupError,
      login,
      changePassword,
      logout,
      reloadSession,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
