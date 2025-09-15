CREATE TABLE IF NOT EXISTS inventory_message_log (
                                                     id           BIGSERIAL PRIMARY KEY,
                                                     message_id   VARCHAR(100) NOT NULL UNIQUE,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now()
    );
CREATE UNIQUE INDEX IF NOT EXISTS ux_inventory_message_log_mid ON inventory_message_log(message_id);
