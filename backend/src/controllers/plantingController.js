const { pool } = require("../config/db");
const AppError = require("../errors/AppError");
const {
  guatemalaToday,
  buildPlantingTasks,
  validatePlantingInput,
} = require("../services/plantingCalendar");

const isPositiveInteger = (value) => /^[1-9]\d*$/.test(String(value));

const requireFarmerId = (req) => {
  if (req.user?.tipo !== "agricultor") {
    throw new AppError(403, "El calendario de cultivo solo está disponible para agricultores");
  }
  if (!isPositiveInteger(req.user.id)) {
    throw new AppError(400, "Usuario autenticado inválido");
  }
  return Number(req.user.id);
};

const categoryOf = (id, nombre) =>
  id == null ? null : { id_categoria: Number(id), nombre };

const CATEGORY_JSON = (alias) => `CASE WHEN ${alias}.id_categoria IS NULL THEN NULL
  ELSE json_build_object('id_categoria', ${alias}.id_categoria, 'nombre', ${alias}.nombre) END`;

const createPlantingController = ({
  database = pool,
  today = () => guatemalaToday(),
} = {}) => {
  const getTemplates = async (req, res) => {
    const result = await database.query(
      `SELECT p.id_plantilla, p.cultivo, p.nombre, p.descripcion,
              json_agg(json_build_object(
                'nombre', t.nombre,
                'tipo', t.tipo,
                'dia_desde_siembra', t.dia_desde_siembra,
                'categoria_sugerida', ${CATEGORY_JSON("c")}
              ) ORDER BY t.dia_desde_siembra, t.orden, t.id_plantilla_tarea) AS tareas
       FROM plantilla_cultivo p
       JOIN plantilla_tarea t ON t.id_plantilla = p.id_plantilla
       LEFT JOIN categoria c ON c.id_categoria = t.id_categoria_sugerida
       GROUP BY p.id_plantilla
       ORDER BY p.nombre`,
    );

    return res.json(result.rows.map((row) => ({
      id_plantilla: Number(row.id_plantilla),
      cultivo: row.cultivo,
      nombre: row.nombre,
      descripcion: row.descripcion,
      duracion_dias: Math.max(...row.tareas.map((task) => Number(task.dia_desde_siembra))),
      tareas: row.tareas,
    })));
  };

  const createPlanting = async (req, res) => {
    const userId = requireFarmerId(req);
    const { crop, fechaSiembra } = validatePlantingInput(req.body, today());

    const templateResult = await database.query(
      `SELECT p.id_plantilla, p.cultivo, p.nombre AS nombre_cultivo,
              t.nombre, t.tipo, t.dia_desde_siembra, t.id_categoria_sugerida,
              c.nombre AS categoria_nombre
       FROM plantilla_cultivo p
       JOIN plantilla_tarea t ON t.id_plantilla = p.id_plantilla
       LEFT JOIN categoria c ON c.id_categoria = t.id_categoria_sugerida
       WHERE p.cultivo = $1
       ORDER BY t.dia_desde_siembra, t.orden, t.id_plantilla_tarea`,
      [crop],
    );
    if (templateResult.rows.length === 0) {
      throw new AppError(400, "No hay calendario disponible para ese cultivo");
    }

    const template = templateResult.rows[0];
    const categoryNames = new Map(templateResult.rows.map((row) =>
      [row.id_categoria_sugerida, row.categoria_nombre]));
    const tasks = buildPlantingTasks(templateResult.rows, fechaSiembra);

    // La siembra y sus tareas van juntas: una siembra sin tareas dejaría al
    // agricultor con un calendario vacío y sin forma de regenerarlo.
    const client = await database.connect();
    let planting;
    let insertedTasks;
    try {
      await client.query("BEGIN");
      const plantingResult = await client.query(
        `INSERT INTO siembra (id_usuario, id_plantilla, fecha_siembra)
         VALUES ($1, $2, $3)
         RETURNING id_siembra, fecha_creacion`,
        [userId, template.id_plantilla, fechaSiembra],
      );
      planting = plantingResult.rows[0];

      const tasksResult = await client.query(
        `INSERT INTO tarea_siembra
           (id_siembra, nombre, tipo, fecha_programada, id_categoria_sugerida)
         SELECT $1, nombre, tipo, fecha_programada, id_categoria_sugerida
         FROM unnest($2::text[], $3::text[], $4::date[], $5::int[])
           AS t (nombre, tipo, fecha_programada, id_categoria_sugerida)
         RETURNING id_tarea, nombre, tipo,
                   to_char(fecha_programada, 'YYYY-MM-DD') AS fecha_programada,
                   id_categoria_sugerida, hecha, fecha_hecha`,
        [
          planting.id_siembra,
          tasks.map((task) => task.nombre),
          tasks.map((task) => task.tipo),
          tasks.map((task) => task.fecha_programada),
          tasks.map((task) => task.id_categoria_sugerida),
        ],
      );
      insertedTasks = tasksResult.rows;
      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK").catch(() => {});
      throw error;
    } finally {
      client.release();
    }

    return res.status(201).json({
      id_siembra: Number(planting.id_siembra),
      id_plantilla: Number(template.id_plantilla),
      cultivo: template.cultivo,
      nombre_cultivo: template.nombre_cultivo,
      fecha_siembra: fechaSiembra,
      fecha_creacion: planting.fecha_creacion,
      tareas: insertedTasks
        .map((task) => ({
          id_tarea: Number(task.id_tarea),
          nombre: task.nombre,
          tipo: task.tipo,
          fecha_programada: task.fecha_programada,
          categoria_sugerida: categoryOf(
            task.id_categoria_sugerida,
            categoryNames.get(task.id_categoria_sugerida),
          ),
          hecha: task.hecha,
          fecha_hecha: task.fecha_hecha,
        }))
        .sort((left, right) =>
          left.fecha_programada.localeCompare(right.fecha_programada) ||
          left.id_tarea - right.id_tarea),
    });
  };

  const getMyPlantings = async (req, res) => {
    const userId = requireFarmerId(req);

    const result = await database.query(
      `SELECT s.id_siembra, s.id_plantilla, p.cultivo, p.nombre AS nombre_cultivo,
              to_char(s.fecha_siembra, 'YYYY-MM-DD') AS fecha_siembra,
              s.fecha_creacion,
              COALESCE(
                json_agg(json_build_object(
                  'id_tarea', t.id_tarea,
                  'nombre', t.nombre,
                  'tipo', t.tipo,
                  'fecha_programada', to_char(t.fecha_programada, 'YYYY-MM-DD'),
                  'categoria_sugerida', ${CATEGORY_JSON("c")},
                  'hecha', t.hecha,
                  -- Mismo formato ISO en UTC que las fechas que pg convierte a
                  -- Date (fecha_creacion, PATCH): la app parsea uno solo.
                  'fecha_hecha', to_char(t.fecha_hecha AT TIME ZONE 'UTC',
                                         'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
                ) ORDER BY t.fecha_programada, t.id_tarea)
                  FILTER (WHERE t.id_tarea IS NOT NULL),
                '[]'::json
              ) AS tareas
       FROM siembra s
       JOIN plantilla_cultivo p ON p.id_plantilla = s.id_plantilla
       LEFT JOIN tarea_siembra t ON t.id_siembra = s.id_siembra
       LEFT JOIN categoria c ON c.id_categoria = t.id_categoria_sugerida
       WHERE s.id_usuario = $1
       GROUP BY s.id_siembra, p.id_plantilla
       ORDER BY s.fecha_siembra DESC, s.id_siembra DESC`,
      [userId],
    );

    return res.json(result.rows.map((row) => ({
      id_siembra: Number(row.id_siembra),
      id_plantilla: Number(row.id_plantilla),
      cultivo: row.cultivo,
      nombre_cultivo: row.nombre_cultivo,
      fecha_siembra: row.fecha_siembra,
      fecha_creacion: row.fecha_creacion,
      tareas: row.tareas,
    })));
  };

  const updateTaskStatus = async (req, res) => {
    const userId = requireFarmerId(req);
    if (!isPositiveInteger(req.params.id)) {
      throw new AppError(400, "id de tarea inválido");
    }
    const hecha = req.body?.hecha;
    if (typeof hecha !== "boolean") {
      throw new AppError(400, "hecha debe ser true o false");
    }

    const owner = await database.query(
      `SELECT s.id_usuario
       FROM tarea_siembra t
       JOIN siembra s ON s.id_siembra = t.id_siembra
       WHERE t.id_tarea = $1`,
      [Number(req.params.id)],
    );
    if (owner.rows.length === 0) {
      throw new AppError(404, "Tarea no encontrada");
    }
    if (Number(owner.rows[0].id_usuario) !== userId) {
      throw new AppError(403, "No puedes modificar tareas de una siembra que no es tuya");
    }

    // Volver a marcar una tarea ya hecha conserva la fecha en que se hizo.
    const result = await database.query(
      `UPDATE tarea_siembra t
       SET hecha = $2,
           fecha_hecha = CASE WHEN $2 THEN COALESCE(t.fecha_hecha, NOW()) ELSE NULL END
       FROM siembra s
       WHERE t.id_tarea = $1
         AND s.id_siembra = t.id_siembra
         AND s.id_usuario = $3
       RETURNING t.id_tarea, t.id_siembra, t.hecha, t.fecha_hecha`,
      [Number(req.params.id), hecha, userId],
    );
    if (result.rows.length === 0) {
      throw new AppError(404, "Tarea no encontrada");
    }

    const task = result.rows[0];
    return res.json({
      id_tarea: Number(task.id_tarea),
      id_siembra: Number(task.id_siembra),
      hecha: task.hecha,
      fecha_hecha: task.fecha_hecha,
    });
  };

  return { getTemplates, createPlanting, getMyPlantings, updateTaskStatus };
};

module.exports = {
  createPlantingController,
  ...createPlantingController(),
};
