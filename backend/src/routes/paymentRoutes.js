const express = require("express");
const router = express.Router();

const { createMobilePayment, paymentWebhook } = require("../controllers/paymentController");

router.post("/mobile", createMobilePayment);
router.post("/webhook", paymentWebhook);
module.exports = router;