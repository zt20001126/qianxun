package com.qianxun.homestay.modules.room.service;

import com.qianxun.homestay.config.OssProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/** 为小程序生成私有 OSS 图片的短期读取地址，不向客户端暴露长期访问密钥。 */
@Service
public class RoomImageUrlService {

    private static final long URL_LIFETIME_SECONDS = 3600;
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private final OssProperties ossProperties;

    public RoomImageUrlService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    public List<String> resolveForApp(List<String> imageReferences) {
        if (imageReferences == null || imageReferences.isEmpty()) {
            return List.of();
        }
        List<String> resolved = new ArrayList<>(imageReferences.size());
        for (String imageReference : imageReferences) {
            String url = resolve(imageReference);
            if (url != null) {
                resolved.add(url);
            }
        }
        return List.copyOf(resolved);
    }

    private String resolve(String imageReference) {
        if (imageReference == null || imageReference.isBlank()) {
            return null;
        }
        if (imageReference.startsWith("https://") || imageReference.startsWith("http://")) {
            return imageReference;
        }
        if (!imageReference.startsWith("oss://")) {
            return null;
        }
        return createSignedUrl(imageReference.substring("oss://".length()));
    }

    private String createSignedUrl(String bucketAndKey) {
        String configuredBucket = ossProperties.getBucket();
        String accessKeyId = ossProperties.getAccessKeyId();
        String accessKeySecret = ossProperties.getAccessKeySecret();
        String endpoint = ossProperties.getEndpoint();
        if (isBlank(configuredBucket) || isBlank(accessKeyId) || isBlank(accessKeySecret) || isBlank(endpoint)) {
            return null;
        }

        int separator = bucketAndKey.indexOf('/');
        if (separator <= 0 || separator == bucketAndKey.length() - 1) {
            return null;
        }
        String imageBucket = bucketAndKey.substring(0, separator);
        String objectKey = bucketAndKey.substring(separator + 1);
        if (!configuredBucket.equals(imageBucket)) {
            return null;
        }

        try {
            URI endpointUri = URI.create(endpoint.contains("://") ? endpoint : "https://" + endpoint);
            String host = endpointUri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            if (!host.startsWith(configuredBucket + ".")) {
                host = configuredBucket + "." + host;
            }

            long expiresAt = Instant.now().getEpochSecond() + URL_LIFETIME_SECONDS;
            String canonicalResource = "/" + configuredBucket + "/" + objectKey;
            String stringToSign = "GET\n\n\n" + expiresAt + "\n" + canonicalResource;
            String signature = sign(stringToSign, accessKeySecret);
            String encodedKey = encodePath(objectKey);
            String scheme = endpointUri.getScheme() == null ? "https" : endpointUri.getScheme();
            String port = endpointUri.getPort() < 0 ? "" : ":" + endpointUri.getPort();
            String path = endpointUri.getPath() == null || endpointUri.getPath().equals("/")
                    ? ""
                    : endpointUri.getPath().replaceAll("/+$", "");
            return scheme + "://" + host + port + path + "/" + encodedKey
                    + "?Expires=" + expiresAt
                    + "&OSSAccessKeyId=" + encodeQuery(accessKeyId)
                    + "&Signature=" + encodeQuery(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成客房图片读取地址", exception);
        }
    }

    private String sign(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return java.util.Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String encodePath(String objectKey) {
        return URLEncoder.encode(objectKey, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/");
    }

    private String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
