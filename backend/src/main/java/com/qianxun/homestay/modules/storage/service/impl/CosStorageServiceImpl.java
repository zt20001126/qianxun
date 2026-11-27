package com.qianxun.homestay.modules.storage.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.region.Region;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.config.CosProperties;
import com.qianxun.homestay.modules.storage.service.CosStorageService;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 独立封装腾讯云 COS，避免影响现有阿里云存储链路。 */
@Service
public class CosStorageServiceImpl implements CosStorageService {
    private static final int PART_SIZE = 8 * 1024 * 1024;
    private static final int MAX_PARTS = 10000;
    private static final Duration MAX_SIGNATURE_LIFETIME = Duration.ofDays(7);
    private final CosProperties properties;

    public CosStorageServiceImpl(CosProperties properties) {
        this.properties = properties;
    }

    @Override
    public void upload(String objectKey, InputStream input, long contentLength, String contentType) {
        validateKey(objectKey);
        if (input == null || contentLength < 0 || blank(contentType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COS 上传参数无效");
        }
        validateCredentials();
        COSClient client = createClient();
        try {
            // 上传不更改对象 ACL，权限由存储桶或对象策略决定。
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            metadata.setContentType(contentType);
            if (contentLength <= PART_SIZE) {
                client.putObject(properties.getBucket(), objectKey, input, metadata);
            } else {
                uploadMultipart(client, objectKey, input, contentLength, metadata);
            }
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "腾讯云 COS 上传失败");
        } finally {
            client.shutdown();
        }
    }

    /** 分片大小随文件增长，限制内存占用并遵守 COS 分片数量限制。 */
    private void uploadMultipart(COSClient client, String key, InputStream input, long length,
            ObjectMetadata metadata) throws IOException {
        long partSize = Math.max(PART_SIZE, length / MAX_PARTS + (length % MAX_PARTS == 0 ? 0 : 1));
        if (partSize > Integer.MAX_VALUE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "文件超出 COS 分片处理范围");
        }
        String bucket = properties.getBucket();
        String uploadId = client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key, metadata)).getUploadId();
        boolean completed = false;
        try {
            List<PartETag> tags = new ArrayList<>();
            long remaining = length;
            int partNumber = 1;
            while (remaining > 0) {
                int expected = (int) Math.min(partSize, remaining);
                byte[] chunk = input.readNBytes(expected);
                if (chunk.length != expected) {
                    throw new IOException("文件内容长度不足");
                }
                UploadPartRequest request = new UploadPartRequest().withBucketName(bucket).withKey(key)
                        .withUploadId(uploadId).withPartNumber(partNumber).withPartSize(chunk.length)
                        .withInputStream(new ByteArrayInputStream(chunk));
                tags.add(client.uploadPart(request).getPartETag());
                remaining -= chunk.length;
                partNumber++;
            }
            client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucket, key, uploadId, tags));
            completed = true;
        } finally {
            if (!completed) {
                // 未完成的分片可能占用空间，失败时尽力中止。
                try { client.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId)); }
                catch (RuntimeException ignored) { /* 保留原始上传错误。 */ }
            }
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        validateKey(objectKey);
        validateLocation();
        String base = properties.getPublicBaseUrl();
        if (blank(base)) {
            base = "https://" + properties.getBucket() + ".cos." + properties.getRegion() + ".myqcloud.com";
        }
        try {
            URI uri = URI.create(base);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "COS 公开访问域名配置无效");
        }
        return base.replaceAll("/+$", "") + "/" + URLEncoder.encode(objectKey, StandardCharsets.UTF_8)
                .replace("+", "%20").replace("%2F", "/");
    }

    @Override
    public String signedReadUrl(String objectKey, Duration lifetime) {
        validateKey(objectKey);
        if (lifetime == null || lifetime.isNegative() || lifetime.isZero()
                || lifetime.compareTo(MAX_SIGNATURE_LIFETIME) > 0 || lifetime.toMillis() < 1000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COS 签名有效期无效");
        }
        validateCredentials();
        COSClient client = createClient();
        try {
            return client.generatePresignedUrl(properties.getBucket(), objectKey,
                    Date.from(Instant.now().plus(lifetime)), HttpMethodName.GET).toExternalForm();
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "腾讯云 COS 签名生成失败");
        } finally {
            client.shutdown();
        }
    }

    private COSClient createClient() {
        ClientConfig config = new ClientConfig(new Region(properties.getRegion()));
        config.setHttpProtocol(HttpProtocol.https);
        return new COSClient(new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey()), config);
    }

    private void validateCredentials() {
        validateLocation();
        if (blank(properties.getSecretId()) || blank(properties.getSecretKey())) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "腾讯云 COS 密钥尚未配置");
        }
    }

    private void validateLocation() {
        if (blank(properties.getRegion()) || blank(properties.getBucket())) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "腾讯云 COS 地域或存储桶尚未配置");
        }
    }

    private void validateKey(String objectKey) {
        if (blank(objectKey) || objectKey.startsWith("/")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COS 对象键无效");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
