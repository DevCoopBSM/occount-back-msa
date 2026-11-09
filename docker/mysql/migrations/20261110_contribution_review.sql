-- Run once on the member database before deploying the contribution review fix.
-- Stop contribution writes and take a backup first: MySQL DDL auto-commits.
-- Existing contribution tables must already exist. Do not run after Hibernate
-- has added these columns/indexes; inspect SHOW CREATE TABLE first.
ALTER TABLE contribution_account
    MODIFY COLUMN balance BIGINT NOT NULL;

ALTER TABLE contribution_deposit_request
    MODIFY COLUMN amount BIGINT NOT NULL,
    ADD COLUMN processed_by BIGINT NULL,
    ADD COLUMN processed_at DATETIME(6) NULL,
    ADD INDEX idx_contribution_deposit_request_status_requested (status, requested_at),
    ADD INDEX idx_contribution_deposit_request_user_requested (user_id, requested_at),
    ADD INDEX idx_contribution_deposit_request_requested (requested_at);

ALTER TABLE contribution_withdrawal_request
    MODIFY COLUMN amount BIGINT NOT NULL,
    ADD COLUMN processed_by BIGINT NULL,
    ADD COLUMN processed_at DATETIME(6) NULL,
    ADD INDEX idx_contribution_withdrawal_request_status_requested (status, requested_at),
    ADD INDEX idx_contribution_withdrawal_request_user_requested (user_id, requested_at),
    ADD INDEX idx_contribution_withdrawal_request_requested (requested_at);

ALTER TABLE contribution_transaction
    MODIFY COLUMN amount BIGINT NOT NULL,
    MODIFY COLUMN before_balance BIGINT NOT NULL,
    MODIFY COLUMN after_balance BIGINT NOT NULL;
