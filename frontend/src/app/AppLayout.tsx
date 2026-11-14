import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { useAuth } from "../features/auth/useAuth";

export function AppLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => localStorage.getItem("sectio-sidebar") === "collapsed");

  function toggleSidebar() {
    setSidebarCollapsed((current) => {
      localStorage.setItem("sectio-sidebar", current ? "expanded" : "collapsed");
      return !current;
    });
  }

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className={`app-shell${sidebarCollapsed ? " sidebar-collapsed" : ""}`}>
      <aside className="sidebar">
        <div className="sidebar-top"><NavLink className="brand-lockup compact" to="/" aria-label="Sectio ana sayfa"><span className="brand-mark" aria-hidden="true">S</span><span className="sidebar-label">Sectio</span></NavLink><button className="sidebar-toggle" type="button" aria-label={sidebarCollapsed ? "Menüyü genişlet" : "Menüyü daralt"} aria-expanded={!sidebarCollapsed} onClick={toggleSidebar}>{sidebarCollapsed ? "›" : "‹"}</button></div>

        <nav aria-label="Ana menü">
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} end to="/">
            <span className="nav-symbol" aria-hidden="true">⌂</span><span className="sidebar-label">Genel bakış</span>
          </NavLink>
          {user?.role === "ADMIN" && (
            <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/users">
              <span className="nav-symbol" aria-hidden="true">◎</span><span className="sidebar-label">Kullanıcılar</span>
            </NavLink>
          )}
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/projects"><span className="nav-symbol" aria-hidden="true">◇</span><span className="sidebar-label">Projeler</span></NavLink>
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/my-tasks"><span className="nav-symbol" aria-hidden="true">✓</span><span className="sidebar-label">Görevlerim</span></NavLink>
        </nav>

        <div className="sidebar-user">
          <div className="avatar" aria-hidden="true">
            {user?.name.slice(0, 1).toUpperCase()}
          </div>
          <div className="sidebar-label">
            <strong>{user?.name}</strong>
            <span>{user?.role === "ADMIN" ? "Yönetici" : "Çalışan"}</span>
          </div>
          <button type="button" aria-label="Çıkış yap" onClick={handleLogout}><span className="sidebar-label">Çıkış</span><span className="collapsed-only" aria-hidden="true">↪</span></button>
        </div>
      </aside>

      <main className="page-content">
        <Outlet />
      </main>
    </div>
  );
}
