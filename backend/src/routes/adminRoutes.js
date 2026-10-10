const express = require("express");
const router = express.Router();

const verifyAdmin = require("../middleware/adminMiddleware");
const {
  getPendingDistributors,
  verifyDistributor,
  rejectDistributor,
} = require("../controllers/adminDistributorController");
const { getAdminMetrics } = require("../controllers/adminMetricsController");
const asyncHandler = require("../middleware/asyncHandler");
const { invalidateCache } = require("../middleware/cacheMiddleware");

// GET /api/admin/metrics
router.get("/metrics", verifyAdmin, asyncHandler(getAdminMetrics));

// GET /api/admin/distributors/pending
router.get("/distributors/pending", verifyAdmin, asyncHandler(getPendingDistributors));

// PATCH /api/admin/distributors/:id/verify
router.patch(
  "/distributors/:id/verify",
  verifyAdmin,
  invalidateCache("distributors"),
  asyncHandler(verifyDistributor),
);

// PATCH /api/admin/distributors/:id/reject
router.patch(
  "/distributors/:id/reject",
  verifyAdmin,
  invalidateCache("distributors"),
  asyncHandler(rejectDistributor),
);

module.exports = router;
