package com.qianxun.homestay.modules.room.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.cache.DisplayCacheService;
import com.qianxun.homestay.config.DisplayCacheProperties;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.availability.domain.StayDateRange;
import com.qianxun.homestay.modules.availability.service.AvailabilityService;
import com.qianxun.homestay.modules.room.dto.RoomSaveRequest;
import com.qianxun.homestay.modules.room.dto.RoomCatalogCacheDTO;
import com.qianxun.homestay.modules.room.entity.RoomEntity;
import com.qianxun.homestay.modules.room.mapper.RoomMapper;
import com.qianxun.homestay.modules.room.service.RoomImageUrlService;
import com.qianxun.homestay.modules.room.service.RoomPriceService;
import com.qianxun.homestay.modules.room.service.RoomService;
import com.qianxun.homestay.modules.room.vo.AppRoomDetailVO;
import com.qianxun.homestay.modules.room.vo.AppRoomSummaryVO;
import com.qianxun.homestay.modules.room.vo.RoomAdminVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 客房基础资料查询和维护；日期可订判断统一委托房态模块。 */
@Service
public class RoomServiceImpl implements RoomService {

    private static final Set<String> PROPERTY_ROOM_NUMBERS = Set.of("101", "102", "103", "201", "202", "203");

    private static final String CACHE_DOMAIN = "rooms";
    private final DisplayCacheProperties cacheProperties;
    private final DisplayCacheService cache;
    private final RoomMapper roomMapper;
    private final RoomImageUrlService roomImageUrlService;
    private final AvailabilityService availabilityService;
    private final RoomPriceService roomPriceService;

    public RoomServiceImpl(
            RoomMapper roomMapper,
            RoomImageUrlService roomImageUrlService,
            AvailabilityService availabilityService, RoomPriceService roomPriceService,
            DisplayCacheService cache, DisplayCacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
        this.cache = cache;
        this.roomMapper = roomMapper;
        this.roomImageUrlService = roomImageUrlService;
        this.availabilityService = availabilityService;
        this.roomPriceService = roomPriceService;
    }

    @Override
    public List<RoomAdminVO> findAdminRooms() {
        return roomMapper.findAll().stream().map(this::toAdminVO).toList();
    }

    @Override
    public RoomAdminVO getAdminRoom(Long id) {
        return toAdminVO(requireRoom(id));
    }

    @Override
    public List<AppRoomSummaryVO> findAppRooms(LocalDate checkIn, LocalDate checkOut) {
        if ((checkIn == null) != (checkOut == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "入住日期和退房日期必须同时提供");
        }
        if (checkIn != null) {
            availabilityService.validateMiniProgramRange(checkIn, checkOut);
            // 带日期的查询返回可用于比较的实时房费，直接读取两档价格。
            List<RoomEntity> availableRooms = roomMapper.findEnabled().stream()
                    .filter(room -> availabilityService.isAvailable(room.getId(), checkIn, checkOut))
                    .toList();
            StayDateRange stay = new StayDateRange(checkIn, checkOut);
            Map<Long, BigDecimal> amounts = roomPriceService.quoteAll(availableRooms, stay);
            return availableRooms.stream()
                    .map(room -> toAppSummaryVO(RoomCatalogCacheDTO.from(room), true, amounts.get(room.getId())))
                    .toList();
        }
        return enabledRooms().stream().map(room -> toAppSummaryVO(room, null, null)).toList();
    }

