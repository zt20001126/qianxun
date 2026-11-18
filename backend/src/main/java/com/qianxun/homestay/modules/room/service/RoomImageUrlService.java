package com.qianxun.homestay.modules.room.service;

import java.util.List;

/** 客房图片地址处理业务接口。 */
public interface RoomImageUrlService {

    /** 将图片引用转换为小程序可读取的地址。 */
    List<String> resolveForApp(List<String> imageReferences);
}
