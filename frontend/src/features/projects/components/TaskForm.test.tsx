import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { TaskForm } from "./TaskForm";

const members = [
  { userId: 2, name: "Ada Çalışan", email: "ada@sectio.local", role: "EMPLOYEE" as const, jobTitle: "SOFTWARE_DEVELOPER" as const, joinedAt: "2026-11-13T10:00:00Z" },
  { userId: 3, name: "Ece Lider", email: "ece@sectio.local", role: "EMPLOYEE" as const, jobTitle: "TEAM_LEADER" as const, joinedAt: "2026-11-13T10:00:00Z" },
];

const teams = [{
  id: 11,
  name: "Platform Takımı",
  createdAt: "2026-11-13T10:00:00Z",
  members: members.map(({ userId, name, email, jobTitle }) => ({ userId, name, email, jobTitle })),
}];

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

  it("clears and locks other assignments when everyone is selected", async () => {
    const user = userEvent.setup();
    render(<TaskForm members={members} teams={teams} submitLabel="Görev oluştur" onSubmit={vi.fn()} />);

    await user.click(screen.getByLabelText(/Ada Çalışan/));
    await user.click(screen.getByLabelText(/Herkes/));

    expect(screen.getByLabelText(/Platform Takımı/)).toBeDisabled();
    expect(screen.getByLabelText(/Ada Çalışan/)).toBeDisabled();
    expect(screen.getByLabelText(/Ada Çalışan/)).not.toBeChecked();
  });

  it("replaces individual selections with the matching team", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<TaskForm members={members} teams={teams} submitLabel="Görev oluştur" onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("Başlık"), "Takım görevi");
    await user.click(screen.getByLabelText(/Ada Çalışan/));
    await user.click(screen.getByLabelText(/Ece Lider/));

    expect(screen.getByLabelText(/Platform Takımı/)).toBeChecked();
    expect(screen.getByLabelText(/Ada Çalışan/)).toBeDisabled();
    expect(screen.getByLabelText(/Ece Lider/)).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Görev oluştur" }));
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      assigneeIds: [],
      teamIds: [11],
      allMembers: false,
    }));
  });

  it("removes duplicate people when their team is selected", async () => {
    const user = userEvent.setup();
    render(<TaskForm members={members} teams={teams} submitLabel="Görev oluştur" onSubmit={vi.fn()} />);

    await user.click(screen.getByLabelText(/Ada Çalışan/));
    await user.click(screen.getByLabelText(/Platform Takımı/));

    expect(screen.getByLabelText(/Ada Çalışan/)).not.toBeChecked();
    expect(screen.getByLabelText(/Ada Çalışan/)).toBeDisabled();
    expect(screen.getByLabelText(/Ece Lider/)).toBeDisabled();
  });
});
