const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { installCommonJsMock } = require("./helpers/commonJsMocks");

// La prueba de punta a punta levanta app.js, que lee UPLOADS_DIR al importarse.
const uploadsDir = fs.mkdtempSync(path.join(os.tmpdir(), "agroconecta-stock-uploads-"));
process.env.UPLOADS_DIR = uploadsDir;

const pool = { query: vi.fn() };
installCommonJsMock(require.resolve("../src/config/db"), { pool });

const bcrypt = require("bcrypt");
const app = require("../app");
const {
  buildQueries,
  cleanProductName,
  discoverProducts,
  fetchStockImages,
} = require("../scripts/fetch_stock_images");
const { uploadProductImages } = require("../scripts/upload_product_images");

const API = "http://api.test/api";
const PNG = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  Buffer.alloc(32, 1),
]);
const JPEG = Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), Buffer.alloc(32, 2)]);

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

let workDir;
beforeEach(() => {
  vi.clearAllMocks();
  workDir = fs.mkdtempSync(path.join(os.tmpdir(), "agroconecta-stock-"));
});
afterEach(() => fs.rmSync(workDir, { recursive: true, force: true }));
afterAll(() => fs.rmSync(uploadsDir, { recursive: true, force: true }));

describe("búsqueda de fotos", () => {
  test("limpia porcentajes y códigos de formulación del nombre", () => {
    expect(cleanProductName("Urea 46%")).toBe("urea");
    expect(cleanProductName("Glifosato 35.6 SL (1 L)")).toBe("glifosato");
    expect(cleanProductName("Semilla de Maíz Blanco")).toBe("semilla de maíz blanco");
  });

  test("busca primero nombre con categoría y termina en la categoría sola", () => {
    expect(buildQueries({ nombre: "Urea 46%", categoria: "Fertilizantes" })).toEqual([
      "urea fertilizante agricultura",
      "urea",
      "fertilizante agricultura",
    ]);
    // Un nombre que se queda vacío al limpiarlo cae directo en la categoría.
    expect(buildQueries({ nombre: "20-20-20", categoria: "Fertilizantes" })).toEqual([
      "fertilizante agricultura",
    ]);
    expect(buildQueries({ nombre: "Pala", categoria: "Otra cosa" })).toEqual([
      "pala agricultura cultivo",
      "pala",
      "agricultura cultivo",
    ]);
  });

  test("encuentra todos los productos aunque haya ids desactivados en medio", async () => {
    const activos = { 1: "Urea", 4: "Semilla", 9: "Glifosato" };
    const fetchImpl = vi.fn(async (url) => {
      if (url.endsWith("/products?limit=1")) return json({ total: 3, products: [] });
      const id = Number(url.split("/").pop());
      return activos[id]
        ? json({ id_producto: id, nombre: activos[id], categoria: "Fertilizantes", imagen_url: null })
        : json({ error: "Producto no encontrado" }, 404);
    });

    const { products, expected } = await discoverProducts({ apiBase: API, fetchImpl });

    expect(expected).toBe(3);
    expect(products.map((product) => product.id_producto)).toEqual([1, 4, 9]);
    // Se detiene al encontrar los tres, sin seguir pidiendo ids.
    expect(fetchImpl).toHaveBeenCalledTimes(1 + 9);
  });

  test("descarga una foto distinta por producto y arma la página de revisión", async () => {
    const catalogo = {
      1: { nombre: "Urea 46%", categoria: "Fertilizantes", imagen_url: null },
      2: { nombre: "Abono con foto", categoria: "Fertilizantes", imagen_url: "/uploads/productos/x.png" },
      4: { nombre: "Nitrato <especial>", categoria: "Fertilizantes", imagen_url: null },
      5: { nombre: "Raro", categoria: "Semillas", imagen_url: null },
    };
    const photo = (id) => ({
      id,
      url: `https://www.pexels.com/photo/${id}/`,
      photographer: `Fotógrafa ${id}`,
      src: { large: `https://images.pexels.com/${id}.jpeg` },
    });
    const pexelsCalls = [];
    const fetchImpl = vi.fn(async (url, options) => {
      if (url.endsWith("/products?limit=1")) return json({ total: 4 });
      if (url.startsWith(`${API}/products/`)) {
        const id = Number(url.split("/").pop());
        return catalogo[id]
          ? json({ id_producto: id, ...catalogo[id] })
          : json({ error: "no" }, 404);
      }
      if (url.startsWith("https://api.pexels.com/")) {
        const query = new URL(url).searchParams;
        pexelsCalls.push({ query: query.get("query"), locale: query.get("locale"), auth: options.headers.Authorization });
        if (query.get("query").startsWith("raro") || query.get("query").startsWith("semillas")) {
          return json({ photos: [] });
        }
        // Las dos búsquedas de fertilizantes traen la misma foto 100 primero.
        return json({ photos: [photo(100), photo(200)] });
      }
      if (url.startsWith("https://images.pexels.com/")) {
        return new Response(JPEG, { status: 200, headers: { "Content-Type": "image/jpeg" } });
      }
      throw new Error(`URL no esperada: ${url}`);
    });

    const { rows } = await fetchStockImages({
      apiBase: API,
      apiKey: "clave-pexels",
      outDir: workDir,
      fetchImpl,
      pauseMs: 0,
      log: () => {},
    });

    const byId = Object.fromEntries(rows.map((row) => [row.id_producto, row]));
    expect(byId[2]).toBeUndefined();
    expect(byId[1]).toMatchObject({ status: "descargada", archivo: "1.jpg", query: "urea fertilizante agricultura" });
    expect(byId[4]).toMatchObject({ status: "descargada", archivo: "4.jpg", pexels_url: "https://www.pexels.com/photo/200/" });
    expect(byId[5]).toMatchObject({ status: "sin_resultado" });

    expect(fs.readFileSync(path.join(workDir, "1.jpg"))).toEqual(JPEG);
    expect(fs.existsSync(path.join(workDir, "2.jpg"))).toBe(false);
    expect(pexelsCalls.every((call) => call.auth === "clave-pexels" && call.locale === "es-ES")).toBe(true);

    const html = fs.readFileSync(path.join(workDir, "revisar.html"), "utf8");
    expect(html).toContain("Nitrato &lt;especial&gt;");
    expect(html).not.toContain("<especial>");
    expect(html).toContain("Sin foto (1)");
    const csv = fs.readFileSync(path.join(workDir, "creditos.csv"), "utf8").trim().split("\n");
    expect(csv).toHaveLength(3);
  });

  test("una API key inválida corta con un mensaje claro", async () => {
    const fetchImpl = vi.fn(async (url) => {
      if (url.endsWith("/products?limit=1")) return json({ total: 1 });
      if (url.startsWith(`${API}/products/`)) {
        return json({ id_producto: 1, nombre: "Urea", categoria: "Fertilizantes", imagen_url: null });
      }
      return json({ error: "Unauthorized" }, 401);
    });

    await expect(fetchStockImages({
      apiBase: API, apiKey: "mala", outDir: workDir, fetchImpl, pauseMs: 0, log: () => {},
    })).rejects.toThrow("Pexels rechazó la API key");
  });
});

