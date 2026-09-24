const jwt = require("jsonwebtoken");
const { pool } = require("../config/db");

const RADIUS_KM = 25;
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
let cachedToken;

const firebaseConfig = () => {
  const encoded = process.env.FIREBASE_SERVICE_ACCOUNT_BASE64?.trim();
  if (!encoded) return null;
  const account = JSON.parse(Buffer.from(encoded, "base64").toString("utf8"));
  const projectId = process.env.FIREBASE_PROJECT_ID?.trim() || account.project_id;
  if (!projectId || !account.client_email || !account.private_key) {
    throw new Error("Configuración de Firebase incompleta");
  }
  return { account, projectId };
};

const getAccessToken = async ({ account }) => {
  if (cachedToken?.expiresAt > Date.now() + 60_000) return cachedToken.value;
  const tokenUrl = account.token_uri || TOKEN_URL;
  const assertion = jwt.sign({ scope: FCM_SCOPE }, account.private_key, {
    algorithm: "RS256",
    issuer: account.client_email,
    audience: tokenUrl,
    expiresIn: "1h",
  });
  const response = await fetch(tokenUrl, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error(`Firebase OAuth respondió ${response.status}`);
  const body = await response.json();
  cachedToken = {
    value: body.access_token,
    expiresAt: Date.now() + Number(body.expires_in || 3600) * 1000,
  };
  return cachedToken.value;
};

const sendMessage = (config, accessToken, message) => fetch(
  `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(config.projectId)}/messages:send`,
  {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ message }),
  },
);

const notifyNearbyInstallations = async (
  { alert, reporterUserId },
  {
    database = pool,
    config = firebaseConfig(),
    tokenProvider = getAccessToken,
    sender = sendMessage,
  } = {},
) => {
  if (!config) return { sent: 0, failed: 0, skipped: "firebase_not_configured" };
  const result = await database.query(
    `SELECT fcm_registration_token
     FROM instalacion_alerta_plaga
     WHERE id_usuario <> $3 AND fecha_actualizacion > NOW() - INTERVAL '90 days'
       AND (6371 * acos(LEAST(1, GREATEST(-1,
         cos(radians($1)) * cos(radians(latitud))
         * cos(radians(longitud) - radians($2))
         + sin(radians($1)) * sin(radians(latitud))
       )))) <= $4`,
    [Number(alert.latitud), Number(alert.longitud), reporterUserId, RADIUS_KM],
  );
  if (result.rows.length === 0) return { sent: 0, failed: 0 };

  const accessToken = await tokenProvider(config);
  const data = {
    type: "pest_alert",
    alert_id: String(alert.id_alerta),
    pest_type: String(alert.tipo_plaga),
    crop: String(alert.cultivo_afectado),
  };
  const deliveries = await Promise.all(result.rows.map(async (row) => {
    try {
      const response = await sender(config, accessToken, {
        token: row.fcm_registration_token,
        data,
        android: { priority: "high" },
      });
      return response.ok;
    } catch (_) {
      return false;
    }
  }));
  const sent = deliveries.filter(Boolean).length;
  return { sent, failed: deliveries.length - sent };
};

module.exports = { notifyNearbyInstallations };
