package com.velox.auth.dto;

import com.velox.auth.domain.Address;
import com.velox.auth.domain.User;
import com.velox.security.Role;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What the auth endpoints receive and return. Validation here is the first line; services re-check rules. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public static final String EG_MOBILE = "^01[0125]\\d{8}$";

    public record RegisterRequest(@NotBlank @Size(min = 2, max = 60) String name,
                                  @NotBlank @Email @Size(max = 120) String email,
                                  @NotBlank @Pattern(regexp = EG_MOBILE, message = "Egyptian mobile number") String phone,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  Role role) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record PasswordChangeRequest(@NotBlank String current, @NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record AddressRequest(@NotBlank @Size(max = 30) String label,
                                 @NotBlank @Size(max = 30) String zone,
                                 @NotBlank @Size(min = 5, max = 300) String address,
                                 @Size(max = 150) String landmark,
                                 @NotBlank @Pattern(regexp = EG_MOBILE, message = "Egyptian mobile number") String phone,
                                 @DecimalMin("-90") @DecimalMax("90") Double lat,
                                 @DecimalMin("-180") @DecimalMax("180") Double lng) {

        public Address toAddress() {
            return new Address(0, label.trim(), zone.trim(), address.trim(), landmark == null ? null : landmark.trim(),
                    phone.trim(), lat, lng);
        }
    }

    public record BlockRequest(@NotNull Boolean blocked) {
    }

    /** The public view of an account: no internal id, no password hash, no token version. */
    public record UserResponse(String id, String email, String name, String phone, Role role, String storeId) {
        public static UserResponse of(User u) {
            return new UserResponse(u.publicId(), u.email(), u.name(), u.phone(), u.role(), u.storeId());
        }
    }

    public record SessionResponse(String token, UserResponse user) {
    }

    public record AdminUserResponse(String id, String email, String name, String phone, Role role, String storeId,
                                    boolean blocked, String createdAt) {
        public static AdminUserResponse of(User u) {
            return new AdminUserResponse(u.publicId(), u.email(), u.name(), u.phone(), u.role(), u.storeId(), u.blocked(),
                    u.createdAt().toString());
        }
    }

    public record TempPasswordResponse(String password) {
    }
}
