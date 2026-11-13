import { priorityLabels, statusLabels } from "../projectFormat";
import type { ProjectMember, TaskPriority, TaskStatus } from "../types";

export type TaskFilterValue = { query: string; status: TaskStatus | ""; priority: TaskPriority | ""; assigneeId: string };
export const emptyTaskFilters: TaskFilterValue = { query: "", status: "", priority: "", assigneeId: "" };

type Props = { value: TaskFilterValue; members?: ProjectMember[]; onChange: (value: TaskFilterValue) => void; onApply: () => void; onClear: () => void };

export function TaskFilters({ value, members, onChange, onApply, onClear }: Props) {
  return <div className="task-filter-bar">
    <input aria-label="Görev ara" placeholder="Görev ara" value={value.query} onChange={(event) => onChange({ ...value, query: event.target.value })} onKeyDown={(event) => { if (event.key === "Enter") onApply(); }} />
    <select aria-label="Duruma göre filtrele" value={value.status} onChange={(event) => onChange({ ...value, status: event.target.value as TaskStatus | "" })}><option value="">Tüm durumlar</option>{Object.entries(statusLabels).map(([status, label]) => <option value={status} key={status}>{label}</option>)}</select>
    <select aria-label="Önceliğe göre filtrele" value={value.priority} onChange={(event) => onChange({ ...value, priority: event.target.value as TaskPriority | "" })}><option value="">Tüm öncelikler</option>{Object.entries(priorityLabels).map(([priority, label]) => <option value={priority} key={priority}>{label}</option>)}</select>
    {members && <select aria-label="Atanana göre filtrele" value={value.assigneeId} onChange={(event) => onChange({ ...value, assigneeId: event.target.value })}><option value="">Tüm üyeler</option>{members.map((member) => <option value={member.userId} key={member.userId}>{member.name}</option>)}</select>}
    <button className="secondary-button" type="button" onClick={onApply}>Uygula</button>
    <button className="text-button neutral" type="button" onClick={onClear}>Temizle</button>
  </div>;
}
