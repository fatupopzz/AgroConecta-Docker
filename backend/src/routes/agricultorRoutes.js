const express = require("express");
const router = express.Router();

const {
  getAgricultores,
  getAgricultorById,
  createAgricultor,
  updateAgricultor,
  deleteAgricultor
} = require("../controllers/agricultorController");

router.get("/", getAgricultores);
router.get("/:id", getAgricultorById);
router.post("/", createAgricultor);
router.put("/:id", updateAgricultor);
router.delete("/:id", deleteAgricultor);

module.exports = router;