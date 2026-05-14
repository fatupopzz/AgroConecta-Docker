const bcrypt = require("bcrypt");
const { pool } = require("../config/db");
const jwt = require("jsonwebtoken");

const TIPOS_VALIDOS = ["agricultor", "distribuidor"];

const register = async (req, res) => {
  let client;

  try {
    const {
      nombre,
      apellido,
      telefono,
      email,
      password,
      tipo_usuario,
      // Datos del perfil (paso 2 del registro)
      departamento,
      municipio,
      // Solo distribuidor
      nombre_negocio,
      nit,
    } = req.body;

    // Validaciones basicas
    if (!nombre || !telefono || !email || !password || !tipo_usuario) {
      return res.status(400).json({
        error:
          "Datos incompletos. Requeridos: nombre, telefono, email, password, tipo_usuario",
      });
    }

    if (!TIPOS_VALIDOS.includes(tipo_usuario)) {
      return res.status(400).json({
        error: `tipo_usuario invalido. Use: ${TIPOS_VALIDOS.join(" | ")}`,
      });
    }

    // Si es distribuidor, nombre_negocio es obligatorio
    if (tipo_usuario === "distribuidor" && (!nombre_negocio || typeof nombre_negocio !== "string")) {
      return res.status(400).json({
        error: "nombre_negocio es obligatorio para distribuidores",
      });
    }

    try {
      client = await pool.connect();
    } catch (connectionError) {
      console.error("Error de conexión en register:", connectionError.message);
      return res.status(500).json({ error: "Error de conexión con la base de datos" });
    }

    // Verificar duplicados
    const userExist = await client.query(
      "SELECT 1 FROM usuario WHERE telefono = $1 OR email = $2",
      [telefono, email]
    );

    if (userExist.rows.length > 0) {
      return res.status(400).json({ error: "El usuario ya existe" });
    }

    await client.query("BEGIN");

    const hash = await bcrypt.hash(password, 10);

    // Insertar usuario
    const userResult = await client.query(
      `INSERT INTO usuario (nombre, apellido, telefono, email, contrasena_hash, tipo_usuario)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id_usuario, nombre, apellido, telefono, email, tipo_usuario, fecha_registro`,
      [nombre, apellido || null, telefono, email, hash, tipo_usuario]
    );

    const newUser = userResult.rows[0];

    // Insertar perfil segun tipo
    let perfil = null;

    if (tipo_usuario === "agricultor") {
      const perfilResult = await client.query(
        `INSERT INTO agricultor (id_usuario, departamento, municipio, tipo_agricultor)
         VALUES ($1, $2, $3, 'pequena_escala')
         RETURNING id_agricultor, departamento, municipio, tipo_agricultor`,
        [newUser.id_usuario, departamento || null, municipio || null]
      );
      perfil = perfilResult.rows[0];
    } else if (tipo_usuario === "distribuidor") {
      const perfilResult = await client.query(
        `INSERT INTO distribuidor (id_usuario, nombre_negocio, nit, departamento, estado_verificacion)
         VALUES ($1, $2, $3, $4, 'pendiente')
         RETURNING id_distribuidor, nombre_negocio, nit, departamento, estado_verificacion`,
        [newUser.id_usuario, nombre_negocio, nit || null, departamento || null]
      );
      perfil = perfilResult.rows[0];
    }

    await client.query("COMMIT");

    return res.status(201).json({
      message: "Usuario creado correctamente",
      user: newUser,
      perfil,
    });
  } catch (error) {
    try {
      await client.query("ROLLBACK");
    } catch (rollbackError) {
      console.error("Error en rollback de register:", rollbackError);
    }
    console.error("Error en register:", error);
    return res.status(500).json({ error: "Error en servidor" });
  } finally {
    if (client) client.release();
  }
};

const login = async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: "Datos incompletos" });
    }

    const result = await pool.query(
      "SELECT * FROM usuario WHERE email = $1",
      [email]
    );

    if (result.rows.length === 0) {
      return res.status(400).json({ error: "Usuario no existe" });
    }

    const user = result.rows[0];

    const validPassword = await bcrypt.compare(password, user.contrasena_hash);

    if (!validPassword) {
      return res.status(401).json({ error: "Contrasena incorrecta" });
    }

    const token = jwt.sign(
      {
        id: user.id_usuario,
        email: user.email,
        tipo: user.tipo_usuario,
      },
      process.env.JWT_SECRET,
      { expiresIn: "1h" }
    );

    res.json({
      message: "Login exitoso",
      token,
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Error en servidor" });
  }
};

module.exports = { register, login };
