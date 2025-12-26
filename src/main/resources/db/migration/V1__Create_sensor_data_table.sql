CREATE TABLE IF NOT EXISTS sensor_data (
    id BIGSERIAL PRIMARY KEY,
    sensor_id BIGINT NOT NULL,
    timestamp BIGINT NOT NULL,
    temperature DOUBLE PRECISION NOT NULL,
    humidity DOUBLE PRECISION NOT NULL,
    pressure DOUBLE PRECISION NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    anomaly BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sensor_id ON sensor_data(sensor_id);
CREATE INDEX IF NOT EXISTS idx_timestamp ON sensor_data(timestamp);
CREATE INDEX IF NOT EXISTS idx_sensor_timestamp ON sensor_data(sensor_id, timestamp);
