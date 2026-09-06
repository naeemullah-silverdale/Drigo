// Utility for resolving physical addresses and formatting latitude & longitude coordinates

interface KnownLandmark {
  name: string;
  address: string;
  lat: number;
  lng: number;
  radiusKm: number;
}

// Curated landmarks for instant offline resolution across Pakistan
const KNOWN_LANDMARKS: KnownLandmark[] = [
  // Peshawar Landmarks
  {
    name: 'Defense Colony, Peshawar Cantt',
    address: 'Defense Colony, Peshawar Cantonment, Khyber Pakhtunkhwa, Pakistan',
    lat: 34.0151,
    lng: 71.5249,
    radiusKm: 1.5,
  },
  {
    name: 'Saddar Bazaar, Peshawar Cantt',
    address: 'Saddar Road, Peshawar Cantonment, Peshawar, KP, Pakistan',
    lat: 34.0044,
    lng: 71.5369,
    radiusKm: 1.5,
  },
  {
    name: 'Hayatabad Phase 3',
    address: 'Phase 3, Hayatabad, Peshawar, Khyber Pakhtunkhwa, Pakistan',
    lat: 33.9925,
    lng: 71.4380,
    radiusKm: 2.0,
  },
  {
    name: 'University of Peshawar & KTH',
    address: 'University Road, Jamrud Road, Peshawar, KP, Pakistan',
    lat: 34.0086,
    lng: 71.4872,
    radiusKm: 2.0,
  },
  {
    name: 'Shero Jhangi / Ring Road',
    address: 'Ring Road, Shero Jhangi, Peshawar, KP, Pakistan',
    lat: 34.0205,
    lng: 71.5750,
    radiusKm: 2.0,
  },
  {
    name: 'Gulbahar / GT Road',
    address: 'GT Road, Gulbahar, Peshawar, KP, Pakistan',
    lat: 34.0125,
    lng: 71.5830,
    radiusKm: 2.0,
  },
  {
    name: 'Karkhano Market',
    address: 'Karkhano Markets, Jamrud Road, Peshawar, KP, Pakistan',
    lat: 33.9850,
    lng: 71.4150,
    radiusKm: 2.5,
  },
  {
    name: 'Bacha Khan International Airport',
    address: 'Civil Airport Road, Peshawar, KP, Pakistan',
    lat: 33.9940,
    lng: 71.5150,
    radiusKm: 2.5,
  },
  {
    name: 'Dabgari Gardens, Peshawar',
    address: 'Dabgari Gardens, Kohat Road, Peshawar, KP, Pakistan',
    lat: 34.0020,
    lng: 71.5580,
    radiusKm: 1.5,
  },

  // Islamabad Landmarks
  {
    name: 'Islamabad Blue Area / Zero Point',
    address: 'Jinnah Avenue, Blue Area, Islamabad, Federal Capital Territory, Pakistan',
    lat: 33.6844,
    lng: 73.0479,
    radiusKm: 3.5,
  },
  {
    name: 'F-6 Markaz (Super Market)',
    address: 'Sector F-6, Islamabad, Federal Capital Territory, Pakistan',
    lat: 33.7294,
    lng: 73.0931,
    radiusKm: 2.0,
  },
  {
    name: 'F-7 Markaz (Jinnah Super)',
    address: 'Sector F-7, Islamabad, Federal Capital Territory, Pakistan',
    lat: 33.7210,
    lng: 73.0560,
    radiusKm: 2.0,
  },
  {
    name: 'F-8 Markaz / District Courts',
    address: 'Sector F-8, Islamabad, Federal Capital Territory, Pakistan',
    lat: 33.7167,
    lng: 73.0556,
    radiusKm: 2.0,
  },
  {
    name: 'F-10 Markaz',
    address: 'Sector F-10, Islamabad, Federal Capital Territory, Pakistan',
    lat: 33.6938,
    lng: 73.0167,
    radiusKm: 2.0,
  },
  {
    name: 'F-11 Markaz',
    address: 'Sector F-11, Islamabad, Federal Capital Territory, Pakistan',
    lat: 33.6840,
    lng: 72.9850,
    radiusKm: 2.0,
  },
  {
    name: 'G-9 Markaz (Karachi Company)',
    address: 'Sector G-9, Islamabad, Federal Capital Territory, Pakistan',
    lat: 33.6910,
    lng: 73.0300,
    radiusKm: 2.0,
  },
  {
    name: 'I-8 Markaz',
    address: 'Sector I-8, Islamabad, Federal Capital Territory, Pakistan',
    lat: 33.6680,
    lng: 73.0760,
    radiusKm: 2.0,
  },
  {
    name: 'H-12 NUST University',
    address: 'Sector H-12, Kashmir Highway, Islamabad, Pakistan',
    lat: 33.6425,
    lng: 72.9904,
    radiusKm: 2.5,
  },
  {
    name: 'Islamabad International Airport',
    address: 'Airport Avenue, Islamabad, Pakistan',
    lat: 33.5651,
    lng: 72.8465,
    radiusKm: 4.0,
  },

  // Rawalpindi
  {
    name: 'Saddar, Rawalpindi',
    address: 'The Mall, Saddar, Rawalpindi, Punjab, Pakistan',
    lat: 33.5973,
    lng: 73.0479,
    radiusKm: 2.5,
  },
  {
    name: 'Bahria Town Rawalpindi / Islamabad',
    address: 'Phase 1-8, Bahria Town, Rawalpindi, Punjab, Pakistan',
    lat: 33.5280,
    lng: 73.1120,
    radiusKm: 4.0,
  },
];

