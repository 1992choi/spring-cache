package com.example.cache.service.strategy.splitbloomfilter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.LongStream;

@Component
@RequiredArgsConstructor
public class SplitBloomFilterRedisHandler {

    private final StringRedisTemplate redisTemplate;

    /**
     * Split Bloom Filter 초기화
     * <p>
     * - Split 개수만큼 Redis Key를 생성
     * - 각 Split의 비트맵 크기만큼 메모리를 미리 할당
     * - Redis는 setBit 시점에 메모리를 할당하므로,
     * 대용량 Bloom Filter의 경우 지연을 줄이기 위해 사전 초기화가 필요
     */
    public void init(SplitBloomFilter splitBloomFilter) {
        for (long splitIndex = 0; splitIndex < splitBloomFilter.getSplitCount(); splitIndex++) {
            String key = genKey(splitBloomFilter, splitIndex);

            // 해당 Split이 담당하는 비트 수 계산
            long bitSize = splitBloomFilter.calSplitBitSize(splitIndex);

            /*
             * Redis BITMAP은 offset 접근 시 메모리를 할당하므로
             * 한 번에 큰 offset을 설정하면 blocking 가능성 있음
             * → 8MB 단위로 나누어 점진적으로 메모리 할당
             */
            for (long offset = 0; offset < bitSize; offset += 8L * 1024 * 1024 * 8 /* 8MB */) {
                redisTemplate.opsForValue().setBit(key, offset, false);
            }
        }
    }

    /**
     * 데이터 추가
     * <p>
     * - Bloom Filter 해시 결과를 기반으로
     * - 각 해시 인덱스가 속한 Split을 계산
     * - Split 내부에서의 상대 offset으로 비트를 설정
     */
    public void add(SplitBloomFilter splitBloomFilter, String value) {
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection conn = (StringRedisConnection) action;

            // Bloom Filter의 k개 해시 결과
            List<Long> hashedIndexes = splitBloomFilter.getBloomFilter().hash(value);

            for (Long hashedIndex : hashedIndexes) {
                // 전체 비트 인덱스가 속한 Split 계산
                long splitIndex = splitBloomFilter.findSplitIndex(hashedIndex);

                // Split 내부에서의 실제 비트 위치 계산
                conn.setBit(
                        genKey(splitBloomFilter, splitIndex),
                        hashedIndex % SplitBloomFilter.BIT_SPLIT_UNIT,
                        true
                );
            }
            return null;
        });
    }

    /**
     * 데이터 존재 가능성 조회
     * <p>
     * - 모든 해시 결과에 대해 비트가 1이면 "있을 수도 있음"
     * - 하나라도 0이면 "확실히 없음"
     * - False Positive는 가능하지만 False Negative는 발생하지 않음
     */
    public boolean mightContain(SplitBloomFilter splitBloomFilter, String value) {
        return redisTemplate.executePipelined((RedisCallback<?>) action -> {
                    StringRedisConnection conn = (StringRedisConnection) action;

                    // Bloom Filter 해시 결과 조회
                    List<Long> hashedIndexes = splitBloomFilter.getBloomFilter().hash(value);

                    for (Long hashedIndex : hashedIndexes) {
                        long splitIndex = splitBloomFilter.findSplitIndex(hashedIndex);

                        // 해당 Split의 비트 값 조회
                        conn.getBit(
                                genKey(splitBloomFilter, splitIndex),
                                hashedIndex % SplitBloomFilter.BIT_SPLIT_UNIT
                        );
                    }
                    return null;
                })
                .stream()
                .map(Boolean.class::cast)
                // 모든 비트가 true일 때만 존재 가능성 있음
                .allMatch(Boolean.TRUE::equals);
    }

    /**
     * Split Bloom Filter 삭제
     * <p>
     * - Split 단위로 생성된 모든 Redis Key를 삭제
     */
    public void delete(SplitBloomFilter splitBloomFilter) {
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection conn = (StringRedisConnection) action;

            // Split 개수만큼 생성된 모든 Key 삭제
            genKeys(splitBloomFilter).forEach(conn::del);
            return null;
        });
    }

    /**
     * Split 개수만큼 Redis Key 목록 생성
     */
    private List<String> genKeys(SplitBloomFilter splitBloomFilter) {
        return LongStream.range(0, splitBloomFilter.getSplitCount())
                .mapToObj(splitIndex -> genKey(splitBloomFilter, splitIndex))
                .toList();
    }

    /**
     * Split Bloom Filter Redis Key 생성 규칙
     * <p>
     * 예)
     * split-bloom-filter:item:split:0
     * split-bloom-filter:item:split:1
     */
    private String genKey(SplitBloomFilter splitBloomFilter, long splitIndex) {
        return "split-bloom-filter:%s:split:%s"
                .formatted(splitBloomFilter.getId(), splitIndex);
    }

}
