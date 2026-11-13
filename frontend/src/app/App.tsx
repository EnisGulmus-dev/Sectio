import { Route, Routes } from "react-router-dom";

import { AuthProvider } from "../features/auth/AuthProvider";
import { AppLayout } from "./AppLayout";
import { ChangePasswordPage } from "../features/auth/pages/ChangePasswordPage";
import { DashboardPage } from "../features/auth/pages/DashboardPage";
import { LoginPage } from "../features/auth/pages/LoginPage";
import {
  PublicOnlyRoute,
  RequireAdminRoute,
  RequireAuthenticatedRoute,
  RequirePasswordChangeRoute,
} from "../features/auth/routes/AuthRoutes";
import { UsersPage } from "../features/users/pages/UsersPage";
import { ErrorBoundary } from "../shared/ui/ErrorBoundary";
import { ToastProvider } from "../shared/ui/ToastProvider";
import { NotFoundPage } from "./NotFoundPage";

export function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/login" element={<LoginPage />} />
            </Route>

            <Route element={<RequirePasswordChangeRoute />}>
              <Route path="/change-password" element={<ChangePasswordPage />} />
            </Route>

            <Route element={<RequireAuthenticatedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/" element={<DashboardPage />} />
                <Route element={<RequireAdminRoute />}>
                  <Route path="/users" element={<UsersPage />} />
                </Route>
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Route>
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}
