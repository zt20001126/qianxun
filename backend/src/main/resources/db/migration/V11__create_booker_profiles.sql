ALTER TABLE orders
    ADD COLUMN admin_remark VARCHAR(500) NULL COMMENT '管理员在录入订单时填写的内部备注';

CREATE TABLE booker_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预订人画像主键',
    identity_key CHAR(64) NOT NULL COMMENT '预订人身份组合的 SHA-256 键；资料不全时按订单生成',
    guest_name VARCHAR(80) NULL COMMENT '预订人姓名快照；来自关联订单',
    phone VARCHAR(20) NULL COMMENT '预订人手机号快照；管理端完整展示',
    age SMALLINT UNSIGNED NULL COMMENT '管理员录入或由身份证号本地推算的年龄；不保存身份证号',
    party_type VARCHAR(32) NULL COMMENT '主要出行群体分类编码',
    inferred_region VARCHAR(120) NULL COMMENT '身份证号本地推算后仅保存的省级地区',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '画像创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '画像更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_booker_profile_identity (identity_key),
    KEY idx_booker_profile_phone_name (phone, guest_name),
    CONSTRAINT chk_booker_profile_age CHECK (age IS NULL OR age <= 120),
    CONSTRAINT chk_booker_profile_party_type CHECK (
        party_type IS NULL OR party_type IN (
            'SOLO', 'COUPLE', 'PARENT_CHILD', 'FAMILY_FRIENDS',
            'FRIENDS', 'BUSINESS', 'GROUP', 'OTHER'
        )
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='按预订人姓名与手机号归并的顾客画像';

CREATE TABLE booker_profile_order (
    order_id BIGINT UNSIGNED NOT NULL COMMENT '关联订单主键',
    profile_id BIGINT UNSIGNED NOT NULL COMMENT '预订人画像主键',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '订单关联时间',
    PRIMARY KEY (order_id),
    KEY idx_booker_profile_order_profile (profile_id, order_id),
    CONSTRAINT fk_booker_profile_order_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_booker_profile_order_profile FOREIGN KEY (profile_id) REFERENCES booker_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预订人画像与订单关联';

CREATE TABLE portrait_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '画像标签主键',
    name VARCHAR(40) NOT NULL COMMENT '管理员自定义标签名称',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许新画像选择',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '标签创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '标签更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_portrait_tag_name (name),
    CONSTRAINT chk_portrait_tag_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='顾客画像自定义标签字典';

CREATE TABLE booker_profile_tag (
    profile_id BIGINT UNSIGNED NOT NULL COMMENT '预订人画像主键',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '画像标签主键',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '标签关联时间',
    PRIMARY KEY (profile_id, tag_id),
    KEY idx_booker_profile_tag_tag (tag_id, profile_id),
    CONSTRAINT fk_booker_profile_tag_profile FOREIGN KEY (profile_id) REFERENCES booker_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_booker_profile_tag_tag FOREIGN KEY (tag_id) REFERENCES portrait_tag (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预订人画像与自定义标签关联';

INSERT INTO booker_profile (identity_key, guest_name, phone)
SELECT legacy.identity_key,
       MAX(NULLIF(TRIM(legacy.guest_name), '')),
       MAX(NULLIF(TRIM(legacy.phone), ''))
FROM (
    SELECT id, guest_name, phone,
           CASE
               WHEN NULLIF(TRIM(guest_name), '') IS NOT NULL
                AND NULLIF(TRIM(phone), '') IS NOT NULL
                   THEN SHA2(CONCAT(LOWER(TRIM(guest_name)), CHAR(31), TRIM(phone)), 256)
               ELSE SHA2(CONCAT('ORDER:', id), 256)
           END AS identity_key
    FROM orders
) AS legacy
GROUP BY legacy.identity_key;

INSERT INTO booker_profile_order (order_id, profile_id)
SELECT legacy.id, profile.id
FROM (
    SELECT id,
           CASE
               WHEN NULLIF(TRIM(guest_name), '') IS NOT NULL
                AND NULLIF(TRIM(phone), '') IS NOT NULL
                   THEN SHA2(CONCAT(LOWER(TRIM(guest_name)), CHAR(31), TRIM(phone)), 256)
               ELSE SHA2(CONCAT('ORDER:', id), 256)
           END AS identity_key
    FROM orders
) AS legacy
INNER JOIN booker_profile AS profile ON profile.identity_key = legacy.identity_key;
