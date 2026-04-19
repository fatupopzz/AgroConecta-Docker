const express = require("express");
const router = express.Router();

const {
  getDistributors,
  getDistributorById,
  createDistributor,
  updateDistributor,
  deleteDistributor,
} = require("../controllers/distribuidorController");

router.get("/", getDistributors);
router.get("/:id", getDistributorById);
router.post("/", createDistributor);
router.put("/:id", updateDistributor);
router.delete("/:id", deleteDistributor);

module.exports = router;