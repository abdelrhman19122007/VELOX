package com.velox.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.auth.domain.Address;
import com.velox.auth.domain.User;
import com.velox.auth.dto.AuthDtos.SessionResponse;
import com.velox.auth.dto.AuthDtos.UserResponse;
import com.velox.auth.repository.AddressRepository;
import com.velox.auth.repository.UserRepository;
import com.velox.common.error.ApiException;
import com.velox.security.AuthUser;
import com.velox.security.PasswordHasher;
import com.velox.security.TokenService;

/** What a signed-in user manages about their own account: password and saved addresses (FR-AUTH-04, -07). */
@Service
public class AccountService {

    static final int MAX_ADDRESSES = 10;

    private final UserRepository users;
    private final AddressRepository addresses;
    private final PasswordHasher hasher;
    private final TokenService tokens;

    public AccountService(UserRepository users, AddressRepository addresses, PasswordHasher hasher, TokenService tokens) {
        this.users = users;
        this.addresses = addresses;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    /**
     * Needs the current password. Changing it ends every other session; the caller gets a fresh token
     * so this device stays signed in.
     */
    @Transactional
    public SessionResponse changePassword(AuthUser me, String current, String next) {
        User user = users.findByPublicId(me.publicId()).orElseThrow(ApiException::unauthorized);
        if (!hasher.matches(current, user.passwordHash())) {
            throw ApiException.badRequest("WRONG_PASSWORD", "The current password is wrong");
        }
        users.updatePassword(user.id(), hasher.hash(next));
        User updated = users.findByPublicId(me.publicId()).orElseThrow();
        return new SessionResponse(tokens.issue(updated.publicId(), updated.tokenVersion()), UserResponse.of(updated));
    }

    public List<Address> addresses(AuthUser me) {
        return addresses.findByUser(me.id());
    }

    @Transactional
    public List<Address> addAddress(AuthUser me, Address address) {
        if (!addresses.zoneExists(address.zone())) {
            throw ApiException.badRequest("UNKNOWN_ZONE", "We don't deliver to that area yet");
        }
        if (addresses.countByUser(me.id()) >= MAX_ADDRESSES) {
            throw ApiException.conflict("TOO_MANY_ADDRESSES", "Up to " + MAX_ADDRESSES + " addresses");
        }
        addresses.insert(me.id(), address);
        return addresses.findByUser(me.id());
    }

    public List<Address> deleteAddress(AuthUser me, long addressId) {
        addresses.delete(me.id(), addressId);
        return addresses.findByUser(me.id());
    }
}
