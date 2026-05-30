package br.com.fiap.solarshield.notifier.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisIdempotencyStore implements IdempotencyStore {
    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisIdempotencyStore(
            StringRedisTemplate redisTemplate,
            @Value("${solar.idempotency.ttl-seconds}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public boolean markFirst(String eventId) {
        Boolean inserted = redisTemplate.opsForValue().setIfAbsent("idem:notifier:" + eventId, "1", ttl);
        return Boolean.TRUE.equals(inserted);
    }

    @Override
    public void release(String eventId) {
        redisTemplate.delete("idem:notifier:" + eventId);
    }
}
