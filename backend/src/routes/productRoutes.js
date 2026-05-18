const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
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
router.post("/", verifyToken, createProduct);
router.put("/:id", verifyToken, updateProduct);
router.delete("/:id", verifyToken, deleteProduct);

module.exports = router;