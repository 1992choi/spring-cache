package com.example.cache.service.strategy.per;

import com.example.cache.common.cache.CacheStrategy;
import com.example.cache.common.cache.CustomCacheHandler;
import com.example.cache.serde.DataSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ProbabilisticEarlyRecomputationCacheHandler implements CustomCacheHandler {

    private final StringRedisTemplate redisTemplate;

    @Override
    public <T> T fetch(String key, Duration ttl, Supplier<T> dataSourceSupplier, Class<T> clazz) {
        /*
            PER 기반 캐시 조회 진입점

            - 캐시 미존재 / 역직렬화 실패 / 확률적 재계산 조건 충족 시
              → refresh 수행
            - 그렇지 않으면 캐시 데이터 그대로 반환
         */
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            // 캐시 미스 → 즉시 재계산
            return refresh(key, ttl, dataSourceSupplier);
        }

        CacheData cacheData = DataSerializer.deserializeOrNull(cached, CacheData.class);
        if (cacheData == null) {
            // 캐시 데이터 파싱 실패 → 안전하게 재계산
            return refresh(key, ttl, dataSourceSupplier);
        }

        if (cacheData.shouldRecompute(1)) {
            /*
                Probabilistic Early Recomputation 조건 충족

                - TTL 만료가 가까워질수록
                - 재계산 비용(delta)이 클수록
                - 확률적으로 일부 요청만 재계산 책임을 가짐
             */
            return refresh(key, ttl, dataSourceSupplier);
        }

        T data = cacheData.parseData(clazz);
        if (data == null) {
            // 데이터 역직렬화 실패 시 재계산
            return refresh(key, ttl, dataSourceSupplier);
        }

        // 캐시 히트 & 재계산 조건 미충족 → 즉시 반환
        return data;
    }

    private <T> T refresh(String key, Duration ttl, Supplier<T> dataSourceSupplier) {
        /*
            실제 데이터 재계산 수행

            - Data Source 조회 시간 측정
            - 측정된 시간을 delta로 함께 캐시에 저장
            - 이후 PER 확률 계산에 활용됨
         */
        long startMillis = Instant.now().toEpochMilli();
        T sourceResult = dataSourceSupplier.get();
        long computationTimeMillis = Instant.now().toEpochMilli() - startMillis;

        put(key, ttl, sourceResult, computationTimeMillis);
        return sourceResult;
    }

    private void put(String key, Duration ttl, Object data, long computationTimeMillis) {
        /*
            캐시 저장

            - 실제 데이터 + 재계산 소요 시간(delta) + 만료 시점(expiry)을 함께 저장
            - TTL은 Redis 레벨에서도 함께 설정
         */
        CacheData cacheData = CacheData.of(data, computationTimeMillis, ttl);
        redisTemplate.opsForValue()
                .set(key, DataSerializer.serializeOrException(cacheData), ttl);
    }

    @Override
    public void put(String key, Duration ttl, Object value) {
        /*
            외부 put 호출용 기본 구현

            - 명시적인 재계산 시간이 없으므로
              delta는 기본값(예: 100ms)으로 설정
         */
        put(key, ttl, value, 100);
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean supports(CacheStrategy cacheStrategy) {
        return CacheStrategy.PROBABILISTIC_EARLY_RECOMPUTATION == cacheStrategy;
    }

}
