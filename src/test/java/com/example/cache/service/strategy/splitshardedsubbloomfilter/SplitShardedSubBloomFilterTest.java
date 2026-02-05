package com.example.cache.service.strategy.splitshardedsubbloomfilter;

import com.example.cache.service.strategy.splitshardedbloomfilter.SplitShardedBloomFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SplitShardedSubBloomFilterTest {

    @Test
    @DisplayName("SplitShardedSubBloomFilter 생성 시 id가 정상적으로 설정된다")
    void create() {
        SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
                "testId", 1000, 0.01, 4
        );

        assertThat(splitShardedSubBloomFilter.getId()).isEqualTo("testId");
    }

    @Test
    @DisplayName("Sub Filter 인덱스에 따라 데이터 수와 오차율이 지수적으로 조정된다")
    void findSubFilter() {
        // given
        SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
                "testId", 1000, 0.01, 4
        );

        // when
        SplitShardedBloomFilter subFilter0 = splitShardedSubBloomFilter.findSubFilter(0);
        SplitShardedBloomFilter subFilter1 = splitShardedSubBloomFilter.findSubFilter(1);
        SplitShardedBloomFilter subFilter2 = splitShardedSubBloomFilter.findSubFilter(2);

        // then - Sub Filter index에 따라 capacity는 2배씩 증가, 오차율은 1/2씩 감소
        assertThat(subFilter0.getId()).isEqualTo("testId:sub:0");
        assertThat(subFilter0.getDataCount()).isEqualTo(2000);
        assertThat(subFilter0.getFalsePositiveRate()).isEqualTo(0.005);
        assertThat(subFilter0.getShardCount()).isEqualTo(4);

        assertThat(subFilter1.getId()).isEqualTo("testId:sub:1");
        assertThat(subFilter1.getDataCount()).isEqualTo(4000);
        assertThat(subFilter1.getFalsePositiveRate()).isEqualTo(0.0025);

        assertThat(subFilter2.getId()).isEqualTo("testId:sub:2");
        assertThat(subFilter2.getDataCount()).isEqualTo(8000);
        assertThat(subFilter2.getFalsePositiveRate()).isEqualTo(0.00125);
    }

    @Test
    @DisplayName("Sub Filter가 없으면 기본 SplitShardedBloomFilter가 활성 필터로 사용된다")
    void findActivatedFilter_shouldReturnOriginFilter_whenSubFilterNotExists() {
        // given
        SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
                "testId", 1000, 0.01, 4
        );

        // when
        SplitShardedBloomFilter activatedFilter = splitShardedSubBloomFilter.findActivatedFilter(0);

        // then
        assertThat(activatedFilter.getId())
                .isEqualTo(splitShardedSubBloomFilter.getSplitShardedBloomFilter().getId());
    }

    @Test
    @DisplayName("Sub Filter가 존재하면 가장 마지막 Sub Filter가 활성 필터로 선택된다")
    void findActivatedFilter_shouldReturnSubFilter_whenSubFilterExists() {
        // given
        SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
                "testId", 1000, 0.01, 4
        );

        // when
        SplitShardedBloomFilter activatedFilter =
                splitShardedSubBloomFilter.findActivatedFilter(3);

        // then
        assertThat(activatedFilter.getId()).isEqualTo("testId:sub:2");
    }

    @Test
    @DisplayName("기본 필터 + 모든 Sub Filter를 순서대로 반환한다")
    void findAll() {
        // given
        SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
                "testId", 1000, 0.01, 4
        );

        // when
        List<SplitShardedBloomFilter> filters =
                splitShardedSubBloomFilter.findAll(3);

        // then
        // 기본 필터 + Sub Filter 개수
        assertThat(filters).hasSize(4);
        assertThat(filters.getFirst().getId())
                .isEqualTo(splitShardedSubBloomFilter.getSplitShardedBloomFilter().getId());

        // Sub Filter는 index 순서대로 포함된다
        for (int i = 0; i < 3; i++) {
            assertThat(filters.get(i + 1).getId())
                    .isEqualTo("testId:sub:" + i);
        }
    }

}
