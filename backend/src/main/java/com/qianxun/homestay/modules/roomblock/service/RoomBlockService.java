package com.qianxun.homestay.modules.roomblock.service;

import com.qianxun.homestay.modules.roomblock.dto.RoomBlockSaveRequest;
import com.qianxun.homestay.modules.roomblock.vo.RoomBlockVO;
import java.time.YearMonth;
import java.util.List;

/** 管理员主动封房记录的查询和维护业务接口。 */
public interface RoomBlockService {

    /** 查询封房记录，可按房间和月份筛选。 */
    List<RoomBlockVO> findRoomBlocks(Long roomId, YearMonth month);

    /** 新增封房并复核整个住宿区间的冲突。 */
    RoomBlockVO createRoomBlock(RoomBlockSaveRequest request);

    /** 修改封房区间并排除自身后复核库存冲突。 */
    RoomBlockVO updateRoomBlock(Long id, RoomBlockSaveRequest request);

    /** 删除整段封房记录。 */
    void deleteRoomBlock(Long id);
}
