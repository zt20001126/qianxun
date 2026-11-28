package com.qianxun.homestay.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.dao.DataAccessException;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 展示数据的按需缓存；Redis 不可用时回退数据库。 */
@Service
public class RedisDisplayCacheService implements DisplayCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDisplayCacheService.class);
    private static final String PREFIX = "qx:v1:";
    private static final DefaultRedisScript<Long> PUT = new DefaultRedisScript<>(
            "local now = redis.call('TIME'); local millis = now[1] * 1000 + math.floor(now[2] / 1000); "
            + "if redis.call('GET', KEYS[1]) == ARGV[1] and tonumber(ARGV[3]) > millis then "
            + "redis.call('SET', KEYS[2], ARGV[2], 'PXAT', ARGV[3]); return 1 end return 0", Long.class);
    /** 失效失败的领域先回源，恢复连接后重试切换代次。 */
    private static final int MAX_BACKGROUND_RETRIES = 3;
    private static final long RETRY_DELAY_MILLIS = 5000L;
    private static final long CATALOG_JITTER_MILLIS = 15000L;
    private static final long PAGE_JITTER_MILLIS = 5000L;
    private final Set<String> pendingInvalidations = ConcurrentHashMap.newKeySet();
    private final Map<String, AtomicInteger> retryCounts = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisDisplayCacheService(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    /** 读取展示快照，未命中时执行加载并限制缓存有效期。 */
    public <T> T get(String domain, String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) return loader.get();
        if (pendingInvalidations.contains(domain) && !rotate(domain)) return loader.get();
        String generationKey = PREFIX + "{" + domain + "}:generation";
        String generation;
        String dataKey;
        try {
            generation = redis.opsForValue().get(generationKey);
            if (generation == null) {
                String candidate = UUID.randomUUID().toString();
                redis.opsForValue().setIfAbsent(generationKey, candidate);
                generation = redis.opsForValue().get(generationKey);
            }
            if (generation == null) throw new IllegalStateException("缓存代次不可用");
            dataKey = PREFIX + "{" + domain + "}:" + generation + ":" + key;
            String value = redis.opsForValue().get(dataKey);
            if (value != null) {
                T result = mapper.readValue(value, type);
                Metrics.counter("qianxun.display.cache.hit").increment();
                return result;
            }
            Metrics.counter("qianxun.display.cache.miss").increment();
        } catch (DataAccessException | JsonProcessingException | IllegalStateException exception) {
            // Redis 连接中断后，恢复时先切换展示领域代次，避免继续读取中断前的旧值。
            if (exception instanceof DataAccessException) {
                pendingInvalidations.add("rooms");
                pendingInvalidations.add("mall");
            }
            Metrics.counter("qianxun.display.cache.read.failure").increment();
            LOGGER.warn("展示缓存读取失败，回退数据库，domain={}", domain);
            return loader.get();
        }
        // 签名链接不能延长有效期；展示快照错开过期，降低集中回源。
        long jitter = "rooms".equals(domain) || ("mall".equals(domain) && key.startsWith("product:"))
                ? CATALOG_JITTER_MILLIS : ("mall".equals(domain) && key.startsWith("page:") ? PAGE_JITTER_MILLIS : 0);
        long deadline = System.currentTimeMillis() + ttl.toMillis()
                + (jitter == 0 ? 0 : ThreadLocalRandom.current().nextLong(jitter + 1));
        T value = loader.get();
        if (value != null && System.currentTimeMillis() < deadline) {
            try {
                Long written = redis.execute(PUT, List.of(generationKey, dataKey), generation, mapper.writeValueAsString(value), Long.toString(deadline));
                if (Long.valueOf(0).equals(written)) Metrics.counter("qianxun.display.cache.fill.rejected").increment();
            } catch (DataAccessException | JsonProcessingException exception) {
                Metrics.counter("qianxun.display.cache.write.failure").increment();
                LOGGER.warn("展示缓存写入失败，domain={}", domain);
            }
        }
        return value;
    }

    /** 数据库事务提交后切换缓存代次。 */
    public void invalidateAfterCommit(String domain) {
        Runnable invalidate = () -> scheduleInvalidation(domain);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { invalidate.run(); }
            });
        } else invalidate.run();
    }

    /** 以同一锁协调首次失效和重试注册，避免并发恢复后留下无效任务。 */
    private synchronized void scheduleInvalidation(String domain) {
        if (!rotate(domain)) retryCounts.put(domain, new AtomicInteger());
    }

    /** 后台合并同一领域的失效重试，超过次数后保留回源状态。 */
    @Scheduled(fixedDelay = RETRY_DELAY_MILLIS)
    public void retryInvalidations() {
        retryCounts.forEach((domain, count) -> {
            if (count.incrementAndGet() <= MAX_BACKGROUND_RETRIES) {
                Metrics.counter("qianxun.display.cache.invalidate.retry").increment();
                if (rotate(domain)) retryCounts.remove(domain, count);
            } else {
                retryCounts.remove(domain, count);
            }
        });
    }

    /** 失效失败时暂不使用该领域的本地缓存访问。 */
    private synchronized boolean rotate(String domain) {
        try {
            redis.opsForValue().set(PREFIX + "{" + domain + "}:generation", UUID.randomUUID().toString());
            pendingInvalidations.remove(domain);
            retryCounts.remove(domain);
            return true;
        } catch (DataAccessException exception) {
            pendingInvalidations.add(domain);
            Metrics.counter("qianxun.display.cache.invalidate.failure").increment();
            LOGGER.warn("展示缓存失效失败，暂时回源，domain={}", domain);
            return false;
        }
    }
}
