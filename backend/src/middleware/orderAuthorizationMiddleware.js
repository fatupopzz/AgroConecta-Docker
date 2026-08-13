const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const canCreateOrder = async (req, res, next) => {
  const usuarioAutenticado = req.user;
  const idAgricultorBody = req.body.id_agricultor;

  if (!usuarioAutenticado || !usuarioAutenticado.id || !usuarioAutenticado.tipo) {
    return res.status(401).json({ error: "Usuario no autenticado" });
  }

  // Administrador puede crear pedidos indicando id_agricultor en el body
  if (usuarioAutenticado.tipo === "administrador") {
    if (!isPositiveInteger(idAgricultorBody)) {
      return res.status(400).json({ error: "id_agricultor inválido" });
    }

    try {
      const result = await pool.query(
        "SELECT id_agricultor FROM agricultor WHERE id_agricultor = $1",
        [Number(idAgricultorBody)]
      );

      if (result.rows.length === 0) {
        return res.status(404).json({ error: "Agricultor no encontrado" });
      }

      req.agricultorId = Number(idAgricultorBody);
      return next();
    } catch (error) {
      console.error("Error en canCreateOrder (admin):", error);
      return res.status(500).json({ error: "Error en servidor" });
    }
  }

  // Solo agricultores pueden crear pedidos para sí mismos
  if (usuarioAutenticado.tipo !== "agricultor") {
    return res.status(403).json({ error: "Solo un agricultor puede crear pedidos" });
  }

  try {
    const result = await pool.query(
      "SELECT id_agricultor FROM agricultor WHERE id_usuario = $1",
      [Number(usuarioAutenticado.id)]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Perfil de agricultor no encontrado" });
    }
    req.agricultorId = Number(result.rows[0].id_agricultor);
    return next();
  } catch (error) {
    console.error("Error en canCreateOrder (agricultor):", error);
    return res.status(500).json({ error: "Error en servidor" });
  }
};

const canManageOrderStatus = async (req, res, next) => {
  const usuarioAutenticado = req.user;
  const { id_distribuidor } = req.body;

  if (!usuarioAutenticado || !usuarioAutenticado.id || !usuarioAutenticado.tipo) {
    return res.status(401).json({ error: "Usuario no autenticado" });
  }

  if (!isPositiveInteger(id_distribuidor)) {
    return res.status(400).json({ error: "id_distribuidor inválido" });
  }

  if (usuarioAutenticado.tipo === "administrador") {
    return next();
  }

  if (usuarioAutenticado.tipo !== "distribuidor") {
    return res.status(403).json({ error: "Solo un distribuidor puede actualizar el estado del pedido" });
  }

  const distributorResult = await pool.query(
    "SELECT id_distribuidor FROM distribuidor WHERE id_usuario = $1",
    [Number(usuarioAutenticado.id)]
  );

  if (distributorResult.rows.length === 0) {
    return res.status(403).json({ error: "No tienes perfil de distribuidor" });
  }

  const distributorId = Number(distributorResult.rows[0].id_distribuidor);

  if (distributorId !== Number(id_distribuidor)) {
    return res.status(403).json({
      error: "Solo puedes actualizar pedidos de tu propio distribuidor",
    });
  }

  req.distributorId = distributorId;
  return next();
};

const authorizeDistributorOwner = ({ allowAdmin, forbiddenMessage }) => async (req, res, next) => {
  const usuarioAutenticado = req.user;
  const { id } = req.params;

  if (!usuarioAutenticado || !usuarioAutenticado.id || !usuarioAutenticado.tipo) {
    return res.status(401).json({ error: "Usuario no autenticado" });
  }

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de distribuidor inválido" });
  }

  if (allowAdmin && usuarioAutenticado.tipo === "administrador") {
    return next();
  }

  if (usuarioAutenticado.tipo !== "distribuidor") {
    return res.status(403).json({ error: forbiddenMessage });
  }

  const distributorResult = await pool.query(
    "SELECT id_distribuidor FROM distribuidor WHERE id_usuario = $1",
    [Number(usuarioAutenticado.id)]
  );

  if (distributorResult.rows.length === 0) {
    return res.status(403).json({ error: "No tienes perfil de distribuidor" });
  }

  const distributorId = Number(distributorResult.rows[0].id_distribuidor);

  if (distributorId !== Number(id)) {
    return res.status(403).json({
      error: forbiddenMessage,
    });
  }

  req.distributorId = distributorId;
  return next();
};

const canViewDistributorOrders = authorizeDistributorOwner({
  allowAdmin: true,
  forbiddenMessage: "Solo puedes consultar los pedidos de tu propio distribuidor",
});

const canViewDistributorStats = authorizeDistributorOwner({
  allowAdmin: false,
  forbiddenMessage: "Solo puedes consultar las estadísticas de tu propio distribuidor",
});

module.exports = {
  canCreateOrder,
  canManageOrderStatus,
  canViewDistributorOrders,
  canViewDistributorStats,
};
