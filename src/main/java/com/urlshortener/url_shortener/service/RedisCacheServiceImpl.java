package com.urlshortener.url_shortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RedisCacheServiceImpl implements CacheService{
	private static final Logger log = LoggerFactory.getLogger(RedisCacheServiceImpl.class);
	
	private static final String KEY_PREFIX = "url:";

    private final StringRedisTemplate redisTemplate;
    private final Duration defaultTtl;

    public RedisCacheServiceImpl(
            StringRedisTemplate redisTemplate,
            @Value("${app.cache.default-ttl-seconds:86400}") long defaultTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.defaultTtl = Duration.ofSeconds(defaultTtlSeconds);
    }

    @Override
    public void put(String shortKey, String longUrl, Duration ttl) {
        Duration effectiveTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero())
                ? ttl
                : defaultTtl;
        log.debug("Redis PUT key='{}' ttl='{}'", buildKey(shortKey), effectiveTtl);
        redisTemplate.opsForValue().set(buildKey(shortKey), longUrl, effectiveTtl);
    }

    @Override
    public Optional<String> get(String shortKey) {
        String value = redisTemplate.opsForValue().get(buildKey(shortKey));
        log.debug("Redis GET key='{}' → {}", buildKey(shortKey), value != null ? "found" : "null");
        return Optional.ofNullable(value);
    }

    @Override
    public void evict(String shortKey) {
        redisTemplate.delete(buildKey(shortKey));
    }

    private String buildKey(String shortKey) {
        return KEY_PREFIX + shortKey;
    }

}
