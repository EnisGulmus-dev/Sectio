import type { ReactNode } from "react";

export function LoadingState({ label = "Yükleniyor…" }: { label?: string }) {
  return <div className="feedback-state" role="status">{label}</div>;
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="feedback-state">
      <strong>{title}</strong>
      {description && <p>{description}</p>}
      {action}
    </div>
  );
}

export function InlineAlert({ message }: { message: string }) {
  return <div className="form-alert" role="alert">{message}</div>;
}
