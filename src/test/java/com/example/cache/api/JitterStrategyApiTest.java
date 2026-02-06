package com.example.cache.api;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.service.response.ItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JitterStrategyApiTest {

    static final CacheStrategy CACHE_STRATEGY = CacheStrategy.JITTER;

    @Test
    @DisplayName("Jitter 전략에서 다수의 동시 조회 시 캐시 만료 시점이 분산되어 Stampede가 완화되는지 확인")
    void test() throws InterruptedException {
        // given
        // Jitter 캐시 전략을 사용하는 아이템 3개 생성
        List<ItemResponse> items = List.of(
                ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data1")),
                ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data2")),
                ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data3"))
        );

        // when
        // 여러 스레드에서 반복적으로 read 요청을 발생시켜
        // 캐시 만료 시점이 겹치지 않도록 jitter 효과를 유도
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        long start = System.nanoTime();

        while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(20)) {
            for (ItemResponse item : items) {
                executorService.execute(() ->
                        ItemApiTestUtils.read(CACHE_STRATEGY, item.itemId())
                );
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }

        // then
        // 캐시가 사용 중인 상태에서 update 이후 최신 데이터 조회 가능 여부 확인
        ItemApiTestUtils.update(
                CACHE_STRATEGY,
                items.getFirst().itemId(),
                new ItemUpdateRequest("updated")
        );
        ItemResponse updated = ItemApiTestUtils.read(
                CACHE_STRATEGY,
                items.getFirst().itemId()
        );
        System.out.println("updated = " + updated);

        // 삭제 이후 캐시/원본 데이터 동작 확인
        ItemApiTestUtils.delete(CACHE_STRATEGY, items.getFirst().itemId());
        ItemResponse deleted = ItemApiTestUtils.read(
                CACHE_STRATEGY,
                items.getFirst().itemId()
        );
        System.out.println("deleted = " + deleted);
    }

}
