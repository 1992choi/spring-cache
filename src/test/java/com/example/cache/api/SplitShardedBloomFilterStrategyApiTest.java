package com.example.cache.api;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.model.ItemCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SplitShardedBloomFilterStrategyApiTest {

    static final CacheStrategy CACHE_STRATEGY = CacheStrategy.SPLIT_SHARDED_BLOOM_FILTER;

    @Test
    @DisplayName("Bloom Filter 전략에서 존재하지 않는 ID 대량 조회 시 Cache Penetration이 발생하지 않는다")
    void test() {
        // 실제 존재하는 데이터 1,000개 생성
        for (int i = 0; i < 1000; i++) {
            ItemApiTestUtils.create(CACHE_STRATEGY, new ItemCreateRequest("data" + i));
        }

        // 존재하지 않는 데이터 10,000개 조회
        // 어플리케이션 로그에서 '[ItemRepository.read]'을 검색해보면 100여개 정도만 로그가 찍혀있음.
        // 없는 데이터에 대해서 DB로 요청이 가지 않은 것을 확인하는 테스트
        for (long itemId = 10000; itemId < 20000; itemId++) {
            ItemApiTestUtils.read(CACHE_STRATEGY, itemId);
        }
    }

}
