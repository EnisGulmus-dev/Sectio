import { render, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { server } from "../test/server";
import { AppLayout } from "./AppLayout";

vi.mock("../features/auth/useAuth", () => ({ useAuth: () => ({ user: { id: 1, name: "Ada", role: "ADMIN" }, logout: vi.fn() }) }));
vi.mock("../shared/ui/useTheme", () => ({ useTheme: () => ({ theme: "cloud", toggleTheme: vi.fn() }) }));

describe("AppLayout", () => {
  it("refreshes the saved profile in the sidebar and keeps actions accessible", async () => {
    let displayName = "Ada";
    server.use(http.get("*/api/v1/profile", () => HttpResponse.json({ userId: 1, name: "Ada", displayName, jobTitle: "CIO", avatarId: "erkek_01_sakalsiz", hasPhoto: false })));
    render(<MemoryRouter><AppLayout /></MemoryRouter>);
    expect(await screen.findByText("Ada")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Çıkış yap" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Koyu temaya geç" })).toBeInTheDocument();
    displayName = "Yeni görünen ad";
    window.dispatchEvent(new Event("sectio-profile-updated"));
    await waitFor(() => expect(screen.getByText("Yeni görünen ad")).toBeInTheDocument());
  });
});
