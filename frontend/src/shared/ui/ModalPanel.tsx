import { useEffect, useId, useRef, type MouseEvent, type ReactNode } from "react";

type Props = {
  open: boolean;
  title: string;
  children: ReactNode;
  onClose: () => void;
  className?: string;
};

export function ModalPanel({ open, title, children, onClose, className = "" }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      if (typeof dialog.showModal === "function") dialog.showModal();
      else dialog.setAttribute("open", "");
    }
    if (!open && dialog.open) {
      if (typeof dialog.close === "function") dialog.close();
      else dialog.removeAttribute("open");
    }
  }, [open]);

  function closeFromBackdrop(event: MouseEvent<HTMLDialogElement>) {
    if (event.target === event.currentTarget) onClose();
  }

  return <dialog className={`form-dialog${className ? ` ${className}` : ""}`} ref={ref} aria-labelledby={titleId} onCancel={onClose} onClick={closeFromBackdrop}>
    <section className="form-dialog-card">
      <header><h2 id={titleId}>{title}</h2><button type="button" aria-label="Pencereyi kapat" onClick={onClose}>×</button></header>
      <div className="form-dialog-body">{children}</div>
    </section>
  </dialog>;
}
