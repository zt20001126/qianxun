package com.qianxun.homestay.modules.storage.service;

import org.springframework.web.multipart.MultipartFile;

/** 校验并上传客房和商城使用的展示图片。 */
public interface CatalogImageUploadService {

    /** 将图片写入指定业务目录并返回稳定的 COS 访问地址。 */
    String upload(MultipartFile file, Target target);

    /** 由服务端固定各业务的目录和错误提示用语。 */
    enum Target {
        ROOM("rooms", "客房图片"),
        MALL_PRODUCT("mall/products", "商品图片");

        private final String directory;
        private final String label;

        Target(String directory, String label) {
            this.directory = directory;
            this.label = label;
        }

        public String directory() {
            return directory;
        }

        public String label() {
            return label;
        }
    }
}
