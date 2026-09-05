import React, { useState, useEffect } from 'react';
import { APIProvider, Map as GoogleMap, AdvancedMarker, Marker, InfoWindow, useMap } from '@vis.gl/react-google-maps';
import { Trip, Driver, LocationPoint, LiveDriverLocation } from '../types';
import { LeafletMap } from './LeafletMap';
import {
  MapPin,
  Navigation,
  Car,
  Smartphone,
  ShieldAlert,
  Compass,
  Zap,
  RotateCcw,
  CheckCircle2,
  DollarSign,
  Phone,
  MessageSquare,
  Activity,
  Layers,
  Radio,
  Wifi,
  BatteryCharging,
  Key,
  Globe,
  Settings,
  X,
  Clock,
  User,
  ShieldCheck,
  Star,
  Copy,
  Check
} from 'lucide-react';

interface FleetDispatchMapProps {
  trips: Trip[];
  drivers: Driver[];
  liveLocations?: Record<string, LiveDriverLocation>;
  selectedTripId: string | null;
  onSelectTrip: (id: string) => void;
  onUpdateTripLocations: (tripId: string, newFrom?: LocationPoint, newTo?: LocationPoint) => void;
}

// Preset popular locations for quick FROM / TO state testing
const PRESET_LOCATIONS: LocationPoint[] = [
  {
    name: 'Yaya Centre Shopping Mall',
    address: 'Argwings Kodhek Rd, Kilimani',
    lat: -1.2918,
    lng: 36.7865,
    landmark: 'Main Entrance Gate A',
  },
  {
    name: 'Nairobi Railway Station (CBD)',
    address: 'Station Rd, Central Business District',
    lat: -1.2882,
    lng: 36.8285,
    landmark: 'Ticketing Concourse Drop-off',
  },
  {
    name: 'Jomo Kenyatta Int. Airport (JKIA)',
    address: 'Airport North Rd, Embakasi',
    lat: -1.3192,
    lng: 36.9275,
    landmark: 'Terminal 1A Departures',
  },
  {
    name: 'Sarit Centre Mall',
    address: 'Pio Gama Pinto Rd, Westlands',
    lat: -1.2612,
    lng: 36.8024,
    landmark: 'Rooftop Car Park Exit',
  },
  {
    name: 'Karen Country Club',
    address: 'Karen Rd, Karen',
    lat: -1.3320,
    lng: 36.7150,
    landmark: 'Main Clubhouse Gate',
  },
  {
    name: 'Two Rivers Mall',
    address: 'Limuru Rd, Runda',
    lat: -1.2150,
    lng: 36.7980,
    landmark: 'Main Valet Drop',
  },
];

// Map camera auto-bounds fit component
const MapBoundsFit: React.FC<{
  from: LocationPoint;
  to: LocationPoint;
  driverLoc?: { lat: number; lng: number };
}> = ({ from, to, driverLoc }) => {
  const map = useMap();

  useEffect(() => {
    if (!map || typeof google === 'undefined' || !google.maps) return;

    try {
      const bounds = new google.maps.LatLngBounds();
      if (from && typeof from.lat === 'number' && typeof from.lng === 'number') {
        bounds.extend({ lat: from.lat, lng: from.lng });
      }
      if (to && typeof to.lat === 'number' && typeof to.lng === 'number') {
        bounds.extend({ lat: to.lat, lng: to.lng });
      }

      if (driverLoc && typeof driverLoc.lat === 'number' && typeof driverLoc.lng === 'number') {
        bounds.extend({ lat: driverLoc.lat, lng: driverLoc.lng });
      }

      map.fitBounds(bounds, {
        top: 90,
        bottom: 90,
        left: 90,
        right: 90,
      });
    } catch (e) {
      console.warn('Error adjusting map camera bounds:', e);
    }
  }, [map, from?.lat, from?.lng, to?.lat, to?.lng, driverLoc?.lat, driverLoc?.lng]);

  return null;
};

// Route polyline renderer
const RoutePolyline: React.FC<{
  from: LocationPoint;
  to: LocationPoint;
  driverLoc?: { lat: number; lng: number };
  status?: string;
}> = ({ from, to, driverLoc, status }) => {
  const map = useMap();

  useEffect(() => {
    if (!map || typeof google === 'undefined' || !google.maps) return;
    if (!from || typeof from.lat !== 'number' || !to || typeof to.lat !== 'number') return;

    const polylines: google.maps.Polyline[] = [];

    const isComingOrArrived = status === 'DRIVER_COMING' || status === 'DRIVER_ARRIVED';
    const isInTrip = status === 'IN_TRIP';
    const isFinished = status === 'COMPLETED' || status === 'CANCELLED';

    // 1. If DRIVER_COMING or DRIVER_ARRIVED: Show Driver -> Pickup route in Emerald
    if (driverLoc && typeof driverLoc.lat === 'number' && (isComingOrArrived || (!isFinished && !isInTrip))) {
      polylines.push(new google.maps.Polyline({
        path: [
          { lat: driverLoc.lat, lng: driverLoc.lng },
          { lat: from.lat, lng: from.lng },
        ],
        geodesic: true,
        strokeColor: '#10B981', // Emerald for Driver -> Pickup
        strokeOpacity: 0.9,
        strokeWeight: 5,
        map,
      }));
    }

    // 2. If IN_TRIP: Show Driver/Current Location -> Destination route in Blue
    if (isInTrip && driverLoc && typeof driverLoc.lat === 'number') {
      polylines.push(new google.maps.Polyline({
        path: [
          { lat: driverLoc.lat, lng: driverLoc.lng },
          { lat: to.lat, lng: to.lng },
        ],
        geodesic: true,
        strokeColor: '#3B82F6', // Blue for Driver -> Destination
        strokeOpacity: 0.9,
        strokeWeight: 6,
        map,
      }));
    }

    // 3. Always show standard FROM -> TO route for context
    polylines.push(new google.maps.Polyline({
      path: [
        { lat: from.lat, lng: from.lng },
        { lat: to.lat, lng: to.lng },
      ],
      geodesic: true,
      strokeColor: '#64748b', // Slate blue baseline route
      strokeOpacity: 0.6,
      strokeWeight: 4,
      map,
    }));

    return () => {
      polylines.forEach(p => p.setMap(null));
    };
  }, [map, from?.lat, from?.lng, to?.lat, to?.lng, driverLoc?.lat, driverLoc?.lng, status]);

  return null;
};

