const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const {
  getDistributorInventory,
  createInventory,
  updateInventory,
} = require("../controllers/inventoryController");
const asyncHandler = require("../middleware/asyncHandler");
const { invalidateCache } = require("../middleware/cacheMiddleware");

router.get("/", verifyToken, asyncHandler(getDistributorInventory));
router.post(
  "/",
  verifyToken,
  invalidateCache("products", "distributors"),
  asyncHandler(createInventory),
);
router.put(
  "/:id",
  verifyToken,
  invalidateCache("products", "distributors"),
  asyncHandler(updateInventory),
);

module.exports = router;
