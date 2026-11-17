import type { TaskPriority } from "../types";
import { priorityLabels } from "../projectFormat";
import styles from "./PriorityBadge.module.css";

export function PriorityBadge({ priority }: { priority: TaskPriority }) {
  return <span className={styles.badge} data-priority={priority} aria-label={`Öncelik: ${priorityLabels[priority]}`}>
    <span className={styles.dot} aria-hidden="true" />{priorityLabels[priority]}
  </span>;
}
