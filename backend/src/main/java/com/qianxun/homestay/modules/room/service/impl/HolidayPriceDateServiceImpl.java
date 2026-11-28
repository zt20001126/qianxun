package com.qianxun.homestay.modules.room.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.room.dto.HolidayPriceDatesUpdateRequest;
import com.qianxun.homestay.modules.room.mapper.HolidayPriceDateMapper;
import com.qianxun.homestay.modules.room.service.HolidayPriceDateService;
import com.qianxun.homestay.modules.room.vo.HolidayPriceDatesVO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 校验并原子保存全店节假日计价日期。 */
@Service
public class HolidayPriceDateServiceImpl implements HolidayPriceDateService {

    private final HolidayPriceDateMapper mapper;

    public HolidayPriceDateServiceImpl(HolidayPriceDateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public HolidayPriceDatesVO findMonth(YearMonth month) {
        return new HolidayPriceDatesVO(mapper.findDates(month.atDay(1), month.atEndOfMonth()));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HolidayPriceDatesVO update(HolidayPriceDatesUpdateRequest request) {
        YearMonth month = YearMonth.parse(request.month());
        Map<LocalDate, Boolean> changes = new TreeMap<>();
        collectChanges(changes, request.addDates(), month, true);
        collectChanges(changes, request.removeDates(), month, false);

        // 只更新本次编辑涉及的日期，多个请求按同一日期顺序写入。
        changes.forEach((date, enabled) -> {
            if (enabled) {
                mapper.enable(date);
            } else {
                mapper.disable(date);
            }
        });
        return findMonth(month);
    }

    private void collectChanges(
            Map<LocalDate, Boolean> changes, List<LocalDate> dates, YearMonth month, boolean enabled) {
        for (LocalDate date : dates) {
            if (!YearMonth.from(date).equals(month)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "变更日期必须属于指定月份");
            }
            Boolean previous = changes.putIfAbsent(date, enabled);
            if (previous != null && previous != enabled) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "同一天不能同时勾选和取消");
            }
        }
    }
}
