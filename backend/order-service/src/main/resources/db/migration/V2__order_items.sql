CREATE TABLE IF NOT EXISTS order_items (
                                           id        BIGSERIAL PRIMARY KEY,
                                           order_id  BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku       VARCHAR(64) NOT NULL,
    qty       INTEGER NOT NULL CHECK (qty > 0)
    );

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
