const ProductRepository = require("../src/repositories/ProductRepository");

describe("ProductRepository", () => {
  let db;
  let repository;

  beforeEach(() => {
    db = { query: vi.fn() };
    repository = new ProductRepository(db);
  });

  test("parametriza filtros, escapa LIKE y pagina con el mismo filtro del conteo", async () => {
    const products = [{ id_producto: 3 }];
    db.query.mockResolvedValueOnce({ rows: [{ total: "12" }] })
      .mockResolvedValueOnce({ rows: products });
    expect(await repository.findAll({ nombre: "  50%_! '  ", id_categoria: "4", page: 2, limit: 5 }))
      .toEqual({ page: 2, limit: 5, total: 12, products });
    expect(db.query.mock.calls[0][1]).toEqual(["%50!%!_!! '%", 4]);
    expect(db.query.mock.calls[1][1]).toEqual(["%50!%!_!! '%", 4, 5, 5]);
    for (const [sql] of db.query.mock.calls) {
      expect(sql).toContain("p.activo = true");
      expect(sql).toContain("p.nombre ILIKE $1 ESCAPE '!'");
      expect(sql).toContain("p.id_categoria = $2");
      expect(sql).not.toContain("50%");
    }
    expect(db.query.mock.calls[1][0]).toContain("ORDER BY LOWER(p.nombre) ASC, p.id_producto ASC");
    expect(db.query.mock.calls[1][0]).toContain("LIMIT $3 OFFSET $4");
  });

  test("sin filtros conserva el orden aleatorio y admite un catalogo vacio", async () => {
    db.query.mockResolvedValueOnce({ rows: [{ total: "0" }] }).mockResolvedValueOnce({ rows: [] });
    expect(await repository.findAll({ page: 1, limit: 10 }))
      .toEqual({ page: 1, limit: 10, total: 0, products: [] });
    expect(db.query.mock.calls[0][1]).toEqual([]);
    expect(db.query.mock.calls[1][1]).toEqual([10, 0]);
    expect(db.query.mock.calls[1][0]).toContain("ORDER BY RANDOM()");
  });

  test.each(["findById", "findForComparison"])("%s excluye productos inactivos", async (method) => {
    db.query.mockResolvedValue({ rows: [] });
    expect(await repository[method](7)).toEqual([]);
    expect(db.query).toHaveBeenCalledWith(expect.stringContaining("id_producto = $1 AND p.activo = true"), [7]);
  });

  test.each(["findOffers", "findVerifiedOffers", "comparePrices"])("%s devuelve ofertas con stock ordenadas por precio", async (method) => {
    const rows = [{ precio: "12.50" }];
    db.query.mockResolvedValue({ rows });
    expect(await repository[method](7)).toEqual(rows);
    const [sql, params] = db.query.mock.calls[0];
    expect(params).toEqual([7]);
    expect(sql).toContain("i.stock_disponible > 0");
    expect(sql).toContain("ORDER BY i.precio ASC");
    if (method !== "findOffers") expect(sql).toContain("d.estado_verificacion = 'verificado'");
  });

  test.each([true, false])("categoryExists devuelve %s", async (exists) => {
    db.query.mockResolvedValue({ rows: exists ? [{}] : [] });
    expect(await repository.categoryExists(2)).toBe(exists);
    expect(db.query).toHaveBeenCalledWith(expect.stringContaining("id_categoria = $1"), [2]);
  });

  test.each([undefined, 0, 4.25])("create conserva la calificacion opcional %s", async (rating) => {
    const product = { id_producto: 9 };
    db.query.mockResolvedValue({ rows: [product] });
    expect(await repository.create({ id_categoria: 2, nombre: "Semilla", calificacion_promedio: rating })).toEqual(product);
    const [sql, values] = db.query.mock.calls[0];
    expect(values.slice(0, 2)).toEqual([2, "Semilla"]);
    expect(values).toHaveLength(rating === undefined ? 7 : 8);
    if (rating === undefined) expect(sql).not.toContain("calificacion_promedio");
    else {
      expect(sql).toContain("calificacion_promedio");
      expect(values[7]).toBe(rating);
    }
  });

  test("update usa columnas permitidas, conserva null y cero y omite undefined", async () => {
    db.query.mockResolvedValue({ rows: [{ id_producto: 8 }] });
    expect(await repository.update(8, {
      nombre: "x'; DROP TABLE producto; --", marca: null, descripcion: undefined,
      calificacion_promedio: 0, activo: false, "nombre = NULL --": "invalido",
    })).toEqual({ id_producto: 8 });
    const [sql, params] = db.query.mock.calls[0];
    expect(sql).toContain("SET nombre = $1, marca = $2, calificacion_promedio = $3");
    expect(sql).toContain("WHERE id_producto = $4 AND activo = true");
    expect(sql).not.toContain("DROP TABLE");
    expect(params).toEqual(["x'; DROP TABLE producto; --", null, 0, 8]);
  });

  test("update devuelve null para un producto inexistente", async () => {
    db.query.mockResolvedValue({ rows: [] });
    expect(await repository.update(9, { nombre: "Semilla" })).toBeNull();
  });

  test("update no ejecuta SQL sin campos permitidos", async () => {
    expect(await repository.update(9, { activo: false })).toBeNull();
    expect(db.query).not.toHaveBeenCalled();
  });

  test("delete conserva la baja logica", async () => {
    db.query.mockResolvedValue({ rows: [{ id_producto: 7, activo: false }] });
    expect(await repository.delete(7)).toEqual([{ id_producto: 7, activo: false }]);
    expect(db.query).toHaveBeenCalledWith(expect.stringMatching(/SET activo = false[\s\S]*WHERE id_producto = \$1 AND activo = true/), [7]);
  });

  test("personaliza por cultivos y departamento sin interpolar datos", async () => {
    db.query.mockResolvedValueOnce({ rows: [{ departamento: "  Guatemala  ", cultivos_principales: "MAIZ; frijol/a" }] })
      .mockResolvedValueOnce({ rows: Array.from({ length: 12 }, (_, id_producto) => ({ id_producto })) });
    expect(await repository.findRecommended(6)).toHaveLength(10);
    expect(db.query.mock.calls[0][1]).toEqual([6]);
    const [sql, params] = db.query.mock.calls[1];
    expect(params).toEqual([["Semillas", "Fertilizantes", "Pesticidas", "Herbicidas"], ["maiz", "frijol"], "Guatemala"]);
    expect(sql).toContain("c.nombre = ANY($1::text[])");
    expect(sql).toContain("LOWER($3)");
    expect(sql).toContain("orders.estado = 'entregado'");
  });

  test.each([{ rows: [] }, { rows: [{ departamento: "Guatemala" }] }])("sin perfil completo usa recomendaciones generales", async ({ rows }) => {
    db.query.mockResolvedValueOnce({ rows }).mockResolvedValueOnce({ rows: [] });
    expect(await repository.findRecommended(6)).toEqual([]);
    expect(db.query.mock.calls[1][1]).toEqual([]);
    expect(db.query.mock.calls[1][0]).not.toContain("$3");
  });

  test("propaga el error original para el middleware centralizado", async () => {
    const error = Object.assign(new Error("foreign key"), { code: "23503" });
    db.query.mockRejectedValue(error);
    await expect(repository.create({ id_categoria: 99, nombre: "Semilla" })).rejects.toBe(error);
  });
});
