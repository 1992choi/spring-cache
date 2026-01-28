package com.example.cache.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CustomCacheAspect {
    private final List<CustomCacheHandler> CustomCacheHandlers;
    private final CustomCacheKeyGenerator CustomCacheKeyGenerator;

    @Around("@annotation(customCacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, CustomCacheable CustomCacheable) {
        CacheStrategy cacheStrategy = CustomCacheable.cacheStrategy();
        CustomCacheHandler cacheHandler = findCacheHandler(cacheStrategy);

        String key = CustomCacheKeyGenerator.genKey(joinPoint, cacheStrategy, CustomCacheable.cacheName(), CustomCacheable.key());
        Duration ttl = Duration.ofSeconds(CustomCacheable.ttlSeconds());
        Supplier<Object> dataSourceSupplier = createDataSourceSupplier(joinPoint);
        Class returnType = findReturnType(joinPoint);

        try {
            log.info("[CustomCacheAspect.handleCacheable] key={}", key);
            return cacheHandler.fetch(
                    key,
                    ttl,
                    dataSourceSupplier,
                    returnType
            );
        } catch (Exception e) {
            log.error("[CustomCacheAspect.handleCacheable] key={}", key, e);
            return dataSourceSupplier.get();
        }
    }

    private CustomCacheHandler findCacheHandler(CacheStrategy cacheStrategy) {
        return CustomCacheHandlers.stream()
                .filter(handler -> handler.supports(cacheStrategy))
                .findFirst()
                .orElseThrow();
    }

    private Supplier<Object> createDataSourceSupplier(ProceedingJoinPoint joinPoint) {
        return () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    private Class findReturnType(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        return methodSignature.getReturnType();
    }

    @AfterReturning(pointcut = "@annotation(CustomCachePut)", returning = "result")
    public void handleCachePut(JoinPoint joinPoint, CustomCachePut CustomCachePut, Object result) {
        CacheStrategy cacheStrategy = CustomCachePut.cacheStrategy();
        CustomCacheHandler cacheHandler = findCacheHandler(cacheStrategy);
        String key = CustomCacheKeyGenerator.genKey(joinPoint, cacheStrategy, CustomCachePut.cacheName(), CustomCachePut.key());
        log.info("[CustomCacheAspect.handleCachePut] key={}", key);
        cacheHandler.put(key, Duration.ofSeconds(CustomCachePut.ttlSeconds()), result);
    }

    @AfterReturning(pointcut = "@annotation(CustomCacheEvict)")
    public void handleCacheEvict(JoinPoint joinPoint, CustomCacheEvict CustomCacheEvict) {
        CacheStrategy cacheStrategy = CustomCacheEvict.cacheStrategy();
        CustomCacheHandler cacheHandler = findCacheHandler(cacheStrategy);
        String key = CustomCacheKeyGenerator.genKey(joinPoint, cacheStrategy, CustomCacheEvict.cacheName(), CustomCacheEvict.key());
        log.info("[CustomCacheAspect.handleCacheEvict] key={}", key);
        cacheHandler.evict(key);
    }
}
