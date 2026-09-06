import React, { useState, useEffect } from 'react';
import { Trip } from '../../types';
import {
  ShieldAlert,
  Phone,
  Radio,
  CheckCircle,
  AlertTriangle,
  Volume2,
  VolumeX,
  Play,
  Pause,
  MapPin,
  ExternalLink,
  ShieldCheck,
  Smartphone,
  BatteryCharging,
  Wifi,
  Siren,
  Send,
  UserX,
  Search,
  CheckCircle2,
  Lock,
  FileCheck
} from 'lucide-react';

interface SosIncidentInspectorProps {
  trips: Trip[];
  selectedTrip: Trip | null;
  onSelectTrip: (trip: Trip) => void;
  onResolveSosAlert: (tripId: string, notes: string) => void;
}

export const SosIncidentInspector: React.FC<SosIncidentInspectorProps> = ({
  trips,
  selectedTrip,
  onSelectTrip,
  onResolveSosAlert,
}) => {
  const sosTrips = trips.filter(
    (t) => t.status === 'sos_alert' || (t.sosAlert && t.sosAlert.isTriggered)
  );

  const [searchTerm, setSearchTerm] = useState('');
  const [filterMode, setFilterMode] = useState<'all' | 'active' | 'resolved'>('all');
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [audioTimer, setAudioTimer] = useState(14);
  const [resolutionCategory, setResolutionCategory] = useState('Route Deviation Verified & Resolved');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [actionFeedback, setActionFeedback] = useState<string | null>(null);

  // Filtered list of SOS incidents
  const filteredTrips = sosTrips.filter((trip) => {
    const isResolved = trip.sosAlert?.resolved;
    if (filterMode === 'active' && isResolved) return false;
    if (filterMode === 'resolved' && !isResolved) return false;

    if (!searchTerm.trim()) return true;
    const q = searchTerm.toLowerCase();
    return (
      trip.tripCode.toLowerCase().includes(q) ||
      trip.passengerName.toLowerCase().includes(q) ||
      (trip.driverName && trip.driverName.toLowerCase().includes(q)) ||
      (trip.sosAlert?.reason && trip.sosAlert.reason.toLowerCase().includes(q))
    );
  });

  // Simulated audio playback progress timer
  useEffect(() => {
    let interval: NodeJS.Timeout | null = null;
    if (isPlayingAudio) {
      interval = setInterval(() => {
        setAudioTimer((prev) => (prev >= 45 ? 0 : prev + 1));
      }, 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isPlayingAudio]);

  const triggerActionMessage = (msg: string) => {
    setActionFeedback(msg);
    setTimeout(() => {
      setActionFeedback(null);
    }, 4000);
  };

  const handlePoliceDispatch = () => {
    if (!selectedTrip) return;
    triggerActionMessage(
      `🚨 Live GPS coordinates (${selectedTrip.fromLocation.lat.toFixed(4)}, ${selectedTrip.fromLocation.lng.toFixed(4)}) & Vehicle ${selectedTrip.vehiclePlate || 'N/A'} transmitted to Punjab Police 15 Dispatch Desk.`
    );
  };

  const handleBroadcastSms = () => {
    if (!selectedTrip) return;
    triggerActionMessage(
      `📱 Emergency automated SMS broadcast dispatched to ${selectedTrip.passengerName}'s verified emergency contact.`
    );
  };

  const handleFreezeDriver = () => {
    if (!selectedTrip) return;
    triggerActionMessage(
      `🔒 Driver ${selectedTrip.driverName || 'Captain'} temporarily restricted from taking further rides pending safety review.`
    );
  };

  const handleResolve = () => {
    if (!selectedTrip) return;
    const finalNotes = resolutionNotes.trim()
      ? `[${resolutionCategory}] ${resolutionNotes}`
      : `[${resolutionCategory}] Verified and resolved by safety dispatcher.`;
    onResolveSosAlert(selectedTrip.id, finalNotes);
    setResolutionNotes('');
    triggerActionMessage(`✓ Incident on trip ${selectedTrip.tripCode} marked as RESOLVED.`);
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
      {/* Left Column: SOS Incidents List */}
      <div className="lg:col-span-4 bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-xs flex flex-col space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <ShieldAlert className="w-5 h-5 text-rose-600" />
            <h3 className="text-sm font-bold text-slate-900">Emergency Queue</h3>
          </div>
          <span className="text-[11px] font-mono font-bold bg-slate-100 text-slate-700 px-2.5 py-0.5 rounded-full">
            {filteredTrips.length} Total
          </span>
        </div>

        {/* Search bar */}
        <div className="relative">
          <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search trip code, passenger, driver..."
            className="w-full pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center space-x-1 p-1 bg-slate-100 rounded-xl">
          <button
            onClick={() => setFilterMode('all')}
            className={`flex-1 py-1 text-[11px] font-bold rounded-lg transition-all ${
              filterMode === 'all'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            All ({sosTrips.length})
          </button>
          <button
            onClick={() => setFilterMode('active')}
            className={`flex-1 py-1 text-[11px] font-bold rounded-lg transition-all ${
              filterMode === 'active'
                ? 'bg-rose-600 text-white shadow-xs'
                : 'text-rose-700 hover:text-rose-900'
            }`}
          >
            Active ({sosTrips.filter((t) => !t.sosAlert?.resolved).length})
          </button>
          <button
            onClick={() => setFilterMode('resolved')}
            className={`flex-1 py-1 text-[11px] font-bold rounded-lg transition-all ${
              filterMode === 'resolved'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-emerald-700 hover:text-emerald-900'
            }`}
          >
            Resolved ({sosTrips.filter((t) => t.sosAlert?.resolved).length})
          </button>
        </div>

        {/* List of SOS Trips */}
        <div className="space-y-2.5 overflow-y-auto max-h-[540px] pr-1">
          {filteredTrips.length === 0 ? (
            <div className="p-8 text-center text-slate-400 space-y-2 bg-slate-50 rounded-xl border border-slate-100">
              <ShieldCheck className="w-8 h-8 text-emerald-500 mx-auto" />
              <p className="text-xs font-bold text-slate-700">No Emergency Incidents</p>
              <p className="text-[11px]">
                {filterMode === 'active'
                  ? 'No active emergencies requiring intervention.'
                  : 'No incidents match your filter criteria.'}
              </p>
            </div>
          ) : (
            filteredTrips.map((trip) => {
              const isSelected = selectedTrip?.id === trip.id;
              const isResolved = trip.sosAlert?.resolved;

              return (
                <button
                  key={trip.id}
                  onClick={() => onSelectTrip(trip)}
                  className={`w-full text-left p-3.5 rounded-xl border transition-all space-y-2 ${
                    isSelected
                      ? 'bg-rose-50/90 border-rose-500 ring-2 ring-rose-500/20 shadow-xs'
                      : isResolved
                      ? 'bg-white border-slate-200 hover:bg-slate-50 opacity-80'
                      : 'bg-rose-50/40 border-rose-200 hover:bg-rose-50'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-mono font-bold text-rose-700 bg-rose-100 px-2 py-0.5 rounded text-xs">
                      {trip.tripCode}
                    </span>

                    <span
                      className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase ${
                        isResolved
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-rose-600 text-white animate-pulse'
                      }`}
                    >
                      {isResolved ? 'Resolved' : 'Active SOS'}
                    </span>
                  </div>

                  <div>
                    <div className="text-xs font-bold text-slate-900 flex items-center justify-between">
                      <span>{trip.passengerName} (Passenger)</span>
                      <span className="text-[10px] text-slate-400 font-mono">
                        {trip.sosAlert?.triggeredAt || 'Recent'}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-600 line-clamp-2 mt-0.5">
                      {trip.sosAlert?.reason || 'SOS Emergency Button Activated.'}
                    </p>
                  </div>

                  <div className="flex items-center justify-between text-[10px] text-slate-500 pt-1 border-t border-slate-200/60">
                    <span className="truncate max-w-[140px]">
                      📍 {trip.fromLocation.name}
                    </span>
                    <span className="font-semibold text-slate-700">
                      Driver: {trip.driverName || 'N/A'}
                    </span>
                  </div>
                </button>
              );
            })
          )}
        </div>
      </div>

      {/* Right Column: Selected Incident Command Center */}
      <div className="lg:col-span-8 bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-6 shadow-xs space-y-5">
        {selectedTrip ? (
          <>
            {/* Action Feedback Banner */}
            {actionFeedback && (
              <div className="p-3 bg-slate-900 text-white rounded-xl text-xs flex items-center justify-between animate-fade-in shadow-md">
                <div className="flex items-center space-x-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>{actionFeedback}</span>
                </div>
                <button
                  onClick={() => setActionFeedback(null)}
                  className="text-slate-400 hover:text-white text-xs px-2"
                >
                  ✕
                </button>
              </div>
            )}

            {/* Header / Emergency Info */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-100">
              <div>
                <div className="flex items-center space-x-2.5">
                  <span className="font-mono text-sm font-bold text-rose-700 bg-rose-100 px-3 py-1 rounded-md">
                    {selectedTrip.tripCode}
                  </span>
                  <h3 className="text-base sm:text-lg font-extrabold text-slate-900">
                    Emergency Alert on {selectedTrip.fromLocation.name}
                  </h3>
                </div>
                <p className="text-xs text-slate-500 mt-1">
                  Triggered by{' '}
                  <strong className="text-slate-800 uppercase">
                    {selectedTrip.sosAlert?.triggeredBy || 'Passenger'}
                  </strong>{' '}
                  at{' '}
                  <span className="font-mono">{selectedTrip.sosAlert?.triggeredAt || 'Recent'}</span> • Ride status: <span className="uppercase font-bold text-rose-600">{selectedTrip.status}</span>
                </p>
              </div>

              <div className="flex items-center space-x-2">
                <span
                  className={`px-3.5 py-1.5 rounded-full text-xs font-extrabold uppercase tracking-wide ${
                    selectedTrip.sosAlert?.resolved
                      ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                      : 'bg-rose-600 text-white animate-pulse shadow-sm shadow-rose-600/30'
                  }`}
                >
                  {selectedTrip.sosAlert?.resolved ? 'RESOLVED' : '🚨 ACTIVE EMERGENCY'}
                </span>
              </div>
            </div>

            {/* Live Audio Intercept Player Simulator */}
            <div className="p-4 bg-slate-900 rounded-2xl text-white space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center space-x-2.5">
                  <div className="w-8 h-8 rounded-lg bg-rose-600/30 border border-rose-500 flex items-center justify-center text-rose-400">
                    <Radio className="w-4 h-4 animate-pulse" />
                  </div>
                  <div>
                    <div className="text-xs font-bold text-slate-100 flex items-center space-x-2">
                      <span>In-Cabin Live Audio Stream</span>
                      <span className="bg-rose-500/20 text-rose-300 text-[10px] font-mono px-2 py-0.5 rounded-full border border-rose-500/30">
                        AES-256 ENCRYPTED
                      </span>
                    </div>
                    <div className="text-[11px] text-slate-400">
                      Channel active via passenger phone microphone stream
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => setIsMuted(!isMuted)}
                    className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                    title={isMuted ? 'Unmute' : 'Mute'}
                  >
                    {isMuted ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
                  </button>

                  <button
                    onClick={() => setIsPlayingAudio(!isPlayingAudio)}
                    className="px-3.5 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs flex items-center space-x-1.5 transition-colors shadow-sm"
                  >
                    {isPlayingAudio ? (
                      <>
                        <Pause className="w-3.5 h-3.5" />
                        <span>Pause Feed</span>
                      </>
                    ) : (
                      <>
                        <Play className="w-3.5 h-3.5" />
                        <span>Listen Live</span>
                      </>
                    )}
                  </button>
                </div>
              </div>

              {/* Animated Waveform Visualizer */}
              <div className="p-3 bg-slate-950/70 rounded-xl border border-slate-800 flex items-center justify-between space-x-3">
                <div className="flex items-end space-x-1 h-8 flex-1">
                  {[40, 65, 85, 30, 95, 45, 70, 90, 60, 35, 80, 50, 95, 40, 75, 60, 85, 30, 70, 90, 45, 80].map(
                    (height, idx) => (
                      <div
                        key={idx}
                        className={`w-full rounded-full transition-all duration-300 ${
                          isPlayingAudio
                            ? 'bg-rose-500 animate-pulse'
                            : 'bg-slate-700'
                        }`}
                        style={{
                          height: isPlayingAudio
                            ? `${Math.max(15, (height * ((idx % 3) + 1)) % 100)}%`
                            : '25%',
                        }}
                      />
                    )
                  )}
                </div>

                <div className="font-mono text-xs text-rose-400 font-bold shrink-0">
                  00:{audioTimer < 10 ? `0${audioTimer}` : audioTimer} / LIVE
                </div>
              </div>
            </div>

            {/* Live Telemetry & Speed / Deviation Panel */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
              {/* Route & Deviation */}
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2 text-xs">
                <div className="flex items-center justify-between text-[11px] font-bold text-slate-700">
                  <span className="flex items-center space-x-1.5">
                    <MapPin className="w-3.5 h-3.5 text-rose-600" />
                    <span>Route & Geo-Coordinates</span>
                  </span>
                  <a
                    href={`https://maps.google.com/?q=${selectedTrip.fromLocation.lat},${selectedTrip.fromLocation.lng}`}
                    target="_blank"
                    rel="noreferrer"
                    className="text-rose-600 hover:text-rose-700 flex items-center space-x-1"
                  >
                    <span>Google Maps</span>
                    <ExternalLink className="w-3 h-3" />
                  </a>
                </div>

                <div className="space-y-1 text-[11px]">
                  <div className="flex justify-between">
                    <span className="text-slate-500">Pick-up:</span>
                    <span className="font-semibold text-slate-900 text-right truncate max-w-[180px]">
                      {selectedTrip.fromLocation.name}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Drop-off:</span>
                    <span className="font-semibold text-slate-900 text-right truncate max-w-[180px]">
                      {selectedTrip.toLocation.name}
                    </span>
                  </div>
                  <div className="flex justify-between pt-1 border-t border-slate-200 font-mono text-[10px]">
                    <span className="text-slate-500">GPS Coordinates:</span>
                    <span className="text-slate-700 font-bold">
                      {selectedTrip.fromLocation.lat.toFixed(5)}, {selectedTrip.fromLocation.lng.toFixed(5)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Vehicle & Telemetry */}
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2 text-xs">
                <div className="flex items-center justify-between text-[11px] font-bold text-slate-700">
                  <span className="flex items-center space-x-1.5">
                    <Smartphone className="w-3.5 h-3.5 text-blue-600" />
                    <span>Device & Live Telemetry</span>
                  </span>
                  <span className="px-2 py-0.5 bg-amber-100 text-amber-800 rounded font-mono font-bold text-[10px]">
                    ⚠️ 3.4 km Off-Route
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-2 text-[11px]">
                  <div className="p-2 bg-white rounded-lg border border-slate-100">
                    <div className="text-[10px] text-slate-400 font-bold uppercase">PASSENGER PHONE</div>
                    <div className="font-bold text-slate-800 mt-0.5 truncate">
                      {selectedTrip.passengerTelemetry?.device || 'Android Smartphone'}
                    </div>
                    <div className="text-[10px] text-slate-500 flex items-center space-x-1 mt-0.5">
                      <BatteryCharging className="w-3 h-3 text-emerald-600" />
                      <span>{selectedTrip.passengerTelemetry?.battery || 82}% Battery</span>
                    </div>
                  </div>

                  <div className="p-2 bg-white rounded-lg border border-slate-100">
                    <div className="text-[10px] text-slate-400 font-bold uppercase">DRIVER PHONE</div>
                    <div className="font-bold text-slate-800 mt-0.5 truncate">
                      {selectedTrip.driverTelemetry?.device || 'Samsung A12'}
                    </div>
                    <div className="text-[10px] text-slate-500 flex items-center space-x-1 mt-0.5">
                      <Wifi className="w-3 h-3 text-blue-600" />
                      <span>{selectedTrip.driverTelemetry?.network || '4G LTE'}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Trip Participants (Passenger & Driver Cards) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {/* Passenger Details */}
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    PASSENGER IN DISTRESS
                  </span>
                  <a
                    href={`tel:${selectedTrip.passengerPhone}`}
                    className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-[11px] font-bold flex items-center space-x-1 transition-colors"
                  >
                    <Phone className="w-3 h-3" />
                    <span>Call Passenger</span>
                  </a>
                </div>

                <div>
                  <div className="text-xs font-bold text-slate-900">{selectedTrip.passengerName}</div>
                  <div className="text-xs font-mono text-slate-600">{selectedTrip.passengerPhone}</div>
                  <div className="text-[11px] text-slate-500 mt-1">
                    Emergency Contact: <strong className="text-slate-700">Tariq (Brother) • +92 300 9988776</strong>
                  </div>
                </div>
              </div>

              {/* Driver Details */}
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    ASSIGNED DRIVER & VEHICLE
                  </span>
                  <a
                    href={`tel:${selectedTrip.driverPhone || ''}`}
                    className="px-2.5 py-1 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-[11px] font-bold flex items-center space-x-1 transition-colors"
                  >
                    <Phone className="w-3 h-3" />
                    <span>Call Driver</span>
                  </a>
                </div>

                <div>
                  <div className="text-xs font-bold text-slate-900">{selectedTrip.driverName || 'N/A'}</div>
                  <div className="text-xs font-mono text-slate-600">
                    {selectedTrip.driverPhone || 'N/A'} • Plate: <strong className="text-slate-800">{selectedTrip.vehiclePlate || 'N/A'}</strong>
                  </div>
                  <div className="text-[11px] text-slate-500 mt-1">
                    Vehicle: {selectedTrip.vehicleModel || 'Toyota Corolla'} ({selectedTrip.vehicleColor || 'White'})
                  </div>
                </div>
              </div>
            </div>

            {/* Rapid Escalation Action Buttons */}
            <div className="p-4 bg-slate-100 rounded-xl border border-slate-200/80 space-y-3">
              <div className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                Emergency Dispatch & Response Controls
              </div>

              <div className="flex flex-wrap gap-2.5">
                <button
                  onClick={handlePoliceDispatch}
                  className="px-3.5 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 transition-colors shadow-sm"
                >
                  <Siren className="w-4 h-4" />
                  <span>Transmit Coordinates to Police 15</span>
                </button>

                <button
                  onClick={handleBroadcastSms}
                  className="px-3.5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 transition-colors shadow-sm"
                >
                  <Send className="w-4 h-4" />
                  <span>Broadcast SMS to Family Contact</span>
                </button>

                <button
                  onClick={handleFreezeDriver}
                  className="px-3.5 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 transition-colors shadow-sm"
                >
                  <Lock className="w-4 h-4" />
                  <span>Temporary Restrict Driver</span>
                </button>
              </div>
            </div>

            {/* Resolution Form */}
            {!selectedTrip.sosAlert?.resolved ? (
              <div className="p-4 bg-slate-900 text-white rounded-2xl space-y-3 shadow-md">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <FileCheck className="w-4 h-4 text-emerald-400" />
                    <h4 className="text-xs font-bold text-slate-100">
                      Resolve Incident & File Supervisor Audit Report
                    </h4>
                  </div>
                  <span className="text-[10px] text-slate-400">Required before closing</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <div className="sm:col-span-1">
                    <label className="text-[10px] font-bold text-slate-400 block mb-1">
                      Resolution Category
                    </label>
                    <select
                      value={resolutionCategory}
                      onChange={(e) => setResolutionCategory(e.target.value)}
                      className="w-full p-2 bg-slate-800 border border-slate-700 rounded-lg text-xs text-white focus:outline-none focus:border-emerald-500 cursor-pointer"
                    >
                      <option value="Route Deviation Verified & Resolved">Route Deviation Resolved</option>
                      <option value="False Alarm / Accidental Trigger">False Alarm / Accidental</option>
                      <option value="Police Dispatch On-Site">Police Dispatch On-Site</option>
                      <option value="Medical Assistance Provided">Medical Assistance Provided</option>
                      <option value="Passenger Safely Reached">Passenger Safely Reached</option>
                      <option value="Driver Replaced / Dispatched">Driver Replaced</option>
                    </select>
                  </div>

                  <div className="sm:col-span-2">
                    <label className="text-[10px] font-bold text-slate-400 block mb-1">
                      Supervisor Audit Remarks & Action Taken
                    </label>
                    <input
                      type="text"
                      value={resolutionNotes}
                      onChange={(e) => setResolutionNotes(e.target.value)}
                      placeholder="e.g., Contacted passenger; confirmed safe arrival. Road construction caused detour."
                      className="w-full p-2 bg-slate-800 border border-slate-700 rounded-lg text-xs text-white placeholder-slate-400 focus:outline-none focus:border-emerald-500"
                    />
                  </div>
                </div>

                <div className="flex justify-end pt-1">
                  <button
                    onClick={handleResolve}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-colors shadow-md flex items-center space-x-1.5"
                  >
                    <CheckCircle className="w-3.5 h-3.5" />
                    <span>Complete & Mark Incident Resolved</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl space-y-1.5 text-xs">
                <div className="flex items-center space-x-2 text-emerald-800 font-bold">
                  <CheckCircle className="w-4 h-4 text-emerald-600" />
                  <span>Incident Resolved and Closed</span>
                </div>
                <p className="text-slate-600 text-[11px]">
                  {selectedTrip.sosAlert?.resolutionNotes ||
                    'Verified and resolved by safety dispatcher.'}
                </p>
              </div>
            )}
          </>
        ) : (
          <div className="p-12 text-center text-slate-400 space-y-2">
            <ShieldCheck className="w-10 h-10 text-slate-300 mx-auto" />
            <p className="text-sm font-bold text-slate-700">No Incident Selected</p>
            <p className="text-xs">Select an emergency incident from the left queue to inspect live telemetry.</p>
          </div>
        )}
      </div>
    </div>
  );
};
