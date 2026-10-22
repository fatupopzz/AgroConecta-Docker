const { pool } = require("../config/db");
const {
  firebaseConfig,
  getAccessToken,
  sendMessage,
} = require("./PestAlertPushService");
const { guatemalaToday, guatemalaHour } = require("./plantingCalendar");

// Revisa cada 15 minutos pero solo avisa a partir de la hora configurada: si
// el backend se reinicia a media mañana, los avisos del día igual salen, y la
// marca de notificada evita repetirlos en las siguientes vueltas.
const DEFAULT_INTERVAL_MS = 15 * 60_000;
const DEFAULT_REMINDER_HOUR = 7;
let scheduler = null;
let running = false;

const configuredReminderHour = () => {
  const hour = Number(process.env.PLANTING_REMINDERS_HOUR);
  return Number.isInteger(hour) && hour >= 0 && hour <= 23 ? hour : DEFAULT_REMINDER_HOUR;
};

const buildMessage = (token, task) => ({
  token,
  // Con notification el sistema muestra el aviso aunque la app esté cerrada;
  // data lleva lo necesario para abrir la tarea desde la notificación.
  notification: {
    title: "Tarea de tu calendario de cultivo",
    body: `${task.nombre_cultivo}: ${task.nombre}`,
  },
  data: {
    type: "planting_task",
    task_id: String(task.id_tarea),
    planting_id: String(task.id_siembra),
    task_type: String(task.tipo),
    crop: String(task.nombre_cultivo),
  },
  android: { priority: "high" },
});

/**
 * Manda un push por cada tarea de hoy que no esté hecha ni notificada.
 *
 * Cada tarea se reserva con un UPDATE condicionado antes de enviarla: si dos
 * vueltas del scheduler coinciden, solo una gana la tarea. Si ningún envío
 * llega a FCM se libera la reserva para reintentar en la siguiente vuelta.
 * Las tareas de usuarios sin token FCM se saltan sin marcarlas, así reciben
 * el aviso si registran el teléfono más tarde ese mismo día.
 */
const processTodayPlantingTasks = async ({
  database = pool,
  now = new Date(),
  config = firebaseConfig(),
  tokenProvider = getAccessToken,
  sender = sendMessage,
  reminderHour = configuredReminderHour(),
} = {}) => {
  const summary = { sent: 0, failed: 0, withoutToken: 0 };
  if (!config) return { ...summary, skipped: "firebase_not_configured" };
  if (guatemalaHour(now) < reminderHour) return { ...summary, skipped: "before_reminder_hour" };
  if (running) return { ...summary, skipped: "already_running" };

  running = true;
  try {
    const due = await database.query(
      `SELECT t.id_tarea, t.nombre, t.tipo, s.id_siembra, s.id_usuario,
              p.nombre AS nombre_cultivo
       FROM tarea_siembra t
       JOIN siembra s ON s.id_siembra = t.id_siembra
       JOIN plantilla_cultivo p ON p.id_plantilla = s.id_plantilla
       WHERE t.fecha_programada = $1
         AND NOT t.hecha
         AND NOT t.notificada
       ORDER BY t.id_tarea`,
      [guatemalaToday(now)],
    );
    if (due.rows.length === 0) return summary;

    // Los tokens se registran con las alertas de plaga (HU-029); se usa la
    // misma ventana de 90 días que ese servicio para descartar teléfonos viejos.
    const userIds = [...new Set(due.rows.map((task) => Number(task.id_usuario)))];
    const tokenResult = await database.query(
      `SELECT id_usuario, fcm_registration_token
       FROM instalacion_alerta_plaga
       WHERE id_usuario = ANY($1::int[])
         AND fecha_actualizacion > NOW() - INTERVAL '90 days'`,
      [userIds],
    );
    const tokensByUser = new Map();
    for (const row of tokenResult.rows) {
      const userId = Number(row.id_usuario);
      tokensByUser.set(userId, [...(tokensByUser.get(userId) || []), row.fcm_registration_token]);
    }

    let accessToken = null;
    for (const task of due.rows) {
      const tokens = tokensByUser.get(Number(task.id_usuario)) || [];
      if (tokens.length === 0) {
        summary.withoutToken += 1;
        continue;
      }

      try {
        const claim = await database.query(
          `UPDATE tarea_siembra
           SET notificada = TRUE
           WHERE id_tarea = $1 AND NOT notificada AND NOT hecha
           RETURNING id_tarea`,
          [task.id_tarea],
        );
        // Otra vuelta ya la tomó, o el agricultor la marcó como hecha.
        if (claim.rows.length === 0) continue;

        let delivered = false;
        try {
          accessToken ??= await tokenProvider(config);
          const deliveries = await Promise.all(tokens.map((token) =>
            Promise.resolve()
              .then(() => sender(config, accessToken, buildMessage(token, task)))
              .then((response) => response.ok)
              .catch(() => false)));
          delivered = deliveries.some(Boolean);
        } catch (error) {
          console.error("No se pudo obtener el token de acceso de Firebase:", error.message);
        }

        if (delivered) {
          summary.sent += 1;
        } else {
          summary.failed += 1;
          await database.query(
            "UPDATE tarea_siembra SET notificada = FALSE WHERE id_tarea = $1",
            [task.id_tarea],
          );
        }
      } catch (error) {
        summary.failed += 1;
        console.error(`Error al notificar la tarea de siembra ${task.id_tarea}:`, error);
      }
    }
    return summary;
  } finally {
    running = false;
  }
};

const startPlantingReminderScheduler = () => {
  if (scheduler) return scheduler;
  const configured = Number(process.env.PLANTING_REMINDERS_INTERVAL_MS);
  const intervalMs = Number.isFinite(configured) && configured >= 60_000
    ? configured
    : DEFAULT_INTERVAL_MS;
  const run = () => processTodayPlantingTasks().catch((error) => {
    console.error("Error del scheduler de tareas de siembra:", error);
  });

  run();
  scheduler = setInterval(run, intervalMs);
  scheduler.unref?.();
  return scheduler;
};

const stopPlantingReminderScheduler = () => {
  if (scheduler) clearInterval(scheduler);
  scheduler = null;
};

module.exports = {
  processTodayPlantingTasks,
  startPlantingReminderScheduler,
  stopPlantingReminderScheduler,
};