// Distance calculation using Haversine formula
function calculateDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371; // Earth radius in km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

// In-memory cache for reverse geocoding results
const geocodeCache = new Map<string, string>();

/**
 * Checks if a name string is just a generic placeholder or contains raw coordinates
 */
export function isPlaceholderOrCoords(str?: string): boolean {
  if (!str) return true;
  const s = str.trim().toLowerCase();
  if (
    s === '' ||
    s === 'pickup location' ||
    s === 'drop-off destination' ||
    s === 'destination' ||
    s === 'pickup' ||
    s === 'selected location' ||
    s.startsWith('location (') ||
    s.startsWith('location(') ||
    s.startsWith('selected location') ||
    /^-?\d+(\.\d+)?,\s*-?\d+(\.\d+)?$/.test(s)
  ) {
    return true;
  }
  return false;
}

/**
 * Resolves a clean, human-readable Physical Address and Area Name
 * for any LocationPoint, replacing raw coordinates/placeholders with real physical addresses.
 */
export function resolvePhysicalAddress(loc?: {
  name?: string;
  address?: string;
  lat?: number;
  lng?: number;
}): {
  name: string;
  physicalAddress: string;
  lat: number;
  lng: number;
  formattedCoords: string;
} {
  let lat = Number(loc?.lat);
  let lng = Number(loc?.lng);

  if (isNaN(lat)) lat = 34.0151;
  if (isNaN(lng)) lng = 71.5249;

  let rawName = (loc?.name || '').trim();
  let rawAddress = (loc?.address || '').trim();

  // If destination is specifically Islamabad or other major Pakistan hubs
  const lowerName = rawName.toLowerCase();
  const lowerAddr = rawAddress.toLowerCase();

  const isIslamabad = lowerName.includes('islamabad') || lowerAddr.includes('islamabad');
  const isRawalpindi = lowerName.includes('rawalpindi') || lowerAddr.includes('rawalpindi') || lowerName.includes('pindi') || lowerAddr.includes('pindi');
  const isLahore = lowerName.includes('lahore') || lowerAddr.includes('lahore');
  const isMardan = lowerName.includes('mardan') || lowerAddr.includes('mardan');
  const isNowshera = lowerName.includes('nowshera') || lowerAddr.includes('nowshera');
  const isCharsadda = lowerName.includes('charsadda') || lowerAddr.includes('charsadda');
  const isAbbottabad = lowerName.includes('abbottabad') || lowerAddr.includes('abbottabad');
  const isSwat = lowerName.includes('swat') || lowerAddr.includes('swat') || lowerName.includes('mingora') || lowerAddr.includes('mingora');

  if (isIslamabad && (lat > 33.9 || lat < 33.4 || isNaN(lat))) {
    lat = 33.6844;
    lng = 73.0479;
  } else if (isRawalpindi && (lat > 33.8 || lat < 33.3 || isNaN(lat))) {
    lat = 33.5973;
    lng = 73.0479;
  } else if (isLahore && (lat > 32.5 || lat < 30.5 || isNaN(lat))) {
    lat = 31.5204;
    lng = 74.3587;
  } else if (isMardan && (lat > 34.4 || lat < 34.05 || isNaN(lat))) {
    lat = 34.1989;
    lng = 72.0404;
  } else if (isNowshera && (lat > 34.1 || lat < 33.9 || isNaN(lat))) {
    lat = 34.0153;
    lng = 71.9747;
  } else if (isCharsadda && (lat > 34.25 || lat < 34.05 || isNaN(lat))) {
    lat = 34.1495;
    lng = 71.7428;
  } else if (isAbbottabad && (lat > 34.3 || lat < 34.0 || isNaN(lat))) {
    lat = 34.1688;
    lng = 73.2215;
  } else if (isSwat && (lat > 35.0 || lat < 34.5 || isNaN(lat))) {
    lat = 34.7717;
    lng = 72.3602;
  }

  // Find nearest known landmark in database
  let bestLandmark: KnownLandmark | null = null;
  let minDistance = Infinity;

  for (const lm of KNOWN_LANDMARKS) {
    const dist = calculateDistanceKm(lat, lng, lm.lat, lm.lng);
    if (dist <= lm.radiusKm && dist < minDistance) {
      minDistance = dist;
      bestLandmark = lm;
    }
  }

  // Format coordinates string
  const formattedCoords = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;

  // Determine Physical Name
  let finalName = rawName;
  if (isPlaceholderOrCoords(finalName)) {
    if (bestLandmark) {
      finalName = bestLandmark.name;
    } else if (isIslamabad) {
      finalName = 'Islamabad Capital Territory';
    } else if (isRawalpindi) {
      finalName = 'Rawalpindi City';
    } else if (isLahore) {
      finalName = 'Lahore City';
    } else if (isMardan) {
      finalName = 'Mardan City';
    } else if (isNowshera) {
      finalName = 'Nowshera Cantonment';
    } else if (isCharsadda) {
      finalName = 'Charsadda Bazaar';
    } else if (isAbbottabad) {
      finalName = 'Abbottabad City';
    } else if (isSwat) {
      finalName = 'Mingora / Swat Valley';
    } else if (lat >= 33.9 && lat <= 34.2 && lng >= 71.3 && lng <= 71.7) {
      finalName = 'Peshawar Urban Area';
    } else {
      finalName = `Area (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
    }
  }

  // Determine Full Physical Address
  let finalAddress = rawAddress;
  if (
    !finalAddress ||
    isPlaceholderOrCoords(finalAddress) ||
    finalAddress.toLowerCase().includes('selected location') ||
    (isIslamabad && finalAddress.toLowerCase().includes('peshawar'))
  ) {
    if (bestLandmark) {
      finalAddress = bestLandmark.address;
    } else if (isIslamabad) {
      finalAddress = 'Islamabad, Federal Capital Territory, 44000, Pakistan';
    } else if (isRawalpindi) {
      finalAddress = 'Rawalpindi, Punjab, 46000, Pakistan';
    } else if (isLahore) {
      finalAddress = 'Lahore, Punjab, 54000, Pakistan';
    } else if (isMardan) {
      finalAddress = 'Mardan, Khyber Pakhtunkhwa, 23200, Pakistan';
    } else if (isNowshera) {
      finalAddress = 'Nowshera, Khyber Pakhtunkhwa, 24100, Pakistan';
    } else if (isCharsadda) {
      finalAddress = 'Charsadda, Khyber Pakhtunkhwa, 24420, Pakistan';
    } else if (isAbbottabad) {
      finalAddress = 'Abbottabad, Khyber Pakhtunkhwa, 22010, Pakistan';
    } else if (isSwat) {
      finalAddress = 'Mingora, Swat, Khyber Pakhtunkhwa, 19130, Pakistan';
    } else if (lat >= 33.9 && lat <= 34.2 && lng >= 71.3 && lng <= 71.7) {
      finalAddress = 'Peshawar, Khyber Pakhtunkhwa, 25000, Pakistan';
    } else {
      finalAddress = `Pakistan (${lat.toFixed(5)}, ${lng.toFixed(5)})`;
    }
  }

  return {
    name: finalName,
    physicalAddress: finalAddress,
    lat,
    lng,
    formattedCoords,
  };
}

/**
 * Async live reverse geocoder from OpenStreetMap Nominatim with memory cache
 */
export async function reverseGeocodeCoords(
  lat: number,
  lng: number
): Promise<{ displayName: string; areaName: string } | null> {
  const cacheKey = `${lat.toFixed(4)},${lng.toFixed(4)}`;
  if (geocodeCache.has(cacheKey)) {
    const cached = geocodeCache.get(cacheKey)!;
    return { displayName: cached, areaName: cached.split(',')[0].trim() };
  }

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3500);

    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`,
      {
        signal: controller.signal,
        headers: {
          'Accept-Language': 'en',
        },
      }
    );
    clearTimeout(timeoutId);

    if (res.ok) {
      const data = await res.json();
      if (data && data.display_name) {
        const full = data.display_name;
        const addr = data.address || {};
        const area =
          addr.suburb ||
          addr.neighbourhood ||
          addr.road ||
          addr.city_district ||
          addr.town ||
          addr.city ||
          full.split(',')[0];

        geocodeCache.set(cacheKey, full);
        return { displayName: full, areaName: area };
      }
    }
  } catch {
    // Graceful fallback to offline resolution
  }

  return null;
}

