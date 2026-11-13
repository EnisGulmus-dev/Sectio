import { http, HttpResponse } from "msw";

export const initialUsers = [
  {
    id: 1,
    name: "Sectio Admin",
    email: "admin@sectio.local",
    role: "ADMIN" as const,
    jobTitle: "MANAGER" as const,
    active: true,
    mustChangePassword: false,
    createdAt: "2026-11-13T09:00:00Z",
    updatedAt: "2026-11-13T09:00:00Z",
  },
];

export const handlers = [
  http.get("*/api/v1/projects/10/member-candidates", () => HttpResponse.json([
    { id: 2, name: "Ada Çalışan", email: "ada@sectio.local", role: "EMPLOYEE", jobTitle: "EMPLOYEE" },
    { id: 3, name: "Mert Lider", email: "mert@sectio.local", role: "EMPLOYEE", jobTitle: "TEAM_LEADER" },
  ])),
  http.get("*/api/v1/projects/10/members", () => HttpResponse.json([
    { userId: 1, name: "Sectio Admin", email: "admin@sectio.local", role: "ADMIN", jobTitle: "MANAGER", joinedAt: "2026-11-13T10:00:00Z" },
  ])),
  http.get("*/api/v1/projects/10/tasks*", () => HttpResponse.json({
    content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true,
  })),
  http.get("*/api/v1/projects/10", () => HttpResponse.json({
    id: 10, ownerId: 1, ownerName: "Sectio Admin", name: "Web Projesi", description: "İlk proje", archived: false, createdAt: "2026-11-13T10:00:00Z", updatedAt: "2026-11-13T10:00:00Z",
  })),
  http.post("*/api/v1/projects/10/members", async ({ request }) => {
    const input = await request.json() as { userIds: number[] };
    return HttpResponse.json(input.userIds.map((userId) => ({
      userId,
      name: userId === 2 ? "Ada Çalışan" : "Mert Lider",
      email: userId === 2 ? "ada@sectio.local" : "mert@sectio.local",
      role: "EMPLOYEE",
      jobTitle: userId === 2 ? "EMPLOYEE" : "TEAM_LEADER",
      joinedAt: "2026-11-13T10:30:00Z",
    })), { status: 201 });
  }),
  http.get("*/api/v1/projects*", () => HttpResponse.json({
    content: [{ id: 10, ownerId: 1, ownerName: "Sectio Admin", name: "Web Projesi", description: "İlk proje", archived: false, createdAt: "2026-11-13T10:00:00Z", updatedAt: "2026-11-13T10:00:00Z" }],
    page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true,
  })),
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
      jobTitle: "EMPLOYEE" | "TEAM_LEADER" | "MANAGER";
    };
    return HttpResponse.json({
      id: 2,
      name: input.name,
      email: input.email,
      role: input.role,
      jobTitle: input.jobTitle,
      active: true,
      mustChangePassword: true,
      createdAt: "2026-11-13T10:00:00Z",
      updatedAt: "2026-11-13T10:00:00Z",
    }, { status: 201 });
  }),
];
