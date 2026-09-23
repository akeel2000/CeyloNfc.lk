package com.nfcplatform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for both paths RateLimiterService can take: the Redis-backed fixed-window counter
 * (the normal path, correct across multiple backend instances) and the in-memory Bucket4j
 * fallback it degrades to whenever the Redis call itself fails (docs/SECURITY.md's documented
 * "Redis-backed when available, in-memory fallback in dev" plan). Pure Mockito, no real Redis.
 */
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiterService = new RateLimiterService(redisTemplate);
    }

    @Test
    void allowsRequestsWithinCapacityViaRedis() {
        when(valueOperations.increment("ratelimit:key")).thenReturn(1L);

        boolean allowed = rateLimiterService.tryConsume("key", 5, Duration.ofMinutes(1));

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire("ratelimit:key", Duration.ofMinutes(1));
    }

    @Test
    void onlySetsExpiryOnTheFirstIncrementOfAWindow() {
        when(valueOperations.increment("ratelimit:key")).thenReturn(3L);

        rateLimiterService.tryConsume("key", 5, Duration.ofMinutes(1));

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void deniesRequestsOverCapacityViaRedis() {
        when(valueOperations.increment("ratelimit:key")).thenReturn(6L);

        boolean allowed = rateLimiterService.tryConsume("key", 5, Duration.ofMinutes(1));

        assertThat(allowed).isFalse();
    }

    @Test
    void fallsBackToInMemoryLimitingWhenRedisThrows() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis unreachable"));

        boolean allowed = rateLimiterService.tryConsume("fallback-key", 2, Duration.ofMinutes(1));

        assertThat(allowed).isTrue();
    }

    @Test
    void inMemoryFallbackStillEnforcesTheCapacityLimit() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis unreachable"));

        assertThat(rateLimiterService.tryConsume("capped-key", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiterService.tryConsume("capped-key", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiterService.tryConsume("capped-key", 2, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void differentKeysAreRateLimitedIndependentlyInRedis() {
        when(valueOperations.increment("ratelimit:key-a")).thenReturn(6L);
        when(valueOperations.increment("ratelimit:key-b")).thenReturn(1L);

        assertThat(rateLimiterService.tryConsume("key-a", 5, Duration.ofMinutes(1))).isFalse();
        assertThat(rateLimiterService.tryConsume("key-b", 5, Duration.ofMinutes(1))).isTrue();
    }
}
