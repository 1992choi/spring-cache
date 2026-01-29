package com.example.cache.api;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.service.response.ItemPageResponse;
import com.example.cache.service.response.ItemResponse;
import org.junit.jupiter.api.Test;

public class SpringCacheAnnotationStrategyApiTest {
    static final CacheStrategy CACHE_STRATEGY = CacheStrategy.SPRING_CACHE_ANNOTATION;

    @Test
    void createAndReadAndReadAllAndUpdateAndDelete() {
        // create
        ItemResponse item1 = ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data1"));
        ItemResponse item2 = ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data2"));
        ItemResponse item3 = ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data3"));

        // read
        /*
            각각의 item에 대하여 3번씩 read를 했지만, 애플리케이션의 로그에는 각 item마다 1개의 로그만 찍혀있음.

            [ItemRepository.read] itemId=1
            [ItemRepository.read] itemId=2
            [ItemRepository.read] itemId=3

            그 이유는 캐싱이 되어서 2번째와 3번째 read에 대한 로그는 없기 때문.
            만약 캐싱이 안되었다면, 아래와 같은 로그가 출력되었을 것임.

            [ItemRepository.read] itemId=1
            [ItemRepository.read] itemId=1
            [ItemRepository.read] itemId=1
            [ItemRepository.read] itemId=2
            [ItemRepository.read] itemId=2
            [ItemRepository.read] itemId=2
            [ItemRepository.read] itemId=3
            [ItemRepository.read] itemId=3
            [ItemRepository.read] itemId=3


            readAll / readAllInfiniteScroll 도 동일함.
         */
        ItemResponse item1Read1 = ItemApiTestUtils.read(CACHE_STRATEGY, item1.itemId());
        ItemResponse item1Read2 = ItemApiTestUtils.read(CACHE_STRATEGY, item1.itemId());
        ItemResponse item1Read3 = ItemApiTestUtils.read(CACHE_STRATEGY, item1.itemId());
        System.out.println("item1Read1 = " + item1Read1);
        System.out.println("item1Read2 = " + item1Read2);
        System.out.println("item1Read3 = " + item1Read3);

        ItemResponse item2Read1 = ItemApiTestUtils.read(CACHE_STRATEGY, item2.itemId());
        ItemResponse item2Read2 = ItemApiTestUtils.read(CACHE_STRATEGY, item2.itemId());
        ItemResponse item2Read3 = ItemApiTestUtils.read(CACHE_STRATEGY, item2.itemId());
        System.out.println("item2Read1 = " + item2Read1);
        System.out.println("item2Read2 = " + item2Read2);
        System.out.println("item2Read3 = " + item2Read3);

        ItemResponse item3Read1 = ItemApiTestUtils.read(CACHE_STRATEGY, item3.itemId());
        ItemResponse item3Read2 = ItemApiTestUtils.read(CACHE_STRATEGY, item3.itemId());
        ItemResponse item3Read3 = ItemApiTestUtils.read(CACHE_STRATEGY, item3.itemId());
        System.out.println("item3Read1 = " + item3Read1);
        System.out.println("item3Read2 = " + item3Read2);
        System.out.println("item3Read3 = " + item3Read3);

        // readAll
        ItemPageResponse itemPageReadAll1 = ItemApiTestUtils.readAll(CACHE_STRATEGY, 1L, 2L);
        ItemPageResponse itemPageReadAll2 = ItemApiTestUtils.readAll(CACHE_STRATEGY, 1L, 2L);
        System.out.println("itemPageReadAll1 = " + itemPageReadAll1);
        System.out.println("itemPageReadAll2 = " + itemPageReadAll2);

        // readAllInfiniteScroll
        ItemPageResponse itemPageReadAllInfiniteScroll1 = ItemApiTestUtils.readAllInfiniteScroll(CACHE_STRATEGY, null, 2L);
        ItemPageResponse itemPageReadAllInfiniteScroll2 = ItemApiTestUtils.readAllInfiniteScroll(CACHE_STRATEGY, null, 2L);
        System.out.println("itemPageReadAllInfiniteScroll1 = " + itemPageReadAllInfiniteScroll1);
        System.out.println("itemPageReadAllInfiniteScroll2 = " + itemPageReadAllInfiniteScroll2);

        // update
        ItemApiTestUtils.update(CACHE_STRATEGY, item1.itemId(), new ItemUpdateRequest("updatedData"));
        ItemResponse updated = ItemApiTestUtils.read(CACHE_STRATEGY, item1.itemId());
        System.out.println("updated = " + updated);

        // delete
        ItemApiTestUtils.delete(CACHE_STRATEGY, item1.itemId());
        try {
            ItemApiTestUtils.read(CACHE_STRATEGY, item1.itemId()); // null 데이터는 저장하지 못하여서 오류가 발생. (SpringCacheConfig.java에 disableCachingNullValues 옵션을 지정했기 때문에 null을 저장하지 않음)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void readNullData() {
        for (int i = 0; i < 3; i++) {
            try {
                ItemApiTestUtils.read(CACHE_STRATEGY, 99999L);
            } catch (Exception ignored) {

            }
        }
    }
}
