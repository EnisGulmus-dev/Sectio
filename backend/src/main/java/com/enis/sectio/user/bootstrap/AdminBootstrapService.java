package com.enis.sectio.user.bootstrap;

import com.enis.sectio.user.Role;
import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AdminBootstrapService {

    private static final Logger log =
            LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    public AdminBootstrapService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AdminBootstrapProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional
    public void bootstrapAdmin() {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin bootstrap skipped because an admin already exists");
            return;
        }

        String name = requireNonBlank(properties.getName(), "ADMIN_BOOTSTRAP_NAME");
        String email = normalizeEmail(properties.getEmail());
        String rawPassword = requirePassword(properties.getPassword());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException(
                    "Admin bootstrap email is already used by another user"
            );
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User admin = new User(
                name,
                email,
                passwordHash,
                Role.ADMIN
        );

        userRepository.save(admin);

        log.info("Initial admin account created");
    }

    private String normalizeEmail(String email) {
        String requiredEmail =
                requireNonBlank(email, "ADMIN_BOOTSTRAP_EMAIL");

        return requiredEmail.toLowerCase(Locale.ROOT);
    }

    private String requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    propertyName + " must be configured when no admin exists"
            );
        }

        return value.trim();
    }

    private String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_PASSWORD must be configured when no admin exists"
            );
        }

        return password;
    }
}