describe("subida por la API", () => {
  const loginOk = (tipoUsuario = "administrador") => json({ token: "tok", tipoUsuario });

  test("rechaza una cuenta de agricultor antes de subir nada", async () => {
    fs.writeFileSync(path.join(workDir, "1.jpg"), JPEG);
    const fetchImpl = vi.fn(async () => loginOk("agricultor"));

    await expect(uploadProductImages({
      apiBase: API, email: "a@b", password: "x", folder: workDir, fetchImpl,
    })).rejects.toThrow("La cuenta debe ser de administrador o distribuidor");
    expect(fetchImpl).toHaveBeenCalledTimes(1);
  });

  test("no pisa fotos existentes salvo con reemplazar y salta lo que no corresponde", async () => {
    fs.writeFileSync(path.join(workDir, "1.jpg"), JPEG);
    fs.writeFileSync(path.join(workDir, "2.jpg"), JPEG);
    fs.writeFileSync(path.join(workDir, "9.png"), PNG);
    fs.writeFileSync(path.join(workDir, "notas.txt"), "hola");
    fs.writeFileSync(path.join(workDir, "revisar.html"), "<html></html>");
    const uploads = [];
    const fetchImpl = vi.fn(async (url, options = {}) => {
      if (url.endsWith("/auth/login")) return loginOk();
      if (options.method === "POST") {
        const file = options.body.get("imagen");
        uploads.push({ url, auth: options.headers.Authorization, name: file.name, type: file.type });
        return json({ producto: { imagen_url: "/uploads/productos/nueva.jpg" } });
      }
      const id = Number(url.split("/").pop());
      if (id === 9) return json({ error: "Producto no encontrado" }, 404);
      return json({ id_producto: id, imagen_url: id === 2 ? "/uploads/productos/real.jpg" : null });
    });

    const results = await uploadProductImages({
      apiBase: API, email: "admin@agroconecta.gt", password: "x", folder: workDir, fetchImpl,
    });

    const byFile = Object.fromEntries(results.map((result) => [result.fileName, result]));
    expect(byFile["1.jpg"]).toMatchObject({ status: "subida", idProducto: 1 });
    expect(byFile["2.jpg"]).toMatchObject({ status: "omitido", reason: "ya tiene foto (usa --reemplazar para cambiarla)" });
    expect(byFile["9.png"]).toMatchObject({ status: "omitido", reason: "el producto no existe o está inactivo" });
    expect(byFile["notas.txt"].status).toBe("omitido");
    expect(byFile["revisar.html"]).toBeUndefined();
    expect(uploads).toEqual([
      { url: `${API}/products/1/image`, auth: "Bearer tok", name: "1.jpg", type: "image/jpeg" },
    ]);

    uploads.length = 0;
    await uploadProductImages({
      apiBase: API, email: "a", password: "x", folder: workDir, fetchImpl, replace: true,
    });
    expect(uploads.map((upload) => upload.url)).toEqual([
      `${API}/products/1/image`,
      `${API}/products/2/image`,
    ]);
  });

  test("de punta a punta contra el backend real: la foto pasa por multer y queda guardada", async () => {
    const hash = await bcrypt.hash("admin123", 4);
    const updates = [];
    pool.query.mockImplementation(async (sql, params) => {
      if (sql.includes("FROM usuario WHERE email")) {
        return { rows: [{ id_usuario: 1, email: params[0], contrasena_hash: hash, tipo_usuario: "administrador", nombre: "Admin" }] };
      }
      if (sql.includes("FROM producto p") && sql.includes("JOIN categoria c")) {
        return { rows: [{ id_producto: params[0], nombre: "Urea", categoria: "Fertilizantes", imagen_url: null }] };
      }
      // Antes que las ofertas: esta consulta también menciona inventario_distribuidor.
      if (sql.includes("AS es_duenio")) {
        return { rows: [{ id_producto: params[0], es_duenio: false, verificado: false }] };
      }
      if (sql.includes("FROM inventario_distribuidor i")) return { rows: [] };
      if (sql.includes("SET imagen_url")) {
        updates.push(params);
        return { rowCount: 1, rows: [{ id_producto: params[1], imagen_url: params[0], imagen_anterior: null }] };
      }
      throw new Error(`Consulta no esperada: ${sql}`);
    });
    fs.writeFileSync(path.join(workDir, "7.png"), PNG);

    const server = app.listen(0);
    try {
      const { port } = server.address();
      const results = await uploadProductImages({
        apiBase: `http://127.0.0.1:${port}/api`,
        email: "admin@agroconecta.gt",
        password: "admin123",
        folder: workDir,
      });

      expect(results[0].reason).toBeUndefined();
      expect(results).toHaveLength(1);
      expect(results[0]).toMatchObject({ status: "subida", idProducto: 7 });
      expect(results[0].imagenUrl).toMatch(/^\/uploads\/productos\/[0-9a-f-]{36}\.png$/);
      const stored = path.join(uploadsDir, "productos", path.basename(results[0].imagenUrl));
      expect(fs.readFileSync(stored)).toEqual(PNG);
      expect(updates).toHaveLength(1);
    } finally {
      await new Promise((resolve) => server.close(resolve));
    }
  });
});
