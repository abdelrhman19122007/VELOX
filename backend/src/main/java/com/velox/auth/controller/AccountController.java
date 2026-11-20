package com.velox.auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.velox.auth.domain.Address;
import com.velox.auth.dto.AuthDtos;
import com.velox.auth.dto.AuthDtos.AddressRequest;
import com.velox.auth.dto.AuthDtos.UserResponse;
import com.velox.auth.dto.AuthDtos.PasswordChangeRequest;
import com.velox.auth.dto.AuthDtos.SessionResponse;
import com.velox.auth.service.AccountService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    /** Deletes the signed-in account after checking its password (POST, so the password travels in the body). */
    @PostMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@CurrentUser AuthUser me, @RequestBody DeleteAccountRequest req) {
        account.deleteAccount(me, req.password());
    }

    public record DeleteAccountRequest(String password) {
    }

    /** Edit my name and phone; the email stays (it is the sign-in). Returns the updated account. */
    @PostMapping("/profile")
    public UserResponse updateProfile(@CurrentUser AuthUser me, @Valid @RequestBody ProfileRequest req) {
        return account.updateProfile(me, req.name().trim(), req.phone().trim());
    }

    public record ProfileRequest(@NotBlank @Size(min = 2, max = 60) @Pattern(regexp = AuthDtos.PLAIN_NAME) String name,
                                 @NotBlank @Pattern(regexp = AuthDtos.EG_MOBILE, message = "Egyptian mobile number") String phone) {
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
