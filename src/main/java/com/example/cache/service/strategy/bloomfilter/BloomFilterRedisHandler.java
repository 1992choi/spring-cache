package com.example.cache.service.strategy.bloomfilter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BloomFilterRedisHandler {

    private final StringRedisTemplate redisTemplate;

    /**
     * 최초에 큰 메모리를 할당하려면 블로킹이 생길 수 있으며, 싱글 스레드 특성상 다른 연산에 지연이 생길 수도 있다.
     * BloomFilter 활성화 전에 내부 관리 도구에서 점차 메모리를 늘려가는 전략으로 미리 필요한 만큼 할당해둘 수 있다.
     *
     * printExecutionTime_addToLargeBloomFilter와 printExecutionTime_addToLargeBloomFilterAfterInit의 속도 차이로 init()의 사용용도를 명확하게 알 수 있음
     */
    public void init(BloomFilter bloomFilter) {
        String key = genKey(bloomFilter);
        for (long offset = 0; offset < bloomFilter.getBitSize(); offset += 8L * 1024 * 1024 * 8 /* 8MB */) {
            redisTemplate.opsForValue().setBit(key, offset, false);
        }
    }

    public void add(BloomFilter bloomFilter, String value) {
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection conn = (StringRedisConnection) action;
            String key = genKey(bloomFilter);
            List<Long> hashedIndexes = bloomFilter.hash(value);
            for (Long hashedIndex : hashedIndexes) {
                conn.setBit(key, hashedIndex, true);
            }
            return null;
        });
    }

    public boolean mightContain(BloomFilter bloomFilter, String value) {
        return redisTemplate.executePipelined((RedisCallback<?>) action -> {
                    StringRedisConnection conn = (StringRedisConnection) action;
                    String key = genKey(bloomFilter);
                    List<Long> hashedIndexes = bloomFilter.hash(value);
                    for (Long hashedIndex : hashedIndexes) {
                        conn.getBit(key, hashedIndex);
                    }
                    return null;
                })
                .stream()
                .map(Boolean.class::cast)
                .allMatch(Boolean.TRUE::equals);
    }

    public void delete(BloomFilter bloomFilter) {
        redisTemplate.delete(genKey(bloomFilter));
    }

    private String genKey(BloomFilter bloomFilter) {
        return genKey(bloomFilter.getId());
    }

    private String genKey(String id) {
        return "bloom-filter:%s".formatted(id);
    }
}
