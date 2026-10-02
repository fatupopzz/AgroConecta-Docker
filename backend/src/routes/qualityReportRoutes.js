const express = require("express");
const router = express.Router();

const verifyToken = require("../middleware/authMiddleware");
const verifyAdmin = require("../middleware/adminMiddleware");

const {
    createQualityReport,
    getAllQualityReports,
    updateQualityReport
} = require("../controllers/qualityReportController");
const asyncHandler = require("../middleware/asyncHandler");

router.post("/quality-reports", verifyToken, asyncHandler(createQualityReport));

router.get("/admin/quality-reports", verifyAdmin, asyncHandler(getAllQualityReports));

router.patch("/admin/quality-reports/:id", verifyAdmin, asyncHandler(updateQualityReport));

module.exports = router;