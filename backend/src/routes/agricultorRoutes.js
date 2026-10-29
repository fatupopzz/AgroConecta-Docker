const express = require("express");
const router = express.Router();

const {
  getAgricultores,
  getAgricultorById,
  createAgricultor,
  updateAgricultor,
  deleteAgricultor
} = require("../controllers/agricultorController");
const { getFarmLocation, saveFarmLocation } = require("../controllers/farmLocationController");
const asyncHandler = require("../middleware/asyncHandler");

// Antes de "/:id" para que "me" no se tome como un ID.
router.get("/me/ubicacion", asyncHandler(getFarmLocation));
router.put("/me/ubicacion", asyncHandler(saveFarmLocation));

router.get("/", asyncHandler(getAgricultores));
router.get("/:id", asyncHandler(getAgricultorById));
router.post("/", asyncHandler(createAgricultor));
router.put("/:id", asyncHandler(updateAgricultor));
router.delete("/:id", asyncHandler(deleteAgricultor));

module.exports = router;