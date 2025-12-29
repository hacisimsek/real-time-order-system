INSERT INTO inventory (sku, available_qty, reserved_qty, updated_at)
VALUES
  ('ABC-002', 40, 0, now()),
  ('ABC-003', 55, 0, now())
ON CONFLICT (sku) DO UPDATE
SET available_qty = EXCLUDED.available_qty,
    reserved_qty = EXCLUDED.reserved_qty,
    updated_at = EXCLUDED.updated_at;
