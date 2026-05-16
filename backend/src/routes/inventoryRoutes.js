const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const { createInventory } = require("../controllers/inventoryController");

router.post("/", verifyToken, createInventory);

module.exports = router;const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const { createInventory } = require("../controllers/inventoryController");

router.post("/", verifyToken, createInventory);

module.exports = router;
