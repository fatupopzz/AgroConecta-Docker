const express = require("express");
const {
  getTemplates,
  createPlanting,
  getMyPlantings,
  updateTaskStatus,
} = require("../controllers/plantingController");
const asyncHandler = require("../middleware/asyncHandler");

const router = express.Router();

// GET /api/plantings/templates - Cultivos disponibles para el selector
router.get("/templates", asyncHandler(getTemplates));

// GET /api/plantings/mine - Siembras del agricultor con sus tareas
router.get("/mine", asyncHandler(getMyPlantings));

// POST /api/plantings - Registrar una siembra y generar su calendario
router.post("/", asyncHandler(createPlanting));

// PATCH /api/plantings/tasks/:id - Marcar o desmarcar una tarea como hecha
router.patch("/tasks/:id", asyncHandler(updateTaskStatus));

module.exports = router;
