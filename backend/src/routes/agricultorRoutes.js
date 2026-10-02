const express = require("express");
const router = express.Router();

const {
  getAgricultores,
  getAgricultorById,
  createAgricultor,
  updateAgricultor,
  deleteAgricultor
} = require("../controllers/agricultorController");
const asyncHandler = require("../middleware/asyncHandler");

router.get("/", asyncHandler(getAgricultores));
router.get("/:id", asyncHandler(getAgricultorById));
router.post("/", asyncHandler(createAgricultor));
router.put("/:id", asyncHandler(updateAgricultor));
router.delete("/:id", asyncHandler(deleteAgricultor));

module.exports = router;