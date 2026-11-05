const UserRepository = require("../repositories/UserRepository");
const userRepository = new UserRepository();
const bcrypt = require("bcrypt");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));


const getUsers = async (req, res) => {
  const result = await userRepository.findAll();

  res.json(result);
};


const getUserById = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  const result = await userRepository.findById(id);

  if (result.length === 0) {
    return res.status(404).json({ error: "Usuario no encontrado" });
  }

  res.json(result[0]);
};


const createUser = async (req, res) => {
  const { nombre, telefono, email, password, tipo_usuario } = req.body;

  if (!nombre || !telefono || !password || !tipo_usuario) {
    return res.status(400).json({
      error: "Campos obligatorios: nombre, telefono, password, tipo_usuario",
    });
  }

  const tiposValidos = ["agricultor", "distribuidor", "administrador"];

  if (!tiposValidos.includes(tipo_usuario)) {
    return res.status(400).json({ error: "tipo_usuario inválido" });
  }

  const hash = await bcrypt.hash(password, 10);

  const result = await userRepository.create({
    nombre: nombre.trim(), telefono, email: email || null, hash, tipo_usuario,
  });

  res.status(201).json({
    message: "Usuario creado correctamente",
    usuario: result[0],
  });
};


const updateUser = async (req, res) => {
  const { id } = req.params;
  const { nombre, telefono, email, activo } = req.body;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  if (req.user.tipo !== "administrador" && Object.hasOwn(req.body, "activo")) {
    return res.status(403).json({ error: "Solo un administrador puede cambiar el estado de una cuenta" });
  }

  const result = await userRepository.update(id, { nombre, telefono, email, activo });

  if (result.length === 0) {
    return res.status(404).json({ error: "Usuario no encontrado" });
  }

  res.json({
    message: "Usuario actualizado",
    usuario: result[0],
  });
};


const deleteUser = async (req, res) => {
  const { id } = req.params;

  if (!isPositiveInteger(id)) {
    return res.status(400).json({ error: "ID inválido" });
  }

  const result = await userRepository.delete(id);

  if (result.length === 0) {
    return res.status(404).json({ error: "Usuario no encontrado" });
  }

  res.json({ message: "Usuario eliminado" });
};

const getFarmerDashboard = async (req, res) => {
  const authenticatedUser = req.user;

  if (!authenticatedUser || authenticatedUser.tipo !== "agricultor") {
    return res.status(403).json({
      error: "Solo un agricultor puede consultar este dashboard",
    });
  }

  const farmerResult = await userRepository.findFarmerByUserId(Number(authenticatedUser.id));

  if (farmerResult.length === 0) {
    return res.status(404).json({ error: "Perfil de agricultor no encontrado" });
  }

  const farmerId = Number(farmerResult[0].id_agricultor);
  const { summary, monthly, topProducts, lastOrder } =
    await userRepository.getFarmerDashboard(farmerId);

  return res.json({
    totalGastadoHistorico: Number(summary.total_gastado_historico || 0),
    totalGastadoMesActual: Number(summary.total_gastado_mes_actual || 0),
    cantidadPedidos: Number(summary.total_pedidos || 0),
    gastosPorMes: monthly.map((row) => ({
      mes: row.mes,
      total: Number(row.total),
    })),
    productosMasComprados: topProducts.map((row) => ({
      idProducto: Number(row.id_producto),
      nombre: row.nombre,
      imagenUrl: row.imagen_url ?? null,
      cantidad: Number(row.cantidad),
      totalGastado: Number(row.total_gastado),
    })),
    ultimoPedido: lastOrder
      ? {
        id: Number(lastOrder.id_pedido),
        fechaPedido: lastOrder.fecha_pedido,
        estado: lastOrder.estado,
        totalPedido: Number(lastOrder.total_pedido),
        distribuidorNombre: lastOrder.distribuidor_nombre,
      }
      : null,
  });
};

module.exports = {
  getUsers,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
  getFarmerDashboard,
};
