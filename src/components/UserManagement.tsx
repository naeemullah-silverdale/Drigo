import React, { useState, useEffect, useMemo } from 'react';
import { Driver, Rider, VehicleType } from '../types';
import {
  Users,
  ShieldCheck,
  Smartphone,
  CheckCircle,
  XCircle,
  AlertTriangle,
  FileText,
  Search,
  DollarSign,
  Ban,
  RotateCcw,
  Star,
  Phone,
  Mail,
  Car,
  Clock,
  Eye,
  ChevronRight,
  UserCheck,
  UserX,
  Filter,
  CreditCard,
  Calendar,
  User,
  ShieldAlert,
  Sparkles,
  Download,
  RefreshCw,
  Cpu,
  Battery,
  Wifi,
  Activity,
  Check,
  X,
  SlidersHorizontal,
  LayoutGrid,
  List
} from 'lucide-react';
import { initFirebaseService } from '../firebase';
import { collection, onSnapshot } from 'firebase/firestore';

// Modular Subcomponents
import { UserStatCards } from './user-management/UserStatCards';
import { UserFilters } from './user-management/UserFilters';
import { UserDetailModal } from './user-management/UserDetailModal';
import { DriverKycModal } from './user-management/DriverKycModal';
import { AdjustWalletModal } from './user-management/AdjustWalletModal';
import { AndroidTelemetryView } from './user-management/AndroidTelemetryView';

interface UserManagementProps {
  drivers: Driver[];
  riders: Rider[];
  onApproveDriverDocument: (driverId: string, docId: string) => void;
  onRejectDriverDocument: (driverId: string, docId: string, reason: string) => void;
  onApproveAllDriverDocuments?: (driverId: string) => void;
  onToggleDriverStatus: (driverId: string, status: Driver['status']) => void;
  onAdjustDriverWallet: (driverId: string, amount: number) => void;
  onToggleRiderStatus: (riderId: string, status: Rider['status']) => void;
}

