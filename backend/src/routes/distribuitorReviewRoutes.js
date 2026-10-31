const express = require("express");
const router = express.Router();

const verifyToken = require("../middleware/authMiddleware");

const {
  createDistributorReview,
  getDistributorReviews,
} = require("../controllers/distribuitorReviewController");
const asyncHandler = require("../middleware/asyncHandler");
const { invalidateCache } = require("../middleware/cacheMiddleware");

// El listado de distribuidores está en caché; sin esto las estrellas del menú
// principal no cambiaban hasta que vencía.
router.post(
  "/:id/reviews",
  verifyToken,
  invalidateCache("distributors"),
  asyncHandler(createDistributorReview)
);

router.get("/:id/reviews", asyncHandler(getDistributorReviews));

module.exports = router;