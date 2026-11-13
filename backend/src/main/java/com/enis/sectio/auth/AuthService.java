package com.enis.sectio.auth;

import com.enis.sectio.user.User;
import com.enis.sectio.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String email) {
        return CurrentUserResponse.from(findUser(email));
    }

    @Transactional
    public CurrentUserResponse changePassword(
            String email,
            String currentPassword,
            String newPassword) {
        if (newPassword == null
                || newPassword.length() < 12
                || newPassword.length() > 72) {
            throw new PasswordPolicyViolationException();
        }

        User user = findUser(email);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CurrentPasswordMismatchException();
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new PasswordReuseException();
        }

        user.changePasswordHash(passwordEncoder.encode(newPassword));
        return CurrentUserResponse.from(user);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
    }
}
