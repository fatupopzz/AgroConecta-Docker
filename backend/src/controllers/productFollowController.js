const { pool } = require("../config/db");
const ProductoSeguidoRequest = require("../dto/ProductoSeguidoRequest");
const {
  ProductoSeguidoService,
  ProductoSeguidoServiceError,
} = require("../services/ProductoSeguidoService");

const service = new ProductoSeguidoService();

const getAuthenticatedFarmerId = async (req) => {
  if (req.user?.tipo !== "agricultor") {
    throw new ProductoSeguidoServiceError(
      "Solo agricultores pueden seguir precios",
      403
    );
  }

  const result = await pool.query(
    "SELECT id_agricultor FROM agricultor WHERE id_usuario = $1",
    [Number(req.user.id)]
  );

  if (result.rowCount === 0) {
    throw new ProductoSeguidoServiceError("Perfil de agricultor no encontrado", 404);
  }

  return Number(result.rows[0].id_agricultor);
};

const buildRequest = async (req) => {
  const idAgricultor = await getAuthenticatedFarmerId(req);

  return ProductoSeguidoRequest.fromValues({
    idAgricultor,
    idProducto: req.params.id,
  });
};

const handleError = (res, error, fallbackMessage) => {
  if (error instanceof ProductoSeguidoServiceError) {
    return res.status(error.statusCode).json({ error: error.message });
  }

  console.error(fallbackMessage, error);
  return res.status(500).json({ error: fallbackMessage });
};

const followProductPrice = async (req, res) => {
  try {
    const request = await buildRequest(req);
    const result = await service.seguirProducto(
      request.idAgricultor,
      request.idProducto
    );

    return res.status(201).json({
      message: "Producto marcado para seguir precio",
      ...result,
    });
  } catch (error) {
    return handleError(res, error, "Error al seguir precio del producto");
  }
};

const unfollowProductPrice = async (req, res) => {
  try {
    const request = await buildRequest(req);
    const result = await service.dejarDeSeguir(
      request.idAgricultor,
      request.idProducto
    );

    return res.json({
      message: "Producto removido del seguimiento de precio",
      ...result,
    });
  } catch (error) {
    return handleError(res, error, "Error al dejar de seguir precio del producto");
  }
};

const getProductFollowStatus = async (req, res) => {
  try {
    const request = await buildRequest(req);
    const result = await service.estaSiguiendo(
      request.idAgricultor,
      request.idProducto
    );

    return res.json(result);
  } catch (error) {
    return handleError(res, error, "Error al verificar seguimiento del producto");
  }
};

module.exports = {
  followProductPrice,
  unfollowProductPrice,
  getProductFollowStatus,
};
