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

function normalizeAssignments(value: TaskFormValue, teams: ProjectTeam[]): TaskFormValue {
  if (value.allMembers) return { ...value, assigneeIds: [], teamIds: [] };

  const teamIds = [...new Set(value.teamIds)];
  const assigneeIds = new Set(value.assigneeIds);

  for (const team of teams) {
    if (teamIds.includes(team.id)) {
      team.members.forEach((member) => assigneeIds.delete(member.userId));
    }
  }

  for (const team of teams) {
    if (teamIds.includes(team.id) || team.members.length === 0) continue;
    if (team.members.every((member) => assigneeIds.has(member.userId))) {
      teamIds.push(team.id);
      team.members.forEach((member) => assigneeIds.delete(member.userId));
    }
  }

  return { ...value, assigneeIds: [...assigneeIds], teamIds };
}

export function TaskForm({ initialValue = emptyTaskForm, members, teams = [], submitLabel, busy = false, onSubmit, onCancel }: Props) {
  const [value, setValue] = useState(() => normalizeAssignments(initialValue, teams));
  const [assignmentError, setAssignmentError] = useState(false);
  const selectedTeamMemberIds = new Set(teams
    .filter((team) => value.teamIds.includes(team.id))
    .flatMap((team) => team.members.map((member) => member.userId)));

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
      <label className="assignment-option everyone-option"><input type="checkbox" checked={value.allMembers} onChange={(event) => { setAssignmentError(false); setValue((current) => normalizeAssignments({ ...current, allMembers: event.target.checked }, teams)); }} /><span><strong>Herkes</strong><small>Projede yer alan tüm aktif üyeler</small></span></label>
      {teams.length > 0 && <div className="assignment-group"><strong>Takımlar</strong><div className="assignment-options">{teams.map((team) => <label className="assignment-option" key={team.id}><input type="checkbox" disabled={value.allMembers} checked={value.teamIds.includes(team.id)} onChange={() => { setAssignmentError(false); setValue((current) => normalizeAssignments({ ...current, teamIds: toggle(current.teamIds, team.id) }, teams)); }} /><span><strong>{team.name}</strong><small>{team.members.length} kişi</small></span></label>)}</div></div>}
      <div className="assignment-group"><strong>Proje üyeleri</strong><div className="assignment-options">{members.map((member) => { const coveredByTeam = selectedTeamMemberIds.has(member.userId); return <label className="assignment-option" key={member.userId}><input type="checkbox" disabled={value.allMembers || coveredByTeam} checked={value.assigneeIds.includes(member.userId)} onChange={() => { setAssignmentError(false); setValue((current) => normalizeAssignments({ ...current, assigneeIds: toggle(current.assigneeIds, member.userId) }, teams)); }} /><span><strong>{member.name}</strong><small>{coveredByTeam ? "Seçili takım kapsamında" : member.email}</small></span></label>; })}</div></div>
      {assignmentError && <span className="field-error">En az bir çalışan, takım veya “Herkes” seçeneğini belirleyin.</span>}
    </fieldset>

    <DateTimePicker value={value.dueAt} onChange={(dueAt) => setValue({ ...value, dueAt })} />
    <div className="dialog-actions">{onCancel && <button className="secondary-button" type="button" onClick={onCancel}>Vazgeç</button>}<button className="primary-button compact-button" disabled={busy}>{busy ? "Kaydediliyor…" : submitLabel}</button></div>
  </form>;
}
