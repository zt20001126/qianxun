-- 节假日价为空的存量客房继续沿用平时价。
ALTER TABLE room
    ADD COLUMN holiday_price DECIMAL(12,2) NULL COMMENT '节假日每晚价格，空值表示沿用平时价' AFTER price,
    ADD CONSTRAINT chk_room_holiday_price_nonnegative CHECK (holiday_price >= 0);

CREATE TABLE holiday_price_date (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录主键',
    holiday_date DATE NOT NULL COMMENT '全店使用节假日价的住宿日期',
    PRIMARY KEY (id),
    UNIQUE KEY uk_holiday_price_date (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员勾选的全店节假日价日期';
