const { pool } = require("../config/db");
const bcrypt = require("bcrypt");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));


const getUsers = async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro
       FROM usuario`
    );

    res.json(result.rows);
  } catch (error) {
    console.error("Error en getUsers:", error);
    res.status(500).json({ error: "Error al obtener usuarios" });
  }
};


const getUserById = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      `SELECT id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro
       FROM usuario
       WHERE id_usuario = $1`,
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }

    res.json(result.rows[0]);
  } catch (error) {
    console.error("Error en getUserById:", error);
    res.status(500).json({ error: "Error al obtener usuario" });
  }
};


const createUser = async (req, res) => {
  const { nombre, telefono, email, password, tipo_usuario } = req.body;

  if (!nombre || !telefono || !password || !tipo_usuario) {
    return res.status(400).json({
      error: "Campos obligatorios: nombre, telefono, password, tipo_usuario",
    });
  }

  const tiposValidos = ["agricultor", "distribuidor", "administrador"];

  if (!tiposValidos.includes(tipo_usuario)) {
    return res.status(400).json({ error: "tipo_usuario inválido" });
  }

  try {
    const hash = await bcrypt.hash(password, 10);

    const result = await pool.query(
      `INSERT INTO usuario
       (nombre, telefono, email, contrasena_hash, tipo_usuario)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro`,
      [
        nombre.trim(),
        telefono,
        email || null,
        hash,
        tipo_usuario,
      ]
    );

    res.status(201).json({
      message: "Usuario creado correctamente",
      usuario: result.rows[0],
    });

  } catch (error) {
    console.error("Error en createUser:", error);
    res.status(500).json({ error: "Error al crear usuario" });
  }
};


const updateUser = async (req, res) => {
  const { id } = req.params;
  const { nombre, telefono, email, activo } = req.body;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  if (req.user.tipo !== "administrador" && Object.hasOwn(req.body, "activo")) {
    return res.status(403).json({ error: "Solo un administrador puede cambiar el estado de una cuenta" });
  }

  try {
    const result = await pool.query(
      `UPDATE usuario SET
        nombre = COALESCE($2, nombre),
        telefono = COALESCE($3, telefono),
        email = COALESCE($4, email),
        activo = COALESCE($5, activo)
       WHERE id_usuario = $1
       RETURNING id_usuario, nombre, telefono, email, tipo_usuario, activo, fecha_registro`,
      [id, nombre, telefono, email, activo]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }

    res.json({
      message: "Usuario actualizado",
      usuario: result.rows[0],
    });

  } catch (error) {
    console.error("Error en updateUser:", error);
    res.status(500).json({ error: "Error al actualizar usuario" });
  }
};


const deleteUser = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  try {
    const result = await pool.query(
      "DELETE FROM usuario WHERE id_usuario = $1 RETURNING *",
      [id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }

    res.json({ message: "Usuario eliminado" });

  } catch (error) {
    console.error("Error en deleteUser:", error);
    res.status(500).json({ error: "Error al eliminar usuario" });
  }
};

const getFarmerDashboard = async (req, res) => {
  const authenticatedUser = req.user;

  if (!authenticatedUser || authenticatedUser.tipo !== "agricultor") {
    return res.status(403).json({
      error: "Solo un agricultor puede consultar este dashboard",
    });
  }

  try {
    const farmerResult = await pool.query(
      `SELECT id_agricultor
       FROM agricultor
       WHERE id_usuario = $1`,
      [Number(authenticatedUser.id)]
    );

    if (farmerResult.rows.length === 0) {
      return res.status(404).json({ error: "Perfil de agricultor no encontrado" });
    }

    const farmerId = Number(farmerResult.rows[0].id_agricultor);
    const canceledState = "cancelado";

    const [summaryResult, monthlyResult, topProductsResult, lastOrderResult] =
      await Promise.all([
        pool.query(
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
        pool.query(
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
        pool.query(
          `SELECT
             pr.id_producto,
             pr.nombre,
             SUM(dp.cantidad)::int AS cantidad,
             COALESCE(SUM(dp.subtotal), 0) AS total_gastado
           FROM pedido pe
           JOIN detalle_pedido dp ON dp.id_pedido = pe.id_pedido
           JOIN inventario_distribuidor i ON i.id_inventario = dp.id_inventario
           JOIN producto pr ON pr.id_producto = i.id_producto
           WHERE pe.id_agricultor = $1
             AND pe.estado <> $2
           GROUP BY pr.id_producto, pr.nombre
           ORDER BY cantidad DESC, total_gastado DESC, pr.nombre ASC
           LIMIT 5`,
          [farmerId, canceledState]
        ),
        pool.query(
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

    const summary = summaryResult.rows[0] || {};
    const lastOrder = lastOrderResult.rows[0];

    return res.json({
      totalGastadoHistorico: Number(summary.total_gastado_historico || 0),
      totalGastadoMesActual: Number(summary.total_gastado_mes_actual || 0),
      cantidadPedidos: Number(summary.total_pedidos || 0),
      gastosPorMes: monthlyResult.rows.map((row) => ({
        mes: row.mes,
        total: Number(row.total),
      })),
      productosMasComprados: topProductsResult.rows.map((row) => ({
        idProducto: Number(row.id_producto),
        nombre: row.nombre,
        cantidad: Number(row.cantidad),
        totalGastado: Number(row.total_gastado),
      })),
      ultimoPedido: lastOrder
        ? {
          id: Number(lastOrder.id_pedido),
          fechaPedido: lastOrder.fecha_pedido,
          estado: lastOrder.estado,
          totalPedido: Number(lastOrder.total_pedido),
          distribuidorNombre: lastOrder.distribuidor_nombre,
        }
        : null,
    });
  } catch (error) {
    console.error("Error en getFarmerDashboard:", error);
    return res.status(500).json({
      error: "Error al obtener el dashboard del agricultor",
    });
  }
};

module.exports = {
  getUsers,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
  getFarmerDashboard,
};
