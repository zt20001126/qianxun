package com.qianxun.homestay.modules.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.modules.room.service.impl.RoomImageUrlServiceImpl;
import com.qianxun.homestay.modules.storage.service.CosStorageService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证旧客房图片引用经腾讯云生成临时地址。 */
class RoomImageUrlServiceImplTest {
    @Test
    void resolvesLegacyReferenceUsingCosAndLeavesHttpUrlsUnchanged() {
        CosStorageService cos = mock(CosStorageService.class);
        when(cos.signedReadUrl("rooms/a.jpg", Duration.ofHours(1))).thenReturn("https://cos.example/signed");
        var cache = mock(com.qianxun.homestay.common.cache.DisplayCacheService.class);
        when(cache.get(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(String.class), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(4)).get());
        RoomImageUrlServiceImpl service = new RoomImageUrlServiceImpl(cos, cache, new com.qianxun.homestay.config.CosProperties(), new com.qianxun.homestay.config.DisplayCacheProperties());
        assertEquals(List.of("https://cos.example/signed", "https://example.com/image.jpg"),
                service.resolveForApp(List.of("oss://qianxunminsu/rooms/a.jpg", "https://example.com/image.jpg")));
    }
}
