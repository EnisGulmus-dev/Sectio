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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    List<AdminUserResponse> listUsers() {
        return adminUserService.listUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return adminUserService.createUser(
                request.name(), request.email(), request.temporaryPassword(), request.role(), request.jobTitle()
        );
    }

    @PatchMapping("/{userId}")
    AdminUserResponse updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        return adminUserService.updateUser(
                userId, request.name(), request.role(), request.jobTitle(), authentication.getName()
        );
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
            @Size(max = 255, message = "Email must contain at most 255 characters")
            String email,

            @NotBlank(message = "Temporary password is required")
            @Size(min = 12, max = 72,
                    message = "Temporary password must contain between 12 and 72 characters")
            String temporaryPassword,

            @NotNull(message = "Role is required")
            Role role,

            @NotNull(message = "Job title is required")
            JobTitle jobTitle
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must contain at most 100 characters")
            String name,

            @NotNull(message = "Role is required")
            Role role,

            @NotNull(message = "Job title is required")
            JobTitle jobTitle
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