    @Override
    public AppRoomDetailVO getAppRoom(Long id) {
        RoomCatalogCacheDTO room = enabledRooms().stream().filter(item -> item.id().equals(id)).findFirst().orElse(null);
        if (room == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "房间不存在");
        }
        return new AppRoomDetailVO(
                room.id(), room.number(), room.name(), room.floor(), formatPrice(room.price()),
                room.capacity(), room.bedInfo(), room.area(), emptyIfNull(room.facilities()),
                roomImageUrlService.resolveForApp(room.images()), room.description());
    }

    @Override
    @Transactional
    public RoomAdminVO createRoom(RoomSaveRequest request) {
        RoomEntity room = toEntity(request);
        validateRoomNumberAndFloor(room);
        if (roomMapper.existsNumber(room.getNumber())) {
            throw new BusinessException(HttpStatus.CONFLICT, "房号已存在");
        }
        try {
            roomMapper.insert(room);
            cache.invalidateAfterCommit(CACHE_DOMAIN);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "房号已存在");
        }
        return getAdminRoom(room.getId());
    }

    @Override
    @Transactional
    public RoomAdminVO updateRoom(Long id, RoomSaveRequest request) {
        requireRoom(id);
        RoomEntity room = toEntity(request);
        room.setId(id);
        validateRoomNumberAndFloor(room);
        if (roomMapper.existsNumberForOtherRoom(room.getNumber(), id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "房号已存在");
        }
        try {
            roomMapper.update(room);
            // 旧客户端省略节假日价时保留原配置，避免覆盖独立维护的价格。
            if (request.hasHolidayPrice()) {
                roomMapper.updateHolidayPrice(id, room.getHolidayPrice());
            }
            cache.invalidateAfterCommit(CACHE_DOMAIN);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "房号已存在");
        }
        return getAdminRoom(id);
    }

    @Override
    @Transactional
    public RoomAdminVO setRoomEnabled(Long id, boolean enabled) {
        requireRoom(id);
        roomMapper.updateEnabled(id, enabled);
        cache.invalidateAfterCommit(CACHE_DOMAIN);
        return getAdminRoom(id);
    }

    /** 复用客房基础资料，可订状态仍实时查询。 */
    private List<RoomCatalogCacheDTO> enabledRooms() {
        return Arrays.asList(cache.get(CACHE_DOMAIN, "catalog", cacheProperties.getRoomTtl(), RoomCatalogCacheDTO[].class,
                () -> roomMapper.findEnabled().stream().map(RoomCatalogCacheDTO::from).toArray(RoomCatalogCacheDTO[]::new)));
    }

    private RoomEntity requireRoom(Long id) {
        RoomEntity room = roomMapper.findById(id);
        if (room == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "房间不存在");
        }
        return room;
    }

    private RoomEntity toEntity(RoomSaveRequest request) {
        RoomEntity room = new RoomEntity();
        room.setNumber(request.getNumber().trim());
        room.setName(request.getName().trim());
        room.setFloor(request.getFloor());
        room.setPrice(request.getPrice().setScale(2));
        room.setHolidayPrice(request.getHolidayPrice());
        room.setCapacity(request.getCapacity());
        room.setBedInfo(trimToNull(request.getBedInfo()));
        room.setArea(request.getArea() == null ? null : request.getArea().setScale(2));
        room.setFacilities(normalizeList(request.getFacilities()));
        room.setImages(normalizeList(request.getImages()));
        room.setDescription(trimToNull(request.getDescription()));
        return room;
    }

    private void validateRoomNumberAndFloor(RoomEntity room) {
        if (!PROPERTY_ROOM_NUMBERS.contains(room.getNumber())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "房号仅支持 101、102、103、201、202、203");
        }
        int expectedFloor = room.getNumber().startsWith("1") ? 1 : 2;
        if (room.getFloor() != expectedFloor) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "楼层必须与房号对应");
        }
    }

    private RoomAdminVO toAdminVO(RoomEntity room) {
        return new RoomAdminVO(
                room.getId(), room.getNumber(), room.getName(), room.getFloor(), formatPrice(room.getPrice()),
                room.getHolidayPrice() == null ? null : formatPrice(room.getHolidayPrice()),
                room.getCapacity(), room.getBedInfo(), room.getArea(), emptyIfNull(room.getFacilities()),
                emptyIfNull(room.getImages()), room.getDescription(), room.getEnabled(),
                ApiTime.fromShanghai(room.getCreatedAt()), ApiTime.fromShanghai(room.getUpdatedAt()));
    }

    private AppRoomSummaryVO toAppSummaryVO(RoomCatalogCacheDTO room, Boolean available, BigDecimal roomAmount) {
        return new AppRoomSummaryVO(
                room.id(), room.number(), room.name(), formatPrice(room.price()), room.capacity(),
                room.bedInfo(), roomImageUrlService.resolveForApp(room.images()), available,
                roomAmount == null ? null : formatPrice(roomAmount));
    }

    private String formatPrice(BigDecimal price) {
        return price.setScale(2).toPlainString();
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    private List<String> emptyIfNull(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
