/**
 * Busca en Pexels una foto para cada producto que todavía no tiene y la deja en
 * una carpeta local como <id_producto>.jpg, junto con revisar.html para verlas
 * todas antes de subirlas.
 *
 * No escribe nada en la base ni en el servidor: solo lee el catálogo público.
 * La subida va aparte (upload_product_images.js) a propósito, para que una foto
 * rara que trajo la búsqueda se pueda borrar antes de que la vea un agricultor.
 *
 * Uso (desde cualquier computadora con Node 18+, no hace falta la VM):
 *   PEXELS_API_KEY=xxxx node scripts/fetch_stock_images.js <carpeta> [--api URL] [--todos]
 */
const fs = require("node:fs/promises");
const path = require("node:path");

const DEFAULT_API = "https://agroconecta.canadacentral.cloudapp.azure.com/api";
const PEXELS_SEARCH_URL = "https://api.pexels.com/v1/search";

// Pexels entiende español con locale=es-ES, pero un nombre comercial solo
// ("Urea 46%") trae fotos al azar. La pista de categoría ancla la búsqueda al
// campo; las claves van sin tildes porque así se comparan.
const CATEGORY_HINTS = {
  fertilizantes: "fertilizante agricultura",
  semillas: "semillas siembra",
  pesticidas: "fumigacion cultivo",
  insecticidas: "fumigacion cultivo",
  fungicidas: "fumigacion cultivo",
  herbicidas: "herbicida maleza campo",
  herramientas: "herramienta agricola",
};
const DEFAULT_HINT = "agricultura cultivo";

// Códigos de formulación que aparecen en los nombres de agroquímicos y que a
// Pexels solo le meten ruido.
const FORMULATION_CODES = new Set(["sl", "ec", "wp", "wg", "sc", "ew", "gr", "sp", "cs", "od"]);

const stripAccents = (text) => text.normalize("NFD").replace(/[̀-ͯ]/g, "");

const cleanProductName = (nombre) =>
  String(nombre || "")
    .toLowerCase()
    .replace(/\([^)]*\)/g, " ")
    .split(/\s+/)
    .filter((word) => word && !/\d/.test(word) && !FORMULATION_CODES.has(word))
    .join(" ")
    .trim();

const categoryHint = (categoria) =>
  CATEGORY_HINTS[stripAccents(String(categoria || "").toLowerCase()).trim()] ?? DEFAULT_HINT;

/** Consultas de la más específica a la más general, sin repetidas. */
const buildQueries = ({ nombre, categoria }) => {
  const name = cleanProductName(nombre);
  const hint = categoryHint(categoria);
  return [...new Set([name && `${name} ${hint}`, name, hint].filter(Boolean))];
};

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const getJson = async (fetchImpl, url, options) => {
  const response = await fetchImpl(url, options);
  const body = await response.json().catch(() => null);
  return { response, body };
};

/**
 * GET /api/products ordena al azar, así que paginarlo puede saltarse productos.
 * Se usa solo para saber cuántos hay y luego se pide cada id; los huecos (ids de
 * productos desactivados) se saltan hasta encontrarlos todos.
 */
const discoverProducts = async ({ apiBase, fetchImpl, maxMisses = 50 }) => {
  const { response: listResponse, body: list } = await getJson(
    fetchImpl,
    `${apiBase}/products?limit=1`,
  );
  if (!listResponse.ok || typeof list?.total !== "number") {
    throw new Error(`No se pudo leer el catálogo (${listResponse.status})`);
  }

  const products = [];
  let misses = 0;
  for (let id = 1; products.length < list.total && misses < maxMisses; id += 1) {
    const { response, body } = await getJson(fetchImpl, `${apiBase}/products/${id}`);
    if (response.ok && body) {
      products.push({
        id_producto: body.id_producto,
        nombre: body.nombre,
        marca: body.marca ?? null,
        categoria: body.categoria ?? null,
        imagen_url: body.imagen_url ?? null,
      });
      misses = 0;
    } else {
      misses += 1;
    }
  }
  return { products, expected: list.total };
};

const searchPexels = async ({ query, apiKey, fetchImpl }) => {
  const params = new URLSearchParams({
    query,
    per_page: "15",
    locale: "es-ES",
    orientation: "landscape",
  });
  const { response, body } = await getJson(fetchImpl, `${PEXELS_SEARCH_URL}?${params}`, {
    headers: { Authorization: apiKey },
  });
  if (response.status === 401 || response.status === 403) {
    throw new Error("Pexels rechazó la API key (revisa PEXELS_API_KEY)");
  }
  if (response.status === 429) {
    throw new Error("Pexels limitó las búsquedas por hora; espera un rato y vuelve a correrlo");
  }
  if (!response.ok) {
    throw new Error(`Pexels respondió ${response.status}`);
  }
  return body?.photos ?? [];
};

const escapeHtml = (text) =>
  String(text ?? "").replace(/[&<>"']/g, (char) => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char]
  ));

const csvCell = (text) => `"${String(text ?? "").replace(/"/g, '""')}"`;

