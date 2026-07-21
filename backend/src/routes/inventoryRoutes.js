const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const {
  getDistributorInventory,
  createInventory,
  updateInventory,
} = require("../controllers/inventoryController");

router.get("/", verifyToken, getDistributorInventory);
router.post("/", verifyToken, createInventory);
router.put("/:id", verifyToken, updateInventory);

module.exports = router;
