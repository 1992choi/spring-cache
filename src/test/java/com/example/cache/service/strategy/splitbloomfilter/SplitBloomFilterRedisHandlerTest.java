package com.example.cache.service.strategy.splitbloomfilter;

import com.example.cache.RedisTestContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SplitBloomFilterRedisHandlerTest extends RedisTestContainerSupport {

    @Autowired
    SplitBloomFilterRedisHandler splitBloomFilterRedisHandler;

    @Test
    @DisplayName("추가된 값은 반드시 mightContain=true 를 반환해야 한다")
    void mightContain() {
        // given
        // 데이터 수 1000, 오차율 1%의 Split Bloom Filter 생성
        SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1000, 0.01);

        // Bloom Filter에 추가할 값들
        List<String> values = IntStream.range(0, 1000)
                .mapToObj(idx -> "value" + idx)
                .toList();

        // 모든 값 Bloom Filter에 추가
        for (String value : values) {
            splitBloomFilterRedisHandler.add(splitBloomFilter, value);
        }

        // when & then
        // 추가한 값들은 반드시 true여야 함 (False Negative 발생 X)
        for (String value : values) {
            boolean result = splitBloomFilterRedisHandler.mightContain(splitBloomFilter, value);
            assertThat(result).isTrue();
        }

        // 추가하지 않은 값들에 대해서는 False Positive가 발생할 수 있음 (출력되는 양으로 오차율 확인하는 용도)
        for (int i = 0; i < 10000; i++) {
            String value = "notAddedValue" + i;
            boolean result = splitBloomFilterRedisHandler.mightContain(splitBloomFilter, value);

            if (result) {
                // False Positive 케이스 출력 (테스트 실패 아님)
                System.out.println("false positive value = " + value);
            }
        }
    }

    @Test
    @DisplayName("init 호출 시 Split 개수만큼 Redis Bitmap Key가 초기화된다")
    void init() {
        // given
        SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1_000L, 0.01);

        // init 이전에는 Redis Key가 존재하지 않아야 함
        for (int splitIndex = 0; splitIndex < splitBloomFilter.getSplitCount(); splitIndex++) {
            String result = redisTemplate.opsForValue().get(
                    "split-bloom-filter:%s:split:%s"
                            .formatted(splitBloomFilter.getId(), splitIndex)
            );
            assertThat(result).isNull();
        }

        // when
        splitBloomFilterRedisHandler.init(splitBloomFilter);

        // then
        // init 이후에는 모든 Split Key가 생성되어 있어야 함
        for (int splitIndex = 0; splitIndex < splitBloomFilter.getSplitCount(); splitIndex++) {
            String result = redisTemplate.opsForValue().get(
                    "split-bloom-filter:%s:split:%s"
                            .formatted(splitBloomFilter.getId(), splitIndex)
            );
            assertThat(result).isNotNull();
        }
    }

    @Test
    @DisplayName("add 호출 시 해시된 비트 위치만 true로 설정된다")
    void add() {
        // given
        SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1_000L, 0.01);

        // when
        splitBloomFilterRedisHandler.add(splitBloomFilter, "value");

        // then
        // Bloom Filter 해시 결과
        List<Long> hashedIndexes = splitBloomFilter.getBloomFilter().hash("value");

        // 전체 비트 영역을 순회하며
        // 해시된 위치만 true인지 검증
        for (long offset = 0; offset < splitBloomFilter.getBloomFilter().getBitSize(); offset++) {
            long splitIndex = splitBloomFilter.findSplitIndex(offset);

            Boolean result = redisTemplate.opsForValue().getBit(
                    "split-bloom-filter:%s:split:%s"
                            .formatted(splitBloomFilter.getId(), splitIndex),
                    offset % SplitBloomFilter.BIT_SPLIT_UNIT
            );

            assertThat(result).isEqualTo(hashedIndexes.contains(offset));
        }
    }

    @Test
    @DisplayName("delete 호출 시 모든 Split Bloom Filter Key가 삭제된다")
    void delete() {
        // given
        SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1_000L, 0.01);
        splitBloomFilterRedisHandler.add(splitBloomFilter, "value");

        // when
        splitBloomFilterRedisHandler.delete(splitBloomFilter);

        // then
        // 모든 비트 조회 결과는 false (또는 Key 미존재)여야 함
        for (long offset = 0; offset < splitBloomFilter.getBloomFilter().getBitSize(); offset++) {
            long splitIndex = splitBloomFilter.findSplitIndex(offset);

            Boolean result = redisTemplate.opsForValue().getBit(
                    "split-bloom-filter:%s:split:%s"
                            .formatted(splitBloomFilter.getId(), splitIndex),
                    offset
            );

            assertThat(result).isFalse();
        }
    }

}
