package com.velox.catalog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.velox.catalog.dto.CatalogDtos.AcceptingRequest;
import com.velox.catalog.dto.CatalogDtos.ProductUpdateRequest;
import com.velox.catalog.dto.CatalogDtos.StoreDetail;
import com.velox.catalog.service.RestaurantCatalogService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** Store staff: their own store and menu. */
@RestController
@RequestMapping("/api/restaurant")
public class RestaurantCatalogController {

    private final RestaurantCatalogService restaurant;

    public RestaurantCatalogController(RestaurantCatalogService restaurant) {
        this.restaurant = restaurant;
    }

    @GetMapping("/store")
    public StoreDetail myStore(@CurrentUser AuthUser me) {
        return restaurant.myStore(me);
    }

    @PatchMapping("/store")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAccepting(@CurrentUser AuthUser me, @Valid @RequestBody AcceptingRequest req) {
        restaurant.setAccepting(me, req.accepting());
    }

    @PatchMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProduct(@CurrentUser AuthUser me, @PathVariable String id, @Valid @RequestBody ProductUpdateRequest req) {
        restaurant.updateProduct(me, id, req);
    }
}
