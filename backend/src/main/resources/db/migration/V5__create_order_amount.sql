CREATE TABLE order_amount (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    paid_amount DECIMAL(12,2) NOT NULL,
    platform_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    platform_commission DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    extra_commission DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    expected_amount DECIMAL(12,2) NULL,
    net_amount DECIMAL(12,2) GENERATED ALWAYS AS (
        paid_amount - platform_fee - platform_commission - extra_commission
    ) STORED,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_amount_order_id (order_id),
    CONSTRAINT fk_order_amount_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_order_amount_nonnegative CHECK (
        paid_amount >= 0 AND platform_fee >= 0 AND platform_commission >= 0
        AND extra_commission >= 0 AND (expected_amount IS NULL OR expected_amount >= 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单创建时金额快照';
