package com.example.cache.service.response;

import com.example.cache.model.Item;

import java.util.List;

public record ItemPageResponse(
        List<ItemResponse> items,
        long count
) {
    public static ItemPageResponse fromResponse(List<ItemResponse> items, long count) {
        return new ItemPageResponse(items, count);
    }

    public static ItemPageResponse from(List<Item> items, long count) {
        return fromResponse(items.stream().map(ItemResponse::from).toList(), count);
    }
}
