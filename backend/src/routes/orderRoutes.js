const express = require("express");
const router = express.Router();

const {
  createOrder,
  getOrderById,
  getOrdersByFarmer,
  getOrdersByDistributor,
  updateOrderStatus,
} = require("../controllers/orderController");

router.post("/", createOrder);
router.get("/farmer/:id", getOrdersByFarmer);
router.get("/distributor/:id", getOrdersByDistributor);
router.patch("/:id/status", updateOrderStatus);
router.get("/:id", getOrderById);

module.exports = router;