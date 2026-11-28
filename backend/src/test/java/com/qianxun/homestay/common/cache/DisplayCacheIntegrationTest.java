package com.qianxun.homestay.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 Redis 验证缓存命中、失效及并发旧值回填。 */
@Testcontainers(disabledWithoutDocker = true)
class DisplayCacheIntegrationTest {
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4.7-alpine").withExposedPorts(6379);

    @Container static final org.testcontainers.containers.MySQLContainer<?> MYSQL =
            new org.testcontainers.containers.MySQLContainer<>("mysql:8.0").withDatabaseName("cache_transaction");

    @Test
    void transactionCommitAndRollbackControlInvalidation() {
        var factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        factory.afterPropertiesSet();
        factory.start();
        try {
            var redis = new StringRedisTemplate(factory);
            redis.afterPropertiesSet();
            var cache = new RedisDisplayCacheService(redis, new ObjectMapper());
            var source = new org.springframework.jdbc.datasource.DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
            var jdbc = new org.springframework.jdbc.core.JdbcTemplate(source);
            jdbc.execute("CREATE TABLE cache_test (id INT PRIMARY KEY, name VARCHAR(30))");
            jdbc.update("INSERT INTO cache_test VALUES (1, 'old')");
            var tx = new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(source));
            java.util.function.Supplier<String> load = () -> jdbc.queryForObject("SELECT name FROM cache_test WHERE id = 1", String.class);
            assertThat(cache.get("tx-test", "item", Duration.ofMinutes(1), String.class, load)).isEqualTo("old");
            tx.executeWithoutResult(status -> { jdbc.update("UPDATE cache_test SET name = 'rollback' WHERE id = 1"); cache.invalidateAfterCommit("tx-test"); status.setRollbackOnly(); });
            assertThat(cache.get("tx-test", "item", Duration.ofMinutes(1), String.class, load)).isEqualTo("old");
            tx.executeWithoutResult(status -> { jdbc.update("UPDATE cache_test SET name = 'new' WHERE id = 1"); cache.invalidateAfterCommit("tx-test"); });
            assertThat(cache.get("tx-test", "item", Duration.ofMinutes(1), String.class, load)).isEqualTo("new");
        } finally { factory.destroy(); }
    }

    @Test
    void cachesAndRejectsStaleConcurrentFill() throws Exception {
        var factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        factory.afterPropertiesSet();
        factory.start();
        try {
            var template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();
            var cache = new RedisDisplayCacheService(template, new ObjectMapper());
            cache.get("rooms", "catalog", Duration.ofSeconds(120), String.class, () -> "目录");
            String roomGeneration = template.opsForValue().get("qx:v1:{rooms}:generation");
            Long roomTtl = template.getExpire("qx:v1:{rooms}:" + roomGeneration + ":catalog", TimeUnit.MILLISECONDS);
            assertThat(roomTtl).isBetween(118000L, 135000L);
            cache.get("mall", "page:1:20", Duration.ofSeconds(30), String.class, () -> "分页");
            String mallGeneration = template.opsForValue().get("qx:v1:{mall}:generation");
            Long pageTtl = template.getExpire("qx:v1:{mall}:" + mallGeneration + ":page:1:20", TimeUnit.MILLISECONDS);
            assertThat(pageTtl).isBetween(28000L, 35000L);
            AtomicInteger loads = new AtomicInteger();
            AtomicInteger signatures = new AtomicInteger();
            assertThat(cache.get("images-test", "image", Duration.ofMinutes(50), String.class,
                    () -> { signatures.incrementAndGet(); return "https://example.test/signed"; })).isEqualTo("https://example.test/signed");
            assertThat(cache.get("images-test", "image", Duration.ofMinutes(50), String.class,
                    () -> { signatures.incrementAndGet(); return "different"; })).isEqualTo("https://example.test/signed");
            assertThat(signatures.get()).isEqualTo(1);
            assertThat(cache.get("test", "expired", Duration.ofMillis(1), String.class, () -> {
                try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return "expired";
            })).isEqualTo("expired");
            assertThat(cache.get("test", "expired", Duration.ofSeconds(30), String.class, () -> "fresh")).isEqualTo("fresh");
            assertThat(cache.get("test", "item", Duration.ofSeconds(30), String.class, () -> { loads.incrementAndGet(); return "old"; })).isEqualTo("old");
            assertThat(cache.get("test", "item", Duration.ofSeconds(30), String.class, () -> { loads.incrementAndGet(); return "other"; })).isEqualTo("old");
            assertThat(loads.get()).isEqualTo(1);
            org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
            try {
                cache.invalidateAfterCommit("test");
                assertThat(cache.get("test", "item", Duration.ofSeconds(30), String.class, () -> "wrong")).isEqualTo("old");
                org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().forEach(
                        sync -> sync.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
            } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
            assertThat(cache.get("test", "item", Duration.ofSeconds(30), String.class, () -> "wrong")).isEqualTo("old");
            org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
            try {
                cache.invalidateAfterCommit("test");
                org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().forEach(
                        org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
            CountDownLatch readStarted = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            try (var executor = Executors.newSingleThreadExecutor()) {
                var oldRead = executor.submit(() -> cache.get("test", "item", Duration.ofSeconds(30), String.class, () -> {
                    readStarted.countDown();
                    try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("等待超时"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                    return "stale";
                }));
                assertThat(readStarted.await(5, TimeUnit.SECONDS)).isTrue();
                cache.invalidateAfterCommit("test");
                release.countDown();
                assertThat(oldRead.get(5, TimeUnit.SECONDS)).isEqualTo("stale");
                assertThat(cache.get("test", "item", Duration.ofSeconds(30), String.class, () -> "fresh")).isEqualTo("fresh");
                assertThat(cache.get("test", "item", Duration.ofSeconds(30), String.class, () -> "wrong")).isEqualTo("fresh");
            }
        } finally { factory.destroy(); }
    }
}
