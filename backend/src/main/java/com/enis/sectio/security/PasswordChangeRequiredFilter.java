package com.enis.sectio.security;

import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/";
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/auth/csrf",
            "/api/v1/auth/login",
            "/api/v1/auth/me",
            "/api/v1/auth/change-password",
            "/api/v1/auth/logout"
    );

    private final UserRepository userRepository;

    public PasswordChangeRequiredFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());

        SessionUserDetails principal = authentication.getPrincipal() instanceof SessionUserDetails details
                ? details : null;
        Optional<User> user = principal == null || principal.getUserId() == null
                ? Optional.empty() : userRepository.findById(principal.getUserId());

        if (user.isEmpty() || !user.get().isActive() || user.get().getDeletedAt() != null) {
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"ACCOUNT_DISABLED\","
                            + "\"message\":\"Hesabınız pasife alındı, yöneticinizle görüşün\","
                            + "\"fieldErrors\":{}}"
            );
            return;
        }

        if (principal.getSessionVersion() != user.get().getSessionVersion()) {
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"SESSION_REVOKED\","
                    + "\"message\":\"Oturumunuz sonlandırıldı. Yeniden giriş yapın.\",\"fieldErrors\":{}}");
            return;
        }

        if (ALLOWED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean passwordChangeRequired = user.get().isMustChangePassword();

        if (!passwordChangeRequired) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"PASSWORD_CHANGE_REQUIRED\","
                        + "\"message\":\"Password must be changed before continuing\","
                        + "\"fieldErrors\":{}}"
        );
    }
}
