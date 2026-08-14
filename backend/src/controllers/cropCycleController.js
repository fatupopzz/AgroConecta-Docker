const { pool } = require("../config/db");
const { normalizeCropName } = require("../utils/cropNames");

const GUATEMALA_TIME_ZONE = "America/Guatemala";

const getGuatemalaMonth = (date = new Date()) =>
  Number(
    new Intl.DateTimeFormat("en-US", {
      month: "numeric",
      timeZone: GUATEMALA_TIME_ZONE,
    }).format(date),
  );

const isPhaseActive = (phase, month) => {
  const startMonth = Number(phase.mes_inicio);
  const endMonth = Number(phase.mes_fin);

  if (startMonth <= endMonth) {
    return month >= startMonth && month <= endMonth;
  }

  return month >= startMonth || month <= endMonth;
};

const monthsUntilPhase = (phase, currentMonth) => {
  const distance = (Number(phase.mes_inicio) - currentMonth + 12) % 12;
  return distance === 0 ? 12 : distance;
};

const resolveCropPhases = (phases, currentMonth) => {
  const activePhases = phases.filter((phase) => isPhaseActive(phase, currentMonth));
  const activeIds = new Set(activePhases.map((phase) => phase.id_ciclo));
  const inactivePhases = phases.filter((phase) => !activeIds.has(phase.id_ciclo));
  const nextCandidates = inactivePhases.length > 0 ? inactivePhases : phases;

  const nextPhase = [...nextCandidates].sort((left, right) => {
    const distanceDifference =
      monthsUntilPhase(left, currentMonth) - monthsUntilPhase(right, currentMonth);

    if (distanceDifference !== 0) return distanceDifference;
    return Number(left.id_ciclo) - Number(right.id_ciclo);
  })[0] || null;

  return { activePhases, nextPhase };
};

const formatPhase = (phase) => {
  if (!phase) return null;

  return {
    id_ciclo: Number(phase.id_ciclo),
    fase: phase.fase,
    mes_inicio: Number(phase.mes_inicio),
    mes_fin: Number(phase.mes_fin),
    descripcion: phase.descripcion,
    productos_recomendados: Array.isArray(phase.productos_recomendados)
      ? phase.productos_recomendados
      : [],
  };
};

const createGetCropCycles = ({ database = pool, now = () => new Date() } = {}) =>
  async (req, res) => {
    const cropName = normalizeCropName(req.params.cultivo);
    if (!cropName) {
      return res.status(400).json({ error: "Cultivo inválido" });
    }

    try {
      const result = await database.query(
        `SELECT id_ciclo, cultivo, fase, mes_inicio, mes_fin,
                descripcion, productos_recomendados
         FROM ciclo_cultivo
         WHERE LOWER(cultivo) = $1
         ORDER BY mes_inicio ASC, id_ciclo ASC`,
        [cropName],
      );

      if (result.rows.length === 0) {
        return res.status(404).json({
          error: "No se encontraron ciclos para el cultivo solicitado",
        });
      }

      const currentMonth = getGuatemalaMonth(now());
      const { activePhases, nextPhase } = resolveCropPhases(
        result.rows,
        currentMonth,
      );
      const formattedActivePhases = activePhases.map(formatPhase);

      return res.status(200).json({
        cultivo: result.rows[0].cultivo,
        mes_actual: currentMonth,
        fase_actual: formattedActivePhases[0] || null,
        fases_activas: formattedActivePhases,
        proxima_fase: formatPhase(nextPhase),
      });
    } catch (error) {
      console.error("Error al obtener ciclos de cultivo:", error);
      return res.status(500).json({ error: "Error al obtener ciclos de cultivo" });
    }
  };

const getCropCycles = createGetCropCycles();

module.exports = {
  createGetCropCycles,
  getCropCycles,
  getGuatemalaMonth,
  isPhaseActive,
  normalizeCropName,
  resolveCropPhases,
};
