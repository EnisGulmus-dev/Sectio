package com.enis.sectio.user.admin;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import com.enis.sectio.project.ProjectRepository;
import com.enis.sectio.project.ProjectMemberRepository;
import com.enis.sectio.project.ProjectTeamRepository;
import com.enis.sectio.task.TaskRepository;
import org.springframework.data.domain.Sort;
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

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            ProjectRepository projectRepository,
                            ProjectMemberRepository projectMemberRepository,
                            TaskRepository taskRepository,
                            ProjectTeamRepository projectTeamRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
        this.projectTeamRepository = projectTeamRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(String name, String email, String password, Role role, JobTitle jobTitle) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw conflict("EMAIL_ALREADY_EXISTS", "A user with this email already exists");
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
        User user = findUser(userId);
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
        User user = findUser(userId);
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
        User user = findUser(userId);
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
        User user = findUser(userId);
        if (user.getEmail().equals(normalizeEmail(currentAdminEmail))) {
            throw conflict("SELF_DELETION_NOT_ALLOWED", "You cannot delete your own account");
        }
        if (projectRepository.existsByOwnerId(userId)) {
            throw conflict("PROJECT_OWNER_DELETION_NOT_ALLOWED", "Transfer or delete projects owned by this user first");
        }
        taskRepository.deleteByCreatedById(userId);
        taskRepository.deleteAssigneeReferencesByUserId(userId);
        taskRepository.deleteTasksWithoutAssignees();
        projectTeamRepository.deleteMembershipsByUserId(userId);
        projectMemberRepository.deleteByUserId(userId);
        userRepository.delete(user);
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserManagementException(
                        "USER_NOT_FOUND",
                        "User was not found",
                        HttpStatus.NOT_FOUND
                ));
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
