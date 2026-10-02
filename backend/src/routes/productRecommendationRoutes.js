const express = require("express");
const verifyToken = require("../middleware/authMiddleware");
const {
  getRecommendedProducts,
} = require("../controllers/productController");
const asyncHandler = require("../middleware/asyncHandler");

const router = express.Router();

router.get("/recomendados", verifyToken, asyncHandler(getRecommendedProducts));

module.exports = router;
