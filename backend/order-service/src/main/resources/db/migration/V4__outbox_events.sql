CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    aggregate_id BIGINT NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    exchange VARCHAR(128) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_status_created_at ON outbox_events(status, created_at);
