package com.qianxun.homestay.modules.roomblock.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.roomblock.dto.RoomBlockSaveRequest;
import com.qianxun.homestay.modules.roomblock.entity.RoomBlockEntity;
import com.qianxun.homestay.modules.roomblock.mapper.RoomBlockMapper;
import com.qianxun.homestay.modules.roomblock.service.RoomBlockService;
import com.qianxun.homestay.modules.roomblock.vo.RoomBlockVO;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 在事务中锁定房间并维护管理员封房记录。 */
@Service
public class RoomBlockServiceImpl implements RoomBlockService {

    private final RoomBlockMapper roomBlockMapper;
    private final AvailabilityService availabilityService;

    public RoomBlockServiceImpl(RoomBlockMapper roomBlockMapper, AvailabilityService availabilityService) {
        this.roomBlockMapper = roomBlockMapper;
        this.availabilityService = availabilityService;
    }

    @Override
    public List<RoomBlockVO> findRoomBlocks(Long roomId, YearMonth month) {
        return roomBlockMapper.findAll(
                        roomId,
                        month == null ? null : month.atDay(1),
                        month == null ? null : month.plusMonths(1).atDay(1))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public RoomBlockVO createRoomBlock(RoomBlockSaveRequest request) {
        availabilityService.validateStayRange(request.startDate(), request.endDate());
        availabilityService.lockRoomsForUpdate(List.of(request.roomId()));
        if (availabilityService.hasConflict(request.roomId(), request.startDate(), request.endDate(), null)) {
            throw new BusinessException(HttpStatus.CONFLICT, "封房区间与已有订单、封房或有效锁房冲突");
        }
        RoomBlockEntity roomBlock = toEntity(request);
        roomBlockMapper.insert(roomBlock);
        return toVO(roomBlockMapper.findById(roomBlock.getId()));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomBlockVO updateRoomBlock(Long id, RoomBlockSaveRequest request) {
        availabilityService.validateStayRange(request.startDate(), request.endDate());
        RoomBlockEntity initial = requireRoomBlock(id);
        availabilityService.lockRoomsForUpdate(List.of(initial.getRoomId(), request.roomId()));
        RoomBlockEntity current = roomBlockMapper.findByIdForUpdate(id);
        if (current == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "封房记录不存在");
        }
        if (!current.getRoomId().equals(initial.getRoomId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "封房记录已被修改，请刷新后重试");
        }
        if (availabilityService.hasConflict(
                request.roomId(), request.startDate(), request.endDate(), id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "封房区间与已有订单、封房或有效锁房冲突");
        }
        current.setRoomId(request.roomId());
        current.setStartDate(request.startDate());
        current.setEndDate(request.endDate());
        current.setReason(trimToNull(request.reason()));
        roomBlockMapper.update(current);
        return toVO(roomBlockMapper.findById(id));
    }

    @Override
    @Transactional
    public void deleteRoomBlock(Long id) {
        RoomBlockEntity initial = requireRoomBlock(id);
        availabilityService.lockRoomsForUpdate(List.of(initial.getRoomId()));
        RoomBlockEntity current = roomBlockMapper.findByIdForUpdate(id);
        if (current == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "封房记录不存在");
        }
        if (!current.getRoomId().equals(initial.getRoomId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "封房记录已被修改，请刷新后重试");
        }
        roomBlockMapper.delete(id);
    }

    private RoomBlockEntity requireRoomBlock(Long id) {
        RoomBlockEntity roomBlock = roomBlockMapper.findById(id);
        if (roomBlock == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "封房记录不存在");
        }
        return roomBlock;
    }

    private RoomBlockEntity toEntity(RoomBlockSaveRequest request) {
        RoomBlockEntity roomBlock = new RoomBlockEntity();
        roomBlock.setRoomId(request.roomId());
        roomBlock.setStartDate(request.startDate());
        roomBlock.setEndDate(request.endDate());
        roomBlock.setReason(trimToNull(request.reason()));
        return roomBlock;
    }

    private RoomBlockVO toVO(RoomBlockEntity roomBlock) {
        return new RoomBlockVO(
                roomBlock.getId(), roomBlock.getRoomId(), roomBlock.getStartDate(), roomBlock.getEndDate(),
                roomBlock.getReason(), ApiTime.fromShanghai(roomBlock.getCreatedAt()),
                ApiTime.fromShanghai(roomBlock.getUpdatedAt()));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
