const AppError = require("../errors/AppError");
const { normalizeCropName } = require("../utils/cropNames");

const GUATEMALA_TIME_ZONE = "America/Guatemala";
const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;
const DAY_MS = 24 * 60 * 60 * 1000;
// Margen para registrar siembras que ya ocurrieron o que se están planeando,
// sin aceptar errores de tipeo como 2062 que llenarían el calendario de basura.
const MAX_DAYS_FROM_TODAY = 365;

/**
 * "Hoy" se calcula en hora de Guatemala y no en UTC: el servidor corre en UTC
 * y a partir de las 18:00 locales ya sería el día siguiente.
 */
const guatemalaToday = (now = new Date()) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: GUATEMALA_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(now);

const guatemalaHour = (now = new Date()) =>
  Number(
    new Intl.DateTimeFormat("en-US", {
      timeZone: GUATEMALA_TIME_ZONE,
      hour: "numeric",
      hourCycle: "h23",
    }).format(now),
  );

/**
 * Devuelve el instante UTC de una fecha YYYY-MM-DD o null si no existe en el
 * calendario. Date.UTC acepta 2026-02-30 y lo convierte en 2 de marzo, por eso
 * se compara contra los componentes originales.
 */
const parseIsoDate = (value) => {
  if (typeof value !== "string") return null;
  const match = ISO_DATE.exec(value);
  if (!match) return null;

  const [, year, month, day] = match.map(Number);
  const time = Date.UTC(year, month - 1, day);
  const date = new Date(time);
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return null;
  }
  return time;
};

// Se trabaja en UTC puro para que sumar días nunca cruce un cambio de horario.
const addDays = (isoDate, days) =>
  new Date(parseIsoDate(isoDate) + days * DAY_MS).toISOString().slice(0, 10);

const daysBetween = (fromIsoDate, toIsoDate) =>
  Math.round((parseIsoDate(toIsoDate) - parseIsoDate(fromIsoDate)) / DAY_MS);

const validatePlantingInput = (body, today) => {
  const { cultivo, fecha_siembra: fechaSiembra } = body || {};

  const crop = normalizeCropName(cultivo);
  if (!crop) {
    throw new AppError(400, "Debes indicar un cultivo válido");
  }

  if (parseIsoDate(fechaSiembra) === null) {
    throw new AppError(400, "fecha_siembra debe ser una fecha válida con formato YYYY-MM-DD");
  }

  if (Math.abs(daysBetween(today, fechaSiembra)) > MAX_DAYS_FROM_TODAY) {
    throw new AppError(400, "fecha_siembra debe estar dentro de un año antes o después de hoy");
  }

  return { crop, fechaSiembra };
};

/**
 * Convierte las tareas de una plantilla en tareas con fecha. Se ordenan por
 * fecha y, en el mismo día, por el orden de la plantilla.
 */
const buildPlantingTasks = (templateTasks, fechaSiembra) =>
  templateTasks
    .map((task, index) => ({ task, index }))
    .sort((left, right) =>
      left.task.dia_desde_siembra - right.task.dia_desde_siembra || left.index - right.index)
    .map(({ task }) => ({
      nombre: task.nombre,
      tipo: task.tipo,
      fecha_programada: addDays(fechaSiembra, Number(task.dia_desde_siembra)),
      id_categoria_sugerida: task.id_categoria_sugerida ?? null,
    }));

module.exports = {
  MAX_DAYS_FROM_TODAY,
  guatemalaToday,
  guatemalaHour,
  parseIsoDate,
  addDays,
  buildPlantingTasks,
  validatePlantingInput,
};
