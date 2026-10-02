const express = require("express");
const router = express.Router({ mergeParams: true });
const verifyToken = require("../middleware/authMiddleware");
const { getResenasByProducto, createResena } = require("../controllers/resenaController");
const asyncHandler = require("../middleware/asyncHandler");

router.get("/", asyncHandler(getResenasByProducto));
router.post("/", verifyToken, asyncHandler(createResena));

module.exports = router;
