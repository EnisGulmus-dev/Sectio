package com.enis.sectio.task;

import com.enis.sectio.SectioApplication;
import com.enis.sectio.project.*;
import com.enis.sectio.user.*;
import com.enis.sectio.user.admin.AdminUserService;
import com.enis.sectio.user.admin.UserWorkTrackingScope;
import com.enis.sectio.user.admin.UserWorkTrackingEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class WorkPreservationPostgreSqlTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");
    private static final String ADMIN_EMAIL = "preservation-admin@example.com";
    private static final String PASSWORD = "test-preservation-password";
    private ConfigurableApplicationContext context;
    private TransactionTemplate transactions;

    @BeforeEach
    void start() {
        context = new SpringApplicationBuilder(SectioApplication.class)
                .web(WebApplicationType.SERVLET)
                .run("--server.port=0", "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--app.bootstrap.admin.name=Preservation Admin",
                        "--app.bootstrap.admin.email=" + ADMIN_EMAIL,
                        "--app.bootstrap.admin.password=" + PASSWORD);
        transactions = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    @AfterEach
    void stop() { if (context != null) context.close(); }

    @Test
    void memberRemovalPreservesOpenSharedAuthoredAndUnrelatedUnassignedTasks() {
        Fixture f = fixture();
        context.getBean(ProjectService.class).removeMember(f.project(), f.departing(), ADMIN_EMAIL);

        assertWork(f);
        transactions.executeWithoutResult(status -> {
            assertThat(context.getBean(ProjectMemberRepository.class)
                    .existsByProjectIdAndUserId(f.project(), f.departing())).isFalse();
            assertThat(context.getBean(ProjectTeamRepository.class).findById(f.team()).orElseThrow()
                    .getMembers()).extracting(User::getId).containsExactly(f.colleague());
            assertThat(context.getBean(UserRepository.class).findById(f.departing()).orElseThrow().isActive()).isTrue();
        });
        assertThat(context.getBean(ProjectService.class).listMembers(f.otherProject(), ADMIN_EMAIL))
                .extracting(ProjectMemberResponse::userId).contains(f.departing());
        var history = context.getBean(AdminUserService.class).workHistory(f.departing(), 0, 20);
        assertThat(history.totalElements()).isEqualTo(3);
        assertThat(history.content()).extracting(com.enis.sectio.user.admin.UserWorkHistoryResponse::reason)
                .containsOnly("PROJECT_MEMBER_REMOVED");
        var tracking = context.getBean(AdminUserService.class).workTracking(f.departing(), UserWorkTrackingScope.ALL, null, "", 0, 20);
        assertThat(tracking.totalElements()).isEqualTo(3);
        assertThat(tracking.content()).extracting(UserWorkTrackingEntry::assignmentState).containsOnly("ENDED");
        // The preserved open task can be reassigned by the project manager.
        context.getBean(TaskService.class).update(f.singleTask(), ADMIN_EMAIL, List.of(f.colleague()),
                List.of(), false, "Reassigned work", null, TaskPriority.MEDIUM, null);
        assertThat(context.getBean(TaskService.class).get(f.singleTask(), ADMIN_EMAIL).assignees())
                .extracting(TaskResponse.AssigneeResponse::userId).containsExactly(f.colleague());
    }

    @Test
    void activeAndInactiveEmployeesHaveLiveCompletedWorkWithoutAnyDeletion() throws Exception {
        Fixture f = fixture();
        var admin = context.getBean(AdminUserService.class);
        assertThat(admin.workHistory(f.departing(), 0, 20).totalElements()).isZero();
        var all = admin.workTracking(f.departing(), UserWorkTrackingScope.ALL, null, "", 0, 1);
        assertThat(all.totalElements()).isEqualTo(3);
        assertThat(all.content()).hasSize(1);
        var completed = admin.workTracking(f.departing(), UserWorkTrackingScope.CURRENT, TaskStatus.DONE, "", 0, 20);
        assertThat(completed.totalElements()).isEqualTo(1);
        assertThat(completed.content().getFirst().taskId()).isEqualTo(f.authoredTask());
        assertThat(completed.content().getFirst().completedAt()).isNotNull();
        assertThat(completed.content().getFirst().assignmentState()).isEqualTo("CURRENT");
        assertThat(admin.workTracking(f.departing(), UserWorkTrackingScope.ALL, null, "Shared", 0, 20).totalElements()).isEqualTo(1);
        assertThat(admin.workTracking(f.departing(), UserWorkTrackingScope.ENDED, null, "", 0, 20).totalElements()).isZero();
        admin.changeStatus(f.departing(), false, ADMIN_EMAIL);
        assertThat(admin.workTracking(f.departing(), UserWorkTrackingScope.CURRENT, TaskStatus.DONE, "", 0, 20).totalElements()).isEqualTo(1);

        HttpClient observer = client();
        assertThat(login(observer, ADMIN_EMAIL).statusCode()).isEqualTo(200);
        var response = get(observer, "/api/v1/admin/users/" + f.departing() + "/work-tracking?status=DONE&scope=CURRENT");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Authored", "CURRENT", "DONE");
    }

    @Test
    void accountClosureKeepsCreatorAndOwnershipWhileDetachingMemberships() throws Exception {
        Fixture f = fixture();
        context.getBean(AdminUserService.class).deleteUser(f.departing(), ADMIN_EMAIL);

        assertWork(f);
        transactions.executeWithoutResult(status -> {
            User user = context.getBean(UserRepository.class).findById(f.departing()).orElseThrow();
            assertThat(user.isActive()).isFalse();
            assertThat(user.getSessionVersion()).isEqualTo(1);
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(context.getBean(ProjectMemberRepository.class).countByUserId(f.departing())).isZero();
            assertThat(context.getBean(TaskRepository.class).countDistinctByAssigneesId(f.departing())).isZero();
            assertThat(context.getBean(ProjectRepository.class).findById(f.ownedProject()).orElseThrow()
                    .getOwner().getId()).isEqualTo(f.departing());
            assertThat(context.getBean(ProjectTeamRepository.class).findById(f.team()).orElseThrow()
                    .getMembers()).extracting(User::getId).containsExactly(f.colleague());
        });
        var admin = context.getBean(AdminUserService.class);
        assertThat(admin.listUsers(false)).extracting(com.enis.sectio.user.admin.AdminUserResponse::id)
                .doesNotContain(f.departing());
        assertThat(admin.listUsers(true)).extracting(com.enis.sectio.user.admin.AdminUserResponse::id)
                .contains(f.departing());
        assertThatThrownBy(() -> admin.changeStatus(f.departing(), true, ADMIN_EMAIL))
                .isInstanceOf(com.enis.sectio.user.admin.UserManagementException.class)
                .hasMessageContaining("Deleted accounts");

        var history = admin.workHistory(f.departing(), 0, 1);
        assertThat(history.totalElements()).isEqualTo(3);
        var entries = admin.workHistory(f.departing(), 0, 20).content();
        assertThat(entries).extracting(com.enis.sectio.user.admin.UserWorkHistoryResponse::taskTitle)
                .containsExactlyInAnyOrder("Single", "Shared", "Authored");
        assertThat(entries).extracting(com.enis.sectio.user.admin.UserWorkHistoryResponse::statusAtEnd)
                .containsExactlyInAnyOrder("TODO", "IN_PROGRESS", "DONE");
        assertThat(entries).extracting(com.enis.sectio.user.admin.UserWorkHistoryResponse::reason)
                .containsOnly("USER_DELETED");
        var tracking = admin.workTracking(f.departing(), UserWorkTrackingScope.ALL, null, "", 0, 20);
        assertThat(tracking.totalElements()).isEqualTo(3);
        assertThat(tracking.content()).extracting(UserWorkTrackingEntry::assignmentState).containsOnly("ENDED");
        assertThat(admin.workTracking(f.departing(), UserWorkTrackingScope.CURRENT, null, "", 0, 20).totalElements()).isZero();
        HttpClient observer = client();
        assertThat(login(observer, ADMIN_EMAIL).statusCode()).isEqualTo(200);
        var response = get(observer, "/api/v1/admin/users/" + f.departing() + "/work-history");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Single", "Shared", "USER_DELETED");

        // A deliberate task deletion must not erase the former employee's assignment snapshot.
        context.getBean(TaskService.class).delete(f.singleTask(), ADMIN_EMAIL);
        var retained = admin.workHistory(f.departing(), 0, 20).content().stream()
                .filter(entry -> entry.taskTitle().equals("Single")).findFirst().orElseThrow();
        assertThat(retained.taskId()).isNull();
        assertThat(retained.projectName()).isNotBlank();
        assertThat(admin.workTracking(f.departing(), UserWorkTrackingScope.ENDED, null, "Single", 0, 20).content().getFirst().taskId()).isNull();
    }

    @Test
    void temporaryDeactivationAndReactivationDoNotReviveAnyPreviouslyIssuedSession() throws Exception {
        Fixture f = fixture();
        HttpClient first = client();
        HttpClient second = client();
        assertThat(login(first, f.email()).statusCode()).isEqualTo(200);
        assertThat(login(second, f.email()).statusCode()).isEqualTo(200);
        assertThat(get(first, "/api/v1/auth/me").statusCode()).isEqualTo(200);

        var admin = context.getBean(AdminUserService.class);
        admin.changeStatus(f.departing(), false, ADMIN_EMAIL);
        assertThat(get(first, "/api/v1/auth/me").statusCode()).isEqualTo(401);
        admin.changeStatus(f.departing(), true, ADMIN_EMAIL);
        // This session made no request while the account was inactive.
        HttpResponse<String> revoked = get(second, "/api/v1/auth/me");
        assertThat(revoked.statusCode()).isEqualTo(401);
        assertThat(revoked.body()).contains("SESSION_REVOKED");
        assertThat(get(second, "/api/v1/projects").statusCode()).isEqualTo(401);
        HttpClient fresh = client();
        assertThat(login(fresh, f.email()).statusCode()).isEqualTo(200);
        var denied = get(fresh, "/api/v1/admin/users/" + f.departing() + "/work-history");
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(denied.body()).contains("ACCESS_DENIED");
        var trackingDenied = get(fresh, "/api/v1/admin/users/" + f.departing() + "/work-tracking");
        assertThat(trackingDenied.statusCode()).isEqualTo(403);
        assertThat(trackingDenied.body()).contains("ACCESS_DENIED");
        assertThat(admin.listUsers(false)).extracting(com.enis.sectio.user.admin.AdminUserResponse::id)
                .contains(f.departing());
        transactions.executeWithoutResult(status -> {
            assertThat(context.getBean(UserRepository.class).findById(f.departing()).orElseThrow().getDeletedAt()).isNull();
            assertThat(context.getBean(ProjectMemberRepository.class).countByUserId(f.departing())).isEqualTo(3);
            assertThat(context.getBean(TaskRepository.class).countDistinctByAssigneesId(f.departing())).isEqualTo(3);
        });
    }

    @Test
    void historyWriteFailureRollsBackAccountDeletionAndAssignmentRemoval() throws Exception {
        Fixture f = fixture();
        executeSql("ALTER TABLE task_assignment_history ADD CONSTRAINT test_reject_history CHECK (false) NOT VALID");
        try {
            assertThatThrownBy(() -> context.getBean(AdminUserService.class).deleteUser(f.departing(), ADMIN_EMAIL))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        } finally {
            executeSql("ALTER TABLE task_assignment_history DROP CONSTRAINT test_reject_history");
        }
        transactions.executeWithoutResult(status -> {
            User user = context.getBean(UserRepository.class).findById(f.departing()).orElseThrow();
            assertThat(user.isActive()).isTrue();
            assertThat(user.getDeletedAt()).isNull();
            assertThat(context.getBean(TaskRepository.class).countDistinctByAssigneesId(f.departing())).isEqualTo(3);
            assertThat(context.getBean(ProjectMemberRepository.class).countByUserId(f.departing())).isEqualTo(3);
        });
        assertThat(context.getBean(AdminUserService.class).workHistory(f.departing(), 0, 20).totalElements()).isZero();
    }

    private void executeSql(String sql) throws Exception {
        try (var connection = context.getBean(javax.sql.DataSource.class).getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void assertWork(Fixture f) {
        var tasks = context.getBean(TaskService.class);
        var single = tasks.get(f.singleTask(), ADMIN_EMAIL);
        assertThat(single.assignees()).isEmpty();
        assertThat(single.status()).isEqualTo(TaskStatus.TODO);
        assertThat(single.createdById()).isEqualTo(f.departing());
        assertThat(tasks.get(f.sharedTask(), ADMIN_EMAIL).assignees())
                .extracting(TaskResponse.AssigneeResponse::userId).containsExactly(f.colleague());
        assertThat(tasks.get(f.authoredTask(), ADMIN_EMAIL).createdById()).isEqualTo(f.departing());
        assertThat(tasks.get(f.authoredTask(), ADMIN_EMAIL).status()).isEqualTo(TaskStatus.DONE);
        assertThat(tasks.get(f.authoredTask(), ADMIN_EMAIL).completedAt()).isNotNull();
        assertThat(tasks.get(f.unrelatedTask(), ADMIN_EMAIL).assignees()).isEmpty();
        var page = tasks.listProject(f.project(), ADMIN_EMAIL, "", null, null, null, false, 0, 1);
        assertThat(page.totalElements()).isEqualTo(3); // Exercises the count query as well.
        var all = tasks.listProject(f.project(), ADMIN_EMAIL, "", null, null, null, false, 0, 100);
        assertThat(all.content()).extracting(TaskResponse::id)
                .containsExactlyInAnyOrder(f.singleTask(), f.sharedTask(), f.authoredTask());
        var assigned = tasks.listProject(f.project(), ADMIN_EMAIL, "", null, null, f.colleague(), false, 0, 100);
        assertThat(assigned.content()).extracting(TaskResponse::id)
                .containsExactlyInAnyOrder(f.sharedTask(), f.authoredTask());
        var unassigned = tasks.listProject(f.project(), ADMIN_EMAIL, "", null, null, null, true, 0, 1);
        assertThat(unassigned.totalElements()).isEqualTo(1);
        assertThat(unassigned.content()).extracting(TaskResponse::id).containsExactly(f.singleTask());
        transactions.executeWithoutResult(status -> {
            var repository = context.getBean(TaskRepository.class);
            assertThat(repository.countByCreatedById(f.departing())).isEqualTo(3);
            assertThat(repository.countByStatusNotIn(List.of(TaskStatus.DONE, TaskStatus.CANCELLED)))
                    .isEqualTo(f.openBefore());
            assertThat(repository.countByStatus(TaskStatus.DONE)).isEqualTo(f.completedBefore());
        });
    }

    @Test
    void savedTeamCanTargetMultipleTasksAndDeletionPreservesTheirPeopleAndWork() {
        Fixture f = fixture();
        var service = context.getBean(TaskService.class);
        var first = service.create(f.project(), ADMIN_EMAIL, List.of(), List.of(f.team()), false,
                "First team task", null, TaskPriority.HIGH, null);
        service.status(first.id(), ADMIN_EMAIL, TaskStatus.DONE);
        var second = service.create(f.project(), ADMIN_EMAIL, List.of(), List.of(f.team()), false,
                "Second team task", null, TaskPriority.MEDIUM, null);
        assertThat(service.get(first.id(), ADMIN_EMAIL).teams()).extracting(TaskResponse.TeamResponse::id).containsExactly(f.team());
        assertThat(second.teams()).extracting(TaskResponse.TeamResponse::id).containsExactly(f.team());
        assertThat(second.assignees()).extracting(TaskResponse.AssigneeResponse::userId)
                .containsExactlyInAnyOrder(f.departing(), f.colleague());
        assertThatThrownBy(() -> service.create(f.otherProject(), ADMIN_EMAIL, List.of(), List.of(f.team()), false,
                "Wrong project", null, TaskPriority.LOW, null)).isInstanceOf(ProjectException.class);
        assertThat(service.get(f.sharedTask(), ADMIN_EMAIL).teams()).isEmpty(); // No guessed historical team.

        service.update(second.id(), ADMIN_EMAIL, List.of(), List.of(f.team()), false,
                "Updated team task", null, TaskPriority.HIGH, null);
        assertThat(service.get(second.id(), ADMIN_EMAIL).teams()).extracting(TaskResponse.TeamResponse::id).containsExactly(f.team());
        context.getBean(ProjectTeamService.class).delete(f.project(), f.team(), ADMIN_EMAIL);
        for (Long id : List.of(first.id(), second.id())) {
            var preserved = service.get(id, ADMIN_EMAIL);
            assertThat(preserved.teams()).isEmpty();
            assertThat(preserved.assignees()).extracting(TaskResponse.AssigneeResponse::userId)
                    .containsExactlyInAnyOrder(f.departing(), f.colleague());
        }
        assertThat(service.get(first.id(), ADMIN_EMAIL).status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void projectAndPersonalBucketsHaveConsistentCountsAndHttpDefaultsToActive() throws Exception {
        Fixture f = fixture();
        var service = context.getBean(TaskService.class);
        service.status(f.singleTask(), ADMIN_EMAIL, TaskStatus.CANCELLED);
        var expected = java.util.Map.of(TaskBucket.ACTIVE, f.sharedTask(), TaskBucket.COMPLETED, f.authoredTask(),
                TaskBucket.CANCELLED, f.singleTask());
        for (var entry : expected.entrySet()) {
            var project = service.listProject(f.project(), ADMIN_EMAIL, "", null, null, null, false, entry.getKey(), 0, 1);
            var personal = service.mine(f.email(), "", null, null, entry.getKey(), 0, 1);
            assertThat(project.totalElements()).isEqualTo(1);
            assertThat(project.content()).extracting(TaskResponse::id).containsExactly(entry.getValue());
            assertThat(personal.totalElements()).isEqualTo(1);
            assertThat(personal.content()).extracting(TaskResponse::id).containsExactly(entry.getValue());
        }
        assertThat(service.listProject(f.project(), ADMIN_EMAIL, "", null, null, null, false, TaskBucket.ALL, 0, 1).totalElements()).isEqualTo(3);
        assertThat(service.mine(f.email(), "", null, null, TaskBucket.ALL, 0, 1).totalElements()).isEqualTo(3);
        HttpClient employee = client();
        assertThat(login(employee, f.email()).statusCode()).isEqualTo(200);
        for (String path : List.of("/api/v1/tasks/my", "/api/v1/projects/" + f.project() + "/tasks")) {
            var active = get(employee, path);
            assertThat(active.statusCode()).isEqualTo(200);
            assertThat(active.body()).contains("Shared").doesNotContain("Authored", "Single");
            assertThat(get(employee, path + "?bucket=COMPLETED").body()).contains("Authored").doesNotContain("Shared", "Single");
            assertThat(get(employee, path + "?status=DONE").body()).contains("Authored");
        }
        service.status(f.sharedTask(), ADMIN_EMAIL, TaskStatus.DONE);
        assertThat(service.mine(f.email(), "", null, null, TaskBucket.ACTIVE, 0, 20).totalElements()).isZero();
        assertThat(service.mine(f.email(), "", null, null, TaskBucket.COMPLETED, 0, 20).totalElements()).isEqualTo(2);
    }

    @Test
    void initialProjectMembershipsCommitTogetherAndInvalidSelectionLeavesNoProject() {
        Fixture f = fixture();
        var service = context.getBean(ProjectService.class);
        var created = service.create(ADMIN_EMAIL, "Initial people " + UUID.randomUUID(), null,
                List.of(f.departing(), f.colleague(), f.departing()));
        Long owner = context.getBean(UserRepository.class).findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        assertThat(service.listMembers(created.id(), ADMIN_EMAIL)).extracting(ProjectMemberResponse::userId)
                .containsExactlyInAnyOrder(owner, f.departing(), f.colleague());
        long before = context.getBean(ProjectRepository.class).count();
        context.getBean(AdminUserService.class).changeStatus(f.colleague(), false, ADMIN_EMAIL);
        assertThatThrownBy(() -> service.create(ADMIN_EMAIL, "Must roll back", null, List.of(f.departing(), f.colleague())))
                .isInstanceOf(ProjectException.class);
        assertThat(context.getBean(ProjectRepository.class).count()).isEqualTo(before);
    }

    @Test
    void unicodeEmailIsRejectedDuringAccountCreation() throws Exception {
        fixture(); // Ensure the bootstrap admin has completed password change.
        HttpClient admin = client();
        assertThat(login(admin, ADMIN_EMAIL).statusCode()).isEqualTo(200);
        String email = "hüseyin-" + UUID.randomUUID() + "@example.com";
        var response = mutate(admin, "/api/v1/admin/users", "POST", """
                {"name":"Hüseyin Ahmer","email":" %s ","temporaryPassword":"test-preservation-password","role":"EMPLOYEE","jobTitle":"SOFTWARE_DEVELOPER"}
                """.formatted(email));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(context.getBean(UserRepository.class).findByEmail(email)).isEmpty();
    }

    @Test
    void restoreThenPurgeKeepsHistoryReleasesEmailAndNeverRevivesOldSessions() throws Exception {
        Fixture f = fixture();
        HttpClient oldSession = client();
        assertThat(login(oldSession, f.email()).statusCode()).isEqualTo(200);
        HttpClient adminSession = client();
        assertThat(login(adminSession, ADMIN_EMAIL).statusCode()).isEqualTo(200);
        var admin = context.getBean(AdminUserService.class);
        admin.deleteUser(f.departing(), ADMIN_EMAIL);
        var restored = mutate(adminSession, "/api/v1/admin/users/" + f.departing() + "/restore", "POST",
                "{\"temporaryPassword\":\"restored-password-123\"}");
        assertThat(restored.statusCode()).isEqualTo(200);
        assertThat(restored.body()).contains("\"active\":true", "\"mustChangePassword\":true");
        assertThat(get(oldSession, "/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(login(client(), f.email()).statusCode()).isEqualTo(401); // Original password cannot log in.
        assertThat(login(client(), f.email(), "restored-password-123").statusCode()).isEqualTo(200);
        assertThat(context.getBean(ProjectMemberRepository.class).countByUserId(f.departing())).isZero();
        assertThat(context.getBean(TaskRepository.class).countDistinctByAssigneesId(f.departing())).isZero();
        assertThat(admin.workHistory(f.departing(), 0, 20).totalElements()).isEqualTo(3);

        admin.deleteUser(f.departing(), ADMIN_EMAIL);
        var purged = mutate(adminSession, "/api/v1/admin/users/" + f.departing() + "/purge", "DELETE", "");
        assertThat(purged.statusCode()).isEqualTo(204);
        assertThat(admin.listUsers(true)).extracting(com.enis.sectio.user.admin.AdminUserResponse::id).doesNotContain(f.departing());
        assertThat(admin.listUsers(false)).extracting(com.enis.sectio.user.admin.AdminUserResponse::id).doesNotContain(f.departing());
        assertThat(context.getBean(UserRepository.class).findByEmail(f.email())).isEmpty();
        assertThat(admin.workHistory(f.departing(), 0, 20).totalElements()).isEqualTo(3);
        assertThat(context.getBean(TaskService.class).get(f.authoredTask(), ADMIN_EMAIL).createdByName()).startsWith("Silinmiş kullanıcı #");
        assertThat(context.getBean(ProjectService.class).get(f.ownedProject(), ADMIN_EMAIL).ownerId()).isEqualTo(f.departing());
        assertThat(mutate(adminSession, "/api/v1/admin/users/" + f.departing() + "/restore", "POST",
                "{\"temporaryPassword\":\"restored-password-123\"}").statusCode()).isEqualTo(409);
        var replacement = admin.createUser("Replacement", f.email(), PASSWORD, Role.EMPLOYEE, JobTitle.SOFTWARE_DEVELOPER);
        assertThat(replacement.id()).isNotEqualTo(f.departing());
        assertThat(context.getBean(TaskService.class).mine(f.email(), "", null, null, 0, 20).totalElements()).isZero();
    }

    @Test
    void restoreAndPurgeRequireAdminAndCsrfAndCannotPurgeLiveAccounts() throws Exception {
        Fixture f = fixture();
        HttpClient employee = client();
        assertThat(login(employee, f.email()).statusCode()).isEqualTo(200);
        assertThat(mutate(employee, "/api/v1/admin/users/" + f.colleague() + "/restore", "POST",
                "{\"temporaryPassword\":\"restored-password-123\"}").statusCode()).isEqualTo(403);
        assertThat(mutate(employee, "/api/v1/admin/users/" + f.colleague() + "/purge", "DELETE", "").statusCode()).isEqualTo(403);
        HttpClient admin = client();
        assertThat(login(admin, ADMIN_EMAIL).statusCode()).isEqualTo(200);
        assertThat(mutate(admin, "/api/v1/admin/users/" + f.colleague() + "/purge", "DELETE", "").statusCode()).isEqualTo(409);
        var denied = admin.send(HttpRequest.newBuilder(uri("/api/v1/admin/users/" + f.colleague() + "/restore"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                        "{\"temporaryPassword\":\"restored-password-123\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(denied.statusCode()).isEqualTo(403);
    }

    private HttpResponse<String> mutate(HttpClient client, String path, String method, String body) throws Exception {
        var csrf = get(client, "/api/v1/auth/csrf");
        var matcher = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"").matcher(csrf.body());
        assertThat(matcher.find()).isTrue();
        return client.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", matcher.group(1)).method(method, HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void progressNotesAndCompletedTaskHandoffsPreserveEarlierWork() {
        Fixture f = fixture();
        var service = context.getBean(TaskService.class);
        service.status(f.singleTask(), ADMIN_EMAIL, TaskStatus.IN_PROGRESS, "Geliştirme başladı", TaskStatus.TODO);
        service.status(f.singleTask(), ADMIN_EMAIL, TaskStatus.DONE, "Teste hazır", TaskStatus.IN_PROGRESS);
        assertThat(service.progress(f.singleTask(), ADMIN_EMAIL)).extracting(TaskService.ProgressResponse::note)
                .containsExactly("Teste hazır", "Geliştirme başladı");
        assertThatThrownBy(() -> service.status(f.singleTask(), ADMIN_EMAIL, TaskStatus.CANCELLED, null, TaskStatus.TODO))
                .isInstanceOf(ProjectException.class);
        var next = service.handoff(f.singleTask(), ADMIN_EMAIL, List.of(f.colleague()), List.of(), false,
                "Test aşaması", "Geliştirilen işi test et", TaskPriority.HIGH, null);
        assertThat(next.predecessorId()).isEqualTo(f.singleTask());
        assertThat(next.status()).isEqualTo(TaskStatus.TODO);
        var history = service.handoffHistory(next.id(), ADMIN_EMAIL);
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().sourceTitle()).isEqualTo("Single");
        assertThat(history.getFirst().targetTitle()).isEqualTo("Test aşaması");
        assertThat(history.getFirst().recipientPeople()).contains("Colleague");
        assertThat(service.handoffHistory(f.singleTask(), ADMIN_EMAIL)).hasSize(1);
        service.update(next.id(), ADMIN_EMAIL, List.of(f.departing()), List.of(), false,
                "Yeniden adlandırıldı", null, TaskPriority.HIGH, null);
        assertThat(service.handoffHistory(next.id(), ADMIN_EMAIL).getFirst().targetTitle()).isEqualTo("Test aşaması");
        assertThat(service.handoffHistory(next.id(), ADMIN_EMAIL).getFirst().recipientPeople()).contains("Colleague");
        service.delete(f.singleTask(), ADMIN_EMAIL);
        assertThat(service.handoffHistory(next.id(), ADMIN_EMAIL).getFirst().sourceTaskId()).isNull();
        assertThatThrownBy(() -> service.handoff(f.sharedTask(), ADMIN_EMAIL, List.of(f.colleague()), List.of(), false,
                "Erken devir", null, TaskPriority.HIGH, null)).isInstanceOf(ProjectException.class);
    }

    @Test
    void ownProfileAndPresetAvatarArePersistedAndPurgedWithAccount() throws Exception {
        Fixture f = fixture();
        HttpClient employee = client();
        assertThat(login(employee, f.email()).statusCode()).isEqualTo(200);
        var updated = mutate(employee, "/api/v1/profile", "PATCH",
                "{\"displayName\":\"Deneme\",\"biography\":\"Ekip üyesi\",\"timeZone\":\"Europe/Istanbul\"}");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(get(employee, "/api/v1/profile").body()).contains("Deneme", "Europe/Istanbul");
        assertThat(mutate(employee, "/api/v1/profile/avatar", "PUT", "{\"avatarId\":\"erkek_01_sakalsiz\"}").statusCode()).isEqualTo(200);
        assertThat(get(employee, "/api/v1/profile").body()).contains("erkek_01_sakalsiz");
        assertThat(mutate(employee, "/api/v1/profile/avatar", "PUT", "{\"avatarId\":\"../../secret\"}").statusCode()).isEqualTo(400);
        assertThat(mutate(employee, "/api/v1/profile", "PATCH", "{\"timeZone\":\"Invalid/Zone\"}").statusCode()).isEqualTo(400);
        var admin = context.getBean(AdminUserService.class);
        admin.deleteUser(f.departing(), ADMIN_EMAIL);
        admin.purgeUser(f.departing(), ADMIN_EMAIL);
        assertThat(context.getBean(ProfileRepository.class).findById(f.departing())).isEmpty();
        assertThat(get(employee, "/api/v1/profile").statusCode()).isEqualTo(401);
    }

    @Test
    void channelVisibilityAndMembershipRespectPrivateAccessEvenForAdmins() {
        Fixture f = fixture();
        var channels = context.getBean(com.enis.sectio.channel.ChannelService.class);
        var userRepository = context.getBean(UserRepository.class);
        Long admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        Long secondAdmin = transactions.execute(status -> userRepository.save(new User("Other admin", UUID.randomUUID() + "@example.com", "test-hash", Role.ADMIN)).getId());
        var privateChannel = channels.create(admin, "Private " + UUID.randomUUID(), "", "PRIVATE");
        var publicChannel = channels.create(admin, "Public " + UUID.randomUUID(), "", "PUBLIC");
        assertThat(channels.list(secondAdmin)).extracting(com.enis.sectio.channel.ChannelService.ChannelView::id)
                .contains(publicChannel.id()).doesNotContain(privateChannel.id());
        assertThatThrownBy(() -> channels.get(privateChannel.id(), secondAdmin)).isInstanceOf(ProjectException.class);
        assertThatThrownBy(() -> channels.members(privateChannel.id(), f.departing())).isInstanceOf(ProjectException.class);
        assertThatThrownBy(() -> channels.create(f.departing(), "Forbidden", null, "PUBLIC")).isInstanceOf(ProjectException.class);
        channels.addMember(privateChannel.id(), admin, f.departing());
        assertThat(channels.candidates(privateChannel.id(), admin)).extracting(com.enis.sectio.channel.ChannelService.CandidateView::userId).contains(f.colleague()).doesNotContain(f.departing());
        assertThatThrownBy(() -> channels.candidates(privateChannel.id(), f.departing())).isInstanceOf(ProjectException.class);
        assertThat(channels.members(privateChannel.id(), f.departing())).extracting(com.enis.sectio.channel.ChannelService.MemberView::userId).contains(admin, f.departing());
        assertThatThrownBy(() -> context.getBean(AdminUserService.class).deleteUser(admin, f.email())).isInstanceOf(com.enis.sectio.user.admin.UserManagementException.class);
        channels.transferOwnership(privateChannel.id(), admin, f.departing());
        assertThat(channels.get(privateChannel.id(), f.departing()).ownerId()).isEqualTo(f.departing());
        assertThatThrownBy(() -> context.getBean(AdminUserService.class).deleteUser(f.departing(), ADMIN_EMAIL)).isInstanceOf(com.enis.sectio.user.admin.UserManagementException.class);
        channels.transferOwnership(privateChannel.id(), f.departing(), admin);
        assertThatThrownBy(() -> channels.addMember(privateChannel.id(), f.departing(), f.colleague())).isInstanceOf(ProjectException.class);
        channels.removeMember(privateChannel.id(), admin, f.departing());
        assertThatThrownBy(() -> channels.members(privateChannel.id(), f.departing())).isInstanceOf(ProjectException.class);
        channels.join(publicChannel.id(), f.departing());
        assertThat(channels.get(publicChannel.id(), f.departing()).member()).isTrue();
        channels.removeMember(publicChannel.id(), f.departing(), f.departing());
        assertThat(channels.get(publicChannel.id(), f.departing()).member()).isFalse();
        assertThatThrownBy(() -> channels.removeMember(publicChannel.id(), admin, admin)).isInstanceOf(ProjectException.class);
        context.getBean(AdminUserService.class).changeStatus(f.colleague(), false, ADMIN_EMAIL);
        assertThatThrownBy(() -> channels.addMember(privateChannel.id(), admin, f.colleague())).isInstanceOf(ProjectException.class);
    }

    @Test
    void channelHttpRequiresAuthenticationAndCsrfAndEnforcesMembership() throws Exception {
        Fixture f = fixture();
        HttpClient admin = client();
        HttpClient employee = client();
        assertThat(login(admin, ADMIN_EMAIL).statusCode()).isEqualTo(200);
        assertThat(login(employee, f.email()).statusCode()).isEqualTo(200);
        assertThat(get(client(), "/api/v1/channels").statusCode()).isEqualTo(401);
        var deniedCsrf = admin.send(HttpRequest.newBuilder(uri("/api/v1/channels"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                        "{\"name\":\"Without CSRF\",\"visibility\":\"PRIVATE\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(deniedCsrf.statusCode()).isEqualTo(403);
        var actor = context.getBean(UserRepository.class).findByEmail(ADMIN_EMAIL).orElseThrow();
        var channel = context.getBean(com.enis.sectio.channel.ChannelService.class).create(actor.getId(), "HTTP " + UUID.randomUUID(), null, "PRIVATE");
        assertThat(get(employee, "/api/v1/channels/" + channel.id()).statusCode()).isEqualTo(404);
        assertThat(mutate(admin, "/api/v1/channels/" + channel.id() + "/members/" + f.departing(), "PUT", "").statusCode()).isEqualTo(204);
        assertThat(get(employee, "/api/v1/channels/" + channel.id() + "/members").statusCode()).isEqualTo(200);
        assertThat(mutate(admin, "/api/v1/channels/" + channel.id() + "/members/" + f.departing(), "DELETE", "").statusCode()).isEqualTo(204);
        assertThat(get(employee, "/api/v1/channels/" + channel.id() + "/members").statusCode()).isEqualTo(404);
    }

    @Test
    void channelMessagesRespectMembershipOwnershipHistoryAndAnonymization() {
        Fixture f = fixture();
        var channels = context.getBean(com.enis.sectio.channel.ChannelService.class);
        var messages = context.getBean(com.enis.sectio.channel.ChannelMessageService.class);
        Long admin = context.getBean(UserRepository.class).findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        var channel = channels.create(admin, "Messages " + UUID.randomUUID(), null, "PRIVATE");
        channels.addMember(channel.id(), admin, f.departing());
        var first = messages.create(channel.id(), f.departing(), " First message ");
        var second = messages.create(channel.id(), f.departing(), "Second message");
        assertThat(first.body()).isEqualTo("First message");
        assertThat(messages.history(channel.id(), admin, null, 1)).extracting(com.enis.sectio.channel.ChannelMessageService.MessageView::id).containsExactly(second.id());
        assertThat(messages.history(channel.id(), admin, second.id(), 10)).extracting(com.enis.sectio.channel.ChannelMessageService.MessageView::id).containsExactly(first.id());
        assertThatThrownBy(() -> messages.edit(channel.id(), first.id(), admin, "Not mine")).isInstanceOf(ProjectException.class);
        assertThat(messages.edit(channel.id(), first.id(), f.departing(), "Edited").editedAt()).isNotNull();
        messages.delete(channel.id(), second.id(), f.departing());
        var deleted = messages.history(channel.id(), admin, null, 10).getFirst();
        assertThat(deleted.deleted()).isTrue();
        assertThat(deleted.body()).isNull();
        channels.removeMember(channel.id(), admin, f.departing());
        assertThatThrownBy(() -> messages.create(channel.id(), f.departing(), "No access")).isInstanceOf(ProjectException.class);
        channels.addMember(channel.id(), admin, f.departing());
        var history = messages.history(channel.id(), f.departing(), null, 10);
        assertThat(history).hasSize(3);
        var event = history.stream().filter(message -> "MEMBER_REMOVED".equals(message.eventType())).findFirst().orElseThrow();
        assertThat(event.authorId()).isEqualTo(admin);
        assertThat(event.subjectName()).isEqualTo("Departing");
        var userAdmin = context.getBean(AdminUserService.class);
        userAdmin.deleteUser(f.departing(), ADMIN_EMAIL);
        userAdmin.purgeUser(f.departing(), ADMIN_EMAIL);
        var retained = messages.history(channel.id(), admin, null, 10).stream().filter(message -> message.id().equals(first.id())).findFirst().orElseThrow();
        assertThat(retained.body()).isEqualTo("Edited");
        assertThat(retained.authorName()).startsWith("Silinmiş kullanıcı #");
        assertThat(retained.displayName()).isNull();
    }

    @Test
    void channelMessageHttpRequiresCsrfAndCurrentMembership() throws Exception {
        Fixture f = fixture();
        HttpClient employee = client();
        assertThat(login(employee, f.email()).statusCode()).isEqualTo(200);
        Long admin = context.getBean(UserRepository.class).findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        var channels = context.getBean(com.enis.sectio.channel.ChannelService.class);
        var channel = channels.create(admin, "HTTP messages " + UUID.randomUUID(), null, "PRIVATE");
        channels.addMember(channel.id(), admin, f.departing());
        String path = "/api/v1/channels/" + channel.id() + "/messages";
        var denied = employee.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"body\":\"Hello\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(mutate(employee, path, "POST", "{\"body\":\"Hello\"}").statusCode()).isEqualTo(201);
        assertThat(get(employee, path).body()).contains("Hello");
        channels.removeMember(channel.id(), admin, f.departing());
        assertThat(get(employee, path).statusCode()).isEqualTo(404);
        assertThat(mutate(employee, path, "POST", "{\"body\":\"No\"}").statusCode()).isEqualTo(404);
    }

    @Test
    void liveChannelNotifiesMembersAndClosesRemovedUser() throws Exception {
        Fixture f = fixture();
        var channels = context.getBean(com.enis.sectio.channel.ChannelService.class);
        var messages = context.getBean(com.enis.sectio.channel.ChannelMessageService.class);
        Long admin = context.getBean(UserRepository.class).findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        var channel = channels.create(admin, "Live " + UUID.randomUUID(), null, "PRIVATE");
        HttpClient employee = client();
        assertThat(login(employee, f.email()).statusCode()).isEqualTo(200);
        String ws = uri("/api/v1/channels/" + channel.id() + "/live").toString().replaceFirst("^http", "ws");
        String origin = uri("/").toString().replaceAll("/$", "");
        assertThatThrownBy(() -> employee.newWebSocketBuilder().header("Origin", origin).buildAsync(URI.create(ws), new LiveListener()).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class);
        channels.addMember(channel.id(), admin, f.departing());
        assertThatThrownBy(() -> employee.newWebSocketBuilder().header("Origin", "https://untrusted.example")
                .buildAsync(URI.create(ws), new LiveListener()).join()).isInstanceOf(java.util.concurrent.CompletionException.class);
        LiveListener listener = new LiveListener();
        var socket = employee.newWebSocketBuilder().header("Origin", origin).buildAsync(URI.create(ws), listener).get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(listener.messages.poll(10, java.util.concurrent.TimeUnit.SECONDS)).contains("connected");
        messages.create(channel.id(), admin, "Canlı bildirim");
        assertThat(listener.messages.poll(10, java.util.concurrent.TimeUnit.SECONDS)).contains("messages_changed");
        channels.removeMember(channel.id(), admin, f.departing());
        assertThat(listener.closed.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(1008);
        socket.abort();
    }

    private static class LiveListener implements java.net.http.WebSocket.Listener {
        final java.util.concurrent.BlockingQueue<String> messages = new java.util.concurrent.LinkedBlockingQueue<>();
        final java.util.concurrent.CompletableFuture<Integer> closed = new java.util.concurrent.CompletableFuture<>();
        @Override public void onOpen(java.net.http.WebSocket socket) { socket.request(1); }
        @Override public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket socket, CharSequence data, boolean last) {
            messages.add(data.toString()); socket.request(1); return null;
        }
        @Override public java.util.concurrent.CompletionStage<?> onClose(java.net.http.WebSocket socket, int statusCode, String reason) {
            closed.complete(statusCode); return null;
        }
        @Override public void onError(java.net.http.WebSocket socket, Throwable error) { closed.completeExceptionally(error); }
    }

    @Test
    void companyTeamsAndManagerRelationshipsRespectOwnershipAndProtectHierarchy() {
        Fixture f = fixture();
        var org = context.getBean(com.enis.sectio.organization.OrganizationService.class);
        var users = context.getBean(UserRepository.class);
        Long admin = users.findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        transactions.executeWithoutResult(status -> users.findById(f.departing()).orElseThrow().changeJobTitle(JobTitle.TEAM_LEADER));
        var firstTeam = org.createTeam(admin, "Engineering " + UUID.randomUUID());
        var secondTeam = org.createTeam(admin, "Quality " + UUID.randomUUID());
        var batchTeam = org.createTeam(admin, "Batch " + UUID.randomUUID(), List.of(f.departing(), f.colleague()));
        assertThat(org.relationship(admin, f.departing()).teams()).extracting(com.enis.sectio.organization.OrganizationService.Team::id).contains(batchTeam.id());
        assertThat(org.relationship(admin, f.colleague()).teams()).extracting(com.enis.sectio.organization.OrganizationService.Team::id).contains(batchTeam.id());
        org.assign(admin, f.departing(), admin, List.of(firstTeam.id()));
        var assigned = org.assign(admin, f.colleague(), f.departing(), List.of(firstTeam.id(), secondTeam.id()));
        assertThat(assigned.managerId()).isEqualTo(f.departing());
        assertThat(assigned.teams()).extracting(com.enis.sectio.organization.OrganizationService.Team::id).containsExactlyInAnyOrder(firstTeam.id(), secondTeam.id());
        assertThat(org.assign(f.departing(), f.colleague(), f.departing(), List.of(secondTeam.id())).teams()).hasSize(1);
        assertThatThrownBy(() -> org.assign(f.departing(), f.colleague(), admin, List.of())).isInstanceOf(ProjectException.class);
        assertThatThrownBy(() -> org.assign(f.colleague(), f.departing(), f.colleague(), List.of())).isInstanceOf(ProjectException.class);
        assertThatThrownBy(() -> org.assign(admin, admin, f.departing(), List.of())).isInstanceOf(ProjectException.class);
        assertThatThrownBy(() -> context.getBean(AdminUserService.class).changeStatus(f.departing(), false, ADMIN_EMAIL)).isInstanceOf(com.enis.sectio.user.admin.UserManagementException.class);
        org.assign(admin, f.colleague(), admin, List.of(secondTeam.id()));
        context.getBean(AdminUserService.class).changeStatus(f.departing(), false, ADMIN_EMAIL);
        String invalidTeamName = "Invalid batch " + UUID.randomUUID();
        assertThatThrownBy(() -> org.createTeam(admin, invalidTeamName, List.of(f.departing()))).isInstanceOf(ProjectException.class);
        assertThat(org.teams(admin)).extracting(com.enis.sectio.organization.OrganizationService.Team::name).doesNotContain(invalidTeamName);
        org.deleteTeam(admin, secondTeam.id());
        assertThat(org.relationship(admin, f.colleague()).teams()).isEmpty();
        assertThat(context.getBean(TaskService.class).get(f.sharedTask(), ADMIN_EMAIL).id()).isEqualTo(f.sharedTask());
    }

    @Test
    void createUserAtomicallyCreatesProfileAndOptionalOrganizationAssignments() throws Exception {
        fixture();
        var users = context.getBean(UserRepository.class);
        Long admin = users.findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        var org = context.getBean(com.enis.sectio.organization.OrganizationService.class);
        var team = org.createTeam(admin, "New member " + UUID.randomUUID());
        HttpClient session = client();
        assertThat(login(session, ADMIN_EMAIL).statusCode()).isEqualTo(200);
        String address = UUID.randomUUID() + "@example.com";
        String body = "{\"name\":\"New User\",\"email\":\"" + address + "\",\"temporaryPassword\":\"test-preservation-password\",\"role\":\"EMPLOYEE\",\"jobTitle\":\"SOFTWARE_DEVELOPER\",\"managerId\":" + admin + ",\"teamIds\":[" + team.id() + "]}";
        assertThat(mutate(session, "/api/v1/admin/users", "POST", body).statusCode()).isEqualTo(201);
        Long newId = users.findByEmail(address).orElseThrow().getId();
        assertThat(context.getBean(ProfileRepository.class).findById(newId)).isPresent();
        assertThat(org.relationship(admin, newId).managerId()).isEqualTo(admin);
        assertThat(org.relationship(admin, newId).teams()).extracting(com.enis.sectio.organization.OrganizationService.Team::id).containsExactly(team.id());
        String invalidAddress = UUID.randomUUID() + "@example.com";
        String invalid = body.replace(address, invalidAddress).replace("[" + team.id() + "]", "[999999999]");
        assertThat(mutate(session, "/api/v1/admin/users", "POST", invalid).statusCode()).isEqualTo(404);
        assertThat(users.findByEmail(invalidAddress)).isEmpty();
    }

    @Test
    void profileSingleCommitKeepsTextAndAvatarTogetherOnInvalidSelection() throws Exception {
        Fixture f = fixture();
        HttpClient employee = client();
        assertThat(login(employee, f.email()).statusCode()).isEqualTo(200);
        String valid = "{\"displayName\":\"İlk ad\",\"biography\":\"Not\",\"timeZone\":\"Europe/Istanbul\",\"imageMode\":\"AVATAR\",\"avatarId\":\"erkek_01_sakalsiz\"}";
        assertThat(commitProfile(employee, valid).statusCode()).isEqualTo(200);
        String invalid = valid.replace("İlk ad", "Yeni ad").replace("erkek_01_sakalsiz", "invalid-avatar");
        assertThat(commitProfile(employee, invalid).statusCode()).isEqualTo(400);
        assertThat(get(employee, "/api/v1/profile").body()).contains("İlk ad", "erkek_01_sakalsiz").doesNotContain("Yeni ad");
    }

    private HttpResponse<String> commitProfile(HttpClient client, String data) throws Exception {
        var csrf = get(client, "/api/v1/auth/csrf");
        var matcher = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"").matcher(csrf.body());
        assertThat(matcher.find()).isTrue();
        String boundary = "sectio-profile-test-boundary";
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"data\"\r\nContent-Type: application/json\r\n\r\n" + data + "\r\n--" + boundary + "--\r\n";
        return client.send(HttpRequest.newBuilder(uri("/api/v1/profile/commit"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary).header("X-XSRF-TOKEN", matcher.group(1))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private Fixture fixture() {
        return transactions.execute(status -> {
            var users = context.getBean(UserRepository.class);
            var projects = context.getBean(ProjectRepository.class);
            var members = context.getBean(ProjectMemberRepository.class);
            var tasks = context.getBean(TaskRepository.class);
            var teams = context.getBean(ProjectTeamRepository.class);
            User admin = users.findByEmail(ADMIN_EMAIL).orElseThrow();
            admin.changePasswordHash(admin.getPasswordHash());
            String suffix = UUID.randomUUID().toString();
            User departing = new User("Departing", suffix + "@example.com",
                    context.getBean(PasswordEncoder.class).encode(PASSWORD), Role.EMPLOYEE);
            departing.changePasswordHash(departing.getPasswordHash());
            users.save(departing);
            User colleague = users.save(new User("Colleague", suffix + "-other@example.com", "test-hash", Role.EMPLOYEE));
            Project project = projects.save(new Project(admin, "Work " + suffix, null));
            Project other = projects.save(new Project(admin, "Other " + suffix, null));
            Project owned = projects.save(new Project(departing, "Owned " + suffix, null));
            members.save(new ProjectMember(project, departing));
            members.save(new ProjectMember(project, colleague));
            members.save(new ProjectMember(other, departing));
            members.save(new ProjectMember(owned, departing));
            ProjectTeam team = teams.save(new ProjectTeam(project, "Team", Set.of(departing, colleague)));
            Task single = tasks.save(new Task(project, departing, Set.of(departing), "Single", null, TaskPriority.MEDIUM, null));
            Task shared = new Task(project, departing, Set.of(departing, colleague), "Shared", null, TaskPriority.HIGH, null);
            shared.changeStatus(TaskStatus.IN_PROGRESS);
            tasks.save(shared);
            Task authored = new Task(project, departing, Set.of(departing, colleague), "Authored", null, TaskPriority.MEDIUM, null);
            authored.changeStatus(TaskStatus.DONE);
            tasks.save(authored);
            Task unrelated = tasks.save(new Task(other, admin, Set.of(), "Unrelated", null, TaskPriority.MEDIUM, null));
            return new Fixture(project.getId(), other.getId(), owned.getId(), departing.getId(), colleague.getId(),
                    team.getId(), single.getId(), shared.getId(), authored.getId(), unrelated.getId(), departing.getEmail(),
                    tasks.countByStatusNotIn(List.of(TaskStatus.DONE, TaskStatus.CANCELLED)), tasks.countByStatus(TaskStatus.DONE));
        });
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + context.getEnvironment().getRequiredProperty("local.server.port") + path);
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> login(HttpClient client, String email) throws Exception {
        return login(client, email, PASSWORD);
    }

    private HttpResponse<String> login(HttpClient client, String email, String password) throws Exception {
        var csrf = get(client, "/api/v1/auth/csrf");
        assertThat(csrf.statusCode()).isEqualTo(200);
        var matcher = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"").matcher(csrf.body());
        assertThat(matcher.find()).isTrue();
        return client.send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json").header("X-XSRF-TOKEN", matcher.group(1))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private record Fixture(Long project, Long otherProject, Long ownedProject, Long departing,
                           Long colleague, Long team, Long singleTask, Long sharedTask,
                           Long authoredTask, Long unrelatedTask, String email, long openBefore, long completedBefore) {}
}
