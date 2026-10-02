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
  getFarmerDashboard,
} = require("../controllers/userController");
const asyncHandler = require("../middleware/asyncHandler");

router.get("/", asyncHandler(getUsers));
router.get("/dashboard", asyncHandler(getFarmerDashboard));
router.get("/:id", asyncHandler(getUserById));
//router.post("/", createUser);
router.put("/:id", canUpdateUserByRole, asyncHandler(updateUser));
router.delete("/:id", canDeleteUserByRole, asyncHandler(deleteUser));

module.exports = router;
