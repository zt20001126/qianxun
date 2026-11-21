ALTER TABLE booker_profile
    MODIFY COLUMN identity_key CHAR(64) NOT NULL
        COMMENT '手机号 SHA-256 键；手机号缺失时按订单生成',
    COMMENT = '按手机号归并的预订人画像';

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

CREATE TEMPORARY TABLE qx_booker_phone_fields (
    phone VARCHAR(20) NOT NULL,
    guest_name VARCHAR(80) NULL,
    age SMALLINT UNSIGNED NULL,
    party_type VARCHAR(32) NULL,
    inferred_region VARCHAR(120) NULL,
    PRIMARY KEY (phone)
) ENGINE=InnoDB;

INSERT INTO qx_booker_phone_fields (phone, guest_name, age, party_type, inferred_region)
SELECT canonical.phone,
       (SELECT source.guest_name
        FROM booker_profile AS source
        WHERE TRIM(source.phone) = canonical.phone AND source.guest_name IS NOT NULL
        ORDER BY source.id LIMIT 1),
       (SELECT source.age
        FROM booker_profile AS source
        WHERE TRIM(source.phone) = canonical.phone AND source.age IS NOT NULL
        ORDER BY source.id LIMIT 1),
       (SELECT source.party_type
        FROM booker_profile AS source
        WHERE TRIM(source.phone) = canonical.phone AND source.party_type IS NOT NULL
        ORDER BY source.id LIMIT 1),
       (SELECT source.inferred_region
        FROM booker_profile AS source
        WHERE TRIM(source.phone) = canonical.phone AND source.inferred_region IS NOT NULL
        ORDER BY source.id LIMIT 1)
FROM qx_booker_phone_canonical AS canonical;

-- 合并同手机号画像的标签，并把订单关联统一到最早创建的画像；旧画像行保留以避免删除历史资料。
INSERT IGNORE INTO booker_profile_tag (profile_id, tag_id)
SELECT canonical.canonical_profile_id, profile_tag.tag_id
FROM booker_profile_tag AS profile_tag
INNER JOIN booker_profile AS source ON source.id = profile_tag.profile_id
INNER JOIN qx_booker_phone_canonical AS canonical ON canonical.phone = TRIM(source.phone)
WHERE source.id <> canonical.canonical_profile_id;

UPDATE booker_profile_order AS profile_order
INNER JOIN booker_profile AS source ON source.id = profile_order.profile_id
INNER JOIN qx_booker_phone_canonical AS canonical ON canonical.phone = TRIM(source.phone)
SET profile_order.profile_id = canonical.canonical_profile_id
WHERE profile_order.profile_id <> canonical.canonical_profile_id;

UPDATE booker_profile AS profile
INNER JOIN qx_booker_phone_canonical AS canonical ON canonical.canonical_profile_id = profile.id
INNER JOIN qx_booker_phone_fields AS profile_fields ON profile_fields.phone = canonical.phone
SET profile.identity_key = SHA2(CONCAT('PHONE:', canonical.phone), 256),
    profile.phone = canonical.phone,
    profile.guest_name = COALESCE(profile.guest_name, profile_fields.guest_name),
    profile.age = COALESCE(profile.age, profile_fields.age),
    profile.party_type = COALESCE(profile.party_type, profile_fields.party_type),
    profile.inferred_region = COALESCE(profile.inferred_region, profile_fields.inferred_region);

DROP TEMPORARY TABLE qx_booker_phone_fields;
DROP TEMPORARY TABLE qx_booker_phone_canonical;
