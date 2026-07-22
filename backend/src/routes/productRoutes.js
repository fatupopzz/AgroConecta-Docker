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
const {
  followProductPrice,
  unfollowProductPrice,
  getProductFollowStatus,
} = require("../controllers/productFollowController");

router.get("/", getProducts);
router.get("/compare", comparePrices);
router.get("/:id/compare", getProductComparison);
router.get("/:id/seguidos", verifyToken, getProductFollowStatus);
router.post("/:id/seguir", verifyToken, followProductPrice);
router.delete("/:id/seguir", verifyToken, unfollowProductPrice);
router.get("/:id", getProductById);
router.post("/", verifyToken, createProduct);
router.put("/:id", verifyToken, updateProduct);
router.delete("/:id", verifyToken, deleteProduct);

module.exports = router;
