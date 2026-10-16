package com.velox.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.velox.common.error.ApiException;
import com.velox.payment.repository.PromoRepository;
import com.velox.payment.repository.PromoRepository.Promo;
import com.velox.security.AuditLog;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/** Admin: list and create promo codes. */
@Service
public class PromoAdminService {

    private final PromoRepository promos;
    private final AuditLog audit;

    public PromoAdminService(PromoRepository promos, AuditLog audit) {
        this.promos = promos;
        this.audit = audit;
    }

    public List<Promo> list(AuthUser admin) {
        admin.require(Role.ADMIN);
        return promos.all();
    }

    public void create(AuthUser admin, Promo promo) {
        admin.require(Role.ADMIN);
        if (promos.find(promo.code()).isPresent()) {
            throw ApiException.conflict("PROMO_EXISTS", "This code exists");
        }
        promos.insert(promo);
        audit.record(admin, "PROMO_CREATE", "promo", promo.code(), promo.kind() + " " + promo.value());
    }
}
