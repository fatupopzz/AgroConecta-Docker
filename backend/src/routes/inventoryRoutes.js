const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const {
  getDistributorInventory,
  createInventory,
  updateInventory,
} = require("../controllers/inventoryController");
const asyncHandler = require("../middleware/asyncHandler");

router.get("/", verifyToken, asyncHandler(getDistributorInventory));
router.post("/", verifyToken, asyncHandler(createInventory));
router.put("/:id", verifyToken, asyncHandler(updateInventory));

module.exports = router;
