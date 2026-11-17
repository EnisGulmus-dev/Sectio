package com.enis.sectio.user.admin;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.JobTitle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final com.enis.sectio.organization.OrganizationService organization;
    private final com.enis.sectio.user.ProfileRepository profiles;

    public AdminUserController(AdminUserService adminUserService, com.enis.sectio.organization.OrganizationService organization, com.enis.sectio.user.ProfileRepository profiles) {
        this.adminUserService = adminUserService;
        this.organization = organization; this.profiles = profiles;
    }

    @GetMapping
    List<AdminUserResponse> listUsers(@RequestParam(defaultValue = "false") boolean deleted) {
        return adminUserService.listUsers(deleted);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.transaction.annotation.Transactional
    public AdminUserResponse createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof com.enis.sectio.security.SessionUserDetails principal)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        AdminUserResponse created = adminUserService.createUser(
                request.name(), request.email(), request.temporaryPassword(), request.role(), request.jobTitle()
        );
        profiles.save(new com.enis.sectio.user.Profile(created.id()));
        if (request.managerId() != null || (request.teamIds() != null && !request.teamIds().isEmpty())) {
            organization.assign(principal.getUserId(), created.id(), request.managerId(), request.teamIds());
        }
        return created;
    }

    @PatchMapping("/{userId}")
    @org.springframework.transaction.annotation.Transactional
    public AdminUserResponse updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof com.enis.sectio.security.SessionUserDetails principal)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (request.managerId() != null && request.teamIds() == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Takım listesi gerekli.");
        AdminUserResponse updated = adminUserService.updateUser(
                userId, request.name(), request.email(), request.role(), request.jobTitle(), authentication.getName()
        );
        if (request.teamIds() != null) organization.assign(principal.getUserId(), userId, request.managerId(), request.teamIds());
        return updated;
    }

    @PatchMapping("/{userId}/status")
    AdminUserResponse changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeStatusRequest request,
            Authentication authentication) {
        return adminUserService.changeStatus(
                userId, request.active(), authentication.getName()
        );
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteUser(@PathVariable Long userId, Authentication authentication) {
        adminUserService.deleteUser(userId, authentication.getName());
    }

    @GetMapping("/{userId}/deletion-impact")
    AdminUserService.UserDeletionImpact deletionImpact(@PathVariable Long userId) {
        return adminUserService.deletionImpact(userId);
    }

    @PostMapping("/{userId}/restore")
    AdminUserResponse restoreUser(@PathVariable Long userId, @Valid @RequestBody ResetPasswordRequest request) {
        return adminUserService.restoreUser(userId, request.temporaryPassword());
    }

    @DeleteMapping("/{userId}/purge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void purgeUser(@PathVariable Long userId, Authentication authentication) {
        adminUserService.purgeUser(userId, authentication.getName());
    }

    @GetMapping("/{userId}/work-history")
    com.enis.sectio.common.web.PageResponse<UserWorkHistoryResponse> workHistory(
            @PathVariable Long userId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminUserService.workHistory(userId, page, size);
    }

    @GetMapping("/{userId}/work-tracking")
    com.enis.sectio.common.web.PageResponse<UserWorkTrackingEntry> workTracking(
            @PathVariable Long userId, @RequestParam(defaultValue = "ALL") UserWorkTrackingScope scope,
            @RequestParam(required = false) com.enis.sectio.task.TaskStatus status,
            @RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminUserService.workTracking(userId, scope, status, query, page, size);
    }

    @PostMapping("/{userId}/reset-password")
    AdminUserResponse resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ResetPasswordRequest request,
            Authentication authentication) {
        return adminUserService.resetPassword(
                userId, request.temporaryPassword(), authentication.getName()
        );
    }

    public record CreateUserRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must contain at most 100 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @jakarta.validation.constraints.Pattern(regexp = "^[\\x21-\\x7E]+$", message = "E-posta yalnızca ASCII karakterler içermelidir")
            @Size(max = 255, message = "Email must contain at most 255 characters")
            String email,

            @NotBlank(message = "Temporary password is required")
            @Size(min = 12, max = 72,
                    message = "Temporary password must contain between 12 and 72 characters")
            String temporaryPassword,

            @NotNull(message = "Role is required")
            Role role,

            @NotNull(message = "Job title is required")
            JobTitle jobTitle,

            @jakarta.validation.constraints.Positive Long managerId,
            @Size(max = 50) List<@NotNull @jakarta.validation.constraints.Positive Long> teamIds
    ) {
        public CreateUserRequest {
            if (email != null) email = email.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record UpdateUserRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must contain at most 100 characters")
            String name,

            @Email(message = "E-posta geçerli olmalıdır")
            @Size(max = 255)
            @jakarta.validation.constraints.Pattern(regexp = "^[\\x21-\\x7E]+$", message = "E-posta yalnızca ASCII karakterler içermelidir")
            String email,

            @NotNull(message = "Role is required")
            Role role,

            @NotNull(message = "Job title is required")
            JobTitle jobTitle,
            @jakarta.validation.constraints.Positive Long managerId,
            @Size(max=50) List<@NotNull @jakarta.validation.constraints.Positive Long> teamIds
    ) {
    }

    public record ChangeStatusRequest(@NotNull(message = "Active status is required") Boolean active) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Temporary password is required")
            @Size(min = 12, max = 72,
                    message = "Temporary password must contain between 12 and 72 characters")
            String temporaryPassword
    ) {
    }
}
