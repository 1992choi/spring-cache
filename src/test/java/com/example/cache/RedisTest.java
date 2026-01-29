package com.example.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("RedisTestContainerSupport 을 위한 테스트 : 테스트마다 데이터 삭제 및 격리된 공간에서의 테스트를 지원")
public class RedisTest extends RedisTestContainerSupport {

    @Test
    void test1() {
        redisTemplate.opsForValue().set("mykey", "myvalue");
        String result = redisTemplate.opsForValue().get("mykey");
        System.out.println("result = " + result); // result = myvalue
    }

    @Test
    void test2() {
        String result = redisTemplate.opsForValue().get("mykey");
        System.out.println("result = " + result); // null : RedisTestContainerSupport의 @BeforeEach 에서 지워짐
    }

    @Test
    void test3() {
        String result = redisTemplate.opsForValue().get("mykey");
        System.out.println("result = " + result); // null : RedisTestContainerSupport의 @BeforeEach 에서 지워짐
    }
}
