package com.enis.sectio.user.admin;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import com.enis.sectio.project.ProjectRepository;
import com.enis.sectio.project.ProjectMemberRepository;
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

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            ProjectRepository projectRepository,
                            ProjectMemberRepository projectMemberRepository,
                            TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
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
        if (user.isActive()) {
            throw conflict("ACTIVE_USER_DELETION_NOT_ALLOWED", "Deactivate the user before permanent deletion");
        }
        if (projectRepository.existsByOwnerId(userId)
                || projectMemberRepository.existsByUserId(userId)
                || taskRepository.existsByCreatedById(userId)
                || taskRepository.existsByAssigneeId(userId)) {
            throw conflict("USER_HAS_RELATED_DATA", "User with project or task history cannot be permanently deleted");
        }
        userRepository.delete(user);
    }

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
