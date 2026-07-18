const express = require("express");
const router = express.Router();

const verifyToken = require("../middleware/authMiddleware");

const {
  createDistributorReview,
  getDistributorReviews,
} = require("../controllers/distribuitorReviewController");

router.post("/:id/reviews", verifyToken, createDistributorReview);

router.get("/:id/reviews", getDistributorReviews);

module.exports = router;