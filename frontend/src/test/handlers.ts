import { http, HttpResponse } from "msw";

export const initialUsers = [
  {
    id: 1,
    name: "Sectio Admin",
    email: "admin@sectio.local",
    role: "ADMIN" as const,
    active: true,
    mustChangePassword: false,
    createdAt: "2026-11-13T09:00:00Z",
    updatedAt: "2026-11-13T09:00:00Z",
  },
];

export const handlers = [
  http.get("*/api/v1/auth/csrf", () =>
    HttpResponse.json({
      headerName: "X-XSRF-TOKEN",
      parameterName: "_csrf",
      token: "test-csrf-token",
    }),
  ),
  http.get("*/api/v1/admin/users", () =>
    HttpResponse.json(initialUsers),
  ),
  http.post("*/api/v1/admin/users", async ({ request }) => {
    const input = await request.json() as {
      name: string;
      email: string;
      role: "ADMIN" | "EMPLOYEE";
    };
    return HttpResponse.json({
      id: 2,
      name: input.name,
      email: input.email,
      role: input.role,
      active: true,
      mustChangePassword: true,
      createdAt: "2026-11-13T10:00:00Z",
      updatedAt: "2026-11-13T10:00:00Z",
    }, { status: 201 });
  }),
];
