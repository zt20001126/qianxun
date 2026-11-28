package com.qianxun.homestay.modules.room.service.impl;

import com.qianxun.homestay.modules.room.service.RoomImageUrlService;
import com.qianxun.homestay.common.cache.DisplayCacheService;
import com.qianxun.homestay.config.CosProperties;
import com.qianxun.homestay.config.DisplayCacheProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import com.qianxun.homestay.modules.storage.service.CosStorageService;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

/** 将已迁移的旧客房图片引用转换为 COS 临时访问地址。 */
@Service
public class RoomImageUrlServiceImpl implements RoomImageUrlService {
    private static final String LEGACY_PREFIX = "oss://qianxunminsu/";
    private static final Duration URL_LIFETIME = Duration.ofHours(1);
    private final DisplayCacheProperties cacheProperties;
    private final CosStorageService cosStorageService;
    private final DisplayCacheService cache;
    private final CosProperties properties;

    public RoomImageUrlServiceImpl(CosStorageService cosStorageService, DisplayCacheService cache, CosProperties properties, DisplayCacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
        this.cosStorageService = cosStorageService;
        this.cache = cache;
        this.properties = properties;
    }

    @Override
    public List<String> resolveForApp(List<String> imageReferences) {
        if (imageReferences == null) return List.of();
        return imageReferences.stream().filter(value -> value != null && !value.isBlank())
                .map(this::resolve).filter(value -> value != null).toList();
    }

    private String resolve(String reference) {
        if (reference.startsWith(LEGACY_PREFIX) && reference.length() > LEGACY_PREFIX.length()) {
            String key = reference.substring(LEGACY_PREFIX.length());
            String identity = properties.getRegion() + "|" + properties.getBucket() + "|" + properties.getSecretId() + "|" + properties.getSecretKey() + "|" + key;
            try {
                String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
                return cache.get("images", digest, cacheProperties.getImageTtl(), String.class,
                        () -> cosStorageService.signedReadUrl(key, URL_LIFETIME));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("签名缓存摘要算法不可用", exception);
            }
        }
        return reference.startsWith("https://") || reference.startsWith("http://") ? reference : null;
    }
}
