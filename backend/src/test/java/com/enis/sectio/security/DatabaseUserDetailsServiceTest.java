package com.enis.sectio.security;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseUserDetailsServiceTest {

    @Test
    void mapsAdminRoleToSpringSecurityAuthority() {
        UserRepository repository = mock(UserRepository.class);
        User admin = new User(
                "Administrator",
                "admin@example.com",
                "hashed-password",
                Role.ADMIN
        );
        when(repository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));

        UserDetails details = new DatabaseUserDetailsService(repository)
                .loadUserByUsername(" ADMIN@EXAMPLE.COM ");

        assertThat(details.getUsername()).isEqualTo("admin@example.com");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void rejectsUnknownUserWithoutLeakingTheEmail() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DatabaseUserDetailsService(repository)
                .loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
