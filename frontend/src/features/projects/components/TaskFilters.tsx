import { priorityLabels, statusLabels } from "../projectFormat";
import type { ProjectMember, TaskPriority, TaskStatus, TaskBucket } from "../types";

export type TaskFilterValue = { query: string; status: TaskStatus | ""; priority: TaskPriority | ""; assigneeId: string };
export const emptyTaskFilters: TaskFilterValue = { query: "", status: "", priority: "", assigneeId: "" };

type Props = { value: TaskFilterValue; members?: ProjectMember[]; bucket?: TaskBucket; disabled?: boolean; onChange: (value: TaskFilterValue) => void; onApply: () => void; onClear: () => void };

export function TaskFilters({ value, members, bucket = "ALL", disabled = false, onChange, onApply, onClear }: Props) {
  const visibleStatuses = Object.entries(statusLabels).filter(([status]) => bucket === "ALL"
    || (bucket === "ACTIVE" && (status === "TODO" || status === "IN_PROGRESS"))
    || (bucket === "COMPLETED" && status === "DONE") || (bucket === "CANCELLED" && status === "CANCELLED"));
  return <div className="task-filter-bar">
    <input disabled={disabled} aria-label="Görev ara" placeholder="Görev ara" value={value.query} onChange={(event) => onChange({ ...value, query: event.target.value })} onKeyDown={(event) => { if (event.key === "Enter") onApply(); }} />
    <select disabled={disabled} aria-label="Duruma göre filtrele" value={value.status} onChange={(event) => onChange({ ...value, status: event.target.value as TaskStatus | "" })}><option value="">Tüm durumlar</option>{visibleStatuses.map(([status, label]) => <option value={status} key={status}>{label}</option>)}</select>
    <select disabled={disabled} aria-label="Önceliğe göre filtrele" value={value.priority} onChange={(event) => onChange({ ...value, priority: event.target.value as TaskPriority | "" })}><option value="">Tüm öncelikler</option>{Object.entries(priorityLabels).map(([priority, label]) => <option value={priority} key={priority}>{label}</option>)}</select>
    {members && <select disabled={disabled} aria-label="Atanana göre filtrele" value={value.assigneeId} onChange={(event) => onChange({ ...value, assigneeId: event.target.value })}><option value="">Tüm görevler</option><option value="unassigned">Atanmamış görevler</option>{members.map((member) => <option value={member.userId} key={member.userId}>{member.name}</option>)}</select>}
    <button disabled={disabled} className="secondary-button" type="button" onClick={onApply}>Uygula</button>
    <button disabled={disabled} className="text-button neutral" type="button" onClick={onClear}>Temizle</button>
  </div>;
}
