package com.example.cache.service.strategy.splitshardedsubbloomfilter;

import com.example.cache.common.distributedlock.DistributedLockProvider;
import com.example.cache.service.strategy.splitshardedbloomfilter.SplitShardedBloomFilter;
import com.example.cache.service.strategy.splitshardedbloomfilter.SplitShardedBloomFilterRedisHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SplitShardedSubBloomFilterRedisHandler {

    // Redis 접근용 템플릿
    private final StringRedisTemplate redisTemplate;

    // 서브 필터 추가 시 동시성 제어를 위한 분산 락
    private final DistributedLockProvider distributedLockProvider;

    // 실제 BloomFilter Redis 처리 핸들러
    private final SplitShardedBloomFilterRedisHandler splitShardedBloomFilterRedisHandler;

    // 생성 가능한 최대 서브 필터 개수
    public static final int MAX_SUB_FILTER_COUNT = 2;

    /**
     * 메인 SplitShardedBloomFilter 초기화
     */
    public void init(SplitShardedSubBloomFilter splitShardedSubBloomFilter) {
        SplitShardedBloomFilter splitShardedBloomFilter =
                splitShardedSubBloomFilter.getSplitShardedBloomFilter();
        splitShardedBloomFilterRedisHandler.init(splitShardedBloomFilter);
    }

    /**
     * 값 추가
     * - 현재 활성화된 서브 필터에 데이터 저장
     * - 데이터가 가득 찼다면 신규 서브 필터 생성 시도
     */
    public void add(SplitShardedSubBloomFilter splitShardedSubBloomFilter, String value) {
        int subFilterCount = findSubFilterCount(splitShardedSubBloomFilter);
        SplitShardedBloomFilter activated =
                splitShardedSubBloomFilter.findActivatedFilter(subFilterCount);

        splitShardedBloomFilterRedisHandler.add(activated, value);

        // 데이터 개수 증가
        Long dataCount = redisTemplate.opsForValue()
                .increment(genDataCountKey(activated));

        // 필터가 가득 찼으면 서브 필터 추가
        appendSubFilterIfFull(splitShardedSubBloomFilter, activated, dataCount);
    }

    /**
     * 현재 생성된 서브 필터 개수 조회
     */
    private int findSubFilterCount(SplitShardedSubBloomFilter splitShardedSubBloomFilter) {
        String result = redisTemplate.opsForValue()
                .get(genSubFilterCountKey(splitShardedSubBloomFilter));

        if (!StringUtils.hasText(result)) {
            return 0;
        }
        return Integer.parseInt(result);
    }

    /**
     * 활성화된 필터가 가득 찼을 경우
     * - 분산 락을 획득한 뒤 신규 서브 필터를 생성
     */
    private void appendSubFilterIfFull(
            SplitShardedSubBloomFilter splitShardedSubBloomFilter,
            SplitShardedBloomFilter activated,
            Long dataCount
    ) {
        if (!isFull(activated, dataCount)) {
            return;
        }

        String distributedLockKey =
                genSubFilterCountDistributedLockKey(splitShardedSubBloomFilter);

        if (!distributedLockProvider.lock(distributedLockKey, Duration.ofMinutes(1))) {
            return;
        }

        try {
            int subFilterCount = findSubFilterCount(splitShardedSubBloomFilter);

            // 서브 필터 최대 개수 초과 방지
            if (subFilterCount >= MAX_SUB_FILTER_COUNT) {
                log.warn("sub-filter limit reached. id = {}, subFilterCount = {}",
                        splitShardedSubBloomFilter.getId(), subFilterCount);
                return;
            }

            // 신규 서브 필터 초기화
            splitShardedBloomFilterRedisHandler.init(
                    splitShardedSubBloomFilter.findSubFilter(subFilterCount)
            );

            // 서브 필터 개수 증가
            redisTemplate.opsForValue()
                    .increment(genSubFilterCountKey(splitShardedSubBloomFilter));
        } finally {
            distributedLockProvider.unlock(distributedLockKey);
        }
    }

    /**
     * BloomFilter 용량 초과 여부 판단
     */
    private boolean isFull(SplitShardedBloomFilter activated, Long dataCount) {
        return dataCount != null && activated.getDataCount() <= dataCount;
    }

    /**
     * 모든 서브 필터를 대상으로 포함 여부 확인
     */
    public boolean mightContain(
            SplitShardedSubBloomFilter splitShardedSubBloomFilter,
            String value
    ) {
        int subFilterCount = findSubFilterCount(splitShardedSubBloomFilter);

        return splitShardedSubBloomFilter.findAll(subFilterCount).stream()
                .anyMatch(splitShardedBloomFilter ->
                        splitShardedBloomFilterRedisHandler
                                .mightContain(splitShardedBloomFilter, value));
    }

    /**
     * 모든 서브 필터 및 관련 메타 데이터 삭제
     */
    public void delete(SplitShardedSubBloomFilter splitShardedSubBloomFilter) {
        int subFilterCount = findSubFilterCount(splitShardedSubBloomFilter);
        List<SplitShardedBloomFilter> filters =
                splitShardedSubBloomFilter.findAll(subFilterCount);

        for (SplitShardedBloomFilter filter : filters) {
            splitShardedBloomFilterRedisHandler.delete(filter);
            redisTemplate.delete(genDataCountKey(filter));
        }

        redisTemplate.delete(genSubFilterCountKey(splitShardedSubBloomFilter));
    }

    /**
     * 서브 필터별 데이터 개수 저장 키
     */
    private String genDataCountKey(SplitShardedBloomFilter splitShardedBloomFilter) {
        return "split-sharded-sub-bloom-filter:data-count:%s"
                .formatted(splitShardedBloomFilter.getId());
    }

    /**
     * 현재 서브 필터 개수 저장 키
     */
    private String genSubFilterCountKey(SplitShardedSubBloomFilter splitShardedSubBloomFilter) {
        return "split-sharded-sub-bloom-filter:sub-filter-count:%s"
                .formatted(splitShardedSubBloomFilter.getId());
    }

    /**
     * 서브 필터 증가 시 사용되는 분산 락 키
     */
    private String genSubFilterCountDistributedLockKey(
            SplitShardedSubBloomFilter splitShardedSubBloomFilter
    ) {
        return "split-sharded-sub-bloom-filter:sub-filter-count:%s:distributed-lock"
                .formatted(splitShardedSubBloomFilter.getId());
    }

}
