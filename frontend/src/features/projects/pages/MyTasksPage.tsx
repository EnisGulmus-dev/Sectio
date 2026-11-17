import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

import { isApiClientError } from "../../../shared/api/apiError";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";

import { projectsApi } from "../api/projectsApi";
import { emptyTaskFilters, TaskFilters, type TaskFilterValue } from "../components/TaskFilters";
import { formatDate } from "../projectFormat";
import type { Task, TaskBucket } from "../types";
import { TaskBucketSwitch, readTaskBucket, taskBucketLabels } from "../components/TaskBucketSwitch";
import { PriorityBadge } from "../components/PriorityBadge";
import { TaskDetailDialog } from "../components/TaskDetailDialog";

export function MyTasksPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const bucket = readTaskBucket(searchParams.get("bucket"));
  const requestId = useRef(0);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [detailTask, setDetailTask] = useState<Task | null>(null);
  const [filters, setFilters] = useState<TaskFilterValue>(emptyTaskFilters);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const updating = false;
  const [error, setError] = useState<string | null>(null);


  async function load(targetPage = 0, selected = filters, selectedBucket: TaskBucket = bucket) {
    const current = ++requestId.current;
    setLoading(true);
    try {
      const response = await projectsApi.mine({ query: selected.query, status: selected.status, priority: selected.priority, bucket: selectedBucket, page: targetPage });
      if (current !== requestId.current) return;
      setTasks(response.content);
      setPage(response.page);
      setTotalPages(response.totalPages);
      setTotal(response.totalElements);
      setError(null);
    } catch (caught) {
      if (current === requestId.current) {
        setTasks([]); setTotal(0); setTotalPages(0);
        setError(isApiClientError(caught) ? caught.message : "Görevler yüklenemedi");
      }
    }
    finally { if (current === requestId.current) setLoading(false); }
  }

  useEffect(() => { setFilters(emptyTaskFilters); void load(0, emptyTaskFilters, bucket); return () => { requestId.current++; }; }, [bucket]);

  function selectBucket(next: TaskBucket) {
    setFilters(emptyTaskFilters);
    if (next === bucket) void load(0, emptyTaskFilters, next);
    setSearchParams({ bucket: next });
  }

  return <div className="page-stack">
    <header className="page-header"><div><p className="eyebrow">Kişisel görünüm</p><h1>Görevlerim</h1><p>Size atanmış işleri filtreleyin ve durumlarını güncelleyin.</p></div><span className="count-badge">{total} görev</span></header>
    {error && <InlineAlert message={error} />}
    <TaskBucketSwitch value={bucket} onChange={(next) => { if (next !== "UNASSIGNED") selectBucket(next); }} disabled={updating} />
    <h2>{taskBucketLabels[bucket]}</h2>
    <section className="panel"><TaskFilters value={filters} bucket={bucket} disabled={updating} onChange={setFilters} onApply={() => void load(0)} onClear={() => { setFilters(emptyTaskFilters); void load(0, emptyTaskFilters); }} /></section>
    {loading ? <LoadingState /> : tasks.length === 0 ? <EmptyState title="Filtreye uygun görev yok" description="Filtreleri temizleyebilir veya proje yöneticinizden görev atamasını isteyebilirsiniz." /> : <div className="my-task-grid">{tasks.map((task) => <article className={`panel my-task-card ${task.dueAt && new Date(task.dueAt) < new Date() && !["DONE", "CANCELLED"].includes(task.status) ? "overdue" : ""}`} key={task.id}><div className="task-card-top"><PriorityBadge priority={task.priority} /><Link to={`/projects/${task.projectId}`}>{task.projectName}</Link></div><h2>{task.title}</h2><p>{task.description || "Açıklama yok"}</p><dl><div><dt>Son tarih</dt><dd>{formatDate(task.dueAt)}</dd></div><div><dt>Oluşturan</dt><dd>{task.createdByName}</dd></div></dl><button className="secondary-button" type="button" onClick={() => setDetailTask(task)}>Görev detayını aç</button></article>)}</div>}
    <TaskDetailDialog task={detailTask} canChange canHandoff={false} onClose={() => setDetailTask(null)} onChanged={() => void load(0)} />
    {totalPages > 1 && <nav className="pagination" aria-label="Görev sayfaları"><button className="secondary-button" disabled={updating || loading || page === 0} onClick={() => void load(page - 1)}>Önceki</button><span>{page + 1} / {totalPages}</span><button className="secondary-button" disabled={updating || loading || page + 1 >= totalPages} onClick={() => void load(page + 1)}>Sonraki</button></nav>}
  </div>;
}
