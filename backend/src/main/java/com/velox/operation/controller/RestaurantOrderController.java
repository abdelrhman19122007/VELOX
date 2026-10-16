package com.velox.operation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.velox.operation.dto.OperationDtos.ReasonRequest;
import com.velox.operation.dto.OperationDtos.StoreStats;
import com.velox.operation.repository.FeedbackRepository.Review;
import com.velox.operation.service.RestaurantOrderService;
import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** The store dashboard: its orders and what it does with them. */
@RestController
@RequestMapping("/api/restaurant")
public class RestaurantOrderController {

    private final RestaurantOrderService service;

    public RestaurantOrderController(RestaurantOrderService service) {
        this.service = service;
    }

    @GetMapping("/orders")
    public List<OrderView> orders(@CurrentUser AuthUser me, @RequestParam(defaultValue = "true") boolean active) {
        return service.orders(me, active);
    }

    @PostMapping("/orders/{code}/accept")
    public OrderView accept(@CurrentUser AuthUser me, @PathVariable String code) {
        return service.accept(me, code);
    }

    @PostMapping("/orders/{code}/reject")
    public OrderView reject(@CurrentUser AuthUser me, @PathVariable String code, @Valid @RequestBody ReasonRequest req) {
        return service.reject(me, code, req.reason());
    }

    @PostMapping("/orders/{code}/ready")
    public OrderView ready(@CurrentUser AuthUser me, @PathVariable String code) {
        return service.ready(me, code);
    }

    @GetMapping("/stats")
    public StoreStats stats(@CurrentUser AuthUser me) {
        return service.stats(me);
    }

    @GetMapping("/reviews")
    public List<Review> reviews(@CurrentUser AuthUser me) {
        return service.reviews(me);
    }
}
