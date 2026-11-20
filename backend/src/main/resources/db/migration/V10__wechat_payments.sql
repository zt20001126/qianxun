ALTER TABLE payments
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'MOCK' AFTER payment_no,
    ADD COLUMN wechat_prepay_id VARCHAR(128) NULL AFTER transaction_id,
    ADD KEY idx_payments_method_status (payment_method, status),
    ADD CONSTRAINT chk_payments_method CHECK (payment_method IN ('MOCK', 'WECHAT'));

ALTER TABLE payments
    COMMENT = '模拟支付及微信支付流水';
