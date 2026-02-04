package com.example.cache.service.strategy.splitshardedbloomfilter;

import com.example.cache.service.strategy.splitbloomfilter.SplitBloomFilter;
import com.google.common.hash.Hashing;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SplitShardedBloomFilter {

    private String id;
    private long dataCount; // 데이터 수(n)
    private double falsePositiveRate; // 오차율(p)
    private List<SplitBloomFilter> shards;
    private int shardCount;

    public static SplitShardedBloomFilter create(String id, long dataCount, double falsePositiveRate, int shardCount) {
        SplitShardedBloomFilter splitShardedBloomFilter = new SplitShardedBloomFilter();
        splitShardedBloomFilter.id = id;
        splitShardedBloomFilter.dataCount = dataCount;
        splitShardedBloomFilter.falsePositiveRate = falsePositiveRate;
        splitShardedBloomFilter.shards = createShards(id, dataCount, falsePositiveRate, shardCount);
        splitShardedBloomFilter.shardCount = shardCount;
        return splitShardedBloomFilter;
    }

    private static List<SplitBloomFilter> createShards(String id, long dataCount, double falsePositiveRate, int shardCount) {
        long dataChunkCount = dataCount / shardCount;
        long remainder = dataCount % shardCount;

        List<SplitBloomFilter> shards = new ArrayList<>();
        for (int shardIndex = 0; shardIndex < shardCount; shardIndex++) {
            SplitBloomFilter shard = SplitBloomFilter.create(
                    id + ":shard:" + shardIndex,
                    dataChunkCount + (shardIndex < remainder ? 1 : 0),
                    falsePositiveRate
            );
            shards.add(shard);
        }
        return shards;
    }

    public SplitBloomFilter findShard(String value) {
        return shards.get(findShardIndex(value));
    }

    private int findShardIndex(String value) {
        return Math.abs(Hashing.murmur3_32_fixed()
                .hashString(value, StandardCharsets.UTF_8)
                .asInt() % shardCount);
    }

}
