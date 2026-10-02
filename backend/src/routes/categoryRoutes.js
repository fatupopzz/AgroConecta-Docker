const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const { getCategories, createCategory, getCategoryById, updateCategory, deleteCategory } = require("../controllers/categoriaController");
const asyncHandler = require("../middleware/asyncHandler");

router.get("/", asyncHandler(getCategories));
router.post("/", verifyToken, asyncHandler(createCategory));
router.get("/:id", asyncHandler(getCategoryById));
router.put("/:id", verifyToken, asyncHandler(updateCategory));
router.delete("/:id", verifyToken, asyncHandler(deleteCategory));

module.exports = router;
