import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { useAuth } from "../features/auth/useAuth";
import { useTheme } from "../shared/ui/useTheme";
import styles from "./AppLayout.module.css";
import { profileApi, type Profile } from "../features/profile/profileApi";
import { avatarUrl } from "../features/profile/avatarCatalog";

export function AppLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => localStorage.getItem("sectio-sidebar") === "collapsed");
  const { theme, toggleTheme } = useTheme();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [photoVersion, setPhotoVersion] = useState(0);
  const [imageFailed, setImageFailed] = useState(false);
  useEffect(() => {
    let live = true; setProfile(null);
    function refresh() { void profileApi.get().then((value) => { if (live) { setProfile(value); setPhotoVersion(Date.now()); setImageFailed(false); } }).catch(() => { if (live) setProfile(null); }); }
    if (user) refresh();
    window.addEventListener("sectio-profile-updated", refresh);
    return () => { live = false; window.removeEventListener("sectio-profile-updated", refresh); };
  }, [user?.id]);
  const picture = profile?.hasPhoto ? `/api/v1/profile/${profile.userId}/photo?v=${photoVersion}` : profile?.avatarId ? avatarUrl(profile.avatarId) : null;

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
        <div className="sidebar-top"><NavLink className="brand-lockup compact sidebar-brand" to="/" aria-label="Sectio ana sayfa"><img className="brand-image sidebar-brand-wide" src={theme === "cloud" ? "/brand/01_Ana_Renkli_Yatay.png" : "/brand/03_Beyaz_Negatif_Yatay.png"} alt="Sectio" /><img className={`brand-image sidebar-brand-mark ${theme === "midnight" ? styles.whiteMark : ""}`} src="/brand/04_Sadece_S_Amblemi.png" alt="" /></NavLink><button className="sidebar-toggle" type="button" aria-label={sidebarCollapsed ? "Menüyü genişlet" : "Menüyü daralt"} aria-expanded={!sidebarCollapsed} onClick={toggleSidebar}>{sidebarCollapsed ? "›" : "‹"}</button></div>

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
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/channels"><span className="nav-symbol" aria-hidden="true">#</span><span className="sidebar-label">Kanallar</span></NavLink>
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/organization"><span className="nav-symbol" aria-hidden="true">◇</span><span className="sidebar-label">Organizasyon</span></NavLink>
          <NavLink className={({ isActive }) => `nav-item${isActive ? " active" : ""}`} to="/profile"><span className="nav-symbol" aria-hidden="true">◉</span><span className="sidebar-label">Profilim</span></NavLink>
        </nav>

        <div className="sidebar-user">
          <div className={`avatar ${styles.profileAvatar}`} aria-hidden="true">
            {picture && !imageFailed ? <img className={styles.userImage} src={picture} alt="" onError={() => setImageFailed(true)} /> : (profile?.displayName || user?.name)?.slice(0, 1).toLocaleUpperCase("tr-TR")}
          </div>
          <div className="sidebar-label">
            <strong>{profile?.displayName || user?.name}</strong>
            <span>{user?.role === "ADMIN" ? "Yönetici" : "Çalışan"}</span>
          </div>
          <div className={styles.sidebarActions}><button type="button" aria-label={theme === "midnight" ? "Açık temaya geç" : "Koyu temaya geç"} title={theme === "midnight" ? "Açık temaya geç" : "Koyu temaya geç"} onClick={toggleTheme}><span aria-hidden="true">{theme === "midnight" ? "☼" : "☾"}</span></button><button type="button" aria-label="Çıkış yap" title="Çıkış yap" onClick={handleLogout}><span aria-hidden="true">⏻</span></button></div>
        </div>
      </aside>

      <main className="page-content">
        <Outlet />
      </main>
    </div>
  );
}
