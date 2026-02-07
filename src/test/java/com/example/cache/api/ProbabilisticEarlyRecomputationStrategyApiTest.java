package com.example.cache.api;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import com.example.cache.service.response.ItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProbabilisticEarlyRecomputationStrategyApiTest {

    static final CacheStrategy CACHE_STRATEGY = CacheStrategy.PROBABILISTIC_EARLY_RECOMPUTATION;

    @Test
    @DisplayName("PER 전략에서 다수의 동시 read 요청 중 일부만 확률적으로 캐시를 재계산한다")
    void test() throws InterruptedException {
        // given: PER 전략으로 아이템 생성
        ItemResponse item = ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data"));

        // when: 동일한 Key에 대해 다수의 동시 read 요청 발생
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        long start = System.nanoTime();
        while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(20)) {
            for (int i = 0; i < 3; i++) {
                executorService.execute(() -> ItemApiTestUtils.read(CACHE_STRATEGY, item.itemId()));
            }

            TimeUnit.MILLISECONDS.sleep(10);
        }

        // then: update 이후 최신 데이터가 정상적으로 반영된다
        ItemApiTestUtils.update(CACHE_STRATEGY, item.itemId(), new ItemUpdateRequest("updated"));
        ItemResponse updated = ItemApiTestUtils.read(CACHE_STRATEGY, item.itemId());
        System.out.println("updated = " + updated);

        // then: delete 이후 캐시 및 원본 데이터가 정상적으로 제거된다
        ItemApiTestUtils.delete(CACHE_STRATEGY, item.itemId());
        ItemResponse deleted = ItemApiTestUtils.read(CACHE_STRATEGY, item.itemId());
        System.out.println("deleted = " + deleted);
    }

}
