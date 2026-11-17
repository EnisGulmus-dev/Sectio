import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { LoginPage } from "./LoginPage";
import { useAuth } from "../useAuth";

vi.mock("../useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("../../../shared/ui/useTheme", () => ({ useTheme: () => ({ theme: "cloud", toggleTheme: vi.fn() }) }));

describe("LoginPage", () => {
  it("allows a Turkish email address to reach authentication", async () => {
    const login = vi.fn().mockResolvedValue({ mustChangePassword: false });
    vi.mocked(useAuth).mockReturnValue({ login, startupError: null } as unknown as ReturnType<typeof useAuth>);
    const user = userEvent.setup();
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await user.type(screen.getByLabelText("Email address"), "hüseyin@gmail.com");
    await user.type(screen.getByLabelText(/^Password/), "temporary-123");
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    await waitFor(() => expect(login).toHaveBeenCalledWith({ email: "hüseyin@gmail.com", password: "temporary-123" }));
  });
});
