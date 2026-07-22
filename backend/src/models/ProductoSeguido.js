class ProductoSeguido {
  constructor({ id, id_agricultor, id_producto, precio_al_seguir, fecha }) {
    this.id = id !== undefined && id !== null ? Number(id) : null;
    this.idAgricultor = Number(id_agricultor);
    this.idProducto = Number(id_producto);
    this.precioAlSeguir = Number(precio_al_seguir);
    this.fecha = fecha || null;
  }

  static fromRow(row) {
    if (!row) return null;
    return new ProductoSeguido(row);
  }

  toPersistence() {
    return {
      id_agricultor: this.idAgricultor,
      id_producto: this.idProducto,
      precio_al_seguir: this.precioAlSeguir,
    };
  }
}

module.exports = ProductoSeguido;
