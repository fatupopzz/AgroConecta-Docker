const { pool } = require("../config/db");

class UserRepository {
  constructor(db = pool) { this.db = db; }

  async findAll() {
    const result = await this.db.query(
      `SELECT id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro
       FROM usuario`
    );
    return result.rows;
  }

  async findById(id) {
    const result = await this.db.query(
      `SELECT id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro
       FROM usuario
       WHERE id_usuario = $1`,
      [id]
    );
    return result.rows;
  }

  async create({ nombre, telefono, email, hash, tipo_usuario }) {
    const result = await this.db.query(
      `INSERT INTO usuario
       (nombre, telefono, email, contrasena_hash, tipo_usuario)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro`,
      [
        nombre,
        telefono,
        email,
        hash,
        tipo_usuario,
      ]
    );
    return result.rows;
  }

  async update(id, { nombre, telefono, email, activo }) {
    const result = await this.db.query(
      `UPDATE usuario SET
        nombre = COALESCE($2, nombre),
        telefono = COALESCE($3, telefono),
        email = COALESCE($4, email),
        activo = COALESCE($5, activo)
       WHERE id_usuario = $1
       RETURNING id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro`,
      [id, nombre, telefono, email, activo]
    );
    return result.rows;
  }

  async delete(id) {
    const result = await this.db.query(
      "DELETE FROM usuario WHERE id_usuario = $1 RETURNING *",
      [id]
    );
    return result.rows;
  }

  async findFarmerByUserId(userId) {
    const farmerResult = await this.db.query(
      `SELECT id_agricultor
       FROM agricultor
       WHERE id_usuario = $1`,
      [userId]
    );
    return farmerResult.rows;
  }

  async getFarmerDashboard(farmerId) {
    const canceledState = "cancelado";

    const [summaryResult, monthlyResult, topProductsResult, lastOrderResult] =
      await Promise.all([
        this.db.query(
          `SELECT
             COUNT(*)::int AS total_pedidos,
             COALESCE(
               SUM(total_pedido) FILTER (WHERE estado <> $2),
               0
             ) AS total_gastado_historico,
             COALESCE(
               SUM(total_pedido) FILTER (
                 WHERE estado <> $2
                   AND fecha_pedido >= DATE_TRUNC('month', NOW())
                   AND fecha_pedido < DATE_TRUNC('month', NOW()) + INTERVAL '1 month'
               ),
               0
             ) AS total_gastado_mes_actual
           FROM pedido
           WHERE id_agricultor = $1`,
          [farmerId, canceledState]
        ),
        this.db.query(
          `WITH meses AS (
             SELECT GENERATE_SERIES(
               DATE_TRUNC('month', NOW()) - INTERVAL '5 months',
               DATE_TRUNC('month', NOW()),
               INTERVAL '1 month'
             ) AS mes
           )
           SELECT
             TO_CHAR(m.mes, 'YYYY-MM') AS mes,
             COALESCE(SUM(p.total_pedido), 0) AS total
           FROM meses m
           LEFT JOIN pedido p
             ON p.id_agricultor = $1
            AND p.estado <> $2
            AND p.fecha_pedido >= m.mes
            AND p.fecha_pedido < m.mes + INTERVAL '1 month'
           GROUP BY m.mes
           ORDER BY m.mes ASC`,
          [farmerId, canceledState]
        ),
        this.db.query(
          `SELECT
             pr.id_producto,
             pr.nombre,
             pr.imagen_url,
             SUM(dp.cantidad)::int AS cantidad,
             COALESCE(SUM(dp.subtotal), 0) AS total_gastado
           FROM pedido pe
           JOIN detalle_pedido dp ON dp.id_pedido = pe.id_pedido
           JOIN inventario_distribuidor i ON i.id_inventario = dp.id_inventario
           JOIN producto pr ON pr.id_producto = i.id_producto
           WHERE pe.id_agricultor = $1
             AND pe.estado <> $2
           GROUP BY pr.id_producto, pr.nombre, pr.imagen_url
           ORDER BY cantidad DESC, total_gastado DESC, pr.nombre ASC
           LIMIT 5`,
          [farmerId, canceledState]
        ),
        this.db.query(
          `SELECT
             p.id_pedido,
             p.fecha_pedido,
             p.estado,
             p.total_pedido,
             d.nombre_negocio AS distribuidor_nombre
           FROM pedido p
           JOIN distribuidor d ON d.id_distribuidor = p.id_distribuidor
           WHERE p.id_agricultor = $1
           ORDER BY p.fecha_pedido DESC, p.id_pedido DESC
           LIMIT 1`,
          [farmerId]
        ),
      ]);

    return {
      summary: summaryResult.rows[0] || {},
      monthly: monthlyResult.rows,
      topProducts: topProductsResult.rows,
      lastOrder: lastOrderResult.rows[0] || null,
    };
  }
}

module.exports = UserRepository;
