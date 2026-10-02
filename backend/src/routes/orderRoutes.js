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
const { exportOrderHistoryPdf } = require("../controllers/orderExportController");
const {
  createRecurringOrder,
  listRecurringOrders,
  updateRecurringOrder,
} = require("../controllers/recurringOrderController");
const asyncHandler = require("../middleware/asyncHandler");

router.post("/recurring", verifyToken, asyncHandler(createRecurringOrder));
router.get("/recurring", verifyToken, asyncHandler(listRecurringOrders));
router.patch("/recurring/:id", verifyToken, asyncHandler(updateRecurringOrder));
router.post("/", verifyToken, canCreateOrder, asyncHandler(createOrder));
router.get("/export/pdf", verifyToken, asyncHandler(exportOrderHistoryPdf));
router.get("/farmer/:id", verifyToken, asyncHandler(getOrdersByFarmer));
router.get("/distributor/:id", verifyToken, canViewDistributorOrders, asyncHandler(getOrdersByDistributor));
router.patch("/:id/status", verifyToken, canManageOrderStatus, asyncHandler(updateOrderStatus));
router.patch("/:id/receive", verifyToken, asyncHandler(receiveOrder));
router.get("/:id/advice", verifyToken, canAccessOrderAdvice, asyncHandler(getAdviceMessages));
router.post("/:id/advice", verifyToken, canSendOrderAdvice, asyncHandler(sendAdviceMessage));
router.get("/:id/tracking", verifyToken, asyncHandler(getOrderTracking));
router.get("/:id", verifyToken, asyncHandler(getOrderById));

module.exports = router;
