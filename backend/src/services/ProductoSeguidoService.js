const { pool } = require("../config/db");
const ProductoSeguido = require("../models/ProductoSeguido");
const ProductoSeguidoResponse = require("../dto/ProductoSeguidoResponse");
const ProductoSeguidoRepository = require("../repositories/ProductoSeguidoRepository");

class ProductoSeguidoServiceError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.name = "ProductoSeguidoServiceError";
    this.statusCode = statusCode;
  }
}

const parsePositiveInteger = (value, fieldName) => {
  const parsed = Number(value);

  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new ProductoSeguidoServiceError(`${fieldName} inválido`, 400);
  }

  return parsed;
};

class ProductoSeguidoService {
  constructor(db = pool, repository = new ProductoSeguidoRepository(db)) {
    this.db = db;
    this.repository = repository;
  }

  async seguirProducto(idAgricultor, idProducto) {
    const agricultorId = parsePositiveInteger(idAgricultor, "id_agricultor");
    const productoId = parsePositiveInteger(idProducto, "id_producto");

    await this.ensureAgricultorExists(agricultorId);
    const product = await this.getProductWithCurrentPrice(productoId);

    if (await this.repository.existsByIdAgricultorAndIdProducto(agricultorId, productoId)) {
      throw new ProductoSeguidoServiceError("El agricultor ya sigue este producto", 409);
    }

    const result = await this.db.query(
      `INSERT INTO producto_seguido
         (id_agricultor, id_producto, precio_al_seguir)
       VALUES ($1, $2, $3)
       RETURNING id,
                 id_agricultor,
                 id_producto,
                 precio_al_seguir,
                 fecha`,
      [agricultorId, productoId, Number(product.precio_actual)]
    );

    return {
      siguiendo: true,
      producto_seguido: ProductoSeguidoResponse.fromEntity(
        ProductoSeguido.fromRow(result.rows[0])
      ),
    };
  }

  async dejarDeSeguir(idAgricultor, idProducto) {
    const agricultorId = parsePositiveInteger(idAgricultor, "id_agricultor");
    const productoId = parsePositiveInteger(idProducto, "id_producto");

    await this.ensureAgricultorExists(agricultorId);
    await this.ensureProductExists(productoId);

    const deleted = await this.repository.deleteByIdAgricultorAndIdProducto(
      agricultorId,
      productoId
    );

    if (!deleted) {
      throw new ProductoSeguidoServiceError("El producto no estaba en seguimiento", 404);
    }

    return {
      siguiendo: false,
      producto_seguido: ProductoSeguidoResponse.fromEntity(deleted),
    };
  }

  async estaSiguiendo(idAgricultor, idProducto) {
    const agricultorId = parsePositiveInteger(idAgricultor, "id_agricultor");
    const productoId = parsePositiveInteger(idProducto, "id_producto");

    await this.ensureAgricultorExists(agricultorId);
    await this.ensureProductExists(productoId);

    const followed = await this.repository.findByIdAgricultorAndIdProducto(
      agricultorId,
      productoId
    );

    return {
      siguiendo: followed !== null,
      producto_seguido: ProductoSeguidoResponse.fromEntity(followed),
    };
  }

  async ensureAgricultorExists(idAgricultor) {
    const result = await this.db.query(
      "SELECT 1 FROM agricultor WHERE id_agricultor = $1",
      [idAgricultor]
    );

    if (result.rowCount === 0) {
      throw new ProductoSeguidoServiceError("Agricultor no encontrado", 404);
    }
  }

  async ensureProductExists(idProducto) {
    const result = await this.db.query(
      "SELECT 1 FROM producto WHERE id_producto = $1 AND activo = true",
      [idProducto]
    );

    if (result.rowCount === 0) {
      throw new ProductoSeguidoServiceError("Producto no encontrado", 404);
    }
  }

  async getProductWithCurrentPrice(idProducto) {
    const result = await this.db.query(
      `SELECT p.id_producto,
              p.nombre,
              MIN(i.precio) AS precio_actual
       FROM producto p
       LEFT JOIN inventario_distribuidor i
         ON p.id_producto = i.id_producto
        AND i.stock_disponible > 0
       WHERE p.id_producto = $1
         AND p.activo = true
       GROUP BY p.id_producto, p.nombre`,
      [idProducto]
    );

    if (result.rowCount === 0) {
      throw new ProductoSeguidoServiceError("Producto no encontrado", 404);
    }

    const product = result.rows[0];

    if (product.precio_actual === null) {
      throw new ProductoSeguidoServiceError("Producto sin precio disponible", 409);
    }

    return product;
  }
}

module.exports = {
  ProductoSeguidoService,
  ProductoSeguidoServiceError,
};
