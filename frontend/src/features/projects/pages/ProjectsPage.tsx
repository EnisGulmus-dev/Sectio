import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";

import { useAuth } from "../../auth/useAuth";
import { canCreateProjects } from "../../auth/jobTitles";
import { isApiClientError } from "../../../shared/api/apiError";
import { ConfirmDialog } from "../../../shared/ui/ConfirmDialog";
import { ModalPanel } from "../../../shared/ui/ModalPanel";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { useToast } from "../../../shared/ui/ToastProvider";
import { projectsApi } from "../api/projectsApi";
import { formatDate } from "../projectFormat";
import type { Project } from "../types";

type ProjectAction = { type: "archive" | "restore" | "delete"; project: Project } | null;

export function ProjectsPage() {
  const [items, setItems] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [archived, setArchived] = useState(false);
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [form, setForm] = useState({ name: "", description: "" });
  const [projectAction, setProjectAction] = useState<ProjectAction>(null);
  const [busy, setBusy] = useState(false);
  const { user } = useAuth();
  const { showToast } = useToast();
  const canCreateProject = !!user && canCreateProjects(user.role, user.jobTitle);

  async function load(targetPage = page) {
    setLoading(true);
    try {
      const response = await projectsApi.list(query, archived, targetPage);
      setItems(response.content);
      setPage(response.page);
      setTotalPages(response.totalPages);
      setError(null);
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Projeler yüklenemedi"); }
    finally { setLoading(false); }
  }

  useEffect(() => { setPage(0); void load(0); }, [archived]);

  async function create(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      await projectsApi.create(form);
      setForm({ name: "", description: "" });
      setShowForm(false);
      await load(0);
      showToast("Proje oluşturuldu");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Proje oluşturulamadı"); }
    finally { setBusy(false); }
  }

  async function confirmProjectAction() {
    if (!projectAction) return;
    setBusy(true);
    try {
      if (projectAction.type === "archive") {
        await projectsApi.archive(projectAction.project.id);
        showToast("Proje arşivlendi");
      } else if (projectAction.type === "restore") {
        await projectsApi.restore(projectAction.project.id);
        showToast("Proje geri yüklendi");
      } else {
        await projectsApi.deleteProject(projectAction.project.id);
        showToast("Proje kalıcı olarak silindi");
      }
      setProjectAction(null);
      await load(page);
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Proje işlemi tamamlanamadı"); }
    finally { setBusy(false); }
  }

  const actionCopy = projectAction?.type === "restore"
    ? { title: "Proje geri yüklensin mi?", description: `“${projectAction.project.name}” yeniden aktif olacak ve düzenlenebilecek.`, label: "Projeyi geri yükle" }
    : projectAction?.type === "delete"
      ? { title: "Proje kalıcı olarak silinsin mi?", description: `“${projectAction.project.name}” ile birlikte üyelikler ve görevler geri alınamayacak şekilde silinecek.`, label: "Kalıcı olarak sil" }
      : { title: "Proje arşivlensin mi?", description: `“${projectAction?.project.name ?? "Seçilen proje"}” salt okunur hale gelecek; görev ve üye değişiklikleri durdurulacak.`, label: "Projeyi arşivle" };

  return <div className="page-stack">
    <header className="page-header"><div><p className="eyebrow">Çalışma alanı</p><h1>Projeler</h1><p>Projeleri, üyeleri ve görevleri tek yerden yönetin.</p></div>{canCreateProject && <button className="primary-button compact-button" aria-haspopup="dialog" onClick={() => setShowForm(true)}>Yeni proje</button>}</header>
    {error && <InlineAlert message={error} />}
    <form className="filter-row" onSubmit={(event) => { event.preventDefault(); void load(0); }}><input aria-label="Proje ara" placeholder="Proje adıyla ara" value={query} onChange={(event) => setQuery(event.target.value)} /><button className="secondary-button">Ara</button><label><input type="checkbox" checked={archived} onChange={(event) => setArchived(event.target.checked)} /> Arşivlenenleri göster</label></form>
    <ModalPanel open={showForm && !!canCreateProject} title="Yeni proje" onClose={() => setShowForm(false)}><p className="modal-description">Proje sahibi olarak otomatik biçimde üye listesine eklenirsiniz.</p><form className="management-form" onSubmit={create}><label>Proje adı<input autoFocus required maxLength={150} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label><label className="full-field">Açıklama<textarea rows={3} maxLength={1000} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><button className="primary-button compact-button" disabled={busy}>{busy ? "Oluşturuluyor…" : "Oluştur"}</button></form></ModalPanel>
    {loading ? <LoadingState /> : items.length === 0 ? <EmptyState title="Proje bulunamadı" description={query ? "Arama ifadenizi değiştirip yeniden deneyin." : "Yeni proje oluşturarak başlayın."} /> : <><div className="project-grid">{items.map((project) => { const canManage = user?.role === "ADMIN" || user?.id === project.ownerId; return <article className="panel project-card" key={project.id}><div><div className="task-card-top"><span className={`status-label ${project.archived ? "inactive" : "active"}`}>{project.archived ? "Arşiv" : "Aktif"}</span><small>{formatDate(project.updatedAt)}</small></div><h2>{project.name}</h2><p>{project.description || "Bu proje için açıklama eklenmedi."}</p><small>Sahip: {project.ownerName}</small></div><div className="button-row project-actions"><Link className="primary-link" to={`/projects/${project.id}`}>Projeyi aç</Link>{canManage && !project.archived && <button className="secondary-button" type="button" onClick={() => setProjectAction({ type: "archive", project })}>Arşivle</button>}{canManage && project.archived && <button className="secondary-button" type="button" onClick={() => setProjectAction({ type: "restore", project })}>Geri yükle</button>}{canManage && <button className="text-button" type="button" onClick={() => setProjectAction({ type: "delete", project })}>Kalıcı sil</button>}</div></article>; })}</div>{totalPages > 1 && <nav className="pagination" aria-label="Proje sayfaları"><button className="secondary-button" disabled={page === 0} onClick={() => void load(page - 1)}>Önceki</button><span>{page + 1} / {totalPages}</span><button className="secondary-button" disabled={page + 1 >= totalPages} onClick={() => void load(page + 1)}>Sonraki</button></nav>}</>}
    <ConfirmDialog open={!!projectAction} title={actionCopy.title} description={actionCopy.description} confirmLabel={actionCopy.label} tone={projectAction?.type === "restore" ? "primary" : "danger"} busy={busy} onConfirm={() => void confirmProjectAction()} onCancel={() => setProjectAction(null)} />
  </div>;
}
