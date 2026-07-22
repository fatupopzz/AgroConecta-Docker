const express = require("express");
const router = express.Router();

const { getNotifications, markNotificationAsRead } = require("../controllers/notificationController");

// GET /api/notifications
router.get("/", getNotifications);

// PATCH /api/notifications/:id/read
router.patch("/:id/read", markNotificationAsRead);

module.exports = router;