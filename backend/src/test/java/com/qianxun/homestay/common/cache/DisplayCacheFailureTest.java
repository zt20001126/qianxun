package com.qianxun.homestay.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 验证 Redis 故障不会改变数据库查询结果。 */
class DisplayCacheFailureTest {
    @Test
    void returnsLoadedValueWhenRedisIsUnavailable() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("不可用"));
        var cache = new RedisDisplayCacheService(redis, new ObjectMapper());
        AtomicInteger loads = new AtomicInteger();
        assertThat(cache.get("rooms", "catalog", Duration.ofSeconds(30), String.class,
                () -> { loads.incrementAndGet(); return "数据库结果"; })).isEqualTo("数据库结果");
        assertThat(loads.get()).isEqualTo(1);
    }
    @Test
    void invalidationFailureRetriesBeforeUsingCache() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        org.springframework.data.redis.core.ValueOperations<String, String> values = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        doThrow(new org.springframework.data.redis.RedisConnectionFailureException("不可用"))
                .doNothing().when(values).set(anyString(), anyString());
        when(values.get(anyString())).thenReturn("generation", "\"新值\"");
        var cache = new RedisDisplayCacheService(redis, new ObjectMapper());
        cache.invalidateAfterCommit("mall");
        assertThat(cache.get("mall", "item", Duration.ofSeconds(30), String.class, () -> "数据库")).isEqualTo("新值");
        verify(values, times(2)).set(anyString(), anyString());
    }
    @Test
    void backgroundRetriesAreBoundedAndRecoveryClearsPending() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        org.springframework.data.redis.core.ValueOperations<String, String> values = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        doThrow(new org.springframework.data.redis.RedisConnectionFailureException("不可用")).when(values).set(anyString(), anyString());
        var cache = new RedisDisplayCacheService(redis, new ObjectMapper());
        cache.invalidateAfterCommit("rooms");
        for (int i = 0; i < 5; i++) cache.retryInvalidations();
        verify(values, times(4)).set(anyString(), anyString());
        assertThat(cache.get("rooms", "catalog", Duration.ofSeconds(30), String.class, () -> "数据库")).isEqualTo("数据库");
        doNothing().when(values).set(anyString(), anyString());
        when(values.get(anyString())).thenReturn("generation", "\"缓存\"");
        assertThat(cache.get("rooms", "catalog", Duration.ofSeconds(30), String.class, () -> "数据库")).isEqualTo("缓存");
    }
    @Test
    void reconnectRotatesBothDisplayDomainsBeforeReading() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        org.springframework.data.redis.core.ValueOperations<String, String> values = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("断连")).thenReturn(values);
        when(values.get(anyString())).thenReturn("generation", "\"房间\"", "generation", "\"商品\"");
        var cache = new RedisDisplayCacheService(redis, new ObjectMapper());
        assertThat(cache.get("rooms", "catalog", Duration.ofSeconds(30), String.class, () -> "回源")).isEqualTo("回源");
        assertThat(cache.get("rooms", "catalog", Duration.ofSeconds(30), String.class, () -> "回源")).isEqualTo("房间");
        assertThat(cache.get("mall", "item", Duration.ofSeconds(30), String.class, () -> "回源")).isEqualTo("商品");
        verify(values).set(eq("qx:v1:{rooms}:generation"), anyString());
        verify(values).set(eq("qx:v1:{mall}:generation"), anyString());
    }
}
