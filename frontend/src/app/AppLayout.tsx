import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { useAuth } from "../features/auth/useAuth";

export function AppLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <NavLink className="brand-lockup compact" to="/" aria-label="Sectio ana sayfa">
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>Sectio</span>
        </NavLink>

        <nav aria-label="Ana menü">
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} end to="/">
            Genel bakış
          </NavLink>
          {user?.role === "ADMIN" && (
            <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/users">
              Kullanıcılar
            </NavLink>
          )}
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/projects">Projeler</NavLink>
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/my-tasks">Görevlerim</NavLink>
        </nav>

        <div className="sidebar-user">
          <div className="avatar" aria-hidden="true">
            {user?.name.slice(0, 1).toUpperCase()}
          </div>
          <div>
            <strong>{user?.name}</strong>
            <span>{user?.role === "ADMIN" ? "Yönetici" : "Çalışan"}</span>
          </div>
          <button type="button" onClick={handleLogout}>Çıkış</button>
        </div>
      </aside>

      <main className="page-content">
        <Outlet />
      </main>
    </div>
  );
}
