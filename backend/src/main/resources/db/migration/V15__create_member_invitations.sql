ALTER TABLE members ADD COLUMN invite_code VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD UNIQUE KEY uk_members_invite_code (invite_code);

CREATE TABLE member_invitation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    inviter_member_id BIGINT UNSIGNED NOT NULL,
    invitee_member_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitation_invitee (invitee_member_id),
    KEY idx_invitation_inviter_time (inviter_member_id, created_at, id),
    CONSTRAINT fk_invitation_inviter FOREIGN KEY (inviter_member_id) REFERENCES members(id),
    CONSTRAINT fk_invitation_invitee FOREIGN KEY (invitee_member_id) REFERENCES members(id),
    CONSTRAINT chk_invitation_not_self CHECK (inviter_member_id <> invitee_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员邀请关系';
