package com.example.cache.service.strategy.splitshardedsubbloomfilter;

import com.example.cache.service.strategy.splitshardedbloomfilter.SplitShardedBloomFilter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SplitShardedSubBloomFilter {

    // 기준이 되는 Bloom Filter (Sub Filter가 없을 때 사용)
    private String id;
    private SplitShardedBloomFilter splitShardedBloomFilter;

    // 최초 기준 Bloom Filter 생성
    public static SplitShardedSubBloomFilter create(
            String id,
            long dataCount,
            double falsePositiveRate,
            int shardCount
    ) {
        SplitShardedSubBloomFilter splitShardedSubBloomFilter = new SplitShardedSubBloomFilter();
        splitShardedSubBloomFilter.id = id;
        splitShardedSubBloomFilter.splitShardedBloomFilter =
                SplitShardedBloomFilter.create(id, dataCount, falsePositiveRate, shardCount);
        return splitShardedSubBloomFilter;
    }

    // subFilterIndex에 해당하는 Sub Filter 생성
    // - 데이터 수용량은 2배씩 증가
    // - 오차율은 1/2씩 감소
    public SplitShardedBloomFilter findSubFilter(int subFilterIndex) {
        return SplitShardedBloomFilter.create(
                id + ":sub:" + subFilterIndex,
                splitShardedBloomFilter.getDataCount() * (1L << (subFilterIndex + 1)),
                splitShardedBloomFilter.getFalsePositiveRate() / (1L << (subFilterIndex + 1)),
                splitShardedBloomFilter.getShardCount()
        );
    }

    // 현재 데이터가 입력되는 활성 Bloom Filter 반환
    // Sub Filter가 없다면 기본 Bloom Filter 사용
    public SplitShardedBloomFilter findActivatedFilter(int subFilterCount) {
        if (subFilterCount == 0) {
            return splitShardedBloomFilter;
        }
        return findSubFilter(subFilterCount - 1);
    }

    // 조회 시 사용되는 전체 Bloom Filter 목록 반환
    // 기본 Bloom Filter + 모든 Sub Filter
    public List<SplitShardedBloomFilter> findAll(int subFilterCount) {
        List<SplitShardedBloomFilter> splitShardedBloomFilters = new ArrayList<>();
        splitShardedBloomFilters.add(splitShardedBloomFilter);

        for (int subFilterIndex = 0; subFilterIndex < subFilterCount; subFilterIndex++) {
            splitShardedBloomFilters.add(findSubFilter(subFilterIndex));
        }
        return splitShardedBloomFilters;
    }

}
