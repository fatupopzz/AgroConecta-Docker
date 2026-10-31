-- KAN-107: índices respaldados por EXPLAIN (ANALYZE, BUFFERS).
-- Evidencia y reproducción: backend/KAN-107.md y benchmarks/kan107/.
-- Aplicación local idempotente; para producción consultar la estrategia CONCURRENTLY del informe.

-- GET /api/products?id_categoria=:id: conteo de productos activos. No optimiza ORDER BY RANDOM().
CREATE INDEX IF NOT EXISTS idx_kan107_producto_categoria_activo ON producto(id_categoria) WHERE activo = true;

-- GET /api/products/:id y comparación por precio; JOIN por producto en KAN-102. El UNIQUE existente empieza por distribuidor.
CREATE INDEX IF NOT EXISTS idx_kan107_inventario_producto_precio ON inventario_distribuidor(id_producto, precio);

-- GET /api/orders/farmer/:id: filtra propietario; fecha e ID coinciden con el orden del historial. El GROUP BY aún puede ordenar.
CREATE INDEX IF NOT EXISTS idx_kan107_pedido_agricultor_fecha ON pedido(id_agricultor, fecha_pedido DESC, id_pedido DESC);

-- Pedidos y estadísticas del distribuidor: igualdad por propietario y estado. No pretende resolver el orden posterior al GROUP BY.
CREATE INDEX IF NOT EXISTS idx_kan107_pedido_distribuidor_estado ON pedido(id_distribuidor, estado);

-- Detalles de pedido en orden de id_detalle; JOIN y conteo de detalles en historiales y estadísticas.
CREATE INDEX IF NOT EXISTS idx_kan107_detalle_pedido_orden ON detalle_pedido(id_pedido, id_detalle);

-- KAN-102: desde los inventarios de un producto obtiene los IDs de sus pedidos sin recorrer todos los detalles.
CREATE INDEX IF NOT EXISTS idx_kan107_detalle_inventario_pedido ON detalle_pedido(id_inventario, id_pedido);

-- Reseñas de producto por fecha y JOIN de reseñas de productos vendidos por un distribuidor.
CREATE INDEX IF NOT EXISTS idx_kan107_resena_producto_fecha ON resena(id_producto, fecha_resena DESC);

-- GET /api/notifications de distribuidores por fecha. El filtro por propietario implica el predicado IS NOT NULL.
CREATE INDEX IF NOT EXISTS idx_kan107_notificacion_distribuidor_fecha ON notificacion(id_distribuidor, fecha DESC) WHERE id_distribuidor IS NOT NULL;

-- KAN-102, producto popular: permite unir por hash los pedidos entregados de la ventana temporal sin miles de accesos al heap.
CREATE INDEX IF NOT EXISTS idx_kan107_pedido_entregado_fecha ON pedido(fecha_pedido, id_pedido) INCLUDE(id_agricultor) WHERE estado = 'entregado';
