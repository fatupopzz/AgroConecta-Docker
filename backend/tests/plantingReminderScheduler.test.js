const { installCommonJsMock } = require("./helpers/commonJsMocks");

installCommonJsMock(require.resolve("../src/config/db"), { pool: { query: vi.fn() } });

const { processTodayPlantingTasks } = require("../src/services/plantingReminderScheduler");

// 15:00 UTC son las 9:00 en Guatemala, después de la hora de aviso por defecto.
const MORNING = new Date("2026-10-16T15:00:00Z");
const config = { projectId: "agroconecta-test" };

/**
 * Base falsa con estado: reproduce el filtro de tareas pendientes y la
 * reserva con UPDATE ... WHERE NOT notificada, que es lo que impide repetir.
 */
const createDatabase = ({ tasks, tokens = [] }) => {
  const rows = tasks.map((task) => ({ hecha: false, notificada: false, ...task }));
  const find = (id) => rows.find((task) => task.id_tarea === id);
  const database = {
    rows,
    query: vi.fn(async (sql, params) => {
      if (sql.includes("FROM tarea_siembra t")) {
        return {
          rows: rows.filter((task) =>
            task.fecha_programada === params[0] && !task.hecha && !task.notificada),
        };
      }
      if (sql.includes("FROM instalacion_alerta_plaga")) {
        return { rows: tokens.filter((row) => params[0].includes(row.id_usuario)) };
      }
      if (sql.includes("SET notificada = TRUE")) {
        const task = find(params[0]);
        if (!task || task.notificada || task.hecha) return { rows: [] };
        task.notificada = true;
        return { rows: [{ id_tarea: task.id_tarea }] };
      }
      if (sql.includes("SET notificada = FALSE")) {
        find(params[0]).notificada = false;
        return { rows: [] };
      }
      throw new Error(`Consulta no esperada: ${sql}`);
    }),
  };
  return database;
};

const task = (id, idUsuario, extra = {}) => ({
  id_tarea: id,
  id_siembra: 10 + id,
  id_usuario: idUsuario,
  nombre: `Tarea ${id}`,
  tipo: "fertilizar",
  nombre_cultivo: "Maíz (elote)",
  fecha_programada: "2026-10-16",
  ...extra,
});

const okSender = () => vi.fn(async () => ({ ok: true }));

const runScheduler = (database, sender, options = {}) =>
  processTodayPlantingTasks({
    database,
    now: MORNING,
    config,
    tokenProvider: vi.fn(async () => "access-token"),
    sender,
    reminderHour: 7,
    ...options,
  });

