package com.example.cache.service.strategy.splitbloomfilter;

import com.example.cache.service.strategy.bloomfilter.BloomFilter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SplitBloomFilter {

    private String id;
    private BloomFilter bloomFilter;
    private long splitCount;

    //	public static final long BIT_SPLIT_UNIT = 1L << 32; // 2^32 비트맵의 최대 크기로 설정해야하지만, 테스트를 고려해 아래와 같이 낮은 값으로 설정
    public static final long BIT_SPLIT_UNIT = 1L << 10; // 2^10 == 1024

    public static SplitBloomFilter create(String id, long dataCount, double falsePositiveRate) {
        BloomFilter bloomFilter = BloomFilter.create(id, dataCount, falsePositiveRate);

        /*
            splitCount 개수 구하는 예시
            예시) 비트 사이즈가 1024라면? (1024 - 1) / 1024 + 1 == 1개의 split
            예시) 비트 사이즈가 1025라면? (1025 - 1) / 1024 + 1 == 2개의 split
         */
        long splitCount = (bloomFilter.getBitSize() - 1) / BIT_SPLIT_UNIT + 1;

        SplitBloomFilter splitBloomFilter = new SplitBloomFilter();
        splitBloomFilter.id = id;
        splitBloomFilter.bloomFilter = bloomFilter;
        splitBloomFilter.splitCount = splitCount;
        return splitBloomFilter;
    }

    public long findSplitIndex(Long hashedIndex) {
        if (hashedIndex >= bloomFilter.getBitSize()) {
            throw new IllegalArgumentException("hashedIndex out of bounds");
        }

        return hashedIndex / BIT_SPLIT_UNIT;
    }

    public long calSplitBitSize(long splitIndex) {
        if (splitIndex == splitCount - 1) {
            return bloomFilter.getBitSize() - (BIT_SPLIT_UNIT * splitIndex);
        }

        return BIT_SPLIT_UNIT;
    }
}
