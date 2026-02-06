package com.example.cache.service.strategy.jitter;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.common.cache.CustomCacheEvict;
import com.example.cache.common.cache.CustomCachePut;
import com.example.cache.common.cache.CustomCacheable;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.service.ItemCacheService;
import com.example.cache.service.ItemService;
import com.example.cache.service.response.ItemPageResponse;
import com.example.cache.service.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemJitterCacheService implements ItemCacheService {
    private final ItemService itemService;

    @Override
    @CustomCacheable(
            cacheStrategy = CacheStrategy.JITTER,
            cacheName = "item",
            key = "#itemId",
            ttlSeconds = 5
    )
    public ItemResponse read(Long itemId) {
        return itemService.read(itemId);
    }

    @Override
    public ItemPageResponse readAll(Long page, Long pageSize) {
        return itemService.readAll(page, pageSize);
    }

    @Override
    public ItemPageResponse readAllInfiniteScroll(Long lastItemId, Long pageSize) {
        return itemService.readAllInfiniteScroll(lastItemId, pageSize);
    }

    @Override
    public ItemResponse create(ItemCreateRequest request) {
        return itemService.create(request);
    }

    @Override
    @CustomCachePut(
            cacheStrategy = CacheStrategy.JITTER,
            cacheName = "item",
            key = "#itemId",
            ttlSeconds = 5
    )
    public ItemResponse update(Long itemId, ItemUpdateRequest request) {
        return itemService.update(itemId, request);
    }

    @Override
    @CustomCacheEvict(
            cacheStrategy = CacheStrategy.JITTER,
            cacheName = "item",
            key = "#itemId"
    )
    public void delete(Long itemId) {
        itemService.delete(itemId);
    }

    @Override
    public boolean supports(CacheStrategy cacheStrategy) {
        return CacheStrategy.JITTER == cacheStrategy;
    }

}
