import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";

import { useAuth } from "../../auth/useAuth";
import { isApiClientError } from "../../../shared/api/apiError";
import { ConfirmDialog } from "../../../shared/ui/ConfirmDialog";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { useToast } from "../../../shared/ui/ToastProvider";
import { projectsApi } from "../api/projectsApi";
import { emptyTaskFilters, TaskFilters, type TaskFilterValue } from "../components/TaskFilters";
import { TaskForm, toTaskInput, type TaskFormValue } from "../components/TaskForm";
import { formatDate, priorityLabels, statusLabels, toLocalDateTime } from "../projectFormat";
import type { Project, ProjectMember, ProjectMemberCandidate, Task, TaskStatus } from "../types";

type PendingAction = { type: "member"; member: ProjectMember } | { type: "task"; task: Task } | null;

export function ProjectDetailPage() {
  const id = Number(useParams().projectId);
  const { user } = useAuth();
  const { showToast } = useToast();
  const [project, setProject] = useState<Project | null>(null);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [candidates, setCandidates] = useState<ProjectMemberCandidate[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [taskPage, setTaskPage] = useState(0);
  const [taskPages, setTaskPages] = useState(0);
  const [taskTotal, setTaskTotal] = useState(0);
  const [filters, setFilters] = useState<TaskFilterValue>(emptyTaskFilters);
  const [candidateQuery, setCandidateQuery] = useState("");
  const [candidateResultsOpen, setCandidateResultsOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedMemberIds, setSelectedMemberIds] = useState<number[]>([]);
  const memberPickerRef = useRef<HTMLDivElement>(null);
  const [projectForm, setProjectForm] = useState({ name: "", description: "" });

  const canManage = !!project && (user?.role === "ADMIN" || user?.id === project.ownerId);

  useEffect(() => {
    function closeCandidateResults(event: PointerEvent) {
      if (!memberPickerRef.current?.contains(event.target as Node)) setCandidateResultsOpen(false);
    }
    document.addEventListener("pointerdown", closeCandidateResults);
    return () => document.removeEventListener("pointerdown", closeCandidateResults);
  }, []);

  function errorMessage(caught: unknown, fallback: string) {
    return isApiClientError(caught) ? caught.message : fallback;
  }

  async function loadTasks(page = 0, selectedFilters = filters) {
    const response = await projectsApi.tasks(id, {
      query: selectedFilters.query,
      status: selectedFilters.status,
      priority: selectedFilters.priority,
      assigneeId: selectedFilters.assigneeId ? Number(selectedFilters.assigneeId) : "",
      page,
    });
    setTasks(response.content);
    setTaskPage(response.page);
    setTaskPages(response.totalPages);
    setTaskTotal(response.totalElements);
  }

  useEffect(() => {
    Promise.all([projectsApi.get(id), projectsApi.members(id), projectsApi.tasks(id)])
      .then(async ([loadedProject, loadedMembers, loadedTasks]) => {
        setProject(loadedProject);
        setProjectForm({ name: loadedProject.name, description: loadedProject.description ?? "" });
        setMembers(loadedMembers);
        setTasks(loadedTasks.content);
        setTaskPage(loadedTasks.page);
        setTaskPages(loadedTasks.totalPages);
        setTaskTotal(loadedTasks.totalElements);
        if (!loadedProject.archived && (user?.role === "ADMIN" || user?.id === loadedProject.ownerId)) {
          setCandidates(await projectsApi.memberCandidates(id));
        }
      })
      .catch((caught) => setError(errorMessage(caught, "Proje yüklenemedi")))
      .finally(() => setLoading(false));
  }, [id, user?.id, user?.role]);

  async function updateProject(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      setProject(await projectsApi.update(id, projectForm));
      setError(null);
      showToast("Proje güncellendi");
    } catch (caught) { setError(errorMessage(caught, "Proje güncellenemedi")); }
    finally { setBusy(false); }
  }

  async function addMember(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      const added = await projectsApi.addMembers(id, selectedMemberIds);
      setMembers((current) => [...current, ...added]);
      const addedIds = new Set(added.map((member) => member.userId));
      setCandidates((current) => current.filter((candidate) => !addedIds.has(candidate.id)));
      setSelectedMemberIds([]);
      setCandidateQuery("");
      setCandidateResultsOpen(false);
      showToast(`${added.length} üye eklendi`);
    } catch (caught) { setError(errorMessage(caught, "Üye eklenemedi")); }
    finally { setBusy(false); }
  }

  async function createTask(value: TaskFormValue) {
    setBusy(true);
    try {
      await projectsApi.createTask(id, toTaskInput(value));
      await loadTasks(0);
      showToast("Görev oluşturuldu");
      return true;
    } catch (caught) { setError(errorMessage(caught, "Görev oluşturulamadı")); return false; }
    finally { setBusy(false); }
  }

  async function updateTask(value: TaskFormValue) {
    if (!editingTask) return;
    setBusy(true);
    try {
      const updated = await projectsApi.updateTask(editingTask.id, toTaskInput(value));
      setTasks((current) => current.map((item) => item.id === updated.id ? updated : item));
      setEditingTask(null);
      showToast("Görev güncellendi");
      return true;
    } catch (caught) { setError(errorMessage(caught, "Görev güncellenemedi")); return false; }
    finally { setBusy(false); }
  }

  async function changeStatus(item: Task, status: TaskStatus) {
    try {
      const updated = await projectsApi.status(item.id, status);
      setTasks((current) => current.map((task) => task.id === updated.id ? updated : task));
      showToast("Görev durumu güncellendi");
    } catch (caught) { setError(errorMessage(caught, "Durum güncellenemedi")); }
  }

  async function confirmAction() {
    if (!pendingAction) return;
    setBusy(true);
    try {
      if (pendingAction.type === "member") {
        const member = pendingAction.member;
        await projectsApi.removeMember(id, member.userId);
        setMembers((current) => current.filter((item) => item.userId !== member.userId));
        setCandidates((current) => [...current, { id: member.userId, name: member.name, email: member.email, role: member.role, jobTitle: member.jobTitle }]);
        showToast("Üye çıkarıldı");
      } else {
        await projectsApi.deleteTask(pendingAction.task.id);
        await loadTasks(taskPage);
        showToast("Görev silindi");
      }
      setPendingAction(null);
    } catch (caught) { setError(errorMessage(caught, "İşlem tamamlanamadı")); }
    finally { setBusy(false); }
  }

  if (loading) return <LoadingState />;
  if (!project) return <EmptyState title="Proje bulunamadı" />;

  const visibleCandidates = candidates.filter((candidate) => !selectedMemberIds.includes(candidate.id) && `${candidate.name} ${candidate.email}`.toLocaleLowerCase("tr-TR").includes(candidateQuery.toLocaleLowerCase("tr-TR")));
  const jobTitleLabel = (title: ProjectMember["jobTitle"]) => ({ EMPLOYEE: "Çalışan", TEAM_LEADER: "Takım lideri", MANAGER: "Müdür" })[title];

  return <div className="page-stack">
    <nav className="breadcrumb" aria-label="Sayfa yolu"><Link to="/projects">Projeler</Link><span>/</span><span>{project.name}</span></nav>
    <header className="page-header"><div><p className="eyebrow">Proje çalışma alanı</p><h1>{project.name}</h1><p>{project.description || "Bu proje için henüz açıklama eklenmedi."}</p></div><div className="header-meta"><span className={`status-label ${project.archived ? "inactive" : "active"}`}>{project.archived ? "Arşiv" : "Aktif"}</span><small>Güncellendi: {formatDate(project.updatedAt)}</small></div></header>
    {error && <InlineAlert message={error} />}
    <section className="summary-strip" aria-label="Proje özeti"><div><strong>{members.length}</strong><span>üye</span></div><div><strong>{taskTotal}</strong><span>görev</span></div><div><strong>{project.ownerName}</strong><span>proje sahibi</span></div></section>

    {canManage && !project.archived && <details className="panel collapsible-panel"><summary>Proje bilgilerini düzenle</summary><form className="management-form" onSubmit={updateProject}><label>Proje adı<input required maxLength={150} value={projectForm.name} onChange={(event) => setProjectForm({ ...projectForm, name: event.target.value })} /></label><label className="full-field">Açıklama<textarea rows={3} maxLength={1000} value={projectForm.description} onChange={(event) => setProjectForm({ ...projectForm, description: event.target.value })} /></label><button className="primary-button compact-button" disabled={busy}>Kaydet</button></form></details>}

    <section className="panel"><div className="section-heading"><div><h2>Proje üyeleri</h2><p>Görev atanabilecek aktif kullanıcılar.</p></div><span className="count-badge">{members.length}</span></div>
      {canManage && !project.archived && <form className="member-picker" onSubmit={addMember}><div className="member-combobox" ref={memberPickerRef}><input role="combobox" aria-label="Eklenecek üyeyi ara" aria-expanded={candidateResultsOpen} aria-controls="member-candidate-results" autoComplete="off" placeholder="İsim veya e-posta yazın" value={candidateQuery} onFocus={() => setCandidateResultsOpen(true)} onChange={(event) => { setCandidateQuery(event.target.value); setCandidateResultsOpen(true); }} />{selectedMemberIds.length > 0 && <div className="selected-members" aria-label="Seçilen üyeler">{selectedMemberIds.map((selectedId) => { const candidate = candidates.find((item) => item.id === selectedId); return candidate && <span key={selectedId}>{candidate.name}<button type="button" aria-label={`${candidate.name} seçimini kaldır`} onClick={() => setSelectedMemberIds((current) => current.filter((value) => value !== selectedId))}>×</button></span>; })}</div>}{candidateResultsOpen && <div className="candidate-dropdown" id="member-candidate-results" role="listbox">{visibleCandidates.length === 0 ? <p>Uygun kullanıcı bulunamadı.</p> : visibleCandidates.map((candidate) => <button type="button" role="option" aria-selected={selectedMemberIds.includes(candidate.id)} key={candidate.id} onClick={() => { setSelectedMemberIds((current) => [...current, candidate.id]); setCandidateQuery(""); }}><strong>{candidate.name}</strong><span>{candidate.email} · {jobTitleLabel(candidate.jobTitle)}</span></button>)}</div>}</div><button className="primary-button compact-button" disabled={busy || selectedMemberIds.length === 0}>{selectedMemberIds.length > 0 ? `${selectedMemberIds.length} üyeyi ekle` : "Üye ekle"}</button></form>}
      <div className="member-list">{members.map((member) => <span key={member.userId}><span className="member-copy"><strong>{member.name}</strong><small>{member.email} · {jobTitleLabel(member.jobTitle)}</small></span>{member.userId === project.ownerId && <em>Sahip</em>}{canManage && !project.archived && member.userId !== project.ownerId && <button className="text-button" type="button" onClick={() => setPendingAction({ type: "member", member })}>Çıkar</button>}</span>)}</div>
    </section>

    {!project.archived && <details className="panel collapsible-panel"><summary>Yeni görev oluştur</summary><TaskForm members={members} submitLabel="Görev oluştur" busy={busy} onSubmit={createTask} /></details>}

    <section className="panel"><div className="section-heading"><div><h2>Görevler</h2><p>Projedeki işleri filtreleyin ve ilerlemeyi güncelleyin.</p></div><span className="count-badge">{taskTotal}</span></div>
      <TaskFilters value={filters} members={members} onChange={setFilters} onApply={() => void loadTasks(0)} onClear={() => { setFilters(emptyTaskFilters); void loadTasks(0, emptyTaskFilters); }} />
      {tasks.length === 0 ? <EmptyState title="Filtreye uygun görev yok" /> : <div className="task-list">{tasks.map((item) => <article key={item.id} className={item.dueAt && new Date(item.dueAt) < new Date() && !["DONE", "CANCELLED"].includes(item.status) ? "overdue" : ""}><div className="task-main"><div className="task-title-line"><strong>{item.title}</strong><span className={`priority-badge priority-${item.priority.toLowerCase()}`}>{priorityLabels[item.priority]}</span></div><p>{item.description || "Açıklama yok"}</p><span>{item.assigneeName || "Atanmamış"} · Son tarih: {formatDate(item.dueAt)}</span></div><div className="task-actions"><select aria-label={`${item.title} durumu`} value={item.status} disabled={project.archived} onChange={(event) => void changeStatus(item, event.target.value as TaskStatus)}>{Object.entries(statusLabels).map(([status, label]) => <option value={status} key={status}>{label}</option>)}</select>{!project.archived && <button className="secondary-button" type="button" onClick={() => setEditingTask(item)}>Düzenle</button>}{!project.archived && <button className="text-button" type="button" onClick={() => setPendingAction({ type: "task", task: item })}>Sil</button>}</div></article>)}</div>}
      {taskPages > 1 && <nav className="pagination" aria-label="Görev sayfaları"><button className="secondary-button" disabled={taskPage === 0} onClick={() => void loadTasks(taskPage - 1)}>Önceki</button><span>{taskPage + 1} / {taskPages}</span><button className="secondary-button" disabled={taskPage + 1 >= taskPages} onClick={() => void loadTasks(taskPage + 1)}>Sonraki</button></nav>}
    </section>

    {editingTask && <div className="modal-layer" role="dialog" aria-modal="true" aria-labelledby="edit-task-title"><section className="panel modal-card"><h2 id="edit-task-title">Görevi düzenle</h2><TaskForm key={editingTask.id} members={members} submitLabel="Değişiklikleri kaydet" busy={busy} initialValue={{ title: editingTask.title, description: editingTask.description ?? "", assigneeId: editingTask.assigneeId ? String(editingTask.assigneeId) : "", priority: editingTask.priority, dueAt: toLocalDateTime(editingTask.dueAt) }} onSubmit={updateTask} onCancel={() => setEditingTask(null)} /></section></div>}
    <ConfirmDialog open={!!pendingAction} title={pendingAction?.type === "task" ? "Görev silinsin mi?" : "Üye projeden çıkarılsın mı?"} description={pendingAction?.type === "task" ? `“${pendingAction.task.title}” kalıcı olarak silinecek.` : `${pendingAction?.member.name ?? "Seçilen kullanıcı"} artık bu projeye erişemeyecek.`} confirmLabel={pendingAction?.type === "task" ? "Görevi sil" : "Üyeyi çıkar"} busy={busy} onConfirm={() => void confirmAction()} onCancel={() => setPendingAction(null)} />
  </div>;
}
