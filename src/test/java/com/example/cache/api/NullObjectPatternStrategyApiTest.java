package com.example.cache.api;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.service.response.ItemResponse;
import org.junit.jupiter.api.Test;

public class NullObjectPatternStrategyApiTest {
    static final CacheStrategy CACHE_STRATEGY = CacheStrategy.NULL_OBJECT_PATTERN;

    /*
        SpringCacheAnnotationStrategyApiTest와 로그를 비교해보면,
        SpringCacheAnnotationStrategyApiTest에서는 null을 캐시처리하지 못해서 오류가 발생하지만,
        현재 코드에서는 Null Object Pattern 을 적용하여, 없는 데이터에 대해서도 캐싱처리가 되어 캐시 관통 현상을 보완할 수 있게 되었다.
     */
    @Test
    void read() {
        for (int i = 0; i < 3; i++) {
            ItemResponse item = ItemApiTestUtils.read(CACHE_STRATEGY, 99999L);
            System.out.println("item = " + item);
        }
    }
}
