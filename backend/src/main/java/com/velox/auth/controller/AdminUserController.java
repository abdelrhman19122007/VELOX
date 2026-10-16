package com.velox.auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.velox.auth.dto.AuthDtos.AdminUserResponse;
import com.velox.auth.dto.AuthDtos.BlockRequest;
import com.velox.auth.dto.AuthDtos.TempPasswordResponse;
import com.velox.auth.service.UserAdminService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;
import com.velox.security.Role;

import jakarta.validation.Valid;

/** Admin: accounts. The role check happens in the service, so no route can skip it. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdminService admin;

    public AdminUserController(UserAdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    public List<AdminUserResponse> list(@CurrentUser AuthUser me, @RequestParam(required = false) Role role) {
        return admin.list(me, role).stream().map(AdminUserResponse::of).toList();
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@CurrentUser AuthUser me, @PathVariable String id, @Valid @RequestBody BlockRequest req) {
        admin.setBlocked(me, id, req.blocked());
    }

    @PostMapping("/{id}/reset-password")
    public TempPasswordResponse resetPassword(@CurrentUser AuthUser me, @PathVariable String id) {
        return new TempPasswordResponse(admin.resetPassword(me, id));
    }
}
