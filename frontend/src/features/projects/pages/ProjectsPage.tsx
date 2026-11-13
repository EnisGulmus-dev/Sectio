import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";

import { useAuth } from "../../auth/useAuth";
import { isApiClientError } from "../../../shared/api/apiError";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { useToast } from "../../../shared/ui/ToastProvider";
import { projectsApi } from "../api/projectsApi";
import type { Project } from "../types";

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
  const { user } = useAuth();
  const { showToast } = useToast();

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
    try {
      await projectsApi.create(form);
      setForm({ name: "", description: "" });
      setShowForm(false);
      await load(0);
      showToast("Proje oluşturuldu");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Proje oluşturulamadı"); }
  }

  async function archive(project: Project) {
    try {
      await projectsApi.archive(project.id);
      await load(page);
      showToast("Proje arşivlendi");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Proje arşivlenemedi"); }
  }

  return <div className="page-stack">
    <header className="page-header"><div><p className="eyebrow">Çalışma alanı</p><h1>Projeler</h1><p>Projeleri, üyeleri ve görevleri tek yerden yönetin.</p></div><button className="primary-button compact-button" onClick={() => setShowForm(!showForm)}>Yeni proje</button></header>
    {error && <InlineAlert message={error} />}
    <div className="filter-row"><input aria-label="Proje ara" placeholder="Proje ara" value={query} onChange={(event) => setQuery(event.target.value)} /><button className="secondary-button" onClick={() => void load(0)}>Ara</button><label><input type="checkbox" checked={archived} onChange={(event) => setArchived(event.target.checked)} /> Arşivlenenler</label></div>
    {showForm && <form className="panel management-form" onSubmit={create}><label>Proje adı<input required maxLength={150} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label><label>Açıklama<input maxLength={1000} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><button className="primary-button compact-button">Oluştur</button></form>}
    {loading ? <LoadingState /> : items.length === 0 ? <EmptyState title="Proje bulunamadı" description="Yeni proje oluşturarak başlayın." /> : <><div className="project-grid">{items.map((project) => <article className="panel project-card" key={project.id}><div><span className="status-label active">{project.archived ? "Arşiv" : "Aktif"}</span><h2>{project.name}</h2><p>{project.description || "Açıklama yok"}</p><small>Sahip: {project.ownerName}</small></div><div className="button-row"><Link className="primary-link" to={`/projects/${project.id}`}>Aç</Link>{!project.archived && (user?.role === "ADMIN" || user?.id === project.ownerId) && <button className="secondary-button" onClick={() => void archive(project)}>Arşivle</button>}</div></article>)}</div>{totalPages > 1 && <nav className="pagination" aria-label="Proje sayfaları"><button className="secondary-button" disabled={page === 0} onClick={() => void load(page - 1)}>Önceki</button><span>{page + 1} / {totalPages}</span><button className="secondary-button" disabled={page + 1 >= totalPages} onClick={() => void load(page + 1)}>Sonraki</button></nav>}</>}
  </div>;
}
