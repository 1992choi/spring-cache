package com.example.cache.service.strategy.per;

import com.example.cache.serde.DataSerializer;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;
import java.util.random.RandomGenerator;

@Getter
@ToString
public class CacheData {

    // 실제 캐시에 저장될 데이터 (직렬화된 형태)
    private String data;

    // 캐시 데이터를 재계산하는 데 걸린 시간 (PER 알고리즘의 delta)
    private long computationTimeMillis;

    // 캐시 만료 시점 (PER 알고리즘의 expiry)
    private long expiredAtMillis;

    /*
        캐시 데이터 생성

        - data: 실제 비즈니스 데이터
        - computationTimeMillis: 데이터 계산에 소요된 시간 (delta)
        - ttl: 캐시 TTL
    */
    public static CacheData of(Object data, long computationTimeMillis, Duration ttl) {
        CacheData cacheData = new CacheData();
        cacheData.data = DataSerializer.serializeOrException(data);
        cacheData.computationTimeMillis = computationTimeMillis;
        cacheData.expiredAtMillis = Instant.now().plus(ttl).toEpochMilli();
        return cacheData;
    }

    /*
        직렬화된 데이터를 요청 타입으로 역직렬화
     */
    public <T> T parseData(Class<T> dataType) {
        return DataSerializer.deserializeOrNull(data, dataType);
    }

    /*
        Probabilistic Early Recomputation 여부 판단

        - beta: 재계산 민감도 계수
        - 만료 시점이 가까워질수록
        - 재계산 비용(delta)이 클수록
        - 확률적으로 재계산이 발생하도록 설계
    */
    public boolean shouldRecompute(double beta) {
        long nowMillis = Instant.now().toEpochMilli();
        double rand = RandomGenerator.getDefault().nextDouble();

        return nowMillis - computationTimeMillis * beta * Math.log(rand) >= expiredAtMillis;
    }

}
