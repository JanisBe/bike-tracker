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
      byte = encoded.codePointAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);

    const dLat = (result & 1) ? ~(result >> 1) : (result >> 1);
    lat += dLat;

    shift = 0;
    result = 0;

    do {
      byte = encoded.codePointAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);

    const dLng = (result & 1) ? ~(result >> 1) : (result >> 1);
    lng += dLng;

    points.push([lat / 1e5, lng / 1e5]);
  }

  return points;
}

/**
 * Encodes an array of [lat, lng] coordinates into a Google Encoded Polyline string.
 * @param {Array<[number, number]>} points
 * @returns {string}
 */
export function encodePolyline(points) {
  if (!points || points.length === 0) return '';
  let encoded = '';
  let prevLat = 0;
  let prevLng = 0;

  for (const point of points) {
    const lat = Math.round(point[0] * 1e5);
    const lng = Math.round(point[1] * 1e5);

    encoded += encodeSignedValue(lat - prevLat);
    encoded += encodeSignedValue(lng - prevLng);

    prevLat = lat;
    prevLng = lng;
  }
  return encoded;
}

function encodeSignedValue(val) {
  let sgn = val < 0 ? ~(val << 1) : (val << 1);
  let str = '';
  while (sgn >= 0x20) {
    str += String.fromCharCode((0x20 | (sgn & 0x1f)) + 63);
    sgn >>= 5;
  }
  str += String.fromCharCode(sgn + 63);
  return str;
}
