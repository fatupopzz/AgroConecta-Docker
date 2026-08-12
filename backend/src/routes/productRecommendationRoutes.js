const express = require("express");
const verifyToken = require("../middleware/authMiddleware");
const {
  getRecommendedProducts,
} = require("../controllers/productController");

const router = express.Router();

router.get("/recomendados", verifyToken, getRecommendedProducts);

module.exports = router;
