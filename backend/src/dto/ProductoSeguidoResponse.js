class ProductoSeguidoResponse {
  constructor(productoSeguido) {
    this.id = productoSeguido.id;
    this.id_agricultor = productoSeguido.idAgricultor;
    this.id_producto = productoSeguido.idProducto;
    this.precio_al_seguir = productoSeguido.precioAlSeguir;
    this.fecha = productoSeguido.fecha;
  }

  static fromEntity(productoSeguido) {
    if (!productoSeguido) return null;
    return new ProductoSeguidoResponse(productoSeguido);
  }
}

module.exports = ProductoSeguidoResponse;
