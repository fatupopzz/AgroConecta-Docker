const express = require("express");
const { getCropCycles } = require("../controllers/cropCycleController");
const asyncHandler = require("../middleware/asyncHandler");

const router = express.Router();

router.get("/:cultivo", asyncHandler(getCropCycles));

module.exports = router;
