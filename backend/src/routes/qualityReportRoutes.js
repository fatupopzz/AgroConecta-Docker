const express = require("express");
const router = express.Router();

const verifyToken = require("../middleware/authMiddleware");
const verifyAdmin = require("../middleware/adminMiddleware");

const {
    createQualityReport,
    getAllQualityReports,
    updateQualityReport
} = require("../controllers/qualityReportController");

router.post("/quality-reports", verifyToken, createQualityReport);

router.get("/admin/quality-reports", verifyAdmin, getAllQualityReports);

router.patch("/admin/quality-reports/:id", verifyAdmin, updateQualityReport);

module.exports = router;