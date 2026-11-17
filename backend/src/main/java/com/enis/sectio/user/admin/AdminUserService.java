package com.enis.sectio.user.admin;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import com.enis.sectio.project.ProjectRepository;
import com.enis.sectio.project.ProjectMemberRepository;
import com.enis.sectio.project.ProjectTeamRepository;
import com.enis.sectio.task.TaskRepository;
import com.enis.sectio.task.EndedTaskAssignmentRepository;
import com.enis.sectio.common.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final EndedTaskAssignmentRepository workHistory;
    private final UserWorkTrackingRepository workTracking;
    private final com.enis.sectio.user.ProfileRepository profiles;
    private final com.enis.sectio.user.ProfilePhotoStore profilePhotos;
    private final com.enis.sectio.organization.OrganizationService organization;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            ProjectRepository projectRepository,
                            ProjectMemberRepository projectMemberRepository,
                            TaskRepository taskRepository,
                            ProjectTeamRepository projectTeamRepository,
                            EndedTaskAssignmentRepository workHistory,
                            UserWorkTrackingRepository workTracking, com.enis.sectio.user.ProfileRepository profiles,
                            com.enis.sectio.user.ProfilePhotoStore profilePhotos, com.enis.sectio.organization.OrganizationService organization) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
        this.projectTeamRepository = projectTeamRepository;
        this.workHistory = workHistory;
        this.workTracking = workTracking;
        this.profiles = profiles; this.profilePhotos = profilePhotos; this.organization = organization;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(boolean deleted) {
        return (deleted ? userRepository.findByDeletedAtIsNotNullAndPurgedAtIsNullOrderByNameAsc()
                : userRepository.findByDeletedAtIsNullOrderByNameAsc())
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(String name, String email, String password, Role role, JobTitle jobTitle) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);

        if (userRepository.existsByEmail(normalizedEmail)) {
            if (userRepository.findByEmail(normalizedEmail).filter(existing -> existing.getDeletedAt() != null).isPresent()) {
                throw conflict("EMAIL_BELONGS_TO_DELETED_USER", "Bu e-posta silinmiş bir hesaba ait. Silinenler bölümünden hesabı geri yükleyin veya kalıcı silin.");
            }
            throw conflict("EMAIL_ALREADY_EXISTS", "Bu e-posta adresiyle kayıtlı bir kullanıcı zaten var.");
        }

        User user = new User(name, normalizedEmail, passwordEncoder.encode(password), role);
        user.changeJobTitle(jobTitle);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateUser(
            Long userId,
            String name,
            Role role,
            JobTitle jobTitle,
            String currentAdminEmail) {
        return updateUser(userId, name, null, role, jobTitle, currentAdminEmail);
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, String name, String email, Role role, JobTitle jobTitle, String currentAdminEmail) {
        organization.lockOrganization();
        User user = findManageableUser(userId);
        boolean currentUser = user.getEmail().equals(normalizeEmail(currentAdminEmail));

        if (currentUser && user.getRole() != role) {
            throw conflict("SELF_ROLE_CHANGE_NOT_ALLOWED", "You cannot change your own role");
        }

        if (user.getRole() == Role.ADMIN
                && role != Role.ADMIN
                && user.isActive()
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw conflict("LAST_ACTIVE_ADMIN", "The last active admin cannot lose the admin role");
        }

        if (role != Role.ADMIN && !jobTitle.canCreateProjects() && organization.hasDirectReports(userId)) {
            throw conflict("MANAGER_HAS_REPORTS", "Yönetici yetkisini kaldırmadan önce bağlı çalışanları devredin.");
        }
        if (email != null) {
            String normalized = normalizeEmail(email);
            if (!normalized.equals(user.getEmail()) && userRepository.existsByEmail(normalized)) {
                throw conflict("EMAIL_ALREADY_EXISTS", "Bu e-posta adresi başka bir hesap tarafından kullanılıyor.");
            }
            user.setEmail(normalized);
        }
        user.setName(name);
        user.changeRole(role);
        user.changeJobTitle(jobTitle);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse changeStatus(
            Long userId,
            boolean active,
            String currentAdminEmail) {
        organization.lockOrganization();
        User user = findManageableUser(userId);
        boolean currentUser = user.getEmail().equals(normalizeEmail(currentAdminEmail));

        if (!active && currentUser) {
            throw conflict("SELF_DEACTIVATION_NOT_ALLOWED", "You cannot deactivate your own account");
        }

        if (!active
                && user.getRole() == Role.ADMIN
                && user.isActive()
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw conflict("LAST_ACTIVE_ADMIN", "The last active admin cannot be deactivated");
        }

        if (!active && organization.hasDirectReports(userId)) {
            throw conflict("MANAGER_HAS_REPORTS", "Önce bağlı çalışanları başka bir yöneticiye devredin.");
        }
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse resetPassword(
            Long userId,
            String temporaryPassword,
            String currentAdminEmail) {
        User user = findManageableUser(userId);
        if (user.getEmail().equals(normalizeEmail(currentAdminEmail))) {
            throw conflict(
                    "SELF_PASSWORD_RESET_NOT_ALLOWED",
                    "Use the change-password flow to change your own password"
            );
        }

        validatePassword(temporaryPassword);
        user.resetPasswordHash(passwordEncoder.encode(temporaryPassword));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId, String currentAdminEmail) {
        organization.lockOrganization();
        User user = lockUser(userId);
        if (user.getDeletedAt() != null) return;
        if (user.getEmail().equals(normalizeEmail(currentAdminEmail))) {
            throw conflict("SELF_DELETION_NOT_ALLOWED", "You cannot delete your own account");
        }
        if (userRepository.countOwnedChannels(userId) > 0) {
            throw conflict("USER_OWNS_CHANNELS", "Kullanıcıyı silmeden önce sahip olduğu kanalları devretmesi veya silmesi gerekir.");
        }
        // Deletion is distinct from temporary deactivation, while the historical
        // identity remains referenced by authored work and project ownership.
        changeStatus(userId, false, currentAdminEmail);
        organization.removeDeletedUser(userId);
        user.deleteAccount();
        taskRepository.deleteAssigneeReferencesByUserId(userId);
        projectTeamRepository.deleteMembershipsByUserId(userId);
        projectMemberRepository.deleteByUserId(userId);
    }

    @Transactional
    public AdminUserResponse restoreUser(Long userId, String temporaryPassword) {
        User user = lockUser(userId);
        if (user.getPurgedAt() != null) throw conflict("USER_PURGED", "Kalıcı silinen hesap geri yüklenemez.");
        if (user.getDeletedAt() == null) throw conflict("USER_NOT_DELETED", "Bu hesap silinmiş değil.");
        validatePassword(temporaryPassword);
        user.restoreAccount(passwordEncoder.encode(temporaryPassword));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void purgeUser(Long userId, String currentAdminEmail) {
        User user = lockUser(userId);
        if (user.getEmail().equals(normalizeEmail(currentAdminEmail))) {
            throw conflict("SELF_DELETION_NOT_ALLOWED", "Kendi hesabınızı kalıcı silemezsiniz.");
        }
        if (user.getPurgedAt() != null) return;
        if (user.getDeletedAt() == null) throw conflict("USER_NOT_DELETED", "Önce hesabı Silinenler bölümüne taşıyın.");
        profiles.findById(userId).ifPresent(profile -> profilePhotos.deleteAfterCommit(profile.getPhotoKey()));
        userRepository.deleteProfileByUserId(userId);
        user.purgeAccount(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
    }

    @Transactional(readOnly = true)
    public UserDeletionImpact deletionImpact(Long userId) {
        findUser(userId);
        return new UserDeletionImpact(
                projectRepository.countByOwnerId(userId),
                projectMemberRepository.countByUserId(userId),
                taskRepository.countDistinctByAssigneesId(userId),
                taskRepository.countByCreatedById(userId),
                taskRepository.countByUserReference(userId)
        );
    }

    public record UserDeletionImpact(long ownedProjects, long projectMemberships,
                                     long assignedTasks, long createdTasks,
                                     long affectedTasks) {}

    @Transactional(readOnly = true)
    public PageResponse<UserWorkHistoryResponse> workHistory(Long userId, int page, int size) {
        findUser(userId); // Historical identities remain readable, including deleted accounts.
        return PageResponse.from(workHistory.findByUserIdOrderByEndedAtDescIdDesc(userId,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))), UserWorkHistoryResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserWorkTrackingEntry> workTracking(Long userId, UserWorkTrackingScope scope,
            com.enis.sectio.task.TaskStatus status, String query, int page, int size) {
        findUser(userId); // Active, inactive and deleted identities are all observable by ADMIN.
        return PageResponse.from(workTracking.find(userId, scope, status, query,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))), entry -> entry);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserManagementException(
                        "USER_NOT_FOUND",
                        "User was not found",
                        HttpStatus.NOT_FOUND
                ));
    }

    private User findManageableUser(Long userId) {
        User user = lockUser(userId);
        if (user.getDeletedAt() != null) {
            throw conflict("USER_DELETED", "Deleted accounts cannot be edited or reactivated");
        }
        return user;
    }

    private User lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserManagementException(
                        "USER_NOT_FOUND", "User was not found", HttpStatus.NOT_FOUND));
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 72) {
            throw new UserManagementException(
                    "PASSWORD_POLICY_VIOLATION",
                    "Password must contain between 12 and 72 characters",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserManagementException conflict(String code, String message) {
        return new UserManagementException(code, message, HttpStatus.CONFLICT);
    }
}
