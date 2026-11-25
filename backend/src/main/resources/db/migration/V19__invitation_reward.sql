ALTER TABLE member_invitation
    ADD COLUMN reward_order_id BIGINT UNSIGNED NULL COMMENT '首笔符合条件的完成订单',
    ADD COLUMN reward_amount DECIMAL(12,2) NULL COMMENT '按实付金额计算的邀请奖励',
    ADD COLUMN reward_issued BOOLEAN NOT NULL DEFAULT FALSE COMMENT '管理员记录是否已发放',
    ADD UNIQUE KEY uk_invitation_reward_order (reward_order_id),
    ADD CONSTRAINT fk_invitation_reward_order FOREIGN KEY (reward_order_id) REFERENCES orders(id),
    ADD CONSTRAINT chk_invitation_reward_fields CHECK (
        (reward_order_id IS NULL AND reward_amount IS NULL AND reward_issued = FALSE)
        OR (reward_order_id IS NOT NULL AND reward_amount IS NOT NULL AND reward_amount >= 0)
    );
