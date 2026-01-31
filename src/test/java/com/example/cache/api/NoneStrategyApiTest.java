package com.example.cache.api;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.service.response.ItemPageResponse;
import com.example.cache.service.response.ItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CRUD 및 간단하게 구현한 캐시가 잘 동작하는지 확인하는 용도")
public class NoneStrategyApiTest {
    static final CacheStrategy CACHE_STRATEGY = CacheStrategy.NONE;

    @Test
    void createAndReadAndUpdateAndDelete() {
        ItemResponse created = ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data"));
        System.out.println("created = " + created); // ItemResponse[itemId=1, data=data]

        ItemResponse read1 = ItemApiTestUtils.read(CACHE_STRATEGY, created.itemId());
        System.out.println("read1 = " + read1); // ItemResponse[itemId=1, data=data]

        ItemResponse updated = ItemApiTestUtils.update(CACHE_STRATEGY, read1.itemId(), new ItemUpdateRequest("updatedData"));
        System.out.println("updated = " + updated); // ItemResponse[itemId=1, data=updatedData]

        ItemResponse read2 = ItemApiTestUtils.read(CACHE_STRATEGY, read1.itemId());
        System.out.println("read2 = " + read2); // ItemResponse[itemId=1, data=updatedData]  ->  변경된 데이터인 updatedData로 정상조회

        ItemApiTestUtils.delete(CACHE_STRATEGY, read1.itemId());

        ItemResponse read3 = ItemApiTestUtils.read(CACHE_STRATEGY, read1.itemId());
        System.out.println("read3 = " + read3); // read3 = null
    }

    @Test
    void readAll() {
        for (int i = 0; i < 3; i++) {
            ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data" + i));
        }

        ItemPageResponse itemPage1 = ItemApiTestUtils.readAll(CACHE_STRATEGY, 1L, 2L);
        System.out.println("itemPage1 = " + itemPage1);

        ItemPageResponse itemPage2 = ItemApiTestUtils.readAll(CACHE_STRATEGY, 2L, 2L);
        System.out.println("itemPage2 = " + itemPage2);
    }

    @Test
    void readAllInfiniteScroll() {
        for (int i = 0; i < 3; i++) {
            ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data" + i));
        }

        ItemPageResponse itemPage1 = ItemApiTestUtils.readAllInfiniteScroll(CACHE_STRATEGY, null, 2L);
        System.out.println("itemPage1 = " + itemPage1);

        ItemPageResponse itemPage2 = ItemApiTestUtils.readAllInfiniteScroll(CACHE_STRATEGY, itemPage1.items().getLast().itemId(), 2L);
        System.out.println("itemPage2 = " + itemPage2);
    }
}
