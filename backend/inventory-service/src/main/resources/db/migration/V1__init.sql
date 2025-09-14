CREATE TABLE IF NOT EXISTS inventory (
                                         sku           VARCHAR(64) PRIMARY KEY,
    available_qty INTEGER      NOT NULL DEFAULT 0,
    reserved_qty  INTEGER      NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
    );
CREATE INDEX IF NOT EXISTS idx_inventory_available ON inventory(available_qty);
