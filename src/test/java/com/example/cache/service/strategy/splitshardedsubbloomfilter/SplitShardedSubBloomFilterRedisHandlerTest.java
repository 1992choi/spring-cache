package com.example.cache.service.strategy.splitshardedsubbloomfilter;

import com.example.cache.RedisTestContainerSupport;
import com.example.cache.service.strategy.splitshardedbloomfilter.SplitShardedBloomFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SplitShardedSubBloomFilterRedisHandlerTest extends RedisTestContainerSupport {

    @Autowired
    SplitShardedSubBloomFilterRedisHandler handler;

    @Test
    @DisplayName("값을 추가하면 활성화된 서브 필터에 데이터가 저장된다")
    void add() {
        // given: 빈 SplitShardedSubBloomFilter
        SplitShardedSubBloomFilter splitShardedSubBloomFilter =
                SplitShardedSubBloomFilter.create("testId", 1000, 0.01, 4);

        // when: 값 추가
        handler.add(splitShardedSubBloomFilter, "value");

        // then: 서브 필터는 아직 추가되지 않고, 데이터 개수만 증가
        assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(0);
        assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(0)))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("필터가 가득 차면 새로운 서브 필터가 추가된다")
    void add_shouldAddSubFilter_whenFilterIsFull() {
        // given: 첫 번째 서브 필터가 가득 차기 직전 상태
        SplitShardedSubBloomFilter splitShardedSubBloomFilter =
                SplitShardedSubBloomFilter.create("testId", 1000, 0.01, 4);

        int count = 1000 - 1;
        for (int i = 0; i < count; i++) {
            handler.add(splitShardedSubBloomFilter, "value" + i);
        }

        assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(0);
        assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(0)))
                .isEqualTo(999);

        // when: 한 건을 더 추가하여 필터가 가득 참
        handler.add(splitShardedSubBloomFilter, "value1000");

        // then: 새로운 서브 필터가 생성되고 활성화됨
        assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(1);
        assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(1)))
                .isEqualTo(0);
    }

    @Test
    @DisplayName("서브 필터 개수가 최대치에 도달하면 더 이상 추가되지 않는다")
    void add_shouldNotAddSubFilter_whenSubFilterCountReachesMaxLimit() {
        // given: 최대 서브 필터 개수까지 모두 채운 상태
        SplitShardedSubBloomFilter splitShardedSubBloomFilter =
                SplitShardedSubBloomFilter.create("testId", 1000, 0.01, 4);

        int count = 1000 + 2000 + 4000 - 1;
        for (int i = 0; i < count; i++) {
            handler.add(splitShardedSubBloomFilter, "value" + i);
        }

        assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(2);
        assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(2)))
                .isEqualTo(3999);

        // when: 최대 개수를 초과하는 데이터 추가
        handler.add(splitShardedSubBloomFilter, "new value");

        // then: 서브 필터는 추가되지 않고, 마지막 필터에만 데이터가 추가됨
        assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(2);
        assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(2)))
                .isEqualTo(4000);
    }

    @Test
    @DisplayName("추가된 값은 모든 서브 필터를 대상으로 조회된다")
    void mightContain() {
        // given: 여러 서브 필터에 걸쳐 값이 저장된 상태
        SplitShardedSubBloomFilter splitShardedSubBloomFilter =
                SplitShardedSubBloomFilter.create("testId", 1000, 0.01, 4);

        List<String> values = IntStream.range(0, 3000)
                .mapToObj(idx -> "value" + idx)
                .toList();

        for (String value : values) {
            handler.add(splitShardedSubBloomFilter, value);
        }

        // when & then: 추가된 값은 반드시 true
        for (String value : values) {
            assertThat(handler.mightContain(splitShardedSubBloomFilter, value))
                    .isTrue();
        }

        // when: 추가하지 않은 값 조회
        // then: false 또는 false-positive 가능
        for (int i = 0; i < 1000; i++) {
            String value = "notAddedValue" + i;
            boolean result = handler.mightContain(splitShardedSubBloomFilter, value);
            if (result) {
                // BloomFilter 특성상 false positive 허용
                System.out.println("false positive value = " + value);
            }
        }
    }

    @Test
    @DisplayName("SplitShardedSubBloomFilter 초기화 시 예외 없이 수행된다")
    void init() {
        // given
        SplitShardedSubBloomFilter splitShardedSubBloomFilter =
                SplitShardedSubBloomFilter.create("testId", 1_000, 0.01, 4);

        // when & then
        handler.init(splitShardedSubBloomFilter);
    }

    @Test
    @DisplayName("삭제 시 모든 서브 필터와 메타 데이터가 제거된다")
    void delete() {
        // given: 데이터가 하나 이상 저장된 상태
        SplitShardedSubBloomFilter splitShardedSubBloomFilter =
                SplitShardedSubBloomFilter.create("testId", 1_000, 0.01, 4);
        handler.add(splitShardedSubBloomFilter, "value");

        // when: 삭제 수행
        handler.delete(splitShardedSubBloomFilter);

        // then: 서브 필터 개수 및 데이터 카운트 초기화
        assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(0);
        assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(0)))
                .isEqualTo(0);
    }

    /**
     * Redis에 저장된 서브 필터별 데이터 개수 조회
     */
    private long getDataCount(SplitShardedBloomFilter splitShardedBloomFilter) {
        String result = redisTemplate.opsForValue().get(
                "split-sharded-sub-bloom-filter:data-count:%s"
                        .formatted(splitShardedBloomFilter.getId())
        );
        return result == null ? 0 : Long.parseLong(result);
    }

    /**
     * 현재 생성된 서브 필터 개수 조회
     */
    private int getSubFilterCount(SplitShardedSubBloomFilter splitShardedSubBloomFilter) {
        String result = redisTemplate.opsForValue().get(
                "split-sharded-sub-bloom-filter:sub-filter-count:%s"
                        .formatted(splitShardedSubBloomFilter.getId())
        );
        return result == null ? 0 : Integer.parseInt(result);
    }

}
