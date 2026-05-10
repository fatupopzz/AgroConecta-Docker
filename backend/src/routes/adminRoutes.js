const express = require("express");
const router = express.Router();

const verifyAdmin = require("../middleware/adminMiddleware");
const {
  getPendingDistributors,
  verifyDistributor,
  rejectDistributor,
} = require("../controllers/adminDistributorController");

// GET /api/admin/distributors/pending
router.get("/distributors/pending", verifyAdmin, getPendingDistributors);

// PATCH /api/admin/distributors/:id/verify
router.patch("/distributors/:id/verify", verifyAdmin, verifyDistributor);

// PATCH /api/admin/distributors/:id/reject
router.patch("/distributors/:id/reject", verifyAdmin, rejectDistributor);

module.exports = router;
