package com.enis.sectio.security;

import com.enis.sectio.common.exception.ApiExceptionHandler;
import com.enis.sectio.project.*;
import com.enis.sectio.task.*;
import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig
@ContextConfiguration(classes = ProjectTaskAuthorizationIntegrationTest.TestConfiguration.class)
@WebAppConfiguration
class ProjectTaskAuthorizationIntegrationTest {
    private static final String OWNER_EMAIL = "owner@test.local";
    private static final String MEMBER_EMAIL = "member@test.local";
    private static final String OUTSIDER_EMAIL = "outsider@test.local";
    private static final String ADMIN_EMAIL = "admin@test.local";

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired ProjectMemberRepository members;
    @Autowired TaskRepository tasks;

    private MockMvc mvc;
    private User owner;
    private User member;
    private User outsider;
    private User admin;
    private Project project;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        reset(users, projects, members, tasks);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = user(1L, "Owner", OWNER_EMAIL, Role.EMPLOYEE);
        member = user(2L, "Member", MEMBER_EMAIL, Role.EMPLOYEE);
        outsider = user(3L, "Outsider", OUTSIDER_EMAIL, Role.EMPLOYEE);
        admin = user(4L, "Admin", ADMIN_EMAIL, Role.ADMIN);
        project = new Project(owner, "Authorized project", "Description");
        ReflectionTestUtils.setField(project, "id", 10L);
        when(projects.findById(10L)).thenReturn(Optional.of(project));
    }

    @Test
    void anonymousUserCannotReadProjects() throws Exception {
        mvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @WithMockUser(username = OUTSIDER_EMAIL, roles = "EMPLOYEE")
    void nonMemberCannotReadProject() throws Exception {
        authenticate(outsider);
        when(members.existsByProjectIdAndUserId(10L, 3L)).thenReturn(false);

        mvc.perform(get("/api/v1/projects/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = MEMBER_EMAIL, roles = "EMPLOYEE")
    void projectMemberCanReadProject() throws Exception {
        authenticate(member);
        when(members.existsByProjectIdAndUserId(10L, 2L)).thenReturn(true);

        mvc.perform(get("/api/v1/projects/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Authorized project"));
    }

    @Test
    @WithMockUser(username = MEMBER_EMAIL, roles = "EMPLOYEE")
    void ordinaryMemberCannotArchiveProject() throws Exception {
        authenticate(member);

        mvc.perform(post("/api/v1/projects/10/archive").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_MANAGEMENT_DENIED"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "EMPLOYEE")
    void projectOwnerCanArchiveProject() throws Exception {
        authenticate(owner);

        mvc.perform(post("/api/v1/projects/10/archive").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "EMPLOYEE")
    void projectOwnerCanRestoreArchivedProject() throws Exception {
        authenticate(owner);
        project.archive();

        mvc.perform(post("/api/v1/projects/10/restore").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "EMPLOYEE")
    void archivedProjectCanBePermanentlyDeleted() throws Exception {
        authenticate(owner);
        project.archive();

        mvc.perform(delete("/api/v1/projects/10").with(csrf()))
                .andExpect(status().isNoContent());

        verify(tasks).deleteByProjectId(10L);
        verify(members).deleteByProjectId(10L);
        verify(projects).delete(project);
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "EMPLOYEE")
    void projectOwnerCannotBeRemoved() throws Exception {
        authenticate(owner);

        mvc.perform(delete("/api/v1/projects/10/members/1").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNER_REMOVAL_NOT_ALLOWED"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "EMPLOYEE")
    void stateChangingRequestRequiresCsrf() throws Exception {
        authenticate(owner);

        mvc.perform(post("/api/v1/projects/10/archive"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "EMPLOYEE")
    void taskCannotBeAssignedToProjectOutsider() throws Exception {
        authenticate(owner);
        when(users.findById(3L)).thenReturn(Optional.of(outsider));
        when(members.existsByProjectIdAndUserId(10L, 3L)).thenReturn(false);

        mvc.perform(post("/api/v1/projects/10/tasks").with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"assigneeIds":[3],"teamIds":[],"allMembers":false,"title":"Forbidden assignment","description":"","priority":"HIGH","dueAt":null}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNEE"));
    }

    @Test
    @WithMockUser(username = OWNER_EMAIL, roles = "EMPLOYEE")
    void archivedProjectRejectsTaskChanges() throws Exception {
        authenticate(owner);
        project.archive();
        Task task = new Task(project, owner, Set.of(owner), "Archived task", null, TaskPriority.MEDIUM, null);
        ReflectionTestUtils.setField(task, "id", 20L);
        when(tasks.findById(20L)).thenReturn(Optional.of(task));

        mvc.perform(patch("/api/v1/tasks/20/status").with(csrf())
                        .contentType("application/json")
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_ARCHIVED"));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
    void adminCanReadAnyProject() throws Exception {
        authenticate(admin);

        mvc.perform(get("/api/v1/projects/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
        verify(members, never()).existsByProjectIdAndUserId(any(), any());
    }

    private void authenticate(User user) { when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user)); }

    private static User user(Long id, String name, String email, Role role) {
        User user = new User(name, email, "password-hash", role);
        user.changePasswordHash("changed-password-hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
            PasswordChangeRequiredFilter.class, ProjectController.class, ProjectService.class, ProjectTeamService.class,
            TaskController.class, TaskService.class, ApiExceptionHandler.class})
    static class TestConfiguration {
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean ProjectRepository projectRepository() { return mock(ProjectRepository.class); }
        @Bean ProjectMemberRepository projectMemberRepository() { return mock(ProjectMemberRepository.class); }
        @Bean ProjectTeamRepository projectTeamRepository() { return mock(ProjectTeamRepository.class); }
        @Bean TaskRepository taskRepository() { return mock(TaskRepository.class); }
    }
}
