import React, { useState, useEffect } from 'react';
import { Sidebar, ActiveTab } from './components/Sidebar';
import { Header } from './components/Header';
import { DashboardOverview } from './components/DashboardOverview';
import { FleetDispatchMap } from './components/FleetDispatchMap';
import { UserManagement } from './components/UserManagement';
import { RealtimeAnalytics } from './components/RealtimeAnalytics';
import { SafetySOSCenter } from './components/SafetySOSCenter';
import { PricingControls } from './components/PricingControls';
import { TripsManagement } from './components/TripsManagement';
import { RatingsManagement } from './components/RatingsManagement';
import { FinancePayouts } from './components/FinancePayouts';
import { AutonomousOpsCenter } from './components/AutonomousOpsCenter';
import { FirebaseConfigModal } from './components/FirebaseConfigModal';
import { AdminLogin } from './components/AdminLogin';
import {
  subscribeToDrivers,
  subscribeToRiders,
  subscribeToTrips,
  subscribeToLiveDriverLocations,
  toggleDriverStatusInFirebase,
  toggleRiderStatusInFirebase,
  updateDriverDocumentInFirebase,
  adjustDriverWalletInFirebase,
  seedFirebaseDatabase,
  subscribeToSafetyReports,
  updateSafetyReportStatusInFirebase,
  subscribeToRideRatings,
  subscribeToAdminNotifications,
  markNotificationAsReadInFirebase,
  markAllNotificationsAsReadInFirebase,
  subscribeToPricingConfigs,
  updatePricingConfigInFirebase,
  subscribeToSurgeZones,
  updateSurgeZoneInFirebase,
  subscribeToPayoutRequests,
  updatePayoutRequestInFirebase,
  subscribeToOperationalRecommendations,
  executeOperationalRecommendationInFirebase,
  subscribeToSystemMemory,
  auth,
  checkUserIsAdmin
} from './firebase';
import { onAuthStateChanged } from 'firebase/auth';
import {
  DEFAULT_PRICING_CONFIGS
} from './mockData';
import { Driver, Rider, Trip, SurgeZone, PricingConfig, SupportTicket, LocationPoint, LiveDriverLocation, SafetyReport, LiveActivityFeedItem, RideRating, AdminNotification, PayoutRequest, OperationalRecommendation } from './types';
import { CheckCircle2, Info, Loader2 } from 'lucide-react';

