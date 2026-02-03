package com.example.cache.service.strategy.splitbloomfilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitBloomFilterTest {

    @Test
    @DisplayName("Bloom Filter bitSize를 기준으로 split 개수가 올바르게 계산되는지 검증")
    void create() {
        // given
        // dataCount=1000, falsePositiveRate=0.01인 Bloom Filter 생성
        // 내부 BloomFilter의 bitSize는 약 9586
        SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1000, 0.01);

        // when
        long bitSize = splitBloomFilter.getBloomFilter().getBitSize();
        long splitCount = splitBloomFilter.getSplitCount();

        // then
        // BIT_SPLIT_UNIT = 1024 기준으로
        // (9586 - 1) / 1024 + 1 = 10
        assertThat(splitCount).isEqualTo(10);
    }

    @Test
    @DisplayName("hashedIndex 값에 따라 올바른 split index가 계산되는지 검증")
    void findSplitIndex() {
        // given
        SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1000, 0.01);

        // when, then
        // 첫 번째 split 범위 [0 ~ 1023]
        assertThat(splitBloomFilter.findSplitIndex(0L)).isEqualTo(0);
        assertThat(splitBloomFilter.findSplitIndex(1023L)).isEqualTo(0);

        // 두 번째 split 시작
        assertThat(splitBloomFilter.findSplitIndex(1024L)).isEqualTo(1);

        // 마지막 split 영역
        assertThat(splitBloomFilter.findSplitIndex(9585L)).isEqualTo(9);
    }

    @Test
    @DisplayName("hashedIndex가 Bloom Filter bitSize를 초과하면 예외가 발생하는지 검증")
    void findSplitIndex_shouldThrowException_whenHashedIndexExceedsBitSize() {
        // given
        SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1000, 0.01);

        // when, then
        // bitSize는 9586이므로, 9586 이상은 유효하지 않은 인덱스. (9586 도출 : dataCount=1000, falsePositiveRate=0.01 기준, Bloom Filter 공식에 따라 계산된 bitSize = 9586)
        assertThatThrownBy(() -> splitBloomFilter.findSplitIndex(9586L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("각 split의 비트 크기가 올바르게 계산되는지 검증")
    void calSplitBitSize() {
        // given
        SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1000, 0.01);
        long splitCount = splitBloomFilter.getSplitCount();

        // when, then
        // 마지막 split을 제외한 모든 split은 BIT_SPLIT_UNIT 크기를 가져야 함
        for (long splitIndex = 0; splitIndex < splitCount - 1; splitIndex++) {
            assertThat(splitBloomFilter.calSplitBitSize(splitIndex))
                    .isEqualTo(SplitBloomFilter.BIT_SPLIT_UNIT);
        }

        // 마지막 split은 전체 bitSize에서 앞선 split들의 크기를 제외한 나머지
        long bitSize = splitBloomFilter.getBloomFilter().getBitSize();
        assertThat(splitBloomFilter.calSplitBitSize(splitCount - 1))
                .isEqualTo(bitSize - SplitBloomFilter.BIT_SPLIT_UNIT * (splitCount - 1));
    }

}
