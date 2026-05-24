const {pool} = require('../config/db');

const createMobilePayment = async (req, res) => {
  try {
    const { id_pedido, proveedor, numero_telefono, monto } = req.body;

    
    if (!id_pedido || !proveedor || !numero_telefono || !monto) {
      return res.status(400).json({
        error: "Campos requeridos: id_pedido, proveedor, numero_telefono, monto",
      });
    }

    const proveedoresValidos = ["tigo_money", "banrural_movil"];

    if (!proveedoresValidos.includes(proveedor)) {
      return res.status(400).json({ error: "Proveedor inválido" });
    }

    const pedido = await pool.query(
      "SELECT * FROM pedido WHERE id_pedido = $1",
      [id_pedido]
    );

    if (pedido.rows.length === 0) {
      return res.status(404).json({ error: "Pedido no encontrado" });
    }


    const result = await pool.query(
      `UPDATE pago
       SET metodo_pago = $2, monto = $3, estado_pago = 'pendiente', proveedor = $4
       WHERE id_pedido = $1
       RETURNING *`,
      [id_pedido, proveedor, monto, proveedor]
    );

    const pago = result.rows[0];

    setTimeout(async () => {
      try {
        await pool.query(
          `UPDATE pago SET estado_pago = 'processing' WHERE id_pago = $1`,
          [pago.id_pago]
        );

        setTimeout(async () => {
          const estadoFinal = Math.random() > 0.5 ? "completado" : "failed";

          await pool.query(
            `UPDATE pago
             SET estado_pago = $1,
                 referencia_transaccion = $2,
                 fecha_confirmacion = NOW()
             WHERE id_pago = $3`,
            [
              estadoFinal,
              `TX-${Date.now()}`,
              pago.id_pago,
            ]
          );

        }, 2000);

      } catch (err) {
        console.error("Error simulando procesamiento:", err);
      }
    }, 1000);

    return res.status(201).json({
      message: "Pago iniciado",
      pago,
    });

  } catch (error) {
    console.error("Error en createMobilePayment:", error);
    res.status(500).json({ error: "Error al procesar pago" });
  }
};

const paymentWebhook = async (req, res) => {
  try {
    const { id_pago, estado, referencia_transaccion } = req.body;

    if (!id_pago || !estado) {
      return res.status(400).json({
        error: "id_pago y estado son requeridos",
      });
    }

    const estadosValidos = ["completado", "failed"];

    if (!estadosValidos.includes(estado)) {
      return res.status(400).json({
        error: "Estado inválido",
      });
    }

    const result = await pool.query(
      `UPDATE pago
       SET estado_pago = $1,
           referencia_transaccion = $2,
           fecha_confirmacion = NOW()
       WHERE id_pago = $3
       RETURNING *`,
      [
        estado,
        referencia_transaccion || `TX-${Date.now()}`,
        id_pago,
      ]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        error: "Pago no encontrado",
      });
    }

    res.json({
      message: "Webhook procesado correctamente",
      pago: result.rows[0],
    });

  } catch (error) {
    console.error("Error en paymentWebhook:", error);
    res.status(500).json({
      error: "Error al procesar webhook",
    });
  }
};

module.exports = {
  createMobilePayment,
  paymentWebhook,
};