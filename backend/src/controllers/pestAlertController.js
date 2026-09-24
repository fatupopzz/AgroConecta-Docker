const { pool } = require("../config/db");
const { notifyNearbyInstallations } = require("../services/PestAlertPushService");

const EARTH_RADIUS_KM = 6371;
const DEFAULT_RADIUS_KM = 25;
const MAX_RADIUS_KM = 100;

const createPestAlert = async (req, res) => {
    try {
        const {
            tipo_plaga,
            cultivo_afectado,
            descripcion,
            latitud,
            longitud,
            departamento,
            municipio,
            severidad
        } = req.body;

        if (!tipo_plaga || !cultivo_afectado || latitud == null || longitud == null) {
            return res.status(400).json({
                error: "Debe enviar tipo_plaga, cultivo_afectado, latitud y longitud"
            });
        }

        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            return res.status(400).json({
                error: "Coordenadas inválidas"
            });
        }

        const id_usuario = req.user.id;

        const result = await pool.query(
            `INSERT INTO alerta_plaga
            (id_usuario, tipo_plaga, cultivo_afectado, descripcion,
             latitud, longitud, departamento, municipio, severidad)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING *`,
            [
                id_usuario,
                tipo_plaga.trim(),
                cultivo_afectado.trim(),
                descripcion || null,
                latitud,
                longitud,
                departamento || null,
                municipio || null,
                severidad || "media"
            ]
        );

        const createdAlert = result.rows[0];

        res.status(201).json({
            message: "Alerta de plaga creada correctamente",
            alerta: createdAlert
        });

        void notifyNearbyInstallations({
            alert: createdAlert,
            reporterUserId: id_usuario
        }).catch((pushError) => {
            console.error("Error al notificar alerta de plaga:", pushError);
        });

    } catch (error) {
        console.error("Error al crear alerta de plaga:", error);
        res.status(500).json({
            error: "Error al crear la alerta de plaga"
        });
    }
};

const registerPestAlertToken = async (req, res) => {
    try {
        const {
            token: rawToken,
            latitud: rawLatitude,
            longitud: rawLongitude
        } = req.body || {};
        const token = typeof rawToken === "string"
            ? rawToken.trim()
            : null;
        const latitude = Number(rawLatitude);
        const longitude = Number(rawLongitude);

        if (!token || token.length > 4096) {
            return res.status(400).json({
                error: "Debe enviar un token FCM válido"
            });
        }

        if (
            rawLatitude == null || rawLongitude == null ||
            !Number.isFinite(latitude) || latitude < -90 || latitude > 90 ||
            !Number.isFinite(longitude) || longitude < -180 || longitude > 180
        ) {
            return res.status(400).json({ error: "Coordenadas inválidas" });
        }

        await pool.query(
            `INSERT INTO instalacion_alerta_plaga
                (id_usuario, fcm_registration_token, latitud, longitud)
             VALUES ($1, $2, $3, $4)
             ON CONFLICT (fcm_registration_token)
             DO UPDATE SET
                id_usuario = EXCLUDED.id_usuario,
                latitud = EXCLUDED.latitud,
                longitud = EXCLUDED.longitud,
                fecha_actualizacion = NOW()`,
            [req.user.id, token, latitude, longitude]
        );

        return res.status(200).json({
            message: "Token FCM registrado correctamente"
        });
    } catch (error) {
        console.error("Error al registrar token FCM para alertas:", error);
        return res.status(500).json({
            error: "Error al registrar el token FCM"
        });
    }
};

