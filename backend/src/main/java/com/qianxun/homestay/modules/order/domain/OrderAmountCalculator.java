package com.qianxun.homestay.modules.order.domain;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.order.dto.OrderAmountRequest;
import com.qianxun.homestay.modules.order.entity.OrderAmountEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;

/** 按第三方订单规则生成不可变的创建时金额快照。 */
public class OrderAmountCalculator {

    /** 计算并校验金额快照；净到手金额不能为负数。 */
    public OrderAmountEntity calculate(OrderAmountRequest request, Long orderId, boolean hasCreator) {
        BigDecimal paid = amount(request.paidAmount());
        BigDecimal platformFee = optionalAmount(request.platformFee());
        BigDecimal platformCommission = hasCreator ? optionalAmount(request.platformCommission()) : BigDecimal.ZERO;
        BigDecimal expected = hasCreator ? requiredExpectedAmount(request.expectedAmount()) : null;
        BigDecimal extraCommission = BigDecimal.ZERO;
        if (hasCreator) {
            extraCommission = paid.subtract(platformFee).subtract(platformCommission).subtract(expected)
                    .max(BigDecimal.ZERO);
        }
        BigDecimal netAmount = paid.subtract(platformFee).subtract(platformCommission).subtract(extraCommission);
        if (netAmount.signum() < 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "商家到手金额不能为负，请核对订单费用");
        }

        OrderAmountEntity result = new OrderAmountEntity();
        result.setOrderId(orderId);
        result.setPaidAmount(paid);
        result.setPlatformFee(platformFee);
        result.setPlatformCommission(platformCommission);
        result.setExtraCommission(extraCommission.setScale(2, RoundingMode.UNNECESSARY));
        result.setExpectedAmount(expected);
        return result;
    }

    private BigDecimal requiredExpectedAmount(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "选择达人时必须填写预期收入");
        }
        return amount(value);
    }

    private BigDecimal optionalAmount(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO.setScale(2) : amount(value);
    }

    private BigDecimal amount(String value) {
        BigDecimal parsed = new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
        if (parsed.signum() < 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "金额不能为负数");
        }
        return parsed;
    }
}