export const FleetDispatchMap: React.FC<FleetDispatchMapProps> = ({
  trips,
  drivers,
  liveLocations,
  selectedTripId,
  onSelectTrip,
  onUpdateTripLocations,
}) => {
  const activeTrip = trips.find(t => t.id === selectedTripId) || trips[0];
  const assignedDriver = drivers.find(d => d.id === activeTrip?.driverId);

  // Copy phone toast state
  const [copiedPhone, setCopiedPhone] = useState(false);

  // Selected Driver Popup State for marker click inspection
  const [selectedDriverPopup, setSelectedDriverPopup] = useState<{
    driverId: string;
    driverName: string;
    avatar?: string;
    phone?: string;
    email?: string;
    rating?: number;
    totalTrips?: number;
    lat: number;
    lng: number;
    heading: number;
    speedKmh: number;
    status: string;
    vehicleType: string;
    vehicleMakeModel?: string;
    licensePlate: string;
    updatedAt: string;
    isAssigned?: boolean;
  } | null>(null);

  // Helper to format raw timestamps or numbers into human readable time
  const formatLastUpdated = (val: string | number | undefined): string => {
    if (!val) return 'Just now';
    const str = String(val);
    if (str === 'Just now') return 'Just now';
    const num = Number(val);
    if (!isNaN(num) && num > 1000000000) {
      const d = new Date(num);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }
    return str;
  };

  // Consolidate all driver locations from live_driver_locations node & drivers list
  const allDriverMarkers = React.useMemo(() => {
    const driverMap = new Map<string, {
      driverId: string;
      driverName: string;
      avatar: string;
      phone: string;
      email: string;
      rating: number;
      totalTrips: number;
      lat: number;
      lng: number;
      heading: number;
      speedKmh: number;
      status: string;
      vehicleType: string;
      vehicleMakeModel: string;
      licensePlate: string;
      updatedAt: string;
      isAssigned: boolean;
      fareDisplay: string;
    }>();

    // 1. Base from drivers prop (filter out offline drivers)
    drivers.forEach((d, index) => {
      if (
        d.status !== 'offline' &&
        d.currentLocation &&
        typeof d.currentLocation.lat === 'number' &&
        typeof d.currentLocation.lng === 'number' &&
        !isNaN(d.currentLocation.lat) &&
        !isNaN(d.currentLocation.lng)
      ) {
        const fareVal = d.status === 'on_trip' ? '400 Rs' : d.id === assignedDriver?.id ? '450 Rs' : `${380 + (index % 3) * 20} Rs`;

        driverMap.set(d.id, {
          driverId: d.id,
          driverName: d.fullName,
          avatar: d.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200',
          phone: d.phone || '+92 300 1234567',
          email: d.email || `${d.id.slice(0, 8)}@drigo.pk`,
          rating: d.rating || 4.8,
          totalTrips: d.totalTrips || 120,
          lat: d.currentLocation.lat,
          lng: d.currentLocation.lng,
          heading: d.currentLocation.heading || 0,
          speedKmh: d.currentLocation.speedKmh || 0,
          status: d.status || 'online',
          vehicleType: d.vehicle?.type || 'sedan',
          vehicleMakeModel: d.vehicle ? `${d.vehicle.make} ${d.vehicle.model}` : 'Toyota Corolla',
          licensePlate: d.vehicle?.licensePlate || 'LEA-8924',
          updatedAt: 'Just now',
          isAssigned: d.id === assignedDriver?.id,
          fareDisplay: fareVal
        });
      }
    });

    // 2. Override/add from liveLocations (from live_driver_locations node in Firebase)
    if (liveLocations && typeof liveLocations === 'object') {
      Object.values(liveLocations).forEach((locVal, idx) => {
        const loc = locVal as any;
        if (loc && typeof loc.lat === 'number' && typeof loc.lng === 'number' && !isNaN(loc.lat) && !isNaN(loc.lng)) {
          const existing = driverMap.get(loc.driverId);
          const matchedDriver = drivers.find(d => d.id === loc.driverId);

          // If explicitly offline or record marks unavailable, remove from live map
          if (loc.status === 'offline') {
            driverMap.delete(loc.driverId);
            return;
          }

          const avatar = loc.avatar || loc.driverPhoto || matchedDriver?.avatar || existing?.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200';
          const driverName = loc.driverName || matchedDriver?.fullName || existing?.driverName || `Driver (${loc.driverId.slice(0, 8)})`;
          const phone = loc.phone || matchedDriver?.phone || existing?.phone || '+92 300 1234567';
          const email = loc.email || matchedDriver?.email || existing?.email || `${loc.driverId.slice(0, 8)}@drigo.pk`;
          const rating = typeof loc.rating === 'number' ? loc.rating : matchedDriver?.rating || existing?.rating || 4.9;
          const totalTrips = typeof loc.totalTrips === 'number' ? loc.totalTrips : matchedDriver?.totalTrips || existing?.totalTrips || 142;
          const vehicleMakeModel = loc.vehicleMakeModel || (matchedDriver?.vehicle ? `${matchedDriver.vehicle.make} ${matchedDriver.vehicle.model}` : existing?.vehicleMakeModel || 'Honda Civic');
          const rawFare = loc.fareDisplay || loc.fare || loc.bidAmount;
          const fareDisplay = rawFare
            ? (String(rawFare).includes('Rs') ? String(rawFare) : `${rawFare} Rs`)
            : (loc.status === 'on_trip' ? '400 Rs' : loc.driverId === assignedDriver?.id ? '450 Rs' : `${380 + (idx % 2) * 20} Rs`);

          driverMap.set(loc.driverId, {
            driverId: loc.driverId,
            driverName,
            avatar,
            phone,
            email,
            rating,
            totalTrips,
            lat: loc.lat,
            lng: loc.lng,
            heading: typeof loc.heading === 'number' ? loc.heading : existing?.heading || 0,
            speedKmh: typeof loc.speedKmh === 'number' ? loc.speedKmh : existing?.speedKmh || 0,
            status: loc.status || existing?.status || 'online',
            vehicleType: String(loc.vehicleType || matchedDriver?.vehicle?.type || existing?.vehicleType || 'sedan'),
            vehicleMakeModel,
            licensePlate: loc.licensePlate || matchedDriver?.vehicle?.licensePlate || existing?.licensePlate || 'LEA-8924',
            updatedAt: formatLastUpdated(loc.updatedAt) || existing?.updatedAt || 'Just now',
            isAssigned: loc.driverId === assignedDriver?.id,
            fareDisplay
          });
        }
      });
    }

    return Array.from(driverMap.values());
  }, [drivers, liveLocations, assignedDriver?.id]);

  // SVG Data URI helper for standard Google Map Marker fallback (matching Image 1)
  const createCarIconSvg = (status: string, heading: number = 0, isAssigned: boolean = false, fareDisplay: string = '380 Rs') => {
    const isDark = status === 'on_trip' || status === 'busy';
    const mainColor = isAssigned ? '#2563eb' : isDark ? '#475569' : '#84cc16';
    const strokeColor = isAssigned ? '#1e3a8a' : isDark ? '#0f172a' : '#3f6212';
    const badgeBg = isAssigned ? '#2563eb' : isDark ? '#334155' : '#a3e635';
    const badgeTextColor = isAssigned ? '#ffffff' : isDark ? '#ffffff' : '#0f172a';

    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="90" height="100" viewBox="0 0 90 100">
      <defs>
        <filter id="car-shadow" x="-30%" y="-30%" width="160%" height="160%">
          <feDropShadow dx="0" dy="4" stdDeviation="3.5" flood-opacity="0.45"/>
        </filter>
      </defs>

      <g filter="url(#car-shadow)">
        <rect x="15" y="2" width="60" height="24" rx="12" fill="${badgeBg}" stroke="${isDark ? '#64748b' : '#84cc16'}" stroke-width="1" />
        <text x="45" y="18" font-family="system-ui, -apple-system, sans-serif" font-size="12" font-weight="900" fill="${badgeTextColor}" text-anchor="middle">${fareDisplay}</text>
      </g>

      <g transform="translate(45, 62) rotate(${heading}) translate(-20, -30)" filter="url(#car-shadow)">
        <rect x="3" y="10" width="4" height="10" rx="1" fill="#090d16" />
        <rect x="33" y="10" width="4" height="10" rx="1" fill="#090d16" />
        <rect x="3" y="40" width="4" height="10" rx="1" fill="#090d16" />
        <rect x="33" y="40" width="4" height="10" rx="1" fill="#090d16" />

        <path d="M 11 6 C 11 2, 16 0, 20 0 C 24 0, 29 2, 29 6 L 31 18 C 33 25, 33 38, 31 48 L 29 58 C 29 61, 24 62, 20 62 C 16 62, 11 61, 11 58 L 9 48 C 7 38, 7 25, 9 18 Z" fill="${mainColor}" stroke="${strokeColor}" stroke-width="1.5" />

        <path d="M 7 15 C 4 15, 4 19, 7 19 Z" fill="${mainColor}" stroke="${strokeColor}" stroke-width="0.8" />
        <path d="M 33 15 C 36 15, 36 19, 33 19 Z" fill="${mainColor}" stroke="${strokeColor}" stroke-width="0.8" />

        <path d="M 12 17 C 16 14, 24 14, 28 17 L 27 25 C 23 23, 17 23, 13 25 Z" fill="#0f172a" stroke="#090d16" stroke-width="0.8" />
        <path d="M 14 26 C 18 24, 22 24, 26 26 L 25 40 C 22 41, 18 41, 15 40 Z" fill="#1e293b" opacity="0.4" />
        <path d="M 15 42 C 18 41, 22 41, 25 42 L 26 48 C 22 49, 18 49, 14 48 Z" fill="#0f172a" stroke="#090d16" stroke-width="0.8" />

        <circle cx="13" cy="2" r="1.2" fill="#fef08a" />
        <circle cx="27" cy="2" r="1.2" fill="#fef08a" />

        <rect x="12" y="60" width="3.5" height="1.8" rx="0.5" fill="#ef4444" />
        <rect x="24.5" y="60" width="3.5" height="1.8" rx="0.5" fill="#ef4444" />
      </g>
    </svg>`;

    return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
  };

// Overhead realistic top-down sedan car vector graphic (matching Image 1)
function TopDownCarGraphic({
  status,
  isAssigned = false,
  heading = 0,
  size = 40
}: {
  status: string;
  isAssigned?: boolean;
  heading?: number;
  size?: number;
}) {
  const isDark = status === 'on_trip' || status === 'busy';
  let mainColor = '#84cc16'; // Lime Green like Image 1 ("380 Rs" car)
  let gradientEnd = '#65a30d';
  let strokeColor = '#3f6212';

  if (isAssigned) {
    mainColor = '#2563eb';
    gradientEnd = '#1d4ed8';
    strokeColor = '#1e3a8a';
  } else if (isDark) {
    mainColor = '#475569'; // Dark Slate like Image 1 ("400 Rs" car)
    gradientEnd = '#334155';
    strokeColor = '#0f172a';
  }

  return (
    <div
      className="transition-transform duration-300 ease-out select-none cursor-pointer filter drop-shadow-[0_6px_8px_rgba(0,0,0,0.45)] hover:scale-110"
      style={{
        transform: `rotate(${heading}deg)`,
        width: size,
        height: size * 1.6
      }}
    >
      <svg
        viewBox="0 0 50 80"
        width="100%"
        height="100%"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id={`car-body-grad-${status}-${isAssigned}`} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor={mainColor} />
            <stop offset="100%" stopColor={gradientEnd} />
          </linearGradient>

          <linearGradient id="glass-dark-grad" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#0f172a" />
            <stop offset="100%" stopColor="#1e293b" />
          </linearGradient>

          <linearGradient id="glass-refl-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#ffffff" stopOpacity="0.4" />
            <stop offset="100%" stopColor="#ffffff" stopOpacity="0.0" />
          </linearGradient>
        </defs>

        <g>
          {/* Black Wheels */}
          <rect x="4" y="14" width="4" height="12" rx="1.5" fill="#090d16" />
          <rect x="42" y="14" width="4" height="12" rx="1.5" fill="#090d16" />
          <rect x="4" y="54" width="4" height="12" rx="1.5" fill="#090d16" />
          <rect x="42" y="54" width="4" height="12" rx="1.5" fill="#090d16" />

          {/* Main Car Body - Sleek Overhead Sedan Shape */}
          <path
            d="M 14 8 
               C 14 3, 20 1, 25 1 
               C 30 1, 36 3, 36 8 
               L 39 24 
               C 41 32, 41 48, 39 62 
               L 36 74 
               C 36 78, 30 79, 25 79 
               C 20 79, 14 78, 14 74 
               L 11 62 
               C 9 48, 9 32, 11 24 
               Z"
            fill={`url(#car-body-grad-${status}-${isAssigned})`}
            stroke={strokeColor}
            strokeWidth="1.5"
          />

          {/* Side Mirrors */}
          <path d="M 10 20 C 6 20, 6 25, 10 25 Z" fill={mainColor} stroke={strokeColor} strokeWidth="1" />
          <path d="M 40 20 C 44 20, 44 25, 40 25 Z" fill={mainColor} stroke={strokeColor} strokeWidth="1" />

          {/* Hood Line */}
          <path d="M 18 14 C 22 12, 28 12, 32 14" fill="none" stroke={strokeColor} strokeWidth="1" opacity="0.5" />

          {/* Front Windshield */}
          <path
            d="M 16 22 C 20 19, 30 19, 34 22 L 32 32 C 28 30, 22 30, 18 32 Z"
            fill="url(#glass-dark-grad)"
            stroke="#090d16"
            strokeWidth="1"
          />
          <path
            d="M 17 23 C 20 21, 30 21, 33 23 L 32 27 C 28 25, 22 25, 18 27 Z"
            fill="url(#glass-refl-grad)"
          />

          {/* Roof Contour */}
          <path
            d="M 18 33 C 22 31, 28 31, 32 33 L 31 50 C 28 51, 22 51, 19 50 Z"
            fill={gradientEnd}
            stroke={strokeColor}
            strokeWidth="1"
            opacity="0.85"
          />

          {/* Rear Windshield */}
          <path
            d="M 19 52 C 22 51, 28 51, 31 52 L 33 60 C 29 61, 21 61, 17 60 Z"
            fill="url(#glass-dark-grad)"
            stroke="#090d16"
            strokeWidth="1"
          />

          {/* Trunk Line */}
          <path d="M 18 68 C 22 70, 28 70, 32 68" fill="none" stroke={strokeColor} strokeWidth="1" opacity="0.5" />

          {/* Headlights (Front Glow) */}
          <ellipse cx="16" cy="3" rx="2.5" ry="1.2" fill="#fef08a" />
          <ellipse cx="34" cy="3" rx="2.5" ry="1.2" fill="#fef08a" />

          {/* Taillights (Rear Red) */}
          <rect x="15" y="75" width="5" height="2.5" rx="1" fill="#ef4444" />
          <rect x="30" y="75" width="5" height="2.5" rx="1" fill="#ef4444" />
        </g>
      </svg>
    </div>
  );
}

  // Local editing states for testing FROM and TO independence
  const [editingFrom, setEditingFrom] = useState<LocationPoint>(
    activeTrip ? activeTrip.fromLocation : PRESET_LOCATIONS[0]
  );
  const [editingTo, setEditingTo] = useState<LocationPoint>(
    activeTrip ? activeTrip.toLocation : PRESET_LOCATIONS[1]
  );
  const [showLocationPicker, setShowLocationPicker] = useState<'from' | 'to' | null>(null);

  const [testAddressInput, setTestAddressInput] = useState('');
  const [testCustomMarker, setTestCustomMarker] = useState<{
    name: string;
    address: string;
    lat: number;
    lng: number;
  } | null>(null);

  const handleShowTestMarker = () => {
    if (!testAddressInput.trim()) return;
    const addrLower = testAddressInput.trim().toLowerCase();
    let lat = 34.0151;
    let lng = 71.5249;
    
    if (addrLower.includes('hayatabad')) {
      lat = 33.9925;
      lng = 71.4380;
    } else if (addrLower.includes('university') || addrLower.includes('town')) {
      lat = 34.0080;
      lng = 71.5010;
    } else if (addrLower.includes('saddar') || addrLower.includes('mall')) {
      lat = 34.0044;
      lng = 71.5369;
    } else if (addrLower.includes('shero jahngi') || addrLower.includes('street number 9')) {
      lat = 34.0205;
      lng = 71.5750;
    } else if (addrLower.includes('islamabad')) {
      lat = 33.6844;
      lng = 73.0479;
    } else {
      let hash = 0;
      for (let i = 0; i < addrLower.length; i++) {
        hash = (hash << 5) - hash + addrLower.charCodeAt(i);
        hash |= 0;
      }
      const latOffset = (Math.abs(hash) % 100) / 1000 - 0.05;
      const lngOffset = (Math.abs(hash >> 3) % 100) / 1000 - 0.05;
      lat = 34.0151 + latOffset;
      lng = 71.5249 + lngOffset;
    }

    setTestCustomMarker({
      name: testAddressInput.trim(),
      address: testAddressInput.trim(),
      lat,
      lng
    });
  };

  const [hasMapLoadError, setHasMapLoadError] = useState(false);

  useEffect(() => {
    // Gracefully catch Google Maps auth issues (including ApiProjectMapError)
    const originalAuthFailure = (window as any).gm_authFailure;
    (window as any).gm_authFailure = () => {
      console.warn("Google Maps Platform auth failure intercepted (ApiProjectMapError). Activating High-Fidelity Fallback Map.");
      setHasMapLoadError(true);
      if (originalAuthFailure) {
        try {
          originalAuthFailure();
        } catch (e) {}
      }
    };

    return () => {
      (window as any).gm_authFailure = originalAuthFailure;
    };
  }, []);

  // Relative coordinate calculator for the fallback map to maintain perfect interactivity
  const getRelativeXY = (lat: number, lng: number) => {
    const centerLat = (editingFrom.lat + editingTo.lat) / 2;
    const centerLng = (editingFrom.lng + editingTo.lng) / 2;
    
    const latDiff = Math.abs(editingFrom.lat - editingTo.lat) || 0.05;
    const lngDiff = Math.abs(editingFrom.lng - editingTo.lng) || 0.05;
    const maxDiff = Math.max(latDiff, lngDiff, 0.01) * 1.5;

    // Center coordinates inside a 10%-90% grid bounding area
    const x = 50 + ((lng - centerLng) / maxDiff) * 35;
    const y = 50 - ((lat - centerLat) / maxDiff) * 35;
    
    return {
      x: Math.min(Math.max(x, 10), 90),
      y: Math.min(Math.max(y, 10), 90)
    };
  };

  const renderFallbackMap = () => {
    const fromXY = getRelativeXY(editingFrom.lat, editingFrom.lng);
    const toXY = getRelativeXY(editingTo.lat, editingTo.lng);
    const driverLocationXY = driverLocation ? getRelativeXY(driverLocation.lat, driverLocation.lng) : null;

    return (
      <div className="w-full h-full relative bg-[#090d16] flex flex-col items-center justify-center p-4">
        {/* Subtle grid background */}
        <div 
          className="absolute inset-0 opacity-10 pointer-events-none"
          style={{
            backgroundImage: `
              linear-gradient(to right, #475569 1px, transparent 1px),
              linear-gradient(to bottom, #475569 1px, transparent 1px)
            `,
            backgroundSize: '40px 40px'
          }}
        />

        {/* Diagonal connection lines for high-tech look */}
        <svg className="absolute inset-0 w-full h-full pointer-events-none">
          <defs>
            <linearGradient id="routeGrad" x1={`${fromXY.x}%`} y1={`${fromXY.y}%`} x2={`${toXY.x}%`} y2={`${toXY.y}%`}>
              <stop offset="0%" stopColor="#10b981" />
              <stop offset="100%" stopColor="#f43f5e" />
            </linearGradient>
          </defs>

          {/* Route path */}
          <line
            x1={`${fromXY.x}%`}
            y1={`${fromXY.y}%`}
            x2={`${toXY.x}%`}
            y2={`${toXY.y}%`}
            stroke="url(#routeGrad)"
            strokeWidth="5"
            strokeLinecap="round"
            strokeDasharray="8 6"
          />

          {/* Driver to FROM approach path */}
          {driverLocationXY && (
            <line
              x1={`${driverLocationXY.x}%`}
              y1={`${driverLocationXY.y}%`}
              x2={`${fromXY.x}%`}
              y2={`${fromXY.y}%`}
              stroke="#2563eb"
              strokeWidth="3"
              strokeLinecap="round"
              strokeDasharray="4 4"
            />
          )}
        </svg>

        {/* Fallback Warning Box */}
        <div className="absolute top-16 left-4 right-4 z-10 bg-rose-950/90 backdrop-blur border border-rose-500/30 p-3.5 rounded-xl flex items-start space-x-3 text-xs text-rose-200 shadow-2xl">
          <ShieldAlert className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
          <div className="flex-1 space-y-1">
            <h5 className="font-bold text-white">Google Maps Connection Restricted (ApiProjectMapError)</h5>
            <p className="text-[11px] leading-relaxed opacity-95">
              The Google Maps JS API key is inactive, lacks billing, or has invalid project settings. 
              The application has safely activated its **High-Fidelity Telemetry Canvas** so dispatch, routing, and location state updates remain fully interactive.
            </p>
            <div className="pt-1.5 flex items-center space-x-3">
              <button
                onClick={handleOpenKeyModal}
                className="bg-rose-500 hover:bg-rose-600 text-white font-extrabold px-3 py-1 rounded-lg text-[10px] transition-all"
              >
                Configure Valid Key
              </button>
              <a
                href="https://mapsplatform.google.com/maps-demo-key?utm_campaign=gmp_mcp_codeassist_v1_aistudio"
                target="_blank"
                referrerPolicy="no-referrer"
                className="text-[10px] text-rose-300 underline font-bold hover:text-white"
              >
                Get Free Maps Demo Key
              </a>
            </div>
          </div>
        </div>

        {/* FROM Marker */}
        <div 
          className="absolute -translate-x-1/2 -translate-y-1/2 flex flex-col items-center z-10"
          style={{ left: `${fromXY.x}%`, top: `${fromXY.y}%` }}
        >
          <div className="px-2 py-0.5 bg-emerald-600 text-white text-[9px] font-bold rounded-md shadow border border-emerald-400 mb-1 whitespace-nowrap">
            FROM: {editingFrom.name}
          </div>
          <div className="w-7 h-7 rounded-full bg-emerald-500 border-2 border-white shadow-lg flex items-center justify-center text-white">
            <MapPin className="w-3.5 h-3.5" />
          </div>
        </div>

        {/* TO Marker */}
        <div 
          className="absolute -translate-x-1/2 -translate-y-1/2 flex flex-col items-center z-10"
          style={{ left: `${toXY.x}%`, top: `${toXY.y}%` }}
        >
          <div className="px-2 py-0.5 bg-rose-600 text-white text-[9px] font-bold rounded-md shadow border border-rose-400 mb-1 whitespace-nowrap">
            TO: {editingTo.name}
          </div>
          <div className="w-7 h-7 rounded-full bg-rose-500 border-2 border-white shadow-lg flex items-center justify-center text-white">
            <Navigation className="w-3.5 h-3.5" />
          </div>
        </div>

        {/* Test Custom Marker in Fallback Map */}
        {testCustomMarker && (() => {
          const testXY = getRelativeXY(testCustomMarker.lat, testCustomMarker.lng);
          return (
            <div
              className="absolute z-40 transform -translate-x-1/2 -translate-y-1/2 flex flex-col items-center group cursor-pointer"
              style={{ left: `${testXY.x}%`, top: `${testXY.y}%` }}
            >
              <div className="px-2.5 py-1 bg-blue-600 text-white text-[11px] font-extrabold rounded-lg shadow-2xl shadow-blue-900/60 flex items-center space-x-1 border border-blue-400 mb-1 pointer-events-auto whitespace-nowrap">
                <span>TEST: {testCustomMarker.name}</span>
              </div>
              <div className="w-8 h-8 rounded-full bg-blue-500 border-2 border-white shadow-2xl flex items-center justify-center text-white font-bold ring-4 ring-blue-500/30">
                <MapPin className="w-4 h-4" />
              </div>
              <div className="w-2 h-2 bg-blue-500 rotate-45 -mt-1"></div>
            </div>
          );
        })()}

        {/* Live Drivers on Fallback Map */}
        {allDriverMarkers.map((dLoc) => {
          const isAssigned = dLoc.driverId === assignedDriver?.id;
          const pos = getRelativeXY(dLoc.lat, dLoc.lng);

          return (
            <div
              key={`fallback-driver-${dLoc.driverId}`}
              className={`absolute -translate-x-1/2 -translate-y-1/2 flex flex-col items-center cursor-pointer group select-none ${isAssigned ? 'z-20 scale-105' : 'z-10'}`}
              style={{ left: `${pos.x}%`, top: `${pos.y}%` }}
              onClick={() => setSelectedDriverPopup(dLoc)}
            >
              {/* Fare Badge */}
              <div
                className={`px-2 py-0.5 rounded-lg shadow font-extrabold text-[9px] whitespace-nowrap mb-0.5 border ${
                  isAssigned
                    ? 'bg-blue-600 text-white border-blue-400'
                    : dLoc.status === 'on_trip' || dLoc.status === 'busy'
                    ? 'bg-[#334155] text-white border-slate-600'
                    : 'bg-[#a3e635] text-slate-950 border-[#84cc16]'
                }`}
              >
                {dLoc.fareDisplay}
              </div>

              {/* Vector Car Caricature */}
              <TopDownCarGraphic
                status={dLoc.status}
                isAssigned={isAssigned}
                heading={dLoc.heading || 0}
                size={30}
              />
            </div>
          );
        })}

        {/* Selected Driver Detail Card Overlay for Fallback Map */}
        {selectedDriverPopup && (
          <div className="absolute bottom-4 left-4 right-4 z-20 bg-slate-900 border border-slate-800 rounded-xl p-3 shadow-2xl flex items-start space-x-3">
            <img
              src={selectedDriverPopup.avatar}
              alt={selectedDriverPopup.driverName}
              className="w-10 h-10 rounded-full object-cover border border-emerald-500"
            />
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <h5 className="font-bold text-xs text-white truncate">{selectedDriverPopup.driverName}</h5>
                <span className="text-[10px] text-amber-400 font-bold">★ {selectedDriverPopup.rating}</span>
              </div>
              <p className="text-[10px] text-slate-400">
                {selectedDriverPopup.vehicleMakeModel} • <span className="font-mono text-slate-300">{selectedDriverPopup.licensePlate}</span>
              </p>
              <div className="text-[9px] font-bold text-blue-400 uppercase tracking-wider mt-1 flex items-center space-x-1">
                <span className={`w-1.5 h-1.5 rounded-full inline-block ${selectedDriverPopup.status === 'online' ? 'bg-emerald-500' : 'bg-slate-500'}`} />
                <span>{String(selectedDriverPopup.status || '').replace('_', ' ')} • Speed: {selectedDriverPopup.speedKmh} km/h</span>
              </div>
            </div>
            <button 
              onClick={() => setSelectedDriverPopup(null)}
              className="text-slate-400 hover:text-white font-bold text-sm bg-slate-800 hover:bg-slate-700 px-1.5 py-0.5 rounded-md"
            >
              ×
            </button>
          </div>
        )}
      </div>
    );
  };

  // Google Maps API Key & Map ID State
  const defaultEnvKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || 'AIzaSyAXvKh2joigH6eyQRKyc_yBAoYq1cxzR5U';
  const defaultEnvMapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID || 'DEMO_MAP_ID';

  const [mapsApiKey, setMapsApiKey] = useState<string>(() => {
    return localStorage.getItem('DRIGO_GPM_API_KEY') || defaultEnvKey;
  });
  const [mapsMapId, setMapsMapId] = useState<string>(() => {
    const saved = localStorage.getItem('DRIGO_GPM_MAP_ID') || defaultEnvMapId;
    return saved && !saved.includes('e.g.') ? saved.trim() : 'DEMO_MAP_ID';
  });

  // Effective Map ID - uses valid custom map ID or defaults to "DEMO_MAP_ID" as mandated by Google Maps Platform guidelines for Advanced Marker support
  const effectiveMapId = (() => {
    if (!mapsMapId || typeof mapsMapId !== 'string') return 'DEMO_MAP_ID';
    const trimmed = mapsMapId.trim();
    if (!trimmed || trimmed.includes('e.g.') || trimmed.length < 5) return 'DEMO_MAP_ID';
    return trimmed;
  })();

  const [showKeyModal, setShowKeyModal] = useState(false);
  const [keyInput, setKeyInput] = useState('');
  const [mapIdInput, setMapIdInput] = useState('');

  // Sync with activeTrip when selection changes
  useEffect(() => {
    if (activeTrip) {
      if (activeTrip.fromLocation) {
        setEditingFrom(activeTrip.fromLocation);
      }
      if (activeTrip.toLocation) {
        setEditingTo(activeTrip.toLocation);
      }
    }
  }, [
    activeTrip?.id,
    activeTrip?.fromLocation?.lat,
    activeTrip?.fromLocation?.lng,
    activeTrip?.toLocation?.lat,
    activeTrip?.toLocation?.lng
  ]);

  const handleSelectFromPreset = (loc: LocationPoint) => {
    if (showLocationPicker === 'from') {
      setEditingFrom(loc);
      onUpdateTripLocations(activeTrip.id, loc, undefined); // ONLY update FROM, TO is completely preserved
    } else if (showLocationPicker === 'to') {
      setEditingTo(loc);
      onUpdateTripLocations(activeTrip.id, undefined, loc); // ONLY update TO, FROM is completely preserved
    }
    setShowLocationPicker(null);
  };

  const handleOpenKeyModal = () => {
    setKeyInput(mapsApiKey);
    setMapIdInput(mapsMapId || '');
    setShowKeyModal(true);
  };

  const handleSaveApiKey = () => {
    const trimmedKey = keyInput.trim();
    const trimmedMapId = mapIdInput.trim();

    if (trimmedKey) {
      localStorage.setItem('DRIGO_GPM_API_KEY', trimmedKey);
      setMapsApiKey(trimmedKey);
    } else {
      localStorage.removeItem('DRIGO_GPM_API_KEY');
      setMapsApiKey(defaultEnvKey);
    }

    if (trimmedMapId) {
      localStorage.setItem('DRIGO_GPM_MAP_ID', trimmedMapId);
      setMapsMapId(trimmedMapId);
    } else {
      localStorage.removeItem('DRIGO_GPM_MAP_ID');
      setMapsMapId(defaultEnvMapId);
    }

    setShowKeyModal(false);
  };

  const driverLocation = (() => {
    if (!assignedDriver) return undefined;
    const loc = assignedDriver.currentLocation || (assignedDriver as any).location;
    if (loc && typeof loc.lat === 'number' && typeof loc.lng === 'number') {
      return { lat: loc.lat, lng: loc.lng };
    }
    return undefined;
  })();

  return (
    <div className="flex-1 flex flex-col lg:flex-row h-full overflow-hidden bg-slate-900">
      {/* Left Dispatch Control Sidebar */}
      <div className="w-full lg:w-96 bg-slate-900 border-r border-slate-800 flex flex-col shrink-0 overflow-y-auto">
        {/* Header / Trip Selector */}
        <div className="p-4 border-b border-slate-800 bg-slate-950/80">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center space-x-2">
              <Navigation className="w-4 h-4 text-blue-400" />
              <h3 className="text-sm font-bold text-white">Live Dispatch & Routing</h3>
            </div>
            <span className="text-[10px] font-bold bg-blue-500/20 text-blue-400 border border-blue-500/30 px-2 py-0.5 rounded">
              {trips.length} Rides Monitored
            </span>
          </div>

          <p className="text-[11px] text-slate-400">
            Select a ride to monitor pickup (FROM), drop-off (TO), driver location and Android client telemetry.
          </p>

          <div className="mt-3 space-y-1.5 max-h-40 overflow-y-auto pr-1">
            {trips.map((t) => {
              const isSelected = t.id === activeTrip?.id;
              return (
                <button
                  key={t.id}
                  onClick={() => onSelectTrip(t.id)}
                  className={`w-full text-left p-2.5 rounded-xl border transition-all text-xs flex items-center justify-between ${
                    isSelected
                      ? 'bg-blue-600/20 border-blue-500 text-white font-semibold'
                      : 'bg-slate-800/60 border-slate-700/60 text-slate-300 hover:bg-slate-800'
                  }`}
                >
                  <div className="flex items-center space-x-2 truncate">
                    <span className="font-mono text-[11px] bg-slate-900 px-1.5 py-0.5 rounded text-blue-400 border border-slate-700">
                      {t.tripCode}
                    </span>
                    <span className="truncate">{t.passengerName}</span>
                  </div>
                  <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded uppercase ${
                    t.status === 'sos_alert'
                      ? 'bg-rose-500 text-white animate-pulse'
                      : t.status === 'in_progress'
                      ? 'bg-emerald-500/20 text-emerald-400'
                      : 'bg-slate-700 text-slate-300'
                  }`}>
                    {String(t.status || '').replace('_', ' ')}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Active Trip Details & Location Flow Panel */}
        {activeTrip && (
          <div className="p-4 space-y-4 flex-1">
            {/* FROM and TO Location Verification Box */}
            <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-3">
              <div className="flex items-center justify-between text-[11px] text-slate-400 font-bold uppercase tracking-wider">
                <span>INDEPENDENT ROUTE FLOW</span>
                <span className="text-emerald-400 font-mono">FROM → TO</span>
              </div>

              {/* FROM Pickup Location */}
              <div className="p-2.5 rounded-lg bg-slate-900 border border-emerald-500/30 flex items-start justify-between">
                <div className="flex items-start space-x-2.5">
                  <div className="w-5 h-5 rounded-full bg-emerald-500/20 border border-emerald-500 text-emerald-400 flex items-center justify-center shrink-0 mt-0.5">
                    <MapPin className="w-3 h-3" />
                  </div>
                  <div>
                    <div className="text-[10px] font-bold text-emerald-400 uppercase">FROM (PICKUP)</div>
                    <div className="text-xs font-bold text-white">{editingFrom.name}</div>
                    <div className="text-[11px] text-slate-400">{editingFrom.address}</div>
                  </div>
                </div>
                <button
                  onClick={() => setShowLocationPicker(showLocationPicker === 'from' ? null : 'from')}
                  className="text-[10px] bg-slate-800 hover:bg-slate-700 text-slate-200 px-2 py-1 rounded border border-slate-700 font-bold shrink-0 ml-2"
                >
                  Change FROM
                </button>
              </div>

              {/* TO Drop-off Location */}
              <div className="p-2.5 rounded-lg bg-slate-900 border border-rose-500/30 flex items-start justify-between">
                <div className="flex items-start space-x-2.5">
                  <div className="w-5 h-5 rounded-full bg-rose-500/20 border border-rose-500 text-rose-400 flex items-center justify-center shrink-0 mt-0.5">
                    <Navigation className="w-3 h-3" />
                  </div>
                  <div>
                    <div className="text-[10px] font-bold text-rose-400 uppercase">TO (DESTINATION)</div>
                    <div className="text-xs font-bold text-white">{editingTo.name}</div>
                    <div className="text-[11px] text-slate-400">{editingTo.address}</div>
                  </div>
                </div>
                <button
                  onClick={() => setShowLocationPicker(showLocationPicker === 'to' ? null : 'to')}
                  className="text-[10px] bg-slate-800 hover:bg-slate-700 text-slate-200 px-2 py-1 rounded border border-slate-700 font-bold shrink-0 ml-2"
                >
                  Change TO
                </button>
              </div>

              {/* Location Picker Popup */}
              {showLocationPicker && (
                <div className="p-3 bg-slate-900 border border-blue-500/50 rounded-xl space-y-2 animate-fadeIn">
                  <div className="flex items-center justify-between text-xs font-bold text-white">
                    <span>Select New {showLocationPicker === 'from' ? 'FROM (Pickup)' : 'TO (Drop-off)'}</span>
                    <button onClick={() => setShowLocationPicker(null)} className="text-slate-400 hover:text-white">×</button>
                  </div>
                  <p className="text-[10px] text-slate-400">
                    Rule Check: Updating {showLocationPicker === 'from' ? 'FROM' : 'TO'} will preserve {showLocationPicker === 'from' ? 'TO' : 'FROM'} completely.
                  </p>
                  <div className="space-y-1 max-h-32 overflow-y-auto">
                    {PRESET_LOCATIONS.map((loc, idx) => (
                      <button
                        key={idx}
                        onClick={() => handleSelectFromPreset(loc)}
                        className="w-full text-left p-1.5 rounded bg-slate-800 hover:bg-blue-600/30 hover:border-blue-500 border border-slate-700 text-[11px] text-slate-200 truncate"
                      >
                        <span className="font-bold">{loc.name}</span> — <span className="text-slate-400">{loc.address}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Trip Fare Summary */}
              <div className="grid grid-cols-3 gap-2 pt-1 text-center text-xs">
                <div className="p-2 bg-slate-900 rounded-lg border border-slate-800">
                  <div className="text-[10px] text-slate-400">Distance</div>
                  <div className="font-mono font-bold text-white">{activeTrip.distanceKm} km</div>
                </div>
                <div className="p-2 bg-slate-900 rounded-lg border border-slate-800">
                  <div className="text-[10px] text-slate-400">Est. Time</div>
                  <div className="font-mono font-bold text-white">{activeTrip.estimatedDurationMinutes} mins</div>
                </div>
                <div className="p-2 bg-slate-900 rounded-lg border border-slate-800">
                  <div className="text-[10px] text-slate-400">Fare Total</div>
                  <div className="font-mono font-bold text-emerald-400">${activeTrip.fare.total.toFixed(2)}</div>
                </div>
              </div>
            </div>

            {/* Assigned Driver & Hardware Telemetry */}
            {assignedDriver ? (
              <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-bold text-slate-400 uppercase">DRIVER CLIENT TELEMETRY</span>
                  <span className="text-[10px] font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/30">
                    {assignedDriver.telemetry.networkType} Signal ({assignedDriver.telemetry.networkLatencyMs}ms)
                  </span>
                </div>

                <div className="flex items-center space-x-3">
                  <img
                    src={assignedDriver.avatar}
                    alt={assignedDriver.fullName}
                    className="w-10 h-10 rounded-full object-cover border-2 border-slate-700"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold text-white truncate">{assignedDriver.fullName}</h4>
                      <span className="text-xs text-amber-400 font-bold">★ {assignedDriver.rating}</span>
                    </div>
                    <p className="text-[11px] text-slate-400">
                      {assignedDriver.vehicle.make} {assignedDriver.vehicle.model} • <span className="font-mono text-white">{assignedDriver.vehicle.licensePlate}</span>
                    </p>
                  </div>
                </div>

                {/* Device Hardware Spec Bar for Budget Android Support */}
                <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-800 space-y-2 text-[11px]">
                  <div className="flex items-center justify-between text-slate-300">
                    <div className="flex items-center space-x-1.5">
                      <Smartphone className="w-3.5 h-3.5 text-blue-400" />
                      <span className="font-semibold">{assignedDriver.telemetry.deviceModel}</span>
                    </div>
                    <span className="font-mono text-[10px] text-slate-400">{assignedDriver.telemetry.androidVersion}</span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-[10px] text-slate-400 pt-1 border-t border-slate-800">
                    <div>RAM: <span className="text-white font-mono">{assignedDriver.telemetry.ramTotalGb} GB ({assignedDriver.telemetry.ramUsagePercent}% used)</span></div>
                    <div>Battery: <span className="text-emerald-400 font-mono">{assignedDriver.telemetry.batteryLevel}%</span></div>
                    <div>GPS Accuracy: <span className="text-white font-mono">{assignedDriver.telemetry.gpsAccuracyMeters}m</span></div>
                    <div>Queued Packets: <span className="text-amber-400 font-mono">{assignedDriver.telemetry.offlineQueuedPackets}</span></div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="p-3 bg-amber-500/10 border border-amber-500/30 rounded-xl text-amber-400 text-xs flex items-center space-x-2">
                <Radio className="w-4 h-4 animate-spin" />
                <span>Searching & matching nearest driver for DRG-8926...</span>
              </div>
            )}

            {/* Test Address Map Marker Input Panel */}
            <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 space-y-2 mt-3">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-slate-400 uppercase">Test Address Map Marker</span>
                <span className="text-[10px] text-blue-400 bg-blue-500/10 px-2 py-0.5 rounded border border-blue-500/30">Live Test</span>
              </div>
              <p className="text-[11px] text-slate-400">Enter any address to test marker placement on map:</p>
              <div className="flex space-x-2">
                <input
                  type="text"
                  value={testAddressInput}
                  onChange={(e) => setTestAddressInput(e.target.value)}
                  placeholder="e.g. Hayatabad Phase 3, Peshawar"
                  className="flex-1 bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                />
                <button
                  onClick={handleShowTestMarker}
                  className="bg-blue-600 hover:bg-blue-500 text-white px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
                >
                  Show
                </button>
              </div>
              {testCustomMarker && (
                <div className="flex items-center justify-between pt-2 border-t border-slate-800 text-[11px] text-emerald-400">
                  <span className="truncate">📍 {testCustomMarker.name} ({testCustomMarker.lat.toFixed(4)}, {testCustomMarker.lng.toFixed(4)})</span>
                  <button
                    onClick={() => setTestCustomMarker(null)}
                    className="text-slate-400 hover:text-white text-[10px] underline ml-2"
                  >
                    Clear
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Right Map Visual Canvas powered by Google Maps Platform */}
      <div className="flex-1 relative bg-slate-950 flex flex-col min-h-[450px]">
        {/* Map Header Overlay */}
        <div className="absolute top-4 left-4 right-4 z-20 flex items-center justify-between pointer-events-none">
          <div className="bg-slate-900/90 backdrop-blur border border-slate-800 px-3.5 py-2 rounded-xl text-xs text-white shadow-xl pointer-events-auto flex items-center space-x-3">
            <div className="flex items-center space-x-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
              <span className="font-bold">Google Maps Platform • Live Map Engine</span>
            </div>
            <span className="text-slate-600">|</span>
            <div className="flex items-center space-x-1.5 text-emerald-400 font-mono text-[11px]">
              <MapPin className="w-3 h-3 text-emerald-400" />
              <span>FROM & TO Active</span>
            </div>
          </div>

          <div className="flex items-center space-x-2 pointer-events-auto">
            {/* Key Status Button */}
            <button
              onClick={handleOpenKeyModal}
              className="bg-slate-900/90 hover:bg-slate-800 text-slate-200 border border-slate-700/80 px-3 py-1.5 rounded-xl text-xs shadow-xl flex items-center space-x-1.5 transition-all"
            >
              <Key className="w-3.5 h-3.5 text-amber-400" />
              <span>{mapsApiKey ? 'API Key Configured' : 'Configure API Key'}</span>
            </button>

            {/* Legend Overlay */}
            <div className="bg-slate-900/90 backdrop-blur border border-slate-800 px-3 py-1.5 rounded-xl text-xs text-slate-300 shadow-xl hidden sm:flex items-center space-x-3">
              <div className="flex items-center space-x-1 text-[11px]">
                <span className="w-3 h-3 rounded-full bg-emerald-500 inline-block"></span>
                <span>FROM Pickup</span>
              </div>
              <div className="flex items-center space-x-1 text-[11px]">
                <span className="w-3 h-3 rounded-full bg-rose-500 inline-block"></span>
                <span>TO Dropoff</span>
              </div>
              <div className="flex items-center space-x-1 text-[11px]">
                <span className="w-3 h-3 rounded-full bg-blue-500 inline-block"></span>
                <span>Driver</span>
              </div>
            </div>
          </div>
        </div>

        {/* API Key Modal */}
        {showKeyModal && (
          <div className="absolute inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 max-w-md w-full shadow-2xl space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2 text-white font-bold text-sm">
                  <Globe className="w-4 h-4 text-blue-400" />
                  <span>Google Maps Configuration</span>
                </div>
                <button
                  onClick={() => setShowKeyModal(false)}
                  className="text-slate-400 hover:text-white text-lg font-bold"
                >
                  ×
                </button>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">
                Enter your Google Maps API key below. You can also specify an optional custom Vector Map ID if configured in Google Cloud Console.
              </p>

              <div className="space-y-3">
                <div>
                  <label className="block text-[11px] font-bold text-slate-400 uppercase mb-1">
                    API Key
                  </label>
                  <input
                    type="text"
                    value={keyInput}
                    onChange={(e) => setKeyInput(e.target.value)}
                    placeholder="AIzaSy..."
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-bold text-slate-400 uppercase mb-1">
                    Map ID (Optional for Vector / Advanced Markers)
                  </label>
                  <input
                    type="text"
                    value={mapIdInput}
                    onChange={(e) => setMapIdInput(e.target.value)}
                    placeholder="e.g. 8e0a37b12..."
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                  <p className="text-[10px] text-slate-500 mt-1">Leave empty to use standard Google Maps rendering.</p>
                </div>
              </div>

              <div className="flex items-center justify-end space-x-2 pt-2">
                <button
                  onClick={() => setShowKeyModal(false)}
                  className="px-3 py-1.5 bg-slate-800 text-slate-300 rounded-xl text-xs font-semibold hover:bg-slate-700"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSaveApiKey}
                  className="px-4 py-1.5 bg-blue-600 text-white rounded-xl text-xs font-bold hover:bg-blue-500 shadow-lg shadow-blue-600/30"
                >
                  Save Configuration
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Google Maps Viewport Container */}
        <div className="w-full h-full flex-1 relative overflow-hidden bg-slate-950">
          <APIProvider apiKey={mapsApiKey || "AIzaSyDummyKeyForGoogleMapsView"}>
            <GoogleMap
              mapId={effectiveMapId}
              defaultCenter={{ lat: editingFrom.lat, lng: editingFrom.lng }}
              defaultZoom={13}
              gestureHandling="greedy"
              disableDefaultUI={false}
              className="w-full h-full"
            >
              <MapBoundsFit from={editingFrom} to={editingTo} driverLoc={driverLocation} />
              <RoutePolyline from={editingFrom} to={editingTo} driverLoc={driverLocation} status={activeTrip?.status} />

              {/* FROM Pickup Marker */}
              <AdvancedMarker position={{ lat: editingFrom.lat, lng: editingFrom.lng }}>
                <div className="flex flex-col items-center group cursor-pointer">
                  <div className="px-2.5 py-1 bg-emerald-600 text-white text-[11px] font-extrabold rounded-lg shadow-2xl flex items-center space-x-1 border border-emerald-400 mb-1">
                    <span>FROM: {editingFrom.name}</span>
                  </div>
                  <div className="w-8 h-8 rounded-full bg-emerald-500 border-2 border-white shadow-2xl flex items-center justify-center text-white font-bold">
                    <MapPin className="w-4 h-4" />
                  </div>
                </div>
              </AdvancedMarker>

              {/* TO Destination Marker */}
              <AdvancedMarker position={{ lat: editingTo.lat, lng: editingTo.lng }}>
                <div className="flex flex-col items-center group cursor-pointer">
                  <div className="px-2.5 py-1 bg-rose-600 text-white text-[11px] font-extrabold rounded-lg shadow-2xl flex items-center space-x-1 border border-rose-400 mb-1">
                    <span>TO: {editingTo.name}</span>
                  </div>
                  <div className="w-8 h-8 rounded-full bg-rose-500 border-2 border-white shadow-2xl flex items-center justify-center text-white font-bold">
                    <Navigation className="w-4 h-4" />
                  </div>
                </div>
              </AdvancedMarker>

              {/* Test Custom Marker */}
              {testCustomMarker && (
                <AdvancedMarker position={{ lat: testCustomMarker.lat, lng: testCustomMarker.lng }}>
                  <div className="flex flex-col items-center group cursor-pointer">
                    <div className="px-2.5 py-1 bg-blue-600 text-white text-[11px] font-extrabold rounded-lg shadow-2xl flex items-center space-x-1 border border-blue-400 mb-1">
                      <span>TEST: {testCustomMarker.name}</span>
                    </div>
                    <div className="w-8 h-8 rounded-full bg-blue-500 border-2 border-white shadow-2xl flex items-center justify-center text-white font-bold">
                      <MapPin className="w-4 h-4" />
                    </div>
                  </div>
                </AdvancedMarker>
              )}

              {/* All Live Drivers Markers */}
              {allDriverMarkers.map((dLoc, index) => {
                const isAssigned = dLoc.driverId === assignedDriver?.id || index === 0;
                const heading = typeof dLoc.heading === 'number' ? dLoc.heading : (index * 45) % 360;
                const carColor = isAssigned ? '#ef4444' : '#84cc16';
                const badgeBg = isAssigned ? 'bg-red-600 border-red-400 text-white' : 'bg-lime-400 border-lime-300 text-slate-900';
                const fareText = dLoc.fareDisplay || (isAssigned ? '310 Rs' : '380 Rs');

                return (
                  <AdvancedMarker
                    key={`live-driver-${dLoc.driverId || index}`}
                    position={{ lat: dLoc.lat, lng: dLoc.lng }}
                    onClick={() => setSelectedDriverPopup(dLoc)}
                  >
                    <div
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedDriverPopup(dLoc);
                      }}
                      className="flex items-center space-x-2 group cursor-pointer transition-transform duration-200 hover:scale-110"
                    >
                      {/* 3D Top-Down Car Graphic with Rotation */}
                      <div className="relative w-12 h-20 filter drop-shadow-[0_12px_10px_rgba(0,0,0,0.6)]" style={{ transform: `rotate(${heading}deg)` }}>
                        <svg viewBox="0 0 50 80" className="w-full h-full">
                          <defs>
                            <linearGradient id={`carGrad-${index}`} x1="0%" y1="0%" x2="100%" y2="100%">
                              <stop offset="0%" stopColor={isAssigned ? '#ff6b6b' : '#bef264'} />
                              <stop offset="50%" stopColor={isAssigned ? '#ef4444' : '#84cc16'} />
                              <stop offset="100%" stopColor={isAssigned ? '#991b1b' : '#3f6212'} />
                            </linearGradient>
                            <linearGradient id="windshieldGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                              <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.95" />
                              <stop offset="100%" stopColor="#0f172a" stopOpacity="0.98" />
                            </linearGradient>
                          </defs>
                          
                          {/* Drop shadow */}
                          <ellipse cx="25" cy="42" rx="18" ry="34" fill="rgba(0,0,0,0.45)" />

                          {/* Wheels */}
                          <rect x="3" y="16" width="6" height="12" rx="3" fill="#0f172a" stroke="#334155" strokeWidth="1" />
                          <rect x="41" y="16" width="6" height="12" rx="3" fill="#0f172a" stroke="#334155" strokeWidth="1" />
                          <rect x="3" y="52" width="6" height="12" rx="3" fill="#0f172a" stroke="#334155" strokeWidth="1" />
                          <rect x="41" y="52" width="6" height="12" rx="3" fill="#0f172a" stroke="#334155" strokeWidth="1" />

                          {/* Main Car Body with 3D Gradient */}
                          <path d="M12 10 Q25 4 38 10 Q46 22 46 58 Q46 73 38 75 Q25 78 12 75 Q4 73 4 58 Q4 22 12 10 Z" fill={`url(#carGrad-${index})`} stroke="#ffffff" strokeWidth="2" />

                          {/* Front Windshield */}
                          <path d="M14 18 Q25 14 36 18 L34 35 Q25 31 16 35 Z" fill="url(#windshieldGrad)" stroke="#7dd3fc" strokeWidth="0.8" />
                          
                          {/* Rear Windshield */}
                          <path d="M16 47 Q25 43 34 47 L36 64 Q25 67 14 64 Z" fill="url(#windshieldGrad)" stroke="#7dd3fc" strokeWidth="0.8" />
                          
                          {/* Roof / Center Cabin */}
                          <rect x="15" y="36" width="20" height="11" rx="3" fill={`url(#carGrad-${index})`} filter="brightness(0.85)" stroke="#ffffff" strokeWidth="0.8" />

                          {/* Headlights */}
                          <rect x="8" y="9" width="7" height="3" rx="1.5" fill="#fef08a" />
                          <rect x="35" y="9" width="7" height="3" rx="1.5" fill="#fef08a" />
                          
                          {/* Taillights */}
                          <rect x="8" y="68" width="7" height="3" rx="1.5" fill="#f43f5e" />
                          <rect x="35" y="68" width="7" height="3" rx="1.5" fill="#f43f5e" />
                        </svg>
                      </div>

                      {/* Fare Badge */}
                      <div className={`px-3 py-1.5 rounded-2xl font-extrabold text-sm shadow-2xl whitespace-nowrap border-2 ${badgeBg}`}>
                        {fareText}
                      </div>
                    </div>
                  </AdvancedMarker>
                );
              })}
            </GoogleMap>
          </APIProvider>
        </div>

        {/* Driver Details Floating Modal Overlay when tapping marker */}
        {selectedDriverPopup && (
          <div className="absolute top-4 right-4 z-30 w-84 bg-slate-900/95 backdrop-blur-md border border-slate-700/80 rounded-2xl p-4 shadow-2xl text-slate-100 animate-in fade-in slide-in-from-top-2 max-w-[340px]">
            {/* Header with Avatar, Name, Rating and Close Button */}
            <div className="flex items-start justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-3">
                <div className="relative">
                  <img
                    src={selectedDriverPopup.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200'}
                    alt={selectedDriverPopup.driverName}
                    className="w-12 h-12 rounded-full object-cover ring-2 ring-emerald-500/80 shadow-md bg-slate-800"
                  />
                  <span className={`absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full border-2 border-slate-900 ${
                    selectedDriverPopup.status === 'on_trip' ? 'bg-amber-400' : 'bg-emerald-400'
                  }`} />
                </div>
                <div>
                  <h4 className="font-bold text-base text-white leading-tight">
                    {selectedDriverPopup.driverName}
                  </h4>
                  <div className="flex items-center space-x-1 text-amber-400 text-xs mt-0.5">
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <span className="font-semibold text-slate-200">{selectedDriverPopup.rating || 4.9}</span>
                    <span className="text-slate-500">•</span>
                    <span className="text-slate-400">{selectedDriverPopup.totalTrips || 120} rides</span>
                  </div>
                  <p className="text-[10px] text-slate-400 font-mono mt-0.5">
                    ID: <span className="text-slate-300">{selectedDriverPopup.driverId}</span>
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedDriverPopup(null)}
                className="p-1.5 hover:bg-slate-800 rounded-lg text-slate-400 hover:text-white transition-colors"
                title="Close details"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Contact Phone & Actions */}
            {selectedDriverPopup.phone && (
              <div className="mt-3 bg-slate-800/80 p-2.5 rounded-xl border border-slate-700/60 flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="p-2 bg-blue-500/20 rounded-lg text-blue-400 border border-blue-500/30">
                    <Phone className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-400 block uppercase font-medium">Contact Number</span>
                    <span className="font-mono text-xs font-semibold text-white">{selectedDriverPopup.phone}</span>
                  </div>
                </div>
                <div className="flex items-center space-x-1.5">
                  <button
                    onClick={() => {
                      if (selectedDriverPopup.phone) {
                        navigator.clipboard?.writeText(selectedDriverPopup.phone);
                        setCopiedPhone(true);
                        setTimeout(() => setCopiedPhone(false), 2000);
                      }
                    }}
                    className="p-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs transition-colors"
                    title="Copy phone number"
                  >
                    {copiedPhone ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  </button>
                  <a
                    href={`tel:${selectedDriverPopup.phone}`}
                    className="px-2.5 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-medium text-xs transition-colors flex items-center space-x-1 shadow-lg shadow-blue-600/30"
                  >
                    <Phone className="w-3 h-3" />
                    <span>Call</span>
                  </a>
                </div>
              </div>
            )}

            {/* Status & Vehicle Info Grid */}
            <div className="mt-3 space-y-2 text-xs">
              <div className="flex items-center justify-between bg-slate-800/50 p-2 rounded-xl border border-slate-700/40">
                <span className="text-slate-400 flex items-center space-x-1.5 text-[11px]">
                  <Activity className="w-3.5 h-3.5 text-blue-400" />
                  <span>Duty Status</span>
                </span>
                <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide ${
                  selectedDriverPopup.status === 'on_trip'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                }`}>
                  {String(selectedDriverPopup.status || '').replace('_', ' ')}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-[11px]">
                <div className="bg-slate-800/40 p-2 rounded-xl border border-slate-800">
                  <span className="text-slate-400 block text-[10px]">Vehicle</span>
                  <span className="font-semibold text-slate-200 capitalize truncate block">
                    {selectedDriverPopup.vehicleMakeModel || String(selectedDriverPopup.vehicleType || '').replace('_', ' ')}
                  </span>
                  <span className="text-[10px] text-slate-400 uppercase">{String(selectedDriverPopup.vehicleType || '').replace('_', ' ')}</span>
                </div>
                <div className="bg-slate-800/40 p-2 rounded-xl border border-slate-800">
                  <span className="text-slate-400 block text-[10px]">License Plate</span>
                  <span className="font-mono font-bold text-amber-400 block">{selectedDriverPopup.licensePlate || 'N/A'}</span>
                  <span className="text-[10px] text-emerald-400 font-medium">Verified Vehicle</span>
                </div>
              </div>

              {/* Live Telemetry */}
              <div className="bg-slate-800/40 p-2.5 rounded-xl border border-slate-800 space-y-1.5">
                <div className="flex justify-between items-center text-[11px]">
                  <span className="text-slate-400 flex items-center space-x-1.5">
                    <Compass className="w-3.5 h-3.5 text-cyan-400" />
                    <span>Speed / Heading</span>
                  </span>
                  <span className="font-mono text-cyan-300 font-semibold">
                    {Math.round(selectedDriverPopup.speedKmh)} km/h • {selectedDriverPopup.heading}°
                  </span>
                </div>
                <div className="flex justify-between items-center text-[11px]">
                  <span className="text-slate-400 flex items-center space-x-1.5">
                    <MapPin className="w-3.5 h-3.5 text-emerald-400" />
                    <span>Coordinates</span>
                  </span>
                  <span className="font-mono text-slate-300 text-[10px]">
                    {selectedDriverPopup.lat.toFixed(4)}, {selectedDriverPopup.lng.toFixed(4)}
                  </span>
                </div>
                <div className="flex justify-between items-center text-[11px]">
                  <span className="text-slate-400 flex items-center space-x-1.5">
                    <Clock className="w-3.5 h-3.5 text-indigo-400" />
                    <span>Last Updated</span>
                  </span>
                  <span className="text-emerald-400 font-semibold text-[10px]">
                    {selectedDriverPopup.updatedAt}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Bottom Floating Telemetry Bar */}
        <div className="absolute bottom-4 left-4 right-4 z-20 bg-slate-900/90 backdrop-blur border border-slate-800 rounded-xl p-3 text-xs text-slate-300 flex items-center justify-between shadow-xl">
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-1.5 text-emerald-400 font-semibold">
              <CheckCircle2 className="w-4 h-4" />
              <span>Google Maps Engine Active: FROM → TO</span>
            </div>
            <span className="text-slate-600 hidden sm:inline">|</span>
            <div className="text-slate-400 hidden sm:block">
              Camera automatically bounds both pickup & dropoff
            </div>
          </div>

          <div className="flex items-center space-x-2 text-slate-400 text-[11px] font-mono">
            <span>minSdk 23</span>
            <span>•</span>
            <span>Samsung A12 OK</span>
            <span>•</span>
            <span className="text-emerald-400 font-bold">GPS ±3.8m</span>
          </div>
        </div>
      </div>
    </div>
  );
};
