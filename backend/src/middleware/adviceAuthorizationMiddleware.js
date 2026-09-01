const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const authorizeOrderAdvice = ({ requireVerifiedDistributor = false } = {}) =>
  async (req, res, next) => {
    const authenticatedUser = req.user;
    const { id } = req.params;

    if (!authenticatedUser?.id || !authenticatedUser?.tipo) {
      return res.status(401).json({ error: "Usuario no autenticado" });
    }

    if (!isPositiveInteger(id)) {
      return res.status(400).json({ error: "ID de pedido inválido" });
    }

    try {
      const result = await pool.query(
        `SELECT
            p.id_pedido,
            a.id_usuario AS agricultor_usuario_id,
            d.id_usuario AS distribuidor_usuario_id,
            d.estado_verificacion
         FROM pedido p
         JOIN agricultor a ON a.id_agricultor = p.id_agricultor
         JOIN distribuidor d ON d.id_distribuidor = p.id_distribuidor
         WHERE p.id_pedido = $1`,
        [Number(id)]
      );

      if (result.rows.length === 0) {
        return res.status(404).json({ error: "Pedido no encontrado" });
      }

      const order = result.rows[0];
      const userId = Number(authenticatedUser.id);
      const isFarmer =
        authenticatedUser.tipo === "agricultor" &&
        userId === Number(order.agricultor_usuario_id);
      const isDistributor =
        authenticatedUser.tipo === "distribuidor" &&
        userId === Number(order.distribuidor_usuario_id);

      if (!isFarmer && !isDistributor) {
        return res.status(403).json({
          error: "No tienes acceso a la asesoría de este pedido",
        });
      }

      if (
        requireVerifiedDistributor &&
        isDistributor &&
        order.estado_verificacion !== "verificado"
      ) {
        return res.status(403).json({
          error: "Solo un distribuidor verificado puede enviar asesoría técnica",
        });
      }

      req.adviceOrder = order;
      return next();
    } catch (error) {
      console.error("Error al autorizar asesoría del pedido:", error);
      return res.status(500).json({ error: "Error en servidor" });
    }
  };

const canAccessOrderAdvice = authorizeOrderAdvice();
const canSendOrderAdvice = authorizeOrderAdvice({
  requireVerifiedDistributor: true,
});

module.exports = {
  canAccessOrderAdvice,
  canSendOrderAdvice,
};
