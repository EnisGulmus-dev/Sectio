import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { ToastProvider } from "../../../shared/ui/ToastProvider";
import { server } from "../../../test/server";
import { UsersPage } from "./UsersPage";

function renderPage() {
  return render(
    <ToastProvider>
      <UsersPage />
    </ToastProvider>,
  );
}

describe("UsersPage", () => {
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
    await user.type(screen.getByLabelText("Ad soyad"), "Yeni Çalışan");
    await user.type(screen.getByLabelText("E-posta"), "employee@sectio.local");
    await user.type(screen.getByLabelText(/^Geçici parola/), "temporary-123");
    await user.click(screen.getByRole("button", { name: "Kaydet" }));

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
});
