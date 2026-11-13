# Sectio architecture

## Runtime topology

Sectio is built as two source projects and runs as one local application:

```text
frontend (React + TypeScript)
        | npm run build
        v
backend/target/classes/static
        | served by
        v
Spring Boot + embedded Tomcat :8080
        |
        +-- /                 React application
        +-- /api/v1/**        JSON API
        +-- /actuator/health  Health endpoint
```

Maven runs an idempotent `npm install`, builds the frontend and copies `dist`
into the backend classpath before compiling Java. Direct client routes without a
file extension are forwarded to `index.html`; `/api` and `/actuator` are never
forwarded to React.

## Backend packages

```text
com.enis.sectio
├── auth                 Session authentication and password change
├── common
│   ├── exception        Stable API error contract
│   └── web              Reusable HTTP models such as PageResponse
├── config               Shared Spring beans
├── security             Authorization, CSRF and security filters
├── user
│   ├── admin            Admin user-management use cases
│   └── bootstrap        First-admin bootstrap
└── web                  React SPA forwarding
```

Controllers own HTTP validation and status codes. Services own business rules
and transaction boundaries. Repositories only provide persistence access.
Entities keep state transitions such as activation and password reset close to
the data they protect.

## Frontend packages

```text
src
├── app                   Routing, application shell and global pages
├── features
│   ├── auth              Authentication state and pages
│   └── users             User-management API, types and page
├── shared
│   ├── api               CSRF-aware HTTP client and API errors
│   └── ui                Toasts, dialogs, feedback and error boundary
├── test                  MSW server, handlers and global test setup
└── styles                Temporary neutral design system
```

Feature folders own domain-specific code. `shared` contains only behavior that
can be reused by more than one feature. Components call feature API modules;
only the shared HTTP client deals with cookies, CSRF headers and error parsing.

## UI infrastructure

- `ToastProvider`: short-lived success, information and error notifications.
- `ConfirmDialog`: accessible confirmation before recoverable state changes.
- `Feedback`: shared loading, empty and inline-error states.
- `ErrorBoundary`: final protection against unexpected render failures.
- `NotFoundPage`: authenticated in-app 404 page.

## Testing

Backend tests use JUnit, Mockito and Testcontainers. Testcontainers tests are
skipped when Docker is unavailable.

Frontend tests use:

- Vitest as the runner
- React Testing Library for user-visible behavior
- `user-event` for realistic interactions
- MSW for network-level API mocks
- jsdom as the browser-like test environment

Commands:

```powershell
cd frontend
npm run test       # one run
npm run test:watch # watch mode
npm run check      # tests and production build

cd ..\backend
.\mvnw.cmd test    # frontend tests/build plus backend tests
```

## Conventions for upcoming features

- List endpoints return `PageResponse<T>` once pagination is needed.
- Mutating API calls use the shared CSRF-aware HTTP client.
- Business conflicts return the stable `ApiErrorResponse` shape.
- New React routes need no individual Tomcat mapping; the generic SPA fallback
  handles them automatically.
- Feature-specific types and API calls stay in their feature folder.
- Shared UI behavior moves to `shared/ui` only after it is reusable.
