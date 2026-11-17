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
import static org.mockito.Mockito.never;
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
    @Mock private com.enis.sectio.task.EndedTaskAssignmentRepository workHistory;
    @Mock private UserWorkTrackingRepository workTracking;
    @Mock private com.enis.sectio.user.ProfileRepository profiles;
    @Mock private com.enis.sectio.user.ProfilePhotoStore profilePhotos;
    @Mock private com.enis.sectio.organization.OrganizationService organization;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, passwordEncoder, projectRepository, projectMemberRepository, taskRepository, projectTeamRepository, workHistory, workTracking, profiles, profilePhotos, organization);
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
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.changeStatus(1L, false, "admin@sectio.local"))
                .isInstanceOf(UserManagementException.class)
                .extracting(exception -> ((UserManagementException) exception).getCode())
                .isEqualTo("SELF_DEACTIVATION_NOT_ALLOWED");
    }

    @Test
    void protectsLastActiveAdminFromRoleChange() {
        User admin = user("Admin", "admin@sectio.local", Role.ADMIN);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
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
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));
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
    void closesAccessAndRemovesAssignmentsWithoutDeletingUserOrWork() {
        User employee = user("Employee", "employee@sectio.local", Role.EMPLOYEE);
        employee.deactivate();
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));

        service.deleteUser(2L, "admin@sectio.local");

        verify(taskRepository).deleteAssigneeReferencesByUserId(2L);
        verify(projectTeamRepository).deleteMembershipsByUserId(2L);
        verify(projectMemberRepository).deleteByUserId(2L);
        verify(userRepository, never()).delete(employee);
        assertThat(employee.isActive()).isFalse();
        assertThat(employee.getDeletedAt()).isNotNull();
    }

    @Test
    void activeUserIsRetainedWithRevokedSessionVersion() {
        User employee = user("Employee", "employee@sectio.local", Role.EMPLOYEE);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));

        service.deleteUser(2L, "admin@sectio.local");

        verify(userRepository, never()).delete(employee);
        assertThat(employee.isActive()).isFalse();
        assertThat(employee.getSessionVersion()).isEqualTo(1);
    }

    @Test
    void ownershipDoesNotPreventAccessClosureOrDeleteTheProject() {
        User employee = user("Employee", "employee@sectio.local", Role.EMPLOYEE);
        employee.deactivate();
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));
        service.deleteUser(2L, "admin@sectio.local");
        assertThat(employee.isActive()).isFalse();
        verify(userRepository, never()).delete(employee);
        org.mockito.Mockito.verifyNoInteractions(projectRepository);
    }

    @Test
    void lastActiveAdminCannotBeClosedThroughDeleteEndpoint() {
        User admin = user("Other admin", "other-admin@sectio.local", Role.ADMIN);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);
        assertThatThrownBy(() -> service.deleteUser(2L, "admin@sectio.local"))
                .isInstanceOf(UserManagementException.class)
                .extracting(exception -> ((UserManagementException) exception).getCode())
                .isEqualTo("LAST_ACTIVE_ADMIN");
        assertThat(admin.isActive()).isTrue();
        org.mockito.Mockito.verifyNoInteractions(taskRepository, projectMemberRepository, projectTeamRepository);
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

    @Test
    void deletedUserCannotBeReactivatedOrResetThroughManagementEndpoints() {
        User employee = user("Former", "former@example.com", Role.EMPLOYEE);
        employee.deleteAccount();
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));
        assertThatThrownBy(() -> service.changeStatus(2L, true, "admin@sectio.local"))
                .isInstanceOf(UserManagementException.class)
                .extracting(exception -> ((UserManagementException) exception).getCode())
                .isEqualTo("USER_DELETED");
        assertThatThrownBy(() -> service.resetPassword(2L, "temporary-123", "admin@sectio.local"))
                .isInstanceOf(UserManagementException.class);
        assertThatThrownBy(() -> service.updateUser(2L, "Renamed", Role.ADMIN, JobTitle.CIO, "admin@sectio.local"))
                .isInstanceOf(UserManagementException.class);
        assertThat(employee.isActive()).isFalse();
    }

    @Test
    void repeatedDeletionDoesNotChangeTheDeletionTime() {
        User employee = user("Former", "former@example.com", Role.EMPLOYEE);
        employee.deleteAccount();
        var deletedAt = employee.getDeletedAt();
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));
        service.deleteUser(2L, "admin@sectio.local");
        assertThat(employee.getDeletedAt()).isEqualTo(deletedAt);
        org.mockito.Mockito.verifyNoInteractions(taskRepository, projectMemberRepository, projectTeamRepository);
    }

    @Test
    void restoreRequiresDeletedAccountAndNewPasswordAndRevokesOldSessions() {
        User employee = user("Former", "former@example.com", Role.EMPLOYEE);
        employee.deleteAccount();
        long version = employee.getSessionVersion();
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("new-temporary-123")).thenReturn("new-hash");
        var restored = service.restoreUser(2L, "new-temporary-123");
        assertThat(restored.active()).isTrue();
        assertThat(restored.deletedAt()).isNull();
        assertThat(restored.mustChangePassword()).isTrue();
        assertThat(employee.getSessionVersion()).isGreaterThan(version);
        assertThat(employee.getPasswordHash()).isEqualTo("new-hash");
        org.mockito.Mockito.verifyNoInteractions(projectMemberRepository, taskRepository, projectTeamRepository);
        assertThatThrownBy(() -> service.restoreUser(2L, "new-temporary-123")).isInstanceOf(UserManagementException.class);
    }

    @Test
    void purgeAnonymizesOnlyDeletedAccountsAndPreventsRestore() {
        User employee = user("Former", "former@example.com", Role.EMPLOYEE);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));
        assertThatThrownBy(() -> service.purgeUser(2L, "admin@example.com")).isInstanceOf(UserManagementException.class);
        employee.deleteAccount();
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("unusable-hash");
        service.purgeUser(2L, "admin@example.com");
        assertThat(employee.getPurgedAt()).isNotNull();
        assertThat(employee.isActive()).isFalse();
        assertThat(employee.getEmail()).endsWith("@deleted.invalid").isNotEqualTo("former@example.com");
        assertThat(employee.getName()).doesNotContain("Former");
        assertThatThrownBy(() -> service.restoreUser(2L, "new-temporary-123")).isInstanceOf(UserManagementException.class);
        service.purgeUser(2L, "admin@example.com"); // Idempotent, no historical work removal.
        org.mockito.Mockito.verifyNoInteractions(projectRepository, projectMemberRepository, taskRepository, projectTeamRepository, workHistory);
    }

    @Test
    void purgeSchedulesStoredProfilePhotoForRemoval() {
        User employee = user("Former", "former@example.com", Role.EMPLOYEE);
        employee.deleteAccount();
        var profile = new com.enis.sectio.user.Profile(2L);
        profile.setPhotoKey("12345678-1234-1234-1234-123456789abc.png");
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));
        when(profiles.findById(2L)).thenReturn(Optional.of(profile));
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("unusable-hash");
        service.purgeUser(2L, "admin@example.com");
        verify(profilePhotos).deleteAfterCommit(profile.getPhotoKey());
        verify(userRepository).deleteProfileByUserId(2L);
    }

    @Test
    void reusedEmailOfDeletedAccountExplainsRestoreOption() {
        User employee = user("Former", "former@example.com", Role.EMPLOYEE);
        employee.deleteAccount();
        when(userRepository.existsByEmail(employee.getEmail())).thenReturn(true);
        when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        assertThatThrownBy(() -> service.createUser("Another", employee.getEmail(), "temporary-123", Role.EMPLOYEE, JobTitle.SOFTWARE_DEVELOPER))
                .isInstanceOf(UserManagementException.class).hasMessageContaining("Silinenler");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void changingEmailRevokesSessionsAndRejectsDuplicateAddresses() {
        User employee = user("Employee", "old@example.com", Role.EMPLOYEE);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.updateUser(2L, "Employee", "taken@example.com", Role.EMPLOYEE, JobTitle.SOFTWARE_DEVELOPER, "admin@example.com"))
                .isInstanceOf(UserManagementException.class);
        assertThat(employee.getEmail()).isEqualTo("old@example.com");
        long previous = employee.getSessionVersion();
        service.updateUser(2L, "Employee", "NEW@example.com", Role.EMPLOYEE, JobTitle.SOFTWARE_DEVELOPER, "admin@example.com");
        assertThat(employee.getEmail()).isEqualTo("new@example.com");
        assertThat(employee.getSessionVersion()).isGreaterThan(previous);
    }

    private static User user(String name, String email, Role role) {
        return new User(name, email, "password-hash", role);
    }
}
