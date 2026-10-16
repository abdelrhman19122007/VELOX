package com.velox.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.velox.auth.dto.AuthDtos.LoginRequest;
import com.velox.auth.dto.AuthDtos.RegisterRequest;
import com.velox.auth.dto.AuthDtos.SessionResponse;
import com.velox.auth.dto.AuthDtos.UserResponse;
import com.velox.auth.service.AuthService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;
import com.velox.security.RequestContext;

import jakarta.validation.Valid;

/** Sign-up / sign-in / sign-out. Controllers only translate HTTP ↔ service calls. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public SessionResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.password(), RequestContext.clientIp());
    }

    @GetMapping("/me")
    public UserResponse me(@CurrentUser AuthUser user) {
        return auth.me(user);
    }

    @PostMapping("/logout-everywhere")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutEverywhere(@CurrentUser AuthUser user) {
        auth.logoutEverywhere(user);
    }
}
