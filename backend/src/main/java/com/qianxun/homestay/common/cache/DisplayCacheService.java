package com.qianxun.homestay.common.cache;

import java.time.Duration;
import java.util.function.Supplier;

/** 房间和商品展示资料的缓存接口。 */
public interface DisplayCacheService {
    /** 按需读取缓存，缺失时调用加载函数。 */
    <T> T get(String domain, String key, Duration ttl, Class<T> type, Supplier<T> loader);
    /** 在业务事务提交后使领域缓存失效。 */
    void invalidateAfterCommit(String domain);
}
