const express = require("express");
const router = express.Router();

const {
    createQualityReport,
    getAllQualityReports,
    updateQualityReport
} = require("../controllers/qualityReportController");

router.post("/quality-reports", createQualityReport);

router.get("/admin/quality-reports", getAllQualityReports);

router.patch("/admin/quality-reports/:id", updateQualityReport);

module.exports = router;
