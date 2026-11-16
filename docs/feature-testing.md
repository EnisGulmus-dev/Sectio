# Feature verification

The current employee observation flow is **İş takibi** for every user state;
deletion is not required to see a completed task. Switch lists using compact
**Kullanıcılar / Silinenler** buttons. The styled work cards and server filters
are verified by the user sequence in `validation/employee-work-tracking.md`.

This file describes current code. ADMIN business writes still differ from the
accepted target in `requirements.md`. User/member removal now preserves tasks;
the B01 regression tests are prepared but have not been run. Use disposable data
for lifecycle checks. See `validation/work-preservation.md` for the run order.

Start the integrated application with `backend\.\mvnw.cmd spring-boot:run`, sign in,
and use the URLs below.

## Users

- UI: `/users`
- API: `DELETE /api/v1/admin/users/{userId}`

Choose **Pasifleştir**: the user stays visible as inactive; memberships and
assignments remain. Choose **Sil**: the user disappears from the normal list,
memberships/assignments are removed and work history remains. The deleted-user
view is read-only; edit/reset/reactivate are unavailable and rejected by API.
Email remains reserved. Self-deletion and last-active-admin deletion are rejected.
Authored tasks, project ownership and unassigned tasks remain.

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

Create users with `SOFTWARE_DEVELOPER`, `TEAM_LEADER`, `PROJECT_MANAGER` and
`PROJECT_OWNER` job titles. An admin or one of the latter three titles can
create a project; a regular employee cannot. These
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

Assign TODO/IN_PROGRESS and DONE tasks to a member and remove the member.
Only that project's memberships/assignments are removed; every task remains.
An unassigned task must stay listed with the correct total and be reassignable.
Assignments and unassigned tasks in another project must remain unaffected.
Use **Görevler → Atanmamış görevler** or the assignment dropdown to find it.
After reassignment it leaves that filter and remains under Tüm görevler.

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
be created without an assignee in the current create flow. Removing the final
assignee through account/member removal keeps the task, displayed as Atanmamış.

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
Testcontainers tests require Docker and fail when it is unavailable.
The web authorization integration suite still runs without Docker and covers
anonymous access, CSRF, owner/member/admin boundaries, invalid assignment and
archived-project mutation rules. See `docs/api/authorization-matrix.md`.
