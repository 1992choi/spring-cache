package com.example.cache.service.strategy.splitshardedbloomfilter;

import com.example.cache.service.strategy.splitbloomfilter.SplitBloomFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SplitShardedBloomFilterTest {

    @Test
    @DisplayName("Sharding 생성 시 전체 데이터 수가 각 Shard에 올바르게 분배되는지 확인")
    void create() {
        // given
        // dataCount = 1000, shardCount = 4인 SplitShardedBloomFilter 생성
        SplitShardedBloomFilter splitShardedBloomFilter = SplitShardedBloomFilter.create(
                "testId", 1000, 0.01, 4
        );

        // when
        // 각 Shard(SplitBloomFilter)가 관리하는 데이터 수를 모두 합산
        long dataCount = 0;
        List<SplitBloomFilter> shards = splitShardedBloomFilter.getShards();
        for (SplitBloomFilter shard : shards) {
            System.out.println("shard = " + shard); // 출력되는 값을보면 250개씩 잘 쪼개진 것을 확인할 수 있음 (dataCount이 250개씩 할당됨)
            dataCount += shard.getBloomFilter().getDataCount();
        }

        // then
        // 전체 Shard에 분배된 데이터 수의 합이
        // SplitShardedBloomFilter가 관리하는 총 데이터 수와 동일해야 함
        assertThat(dataCount).isEqualTo(splitShardedBloomFilter.getDataCount());
    }

    @Test
    @DisplayName("입력 값의 해시를 기준으로 특정 Shard를 정상적으로 선택하는지 확인")
    void findShard() {
        // given
        // 4개의 Shard로 구성된 SplitShardedBloomFilter 생성
        SplitShardedBloomFilter splitShardedBloomFilter = SplitShardedBloomFilter.create(
                "testId", 1000, 0.01, 4
        );

        // when
        // value 값을 해싱하여 접근해야 할 Shard를 조회
        SplitBloomFilter shard = splitShardedBloomFilter.findShard("value");
        System.out.println("shard = " + shard);

        // then
        // 해시 기반 Sharding 결과로 항상 하나의 Shard가 선택되어야 함 (테스트를 여러번 실행했을 때 동일한 Shard로 할당되는 것을 확인할 수 있음)
        assertThat(shard).isNotNull();
    }

}