/**
 * Accurately calculates route driving distance (in km) and duration (in minutes)
 * between two GPS coordinate pairs.
 * 
 * If a stored dbDistance is provided, it validates whether it is physically plausible
 * (i.e. road distance must be >= straight-line distance). If the dbDistance is severely
 * anomalous (e.g. 4.7 km for a 145+ km intercity trip), it overrides it with the true
 * road network distance.
 */
export function calculateAccurateRouteDistance(
  fromLat: number,
  fromLng: number,
  toLat: number,
  toLng: number,
  dbDistanceKm?: number,
  dbDurationMinutes?: number
): {
  distanceKm: number;
  durationMinutes: number;
  isIntercity: boolean;
  straightLineKm: number;
} {
  const safeFromLat = isNaN(Number(fromLat)) ? 34.0151 : Number(fromLat);
  const safeFromLng = isNaN(Number(fromLng)) ? 71.5249 : Number(fromLng);
  const safeToLat = isNaN(Number(toLat)) ? 34.0044 : Number(toLat);
  const safeToLng = isNaN(Number(toLng)) ? 71.5369 : Number(toLng);

  const straightLine = calculateDistanceKm(safeFromLat, safeFromLng, safeToLat, safeToLng);
  const isIntercity = straightLine > 35; // Over 35 km is intercity highway

  // Road factor based on road network topography:
  // Intercity motorway (e.g. M-1 Peshawar-Islamabad is ~184km vs ~145km straight line = 1.27x)
  // Urban road network is ~1.25x - 1.30x
  const roadFactor = isIntercity ? 1.27 : 1.25;
  const calculatedRoadKm = Math.round(Math.max(0.8, straightLine * roadFactor) * 10) / 10;

  let finalDistanceKm: number;
  let finalDurationMinutes: number;

  const dbDist = typeof dbDistanceKm === 'number' && !isNaN(dbDistanceKm) ? dbDistanceKm : 0;

  // Validation: road distance can NEVER be less than 85% of straight-line distance
  // And if it is an intercity trip (e.g., Peshawar to Islamabad = 145+ km air distance),
  // a distance like 4.7 km or 5.2 km is mathematically and physically impossible.
  const isDbDistInvalid =
    dbDist <= 0 ||
    dbDist < straightLine * 0.85 ||
    (isIntercity && dbDist < 30);

  if (isDbDistInvalid) {
    finalDistanceKm = calculatedRoadKm;
  } else {
    // Plausible road distance stored in database
    finalDistanceKm = Math.round(dbDist * 10) / 10;
  }

  // Realistic duration calculation
  if (dbDurationMinutes && dbDurationMinutes > 0 && !isDbDistInvalid) {
    finalDurationMinutes = dbDurationMinutes;
  } else {
    if (finalDistanceKm > 40) {
      // Highway average speed ~85 km/h + toll plaza / interchange transit
      finalDurationMinutes = Math.round((finalDistanceKm / 85) * 60) + 12;
    } else {
      // Urban traffic speed ~25-28 km/h
      finalDurationMinutes = Math.max(5, Math.round((finalDistanceKm / 28) * 60));
    }
  }

  return {
    distanceKm: finalDistanceKm,
    durationMinutes: finalDurationMinutes,
    isIntercity,
    straightLineKm: Math.round(straightLine * 10) / 10,
  };
}

