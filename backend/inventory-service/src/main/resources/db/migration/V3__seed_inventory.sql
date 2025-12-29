INSERT INTO inventory (sku, available_qty, reserved_qty)
VALUES
  ('ABC-002', 18, 0),
  ('ABC-003', 32, 0)
ON CONFLICT (sku) DO NOTHING;
