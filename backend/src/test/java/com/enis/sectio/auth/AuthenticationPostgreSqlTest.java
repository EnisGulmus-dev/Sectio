package com.enis.sectio.auth;

import com.enis.sectio.SectioApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class AuthenticationPostgreSqlTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\\"token\\\":\\\"([^\\\"]+)\\\""
    );

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine");

    @Test
    void sessionAuthenticationPasswordChangeAndLogoutWorkEndToEnd()
            throws Exception {
        try (ConfigurableApplicationContext context = startApplication()) {
            int port = context.getBean(
                    ServletWebServerApplicationContext.class
            ).getWebServer().getPort();
            URI baseUri = URI.create("http://localhost:" + port);

            CookieManager cookieManager = new CookieManager(
                    null,
                    CookiePolicy.ACCEPT_ALL
            );
            HttpClient client = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build();

            String loginCsrfToken = fetchCsrfToken(client, baseUri);
            HttpResponse<String> login = postJson(
                    client,
                    baseUri.resolve("/api/v1/auth/login"),
                    loginCsrfToken,
                    """
                            {
                              "email": "admin@example.com",
                              "password": "temporary-password"
                            }
                            """
            );
            assertThat(login.statusCode()).isEqualTo(200);
            assertThat(login.body())
                    .contains("\"role\":\"ADMIN\"")
                    .contains("\"mustChangePassword\":true");

            HttpResponse<String> blocked = get(
                    client,
                    baseUri.resolve("/api/v1/projects")
            );
            assertThat(blocked.statusCode()).isEqualTo(403);
            assertThat(blocked.body()).contains("PASSWORD_CHANGE_REQUIRED");

            String authenticatedCsrfToken = fetchCsrfToken(client, baseUri);
            HttpResponse<String> changed = postJson(
                    client,
                    baseUri.resolve("/api/v1/auth/change-password"),
                    authenticatedCsrfToken,
                    """
                            {
                              "currentPassword": "temporary-password",
                              "newPassword": "new-secure-password"
                            }
                            """
            );
            assertThat(changed.statusCode()).isEqualTo(200);
            assertThat(changed.body())
                    .contains("\"mustChangePassword\":false");

            HttpResponse<String> me = get(
                    client,
                    baseUri.resolve("/api/v1/auth/me")
            );
            assertThat(me.statusCode()).isEqualTo(200);
            assertThat(me.body()).contains("\"email\":\"admin@example.com\"");

            HttpResponse<String> logout = postJson(
                    client,
                    baseUri.resolve("/api/v1/auth/logout"),
                    authenticatedCsrfToken,
                    ""
            );
            assertThat(logout.statusCode()).isEqualTo(204);

            HttpResponse<String> afterLogout = get(
                    client,
                    baseUri.resolve("/api/v1/auth/me")
            );
            assertThat(afterLogout.statusCode()).isEqualTo(401);
        }
    }

    private ConfigurableApplicationContext startApplication() {
        return new SpringApplicationBuilder(SectioApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--app.bootstrap.admin.name=Initial Administrator",
                        "--app.bootstrap.admin.email=admin@example.com",
                        "--app.bootstrap.admin.password=temporary-password"
                );
    }

    private String fetchCsrfToken(HttpClient client, URI baseUri)
            throws Exception {
        HttpResponse<String> response = get(
                client,
                baseUri.resolve("/api/v1/auth/csrf")
        );
        assertThat(response.statusCode()).isEqualTo(200);

        Matcher matcher = TOKEN_PATTERN.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private HttpResponse<String> get(HttpClient client, URI uri)
            throws Exception {
        return client.send(
                HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> postJson(
            HttpClient client,
            URI uri,
            String csrfToken,
            String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/json")
                        .header("X-XSRF-TOKEN", csrfToken)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }
}
