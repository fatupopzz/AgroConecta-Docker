const path = require("node:path");
const PDFDocument = require("pdfkit");

const logoPath = path.join(__dirname, "../../assets/agroconecta-logo.png");
const safeText = (value) => String(value ?? "No disponible");

const renderOrderHistoryPdf = ({ farmer, orders }) => new Promise((resolve, reject) => {
  const doc = new PDFDocument({ size: "A4", margin: 48, autoFirstPage: true });
  const chunks = [];
  doc.on("data", (chunk) => chunks.push(chunk));
  doc.on("error", reject);
  doc.on("end", () => resolve(Buffer.concat(chunks)));

  doc.roundedRect(48, 38, 64, 64, 12).fill("#185c39");
  doc.image(logoPath, 55, 45, { fit: [50, 50] });
  doc.fillColor("#185c39").fontSize(19).text("Historial de pedidos", 128, 56);
  doc.x = 48;
  doc.y = 112;
  doc.fillColor("#222222").fontSize(11);
  doc.text(`Agricultor: ${safeText(farmer.nombre)} ${farmer.apellido || ""}`.trim());
  if (farmer.email) doc.text(`Correo: ${farmer.email}`);
  if (farmer.telefono) doc.text(`Teléfono: ${farmer.telefono}`);
  if (farmer.departamento || farmer.municipio) {
    doc.text(`Ubicación: ${[farmer.municipio, farmer.departamento].filter(Boolean).join(", ")}`);
  }
  if (farmer.tipo_agricultor) doc.text(`Tipo de agricultor: ${farmer.tipo_agricultor}`);
  if (farmer.tamano_terreno_ha) doc.text(`Terreno: ${farmer.tamano_terreno_ha} ha`);
  if (farmer.cultivos_principales) doc.text(`Cultivos: ${farmer.cultivos_principales}`);
  doc.moveDown();

  if (orders.length === 0) {
    doc.text("No existen pedidos en tu historial.");
  }

  for (const order of orders) {
    if (doc.y > 680) doc.addPage();
    doc.fillColor("#185c39").fontSize(13).text(`Pedido #${order.id}`);
    doc.fillColor("#222222").fontSize(10);
    doc.text(`Fecha: ${safeText(order.date)}`);
    doc.text(`Estado: ${safeText(order.status)}`);
    doc.text("Productos:");
    if (order.products.length === 0) doc.text("  Sin detalles de productos");
    for (const item of order.products) {
      if (doc.y > 745) doc.addPage();
      doc.text(`  ${safeText(item.name)} — Cantidad: ${safeText(item.quantity)}`);
    }
    if (doc.y > 745) doc.addPage();
    const total = Number(order.total);
    doc.font("Helvetica-Bold").text(`Total: ${Number.isFinite(total) ? `Q${total.toFixed(2)}` : "No disponible"}`);
    doc.font("Helvetica").moveDown();
  }
  doc.end();
});

module.exports = { renderOrderHistoryPdf };
