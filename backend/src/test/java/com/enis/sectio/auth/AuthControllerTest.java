package com.enis.sectio.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulLoginRotatesSessionAndPersistsSecurityContext() {
        AuthenticationManager authenticationManager =
                mock(AuthenticationManager.class);
        SecurityContextRepository securityContextRepository =
                mock(SecurityContextRepository.class);
        SessionAuthenticationStrategy sessionAuthenticationStrategy =
                mock(SessionAuthenticationStrategy.class);
        AuthService authService = mock(AuthService.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@example.com");
        when(authenticationManager.authenticate(
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(authentication);

        CurrentUserResponse expected = new CurrentUserResponse(
                1L,
                "Administrator",
                "admin@example.com",
                com.enis.sectio.user.Role.ADMIN,
                true
        );
        when(authService.currentUser("admin@example.com"))
                .thenReturn(expected);

        AuthController controller = new AuthController(
                authenticationManager,
                securityContextRepository,
                sessionAuthenticationStrategy,
                authService
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CurrentUserResponse actual = controller.login(
                new LoginRequest("admin@example.com", "password"),
                request,
                response
        );

        verify(sessionAuthenticationStrategy).onAuthentication(
                authentication,
                request,
                response
        );

        ArgumentCaptor<SecurityContext> contextCaptor =
                ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(
                contextCaptor.capture(),
                org.mockito.ArgumentMatchers.<HttpServletRequest>any(),
                org.mockito.ArgumentMatchers.<HttpServletResponse>any()
        );
        assertThat(contextCaptor.getValue().getAuthentication())
                .isSameAs(authentication);
        assertThat(actual).isEqualTo(expected);
    }
}
