package com.qianxun.homestay.modules.room.dto;

import com.qianxun.homestay.modules.room.entity.RoomEntity;
import java.math.BigDecimal;
import java.util.List;

/** 客房展示缓存快照，图片保留原始引用，不包含实时房态。 */
public record RoomCatalogCacheDTO(
        /** 客房编号。 */ Long id,
        /** 房号。 */ String number,
        /** 客房名称。 */ String name,
        /** 楼层。 */ Integer floor,
        /** 每晚基础价格。 */ BigDecimal price,
        /** 最大入住人数。 */ Integer capacity,
        /** 床型。 */ String bedInfo,
        /** 房间面积。 */ BigDecimal area,
        /** 设施列表。 */ List<String> facilities,
        /** 原始图片引用。 */ List<String> images,
        /** 客房介绍。 */ String description) {
    /** 将持久化资料复制为展示快照。 */
    public static RoomCatalogCacheDTO from(RoomEntity room) {
        return new RoomCatalogCacheDTO(room.getId(), room.getNumber(), room.getName(), room.getFloor(),
                room.getPrice(), room.getCapacity(), room.getBedInfo(), room.getArea(),
                room.getFacilities() == null ? List.of() : List.copyOf(room.getFacilities()),
                room.getImages() == null ? List.of() : List.copyOf(room.getImages()), room.getDescription());
    }
}
