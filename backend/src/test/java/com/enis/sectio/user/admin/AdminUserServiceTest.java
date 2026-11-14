package com.enis.sectio.user.admin;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import com.enis.sectio.project.ProjectRepository;
import com.enis.sectio.project.ProjectMemberRepository;
import com.enis.sectio.project.ProjectTeamRepository;
import com.enis.sectio.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ProjectTeamRepository projectTeamRepository;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, passwordEncoder, projectRepository, projectMemberRepository, taskRepository, projectTeamRepository);
    }

    @Test
    void createsUserWithNormalizedEmailAndEncodedTemporaryPassword() {
        when(userRepository.existsByEmail("employee@sectio.local")).thenReturn(false);
        when(passwordEncoder.encode("temporary-123")).thenReturn("encoded-password");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = service.createUser(
                "  New Employee  ",
                " Employee@Sectio.Local ",
                "temporary-123",
                Role.EMPLOYEE,
                JobTitle.TEAM_LEADER
        );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("New Employee");
        assertThat(saved.getEmail()).isEqualTo("employee@sectio.local");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getJobTitle()).isEqualTo(JobTitle.TEAM_LEADER);
        assertThat(response.mustChangePassword()).isTrue();
    }

    @Test
    void preventsAdminFromDeactivatingOwnAccount() {
        User admin = user("Admin", "admin@sectio.local", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.changeStatus(1L, false, "admin@sectio.local"))
                .isInstanceOf(UserManagementException.class)
                .extracting(exception -> ((UserManagementException) exception).getCode())
                .isEqualTo("SELF_DEACTIVATION_NOT_ALLOWED");
    }

    @Test
    void protectsLastActiveAdminFromRoleChange() {
        User admin = user("Admin", "admin@sectio.local", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.updateUser(
                1L,
                "Admin",
                Role.EMPLOYEE,
                JobTitle.SOFTWARE_DEVELOPER,
                "another-admin@sectio.local"
        ))
                .isInstanceOf(UserManagementException.class)
                .extracting(exception -> ((UserManagementException) exception).getCode())
                .isEqualTo("LAST_ACTIVE_ADMIN");
    }

    @Test
    void resetPasswordRequiresChangeAtNextLogin() {
        User employee = user("Employee", "employee@sectio.local", Role.EMPLOYEE);
        employee.changePasswordHash("old-hash");
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("new-temporary-123")).thenReturn("new-hash");

        AdminUserResponse response = service.resetPassword(
                2L,
                "new-temporary-123",
                "admin@sectio.local"
        );

        assertThat(employee.getPasswordHash()).isEqualTo("new-hash");
        assertThat(response.mustChangePassword()).isTrue();
    }

    @Test
    void deletesUserTogetherWithTasksAndMemberships() {
        User employee = user("Employee", "employee@sectio.local", Role.EMPLOYEE);
        employee.deactivate();
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));

        service.deleteUser(2L, "admin@sectio.local");

        verify(taskRepository).deleteByCreatedById(2L);
        verify(taskRepository).deleteAssigneeReferencesByUserId(2L);
        verify(taskRepository).deleteTasksWithoutAssignees();
        verify(projectTeamRepository).deleteMembershipsByUserId(2L);
        verify(projectMemberRepository).deleteByUserId(2L);
        verify(userRepository).delete(employee);
    }

    @Test
    void activeUserCanBeDeletedAfterImpactConfirmation() {
        User employee = user("Employee", "employee@sectio.local", Role.EMPLOYEE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));

        service.deleteUser(2L, "admin@sectio.local");

        verify(userRepository).delete(employee);
    }

    @Test
    void projectOwnerCannotBeDeleted() {
        User employee = user("Employee", "employee@sectio.local", Role.EMPLOYEE);
        employee.deactivate();
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(projectRepository.existsByOwnerId(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteUser(2L, "admin@sectio.local"))
                .isInstanceOf(UserManagementException.class)
                .extracting(exception -> ((UserManagementException) exception).getCode())
                .isEqualTo("PROJECT_OWNER_DELETION_NOT_ALLOWED");
    }

    @Test
    void reportsDeletionImpactBeforeConfirmation() {
        User employee = user("Employee", "employee@sectio.local", Role.EMPLOYEE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(projectMemberRepository.countByUserId(2L)).thenReturn(2L);
        when(taskRepository.countDistinctByAssigneesId(2L)).thenReturn(3L);
        when(taskRepository.countByCreatedById(2L)).thenReturn(1L);
        when(taskRepository.countByUserReference(2L)).thenReturn(3L);

        var impact = service.deletionImpact(2L);

        assertThat(impact.projectMemberships()).isEqualTo(2);
        assertThat(impact.assignedTasks()).isEqualTo(3);
        assertThat(impact.affectedTasks()).isEqualTo(3);
    }

    private static User user(String name, String email, Role role) {
        return new User(name, email, "password-hash", role);
    }
}
