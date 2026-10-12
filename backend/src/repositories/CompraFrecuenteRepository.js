const { pool } = require("../config/db");

// KAN-102: ventana basada en fecha de compra, no en fecha de entrega.
class CompraFrecuenteRepository {
  constructor(db = pool) { this.db = db; }

  async findByIdProducto(idProducto) {
    const result = await this.db.query(
      `SELECT p.id_agricultor
       FROM pedido p
       JOIN detalle_pedido d ON d.id_pedido = p.id_pedido
       JOIN inventario_distribuidor i ON i.id_inventario = d.id_inventario
       WHERE i.id_producto = $1
         AND p.estado = 'entregado'
         AND p.fecha_pedido >= NOW() - INTERVAL '90 days'
         AND p.fecha_pedido <= NOW()
       GROUP BY p.id_agricultor
       HAVING COUNT(DISTINCT p.id_pedido) >= 2`,
      [Number(idProducto)]
    );
    return result.rows.map(row => ({ idAgricultor: Number(row.id_agricultor) }));
  }
}
module.exports = CompraFrecuenteRepository;
