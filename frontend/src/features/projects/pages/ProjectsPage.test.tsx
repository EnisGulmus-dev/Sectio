import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "../../../test/server";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { ToastProvider } from "../../../shared/ui/ToastProvider";
import { ProjectsPage } from "./ProjectsPage";

vi.mock("../../auth/useAuth", () => ({
  useAuth: () => ({ user: { id: 1, name: "Sectio Admin", email: "admin@sectio.local", role: "ADMIN", jobTitle: "PROJECT_MANAGER", mustChangePassword: false } }),
}));

describe("ProjectsPage", () => {
  it("creates a project and its selected members in one request", async () => {
    let input: unknown;
    server.use(http.post("*/api/v1/projects", async ({ request }) => {
      input = await request.json();
      return HttpResponse.json({ id: 11 }, { status: 201 });
    }));
    const user = userEvent.setup();
    render(<MemoryRouter><ToastProvider><ProjectsPage /></ToastProvider></MemoryRouter>);
    await user.click(screen.getByRole("button", { name: "Yeni proje" }));
    const dialog = within(screen.getByRole("dialog", { name: "Yeni proje" }));
    await user.type(dialog.getByLabelText("Proje adı"), "Yeni çalışma");
    await user.click(await dialog.findByRole("checkbox", { name: /Ada Çalışan/ }));
    await user.click(dialog.getByRole("button", { name: "Oluştur" }));
    await waitFor(() => expect(input).toEqual({ name: "Yeni çalışma", description: "", memberIds: [2] }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });
  it("lists accessible projects", async () => {
    render(<MemoryRouter><ToastProvider><ProjectsPage /></ToastProvider></MemoryRouter>);
    expect(await screen.findByText("Web Projesi")).toBeInTheDocument();
    expect(screen.getByText("Sahip: Sectio Admin")).toBeInTheDocument();
  });
});
