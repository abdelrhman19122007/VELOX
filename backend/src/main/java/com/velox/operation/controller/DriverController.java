package com.velox.operation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.velox.operation.dto.OperationDtos.DriverStatusRequest;
import com.velox.operation.dto.OperationDtos.DriverView;
import com.velox.operation.dto.OperationDtos.Earnings;
import com.velox.operation.dto.OperationDtos.LocationRequest;
import com.velox.operation.service.DriverService;
import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** The driver app: shift, location, offers and the delivery steps. */
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverService service;

    public DriverController(DriverService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public DriverView status(@CurrentUser AuthUser me) {
        return service.status(me);
    }

    @PostMapping("/status")
    public DriverView setStatus(@CurrentUser AuthUser me, @Valid @RequestBody DriverStatusRequest req) {
        return service.setStatus(me, req.online(), req.vehicle());
    }

    @PostMapping("/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void location(@CurrentUser AuthUser me, @Valid @RequestBody LocationRequest req) {
        service.location(me, req.lat(), req.lng());
    }

    @GetMapping("/offers")
    public List<OrderView> offers(@CurrentUser AuthUser me) {
        return service.offers(me);
    }

    @PostMapping("/orders/{code}/take")
    public OrderView take(@CurrentUser AuthUser me, @PathVariable String code) {
        return service.take(me, code);
    }

    @PostMapping("/orders/{code}/pickup")
    public OrderView pickUp(@CurrentUser AuthUser me, @PathVariable String code) {
        return service.pickUp(me, code);
    }

    @PostMapping("/orders/{code}/deliver")
    public OrderView deliver(@CurrentUser AuthUser me, @PathVariable String code) {
        return service.deliver(me, code);
    }

    @GetMapping("/orders")
    public List<OrderView> orders(@CurrentUser AuthUser me, @RequestParam(defaultValue = "true") boolean active) {
        return service.myOrders(me, active);
    }

    @GetMapping("/earnings")
    public Earnings earnings(@CurrentUser AuthUser me) {
        return service.earnings(me);
    }
}
