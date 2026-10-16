package com.velox.catalog.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.velox.catalog.repository.FavoriteRepository;
import com.velox.common.error.ApiException;
import com.velox.security.AuthUser;

/** Customers' favourite stores and products (FR-CAT-08). Keys look like {@code store:R1} or {@code product:F3}. */
@Service
public class FavoriteService {

    private static final Pattern KEY = Pattern.compile("^(store|product):[A-Z][0-9]{1,8}$");

    private final FavoriteRepository favorites;

    public FavoriteService(FavoriteRepository favorites) {
        this.favorites = favorites;
    }

    public List<String> list(AuthUser me) {
        return favorites.findByUser(me.id());
    }

    public List<String> toggle(AuthUser me, String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw ApiException.badRequest("INVALID_FAVORITE", "Unknown favourite");
        }
        favorites.toggle(me.id(), key);
        return favorites.findByUser(me.id());
    }
}
