const jwt = require("jsonwebtoken");
const { installCommonJsMock } = require("./helpers/commonJsMocks");

// Antes de cargar app.js: las rutas usan el controlador armado con este pool.
const pool = { query: vi.fn(), connect: vi.fn() };
installCommonJsMock(require.resolve("../src/config/db"), { pool });

const request = require("supertest");
const app = require("../app");
const { createPlantingController } = require("../src/controllers/plantingController");
const {
  buildPlantingTasks,
  validatePlantingInput,
} = require("../src/services/plantingCalendar");

const TODAY = "2026-10-16";
const farmer = { id: 501, tipo: "agricultor" };

// Filas tal como las devuelve la consulta de plantilla del POST.
const MAIZ = [
  { dia_desde_siembra: 0, nombre: "Siembra", tipo: "sembrar", id_categoria_sugerida: 4, categoria_nombre: "Semillas" },
  { dia_desde_siembra: 10, nombre: "Primera fertilización", tipo: "fertilizar", id_categoria_sugerida: 1, categoria_nombre: "Fertilizantes" },
  { dia_desde_siembra: 25, nombre: "Control de cogollero", tipo: "fumigar", id_categoria_sugerida: 2, categoria_nombre: "Pesticidas" },
  { dia_desde_siembra: 80, nombre: "Cosecha de elote", tipo: "cosechar", id_categoria_sugerida: null, categoria_nombre: null },
].map((row) => ({ ...row, id_plantilla: 1, cultivo: "maíz", nombre_cultivo: "Maíz (elote)" }));

const response = () => ({
  statusCode: 200,
  body: null,
  status(code) {
    this.statusCode = code;
    return this;
  },
  json(body) {
    this.body = body;
    return this;
  },
});

/**
 * Base falsa del POST: la plantilla de maíz existe y los INSERT devuelven lo
 * mismo que recibieron, como haría RETURNING.
 */
const plantingDatabase = ({ failTasksInsert = false } = {}) => {
  const client = {
    query: vi.fn(async (sql, params) => {
      if (sql.includes("INSERT INTO siembra")) {
        return { rows: [{ id_siembra: 9, fecha_creacion: new Date("2026-10-16T12:00:00Z") }] };
      }
      if (sql.includes("INSERT INTO tarea_siembra")) {
        if (failTasksInsert) throw new Error("conexión perdida");
        const [, nombres, tipos, fechas, categorias] = params;
        return {
          rows: nombres.map((nombre, index) => ({
            id_tarea: 100 + index,
            nombre,
            tipo: tipos[index],
            fecha_programada: fechas[index],
            id_categoria_sugerida: categorias[index],
            hecha: false,
            fecha_hecha: null,
          })),
        };
      }
      return { rows: [] };
    }),
    release: vi.fn(),
  };
  return {
    client,
    connect: vi.fn(async () => client),
    query: vi.fn(async (sql, params) => {
      if (sql.includes("WHERE p.cultivo = $1")) {
        return { rows: params[0] === "maíz" ? MAIZ : [] };
      }
      throw new Error(`Consulta no esperada: ${sql}`);
    }),
  };
};

const run = async (handler, req) => {
  const res = response();
  await handler({ params: {}, body: {}, ...req }, res);
  return res;
};

