import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { useAuth } from "../features/auth/useAuth";
import { useTheme } from "../shared/ui/useTheme";

export function AppLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => localStorage.getItem("sectio-sidebar") === "collapsed");
  const { theme, toggleTheme } = useTheme();

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
    <div className={`app-shell${sidebarCollapsed ? " sidebar-collapsed" : ""}`} data-theme={theme}>
      <aside className="sidebar">
        <div className="sidebar-top"><NavLink className="brand-lockup compact sidebar-brand" to="/" aria-label="Sectio ana sayfa"><img className="brand-image sidebar-brand-wide" src={theme === "cloud" ? "/brand/01_Ana_Renkli_Yatay.png" : "/brand/03_Beyaz_Negatif_Yatay.png"} alt="Sectio" /><img className="brand-image sidebar-brand-mark" src="/brand/04_Sadece_S_Amblemi.png" alt="" /></NavLink><button className="sidebar-toggle" type="button" aria-label={sidebarCollapsed ? "Menüyü genişlet" : "Menüyü daralt"} aria-expanded={!sidebarCollapsed} onClick={toggleSidebar}>{sidebarCollapsed ? "›" : "‹"}</button></div>

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
          <button className="theme-toggle" type="button" onClick={toggleTheme} aria-label="Tema değiştir"><span className="sidebar-label">{theme === "midnight" ? "Açık temaya geç" : "Koyu temaya geç"}</span><span aria-hidden="true">{theme === "midnight" ? "☼" : "◐"}</span></button>
        </div>
      </aside>

      <main className="page-content">
        <Outlet />
      </main>
    </div>
  );
}
