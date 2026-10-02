const express = require("express");
const router = express.Router();
const { register, login, getMe, updateMe } = require("../controllers/authController");
const verifyToken = require("../middleware/authMiddleware");
const asyncHandler = require("../middleware/asyncHandler");

router.post("/register", asyncHandler(register));
router.post("/login", asyncHandler(login));
router.get("/me", verifyToken, asyncHandler(getMe));
router.put("/me", verifyToken, asyncHandler(updateMe));

module.exports = router;
