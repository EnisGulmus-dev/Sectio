import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { TaskForm } from "./TaskForm";

const members = [{ userId: 2, name: "Ada Çalışan", email: "ada@sectio.local", role: "EMPLOYEE" as const, jobTitle: "EMPLOYEE" as const, joinedAt: "2026-11-13T10:00:00Z" }];

describe("TaskForm", () => {
  it("submits editable task fields", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<TaskForm members={members} submitLabel="Görev oluştur" onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("Başlık"), "API sözleşmesini tamamla");
    await user.click(screen.getByLabelText(/Ada Çalışan/));
    await user.selectOptions(screen.getByLabelText("Öncelik"), "HIGH");
    await user.type(screen.getByLabelText("Açıklama"), "OpenAPI şemalarını ekle");
    await user.click(screen.getByRole("button", { name: "Görev oluştur" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      title: "API sözleşmesini tamamla",
      assigneeIds: [2],
      priority: "HIGH",
      description: "OpenAPI şemalarını ekle",
    }));
  });
});
