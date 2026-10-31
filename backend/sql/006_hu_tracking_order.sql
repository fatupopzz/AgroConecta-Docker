ALTER TABLE pedido
DROP CONSTRAINT IF EXISTS pedido_estado_check;

ALTER TABLE pedido
ALTER COLUMN estado SET DEFAULT 'confirmado';

UPDATE pedido
SET estado = 'confirmado'
WHERE estado = 'pendiente';

UPDATE pedido
SET estado = 'en_ruta'
WHERE estado = 'en_camino';

ALTER TABLE pedido
ADD CONSTRAINT pedido_estado_check
CHECK (estado IN ('confirmado', 'preparando', 'en_ruta', 'entregado', 'cancelado'));

CREATE TABLE IF NOT EXISTS pedido_tracking (
    id_tracking SERIAL PRIMARY KEY,
    id_pedido   INT NOT NULL REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    estado      VARCHAR(30) NOT NULL,
    "timestamp" TIMESTAMP DEFAULT NOW(),
    notas       TEXT
);

ALTER TABLE pedido_tracking
DROP CONSTRAINT IF EXISTS pedido_tracking_estado_check;

UPDATE pedido_tracking
SET estado = 'confirmado'
WHERE estado = 'pendiente';

UPDATE pedido_tracking
SET estado = 'en_ruta'
WHERE estado = 'en_camino';

ALTER TABLE pedido_tracking
ADD CONSTRAINT pedido_tracking_estado_check
CHECK (estado IN ('confirmado', 'preparando', 'en_ruta', 'entregado', 'cancelado'));

CREATE INDEX IF NOT EXISTS idx_pedido_tracking_id_pedido_timestamp
    ON pedido_tracking (id_pedido, "timestamp");

INSERT INTO pedido_tracking (id_pedido, estado, "timestamp", notas)
SELECT p.id_pedido, p.estado, COALESCE(p.fecha_pedido, NOW()), 'Estado inicial del pedido'
FROM pedido p
WHERE NOT EXISTS (
    SELECT 1
    FROM pedido_tracking pt
    WHERE pt.id_pedido = p.id_pedido
);
