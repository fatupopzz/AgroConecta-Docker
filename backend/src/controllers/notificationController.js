const { pool } = require("../config/db");

const getNotifications = async (req, res) => {
    const idUsuario = req.user?.id;
    const tipoUsuario = req.user?.tipo;

    if (!["distribuidor", "agricultor"].includes(tipoUsuario)) {
        return res.status(403).json({
            error: "Tipo de usuario sin acceso a notificaciones"
        });
    }

    try {

        const profileTable = tipoUsuario === "distribuidor" ? "distribuidor" : "agricultor";
        const profileId = tipoUsuario === "distribuidor" ? "id_distribuidor" : "id_agricultor";
        const profileResult = await pool.query(
            `SELECT ${profileId}
             FROM ${profileTable}
             WHERE id_usuario = $1`,
            [Number(idUsuario)]
        );

        if (profileResult.rows.length === 0) {
            return res.status(404).json({
                error: "Perfil de usuario no encontrado"
            });
        }

        const ownerId = profileResult.rows[0][profileId];

        const result = await pool.query(
            `SELECT
                id_notificacion,
                tipo,
                contenido,
                id_pedido,
                leida,
                fecha
             FROM notificacion
             WHERE ${profileId} = $1
             ORDER BY fecha DESC`,
            [ownerId]
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
    const idUsuario = req.user?.id;
    const tipoUsuario = req.user?.tipo;

    if (!["distribuidor", "agricultor"].includes(tipoUsuario)) {
        return res.status(403).json({ error: "Tipo de usuario sin acceso a notificaciones" });
    }

    try {

        const profileTable = tipoUsuario === "distribuidor" ? "distribuidor" : "agricultor";
        const profileId = tipoUsuario === "distribuidor" ? "id_distribuidor" : "id_agricultor";
        const result = await pool.query(
            `UPDATE notificacion n
             SET leida = TRUE
             FROM ${profileTable} p
             WHERE n.id_notificacion = $1
               AND p.id_usuario = $2
               AND n.${profileId} = p.${profileId}
             RETURNING n.*`,
            [Number(id), Number(idUsuario)]
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
