const express = require("express");
const verifyToken = require("../middleware/authMiddleware");
const asyncHandler = require("../middleware/asyncHandler");
const { createWeatherController } = require("../controllers/weatherController");

const router = express.Router();
router.get("/", verifyToken, asyncHandler(createWeatherController()));
module.exports = router;
