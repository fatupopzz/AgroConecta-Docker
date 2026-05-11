const pool = require("../config/db");

const createQualityReport = async (req, res) => {
    try {
        const {
            agricultor_id,
            producto_id,
            pedido_id,
            descripcion
        } = req.body;

        const result = await pool.query(
            `INSERT INTO reporte_calidad
            (agricultor_id, producto_id, pedido_id, descripcion)
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

        let query = "SELECT * FROM reporte_calidad";
        let values = [];

        if (estado) {
            query += " WHERE estado = $1";
            values.push(estado);
        }

        query += " ORDER BY fecha_creacion DESC";

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

        const result = await pool.query(
            `UPDATE reporte_calidad
            SET estado = $1,
                accion_tomada = $2,
                fecha_resolucion = CURRENT_TIMESTAMP
            WHERE id = $3
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
