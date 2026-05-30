package br.com.fiap.solarshield.notifier.service;

import br.com.fiap.solarshield.notifier.domain.AlertPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAlertStore implements AlertStore {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAlertStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(AlertPayload payload) {
        try {
            redisTemplate.opsForList().leftPush("alerts:processed", objectMapper.writeValueAsString(payload));
            redisTemplate.opsForList().trim("alerts:processed", 0, 99);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
