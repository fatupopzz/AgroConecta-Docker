ALTER TABLE pago
ADD COLUMN IF NOT EXISTS estado_pago VARCHAR(20) DEFAULT 'pendiente'
    CHECK (estado_pago IN ('pendiente', 'processing', 'completado', 'failed'));

ALTER TABLE pago
ADD COLUMN IF NOT EXISTS fecha_pago TIMESTAMP;

ALTER TABLE pago
ADD COLUMN IF NOT EXISTS referencia_transaccion VARCHAR(100);

ALTER TABLE pago
ADD COLUMN IF NOT EXISTS proveedor VARCHAR(30);

ALTER TABLE pago
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

ALTER TABLE pago
ADD COLUMN IF NOT EXISTS fecha_confirmacion TIMESTAMP;
