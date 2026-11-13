import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { useAuth } from "../useAuth";
import { RequireAdminRoute } from "./AuthRoutes";

vi.mock("../useAuth", () => ({ useAuth: vi.fn() }));

const mockedUseAuth = vi.mocked(useAuth);

describe("RequireAdminRoute", () => {
  beforeEach(() => {
    mockedUseAuth.mockReturnValue({
      status: "authenticated",
      user: {
        id: 2,
        name: "Employee",
        email: "employee@sectio.local",
        role: "EMPLOYEE",
        mustChangePassword: false,
      },
      startupError: null,
      login: vi.fn(),
      changePassword: vi.fn(),
      logout: vi.fn(),
      reloadSession: vi.fn(),
    });
  });

  it("redirects employees away from admin pages", () => {
    render(
      <MemoryRouter initialEntries={["/users"]}>
        <Routes>
          <Route path="/" element={<p>Genel bakış</p>} />
          <Route element={<RequireAdminRoute />}>
            <Route path="/users" element={<p>Kullanıcı yönetimi</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Genel bakış")).toBeInTheDocument();
    expect(screen.queryByText("Kullanıcı yönetimi")).not.toBeInTheDocument();
  });

  it("allows admins to open admin pages", () => {
    mockedUseAuth.mockReturnValue({
      ...mockedUseAuth(),
      user: {
        id: 1,
        name: "Admin",
        email: "admin@sectio.local",
        role: "ADMIN",
        mustChangePassword: false,
      },
    });

    render(
      <MemoryRouter initialEntries={["/users"]}>
        <Routes>
          <Route element={<RequireAdminRoute />}>
            <Route path="/users" element={<p>Kullanıcı yönetimi</p>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Kullanıcı yönetimi")).toBeInTheDocument();
  });
});