describe("scheduler de tareas de siembra", () => {
  test("notifica las tareas de hoy y no las repite en la siguiente vuelta", async () => {
    const database = createDatabase({
      tasks: [task(1, 501), task(2, 501, { tipo: "fumigar", nombre: "Control de cogollero" })],
      tokens: [{ id_usuario: 501, fcm_registration_token: "tok-ana" }],
    });
    const sender = okSender();

    const first = await runScheduler(database, sender);
    const second = await runScheduler(database, sender);

    expect(first).toEqual({ sent: 2, failed: 0, withoutToken: 0 });
    expect(second).toEqual({ sent: 0, failed: 0, withoutToken: 0 });
    expect(sender).toHaveBeenCalledTimes(2);
    expect(database.rows.every((row) => row.notificada)).toBe(true);

    const [, accessToken, message] = sender.mock.calls[1];
    expect(accessToken).toBe("access-token");
    expect(message).toEqual({
      token: "tok-ana",
      notification: {
        title: "Tarea de tu calendario de cultivo",
        body: "Maíz (elote): Control de cogollero",
      },
      data: {
        type: "planting_task",
        task_id: "2",
        planting_id: "12",
        task_type: "fumigar",
        crop: "Maíz (elote)",
      },
      android: { priority: "high" },
    });
  });

  test("hoy es la fecha de Guatemala aunque en UTC ya sea el día siguiente", async () => {
    const database = createDatabase({ tasks: [] });

    // 03:00 UTC del 17 = 21:00 del 16 en Guatemala.
    await runScheduler(database, okSender(), { now: new Date("2026-10-17T03:00:00Z") });

    expect(database.query.mock.calls[0][1]).toEqual(["2026-10-16"]);
  });

  test("salta a quien no tiene token FCM sin marcarlo y sigue con los demás", async () => {
    const database = createDatabase({
      tasks: [task(1, 502), task(2, 501)],
      tokens: [{ id_usuario: 501, fcm_registration_token: "tok-ana" }],
    });
    const sender = okSender();

    const result = await runScheduler(database, sender);

    expect(result).toEqual({ sent: 1, failed: 0, withoutToken: 1 });
    expect(sender).toHaveBeenCalledTimes(1);
    // Queda pendiente para avisarle si registra el teléfono más tarde.
    expect(database.rows.find((row) => row.id_tarea === 1).notificada).toBe(false);
  });

  test("si ningún envío llega a FCM libera la tarea para reintentarla", async () => {
    const database = createDatabase({
      tasks: [task(1, 501)],
      tokens: [{ id_usuario: 501, fcm_registration_token: "tok-ana" }],
    });
    const failing = vi.fn(async () => ({ ok: false, status: 500 }));

    expect(await runScheduler(database, failing)).toEqual({ sent: 0, failed: 1, withoutToken: 0 });
    expect(database.rows[0].notificada).toBe(false);

    const sender = okSender();
    expect(await runScheduler(database, sender)).toEqual({ sent: 1, failed: 0, withoutToken: 0 });
    expect(database.rows[0].notificada).toBe(true);
  });

  test("con varios teléfonos basta que uno reciba el aviso", async () => {
    const database = createDatabase({
      tasks: [task(1, 501)],
      tokens: [
        { id_usuario: 501, fcm_registration_token: "tok-viejo" },
        { id_usuario: 501, fcm_registration_token: "tok-nuevo" },
      ],
    });
    const sender = vi.fn(async (_config, _token, message) => {
      if (message.token === "tok-viejo") throw new Error("token vencido");
      return { ok: true };
    });

    expect(await runScheduler(database, sender)).toEqual({ sent: 1, failed: 0, withoutToken: 0 });
    expect(sender).toHaveBeenCalledTimes(2);
    expect(database.rows[0].notificada).toBe(true);
  });

  test("no envía una tarea que otra vuelta ya reservó", async () => {
    const database = createDatabase({
      tasks: [task(1, 501)],
      tokens: [{ id_usuario: 501, fcm_registration_token: "tok-ana" }],
    });
    const original = database.query.getMockImplementation();
    // Simula que otra instancia la marcó entre la búsqueda y la reserva.
    database.query.mockImplementation(async (sql, params) => {
      if (sql.includes("SET notificada = TRUE")) return { rows: [] };
      return original(sql, params);
    });
    const sender = okSender();

    expect(await runScheduler(database, sender)).toEqual({ sent: 0, failed: 0, withoutToken: 0 });
    expect(sender).not.toHaveBeenCalled();
  });

  test("antes de la hora de aviso no consulta nada", async () => {
    const database = createDatabase({ tasks: [task(1, 501)] });

    // 12:59 UTC = 6:59 en Guatemala.
    const result = await runScheduler(database, okSender(), { now: new Date("2026-10-16T12:59:00Z") });

    expect(result.skipped).toBe("before_reminder_hour");
    expect(database.query).not.toHaveBeenCalled();
  });

  test("sin Firebase configurado no toca la base", async () => {
    const database = createDatabase({ tasks: [task(1, 501)] });

    const result = await runScheduler(database, okSender(), { config: null });

    expect(result.skipped).toBe("firebase_not_configured");
    expect(database.query).not.toHaveBeenCalled();
    expect(database.rows[0].notificada).toBe(false);
  });
});