export const UserManagement: React.FC<UserManagementProps> = ({
  drivers,
  riders,
  onApproveDriverDocument,
  onRejectDriverDocument,
  onApproveAllDriverDocuments,
  onToggleDriverStatus,
  onAdjustDriverWallet,
  onToggleRiderStatus,
}) => {
  // Navigation tabs
  const [subTab, setSubTab] = useState<'all_users' | 'drivers' | 'riders' | 'telemetry'>('all_users');
  const [viewMode, setViewMode] = useState<'table' | 'cards'>('table');

  // Search & Filter State
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState<'all' | 'driver' | 'rider'>('all');
  const [statusFilter, setStatusFilter] = useState<'all' | 'active' | 'pending' | 'suspended_flagged'>('all');

  // Modals State using IDs to guarantee live reactive sync
  const [selectedUserDetailInfo, setSelectedUserDetailInfo] = useState<{ id: string; type: 'driver' | 'rider' } | null>(null);
  const [selectedDriverKycId, setSelectedDriverKycId] = useState<string | null>(null);
  const [selectedDriverWalletId, setSelectedDriverWalletId] = useState<string | null>(null);

  // Firestore Driver Verifications Listener
  const [verificationsData, setVerificationsData] = useState<Record<string, any>>({});
  const [isFirebaseSyncActive, setIsFirebaseSyncActive] = useState<boolean>(false);

  useEffect(() => {
    const { firestore } = initFirebaseService();
    if (!firestore) {
      setIsFirebaseSyncActive(false);
      return;
    }

    try {
      const verificationsRef = collection(firestore, 'driver_verifications');
      const unsubscribe = onSnapshot(
        verificationsRef,
        (snapshot) => {
          const docsMap: Record<string, any> = {};
          snapshot.forEach((docSnap) => {
            docsMap[docSnap.id] = docSnap.data();
          });
          setVerificationsData(docsMap);
          setIsFirebaseSyncActive(true);
        },
        (error) => {
          console.warn('Firestore driver_verifications onSnapshot warning:', error);
          setIsFirebaseSyncActive(false);
        }
      );

      return () => unsubscribe();
    } catch (err) {
      console.warn('Firestore listener setup error in UserManagement:', err);
    }
  }, []);

  // Enrich Drivers with live Firestore Verification State if present
  const enrichedDrivers = useMemo(() => {
    return drivers.map((driver) => {
      const liveKyc = verificationsData[driver.id];
      if (liveKyc && liveKyc.documents && Array.isArray(liveKyc.documents) && liveKyc.documents.length > 0) {
        const liveDocs = liveKyc.documents;
        const mergedDocs = (driver.documents || []).map((localDoc) => {
          const liveMatch = liveDocs.find(
            (ld: any) => ld.id === localDoc.id || ld.docType === localDoc.docType || ld.type === localDoc.type
          );
          if (liveMatch) {
            const isApproved = liveMatch.status === 'APPROVED' || liveMatch.status === 'verified';
            const isRejected = liveMatch.status === 'REJECTED' || liveMatch.status === 'rejected';
            return {
              ...localDoc,
              status: isApproved ? ('verified' as const) : isRejected ? ('rejected' as const) : localDoc.status,
              rejectionReason: liveMatch.rejectionReason || localDoc.rejectionReason || '',
            };
          }
          return localDoc;
        });
        const allVerified = mergedDocs.length > 0 && mergedDocs.every((d) => d.status === 'verified');
        return {
          ...driver,
          documents: mergedDocs,
          verificationStatus: allVerified ? ('APPROVED' as const) : (liveKyc.status || driver.verificationStatus),
        };
      }
      return driver;
    });
  }, [drivers, verificationsData]);

  // Dynamically derived active modal objects
  const activeKycDriver = useMemo(() => {
    if (!selectedDriverKycId) return null;
    return enrichedDrivers.find((d) => d.id === selectedDriverKycId) || null;
  }, [enrichedDrivers, selectedDriverKycId]);

  const activeUserDetail = useMemo(() => {
    if (!selectedUserDetailInfo) return null;
    if (selectedUserDetailInfo.type === 'driver') {
      const d = enrichedDrivers.find((dr) => dr.id === selectedUserDetailInfo.id);
      return d ? { user: d, type: 'driver' as const } : null;
    } else {
      const r = riders.find((rd) => rd.id === selectedUserDetailInfo.id);
      return r ? { user: r, type: 'rider' as const } : null;
    }
  }, [selectedUserDetailInfo, enrichedDrivers, riders]);

  const activeDriverWallet = useMemo(() => {
    if (!selectedDriverWalletId) return null;
    return enrichedDrivers.find((d) => d.id === selectedDriverWalletId) || null;
  }, [enrichedDrivers, selectedDriverWalletId]);

  // Unified Users List
  const unifiedUsers = useMemo(() => {
    const list: Array<{
      id: string;
      fullName: string;
      role: 'driver' | 'rider';
      phone: string;
      email: string;
      avatar: string;
      status: string;
      joinedDate: string;
      rating: number;
      totalTripsOrRides: number;
      walletBalance: number;
      vehicleSummary?: string;
      pendingDocsCount?: number;
      rawObject: Driver | Rider;
    }> = [];

    // Add Drivers
    enrichedDrivers.forEach((d) => {
      const pendingDocs = (d.documents || []).filter((doc) => doc.status === 'pending').length;
      list.push({
        id: d.id,
        fullName: d.fullName || 'Unnamed Driver',
        role: 'driver',
        phone: d.phone || '',
        email: d.email || `${(d.id || '').toLowerCase()}@drigo.app`,
        avatar: d.avatar,
        status: d.status || 'offline',
        joinedDate: d.joinedDate || '2026-01-10',
        rating: d.rating,
        totalTripsOrRides: d.totalTrips || 0,
        walletBalance: d.walletBalance || 0,
        vehicleSummary: `${d.vehicle?.make || 'Toyota'} ${d.vehicle?.model || 'Corolla'} (${d.vehicle?.licensePlate || 'DRG-8B15'})`,
        pendingDocsCount: pendingDocs,
        rawObject: d,
      });
    });

    // Add Riders
    riders.forEach((r) => {
      list.push({
        id: r.id,
        fullName: r.fullName || 'Unnamed Passenger',
        role: 'rider',
        phone: r.phone || '',
        email: r.email || `${(r.id || '').toLowerCase()}@drigo.app`,
        avatar: r.avatar,
        status: r.status || 'active',
        joinedDate: r.joinedDate || '2026-01-12',
        rating: r.rating,
        totalTripsOrRides: r.totalRides || 0,
        walletBalance: r.walletBalance || 0,
        vehicleSummary: undefined,
        pendingDocsCount: 0,
        rawObject: r,
      });
    });

    return list;
  }, [enrichedDrivers, riders]);

  // Filtered Users computation
  const filteredUsers = useMemo(() => {
    return unifiedUsers.filter((u) => {
      // Role filter
      if (roleFilter !== 'all' && u.role !== roleFilter) return false;

      // Status filter
      const s = String(u.status || '').toLowerCase();
      if (statusFilter === 'active') {
        if (s !== 'online' && s !== 'active' && s !== 'on_trip') return false;
      } else if (statusFilter === 'pending') {
        if (u.role !== 'driver' || (u.pendingDocsCount || 0) === 0) return false;
      } else if (statusFilter === 'suspended_flagged') {
        if (s !== 'suspended' && s !== 'flagged') return false;
      }

      // Search term
      if (searchTerm.trim()) {
        const q = searchTerm.toLowerCase().trim();
        const matchName = (u.fullName || '').toLowerCase().includes(q);
        const matchPhone = (u.phone || '').includes(q);
        const matchEmail = (u.email || '').toLowerCase().includes(q);
        const matchId = (u.id || '').toLowerCase().includes(q);
        const matchVehicle = u.vehicleSummary ? u.vehicleSummary.toLowerCase().includes(q) : false;

        if (!matchName && !matchPhone && !matchEmail && !matchId && !matchVehicle) {
          return false;
        }
      }

      return true;
    });
  }, [unifiedUsers, roleFilter, statusFilter, searchTerm]);

  // Aggregate stats
  const totalRegisteredUsers = unifiedUsers.length;
  const driversCount = enrichedDrivers.length;
  const ridersCount = riders.length;
  const activeDriversCount = enrichedDrivers.filter(d => d.status === 'online' || d.status === 'on_trip').length;
  const activeRidersCount = riders.filter(r => r.status === 'active').length;
  const pendingKycCount = enrichedDrivers.filter(d => (d.documents || []).some(doc => doc.status === 'pending')).length;
  const suspendedOrFlaggedCount = unifiedUsers.filter(u => u.status === 'suspended' || u.status === 'flagged').length;

  // CSV Export Handler
  const handleExportCsv = () => {
    const headers = [
      'User ID',
      'Role',
      'Full Name',
      'Phone Number',
      'Email',
      'Account Status',
      'Vehicle & License Plate',
      'Rating',
      'Completed Rides',
      'Wallet Balance (PKR)',
      'Joined Date',
    ];

    const rows = filteredUsers.map((u) => [
      `"${u.id}"`,
      `"${u.role.toUpperCase()}"`,
      `"${u.fullName.replace(/"/g, '""')}"`,
      `"${u.phone}"`,
      `"${u.email}"`,
      `"${u.status.toUpperCase()}"`,
      `"${(u.vehicleSummary || 'N/A').replace(/"/g, '""')}"`,
      u.rating,
      u.totalTripsOrRides,
      (u.walletBalance || 0).toFixed(2),
      `"${u.joinedDate}"`,
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `Drigo_User_Fleet_Directory_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Approve all documents for a driver
  const handleApproveAllDocs = (driverId: string) => {
    if (onApproveAllDriverDocuments) {
      onApproveAllDriverDocuments(driverId);
    } else {
      const targetDriver = enrichedDrivers.find((d) => d.id === driverId);
      if (targetDriver && targetDriver.documents) {
        targetDriver.documents.forEach((doc) => {
          if (doc.status !== 'verified') {
            onApproveDriverDocument(driverId, doc.id);
          }
        });
      }
    }
  };

  return (
    <div className="space-y-4">
      {/* Top Header & Sub-Tabs Navigation */}
      <div className="bg-white p-4 sm:p-5 rounded-3xl border border-slate-200/90 shadow-2xs space-y-3.5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-lg font-black text-slate-900 tracking-tight flex items-center gap-2">
                <Users className="w-5 h-5 text-blue-600" />
                User Directory & Fleet Management
              </h2>
              {isFirebaseSyncActive && (
                <span className="hidden sm:inline-flex items-center gap-1 text-[10px] bg-emerald-50 text-emerald-700 font-bold px-2 py-0.5 rounded-full border border-emerald-200">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                  Firebase Live Sync
                </span>
              )}
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Comprehensive driver KYC verification dossiers, passenger directories, PKR wallet settlements, and low-end Android fleet diagnostics.
            </p>
          </div>

          {/* Sub Navigation Tabs */}
          <div className="flex items-center bg-slate-100 p-1 rounded-2xl border border-slate-200/80 overflow-x-auto">
            <button
              onClick={() => {
                setSubTab('all_users');
                setRoleFilter('all');
              }}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                subTab === 'all_users'
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Users className="w-3.5 h-3.5 text-blue-600" />
              <span>All Directory ({totalRegisteredUsers})</span>
            </button>

            <button
              onClick={() => {
                setSubTab('drivers');
                setRoleFilter('driver');
              }}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                subTab === 'drivers'
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Car className="w-3.5 h-3.5 text-blue-600" />
              <span>Fleet & Drivers ({driversCount})</span>
              {pendingKycCount > 0 && (
                <span className="bg-amber-100 text-amber-800 text-[10px] font-mono px-1.5 py-0.2 rounded-full font-bold">
                  {pendingKycCount}
                </span>
              )}
            </button>

            <button
              onClick={() => {
                setSubTab('riders');
                setRoleFilter('rider');
              }}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                subTab === 'riders'
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <User className="w-3.5 h-3.5 text-emerald-600" />
              <span>Passengers ({ridersCount})</span>
            </button>

            <button
              onClick={() => setSubTab('telemetry')}
              className={`px-3 py-1.5 rounded-xl font-bold text-xs transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                subTab === 'telemetry'
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5 text-purple-600" />
              <span>Android Telemetry</span>
            </button>
          </div>
        </div>
      </div>

      {/* Primary KPI Metric Summary Cards */}
      <UserStatCards
        totalRegisteredUsers={totalRegisteredUsers}
        driversCount={driversCount}
        ridersCount={ridersCount}
        activeDriversCount={activeDriversCount}
        activeRidersCount={activeRidersCount}
        pendingKycCount={pendingKycCount}
        suspendedOrFlaggedCount={suspendedOrFlaggedCount}
        onFilterPendingKyc={() => {
          setSubTab('drivers');
          setRoleFilter('driver');
          setStatusFilter('pending');
        }}
        onFilterActive={() => {
          setStatusFilter('active');
        }}
        onFilterSuspended={() => {
          setStatusFilter('suspended_flagged');
        }}
      />

      {/* Main Content Area */}
      {subTab === 'telemetry' ? (
        <AndroidTelemetryView
          drivers={enrichedDrivers}
          onInspectDriver={(d) => setSelectedUserDetailInfo({ id: d.id, type: 'driver' })}
        />
      ) : (
        <div className="space-y-3.5">
          {/* Filters, Search & View Controls */}
          <UserFilters
            searchTerm={searchTerm}
            onSearchChange={setSearchTerm}
            roleFilter={roleFilter}
            onRoleFilterChange={setRoleFilter}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            viewMode={viewMode}
            onViewModeChange={setViewMode}
            onExportCsv={handleExportCsv}
            totalFilteredCount={filteredUsers.length}
            totalAllCount={totalRegisteredUsers}
            driversCount={driversCount}
            ridersCount={ridersCount}
            activeCount={activeDriversCount + activeRidersCount}
            pendingCount={pendingKycCount}
            suspendedCount={suspendedOrFlaggedCount}
          />

          {/* Results count & status hint */}
          <div className="flex items-center justify-between text-xs text-slate-500 px-1">
            <span>
              Showing <strong className="text-slate-800">{filteredUsers.length}</strong> of {totalRegisteredUsers} accounts
            </span>
            <span className="text-[11px] font-mono text-slate-400">
              PKR Currency Active • Real-time State Preservation
            </span>
          </div>

          {/* TABLE VIEW */}
          {viewMode === 'table' ? (
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xs overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-50/90 border-b border-slate-200/80 text-[11px] font-extrabold text-slate-500 uppercase tracking-wider">
                      <th className="py-3 px-4">User / Account</th>
                      <th className="py-3 px-4">Role</th>
                      <th className="py-3 px-4">Contact Details</th>
                      <th className="py-3 px-4">Vehicle / Category</th>
                      <th className="py-3 px-4">Status & Compliance</th>
                      <th className="py-3 px-4 text-right">Wallet (PKR)</th>
                      <th className="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredUsers.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="py-12 text-center text-slate-400 text-xs">
                          <AlertTriangle className="w-6 h-6 mx-auto mb-2 text-slate-300" />
                          No users matched your current search or filter criteria.
                        </td>
                      </tr>
                    ) : (
                      filteredUsers.map((item) => {
                        const isDriver = item.role === 'driver';
                        const driverObj = isDriver ? (item.rawObject as Driver) : null;
                        const riderObj = !isDriver ? (item.rawObject as Rider) : null;

                        const sLower = String(item.status || '').toLowerCase();
                        const isSuspended = sLower === 'suspended';
                        const isFlagged = sLower === 'flagged';
                        const isPending = sLower === 'pending_verification' || (item.pendingDocsCount || 0) > 0;

                        return (
                          <tr
                            key={item.id}
                            className="hover:bg-slate-50/70 transition-colors group"
                          >
                            {/* User column */}
                            <td className="py-3 px-4">
                              <div
                                onClick={() => setSelectedUserDetailInfo({ id: item.id, type: item.role })}
                                className="flex items-center space-x-3 cursor-pointer group/user"
                              >
                                <img
                                  src={item.avatar}
                                  alt={item.fullName}
                                  className="w-9 h-9 rounded-xl object-cover border border-slate-200 shadow-2xs group-hover/user:ring-2 group-hover/user:ring-blue-400 transition-all"
                                />
                                <div>
                                  <div className="font-extrabold text-slate-900 group-hover/user:text-blue-600 transition-colors flex items-center gap-1.5">
                                    {item.fullName}
                                  </div>
                                  <div className="text-[10px] text-slate-400 font-mono flex items-center gap-1">
                                    <span>ID: {item.id}</span>
                                    <span>•</span>
                                    <span className="flex items-center text-amber-600 font-bold">
                                      <Star className="w-2.5 h-2.5 fill-amber-500 text-amber-500 mr-0.5" />
                                      {item.rating}
                                    </span>
                                  </div>
                                </div>
                              </div>
                            </td>

                            {/* Role badge */}
                            <td className="py-3 px-4">
                              <span
                                className={`text-[10px] font-bold px-2 py-0.5 rounded-md uppercase tracking-wider ${
                                  isDriver
                                    ? 'bg-blue-50 text-blue-700 border border-blue-200'
                                    : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                }`}
                              >
                                {isDriver ? 'Driver' : 'Passenger'}
                              </span>
                            </td>

                            {/* Contact Details */}
                            <td className="py-3 px-4">
                              <div className="space-y-0.5 text-[11px]">
                                <div className="font-semibold text-slate-700 flex items-center gap-1">
                                  <Phone className="w-3 h-3 text-slate-400" />
                                  {item.phone}
                                </div>
                                <div className="text-slate-400 text-[10px] truncate max-w-[160px]">
                                  {item.email}
                                </div>
                              </div>
                            </td>

                            {/* Vehicle / Service Type */}
                            <td className="py-3 px-4">
                              {isDriver && driverObj ? (
                                <div className="space-y-0.5">
                                  <div className="font-bold text-slate-800 text-[11px]">
                                    {driverObj.vehicle?.make} {driverObj.vehicle?.model}
                                  </div>
                                  <span className="text-[10px] font-mono bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200">
                                    {driverObj.vehicle?.licensePlate}
                                  </span>
                                </div>
                              ) : (
                                <span className="text-slate-400 text-[11px] font-mono">
                                  {riderObj?.preferredPayment ? `Pref: ${riderObj.preferredPayment}` : 'Standard Passenger'}
                                </span>
                              )}
                            </td>

                            {/* Status & Compliance */}
                            <td className="py-3 px-4">
                              <div className="flex flex-col items-start gap-1">
                                {isSuspended ? (
                                  <span className="bg-rose-100 text-rose-800 border border-rose-300 text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase">
                                    Suspended
                                  </span>
                                ) : isFlagged ? (
                                  <span className="bg-amber-100 text-amber-800 border border-amber-300 text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase">
                                    Flagged
                                  </span>
                                ) : isPending ? (
                                  <span className="bg-amber-100 text-amber-800 border border-amber-300 text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase animate-pulse">
                                    Pending KYC
                                  </span>
                                ) : (
                                  <span className="bg-emerald-100 text-emerald-800 border border-emerald-300 text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase">
                                    {sLower}
                                  </span>
                                )}

                                {isDriver && (item.pendingDocsCount || 0) > 0 && (
                                  <button
                                    onClick={() => setSelectedDriverKycId(driverObj!.id)}
                                    className="text-[10px] text-blue-600 hover:text-blue-800 font-bold underline flex items-center gap-0.5 cursor-pointer"
                                  >
                                    <Clock className="w-2.5 h-2.5" />
                                    {item.pendingDocsCount} docs to review
                                  </button>
                                )}
                              </div>
                            </td>

                            {/* Wallet Balance PKR */}
                            <td className="py-3 px-4 text-right">
                              <div className="font-mono font-black text-slate-900 text-xs">
                                Rs. {(item.walletBalance || 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })}
                              </div>
                              <div className="text-[10px] text-slate-400">
                                {item.totalTripsOrRides} rides
                              </div>
                            </td>

                            {/* Action Buttons */}
                            <td className="py-3 px-4 text-right">
                              <div className="flex items-center justify-end space-x-1.5">
                                {/* Inspect Profile Button */}
                                <button
                                  onClick={() => setSelectedUserDetailInfo({ id: item.id, type: item.role })}
                                  className="p-1.5 text-slate-600 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                                  title="Inspect Full User Profile & Dossier"
                                >
                                  <Eye className="w-4 h-4" />
                                </button>

                                {/* Driver KYC button */}
                                {isDriver && driverObj && (
                                  <button
                                    onClick={() => setSelectedDriverKycId(driverObj.id)}
                                    className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                                      (item.pendingDocsCount || 0) > 0
                                        ? 'text-amber-700 bg-amber-50 hover:bg-amber-100'
                                        : 'text-slate-600 hover:text-blue-600 hover:bg-blue-50'
                                    }`}
                                    title="Inspect KYC Documents"
                                  >
                                    <FileText className="w-4 h-4" />
                                  </button>
                                )}

                                {/* Adjust Wallet Button for Drivers */}
                                {isDriver && driverObj && (
                                  <button
                                    onClick={() => setSelectedDriverWalletId(driverObj.id)}
                                    className="p-1.5 text-slate-600 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors cursor-pointer"
                                    title="Adjust Driver PKR Wallet Balance"
                                  >
                                    <DollarSign className="w-4 h-4" />
                                  </button>
                                )}

                                {/* Suspend / Reactivate Driver */}
                                {isDriver && (
                                  isSuspended ? (
                                    <button
                                      onClick={() => onToggleDriverStatus(item.id, 'online')}
                                      className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-lg font-bold text-[10px] transition-colors"
                                      title="Reactivate Driver"
                                    >
                                      Reactivate
                                    </button>
                                  ) : (
                                    <button
                                      onClick={() => onToggleDriverStatus(item.id, 'suspended')}
                                      className="px-2 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg font-bold text-[10px] transition-colors"
                                      title="Suspend Driver"
                                    >
                                      Suspend
                                    </button>
                                  )
                                )}

                                {/* Passenger Actions */}
                                {!isDriver && (
                                  isSuspended ? (
                                    <button
                                      onClick={() => onToggleRiderStatus(item.id, 'active')}
                                      className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-lg font-bold text-[10px] transition-colors"
                                      title="Reactivate Passenger"
                                    >
                                      Reactivate
                                    </button>
                                  ) : isFlagged ? (
                                    <button
                                      onClick={() => onToggleRiderStatus(item.id, 'active')}
                                      className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-lg font-bold text-[10px] transition-colors"
                                      title="Clear Flag"
                                    >
                                      Unflag
                                    </button>
                                  ) : (
                                    <button
                                      onClick={() => onToggleRiderStatus(item.id, 'flagged')}
                                      className="px-2 py-1 bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 rounded-lg font-bold text-[10px] transition-colors"
                                      title="Flag Passenger for Policy Review"
                                    >
                                      Flag
                                    </button>
                                  )
                                )}
                              </div>
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
            /* CARD GRID VIEW (Optimized for 320dp-360dp budget phone displays & touch) */
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {filteredUsers.length === 0 ? (
                <div className="col-span-full py-12 bg-white rounded-3xl border border-slate-200 text-center text-slate-400 text-xs">
                  <AlertTriangle className="w-6 h-6 mx-auto mb-2 text-slate-300" />
                  No users match the selected filters.
                </div>
              ) : (
                filteredUsers.map((item) => {
                  const isDriver = item.role === 'driver';
                  const driverObj = isDriver ? (item.rawObject as Driver) : null;
                  const riderObj = !isDriver ? (item.rawObject as Rider) : null;
                  const sLower = String(item.status || '').toLowerCase();
                  const isSuspended = sLower === 'suspended';
                  const isFlagged = sLower === 'flagged';
                  const isPending = sLower === 'pending_verification' || (item.pendingDocsCount || 0) > 0;

                  return (
                    <div
                      key={item.id}
                      className="bg-white p-4 rounded-3xl border border-slate-200/90 shadow-2xs hover:border-blue-300 hover:shadow-md transition-all space-y-3"
                    >
                      {/* Top Header Card */}
                      <div className="flex items-start justify-between">
                        <div
                          onClick={() => setSelectedUserDetailInfo({ id: item.id, type: item.role })}
                          className="flex items-center space-x-3 cursor-pointer"
                        >
                          <img
                            src={item.avatar}
                            alt={item.fullName}
                            className="w-11 h-11 rounded-2xl object-cover border border-slate-200 shadow-2xs shrink-0"
                          />
                          <div>
                            <h4 className="font-extrabold text-xs text-slate-900 hover:text-blue-600 transition-colors">
                              {item.fullName}
                            </h4>
                            <p className="text-[10px] text-slate-400 font-mono">ID: {item.id}</p>
                            <div className="flex items-center gap-1.5 text-[10px] text-slate-500 mt-0.5">
                              <span className="flex items-center font-bold text-slate-800">
                                <Star className="w-2.5 h-2.5 fill-amber-500 text-amber-500 mr-0.5" />
                                {item.rating}
                              </span>
                              <span>•</span>
                              <span>{item.totalTripsOrRides} rides</span>
                            </div>
                          </div>
                        </div>

                        <span
                          className={`text-[9px] font-extrabold px-2 py-0.5 rounded-full uppercase tracking-wider ${
                            isDriver
                              ? 'bg-blue-50 text-blue-700 border border-blue-200'
                              : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          }`}
                        >
                          {isDriver ? 'Driver' : 'Passenger'}
                        </span>
                      </div>

                      {/* Contact & Status Information */}
                      <div className="bg-slate-50 p-2.5 rounded-2xl border border-slate-100 text-xs space-y-1.5">
                        <div className="flex items-center justify-between">
                          <span className="text-slate-500 text-[11px] font-semibold flex items-center gap-1">
                            <Phone className="w-3 h-3 text-slate-400" />
                            {item.phone}
                          </span>
                          <span className="font-mono font-black text-slate-900 text-xs">
                            Rs. {(item.walletBalance || 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })}
                          </span>
                        </div>

                        {isDriver && driverObj && (
                          <div className="flex items-center justify-between text-[11px] pt-1 border-t border-slate-200/60">
                            <span className="font-bold text-slate-700 truncate">
                              {driverObj.vehicle?.make} {driverObj.vehicle?.model}
                            </span>
                            <span className="font-mono text-[10px] bg-white px-1.5 py-0.5 rounded border border-slate-200">
                              {driverObj.vehicle?.licensePlate}
                            </span>
                          </div>
                        )}
                      </div>

                      {/* Status Tag & Pending Verification */}
                      <div className="flex items-center justify-between text-xs">
                        {isSuspended ? (
                          <span className="bg-rose-100 text-rose-800 border border-rose-300 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase">
                            Suspended
                          </span>
                        ) : isFlagged ? (
                          <span className="bg-amber-100 text-amber-800 border border-amber-300 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase">
                            Flagged
                          </span>
                        ) : isPending ? (
                          <span className="bg-amber-100 text-amber-800 border border-amber-300 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase animate-pulse">
                            Pending KYC
                          </span>
                        ) : (
                          <span className="bg-emerald-100 text-emerald-800 border border-emerald-300 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase">
                            {sLower}
                          </span>
                        )}

                        {isDriver && (item.pendingDocsCount || 0) > 0 && (
                          <button
                            onClick={() => setSelectedDriverKycId(driverObj!.id)}
                            className="text-[10px] font-bold text-amber-800 bg-amber-100 border border-amber-300 px-2 py-0.5 rounded-full cursor-pointer hover:bg-amber-200 transition-colors"
                          >
                            Review {item.pendingDocsCount} Docs
                          </button>
                        )}
                      </div>

                      {/* Action Bar */}
                      <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
                        <button
                          onClick={() => setSelectedUserDetailInfo({ id: item.id, type: item.role })}
                          className="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold rounded-xl text-[11px] transition-colors cursor-pointer"
                        >
                          View Profile
                        </button>

                        <div className="flex items-center space-x-1.5">
                          {isDriver && driverObj && (
                            <>
                              <button
                                onClick={() => setSelectedDriverKycId(driverObj.id)}
                                className="p-1.5 text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-xl transition-colors cursor-pointer"
                                title="KYC Docs"
                              >
                                <FileText className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => setSelectedDriverWalletId(driverObj.id)}
                                className="p-1.5 text-emerald-600 bg-emerald-50 hover:bg-emerald-100 rounded-xl transition-colors cursor-pointer"
                                title="Adjust PKR Wallet"
                              >
                                <DollarSign className="w-4 h-4" />
                              </button>
                            </>
                          )}

                          {isDriver && (
                            isSuspended ? (
                              <button
                                onClick={() => onToggleDriverStatus(item.id, 'online')}
                                className="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-[11px] shadow-2xs transition-colors"
                              >
                                Reactivate
                              </button>
                            ) : (
                              <button
                                onClick={() => onToggleDriverStatus(item.id, 'suspended')}
                                className="px-2.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 font-bold rounded-xl text-[11px] transition-colors"
                              >
                                Suspend
                              </button>
                            )
                          )}

                          {!isDriver && (
                            isSuspended ? (
                              <button
                                onClick={() => onToggleRiderStatus(item.id, 'active')}
                                className="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-[11px] shadow-2xs transition-colors"
                              >
                                Reactivate
                              </button>
                            ) : isFlagged ? (
                              <button
                                onClick={() => onToggleRiderStatus(item.id, 'active')}
                                className="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-[11px] shadow-2xs transition-colors"
                              >
                                Unflag
                              </button>
                            ) : (
                              <button
                                onClick={() => onToggleRiderStatus(item.id, 'flagged')}
                                className="px-2.5 py-1.5 bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 font-bold rounded-xl text-[11px] transition-colors"
                              >
                                Flag
                              </button>
                            )
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          )}
        </div>
      )}

      {/* MODAL: Full User Profile & Dossier */}
      {activeUserDetail && (
        <UserDetailModal
          user={activeUserDetail.user}
          type={activeUserDetail.type}
          onClose={() => setSelectedUserDetailInfo(null)}
          onToggleStatus={(id, status) => {
            if (activeUserDetail.type === 'driver') {
              onToggleDriverStatus(id, status);
            } else {
              onToggleRiderStatus(id, status);
            }
          }}
          onOpenKyc={(d) => setSelectedDriverKycId(d.id)}
          onOpenWalletAdjustment={(d) => setSelectedDriverWalletId(d.id)}
        />
      )}

      {/* MODAL: Driver KYC Document Inspector */}
      {activeKycDriver && (
        <DriverKycModal
          driver={activeKycDriver}
          onClose={() => setSelectedDriverKycId(null)}
          onApproveDocument={onApproveDriverDocument}
          onRejectDocument={onRejectDriverDocument}
          onApproveAllDocuments={handleApproveAllDocs}
        />
      )}

      {/* MODAL: Adjust Driver Wallet Balance */}
      {activeDriverWallet && (
        <AdjustWalletModal
          driver={activeDriverWallet}
          onClose={() => setSelectedDriverWalletId(null)}
          onConfirmAdjustment={onAdjustDriverWallet}
        />
      )}
    </div>
  );
};
