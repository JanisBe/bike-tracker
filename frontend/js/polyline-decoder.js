/**
 * Decodes a Google Encoded Polyline string into an array of [lat, lng] coordinates.
 * @param {string} encoded
 * @returns {Array<[number, number]>}
 */
export function decodePolyline(encoded) {
  if (!encoded || typeof encoded !== 'string') return [];

  const points = [];
  let index = 0;
  let lat = 0;
  let lng = 0;
  const len = encoded.length;

  while (index < len) {
    let shift = 0;
    let result = 0;
    let byte;

    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);

    const dLat = (result & 1) ? ~(result >> 1) : (result >> 1);
    lat += dLat;

    shift = 0;
    result = 0;

    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);

    const dLng = (result & 1) ? ~(result >> 1) : (result >> 1);
    lng += dLng;

    points.push([lat / 1e5, lng / 1e5]);
  }

  return points;
}
