CREATE TABLE points_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NULL,
    type VARCHAR(30) NOT NULL,
    points INT NOT NULL,
    growth_points INT NOT NULL DEFAULT 0,
    balance_after INT UNSIGNED NOT NULL,
    amount DECIMAL(12,2) NULL,
    discount_amount DECIMAL(12,2) NULL,
    ref_no VARCHAR(100) NULL,
    remark VARCHAR(300) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_points_log_ref_no (ref_no),
    KEY idx_points_log_member_created (member_id, created_at),
    KEY idx_points_log_order (order_id),
    CONSTRAINT fk_points_log_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_points_log_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_points_log_type CHECK (type IN (
        'ORDER_REWARD', 'HISTORY_REWARD', 'GIFT', 'REDEEM', 'RETURN', 'ADJUST'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员积分流水';
