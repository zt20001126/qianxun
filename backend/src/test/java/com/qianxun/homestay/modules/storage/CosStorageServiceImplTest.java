package com.qianxun.homestay.modules.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.config.CosProperties;
import com.qianxun.homestay.modules.storage.service.impl.CosStorageServiceImpl;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

/** 验证独立 COS 服务的配置、上传和地址行为。 */
class CosStorageServiceImplTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CosProperties.class)
    @Import(CosStorageServiceImpl.class)
    static class StorageTestConfiguration { }

    @Test
    void springContextStartsWithoutCosConfiguration() {
        new ApplicationContextRunner().withUserConfiguration(StorageTestConfiguration.class)
                .run(context -> assertNotNull(context.getBean(CosStorageServiceImpl.class)));
    }

    private CosProperties configured() {
        CosProperties properties = new CosProperties();
        properties.setRegion("ap-shanghai");
        properties.setBucket("example-1234567890");
        properties.setSecretId("test-id");
        properties.setSecretKey("test-secret");
        return properties;
    }

    @Test
    void emptyConfigurationDoesNotPreventConstructionAndFailsOnlyWhenCalled() {
        CosStorageServiceImpl service = new CosStorageServiceImpl(new CosProperties());
        BusinessException exception = assertThrows(BusinessException.class, () -> service.publicUrl("test.png"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void encodesObjectKeyAndSupportsCustomBaseUrl() {
        CosProperties properties = configured();
        CosStorageServiceImpl service = new CosStorageServiceImpl(properties);
        assertEquals("https://example-1234567890.cos.ap-shanghai.myqcloud.com/a/%E4%B8%AD%20%2B.png",
                service.publicUrl("a/中 +.png"));
        properties.setPublicBaseUrl("https://images.example.com/prefix/");
        assertEquals("https://images.example.com/prefix/a.png", service.publicUrl("a.png"));
        properties.setPublicBaseUrl("http://images.example.com");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(BusinessException.class, () -> service.publicUrl("a.png")).getStatus());
    }

    @Test
    void uploadsMetadataAndClosesClient() {
        CosStorageServiceImpl service = new CosStorageServiceImpl(configured());
        try (MockedConstruction<COSClient> clients = mockConstruction(COSClient.class)) {
            ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {1, 2});
            service.upload("folder/image.png", input, 2, "image/png");
            COSClient client = clients.constructed().get(0);
            verify(client).putObject(eq("example-1234567890"), eq("folder/image.png"), eq(input), any(ObjectMetadata.class));
            verify(client).shutdown();
        }
    }

    @Test
    void uploadFailureIsSanitizedAndClientClosed() {
        CosStorageServiceImpl service = new CosStorageServiceImpl(configured());
        try (MockedConstruction<COSClient> clients = mockConstruction(COSClient.class, (client, context) ->
                doThrow(new IllegalStateException("secret-detail")).when(client)
                        .putObject(any(String.class), any(String.class), any(ByteArrayInputStream.class), any(ObjectMetadata.class)))) {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.upload("image.png", new ByteArrayInputStream(new byte[] {1}), 1, "image/png"));
            assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
            assertFalse(exception.getMessage().contains("secret-detail"));
            verify(clients.constructed().get(0)).shutdown();
        }
    }

    @Test
    void uploadsLargeObjectsInParts() {
        CosStorageServiceImpl service = new CosStorageServiceImpl(configured());
        try (MockedConstruction<COSClient> clients = mockConstruction(COSClient.class, (client, context) -> {
            InitiateMultipartUploadResult start = new InitiateMultipartUploadResult();
            start.setUploadId("upload-id");
            when(client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(start);
            when(client.uploadPart(any(UploadPartRequest.class))).thenReturn(new UploadPartResult());
        })) {
            byte[] content = new byte[8 * 1024 * 1024 + 1];
            service.upload("large.png", new ByteArrayInputStream(content), content.length, "image/png");
            COSClient client = clients.constructed().get(0);
            verify(client, times(2)).uploadPart(any(UploadPartRequest.class));
            verify(client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
            verify(client).shutdown();
        }
    }

    @Test
    void abortsMultipartUploadWhenSourceIsIncomplete() {
        CosStorageServiceImpl service = new CosStorageServiceImpl(configured());
        try (MockedConstruction<COSClient> clients = mockConstruction(COSClient.class, (client, context) -> {
            InitiateMultipartUploadResult start = new InitiateMultipartUploadResult();
            start.setUploadId("upload-id");
            when(client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(start);
        })) {
            assertThrows(BusinessException.class, () -> service.upload("large.png",
                    new ByteArrayInputStream(new byte[0]), 8 * 1024 * 1024 + 1, "image/png"));
            verify(clients.constructed().get(0)).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        }
    }

    @Test
    void sdkGeneratesHttpsSignedUrlAndRejectsInvalidArguments() {
        CosStorageServiceImpl service = new CosStorageServiceImpl(configured());
        String url = service.signedReadUrl("目录/a b.png", Duration.ofMinutes(5));
        assertTrue(url.startsWith("https://example-1234567890.cos.ap-shanghai.myqcloud.com/"));
        assertTrue(url.contains("q-signature="));
        assertFalse(url.contains("test-secret"));
        assertThrows(BusinessException.class, () -> service.signedReadUrl("x", Duration.ZERO));
        assertThrows(BusinessException.class, () -> service.signedReadUrl("x", Duration.ofDays(8)));
        assertThrows(BusinessException.class, () -> service.publicUrl("/invalid"));
        assertThrows(BusinessException.class, () -> service.upload("x", null, 1, "image/png"));
    }
}
