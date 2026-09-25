const { pool } = require("../config/db");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const getFarmerUserId = (req, res) => {
  if (req.user?.tipo !== "agricultor") {
    res.status(403).json({
      error: "Los favoritos solo están disponibles para agricultores",
    });
    return null;
  }

  if (!isPositiveInteger(req.user.id)) {
    res.status(400).json({ error: "Usuario autenticado inválido" });
    return null;
  }

  return Number(req.user.id);
};

const getProductId = (value, res) => {
  if (!isPositiveInteger(value)) {
    res.status(400).json({ error: "productId inválido" });
    return null;
  }

  return Number(value);
};

const createFavoriteController = (database = pool) => {
  const addFavorite = async (req, res) => {
    const userId = getFarmerUserId(req, res);
    if (userId === null) return;

    const productId = getProductId(req.body?.productId, res);
    if (productId === null) return;

    try {
      const result = await database.query(
        `INSERT INTO favorito (id_usuario, id_producto)
         SELECT $1, p.id_producto
         FROM producto p
         WHERE p.id_producto = $2
           AND p.activo = TRUE
         ON CONFLICT (id_usuario, id_producto)
         DO UPDATE SET id_usuario = EXCLUDED.id_usuario
         RETURNING id, id_usuario, id_producto, fecha_agregado`,
        [userId, productId],
      );

      if (result.rowCount === 0) {
        return res.status(404).json({ error: "Producto no encontrado" });
      }

      return res.status(201).json({
        message: "Producto guardado en favoritos",
        favorite: result.rows[0],
      });
    } catch (error) {
      console.error("Error en addFavorite:", error);
      return res.status(500).json({ error: "Error al guardar el favorito" });
    }
  };

  const getFavorites = async (req, res) => {
    const userId = getFarmerUserId(req, res);
    if (userId === null) return;

    try {
      const result = await database.query(
        `SELECT f.id AS id_favorito,
                f.fecha_agregado,
                p.id_producto,
                p.nombre,
                p.marca,
                p.descripcion,
                p.composicion,
                p.dosis_recomendada,
                p.instrucciones_uso,
                p.calificacion_promedio,
                p.activo,
                c.nombre AS categoria,
                MIN(i.precio) FILTER (WHERE i.stock_disponible > 0) AS precio_desde,
                COUNT(DISTINCT i.id_distribuidor)
                  FILTER (WHERE i.stock_disponible > 0)::int AS num_distribuidores
         FROM favorito f
         JOIN producto p ON p.id_producto = f.id_producto
         JOIN categoria c ON c.id_categoria = p.id_categoria
         LEFT JOIN inventario_distribuidor i ON i.id_producto = p.id_producto
         WHERE f.id_usuario = $1
           AND p.activo = TRUE
         GROUP BY f.id,
                  f.fecha_agregado,
                  p.id_producto,
                  p.nombre,
                  p.marca,
                  p.descripcion,
                  p.composicion,
                  p.dosis_recomendada,
                  p.instrucciones_uso,
                  p.calificacion_promedio,
                  p.activo,
                  c.nombre
         ORDER BY f.fecha_agregado DESC, f.id DESC`,
        [userId],
      );

      return res.json(result.rows);
    } catch (error) {
      console.error("Error en getFavorites:", error);
      return res.status(500).json({ error: "Error al obtener favoritos" });
    }
  };

  const removeFavorite = async (req, res) => {
    const userId = getFarmerUserId(req, res);
    if (userId === null) return;

    const productId = getProductId(req.params.productId, res);
    if (productId === null) return;

    try {
      const result = await database.query(
        `DELETE FROM favorito
         WHERE id_usuario = $1
           AND id_producto = $2
         RETURNING id`,
        [userId, productId],
      );

      if (result.rowCount === 0) {
        return res.status(404).json({ error: "El producto no estaba en favoritos" });
      }

      return res.status(204).send();
    } catch (error) {
      console.error("Error en removeFavorite:", error);
      return res.status(500).json({ error: "Error al eliminar el favorito" });
    }
  };

  return { addFavorite, getFavorites, removeFavorite };
};

const controller = createFavoriteController();

module.exports = {
  ...controller,
  createFavoriteController,
};
