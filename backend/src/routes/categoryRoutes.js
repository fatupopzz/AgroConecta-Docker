const express = require("express");
const router = express.Router();
const { getCategories, createCategory, getCategoryById, updateCategory, deleteCategory } = require("../controllers/categoriaController");

router.get("/", getCategories);
router.post("/", createCategory);
router.get("/:id", getCategoryById);
router.put("/:id", updateCategory);
router.delete("/:id", deleteCategory);

module.exports = router;
