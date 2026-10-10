const express = require("express");
const router = express.Router({ mergeParams: true });
const verifyToken = require("../middleware/authMiddleware");
const { getResenasByProducto, createResena } = require("../controllers/resenaController");
const asyncHandler = require("../middleware/asyncHandler");
const { invalidateCache } = require("../middleware/cacheMiddleware");

router.get("/", asyncHandler(getResenasByProducto));
router.post(
  "/",
  verifyToken,
  invalidateCache("products", "distributors"),
  asyncHandler(createResena),
);

module.exports = router;
