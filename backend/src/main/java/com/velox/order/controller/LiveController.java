package com.velox.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.velox.common.error.ApiException;
import com.velox.order.live.EventHub;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;
import com.velox.security.CurrentUserResolver;

/**
 * Live updates for the signed-in account: a Server-Sent Events stream, or long polling for networks that hold
 * streams back. The token always travels in the Authorization header — never in the URL, where it would end up
 * in proxy and server logs.
 */
@RestController
public class LiveController {

    private final EventHub hub;
    private final CurrentUserResolver auth;

    public LiveController(EventHub hub, CurrentUserResolver auth) {
        this.hub = hub;
        this.auth = auth;
    }

    @GetMapping(value = "/api/events", produces = "text/event-stream")
    public SseEmitter events(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser user = auth.authenticate(authorization);
        if (user == null) {
            throw ApiException.unauthorized();
        }
        return hub.subscribe(user.id());
    }

    @GetMapping("/api/events/poll")
    public DeferredResult<EventHub.Poll> poll(@CurrentUser AuthUser me, @RequestParam(required = false) Long after) {
        return hub.poll(me.id(), after);
    }
}
