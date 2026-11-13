package com.enis.sectio.auth;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private User admin;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = new AuthService(userRepository, passwordEncoder);
        admin = new User(
                "Administrator",
                "admin@example.com",
                "old-hash",
                Role.ADMIN
        );
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
    }

    @Test
    void changesPasswordAndClearsMandatoryChangeFlag() {
        when(passwordEncoder.matches("old-password", "old-hash"))
                .thenReturn(true);
        when(passwordEncoder.matches("new-secure-password", "old-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("new-secure-password"))
                .thenReturn("new-hash");

        CurrentUserResponse response = authService.changePassword(
                "admin@example.com",
                "old-password",
                "new-secure-password"
        );

        assertThat(admin.getPasswordHash()).isEqualTo("new-hash");
        assertThat(admin.isMustChangePassword()).isFalse();
        assertThat(response.mustChangePassword()).isFalse();
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        when(passwordEncoder.matches("wrong-password", "old-hash"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(
                "admin@example.com",
                "wrong-password",
                "new-secure-password"
        )).isInstanceOf(CurrentPasswordMismatchException.class);

        verify(passwordEncoder, never()).encode("new-secure-password");
        assertThat(admin.isMustChangePassword()).isTrue();
    }

    @Test
    void rejectsReusingCurrentPassword() {
        when(passwordEncoder.matches("old-password", "old-hash"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(
                "admin@example.com",
                "old-password",
                "old-password"
        )).isInstanceOf(PasswordReuseException.class);

        verify(passwordEncoder, never()).encode("old-password");
    }

    @Test
    void rejectsPasswordOutsidePolicy() {
        assertThatThrownBy(() -> authService.changePassword(
                "admin@example.com",
                "old-password",
                "short"
        )).isInstanceOf(PasswordPolicyViolationException.class);

        verify(passwordEncoder, never()).matches(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
