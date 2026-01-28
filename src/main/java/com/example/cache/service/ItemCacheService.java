package com.example.cache.service;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.service.response.ItemPageResponse;
import com.example.cache.service.response.ItemResponse;

public interface ItemCacheService {
    ItemResponse read(Long itemId);

    ItemPageResponse readAll(Long page, Long pageSize);

    ItemPageResponse readAllInfiniteScroll(Long lastItemId, Long pageSize);

    ItemResponse create(ItemCreateRequest request);

    ItemResponse update(Long itemId, ItemUpdateRequest request);

    void delete(Long itemId);

    boolean supports(CacheStrategy cacheStrategy);
}
