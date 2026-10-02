const express = require("express");
const router = express.Router();
const {
  getCart,
  addItem,
  updateItem,
  removeItem,
  clearCart,
} = require("../controllers/cartController");
const asyncHandler = require("../middleware/asyncHandler");

router.get("/:id_agricultor", asyncHandler(getCart));
router.post("/:id_agricultor/items", asyncHandler(addItem));
router.patch("/:id_agricultor/items/:id_item", asyncHandler(updateItem));
router.delete("/:id_agricultor/items/:id_item", asyncHandler(removeItem));
router.delete("/:id_agricultor", asyncHandler(clearCart));

module.exports = router;
