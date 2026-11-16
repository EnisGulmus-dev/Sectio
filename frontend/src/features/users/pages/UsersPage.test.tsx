import { render, screen, within, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { ToastProvider } from "../../../shared/ui/ToastProvider";
import { server } from "../../../test/server";
import { UsersPage } from "./UsersPage";

function renderPage() {
  return render(
    <MemoryRouter><ToastProvider>
      <UsersPage />
    </ToastProvider></MemoryRouter>,
  );
}

describe("UsersPage", () => {
  it("deletes a user from the normal list while keeping history read-only", async () => {
    const employee = { id: 2, name: "Ayrılan Çalışan", email: "departing@example.com",
      role: "EMPLOYEE", jobTitle: "SOFTWARE_DEVELOPER", active: true,
      mustChangePassword: false, deletedAt: null, createdAt: "2026-11-16T00:00:00Z", updatedAt: "2026-11-16T00:00:00Z" };
    let closed = false;
    server.use(
      http.get("*/api/v1/admin/users", ({ request }) => HttpResponse.json(
        new URL(request.url).searchParams.get("deleted") === "true"
          ? closed ? [{ ...employee, active: false, deletedAt: "2026-11-16T12:00:00Z" }] : []
          : closed ? [] : [employee],
      )),
      http.get("*/api/v1/admin/users/2/deletion-impact", () => HttpResponse.json({
        ownedProjects: 1, projectMemberships: 2, assignedTasks: 3, createdTasks: 4, affectedTasks: 5,
      })),
      http.delete("*/api/v1/admin/users/2", () => { closed = true; return new HttpResponse(null, { status: 204 }); }),
      http.get("*/api/v1/admin/users/2/work-tracking", () => HttpResponse.json({
        content: [{ id: "ended:10", taskId: 70, projectId: 10, taskTitle: "Önceki görev", projectName: "Eski proje",
          status: "IN_PROGRESS", assignmentState: "ENDED", activityAt: "2026-11-16T12:00:00Z", completedAt: null, endReason: "USER_DELETED" }],
        page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true,
      })),
    );
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Ayrılan Çalışan");
    await user.click(screen.getByRole("button", { name: "Sil" }));
    const dialog = await screen.findByRole("dialog");
    expect(dialog).toHaveTextContent("Kullanıcı silinsin mi?");
    expect(dialog).toHaveTextContent("iş geçmişi");
    expect(dialog).toHaveTextContent("Atanmamış görevler");
    await user.click(within(dialog).getByRole("button", { name: "Kullanıcıyı sil" }));
    expect(await screen.findByText("0 kayıt")).toBeInTheDocument();
    expect(closed).toBe(true);
    expect(screen.queryByText("departing@example.com")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Silinenler" }));
    expect(await screen.findByText("departing@example.com")).toBeInTheDocument();
    expect(screen.getByText("Silindi")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Aktifleştir" })).not.toBeInTheDocument();
    expect(screen.getByText("1 kayıt")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "İş takibi" }));
    const history = await screen.findByRole("dialog", { name: "Ayrılan Çalışan · İş takibi" });
    expect(await within(history).findByText("Önceki görev")).toBeInTheDocument();
    expect(within(history).getByText("Eski proje")).toBeInTheDocument();
    expect(within(history).getByText(/Kullanıcı silindi/)).toBeInTheDocument();
    expect(within(history).queryByRole("button", { name: "Düzenle" })).not.toBeInTheDocument();
  });

  it("shows completed work for an active employee without deleting them", async () => {
    const employee = { id: 2, name: "Aktif Çalışan", email: "active@example.com",
      role: "EMPLOYEE", jobTitle: "SOFTWARE_DEVELOPER", active: true, deletedAt: null,
      mustChangePassword: false, createdAt: "2026-11-16T00:00:00Z", updatedAt: "2026-11-16T00:00:00Z" };
    let requestedStatus: string | null = null;
    const done = { id: "current:90", taskId: 90, projectId: 10, taskTitle: "Tamamlanan çalışma", projectName: "Web Projesi",
      status: "DONE", assignmentState: "CURRENT", activityAt: "2026-11-16T12:00:00Z", completedAt: "2026-11-16T12:00:00Z", endReason: null };
    server.use(
      http.get("*/api/v1/admin/users", () => HttpResponse.json([employee])),
      http.get("*/api/v1/admin/users/2/work-tracking", ({ request }) => {
        requestedStatus = new URL(request.url).searchParams.get("status");
        const content = requestedStatus === "DONE" ? [done] : [done,
          { ...done, id: "current:91", taskId: 91, taskTitle: "Devam eden çalışma", status: "IN_PROGRESS", completedAt: null }];
        return HttpResponse.json({ content, page: 0, size: 20, totalElements: content.length,
          totalPages: 1, first: true, last: true });
      }),
    );
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Aktif Çalışan");
    await user.click(screen.getByRole("button", { name: "İş takibi" }));
    const dialog = await screen.findByRole("dialog", { name: "Aktif Çalışan · İş takibi" });
    expect(await within(dialog).findByText("Tamamlanan çalışma")).toBeInTheDocument();
    expect(within(dialog).getByText("Devam eden çalışma")).toBeInTheDocument();
    await user.selectOptions(within(dialog).getByRole("combobox", { name: "İş durumuna göre filtrele" }), "DONE");
    await waitFor(() => {
      expect(within(dialog).getByText("Tamamlanan çalışma")).toBeInTheDocument();
      expect(within(dialog).queryByText("Devam eden çalışma")).not.toBeInTheDocument();
    });
    expect(requestedStatus).toBe("DONE");
    expect(screen.getByText("active@example.com")).toBeInTheDocument();
    expect(screen.getByText("Aktif")).toBeInTheDocument();
  });

  it("temporarily deactivates without removing the user from the normal list", async () => {
    const employee = { id: 2, name: "Geçici Pasif", email: "temporary@example.com",
      role: "EMPLOYEE", jobTitle: "SOFTWARE_DEVELOPER", active: true, deletedAt: null,
      mustChangePassword: false, createdAt: "2026-11-16T00:00:00Z", updatedAt: "2026-11-16T00:00:00Z" };
    server.use(
      http.get("*/api/v1/admin/users", () => HttpResponse.json([employee])),
      http.patch("*/api/v1/admin/users/2/status", () => HttpResponse.json({ ...employee, active: false })),
    );
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Geçici Pasif");
    await user.click(screen.getByRole("button", { name: "Pasifleştir" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Pasifleştir" }));
    expect(await screen.findByText("Pasif")).toBeInTheDocument();
    expect(screen.getByText("temporary@example.com")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Aktifleştir" })).toBeEnabled();
    expect(screen.getByText("1 kayıt")).toBeInTheDocument();
  });

  it("defaults a new administrator to CIO and allows an explicit title choice", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Sectio Admin");
    await user.click(screen.getByRole("button", { name: "Kullanıcı ekle" }));
    const dialog = screen.getByRole("dialog", { name: "Yeni kullanıcı" });
    const role = within(dialog).getByRole("combobox", { name: "Rol" });
    const title = within(dialog).getByRole("combobox", { name: /^İş unvanı/ });
    await user.selectOptions(role, "ADMIN");
    expect(title).toHaveValue("CIO");
    await user.selectOptions(title, "PROJECT_MANAGER");
    expect(title).toHaveValue("PROJECT_MANAGER");
    await user.selectOptions(role, "EMPLOYEE");
    expect(title).toHaveValue("SOFTWARE_DEVELOPER");
  });

  it("lists users returned by the API", async () => {
    renderPage();

    expect(await screen.findByText("Sectio Admin")).toBeInTheDocument();
    expect(screen.getByText("admin@sectio.local")).toBeInTheDocument();
    expect(screen.getByText("1 kayıt")).toBeInTheDocument();
  });

  it("creates a user and shows it in the table", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Sectio Admin");
    await user.click(screen.getByRole("button", { name: "Kullanıcı ekle" }));
    const dialog = screen.getByRole("dialog", { name: "Yeni kullanıcı" });
    await user.type(within(dialog).getByLabelText("Ad soyad"), "Yeni Çalışan");
    await user.type(within(dialog).getByLabelText("E-posta"), "employee@sectio.local");
    await user.type(within(dialog).getByLabelText(/^Geçici parola/), "temporary-123");
    await user.click(within(dialog).getByRole("button", { name: "Kaydet" }));

    expect(await screen.findByText("Yeni Çalışan")).toBeInTheDocument();
    expect(screen.getByText("employee@sectio.local")).toBeInTheDocument();
    expect(screen.getByText(/İlk girişte parolasını değiştirmesi gerekecek/)).toBeInTheDocument();
  });

  it("shows the standardized API error", async () => {
    server.use(
      http.get("*/api/v1/admin/users", () =>
        HttpResponse.json({
          code: "SERVICE_UNAVAILABLE",
          message: "Kullanıcı servisi kullanılamıyor",
          fieldErrors: {},
        }, { status: 503 }),
      ),
    );

    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("Kullanıcı servisi kullanılamıyor");
  });

  it("shows a popup notification for an incomplete email format", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Sectio Admin");
    await user.click(screen.getByRole("button", { name: "Kullanıcı ekle" }));
    const dialog = screen.getByRole("dialog", { name: "Yeni kullanıcı" });
    await user.type(within(dialog).getByLabelText("Ad soyad"), "Eksik Email");
    await user.type(within(dialog).getByLabelText("E-posta"), "employee@local");
    await user.type(within(dialog).getByLabelText(/^Geçici parola/), "temporary-123");
    await user.click(within(dialog).getByRole("button", { name: "Kaydet" }));

    expect(await screen.findByText(/Geçerli bir e-posta adresi girin/)).toBeInTheDocument();
  });

  it("can reveal and hide the temporary password", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Sectio Admin");
    await user.click(screen.getByRole("button", { name: "Kullanıcı ekle" }));
    const dialog = screen.getByRole("dialog", { name: "Yeni kullanıcı" });
    const password = within(dialog).getByLabelText(/^Geçici parola/);
    expect(password).toHaveAttribute("type", "password");
    await user.click(within(dialog).getByRole("button", { name: "Parolayı göster" }));
    expect(password).toHaveAttribute("type", "text");
  });
});
