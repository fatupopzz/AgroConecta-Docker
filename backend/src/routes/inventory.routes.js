const express = require("express");
const router = express.Router();
const { updateInventory } = require("../controllers/product.controller");

router.patch("/:id", updateInventory);

module.exports = router;