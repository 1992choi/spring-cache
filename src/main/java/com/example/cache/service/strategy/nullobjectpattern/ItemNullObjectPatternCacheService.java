package com.example.cache.service.strategy.nullobjectpattern;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.service.ItemCacheService;
import com.example.cache.service.ItemService;
import com.example.cache.service.response.ItemPageResponse;
import com.example.cache.service.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemNullObjectPatternCacheService implements ItemCacheService {
    private final ItemService itemService;

    /**
     * 현재는 단순 null 데잉터가 들어있는 객체만 반환하지만 더욱 유연한 구조의 nullObject를 만들 수도 있다.
     * 예) 데이터가 정말 없는 것인지, 또는 비공개 처리되어서 접근이 안되는 것인지 등의 예외 필드를 추가하여 유연성을 높일 수 있다.
     */
    private static final ItemResponse nullObject = new ItemResponse(null, null);

    @Override
    @Cacheable(cacheNames = "item", key = "#itemId")
    public ItemResponse read(Long itemId) {
        ItemResponse itemResponse = itemService.read(itemId);
        if (itemResponse == null) {
            return nullObject;
        }
        return itemResponse;
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
    public ItemResponse update(Long itemId, ItemUpdateRequest request) {
        return itemService.update(itemId, request);
    }

    @Override
    public void delete(Long itemId) {
        itemService.delete(itemId);
    }

    @Override
    public boolean supports(CacheStrategy cacheStrategy) {
        return CacheStrategy.NULL_OBJECT_PATTERN == cacheStrategy;
    }
}
