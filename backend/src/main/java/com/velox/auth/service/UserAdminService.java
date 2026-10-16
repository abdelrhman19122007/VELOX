package com.velox.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.auth.domain.User;
import com.velox.auth.repository.UserRepository;
import com.velox.common.error.ApiException;
import com.velox.security.AuditLog;
import com.velox.security.AuthUser;
import com.velox.security.LoginThrottle;
import com.velox.security.PasswordHasher;
import com.velox.security.Role;

/** Admin actions on accounts: list, block / unblock, temporary password (FR-AUTH-05, -08, -09). All audited. */
@Service
public class UserAdminService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final LoginThrottle throttle;
    private final AuditLog audit;

    public UserAdminService(UserRepository users, PasswordHasher hasher, LoginThrottle throttle, AuditLog audit) {
        this.users = users;
        this.hasher = hasher;
        this.throttle = throttle;
        this.audit = audit;
    }

    public List<User> list(AuthUser admin, Role role) {
        admin.require(Role.ADMIN);
        return users.findByRole(role, 300);
    }

    @Transactional
    public void setBlocked(AuthUser admin, String publicId, boolean blocked) {
        admin.require(Role.ADMIN);
        if (admin.publicId().equals(publicId)) {
            throw ApiException.badRequest("INVALID_REQUEST", "You can't block yourself");
        }
        if (!users.setBlocked(publicId, blocked)) {
            throw ApiException.notFound("UNKNOWN_USER", "No such account");
        }
        audit.record(admin, blocked ? "USER_BLOCK" : "USER_UNBLOCK", "user", publicId, null);
    }

    /** "Forgot password": a one-time temporary password the admin passes to the owner. Ends their sessions. */
    @Transactional
    public String resetPassword(AuthUser admin, String publicId) {
        admin.require(Role.ADMIN);
        if (admin.publicId().equals(publicId)) {
            throw ApiException.badRequest("INVALID_REQUEST", "Change your own password from settings");
        }
        User user = users.findByPublicId(publicId).orElseThrow(() -> ApiException.notFound("UNKNOWN_USER", "No such account"));
        String temp = hasher.randomReadable(10);
        users.updatePassword(user.id(), hasher.hash(temp));
        throttle.clear(user.email());
        audit.record(admin, "PASSWORD_RESET", "user", publicId, null);
        return temp;
    }
}
