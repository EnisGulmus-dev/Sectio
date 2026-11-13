package com.enis.sectio.security;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordChangeRequiredFilterTest {

    private UserRepository userRepository;
    private User admin;
    private PasswordChangeRequiredFilter filter;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        admin = new User(
                "Administrator",
                "admin@example.com",
                "password-hash",
                Role.ADMIN
        );
        filter = new PasswordChangeRequiredFilter(userRepository);

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "admin@example.com",
                        "password-hash",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksProtectedApiUntilPasswordIsChanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/projects"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("PASSWORD_CHANGE_REQUIRED");
    }

    @Test
    void allowsPasswordChangeEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/auth/change-password"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsProtectedApiAfterPasswordChange() throws Exception {
        admin.changePasswordHash("new-password-hash");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/projects"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsSessionWhenAccountBecomesInactive() throws Exception {
        admin.deactivate();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/projects"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = (MockHttpSession) request.getSession(true);

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ACCOUNT_UNAVAILABLE");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
        assertThat(session.isInvalid()).isTrue();
    }
}
