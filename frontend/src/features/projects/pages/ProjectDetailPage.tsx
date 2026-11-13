import { useEffect, useState, type FormEvent } from "react";
import { useParams } from "react-router-dom";

import { useAuth } from "../../auth/useAuth";
import { isApiClientError } from "../../../shared/api/apiError";
import { EmptyState, InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { useToast } from "../../../shared/ui/ToastProvider";
import { projectsApi } from "../api/projectsApi";
import type { Project, ProjectMember, ProjectMemberCandidate, Task, TaskPriority, TaskStatus } from "../types";

const emptyTask = { title: "", description: "", assigneeId: "", priority: "MEDIUM" as TaskPriority, dueAt: "" };

export function ProjectDetailPage() {
  const id = Number(useParams().projectId);
  const { user } = useAuth();
  const { showToast } = useToast();
  const [project, setProject] = useState<Project | null>(null);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [candidates, setCandidates] = useState<ProjectMemberCandidate[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [memberId, setMemberId] = useState("");
  const [task, setTask] = useState(emptyTask);
  const [projectForm, setProjectForm] = useState({ name: "", description: "" });

  const canManage = !!project && (user?.role === "ADMIN" || user?.id === project.ownerId);

  useEffect(() => {
    Promise.all([projectsApi.get(id), projectsApi.members(id), projectsApi.tasks(id)])
      .then(async ([loadedProject, loadedMembers, loadedTasks]) => {
        setProject(loadedProject);
        setProjectForm({ name: loadedProject.name, description: loadedProject.description ?? "" });
        setMembers(loadedMembers);
        setTasks(loadedTasks.content);
        if (!loadedProject.archived && (user?.role === "ADMIN" || user?.id === loadedProject.ownerId)) {
          setCandidates(await projectsApi.memberCandidates(id));
        }
      })
      .catch((caught) => setError(isApiClientError(caught) ? caught.message : "Proje yüklenemedi"))
      .finally(() => setLoading(false));
  }, [id, user?.id, user?.role]);

  async function updateProject(event: FormEvent) {
    event.preventDefault();
    try {
      setProject(await projectsApi.update(id, projectForm));
      showToast("Proje güncellendi");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Proje güncellenemedi"); }
  }

  async function addMember(event: FormEvent) {
    event.preventDefault();
    try {
      const added = await projectsApi.addMember(id, Number(memberId));
      setMembers((current) => [...current, added]);
      setCandidates((current) => current.filter((candidate) => candidate.id !== added.userId));
      setMemberId("");
      showToast("Üye eklendi");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Üye eklenemedi"); }
  }

  async function removeMember(member: ProjectMember) {
    try {
      await projectsApi.removeMember(id, member.userId);
      setMembers((current) => current.filter((item) => item.userId !== member.userId));
      setCandidates((current) => [...current, { id: member.userId, name: member.name, email: member.email, role: member.role }]);
      showToast("Üye çıkarıldı");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Üye çıkarılamadı"); }
  }

  async function addTask(event: FormEvent) {
    event.preventDefault();
    try {
      const created = await projectsApi.createTask(id, {
        title: task.title,
        description: task.description,
        assigneeId: task.assigneeId ? Number(task.assigneeId) : null,
        priority: task.priority,
        dueAt: task.dueAt ? new Date(task.dueAt).toISOString() : null,
      });
      setTasks((current) => [created, ...current]);
      setTask(emptyTask);
      showToast("Görev oluşturuldu");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Görev oluşturulamadı"); }
  }

  async function changeStatus(item: Task, status: TaskStatus) {
    try {
      const updated = await projectsApi.status(item.id, status);
      setTasks((current) => current.map((taskItem) => taskItem.id === updated.id ? updated : taskItem));
      showToast("Görev durumu güncellendi");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Durum güncellenemedi"); }
  }

  async function deleteTask(item: Task) {
    try {
      await projectsApi.deleteTask(item.id);
      setTasks((current) => current.filter((taskItem) => taskItem.id !== item.id));
      showToast("Görev silindi");
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Görev silinemedi"); }
  }

  if (loading) return <LoadingState />;
  if (!project) return <EmptyState title="Proje bulunamadı" />;

  return <div className="page-stack">
    <header className="page-header"><div><p className="eyebrow">Proje</p><h1>{project.name}</h1><p>{project.description || "Açıklama yok"}</p></div><span className="role-badge">{project.archived ? "ARŞİV" : "AKTİF"}</span></header>
    {error && <InlineAlert message={error} />}

    {canManage && !project.archived && <section className="panel"><h2>Proje bilgileri</h2><form className="management-form" onSubmit={updateProject}><label>Proje adı<input required maxLength={150} value={projectForm.name} onChange={(event) => setProjectForm({ ...projectForm, name: event.target.value })} /></label><label>Açıklama<input maxLength={1000} value={projectForm.description} onChange={(event) => setProjectForm({ ...projectForm, description: event.target.value })} /></label><button className="primary-button compact-button">Kaydet</button></form></section>}

    <section className="panel"><h2>Üyeler</h2>
      {canManage && !project.archived && <form className="inline-form" onSubmit={addMember}><select aria-label="Eklenecek kullanıcı" required value={memberId} onChange={(event) => setMemberId(event.target.value)}><option value="">Kullanıcı seçin</option>{candidates.map((candidate) => <option value={candidate.id} key={candidate.id}>{candidate.name}</option>)}</select><button className="primary-button compact-button">Üye ekle</button></form>}
      <div className="member-list">{members.map((member) => <span key={member.userId}>{member.name} · {member.role}{canManage && !project.archived && member.userId !== project.ownerId && <button className="text-button" type="button" onClick={() => void removeMember(member)}>Çıkar</button>}</span>)}</div>
    </section>

    {!project.archived && <section className="panel"><h2>Yeni görev</h2><form className="management-form" onSubmit={addTask}><label>Başlık<input required maxLength={200} value={task.title} onChange={(event) => setTask({ ...task, title: event.target.value })} /></label><label>Atanan<select value={task.assigneeId} onChange={(event) => setTask({ ...task, assigneeId: event.target.value })}><option value="">Atanmamış</option>{members.map((member) => <option value={member.userId} key={member.userId}>{member.name}</option>)}</select></label><label>Öncelik<select value={task.priority} onChange={(event) => setTask({ ...task, priority: event.target.value as TaskPriority })}>{["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((priority) => <option key={priority}>{priority}</option>)}</select></label><label>Son tarih<input type="datetime-local" value={task.dueAt} onChange={(event) => setTask({ ...task, dueAt: event.target.value })} /></label><label>Açıklama<input value={task.description} onChange={(event) => setTask({ ...task, description: event.target.value })} /></label><button className="primary-button compact-button">Görev ekle</button></form></section>}

    <section className="panel"><h2>Görevler</h2>{tasks.length === 0 ? <EmptyState title="Henüz görev yok" /> : <div className="task-list">{tasks.map((item) => <article key={item.id}><div><strong>{item.title}</strong><span>{item.assigneeName || "Atanmamış"} · {item.priority}</span></div><div className="task-actions"><select aria-label={`${item.title} durumu`} value={item.status} disabled={project.archived} onChange={(event) => void changeStatus(item, event.target.value as TaskStatus)}><option value="TODO">Yapılacak</option><option value="IN_PROGRESS">Devam ediyor</option><option value="DONE">Tamamlandı</option></select>{!project.archived && <button className="secondary-button" type="button" onClick={() => void deleteTask(item)}>Sil</button>}</div></article>)}</div>}</section>
  </div>;
}
