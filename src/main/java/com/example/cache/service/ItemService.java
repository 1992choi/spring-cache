package com.example.cache.service;

import com.example.cache.model.Item;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.repository.ItemRepository;
import com.example.cache.service.response.ItemPageResponse;
import com.example.cache.service.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemResponse read(Long itemId) {
        return itemRepository.read(itemId)
                .map(ItemResponse::from)
                .orElse(null);
    }

    public ItemPageResponse readAll(Long page, Long pageSize) {
        return ItemPageResponse.from(
                itemRepository.readAll(page, pageSize),
                itemRepository.count()
        );
    }

    public ItemPageResponse readAllInfiniteScroll(Long lastItemId, Long pageSize) {
        return ItemPageResponse.from(
                itemRepository.readAllInfiniteScroll(lastItemId, pageSize),
                itemRepository.count()
        );
    }

    public ItemResponse create(ItemCreateRequest request) {
        return ItemResponse.from(
                itemRepository.create(Item.create(request))
        );
    }

    public ItemResponse update(Long itemId, ItemUpdateRequest request) {
        Item item = itemRepository.read(itemId).orElseThrow();
        item.update(request);
        return ItemResponse.from(
                itemRepository.update(item)
        );
    }

    public void delete(Long itemId) {
        itemRepository.read(itemId).ifPresent(itemRepository::delete);
    }

    public long count() {
        return itemRepository.count();
    }
}
