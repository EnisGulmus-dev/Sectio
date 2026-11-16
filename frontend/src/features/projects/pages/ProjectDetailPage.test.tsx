import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { ToastProvider } from "../../../shared/ui/ToastProvider";
import { ProjectDetailPage } from "./ProjectDetailPage";
import { server } from "../../../test/server";

vi.mock("../../auth/useAuth", () => ({
  useAuth: () => ({ user: { id: 1, name: "Sectio Admin", email: "admin@sectio.local", role: "ADMIN", jobTitle: "PROJECT_MANAGER", mustChangePassword: false } }),
}));

describe("ProjectDetailPage", () => {
  it("keeps an unassigned task visible with a reassignment action", async () => {
    server.use(http.get("*/api/v1/projects/10/tasks", () => HttpResponse.json({
      content: [{ id: 70, projectId: 10, projectName: "Web Projesi", createdById: 2,
        createdByName: "Ayrılan Çalışan", assignees: [], title: "Korunan görev", description: null,
        status: "TODO", priority: "MEDIUM", dueAt: null, completedAt: null,
        createdAt: "2026-11-16T00:00:00Z", updatedAt: "2026-11-16T00:00:00Z" }],
      page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true,
    })));
    render(<MemoryRouter initialEntries={["/projects/10?tab=tasks"]}><ToastProvider><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></ToastProvider></MemoryRouter>);
    expect(await screen.findByText("Korunan görev")).toBeInTheDocument();
    expect(screen.getByText(/^Atanmamış · Son tarih:/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Düzenle" })).toBeEnabled();
  });

  it("offers a direct unassigned filter and requests only unassigned tasks", async () => {
    let lastQuery: URLSearchParams | null = null;
    const unassigned = { id: 70, projectId: 10, projectName: "Web Projesi", createdById: 2,
      createdByName: "Former", assignees: [], title: "Boştaki iş", description: null,
      status: "TODO", priority: "MEDIUM", dueAt: null, completedAt: null,
      createdAt: "2026-11-16T00:00:00Z", updatedAt: "2026-11-16T00:00:00Z" };
    server.use(http.get("*/api/v1/projects/10/tasks", ({ request }) => {
      lastQuery = new URL(request.url).searchParams;
      const content = lastQuery.get("unassigned") === "true" ? [unassigned] : [unassigned,
        { ...unassigned, id: 71, title: "Atanmış iş", assignees: [{ userId: 1, name: "Admin" }] }];
      return HttpResponse.json({ content, page: 0, size: 20, totalElements: content.length,
        totalPages: 1, first: true, last: true });
    }));
    const user = userEvent.setup();
    render(<MemoryRouter initialEntries={["/projects/10?tab=tasks"]}><ToastProvider><Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes></ToastProvider></MemoryRouter>);
    await screen.findByText("Atanmış iş");
    await user.click(screen.getByRole("button", { name: "Atanmamış görevler" }));
    await waitFor(() => expect(screen.queryByText("Atanmış iş")).not.toBeInTheDocument());
    expect(screen.getByText("Boştaki iş")).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Atanana göre filtrele" })).toHaveValue("unassigned");
    expect(lastQuery!.get("unassigned")).toBe("true");
    expect(lastQuery!.has("assigneeId")).toBe(false);
  });

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
