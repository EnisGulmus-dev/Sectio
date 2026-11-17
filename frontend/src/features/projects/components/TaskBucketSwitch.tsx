import type { TaskBucket } from "../types";
import styles from "./TaskBucketSwitch.module.css";

export const taskBucketLabels: Record<TaskBucket, string> = {
  ACTIVE: "Aktif görevler", COMPLETED: "Tamamlanan görevler", CANCELLED: "İptal edilen görevler", ALL: "Tüm görevler",
};
export function readTaskBucket(value: string | null): TaskBucket {
  return value && Object.hasOwn(taskBucketLabels, value) ? value as TaskBucket : "ACTIVE";
}
export type TaskView = TaskBucket | "UNASSIGNED";
export function TaskBucketSwitch({ value, onChange, disabled = false, includeUnassigned = false }: {
  value: TaskView; onChange: (bucket: TaskView) => void; disabled?: boolean; includeUnassigned?: boolean;
}) {
  const views: TaskView[] = [...Object.keys(taskBucketLabels) as TaskBucket[], ...(includeUnassigned ? ["UNASSIGNED" as const] : [])];
  return <div className={styles.switch} role="group" aria-label="Görev görünümü">{views.map((bucket) =>
    <button key={bucket} type="button" aria-pressed={value === bucket} disabled={disabled} onClick={() => onChange(bucket)}>{bucket === "UNASSIGNED" ? "Atanmamış görevler" : taskBucketLabels[bucket]}</button>,
  )}</div>;
}
