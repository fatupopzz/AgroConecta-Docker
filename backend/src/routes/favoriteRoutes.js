const express = require("express");
const {
  addFavorite,
  getFavorites,
  removeFavorite,
} = require("../controllers/favoriteController");
const asyncHandler = require("../middleware/asyncHandler");

const router = express.Router();

router.get("/", asyncHandler(getFavorites));
router.post("/", asyncHandler(addFavorite));
router.delete("/:productId", asyncHandler(removeFavorite));

module.exports = router;
