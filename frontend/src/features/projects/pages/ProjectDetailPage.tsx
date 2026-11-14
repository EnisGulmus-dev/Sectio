import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";

import { isApiClientError } from "../../../shared/api/apiError";
import { ConfirmDialog } from "../../../shared/ui/ConfirmDialog";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { ModalPanel } from "../../../shared/ui/ModalPanel";
import { useToast } from "../../../shared/ui/ToastProvider";
import { useAuth } from "../../auth/useAuth";
import { jobTitleLabels } from "../../auth/jobTitles";
import { projectsApi } from "../api/projectsApi";
import { emptyTaskFilters, TaskFilters, type TaskFilterValue } from "../components/TaskFilters";
import { TaskForm, toTaskInput, type TaskFormValue } from "../components/TaskForm";
import { formatDate, priorityLabels, statusLabels, toLocalDateTime } from "../projectFormat";
import type { Project, ProjectMember, ProjectMemberCandidate, ProjectTeam, Task, TaskStatus } from "../types";

type PendingAction =
  | { type: "member"; member: ProjectMember }
  | { type: "task"; task: Task }
  | { type: "team"; team: ProjectTeam }
  | null;

export function ProjectDetailPage() {
  const id = Number(useParams().projectId);
  const { user } = useAuth();
  const { showToast } = useToast();
  const [project, setProject] = useState<Project | null>(null);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [teams, setTeams] = useState<ProjectTeam[]>([]);
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
  const [showMemberForm, setShowMemberForm] = useState(false);
  const [showTaskForm, setShowTaskForm] = useState(false);
  const [showTeamForm, setShowTeamForm] = useState(false);
  const [teamForm, setTeamForm] = useState({ name: "", userIds: [] as number[] });
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

  function errorMessage(caught: unknown, fallback: string) { return isApiClientError(caught) ? caught.message : fallback; }

  async function loadTasks(page = 0, selectedFilters = filters) {
    const response = await projectsApi.tasks(id, { query: selectedFilters.query, status: selectedFilters.status,
      priority: selectedFilters.priority, assigneeId: selectedFilters.assigneeId ? Number(selectedFilters.assigneeId) : "", page });
    setTasks(response.content); setTaskPage(response.page); setTaskPages(response.totalPages); setTaskTotal(response.totalElements);
  }

  useEffect(() => {
    Promise.all([projectsApi.get(id), projectsApi.members(id), projectsApi.teams(id), projectsApi.tasks(id)])
      .then(async ([loadedProject, loadedMembers, loadedTeams, loadedTasks]) => {
        setProject(loadedProject); setProjectForm({ name: loadedProject.name, description: loadedProject.description ?? "" });
        setMembers(loadedMembers); setTeams(loadedTeams); setTasks(loadedTasks.content); setTaskPage(loadedTasks.page);
        setTaskPages(loadedTasks.totalPages); setTaskTotal(loadedTasks.totalElements);
        if (!loadedProject.archived && (user?.role === "ADMIN" || user?.id === loadedProject.ownerId)) setCandidates(await projectsApi.memberCandidates(id));
      })
      .catch((caught) => setError(errorMessage(caught, "Proje yüklenemedi")))
      .finally(() => setLoading(false));
  }, [id, user?.id, user?.role]);

  async function updateProject(event: FormEvent) {
    event.preventDefault(); setBusy(true);
    try { setProject(await projectsApi.update(id, projectForm)); setError(null); showToast("Proje güncellendi"); }
    catch (caught) { setError(errorMessage(caught, "Proje güncellenemedi")); } finally { setBusy(false); }
  }

  async function addMember(event: FormEvent) {
    event.preventDefault(); setBusy(true);
    try {
      const added = await projectsApi.addMembers(id, selectedMemberIds);
      setMembers((current) => [...current, ...added]);
      const addedIds = new Set(added.map((member) => member.userId));
      setCandidates((current) => current.filter((candidate) => !addedIds.has(candidate.id)));
      setSelectedMemberIds([]); setCandidateQuery(""); setCandidateResultsOpen(false); setShowMemberForm(false);
      showToast(`${added.length} üye eklendi`);
    } catch (caught) { setError(errorMessage(caught, "Üye eklenemedi")); } finally { setBusy(false); }
  }

  async function createTeam(event: FormEvent) {
    event.preventDefault(); setBusy(true);
    try {
      const created = await projectsApi.createTeam(id, teamForm);
      setTeams((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, "tr")));
      setTeamForm({ name: "", userIds: [] }); setShowTeamForm(false); showToast("Takım oluşturuldu");
    } catch (caught) { setError(errorMessage(caught, "Takım oluşturulamadı")); } finally { setBusy(false); }
  }

  async function createTask(value: TaskFormValue) {
    setBusy(true);
    try { await projectsApi.createTask(id, toTaskInput(value)); await loadTasks(0); setShowTaskForm(false); showToast("Görev oluşturuldu"); return true; }
    catch (caught) { setError(errorMessage(caught, "Görev oluşturulamadı")); return false; } finally { setBusy(false); }
  }

  async function updateTask(value: TaskFormValue) {
    if (!editingTask) return;
    setBusy(true);
    try { const updated = await projectsApi.updateTask(editingTask.id, toTaskInput(value)); setTasks((current) => current.map((item) => item.id === updated.id ? updated : item)); setEditingTask(null); showToast("Görev güncellendi"); return true; }
    catch (caught) { setError(errorMessage(caught, "Görev güncellenemedi")); return false; } finally { setBusy(false); }
  }

  async function changeStatus(item: Task, status: TaskStatus) {
    try { const updated = await projectsApi.status(item.id, status); setTasks((current) => current.map((task) => task.id === updated.id ? updated : task)); showToast("Görev durumu güncellendi"); }
    catch (caught) { setError(errorMessage(caught, "Durum güncellenemedi")); }
  }

  async function confirmAction() {
    if (!pendingAction) return;
    setBusy(true);
    try {
      if (pendingAction.type === "member") {
        const member = pendingAction.member; await projectsApi.removeMember(id, member.userId);
        setMembers((current) => current.filter((item) => item.userId !== member.userId));
        setTeams((current) => current.map((team) => ({ ...team, members: team.members.filter((item) => item.userId !== member.userId) })));
        await loadTasks(taskPage);
        setCandidates((current) => [...current, { id: member.userId, name: member.name, email: member.email, role: member.role, jobTitle: member.jobTitle }]); showToast("Üye çıkarıldı");
      } else if (pendingAction.type === "task") {
        await projectsApi.deleteTask(pendingAction.task.id); await loadTasks(taskPage); showToast("Görev silindi");
      } else {
        await projectsApi.deleteTeam(id, pendingAction.team.id); setTeams((current) => current.filter((team) => team.id !== pendingAction.team.id)); showToast("Takım silindi");
      }
      setPendingAction(null);
    } catch (caught) { setError(errorMessage(caught, "İşlem tamamlanamadı")); } finally { setBusy(false); }
  }

  if (loading) return <LoadingState />;
  if (!project) return <EmptyState title="Proje bulunamadı" />;

  const visibleCandidates = candidates.filter((candidate) => !selectedMemberIds.includes(candidate.id) && `${candidate.name} ${candidate.email}`.toLocaleLowerCase("tr-TR").includes(candidateQuery.toLocaleLowerCase("tr-TR")));
  const jobTitleLabel = (title: ProjectMember["jobTitle"]) => jobTitleLabels[title];
  const pendingCopy = pendingAction?.type === "task"
    ? { title: "Görev silinsin mi?", description: `“${pendingAction.task.title}” kalıcı olarak silinecek.`, label: "Görevi sil" }
    : pendingAction?.type === "team"
      ? { title: "Takım silinsin mi?", description: `“${pendingAction.team.name}” takım başlığı silinecek; çalışanlar projede kalacak.`, label: "Takımı sil" }
      : { title: "Üye projeden çıkarılsın mı?", description: `${pendingAction?.member.name ?? "Seçilen kullanıcı"} artık bu projeye erişemeyecek.`, label: "Üyeyi çıkar" };

  return <div className="page-stack project-workspace">
    <nav className="breadcrumb" aria-label="Sayfa yolu"><Link to="/projects">Projeler</Link><span>/</span><span>{project.name}</span></nav>
    <header className="page-header"><div><p className="eyebrow">Proje çalışma alanı</p><h1>{project.name}</h1><p>{project.description || "Bu proje için henüz açıklama eklenmedi."}</p></div><div className="header-meta"><span className={`status-label ${project.archived ? "inactive" : "active"}`}>{project.archived ? "Arşiv" : "Aktif"}</span><small>Güncellendi: {formatDate(project.updatedAt)}</small></div></header>
    {error && <InlineAlert message={error} />}
    <section className="summary-strip" aria-label="Proje özeti"><div><strong>{members.length}</strong><span>üye</span></div><div><strong>{teams.length}</strong><span>takım</span></div><div><strong>{taskTotal}</strong><span>görev</span></div><div><strong>{project.ownerName}</strong><span>proje sahibi</span></div></section>

    {canManage && !project.archived && <details className="panel collapsible-panel"><summary>Proje bilgilerini düzenle</summary><form className="management-form" onSubmit={updateProject}><label>Proje adı<input required maxLength={150} value={projectForm.name} onChange={(event) => setProjectForm({ ...projectForm, name: event.target.value })} /></label><label className="full-field">Açıklama<textarea rows={3} maxLength={1000} value={projectForm.description} onChange={(event) => setProjectForm({ ...projectForm, description: event.target.value })} /></label><button className="primary-button compact-button" disabled={busy}>Kaydet</button></form></details>}

    <details className="panel collapsible-panel" open>
      <summary><span><strong>Proje üyeleri ve takımlar</strong><small>Görev paylaşımı için çalışma gruplarını yönetin.</small></span><span className="count-badge">{members.length} üye</span></summary>
      <div className="section-toolbar">{canManage && !project.archived && <><button className="secondary-button" type="button" onClick={() => setShowTeamForm(true)}>Takım oluştur</button><button className="primary-button compact-button" type="button" onClick={() => setShowMemberForm(true)}>Çalışan ekle</button></>}</div>
      {teams.length > 0 && <div className="team-grid">{teams.map((team) => <article className="team-card" key={team.id}>
        <header className="team-card-header"><div><strong>{team.name}</strong><small>{team.members.length} kişi</small></div><div className="team-card-actions">{canManage && !project.archived && <button className="team-delete-button" type="button" onClick={() => setPendingAction({ type: "team", team })}><span aria-hidden="true">×</span>Takımı sil</button>}</div></header>
        <div className="team-member-grid">{team.members.map((member) => <div className="team-member-box" key={member.userId}><strong>{member.name}</strong><span>{jobTitleLabel(member.jobTitle)}</span><small>{member.email}</small></div>)}</div>
      </article>)}</div>}
      <div className="member-list">{members.map((member) => <span key={member.userId}><span className="member-copy"><strong>{member.name}</strong><small>{member.email} · {jobTitleLabel(member.jobTitle)}</small></span>{member.userId === project.ownerId && <em>Sahip</em>}{canManage && !project.archived && member.userId !== project.ownerId && <button className="text-button" type="button" onClick={() => setPendingAction({ type: "member", member })}>Çıkar</button>}</span>)}</div>
    </details>

    <details className="panel collapsible-panel task-workspace" open>
      <summary><span><strong>Görev planı</strong><small>Yapılacak işleri atayın, önceliklendirin ve ilerlemeyi takip edin.</small></span><span className="count-badge">{taskTotal} görev</span></summary>
      <div className="task-command-bar"><div><strong>Sıradaki işi netleştirin</strong><p>Başlık, sorumlular ve son tarihi belirleyerek ekibin ne yapacağını görünür hale getirin.</p></div>{canManage && !project.archived && <button className="primary-button compact-button" type="button" onClick={() => setShowTaskForm(true)}>+ Yeni görev</button>}</div>
      <TaskFilters value={filters} members={members} onChange={setFilters} onApply={() => void loadTasks(0)} onClear={() => { setFilters(emptyTaskFilters); void loadTasks(0, emptyTaskFilters); }} />
      {tasks.length === 0 ? <EmptyState title="Filtreye uygun görev yok" description="Yeni görev oluşturarak ekibin sıradaki işini belirleyin." /> : <div className="task-list">{tasks.map((item) => { const canChangeStatus = canManage || item.assignees.some((assignee) => assignee.userId === user?.id); return <article key={item.id} className={item.dueAt && new Date(item.dueAt) < new Date() && !["DONE", "CANCELLED"].includes(item.status) ? "overdue" : ""}><div className="task-main"><div className="task-title-line"><strong>{item.title}</strong><span className={`priority-badge priority-${item.priority.toLowerCase()}`}>{priorityLabels[item.priority]}</span></div><p>{item.description || "Açıklama yok"}</p><span>{item.assignees.map((assignee) => assignee.name).join(", ")} · Son tarih: {formatDate(item.dueAt)}</span></div><div className="task-actions"><select aria-label={`${item.title} durumu`} value={item.status} disabled={project.archived || !canChangeStatus} onChange={(event) => void changeStatus(item, event.target.value as TaskStatus)}>{Object.entries(statusLabels).map(([status, label]) => <option value={status} key={status}>{label}</option>)}</select>{canManage && !project.archived && <button className="secondary-button" type="button" onClick={() => setEditingTask(item)}>Düzenle</button>}{canManage && !project.archived && <button className="text-button" type="button" onClick={() => setPendingAction({ type: "task", task: item })}>Sil</button>}</div></article>; })}</div>}
      {taskPages > 1 && <nav className="pagination" aria-label="Görev sayfaları"><button className="secondary-button" disabled={taskPage === 0} onClick={() => void loadTasks(taskPage - 1)}>Önceki</button><span>{taskPage + 1} / {taskPages}</span><button className="secondary-button" disabled={taskPage + 1 >= taskPages} onClick={() => void loadTasks(taskPage + 1)}>Sonraki</button></nav>}
    </details>

    <ModalPanel open={canManage && editingTask !== null} title="Görevi düzenle" onClose={() => setEditingTask(null)}>{editingTask && <TaskForm key={editingTask.id} members={members} teams={teams} submitLabel="Değişiklikleri kaydet" busy={busy} initialValue={{ title: editingTask.title, description: editingTask.description ?? "", assigneeIds: editingTask.assignees.map((assignee) => assignee.userId), teamIds: [], allMembers: false, priority: editingTask.priority, dueAt: toLocalDateTime(editingTask.dueAt) }} onSubmit={updateTask} onCancel={() => setEditingTask(null)} />}</ModalPanel>
    <ModalPanel className="project-people-dialog" open={showMemberForm} title="Projeye çalışan ekle" onClose={() => setShowMemberForm(false)}><form className="member-picker modal-member-picker" onSubmit={addMember}><div className="member-combobox" ref={memberPickerRef}><input autoFocus role="combobox" aria-label="Eklenecek üyeyi ara" aria-expanded={candidateResultsOpen} aria-controls="member-candidate-results" autoComplete="off" placeholder="İsim veya e-posta yazın" value={candidateQuery} onFocus={() => setCandidateResultsOpen(true)} onChange={(event) => { setCandidateQuery(event.target.value); setCandidateResultsOpen(true); }} />{selectedMemberIds.length > 0 && <div className="selected-members" aria-label="Seçilen üyeler">{selectedMemberIds.map((selectedId) => { const candidate = candidates.find((item) => item.id === selectedId); return candidate && <span key={selectedId}>{candidate.name}<button type="button" aria-label={`${candidate.name} seçimini kaldır`} onClick={() => setSelectedMemberIds((current) => current.filter((value) => value !== selectedId))}>×</button></span>; })}</div>}{candidateResultsOpen && <div className="candidate-dropdown" id="member-candidate-results" role="listbox">{visibleCandidates.length === 0 ? <p>Uygun kullanıcı bulunamadı.</p> : visibleCandidates.map((candidate) => <button type="button" role="option" aria-selected={selectedMemberIds.includes(candidate.id)} key={candidate.id} onClick={() => { setSelectedMemberIds((current) => [...current, candidate.id]); setCandidateQuery(""); }}><strong>{candidate.name}</strong><span>{candidate.email} · {jobTitleLabel(candidate.jobTitle)}</span></button>)}</div>}</div><button className="primary-button compact-button" disabled={busy || selectedMemberIds.length === 0}>{selectedMemberIds.length > 0 ? `${selectedMemberIds.length} üyeyi ekle` : "Üye ekle"}</button></form></ModalPanel>
    <ModalPanel className="project-people-dialog" open={showTeamForm} title="Yeni takım oluştur" onClose={() => setShowTeamForm(false)}><form className="team-form" onSubmit={createTeam}><label>Takım adı<input autoFocus required maxLength={100} placeholder="Örn. A Takımı" value={teamForm.name} onChange={(event) => setTeamForm({ ...teamForm, name: event.target.value })} /></label><fieldset><legend>Takım üyeleri</legend><div className="assignment-options">{members.map((member) => <label className="assignment-option" key={member.userId}><input type="checkbox" checked={teamForm.userIds.includes(member.userId)} onChange={() => setTeamForm({ ...teamForm, userIds: teamForm.userIds.includes(member.userId) ? teamForm.userIds.filter((id) => id !== member.userId) : [...teamForm.userIds, member.userId] })} /><span><strong>{member.name}</strong><small>{jobTitleLabel(member.jobTitle)}</small></span></label>)}</div></fieldset><div className="dialog-actions"><button className="primary-button compact-button" disabled={busy || teamForm.userIds.length === 0}>Takımı oluştur</button></div></form></ModalPanel>
    <ModalPanel open={canManage && showTaskForm} title="Yeni görev oluştur" onClose={() => setShowTaskForm(false)}><TaskForm members={members} teams={teams} submitLabel="Görev oluştur" busy={busy} onSubmit={createTask} /></ModalPanel>
    <ConfirmDialog open={!!pendingAction} title={pendingCopy.title} description={pendingCopy.description} confirmLabel={pendingCopy.label} busy={busy} onConfirm={() => void confirmAction()} onCancel={() => setPendingAction(null)} />
  </div>;
}
