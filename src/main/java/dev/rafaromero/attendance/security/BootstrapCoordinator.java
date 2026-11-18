package dev.rafaromero.attendance.security;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first coordinator account on an empty database from
 * {@code APP_BOOTSTRAP_USERNAME} / {@code APP_BOOTSTRAP_PASSWORD}.
 * No credentials live in the code or the repository, and nothing happens once any account exists.
 */
@Component
class BootstrapCoordinator implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;
    private static final Logger log = LoggerFactory.getLogger(BootstrapCoordinator.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String username;
    private final String password;

    BootstrapCoordinator(AppUserRepository users, PasswordEncoder passwordEncoder, Clock clock,
            @Value("${app.bootstrap.username:}") String username,
            @Value("${app.bootstrap.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            log.warn("No staff accounts exist. Set APP_BOOTSTRAP_USERNAME and APP_BOOTSTRAP_PASSWORD to create the first coordinator.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("APP_BOOTSTRAP_PASSWORD must have at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        users.save(new AppUser(username.strip(), passwordEncoder.encode(password), AppUser.Role.COORDINATOR,
                Instant.now(clock)));
        log.info("Created the first coordinator account '{}'", username.strip());
    }
}
