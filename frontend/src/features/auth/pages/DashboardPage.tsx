import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest } from "../../../shared/api/httpClient";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { formatDate, statusLabels } from "../../projects/projectFormat";
import type { TaskStatus } from "../../projects/types";
import { jobTitleLabels } from "../jobTitles";
import { useAuth } from "../useAuth";

type Workspace = {
  openTasks: number; completedTasks: number; overdueTasks: number;
  deadlines: { id: number; title: string; projectId: number; projectName: string; dueAt: string | null; status: TaskStatus }[];
  projects: { id: number; name: string; total: number; completed: number; cancelled: number }[];
  teams: { id: number; name: string; members: number; open: number; overdue: number }[];
  system: { users: number; activeUsers: number; activeProjects: number; archivedProjects: number } | null;
};

function PersonalNotes({ userId }: { userId: number }) {
  const key = `sectio-notes-${userId}`;
  const [note, setNote] = useState(() => { try { return localStorage.getItem(key) ?? ""; } catch { return ""; } });
  const [saveState, setSaveState] = useState("");
  function updateNote(value: string) {
    setNote(value);
    try { localStorage.setItem(key, value); setSaveState("Bu tarayıcıya kaydedildi"); }
    catch { setSaveState("Kaydedilemedi; tarayıcı depolamasını kontrol edin"); }
  }
  return <section className="panel dashboard-notes"><header className="section-heading"><div><h2>Kişisel notlarım</h2><p>Günün odağı, hatırlatmalar ve fikirler.</p></div></header><label className="sr-only" htmlFor="dashboard-notes">Kişisel notlarım</label><textarea id="dashboard-notes" rows={8} maxLength={10000} placeholder="Bugün neye odaklanacaksın?" value={note} onChange={(event) => updateNote(event.target.value)} /><small aria-live="polite">{saveState} · {note.length}/10000</small><p className="dashboard-help">Notlar bu cihaz ve tarayıcıda, hesabına özel tutulur.</p></section>;
}

export function DashboardPage() {
  const { user } = useAuth();
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reload, setReload] = useState(0);
  const admin = user?.role === "ADMIN";
  const leader = !admin && user?.jobTitle === "TEAM_LEADER";
  const manager = !admin && (user?.jobTitle === "PROJECT_MANAGER" || user?.jobTitle === "PROJECT_OWNER");

  useEffect(() => {
    if (!user) return;
    const controller = new AbortController();
    setError(null); setWorkspace(null);
    apiRequest<Workspace>("/api/v1/dashboard/workspace", { signal: controller.signal })
      .then((data) => { if (!controller.signal.aborted) setWorkspace(data); })
      .catch(() => { if (!controller.signal.aborted) setError("Çalışma alanı yüklenemedi."); });
    return () => controller.abort();
  }, [user?.id, user?.jobTitle, user?.role, reload]);

  return <div className="page-stack">
    <header className="dashboard-header"><div><p className="eyebrow">{admin ? "Sistem yönetimi" : leader ? "Ekip çalışma alanı" : manager ? "Proje yönetimi" : "Kişisel çalışma alanı"}</p><h1>Merhaba, {user?.name.split(" ")[0]}</h1><p>{admin ? "Kullanıcılar, projeler ve sistem genelindeki görevlerin özeti." : leader ? "Takımlarının iş yükünü ve yaklaşan teslimlerini takip et." : manager ? "Projelerinin ilerlemesini ve teslim tarihlerini takip et." : "Görevlerini planla, yaklaşan teslim tarihlerini takip et."}</p></div><span className="role-badge">{admin ? "Sistem yöneticisi" : user && jobTitleLabels[user.jobTitle]}</span></header>
    {error && <div><InlineAlert message={error} /><button className="secondary-button" onClick={() => setReload((value) => value + 1)}>Tekrar dene</button></div>}
    {!workspace && !error && <LoadingState />}
    {workspace && <>
      <section className="status-grid" aria-label="Görev özeti">{[
        { label: "Açık görev", value: workspace.openTasks, style: "open" },
        { label: "Tamamlanan", value: workspace.completedTasks, style: "completed" },
        { label: "Geciken", value: workspace.overdueTasks, style: "overdue" },
      ].map((metric, index) => <article className={`status-card status-${metric.style}`} key={metric.style}><span className="card-index">0{index + 1}</span><h2>{metric.label}</h2><strong className="metric-value">{metric.value}</strong></article>)}</section>
      {admin && workspace.system && <section className="panel"><header className="section-heading"><div><h2>Sistem özeti</h2><p>Kayıtlı kullanıcı ve proje sayıları.</p></div><Link to="/users">Kullanıcıları yönet</Link></header><dl className="dashboard-system">{[["Toplam kullanıcı", workspace.system.users], ["Aktif kullanıcı", workspace.system.activeUsers], ["Aktif proje", workspace.system.activeProjects], ["Arşiv proje", workspace.system.archivedProjects]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></section>}
      {leader && <section className="panel"><header className="section-heading"><div><h2>Ekip iş yükü</h2><p>Dahil olduğun veya proje sahibi olarak yönettiğin takımlar. Ortak görevler her takımda bir kez sayılır.</p></div></header>{workspace.teams.length === 0 ? <EmptyState title="Henüz takım bulunmuyor" /> : <div className="dashboard-team-grid">{workspace.teams.map((team) => <article key={team.id}><h3>{team.name}</h3><p>{team.members} üye</p><strong>{team.open} açık görev</strong><small>{team.overdue} geciken görev</small></article>)}</div>}</section>}
      {(manager || admin) && <section className="panel"><header className="section-heading"><div><h2>Proje ilerlemesi</h2><p>Son güncellenen 8 aktif proje. İptal görevleri oran hesabına dahil edilmez.</p></div><Link to="/projects">Tüm projeler</Link></header>{workspace.projects.length === 0 ? <EmptyState title="Henüz aktif proje yok" /> : <div className="dashboard-projects">{workspace.projects.map((project) => {
        const total = project.total - project.cancelled;
        const percent = total > 0 ? Math.round(project.completed / total * 100) : 0;
        return <article key={project.id}><div><Link to={`/projects/${project.id}`}>{project.name}</Link><span>{project.completed}/{total} görev · %{percent}</span></div><progress max={100} value={percent} aria-label={`${project.name} tamamlanma oranı`} /></article>;
      })}</div>}</section>}
    </>}
    <div className="dashboard-columns"><section className="panel"><header className="section-heading"><div><h2>{leader || manager || admin ? "Teslim takibi" : "Görevlerim ve yaklaşan tarihler"}</h2><p>En yakın tarihli 8 açık görev; gecikenler önce gösterilir.</p></div></header>{workspace ? workspace.deadlines.length === 0 ? <EmptyState title="Bekleyen görev bulunmuyor" /> : <div className="dashboard-deadlines">{workspace.deadlines.map((task) => <article key={task.id}><div><Link to={`/projects/${task.projectId}?tab=tasks`}>{task.title}</Link><small>{task.projectName} · {statusLabels[task.status]}</small></div><time className={task.dueAt && new Date(task.dueAt) < new Date() ? "dashboard-overdue" : ""} dateTime={task.dueAt ?? undefined}>{task.dueAt ? formatDate(task.dueAt) : "Tarih belirlenmedi"}</time></article>)}</div> : <p className="dashboard-help">{error ? "Görev bilgileri yüklenemedi." : "Görevler yükleniyor…"}</p>}<Link className="primary-link" to="/my-tasks">Görevlerime git</Link></section>{user && <PersonalNotes key={user.id} userId={user.id} />}</div>
  </div>;
}
