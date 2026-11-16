package com.enis.sectio.security;

import com.enis.sectio.user.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/** Immutable identity/revocation snapshot captured when credentials are verified. */
public final class SessionUserDetails extends org.springframework.security.core.userdetails.User {
    private static final long serialVersionUID = 1L;
    private final Long userId;
    private final long sessionVersion;

    public SessionUserDetails(User user) {
        super(user.getEmail(), user.getPasswordHash(), user.isActive() && user.getDeletedAt() == null, true, true, true,
                List.of(new SimpleGrantedAuthority(user.getRole().asAuthority())));
        this.userId = user.getId();
        this.sessionVersion = user.getSessionVersion();
    }

    public Long getUserId() { return userId; }
    public long getSessionVersion() { return sessionVersion; }
}
