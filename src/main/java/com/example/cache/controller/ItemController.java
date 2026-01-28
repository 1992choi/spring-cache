package com.example.cache.controller;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.service.ItemCacheService;
import com.example.cache.service.response.ItemPageResponse;
import com.example.cache.service.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ItemController {
    private final List<ItemCacheService> itemCacheServices;

    @GetMapping("/cache-strategy/{cacheStrategy}/items/{itemId}")
    public ItemResponse read(
            @PathVariable CacheStrategy cacheStrategy,
            @PathVariable Long itemId
    ) {
        return resolveCacheHandler(cacheStrategy).read(itemId);
    }

    @GetMapping("/cache-strategy/{cacheStrategy}/items")
    public ItemPageResponse readAll(
            @PathVariable CacheStrategy cacheStrategy,
            @RequestParam Long page,
            @RequestParam Long pageSize
    ) {
        return resolveCacheHandler(cacheStrategy).readAll(page, pageSize);
    }

    @GetMapping("/cache-strategy/{cacheStrategy}/items/infinite-scroll")
    public ItemPageResponse readAllInfiniteScroll(
            @PathVariable CacheStrategy cacheStrategy,
            @RequestParam(required = false) Long lastItemId,
            @RequestParam Long pageSize
    ) {
        return resolveCacheHandler(cacheStrategy).readAllInfiniteScroll(lastItemId, pageSize);
    }

    @PostMapping("/cache-strategy/{cacheStrategy}/items")
    public ItemResponse create(
            @PathVariable CacheStrategy cacheStrategy,
            @RequestBody ItemCreateRequest request
    ) {
        return resolveCacheHandler(cacheStrategy).create(request);
    }

    @PutMapping("/cache-strategy/{cacheStrategy}/items/{itemId}")
    public ItemResponse update(
            @PathVariable CacheStrategy cacheStrategy,
            @PathVariable Long itemId,
            @RequestBody ItemUpdateRequest request
    ) {
        return resolveCacheHandler(cacheStrategy).update(itemId, request);
    }

    @DeleteMapping("/cache-strategy/{cacheStrategy}/items/{itemId}")
    public void delete(
            @PathVariable CacheStrategy cacheStrategy,
            @PathVariable Long itemId
    ) {
        resolveCacheHandler(cacheStrategy).delete(itemId);
    }

    private ItemCacheService resolveCacheHandler(CacheStrategy cacheStrategy) {
        return itemCacheServices.stream()
                .filter(itemCacheService -> itemCacheService.supports(cacheStrategy))
                .findFirst()
                .orElseThrow();
    }
}
