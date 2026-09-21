const bcrypt = require("bcrypt");
const { pool } = require("../config/db");
const jwt = require("jsonwebtoken");
const { withCropList } = require("../utils/cropNames");

const TIPOS_VALIDOS = ["agricultor", "distribuidor"];
const CAMPOS_COMUNES_EDITABLES = ["nombre", "apellido", "telefono", "email", "departamento"];
const CAMPOS_POR_TIPO = {
  agricultor: ["municipio"],
  distribuidor: ["nombre_negocio", "nit", "direccion"],
};

const register = async (req, res) => {
  // Validaciones de body ANTES de tomar conexion del pool
  const {
    nombre,
    apellido,
    telefono,
    email,
    password,
    tipo_usuario,
    departamento,
    municipio,
    nombre_negocio,
    nit,
    direccion,
  } = req.body;

  if (!nombre || !apellido || !telefono || !email || !password || !tipo_usuario) {
    return res.status(400).json({
      error:
        "Datos incompletos. Requeridos: nombre, apellido, telefono, email, password, tipo_usuario",
    });
  }

  if (!TIPOS_VALIDOS.includes(tipo_usuario)) {
    return res.status(400).json({
      error: `tipo_usuario inválido. Use: ${TIPOS_VALIDOS.join(" | ")}`,
    });
  }

  // Validacion de nombre_negocio con trim para distribuidores
  let nombreNegocioNormalizado = null;
  if (tipo_usuario === "distribuidor") {
    if (typeof nombre_negocio !== "string" || nombre_negocio.trim().length < 2) {
      return res.status(400).json({
        error: "nombre_negocio es obligatorio para distribuidores (mínimo 2 caracteres)",
      });
    }
    nombreNegocioNormalizado = nombre_negocio.trim();
  }

  // Ahora si tomamos conexion del pool
  let client;
  let inTransaction = false;

  try {
    client = await pool.connect();

    // Verificar duplicados antes de la transaccion
    const userExist = await client.query(
      "SELECT 1 FROM usuario WHERE telefono = $1 OR email = $2",
      [telefono, email]
    );

    if (userExist.rows.length > 0) {
      return res.status(400).json({ error: "El usuario ya existe" });
    }

    await client.query("BEGIN");
    inTransaction = true;

    const hash = await bcrypt.hash(password, 10);

    const userResult = await client.query(
      `INSERT INTO usuario (nombre, apellido, telefono, email, contrasena_hash, tipo_usuario)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id_usuario, nombre, apellido, telefono, email, tipo_usuario, fecha_registro`,
      [nombre, apellido || null, telefono, email, hash, tipo_usuario]
    );

    const newUser = userResult.rows[0];

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
        `INSERT INTO distribuidor (
          id_usuario,
          nombre_negocio,
          nit,
          departamento,
          direccion,
          estado_verificacion
        )
        VALUES ($1, $2, $3, $4, $5, 'pendiente')
        RETURNING
          id_distribuidor,
          nombre_negocio,
          nit,
          departamento,
          direccion,
          estado_verificacion`,
        [
          newUser.id_usuario,
          nombreNegocioNormalizado,
          nit || null,
          departamento || null,
          direccion || null,
        ]
      );

      perfil = perfilResult.rows[0];
}

    await client.query("COMMIT");
    inTransaction = false;

    return res.status(201).json({
      message: "Usuario creado correctamente",
      user: newUser,
      perfil,
    });
  } catch (error) {
    // Solo hacer rollback si la transaccion estaba abierta
    if (inTransaction && client) {
      try {
        await client.query("ROLLBACK");
      } catch (rollbackError) {
        console.error("Error en rollback de register:", rollbackError);
      }
    }

    // Manejar violacion de UNIQUE constraint (race condition) con 400 en vez de 500
    if (error.code === "23505") {
      const constraint = error.constraint;

      if (constraint === "distribuidor_nit_key") {
        return res.status(400).json({
          error: "El distribuidor ya existe (NIT duplicado)",
        });
      }

      return res.status(400).json({
        error: "El usuario ya existe (telefono o email duplicado)",
      });
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
      return res.status(401).json({ error: "Contraseña incorrecta" });
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

    let idPerfil = null;

    if (user.tipo_usuario === "agricultor") {
      const r = await pool.query(
        "SELECT id_agricultor FROM agricultor WHERE id_usuario = $1",
        [user.id_usuario]
      );
      if (r.rows.length === 0) {
        return res.status(500).json({ error: "Perfil de agricultor no encontrado" });
      }
      idPerfil = r.rows[0].id_agricultor;
    } else if (user.tipo_usuario === "distribuidor") {
      const r = await pool.query(
        "SELECT id_distribuidor FROM distribuidor WHERE id_usuario = $1",
        [user.id_usuario]
      );
      if (r.rows.length === 0) {
        return res.status(500).json({ error: "Perfil de distribuidor no encontrado" });
      }
      idPerfil = r.rows[0].id_distribuidor;
    }

    return res.json({
      message: "Login exitoso",
      token,
      nombre: user.nombre,
      tipoUsuario: user.tipo_usuario,
      idPerfil,
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Error en servidor" });
  }
};

const getMe = async (req, res) => {
  try {
    const { id, tipo } = req.user;

    const userResult = await pool.query(
      `SELECT id_usuario, nombre, apellido, telefono, email, tipo_usuario, fecha_registro
       FROM usuario WHERE id_usuario = $1`,
      [Number(id)]
    );

    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }

    const user = userResult.rows[0];
    let perfil = null;

    if (tipo === "agricultor") {
      const r = await pool.query(
        `SELECT id_agricultor, departamento, municipio, tipo_agricultor,
                tamano_terreno_ha, cultivos_principales, tiene_membresia
         FROM agricultor WHERE id_usuario = $1`,
        [Number(id)]
      );
      perfil = withCropList(r.rows[0] ?? null);
    } else if (tipo === "distribuidor") {
      const r = await pool.query(
        `SELECT id_distribuidor, nombre_negocio, nit, departamento, direccion,
          estado_verificacion, calificacion_promedio
         FROM distribuidor WHERE id_usuario = $1`,
        [Number(id)]
      );
      perfil = r.rows[0] ?? null;
    }

    return res.json({ user, perfil });
  } catch (error) {
    console.error("Error en getMe:", error);
    return res.status(500).json({ error: "Error en servidor" });
  }
};

const normalizeOptionalText = (value) => {
  if (value === null || value === undefined) return null;
  return String(value).trim() || null;
};

const updateMe = async (req, res) => {
  const { id, tipo } = req.user;
  const allowedFields = new Set([
    ...CAMPOS_COMUNES_EDITABLES,
    ...(CAMPOS_POR_TIPO[tipo] || []),
  ]);
  const forbiddenFields = Object.keys(req.body).filter((field) => !allowedFields.has(field));

  if (!TIPOS_VALIDOS.includes(tipo)) {
    return res.status(403).json({ error: "Este tipo de usuario no puede editar este perfil" });
  }

  if (forbiddenFields.length > 0) {
    return res.status(400).json({
      error: `Campos no permitidos: ${forbiddenFields.join(", ")}`,
    });
  }

  const nombre = normalizeOptionalText(req.body.nombre);
  const apellido = normalizeOptionalText(req.body.apellido);
  const telefono = normalizeOptionalText(req.body.telefono);
  const email = normalizeOptionalText(req.body.email);
  const departamento = normalizeOptionalText(req.body.departamento);

  if (!nombre || nombre.length > 100) {
    return res.status(400).json({ error: "El nombre es obligatorio y debe tener hasta 100 caracteres" });
  }
  if (!telefono || telefono.length > 20) {
    return res.status(400).json({ error: "El teléfono es obligatorio y debe tener hasta 20 caracteres" });
  }
  if (email && (email.length > 150 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))) {
    return res.status(400).json({ error: "Correo electrónico inválido" });
  }

  const nombreNegocio = normalizeOptionalText(req.body.nombre_negocio);
  if (tipo === "distribuidor" && (!nombreNegocio || nombreNegocio.length < 2 || nombreNegocio.length > 150)) {
    return res.status(400).json({ error: "El nombre del negocio debe tener entre 2 y 150 caracteres" });
  }

  let client;
  try {
    client = await pool.connect();
    await client.query("BEGIN");

    const userResult = await client.query(
      `UPDATE usuario
       SET nombre = $2, apellido = $3, telefono = $4, email = $5
       WHERE id_usuario = $1
       RETURNING id_usuario, nombre, apellido, telefono, email, tipo_usuario, fecha_registro`,
      [Number(id), nombre, apellido, telefono, email]
    );

    if (userResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Usuario no encontrado" });
    }

    let profileResult;
    if (tipo === "agricultor") {
      profileResult = await client.query(
        `UPDATE agricultor
         SET departamento = $2, municipio = $3
         WHERE id_usuario = $1
         RETURNING id_agricultor, departamento, municipio, tipo_agricultor,
                   tamano_terreno_ha, cultivos_principales, tiene_membresia`,
        [Number(id), departamento, normalizeOptionalText(req.body.municipio)]
      );
    } else {
      profileResult = await client.query(
        `UPDATE distribuidor
         SET nombre_negocio = $2, nit = $3, departamento = $4, direccion = $5
         WHERE id_usuario = $1
         RETURNING id_distribuidor, nombre_negocio, nit, departamento, direccion,
                   estado_verificacion, calificacion_promedio`,
        [
          Number(id),
          nombreNegocio,
          normalizeOptionalText(req.body.nit),
          departamento,
          normalizeOptionalText(req.body.direccion),
        ]
      );
    }

    if (profileResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return res.status(404).json({ error: "Perfil no encontrado" });
    }

    await client.query("COMMIT");
    const perfil = tipo === "agricultor"
      ? withCropList(profileResult.rows[0])
      : profileResult.rows[0];
    return res.json({ user: userResult.rows[0], perfil });
  } catch (error) {
    if (client) {
      try {
        await client.query("ROLLBACK");
      } catch (rollbackError) {
        console.error("Error en rollback de updateMe:", rollbackError);
      }
    }
    if (error.code === "23505") {
      return res.status(409).json({ error: "El teléfono, correo o NIT ya está registrado" });
    }
    console.error("Error en updateMe:", error);
    return res.status(500).json({ error: "Error al actualizar perfil" });
  } finally {
    if (client) client.release();
  }
};

module.exports = { register, login, getMe, updateMe };
