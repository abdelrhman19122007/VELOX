package com.velox.auth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.velox.auth.domain.Address;
import com.velox.auth.dto.AuthDtos.AddressRequest;
import com.velox.auth.dto.AuthDtos.PasswordChangeRequest;
import com.velox.auth.dto.AuthDtos.SessionResponse;
import com.velox.auth.service.AccountService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** The signed-in user's own account: password and saved addresses. */
@RestController
@RequestMapping("/api/me")
public class AccountController {

    private final AccountService account;

    public AccountController(AccountService account) {
        this.account = account;
    }

    @PostMapping("/password")
    public SessionResponse changePassword(@CurrentUser AuthUser me, @Valid @RequestBody PasswordChangeRequest req) {
        return account.changePassword(me, req.current(), req.password());
    }

    @GetMapping("/addresses")
    public List<Address> addresses(@CurrentUser AuthUser me) {
        return account.addresses(me);
    }

    @PostMapping("/addresses")
    public List<Address> addAddress(@CurrentUser AuthUser me, @Valid @RequestBody AddressRequest req) {
        return account.addAddress(me, req.toAddress());
    }

    @DeleteMapping("/addresses/{id}")
    public List<Address> deleteAddress(@CurrentUser AuthUser me, @PathVariable long id) {
        return account.deleteAddress(me, id);
    }
}
