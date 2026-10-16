package com.velox.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Records sensitive actions — who, what, on which record, from where (OWASP A09, SRS FR-AUTH-09).
 * Never put passwords, tokens or payment codes in {@code details}.
 */
@Component
public class AuditLog {

    private final JdbcTemplate db;

    public AuditLog(JdbcTemplate db) {
        this.db = db;
    }

    public void record(AuthUser actor, String action, String entity, String entityId, String details) {
        db.update("INSERT INTO audit_log (actor_id, action, entity, entity_id, ip, details) VALUES (?,?,?,?,?,?)",
                actor == null ? null : actor.id(), action, entity, entityId, RequestContext.clientIp(), details);
    }
}
