package com.example.cache.service.strategy.jitter;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.common.cache.CustomCacheHandler;
import com.example.cache.serde.DataSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

@Component
@RequiredArgsConstructor
public class JitterCacheHandler implements CustomCacheHandler {

    private final StringRedisTemplate redisTemplate;

    // TTL에 적용할 jitter 범위 (±3초)
    private static final int JITTER_RANGE_SECONDS = 3;

    @Override
    public <T> T fetch(String key, Duration ttl, Supplier<T> dataSourceSupplier, Class<T> clazz) {
        // 1. 캐시 조회
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            // 캐시 미스 → Data Source 조회 후 캐시 갱신
            return refresh(key, ttl, dataSourceSupplier);
        }

        // 2. 역직렬화 실패 시 캐시 무효로 판단
        T data = DataSerializer.deserializeOrNull(cached, clazz);
        if (data == null) {
            return refresh(key, ttl, dataSourceSupplier);
        }

        // 3. 캐시 히트
        return data;
    }

    /**
     * Data Source에서 데이터를 조회하고
     * jitter가 적용된 TTL로 캐시를 갱신한다.
     */
    private <T> T refresh(String key, Duration ttl, Supplier<T> dataSourceSupplier) {
        T sourceResult = dataSourceSupplier.get();
        put(key, ttl, sourceResult);
        return sourceResult;
    }

    @Override
    public void put(String key, Duration ttl, Object value) {
        redisTemplate.opsForValue().set(
                key,
                DataSerializer.serializeOrException(value),
                applyJitter(ttl)
        );
    }

    /**
     * TTL에 랜덤 jitter를 적용하여
     * 캐시 만료 시점을 분산시킨다.
     */
    private Duration applyJitter(Duration ttl) {
        if (ttl.getSeconds() <= JITTER_RANGE_SECONDS) {
            throw new IllegalArgumentException(
                    "Jitter ttl must be greater than " + JITTER_RANGE_SECONDS
            );
        }

        // [-3초 ~ +3초] 범위의 랜덤 jitter 생성
        int jitter = RandomGenerator.getDefault()
                .nextInt(-JITTER_RANGE_SECONDS, JITTER_RANGE_SECONDS + 1);

        return ttl.plusSeconds(jitter);
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean supports(CacheStrategy cacheStrategy) {
        return CacheStrategy.JITTER == cacheStrategy;
    }

}
