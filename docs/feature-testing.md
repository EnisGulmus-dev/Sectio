# Feature verification

Start the integrated application with `backend\.\mvnw.cmd spring-boot:run`, sign in,
and use the URLs below.

## Users

- UI: `/users`
- API: `DELETE /api/v1/admin/users/{userId}`

Choose **Sil** and verify that an impact preview reports project memberships and
affected tasks before confirmation. On confirmation, memberships and assignments
are removed; tasks with no remaining assignee and tasks created by the deleted
user are deleted. The current admin and project owners remain protected;
project ownership must be transferred or the project deleted first.

Verify that invalid email formats show an error notification. Password fields on
login, mandatory password change, user creation and password reset must expose a
**Göster/Gizle** control. A passive user must receive the specific message
“Hesabınız pasife alındı, yöneticinizle görüşün” at login.

## Projects

- UI: `/projects`, `/projects/{id}`
- API: `GET/POST /api/v1/projects`, `GET/PATCH /api/v1/projects/{id}`,
  `POST /api/v1/projects/{id}/archive`

Verify that an admin can create and archive a project. A project owner is added
as its first member automatically. An archived project remains visible with the
archive filter and rejects further edits.

With the archive filter enabled, restore an archived project and verify that it
returns to the active list. Verify that both active and archived projects expose
permanent deletion and that deleting one also removes its tasks and memberships.

Create users with `EMPLOYEE`, `TEAM_LEADER` and `MANAGER` job titles. An admin,
team leader or manager can create a project; a regular employee cannot. These
job titles are separate from the `ADMIN`/`EMPLOYEE` security role.

## Project members

- API: `GET/POST /api/v1/projects/{id}/members`
- API: `GET /api/v1/projects/{id}/member-candidates`
- API: `DELETE /api/v1/projects/{id}/members/{userId}`
- Teams: `GET/POST /api/v1/projects/{id}/teams`,
  `PATCH/DELETE /api/v1/projects/{id}/teams/{teamId}`

Select multiple active users from the project detail page and add them in one
operation. Duplicate and inactive users
must be rejected. The owner cannot be removed. An employee who is neither owner
nor member must receive `403` for project access.

Focus the member search field and verify that eligible active users appear in a
dropdown. Search by name or email and select multiple results. Click outside the
picker and verify that the dropdown closes without clearing selected users. The
project owner must remain first in the member list.

Create “A Takımı” and select multiple project members. Verify that the team is
listed separately from project membership and can be selected while creating a
task. Deleting a team removes only the team heading; its users remain project
members and existing task assignments remain as an assignment snapshot.

Assign an active TODO or IN_PROGRESS task to a member and verify that member
removal returns a conflict. Change every assigned task to DONE or CANCELLED and
verify that the member can then be removed.

## Tasks

- Project tasks: `GET/POST /api/v1/projects/{id}/tasks`
- Task detail: `GET/PATCH/DELETE /api/v1/tasks/{id}`
- Status: `PATCH /api/v1/tasks/{id}/status`
- Personal list: `GET /api/v1/tasks/my`, UI `/my-tasks`

Create a task from the modal panel; assign multiple active members, a named team,
or **Herkes**, then change its
status from TODO to IN_PROGRESS, DONE and CANCELLED. DONE sets `completedAt`; moving away
from DONE clears it. Duplicate members selected directly and through a team are
deduplicated. A non-member or inactive assignee must be rejected. A task cannot
be created without an assignee; removing the final assignee deletes the task.

Verify that project information, members/teams, and tasks are independently
collapsible. Modal workflows use the same viewport-relative shell, keep their
actions visible while content scrolls, and collapse to a nearly full-screen
layout on a phone-sized viewport. The sidebar collapse state persists after a
page refresh.

Use the task toolbar to filter by title, status, priority and assignee. Edit a
task and verify the title, assignee, priority, description and due date are
updated. Deletion and project archiving must require confirmation. On
`/my-tasks`, verify that filters, pagination and status updates work without
opening the project first.

## Dashboard

`GET /api/v1/dashboard/summary` returns open, completed and overdue task counts.
Admins see system-wide totals; employees only see tasks assigned to themselves.

## Automated checks

Run `backend\.\mvnw.cmd test`. This executes frontend dependency verification,
the production frontend build, JUnit tests, and Vitest/MSW tests. Docker-backed
Testcontainers tests are skipped when Docker is unavailable.
The web authorization integration suite still runs without Docker and covers
anonymous access, CSRF, owner/member/admin boundaries, invalid assignment and
archived-project mutation rules. See `docs/api/authorization-matrix.md`.
