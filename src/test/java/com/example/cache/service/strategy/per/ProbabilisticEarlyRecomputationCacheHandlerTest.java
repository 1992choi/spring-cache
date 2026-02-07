package com.example.cache.service.strategy.per;

import com.example.cache.RedisTestContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProbabilisticEarlyRecomputationCacheHandlerTest extends RedisTestContainerSupport {

    @Autowired
    ProbabilisticEarlyRecomputationCacheHandler cacheHandler;

    @Test
    @DisplayName("PER 전략 캐시 put 시 데이터가 정상적으로 저장된다")
    void put() {
        // given, when
        cacheHandler.put("testKey", Duration.ofSeconds(10), "data");

        // then
        String result = redisTemplate.opsForValue().get("testKey");
        assertThat(result).isNotNull();
        System.out.println("result = " + result);
    }

    @Test
    @DisplayName("PER 전략 캐시 evict 호출 시 캐시 데이터가 제거된다")
    void evict() {
        // given
        cacheHandler.put("testKey", Duration.ofSeconds(10), "data");

        // when
        cacheHandler.evict("testKey");

        // then
        String result = redisTemplate.opsForValue().get("testKey");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("PER 전략 fetch 호출 시 캐시 히트 상태에서는 동일한 데이터를 반환한다")
    void fetch() {
        String result1 = fetchData();
        String result2 = fetchData();
        String result3 = fetchData();

        assertThat(result1).isEqualTo("sourceData");
        assertThat(result2).isEqualTo("sourceData");
        assertThat(result3).isEqualTo("sourceData");
    }

    private String fetchData() {
        return cacheHandler.fetch(
                "testKey",
                Duration.ofSeconds(10),
                () -> {
                    // 실제 Data Source 조회를 흉내
                    System.out.println("fetch source data");
                    return "sourceData";
                },
                String.class
        );
    }

}
