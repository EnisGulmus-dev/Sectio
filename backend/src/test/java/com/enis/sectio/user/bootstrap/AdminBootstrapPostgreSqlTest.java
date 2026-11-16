package com.enis.sectio.user.bootstrap;

import com.enis.sectio.SectioApplication;
import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class AdminBootstrapPostgreSqlTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine");

    @Test
    void bootstrapIsIdempotentAndMissingValuesFailOnlyWithoutAnAdmin() {
        try (ConfigurableApplicationContext firstStart = startApplication(
                "Initial Administrator",
                "admin@example.com",
                "temporary-password")) {
            UserRepository repository = firstStart.getBean(UserRepository.class);
            User admin = repository.findByEmail("admin@example.com").orElseThrow();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
            assertThat(admin.getJobTitle()).isEqualTo(JobTitle.CIO);
            assertThat(admin.isActive()).isTrue();
            assertThat(admin.isMustChangePassword()).isTrue();
            assertThat(admin.getPasswordHash()).startsWith("$2");
            assertThat(admin.getPasswordHash()).isNotEqualTo("temporary-password");
        }

        try (ConfigurableApplicationContext restartedWithoutBootstrapValues =
                     startApplication("", "", "")) {
            UserRepository repository = restartedWithoutBootstrapValues
                    .getBean(UserRepository.class);

            assertThat(repository.count()).isEqualTo(1);
            assertThat(repository.findByEmail("admin@example.com").orElseThrow().getJobTitle())
                    .isEqualTo(JobTitle.CIO);
            repository.deleteAll();
        }

        assertThatThrownBy(() -> startApplication("", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ADMIN_BOOTSTRAP_NAME must be configured when no admin exists"
                );
    }

    private ConfigurableApplicationContext startApplication(
            String adminName,
            String adminEmail,
            String adminPassword) {
        return new SpringApplicationBuilder(SectioApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--app.bootstrap.admin.name=" + adminName,
                        "--app.bootstrap.admin.email=" + adminEmail,
                        "--app.bootstrap.admin.password=" + adminPassword
                );
    }
}