const routeDrivingCache = new Map<string, { distanceKm: number; durationMinutes: number }>();

/**
 * Fetches exact turn-by-turn road network distance and transit duration
 * from public routing engine (OSRM), with instant memory cache and fast timeout.
 */
export async function fetchRealDrivingRoute(
  fromLat: number,
  fromLng: number,
  toLat: number,
  toLng: number
): Promise<{ distanceKm: number; durationMinutes: number } | null> {
  const safeFromLat = Number(fromLat);
  const safeFromLng = Number(fromLng);
  const safeToLat = Number(toLat);
  const safeToLng = Number(toLng);

  if (isNaN(safeFromLat) || isNaN(safeFromLng) || isNaN(safeToLat) || isNaN(safeToLng)) {
    return null;
  }

  const cacheKey = `${safeFromLat.toFixed(3)},${safeFromLng.toFixed(3)}_${safeToLat.toFixed(3)},${safeToLng.toFixed(3)}`;
  if (routeDrivingCache.has(cacheKey)) {
    return routeDrivingCache.get(cacheKey)!;
  }

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);

    const url = `https://router.project-osrm.org/route/v1/driving/${safeFromLng},${safeFromLat};${safeToLng},${safeToLat}?overview=false`;
    const res = await fetch(url, { signal: controller.signal });
    clearTimeout(timeoutId);

    if (res.ok) {
      const data = await res.json();
      if (data && data.routes && data.routes.length > 0) {
        const route = data.routes[0];
        const distKm = Math.round((route.distance / 1000) * 10) / 10;
        const durMins = Math.max(3, Math.round(route.duration / 60));

        const result = { distanceKm: distKm, durationMinutes: durMins };
        routeDrivingCache.set(cacheKey, result);
        return result;
      }
    }
  } catch {
    // Return null on failure or timeout
  }

  return null;
}
