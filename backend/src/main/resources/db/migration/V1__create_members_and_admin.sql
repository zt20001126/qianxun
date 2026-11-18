CREATE TABLE members (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内部会员 ID，同时作为会员 JWT 身份标识',
    phone VARCHAR(20) NOT NULL COMMENT '会员手机号，第一阶段不验证手机号归属',
    openid VARCHAR(100) NULL COMMENT '微信 OpenID 预留字段，第一阶段始终为空',
    nickname VARCHAR(80) NULL COMMENT '固定为手机号后四位，第一阶段不可编辑',
    avatar VARCHAR(500) NULL COMMENT '系统统一默认头像地址，第一阶段不可编辑',
    growth_points INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计成长积分',
    points INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_phone (phone),
    UNIQUE KEY uk_members_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序用户与会员合并表';

CREATE TABLE admin (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '管理员 ID，同时作为管理员 JWT 身份标识',
    username VARCHAR(60) NOT NULL COMMENT '后台管理员登录用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希，不存储明文密码',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用管理员账号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='单管理员后台认证表';
