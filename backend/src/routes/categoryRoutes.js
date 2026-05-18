const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const { getCategories, createCategory, getCategoryById, updateCategory, deleteCategory } = require("../controllers/categoriaController");

router.get("/", getCategories);
router.post("/", verifyToken, createCategory);
router.get("/:id", getCategoryById);
router.put("/:id", verifyToken, updateCategory);
router.delete("/:id", verifyToken, deleteCategory);

module.exports = router;
