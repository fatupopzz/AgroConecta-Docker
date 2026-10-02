const express = require("express");
const router = express.Router();

const { createMobilePayment, paymentWebhook } = require("../controllers/paymentController");
const asyncHandler = require("../middleware/asyncHandler");

router.post("/mobile", asyncHandler(createMobilePayment));
router.post("/webhook", asyncHandler(paymentWebhook));
module.exports = router;