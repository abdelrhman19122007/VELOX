package com.velox.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.velox.order.dto.OrderDtos.ZoneRequest;
import com.velox.order.dto.OrderDtos.ZoneView;
import com.velox.order.service.ZoneService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** Delivery zones: the public list for checkout, and the admin's editor. */
@RestController
public class ZoneController {

    private final ZoneService zones;

    public ZoneController(ZoneService zones) {
        this.zones = zones;
    }

    @GetMapping("/api/zones")
    public List<ZoneView> zones() {
        return zones.active().stream().map(ZoneView::of).toList();
    }

    @GetMapping("/api/admin/zones")
    public List<ZoneView> all(@CurrentUser AuthUser me) {
        return zones.all(me).stream().map(ZoneView::of).toList();
    }

    @PostMapping("/api/admin/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@CurrentUser AuthUser me, @Valid @RequestBody ZoneRequest req) {
        zones.create(me, req.toZone());
    }

    @PutMapping("/api/admin/zones/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@CurrentUser AuthUser me, @PathVariable String code, @Valid @RequestBody ZoneRequest req) {
        zones.update(me, code, req.toZone());
    }
}
