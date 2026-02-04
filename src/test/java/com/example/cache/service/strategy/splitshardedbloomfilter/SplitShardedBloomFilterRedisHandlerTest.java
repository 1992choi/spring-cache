package com.example.cache.service.strategy.splitshardedbloomfilter;

import com.example.cache.RedisTestContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SplitShardedBloomFilterRedisHandlerTest extends RedisTestContainerSupport {

    @Autowired
    SplitShardedBloomFilterRedisHandler splitShardedBloomFilterRedisHandler;

    @Test
    @DisplayName("Split + Sharding 구조에서 추가된 데이터는 항상 true로 조회되는지 확인")
    void mightContain() {
        // given
        // 4개의 Shard로 분산된 SplitShardedBloomFilter 생성
        SplitShardedBloomFilter splitShardedBloomFilter = SplitShardedBloomFilter.create(
                "testId", 1000, 0.01, 4
        );

        // Bloom Filter에 추가할 실제 데이터 1,000개
        List<String> values = IntStream.range(0, 1000)
                .mapToObj(idx -> "value" + idx)
                .toList();

        // 모든 값을 Bloom Filter에 추가
        for (String value : values) {
            splitShardedBloomFilterRedisHandler.add(splitShardedBloomFilter, value);
        }

        // when, then
        // 추가된 데이터는 Shard → Split Bloom Filter를 거쳐 항상 true여야 함
        for (String value : values) {
            boolean result = splitShardedBloomFilterRedisHandler.mightContain(splitShardedBloomFilter, value);
            assertThat(result).isTrue();
        }

        // 추가되지 않은 데이터 조회
        // false positive가 발생할 수 있으므로 결과 자체는 검증하지 않고
        // 발생 여부만 로그로 확인
        for (int i = 0; i < 10000; i++) {
            String value = "notAddedValue" + i;
            boolean result = splitShardedBloomFilterRedisHandler.mightContain(splitShardedBloomFilter, value);
            if (result) {
                // Bloom Filter 특성상 허용되는 false positive
                System.out.println("value = " + value);
            }
        }
    }

}
