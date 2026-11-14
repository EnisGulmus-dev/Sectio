import { useState, type FormEvent } from "react";

import { priorityLabels } from "../projectFormat";
import type { ProjectMember, ProjectTeam, TaskInput, TaskPriority } from "../types";
import { DateTimePicker } from "./DateTimePicker";

export type TaskFormValue = {
  title: string;
  description: string;
  assigneeIds: number[];
  teamIds: number[];
  allMembers: boolean;
  priority: TaskPriority;
  dueAt: string;
};

export const emptyTaskForm: TaskFormValue = {
  title: "", description: "", assigneeIds: [], teamIds: [], allMembers: false, priority: "MEDIUM", dueAt: "",
};

export function toTaskInput(value: TaskFormValue): TaskInput {
  return { ...value, dueAt: value.dueAt ? new Date(value.dueAt).toISOString() : null };
}

type Props = {
  initialValue?: TaskFormValue;
  members: ProjectMember[];
  teams?: ProjectTeam[];
  submitLabel: string;
  busy?: boolean;
  onSubmit: (value: TaskFormValue) => Promise<boolean | void>;
  onCancel?: () => void;
};

function toggle(values: number[], id: number) {
  return values.includes(id) ? values.filter((value) => value !== id) : [...values, id];
}

export function TaskForm({ initialValue = emptyTaskForm, members, teams = [], submitLabel, busy = false, onSubmit, onCancel }: Props) {
  const [value, setValue] = useState(initialValue);
  const [assignmentError, setAssignmentError] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!value.allMembers && value.assigneeIds.length === 0 && value.teamIds.length === 0) {
      setAssignmentError(true);
      return;
    }
    const succeeded = await onSubmit(value);
    if (!onCancel && succeeded !== false) setValue(emptyTaskForm);
  }

  return <form className="task-form" onSubmit={submit}>
    <div className="task-form-grid">
      <label>Başlık<input autoFocus required maxLength={200} value={value.title} onChange={(event) => setValue({ ...value, title: event.target.value })} /></label>
      <label>Öncelik<select value={value.priority} onChange={(event) => setValue({ ...value, priority: event.target.value as TaskPriority })}>{Object.entries(priorityLabels).map(([priority, label]) => <option value={priority} key={priority}>{label}</option>)}</select></label>
      <label className="full-field">Açıklama<textarea maxLength={5000} rows={3} value={value.description} onChange={(event) => setValue({ ...value, description: event.target.value })} /></label>
    </div>

    <fieldset className={`assignment-picker${assignmentError ? " invalid" : ""}`}>
      <legend>Görevi kim yapacak?</legend>
      <p>Birden fazla çalışan, takım veya tüm proje üyelerini seçebilirsiniz.</p>
      <label className="assignment-option everyone-option"><input type="checkbox" checked={value.allMembers} onChange={(event) => { setAssignmentError(false); setValue({ ...value, allMembers: event.target.checked }); }} /><span><strong>Herkes</strong><small>Projede yer alan tüm aktif üyeler</small></span></label>
      {teams.length > 0 && <div className="assignment-group"><strong>Takımlar</strong><div className="assignment-options">{teams.map((team) => <label className="assignment-option" key={team.id}><input type="checkbox" disabled={value.allMembers} checked={value.teamIds.includes(team.id)} onChange={() => { setAssignmentError(false); setValue({ ...value, teamIds: toggle(value.teamIds, team.id) }); }} /><span><strong>{team.name}</strong><small>{team.members.length} kişi</small></span></label>)}</div></div>}
      <div className="assignment-group"><strong>Proje üyeleri</strong><div className="assignment-options">{members.map((member) => <label className="assignment-option" key={member.userId}><input type="checkbox" disabled={value.allMembers} checked={value.assigneeIds.includes(member.userId)} onChange={() => { setAssignmentError(false); setValue({ ...value, assigneeIds: toggle(value.assigneeIds, member.userId) }); }} /><span><strong>{member.name}</strong><small>{member.email}</small></span></label>)}</div></div>
      {assignmentError && <span className="field-error">En az bir çalışan, takım veya “Herkes” seçeneğini belirleyin.</span>}
    </fieldset>

    <DateTimePicker value={value.dueAt} onChange={(dueAt) => setValue({ ...value, dueAt })} />
    <div className="dialog-actions">{onCancel && <button className="secondary-button" type="button" onClick={onCancel}>Vazgeç</button>}<button className="primary-button compact-button" disabled={busy}>{busy ? "Kaydediliyor…" : submitLabel}</button></div>
  </form>;
}
