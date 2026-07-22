const { pool } = require("../config/db");

const getNotifications = async (req, res) => {
    const idUsuario = req.user?.id;
    const tipoUsuario = req.user?.tipo;

    if (tipoUsuario !== "distribuidor") {
        return res.status(403).json({
            error: "Solo distribuidores pueden ver sus notificaciones"
        });
    }

    try {

        const distributorResult = await pool.query(
            `SELECT id_distribuidor
             FROM distribuidor
             WHERE id_usuario = $1`,
            [Number(idUsuario)]
        );

        if (distributorResult.rows.length === 0) {
            return res.status(404).json({
                error: "Distribuidor no encontrado"
            });
        }

        const { id_distribuidor } = distributorResult.rows[0];

        const result = await pool.query(
            `SELECT
                id_notificacion,
                tipo,
                contenido,
                id_pedido,
                leida,
                fecha
             FROM notificacion
             WHERE id_distribuidor = $1
             ORDER BY fecha DESC`,
            [id_distribuidor]
        );

        return res.json(result.rows);

    } catch (error) {

        console.error("Error en getNotifications:", error);

        return res.status(500).json({
            error: "Error al obtener notificaciones"
        });

    }
};

const markNotificationAsRead = async (req, res) => {

    const { id } = req.params;

    try {

        const result = await pool.query(
            `UPDATE notificacion
             SET leida = TRUE
             WHERE id_notificacion = $1
             RETURNING *`,
            [Number(id)]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({
                error: "Notificación no encontrada"
            });
        }

        return res.json({
            message: "Notificación marcada como leída"
        });

    } catch (error) {

        console.error("Error en markNotificationAsRead:", error);

        return res.status(500).json({
            error: "Error al actualizar notificación"
        });

    }

};

module.exports = {
    getNotifications,
    markNotificationAsRead
};