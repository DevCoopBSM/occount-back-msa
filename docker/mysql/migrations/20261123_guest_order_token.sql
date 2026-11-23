-- Apply to the existing order database before deploying the new order-api.
ALTER TABLE orders ADD COLUMN guest_order_token_hash VARCHAR(43) NULL;
