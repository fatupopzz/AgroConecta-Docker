const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const canDeleteUserByRole = (req, res, next) => {
  const { id } = req.params;
  const usuarioAutenticado = req.user;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID invalido" });
  }

  if (!usuarioAutenticado || !usuarioAutenticado.id || !usuarioAutenticado.tipo) {
    return res.status(401).json({ error: "Usuario no autenticado" });
  }

  const idObjetivo = Number(id);
  const idSolicitante = Number(usuarioAutenticado.id);
  const tipoSolicitante = usuarioAutenticado.tipo;

  if (tipoSolicitante === "administrador") {
    return next();
  }

  if (tipoSolicitante === "agricultor" || tipoSolicitante === "distribuidor") {
    if (idSolicitante !== idObjetivo) {
      return res.status(403).json({
        error: "Solo puedes eliminar tu propio usuario",
      });
    }

    return next();
  }

  return res.status(403).json({ error: "No tienes permisos para eliminar usuarios" });
};

const canUpdateUserByRole = (req, res, next) => {
  const { id } = req.params;
  const usuarioAutenticado = req.user;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID invalido" });
  }

  if (!usuarioAutenticado || !usuarioAutenticado.id || !usuarioAutenticado.tipo) {
    return res.status(401).json({ error: "Usuario no autenticado" });
  }

  const idObjetivo = Number(id);
  const idSolicitante = Number(usuarioAutenticado.id);
  const tipoSolicitante = usuarioAutenticado.tipo;

  if (tipoSolicitante === "administrador") {
    return next();
  }

  if (tipoSolicitante === "agricultor" || tipoSolicitante === "distribuidor") {
    if (idSolicitante !== idObjetivo) {
      return res.status(403).json({
        error: "Solo puedes actualizar tu propio usuario",
      });
    }

    return next();
  }

  return res.status(403).json({ error: "No tienes permisos para actualizar usuarios" });
};

module.exports = {
  canUpdateUserByRole,
  canDeleteUserByRole,
};
