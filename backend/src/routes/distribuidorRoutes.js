const express = require("express");
const router = express.Router();

const {
  getDistributors,
  getDistributorById,
  createDistributor,
  updateDistributor,
  deleteDistributor,
  getDistributorRating,
  getDistributorReviews,
  getDistributorProducts,
  getDistributorStats
} = require("../controllers/distribuidorController");
const {
  canViewDistributorStats,
} = require("../middleware/orderAuthorizationMiddleware");
const asyncHandler = require("../middleware/asyncHandler");
const { cacheResponse, invalidateCache } = require("../middleware/cacheMiddleware");

router.get("/", cacheResponse("distributors"), asyncHandler(getDistributors));
router.get("/:id/rating", asyncHandler(getDistributorRating));
router.get("/:id/reviews", asyncHandler(getDistributorReviews));
router.get("/:id/productos", asyncHandler(getDistributorProducts));
router.get("/:id/stats", canViewDistributorStats, asyncHandler(getDistributorStats));
router.get("/:id", asyncHandler(getDistributorById));
router.post("/", invalidateCache("distributors"), asyncHandler(createDistributor));
router.put("/:id", invalidateCache("distributors"), asyncHandler(updateDistributor));
router.delete("/:id", invalidateCache("distributors"), asyncHandler(deleteDistributor));

module.exports = router;
