import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { ToastProvider } from "../../../shared/ui/ToastProvider";
import { ProjectsPage } from "./ProjectsPage";

vi.mock("../../auth/useAuth", () => ({
  useAuth: () => ({ user: { id: 1, name: "Sectio Admin", email: "admin@sectio.local", role: "ADMIN", jobTitle: "PROJECT_MANAGER", mustChangePassword: false } }),
}));

describe("ProjectsPage", () => {
  it("lists accessible projects", async () => {
    render(<MemoryRouter><ToastProvider><ProjectsPage /></ToastProvider></MemoryRouter>);
    expect(await screen.findByText("Web Projesi")).toBeInTheDocument();
    expect(screen.getByText("Sahip: Sectio Admin")).toBeInTheDocument();
  });
});
