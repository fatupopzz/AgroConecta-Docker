const express = require("express");
const router = express.Router();
const {
  canUpdateUserByRole,
  canDeleteUserByRole,
} = require("../middleware/userAuthorizationMiddleware");

const {
  getUsers,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
} = require("../controllers/userController");

router.get("/", getUsers);
router.get("/:id", getUserById);
//router.post("/", createUser);
router.put("/:id", canUpdateUserByRole, updateUser);
router.delete("/:id", canDeleteUserByRole, deleteUser);

module.exports = router;