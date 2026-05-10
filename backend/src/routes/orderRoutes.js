const express = require("express");
const router = express.Router();

const verifyToken = require("../middleware/authMiddleware");
const {
  createOrder,
  getOrderById,
  getOrdersByFarmer,
  getOrdersByDistributor,
  updateOrderStatus,
  receiveOrder,
} = require("../controllers/orderController");

router.post("/", createOrder);
router.get("/farmer/:id", verifyToken, getOrdersByFarmer);
router.get("/distributor/:id", getOrdersByDistributor);
router.patch("/:id/status", updateOrderStatus);
router.patch("/:id/receive", verifyToken, receiveOrder);
router.get("/:id", getOrderById);

module.exports = router;
