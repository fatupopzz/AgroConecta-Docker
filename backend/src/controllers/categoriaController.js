const pool = require("../db/connection");

const createCategory = async (req, res ) => {
    try {
        const {nombre, descripcion} = req.body;

        if (!nombre || !descripcion) {
            return res.status(400).json({ message: "Datos incompletos" });
        }

        //verficar si ya existe
        const categoriaExist = await pool.query(
        "SELECT * FROM categoria WHERE nombre = $1",
        [nombre]
        );

        if (categoriaExist.rows.length > 0) {
        return res.status(400).json({ message: "La categoría ya existe" });
        }

        // Crear la nueva categoría
        const newCategory = await pool.query(
        "INSERT INTO categoria (nombre, descripcion) VALUES ($1, $2) RETURNING *",
        [nombre, descripcion]
        );

        res.status(201).json({
            message: "Categoría creada",
            category: newCategory.rows[0]
        });

    } catch (error) {
        console.error("Error al crear la categoría:", error);
        return res.status(500).json({ message: "Error interno del servidor" });

    }
   
}

const getCategories = async (req, res) => {
    try {
        const categories = await pool.query("SELECT * FROM categoria");
        res.status(200).json(categories.rows);
    } catch (err){
        console.error("Error al obtener las categorías:", err);
        return res.status(500).json({ message: "Error interno del servidor" });
    }
}

const getCategoryById = async (req, res) => {
    try {
        const {id} = req.params;
        const category = await pool.query(
            "SELECT * FROM categoria WHERE id_categoria = $1",
            [id]
        );
        if (category.rows.length === 0) {
            return res.status(404).json({ message: "Categoría no encontrada" });
        }
        res.status(200).json(category.rows[0]);
    } catch (err) {
        console.error("Error al obtener la categoría:", err);
        return res.status(500).json({ message: "Error interno del servidor" });
    }
}

const updateCategory = async (req, res) => {
    try {
        const {id} = req.params;
        const {nombre, descripcion} = req.body;

        const updatedCategory = await pool.query(
            "UPDATE categoria SET nombre = COALESCE($1, nombre), descripcion = COALESCE($2, descripcion) WHERE id_categoria = $3 RETURNING *",
            [nombre, descripcion, id]
        );

        if (!nombre && !descripcion) {
             return res.status(400).json({message: "Debe enviar al menos un campo para actualizar"});
        }

        if (updatedCategory.rows.length === 0) {
            return res.status(404).json({ message: "Categoría no encontrada" });
        }

        res.status(200).json({
            message: "Categoría actualizada",
            category: updatedCategory.rows[0]
        });

    } catch (err) {
        console.error("Error al actualizar la categoría:", err);
        return res.status(500).json({ message: "Error interno del servidor" });
    }
}

const deleteCategory = async (req, res) => {
    try {
        const {id} = req.params;
        const deletedCategory = await pool.query(
            "DELETE FROM categoria WHERE id_categoria = $1 RETURNING *",
            [id]
        );

        if (deletedCategory.rows.length === 0) {
            return res.status(404).json({ message: "Categoría no encontrada" });
        }

        res.status(200).json({
            message: "Categoría eliminada",
            category: deletedCategory.rows[0]
        });

    } catch (err) {
        console.error("Error al eliminar la categoría:", err);
        return res.status(500).json({ message: "Error interno del servidor" });
    }
}
 
module.exports = {
    createCategory,
    getCategories,
    getCategoryById,
    updateCategory,
    deleteCategory
}