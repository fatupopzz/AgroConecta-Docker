const MAX_CROP_NAME_LENGTH = 80;
const MAX_PROFILE_CROPS = 20;
const CROP_ALIASES = new Map([
  ["maiz", "maíz"],
  ["cafe", "café"],
  ["frijol", "frijol"],
  ["elote", "elote"],
  ["tomate", "tomate"],
]);

const stripDiacritics = (value) =>
  value.normalize("NFD").replace(/[\u0300-\u036f]/g, "");

const normalizeCropName = (value) => {
  if (typeof value !== "string") return null;

  const normalized = value.trim().replace(/\s+/g, " ").toLocaleLowerCase("es-GT");
  if (
    normalized.length === 0 ||
    normalized.length > MAX_CROP_NAME_LENGTH ||
    !/^[\p{L}]+(?:[ '-][\p{L}]+)*$/u.test(normalized)
  ) {
    return null;
  }

  const aliasKey = stripDiacritics(normalized);
  return CROP_ALIASES.get(aliasKey) || normalized;
};

const parseCropNames = (value) => {
  if (value === null || value === undefined) return [];

  const rawCrops = Array.isArray(value)
    ? value
    : typeof value === "string"
      ? value.trim() === ""
        ? []
        : value.split(/[,;|/\n]+|\s+y\s+/iu)
      : null;

  if (!rawCrops || rawCrops.length > MAX_PROFILE_CROPS) return null;

  const crops = [];
  const seen = new Set();
  for (const rawCrop of rawCrops) {
    const crop = normalizeCropName(rawCrop);
    if (!crop) return null;

    const key = stripDiacritics(crop);
    if (!seen.has(key)) {
      seen.add(key);
      crops.push(crop);
    }
  }

  return crops;
};

const serializeCropNames = (crops) =>
  crops.length > 0 ? crops.join(", ") : null;

const withCropList = (profile) => {
  if (!profile) return profile;

  return {
    ...profile,
    cultivos: parseCropNames(profile.cultivos_principales) || [],
  };
};

module.exports = {
  normalizeCropName,
  parseCropNames,
  serializeCropNames,
  withCropList,
};
