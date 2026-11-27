package com.qianxun.homestay.modules.room.service.impl;

import com.qianxun.homestay.modules.room.service.RoomImageUrlService;
import com.qianxun.homestay.modules.storage.service.CosStorageService;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

/** 将已迁移的旧客房图片引用转换为 COS 临时访问地址。 */
@Service
public class RoomImageUrlServiceImpl implements RoomImageUrlService {
    private static final String LEGACY_PREFIX = "oss://qianxunminsu/";
    private static final Duration URL_LIFETIME = Duration.ofHours(1);
    private final CosStorageService cosStorageService;

    public RoomImageUrlServiceImpl(CosStorageService cosStorageService) {
        this.cosStorageService = cosStorageService;
    }

    @Override
    public List<String> resolveForApp(List<String> imageReferences) {
        if (imageReferences == null) return List.of();
        return imageReferences.stream().filter(value -> value != null && !value.isBlank())
                .map(this::resolve).filter(value -> value != null).toList();
    }

    private String resolve(String reference) {
        if (reference.startsWith(LEGACY_PREFIX) && reference.length() > LEGACY_PREFIX.length()) {
            return cosStorageService.signedReadUrl(reference.substring(LEGACY_PREFIX.length()), URL_LIFETIME);
        }
        return reference.startsWith("https://") || reference.startsWith("http://") ? reference : null;
    }
}
