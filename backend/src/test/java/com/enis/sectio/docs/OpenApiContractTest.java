package com.enis.sectio.docs;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {

    @Test
    @SuppressWarnings("unchecked")
    void openApiDocumentIsValidYamlAndContainsAuthenticationContract()
            throws IOException {
        Path contractPath = Path.of("..", "docs", "api", "openapi.yaml");
        Map<String, Object> document = new Yaml().load(
                Files.readString(contractPath)
        );
        Map<String, Object> paths = (Map<String, Object>) document.get("paths");
        Map<String, Object> components = (Map<String, Object>) document.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        Map<String, Object> parameters = (Map<String, Object>) components.get("parameters");

        assertThat(document.get("openapi")).isEqualTo("3.1.0");
        assertThat(paths).containsKeys(
                "/actuator/health",
                "/api/v1/auth/csrf",
                "/api/v1/auth/login",
                "/api/v1/auth/me",
                "/api/v1/auth/change-password",
                "/api/v1/auth/logout",
                "/api/v1/projects",
                "/api/v1/projects/{projectId}",
                "/api/v1/projects/{projectId}/restore",
                "/api/v1/projects/{projectId}/members",
                "/api/v1/projects/{projectId}/tasks",
                "/api/v1/tasks/my",
                "/api/v1/tasks/{taskId}",
                "/api/v1/tasks/{taskId}/status",
                "/api/v1/dashboard/summary"
        );
        assertThat(schemas).containsKeys(
                "Project", "ProjectPage", "ProjectMember", "Task", "TaskPage",
                "TaskInput", "TaskStatusInput", "DashboardSummary", "ApiError"
        );
        assertThat(parameters).containsKeys(
                "ProjectId", "TaskId", "Page", "Size", "TaskStatusFilter", "TaskPriorityFilter"
        );
    }
}
