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
} = require("../controllers/distribuidorController");

router.get("/", getDistributors);
router.get("/:id/rating", getDistributorRating);
router.get("/:id/reviews", getDistributorReviews);
router.get("/:id", getDistributorById);
router.post("/", createDistributor);
router.put("/:id", updateDistributor);
router.delete("/:id", deleteDistributor);

module.exports = router;