SET SESSION group_concat_max_len = 65535;

ALTER TABLE booker_profile
    ADD COLUMN order_id BIGINT UNSIGNED NULL COMMENT '手机号为空时关联的唯一订单',
    ADD COLUMN tags JSON NULL COMMENT '画像标签名称 JSON 数组',
    ADD COLUMN admin_remark VARCHAR(2000) NULL COMMENT '手机号画像级管理员备注';

CREATE TEMPORARY TABLE qx_booker_phone_canonical (
    phone VARCHAR(20) NOT NULL,
    canonical_profile_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (phone)
) ENGINE=InnoDB;

INSERT INTO qx_booker_phone_canonical (phone, canonical_profile_id)
SELECT TRIM(phone), MIN(id)
FROM booker_profile
WHERE NULLIF(TRIM(phone), '') IS NOT NULL
GROUP BY TRIM(phone);

CREATE TEMPORARY TABLE qx_booker_profile_tags (
    profile_id BIGINT UNSIGNED NOT NULL,
    tags JSON NOT NULL,
    PRIMARY KEY (profile_id)
) ENGINE=InnoDB;

INSERT INTO qx_booker_profile_tags (profile_id, tags)
SELECT tag_names.profile_id,
       CAST(CONCAT('[', GROUP_CONCAT(JSON_QUOTE(tag_names.name) ORDER BY tag_names.name SEPARATOR ','), ']') AS JSON)
FROM (
    SELECT DISTINCT
           COALESCE(canonical.canonical_profile_id, profile.id) AS profile_id,
           tag.name
    FROM booker_profile_tag AS profile_tag
    INNER JOIN portrait_tag AS tag ON tag.id = profile_tag.tag_id
    INNER JOIN booker_profile AS profile ON profile.id = profile_tag.profile_id
    LEFT JOIN qx_booker_phone_canonical AS canonical ON canonical.phone = TRIM(profile.phone)
) AS tag_names
GROUP BY tag_names.profile_id;

UPDATE booker_profile AS profile
INNER JOIN qx_booker_profile_tags AS saved_tags ON saved_tags.profile_id = profile.id
SET profile.tags = saved_tags.tags;

INSERT INTO portrait_tag (name)
SELECT DISTINCT saved_tag.tag_name
FROM booker_profile AS profile
CROSS JOIN JSON_TABLE(profile.tags, '$[*]' COLUMNS (tag_name VARCHAR(40) PATH '$')) AS saved_tag
WHERE TRUE
ON DUPLICATE KEY UPDATE enabled = 1;

INSERT INTO portrait_tag (name) VALUES
    ('古镇游览'),
    ('自驾'),
    ('晚到'),
    ('亲子设施'),
    ('长住'),
    ('复住')
ON DUPLICATE KEY UPDATE enabled = 1;

UPDATE booker_profile AS profile
INNER JOIN (
    SELECT profile_id, MIN(order_id) AS order_id
    FROM booker_profile_order
    GROUP BY profile_id
) AS legacy_order ON legacy_order.profile_id = profile.id
SET profile.order_id = legacy_order.order_id
WHERE NULLIF(TRIM(profile.phone), '') IS NULL;

-- V12 已将同手机号订单关联到最早画像；此处删除失去关联的旧重复画像。
DELETE duplicate_profile
FROM booker_profile AS duplicate_profile
INNER JOIN qx_booker_phone_canonical AS canonical ON canonical.phone = TRIM(duplicate_profile.phone)
WHERE duplicate_profile.id <> canonical.canonical_profile_id;

UPDATE booker_profile
SET phone = NULLIF(TRIM(phone), ''),
    tags = COALESCE(tags, JSON_ARRAY());

DROP TABLE booker_profile_order;
DROP TABLE booker_profile_tag;

ALTER TABLE booker_profile
    MODIFY COLUMN tags JSON NOT NULL COMMENT '画像标签名称 JSON 数组',
    ADD UNIQUE KEY uk_booker_profile_phone (phone),
    ADD UNIQUE KEY uk_booker_profile_order (order_id),
    ADD CONSTRAINT fk_booker_profile_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    COMMENT = '按手机号归并的顾客画像；无手机号时关联单笔订单';

DROP TEMPORARY TABLE qx_booker_profile_tags;
DROP TEMPORARY TABLE qx_booker_phone_canonical;
