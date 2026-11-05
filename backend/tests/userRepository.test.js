const UserRepository = require("../src/repositories/UserRepository");

describe("UserRepository", () => {
  let db;
  let repository;

  beforeEach(() => {
    db = { query: vi.fn() };
    repository = new UserRepository(db);
  });

  test.each(["findAll", "findById"])("%s selecciona solo campos publicos", async (method) => {
    const rows = [{ id_usuario: 5, nombre: "Juan" }];
    db.query.mockResolvedValue({ rows });
    expect(await repository[method](5)).toEqual(rows);
    const [sql, params] = db.query.mock.calls[0];
    expect(sql).not.toMatch(/contrasena_hash|SELECT \*/);
    if (method === "findById") expect(params).toEqual([5]);
  });

  test("create persiste el hash recibido y no lo retorna", async () => {
    db.query.mockResolvedValue({ rows: [{ id_usuario: 5 }] });
    expect(await repository.create({ nombre: "Juan", telefono: "5555", email: null, hash: "hashed", tipo_usuario: "agricultor" }))
      .toEqual([{ id_usuario: 5 }]);
    const [sql, params] = db.query.mock.calls[0];
    expect(params).toEqual(["Juan", "5555", null, "hashed", "agricultor"]);
    expect(sql).toContain("contrasena_hash");
    expect(sql.split("RETURNING")[1]).not.toContain("contrasena_hash");
  });

  test("update conserva COALESCE y permite activo=false", async () => {
    db.query.mockResolvedValue({ rows: [{ id_usuario: 5, activo: false }] });
    expect(await repository.update("5", { nombre: "O'Neil", email: null, activo: false }))
      .toEqual([{ id_usuario: 5, activo: false }]);
    const [sql, params] = db.query.mock.calls[0];
    expect(params).toEqual(["5", "O'Neil", undefined, null, false]);
    expect(sql).toContain("activo = COALESCE($5, activo)");
    expect(sql).not.toContain("O'Neil");
    expect(sql.split("RETURNING")[1]).not.toContain("contrasena_hash");
  });

  test.each(["findById", "delete", "findFarmerByUserId"])("%s devuelve vacio si no existe", async (method) => {
    db.query.mockResolvedValue({ rows: [] });
    expect(await repository[method](99)).toEqual([]);
    expect(db.query.mock.calls[0][1]).toEqual([99]);
  });

  test("delete mantiene la eliminacion fisica", async () => {
    db.query.mockResolvedValue({ rows: [{ id_usuario: 5 }] });
    expect(await repository.delete(5)).toEqual([{ id_usuario: 5 }]);
    expect(db.query).toHaveBeenCalledWith("DELETE FROM usuario WHERE id_usuario = $1 RETURNING *", [5]);
  });

  test("dashboard consulta las cuatro secciones por agricultor y excluye cancelados del gasto", async () => {
    const summary = { total_pedidos: 2 };
    const monthly = [{ mes: "2026-11", total: "10" }];
    const topProducts = [{ id_producto: 3 }];
    const lastOrder = { id_pedido: 8 };
    for (const rows of [[summary], monthly, topProducts, [lastOrder]]) db.query.mockResolvedValueOnce({ rows });
    expect(await repository.getFarmerDashboard(4)).toEqual({ summary, monthly, topProducts, lastOrder });
    expect(db.query).toHaveBeenCalledTimes(4);
    for (const [sql, params] of db.query.mock.calls.slice(0, 3)) {
      expect(params).toEqual([4, "cancelado"]);
      expect(sql).toContain("estado <> $2");
    }
    expect(db.query.mock.calls[1][0]).toContain("INTERVAL '5 months'");
    expect(db.query.mock.calls[2][0]).toContain("LIMIT 5");
    expect(db.query.mock.calls[3][1]).toEqual([4]);
    expect(db.query.mock.calls[3][0]).toContain("ORDER BY p.fecha_pedido DESC, p.id_pedido DESC");
  });

  test("dashboard devuelve valores vacios cuando no hay pedidos", async () => {
    db.query.mockResolvedValue({ rows: [] });
    expect(await repository.getFarmerDashboard(4)).toEqual({ summary: {}, monthly: [], topProducts: [], lastOrder: null });
  });

  test.each(["create", "update", "delete", "getFarmerDashboard"])("%s propaga errores sin perder el codigo PostgreSQL", async (method) => {
    const error = Object.assign(new Error("constraint"), { code: "23505" });
    db.query.mockRejectedValue(error);
    const args = method === "create" ? [{}] : method === "update" ? [5, {}] : [5];
    await expect(repository[method](...args)).rejects.toBe(error);
  });
});