describe("generación de tareas", () => {
  test("cada tarea queda en la fecha de siembra más los días de la plantilla", () => {
    const tasks = buildPlantingTasks(MAIZ, "2026-10-06");

    expect(tasks.map((task) => [task.nombre, task.fecha_programada])).toEqual([
      ["Siembra", "2026-10-06"],
      ["Primera fertilización", "2026-10-16"],
      ["Control de cogollero", "2026-10-31"],
      ["Cosecha de elote", "2026-12-25"],
    ]);
    expect(tasks[1]).toMatchObject({ tipo: "fertilizar", id_categoria_sugerida: 1 });
    expect(tasks[3].id_categoria_sugerida).toBeNull();
  });

  test("cruza meses, años y años bisiestos sin correrse un día", () => {
    const plantilla = [
      { nombre: "A", tipo: "otro", dia_desde_siembra: 10 },
      { nombre: "B", tipo: "otro", dia_desde_siembra: 80 },
    ];

    expect(buildPlantingTasks(plantilla, "2026-12-20").map((t) => t.fecha_programada))
      .toEqual(["2026-12-30", "2027-03-10"]);
    expect(buildPlantingTasks(plantilla, "2028-02-20").map((t) => t.fecha_programada))
      .toEqual(["2028-03-01", "2028-05-10"]);
  });

  test("ordena por día y conserva el orden de la plantilla dentro del mismo día", () => {
    const plantilla = [
      { nombre: "Cosecha", tipo: "cosechar", dia_desde_siembra: 110 },
      { nombre: "Siembra", tipo: "sembrar", dia_desde_siembra: 0 },
      { nombre: "Fertilización de base", tipo: "fertilizar", dia_desde_siembra: 0 },
    ];

    expect(buildPlantingTasks(plantilla, TODAY).map((task) => task.nombre))
      .toEqual(["Siembra", "Fertilización de base", "Cosecha"]);
  });
});

describe("validaciones del POST", () => {
  test.each([
    [{ fecha_siembra: TODAY }, "Debes indicar un cultivo válido"],
    [{ cultivo: "", fecha_siembra: TODAY }, "Debes indicar un cultivo válido"],
    [{ cultivo: 12, fecha_siembra: TODAY }, "Debes indicar un cultivo válido"],
    [{ cultivo: "maíz<script>", fecha_siembra: TODAY }, "Debes indicar un cultivo válido"],
  ])("rechaza un cultivo inválido: %j", (body, message) => {
    expect(() => validatePlantingInput(body, TODAY)).toThrow(message);
  });

  test.each([undefined, null, "", "16/10/2026", "2026-10-6", "2026-02-30", "2026-13-01", 20261016])(
    "rechaza la fecha %j",
    (fecha) => {
      expect(() => validatePlantingInput({ cultivo: "maíz", fecha_siembra: fecha }, TODAY))
        .toThrow("fecha_siembra debe ser una fecha válida con formato YYYY-MM-DD");
    },
  );

  test("acepta hasta un año antes o después de hoy y rechaza más allá", () => {
    expect(validatePlantingInput({ cultivo: "Maiz", fecha_siembra: "2025-10-16" }, TODAY))
      .toEqual({ crop: "maíz", fechaSiembra: "2025-10-16" });
    expect(validatePlantingInput({ cultivo: "papa", fecha_siembra: "2027-10-16" }, TODAY).fechaSiembra)
      .toBe("2027-10-16");
    expect(() => validatePlantingInput({ cultivo: "papa", fecha_siembra: "2027-10-17" }, TODAY))
      .toThrow("dentro de un año");
    expect(() => validatePlantingInput({ cultivo: "papa", fecha_siembra: "2062-10-16" }, TODAY))
      .toThrow("dentro de un año");
  });
});

