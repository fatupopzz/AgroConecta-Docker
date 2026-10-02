const express = require("express");
const {
    createPestAlert,
    getNearbyAlerts,
    getPestAlertById,
    getSuggestedProducts,
    getMyAlerts,
    registerPestAlertToken
} = require("../controllers/pestAlertController");
const asyncHandler = require("../middleware/asyncHandler");

const router = express.Router();

// POST /api/alerts/pests - Crear alerta de plaga
router.post("/", asyncHandler(createPestAlert));

// GET /api/alerts/pests - Obtener alertas cercanas (query: lat, lng, radio, cultivo)
router.get("/", asyncHandler(getNearbyAlerts));

// GET /api/alerts/pests/mine - Obtener mis alertas
router.get("/mine", asyncHandler(getMyAlerts));

// POST /api/alerts/pests/installations - Registrar token FCM y ubicación para push
router.post("/installations", asyncHandler(registerPestAlertToken));

// GET /api/alerts/pests/:id/products - Productos sugeridos para una alerta
router.get("/:id/products", asyncHandler(getSuggestedProducts));

// GET /api/alerts/pests/:id - Obtener una alerta para abrir su notificación
router.get("/:id", asyncHandler(getPestAlertById));

module.exports = router;
