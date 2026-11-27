package com.qianxun.homestay.modules.mall.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.mall.service.MallProductImageUploadService;
import com.qianxun.homestay.modules.storage.service.CosStorageService;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 校验商城商品图片并写入腾讯云 COS。 */
@Service
public class MallProductImageUploadServiceImpl implements MallProductImageUploadService {

    private final CosStorageService cosStorageService;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    public MallProductImageUploadServiceImpl(CosStorageService cosStorageService) {
        this.cosStorageService = cosStorageService;
    }

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择商品图片");
        }
        ImageType imageType = detectType(file);
        String objectKey = "mall/products/" + LocalDate.now(BUSINESS_ZONE) + "/" + UUID.randomUUID() + "." + imageType.extension;
        // 先校验公开域名，避免域名错误导致上传后留下无引用文件。
        String url = cosStorageService.publicUrl(objectKey);
        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
            cosStorageService.upload(objectKey, input, file.getSize(), imageType.contentType);
            return url;
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法读取商品图片");
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

    private boolean startsWith(byte[] value, int... expected) {
        if (value.length < expected.length) return false;
        for (int index = 0; index < expected.length; index++) if (unsigned(value[index]) != expected[index]) return false;
        return true;
    }

    private String ascii(byte[] value, int start, int end) {
        return new String(value, start, end - start, StandardCharsets.US_ASCII);
    }

    private int unsigned(byte value) { return value & 0xFF; }

    private record ImageType(String extension, String contentType) { }
}
