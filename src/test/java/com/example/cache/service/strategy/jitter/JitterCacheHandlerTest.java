package com.example.cache.service.strategy.jitter;

import com.example.cache.RedisTestContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class JitterCacheHandlerTest extends RedisTestContainerSupport {

    @Autowired
    JitterCacheHandler jitterCacheHandler;

    @Test
    @DisplayName("TTL에 jitter가 적용되어 만료 시간이 랜덤 범위로 설정된다")
    void put() {
        // given, when
        jitterCacheHandler.put(
                "testKey",
                Duration.ofSeconds(10),
                String.class
        );

        // then
        // 기본 TTL(10초)에 ±3초 jitter가 적용되었는지 검증
        Long ttlSeconds = redisTemplate.getExpire("testKey", TimeUnit.SECONDS);
        System.out.println("ttlSeconds = " + ttlSeconds);

        assertThat(ttlSeconds).isGreaterThanOrEqualTo(7);
        assertThat(ttlSeconds).isLessThanOrEqualTo(13);
    }

    @Test
    @DisplayName("TTL이 jitter 범위 이하인 경우 예외가 발생한다")
    void put_shouldThrowException_whenTtlIsLessThanOrEqualToJitterRangeSeconds() {
        // TTL이 jitter 범위(±3초) 이하이면 잘못된 설정
        assertThatThrownBy(() ->
                jitterCacheHandler.put(
                        "testKey",
                        Duration.ofSeconds(3),
                        String.class
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("evict 호출 시 캐시 데이터가 삭제된다")
    void evict() {
        // given
        jitterCacheHandler.put(
                "testKey",
                Duration.ofSeconds(10),
                String.class
        );

        // when
        jitterCacheHandler.evict("testKey");

        // then
        String result = redisTemplate.opsForValue().get("testKey");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetch는 최초 1회만 Data Source를 호출하고 이후에는 캐시를 사용한다")
    void fetch() {
        String result1 = fetchData(); // cache miss
        String result2 = fetchData(); // cache hit
        String result3 = fetchData(); // cache hit

        assertThat(result1).isEqualTo("sourceData");
        assertThat(result2).isEqualTo("sourceData");
        assertThat(result3).isEqualTo("sourceData");
    }

    private String fetchData() {
        return jitterCacheHandler.fetch(
                "testKey",
                Duration.ofSeconds(10),
                () -> {
                    // 실제 Data Source 호출을 가정
                    System.out.println("fetch source data"); // fetchData()가 총 3번 호출됐지만, 1회만 Data Source를 호출했기 때문에 해당 로그는 콘솔에 1번만 찍힌다
                    return "sourceData";
                },
                String.class
        );
    }

}
