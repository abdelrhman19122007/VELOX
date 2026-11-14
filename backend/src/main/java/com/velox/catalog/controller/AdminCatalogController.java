package com.velox.catalog.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.velox.catalog.dto.CatalogDtos.Created;
import com.velox.catalog.dto.CatalogDtos.ProductRequest;
import com.velox.catalog.dto.CatalogDtos.StoreRequest;
import com.velox.catalog.dto.CatalogDtos.StoreUpdateRequest;
import com.velox.catalog.service.ImageStorage;
import com.velox.catalog.service.StoreAdminService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** Admin: stores, products and photo uploads. Role checks live in the services. */
@RestController
@RequestMapping("/api")
public class AdminCatalogController {

    private final StoreAdminService admin;
    private final ImageStorage images;

    public AdminCatalogController(StoreAdminService admin, ImageStorage images) {
        this.admin = admin;
        this.images = images;
    }

    @PostMapping("/admin/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public Created createStore(@CurrentUser AuthUser me, @Valid @RequestBody StoreRequest req) {
        return new Created(admin.createStore(me, req));
    }

    @PatchMapping("/admin/stores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStore(@CurrentUser AuthUser me, @PathVariable String id, @Valid @RequestBody StoreUpdateRequest req) {
        admin.updateStore(me, id, req);
    }

    @PostMapping("/admin/stores/{id}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Created addProduct(@CurrentUser AuthUser me, @PathVariable String id, @Valid @RequestBody ProductRequest req) {
        return new Created(admin.addProduct(me, id, req));
    }

    @PutMapping("/admin/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProduct(@CurrentUser AuthUser me, @PathVariable String id, @Valid @RequestBody ProductRequest req) {
        admin.updateProduct(me, id, req);
    }

    /** Admin or store staff upload a photo; the answer is the URL to put in photoUrl. */
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@CurrentUser AuthUser me, @RequestParam("file") MultipartFile file) {
        return Map.of("url", images.store(me, file));
    }
}
