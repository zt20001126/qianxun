CREATE TABLE room_block (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    room_id BIGINT UNSIGNED NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(200) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_room_block_room_dates (room_id, start_date, end_date),
    CONSTRAINT fk_room_block_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT chk_room_block_dates CHECK (end_date > start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员主动封房';

CREATE TABLE orders (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(20) NOT NULL,
    platform_order_no VARCHAR(100) NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    channel_id BIGINT UNSIGNED NOT NULL,
    creator_id BIGINT UNSIGNED NULL,
    member_id BIGINT UNSIGNED NULL,
    guest_name VARCHAR(80) NULL,
    phone VARCHAR(20) NULL,
    guest_count SMALLINT UNSIGNED NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no),
    UNIQUE KEY uk_orders_channel_platform_no (channel_id, platform_order_no),
    KEY idx_orders_room_dates (room_id, check_in, check_out),
    KEY idx_orders_member_status (member_id, status),
    KEY idx_orders_channel_status (channel_id, status),
    CONSTRAINT fk_orders_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_orders_channel FOREIGN KEY (channel_id) REFERENCES channels (id),
    CONSTRAINT fk_orders_creator FOREIGN KEY (creator_id) REFERENCES creators (id),
    CONSTRAINT fk_orders_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT chk_orders_dates CHECK (check_out > check_in),
    CONSTRAINT chk_orders_status CHECK (status IN (
        'PENDING_CONFIRM', 'PENDING_CHECKIN', 'CHECKED_IN', 'COMPLETED', 'CANCELLED'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='七渠道正式订单';

CREATE TABLE booking_lock (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guest_name VARCHAR(80) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    guest_count SMALLINT UNSIGNED NOT NULL,
    remark VARCHAR(500) NULL,
    room_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    points_used INT UNSIGNED NOT NULL DEFAULT 0,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_booking_lock_room_dates_status_expiry (room_id, check_in, check_out, status, expires_at),
    KEY idx_booking_lock_member_status_expiry (member_id, status, expires_at),
    CONSTRAINT fk_booking_lock_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_booking_lock_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT chk_booking_lock_dates CHECK (check_out > check_in),
    CONSTRAINT chk_booking_lock_amounts CHECK (
        room_amount >= 0 AND discount_amount >= 0 AND amount >= 0
    ),
    CONSTRAINT chk_booking_lock_status CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序支付前临时锁房';
