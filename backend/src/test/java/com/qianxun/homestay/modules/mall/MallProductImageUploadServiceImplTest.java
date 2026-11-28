package com.qianxun.homestay.modules.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.service.impl.MallProductImageUploadServiceImpl;
import com.qianxun.homestay.modules.storage.service.CosStorageService;
import com.qianxun.homestay.modules.storage.service.impl.CatalogImageUploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class MallProductImageUploadServiceImplTest {

    private final CosStorageService cos = mock(CosStorageService.class);
    private final MallProductImageUploadServiceImpl service =
            new MallProductImageUploadServiceImpl(new CatalogImageUploadServiceImpl(cos));

    @Test
    void uploadsValidatedImagesToCosWhenSelected() {
        when(cos.publicUrl(anyString())).thenAnswer(invocation -> "https://example.com/" + invocation.getArgument(0));
        String url = service.upload(new MockMultipartFile("file", "image.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}));
        assertTrue(url.startsWith("https://example.com/mall/products/"));
        assertTrue(url.endsWith(".png"));
        verify(cos).upload(anyString(), any(), anyLong(), org.mockito.ArgumentMatchers.eq("image/png"));
    }

    @Test
    void cosFailureDoesNotFallBackToOss() {
        when(cos.publicUrl(anyString())).thenReturn("https://example.com/a.jpg");
        doThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "腾讯云 COS 上传失败")).when(cos)
                .upload(anyString(), any(), anyLong(), anyString());
        BusinessException exception = assertThrows(BusinessException.class, () -> service.upload(
                new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})));
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }

    @Test
    void acceptsCommonImageSignatures() {
        byte[][] signatures = {
                {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
                {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                {0x47, 0x49, 0x46, 0x38},
                {0x42, 0x4D},
                {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50}
        };

        when(cos.publicUrl(anyString())).thenReturn("https://example.com/image");
        for (byte[] signature : signatures) {
            assertEquals("https://example.com/image", service.upload(
                    new MockMultipartFile("file", "image", "application/octet-stream", signature)));
        }
    }

    @Test
    void rejectsNonImageContent() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.upload(new MockMultipartFile("file", "image.svg", "image/svg+xml", "<svg/>".getBytes())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void rejectsEmptyUpload() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.upload(new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0])));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
