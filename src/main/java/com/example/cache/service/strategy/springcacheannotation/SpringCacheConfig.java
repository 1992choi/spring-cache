package com.example.cache.service.strategy.springcacheannotation;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class SpringCacheConfig {

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
			.disableCachingNullValues() // NULL 값은 캐싱되지 않도록 처리하는 옵션
			.serializeValuesWith(
				RedisSerializationContext.SerializationPair.fromSerializer(
					new GenericJackson2JsonRedisSerializer()
				)
			);

		return RedisCacheManager.builder(connectionFactory)
			.withInitialCacheConfigurations(
				Map.of( // key로 들어가는 값은 cacheNames의 값
					"item", defaultCacheConfig.entryTtl(Duration.ofSeconds(1)),
					"itemList", defaultCacheConfig.entryTtl(Duration.ofSeconds(1)),
					"itemListInfiniteScroll", defaultCacheConfig.entryTtl(Duration.ofSeconds(1))
				)
			)
			.build();
	}
}
