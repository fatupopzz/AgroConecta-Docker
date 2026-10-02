const express = require("express");
const router = express.Router();

const verifyToken = require("../middleware/authMiddleware");

const {
  createDistributorReview,
  getDistributorReviews,
} = require("../controllers/distribuitorReviewController");
const asyncHandler = require("../middleware/asyncHandler");

router.post("/:id/reviews", verifyToken, asyncHandler(createDistributorReview));

router.get("/:id/reviews", asyncHandler(getDistributorReviews));

module.exports = router;