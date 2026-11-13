import { useState, type FormEvent } from "react";

import { priorityLabels } from "../projectFormat";
import type { ProjectMember, TaskInput, TaskPriority } from "../types";

export type TaskFormValue = {
  title: string;
  description: string;
  assigneeId: string;
  priority: TaskPriority;
  dueAt: string;
};

export const emptyTaskForm: TaskFormValue = { title: "", description: "", assigneeId: "", priority: "MEDIUM", dueAt: "" };

export function toTaskInput(value: TaskFormValue): TaskInput {
  return {
    title: value.title,
    description: value.description,
    assigneeId: value.assigneeId ? Number(value.assigneeId) : null,
    priority: value.priority,
    dueAt: value.dueAt ? new Date(value.dueAt).toISOString() : null,
  };
}

type Props = {
  initialValue?: TaskFormValue;
  members: ProjectMember[];
  submitLabel: string;
  busy?: boolean;
  onSubmit: (value: TaskFormValue) => Promise<boolean | void>;
  onCancel?: () => void;
};

export function TaskForm({ initialValue = emptyTaskForm, members, submitLabel, busy = false, onSubmit, onCancel }: Props) {
  const [value, setValue] = useState(initialValue);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const succeeded = await onSubmit(value);
    if (!onCancel && succeeded !== false) setValue(emptyTaskForm);
  }

  return <form className="management-form task-form" onSubmit={submit}>
    <label>Başlık<input autoFocus={!!onCancel} required maxLength={200} value={value.title} onChange={(event) => setValue({ ...value, title: event.target.value })} /></label>
    <label>Atanan<select value={value.assigneeId} onChange={(event) => setValue({ ...value, assigneeId: event.target.value })}><option value="">Atanmamış</option>{members.map((member) => <option value={member.userId} key={member.userId}>{member.name}</option>)}</select></label>
    <label>Öncelik<select value={value.priority} onChange={(event) => setValue({ ...value, priority: event.target.value as TaskPriority })}>{Object.entries(priorityLabels).map(([priority, label]) => <option value={priority} key={priority}>{label}</option>)}</select></label>
    <label>Son tarih<input type="datetime-local" value={value.dueAt} onChange={(event) => setValue({ ...value, dueAt: event.target.value })} /></label>
    <label className="full-field">Açıklama<textarea maxLength={5000} rows={4} value={value.description} onChange={(event) => setValue({ ...value, description: event.target.value })} /></label>
    <div className="button-row full-field">{onCancel && <button className="secondary-button" type="button" onClick={onCancel}>Vazgeç</button>}<button className="primary-button compact-button" disabled={busy}>{busy ? "Kaydediliyor…" : submitLabel}</button></div>
  </form>;
}
