package com.qianxun.homestay.modules.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.config.OssProperties;
import com.qianxun.homestay.modules.mall.service.impl.MallProductImageUploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class MallProductImageUploadServiceImplTest {

    private final MallProductImageUploadServiceImpl service = new MallProductImageUploadServiceImpl(new OssProperties());

    @Test
    void acceptsCommonImageSignaturesBeforeCheckingOssConfiguration() {
        byte[][] signatures = {
                {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
                {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                {0x47, 0x49, 0x46, 0x38},
                {0x42, 0x4D},
                {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50}
        };

        for (byte[] signature : signatures) {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.upload(new MockMultipartFile("file", "image", "application/octet-stream", signature)));
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
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