export default function App() {
  const [adminUser, setAdminUser] = useState<{ email: string; uid: string } | null>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);
  const [activeTab, setActiveTab] = useState<ActiveTab>('dashboard');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCity, setSelectedCity] = useState('All');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isFirebaseModalOpen, setIsFirebaseModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // State collections connected directly to live Firebase data
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [riders, setRiders] = useState<Rider[]>([]);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [liveLocations, setLiveLocations] = useState<Record<string, LiveDriverLocation>>({});
  const [surgeZones, setSurgeZones] = useState<SurgeZone[]>([]);
  const [pricingConfigs, setPricingConfigs] = useState<PricingConfig[]>(DEFAULT_PRICING_CONFIGS);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [safetyReports, setSafetyReports] = useState<SafetyReport[]>([]);
  const [ratings, setRatings] = useState<RideRating[]>([]);
  const [notifications, setNotifications] = useState<AdminNotification[]>([]);
  const [payoutRequests, setPayoutRequests] = useState<PayoutRequest[]>([]);
  const [recommendations, setRecommendations] = useState<OperationalRecommendation[]>([]);
  const [liveSystemFeed, setLiveSystemFeed] = useState<LiveActivityFeedItem[]>([]);

  // Synchronized sidebar collapsed state across dashboard
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('DRIGO_SIDEBAR_COLLAPSED');
      if (saved !== null) {
        return saved === 'true';
      }
      return window.innerWidth < 1024;
    }
    return false;
  });

  const handleToggleSidebar = () => {
    setIsSidebarCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('DRIGO_SIDEBAR_COLLAPSED', String(next));
      return next;
    });
  };
  const feed = React.useMemo(() => {
    const items: LiveActivityFeedItem[] = [];

    trips.forEach((t) => {
      if (t.status === 'sos_alert' || (t.sosAlert && t.sosAlert.isTriggered)) {
        items.push({
          id: `sos-${t.id}`,
          type: 'sos_alert',
          title: `SOS Triggered: ${t.tripCode}`,
          description: `Emergency alert raised by ${t.passengerName}.`,
          timestamp: 'Live',
          severity: 'critical',
          tripId: t.id
        });
      } else if (t.status === 'completed') {
        items.push({
          id: `completed-${t.id}`,
          type: 'trip_completed',
          title: `Ride Completed: ${t.tripCode}`,
          description: `${t.passengerName}'s trip completed. Fare: Rs. ${t.fare?.total || 0}`,
          timestamp: 'Recent',
          severity: 'success',
          tripId: t.id
        });
      } else if (t.status === 'cancelled') {
        items.push({
          id: `cancelled-${t.id}`,
          type: 'trip_cancelled',
          title: `Ride Cancelled: ${t.tripCode}`,
          description: `Trip request cancelled.`,
          timestamp: 'Recent',
          severity: 'warning',
          tripId: t.id
        });
      } else if (t.status === 'in_progress' || t.status === 'driver_arriving') {
        items.push({
          id: `trip-${t.id}`,
          type: 'driver_matched',
          title: `Ride In-Flight: ${t.tripCode}`,
          description: `${t.passengerName} heading to ${t.toLocation.name}`,
          timestamp: 'Active',
          severity: 'info',
          tripId: t.id
        });
      }
    });

    safetyReports.forEach((r) => {
      items.push({
        id: `safety-${r.id}`,
        type: 'sos_alert',
        title: `Safety Report: ${String(r.categoryLabel || r.category || 'Incident').toUpperCase()}`,
        description: r.description,
        timestamp: typeof r.timestamp === 'number' ? new Date(r.timestamp).toLocaleTimeString() : String(r.timestamp || 'Recent'),
        severity: 'critical',
      });
    });

    drivers.forEach((d) => {
      if (d.status === 'pending_verification') {
        items.push({
          id: `kyc-${d.id}`,
          type: 'kyc_submitted',
          title: `KYC Submitted: ${d.fullName}`,
          description: `Driver documents awaiting verification in ${d.city}.`,
          timestamp: 'Pending',
          severity: 'warning'
        });
      } else if (d.status === 'suspended') {
        items.push({
          id: `susp-${d.id}`,
          type: 'account_suspended',
          title: `Account Suspended: ${d.fullName}`,
          description: `Driver account suspended by safety policy.`,
          timestamp: 'Actioned',
          severity: 'critical'
        });
      }
    });

    const combined = [...liveSystemFeed, ...items];
    const seen = new Set<string>();
    const deduped: LiveActivityFeedItem[] = [];
    for (const item of combined) {
      if (!seen.has(item.id)) {
        seen.add(item.id);
        deduped.push(item);
      }
    }
    return deduped;
  }, [trips, safetyReports, drivers, liveSystemFeed]);

  const [selectedTripId, setSelectedTripId] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 4000);
  };

  // Check for persistent local administrative session
  useEffect(() => {
    try {
      const savedSession = localStorage.getItem('drigo_admin_session');
      if (savedSession) {
        const parsed = JSON.parse(savedSession);
        if (parsed && parsed.email === 'admin@drigo') {
          setAdminUser(parsed);
        } else {
          localStorage.removeItem('drigo_admin_session');
          setAdminUser(null);
        }
      } else {
        setAdminUser(null);
      }
    } catch (e) {
      console.warn('Session check failed:', e);
      setAdminUser(null);
    } finally {
      setIsAuthChecking(false);
    }
  }, []);

  // Subscribe to live Firebase Realtime Database & Firestore only if authenticated as admin
  useEffect(() => {
    if (!adminUser) return;

    const unsubDrivers = subscribeToDrivers((liveDrivers) => {
      setDrivers(liveDrivers || []);
    });
    const unsubRiders = subscribeToRiders((liveRiders) => {
      setRiders(liveRiders || []);
    });
    const unsubTrips = subscribeToTrips((liveTrips) => {
      setTrips(liveTrips || []);
      if (liveTrips && liveTrips.length > 0 && !selectedTripId) {
        setSelectedTripId(liveTrips[0].id);
      }
    });
    const unsubLiveLocs = subscribeToLiveDriverLocations((liveLocMap) => {
      setLiveLocations(liveLocMap || {});
      if (liveLocMap && Object.keys(liveLocMap).length > 0) {
        setDrivers((prevDrivers) =>
          prevDrivers.map((d) => {
            const liveLoc = liveLocMap[d.id];
            if (liveLoc) {
              return {
                ...d,
                currentLocation: {
                  lat: liveLoc.lat,
                  lng: liveLoc.lng,
                  heading: liveLoc.heading || d.currentLocation.heading,
                  speedKmh: liveLoc.speedKmh || d.currentLocation.speedKmh,
                },
              };
            }
            return d;
          })
        );
      }
    });

    const unsubSafetyReports = subscribeToSafetyReports((reports) => {
      setSafetyReports(reports || []);
    });
    const unsubRatings = subscribeToRideRatings((liveRatings) => {
      setRatings(liveRatings || []);
    });
    const unsubNotifications = subscribeToAdminNotifications((liveNotifs) => {
      setNotifications(liveNotifs || []);
    });
    const unsubPricing = subscribeToPricingConfigs((configs) => {
      setPricingConfigs(configs || DEFAULT_PRICING_CONFIGS);
    });
    const unsubSurge = subscribeToSurgeZones((zones) => {
      setSurgeZones(zones || []);
    });
    const unsubPayouts = subscribeToPayoutRequests((payouts) => {
      setPayoutRequests(payouts || []);
    });
    const unsubRecs = subscribeToOperationalRecommendations((liveRecs) => {
      setRecommendations(liveRecs || []);
    });
    const unsubSystemMemory = subscribeToSystemMemory((liveFeed) => {
      setLiveSystemFeed(liveFeed || []);
    });

    return () => {
      unsubDrivers();
      unsubRiders();
      unsubTrips();
      unsubLiveLocs();
      unsubSafetyReports();
      unsubRatings();
      unsubNotifications();
      unsubPricing();
      unsubSurge();
      unsubPayouts();
      unsubRecs();
      unsubSystemMemory();
    };
  }, [adminUser]);

  const handleMarkNotificationAsRead = async (id: string) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
    await markNotificationAsReadInFirebase(id);
  };

  const handleMarkAllNotificationsAsRead = async () => {
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    await markAllNotificationsAsReadInFirebase();
    showToast('All notifications marked as read.');
  };

  const handleLogout = async () => {
    localStorage.removeItem('drigo_admin_session');
    setAdminUser(null);
    showToast('Securely logged out from fleet console.');
  };

  const handleLoginSuccess = (email: string, uid: string) => {
    setAdminUser({ email, uid });
    showToast('Administrator session successfully established.');
  };

  // Filtered lists based on search & city
  const filteredTrips = trips.filter((t) => {
    const q = (searchTerm || '').toLowerCase();
    const matchesCity = selectedCity === 'All' || (t.fromLocation?.name || '').includes(selectedCity) || (t.toLocation?.name || '').includes(selectedCity);
    const matchesSearch =
      !searchTerm ||
      (t.tripCode || '').toLowerCase().includes(q) ||
      (t.passengerName || '').toLowerCase().includes(q) ||
      (t.driverName || '').toLowerCase().includes(q) ||
      (t.fromLocation?.name || '').toLowerCase().includes(q) ||
      (t.toLocation?.name || '').toLowerCase().includes(q);
    return matchesCity && matchesSearch;
  });

  const filteredDrivers = drivers.filter((d) => {
    const q = (searchTerm || '').toLowerCase();
    const matchesCity = selectedCity === 'All' || (d.city || '').includes(selectedCity);
    const matchesSearch =
      !searchTerm ||
      (d.fullName || '').toLowerCase().includes(q) ||
      (d.phone || '').includes(searchTerm) ||
      (d.vehicle?.licensePlate || '').toLowerCase().includes(q);
    return matchesCity && matchesSearch;
  });

  const filteredRiders = riders.filter((r) => {
    const q = (searchTerm || '').toLowerCase();
    const matchesCity = selectedCity === 'All' || (r.city || '').includes(selectedCity);
    const matchesSearch =
      !searchTerm ||
      (r.fullName || '').toLowerCase().includes(q) ||
      (r.phone || '').includes(searchTerm);
    return matchesCity && matchesSearch;
  });

  // Location updating rule: FROM and TO are completely independent
  const handleUpdateTripLocations = (tripId: string, newFrom?: LocationPoint, newTo?: LocationPoint) => {
    setTrips((prevTrips) =>
      prevTrips.map((t) => {
        if (t.id === tripId) {
          const updatedFrom = newFrom ? newFrom : t.fromLocation; // Preserve existing FROM if undefined
          const updatedTo = newTo ? newTo : t.toLocation;       // Preserve existing TO if undefined
          return {
            ...t,
            fromLocation: updatedFrom,
            toLocation: updatedTo,
          };
        }
        return t;
      })
    );
  };

  // Driver KYC Document Approval (updates local + Firebase)
  const handleApproveDriverDocument = async (driverId: string, docId: string) => {
    setDrivers((prev) =>
      prev.map((d) => {
        if (d.id === driverId) {
          const updatedDocs = d.documents.map((doc) =>
            doc.id === docId ? { ...doc, status: 'verified' as const, lastReviewedAt: 'Just now', lastReviewedBy: 'Admin (Alex Sterling)' } : doc
          );
          const allVerified = updatedDocs.every((doc) => doc.status === 'verified');
          return {
            ...d,
            documents: updatedDocs,
            status: allVerified ? ('online' as const) : d.status,
          };
        }
        return d;
      })
    );
    await updateDriverDocumentInFirebase(driverId, docId, 'verified');
    showToast(`Driver document approved & synced to Firebase Realtime Database!`);
  };

  // Driver KYC Document Rejection
  const handleRejectDriverDocument = async (driverId: string, docId: string, reason: string) => {
    setDrivers((prev) =>
      prev.map((d) => {
        if (d.id === driverId) {
          const updatedDocs = d.documents.map((doc) =>
            doc.id === docId ? { ...doc, status: 'rejected' as const, rejectionReason: reason } : doc
          );
          return {
            ...d,
            documents: updatedDocs,
          };
        }
        return d;
      })
    );
    await updateDriverDocumentInFirebase(driverId, docId, 'rejected', reason);
    showToast(`Driver document rejected & synced to Firebase!`);
  };

  // Toggle Driver status (Approve registration, Suspend, Reactivate)
  const handleToggleDriverStatus = async (driverId: string, status: Driver['status']) => {
    const isSuspended = status === 'suspended';
    const accountStatusVal: 'ACTIVE' | 'SUSPENDED' = isSuspended ? 'SUSPENDED' : 'ACTIVE';
    const driver = drivers.find((d) => d.id === driverId);

    setDrivers((prev) => {
      const exists = prev.some((d) => d.id === driverId);
      if (exists) {
        return prev.map((d) => (d.id === driverId ? { 
          ...d, 
          status,
          accountStatus: accountStatusVal,
          // verificationStatus is strictly preserved!
        } : d));
      } else {
        return [
          ...prev,
          {
            id: driverId,
            fullName: driver?.fullName || 'Driver',
            phone: driver?.phone || 'N/A',
            email: driver?.email || `${driverId}@drigo.app`,
            avatar: driver?.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
            status,
            accountStatus: accountStatusVal,
            verificationStatus: driver?.verificationStatus || 'APPROVED',
            vehicle: driver?.vehicle || { make: 'Toyota', model: 'Corolla', licensePlate: 'DRG-8B15', type: 'sedan', year: 2024, color: 'White', seatingCapacity: 4, photoUrl: '', inspectionPassed: true },
            documents: driver?.documents || [],
            rating: 5.0,
            totalTrips: 0,
            walletBalance: 0,
            todayEarnings: 0,
            joinedDate: 'Just now',
            currentLocation: { lat: 33.6844, lng: 73.0479, heading: 0, speedKmh: 0 },
            telemetry: { deviceModel: 'Android Smartphone', manufacturer: 'Samsung', androidVersion: 'Android 13', apiLevel: 33, ramTotalGb: 4, ramUsagePercent: 40, batteryLevel: 90, isBatterySaver: false, appVersion: 'v2.4.1', networkType: '4G', networkLatencyMs: 30, gpsAccuracyMeters: 3.0, offlineQueuedPackets: 0, lastPingAt: 'Just now' },
            city: 'Islamabad',
            acceptanceRate: 100,
            completionRate: 100,
            cancellationRate: 0
          }
        ];
      }
    });

    await toggleDriverStatusInFirebase(driverId, status);
    
    if (status === 'online') {
      showToast(`Driver ${driver?.fullName || 'account'} REACTIVATED & operational in Firebase.`);
    } else if (status === 'suspended') {
      showToast(`Driver ${driver?.fullName || 'account'} SUSPENDED in Firebase (KYC documents preserved).`);
    } else {
      showToast(`Driver ${driver?.fullName || 'status'} updated to '${status}' in Firebase.`);
    }
  };

  // Adjust Driver Wallet balance
  const handleAdjustDriverWallet = async (driverId: string, amount: number) => {
    let targetNewBalance = 0;
    setDrivers((prev) =>
      prev.map((d) => {
        if (d.id === driverId) {
          targetNewBalance = d.walletBalance + amount;
          return { ...d, walletBalance: targetNewBalance };
        }
        return d;
      })
    );
    await adjustDriverWalletInFirebase(driverId, targetNewBalance);
    showToast(`Driver wallet balance adjusted by $${amount.toFixed(2)} in Firebase.`);
  };

  // Toggle Rider status
  const handleToggleRiderStatus = async (riderId: string, status: Rider['status']) => {
    setRiders((prev) => prev.map((r) => (r.id === riderId ? { ...r, status } : r)));
    await toggleRiderStatusInFirebase(riderId, status);
    showToast(`Passenger account status updated to '${status}' in Firebase!`);
  };

  // Resolve SOS Alert
  const handleResolveSosAlert = (tripId: string, notes: string) => {
    setTrips((prev) =>
      prev.map((t) => {
        if (t.id === tripId) {
          return {
            ...t,
            status: 'in_progress',
            sosAlert: t.sosAlert
              ? {
                  ...t.sosAlert,
                  resolved: true,
                  resolvedAt: 'Just now',
                  resolutionNotes: notes,
                }
              : undefined,
          };
        }
        return t;
      })
    );
  };

  // Update Safety Report Status
  const handleUpdateSafetyReportStatus = async (reportId: string, status: string) => {
    setSafetyReports((prev) =>
      prev.map((r) => (r.id === reportId ? { ...r, status } : r))
    );
    await updateSafetyReportStatusInFirebase(reportId, status);
    showToast(`Safety report status updated to ${status}.`);
  };

  // Update Support Ticket Status
  const handleUpdateTicketStatus = (ticketId: string, status: SupportTicket['status']) => {
    setTickets((prev) => prev.map((t) => (t.id === ticketId ? { ...t, status } : t)));
  };

  // Update Pricing Config (with Firebase Realtime Database and Firestore sync)
  const handleUpdatePricingConfig = async (updated: PricingConfig) => {
    setPricingConfigs((prev) => prev.map((c) => (c.vehicleType === updated.vehicleType ? updated : c)));
    await updatePricingConfigInFirebase(updated);
    showToast(`Fare matrix for ${updated.vehicleType.toUpperCase()} synchronized with Firebase.`);
  };

  // Update Surge Multiplier (with Firebase Realtime Database and Firestore sync)
  const handleUpdateSurgeMultiplier = async (zoneId: string, multiplier: number) => {
    setSurgeZones((prev) => prev.map((z) => (z.id === zoneId ? { ...z, surgeMultiplier: multiplier } : z)));
    await updateSurgeZoneInFirebase(zoneId, multiplier);
    showToast(`Surge multiplier updated to ${multiplier}x in Firebase.`);
  };

  // Update Driver Payout Request Status (with Firebase sync)
  const handleUpdatePayoutStatus = async (
    payoutId: string,
    status: PayoutRequest['status'],
    transactionRef?: string,
    rejectionReason?: string
  ) => {
    setPayoutRequests((prev) =>
      prev.map((p) => (p.id === payoutId ? { ...p, status, transactionRef, rejectionReason } : p))
    );
    await updatePayoutRequestInFirebase(payoutId, status, 'Admin (Ops Manager)', transactionRef, rejectionReason);
    showToast(
      status === 'processed'
        ? `Payout #${payoutId} settled and recorded in Firebase.`
        : `Payout #${payoutId} status updated to '${status}'.`
    );
  };

  // Execute Operational Recommendation from Autonomous AI System
  const handleExecuteRecommendation = async (rec: OperationalRecommendation) => {
    try {
      await executeOperationalRecommendationInFirebase(rec.id, rec.actionType, rec.actionPayload);
    } catch (err) {
      console.warn('executeOperationalRecommendationInFirebase error:', err);
    }

    if (rec.actionType === 'activate_surge') {
      if (rec.actionPayload?.zoneId) {
        handleUpdateSurgeMultiplier(rec.actionPayload.zoneId, rec.actionPayload.multiplier || 1.4);
      }
    } else if (rec.actionType === 'view_kyc') {
      setActiveTab('users');
    } else if (rec.actionType === 'view_safety') {
      setActiveTab('safety');
    } else if (rec.actionType === 'inspect_driver') {
      setActiveTab('users');
      if (rec.actionPayload?.driverId) {
        const d = drivers.find((dr) => dr.id === rec.actionPayload.driverId);
        if (d) setSearchTerm(d.fullName);
      }
    } else if (rec.actionType === 'approve_payout') {
      setActiveTab('finance');
    } else if (rec.actionType === 'open_dispatch') {
      if (rec.actionPayload?.tripId) {
        setSelectedTripId(rec.actionPayload.tripId);
      }
      setActiveTab('dispatch');
    }
    showToast(`Autonomous recommendation executed: "${rec.actionLabel}"`);
  };

  // Refresh Telemetry
  const handleRefreshData = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      setIsRefreshing(false);
    }, 600);
  };

  const pendingKycCount = drivers.reduce(
    (acc, d) => acc + d.documents.filter((doc) => doc.status === 'pending').length,
    0
  );
  const activeSosCount = trips.filter(
    (t) => t.status === 'sos_alert' || (t.sosAlert && t.sosAlert.isTriggered && !t.sosAlert.resolved)
  ).length;

  if (isAuthChecking) {
    return (
      <div className="min-h-screen w-screen bg-slate-950 flex flex-col items-center justify-center p-4 font-sans text-slate-100 select-none">
        <div className="flex flex-col items-center max-w-sm text-center">
          <Loader2 className="w-8 h-8 text-blue-500 animate-spin mb-4" />
          <h2 className="text-sm font-bold uppercase tracking-widest text-slate-200">
            Verifying Operator Credentials
          </h2>
          <p className="text-xs text-slate-500 mt-2 leading-relaxed">
            Establishing secure encrypted connection to Drigo Fleet Operations server...
          </p>
        </div>
      </div>
    );
  }

  if (!adminUser) {
    return <AdminLogin onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div className="flex h-screen w-screen bg-slate-950 font-sans text-slate-900 overflow-hidden">
      {/* Sidebar Navigation */}
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        pendingKycCount={pendingKycCount}
        activeSosCount={activeSosCount}
        activeTripsCount={trips.filter((t) => t.status === 'in_progress').length}
        pendingPayoutCount={payoutRequests.filter((p) => p.status === 'pending').length}
        adminEmail={adminUser.email}
        onLogout={handleLogout}
        isCollapsed={isSidebarCollapsed}
        onToggleCollapse={handleToggleSidebar}
      />

      {/* Main Content Workspace */}
      <div className="flex-1 flex flex-col min-w-0 h-full overflow-hidden bg-slate-50">
        {/* Top Header */}
        <Header
          searchTerm={searchTerm}
          setSearchTerm={setSearchTerm}
          selectedCity={selectedCity}
          setSelectedCity={setSelectedCity}
          activeSosCount={activeSosCount}
          onRefreshData={handleRefreshData}
          isRefreshing={isRefreshing}
          onOpenSosCenter={() => setActiveTab('safety')}
          onOpenFirebaseModal={() => setIsFirebaseModalOpen(true)}
          notifications={notifications}
          onMarkAsRead={handleMarkNotificationAsRead}
          onMarkAllAsRead={handleMarkAllNotificationsAsRead}
          onNavigate={(tab, targetId) => {
            setActiveTab(tab);
            if (targetId) {
              setSearchTerm(targetId);
            }
          }}
        />

        {/* Toast Alert Banner */}
        {toastMessage && (
          <div className="bg-slate-900 text-white px-4 py-2.5 flex items-center justify-between text-xs font-semibold shadow-lg animate-fadeIn z-30">
            <div className="flex items-center space-x-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>{toastMessage}</span>
            </div>
            <button
              onClick={() => setToastMessage(null)}
              className="text-slate-400 hover:text-white text-sm font-bold pl-3"
            >
              ×
            </button>
          </div>
        )}

        {/* Tab View Router */}
        <main className="flex-1 flex flex-col min-h-0 overflow-hidden">
          {activeTab === 'dashboard' && (
            <DashboardOverview
              trips={filteredTrips}
              drivers={filteredDrivers}
              riders={filteredRiders}
              tickets={tickets}
              feed={feed}
              surgeZones={surgeZones}
              safetyReports={safetyReports}
              payoutRequests={payoutRequests}
              isSidebarCollapsed={isSidebarCollapsed}
              onSelectTrip={(id) => {
                setSelectedTripId(id);
                setActiveTab('dispatch');
              }}
              onNavigateTab={(tab, targetId) => {
                setActiveTab(tab);
                if (targetId) setSearchTerm(targetId);
              }}
            />
          )}

          {activeTab === 'dispatch' && (
            <FleetDispatchMap
              trips={filteredTrips}
              drivers={drivers}
              liveLocations={liveLocations}
              selectedTripId={selectedTripId}
              onSelectTrip={(id) => setSelectedTripId(id)}
              onUpdateTripLocations={handleUpdateTripLocations}
            />
          )}

          {activeTab === 'users' && (
            <UserManagement
              drivers={filteredDrivers}
              riders={filteredRiders}
              onApproveDriverDocument={handleApproveDriverDocument}
              onRejectDriverDocument={handleRejectDriverDocument}
              onToggleDriverStatus={handleToggleDriverStatus}
              onAdjustDriverWallet={handleAdjustDriverWallet}
              onToggleRiderStatus={handleToggleRiderStatus}
            />
          )}

          {activeTab === 'ratings' && (
            <RatingsManagement
              ratings={ratings}
              drivers={drivers}
              riders={riders}
              trips={trips}
              onInspectUser={(userId, role) => {
                setActiveTab('users');
              }}
              onInspectTrip={(tripId) => {
                setSelectedTripId(tripId);
                setActiveTab('dispatch');
              }}
            />
          )}

          {activeTab === 'analytics' && (
            <RealtimeAnalytics trips={filteredTrips} drivers={filteredDrivers} />
          )}

          {activeTab === 'safety' && (
            <SafetySOSCenter
              trips={trips}
              tickets={tickets}
              safetyReports={safetyReports}
              onResolveSosAlert={handleResolveSosAlert}
              onUpdateTicketStatus={handleUpdateTicketStatus}
              onUpdateSafetyReportStatus={handleUpdateSafetyReportStatus}
            />
          )}

          {activeTab === 'pricing' && (
            <PricingControls
              configs={pricingConfigs}
              surgeZones={surgeZones}
              onUpdatePricingConfig={handleUpdatePricingConfig}
              onUpdateSurgeMultiplier={handleUpdateSurgeMultiplier}
            />
          )}

          {activeTab === 'rides' && (
            <TripsManagement
              trips={trips}
              drivers={drivers}
              liveLocations={liveLocations}
              onSelectTripOnMap={(id) => {
                setSelectedTripId(id);
                setActiveTab('dispatch');
              }}
            />
          )}

          {activeTab === 'finance' && (
            <FinancePayouts
              drivers={filteredDrivers}
              trips={filteredTrips}
              payoutRequests={payoutRequests}
              onUpdatePayoutStatus={handleUpdatePayoutStatus}
              onAdjustDriverWallet={handleAdjustDriverWallet}
            />
          )}

          {activeTab === 'ops_center' && (
            <AutonomousOpsCenter
              trips={trips}
              drivers={drivers}
              riders={riders}
              surgeZones={surgeZones}
              safetyReports={safetyReports}
              payoutRequests={payoutRequests}
              recommendations={recommendations}
              feed={feed}
              onExecuteRecommendation={handleExecuteRecommendation}
              onNavigateTab={(tab, targetId) => {
                setActiveTab(tab);
                if (targetId) setSearchTerm(targetId);
              }}
            />
          )}
        </main>
      </div>

      {/* Firebase Settings Modal */}
      <FirebaseConfigModal
        isOpen={isFirebaseModalOpen}
        onClose={() => setIsFirebaseModalOpen(false)}
        onNotify={(msg) => showToast(msg)}
      />
    </div>
  );
}
