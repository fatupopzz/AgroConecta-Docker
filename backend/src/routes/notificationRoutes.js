const express = require("express");
const router = express.Router();

const { getNotifications, markNotificationAsRead } = require("../controllers/notificationController");
const asyncHandler = require("../middleware/asyncHandler");

// GET /api/notifications
router.get("/", asyncHandler(getNotifications));

// PATCH /api/notifications/:id/read
router.patch("/:id/read", asyncHandler(markNotificationAsRead));

module.exports = router;