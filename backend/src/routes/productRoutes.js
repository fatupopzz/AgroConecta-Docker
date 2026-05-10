const express = require("express");
const router = express.Router();
const {
  getProducts,
  getProductById,
  getProductComparison,
  createProduct,
  updateProduct,
  deleteProduct,
  comparePrices
} = require("../controllers/productController");

router.get("/", getProducts);
router.get("/compare", comparePrices);
router.get("/:id/compare", getProductComparison);
router.get("/:id", getProductById);
router.post("/", createProduct);
router.put("/:id", updateProduct);
router.delete("/:id", deleteProduct);

module.exports = router;