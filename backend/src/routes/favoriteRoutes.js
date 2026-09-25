const express = require("express");
const {
  addFavorite,
  getFavorites,
  removeFavorite,
} = require("../controllers/favoriteController");

const router = express.Router();

router.get("/", getFavorites);
router.post("/", addFavorite);
router.delete("/:productId", removeFavorite);

module.exports = router;
