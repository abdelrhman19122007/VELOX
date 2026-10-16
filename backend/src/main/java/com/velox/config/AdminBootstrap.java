package com.velox.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.velox.auth.repository.UserRepository;
import com.velox.security.PasswordHasher;
import com.velox.security.Role;

/**
 * Creates the first admin from VELOX_ADMIN_EMAIL / VELOX_ADMIN_PASSWORD when there is none.
 * No default credentials exist in the code (OWASP A05, SRS NFR-SEC-06).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordHasher hasher,
                          @Value("${velox.bootstrap.admin-email:}") String email,
                          @Value("${velox.bootstrap.admin-password:}") String password) {
        this.users = users;
        this.hasher = hasher;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.anyAdmin()) {
            return;
        }
        if (email.isBlank() || password.length() < 12) {
            log.warn("No admin account yet: set VELOX_ADMIN_EMAIL and VELOX_ADMIN_PASSWORD (12+ characters) to create one.");
            return;
        }
        users.insert(email.trim().toLowerCase(), "VELOX Admin", null, Role.ADMIN, null, hasher.hash(password));
        log.info("Created the first admin account ({})", email);
    }
}
