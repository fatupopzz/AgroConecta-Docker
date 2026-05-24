const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const { getDistributorInventory, createInventory } = require("../controllers/inventoryController");

router.get("/", verifyToken, getDistributorInventory);
router.post("/", verifyToken, createInventory);

module.exports = router;
