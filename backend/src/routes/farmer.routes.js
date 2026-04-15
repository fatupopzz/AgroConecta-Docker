/**
 * @file routes/farmer.routes.js
 * @description Rutas para el módulo de agricultores.
 *
 * Base path: /api/farmers
 */

const express = require("express");
const router = express.Router();
const verifyToken = require("../middleware/authMiddleware");
const {
  upsertFarmerProfile,
  getFarmerProfile,
} = require("../controllers/farmer.controller");

/**
 * POST /api/farmers/profile
 * @route   POST /api/farmers/profile
 * @access  Privado — requiere token JWT válido
 */
router.post("/profile", verifyToken, upsertFarmerProfile);

/**
 * GET /api/farmers/profile/:id
 * @route   GET /api/farmers/profile/:id
 * @access  Privado — requiere token JWT válido
 */
router.get("/profile/:id", verifyToken, getFarmerProfile);

module.exports = router;