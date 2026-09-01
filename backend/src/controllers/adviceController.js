const { pool } = require("../config/db");

const MAX_MESSAGE_LENGTH = 1000;

const getAdviceMessages = async (req, res) => {
  try {
    const orderId = Number(req.params.id);
    const result = await pool.query(
      `SELECT
          ma.id_mensaje,
          ma.id_pedido,
          ma.id_usuario_remitente,
          u.nombre AS remitente_nombre,
          u.tipo_usuario AS remitente_tipo,
          ma.mensaje,
          ma.fecha_envio
       FROM mensaje_asesoria ma
       JOIN usuario u ON u.id_usuario = ma.id_usuario_remitente
       WHERE ma.id_pedido = $1
       ORDER BY ma.fecha_envio ASC, ma.id_mensaje ASC`,
      [orderId]
    );

    return res.json({ id_pedido: orderId, mensajes: result.rows });
  } catch (error) {
    console.error("Error al obtener mensajes de asesoría:", error);
    return res.status(500).json({ error: "Error al obtener los mensajes de asesoría" });
  }
};

const sendAdviceMessage = async (req, res) => {
  const rawMessage = req.body?.mensaje;
  const message = typeof rawMessage === "string" ? rawMessage.trim() : "";

  if (message.length < 1 || message.length > MAX_MESSAGE_LENGTH) {
    return res.status(400).json({
      error: `El mensaje debe contener entre 1 y ${MAX_MESSAGE_LENGTH} caracteres`,
    });
  }

  try {
    const result = await pool.query(
      `WITH inserted AS (
         INSERT INTO mensaje_asesoria
           (id_pedido, id_usuario_remitente, mensaje)
         VALUES ($1, $2, $3)
         RETURNING *
       )
       SELECT
          inserted.id_mensaje,
          inserted.id_pedido,
          inserted.id_usuario_remitente,
          u.nombre AS remitente_nombre,
          u.tipo_usuario AS remitente_tipo,
          inserted.mensaje,
          inserted.fecha_envio
       FROM inserted
       JOIN usuario u ON u.id_usuario = inserted.id_usuario_remitente`,
      [Number(req.params.id), Number(req.user.id), message]
    );

    return res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error("Error al enviar mensaje de asesoría:", error);
    return res.status(500).json({ error: "Error al enviar el mensaje de asesoría" });
  }
};

module.exports = { getAdviceMessages, sendAdviceMessage };
