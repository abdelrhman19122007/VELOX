package com.velox.auth.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.auth.domain.User;
import com.velox.auth.dto.AuthDtos.RegisterRequest;
import com.velox.auth.dto.AuthDtos.SessionResponse;
import com.velox.auth.dto.AuthDtos.UserResponse;
import com.velox.auth.repository.UserRepository;
import com.velox.common.error.ApiException;
import com.velox.security.AuthUser;
import com.velox.security.LoginThrottle;
import com.velox.security.PasswordHasher;
import com.velox.security.Role;
import com.velox.security.TokenService;

/** Sign-up, sign-in and sign-out (SRS FR-AUTH-01, -02, -03, -06, -08). */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenService tokens;
    private final LoginThrottle throttle;

    public AuthService(UserRepository users, PasswordHasher hasher, TokenService tokens, LoginThrottle throttle) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.throttle = throttle;
    }

    /** Customers and drivers sign up themselves; store and admin accounts are made by an admin. */
    @Transactional
    public SessionResponse register(RegisterRequest req) {
        Role role = req.role() == null ? Role.CUSTOMER : req.role();
        if (!role.canSelfRegister()) {
            throw ApiException.badRequest("INVALID_ROLE", "Only customers and drivers can sign up");
        }
        String email = normalize(req.email());
        if (users.emailExists(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "This email already has an account");
        }
        User user = users.insert(email, req.name().trim(), req.phone().trim(), role, null, hasher.hash(req.password()));
        return session(user);
    }

    /**
     * The same answer whether the email is unknown or the password is wrong (no account enumeration);
     * every try is recorded and too many failures lock the email / network for a while.
     */
    public SessionResponse login(String email, String password, String ip) {
        String e = normalize(email);
        throttle.check(e, ip);
        User user = users.findByEmail(e).orElse(null);
        boolean ok = user != null && hasher.matches(password, user.passwordHash());
        throttle.record(e, ip, ok);
        if (!ok) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_LOGIN", "Wrong email or password");
        }
        if (user.blocked()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "BLOCKED", "This account is blocked");
        }
        return session(user);
    }

    public UserResponse me(AuthUser user) {
        return users.findByPublicId(user.publicId()).map(UserResponse::of).orElseThrow(ApiException::unauthorized);
    }

    /** Ends every session of the account on every device. */
    public void logoutEverywhere(AuthUser user) {
        users.bumpTokenVersion(user.id());
    }

    private SessionResponse session(User user) {
        return new SessionResponse(tokens.issue(user.publicId(), user.tokenVersion()), UserResponse.of(user));
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
