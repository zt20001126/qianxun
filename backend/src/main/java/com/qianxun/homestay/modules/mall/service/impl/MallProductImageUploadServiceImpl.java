package com.qianxun.homestay.modules.mall.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.qianxun.homestay.config.OssProperties;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.service.MallProductImageUploadService;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 将商城商品图片写入阿里云 OSS，并返回公开访问地址。 */
@Service
public class MallProductImageUploadServiceImpl implements MallProductImageUploadService {

    private final OssProperties properties;

    public MallProductImageUploadServiceImpl(OssProperties properties) {
        this.properties = properties;
    }

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择商品图片");
        }
        ImageType imageType = detectType(file);
        String endpoint = properties.getEndpoint();
        String bucket = properties.getBucket();
        String accessKeyId = properties.getAccessKeyId();
        String accessKeySecret = properties.getAccessKeySecret();
        if (isBlank(endpoint) || isBlank(bucket) || isBlank(accessKeyId) || isBlank(accessKeySecret)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "图片存储服务尚未配置");
        }

        String objectKey = "mall/products/" + LocalDate.now() + "/" + UUID.randomUUID() + "." + imageType.extension;
        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(imageType.contentType);
            client.putObject(bucket, objectKey, input, metadata);
            return publicUrl(endpoint, bucket, objectKey);
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "商品图片上传失败");
        } finally {
            client.shutdown();
        }
    }

    private ImageType detectType(MultipartFile file) {
        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
            input.mark(16);
            byte[] header = input.readNBytes(12);
            if (header.length >= 3 && unsigned(header[0]) == 0xFF && unsigned(header[1]) == 0xD8 && unsigned(header[2]) == 0xFF) {
                return new ImageType("jpg", "image/jpeg");
            }
            if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return new ImageType("png", "image/png");
            if (startsWith(header, 0x47, 0x49, 0x46, 0x38)) return new ImageType("gif", "image/gif");
            if (startsWith(header, 0x42, 0x4D)) return new ImageType("bmp", "image/bmp");
            if (header.length >= 12 && ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 12).equals("WEBP")) {
                return new ImageType("webp", "image/webp");
            }
            throw new BusinessException(HttpStatus.BAD_REQUEST, "仅支持 JPG、PNG、GIF、BMP 或 WebP 图片");
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法读取商品图片");
        }
    }

    private String publicUrl(String endpoint, String bucket, String objectKey) {
        String base = properties.getPublicBaseUrl();
        if (isBlank(base)) {
            URI endpointUri = URI.create(endpoint.contains("://") ? endpoint : "https://" + endpoint);
            String host = endpointUri.getHost();
            if (host == null) throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "OSS 访问域名配置无效");
            if (!host.startsWith(bucket + ".")) host = bucket + "." + host;
            base = endpointUri.getScheme() + "://" + host;
            if (endpointUri.getPort() >= 0) base += ":" + endpointUri.getPort();
            if (endpointUri.getPath() != null && !endpointUri.getPath().equals("/")) base += endpointUri.getPath().replaceAll("/+$", "");
        }
        return base.replaceAll("/+$", "") + "/" + encodePath(objectKey);
    }

    private String encodePath(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private boolean startsWith(byte[] value, int... expected) {
        if (value.length < expected.length) return false;
        for (int index = 0; index < expected.length; index++) if (unsigned(value[index]) != expected[index]) return false;
        return true;
    }

    private String ascii(byte[] value, int start, int end) {
        return new String(value, start, end - start, StandardCharsets.US_ASCII);
    }

    private int unsigned(byte value) { return value & 0xFF; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private record ImageType(String extension, String contentType) { }
}
