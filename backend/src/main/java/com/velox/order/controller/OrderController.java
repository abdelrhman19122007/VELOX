package com.velox.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.order.dto.OrderDtos.PlaceOrderRequest;
import com.velox.order.dto.OrderDtos.Quote;
import com.velox.order.dto.OrderDtos.QuoteRequest;
import com.velox.order.service.OrderService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** Customer orders. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/quote")
    public Quote quote(@CurrentUser(required = false) AuthUser me, @Valid @RequestBody QuoteRequest req) {
        return orders.quote(me, req);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@CurrentUser AuthUser me, @Valid @RequestBody PlaceOrderRequest req) {
        return orders.place(me, req);
    }

    @GetMapping("/mine")
    public List<OrderView> mine(@CurrentUser AuthUser me) {
        return orders.mine(me);
    }

    @GetMapping("/{code}")
    public OrderView get(@CurrentUser AuthUser me, @PathVariable String code) {
        return orders.get(me, code);
    }

    @PostMapping("/{code}/cancel")
    public OrderView cancel(@CurrentUser AuthUser me, @PathVariable String code) {
        return orders.cancel(me, code);
    }

    @PostMapping("/{code}/return")
    public OrderView returnOrder(@CurrentUser AuthUser me, @PathVariable String code) {
        return orders.returnOrder(me, code);
    }
}
