const express = require("express");
const router = express.Router();
const { getCategories } = require("../controllers/product.controller");

router.get("/", getCategories);

module.exports = router;
