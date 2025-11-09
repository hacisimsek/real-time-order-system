CREATE TABLE IF NOT EXISTS report_order_rollup_daily (
    bucket_date DATE PRIMARY KEY,
    total_orders BIGINT NOT NULL DEFAULT 0,
    total_revenue_cents BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS report_message_log (
    message_id VARCHAR(128) PRIMARY KEY,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
