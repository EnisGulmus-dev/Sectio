package com.enis.sectio.user;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class CioJobTitlePostgreSqlTest {
    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine");

    @Test
    void upgradesDefaultAdminTitleWithoutChangingEmployeesOrCustomAdminTitles() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("8").load().migrate();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (name, email, password_hash, role, job_title)
                    VALUES ('Default admin', 'default@example.com', 'unchanged-hash', 'ADMIN', 'SOFTWARE_DEVELOPER'),
                           ('Custom admin', 'custom@example.com', 'unchanged-hash', 'ADMIN', 'PROJECT_MANAGER'),
                           ('Employee', 'employee@example.com', 'unchanged-hash', 'EMPLOYEE', 'SOFTWARE_DEVELOPER')
                    """);
        }
        var migration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("9").load();
        assertThat(migration.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(migration.migrate().migrationsExecuted).isZero();

        Map<String, String> titles = new HashMap<>();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var users = statement.executeQuery(
                     "SELECT email, job_title, role, password_hash, active, must_change_password FROM users")) {
            while (users.next()) {
                String email = users.getString("email");
                titles.put(email, users.getString("job_title"));
                assertThat(users.getString("role"))
                        .isEqualTo(email.equals("employee@example.com") ? "EMPLOYEE" : "ADMIN");
                assertThat(users.getString("password_hash")).isEqualTo("unchanged-hash");
                assertThat(users.getBoolean("active")).isTrue();
                assertThat(users.getBoolean("must_change_password")).isTrue();
            }
        }
        assertThat(titles).containsExactlyInAnyOrderEntriesOf(Map.of(
                "default@example.com", "CIO",
                "custom@example.com", "PROJECT_MANAGER",
                "employee@example.com", "SOFTWARE_DEVELOPER"));

        // V10 must backfill the existing rows without changing their credentials.
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("10").load().migrate();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var rows = statement.executeQuery(
                     "SELECT count(*) FROM users WHERE session_version=0 AND password_hash='unchanged-hash'")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong(1)).isEqualTo(3);
        }

        // An existing inactive user must not be guessed to be a deleted user.
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE users SET active=false WHERE email='employee@example.com'");
        }
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("11").load().migrate();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT count(*) FROM users WHERE deleted_at IS NULL")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong(1)).isEqualTo(3);
        }
    }
}
