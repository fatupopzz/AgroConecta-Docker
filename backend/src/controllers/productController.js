const ProductRepository = require("../repositories/ProductRepository");
const productRepository = new ProductRepository();

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const normalizeOptionalText = (value) => {
  if (value === undefined) return undefined;
  if (value === null) return null;
  if (typeof value !== "string") return null;

  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
};

const parseRating = (value) => {
  if (value === undefined) return { valid: true, parsed: undefined };

  const parsed = Number(value);
  const hasMaxTwoDecimals = /^\d+(\.\d{1,2})?$/.test(String(value));

  if (!Number.isFinite(parsed) || parsed < 0 || parsed > 5 || !hasMaxTwoDecimals) {
    return { valid: false };
  }

  return { valid: true, parsed };
};

const getRecommendedProducts = async (req, res) => {
  if (req.user.tipo !== "agricultor") {
    return res.status(403).json({
      error: "Las recomendaciones personalizadas son solo para agricultores",
    });
  }

  const products = await productRepository.findRecommended(req.user.id);
  return res.json(products);
};

const getProducts = async (req, res) => {
  const { nombre, id_categoria, page = 1, limit = 10 } = req.query;

  const pageNumber = parseInt(page);
  const limitNumber = parseInt(limit);

  if (
    isNaN(pageNumber) ||
    pageNumber < 1 ||
    isNaN(limitNumber) ||
    limitNumber < 1 ||
    limitNumber > 100
  ) {
    return res
      .status(400)
      .json({ error: "Parámetros de paginación inválidos" });
  }

  if (nombre !== undefined && typeof nombre !== "string") {
    return res.status(400).json({ error: "nombre debe ser texto" });
  }

  if (id_categoria) {
    if (!isPositiveInteger(id_categoria)) {
      return res.status(400).json({ error: "id_categoria inválido" });
    }
  }

  const result = await productRepository.findAll({
    nombre, id_categoria, page: pageNumber, limit: limitNumber,
  });
  res.json(result);
};

const getProductById = async (req, res) => {
  if (!isPositiveInteger(req.params.id)) {
    return res.status(400).json({ error: "ID de producto inválido" });
  }

  const id = Number(req.params.id);

  const producto = await productRepository.findById(id);

  if (producto.length === 0) {
    return res.status(404).json({ error: "Producto no encontrado" });
  }

  const ofertas = await productRepository.findOffers(id);

  res.json({
    ...producto[0],
    unidad_medida: ofertas.length > 0 ? ofertas[0].unidad_medida : null,
    ofertas,
  });
};

const getProductComparison = async (req, res) => {
  if (!isPositiveInteger(req.params.id)) {
    return res.status(400).json({ error: "ID de producto inválido" });
  }

  const id = Number(req.params.id);

  const producto = await productRepository.findForComparison(id);

  if (producto.length === 0) {
    return res.status(404).json({ error: "Producto no encontrado" });
  }

  const distribuidores = await productRepository.findVerifiedOffers(id);
  const precioMasBajo = distribuidores.length > 0 ? distribuidores[0].precio : null;

  const distribuidoresConBandera = distribuidores.map((distribuidor) => ({
    ...distribuidor,
    es_precio_mas_bajo:
      precioMasBajo !== null && Number(distribuidor.precio) === Number(precioMasBajo),
  }));

  return res.json({
    ...producto[0],
    precio_mas_bajo: precioMasBajo,
    distribuidores: distribuidoresConBandera,
  });
};

const createProduct = async (req, res) => {
  const {
    id_categoria,
    nombre,
    marca,
    descripcion,
    composicion,
    dosis_recomendada,
    instrucciones_uso,
    calificacion_promedio,
  } = req.body;

  if (!id_categoria || !nombre) {
    return res.status(400).json({
      error: "Faltan campos obligatorios: id_categoria y nombre",
    });
  }

  if (!isPositiveInteger(id_categoria)) {
    return res.status(400).json({ error: "id_categoria inválido" });
  }

  if (typeof nombre !== "string" || nombre.trim().length < 2) {
    return res.status(400).json({ error: "Nombre inválido" });
  }

  const ratingValidation = parseRating(calificacion_promedio);
  if (!ratingValidation.valid) {
    return res.status(400).json({
      error:
        "calificacion_promedio inválida (debe ser un número entre 0 y 5 con máximo 2 decimales)",
    });
  }

  const categoriaExist = await productRepository.categoryExists(Number(id_categoria));

  if (!categoriaExist) {
    return res.status(400).json({ error: "La categoría no existe" });
  }

  const newProduct = await productRepository.create({
    id_categoria: Number(id_categoria),
    nombre: nombre.trim(),
    marca: normalizeOptionalText(marca),
    descripcion: normalizeOptionalText(descripcion),
    composicion: normalizeOptionalText(composicion),
    dosis_recomendada: normalizeOptionalText(dosis_recomendada),
    instrucciones_uso: normalizeOptionalText(instrucciones_uso),
    calificacion_promedio: ratingValidation.parsed,
  });

  res.status(201).json({
    message: "Producto creado correctamente",
    producto: newProduct,
  });
};