const writeReview = async (outDir, rows) => {
  const found = rows.filter((row) => row.status === "descargada");
  const cards = found
    .map((row) => `
  <figure>
    <img src="${escapeHtml(row.archivo)}" alt="">
    <figcaption><b>${escapeHtml(row.archivo)}</b> · ${escapeHtml(row.nombre)}<br>
      <small>${escapeHtml(row.categoria)} · búsqueda: “${escapeHtml(row.query)}” ·
      <a href="${escapeHtml(row.pexels_url)}">${escapeHtml(row.fotografo)}</a></small></figcaption>
  </figure>`)
    .join("");
  const missing = rows.filter((row) => row.status !== "descargada");
  const missingList = missing.length
    ? `<h2>Sin foto (${missing.length})</h2><ul>${missing
      .map((row) => `<li>${row.id_producto} · ${escapeHtml(row.nombre)}: ${escapeHtml(row.motivo)}</li>`)
      .join("")}</ul>`
    : "";

  const html = `<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>Revisar fotos</title>
<style>
  body { font-family: system-ui, sans-serif; margin: 24px; background: #f5f5f5; }
  main { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
  figure { margin: 0; background: #fff; border-radius: 10px; overflow: hidden; }
  img { width: 100%; height: 170px; object-fit: cover; display: block; }
  figcaption { padding: 8px 10px; font-size: 14px; }
  small { color: #666; }
</style></head><body>
<h1>Fotos encontradas (${found.length})</h1>
<p>Borra de esta carpeta los archivos que no quieras subir y luego corre upload_product_images.js.</p>
<main>${cards}</main>
${missingList}
</body></html>
`;
  await fs.writeFile(path.join(outDir, "revisar.html"), html);

  const csv = [
    "id_producto,nombre,archivo,busqueda,fotografo,pexels_url",
    ...found.map((row) => [
      row.id_producto, row.nombre, row.archivo, row.query, row.fotografo, row.pexels_url,
    ].map(csvCell).join(",")),
  ].join("\n");
  // Pexels no exige atribución, pero la agradece; queda registrado de dónde
  // salió cada foto por si alguien pregunta.
  await fs.writeFile(path.join(outDir, "creditos.csv"), `${csv}\n`);
};

const fetchStockImages = async ({
  apiBase = DEFAULT_API,
  apiKey,
  outDir,
  includeWithPhoto = false,
  fetchImpl = fetch,
  pauseMs = 250,
  log = console.log,
}) => {
  if (!apiKey) throw new Error("Falta PEXELS_API_KEY");
  await fs.mkdir(outDir, { recursive: true });

  const { products, expected } = await discoverProducts({ apiBase, fetchImpl });
  if (products.length < expected) {
    log(`Aviso: se encontraron ${products.length} de ${expected} productos activos.`);
  }

  const pending = products.filter((product) => includeWithPhoto || !product.imagen_url);
  log(`${pending.length} productos sin foto de ${products.length}.`);

  // La misma foto para dos productos se notaría en el catálogo.
  const usedPhotoIds = new Set();
  const rows = [];

  for (const product of pending) {
    let chosen = null;
    let usedQuery = null;
    for (const query of buildQueries(product)) {
      const photos = await searchPexels({ query, apiKey, fetchImpl });
      await sleep(pauseMs);
      chosen = photos.find((photo) => !usedPhotoIds.has(photo.id) && photo.src?.large) ?? null;
      if (chosen) {
        usedQuery = query;
        break;
      }
    }

    const base = { id_producto: product.id_producto, nombre: product.nombre, categoria: product.categoria };
    if (!chosen) {
      rows.push({ ...base, status: "sin_resultado", motivo: "Pexels no devolvió fotos" });
      log(`[sin foto] ${product.id_producto} ${product.nombre}`);
      continue;
    }

    const imageResponse = await fetchImpl(chosen.src.large);
    if (!imageResponse.ok) {
      rows.push({ ...base, status: "error", motivo: `descarga falló (${imageResponse.status})` });
      log(`[error] ${product.id_producto} ${product.nombre}: descarga ${imageResponse.status}`);
      continue;
    }

    const archivo = `${product.id_producto}.jpg`;
    await fs.writeFile(path.join(outDir, archivo), Buffer.from(await imageResponse.arrayBuffer()));
    usedPhotoIds.add(chosen.id);
    rows.push({
      ...base,
      status: "descargada",
      archivo,
      query: usedQuery,
      fotografo: chosen.photographer,
      pexels_url: chosen.url,
    });
    log(`[ok] ${archivo} ← “${usedQuery}” (${chosen.photographer})`);
  }

  await writeReview(outDir, rows);
  return { rows, total: products.length };
};

const parseArgs = (argv) => {
  const options = { positional: [], api: DEFAULT_API, flags: new Set() };
  for (let i = 0; i < argv.length; i += 1) {
    if (argv[i] === "--api") {
      options.api = argv[i + 1] ?? DEFAULT_API;
      i += 1;
    } else if (argv[i].startsWith("--")) {
      options.flags.add(argv[i]);
    } else {
      options.positional.push(argv[i]);
    }
  }
  options.api = options.api.replace(/\/+$/, "");
  return options;
};

const main = async () => {
  const { positional, api: apiBase, flags } = parseArgs(process.argv.slice(2));
  const [outDir] = positional;

  if (!outDir) {
    console.error("Uso: PEXELS_API_KEY=xxxx node scripts/fetch_stock_images.js <carpeta> [--api URL] [--todos]");
    process.exitCode = 2;
    return;
  }

  const { rows } = await fetchStockImages({
    apiBase,
    apiKey: process.env.PEXELS_API_KEY,
    outDir,
    includeWithPhoto: flags.has("--todos"),
  });
  const ok = rows.filter((row) => row.status === "descargada").length;
  console.log(`\n${ok} fotos descargadas, ${rows.length - ok} sin foto.`);
  console.log(`Revísalas abriendo ${path.join(outDir, "revisar.html")}`);
};

if (require.main === module) {
  main().catch((error) => {
    console.error(`No se pudieron buscar las fotos: ${error.message}`);
    process.exitCode = 1;
  });
}

module.exports = {
  fetchStockImages,
  buildQueries,
  cleanProductName,
  discoverProducts,
  parseArgs,
  DEFAULT_API,
};
