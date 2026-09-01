const express = require("express");
const router = express.Router();

const verifyToken = require("../middleware/authMiddleware");
const {
  canCreateOrder,
  canManageOrderStatus,
  canViewDistributorOrders,
} = require("../middleware/orderAuthorizationMiddleware");
const {
  createOrder,
  getOrderById,
  getOrdersByFarmer,
  getOrdersByDistributor,
  getOrderTracking,
  updateOrderStatus,
  receiveOrder,
} = require("../controllers/orderController");
const {
  canAccessOrderAdvice,
  canSendOrderAdvice,
} = require("../middleware/adviceAuthorizationMiddleware");
const {
  getAdviceMessages,
  sendAdviceMessage,
} = require("../controllers/adviceController");

router.post("/", verifyToken, canCreateOrder, createOrder);
router.get("/farmer/:id", verifyToken, getOrdersByFarmer);
router.get("/distributor/:id", verifyToken, canViewDistributorOrders, getOrdersByDistributor);
router.patch("/:id/status", verifyToken, canManageOrderStatus, updateOrderStatus);
router.patch("/:id/receive", verifyToken, receiveOrder);
router.get("/:id/advice", verifyToken, canAccessOrderAdvice, getAdviceMessages);
router.post("/:id/advice", verifyToken, canSendOrderAdvice, sendAdviceMessage);
router.get("/:id/tracking", verifyToken, getOrderTracking);
router.get("/:id", verifyToken, getOrderById);

module.exports = router;
