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

const followProductPrice = async (req, res) => {
  const request = await buildRequest(req);
  const result = await service.seguirProducto(
    request.idAgricultor,
    request.idProducto
  );

  return res.status(201).json({
    message: "Producto marcado para seguir precio",
    ...result,
  });
};

const unfollowProductPrice = async (req, res) => {
  const request = await buildRequest(req);
  const result = await service.dejarDeSeguir(
    request.idAgricultor,
    request.idProducto
  );

  return res.json({
    message: "Producto removido del seguimiento de precio",
    ...result,
  });
};

const getProductFollowStatus = async (req, res) => {
  const request = await buildRequest(req);
  const result = await service.estaSiguiendo(
    request.idAgricultor,
    request.idProducto
  );

  return res.json(result);
};

module.exports = {
  followProductPrice,
  unfollowProductPrice,
  getProductFollowStatus,
};
