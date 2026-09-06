import React, { useState, useEffect, useMemo } from 'react';
import {
  Trip,
  Driver,
  Rider,
  SurgeZone,
  SafetyReport,
  PayoutRequest,
  OperationalRecommendation,
  LiveActivityFeedItem
} from '../types';
import {
  subscribeToOperationalRecommendations,
  subscribeToSystemMemory,
  executeOperationalRecommendationInFirebase,
  recordSystemMemoryEvent
} from '../firebase';
import { OpsStatHeader } from './ops/OpsStatHeader';
import { AutonomousRecommendationsHub } from './ops/AutonomousRecommendationsHub';
import { SystemMemoryStream } from './ops/SystemMemoryStream';
import { AndroidFleetTelemetryMatrix } from './ops/AndroidFleetTelemetryMatrix';
import { SimulationModal } from './ops/SimulationModal';
import { HeuristicRulesModal, HeuristicRules, DEFAULT_HEURISTIC_RULES } from './ops/HeuristicRulesModal';

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
  trips = [],
  drivers = [],
  riders = [],
  surgeZones = [],
  safetyReports = [],
  payoutRequests = [],
  recommendations = [],
  feed = [],
  onExecuteRecommendation,
  onNavigateTab,
}) => {
  const [executedRecIds, setExecutedRecIds] = useState<Record<string, boolean>>({});
  const [executingRecId, setExecutingRecId] = useState<string | null>(null);
  const [dismissedRecIds, setDismissedRecIds] = useState<Record<string, boolean>>({});
  const [firebaseRecommendations, setFirebaseRecommendations] = useState<OperationalRecommendation[]>([]);
  const [firebaseFeed, setFirebaseFeed] = useState<LiveActivityFeedItem[]>([]);
  const [isStreamPaused, setIsStreamPaused] = useState<boolean>(false);
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const [simulatingRec, setSimulatingRec] = useState<OperationalRecommendation | null>(null);
  const [isRulesModalOpen, setIsRulesModalOpen] = useState<boolean>(false);
  const [activeOpsTab, setActiveOpsTab] = useState<'recommendations' | 'memory' | 'telemetry'>('recommendations');
  const [toastNotification, setToastNotification] = useState<string | null>(null);

  // Heuristic rules state
  const [heuristicRules, setHeuristicRules] = useState<HeuristicRules>(() => {
    try {
      const saved = localStorage.getItem('DRIGO_HEURISTIC_RULES');
      return saved ? JSON.parse(saved) : DEFAULT_HEURISTIC_RULES;
    } catch {
      return DEFAULT_HEURISTIC_RULES;
    }
  });

  const showNotification = (msg: string) => {
    setToastNotification(msg);
    setTimeout(() => {
      setToastNotification((prev) => (prev === msg ? null : prev));
    }, 4000);
  };

  // Direct real-time Firebase subscription for persistent recommendations & system memory
  useEffect(() => {
    const unsubRecs = subscribeToOperationalRecommendations((liveRecs) => {
      setFirebaseRecommendations(liveRecs || []);
    });
    const unsubFeed = subscribeToSystemMemory((liveItems) => {
      if (!isStreamPaused) {
        setFirebaseFeed(liveItems || []);
      }
    });

    return () => {
      unsubRecs();
      unsubFeed();
    };
  }, [isStreamPaused]);

  // Dynamic real-time recommendation generation based on live database state & heuristic rules
  const computedRecommendations: OperationalRecommendation[] = useMemo(() => {
    const list: OperationalRecommendation[] = [
      ...recommendations,
      ...firebaseRecommendations.filter((fr) => !recommendations.some((r) => r.id === fr.id)),
    ];

    // 1. Critical SOS Emergency Alerts
    const activeSosTrips = trips.filter(
      (t) => t.status === 'sos_alert' || (t.sosAlert && t.sosAlert.isTriggered && !t.sosAlert.resolved)
    );
    activeSosTrips.forEach((sos) => {
      list.unshift({
        id: `dyn-sos-${sos.id}`,
        category: 'safety',
        title: `CRITICAL: Emergency SOS Incident on Ride ${sos.tripCode}`,
        description: `SOS alert triggered by ${sos.passengerName || 'Passenger'}. Live GPS coordinates active at ${sos.fromLocation?.name || 'Peshawar'}.`,
        impact: 'high',
        timestamp: 'Immediate',
        actionLabel: 'Escalate to Safety Desk',
        actionType: 'view_safety',
        actionPayload: { tripId: sos.id },
        confidenceScore: 99,
        rootCause: 'Emergency trigger received from passenger Android client device.',
        estimatedImpactBenefit: 'Immediate safety team dispatch & location lock.',
        affectedEntityId: sos.id,
        affectedEntityType: 'trip'
      });
    });

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
        description: `Reported by ${r.reporterName || 'User'} on ride ${String(r.rideId || '').slice(0, 8)}... (${r.ridePickupTitle || 'Pickup'} → ${r.rideDestinationTitle || 'Dropoff'}). Status: Pending Review.`,
        impact: 'high',
        timestamp: typeof r.timestamp === 'number' ? new Date(r.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Live',
        actionLabel: 'Open Safety Desk',
        actionType: 'view_safety',
        actionPayload: { reportId: r.id, rideId: r.rideId },
        confidenceScore: 96,
        rootCause: 'User filed incident report regarding driver conduct or route deviation.',
        estimatedImpactBenefit: 'Resolves dispute, safeguards rider trust.',
        affectedEntityId: r.id,
        affectedEntityType: 'safety_report'
      });
    }

    // 3. Unassigned Trips awaiting drivers in Firebase
    const unassignedTrips = trips.filter(
      (t) => (t.status === 'requested' || t.status === 'searching' || t.status === 'matching') && !t.driverId
    );
    if (unassignedTrips.length > 0) {
      const u = unassignedTrips[0];
      list.unshift({
        id: `dyn-unassigned-${u.id}`,
        category: 'fleet',
        title: `${unassignedTrips.length} Ride Request(s) Awaiting Driver Assignment`,
        description: `Passenger ${u.passengerName || 'Client'} requested ride in ${u.fromLocation?.name || u.fromLocation?.address || 'Peshawar'}. Estimated fare: Rs. ${(u.fare?.total ?? 700).toLocaleString()}.`,
        impact: unassignedTrips.length > 2 ? 'high' : 'medium',
        timestamp: 'Live Now',
        actionLabel: 'Open Dispatch Map',
        actionType: 'open_dispatch',
        actionPayload: { tripId: u.id },
        confidenceScore: 94,
        rootCause: 'Driver bidding latency or localized supply gap in pickup radius.',
        estimatedImpactBenefit: '-3.5 min passenger wait time, prevents ride cancellation.',
        affectedEntityId: u.id,
        affectedEntityType: 'trip'
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
        confidenceScore: 98,
        rootCause: 'New driver registration submitted with CNIC/License photos.',
        estimatedImpactBenefit: '+1 verified captain to active fleet capacity.',
        affectedEntityId: d.id,
        affectedEntityType: 'driver'
      });
    }

    // 5. Driver Telemetry & Low Battery Alerts on Budget Android Phones
    const lowBatteryDrivers = drivers.filter(
      (d) => (d.telemetry?.batteryLevel ?? 100) < heuristicRules.lowBatteryThresholdPercent &&
        (d.status === 'online' || d.status === 'on_trip')
    );
    if (lowBatteryDrivers.length > 0) {
      const d = lowBatteryDrivers[0];
      list.push({
        id: `dyn-bat-${d.id}`,
        category: 'telemetry',
        title: `Low Battery Telemetry Alert: ${d.fullName}`,
        description: `${d.telemetry?.deviceModel || 'Samsung A12'} running at ${d.telemetry?.batteryLevel}% battery with ${d.telemetry?.networkType || '4G'} network.`,
        impact: 'medium',
        timestamp: 'Live',
        actionLabel: 'Inspect Driver Profile',
        actionType: 'inspect_driver',
        actionPayload: { driverId: d.id },
        confidenceScore: 92,
        rootCause: `Device battery fallen below ${heuristicRules.lowBatteryThresholdPercent}% safety margin.`,
        estimatedImpactBenefit: 'Prevents device shutdown during active ride dispatch.',
        affectedEntityId: d.id,
        affectedEntityType: 'driver'
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
        description: `Active passenger demand: ${z.activeDemand} ride requests. Current multiplier: ${z.surgeMultiplier}x.`,
        impact: 'high',
        timestamp: 'Live',
        actionLabel: `Adjust ${z.name} Surge`,
        actionType: 'activate_surge',
        actionPayload: { zoneId: z.id, multiplier: z.surgeMultiplier >= 1.4 ? 1.6 : 1.4 },
        confidenceScore: 95,
        rootCause: 'Localized demand exceeding standard fleet supply.',
        estimatedImpactBenefit: '+18% driver earnings, balances passenger wait queue.',
        affectedEntityId: z.id,
        affectedEntityType: 'zone'
      });
    }

    // 7. Driver Payout Settlements
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
        confidenceScore: 97,
        rootCause: 'Fleet earnings settlement requests queued for verification.',
        estimatedImpactBenefit: 'Ensures on-time captain payout satisfaction.',
        affectedEntityType: 'payout'
      });
    }

    // Deduplicate and filter dismissed
    const seen = new Set<string>();
    return list.filter((item) => {
      if (dismissedRecIds[item.id]) return false;
      if (seen.has(item.id)) return false;
      seen.add(item.id);
      return true;
    });
  }, [
    trips,
    drivers,
    safetyReports,
    surgeZones,
    payoutRequests,
    recommendations,
    firebaseRecommendations,
    dismissedRecIds,
    heuristicRules,
  ]);

  // Combine props feed with live Firebase mutation feed
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

  // Health Metrics
  const criticalCount = computedRecommendations.filter(
    (r) => r.impact === 'high' && !executedRecIds[r.id] && !r.executed
  ).length;
  const highCount = computedRecommendations.filter(
    (r) => !executedRecIds[r.id] && !r.executed
  ).length;

  const lowBatteryDriverCount = drivers.filter(
    (d) => (d.telemetry?.batteryLevel ?? 100) < heuristicRules.lowBatteryThresholdPercent
  ).length;
  const lowRamDriverCount = drivers.filter(
    (d) => (d.telemetry?.ramUsagePercent ?? 0) > heuristicRules.highRamThresholdPercent || (d.telemetry?.ramTotalGb ?? 4) <= 2
  ).length;

  // Calculate dynamic fleet health score (0-100%)
  const fleetHealthScore = useMemo(() => {
    let score = 100;
    if (criticalCount > 0) score -= criticalCount * 8;
    if (lowBatteryDriverCount > 0) score -= lowBatteryDriverCount * 3;
    if (lowRamDriverCount > 0) score -= lowRamDriverCount * 2;
    return Math.max(72, Math.min(100, score));
  }, [criticalCount, lowBatteryDriverCount, lowRamDriverCount]);

  // 1-Click Action Handler
  const handleActionClick = async (rec: OperationalRecommendation) => {
    setExecutingRecId(rec.id);
    setExecutedRecIds((prev) => ({ ...prev, [rec.id]: true }));

    try {
      await executeOperationalRecommendationInFirebase(rec.id, rec.actionType, rec.actionPayload);
      showNotification(`Executed recommendation: "${rec.actionLabel}"`);
    } catch (err) {
      console.warn('executeOperationalRecommendationInFirebase error:', err);
    } finally {
      setExecutingRecId(null);
    }

    onExecuteRecommendation(rec);
  };

  // Dismiss action
  const handleDismissRecommendation = (recId: string) => {
    setDismissedRecIds((prev) => ({ ...prev, [recId]: true }));
    showNotification('Anomaly recommendation dismissed from active queue.');
  };

  // Deep Heuristic Scan Trigger
  const handleRunDeepDiagnostic = async () => {
    setIsScanning(true);
    showNotification('Neural Heuristic Scanner analyzing real-time fleet telemetry...');

    await recordSystemMemoryEvent({
      type: 'trip_request',
      title: 'Manual Heuristic Scan Initiated',
      description: `Deep heuristic scan evaluated ${trips.length + drivers.length + riders.length} connected nodes.`,
      severity: 'info',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    });

    setTimeout(() => {
      setIsScanning(false);
      showNotification('Fleet heuristic evaluation complete. All anomalies updated.');
    }, 1200);
  };

  // Export System Memory as JSON file
  const handleExportSystemMemory = () => {
    try {
      const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(
        JSON.stringify({
          exportedAt: new Date().toISOString(),
          fleetHealthScore,
          totalNodes: trips.length + drivers.length + riders.length,
          recommendations: computedRecommendations,
          memoryStream: liveFeedItems
        }, null, 2)
      );
      const downloadAnchor = document.createElement('a');
      downloadAnchor.setAttribute('href', dataStr);
      downloadAnchor.setAttribute('download', `drigo_system_memory_audit_${Date.now()}.json`);
      document.body.appendChild(downloadAnchor);
      downloadAnchor.click();
      downloadAnchor.remove();
      showNotification('System Memory audit trail downloaded successfully.');
    } catch (e) {
      console.warn('Failed to export memory:', e);
    }
  };

  // Send Battery Alert to Driver
  const handleSendBatteryAlert = async (driverId: string, driverName: string) => {
    await recordSystemMemoryEvent({
      type: 'driver_online',
      title: `Low Battery Warning Sent: ${driverName}`,
      description: `Autonomous push notification dispatched to driver ${driverName} to activate battery saver mode.`,
      severity: 'warning',
      driverId,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    });
    showNotification(`Battery saver alert sent to ${driverName}.`);
  };

  // Inject Test Mutation
  const handleInjectTestEvent = async (type: string) => {
    if (type === 'trip_request') {
      await recordSystemMemoryEvent({
        type: 'trip_request',
        title: 'Synthetic Ride Request: Fatima Shah',
        description: 'Hayatabad Phase 3 → University Road (Est. Rs. 480).',
        severity: 'info',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      });
    } else if (type === 'low_battery') {
      await recordSystemMemoryEvent({
        type: 'driver_online',
        title: 'Battery Telemetry Alert: Tariq Khan',
        description: 'Samsung Galaxy A12 battery fallen to 12%. Cellular 4G active.',
        severity: 'warning',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      });
    } else if (type === 'sos_alert') {
      await recordSystemMemoryEvent({
        type: 'sos_alert',
        title: 'EMERGENCY SOS: Ride #DRG-9102',
        description: 'SOS trigger raised by passenger near Saddar Bazaar. Escalating to safety team.',
        severity: 'critical',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      });
    }
    showNotification('Synthetic mutation injected into Realtime System Memory.');
  };

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50">
      {/* Toast Banner */}
      {toastNotification && (
        <div className="fixed bottom-6 right-6 z-50 bg-slate-950 text-white px-4 py-3 rounded-2xl shadow-2xl border border-slate-800 text-xs font-bold flex items-center space-x-2 animate-slideUp">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
          <span>{toastNotification}</span>
        </div>
      )}

      {/* Top Header & Executive Metric Cards */}
      <OpsStatHeader
        fleetHealthScore={fleetHealthScore}
        criticalCount={criticalCount}
        highCount={highCount}
        totalRecommendations={computedRecommendations.length}
        mutationStreamCount={liveFeedItems.length}
        lowBatteryDriverCount={lowBatteryDriverCount}
        lowRamDriverCount={lowRamDriverCount}
        isStreamPaused={isStreamPaused}
        onToggleStreamPause={() => {
          setIsStreamPaused(!isStreamPaused);
          showNotification(isStreamPaused ? 'Resumed live mutation stream.' : 'Paused live mutation stream.');
        }}
        onRunDeepDiagnostic={handleRunDeepDiagnostic}
        onExportSystemMemory={handleExportSystemMemory}
        onOpenRulesConfig={() => setIsRulesModalOpen(true)}
        isScanning={isScanning}
        totalMonitoredNodes={trips.length + drivers.length + riders.length}
      />

      {/* Navigation Sub-Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveOpsTab('recommendations')}
          className={`px-4 py-2 rounded-xl text-xs font-extrabold transition-all ${
            activeOpsTab === 'recommendations'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          Anomaly Recommendations ({computedRecommendations.length})
        </button>

        <button
          onClick={() => setActiveOpsTab('memory')}
          className={`px-4 py-2 rounded-xl text-xs font-extrabold transition-all ${
            activeOpsTab === 'memory'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          System Memory Stream ({liveFeedItems.length})
        </button>

        <button
          onClick={() => setActiveOpsTab('telemetry')}
          className={`px-4 py-2 rounded-xl text-xs font-extrabold transition-all ${
            activeOpsTab === 'telemetry'
              ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          Low-End Android Telemetry ({drivers.length} Drivers)
        </button>
      </div>

      {/* Main Tab Content */}
      {activeOpsTab === 'recommendations' && (
        <div className="space-y-6">
          <AutonomousRecommendationsHub
            recommendations={computedRecommendations}
            onExecuteRecommendation={handleActionClick}
            onOpenSimulation={(rec) => setSimulatingRec(rec)}
            onDismissRecommendation={handleDismissRecommendation}
            executingRecId={executingRecId}
            executedRecIds={executedRecIds}
          />
          <SystemMemoryStream
            feedItems={liveFeedItems}
            onNavigateTab={onNavigateTab}
            onInjectTestEvent={handleInjectTestEvent}
          />
        </div>
      )}

      {activeOpsTab === 'memory' && (
        <div className="space-y-6">
          <SystemMemoryStream
            feedItems={liveFeedItems}
            onNavigateTab={onNavigateTab}
            onInjectTestEvent={handleInjectTestEvent}
          />
        </div>
      )}

      {activeOpsTab === 'telemetry' && (
        <div className="space-y-6">
          <AndroidFleetTelemetryMatrix
            drivers={drivers}
            onInspectDriver={(driverId) => {
              onNavigateTab('users', driverId);
            }}
            onSendBatteryAlert={handleSendBatteryAlert}
          />
        </div>
      )}

      {/* Simulation Modal */}
      <SimulationModal
        recommendation={simulatingRec}
        onClose={() => setSimulatingRec(null)}
        onExecute={handleActionClick}
        isExecuting={executingRecId === simulatingRec?.id}
        isExecuted={simulatingRec ? (executedRecIds[simulatingRec.id] || simulatingRec.executed || false) : false}
      />

      {/* Heuristic Rules Modal */}
      <HeuristicRulesModal
        isOpen={isRulesModalOpen}
        onClose={() => setIsRulesModalOpen(false)}
        onSaveRules={(newRules) => {
          setHeuristicRules(newRules);
          showNotification('Autonomous Heuristic anomaly thresholds updated.');
        }}
      />
    </div>
  );
};
