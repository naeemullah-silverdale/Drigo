import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { LocationPoint, LiveDriverLocation } from '../types';

interface LeafletMapProps {
  from: LocationPoint;
  to: LocationPoint;
  driverLocation: LiveDriverLocation | null;
  allDriverMarkers: LiveDriverLocation[];
  onSelectDriver: (driver: LiveDriverLocation) => void;
  tripStatus?: string;
}

export const LeafletMap: React.FC<LeafletMapProps> = ({
  from,
  to,
  driverLocation,
  allDriverMarkers,
  onSelectDriver,
  tripStatus = 'IN_TRIP',
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const layerGroupRef = useRef<L.LayerGroup | null>(null);

  // Initialize Leaflet Map once on mount with Peshawar bounds
  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const defaultLat = (from && typeof from.lat === 'number' && !isNaN(from.lat)) ? from.lat : 34.0151;
      const defaultLng = (from && typeof from.lng === 'number' && !isNaN(from.lng)) ? from.lng : 71.5249;

      // Peshawar bounding box restriction
      const peshawarBounds = L.latLngBounds(
        [33.6, 71.0], // Southwest
        [34.5, 72.0]  // Northeast
      );

      const map = L.map(mapContainerRef.current, {
        zoomControl: true,
        attributionControl: false,
        maxBounds: peshawarBounds,
        maxBoundsViscosity: 1.0,
        minZoom: 11,
        maxZoom: 18,
      }).setView([defaultLat, defaultLng], 13);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
      }).addTo(map);

      const layerGroup = L.layerGroup().addTo(map);
      layerGroupRef.current = layerGroup;
      mapInstanceRef.current = map;

      // Invalidate size after mount to prevent gray tiles
      setTimeout(() => {
        map.invalidateSize();
      }, 150);
    }

    const handleResize = () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.invalidateSize();
      }
    };
    window.addEventListener('resize', handleResize);

    // Cleanup on unmount
    return () => {
      window.removeEventListener('resize', handleResize);
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        layerGroupRef.current = null;
      }
    };
  }, []);

  // Update markers, routes, and bounds whenever props change
  useEffect(() => {
    const map = mapInstanceRef.current;
    const layerGroup = layerGroupRef.current;
    if (!map || !layerGroup) return;

    layerGroup.clearLayers();

    const fLat = (from && typeof from.lat === 'number' && !isNaN(from.lat)) ? from.lat : 34.0151;
    const fLng = (from && typeof from.lng === 'number' && !isNaN(from.lng)) ? from.lng : 71.5249;
    const tLat = (to && typeof to.lat === 'number' && !isNaN(to.lat)) ? to.lat : 34.0044;
    const tLng = (to && typeof to.lng === 'number' && !isNaN(to.lng)) ? to.lng : 71.5369;

    const boundsPoints: [number, number][] = [
      [fLat, fLng],
      [tLat, tLng]
    ];

    // 1. FROM Pickup Marker
    const fromIcon = L.divIcon({
      className: 'leaflet-custom-div-icon',
      html: `<div style="background: #059669; color: white; padding: 6px 12px; border-radius: 8px; font-weight: 800; font-size: 11px; white-space: nowrap; border: 2px solid #34d399; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.5);">📍 FROM: ${from?.name || 'Pickup'}</div>`,
      iconSize: [150, 38],
      iconAnchor: [75, 38]
    });
    L.marker([fLat, fLng], { icon: fromIcon }).addTo(layerGroup);

    // 2. TO Destination Marker
    const toIcon = L.divIcon({
      className: 'leaflet-custom-div-icon',
      html: `<div style="background: #e11d48; color: white; padding: 6px 12px; border-radius: 8px; font-weight: 800; font-size: 11px; white-space: nowrap; border: 2px solid #fb7185; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.5);">🏁 TO: ${to?.name || 'Destination'}</div>`,
      iconSize: [150, 38],
      iconAnchor: [75, 38]
    });
    L.marker([tLat, tLng], { icon: toIcon }).addTo(layerGroup);

    // 3. Driver Location & Approach Route
    if (driverLocation && typeof driverLocation.lat === 'number' && typeof driverLocation.lng === 'number') {
      boundsPoints.push([driverLocation.lat, driverLocation.lng]);

      const driverIcon = L.divIcon({
        className: 'leaflet-custom-div-icon',
        html: `<div style="background: #1e293b; color: white; padding: 5px 10px; border-radius: 16px; font-weight: 800; font-size: 11px; border: 2px solid #3b82f6; box-shadow: 0 6px 12px rgba(0,0,0,0.5); display: flex; align-items: center; gap: 6px;"><span>🚗</span><span style="color: #60a5fa;">${driverLocation.fareDisplay || 'Driver'}</span></div>`,
        iconSize: [120, 38],
        iconAnchor: [60, 19]
      });
      L.marker([driverLocation.lat, driverLocation.lng], { icon: driverIcon })
        .addTo(layerGroup)
        .on('click', () => onSelectDriver(driverLocation));

      if (tripStatus === 'DRIVER_COMING' || tripStatus === 'DRIVER_ARRIVED' || tripStatus === 'ACCEPTED') {
        L.polyline(
          [
            [driverLocation.lat, driverLocation.lng],
            [fLat, fLng]
          ],
          { color: '#10b981', weight: 4, opacity: 0.85, dashArray: '6, 6' }
        ).addTo(layerGroup);
      }
    }

    // 4. Main Trip Route (Pickup -> Destination)
    const tripRoute: [number, number][] = [
      [fLat, fLng],
      [tLat, tLng]
    ];
    L.polyline(tripRoute, { color: '#3b82f6', weight: 6, opacity: 0.9, dashArray: '10, 6' }).addTo(layerGroup);

    // OSRM routing
    fetch(`https://router.project-osrm.org/route/v1/driving/${fLng},${fLat};${tLng},${tLat}?overview=full&geometries=geojson`)
      .then(res => res.json())
      .then(data => {
        if (data && data.routes && data.routes[0] && data.routes[0].geometry) {
          const coords = data.routes[0].geometry.coordinates.map((c: [number, number]) => [c[1], c[0]] as [number, number]);
          if (coords.length > 0 && layerGroupRef.current) {
            L.polyline(coords, { color: '#3b82f6', weight: 6, opacity: 0.9 }).addTo(layerGroupRef.current);
          }
        }
      })
      .catch(() => {});

    // 5. All Other Live Drivers from Firebase live_driver_locations
    allDriverMarkers.forEach((d) => {
      if (d && typeof d.lat === 'number' && typeof d.lng === 'number' && d.driverId !== driverLocation?.driverId) {
        boundsPoints.push([d.lat, d.lng]);
        const otherIcon = L.divIcon({
          className: 'leaflet-custom-div-icon',
          html: `<div style="background: #0f172a; color: white; padding: 4px 8px; border-radius: 12px; font-weight: 700; font-size: 10px; border: 2px solid #64748b; box-shadow: 0 4px 6px rgba(0,0,0,0.4); display: flex; align-items: center; gap: 4px;">🚗 ${d.fareDisplay || d.driverName || 'Driver'}</div>`,
          iconSize: [100, 30],
          iconAnchor: [50, 15]
        });
        const m = L.marker([d.lat, d.lng], { icon: otherIcon }).addTo(layerGroup);
        m.on('click', () => onSelectDriver(d));
      }
    });

    if (boundsPoints.length > 0) {
      const bounds = L.latLngBounds(boundsPoints);
      map.fitBounds(bounds, { padding: [60, 60], maxZoom: 15 });
    }

    setTimeout(() => {
      map.invalidateSize();
    }, 100);

  }, [from, to, driverLocation, allDriverMarkers, tripStatus]);

  return <div ref={mapContainerRef} className="w-full h-full absolute inset-0 z-10" style={{ minHeight: '400px' }} />;
};