describe("POST /api/plantings", () => {
  test("crea la siembra y sus tareas con las fechas de la plantilla", async () => {
    const database = plantingDatabase();
    const controller = createPlantingController({ database, today: () => TODAY });

    const res = await run(controller.createPlanting, {
      user: farmer,
      body: { cultivo: "MAIZ", fecha_siembra: "2026-10-06" },
    });

    expect(res.statusCode).toBe(201);
    expect(res.body).toMatchObject({
      id_siembra: 9,
      id_plantilla: 1,
      cultivo: "maíz",
      nombre_cultivo: "Maíz (elote)",
      fecha_siembra: "2026-10-06",
    });
    expect(res.body.tareas.map((task) => task.fecha_programada))
      .toEqual(["2026-10-06", "2026-10-16", "2026-10-31", "2026-12-25"]);
    expect(res.body.tareas[1]).toEqual({
      id_tarea: 101,
      nombre: "Primera fertilización",
      tipo: "fertilizar",
      fecha_programada: "2026-10-16",
      categoria_sugerida: { id_categoria: 1, nombre: "Fertilizantes" },
      hecha: false,
      fecha_hecha: null,
    });
    expect(res.body.tareas[3].categoria_sugerida).toBeNull();

    // El nombre se normaliza antes de buscar la plantilla.
    expect(database.query.mock.calls[0][1]).toEqual(["maíz"]);
    const [, siembraParams] = database.client.query.mock.calls
      .find(([sql]) => sql.includes("INSERT INTO siembra"));
    expect(siembraParams).toEqual([501, 1, "2026-10-06"]);
    expect(database.client.query).toHaveBeenCalledWith("COMMIT");
    expect(database.client.release).toHaveBeenCalled();
  });

  test("un cultivo sin plantilla responde 400 sin abrir transacción", async () => {
    const database = plantingDatabase();
    const controller = createPlantingController({ database, today: () => TODAY });

    await expect(run(controller.createPlanting, {
      user: farmer,
      body: { cultivo: "café", fecha_siembra: TODAY },
    })).rejects.toMatchObject({ statusCode: 400, message: "No hay calendario disponible para ese cultivo" });
    expect(database.connect).not.toHaveBeenCalled();
  });

  test("una fecha inválida responde 400 sin consultar la base", async () => {
    const database = plantingDatabase();
    const controller = createPlantingController({ database, today: () => TODAY });

    await expect(run(controller.createPlanting, {
      user: farmer,
      body: { cultivo: "maíz", fecha_siembra: "2026-02-30" },
    })).rejects.toMatchObject({ statusCode: 400 });
    expect(database.query).not.toHaveBeenCalled();
  });

  test("si fallan las tareas revierte la siembra", async () => {
    const database = plantingDatabase({ failTasksInsert: true });
    const controller = createPlantingController({ database, today: () => TODAY });

    await expect(run(controller.createPlanting, {
      user: farmer,
      body: { cultivo: "maíz", fecha_siembra: TODAY },
    })).rejects.toThrow("conexión perdida");
    expect(database.client.query).toHaveBeenCalledWith("ROLLBACK");
    expect(database.client.query).not.toHaveBeenCalledWith("COMMIT");
    expect(database.client.release).toHaveBeenCalled();
  });

  test("solo los agricultores pueden registrar siembras", async () => {
    const database = plantingDatabase();
    const controller = createPlantingController({ database, today: () => TODAY });

    await expect(run(controller.createPlanting, {
      user: { id: 7, tipo: "distribuidor" },
      body: { cultivo: "maíz", fecha_siembra: TODAY },
    })).rejects.toMatchObject({ statusCode: 403 });
    expect(database.query).not.toHaveBeenCalled();
  });
});

describe("GET /api/plantings/mine", () => {
  test("consulta solo las siembras del usuario autenticado", async () => {
    const row = {
      id_siembra: "3",
      id_plantilla: "1",
      cultivo: "maíz",
      nombre_cultivo: "Maíz (elote)",
      fecha_siembra: "2026-10-06",
      fecha_creacion: new Date("2026-10-06T15:00:00Z"),
      tareas: [{ id_tarea: 1, fecha_programada: "2026-10-06" }],
    };
    const database = { query: vi.fn(async () => ({ rows: [row] })) };
    const controller = createPlantingController({ database });

    const res = await run(controller.getMyPlantings, { user: farmer });

    expect(res.body).toEqual([{ ...row, id_siembra: 3, id_plantilla: 1 }]);
    expect(database.query.mock.calls[0][1]).toEqual([501]);
    expect(database.query.mock.calls[0][0]).toMatch(/ORDER BY t\.fecha_programada, t\.id_tarea/);
  });
});

