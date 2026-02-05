package com.example.cache.api;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SplitShardedSubBloomFilterStrategyApiTest {
    static final CacheStrategy CACHE_STRATEGY = CacheStrategy.SPLIT_SHARDED_SUB_BLOOM_FILTER;

    @Test
    @DisplayName("Bloom Filter 전략에서 존재하지 않는 ID 대량 조회 시 Cache Penetration이 발생하지 않는다")
    void test() {
        for (int i = 0; i < 1000 + 2000 + 4000; i++) {
            ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data" + i));
        }

        for (long itemId = 10000; itemId < 11000; itemId++) {
            ItemApiTestUtils.read(CACHE_STRATEGY, itemId);
        }
    }

}
