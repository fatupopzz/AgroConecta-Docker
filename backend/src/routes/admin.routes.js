/**
 * @file routes/admin.routes.js
 * @description Rutas administrativas de AgroConecta.
 * Requieren permisos de administrador (a implementar con middleware de auth).
 *
 * Base path: /api/admin
 */

const express = require("express");
const router = express.Router();
const { verifyDistributor } = require("../controllers/distributor.controller");

router.patch("/distributors/:id/verify", verifyDistributor);

module.exports = router;