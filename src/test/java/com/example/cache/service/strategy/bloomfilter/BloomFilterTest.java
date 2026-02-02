package com.example.cache.service.strategy.bloomfilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BloomFilterTest {

    @Test
    @DisplayName("입력값(n, p)에 따라 Bloom Filter 파라미터(m, k)가 계산식에 맞게 생성된다")
    void create() {
        /*
            풀이

            - Bloom Filter 생성 시 입력값으로 데이터 수(n)와 오차율(p)을 받는다
            - 이를 기반으로 비트 배열 크기(m)와 해시 함수 개수(k)를 계산한다
            - 계산 결과가 이론 공식에 맞게 설정되었는지 검증한다
        */
        BloomFilter bloomFilter1 = BloomFilter.create("testId1", 1000, 0.01);

        assertThat(bloomFilter1.getId()).isEqualTo("testId1");
        assertThat(bloomFilter1.getDataCount()).isEqualTo(1000);
        assertThat(bloomFilter1.getFalsePositiveRate()).isEqualTo(0.01);
        assertThat(bloomFilter1.getBitSize()).isEqualTo(9586);
        assertThat(bloomFilter1.getHashFunctionCount()).isEqualTo(7);

        System.out.println("bloomFilter1 = " + bloomFilter1);

        BloomFilter bloomFilter2 = BloomFilter.create("testId2", 100_000_000, 0.01);

        assertThat(bloomFilter2.getId()).isEqualTo("testId2");
        assertThat(bloomFilter2.getDataCount()).isEqualTo(100_000_000);
        assertThat(bloomFilter2.getFalsePositiveRate()).isEqualTo(0.01);
        assertThat(bloomFilter2.getBitSize()).isEqualTo(958_505_838);
        assertThat(bloomFilter2.getHashFunctionCount()).isEqualTo(7);

        System.out.println("bloomFilter2 = " + bloomFilter2);
    }

    @Test
    @DisplayName("하나의 값 입력 시 해시 함수 개수(k)만큼 유효한 비트 인덱스가 생성된다")
    void hash() {
        /*
            풀이

            - Bloom Filter는 하나의 값에 대해 여러 개의 해시 함수를 적용한다
            - 각 해시 함수는 비트 배열 범위 내의 인덱스를 반환해야 한다
            - 반환된 인덱스 개수는 해시 함수 개수(k)와 동일해야 한다
            - 반복 입력에도 항상 유효한 인덱스를 생성하는지 검증한다
        */
        BloomFilter bloomFilter = BloomFilter.create("testId", 1000, 0.01);

        for (int i = 0; i < 100; i++) {
            List<Long> hashedIndexes = bloomFilter.hash("value" + i);

            // 해시 함수 개수만큼 인덱스가 생성되는지 확인
            assertThat(hashedIndexes.size())
                    .isEqualTo(bloomFilter.getHashFunctionCount());

            // 모든 인덱스가 비트 배열 범위 내에 있는지 확인
            for (Long hashedIndex : hashedIndexes) {
                assertThat(hashedIndex).isGreaterThanOrEqualTo(0);
                assertThat(hashedIndex).isLessThan(bloomFilter.getBitSize());
                System.out.println("hashedIndex = " + hashedIndex);
            }

            System.out.println();
        }
    }

}