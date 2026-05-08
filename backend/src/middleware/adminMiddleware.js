const jwt = require("jsonwebtoken");

/**
 * Middleware que verifica que el token JWT corresponde a un usuario administrador.
 * Retorna 403 si no hay token o si el tipo de usuario no es 'administrador'.
 *
 * TODO (Juan Jose): revisar y reemplazar con implementación final si aplica.
 */
const verifyAdmin = (req, res, next) => {
  const authHeader = req.headers["authorization"];

  if (!authHeader) {
    return res.status(403).json({ error: "Token requerido" });
  }

  const token = authHeader.split(" ")[1];

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    if (decoded.tipo !== "administrador") {
      return res.status(403).json({ error: "Acceso restringido a administradores" });
    }

    req.user = decoded;
    next();
  } catch (error) {
    return res.status(403).json({ error: "Token inválido o expirado" });
  }
};

module.exports = verifyAdmin;
