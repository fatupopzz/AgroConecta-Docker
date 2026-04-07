const express = require("express");
const router = express.Router();
const {
  getProducts,
  getProductById,
  getCategories,
} = require("../controllers/product.controller");

// GET /api/products — lista paginada con filtros
router.get("/", getProducts);

// GET /api/products/:id — detalle completo
router.get("/:id", getProductById);

// GET /api/categories — lista de categorías
router.get("/categories", getCategories);

module.exports = router;
