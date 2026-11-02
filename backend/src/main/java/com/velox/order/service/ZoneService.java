package com.velox.order.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.velox.common.error.ApiException;
import com.velox.order.domain.DeliveryZone;
import com.velox.order.repository.ZoneRepository;
import com.velox.security.AuditLog;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/** Delivery zones of Samannoud and its villages (FR-ORD-04): public list, admin add / edit. */
@Service
public class ZoneService {

    private final ZoneRepository zones;
    private final AuditLog audit;

    public ZoneService(ZoneRepository zones, AuditLog audit) {
        this.zones = zones;
        this.audit = audit;
    }

    public List<DeliveryZone> active() {
        return zones.findActive();
    }

    public List<DeliveryZone> all(AuthUser admin) {
        admin.require(Role.ADMIN);
        return zones.findAll();
    }

    public void create(AuthUser admin, DeliveryZone zone) {
        admin.require(Role.ADMIN);
        if (zones.exists(zone.code())) {
            throw ApiException.conflict("ZONE_EXISTS", "A zone with this code exists");
        }
        zones.insert(zone);
        audit.record(admin, "ZONE_CREATE", "zone", zone.code(), zone.fee().toPlainString());
    }

    public void update(AuthUser admin, String code, DeliveryZone zone) {
        admin.require(Role.ADMIN);
        if (!zones.exists(code) || !code.equals(zone.code())) {
            throw ApiException.notFound("UNKNOWN_ZONE", "No such zone");
        }
        zones.update(zone);
        audit.record(admin, "ZONE_UPDATE", "zone", code, zone.fee().toPlainString());
    }
}
