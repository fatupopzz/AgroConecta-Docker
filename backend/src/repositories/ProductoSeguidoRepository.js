const { pool } = require("../config/db");
const ProductoSeguido = require("../models/ProductoSeguido");

class ProductoSeguidoRepository {
  constructor(db = pool) {
    this.db = db;
  }

  async findByIdProducto(idProducto) {
    const result = await this.db.query(
      `SELECT id,
              id_agricultor,
              id_producto,
              precio_al_seguir,
              fecha
       FROM producto_seguido
       WHERE id_producto = $1
       ORDER BY fecha DESC`,
      [Number(idProducto)]
    );

    return result.rows.map((row) => ProductoSeguido.fromRow(row));
  }

  async findByIdAgricultorAndIdProducto(idAgricultor, idProducto) {
    const result = await this.db.query(
      `SELECT id,
              id_agricultor,
              id_producto,
              precio_al_seguir,
              fecha
       FROM producto_seguido
       WHERE id_agricultor = $1
         AND id_producto = $2
       LIMIT 1`,
      [Number(idAgricultor), Number(idProducto)]
    );

    return ProductoSeguido.fromRow(result.rows[0]);
  }

  async existsByIdAgricultorAndIdProducto(idAgricultor, idProducto) {
    const result = await this.db.query(
      `SELECT 1
       FROM producto_seguido
       WHERE id_agricultor = $1
         AND id_producto = $2
       LIMIT 1`,
      [Number(idAgricultor), Number(idProducto)]
    );

    return result.rowCount > 0;
  }

  async deleteByIdAgricultorAndIdProducto(idAgricultor, idProducto) {
    const result = await this.db.query(
      `DELETE FROM producto_seguido
       WHERE id_agricultor = $1
         AND id_producto = $2
       RETURNING id,
                 id_agricultor,
                 id_producto,
                 precio_al_seguir,
                 fecha`,
      [Number(idAgricultor), Number(idProducto)]
    );

    return ProductoSeguido.fromRow(result.rows[0]);
  }
}

module.exports = ProductoSeguidoRepository;
