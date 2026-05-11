const { pool } = require("../config/db");

const createQualityReport = async (req, res) => {
    try {
        const {
            agricultor_id,
            producto_id,
            pedido_id,
            descripcion
        } = req.body;

        if (!agricultor_id || !producto_id || !pedido_id || !descripcion) {
            return res.status(400).json({
                error: "Debe enviar agricultor_id, producto_id, pedido_id y descripcion"
            });
        }

        const result = await pool.query(
            `INSERT INTO reporte_calidad
            (id_agricultor, id_producto, id_pedido, descripcion_problema)
            VALUES ($1, $2, $3, $4)
            RETURNING *`,
            [agricultor_id, producto_id, pedido_id, descripcion]
        );

        res.status(201).json({
            message: "Reporte de calidad creado correctamente",
            report: result.rows[0]
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({
            error: "Error al crear el reporte"
        });
    }
};

const getAllQualityReports = async (req, res) => {
    try {
        const { estado } = req.query;

        let query = `
            SELECT *
            FROM reporte_calidad
        `;
        const values = [];

        if (estado) {
            query += " WHERE estado_reporte = $1";
            values.push(estado);
        }

        query += " ORDER BY fecha_reporte DESC";

        const result = await pool.query(query, values);

        res.status(200).json(result.rows);

    } catch (error) {
        console.error(error);
        res.status(500).json({
            error: "Error al obtener reportes"
        });
    }
};

const updateQualityReport = async (req, res) => {
    try {
        const { id } = req.params;
        const { estado, accion_tomada } = req.body;

        if (!estado || !accion_tomada) {
            return res.status(400).json({
                error: "Debe enviar estado y accion_tomada"
            });
        }

        const result = await pool.query(
            `UPDATE reporte_calidad
            SET estado_reporte = $1,
                resolucion = $2,
                fecha_resolucion = CURRENT_TIMESTAMP
            WHERE id_reporte = $3
            RETURNING *`,
            [estado, accion_tomada, id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({
                error: "Reporte no encontrado"
            });
        }

        res.status(200).json({
            message: "Reporte actualizado correctamente",
            report: result.rows[0]
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({
            error: "Error al actualizar reporte"
        });
    }
};

module.exports = {
    createQualityReport,
    getAllQualityReports,
    updateQualityReport
};
