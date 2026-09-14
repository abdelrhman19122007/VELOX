package com.app.controller;

import com.app.dao.WebOrderDAO;
import com.app.service.AuthTokenStore;
import com.app.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent notifications (self only).
 *
 * GET /api/notifications?userId=&limit=
 * PUT /api/notifications/read-all {userId}
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications = new NotificationService();
    private final WebOrderDAO lookup = new WebOrderDAO();

    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId) {
        Integer id = selfIdOrNull(authorization, userId);
        if (id == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("unread", new NotificationService().unreadCount(id));
        return ResponseEntity.ok(out);
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId,
            @RequestParam(defaultValue = "30") int limit) {
        Integer id = selfIdOrNull(authorization, userId);
        if (id == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        return ResponseEntity.ok(notifications.list(id, limit));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> readAll(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> body) {
        String userId = body != null && body.get("userId") != null
                ? String.valueOf(body.get("userId"))
                : (body != null && body.get("user_id") != null ? String.valueOf(body.get("user_id")) : null);
        Integer id = selfIdOrNull(authorization, userId);
        if (id == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("marked", notifications.markAllRead(id));
        return ResponseEntity.ok(out);
    }

    private Integer selfIdOrNull(String authorization, String userId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null || userId == null) {
            return null;
        }
        if (email.equalsIgnoreCase(userId.trim())) {
            return lookup.findUserId(email);
        }
        Integer ownId = lookup.findUserId(email);
        if (ownId != null && String.valueOf(ownId).equals(userId.trim())) {
            return ownId;
        }
        return null;
    }
}
