const { pool } = require("../config/db");

const getAdminMetrics = async (req, res) => {
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

  return res.json({
    usersByType: usersByType.rows,
    ordersByStatus: ordersByStatus.rows,
    productsTotal: productsTotal.rows[0],
    distributorsByState: distributorsByState.rows,
  });
};

module.exports = {
  getAdminMetrics,
};