const getNearbyAlerts = async (req, res) => {
    try {
        const { lat, lng, radio, cultivo } = req.query;

        if (lat == null || lng == null) {
            return res.status(400).json({
                error: "Debe enviar lat y lng como parámetros de consulta"
            });
        }

        const latitude = parseFloat(lat);
        const longitude = parseFloat(lng);

        if (isNaN(latitude) || isNaN(longitude)) {
            return res.status(400).json({
                error: "lat y lng deben ser valores numéricos"
            });
        }

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return res.status(400).json({
                error: "Coordenadas inválidas"
            });
        }

        let radiusKm = parseFloat(radio) || DEFAULT_RADIUS_KM;
        if (radiusKm > MAX_RADIUS_KM) radiusKm = MAX_RADIUS_KM;

        const values = [latitude, longitude, radiusKm];
        let cultivoFilter = "";

        if (cultivo) {
            cultivoFilter = "AND LOWER(a.cultivo_afectado) = LOWER($4)";
            values.push(cultivo);
        }

        const result = await pool.query(
            `SELECT a.*,
                u.nombre AS nombre_usuario,
                (${EARTH_RADIUS_KM} * acos(
                    cos(radians($1)) * cos(radians(a.latitud))
                    * cos(radians(a.longitud) - radians($2))
                    + sin(radians($1)) * sin(radians(a.latitud))
                )) AS distancia_km
            FROM alerta_plaga a
            JOIN usuario u ON u.id_usuario = a.id_usuario
            WHERE a.activa = TRUE
              AND a.fecha_expiracion > NOW()
              AND (${EARTH_RADIUS_KM} * acos(
                    cos(radians($1)) * cos(radians(a.latitud))
                    * cos(radians(a.longitud) - radians($2))
                    + sin(radians($1)) * sin(radians(a.latitud))
                )) <= $3
              ${cultivoFilter}
            ORDER BY distancia_km ASC`,
            values
        );

        res.status(200).json({
            total: result.rows.length,
            radio_km: radiusKm,
            alertas: result.rows
        });

    } catch (error) {
        console.error("Error al obtener alertas cercanas:", error);
        res.status(500).json({
            error: "Error al obtener alertas cercanas"
        });
    }
};

const getPestAlertById = async (req, res) => {
    const alertId = Number(req.params.id);
    if (!Number.isInteger(alertId) || alertId <= 0) {
        return res.status(400).json({ error: "El id de la alerta no es válido" });
    }

    try {
        const result = await pool.query(
            `SELECT a.*, u.nombre AS nombre_usuario
            FROM alerta_plaga a
            JOIN usuario u ON u.id_usuario = a.id_usuario
            WHERE a.id_alerta = $1`,
            [alertId]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: "Alerta no encontrada" });
        }

        return res.status(200).json(result.rows[0]);
    } catch (error) {
        console.error("Error al obtener alerta de plaga:", error);
        return res.status(500).json({ error: "Error al obtener la alerta" });
    }
};

const getSuggestedProducts = async (req, res) => {
    try {
        const { id } = req.params;

        const alertResult = await pool.query(
            "SELECT tipo_plaga, cultivo_afectado FROM alerta_plaga WHERE id_alerta = $1",
            [id]
        );

        if (alertResult.rows.length === 0) {
            return res.status(404).json({
                error: "Alerta no encontrada"
            });
        }

        const { tipo_plaga, cultivo_afectado } = alertResult.rows[0];

        const result = await pool.query(
            `SELECT p.id_producto, p.nombre, p.marca, p.descripcion,
                    p.composicion, p.dosis_recomendada,
                    c.nombre AS categoria
            FROM producto p
            LEFT JOIN categoria c ON c.id_categoria = p.id_categoria
            WHERE p.activo = TRUE
              AND (
                LOWER(p.nombre) LIKE '%' || LOWER($1) || '%'
                OR LOWER(p.descripcion) LIKE '%' || LOWER($1) || '%'
                OR LOWER(p.nombre) LIKE '%' || LOWER($2) || '%'
                OR LOWER(p.descripcion) LIKE '%' || LOWER($2) || '%'
                OR LOWER(c.nombre) IN ('pesticidas', 'herbicidas', 'fungicidas')
              )
            ORDER BY
              CASE
                WHEN LOWER(p.nombre) LIKE '%' || LOWER($1) || '%' THEN 0
                WHEN LOWER(p.descripcion) LIKE '%' || LOWER($1) || '%' THEN 1
                ELSE 2
              END,
              p.calificacion_promedio DESC
            LIMIT 10`,
            [tipo_plaga, cultivo_afectado]
        );

        res.status(200).json({
            alerta_id: Number(id),
            tipo_plaga,
            cultivo_afectado,
            productos_sugeridos: result.rows
        });

    } catch (error) {
        console.error("Error al obtener productos sugeridos:", error);
        res.status(500).json({
            error: "Error al obtener productos sugeridos"
        });
    }
};

const getMyAlerts = async (req, res) => {
    try {
        const id_usuario = req.user.id;

        const result = await pool.query(
            `SELECT * FROM alerta_plaga
            WHERE id_usuario = $1
            ORDER BY fecha_reporte DESC`,
            [id_usuario]
        );

        res.status(200).json(result.rows);

    } catch (error) {
        console.error("Error al obtener mis alertas:", error);
        res.status(500).json({
            error: "Error al obtener alertas del usuario"
        });
    }
};

module.exports = {
    createPestAlert,
    getNearbyAlerts,
    getPestAlertById,
    getSuggestedProducts,
    getMyAlerts,
    registerPestAlertToken
};
