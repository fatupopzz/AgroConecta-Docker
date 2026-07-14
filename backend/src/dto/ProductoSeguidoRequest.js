class ProductoSeguidoRequest {
  constructor({ idAgricultor, idProducto }) {
    this.idAgricultor = Number(idAgricultor);
    this.idProducto = Number(idProducto);
  }

  static fromValues({ idAgricultor, idProducto }) {
    return new ProductoSeguidoRequest({ idAgricultor, idProducto });
  }
}

module.exports = ProductoSeguidoRequest;
