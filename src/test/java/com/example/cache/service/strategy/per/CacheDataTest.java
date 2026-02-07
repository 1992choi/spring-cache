package com.example.cache.service.strategy.per;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CacheDataTest {

    @Test
    @DisplayName("CacheData 생성 시 데이터가 문자열로 직렬화되고, 다시 원본 타입으로 역직렬화된다")
    void parseData() {
        /*
            CacheData 생성 및 직렬화/역직렬화 검증 테스트

            - data: Long 타입 데이터
            - CacheData 내부에서는 문자열로 직렬화되어 저장됨
            - parseData()를 통해 다시 원래 타입으로 복원 가능한지 확인
         */
        CacheData cacheData = CacheData.of(1234L, 1000L, Duration.ofSeconds(10));
        System.out.println("cacheData = " + cacheData);

        // 직렬화된 문자열 데이터 확인
        assertThat(cacheData.getData()).isEqualTo("1234");

        // 역직렬화 후 원본 데이터와 동일한지 확인
        assertThat(cacheData.parseData(Long.class)).isEqualTo(1234L);
    }

    @Test
    @DisplayName("PER 알고리즘에 따라 TTL이 만료되기 전에도 확률적으로 재계산 대상이 될 수 있다")
    void shouldRecompute() throws InterruptedException {
        /*
            Probabilistic Early Recomputation(PER) 동작 확인 테스트

            - TTL이 짧은 CacheData를 생성
            - 일정 시간 간격으로 shouldRecompute()를 반복 호출
            - 시간이 지남에 따라 확률적으로 true가 발생하는지 관찰
            - (결과는 확정적이지 않고 확률적 특성을 가짐)
         */
        CacheData cacheData = CacheData.of(1234L, 1000L, Duration.ofSeconds(3));

        for (int i = 0; i < 30; i++) {
            boolean result = cacheData.shouldRecompute(1);
            System.out.println("result = " + result); // 콘솔을 확인해보면 뒤로갈수록 true가 많아진다.

            // 시간 경과에 따른 재계산 확률 변화를 보기 위한 sleep
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

}
