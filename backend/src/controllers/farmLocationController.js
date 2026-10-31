const { pool } = require("../config/db");
const { parseOptionalCoordinates } = require("../utils/coordinates");

// La ubicación de la finca vive en direccion_entrega como dirección
// predeterminada. Es la misma fila que ya consulta el clima (weatherController)
// y la que llena la dirección de entrega del checkout, así que guardarla una
// vez sirve para todo eso.
const FARM_ALIAS = "Mi finca";
const MAX_PLACE_LENGTH = 100;
const MAX_REFERENCE_LENGTH = 255;

const toResponse = (row) => ({
  departamento: row.departamento,
  municipio: row.municipio,
  referencia: row.descripcion,
  latitud: row.latitud === null ? null : Number(row.latitud),
  longitud: row.longitud === null ? null : Number(row.longitud),
});

const cleanText = (value) => (typeof value === "string" ? value.trim() : "");

async function farmerIdFor(client, userId) {
  const result = await client.query("SELECT id_agricultor FROM agricultor WHERE id_usuario = $1", [userId]);
  return result.rows[0]?.id_agricultor ?? null;
}

const getFarmLocation = async (req, res) => {
  const farmerId = await farmerIdFor(pool, req.user.id);
  if (farmerId === null) {
    return res.status(403).json({ error: "Solo los agricultores tienen ubicación de finca" });
  }

  const result = await pool.query(
    `SELECT departamento, municipio, descripcion, latitud, longitud
     FROM direccion_entrega
     WHERE id_agricultor = $1 AND es_predeterminada = TRUE
     ORDER BY id_direccion
     LIMIT 1`,
    [farmerId]
  );

  if (result.rows.length === 0) {
    return res.status(404).json({ error: "Todavía no guardaste la ubicación de tu finca" });
  }
  return res.json(toResponse(result.rows[0]));
};

const saveFarmLocation = async (req, res) => {
  const departamento = cleanText(req.body?.departamento);
  const municipio = cleanText(req.body?.municipio);
  const referencia = cleanText(req.body?.referencia);

  if (!departamento || !municipio) {
    return res.status(400).json({ error: "Departamento y municipio son obligatorios" });
  }
  if (departamento.length > MAX_PLACE_LENGTH || municipio.length > MAX_PLACE_LENGTH) {
    return res.status(400).json({ error: "Departamento o municipio demasiado largo" });
  }
  if (referencia.length > MAX_REFERENCE_LENGTH) {
    return res.status(400).json({ error: `La referencia admite hasta ${MAX_REFERENCE_LENGTH} caracteres` });
  }

  // Las coordenadas son opcionales: quien no da permiso de GPS igual puede
  // guardar su municipio y la referencia para la entrega.
  const coordinates = parseOptionalCoordinates(req.body?.latitud, req.body?.longitud);
  if (coordinates.error) {
    return res.status(400).json({ error: coordinates.error });
  }

  const client = await pool.connect();
  try {
    const farmerId = await farmerIdFor(client, req.user.id);
    if (farmerId === null) {
      return res.status(403).json({ error: "Solo los agricultores tienen ubicación de finca" });
    }

    await client.query("BEGIN");
    const values = [
      farmerId,
      departamento,
      municipio,
      referencia || null,
      coordinates.latitude,
      coordinates.longitude,
    ];
    const updated = await client.query(
      `UPDATE direccion_entrega
       SET departamento = $2, municipio = $3, descripcion = $4, latitud = $5, longitud = $6
       WHERE id_agricultor = $1 AND es_predeterminada = TRUE
       RETURNING departamento, municipio, descripcion, latitud, longitud`,
      values
    );
    const saved = updated.rows[0] ?? (
      await client.query(
        `INSERT INTO direccion_entrega
           (id_agricultor, alias, departamento, municipio, descripcion, latitud, longitud, es_predeterminada)
         VALUES ($1, $7, $2, $3, $4, $5, $6, TRUE)
         RETURNING departamento, municipio, descripcion, latitud, longitud`,
        [...values, FARM_ALIAS]
      )
    ).rows[0];

    // El perfil muestra departamento y municipio del agricultor; se mantienen
    // iguales a los de la finca para no tener dos datos distintos.
    await client.query(
      "UPDATE agricultor SET departamento = $2, municipio = $3 WHERE id_agricultor = $1",
      [farmerId, departamento, municipio]
    );
    await client.query("COMMIT");

    return res.json(toResponse(saved));
  } catch (error) {
    await client.query("ROLLBACK").catch(() => {});
    throw error;
  } finally {
    client.release();
  }
};

module.exports = { getFarmLocation, saveFarmLocation };
