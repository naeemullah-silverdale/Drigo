import React, { useState, useEffect, useMemo } from 'react';
import { Trip, Driver, Rider, SurgeZone, SafetyReport, PayoutRequest, OperationalRecommendation, LiveActivityFeedItem } from '../types';
import {
  subscribeToOperationalRecommendations,
  subscribeToSystemMemory,
  executeOperationalRecommendationInFirebase
} from '../firebase';
import {
  BrainCircuit,
  Zap,
  ShieldAlert,
  Smartphone,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  TrendingUp,
  Cpu,
  RefreshCw,
  Clock,
  Car,
  Users,
  Database,
  Sliders,
  Check,
  Activity,
  FileCheck
} from 'lucide-react';

interface AutonomousOpsCenterProps {
  trips: Trip[];
  drivers: Driver[];
  riders: Rider[];
  surgeZones: SurgeZone[];
  safetyReports: SafetyReport[];
  payoutRequests: PayoutRequest[];
  recommendations: OperationalRecommendation[];
  feed: LiveActivityFeedItem[];
  onExecuteRecommendation: (rec: OperationalRecommendation) => void;
  onNavigateTab: (tab: any, targetId?: string) => void;
}

export const AutonomousOpsCenter: React.FC<AutonomousOpsCenterProps> = ({
  trips,
  drivers,
  riders,
  surgeZones,
  safetyReports,
  payoutRequests,
  recommendations,
  feed,
  onExecuteRecommendation,
  onNavigateTab,
}) => {
  const [executedRecIds, setExecutedRecIds] = useState<Record<string, boolean>>({});
  const [activeFilter, setActiveFilter] = useState<'all' | 'surge' | 'safety' | 'kyc' | 'telemetry' | 'finance'>('all');
  const [firebaseRecommendations, setFirebaseRecommendations] = useState<OperationalRecommendation[]>([]);
  const [firebaseFeed, setFirebaseFeed] = useState<LiveActivityFeedItem[]>([]);

  // Direct real-time Firebase subscription for persistent recommendations & system memory
  useEffect(() => {
    const unsubRecs = subscribeToOperationalRecommendations((liveRecs) => {
      setFirebaseRecommendations(liveRecs || []);
    });
    const unsubFeed = subscribeToSystemMemory((liveItems) => {
      setFirebaseFeed(liveItems || []);
    });

    return () => {
      unsubRecs();
      unsubFeed();
    };
  }, []);

  // Generate dynamic real-time insights from live Firebase database state
  const computedRecommendations: OperationalRecommendation[] = useMemo(() => {
    // Combine props recommendations with direct Firebase recommendations
    const list: OperationalRecommendation[] = [
      ...recommendations,
      ...firebaseRecommendations.filter(fr => !recommendations.some(r => r.id === fr.id))
    ];

    // 1. Unassigned Trips from passenger apps in Firebase
    const unassignedTrips = trips.filter(
      (t) => (t.status === 'requested' || t.status === 'searching' || t.status === 'matching') && !t.driverId
    );
    if (unassignedTrips.length > 0) {
      const u = unassignedTrips[0];
      list.unshift({
        id: `dyn-unassigned-${u.id}`,
        category: 'fleet',
        title: `${unassignedTrips.length} Ride Request(s) Awaiting Driver Assignment`,
        description: `Passenger ${u.passengerName || 'Client'} requested ride in ${u.fromLocation?.name || u.fromLocation?.address || 'Peshawar'}. Estimated fare: Rs. ${u.fare?.total || 700}.`,
        impact: 'high',
        timestamp: 'Live Now',
        actionLabel: 'Open Dispatch Map',
        actionType: 'open_dispatch',
        actionPayload: { tripId: u.id },
      });
    }

    // 2. Real Pending Safety Reports from Android users in Firebase
    const pendingSafety = safetyReports.filter(
      (r) => r.status === 'PENDING_ADMIN_REVIEW' || r.status === 'INVESTIGATING'
    );
    if (pendingSafety.length > 0) {
      const r = pendingSafety[0];
      list.unshift({
        id: `dyn-safety-${r.id}`,
        category: 'safety',
        title: `Safety Incident Report: ${r.categoryLabel || r.category || 'Incident'}`,
        description: `Reported by ${r.reporterName} on ride ${String(r.rideId || '').slice(0, 8)}... (${r.ridePickupTitle || 'Pickup'} → ${r.rideDestinationTitle || 'Dropoff'}). Status: Pending Review.`,
        impact: 'high',
        timestamp: typeof r.timestamp === 'number' ? new Date(r.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Live',
        actionLabel: 'Open Safety Desk',
        actionType: 'view_safety',
        actionPayload: { reportId: r.id, rideId: r.rideId },
      });
    }

    // 3. Unresolved Emergency / SOS alerts
    const activeSosTrips = trips.filter(
      (t) => t.status === 'sos_alert' || (t.sosAlert && t.sosAlert.isTriggered && !t.sosAlert.resolved)
    );
    if (activeSosTrips.length > 0) {
      const sos = activeSosTrips[0];
      list.unshift({
        id: `dyn-sos-${sos.id}`,
        category: 'safety',
        title: `CRITICAL: Active Emergency Incident on Trip ${sos.tripCode}`,
        description: `SOS triggered by ${sos.passengerName}. Live GPS coordinates active.`,
        impact: 'high',
        timestamp: 'Immediate',
        actionLabel: 'Open Safety Desk',
        actionType: 'view_safety',
        actionPayload: { tripId: sos.id },
      });
    }

    // 4. Pending Driver KYC & Document Verifications in Firebase
    const pendingKycDrivers = drivers.filter(
      (d) =>
        d.status === 'pending_verification' ||
        d.accountStatus === 'PENDING_REVIEW' ||
        d.documents?.some((doc) => doc.status === 'pending')
    );
    if (pendingKycDrivers.length > 0) {
      const d = pendingKycDrivers[0];
      list.push({
        id: `dyn-kyc-${d.id}`,
        category: 'kyc',
        title: `Driver Verification Queue: ${d.fullName}`,
        description: `${d.vehicle?.make || 'Vehicle'} ${d.vehicle?.model || ''} (${d.vehicle?.licensePlate || 'Plate'}) documents awaiting administrative approval.`,
        impact: 'high',
        timestamp: 'Pending Review',
        actionLabel: 'Review KYC Queue',
        actionType: 'view_kyc',
        actionPayload: { driverId: d.id },
      });
    }

    // 5. Driver Telemetry & Battery Warnings on budget devices
    const lowBatteryDrivers = drivers.filter(
      (d) => d.telemetry && d.telemetry.batteryLevel < 25 && (d.status === 'online' || d.status === 'on_trip')
    );
    if (lowBatteryDrivers.length > 0) {
      const d = lowBatteryDrivers[0];
      list.push({
        id: `dyn-bat-${d.id}`,
        category: 'telemetry',
        title: `Low Battery Telemetry Alert: ${d.fullName}`,
        description: `${d.telemetry?.deviceModel || 'Android Device'} running at ${d.telemetry?.batteryLevel}% battery with ${d.telemetry?.networkType || 'cellular'} network.`,
        impact: 'medium',
        timestamp: 'Live',
        actionLabel: 'Inspect Driver Profile',
        actionType: 'inspect_driver',
        actionPayload: { driverId: d.id },
      });
    }

    // 6. Active Surge Zones & Demand Spikes
    const activeSurge = surgeZones.filter((z) => z.surgeMultiplier > 1.0 || z.activeDemand > 0);
    if (activeSurge.length > 0) {
      const z = activeSurge[0];
      list.push({
        id: `dyn-surge-${z.id}`,
        category: 'surge',
        title: `Demand Spike in ${z.name}`,
        description: `Active passenger demand: ${z.activeDemand} ride requests. Multiplier: ${z.surgeMultiplier}x.`,
        impact: 'high',
        timestamp: 'Live',
        actionLabel: `Adjust ${z.name} Surge`,
        actionType: 'activate_surge',
        actionPayload: { zoneId: z.id, multiplier: z.surgeMultiplier >= 1.4 ? 1.6 : 1.4 },
      });
    }

    // 7. Real Driver Payout Settlements in Firebase
    const pendingPayouts = payoutRequests.filter((p) => p.status === 'pending');
    if (pendingPayouts.length > 0) {
      const sum = pendingPayouts.reduce((acc, p) => acc + (p.amount ?? 0), 0);
      list.push({
        id: 'dyn-payouts-queue',
        category: 'finance',
        title: `${pendingPayouts.length} Driver Payout(s) Pending Settlement (Rs. ${(sum ?? 0).toLocaleString()})`,
        description: `Requested via JazzCash / EasyPaisa / Bank Transfer by verified fleet captains.`,
        impact: 'medium',
        timestamp: 'Recent',
        actionLabel: 'Review Payouts',
        actionType: 'approve_payout',
        actionPayload: { payoutId: pendingPayouts[0].id },
      });
    }

    // Deduplicate by ID
    const seen = new Set<string>();
    return list.filter((item) => {
      if (seen.has(item.id)) return false;
      seen.add(item.id);
      return true;
    });
  }, [trips, drivers, safetyReports, surgeZones, payoutRequests, recommendations, firebaseRecommendations]);

  const filteredRecs = computedRecommendations.filter(
    (r) => activeFilter === 'all' || r.category === activeFilter
  );

  // Combined mutation feed: combines props feed with direct Firebase system memory feed
  const liveFeedItems = useMemo(() => {
    const combined = [...(feed || []), ...firebaseFeed];
    const seen = new Set<string>();
    const deduped: LiveActivityFeedItem[] = [];
    for (const item of combined) {
      if (!seen.has(item.id)) {
        seen.add(item.id);
        deduped.push(item);
      }
    }
    return deduped;
  }, [feed, firebaseFeed]);

  const handleActionClick = async (rec: OperationalRecommendation) => {
    setExecutedRecIds((prev) => ({ ...prev, [rec.id]: true }));
    try {
      await executeOperationalRecommendationInFirebase(rec.id, rec.actionType, rec.actionPayload);
    } catch (err) {
      console.warn('executeOperationalRecommendationInFirebase error:', err);
    }
    onExecuteRecommendation(rec);
  };

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-blue-950 to-indigo-950 rounded-2xl p-6 text-white border border-slate-800 shadow-lg relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-start space-x-3">
            <div className="w-12 h-12 bg-blue-500/20 text-blue-400 rounded-xl flex items-center justify-center border border-blue-400/30 shrink-0">
              <BrainCircuit className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <h2 className="text-xl font-extrabold tracking-tight text-white flex items-center space-x-2">
                <span>Autonomous Fleet Intelligence & System Memory</span>
                <span className="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] uppercase font-bold px-2 py-0.5 rounded-full">
                  Realtime Engine Active
                </span>
              </h2>
              <p className="text-xs text-slate-300 mt-1 max-w-2xl leading-relaxed">
                Continuous telemetry observer monitoring database state transitions, low-end Android device performance, surge demand spikes, and safety anomalies across Drigo fleet operations.
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2 shrink-0">
            <div className="bg-white/10 backdrop-blur-xs px-3 py-1.5 rounded-xl border border-white/10 text-xs text-center font-mono">
              <span className="text-slate-400 block text-[10px] uppercase font-bold">Monitored Objects</span>
              <span className="font-extrabold text-white">
                {trips.length + drivers.length + riders.length} Nodes
              </span>
            </div>
            <div className="bg-white/10 backdrop-blur-xs px-3 py-1.5 rounded-xl border border-white/10 text-xs text-center font-mono">
              <span className="text-slate-400 block text-[10px] uppercase font-bold">Action Queue</span>
              <span className="font-extrabold text-amber-400">
                {computedRecommendations.length} Items
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Operational Recommendations Grid */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-2">
            <Zap className="w-5 h-5 text-amber-500" />
            <h3 className="text-sm font-extrabold text-slate-900">
              Operational Recommendations & Anomaly Mitigations
            </h3>
          </div>

          <div className="flex items-center space-x-1 overflow-x-auto text-xs font-semibold bg-white p-1 rounded-xl border border-slate-200">
            {(['all', 'surge', 'safety', 'kyc', 'telemetry', 'finance'] as const).map((filter) => (
              <button
                key={filter}
                onClick={() => setActiveFilter(filter)}
                className={`px-3 py-1.5 rounded-lg capitalize transition-colors ${
                  activeFilter === filter
                    ? 'bg-blue-600 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {filter}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredRecs.length === 0 ? (
            <div className="col-span-full py-10 px-6 bg-white rounded-2xl border border-dashed border-slate-300 text-center space-y-2">
              <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto" />
              <h4 className="text-sm font-extrabold text-slate-800">Fleet Operations Nominal</h4>
              <p className="text-xs text-slate-500 max-w-md mx-auto">
                No active operational anomalies or action items in this filter. Live telemetry streams from connected Android driver and passenger client nodes are within optimal bounds.
              </p>
            </div>
          ) : (
            filteredRecs.map((rec) => {
              const isExecuted = executedRecIds[rec.id] || (rec as any).executed;
              return (
                <div
                  key={rec.id}
                  className={`bg-white rounded-2xl border p-5 shadow-xs transition-all flex flex-col justify-between space-y-4 ${
                    rec.impact === 'high'
                      ? 'border-rose-200 hover:border-rose-400'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase border ${
                          rec.impact === 'high'
                            ? 'bg-rose-50 text-rose-700 border-rose-200'
                            : 'bg-amber-50 text-amber-700 border-amber-200'
                        }`}
                      >
                        {rec.category.toUpperCase()} • {rec.impact.toUpperCase()} IMPACT
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono flex items-center space-x-1">
                        <Clock className="w-3 h-3" />
                        <span>{rec.timestamp}</span>
                      </span>
                    </div>

                    <h4 className="text-sm font-extrabold text-slate-900 leading-snug">{rec.title}</h4>
                    <p className="text-xs text-slate-600 leading-relaxed">{rec.description}</p>
                  </div>

                  <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                    <span className="text-[11px] text-slate-400">1-Click Dispatch Action</span>
                    <button
                      onClick={() => handleActionClick(rec)}
                      disabled={isExecuted}
                      className={`inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all shadow-xs ${
                        isExecuted
                          ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
                          : rec.impact === 'high'
                          ? 'bg-rose-600 hover:bg-rose-700 text-white'
                          : 'bg-blue-600 hover:bg-blue-700 text-white'
                      }`}
                    >
                      {isExecuted ? (
                        <>
                          <Check className="w-3.5 h-3.5" />
                          <span>Action Executed</span>
                        </>
                      ) : (
                        <>
                          <span>{rec.actionLabel}</span>
                          <ArrowRight className="w-3.5 h-3.5" />
                        </>
                      )}
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Live Activity & Database Event Memory Stream */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Activity className="w-4 h-4 text-blue-600" />
            <h3 className="text-sm font-extrabold text-slate-900">
              Live Database State Mutation Stream
            </h3>
          </div>
          <span className="text-[11px] font-mono text-emerald-600 font-semibold flex items-center space-x-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>Firebase Realtime Database Synced</span>
          </span>
        </div>

        <div className="divide-y divide-slate-100 max-h-96 overflow-y-auto">
          {liveFeedItems.length === 0 ? (
            <div className="py-12 px-6 text-center text-slate-400 text-xs space-y-1.5">
              <Activity className="w-6 h-6 mx-auto mb-2 text-blue-500 opacity-60 animate-pulse" />
              <p className="font-bold text-slate-700">Listening for Realtime Database State Mutations</p>
              <p className="text-[11px] text-slate-400 max-w-sm mx-auto">
                Connected to Firebase Realtime Database. New ride requests, driver verifications, GPS updates, and payout mutations from Android clients will stream here instantaneously.
              </p>
            </div>
          ) : (
            liveFeedItems.slice(0, 20).map((item) => (
              <div key={item.id} className="py-3 flex items-start justify-between space-x-3 text-xs">
                <div className="flex items-start space-x-3">
                  <div
                    className={`w-7 h-7 rounded-lg flex items-center justify-center font-bold shrink-0 mt-0.5 ${
                      item.severity === 'critical'
                        ? 'bg-rose-100 text-rose-700'
                        : item.severity === 'warning'
                        ? 'bg-amber-100 text-amber-700'
                        : item.severity === 'success'
                        ? 'bg-emerald-100 text-emerald-700'
                        : 'bg-blue-100 text-blue-700'
                    }`}
                  >
                    {item.severity === 'critical' ? (
                      <ShieldAlert className="w-3.5 h-3.5" />
                    ) : item.severity === 'warning' ? (
                      <AlertTriangle className="w-3.5 h-3.5" />
                    ) : item.type === 'driver_kyc' ? (
                      <FileCheck className="w-3.5 h-3.5" />
                    ) : (
                      <CheckCircle2 className="w-3.5 h-3.5" />
                    )}
                  </div>
                  <div>
                    <div className="font-bold text-slate-900">{item.title}</div>
                    <p className="text-[11px] text-slate-500 mt-0.5">{item.description}</p>
                  </div>
                </div>

                <span className="text-[10px] font-mono text-slate-400 shrink-0">
                  {item.timestamp}
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
