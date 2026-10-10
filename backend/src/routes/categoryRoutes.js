const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const { getCategories, createCategory, getCategoryById, updateCategory, deleteCategory } = require("../controllers/categoriaController");
const asyncHandler = require("../middleware/asyncHandler");
const { cacheResponse, invalidateCache } = require("../middleware/cacheMiddleware");

router.get("/", cacheResponse("categories"), asyncHandler(getCategories));
router.post(
  "/",
  verifyToken,
  invalidateCache("categories", "products"),
  asyncHandler(createCategory),
);
router.get("/:id", asyncHandler(getCategoryById));
router.put(
  "/:id",
  verifyToken,
  invalidateCache("categories", "products"),
  asyncHandler(updateCategory),
);
router.delete(
  "/:id",
  verifyToken,
  invalidateCache("categories", "products"),
  asyncHandler(deleteCategory),
);

module.exports = router;
