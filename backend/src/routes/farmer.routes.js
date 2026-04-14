/**
 * @file routes/farmer.routes.js
 * @description Rutas para el módulo de agricultores.
 *
 * Base path: /api/farmers
 */

const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const { upsertFarmerProfile } = require("../controllers/farmer.controller");

/**
 * POST /api/farmers/profile
 * @route   POST /api/farmers/profile
 * @access  Privado — requiere token JWT válido
 */
router.post("/profile", verifyToken, upsertFarmerProfile);

module.exports = router;