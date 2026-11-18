package com.qianxun.homestay.modules.payment.service.impl;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.util.ApiTime;
import com.qianxun.homestay.modules.payment.entity.PaymentEntity;
import com.qianxun.homestay.modules.payment.enums.PaymentStatus;
import com.qianxun.homestay.modules.payment.mapper.PaymentMapper;
import com.qianxun.homestay.modules.payment.service.PaymentQueryService;
import com.qianxun.homestay.modules.payment.vo.PaymentListItemVO;
import com.qianxun.homestay.modules.payment.vo.PaymentPageVO;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 将支付流水查询条件转换为后台列表视图。 */
@Service
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentMapper paymentMapper;

    public PaymentQueryServiceImpl(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentPageVO findAdminPayments(
            PaymentStatus status, LocalDate startDate, LocalDate endDate, int page, int size) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "结束日期不能早于开始日期");
        }
        LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endBefore = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        long offset = (long) (page - 1) * size;
        List<PaymentListItemVO> list = paymentMapper.findAdminPage(status, startAt, endBefore, offset, size).stream()
                .map(this::toListItem)
                .toList();
        long total = paymentMapper.countAdmin(status, startAt, endBefore);
        return new PaymentPageVO(list, total, page, size);
    }

    private PaymentListItemVO toListItem(PaymentEntity payment) {
        String amount = payment.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        return new PaymentListItemVO(payment.getId(), payment.getPaymentNo(), payment.getOrderId(), amount,
                payment.getStatus(), ApiTime.fromShanghai(payment.getPaidAt()));
    }
}
