package com.velox.notification;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The notification inbox, push device registration and the public push settings for the apps. */
@RestController
public class NotificationController {

    public record DeviceRequest(@NotBlank @Size(max = 400) String token, String platform, String lang) {
    }

    private final NotificationService notifications;
    private final PushService push;
    private final ObjectMapper json;

    public NotificationController(NotificationService notifications, PushService push, ObjectMapper json) {
        this.notifications = notifications;
        this.push = push;
        this.json = json;
    }

    @GetMapping("/api/me/notifications")
    public List<NotificationService.Notification> inbox(@CurrentUser AuthUser me) {
        return notifications.inbox(me);
    }

    @PostMapping("/api/me/notifications/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@CurrentUser AuthUser me) {
        notifications.markAllRead(me);
    }

    @PostMapping("/api/me/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addDevice(@CurrentUser AuthUser me, @Valid @RequestBody DeviceRequest req) {
        push.register(me.id(), req.token().trim(), req.platform(), req.lang());
    }

    @PostMapping("/api/me/devices/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDevice(@CurrentUser AuthUser me, @Valid @RequestBody DeviceRequest req) {
        push.unregister(me.id(), req.token().trim());
    }

    /** Public Firebase settings so the apps can subscribe ({} when push is off). */
    @GetMapping("/api/push-config")
    public Map<String, String> config() {
        Map<String, String> c = push.clientConfig();
        return c == null ? Map.of() : c;
    }

    /**
     * The browser's push worker (Firebase looks for it at the site root). Built here so it carries the same
     * settings as /api/push-config; it shows the notification when the page is closed.
     */
    @GetMapping(value = "/firebase-messaging-sw.js", produces = "text/javascript")
    public String pushWorker() throws Exception {
        Map<String, String> c = push.clientConfig();
        String config = c == null ? "null" : json.writeValueAsString(c);
        return "importScripts('https://www.gstatic.com/firebasejs/10.12.5/firebase-app-compat.js');\n"
                + "importScripts('https://www.gstatic.com/firebasejs/10.12.5/firebase-messaging-compat.js');\n"
                + "const config = " + config + ";\n"
                + "if (config) { firebase.initializeApp(config); firebase.messaging(); }\n"
                + "self.addEventListener('notificationclick', (e) => { e.notification.close();"
                + " e.waitUntil(clients.openWindow('/app/')); });\n";
    }
}
