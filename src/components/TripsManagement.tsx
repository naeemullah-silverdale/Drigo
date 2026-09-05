import React, { useState, useMemo, useEffect } from 'react';
import { Trip, Driver, LiveDriverLocation } from '../types';
import {
  resolvePhysicalAddress,
  reverseGeocodeCoords,
  calculateAccurateRouteDistance,
  fetchRealDrivingRoute
} from '../utils/geoUtils';
import {
  Navigation,
  Search,
  Filter,
  Clock,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  User,
  Car,
  MapPin,
  DollarSign,
  Phone,
  Shield,
  Star,
  ChevronRight,
  Eye,
  Activity,
  Calendar,
  Radio,
  LayoutGrid,
  List,
  ArrowRight,
  Zap,
  ExternalLink,
  RefreshCw,
  Sparkles,
  Globe,
  Copy,
  Check
} from 'lucide-react';

interface TripsManagementProps {
  trips: Trip[];
  drivers: Driver[];
  liveLocations: Record<string, LiveDriverLocation>;
  onSelectTripOnMap?: (tripId: string) => void;
}

export const TripsManagement: React.FC<TripsManagementProps> = ({
  trips,
  drivers,
  liveLocations,
  onSelectTripOnMap,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [trackMapError, setTrackMapError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'table' | 'cards'>('table');
  const [copiedTarget, setCopiedTarget] = useState<'pickup' | 'dropoff' | null>(null);
  const [dynamicAddresses, setDynamicAddresses] = useState<{ pickup?: string; dropoff?: string }>({});
  const [realDrivingRoute, setRealDrivingRoute] = useState<{ distanceKm: number; durationMinutes: number } | null>(null);

  // Dynamic reverse-geocoding and road routing for currently opened ride
  useEffect(() => {
    if (!selectedTrip) {
      setDynamicAddresses({});
      setRealDrivingRoute(null);
      return;
    }
    let active = true;
    setRealDrivingRoute(null);

    const resolvedP = resolvePhysicalAddress(selectedTrip.fromLocation);
    const resolvedD = resolvePhysicalAddress(selectedTrip.toLocation);

    const pLat = resolvedP.lat;
    const pLng = resolvedP.lng;
    const dLat = resolvedD.lat;
    const dLng = resolvedD.lng;

    if (!isNaN(pLat) && !isNaN(pLng)) {
      reverseGeocodeCoords(pLat, pLng).then((res) => {
        if (active && res && res.displayName) {
          setDynamicAddresses((prev) => ({ ...prev, pickup: res.displayName }));
        }
      });
    }

    if (!isNaN(dLat) && !isNaN(dLng)) {
      reverseGeocodeCoords(dLat, dLng).then((res) => {
        if (active && res && res.displayName) {
          setDynamicAddresses((prev) => ({ ...prev, dropoff: res.displayName }));
        }
      });
    }

    // Fetch turn-by-turn road route distance and duration
    if (!isNaN(pLat) && !isNaN(pLng) && !isNaN(dLat) && !isNaN(dLng)) {
      fetchRealDrivingRoute(pLat, pLng, dLat, dLng).then((route) => {
        if (active && route) {
          setRealDrivingRoute(route);
        }
      });
    }

    return () => {
      active = false;
    };
  }, [selectedTrip]);

  const formatDuration = (mins: number) => {
    if (mins >= 60) {
      const h = Math.floor(mins / 60);
      const m = mins % 60;
      return m > 0 ? `${h}h ${m}m` : `${h}h`;
    }
    return `${mins} mins`;
  };

  const copyCoords = (lat: number, lng: number, target: 'pickup' | 'dropoff') => {
    const text = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
    try {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text);
      }
    } catch {
      // fallback
    }
    setCopiedTarget(target);
    setTimeout(() => setCopiedTarget(null), 2000);
  };

  const handleTrackOnMap = (trip: Trip) => {
    const pickupLat = trip.fromLocation?.lat;
    const pickupLng = trip.fromLocation?.lng;
    const destLat = trip.toLocation?.lat;
    const destLng = trip.toLocation?.lng;

    if (
      typeof pickupLat !== 'number' || isNaN(pickupLat) ||
      typeof pickupLng !== 'number' || isNaN(pickupLng) ||
      typeof destLat !== 'number' || isNaN(destLat) ||
      typeof destLng !== 'number' || isNaN(destLng)
    ) {
      setTrackMapError('Route coordinates are unavailable for this ride.');
      return;
    }

    setTrackMapError(null);
    const url = `https://www.google.com/maps/dir/?api=1&origin=${pickupLat},${pickupLng}&destination=${destLat},${destLng}&travelmode=driving`;
    window.open(url, '_blank');
  };

  // Status counts for scannable filter tabs
  const counts = useMemo(() => {
    const res = {
      all: trips.length,
      searching: 0,
      offer_received: 0,
      driver_coming: 0,
      driver_arrived: 0,
      in_trip: 0,
      completed: 0,
      cancelled: 0,
    };
    trips.forEach((t) => {
      const s = t.status;
      if (s === 'requested' || s === 'searching' || s === 'matching') res.searching++;
      else if (s === 'offer_received') res.offer_received++;
      else if (s === 'accepted' || s === 'driver_arriving') res.driver_coming++;
      else if (s === 'driver_arrived') res.driver_arrived++;
      else if (s === 'in_progress' || s === 'sos_alert') res.in_trip++;
      else if (s === 'completed') res.completed++;
      else if (s === 'cancelled') res.cancelled++;
    });
    return res;
  }, [trips]);

  // Filter trips
  const filteredTrips = useMemo(() => {
    return trips.filter((t) => {
      const q = (searchTerm || '').trim().toLowerCase();
      const matchesSearch =
        !q ||
        (t.tripCode || '').toLowerCase().includes(q) ||
        (t.id || '').toLowerCase().includes(q) ||
        (t.passengerName || '').toLowerCase().includes(q) ||
        (t.passengerPhone || '').toLowerCase().includes(q) ||
        (t.driverName || '').toLowerCase().includes(q) ||
        (t.driverPhone || '').toLowerCase().includes(q) ||
        (t.vehiclePlate || '').toLowerCase().includes(q) ||
        (t.fromLocation?.name || '').toLowerCase().includes(q) ||
        (t.toLocation?.name || '').toLowerCase().includes(q) ||
        (t.fromLocation?.address || '').toLowerCase().includes(q) ||
        (t.toLocation?.address || '').toLowerCase().includes(q);

      if (!matchesSearch) return false;

      if (statusFilter === 'all') return true;
      if (statusFilter === 'searching') {
        return t.status === 'requested' || t.status === 'searching' || t.status === 'matching';
      }
      if (statusFilter === 'offer_received') {
        return t.status === 'offer_received';
      }
      if (statusFilter === 'driver_coming') {
        return t.status === 'driver_arriving' || t.status === 'accepted';
      }
      if (statusFilter === 'driver_arrived') {
        return t.status === 'driver_arrived';
      }
      if (statusFilter === 'in_trip') {
        return t.status === 'in_progress' || t.status === 'sos_alert';
      }
      if (statusFilter === 'completed') {
        return t.status === 'completed';
      }
      if (statusFilter === 'cancelled') {
        return t.status === 'cancelled';
      }
      return t.status === statusFilter;
    });
  }, [trips, searchTerm, statusFilter]);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'requested':
      case 'searching':
      case 'matching':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/10 text-amber-700 border border-amber-500/20">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500 mr-1.5 animate-pulse"></span>
            SEARCHING
          </span>
        );
      case 'offer_received':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-purple-500/10 text-purple-700 border border-purple-500/20">
            <Radio className="w-3 h-3 mr-1 text-purple-600 animate-spin" />
            OFFERS RECEIVED
          </span>
        );
      case 'accepted':
      case 'driver_arriving':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-blue-500/10 text-blue-700 border border-blue-500/20">
            <Car className="w-3 h-3 mr-1 text-blue-600" />
            DRIVER COMING
          </span>
        );
      case 'driver_arrived':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-indigo-500/10 text-indigo-700 border border-indigo-500/20">
            <MapPin className="w-3 h-3 mr-1 text-indigo-600" />
            DRIVER ARRIVED
          </span>
        );
      case 'in_progress':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-700 border border-emerald-500/20">
            <Activity className="w-3 h-3 mr-1 text-emerald-600 animate-pulse" />
            IN TRIP
          </span>
        );
      case 'sos_alert':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-extrabold bg-rose-600 text-white animate-pulse">
            <AlertTriangle className="w-3 h-3 mr-1 text-white" />
            SOS ALERT
          </span>
        );
      case 'completed':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-600 text-white">
            <CheckCircle2 className="w-3 h-3 mr-1 text-white" />
            COMPLETED
          </span>
        );
      case 'cancelled':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-semibold bg-slate-200 text-slate-700 border border-slate-300">
            <XCircle className="w-3 h-3 mr-1 text-slate-500" />
            CANCELLED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-semibold bg-slate-100 text-slate-700">
            {String(status || 'PENDING').toUpperCase()}
          </span>
        );
    }
  };

  const getVehicleBadge = (type: string = 'car') => {
    const t = (type || 'car').toLowerCase();
    if (t.includes('bike')) {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-extrabold bg-amber-50 text-amber-800 border border-amber-200">
          🏍️ BIKE
        </span>
      );
    }
    if (t.includes('auto') || t.includes('rickshaw')) {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-50 text-emerald-800 border border-emerald-200">
          🛺 AUTO
        </span>
      );
    }
    if (t.includes('comfort') || t.includes('ac')) {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-extrabold bg-purple-50 text-purple-800 border border-purple-200">
          ✨ COMFORT
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-extrabold bg-blue-50 text-blue-800 border border-blue-200">
        🚗 CAR / RIDE
      </span>
    );
  };

  const getDriverLocation = (driverId?: string) => {
    if (!driverId) return null;
    return liveLocations[driverId] || null;
  };

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50/50">
      {/* Header & Stats Banner */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-sm">
        <div className="space-y-1">
          <div className="flex items-center space-x-2.5">
            <h1 className="text-xl font-bold text-slate-900 tracking-tight">Ride Requests & Dispatch Management</h1>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 mr-1.5 animate-pulse"></span>
              Live Firebase Sync
            </span>
          </div>
          <p className="text-xs text-slate-500 max-w-2xl">
            Real-time control tower for passenger ride requests, dynamic driver bidding, fare negotiation, and active dispatches across Pakistan.
          </p>
        </div>

        {/* Live Stat Chips */}
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="bg-slate-50 border border-slate-200 px-3 py-2 rounded-xl text-center min-w-[90px]">
            <div className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">Total</div>
            <div className="text-base font-black text-slate-900">{counts.all}</div>
          </div>
          <div className="bg-amber-50 border border-amber-200 px-3 py-2 rounded-xl text-center min-w-[90px]">
            <div className="text-[10px] font-bold text-amber-700 uppercase tracking-wider">Searching</div>
            <div className="text-base font-black text-amber-900">{counts.searching}</div>
          </div>
          <div className="bg-purple-50 border border-purple-200 px-3 py-2 rounded-xl text-center min-w-[90px]">
            <div className="text-[10px] font-bold text-purple-700 uppercase tracking-wider">Bidding</div>
            <div className="text-base font-black text-purple-900">{counts.offer_received}</div>
          </div>
          <div className="bg-blue-50 border border-blue-200 px-3 py-2 rounded-xl text-center min-w-[90px]">
            <div className="text-[10px] font-bold text-blue-700 uppercase tracking-wider">Dispatched</div>
            <div className="text-base font-black text-blue-900">{counts.driver_coming + counts.driver_arrived}</div>
          </div>
          <div className="bg-emerald-50 border border-emerald-200 px-3 py-2 rounded-xl text-center min-w-[90px]">
            <div className="text-[10px] font-bold text-emerald-700 uppercase tracking-wider">In Trip</div>
            <div className="text-base font-black text-emerald-900">{counts.in_trip}</div>
          </div>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by Request ID, Passenger, Driver, Location, Phone..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs font-medium text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>

        <div className="flex items-center space-x-2">
          {/* Status Filter Pills */}
          <div className="flex items-center space-x-1 overflow-x-auto pb-1 md:pb-0">
            {[
              { id: 'all', label: 'All', count: counts.all },
              { id: 'searching', label: 'Searching', count: counts.searching },
              { id: 'offer_received', label: 'Offers', count: counts.offer_received },
              { id: 'driver_coming', label: 'Dispatched', count: counts.driver_coming },
              { id: 'driver_arrived', label: 'Arrived', count: counts.driver_arrived },
              { id: 'in_trip', label: 'In Trip', count: counts.in_trip },
              { id: 'completed', label: 'Completed', count: counts.completed },
              { id: 'cancelled', label: 'Cancelled', count: counts.cancelled },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setStatusFilter(tab.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all flex items-center space-x-1.5 ${
                  statusFilter === tab.id
                    ? 'bg-slate-900 text-white shadow-sm'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                <span>{tab.label}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full font-bold ${
                    statusFilter === tab.id ? 'bg-slate-700 text-white' : 'bg-slate-200 text-slate-700'
                  }`}
                >
                  {tab.count}
                </span>
              </button>
            ))}
          </div>

          {/* View Mode Switcher */}
          <div className="flex items-center border border-slate-200 rounded-lg p-0.5 bg-slate-50 shrink-0 ml-2">
            <button
              onClick={() => setViewMode('table')}
              className={`p-1.5 rounded-md text-xs font-semibold flex items-center transition-colors ${
                viewMode === 'table' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-900'
              }`}
              title="Table View"
            >
              <List className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={() => setViewMode('cards')}
              className={`p-1.5 rounded-md text-xs font-semibold flex items-center transition-colors ${
                viewMode === 'cards' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-900'
              }`}
              title="Cards / Dispatch View"
            >
              <LayoutGrid className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Main Content: Table or Cards */}
      {viewMode === 'table' ? (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Request ID</th>
                  <th className="py-3.5 px-4">Passenger</th>
                  <th className="py-3.5 px-4">Driver</th>
                  <th className="py-3.5 px-4">Route</th>
                  <th className="py-3.5 px-4">Category</th>
                  <th className="py-3.5 px-4">Fare & Offers</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4">Time</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs font-medium text-slate-700">
                {filteredTrips.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="py-16 text-center text-slate-400">
                      <div className="max-w-md mx-auto space-y-3">
                        <div className="w-12 h-12 rounded-2xl bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                          <Car className="w-6 h-6" />
                        </div>
                        <div className="text-sm font-bold text-slate-800">No Ride Requests Found</div>
                        <p className="text-xs text-slate-500 leading-relaxed">
                          {trips.length === 0
                            ? 'The Drigo Real-Time Database listener is connected. As soon as passengers book rides via the Android Passenger app, they will appear here instantly.'
                            : 'No rides matched your active filter or search query. Try switching to "All" or clearing the search box.'}
                        </p>
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredTrips.map((t) => {
                    const bidsCount = Array.isArray(t.bids) ? t.bids.length : 0;
                    return (
                      <tr
                        key={t.id}
                        onClick={() => setSelectedTrip(t)}
                        className="hover:bg-slate-50/80 transition-colors cursor-pointer group"
                      >
                        <td className="py-3.5 px-4">
                          <div className="font-mono font-bold text-slate-900">{t.tripCode || t.id.slice(0, 8)}</div>
                          {t.notes && <div className="text-[10px] text-slate-400 truncate max-w-[120px]">{t.notes}</div>}
                        </td>
                        <td className="py-3.5 px-4">
                          <div className="flex items-center space-x-2.5">
                            <img
                              src={t.passengerAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}
                              alt=""
                              className="w-7 h-7 rounded-full object-cover ring-1 ring-slate-200 shrink-0"
                            />
                            <div>
                              <div className="font-bold text-slate-900">{t.passengerName || 'Passenger'}</div>
                              <div className="text-[10px] text-slate-400">{t.passengerPhone || 'No Phone'}</div>
                            </div>
                          </div>
                        </td>
                        <td className="py-3.5 px-4">
                          {t.driverName ? (
                            <div className="flex items-center space-x-2.5">
                              <img
                                src={t.driverAvatar || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100'}
                                alt=""
                                className="w-7 h-7 rounded-full object-cover ring-1 ring-slate-200 shrink-0"
                              />
                              <div>
                                <div className="font-bold text-slate-900">{t.driverName}</div>
                                <div className="text-[10px] text-slate-400">{t.vehiclePlate || t.driverPhone || 'Assigned'}</div>
                              </div>
                            </div>
                          ) : bidsCount > 0 ? (
                            <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
                              <Zap className="w-3 h-3 mr-1 text-purple-600" />
                              {bidsCount} Driver {bidsCount === 1 ? 'Bid' : 'Bids'}
                            </span>
                          ) : (
                            <span className="text-slate-400 italic text-[11px]">Searching for Drivers...</span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 max-w-xs space-y-1">
                          {(() => {
                            const pRes = resolvePhysicalAddress(t.fromLocation);
                            const dRes = resolvePhysicalAddress(t.toLocation);
                            const rMetrics = calculateAccurateRouteDistance(
                              pRes.lat,
                              pRes.lng,
                              dRes.lat,
                              dRes.lng,
                              t.distanceKm,
                              t.estimatedDurationMinutes
                            );
                            return (
                              <>
                                <div className="space-y-0.5">
                                  <div className="truncate font-semibold text-slate-800 text-xs flex items-center space-x-1" title={pRes.physicalAddress}>
                                    <span className="w-3.5 h-3.5 rounded-full bg-blue-100 text-blue-700 text-[9px] font-black flex items-center justify-center shrink-0">A</span>
                                    <span className="truncate">{pRes.name}</span>
                                  </div>
                                  <div className="text-[10px] font-mono text-slate-400 pl-4.5">
                                    {pRes.lat.toFixed(4)}, {pRes.lng.toFixed(4)}
                                  </div>
                                </div>
                                <div className="space-y-0.5 pt-0.5 border-t border-slate-100">
                                  <div className="truncate font-semibold text-slate-800 text-xs flex items-center space-x-1" title={dRes.physicalAddress}>
                                    <span className="w-3.5 h-3.5 rounded-full bg-emerald-100 text-emerald-700 text-[9px] font-black flex items-center justify-center shrink-0">B</span>
                                    <span className="truncate">{dRes.name}</span>
                                  </div>
                                  <div className="text-[10px] font-mono text-slate-400 pl-4.5">
                                    {dRes.lat.toFixed(4)}, {dRes.lng.toFixed(4)}
                                  </div>
                                </div>
                                <div className="pt-1 flex items-center space-x-1.5 text-[10px] text-slate-600 font-semibold border-t border-slate-100">
                                  <span className="bg-slate-100 text-slate-800 font-mono px-1.5 py-0.5 rounded font-bold">
                                    {rMetrics.distanceKm} km
                                  </span>
                                  <span>•</span>
                                  <span>~{formatDuration(rMetrics.durationMinutes)}</span>
                                  {rMetrics.isIntercity && (
                                    <span className="bg-amber-50 text-amber-700 text-[9px] font-bold px-1 py-0.5 rounded border border-amber-200">
                                      Intercity
                                    </span>
                                  )}
                                </div>
                              </>
                            );
                          })()}
                        </td>
                        <td className="py-3.5 px-4">
                          {getVehicleBadge(t.vehicleType)}
                        </td>
                        <td className="py-3.5 px-4">
                          <div className="font-black text-slate-900">
                            Rs. {t.fare?.total || t.offerAmount || 0}
                          </div>
                          {t.counterOfferAmount && t.counterOfferAmount !== (t.fare?.total || t.offerAmount) && (
                            <div className="text-[10px] text-purple-600 font-bold">
                              Counter: Rs. {t.counterOfferAmount}
                            </div>
                          )}
                          <div className="text-[10px] text-slate-400 uppercase font-semibold">
                            {t.paymentMethod || 'Cash'}
                          </div>
                        </td>
                        <td className="py-3.5 px-4">
                          {getStatusBadge(t.status)}
                        </td>
                        <td className="py-3.5 px-4 text-slate-500 font-mono text-[11px] whitespace-nowrap">
                          {t.requestedAt || 'Just now'}
                        </td>
                        <td className="py-3.5 px-4 text-right whitespace-nowrap">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedTrip(t);
                            }}
                            className="p-1.5 bg-slate-100 hover:bg-blue-600 hover:text-white rounded-lg text-slate-600 transition-colors inline-flex items-center space-x-1 text-xs font-semibold"
                          >
                            <Eye className="w-3.5 h-3.5" />
                            <span>Details</span>
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        /* Cards / Dispatch Grid View */
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filteredTrips.length === 0 ? (
            <div className="col-span-full py-16 bg-white rounded-2xl border border-slate-200/80 text-center text-slate-400">
              <Car className="w-10 h-10 mx-auto mb-2 opacity-40 text-slate-400" />
              <div className="text-sm font-bold text-slate-800">No Ride Requests Found</div>
              <p className="text-xs text-slate-500 mt-1">Listening to live Firebase Realtime Database updates...</p>
            </div>
          ) : (
            filteredTrips.map((t) => {
              const bidsCount = Array.isArray(t.bids) ? t.bids.length : 0;
              return (
                <div
                  key={t.id}
                  onClick={() => setSelectedTrip(t)}
                  className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-sm hover:shadow-md hover:border-blue-400 transition-all cursor-pointer flex flex-col justify-between space-y-4"
                >
                  {/* Card Header */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono font-black text-xs text-slate-900 px-2 py-0.5 bg-slate-100 rounded">
                        {t.tripCode || t.id.slice(0, 8)}
                      </span>
                      {getVehicleBadge(t.vehicleType)}
                    </div>
                    <div>{getStatusBadge(t.status)}</div>
                  </div>

                  {/* Route Visualizer */}
                  {(() => {
                    const pRes = resolvePhysicalAddress(t.fromLocation);
                    const dRes = resolvePhysicalAddress(t.toLocation);
                    const routeMetrics = calculateAccurateRouteDistance(
                      pRes.lat,
                      pRes.lng,
                      dRes.lat,
                      dRes.lng,
                      t.distanceKm,
                      t.estimatedDurationMinutes
                    );
                    return (
                      <div className="space-y-2 text-xs bg-slate-50/90 p-3 rounded-xl border border-slate-200/80">
                        <div className="flex items-start space-x-2.5">
                          <div className="w-5 h-5 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-[10px] font-black shrink-0 mt-0.5 shadow-2xs">
                            A
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="font-extrabold text-slate-900 truncate">{pRes.name}</div>
                            <div className="text-[11px] text-slate-500 truncate">{pRes.physicalAddress}</div>
                            <div className="text-[10px] font-mono font-bold text-blue-700 mt-0.5">
                              Lat: {pRes.lat.toFixed(4)}, Lng: {pRes.lng.toFixed(4)}
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center space-x-2 pl-2 my-1">
                          <div className="border-l-2 border-dashed border-slate-300 h-4"></div>
                          <span className="text-[10px] font-bold text-slate-700 bg-white border border-slate-200 px-2 py-0.5 rounded-full shadow-2xs">
                            Trip Route: {routeMetrics.distanceKm} km • ~{formatDuration(routeMetrics.durationMinutes)}
                            {routeMetrics.isIntercity ? ' (Intercity)' : ''}
                          </span>
                        </div>

                        <div className="flex items-start space-x-2.5">
                          <div className="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center text-[10px] font-black shrink-0 mt-0.5 shadow-2xs">
                            B
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="font-extrabold text-slate-900 truncate">{dRes.name}</div>
                            <div className="text-[11px] text-slate-500 truncate">{dRes.physicalAddress}</div>
                            <div className="text-[10px] font-mono font-bold text-emerald-700 mt-0.5">
                              Lat: {dRes.lat.toFixed(4)}, Lng: {dRes.lng.toFixed(4)}
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })()}

                  {/* Passenger & Driver Quick Info */}
                  <div className="flex items-center justify-between text-xs pt-1 border-t border-slate-100">
                    <div className="flex items-center space-x-2">
                      <img
                        src={t.passengerAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}
                        alt=""
                        className="w-7 h-7 rounded-full object-cover ring-1 ring-slate-200"
                      />
                      <div>
                        <div className="font-bold text-slate-900 leading-tight">{t.passengerName || 'Passenger'}</div>
                        <div className="text-[10px] text-slate-400">{t.passengerPhone || 'App User'}</div>
                      </div>
                    </div>

                    <div className="text-right">
                      <div className="font-black text-sm text-slate-900">
                        Rs. {t.fare?.total || t.offerAmount || 0}
                      </div>
                      <div className="text-[10px] text-slate-400 uppercase font-semibold">
                        {t.paymentMethod || 'Cash'}
                      </div>
                    </div>
                  </div>

                  {/* Card Footer: Bids or Driver */}
                  <div className="flex items-center justify-between text-[11px] pt-2 border-t border-slate-100 text-slate-500">
                    {t.driverName ? (
                      <div className="flex items-center space-x-1.5 text-slate-700 font-semibold truncate">
                        <Car className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                        <span className="truncate">{t.driverName} ({t.vehiclePlate || 'Car'})</span>
                      </div>
                    ) : bidsCount > 0 ? (
                      <span className="text-purple-700 font-bold flex items-center space-x-1">
                        <Zap className="w-3 h-3 text-purple-600" />
                        <span>{bidsCount} Driver offers received</span>
                      </span>
                    ) : (
                      <span className="text-amber-600 italic">Broadcasting to nearby drivers...</span>
                    )}

                    <span className="text-slate-400 font-mono text-[10px] shrink-0">{t.requestedAt || 'Live'}</span>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Detailed Trip View Modal/Drawer */}
      {selectedTrip && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-slate-200">
            {/* Modal Header */}
            <div className="p-6 border-b border-slate-200 flex items-center justify-between bg-slate-900 text-white">
              <div className="flex items-center space-x-3">
                <div className="p-2.5 bg-blue-600 rounded-xl">
                  <Car className="w-5 h-5 text-white" />
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-mono font-extrabold text-base tracking-wide">{selectedTrip.tripCode || selectedTrip.id}</span>
                    {getStatusBadge(selectedTrip.status)}
                    {getVehicleBadge(selectedTrip.vehicleType)}
                  </div>
                  <p className="text-xs text-slate-400 mt-0.5">Real-time ride record synced live from Drigo Android apps</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedTrip(null)}
                className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center justify-center font-bold text-lg transition-colors"
              >
                ×
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-6">
              {/* Lifecycle Progress Stepper */}
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <h3 className="text-xs font-bold text-slate-600 uppercase tracking-wider mb-3">Ride Lifecycle Status</h3>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                  <div className="bg-white p-3 rounded-lg border border-slate-200">
                    <div className="text-[10px] text-slate-400 font-semibold">Requested At</div>
                    <div className="font-bold text-slate-900 mt-0.5">{selectedTrip.requestedAt || 'Recorded'}</div>
                  </div>
                  <div className="bg-white p-3 rounded-lg border border-slate-200">
                    <div className="text-[10px] text-slate-400 font-semibold">Started / Picked Up</div>
                    <div className="font-bold text-slate-900 mt-0.5">{selectedTrip.startedAt || 'Pending'}</div>
                  </div>
                  <div className="bg-white p-3 rounded-lg border border-slate-200">
                    <div className="text-[10px] text-slate-400 font-semibold">Completed At</div>
                    <div className="font-bold text-slate-900 mt-0.5">{selectedTrip.completedAt || 'In Progress'}</div>
                  </div>
                  <div className="bg-white p-3 rounded-lg border border-slate-200">
                    <div className="text-[10px] text-slate-400 font-semibold">Cancellation / SOS</div>
                    <div className="font-bold text-rose-600 mt-0.5">
                      {selectedTrip.cancelledAt ? `Cancelled (${selectedTrip.cancelledBy || 'User'})` : selectedTrip.sosAlert?.isTriggered ? '🚨 SOS Alert' : 'Normal'}
                    </div>
                  </div>
                </div>
              </div>

              {/* Passenger & Driver Cards */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Passenger Info */}
                <div className="bg-white p-4 rounded-xl border border-slate-200 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Passenger Details</span>
                    <span className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded font-bold">⭐ {selectedTrip.passengerRating || 4.9}</span>
                  </div>
                  <div className="flex items-center space-x-3">
                    <img
                      src={selectedTrip.passengerAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}
                      alt=""
                      className="w-12 h-12 rounded-xl object-cover ring-2 ring-blue-500/20"
                    />
                    <div>
                      <div className="font-bold text-sm text-slate-900">{selectedTrip.passengerName || 'Passenger'}</div>
                      <div className="text-xs text-slate-500 flex items-center space-x-1 mt-0.5">
                        <Phone className="w-3 h-3 text-slate-400" />
                        {selectedTrip.passengerPhone ? (
                          <a href={`tel:${selectedTrip.passengerPhone}`} className="text-blue-600 hover:underline">
                            {selectedTrip.passengerPhone}
                          </a>
                        ) : (
                          <span>No phone on file</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Driver Info */}
                <div className="bg-white p-4 rounded-xl border border-slate-200 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Assigned Driver</span>
                    {selectedTrip.driverRating && (
                      <span className="text-xs bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded font-bold">⭐ {selectedTrip.driverRating}</span>
                    )}
                  </div>
                  {selectedTrip.driverName ? (
                    <div className="flex items-center space-x-3">
                      <img
                        src={selectedTrip.driverAvatar || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100'}
                        alt=""
                        className="w-12 h-12 rounded-xl object-cover ring-2 ring-emerald-500/20"
                      />
                      <div>
                        <div className="font-bold text-sm text-slate-900">{selectedTrip.driverName}</div>
                        <div className="text-xs text-slate-500 flex items-center space-x-1 mt-0.5">
                          <Phone className="w-3 h-3 text-slate-400" />
                          {selectedTrip.driverPhone ? (
                            <a href={`tel:${selectedTrip.driverPhone}`} className="text-emerald-600 hover:underline">
                              {selectedTrip.driverPhone}
                            </a>
                          ) : (
                            <span>No direct phone</span>
                          )}
                        </div>
                        {selectedTrip.vehiclePlate && (
                          <div className="text-[10px] font-mono font-bold text-slate-600 mt-0.5">
                            🚗 {selectedTrip.vehicleModel || 'Vehicle'} • {selectedTrip.vehiclePlate}
                          </div>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div className="py-4 text-center text-xs text-slate-400 italic">
                      No driver assigned yet (Ride is currently in bidding or matching state)
                    </div>
                  )}
                </div>
              </div>

              {/* Dynamic Driver Bids Panel (inDrive Style Negotiation) */}
              {Array.isArray(selectedTrip.bids) && selectedTrip.bids.length > 0 && (
                <div className="bg-purple-50/50 p-4 rounded-xl border border-purple-200 space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <Zap className="w-4 h-4 text-purple-600" />
                      <h3 className="text-xs font-bold text-purple-900 uppercase tracking-wider">
                        Driver Bids & Counter-Offers ({selectedTrip.bids.length})
                      </h3>
                    </div>
                    <span className="text-[10px] text-purple-600 font-semibold">inDrive dynamic bidding system</span>
                  </div>

                  <div className="divide-y divide-purple-100 bg-white rounded-lg border border-purple-100 overflow-hidden">
                    {selectedTrip.bids.map((bid, idx) => (
                      <div key={bid.driverId || idx} className="p-3 flex items-center justify-between text-xs hover:bg-purple-50/30">
                        <div className="flex items-center space-x-3">
                          <div className="w-8 h-8 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-xs">
                            {bid.driverName ? bid.driverName.charAt(0) : 'D'}
                          </div>
                          <div>
                            <div className="font-bold text-slate-900 flex items-center space-x-1.5">
                              <span>{bid.driverName || 'Driver'}</span>
                              <span className="text-[10px] text-amber-600 font-semibold">★ {bid.driverRating || '4.8'}</span>
                            </div>
                            <div className="text-[10px] text-slate-500">
                              {bid.vehicleModel || 'Car'} • ETA: {bid.etaMinutes || 5} mins
                            </div>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="font-black text-sm text-purple-900">Rs. {bid.offerAmount || bid.bidAmount || 0}</div>
                          <span
                            className={`text-[9px] font-bold px-1.5 py-0.5 rounded ${
                              bid.status === 'accepted'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-slate-100 text-slate-600'
                            }`}
                          >
                            {String(bid.status || 'Pending').toUpperCase()}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Fare & Negotiation Breakdown */}
              <div className="bg-white p-4 rounded-xl border border-slate-200 space-y-3">
                <h3 className="text-xs font-bold text-slate-600 uppercase tracking-wider">Fare & Payment Breakdown</h3>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
                    <div className="text-[10px] text-slate-400 font-semibold">Passenger Initial Offer</div>
                    <div className="font-bold text-slate-900 text-sm mt-0.5">Rs. {selectedTrip.offerAmount || selectedTrip.fare?.total || 0}</div>
                  </div>
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
                    <div className="text-[10px] text-slate-400 font-semibold">Driver Counter Offer</div>
                    <div className="font-bold text-purple-700 text-sm mt-0.5">{selectedTrip.counterOfferAmount ? `Rs. ${selectedTrip.counterOfferAmount}` : 'None'}</div>
                  </div>
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
                    <div className="text-[10px] text-slate-400 font-semibold">Final Agreed Fare</div>
                    <div className="font-extrabold text-emerald-700 text-sm mt-0.5">Rs. {selectedTrip.fare?.total || selectedTrip.offerAmount || 0}</div>
                  </div>
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
                    <div className="text-[10px] text-slate-400 font-semibold">Payment Method</div>
                    <div className="font-bold text-slate-900 text-sm mt-0.5 uppercase">{selectedTrip.paymentMethod || 'Cash'}</div>
                  </div>
                </div>
              </div>

              {/* Pickup & Destination Coordinates & Physical Address */}
              {(() => {
                const resolvedPickup = resolvePhysicalAddress(selectedTrip.fromLocation);
                const resolvedDropoff = resolvePhysicalAddress(selectedTrip.toLocation);

                const routeMetrics = calculateAccurateRouteDistance(
                  resolvedPickup.lat,
                  resolvedPickup.lng,
                  resolvedDropoff.lat,
                  resolvedDropoff.lng,
                  selectedTrip.distanceKm,
                  selectedTrip.estimatedDurationMinutes
                );

                const activeDistanceKm = realDrivingRoute?.distanceKm ?? routeMetrics.distanceKm;
                const activeDurationMinutes = realDrivingRoute?.durationMinutes ?? routeMetrics.durationMinutes;

                const pickupAddressDisplay = dynamicAddresses.pickup || resolvedPickup.physicalAddress;
                const dropoffAddressDisplay = dynamicAddresses.dropoff || resolvedDropoff.physicalAddress;

                return (
                  <div className="bg-white p-4.5 rounded-xl border border-slate-200 space-y-4 shadow-xs">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                      <div>
                        <h3 className="text-xs font-black text-slate-900 uppercase tracking-wider flex items-center space-x-1.5">
                          <Navigation className="w-3.5 h-3.5 text-blue-600" />
                          <span>Route & Location Coordinates</span>
                        </h3>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          Verified physical addresses and exact GPS latitude & longitude coordinates for dispatch & navigation.
                        </p>
                      </div>
                      <div className="flex items-center space-x-2 text-[11px] font-bold text-slate-800 bg-blue-50/90 px-3 py-1.5 rounded-lg self-start sm:self-auto border border-blue-200/80 shadow-2xs">
                        <Navigation className="w-3.5 h-3.5 text-blue-600" />
                        <span className="font-extrabold">{activeDistanceKm} km route</span>
                        <span>•</span>
                        <span>~{formatDuration(activeDurationMinutes)} transit</span>
                        {routeMetrics.isIntercity && (
                          <span className="bg-amber-100 text-amber-800 text-[10px] font-bold px-1.5 py-0.5 rounded">
                            Intercity Motorway
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="space-y-3">
                      {/* Pickup Location Box */}
                      <div className="p-3.5 bg-blue-50/50 rounded-xl border border-blue-100 space-y-2.5">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            <div className="w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center text-xs font-black shadow-xs">
                              A
                            </div>
                            <div>
                              <span className="text-xs font-black text-blue-950 uppercase tracking-wide">Pickup Location</span>
                              <span className="ml-2 text-[10px] font-bold text-blue-700 bg-blue-100/80 px-1.5 py-0.5 rounded">Origin Point</span>
                            </div>
                          </div>
                          <div className="flex items-center space-x-1.5">
                            <button
                              type="button"
                              onClick={() => copyCoords(resolvedPickup.lat, resolvedPickup.lng, 'pickup')}
                              className="text-[11px] font-bold text-blue-700 hover:text-blue-900 bg-white hover:bg-blue-50 border border-blue-200 px-2.5 py-1 rounded-lg transition-all flex items-center space-x-1 shadow-2xs cursor-pointer"
                              title="Copy Latitude, Longitude"
                            >
                              {copiedTarget === 'pickup' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3 text-blue-600" />}
                              <span>{copiedTarget === 'pickup' ? 'Copied!' : 'Copy Coords'}</span>
                            </button>
                            <a
                              href={`https://www.google.com/maps?q=${resolvedPickup.lat},${resolvedPickup.lng}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-[11px] font-bold text-slate-700 hover:text-slate-900 bg-white hover:bg-slate-50 border border-slate-200 px-2 py-1 rounded-lg transition-all flex items-center space-x-1 shadow-2xs"
                              title="Open in Google Maps"
                            >
                              <ExternalLink className="w-3 h-3 text-slate-500" />
                              <span>View Map</span>
                            </a>
                          </div>
                        </div>

                        {/* Physical Address Block */}
                        <div className="pl-8 space-y-1">
                          <div className="text-[10px] font-extrabold text-blue-900/70 uppercase tracking-wider">
                            Physical Address & Landmark
                          </div>
                          <div className="text-sm font-black text-slate-900">
                            {resolvedPickup.name}
                          </div>
                          <div className="text-xs text-slate-600 flex items-start space-x-1.5">
                            <MapPin className="w-3.5 h-3.5 text-blue-600 shrink-0 mt-0.5" />
                            <span className="leading-snug">{pickupAddressDisplay}</span>
                          </div>
                        </div>

                        {/* Coordinates Block */}
                        <div className="pl-8 pt-1">
                          <div className="inline-flex flex-wrap items-center gap-2 bg-white border border-blue-200/80 px-3 py-1.5 rounded-lg text-xs font-mono font-bold text-slate-800 shadow-2xs">
                            <div className="flex items-center space-x-1 text-blue-700">
                              <Globe className="w-3.5 h-3.5" />
                              <span className="text-[11px] font-sans font-bold uppercase">GPS:</span>
                            </div>
                            <span className="text-slate-600">
                              Lat: <strong className="text-slate-950 font-mono">{resolvedPickup.lat.toFixed(6)}</strong>
                            </span>
                            <span className="text-slate-300">•</span>
                            <span className="text-slate-600">
                              Lng: <strong className="text-slate-950 font-mono">{resolvedPickup.lng.toFixed(6)}</strong>
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Route Connector */}
                      <div className="flex items-center justify-center -my-1 relative z-10">
                        <div className="bg-slate-900 text-white px-4 py-1.5 rounded-full text-[11px] font-black flex items-center space-x-2 shadow-md">
                          <span className="text-blue-400 font-bold">↓</span>
                          <span>
                            Trip Route: {activeDistanceKm} km
                            {routeMetrics.isIntercity ? ' (Intercity Highway)' : ''} • ~{formatDuration(activeDurationMinutes)} transit
                          </span>
                          <span className="text-emerald-400 font-bold">↓</span>
                        </div>
                      </div>

                      {/* Drop-off Destination Box */}
                      <div className="p-3.5 bg-emerald-50/50 rounded-xl border border-emerald-100 space-y-2.5">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            <div className="w-6 h-6 rounded-full bg-emerald-600 text-white flex items-center justify-center text-xs font-black shadow-xs">
                              B
                            </div>
                            <div>
                              <span className="text-xs font-black text-emerald-950 uppercase tracking-wide">Drop-off Destination</span>
                              <span className="ml-2 text-[10px] font-bold text-emerald-700 bg-emerald-100/80 px-1.5 py-0.5 rounded">Destination Point</span>
                            </div>
                          </div>
                          <div className="flex items-center space-x-1.5">
                            <button
                              type="button"
                              onClick={() => copyCoords(resolvedDropoff.lat, resolvedDropoff.lng, 'dropoff')}
                              className="text-[11px] font-bold text-emerald-700 hover:text-emerald-900 bg-white hover:bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-lg transition-all flex items-center space-x-1 shadow-2xs cursor-pointer"
                              title="Copy Latitude, Longitude"
                            >
                              {copiedTarget === 'dropoff' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3 text-emerald-600" />}
                              <span>{copiedTarget === 'dropoff' ? 'Copied!' : 'Copy Coords'}</span>
                            </button>
                            <a
                              href={`https://www.google.com/maps?q=${resolvedDropoff.lat},${resolvedDropoff.lng}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-[11px] font-bold text-slate-700 hover:text-slate-900 bg-white hover:bg-slate-50 border border-slate-200 px-2 py-1 rounded-lg transition-all flex items-center space-x-1 shadow-2xs"
                              title="Open in Google Maps"
                            >
                              <ExternalLink className="w-3 h-3 text-slate-500" />
                              <span>View Map</span>
                            </a>
                          </div>
                        </div>

                        {/* Physical Address Block */}
                        <div className="pl-8 space-y-1">
                          <div className="text-[10px] font-extrabold text-emerald-900/70 uppercase tracking-wider">
                            Physical Address & Landmark
                          </div>
                          <div className="text-sm font-black text-slate-900">
                            {resolvedDropoff.name}
                          </div>
                          <div className="text-xs text-slate-600 flex items-start space-x-1.5">
                            <MapPin className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                            <span className="leading-snug">{dropoffAddressDisplay}</span>
                          </div>
                        </div>

                        {/* Coordinates Block */}
                        <div className="pl-8 pt-1">
                          <div className="inline-flex flex-wrap items-center gap-2 bg-white border border-emerald-200/80 px-3 py-1.5 rounded-lg text-xs font-mono font-bold text-slate-800 shadow-2xs">
                            <div className="flex items-center space-x-1 text-emerald-700">
                              <Globe className="w-3.5 h-3.5" />
                              <span className="text-[11px] font-sans font-bold uppercase">GPS:</span>
                            </div>
                            <span className="text-slate-600">
                              Lat: <strong className="text-slate-950 font-mono">{resolvedDropoff.lat.toFixed(6)}</strong>
                            </span>
                            <span className="text-slate-300">•</span>
                            <span className="text-slate-600">
                              Lng: <strong className="text-slate-950 font-mono">{resolvedDropoff.lng.toFixed(6)}</strong>
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })()}

              {/* Live Driver Telemetry & Map Action */}
              <div className="bg-white p-4 rounded-xl border border-slate-200 space-y-3">
                <h3 className="text-xs font-bold text-slate-600 uppercase tracking-wider">Telemetry & Live Navigation</h3>
                <div className="text-xs text-slate-700 bg-slate-50 p-3 rounded-lg border border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-3">
                  <div>
                    <span className="font-bold">Driver ID:</span> {selectedTrip.driverId || 'Not assigned'}
                    {selectedTrip.driverId && getDriverLocation(selectedTrip.driverId) ? (
                      <div className="text-emerald-600 font-semibold mt-1">
                        🟢 Live GPS: ({getDriverLocation(selectedTrip.driverId)?.lat.toFixed(4)}, {getDriverLocation(selectedTrip.driverId)?.lng.toFixed(4)}) • Speed: {getDriverLocation(selectedTrip.driverId)?.speedKmh || 0} km/h
                      </div>
                    ) : (
                      <div className="text-slate-400 mt-1">Telemetry standing by for driver GPS beacon</div>
                    )}
                  </div>
                  <div className="flex items-center space-x-2 shrink-0">
                    <button
                      onClick={() => handleTrackOnMap(selectedTrip)}
                      className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold shadow-sm inline-flex items-center space-x-1.5"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      <span>Google Directions</span>
                    </button>
                    {onSelectTripOnMap && (
                      <button
                        onClick={() => {
                          onSelectTripOnMap(selectedTrip.id);
                          setSelectedTrip(null);
                        }}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-100 rounded-lg font-bold shadow-sm border border-slate-700"
                      >
                        Open on Admin Map
                      </button>
                    )}
                  </div>
                </div>
                {trackMapError && (
                  <div className="text-rose-600 font-bold text-xs bg-rose-50 p-2.5 rounded-lg border border-rose-200">
                    {trackMapError}
                  </div>
                )}
              </div>

              {/* Feedback, Rating, or Cancellation */}
              {(selectedTrip.rating || selectedTrip.cancellationReason || selectedTrip.sosAlert?.isTriggered) && (
                <div className="bg-amber-50/60 p-4 rounded-xl border border-amber-200 space-y-2 text-xs">
                  <h3 className="text-xs font-bold text-amber-900 uppercase tracking-wider">Ride Notes & Incident Feedback</h3>
                  {selectedTrip.rating && (
                    <div className="flex items-center space-x-2 text-amber-900">
                      <span className="font-bold">Rating:</span>
                      <span className="flex items-center text-amber-600 font-bold">
                        {'★'.repeat(selectedTrip.rating)} ({selectedTrip.rating}/5.0)
                      </span>
                    </div>
                  )}
                  {selectedTrip.review && (
                    <div className="text-amber-800 italic">"{selectedTrip.review}"</div>
                  )}
                  {selectedTrip.cancellationReason && (
                    <div className="text-rose-700 font-medium">
                      <span className="font-bold">Cancellation Reason:</span> {selectedTrip.cancellationReason} (by {selectedTrip.cancelledBy || 'user'})
                    </div>
                  )}
                  {selectedTrip.sosAlert?.isTriggered && (
                    <div className="text-rose-700 font-bold bg-rose-50 p-2 rounded border border-rose-200">
                      🚨 SOS Triggered on this trip! Reason: {selectedTrip.sosAlert.reason || 'Emergency assistance requested'}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-slate-200 bg-slate-50 flex items-center justify-end space-x-3">
              <button
                onClick={() => setSelectedTrip(null)}
                className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-xl font-bold text-xs"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
