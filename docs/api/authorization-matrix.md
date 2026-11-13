# Authorization matrix

All `/api/**` endpoints require an authenticated session except CSRF token retrieval and login.
State-changing requests also require the CSRF header returned by `GET /api/v1/auth/csrf`.

| Operation | Admin | Project owner | Project member | Non-member |
| --- | --- | --- | --- | --- |
| List/read project | All projects | Yes | Yes | No |
| Create project | Yes | If job title is TEAM_LEADER or MANAGER | If job title is TEAM_LEADER or MANAGER | If job title is TEAM_LEADER or MANAGER |
| Update/archive project | Yes | Yes | No | No |
| Restore/delete active or archived project | Yes | Yes | No | No |
| List members/tasks | Yes | Yes | Yes | No |
| Add/remove members | Yes | Yes | No | No |
| Remove project owner | No | No | No | No |
| Remove member assigned to TODO/IN_PROGRESS task | No | No | No | No |
| Create/update/status/delete task | Yes | Yes | Yes | No |
| Assign task | Active project members only | Active project members only | Active project members only | No |

Archived projects remain readable to authorized users but reject project, member and task mutations.
Permanent deletion does not require archival and removes project tasks and memberships as well.
`ADMIN`/`EMPLOYEE` are security roles. `EMPLOYEE`/`TEAM_LEADER`/`MANAGER` are separate company job titles.
Employees see only their assigned counters on the dashboard; admins see system-wide counters.
