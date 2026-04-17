const express = require("express");
const router = express.Router();
const {
  getCart,
  addItem,
  updateItem,
  removeItem,
  clearCart,
} = require("../controllers/cart.controller");

router.get("/:id_agricultor", getCart);
router.post("/:id_agricultor/items", addItem);
router.patch("/:id_agricultor/items/:id_item", updateItem);
router.delete("/:id_agricultor/items/:id_item", removeItem);
router.delete("/:id_agricultor", clearCart);

module.exports = router;