describe("PATCH /api/plantings/tasks/:id", () => {
  const taskDatabase = (ownerId) => ({
    query: vi.fn(async (sql, params) => {
      if (sql.includes("SELECT s.id_usuario")) {
        return { rows: ownerId === null ? [] : [{ id_usuario: ownerId }] };
      }
      if (sql.includes("UPDATE tarea_siembra")) {
        return {
          rows: [{
            id_tarea: params[0],
            id_siembra: 3,
            hecha: params[1],
            fecha_hecha: params[1] ? new Date("2026-10-16T15:00:00Z") : null,
          }],
        };
      }
      throw new Error(`Consulta no esperada: ${sql}`);
    }),
  });
  const updates = (database) =>
    database.query.mock.calls.filter(([sql]) => sql.includes("UPDATE tarea_siembra"));

  test("el dueño marca y desmarca su tarea", async () => {
    const database = taskDatabase(501);
    const controller = createPlantingController({ database });

    const done = await run(controller.updateTaskStatus, {
      user: farmer,
      params: { id: "42" },
      body: { hecha: true },
    });
    expect(done.body).toEqual({
      id_tarea: 42,
      id_siembra: 3,
      hecha: true,
      fecha_hecha: new Date("2026-10-16T15:00:00Z"),
    });

    const undone = await run(controller.updateTaskStatus, {
      user: farmer,
      params: { id: "42" },
      body: { hecha: false },
    });
    expect(undone.body).toMatchObject({ hecha: false, fecha_hecha: null });
    expect(updates(database).map(([, params]) => params)).toEqual([[42, true, 501], [42, false, 501]]);
  });

  test("otro agricultor recibe 403 y la tarea no cambia", async () => {
    const database = taskDatabase(999);
    const controller = createPlantingController({ database });

    await expect(run(controller.updateTaskStatus, {
      user: farmer,
      params: { id: "42" },
      body: { hecha: true },
    })).rejects.toMatchObject({
      statusCode: 403,
      message: "No puedes modificar tareas de una siembra que no es tuya",
    });
    expect(updates(database)).toEqual([]);
  });

  test("una tarea que no existe responde 404", async () => {
    const database = taskDatabase(null);
    const controller = createPlantingController({ database });

    await expect(run(controller.updateTaskStatus, {
      user: farmer,
      params: { id: "42" },
      body: { hecha: true },
    })).rejects.toMatchObject({ statusCode: 404, message: "Tarea no encontrada" });
    expect(updates(database)).toEqual([]);
  });

  test.each([
    [{ id: "42" }, { hecha: "true" }, "hecha debe ser true o false"],
    [{ id: "42" }, {}, "hecha debe ser true o false"],
    [{ id: "abc" }, { hecha: true }, "id de tarea inválido"],
    [{ id: "0" }, { hecha: true }, "id de tarea inválido"],
  ])("valida id y body antes de consultar: %j %j", async (params, body, message) => {
    const database = taskDatabase(501);
    const controller = createPlantingController({ database });

    await expect(run(controller.updateTaskStatus, { user: farmer, params, body }))
      .rejects.toMatchObject({ statusCode: 400, message });
    expect(database.query).not.toHaveBeenCalled();
  });
});

describe("rutas /api/plantings", () => {
  const token = jwt.sign(farmer, process.env.JWT_SECRET);

  beforeEach(() => {
    pool.query.mockReset();
  });

  test.each([
    ["get", "/api/plantings/templates"],
    ["get", "/api/plantings/mine"],
    ["post", "/api/plantings"],
    ["patch", "/api/plantings/tasks/1"],
  ])("%s %s exige token", async (method, path) => {
    const res = await request(app)[method](path);

    expect(res.statusCode).toBe(401);
    expect(pool.query).not.toHaveBeenCalled();
  });

  test("GET /templates arma la duración de cada cultivo", async () => {
    pool.query.mockResolvedValue({
      rows: [{
        id_plantilla: 2,
        cultivo: "frijol",
        nombre: "Frijol",
        descripcion: "Frijol de grano",
        tareas: [
          { nombre: "Siembra", tipo: "sembrar", dia_desde_siembra: 0, categoria_sugerida: null },
          { nombre: "Cosecha", tipo: "cosechar", dia_desde_siembra: 85, categoria_sugerida: null },
        ],
      }],
    });

    const res = await request(app)
      .get("/api/plantings/templates")
      .set("Authorization", `Bearer ${token}`);

    expect(res.statusCode).toBe(200);
    expect(res.body[0]).toMatchObject({ id_plantilla: 2, cultivo: "frijol", duracion_dias: 85 });
    expect(res.body[0].tareas).toHaveLength(2);
  });

  test("los errores de validación llegan con el formato del manejador central", async () => {
    const res = await request(app)
      .post("/api/plantings")
      .set("Authorization", `Bearer ${token}`)
      .send({ cultivo: "maíz", fecha_siembra: "mañana" });

    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ error: "fecha_siembra debe ser una fecha válida con formato YYYY-MM-DD" });
  });
});
