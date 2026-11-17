import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ToastProvider } from "../../shared/ui/ToastProvider";
import { server } from "../../test/server";
import { ProfilePage } from "./ProfilePage";

vi.mock("../auth/useAuth", () => ({ useAuth: () => ({ user: { id: 1, role: "ADMIN", jobTitle: "CIO" } }) }));
afterEach(() => vi.restoreAllMocks());

describe("ProfilePage", () => {
  it("keeps sidebar unchanged when a profile commit fails", async () => {
    const profile = { userId: 1, name: "Ada", jobTitle: "SOFTWARE_DEVELOPER", displayName: "Eski", biography: null, timeZone: null, avatarId: null, hasPhoto: false };
    server.use(http.get("*/api/v1/profile", () => HttpResponse.json(profile)),
      http.get("*/api/v1/organization/users/1", () => HttpResponse.json({ userId: 1, managerId: null, managerName: null, teams: [] })));
    const originalFetch = globalThis.fetch;
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      if (String(input).endsWith("/api/v1/profile/commit") && init?.method === "POST") {
        return new Response(JSON.stringify({ code: "ERROR", message: "Kayıt başarısız" }), { status: 500, headers: { "Content-Type": "application/json" } });
      }
      return originalFetch(input, init);
    });
    const dispatch = vi.spyOn(window, "dispatchEvent");
    const user = userEvent.setup(); render(<ToastProvider><ProfilePage /></ToastProvider>);
    const name = await screen.findByLabelText("Görünen ad");
    await user.clear(name); await user.type(name, "Yeni");
    await user.click(screen.getByRole("button", { name: "Güncelle" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Kayıt başarısız");
    expect(dispatch.mock.calls.some(([event]) => event.type === "sectio-profile-updated")).toBe(false);
  });

  it("keeps avatar and profile edits as a draft until the single update", async () => {
    let calls = 0; let hasData = false;
    const profile = { userId: 1, name: "Ada", jobTitle: "SOFTWARE_DEVELOPER", displayName: null, biography: null, timeZone: null, avatarId: null, hasPhoto: false };
    server.use(
      http.get("*/api/v1/profile", () => HttpResponse.json(profile)),
      http.get("*/api/v1/organization/users/1", () => HttpResponse.json({ userId: 1, managerId: null, managerName: null, teams: [] })),
    );
    const originalFetch = globalThis.fetch;
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      if (String(input).endsWith("/api/v1/profile/commit") && init?.method === "POST") {
        calls++; hasData = (init.body as FormData).has("data");
        expect(new Headers(init.headers).get("Content-Type")).toBeNull();
        return new Response(JSON.stringify({ ...profile, displayName: "Deneme", avatarId: "erkek_01_sakalsiz" }), { status: 200, headers: { "Content-Type": "application/json" } });
      }
      return originalFetch(input, init);
    });
    const user = userEvent.setup();
    render(<ToastProvider><ProfilePage /></ToastProvider>);
    expect(await screen.findByText("Ada")).toBeInTheDocument();
    await user.type(screen.getByLabelText("Görünen ad"), "Deneme");
    const picker = within(screen.getByRole("group", { name: "Hazır avatar seç" }));
    await user.click(picker.getByRole("button", { name: "Erkek avatar 1" }));
    expect(calls).toBe(0);
    expect(picker.getByRole("button", { name: "Erkek avatar 1" })).toHaveAttribute("aria-pressed", "true");
    await user.click(screen.getByRole("button", { name: "Güncelle" }));
    await waitFor(() => expect(calls).toBe(1));
    expect(hasData).toBe(true);
    expect(await screen.findByText("Profil güncellendi.")).toBeInTheDocument();
  });
});
