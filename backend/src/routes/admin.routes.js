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

function requireAdminApiKey(req, res, next) {
  const configuredApiKey = process.env.ADMIN_API_KEY;

  // Deny by default until an admin credential is explicitly configured.
  if (!configuredApiKey) {
    return res.status(503).json({
      message: "Admin route is disabled until ADMIN_API_KEY is configured.",
    });
  }

  const headerApiKey = req.get("x-admin-api-key");
  const authorization = req.get("authorization");
  const bearerToken =
    authorization && authorization.startsWith("Bearer ")
      ? authorization.slice("Bearer ".length)
      : null;

  const providedApiKey = headerApiKey || bearerToken;

  if (providedApiKey !== configuredApiKey) {
    return res.status(401).json({ message: "Unauthorized" });
  }

  return next();
}

router.patch("/distributors/:id/verify", requireAdminApiKey, verifyDistributor);
module.exports = router;