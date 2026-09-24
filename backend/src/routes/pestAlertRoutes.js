const express = require("express");
const {
    createPestAlert,
    getNearbyAlerts,
    getSuggestedProducts,
    getMyAlerts,
    registerPestAlertInstallation
} = require("../controllers/pestAlertController");

const router = express.Router();

// POST /api/alerts/pests - Crear alerta de plaga
router.post("/", createPestAlert);

// GET /api/alerts/pests - Obtener alertas cercanas (query: lat, lng, radio, cultivo)
router.get("/", getNearbyAlerts);

// GET /api/alerts/pests/mine - Obtener mis alertas
router.get("/mine", getMyAlerts);

// POST /api/alerts/pests/installations - Registrar FID y ubicación para push
router.post("/installations", registerPestAlertInstallation);

// GET /api/alerts/pests/:id/products - Productos sugeridos para una alerta
router.get("/:id/products", getSuggestedProducts);

module.exports = router;
