const { pool } = require("../config/db");

const toCount = (value) => Number(value || 0);

const sumTotals = (rows) => rows.reduce((total, row) => total + toCount(row.total), 0);

const getAdminMetrics = async (req, res) => {
  try {
    const [usersByType, ordersByStatus, productsTotal, distributorsByState] =
      await Promise.all([
        pool.query(
          `SELECT tipo_usuario AS type, COUNT(*)::int AS total
           FROM usuario
           GROUP BY tipo_usuario
           ORDER BY tipo_usuario ASC`
        ),
        pool.query(
          `SELECT estado AS status, COUNT(*)::int AS total
           FROM pedido
           GROUP BY estado
           ORDER BY estado ASC`
        ),
        pool.query("SELECT COUNT(*)::int AS total FROM producto WHERE activo = true"),
        pool.query(
          `SELECT CASE
                    WHEN estado_verificacion = 'verificado' THEN 'verified'
                    WHEN estado_verificacion = 'pendiente' THEN 'pending'
                    ELSE estado_verificacion
                  END AS state,
                  COUNT(*)::int AS total
           FROM distribuidor
           GROUP BY state
           ORDER BY state ASC`
        ),
      ]);

    const distributors = distributorsByState.rows.reduce(
      (totals, row) => ({
        ...totals,
        [row.state]: toCount(row.total),
      }),
      { verified: 0, pending: 0 }
    );

    return res.json({
      users: {
        total: sumTotals(usersByType.rows),
        byType: usersByType.rows.map((row) => ({
          type: row.type,
          count: toCount(row.total),
        })),
      },
      orders: {
        total: sumTotals(ordersByStatus.rows),
        byStatus: ordersByStatus.rows.map((row) => ({
          status: row.status,
          count: toCount(row.total),
        })),
      },
      products: {
        total: toCount(productsTotal.rows[0]?.total),
      },
      distributors: {
        verified: distributors.verified || 0,
        pending: distributors.pending || 0,
      },
    });
  } catch (error) {
    console.error("Error en getAdminMetrics:", error);
    return res.status(500).json({ error: "Error al obtener metricas administrativas" });
  }
};

module.exports = {
  getAdminMetrics,
};
