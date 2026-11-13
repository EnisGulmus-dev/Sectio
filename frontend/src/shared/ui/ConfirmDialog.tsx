import { useEffect, useRef } from "react";

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Onayla",
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog className="confirm-dialog" ref={dialogRef} onCancel={onCancel}>
      <h2>{title}</h2>
      <p>{description}</p>
      <div className="button-row">
        <button className="secondary-button" type="button" onClick={onCancel}>Vazgeç</button>
        <button className="danger-button" disabled={busy} type="button" onClick={onConfirm}>
          {busy ? "İşleniyor…" : confirmLabel}
        </button>
      </div>
    </dialog>
  );
}
