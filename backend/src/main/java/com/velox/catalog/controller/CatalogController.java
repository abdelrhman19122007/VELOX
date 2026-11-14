package com.velox.catalog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.velox.catalog.dto.CatalogDtos.SearchResult;
import com.velox.catalog.dto.CatalogDtos.StoreDetail;
import com.velox.catalog.dto.CatalogDtos.StoreSummary;
import com.velox.catalog.service.CatalogService;
import com.velox.catalog.service.FavoriteService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

/** Public catalogue (no sign-in needed) and the signed-in user's favourites. */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;
    private final FavoriteService favorites;

    public CatalogController(CatalogService catalog, FavoriteService favorites) {
        this.catalog = catalog;
        this.favorites = favorites;
    }

    @GetMapping("/stores")
    public List<StoreSummary> stores(@RequestParam(defaultValue = "false") boolean openNow) {
        return catalog.listStores(openNow);
    }

    @GetMapping("/stores/{id}")
    public StoreDetail store(@PathVariable String id) {
        return catalog.store(id);
    }

    @GetMapping("/search")
    public SearchResult search(@RequestParam(name = "q", defaultValue = "") String query) {
        return catalog.search(query);
    }

    @GetMapping("/me/favorites")
    public List<String> favorites(@CurrentUser AuthUser me) {
        return favorites.list(me);
    }

    @PostMapping("/me/favorites/{key}")
    public List<String> toggleFavorite(@CurrentUser AuthUser me, @PathVariable String key) {
        return favorites.toggle(me, key);
    }
}
