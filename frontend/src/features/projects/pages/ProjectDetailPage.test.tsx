import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { ToastProvider } from "../../../shared/ui/ToastProvider";
import { ProjectDetailPage } from "./ProjectDetailPage";

vi.mock("../../auth/useAuth", () => ({
  useAuth: () => ({ user: { id: 1, name: "Sectio Admin", email: "admin@sectio.local", role: "ADMIN", jobTitle: "PROJECT_MANAGER", mustChangePassword: false } }),
}));

describe("ProjectDetailPage", () => {
  it("opens a linked tab and supports keyboard navigation", async () => {
    const user = userEvent.setup();
    render(<MemoryRouter initialEntries={["/projects/10?tab=tasks"]}><ToastProvider><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></ToastProvider></MemoryRouter>);
    const tasks = await screen.findByRole("tab", { name: "Görevler" });
    expect(tasks).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel", { name: "Görevler" })).toBeVisible();
    tasks.focus();
    await user.keyboard("{ArrowRight}");
    expect(screen.getByRole("tab", { name: "Takımlar ve üyeler" })).toHaveFocus();
    expect(screen.getByRole("tabpanel", { name: "Takımlar ve üyeler" })).toBeVisible();
    await user.keyboard("{Home}");
    expect(screen.getByRole("tabpanel", { name: "Genel bakış" })).toBeVisible();
    expect(screen.queryByRole("button", { name: "Çalışan ekle" })).not.toBeInTheDocument();
  });
  it("supports searching, outside-click close and selecting members", async () => {
    const user = userEvent.setup();
    render(<MemoryRouter initialEntries={["/projects/10"]}><ToastProvider><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></ToastProvider></MemoryRouter>);

    await screen.findByRole("tab", { name: "Genel bakış" });
    await user.click(screen.getByRole("tab", { name: "Takımlar ve üyeler" }));
    await user.click(screen.getByRole("button", { name: "Çalışan ekle" }));
    const search = screen.getByRole("combobox", { name: "Eklenecek üyeyi ara" });
    await user.click(search);
    expect(await screen.findByRole("option", { name: /Ada Çalışan/ })).toBeInTheDocument();

    await user.type(search, "ada@");
    expect(screen.getByText(/ada@sectio.local/)).toBeInTheDocument();

    await user.click(screen.getByRole("heading", { name: "Projeye çalışan ekle" }));
    expect(screen.queryByRole("option", { name: /Ada Çalışan/ })).not.toBeInTheDocument();

    await user.click(search);
    await user.click(await screen.findByRole("option", { name: /Ada Çalışan/ }));
    await user.click(await screen.findByRole("option", { name: /Mert Lider/ }));
    expect(screen.getByRole("button", { name: "2 üyeyi ekle" })).toBeEnabled();
    await user.click(screen.getByRole("button", { name: "2 üyeyi ekle" }));
    expect(await screen.findByText("2 üye eklendi")).toBeInTheDocument();
  });
});
