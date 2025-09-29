CREATE TABLE IF NOT EXISTS alerts (
  id BIGSERIAL PRIMARY KEY,
  event_id VARCHAR(64) NOT NULL,
  ts TIMESTAMPTZ NOT NULL,
  source VARCHAR(32) NOT NULL,
  level VARCHAR(16) NOT NULL,
  score DOUBLE PRECISION NOT NULL,
  payload JSONB NOT NULL,
  enrichment JSONB,
  classification JSONB,
  anomaly JSONB NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alerts_ts ON alerts(ts DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_level ON alerts(level);
