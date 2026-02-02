package com.example.cache.service.strategy.bloomfilter;

import com.example.cache.RedisTestContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BloomFilterRedisHandlerTest extends RedisTestContainerSupport {

    @Autowired
    BloomFilterRedisHandler bloomFilterRedisHandler;

    @Test
    @DisplayName("Bloom Filter에 값을 추가하면 해시된 인덱스 위치만 true로 설정된다")
    void add() {
        /*
            - Bloom Filter에 값을 추가하면 해당 값의 해시 결과에 해당하는 비트만 1(true)로 설정되어야 한다
            - 해시되지 않은 나머지 비트들은 false 상태를 유지해야 한다
            - Redis Bitmap에 정확히 반영되었는지 전체 비트를 순회하며 검증한다
        */

        // given
        BloomFilter bloomFilter = BloomFilter.create("testId", 1000, 0.01);

        // when
        bloomFilterRedisHandler.add(bloomFilter, "value");

        // then
        List<Long> hashedIndexes = bloomFilter.hash("value");

        for (long offset = 0; offset < bloomFilter.getBitSize(); offset++) {
            Boolean result = redisTemplate.opsForValue()
                    .getBit("bloom-filter:" + bloomFilter.getId(), offset);

            // 해시된 인덱스만 true, 나머지는 false여야 한다
            assertThat(result).isEqualTo(hashedIndexes.contains(offset));
        }
    }

    @Test
    @DisplayName("Bloom Filter 삭제 시 Redis에 저장된 모든 비트가 제거된다")
    void delete() {
        /*
            - Bloom Filter 삭제는 Redis Key 자체를 제거하는 방식이다
            - 삭제 이후에는 어떤 비트 위치를 조회하더라도 false가 반환되어야 한다
            - 실제로 Redis Bitmap이 초기화되었는지 검증한다
        */

        // given
        BloomFilter bloomFilter = BloomFilter.create("testId", 1000, 0.01);
        bloomFilterRedisHandler.add(bloomFilter, "value");

        // when
        bloomFilterRedisHandler.delete(bloomFilter);

        // then
        for (long offset = 0; offset < bloomFilter.getBitSize(); offset++) {
            Boolean result = redisTemplate.opsForValue()
                    .getBit("bloom-filter:" + bloomFilter.getId(), offset);

            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("Bloom Filter에 추가된 값은 mightContain 조회 시 항상 true로 판단된다")
    void mightContain() {
        /*
            풀이

            - Bloom Filter에 실제로 추가된 값은
              mightContain 조회 시 반드시 true를 반환해야 한다
            - 이는 False Negative가 발생하지 않는다는 Bloom Filter의 핵심 특성을 검증한다
        */

        // given
        BloomFilter bloomFilter = BloomFilter.create("testId", 1000, 0.01);

        List<String> values = IntStream.range(0, 1000)
                .mapToObj(idx -> "value" + idx)
                .toList();

        for (String value : values) {
            bloomFilterRedisHandler.add(bloomFilter, value);
        }

        // when, then
        for (String value : values) {
            boolean result = bloomFilterRedisHandler.mightContain(bloomFilter, value);
            assertThat(result).isTrue();
        }

        // False Positive 발생 여부 확인 (로그 목적)
        for (int i = 0; i < 10000; i++) {
            String value = "notAddedValue" + i;
            boolean result = bloomFilterRedisHandler.mightContain(bloomFilter, value);

            if (result) {
                // Bloom Filter 특성상 발생 가능한 False Positive
                System.out.println("false positive value = " + value);
            }
        }
    }

    @Test
    @DisplayName("대용량 Bloom Filter에 값 추가 시 초기화 여부에 따른 성능 차이를 확인한다")
    void printExecutionTime_addToLargeBloomFilter() {
        /*
            - 매우 큰 Bloom Filter를 사용할 경우 Redis Bitmap이 처음 확장되는 시점에 성능 저하가 발생할 수 있다
            - 초기화 없이 바로 add 했을 때의 수행 시간을 측정한다
        */

        BloomFilter bloomFilter = BloomFilter.create("testId", 400_000_000, 0.01);
        List<Long> hashedIndexes = bloomFilter.hash("value");

        System.out.println("bloomFilter.getBitSize() = " + bloomFilter.getBitSize());
        System.out.println("hashedIndexes = " + hashedIndexes);

        long start = System.nanoTime();
        bloomFilterRedisHandler.add(bloomFilter, "value");
        long timeMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        System.out.println("timeMillis = " + timeMillis);
    }

    @Test
    @DisplayName("대용량 Bloom Filter를 사전 초기화한 경우 add 성능이 개선된다")
    void printExecutionTime_addToLargeBloomFilterAfterInit() {
        /*
            - Bloom Filter를 미리 init하여 Redis Bitmap을 확장해두면 실제 add 시점의 성능 저하를 줄일 수 있다
            - init 이후 add 수행 시간을 측정하여 비교한다
        */

        BloomFilter bloomFilter = BloomFilter.create("testId", 400_000_000, 0.01);
        List<Long> hashedIndexes = bloomFilter.hash("value");

        System.out.println("bloomFilter.getBitSize() = " + bloomFilter.getBitSize());
        System.out.println("hashedIndexes = " + hashedIndexes);

        bloomFilterRedisHandler.init(bloomFilter);

        long start = System.nanoTime();
        bloomFilterRedisHandler.add(bloomFilter, "value");
        long timeMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        System.out.println("timeMillis = " + timeMillis);
    }

    @Test
    @DisplayName("예상 데이터 수를 초과해 추가하면 False Positive 확률이 증가한다")
    void mightContain_whenBloomFilterAddedTooManyData() {
        /*
            - Bloom Filter는 예상 데이터 수(n)를 기준으로 설계된다
            - 이를 초과하여 데이터를 추가하면 False Positive 확률이 증가한다
            - False Negative는 발생하지 않음을 함께 확인한다
        */

        // given
        BloomFilter bloomFilter = BloomFilter.create("testId", 1000, 0.01);

        List<String> values = IntStream.range(0, 2000)
                .mapToObj(idx -> "value" + idx)
                .toList();

        for (String value : values) {
            bloomFilterRedisHandler.add(bloomFilter, value);
        }

        // when, then
        for (String value : values) {
            boolean result = bloomFilterRedisHandler.mightContain(bloomFilter, value);
            assertThat(result).isTrue();
        }

        for (int i = 0; i < 10000; i++) {
            String value = "notAddedValue" + i;
            boolean result = bloomFilterRedisHandler.mightContain(bloomFilter, value);

            if (result) {
                // False Positive 발생 로그
                System.out.println("false positive value = " + value);
            }
        }
    }

}
