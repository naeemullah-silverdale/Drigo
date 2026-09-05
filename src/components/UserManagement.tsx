import React, { useState, useEffect } from 'react';
import { Driver, Rider, DocumentVerification, DriverVerification } from '../types';
import { subscribeToDriverVerifications } from '../firebase';
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
  ExternalLink,
  ZoomIn,
  Maximize2,
  Check,
  X,
  FileCheck
} from 'lucide-react';

interface UserManagementProps {
  drivers: Driver[];
  riders: Rider[];
  onApproveDriverDocument: (driverId: string, docId: string) => void;
  onRejectDriverDocument: (driverId: string, docId: string, reason: string) => void;
  onToggleDriverStatus: (driverId: string, status: Driver['status']) => void;
  onAdjustDriverWallet: (driverId: string, amount: number) => void;
  onToggleRiderStatus: (riderId: string, status: Rider['status']) => void;
}

export const UserManagement: React.FC<UserManagementProps> = ({
  drivers,
  riders,
  onApproveDriverDocument,
  onRejectDriverDocument,
  onToggleDriverStatus,
  onAdjustDriverWallet,
  onToggleRiderStatus,
}) => {
  const [subTab, setSubTab] = useState<'all_users' | 'drivers' | 'riders' | 'telemetry'>('all_users');
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState<'all' | 'driver' | 'rider'>('all');
  const [statusFilter, setStatusFilter] = useState<'all' | 'active' | 'pending' | 'suspended_flagged'>('all');
  
  const [driverFilter, setDriverFilter] = useState<'all' | 'pending' | 'active' | 'suspended'>('all');
  const [selectedDriverKyc, setSelectedDriverKyc] = useState<Driver | null>(null);
  const [selectedUserDetail, setSelectedUserDetail] = useState<{ type: 'driver' | 'rider'; user: Driver | Rider } | null>(null);
  const [walletAmount, setWalletAmount] = useState('');
  const [selectedDriverWallet, setSelectedDriverWallet] = useState<Driver | null>(null);

  // Enlarged doc image lightbox viewer
  const [enlargedDoc, setEnlargedDoc] = useState<{ title: string; url: string; docNumber: string; type: string } | null>(null);
  
  // Rejection reason form state inside KYC modal
  const [rejectingDocId, setRejectingDocId] = useState<string | null>(null);
  const [rejectReasonText, setRejectReasonText] = useState<string>('');

  // Live Firebase driver_verifications collection data
  const [liveVerifications, setLiveVerifications] = useState<DriverVerification[]>([]);

  useEffect(() => {
    const unsub = subscribeToDriverVerifications((data) => {
      setLiveVerifications(data || []);
    });
    return () => unsub();
  }, []);

  const handleDriverStatusToggle = (targetId: string, newStatus: Driver['status']) => {
    setLiveVerifications((prev) =>
      prev.map((v) => {
        if (v.id === targetId || v.driverId === targetId) {
          return {
            ...v,
            accountStatus: newStatus === 'suspended' ? 'SUSPENDED' : 'ACTIVE',
            // verificationStatus is strictly UNTOUCHED during suspend/reactivate!
            status: newStatus === 'suspended' ? 'SUSPENDED' : (v.verificationStatus || 'APPROVED'),
          };
        }
        return v;
      })
    );
    onToggleDriverStatus(targetId, newStatus);
  };

  // Calculate KYC summary for a driver based on their actual document verification states
  const getDriverKycSummary = (docs: DocumentVerification[] = []) => {
    if (!docs || docs.length === 0) {
      return {
        status: 'none' as const,
        label: 'KYC Docs (0 Pending)',
        pendingCount: 0,
        verifiedCount: 0,
        rejectedCount: 0,
        badgeStyle: 'bg-slate-100 text-slate-700 border-slate-200',
      };
    }
    const pendingCount = docs.filter(d => d.status === 'pending').length;
    const verifiedCount = docs.filter(d => d.status === 'verified').length;
    const rejectedCount = docs.filter(d => d.status === 'rejected').length;

    if (pendingCount > 0) {
      return {
        status: 'pending' as const,
        label: `KYC Docs (${pendingCount} Pending)`,
        pendingCount,
        verifiedCount,
        rejectedCount,
        badgeStyle: 'bg-amber-100 text-amber-800 border-amber-300 font-bold animate-pulse hover:bg-amber-200',
      };
    }
    if (verifiedCount === docs.length) {
      return {
        status: 'verified' as const,
        label: 'KYC Docs (Verified)',
        pendingCount: 0,
        verifiedCount,
        rejectedCount: 0,
        badgeStyle: 'bg-emerald-100 text-emerald-800 border-emerald-300 font-bold hover:bg-emerald-200',
      };
    }
    if (rejectedCount > 0) {
      return {
        status: 'rejected' as const,
        label: `KYC Docs (${rejectedCount} Rejected)`,
        pendingCount: 0,
        verifiedCount,
        rejectedCount,
        badgeStyle: 'bg-rose-100 text-rose-800 border-rose-300 font-bold hover:bg-rose-200',
      };
    }
    return {
      status: 'pending' as const,
      label: `KYC Docs (${pendingCount} Pending)`,
      pendingCount,
      verifiedCount,
      rejectedCount,
      badgeStyle: 'bg-amber-100 text-amber-800 border-amber-300 font-bold',
    };
  };

  const handleApproveDocInModal = (driverId: string, docId: string) => {
    onApproveDriverDocument(driverId, docId);
    setSelectedDriverKyc((prev) => {
      if (!prev || prev.id !== driverId) return prev;
      const updatedDocs = prev.documents.map((d) =>
        d.id === docId
          ? {
              ...d,
              status: 'verified' as const,
              lastReviewedAt: 'Just now',
              lastReviewedBy: 'Admin (Alex Sterling)',
            }
          : d
      );
      const allVerified = updatedDocs.every((d) => d.status === 'verified');
      return {
        ...prev,
        documents: updatedDocs,
        status: prev.status === 'pending_verification' && allVerified ? 'online' : prev.status,
      };
    });
  };

  const handleRejectDocInModal = (driverId: string, docId: string, reason: string) => {
    const finalReason = reason.trim() || 'Document image illegible or unverified';
    onRejectDriverDocument(driverId, docId, finalReason);
    setSelectedDriverKyc((prev) => {
      if (!prev || prev.id !== driverId) return prev;
      const updatedDocs = prev.documents.map((d) =>
        d.id === docId
          ? {
              ...d,
              status: 'rejected' as const,
              rejectionReason: finalReason,
              lastReviewedAt: 'Just now',
              lastReviewedBy: 'Admin (Alex Sterling)',
            }
          : d
      );
      return {
        ...prev,
        documents: updatedDocs,
      };
    });
    setRejectingDocId(null);
    setRejectReasonText('');
  };

  const handleApproveAllDocsInModal = (driver: Driver) => {
    driver.documents.forEach((d) => {
      if (d.status !== 'verified') {
        onApproveDriverDocument(driver.id, d.id);
      }
    });
    setSelectedDriverKyc((prev) => {
      if (!prev || prev.id !== driver.id) return prev;
      const updatedDocs = prev.documents.map((d) => ({
        ...d,
        status: 'verified' as const,
        lastReviewedAt: 'Just now',
        lastReviewedBy: 'Admin (Alex Sterling)',
      }));
      return {
        ...prev,
        documents: updatedDocs,
        status: prev.status === 'pending_verification' ? 'online' : prev.status,
      };
    });
  };

  const totalRegisteredUsers = drivers.length + riders.length;
  const pendingKycCount = liveVerifications.filter(v => v.status === 'PENDING' || v.status === 'REVIEW').length || drivers.filter(d => d.documents.some(doc => doc.status === 'pending')).length;
  const activeDrivers = drivers.filter(d => d.status === 'online' || d.status === 'on_trip').length;
  const activeRiders = riders.filter(r => r.status === 'active').length;
  const suspendedOrFlaggedCount = drivers.filter(d => d.status === 'suspended').length + riders.filter(r => r.status === 'flagged').length;

  // Combine drivers, riders, and live driver_verifications documents into unified list
  const verifDriverItems = liveVerifications.map(v => {
    const targetDriverId = v.driverId || v.id;
    const matchedDriver = drivers.find(d => d.id === targetDriverId || d.id === v.id);

    const vAccountUpper = String(v.accountStatus || (matchedDriver?.status === 'suspended' ? 'SUSPENDED' : '') || (v.status === 'SUSPENDED' ? 'SUSPENDED' : 'ACTIVE')).toUpperCase();
    const isSuspended = vAccountUpper === 'SUSPENDED' || matchedDriver?.status === 'suspended';

    const vVerifUpper = String(v.verificationStatus || (v.status !== 'SUSPENDED' ? v.status : '') || 'APPROVED').toUpperCase();

    let itemStatus: Driver['status'] = 'pending_verification';
    if (isSuspended) {
      itemStatus = 'suspended';
    } else if (matchedDriver) {
      itemStatus = matchedDriver.status;
    } else if (vVerifUpper.includes('APPROV') || vVerifUpper.includes('VERIF')) {
      itemStatus = 'online';
    }

    const displayRawStatus = isSuspended ? 'SUSPENDED' : (vVerifUpper || 'APPROVED');

    return {
      id: v.id,
      driverId: targetDriverId,
      documentId: v.documentId,
      role: 'driver' as const,
      fullName: v.driverName || v.fullName || 'Driver',
      email: v.email || `${v.id.toLowerCase()}@drigo.app`,
      phone: v.phone || 'N/A',
      avatar: v.profileImage || v.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
      rating: 5.0,
      totalTrips: 0,
      status: itemStatus,
      rawStatus: displayRawStatus,
      accountStatus: isSuspended ? 'SUSPENDED' : 'ACTIVE',
      verificationStatus: vVerifUpper,
      vehicleMakeModel: `${v.vehicleMake} ${v.vehicleModel}`.trim(),
      licensePlate: v.licensePlate,
      details: `${v.vehicleMake} ${v.vehicleModel} (${v.licensePlate})`,
      walletBalance: matchedDriver ? (matchedDriver.walletBalance ?? 0) : 0,
      isVerificationRecord: true,
      rawVerif: v,
      rawUser: matchedDriver
        ? {
            ...matchedDriver,
            status: itemStatus,
            accountStatus: (isSuspended ? 'SUSPENDED' : 'ACTIVE') as any,
            verificationStatus: vVerifUpper as any,
            documents: (v.documents && v.documents.length > 0)
              ? v.documents
              : matchedDriver.documents
          }
        : {
            id: targetDriverId,
            fullName: v.driverName || v.fullName || 'Driver',
            phone: v.phone,
            email: v.email,
            avatar: v.profileImage || v.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
            status: itemStatus,
            accountStatus: isSuspended ? 'SUSPENDED' : 'ACTIVE',
            verificationStatus: vVerifUpper,
            vehicle: { make: v.vehicleMake, model: v.vehicleModel, licensePlate: v.licensePlate, type: 'sedan', year: 2024, color: 'White', seatingCapacity: 4, photoUrl: '', inspectionPassed: true },
            documents: (v.documents && v.documents.length > 0)
              ? v.documents
              : [
                  { id: v.documentId || 'license', type: 'driver_license', title: "National Driver's License", documentNumber: 'DL-882190-X', issueDate: '2023-01-10', expiryDate: '2028-01-10', status: 'pending', fileUrl: 'https://images.unsplash.com/photo-1628155930542-3c7a64e2c833?w=600&auto=format&fit=crop&q=80' },
                  { id: 'veh_reg', type: 'vehicle_registration', title: 'Vehicle Logbook & Registration', documentNumber: 'REG-882190-V', issueDate: '2023-02-15', expiryDate: '2026-02-15', status: 'pending', fileUrl: 'https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=600&auto=format&fit=crop&q=80' }
                ],
            rating: 5.0,
            totalTrips: 0,
            walletBalance: 0,
            todayEarnings: 0,
            joinedDate: v.submittedAt || '2026-09-01',
            currentLocation: { lat: 33.6844, lng: 73.0479, heading: 0, speedKmh: 0 },
            telemetry: { deviceModel: 'Android Smartphone', manufacturer: 'Samsung', androidVersion: 'Android 13', apiLevel: 33, ramTotalGb: 4, ramUsagePercent: 40, batteryLevel: 90, isBatterySaver: false, appVersion: 'v2.4.1', networkType: '4G', networkLatencyMs: 30, gpsAccuracyMeters: 3.0, offlineQueuedPackets: 0, lastPingAt: 'Just now' },
            city: 'Islamabad',
            acceptanceRate: 100,
            completionRate: 100,
            cancellationRate: 0
          } as Driver
    };
  });

  const combinedUsers = [
    // Live verification records from Firebase
    ...verifDriverItems,
    // Existing drivers in state not already in live verifications
    ...drivers
      .filter(d => !verifDriverItems.some(v => v.driverId === d.id || v.id === d.id))
      .map(d => ({
        id: d.id,
        driverId: d.id,
        documentId: d.documents[0]?.id || `DOC-${String(d.id || '').toUpperCase()}`,
        role: 'driver' as const,
        fullName: d.fullName,
        email: d.email || `${String(d.fullName || '').toLowerCase().replace(/\s+/g, '.')}@drigo.app`,
        phone: d.phone,
        avatar: d.avatar,
        rating: d.rating,
        totalTrips: d.totalTrips,
        status: d.status,
        rawStatus: (d.accountStatus === 'SUSPENDED' || d.status === 'suspended') ? 'SUSPENDED' : (d.accountStatus || String(d.status || '').toUpperCase()),
        accountStatus: d.accountStatus || (d.status === 'suspended' ? 'SUSPENDED' : 'ACTIVE'),
        verificationStatus: d.verificationStatus || 'APPROVED',
        vehicleMakeModel: `${d.vehicle.make} ${d.vehicle.model}`,
        licensePlate: d.vehicle.licensePlate,
        joinedDate: d.joinedDate || '2026-01-15',
        details: `${d.vehicle.make} ${d.vehicle.model} (${d.vehicle.licensePlate})`,
        walletBalance: d.walletBalance ?? 0,
        isVerificationRecord: false,
        rawVerif: null,
        rawUser: d,
      })),
    // Passengers
    ...riders.map(r => ({
      id: r.id,
      driverId: r.id,
      documentId: `USR-${String(r.id || '').toUpperCase()}`,
      role: 'rider' as const,
      fullName: r.fullName,
      email: r.email,
      phone: r.phone,
      avatar: r.avatar,
      rating: r.rating,
      totalTrips: r.totalRides,
      status: r.status,
      rawStatus: String(r.accountStatus || r.status || '').toUpperCase(),
      accountStatus: r.accountStatus || (r.status === 'suspended' ? 'SUSPENDED' : r.status === 'flagged' ? 'FLAGGED' : 'ACTIVE'),
      verificationStatus: 'APPROVED' as const,
      vehicleMakeModel: 'N/A (Passenger)',
      licensePlate: 'N/A',
      joinedDate: r.joinedDate || '2026-02-01',
      details: `Payment: ${r.preferredPayment || (r as any).paymentMethod || 'Cash'}`,
      walletBalance: r.walletBalance || 0,
      isVerificationRecord: false,
      rawVerif: null,
      rawUser: r,
    })),
  ];

  const filteredCombinedUsers = combinedUsers.filter(u => {
    // Role filter
    if (roleFilter === 'driver' && u.role !== 'driver') return false;
    if (roleFilter === 'rider' && u.role !== 'rider') return false;

    // Status filter
    if (statusFilter === 'active' && u.status !== 'online' && u.status !== 'on_trip' && u.status !== 'active') return false;
    if (statusFilter === 'pending' && u.status !== 'pending_verification' && u.rawStatus !== 'PENDING' && u.rawStatus !== 'REVIEW') return false;
    if (statusFilter === 'suspended_flagged' && u.status !== 'suspended' && u.status !== 'flagged') return false;

    // Search term
    if (searchTerm.trim()) {
      const q = searchTerm.toLowerCase();
      const matchName = u.fullName.toLowerCase().includes(q);
      const matchPhone = u.phone.toLowerCase().includes(q);
      const matchEmail = u.email.toLowerCase().includes(q);
      const matchId = u.id.toLowerCase().includes(q);
      const matchDocId = (u.documentId || '').toLowerCase().includes(q);
      const matchDetails = u.details.toLowerCase().includes(q);
      return matchName || matchPhone || matchEmail || matchId || matchDocId || matchDetails;
    }

    return true;
  });

  const allDriversCombined = [
    ...verifDriverItems.map(v => v.rawUser),
    ...drivers.filter(d => !verifDriverItems.some(v => v.driverId === d.id || v.id === d.id))
  ];

  const filteredDrivers = allDriversCombined.filter(d => {
    const isSusp = d.status === 'suspended' || d.accountStatus === 'SUSPENDED';
    if (driverFilter === 'pending') return !isSusp && (d.status === 'pending_verification' || d.documents.some(doc => doc.status === 'pending'));
    if (driverFilter === 'active') return !isSusp && (d.status === 'online' || d.status === 'on_trip');
    if (driverFilter === 'suspended') return isSusp;
    return true;
  });

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50">
      {/* Sub-navigation Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <h2 className="text-xl font-extrabold text-slate-900 flex items-center gap-2">
            User Directory & Fleet Management
            <span className="text-xs bg-blue-100 text-blue-800 font-bold px-2.5 py-0.5 rounded-full border border-blue-200">
              Firebase Live Sync
            </span>
          </h2>
          <p className="text-xs text-slate-500">
            View all registered drivers, passengers, pending KYC verification requests, and account safety controls.
          </p>
        </div>

        <div className="flex items-center space-x-1 bg-slate-200/80 p-1 rounded-xl text-xs font-semibold overflow-x-auto">
          <button
            onClick={() => setSubTab('all_users')}
            className={`px-3.5 py-1.5 rounded-lg transition-all flex items-center space-x-1.5 shrink-0 ${
              subTab === 'all_users'
                ? 'bg-slate-900 text-white shadow-xs font-bold'
                : 'text-slate-700 hover:text-slate-900'
            }`}
          >
            <Users className="w-3.5 h-3.5 text-blue-400" />
            <span>All Registered Users ({totalRegisteredUsers})</span>
          </button>
          <button
            onClick={() => setSubTab('drivers')}
            className={`px-3 py-1.5 rounded-lg transition-all shrink-0 ${
              subTab === 'drivers'
                ? 'bg-white text-slate-900 shadow-xs font-bold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Drivers ({drivers.length})
          </button>
          <button
            onClick={() => setSubTab('riders')}
            className={`px-3 py-1.5 rounded-lg transition-all shrink-0 ${
              subTab === 'riders'
                ? 'bg-white text-slate-900 shadow-xs font-bold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Passengers ({riders.length})
          </button>
          <button
            onClick={() => setSubTab('telemetry')}
            className={`px-3 py-1.5 rounded-lg transition-all shrink-0 ${
              subTab === 'telemetry'
                ? 'bg-white text-slate-900 shadow-xs font-bold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Android Telemetry
          </button>
        </div>
      </div>

      {/* ALL REGISTERED USERS TAB */}
      {subTab === 'all_users' && (
        <div className="space-y-5">
          {/* Summary Stat Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs space-y-1">
              <div className="flex items-center justify-between text-slate-500 text-xs">
                <span>Total Registered Accounts</span>
                <Users className="w-4 h-4 text-blue-600" />
              </div>
              <div className="text-2xl font-extrabold text-slate-900">{totalRegisteredUsers}</div>
              <div className="text-[11px] text-slate-500">
                <span className="font-bold text-blue-600">{drivers.length}</span> Drivers • <span className="font-bold text-emerald-600">{riders.length}</span> Passengers
              </div>
            </div>

            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs space-y-1">
              <div className="flex items-center justify-between text-slate-500 text-xs">
                <span>Active & Online Fleet</span>
                <UserCheck className="w-4 h-4 text-emerald-600" />
              </div>
              <div className="text-2xl font-extrabold text-emerald-700">{activeDrivers + activeRiders}</div>
              <div className="text-[11px] text-slate-500">
                {activeDrivers} active drivers on road
              </div>
            </div>

            <div
              onClick={() => {
                setSubTab('drivers');
                setDriverFilter('pending');
              }}
              className="bg-white p-4 rounded-2xl border border-amber-200 bg-amber-50/20 shadow-2xs space-y-1 cursor-pointer hover:border-amber-300 transition-all"
            >
              <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
                <span>Pending Driver KYC</span>
                <Clock className="w-4 h-4 text-amber-600 animate-pulse" />
              </div>
              <div className="text-2xl font-extrabold text-amber-600">{pendingKycCount}</div>
              <div className="text-[11px] text-amber-700 font-medium">
                Click to filter pending driver KYC
              </div>
            </div>

            <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs space-y-1">
              <div className="flex items-center justify-between text-slate-500 text-xs">
                <span>Suspended / Flagged</span>
                <UserX className="w-4 h-4 text-rose-600" />
              </div>
              <div className="text-2xl font-extrabold text-rose-700">{suspendedOrFlaggedCount}</div>
              <div className="text-[11px] text-slate-500">
                Accounts restricted due to policy violations
              </div>
            </div>
          </div>

          {/* Search and Filters Bar */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs space-y-3">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
              {/* Search Box */}
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search registered users by name, phone, email, vehicle, or ID..."
                  className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none focus:bg-white transition-all"
                />
                {searchTerm && (
                  <button
                    onClick={() => setSearchTerm('')}
                    className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600 text-xs font-bold"
                  >
                    ×
                  </button>
                )}
              </div>

              {/* Role Filters */}
              <div className="flex items-center space-x-1.5 bg-slate-100 p-1 rounded-xl text-xs">
                <button
                  onClick={() => setRoleFilter('all')}
                  className={`px-3 py-1 rounded-lg font-bold transition-all ${
                    roleFilter === 'all' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  All Roles ({totalRegisteredUsers})
                </button>
                <button
                  onClick={() => setRoleFilter('driver')}
                  className={`px-3 py-1 rounded-lg font-bold transition-all ${
                    roleFilter === 'driver' ? 'bg-blue-600 text-white shadow-2xs' : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Drivers ({drivers.length})
                </button>
                <button
                  onClick={() => setRoleFilter('rider')}
                  className={`px-3 py-1 rounded-lg font-bold transition-all ${
                    roleFilter === 'rider' ? 'bg-emerald-600 text-white shadow-2xs' : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Passengers ({riders.length})
                </button>
              </div>
            </div>

            {/* Status Filters */}
            <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-slate-100 text-xs">
              <span className="text-slate-400 text-[11px] font-bold flex items-center gap-1">
                <Filter className="w-3 h-3" /> Status Filter:
              </span>
              <button
                onClick={() => setStatusFilter('all')}
                className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                  statusFilter === 'all'
                    ? 'bg-slate-900 text-white'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                All Statuses
              </button>
              <button
                onClick={() => setStatusFilter('active')}
                className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                  statusFilter === 'active'
                    ? 'bg-emerald-600 text-white'
                    : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                }`}
              >
                Active / Online ({activeDrivers + activeRiders})
              </button>
              <button
                onClick={() => setStatusFilter('pending')}
                className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                  statusFilter === 'pending'
                    ? 'bg-amber-600 text-white'
                    : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
                }`}
              >
                Pending Verification ({pendingKycCount})
              </button>
              <button
                onClick={() => setStatusFilter('suspended_flagged')}
                className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                  statusFilter === 'suspended_flagged'
                    ? 'bg-rose-600 text-white'
                    : 'bg-rose-50 text-rose-700 hover:bg-rose-100'
                }`}
              >
                Suspended / Flagged ({suspendedOrFlaggedCount})
              </button>
            </div>
          </div>

          {/* Unified Users Data Table */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-[10px] tracking-wider border-b border-slate-200">
                  <tr>
                    <th className="px-5 py-3">Registered User Profile</th>
                    <th className="px-5 py-3">Role</th>
                    <th className="px-5 py-3">Contact Details</th>
                    <th className="px-5 py-3">Vehicle / Payment Info</th>
                    <th className="px-5 py-3">Account Status</th>
                    <th className="px-5 py-3">Rides & Rating</th>
                    <th className="px-5 py-3 text-right">KYC & ACTIONS</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredCombinedUsers.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-5 py-12 text-center">
                        <div className="max-w-md mx-auto space-y-3">
                          <div className="p-3 bg-slate-100 text-slate-500 rounded-full w-12 h-12 mx-auto flex items-center justify-center">
                            <Users className="w-6 h-6" />
                          </div>
                          <h4 className="text-sm font-bold text-slate-900">
                            No Registered Users Found Matching Filter
                          </h4>
                          <p className="text-xs text-slate-500">
                            Try broadening your search query or switching your role and status filters.
                          </p>
                        </div>
                      </td>
                    </tr>
                  )}

                  {filteredCombinedUsers.map((u) => {
                    const isDriver = u.role === 'driver';
                    const targetDriverObj = isDriver ? (drivers.find(d => d.id === u.driverId) || u.rawUser as Driver) : null;
                    const kycSummary = targetDriverObj ? getDriverKycSummary(targetDriverObj.documents) : null;

                    return (
                      <tr key={`${u.role}-${u.id}`} className="hover:bg-slate-50/80 transition-colors">
                        {/* User Profile */}
                        <td className="px-5 py-3.5">
                          <div className="flex items-center space-x-3">
                            <img
                              src={u.avatar}
                              alt={u.fullName}
                              className="w-9 h-9 rounded-full object-cover border border-slate-200 shrink-0"
                            />
                            <div>
                              <div className="font-bold text-slate-900 flex items-center gap-1.5">
                                {u.fullName}
                              </div>
                              <div className="text-[10px] text-slate-500 font-mono flex items-center gap-1 mt-0.5">
                                <span className="text-slate-400">ID:</span>
                                <span className="font-bold text-slate-700 bg-slate-100 px-1.5 py-0.5 rounded border border-slate-200">
                                  {u.documentId || u.id}
                                </span>
                              </div>
                            </div>
                          </div>
                        </td>

                        {/* Role */}
                        <td className="px-5 py-3.5">
                          {isDriver ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                              <Car className="w-3 h-3 mr-1 text-blue-600" />
                              Driver
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                              <User className="w-3 h-3 mr-1 text-emerald-600" />
                              Passenger
                            </span>
                          )}
                        </td>

                        {/* Contact */}
                        <td className="px-5 py-3.5">
                          <div className="text-slate-800 font-medium flex items-center gap-1">
                            <Phone className="w-3 h-3 text-slate-400" />
                            {u.phone}
                          </div>
                          <div className="text-[11px] text-slate-500 flex items-center gap-1">
                            <Mail className="w-3 h-3 text-slate-400" />
                            {u.email}
                          </div>
                        </td>

                        {/* Vehicle / Payment Info */}
                        <td className="px-5 py-3.5">
                          <div className="font-semibold text-slate-800">
                            {u.vehicleMakeModel || u.details}
                          </div>
                          {isDriver && u.licensePlate && u.licensePlate !== 'N/A' && (
                            <div className="text-[11px] font-mono font-bold text-slate-600 flex items-center gap-1">
                              <span className="text-slate-400">Plate:</span>
                              <span className="bg-slate-100 px-1.5 py-0.2 rounded border border-slate-200">{u.licensePlate}</span>
                            </div>
                          )}
                          {isDriver && (
                            <div className="text-[10px] font-mono text-emerald-700 font-bold mt-0.5">
                              Wallet: Rs. {(u.walletBalance ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </div>
                          )}
                        </td>

                        {/* Account Status */}
                        <td className="px-5 py-3.5">
                          {u.status === 'suspended' || u.accountStatus === 'SUSPENDED' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-[#ffe4e6] text-[#be123c] border border-rose-200">
                              SUSPENDED
                            </span>
                          ) : u.rawStatus === 'PENDING' || u.status === 'pending_verification' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-amber-100 text-amber-800 border border-amber-200 animate-pulse">
                              PENDING
                            </span>
                          ) : u.rawStatus === 'REVIEW' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-blue-100 text-blue-800 border border-blue-200">
                              REVIEW
                            </span>
                          ) : u.status === 'online' || u.status === 'active' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-emerald-100 text-emerald-800 border border-emerald-200">
                              {isDriver ? 'ONLINE' : 'ACTIVE'}
                            </span>
                          ) : u.status === 'on_trip' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-blue-100 text-blue-800 border border-blue-200">
                              ON TRIP
                            </span>
                          ) : u.status === 'flagged' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-orange-100 text-orange-800 border border-orange-200">
                              FLAGGED
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-slate-100 text-slate-800 border border-slate-200">
                              OFFLINE
                            </span>
                          )}
                        </td>

                        {/* Rating & Rides */}
                        <td className="px-5 py-3.5">
                          <div className="flex items-center space-x-1 font-bold text-slate-800">
                            <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                            <span>{u.rating}</span>
                          </div>
                          <div className="text-[11px] text-slate-500">{u.totalTrips} completed rides</div>
                        </td>

                        {/* Combined KYC & Actions Column */}
                        <td className="px-5 py-3.5 text-right">
                          <div className="flex items-center justify-end space-x-2">
                            {isDriver && targetDriverObj && kycSummary ? (
                              <button
                                onClick={() => setSelectedDriverKyc(targetDriverObj)}
                                className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-200 font-semibold rounded-xl text-xs transition-all shadow-2xs flex items-center space-x-1 shrink-0 cursor-pointer"
                                title="Click to view driver KYC documents"
                              >
                                <span>{kycSummary.label}</span>
                              </button>
                            ) : (
                              <span className="text-xs text-slate-400 italic mr-2">N/A (Passenger)</span>
                            )}

                            {isDriver && (u.status === 'suspended' || u.accountStatus === 'SUSPENDED' || u.rawStatus === 'SUSPENDED') ? (
                              <button
                                id={`reactivate-user-${u.id}`}
                                onClick={() => handleDriverStatusToggle(u.driverId || u.id, 'online')}
                                className="px-3 py-1.5 bg-[#059669] hover:bg-emerald-700 text-white font-bold rounded-xl text-xs transition-all shadow-2xs shrink-0 cursor-pointer"
                                title="Reactivate Driver Account"
                              >
                                Reactivate
                              </button>
                            ) : isDriver ? (
                              <button
                                id={`suspend-user-${u.id}`}
                                onClick={() => handleDriverStatusToggle(u.driverId || u.id, 'suspended')}
                                className="px-3 py-1.5 bg-[#fff1f2] hover:bg-rose-100 text-[#be123c] border border-rose-200 font-bold rounded-xl text-xs transition-all shrink-0 cursor-pointer"
                                title="Suspend Driver Account"
                              >
                                Suspend
                              </button>
                            ) : u.status === 'active' ? (
                              <button
                                id={`flag-rider-${u.id}`}
                                onClick={() => onToggleRiderStatus(u.id, 'flagged')}
                                className="px-3 py-1.5 bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 font-bold rounded-xl text-xs transition-all shrink-0 cursor-pointer"
                              >
                                Flag
                              </button>
                            ) : u.status === 'flagged' ? (
                              <button
                                id={`unflag-rider-${u.id}`}
                                onClick={() => onToggleRiderStatus(u.id, 'active')}
                                className="px-3 py-1.5 bg-[#059669] hover:bg-emerald-700 text-white font-bold rounded-xl text-xs transition-all shadow-2xs shrink-0 cursor-pointer"
                              >
                                Unflag
                              </button>
                            ) : null}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* DRIVERS MANAGEMENT TAB */}
      {subTab === 'drivers' && (
        <div className="space-y-4">
          {/* Filter Pills */}
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setDriverFilter('all')}
              className={`px-3 py-1 rounded-full text-xs font-bold ${
                driverFilter === 'all'
                  ? 'bg-slate-900 text-white'
                  : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
              }`}
            >
              All Drivers ({drivers.length})
            </button>
            <button
              onClick={() => setDriverFilter('pending')}
              className={`px-3 py-1 rounded-full text-xs font-bold ${
                driverFilter === 'pending'
                  ? 'bg-amber-600 text-white'
                  : 'bg-amber-50 text-amber-700 border border-amber-200 hover:bg-amber-100'
              }`}
            >
              Pending KYC ({drivers.filter(d => d.documents.some(doc => doc.status === 'pending') || d.status === 'pending_verification').length})
            </button>
            <button
              onClick={() => setDriverFilter('active')}
              className={`px-3 py-1 rounded-full text-xs font-bold ${
                driverFilter === 'active'
                  ? 'bg-emerald-600 text-white'
                  : 'bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100'
              }`}
            >
              Online / On-Trip ({drivers.filter(d => d.status === 'online' || d.status === 'on_trip').length})
            </button>
            <button
              onClick={() => setDriverFilter('suspended')}
              className={`px-3 py-1 rounded-full text-xs font-bold ${
                driverFilter === 'suspended'
                  ? 'bg-rose-600 text-white'
                  : 'bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100'
              }`}
            >
              Suspended ({drivers.filter(d => d.status === 'suspended').length})
            </button>
          </div>

          {/* Drivers Table */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-[10px] tracking-wider border-b border-slate-200">
                  <tr>
                    <th className="px-5 py-3">Driver Profile</th>
                    <th className="px-5 py-3">Vehicle Details</th>
                    <th className="px-5 py-3">Account Status</th>
                    <th className="px-5 py-3">Trips & Rating</th>
                    <th className="px-5 py-3">Wallet Balance</th>
                    <th className="px-5 py-3">Android Device</th>
                    <th className="px-5 py-3 text-right">KYC & ACTIONS</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredDrivers.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-5 py-12 text-center">
                        <div className="max-w-md mx-auto space-y-3">
                          <div className="p-3 bg-amber-50 text-amber-600 rounded-full w-12 h-12 mx-auto flex items-center justify-center">
                            <ShieldCheck className="w-6 h-6" />
                          </div>
                          <h4 className="text-sm font-bold text-slate-900">
                            No Drivers Found Matching Filter
                          </h4>
                          <p className="text-xs text-slate-500">
                            Registered drivers on your mobile app will automatically synchronize with your Firebase backend and appear here.
                          </p>
                        </div>
                      </td>
                    </tr>
                  )}

                  {filteredDrivers.map((driver) => {
                    const kycSummary = getDriverKycSummary(driver.documents);
                    return (
                      <tr key={driver.id} className="hover:bg-slate-50/80 transition-colors">
                        {/* Profile */}
                        <td className="px-5 py-3.5">
                          <div className="flex items-center space-x-3">
                            <img
                              src={driver.avatar}
                              alt={driver.fullName}
                              className="w-9 h-9 rounded-full object-cover border border-slate-200"
                            />
                            <div>
                              <div className="font-bold text-slate-900">{driver.fullName}</div>
                              <div className="text-[11px] text-slate-500">{driver.phone}</div>
                            </div>
                          </div>
                        </td>

                        {/* Vehicle */}
                        <td className="px-5 py-3.5">
                          <div className="font-medium text-slate-800">
                            {driver.vehicle.make} {driver.vehicle.model}
                          </div>
                          <div className="text-[11px] text-slate-500 font-mono">
                            {driver.vehicle.licensePlate} • <span className="uppercase text-blue-600 font-bold">{driver.vehicle.type}</span>
                          </div>
                        </td>

                        {/* Account Status */}
                        <td className="px-5 py-3.5">
                          {driver.status === 'suspended' || driver.accountStatus === 'SUSPENDED' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-[#ffe4e6] text-[#be123c] border border-rose-200">
                              SUSPENDED
                            </span>
                          ) : driver.status === 'online' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-emerald-100 text-emerald-800 border border-emerald-200">
                              ONLINE
                            </span>
                          ) : driver.status === 'on_trip' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-blue-100 text-blue-800 border border-blue-200">
                              ON TRIP
                            </span>
                          ) : driver.status === 'pending_verification' ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-amber-100 text-amber-800 border border-amber-200 animate-pulse">
                              PENDING
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-slate-100 text-slate-800 border border-slate-200">
                              OFFLINE
                            </span>
                          )}
                        </td>

                        {/* Rating */}
                        <td className="px-5 py-3.5">
                          <div className="flex items-center space-x-1 font-bold text-slate-800">
                            <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                            <span>{driver.rating}</span>
                          </div>
                          <div className="text-[11px] text-slate-500">{driver.totalTrips} rides</div>
                        </td>

                        {/* Wallet */}
                        <td className="px-5 py-3.5">
                          <div className={`font-mono font-bold ${(driver.walletBalance ?? 0) < 0 ? 'text-rose-600' : 'text-emerald-700'}`}>
                            ${(driver.walletBalance ?? 0).toFixed(2)}
                          </div>
                          <button
                            onClick={() => setSelectedDriverWallet(driver as Driver)}
                            className="text-[10px] text-blue-600 hover:underline font-bold cursor-pointer"
                          >
                            Adjust Wallet
                          </button>
                        </td>

                        {/* Android Telemetry */}
                        <td className="px-5 py-3.5">
                          <div className="text-[11px] font-semibold text-slate-700">
                            {driver.telemetry.deviceModel}
                          </div>
                          <div className="text-[10px] text-slate-400">
                            {driver.telemetry.ramTotalGb}GB RAM • {driver.telemetry.batteryLevel}% Battery
                          </div>
                        </td>

                        {/* Combined KYC & Actions Column */}
                        <td className="px-5 py-3.5 text-right">
                          <div className="flex items-center justify-end space-x-2">
                            <button
                              onClick={() => setSelectedDriverKyc(driver as Driver)}
                              className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-200 font-semibold rounded-xl text-xs transition-all shadow-2xs flex items-center space-x-1 shrink-0 cursor-pointer"
                              title="Click to view full driver KYC documents"
                            >
                              <span>{kycSummary.label}</span>
                            </button>

                            {(driver.status === 'suspended' || driver.accountStatus === 'SUSPENDED') ? (
                              <button
                                id={`reactivate-driver-${driver.id}`}
                                onClick={() => handleDriverStatusToggle(driver.id, 'online')}
                                className="px-3 py-1.5 bg-[#059669] hover:bg-emerald-700 text-white font-bold rounded-xl text-xs transition-all shadow-2xs shrink-0 cursor-pointer"
                                title="Reactivate Driver Account"
                              >
                                Reactivate
                              </button>
                            ) : (
                              <button
                                id={`suspend-driver-${driver.id}`}
                                onClick={() => handleDriverStatusToggle(driver.id, 'suspended')}
                                className="px-3 py-1.5 bg-[#fff1f2] hover:bg-rose-100 text-[#be123c] border border-rose-200 font-bold rounded-xl text-xs transition-all shrink-0 cursor-pointer"
                                title="Suspend Driver Account"
                              >
                                Suspend
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* RIDERS MANAGEMENT TAB */}
      {subTab === 'riders' && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
          <div className="p-4 border-b border-slate-200 flex items-center justify-between">
            <h3 className="font-bold text-slate-900 text-sm">Registered Passengers / Riders</h3>
            <span className="text-xs text-slate-500 font-mono">Live Node: /riders</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 uppercase font-bold text-[10px] tracking-wider border-b border-slate-200">
                <tr>
                  <th className="px-5 py-3">Passenger Profile</th>
                  <th className="px-5 py-3">Contact</th>
                  <th className="px-5 py-3">Rating & Rides</th>
                  <th className="px-5 py-3">Payment Method</th>
                  <th className="px-5 py-3">Emergency Contact</th>
                  <th className="px-5 py-3">Account Status</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {riders.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-5 py-12 text-center">
                      <div className="max-w-md mx-auto space-y-3">
                        <div className="p-3 bg-blue-50 text-blue-600 rounded-full w-12 h-12 mx-auto flex items-center justify-center">
                          <Users className="w-6 h-6" />
                        </div>
                        <h4 className="text-sm font-bold text-slate-900">
                          No Passengers in Live Database Node <code className="text-blue-600 font-mono">/riders</code>
                        </h4>
                        <p className="text-xs text-slate-500">
                          Your dashboard is listening to your Firebase project <code className="text-slate-800 font-bold font-mono">drigo-8b15c</code>. As passengers register or request rides via your mobile app, they will automatically appear here.
                        </p>
                      </div>
                    </td>
                  </tr>
                )}

                {riders.map((rider) => (
                  <tr key={rider.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="flex items-center space-x-3">
                        <img
                          src={rider.avatar}
                          alt={rider.fullName}
                          className="w-9 h-9 rounded-full object-cover border border-slate-200"
                        />
                        <div>
                          <div className="font-bold text-slate-900">{rider.fullName}</div>
                          <div className="text-[11px] text-slate-500 font-mono">{rider.id}</div>
                        </div>
                      </div>
                    </td>

                    <td className="px-5 py-3.5">
                      <div className="font-medium text-slate-800">{rider.phone}</div>
                      <div className="text-[11px] text-slate-500">{rider.email}</div>
                    </td>

                    <td className="px-5 py-3.5">
                      <div className="flex items-center space-x-1 font-bold text-slate-800">
                        <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                        <span>{rider.rating}</span>
                      </div>
                      <div className="text-[11px] text-slate-500">{rider.totalRides} trips</div>
                    </td>

                    <td className="px-5 py-3.5 font-medium text-slate-700 capitalize">
                      {rider.preferredPayment || 'Cash'}
                    </td>

                    <td className="px-5 py-3.5 text-slate-600">
                      {rider.emergencyContact ? (
                        <div>
                          <div className="font-medium">{rider.emergencyContact.name}</div>
                          <div className="text-[11px] text-slate-400">{rider.emergencyContact.phone}</div>
                        </div>
                      ) : (
                        <span className="text-slate-400 italic">None</span>
                      )}
                    </td>

                    <td className="px-5 py-3.5">
                      {rider.accountStatus === 'SUSPENDED' || rider.status === 'suspended' ? (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase bg-[#ffe4e6] text-[#be123c] border border-rose-200">
                          SUSPENDED
                        </span>
                      ) : rider.accountStatus === 'FLAGGED' || rider.status === 'flagged' ? (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase bg-orange-100 text-orange-800 border border-orange-200">
                          FLAGGED
                        </span>
                      ) : rider.accountStatus === 'DEACTIVATED' || rider.status === 'deactivated' ? (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase bg-purple-100 text-purple-800 border border-purple-200">
                          DEACTIVATED
                        </span>
                      ) : (rider.accountStatus as string) === 'ON_TRIP' || (rider.status as string) === 'on_trip' ? (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase bg-blue-100 text-blue-800 border border-blue-200">
                          ON TRIP
                        </span>
                      ) : rider.accountStatus === 'INACTIVE' || rider.status === 'inactive' ? (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase bg-slate-100 text-slate-700 border border-slate-200">
                          INACTIVE
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase bg-emerald-100 text-emerald-800 border border-emerald-200">
                          ACTIVE
                        </span>
                      )}
                    </td>

                    <td className="px-5 py-3.5 text-right space-x-2">
                      {rider.accountStatus === 'SUSPENDED' || rider.status === 'suspended' ? (
                        <button
                          id={`reactivate-passenger-${rider.id}`}
                          onClick={() => onToggleRiderStatus(rider.id, 'active')}
                          className="px-2.5 py-1 bg-[#059669] hover:bg-emerald-700 text-white font-bold rounded-lg text-[11px] transition-colors shadow-2xs cursor-pointer"
                        >
                          Reactivate
                        </button>
                      ) : rider.accountStatus === 'FLAGGED' || rider.status === 'flagged' ? (
                        <div className="inline-flex items-center space-x-1.5">
                          <button
                            id={`unflag-passenger-${rider.id}`}
                            onClick={() => onToggleRiderStatus(rider.id, 'active')}
                            className="px-2.5 py-1 bg-[#059669] hover:bg-emerald-700 text-white font-bold rounded-lg text-[11px] transition-colors shadow-2xs cursor-pointer"
                          >
                            Unflag
                          </button>
                          <button
                            id={`suspend-passenger-${rider.id}`}
                            onClick={() => onToggleRiderStatus(rider.id, 'suspended')}
                            className="px-2.5 py-1 bg-[#fff1f2] hover:bg-rose-100 text-[#be123c] border border-rose-200 font-bold rounded-lg text-[11px] transition-colors cursor-pointer"
                          >
                            Suspend
                          </button>
                        </div>
                      ) : (
                        <div className="inline-flex items-center space-x-1.5">
                          <button
                            id={`flag-passenger-${rider.id}`}
                            onClick={() => onToggleRiderStatus(rider.id, 'flagged')}
                            className="px-2.5 py-1 bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 font-bold rounded-lg text-[11px] transition-colors cursor-pointer"
                          >
                            Flag
                          </button>
                          <button
                            id={`suspend-passenger-${rider.id}`}
                            onClick={() => onToggleRiderStatus(rider.id, 'suspended')}
                            className="px-2.5 py-1 bg-[#fff1f2] hover:bg-rose-100 text-[#be123c] border border-rose-200 font-bold rounded-lg text-[11px] transition-colors cursor-pointer"
                          >
                            Suspend
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TELEMETRY TAB */}
      {subTab === 'telemetry' && (
        <div className="space-y-4">
          <div className="bg-slate-900 text-white p-5 rounded-2xl border border-slate-800 space-y-3">
            <div className="flex items-center space-x-2">
              <Smartphone className="w-5 h-5 text-emerald-400" />
              <h3 className="font-bold text-sm">Android App Target Compatibility & Performance Telemetry</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Monitors low-end/budget Android smartphones (minSdk 23, 2GB–4GB RAM, Samsung A12 class) running the Drigo Android driver & passenger app.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {drivers.map(d => (
              <div key={d.id} className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs space-y-3">
                <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                  <div className="flex items-center space-x-2">
                    <img src={d.avatar} alt={d.fullName} className="w-7 h-7 rounded-full object-cover" />
                    <div>
                      <div className="font-bold text-xs text-slate-900">{d.fullName}</div>
                      <div className="text-[10px] text-slate-500 font-mono">{d.telemetry.deviceModel}</div>
                    </div>
                  </div>
                  <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded-full font-mono">
                    Android {d.telemetry.androidVersion}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-slate-50 p-2 rounded-xl border border-slate-100">
                    <div className="text-[10px] text-slate-400">Memory Total</div>
                    <div className="font-bold text-slate-800">{d.telemetry.ramTotalGb} GB RAM</div>
                  </div>
                  <div className="bg-slate-50 p-2 rounded-xl border border-slate-100">
                    <div className="text-[10px] text-slate-400">Battery Level</div>
                    <div className="font-bold text-slate-800">{d.telemetry.batteryLevel}%</div>
                  </div>
                  <div className="bg-slate-50 p-2 rounded-xl border border-slate-100">
                    <div className="text-[10px] text-slate-400">App Latency</div>
                    <div className="font-bold text-emerald-600">{d.telemetry.networkLatencyMs}ms ({d.telemetry.networkType})</div>
                  </div>
                  <div className="bg-slate-50 p-2 rounded-xl border border-slate-100">
                    <div className="text-[10px] text-slate-400">GPS Accuracy</div>
                    <div className="font-bold text-slate-800">±{d.telemetry.gpsAccuracyMeters}m</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* SELECTED USER DETAILED PROFILE MODAL */}
      {selectedUserDetail && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5 border border-slate-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center space-x-3">
                <img
                  src={selectedUserDetail.user.avatar}
                  alt={selectedUserDetail.user.fullName}
                  className="w-12 h-12 rounded-full object-cover border-2 border-slate-200"
                />
                <div>
                  <h3 className="text-base font-extrabold text-slate-900 flex items-center gap-2">
                    {selectedUserDetail.user.fullName}
                    <span className={`text-[10px] px-2 py-0.5 rounded-full uppercase font-bold ${
                      selectedUserDetail.type === 'driver' ? 'bg-blue-100 text-blue-800' : 'bg-emerald-100 text-emerald-800'
                    }`}>
                      {selectedUserDetail.type}
                    </span>
                  </h3>
                  <p className="text-xs text-slate-500 font-mono">ID: {selectedUserDetail.user.id}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedUserDetail(null)}
                className="text-slate-400 hover:text-slate-700 text-xl font-bold p-1 rounded-lg"
              >
                ×
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200 space-y-2">
                <div className="font-bold text-slate-900 flex items-center gap-1.5">
                  <User className="w-4 h-4 text-blue-600" />
                  Account Overview
                </div>
                <div className="grid grid-cols-2 gap-2 text-slate-600">
                  <div><span className="font-semibold text-slate-900">Phone:</span> {selectedUserDetail.user.phone}</div>
                  <div><span className="font-semibold text-slate-900">Rating:</span> {selectedUserDetail.user.rating} ★</div>
                  <div><span className="font-semibold text-slate-900">Status:</span> {selectedUserDetail.user.status}</div>
                  <div><span className="font-semibold text-slate-900">Email:</span> {selectedUserDetail.user.email || 'N/A'}</div>
                </div>
              </div>

              {selectedUserDetail.type === 'driver' && (
                <div className="bg-blue-50/70 p-3.5 rounded-xl border border-blue-200 space-y-2">
                  <div className="font-bold text-blue-900 flex items-center gap-1.5">
                    <Car className="w-4 h-4 text-blue-600" />
                    Vehicle & Wallet Details
                  </div>
                  <div className="text-blue-900">
                    <div>Vehicle: {(selectedUserDetail.user as Driver).vehicle?.make} {(selectedUserDetail.user as Driver).vehicle?.model} ({(selectedUserDetail.user as Driver).vehicle?.licensePlate})</div>
                    <div>Category: {String((selectedUserDetail.user as Driver).vehicle?.type || '').toUpperCase()}</div>
                    <div>Wallet Balance: ${(Number((selectedUserDetail.user as Driver).walletBalance) || 0).toFixed(2)}</div>
                  </div>
                </div>
              )}
            </div>

            <div className="pt-2 border-t border-slate-100 flex justify-end">
              <button
                onClick={() => setSelectedUserDetail(null)}
                className="px-4 py-2 bg-slate-900 text-white font-bold text-xs rounded-xl"
              >
                Close Profile
              </button>
            </div>
          </div>
        </div>
      )}

      {/* KYC DOCUMENT INSPECTION MODAL */}
      {selectedDriverKyc && (() => {
        const kycSummary = getDriverKycSummary(selectedDriverKyc.documents);
        const pendingDocsCount = selectedDriverKyc.documents.filter(d => d.status === 'pending').length;

        return (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn overflow-y-auto">
            <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl space-y-5 border border-slate-200 my-8">
              {/* Modal Header */}
              <div className="flex items-start justify-between border-b border-slate-100 pb-4">
                <div className="flex items-center space-x-3">
                  <img
                    src={selectedDriverKyc.avatar}
                    alt={selectedDriverKyc.fullName}
                    className="w-12 h-12 rounded-full object-cover border-2 border-slate-200"
                  />
                  <div>
                    <div className="flex items-center space-x-2">
                      <h3 className="text-base font-extrabold text-slate-900">
                        {selectedDriverKyc.fullName}
                      </h3>
                      <span className={`text-[10px] px-2 py-0.5 rounded-full border ${kycSummary.badgeStyle}`}>
                        {kycSummary.label}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 font-mono mt-0.5">
                      {selectedDriverKyc.vehicle.make} {selectedDriverKyc.vehicle.model} • <span className="font-bold text-slate-700">{selectedDriverKyc.vehicle.licensePlate}</span>
                    </p>
                    <div className="flex items-center space-x-3 text-[11px] text-slate-500 mt-1">
                      <span><Phone className="w-3 h-3 inline mr-1 text-slate-400" />{selectedDriverKyc.phone}</span>
                      <span><Mail className="w-3 h-3 inline mr-1 text-slate-400" />{selectedDriverKyc.email}</span>
                      <span>
                        Account Status: <strong className={`uppercase ${selectedDriverKyc.status === 'suspended' ? 'text-rose-600' : 'text-emerald-600'}`}>{selectedDriverKyc.status}</strong>
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  {pendingDocsCount > 0 && (
                    <button
                      onClick={() => handleApproveAllDocsInModal(selectedDriverKyc)}
                      className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center gap-1"
                    >
                      <CheckCircle className="w-3.5 h-3.5" />
                      Approve All Docs
                    </button>
                  )}
                  <button
                    onClick={() => {
                      setSelectedDriverKyc(null);
                      setRejectingDocId(null);
                    }}
                    className="text-slate-400 hover:text-slate-700 text-xl font-bold p-1 rounded-lg"
                  >
                    ×
                  </button>
                </div>
              </div>

              {/* Instructions Banner */}
              <div className="bg-blue-50/60 p-3 rounded-xl border border-blue-200/80 flex items-center justify-between text-xs text-blue-900">
                <div className="flex items-center space-x-2">
                  <ShieldCheck className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>
                    Inspect uploaded driver documents carefully against official identity records. Approving documents updates your Firebase backend in real-time.
                  </span>
                </div>
                <span className="text-[10px] bg-blue-100 text-blue-800 font-bold px-2 py-0.5 rounded-md font-mono shrink-0 ml-2">
                  {selectedDriverKyc.documents.length} Docs
                </span>
              </div>

              {/* Document Cards List */}
              <div className="space-y-4 max-h-[60vh] overflow-y-auto pr-1">
                {selectedDriverKyc.documents.map((doc) => {
                  const isRejecting = rejectingDocId === doc.id;
                  const hasImage = Boolean(doc.fileUrl);

                  return (
                    <div
                      key={doc.id}
                      className="p-4 bg-slate-50 border border-slate-200 rounded-2xl space-y-3 transition-all hover:border-slate-300"
                    >
                      {/* Doc Header Row */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-2.5">
                          <div className="p-2 bg-blue-100/80 text-blue-700 rounded-xl">
                            <FileText className="w-5 h-5" />
                          </div>
                          <div>
                            <h4 className="font-extrabold text-xs text-slate-900">{doc.title}</h4>
                            <p className="text-[10px] text-slate-500 font-mono">
                              Doc #{doc.documentNumber || 'DL-8B15-992'} • Type: <span className="uppercase font-bold text-slate-700">{String(doc.type || '').replace('_', ' ')}</span>
                            </p>
                          </div>
                        </div>

                        <div className="flex items-center space-x-2">
                          <span className={`text-[10px] font-extrabold px-2.5 py-0.5 rounded-full uppercase tracking-wider ${
                            doc.status === 'verified'
                              ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                              : doc.status === 'rejected'
                              ? 'bg-rose-100 text-rose-800 border border-rose-300'
                              : 'bg-amber-100 text-amber-800 border border-amber-300 animate-pulse'
                          }`}>
                            {doc.status}
                          </span>
                        </div>
                      </div>

                      {/* Metadata Grid */}
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-[11px] bg-white p-2.5 rounded-xl border border-slate-100">
                        <div>
                          <span className="text-slate-400 block text-[10px]">Issue Date</span>
                          <span className="font-semibold text-slate-700">{doc.issueDate || '2023-01-15'}</span>
                        </div>
                        <div>
                          <span className="text-slate-400 block text-[10px]">Expiry Date</span>
                          <span className="font-semibold text-slate-700">{doc.expiryDate || '2028-01-15'}</span>
                        </div>
                        <div>
                          <span className="text-slate-400 block text-[10px]">Last Review</span>
                          <span className="font-semibold text-slate-700">{doc.lastReviewedAt || 'Submitted'}</span>
                        </div>
                      </div>

                      {/* Rejection Reason Box if Rejected */}
                      {doc.status === 'rejected' && doc.rejectionReason && (
                        <div className="p-2.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start space-x-2">
                          <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                          <div>
                            <span className="font-bold">Rejection Reason:</span> {doc.rejectionReason}
                          </div>
                        </div>
                      )}

                      {/* Document File / Preview Area */}
                      <div className="relative group bg-slate-900 rounded-xl overflow-hidden border border-slate-800 h-44 flex items-center justify-center">
                        {hasImage ? (
                          <img
                            src={doc.fileUrl}
                            alt={doc.title}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300 opacity-90"
                          />
                        ) : (
                          <div className="text-center p-4 space-y-2">
                            <FileCheck className="w-8 h-8 text-blue-400 mx-auto" />
                            <p className="text-xs text-slate-300 font-mono">Google Drive Document File Reference</p>
                            <span className="text-[10px] text-slate-400 bg-slate-800 px-2 py-1 rounded font-mono">
                              ID: {doc.id}
                            </span>
                          </div>
                        )}

                        {/* Hover Overlay with Action Buttons */}
                        <div className="absolute inset-0 bg-slate-900/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center space-x-3 p-4">
                          <button
                            onClick={() => setEnlargedDoc({ title: doc.title, url: doc.fileUrl || 'https://images.unsplash.com/photo-1628155930542-3c7a64e2c833?w=800&auto=format&fit=crop&q=80', docNumber: doc.documentNumber || 'DL-8B15-992', type: doc.type })}
                            className="px-3 py-1.5 bg-white text-slate-900 font-bold text-xs rounded-lg shadow-lg hover:bg-slate-100 flex items-center gap-1.5 transition-all"
                          >
                            <ZoomIn className="w-3.5 h-3.5 text-blue-600" />
                            Zoom Preview
                          </button>
                          {doc.fileUrl && (
                            <a
                              href={doc.fileUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="px-3 py-1.5 bg-slate-800 text-white font-bold text-xs rounded-lg shadow-lg hover:bg-slate-700 flex items-center gap-1.5 transition-all"
                            >
                              <ExternalLink className="w-3.5 h-3.5" />
                              Open File
                            </a>
                          )}
                        </div>
                      </div>

                      {/* Action Buttons Row for each Doc */}
                      <div className="flex items-center justify-between pt-1">
                        <div className="text-[10px] text-slate-400 font-mono">
                          Ref: {doc.id}
                        </div>

                        {!isRejecting ? (
                          <div className="flex items-center space-x-2">
                            {doc.status !== 'verified' && (
                              <button
                                onClick={() => handleApproveDocInModal(selectedDriverKyc.id, doc.id)}
                                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl transition-all shadow-2xs flex items-center gap-1"
                              >
                                <Check className="w-3.5 h-3.5" />
                                Verify Document
                              </button>
                            )}

                            {doc.status !== 'rejected' && (
                              <button
                                onClick={() => {
                                  setRejectingDocId(doc.id);
                                  setRejectReasonText('Document image illegible or unverified');
                                }}
                                className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 font-bold text-xs rounded-xl transition-all flex items-center gap-1"
                              >
                                <X className="w-3.5 h-3.5" />
                                Reject
                              </button>
                            )}
                          </div>
                        ) : (
                          /* Inline Rejection Reason Form */
                          <div className="w-full bg-rose-50 p-3 rounded-xl border border-rose-200 space-y-2.5 animate-fadeIn">
                            <label className="block text-xs font-bold text-rose-900">
                              Select or type reason for rejecting {doc.title}:
                            </label>
                            <select
                              value={rejectReasonText}
                              onChange={(e) => setRejectReasonText(e.target.value)}
                              className="w-full p-2 bg-white border border-rose-300 rounded-lg text-xs font-medium focus:ring-2 focus:ring-rose-500 focus:outline-none"
                            >
                              <option value="Document image illegible or blurry">Document image illegible or blurry</option>
                              <option value="Expired driver license or permit">Expired driver license or permit</option>
                              <option value="Vehicle registration plate mismatch">Vehicle registration plate mismatch</option>
                              <option value="Commercial insurance policy expired">Commercial insurance policy expired</option>
                              <option value="Custom">Other custom reason...</option>
                            </select>

                            {rejectReasonText === 'Custom' && (
                              <input
                                type="text"
                                placeholder="Enter specific rejection reason..."
                                onChange={(e) => setRejectReasonText(e.target.value)}
                                className="w-full p-2 bg-white border border-rose-300 rounded-lg text-xs"
                              />
                            )}

                            <div className="flex justify-end space-x-2 pt-1">
                              <button
                                onClick={() => setRejectingDocId(null)}
                                className="px-3 py-1 bg-slate-200 text-slate-700 font-bold text-xs rounded-lg"
                              >
                                Cancel
                              </button>
                              <button
                                onClick={() => handleRejectDocInModal(selectedDriverKyc.id, doc.id, rejectReasonText)}
                                className="px-3 py-1 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-lg"
                              >
                                Confirm Rejection
                              </button>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Modal Footer */}
              <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs">
                <span className="text-slate-400">
                  Real-time sync active for driver <code className="font-mono text-slate-700">{selectedDriverKyc.id}</code>
                </span>
                <button
                  onClick={() => {
                    setSelectedDriverKyc(null);
                    setRejectingDocId(null);
                  }}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-xl transition-all"
                >
                  Close KYC Inspection
                </button>
              </div>
            </div>
          </div>
        );
      })()}

      {/* ENLARGED DOCUMENT LIGHTBOX MODAL */}
      {enlargedDoc && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-slate-900 text-white rounded-2xl max-w-3xl w-full p-5 shadow-2xl space-y-4 border border-slate-800">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div>
                <h3 className="font-extrabold text-sm text-white flex items-center gap-2">
                  <FileText className="w-4 h-4 text-blue-400" />
                  {enlargedDoc.title}
                </h3>
                <p className="text-xs text-slate-400 font-mono">Doc #{enlargedDoc.docNumber}</p>
              </div>
              <button
                onClick={() => setEnlargedDoc(null)}
                className="text-slate-400 hover:text-white text-2xl font-bold px-2 py-0.5 rounded-lg"
              >
                ×
              </button>
            </div>

            <div className="max-h-[70vh] overflow-auto rounded-xl border border-slate-800 bg-slate-950 flex items-center justify-center p-2">
              <img
                src={enlargedDoc.url}
                alt={enlargedDoc.title}
                className="max-w-full max-h-[65vh] object-contain rounded-lg"
              />
            </div>

            <div className="flex justify-between items-center text-xs pt-1">
              <span className="text-slate-400">Click outside or press Close to exit document viewer.</span>
              <button
                onClick={() => setEnlargedDoc(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-xl"
              >
                Close Viewer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ADJUST DRIVER WALLET MODAL */}
      {selectedDriverWallet && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl space-y-4 border border-slate-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-2">
              <h3 className="font-bold text-sm text-slate-900">
                Adjust Wallet — {selectedDriverWallet.fullName}
              </h3>
              <button
                onClick={() => setSelectedDriverWallet(null)}
                className="text-slate-400 hover:text-slate-700 text-lg font-bold"
              >
                ×
              </button>
            </div>

            <div className="space-y-2 text-xs">
              <p className="text-slate-500">
                Current Wallet Balance: <span className="font-bold text-slate-900">${(selectedDriverWallet?.walletBalance ?? 0).toFixed(2)}</span>
              </p>
              <label className="block font-semibold text-slate-700">Adjustment Amount ($)</label>
              <input
                type="number"
                value={walletAmount}
                onChange={(e) => setWalletAmount(e.target.value)}
                placeholder="e.g. 50 or -25"
                className="w-full p-2.5 bg-slate-50 border border-slate-300 rounded-xl font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div className="flex items-center justify-end space-x-2 pt-2">
              <button
                onClick={() => setSelectedDriverWallet(null)}
                className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  const amt = parseFloat(walletAmount);
                  if (!isNaN(amt)) {
                    onAdjustDriverWallet(selectedDriverWallet.id, amt);
                    setSelectedDriverWallet(null);
                    setWalletAmount('');
                  }
                }}
                className="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl"
              >
                Apply Adjustment
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
