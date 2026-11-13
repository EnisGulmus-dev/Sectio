import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAuth } from "../useAuth";

function LoadingScreen() {
  return (
    <main className="loading-screen" aria-live="polite">
      <div className="brand-mark" aria-hidden="true">
        S
      </div>
      <p>Oturum hazırlanıyor…</p>
    </main>
  );
}

export function PublicOnlyRoute() {
  const { status } = useAuth();

  if (status === "loading") return <LoadingScreen />;
  if (status === "password-change-required") {
    return <Navigate to="/change-password" replace />;
  }
  if (status === "authenticated") return <Navigate to="/" replace />;
  return <Outlet />;
}

export function RequirePasswordChangeRoute() {
  const { status } = useAuth();

  if (status === "loading") return <LoadingScreen />;
  if (status === "unauthenticated") return <Navigate to="/login" replace />;
  if (status === "authenticated") return <Navigate to="/" replace />;
  return <Outlet />;
}

export function RequireAuthenticatedRoute() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "loading") return <LoadingScreen />;
  if (status === "unauthenticated") {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (status === "password-change-required") {
    return <Navigate to="/change-password" replace />;
  }
  return <Outlet />;
}

export function RequireAdminRoute() {
  const { user } = useAuth();

  if (user?.role !== "ADMIN") return <Navigate to="/" replace />;
  return <Outlet />;
}
