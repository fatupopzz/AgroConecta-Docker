const jwt = require("jsonwebtoken");

/**
 * Middleware que verifica que el token JWT corresponde a un usuario administrador.
 * Retorna 401 si falta o falla el token, y 403 si el usuario no es administrador.
 */
const verifyAdmin = (req, res, next) => {
  const authHeader = req.headers["authorization"];
  const [scheme, token] = authHeader ? authHeader.split(" ") : [];

  if (scheme !== "Bearer" || !token) {
    return res.status(401).json({ error: "Token requerido" });
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    if (decoded.tipo !== "administrador") {
      return res.status(403).json({ error: "Acceso restringido a administradores" });
    }

    req.user = decoded;
    next();
  } catch (error) {
    return res.status(401).json({ error: "Token inválido o expirado" });
  }
};

module.exports = verifyAdmin;
