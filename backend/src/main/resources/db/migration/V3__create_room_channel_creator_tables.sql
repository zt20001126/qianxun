CREATE TABLE room (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    number VARCHAR(10) NOT NULL,
    name VARCHAR(80) NOT NULL,
    floor SMALLINT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    capacity SMALLINT UNSIGNED NOT NULL,
    bed_info VARCHAR(120) NULL,
    area DECIMAL(6,2) NULL,
    facilities JSON NULL,
    images JSON NULL,
    description TEXT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_number (number),
    CONSTRAINT chk_room_price_nonnegative CHECK (price >= 0),
    CONSTRAINT chk_room_capacity_positive CHECK (capacity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客房基础资料';

CREATE TABLE channels (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_channels_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='固定订单来源渠道';

CREATE TABLE creators (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    channel_id BIGINT UNSIGNED NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_creators_channel_name (channel_id, name),
    CONSTRAINT fk_creators_channel FOREIGN KEY (channel_id) REFERENCES channels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='渠道达人资料';

INSERT INTO channels (id, name, enabled) VALUES
    (1, '小程序', 1),
    (2, '抖音', 1),
    (3, '美团', 1),
    (4, '携程', 1),
    (5, '同程', 1),
    (6, '飞猪', 1),
    (7, '途家', 1);
