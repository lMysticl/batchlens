CREATE TABLE IF NOT EXISTS demo_invoice (
  run_key VARCHAR(64) NOT NULL,
  invoice_id BIGINT NOT NULL,
  customer VARCHAR(100) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (run_key, invoice_id)
);
