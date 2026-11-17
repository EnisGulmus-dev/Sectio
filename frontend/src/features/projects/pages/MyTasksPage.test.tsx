import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ToastProvider } from "../../../shared/ui/ToastProvider";
import { server } from "../../../test/server";
import { MyTasksPage } from "./MyTasksPage";

describe("MyTasksPage", () => {
  it("moves finished work out of active tasks and exposes completed and cancelled groups", async () => {
    let status = "TODO";
    const buckets: string[] = [];
    server.use(
      http.get("*/api/v1/tasks/my", ({ request }) => {
        const bucket = new URL(request.url).searchParams.get("bucket")!;
        buckets.push(bucket);
        const show = bucket === "ALL" || (bucket === "ACTIVE" && status === "TODO")
          || (bucket === "COMPLETED" && status === "DONE");
        const content = show ? [{ id: 70, projectId: 10, projectName: "Web Projesi", createdById: 1,
          createdByName: "Admin", assignees: [{ userId: 2, name: "Ada" }], teams: [],
          title: "API işi", description: null, status, priority: "HIGH", dueAt: null,
          completedAt: null, createdAt: "2026-11-17T00:00:00Z", updatedAt: "2026-11-17T00:00:00Z" }] : [];
        return HttpResponse.json({ content, page: 0, size: 20, totalElements: content.length,
          totalPages: content.length, first: true, last: true });
      }),
      http.patch("*/api/v1/tasks/70/status", async ({ request }) => {
        status = (await request.json() as { status: string }).status;
        return HttpResponse.json({ id: 70, projectId: 10, projectName: "Web Projesi", createdById: 1, createdByName: "Admin", assignees: [{ userId: 2, name: "Ada" }], teams: [], title: "API işi", description: null, status, priority: "HIGH", dueAt: null, completedAt: null, createdAt: "2026-11-17T00:00:00Z", updatedAt: "2026-11-17T00:00:00Z" });
      }),
      http.get("*/api/v1/tasks/70/progress", () => HttpResponse.json([])),
      http.get("*/api/v1/tasks/70/successors", () => HttpResponse.json([])),
      http.get("*/api/v1/tasks/70/handoff-history", () => HttpResponse.json([{ id: 1, sourceTaskId: 69, targetTaskId: 70,
        sourceTitle: "Geliştirme", targetTitle: "API işi", recipientPeople: "Ada", recipientTeams: "Test takımı",
        actorName: "Yönetici", handedOffAt: "2026-11-17T10:00:00Z" }])),
    );
    const user = userEvent.setup();
    render(<MemoryRouter><ToastProvider><MyTasksPage /></ToastProvider></MemoryRouter>);
    await screen.findByText("API işi");
    expect(buckets[0]).toBe("ACTIVE");
    await user.click(screen.getByRole("button", { name: "Görev detayını aç" }));
    const dialog = within(screen.getByRole("dialog", { name: "Görev detayı" }));
    expect(await dialog.findByText("Geliştirme → API işi")).toBeInTheDocument();
    expect(dialog.getByText(/Test takımı/)).toBeInTheDocument();
    await user.selectOptions(dialog.getByLabelText("Yeni durum"), "DONE");
    await user.type(dialog.getByLabelText("İlerleme notu"), "İş tamamlandı");
    await user.click(dialog.getByRole("button", { name: "Güncellemeyi kaydet" }));
    await waitFor(() => expect(status).toBe("DONE"));
    await user.click(dialog.getByRole("button", { name: "Pencereyi kapat" }));
    await waitFor(() => expect(screen.queryByText("API işi")).not.toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Tamamlanan görevler" }));
    expect(await screen.findByText("API işi")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "İptal edilen görevler" }));
    await waitFor(() => expect(screen.queryByText("API işi")).not.toBeInTheDocument());
    expect(buckets).toContain("COMPLETED");
    expect(buckets).toContain("CANCELLED");
  });
});