const updateProduct = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de producto inválido" });
  }

  const {
    id_categoria,
    nombre,
    marca,
    descripcion,
    composicion,
    dosis_recomendada,
    instrucciones_uso,
    calificacion_promedio,
  } = req.body;

  const changes = {};

  if (id_categoria !== undefined) {
    if (!isPositiveInteger(id_categoria)) {
      return res.status(400).json({ error: "id_categoria inválido" });
    }

    const categoriaExist = await productRepository.categoryExists(Number(id_categoria));

    if (!categoriaExist) {
      return res.status(400).json({ error: "La categoría no existe" });
    }

    changes.id_categoria = Number(id_categoria);
  }

  if (nombre !== undefined) {
    if (typeof nombre !== "string" || nombre.trim().length < 2) {
      return res.status(400).json({ error: "Nombre inválido" });
    }

    changes.nombre = nombre.trim();
  }

  const normalizedMarca = normalizeOptionalText(marca);
  if (marca !== undefined && normalizedMarca === null && marca !== null) {
    return res.status(400).json({ error: "marca debe ser texto" });
  }
  if (marca !== undefined) {
    changes.marca = normalizedMarca;
  }

  const normalizedDescripcion = normalizeOptionalText(descripcion);
  if (
    descripcion !== undefined &&
    normalizedDescripcion === null &&
    descripcion !== null
  ) {
    return res.status(400).json({ error: "descripcion debe ser texto" });
  }
  if (descripcion !== undefined) {
    changes.descripcion = normalizedDescripcion;
  }

  const normalizedComposicion = normalizeOptionalText(composicion);
  if (
    composicion !== undefined &&
    normalizedComposicion === null &&
    composicion !== null
  ) {
    return res.status(400).json({ error: "composicion debe ser texto" });
  }
  if (composicion !== undefined) {
    changes.composicion = normalizedComposicion;
  }

  const normalizedDosis = normalizeOptionalText(dosis_recomendada);
  if (
    dosis_recomendada !== undefined &&
    normalizedDosis === null &&
    dosis_recomendada !== null
  ) {
    return res
      .status(400)
      .json({ error: "dosis_recomendada debe ser texto" });
  }
  if (dosis_recomendada !== undefined) {
    changes.dosis_recomendada = normalizedDosis;
  }

  const normalizedInstrucciones = normalizeOptionalText(instrucciones_uso);
  if (
    instrucciones_uso !== undefined &&
    normalizedInstrucciones === null &&
    instrucciones_uso !== null
  ) {
    return res
      .status(400)
      .json({ error: "instrucciones_uso debe ser texto" });
  }
  if (instrucciones_uso !== undefined) {
    changes.instrucciones_uso = normalizedInstrucciones;
  }

  if (calificacion_promedio !== undefined) {
    const ratingValidation = parseRating(calificacion_promedio);
    if (!ratingValidation.valid) {
      return res.status(400).json({
        error:
          "calificacion_promedio inválida (debe ser un número entre 0 y 5 con máximo 2 decimales)",
      });
    }

    changes.calificacion_promedio = ratingValidation.parsed;
  }

  if (Object.keys(changes).length === 0) {
    return res.status(400).json({
      error: "No hay campos válidos para actualizar",
    });
  }

  const updatedProduct = await productRepository.update(Number(id), changes);

  if (!updatedProduct) {
    return res.status(404).json({ error: "Producto no encontrado" });
  }

  res.json({
    message: "Producto actualizado correctamente",
    producto: updatedProduct,
  });
};

const deleteProduct = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID de producto inválido" });
  }

  const deletedProduct = await productRepository.delete(Number(id));

  if (deletedProduct.length === 0) {
    return res.status(404).json({ error: "Producto no encontrado" });
  }

  res.status(200).json({
    message: "Producto eliminado correctamente",
    producto: deletedProduct[0],
  });
};

const comparePrices = async (req, res) => {
  const { product_id } = req.query;

  if (!product_id) {
    return res.status(400).json({
      error: "product_id es requerido",
    });
  }

  const result = await productRepository.comparePrices(product_id);

  if (result.length === 0) {
    return res.json({
      message: "No hay distribuidores disponibles",
      data: [],
    });
  }

  res.json(result);
};


module.exports = {
  getProducts,
  getRecommendedProducts,
  getProductById,
  getProductComparison,
  createProduct,
  updateProduct,
  deleteProduct,
  comparePrices
};
