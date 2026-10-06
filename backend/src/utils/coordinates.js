const hasValue = (value) => {
  if (value === null || value === undefined) return false;
  return typeof value !== "string" || value.trim() !== "";
};

const parseOptionalCoordinates = (rawLatitude, rawLongitude) => {
  const hasLatitude = hasValue(rawLatitude);
  const hasLongitude = hasValue(rawLongitude);

  if (!hasLatitude && !hasLongitude) {
    return {
      provided: false,
      latitude: null,
      longitude: null,
    };
  }

  if (!hasLatitude || !hasLongitude) {
    return { error: "latitud y longitud deben enviarse juntas" };
  }

  const latitude = Number(rawLatitude);
  const longitude = Number(rawLongitude);

  if (
    !Number.isFinite(latitude) ||
    !Number.isFinite(longitude) ||
    latitude < -90 ||
    latitude > 90 ||
    longitude < -180 ||
    longitude > 180
  ) {
    return { error: "Coordenadas inválidas" };
  }

  return {
    provided: true,
    latitude,
    longitude,
  };
};

module.exports = { parseOptionalCoordinates };
