package com.nfcplatform.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-key (typically IP + route) rate limiting, Redis-backed so limits are shared correctly
 * across multiple backend instances - the gap docs/PROJECT_PROGRESS.md's Phase 10 remainder
 * called out as required before running more than one instance. Implemented as a plain
 * INCR+EXPIRE fixed-window counter rather than porting Bucket4j's smoother token-bucket
 * algorithm to Redis - that needs a dedicated ProxyManager per Redis client library, more
 * moving parts than a well-understood two-command pattern buys here for an anti-abuse limiter,
 * not a billing-grade one. (Trade-off: a fixed window can admit roughly 2x capacity right
 * across a window boundary - acceptable for blunting brute-force/enumeration, not for anything
 * that needs an exact cap.)
 *
 * Falls back to the original in-memory Bucket4j bucket-per-key implementation whenever the
 * Redis call itself fails, matching docs/SECURITY.md's documented "Redis-backed when available,
 * in-memory fallback in dev" plan - local dev keeps working without a running Redis container,
 * and a production Redis outage degrades to per-instance limiting rather than taking down every
 * endpoint this guards (login, password reset, public lead form, ticket creation, NFC/QR
 * redirects).
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Bucket> fallbackBuckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int capacity, Duration refillPeriod) {
        try {
            Long count = redisTemplate.opsForValue().increment(KEY_PREFIX + key);
            if (count == null) {
                return tryConsumeInMemory(key, capacity, refillPeriod);
            }
            if (count == 1L) {
                redisTemplate.expire(KEY_PREFIX + key, refillPeriod);
            }
            return count <= capacity;
        } catch (Exception e) {
            log.warn("Rate limiter Redis call failed, falling back to in-memory limiting for key '{}': {}",
                    key, e.getMessage());
            return tryConsumeInMemory(key, capacity, refillPeriod);
        }
    }

    private boolean tryConsumeInMemory(String key, int capacity, Duration refillPeriod) {
        Bucket bucket = fallbackBuckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod)))
                .build());
        return bucket.tryConsume(1);
    }
}
