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
const asyncHandler = require("../middleware/asyncHandler");
const { cacheResponse, invalidateCache } = require("../middleware/cacheMiddleware");

router.get("/", cacheResponse("products"), asyncHandler(getProducts));
router.get("/compare", asyncHandler(comparePrices));
router.get("/:id/compare", asyncHandler(getProductComparison));
router.get("/:id/seguidos", verifyToken, asyncHandler(getProductFollowStatus));
router.post("/:id/seguir", verifyToken, asyncHandler(followProductPrice));
router.delete("/:id/seguir", verifyToken, asyncHandler(unfollowProductPrice));
router.get("/:id", asyncHandler(getProductById));
router.post("/", verifyToken, invalidateCache("products"), asyncHandler(createProduct));
router.put("/:id", verifyToken, invalidateCache("products"), asyncHandler(updateProduct));
router.delete("/:id", verifyToken, invalidateCache("products"), asyncHandler(deleteProduct));

module.exports = router;
