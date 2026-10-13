/**
 * Sube una carpeta de fotos <id_producto>.jpg|png|webp a través de la API, como
 * lo haría la app: POST /api/products/:id/image con el token de un usuario.
 *
 * A diferencia de seed_product_images.js no necesita entrar a la VM: corre en
 * cualquier computadora y deja que el backend aplique sus propias reglas
 * (formato real, 5 MB, borrar la foto anterior, invalidar la caché).
 *
 * Por defecto no toca productos que ya tienen foto, para no pisar la foto real
 * que un distribuidor haya subido desde "Mis productos".
 *
 * Uso:
 *   AGRO_EMAIL=admin@agroconecta.gt AGRO_PASSWORD=xxxx \
 *     node scripts/upload_product_images.js <carpeta> [--api URL] [--reemplazar]
 */
const fs = require("node:fs/promises");
const path = require("node:path");
const { DEFAULT_API, parseArgs } = require("./fetch_stock_images");

const FILE_NAME_PATTERN = /^([1-9]\d*)\.(jpe?g|png|webp)$/i;
const MIME_BY_EXTENSION = { jpg: "image/jpeg", jpeg: "image/jpeg", png: "image/png", webp: "image/webp" };

const readError = async (response) => {
  const body = await response.json().catch(() => null);
  return body?.error ?? `HTTP ${response.status}`;
};

const login = async ({ apiBase, email, password, fetchImpl }) => {
  const response = await fetchImpl(`${apiBase}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) {
    throw new Error(`No se pudo iniciar sesión: ${await readError(response)}`);
  }
  const body = await response.json();
  // Un agricultor podría iniciar sesión, pero el backend le rechazaría cada
  // foto con 403; mejor avisarlo una vez que llenar la salida de errores.
  if (body.tipoUsuario !== "administrador" && body.tipoUsuario !== "distribuidor") {
    throw new Error("La cuenta debe ser de administrador o distribuidor");
  }
  return { token: body.token, tipoUsuario: body.tipoUsuario };
};

const uploadOne = async ({ apiBase, token, folder, fileName, replace, fetchImpl }) => {
  const match = FILE_NAME_PATTERN.exec(fileName);
  if (!match) {
    return { status: "omitido", fileName, reason: "el nombre no es <id_producto>.jpg|png|webp" };
  }
  const idProducto = Number(match[1]);

  const current = await fetchImpl(`${apiBase}/products/${idProducto}`);
  if (current.status === 404) {
    return { status: "omitido", fileName, idProducto, reason: "el producto no existe o está inactivo" };
  }
  if (!current.ok) {
    return { status: "error", fileName, idProducto, reason: await readError(current) };
  }
  const { imagen_url: existing } = await current.json();
  if (existing && !replace) {
    return { status: "omitido", fileName, idProducto, reason: "ya tiene foto (usa --reemplazar para cambiarla)" };
  }

  const bytes = await fs.readFile(path.join(folder, fileName));
  const form = new FormData();
  form.append(
    "imagen",
    new Blob([bytes], { type: MIME_BY_EXTENSION[match[2].toLowerCase()] }),
    fileName,
  );

  const response = await fetchImpl(`${apiBase}/products/${idProducto}/image`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  if (!response.ok) {
    return { status: "error", fileName, idProducto, reason: await readError(response) };
  }
  const body = await response.json();
  return { status: "subida", fileName, idProducto, imagenUrl: body.producto?.imagen_url };
};

const uploadProductImages = async ({
  apiBase = DEFAULT_API,
  email,
  password,
  folder,
  replace = false,
  fetchImpl = fetch,
}) => {
  if (!email || !password) throw new Error("Faltan AGRO_EMAIL y AGRO_PASSWORD");

  const { token } = await login({ apiBase, email, password, fetchImpl });
  const entries = await fs.readdir(folder, { withFileTypes: true });
  const fileNames = entries
    .filter((entry) => entry.isFile() && !entry.name.startsWith("."))
    .map((entry) => entry.name)
    // revisar.html y creditos.csv son del paso anterior, no fotos.
    .filter((name) => !/\.(html|csv)$/i.test(name))
    .sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));

  const results = [];
  for (const fileName of fileNames) {
    results.push(await uploadOne({ apiBase, token, folder, fileName, replace, fetchImpl }));
  }
  return results;
};

const main = async () => {
  const { positional, api: apiBase, flags } = parseArgs(process.argv.slice(2));
  const [folder] = positional;
  if (!folder) {
    console.error(
      "Uso: AGRO_EMAIL=... AGRO_PASSWORD=... node scripts/upload_product_images.js <carpeta> [--api URL] [--reemplazar]",
    );
    process.exitCode = 2;
    return;
  }

  const results = await uploadProductImages({
    apiBase,
    email: process.env.AGRO_EMAIL,
    password: process.env.AGRO_PASSWORD,
    folder,
    replace: flags.has("--reemplazar"),
  });

  for (const result of results) {
    const id = result.idProducto ? ` (producto ${result.idProducto})` : "";
    const detail = result.status === "subida" ? result.imagenUrl : result.reason;
    console.log(`[${result.status}] ${result.fileName}${id}: ${detail}`);
  }
  const count = (status) => results.filter((result) => result.status === status).length;
  console.log(`\n${count("subida")} subidas, ${count("omitido")} omitidas, ${count("error")} con error.`);
  if (count("error") > 0) process.exitCode = 1;
};

if (require.main === module) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}

module.exports = { uploadProductImages };
