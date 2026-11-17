import { useEffect, useState, type FormEvent } from "react";
import { ModalPanel } from "../../../shared/ui/ModalPanel";
import { InlineAlert, LoadingState } from "../../../shared/ui/Feedback";
import { isApiClientError } from "../../../shared/api/apiError";
import { projectsApi } from "../api/projectsApi";
import { formatDate, statusLabels } from "../projectFormat";
import type { Task, TaskStatus, TaskProgress, TaskHandoff, ProjectMember, ProjectTeam } from "../types";
import { PriorityBadge } from "./PriorityBadge";
import { TaskForm, toTaskInput } from "./TaskForm";
import styles from "./TaskDetailDialog.module.css";

export function TaskDetailDialog({ task, canChange, canHandoff, members = [], teams = [], onClose, onChanged }: {
  task: Task | null; canChange: boolean; canHandoff: boolean; members?: ProjectMember[]; teams?: ProjectTeam[];
  onClose: () => void; onChanged: () => void;
}) {
  const [current, setCurrent] = useState<Task | null>(task);
  const [events, setEvents] = useState<TaskProgress[]>([]);
  const [successors, setSuccessors] = useState<Task[]>([]);
  const [handoffHistory, setHandoffHistory] = useState<TaskHandoff[]>([]);
  const [status, setStatus] = useState<TaskStatus>(task?.status ?? "TODO");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [handoff, setHandoff] = useState(false);
  useEffect(() => {
    setCurrent(task); setStatus(task?.status ?? "TODO"); setNote(""); setHandoff(false); setError(null); setEvents([]); setSuccessors([]); setHandoffHistory([]);
    if (!task) return;
    let live = true; setLoading(true);
    projectsApi.progress(task.id).then((items) => { if (live) setEvents(items); })
      .catch((caught) => { if (live) setError(isApiClientError(caught) ? caught.message : "Geçmiş yüklenemedi."); })
      .finally(() => { if (live) setLoading(false); });
    projectsApi.successors(task.id).then((items) => { if (live) setSuccessors(items); })
      .catch((caught) => { if (live) setError(isApiClientError(caught) ? caught.message : "Sonraki aşamalar yüklenemedi."); });
    projectsApi.handoffHistory(task.id).then((items) => { if (live) setHandoffHistory(items); })
      .catch((caught) => { if (live) setError(isApiClientError(caught) ? caught.message : "Devir geçmişi yüklenemedi."); });
    return () => { live = false; };
  }, [task?.id]);
  async function save(event: FormEvent) {
    event.preventDefault(); if (!current || busy) return;
    setBusy(true); setError(null);
    try {
      const updated = await projectsApi.status(current.id, status, note, current.status);
      setCurrent(updated); setNote(""); onChanged();
      try { setEvents(await projectsApi.progress(current.id)); }
      catch { setError("Durum kaydedildi; geçmiş yüklenemedi. Pencereyi yeniden açın."); }
    } catch (caught) { setError(isApiClientError(caught) ? caught.message : "Durum güncellenemedi."); }
    finally { setBusy(false); }
  }
  return <ModalPanel open={!!task} title="Görev detayı" onClose={onClose}>
    {current && <div className={styles.content}>
      <header><h2>{current.title}</h2><PriorityBadge priority={current.priority} /></header>
      <p>{current.description || "Açıklama yok"}</p>
      <p><strong>Durum:</strong> {statusLabels[current.status]} · <strong>Son tarih:</strong> {formatDate(current.dueAt)}</p>
      <p><strong>Sorumlular:</strong> {current.assignees.map((a) => a.name).join(", ") || "Atanmamış"}{current.teams?.length ? ` · Takım: ${current.teams.map((t) => t.name).join(", ")}` : ""}</p>
      {current.predecessorId && <p>Önceki aşama: #{current.predecessorId}</p>}
      {successors.length > 0 && <p><strong>Sonraki aşamalar:</strong> {successors.map((next) => `#${next.id} ${next.title} (${statusLabels[next.status]})`).join(" · ")}</p>}
      <section><h3>Devir geçmişi</h3>{handoffHistory.length === 0 ? <p>Bu görev için kayıtlı devir bilgisi yok.</p> : <ol className={styles.events}>{handoffHistory.map((entry) => <li key={entry.id}>
        <strong>{entry.sourceTitle} → {entry.targetTitle}</strong>
        <small>{entry.actorName} · {new Date(entry.handedOffAt).toLocaleString("tr-TR")}</small>
        {entry.recipientTeams && <p><strong>Takım:</strong> {entry.recipientTeams}</p>}
        <p><strong>Atanan kişiler:</strong> {entry.recipientPeople || "Kayıt yok"}</p>
        {(!entry.sourceTaskId || !entry.targetTaskId) && <small>İlişkili görevlerden biri silinmiş; devir kaydı korunuyor.</small>}
      </li>)}</ol>}</section>
      {error && <InlineAlert message={error} />}
      {canChange && <form className={styles.form} onSubmit={save}>
        <h3>İlerlemeyi güncelle</h3>
        <label>Yeni durum<select value={status} onChange={(event) => setStatus(event.target.value as TaskStatus)}>{Object.entries(statusLabels).map(([key, label]) => <option value={key} key={key}>{label}</option>)}</select></label>
        <label>İlerleme notu<textarea value={note} maxLength={2000} rows={3} onChange={(event) => setNote(event.target.value)} placeholder="Yapılanları veya sonraki adımı açıklayın" /></label>
        <button className="primary-button compact-button" disabled={busy || status === current.status}>Güncellemeyi kaydet</button>
      </form>}
      {canHandoff && current.status === "DONE" && <section className={styles.form}>
        <button className="secondary-button" type="button" onClick={() => setHandoff(!handoff)}>{handoff ? "Devir formunu kapat" : "Sonraki aşamaya devret"}</button>
        {handoff && <TaskForm members={members} teams={teams} submitLabel="Sonraki görevi oluştur" busy={busy} onSubmit={async (value) => {
          setBusy(true); setError(null);
          try { await projectsApi.handoff(current.id, toTaskInput(value)); setHandoff(false); onChanged(); onClose(); return true; }
          catch (caught) { setError(isApiClientError(caught) ? caught.message : "Devir oluşturulamadı."); return false; }
          finally { setBusy(false); }
        }} />}
      </section>}
      <section><h3>İlerleme geçmişi</h3>{loading ? <LoadingState /> : events.length === 0 ? <p>Henüz kayıtlı durum değişikliği yok. Önceki değişiklikler kaydedilmemiş olabilir.</p> : <ol className={styles.events}>{events.map((item) => <li key={item.id}><strong>{statusLabels[item.oldStatus]} → {statusLabels[item.newStatus]}</strong><small>{item.actorName} · {new Date(item.occurredAt).toLocaleString("tr-TR")}</small>{item.note && <p>{item.note}</p>}</li>)}</ol>}</section>
    </div>}
  </ModalPanel>;
}
