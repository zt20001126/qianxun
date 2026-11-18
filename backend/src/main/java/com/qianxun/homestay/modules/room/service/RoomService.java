package com.qianxun.homestay.modules.room.service;

import com.qianxun.homestay.modules.room.dto.RoomSaveRequest;
import com.qianxun.homestay.modules.room.vo.AppRoomDetailVO;
import com.qianxun.homestay.modules.room.vo.AppRoomSummaryVO;
import com.qianxun.homestay.modules.room.vo.RoomAdminVO;
import java.time.LocalDate;
import java.util.List;

/** 客房基础资料查询和维护业务接口。 */
public interface RoomService {

    /** 查询后台客房列表。 */
    List<RoomAdminVO> findAdminRooms();

    /** 查询单个后台客房。 */
    RoomAdminVO getAdminRoom(Long id);

    /** 查询小程序可展示的客房列表。 */
    List<AppRoomSummaryVO> findAppRooms(LocalDate checkIn, LocalDate checkOut);

    /** 查询小程序客房详情。 */
    AppRoomDetailVO getAppRoom(Long id);

    /** 创建客房资料。 */
    RoomAdminVO createRoom(RoomSaveRequest request);

    /** 更新客房资料。 */
    RoomAdminVO updateRoom(Long id, RoomSaveRequest request);

    /** 更新客房启用状态。 */
    RoomAdminVO setRoomEnabled(Long id, boolean enabled);
}
