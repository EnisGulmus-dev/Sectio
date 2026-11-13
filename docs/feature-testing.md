# Feature verification

Start the integrated application with `backend\.\mvnw.cmd spring-boot:run`, sign in,
and use the URLs below.

## Projects

- UI: `/projects`, `/projects/{id}`
- API: `GET/POST /api/v1/projects`, `GET/PATCH /api/v1/projects/{id}`,
  `POST /api/v1/projects/{id}/archive`

Verify that an admin can create and archive a project. A project owner is added
as its first member automatically. An archived project remains visible with the
archive filter and rejects further edits.

## Project members

- API: `GET/POST /api/v1/projects/{id}/members`
- API: `GET /api/v1/projects/{id}/member-candidates`
- API: `DELETE /api/v1/projects/{id}/members/{userId}`

Add an active user from the project detail page. Duplicate and inactive users
must be rejected. The owner cannot be removed. An employee who is neither owner
nor member must receive `403` for project access.

## Tasks

- Project tasks: `GET/POST /api/v1/projects/{id}/tasks`
- Task detail: `GET/PATCH/DELETE /api/v1/tasks/{id}`
- Status: `PATCH /api/v1/tasks/{id}/status`
- Personal list: `GET /api/v1/tasks/my`, UI `/my-tasks`

Create a task from a project detail page, assign an active member and change its
status from TODO to IN_PROGRESS and DONE. DONE sets `completedAt`; moving away
from DONE clears it. A non-member or inactive assignee must be rejected.

## Dashboard

`GET /api/v1/dashboard/summary` returns open, completed and overdue task counts.
Admins see system-wide totals; employees only see tasks assigned to themselves.

## Automated checks

Run `backend\.\mvnw.cmd test`. This executes frontend dependency verification,
the production frontend build, JUnit tests, and Vitest/MSW tests. Docker-backed
Testcontainers tests are skipped when Docker is unavailable.
