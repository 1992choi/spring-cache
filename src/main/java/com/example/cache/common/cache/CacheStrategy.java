package com.example.cache.common.cache;

public enum CacheStrategy {
    NONE,
    SPRING_CACHE_ANNOTATION,
    NULL_OBJECT_PATTERN,
    BLOOM_FILTER,
    SPLIT_BLOOM_FILTER,
}
