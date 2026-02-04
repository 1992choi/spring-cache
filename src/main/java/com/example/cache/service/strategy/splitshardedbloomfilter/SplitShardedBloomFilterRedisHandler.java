package com.example.cache.service.strategy.splitshardedbloomfilter;

import com.example.cache.service.strategy.splitbloomfilter.SplitBloomFilter;
import com.example.cache.service.strategy.splitbloomfilter.SplitBloomFilterRedisHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SplitShardedBloomFilterRedisHandler {

    private final SplitBloomFilterRedisHandler splitBloomFilterRedisHandler;

    public void init(SplitShardedBloomFilter splitShardedBloomFilter) {
        List<SplitBloomFilter> shards = splitShardedBloomFilter.getShards();
        for (SplitBloomFilter shard : shards) {
            // Split과 Sharding 기법은 서로 다른 기법이기 때문에 중첩으로 사용할 수 있다. split 기법을 사용하기 위하여 split filter에게 위임하는 식으로 중첩사용.
            splitBloomFilterRedisHandler.init(shard);
        }
    }

    public void add(SplitShardedBloomFilter splitShardedBloomFilter, String value) {
        SplitBloomFilter shard = splitShardedBloomFilter.findShard(value);
        splitBloomFilterRedisHandler.add(shard, value);
    }

    public boolean mightContain(SplitShardedBloomFilter splitShardedBloomFilter, String value) {
        SplitBloomFilter shard = splitShardedBloomFilter.findShard(value);
        return splitBloomFilterRedisHandler.mightContain(shard, value);
    }

    public void delete(SplitShardedBloomFilter splitShardedBloomFilter) {
        List<SplitBloomFilter> shards = splitShardedBloomFilter.getShards();
        for (SplitBloomFilter shard : shards) {
            splitBloomFilterRedisHandler.delete(shard);
        }
    }

}
