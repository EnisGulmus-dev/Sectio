package com.enis.sectio.user.bootstrap;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AdminBootstrapProperties properties;
    private AdminBootstrapService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        properties = validProperties();
        service = new AdminBootstrapService(
                userRepository,
                passwordEncoder,
                properties
        );
    }

    @Test
    void createsAnActiveAdminWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("temporary-password"))
                .thenReturn("hashed-password");

        service.bootstrapAdmin();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User admin = captor.getValue();
        assertThat(admin.getName()).isEqualTo("Initial Administrator");
        assertThat(admin.getEmail()).isEqualTo("admin@example.com");
        assertThat(admin.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.isMustChangePassword()).isTrue();
    }

    @Test
    void doesNotCreateASecondAdminWhenBootstrapRunsAgain() {
        when(userRepository.existsByRole(Role.ADMIN))
                .thenReturn(false, true);
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("temporary-password"))
                .thenReturn("hashed-password");

        service.bootstrapAdmin();
        service.bootstrapAdmin();

        verify(userRepository, times(1)).save(ArgumentMatchers.any());
        verify(passwordEncoder, times(1)).encode("temporary-password");
    }

    @ParameterizedTest(name = "rejects missing bootstrap property: {0}")
    @MethodSource("missingBootstrapProperties")
    void rejectsMissingBootstrapValuesWhenNoAdminExists(
            String expectedProperty,
            Consumer<AdminBootstrapProperties> removeProperty) {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        removeProperty.accept(properties);

        assertThatThrownBy(service::bootstrapAdmin)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedProperty);

        verify(userRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void rejectsWeakBootstrapPassword() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        properties.setPassword("short");

        assertThatThrownBy(service::bootstrapAdmin)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12 characters");
    }

    private static Stream<Arguments>
    missingBootstrapProperties() {
        return Stream.of(
                Arguments.of(
                        "ADMIN_BOOTSTRAP_NAME",
                        (Consumer<AdminBootstrapProperties>) value -> value.setName("")
                ),
                Arguments.of(
                        "ADMIN_BOOTSTRAP_EMAIL",
                        (Consumer<AdminBootstrapProperties>) value -> value.setEmail("")
                ),
                Arguments.of(
                        "ADMIN_BOOTSTRAP_PASSWORD",
                        (Consumer<AdminBootstrapProperties>) value -> value.setPassword("")
                )
        );
    }

    private static AdminBootstrapProperties validProperties() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setName("Initial Administrator");
        properties.setEmail(" ADMIN@EXAMPLE.COM ");
        properties.setPassword("temporary-password");
        return properties;
    }
}
