CREATE TABLE report_snapshots (
    id BIGSERIAL PRIMARY KEY,
    period_type VARCHAR(16) NOT NULL,
    snapshot_date DATE NOT NULL,
    total_orders BIGINT NOT NULL,
    total_revenue_cents BIGINT NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_report_snapshots_period_date
    ON report_snapshots (period_type, snapshot_date);
