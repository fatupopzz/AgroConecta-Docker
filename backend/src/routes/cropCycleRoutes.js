const express = require("express");
const { getCropCycles } = require("../controllers/cropCycleController");

const router = express.Router();

router.get("/:cultivo", getCropCycles);

module.exports = router;
