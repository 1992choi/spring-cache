package com.example.cache.common.distributedlock;

import com.example.cache.RedisTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DistributedLockProviderTest extends RedisTestContainerSupport {

    @Autowired
    DistributedLockProvider distributedLockProvider;

    @Test
    void lock() throws InterruptedException {
        // 동일한 key에 대해 최초 1회만 락 획득 가능함을 검증
        assertThat(distributedLockProvider.lock("testId", Duration.ofSeconds(1))).isTrue();
        assertThat(distributedLockProvider.lock("testId", Duration.ofSeconds(1))).isFalse();
        assertThat(distributedLockProvider.lock("testId", Duration.ofSeconds(1))).isFalse();

        // TTL 만료 이후에는 다시 락을 획득할 수 있어야 함
        TimeUnit.SECONDS.sleep(2);
        assertThat(distributedLockProvider.lock("testId", Duration.ofSeconds(1))).isTrue();
    }

    @Test
    void lock_shouldAcquireOnlyOnce_whenMultiThread() throws InterruptedException {
        // 여러 스레드가 동시에 락을 시도해도
        // 오직 하나의 스레드만 락을 획득해야 함을 검증
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger acquiredCount = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                boolean locked = distributedLockProvider.lock("testId", Duration.ofSeconds(10));
                if (locked) {
                    acquiredCount.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await();

        // 락을 획득한 스레드는 정확히 1개여야 함
        assertThat(acquiredCount.get()).isEqualTo(1);
    }

    @Test
    void unlock() {
        // given
        // 락을 먼저 획득한 상태
        distributedLockProvider.lock("testId", Duration.ofSeconds(1));

        // when
        // 명시적으로 락 해제
        distributedLockProvider.unlock("testId");

        // then
        // 동일한 key에 대해 다시 락 획득이 가능해야 함
        boolean locked = distributedLockProvider.lock("testId", Duration.ofSeconds(1));
        assertThat(locked).isTrue();
    }

}
