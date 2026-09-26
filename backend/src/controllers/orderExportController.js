const { pool } = require("../config/db");
const { loadFarmerHistory } = require("../services/orderExportRepository");
const { renderOrderHistoryPdf } = require("../services/orderHistoryPdf");

const exportOrderHistoryPdf = async (req, res) => {
  if (req.user?.tipo !== "agricultor") {
    return res.status(403).json({ error: "Solo los agricultores pueden exportar pedidos" });
  }

  const userId = Number(req.user.id);
  if (!Number.isSafeInteger(userId) || userId < 1) {
    return res.status(401).json({ error: "Usuario no autenticado" });
  }

  try {
    const history = await loadFarmerHistory(pool, userId);
    if (!history) {
      return res.status(403).json({ error: "Perfil de agricultor no disponible" });
    }

    const pdf = await renderOrderHistoryPdf(history);
    res.set("Content-Type", "application/pdf");
    res.set("Content-Disposition", 'attachment; filename="historial-pedidos-agroconecta.pdf"');
    return res.send(pdf);
  } catch (_error) {
    console.error("Error al exportar historial de pedidos");
    return res.status(500).json({ error: "No se pudo generar el PDF de pedidos" });
  }
};

module.exports = { exportOrderHistoryPdf };
