const express = require("express");
const router = express.Router({ mergeParams: true });
const verifyToken = require("../middleware/authMiddleware");
const { getResenasByProducto, createResena } = require("../controllers/resenaController");

router.get("/", getResenasByProducto);
router.post("/", verifyToken, createResena);

module.exports = router;
