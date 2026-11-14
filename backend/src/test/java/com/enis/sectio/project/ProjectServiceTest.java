package com.enis.sectio.project;

import com.enis.sectio.task.TaskRepository;
import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import com.enis.sectio.task.TaskStatus;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock ProjectRepository projects;
    @Mock ProjectMemberRepository members;
    @Mock UserRepository users;
    @Mock TaskRepository tasks;
    @Mock ProjectTeamRepository teams;
    ProjectService service;

    @BeforeEach
    void setup() { service = new ProjectService(projects, members, users, tasks, teams); }

    @Test
    void ownerBecomesMemberWhenProjectIsCreated() {
        User admin = new User("Admin", "admin@test.local", "hash", Role.ADMIN);
        when(users.findByEmail("admin@test.local")).thenReturn(Optional.of(admin));
        when(projects.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create("admin@test.local", "Project", "Description");

        verify(members).save(argThat(member -> member.getUser() == admin));
    }

    @Test
    void inactiveUserCannotBeAdded() {
        User admin = new User("Admin", "admin@test.local", "hash", Role.ADMIN);
        User inactive = new User("Inactive", "inactive@test.local", "hash", Role.EMPLOYEE);
        inactive.deactivate();
        Project project = new Project(admin, "Project", null);
        when(users.findByEmail("admin@test.local")).thenReturn(Optional.of(admin));
        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(users.findById(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.addMember(1L, 2L, "admin@test.local"))
                .isInstanceOf(ProjectException.class).hasMessageContaining("Inactive");
    }

    @Test
    void archivedProjectCanBeRestored() {
        User admin = user(1L, "admin@test.local", Role.ADMIN);
        Project project = project(10L, admin, true);
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(projects.findById(10L)).thenReturn(Optional.of(project));

        ProjectResponse response = service.restore(10L, admin.getEmail());

        assertThat(response.archived()).isFalse();
    }

    @Test
    void deleteRemovesChildrenBeforeArchivedProject() {
        User admin = user(1L, "admin@test.local", Role.ADMIN);
        Project project = project(10L, admin, true);
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(projects.findById(10L)).thenReturn(Optional.of(project));

        service.delete(10L, admin.getEmail());

        var order = inOrder(tasks, members, projects);
        order.verify(tasks).deleteByProjectId(10L);
        order.verify(members).deleteByProjectId(10L);
        order.verify(projects).delete(project);
    }

    @Test
    void activeProjectCanBeDeleted() {
        User admin = user(1L, "admin@test.local", Role.ADMIN);
        Project project = project(10L, admin, false);
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(projects.findById(10L)).thenReturn(Optional.of(project));

        service.delete(10L, admin.getEmail());

        verify(tasks).deleteByProjectId(10L);
        verify(members).deleteByProjectId(10L);
        verify(projects).delete(project);
    }

    @Test
    void employeeCannotCreateProjectButTeamLeaderCan() {
        User employee = user(2L, "employee@test.local", Role.EMPLOYEE);
        when(users.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        assertThatThrownBy(() -> service.create(employee.getEmail(), "Project", null))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getCode())
                .isEqualTo("PROJECT_CREATION_DENIED");

        employee.changeJobTitle(JobTitle.TEAM_LEADER);
        when(projects.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(employee.getEmail(), "Project", null);
        verify(members).save(argThat(member -> member.getUser() == employee));
    }

    @Test
    void memberWithActiveTaskCannotBeRemoved() {
        User admin = user(1L, "admin@test.local", Role.ADMIN);
        User employee = user(2L, "employee@test.local", Role.EMPLOYEE);
        Project project = project(10L, admin, false);
        ProjectMember member = new ProjectMember(project, employee);
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));
        when(tasks.existsByProjectIdAndAssigneesIdAndStatusNotIn(10L, 2L, List.of(TaskStatus.DONE, TaskStatus.CANCELLED))).thenReturn(true);

        assertThatThrownBy(() -> service.removeMember(10L, 2L, admin.getEmail()))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getCode())
                .isEqualTo("MEMBER_HAS_ACTIVE_TASKS");
        verify(members, never()).delete(member);
    }

    @Test
    void memberCanBeRemovedWhenTasksAreCompletedOrCancelled() {
        User admin = user(1L, "admin@test.local", Role.ADMIN);
        User employee = user(2L, "employee@test.local", Role.EMPLOYEE);
        Project project = project(10L, admin, false);
        ProjectMember member = new ProjectMember(project, employee);
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));
        when(tasks.existsByProjectIdAndAssigneesIdAndStatusNotIn(10L, 2L, List.of(TaskStatus.DONE, TaskStatus.CANCELLED))).thenReturn(false);

        service.removeMember(10L, 2L, admin.getEmail());

        verify(tasks).deleteAssigneeReferencesByProjectAndUser(10L, 2L);
        verify(tasks).deleteTasksWithoutAssignees();
        verify(teams).deleteMembershipsByProjectAndUser(10L, 2L);
        verify(members).delete(member);
    }

    @Test
    void projectOwnerIsPinnedBeforeAlphabeticalMembers() {
        User owner = user(1L, "z-owner@test.local", Role.ADMIN);
        User alphabeticallyFirst = user(2L, "a-member@test.local", Role.EMPLOYEE);
        Project project = project(10L, owner, false);
        ProjectMember ownerMember = new ProjectMember(project, owner);
        ProjectMember otherMember = new ProjectMember(project, alphabeticallyFirst);
        when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(projects.findById(10L)).thenReturn(Optional.of(project));
        when(members.findByProjectIdOrderByUserNameAsc(10L)).thenReturn(List.of(otherMember, ownerMember));

        List<ProjectMemberResponse> result = service.listMembers(10L, owner.getEmail());

        assertThat(result).extracting(ProjectMemberResponse::userId).containsExactly(1L, 2L);
    }

    private static User user(Long id, String email, Role role) {
        User user = new User("User", email, "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Project project(Long id, User owner, boolean archived) {
        Project project = new Project(owner, "Project", null);
        ReflectionTestUtils.setField(project, "id", id);
        if (archived) project.archive();
        return project;
    }
}
