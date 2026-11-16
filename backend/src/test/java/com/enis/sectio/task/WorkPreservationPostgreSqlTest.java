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
        var csrf = get(client, "/api/v1/auth/csrf");
        assertThat(csrf.statusCode()).isEqualTo(200);
        var matcher = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"").matcher(csrf.body());
        assertThat(matcher.find()).isTrue();
        return client.send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json").header("X-XSRF-TOKEN", matcher.group(1))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private record Fixture(Long project, Long otherProject, Long ownedProject, Long departing,
                           Long colleague, Long team, Long singleTask, Long sharedTask,
                           Long authoredTask, Long unrelatedTask, String email, long openBefore, long completedBefore) {}
}
