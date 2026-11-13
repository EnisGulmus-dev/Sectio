import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { isApiClientError } from "../../../shared/api/apiError";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { useToast } from "../../../shared/ui/ToastProvider";
import { projectsApi } from "../api/projectsApi";
import { emptyTaskFilters, TaskFilters, type TaskFilterValue } from "../components/TaskFilters";
import { formatDate, priorityLabels, statusLabels } from "../projectFormat";
import type { Task, TaskStatus } from "../types";

export function MyTasksPage() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [filters, setFilters] = useState<TaskFilterValue>(emptyTaskFilters);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { showToast } = useToast();

  async function load(targetPage = 0, selected = filters) {
    setLoading(true);
    try {
      const response = await projectsApi.mine({ query: selected.query, status: selected.status, priority: selected.priority, page: targetPage });
      setTasks(response.content);
      setPage(response.page);
      setTotalPages(response.totalPages);
      setTotal(response.totalElements);
      setError(null);
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Görevler yüklenemedi"); }
    finally { setLoading(false); }
  }

  useEffect(() => { void load(); }, []);

  async function changeStatus(task: Task, status: TaskStatus) {
    try {
      const updated = await projectsApi.status(task.id, status);
      setTasks((current) => current.map((item) => item.id === updated.id ? updated : item));
      showToast("Görev durumu güncellendi");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Durum güncellenemedi"); }
  }

  return <div className="page-stack">
    <header className="page-header"><div><p className="eyebrow">Kişisel görünüm</p><h1>Görevlerim</h1><p>Size atanmış işleri filtreleyin ve durumlarını güncelleyin.</p></div><span className="count-badge">{total} görev</span></header>
    {error && <InlineAlert message={error} />}
    <section className="panel"><TaskFilters value={filters} onChange={setFilters} onApply={() => void load(0)} onClear={() => { setFilters(emptyTaskFilters); void load(0, emptyTaskFilters); }} /></section>
    {loading ? <LoadingState /> : tasks.length === 0 ? <EmptyState title="Filtreye uygun görev yok" description="Filtreleri temizleyebilir veya proje yöneticinizden görev atamasını isteyebilirsiniz." /> : <div className="my-task-grid">{tasks.map((task) => <article className={`panel my-task-card ${task.dueAt && new Date(task.dueAt) < new Date() && !["DONE", "CANCELLED"].includes(task.status) ? "overdue" : ""}`} key={task.id}><div className="task-card-top"><span className={`priority-badge priority-${task.priority.toLowerCase()}`}>{priorityLabels[task.priority]}</span><Link to={`/projects/${task.projectId}`}>{task.projectName}</Link></div><h2>{task.title}</h2><p>{task.description || "Açıklama yok"}</p><dl><div><dt>Son tarih</dt><dd>{formatDate(task.dueAt)}</dd></div><div><dt>Oluşturan</dt><dd>{task.createdByName}</dd></div></dl><label>Durum<select aria-label={`${task.title} durumu`} value={task.status} onChange={(event) => void changeStatus(task, event.target.value as TaskStatus)}>{Object.entries(statusLabels).map(([status, label]) => <option value={status} key={status}>{label}</option>)}</select></label></article>)}</div>}
    {totalPages > 1 && <nav className="pagination" aria-label="Görev sayfaları"><button className="secondary-button" disabled={page === 0} onClick={() => void load(page - 1)}>Önceki</button><span>{page + 1} / {totalPages}</span><button className="secondary-button" disabled={page + 1 >= totalPages} onClick={() => void load(page + 1)}>Sonraki</button></nav>}
  </div>;
}
