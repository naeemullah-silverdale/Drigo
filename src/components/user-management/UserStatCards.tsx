import React from 'react';
import { Users, UserCheck, Clock, UserX, Smartphone, ShieldCheck } from 'lucide-react';

interface UserStatCardsProps {
  totalRegisteredUsers: number;
  driversCount: number;
  ridersCount: number;
  activeDriversCount: number;
  activeRidersCount: number;
  pendingKycCount: number;
  suspendedOrFlaggedCount: number;
  onFilterPendingKyc: () => void;
  onFilterActive: () => void;
  onFilterSuspended: () => void;
}

export const UserStatCards: React.FC<UserStatCardsProps> = ({
  totalRegisteredUsers,
  driversCount,
  ridersCount,
  activeDriversCount,
  activeRidersCount,
  pendingKycCount,
  suspendedOrFlaggedCount,
  onFilterPendingKyc,
  onFilterActive,
  onFilterSuspended,
}) => {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
      {/* Total Registered Accounts */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200/90 shadow-2xs space-y-1 hover:border-slate-300 transition-all">
        <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
          <span>Total Accounts</span>
          <div className="p-1.5 bg-blue-50 text-blue-600 rounded-lg">
            <Users className="w-4 h-4" />
          </div>
        </div>
        <div className="text-2xl font-black text-slate-900 tracking-tight">{totalRegisteredUsers}</div>
        <div className="text-[11px] text-slate-500 flex items-center gap-1.5 flex-wrap">
          <span className="font-bold text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded text-[10px]">{driversCount} Drivers</span>
          <span>•</span>
          <span className="font-bold text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded text-[10px]">{ridersCount} Riders</span>
        </div>
      </div>

      {/* Active & Online Fleet */}
      <div
        onClick={onFilterActive}
        className="bg-white p-4 rounded-2xl border border-emerald-200/80 bg-emerald-50/10 shadow-2xs space-y-1 cursor-pointer hover:border-emerald-300 hover:bg-emerald-50/20 transition-all"
      >
        <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
          <span>Active Fleet / Online</span>
          <div className="p-1.5 bg-emerald-50 text-emerald-600 rounded-lg">
            <UserCheck className="w-4 h-4" />
          </div>
        </div>
        <div className="text-2xl font-black text-emerald-700 tracking-tight">{activeDriversCount + activeRidersCount}</div>
        <div className="text-[11px] text-emerald-700 font-medium truncate">
          {activeDriversCount} online drivers on road
        </div>
      </div>

      {/* Pending Driver KYC */}
      <div
        onClick={onFilterPendingKyc}
        className="bg-white p-4 rounded-2xl border border-amber-200 bg-amber-50/30 shadow-2xs space-y-1 cursor-pointer hover:border-amber-300 hover:bg-amber-50/50 transition-all group"
      >
        <div className="flex items-center justify-between text-slate-600 text-xs font-semibold">
          <span>Pending KYC</span>
          <div className="p-1.5 bg-amber-100 text-amber-700 rounded-lg group-hover:scale-105 transition-transform">
            <Clock className="w-4 h-4 animate-pulse" />
          </div>
        </div>
        <div className="text-2xl font-black text-amber-600 tracking-tight">{pendingKycCount}</div>
        <div className="text-[11px] text-amber-800 font-bold flex items-center gap-1">
          <span>Review submissions</span>
          <span className="text-[10px] bg-amber-200 text-amber-900 px-1.5 py-0.2 rounded-full font-mono">Action</span>
        </div>
      </div>

      {/* Suspended & Flagged */}
      <div
        onClick={onFilterSuspended}
        className="bg-white p-4 rounded-2xl border border-rose-200/80 bg-rose-50/10 shadow-2xs space-y-1 cursor-pointer hover:border-rose-300 hover:bg-rose-50/20 transition-all"
      >
        <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
          <span>Restricted / Flagged</span>
          <div className="p-1.5 bg-rose-50 text-rose-600 rounded-lg">
            <UserX className="w-4 h-4" />
          </div>
        </div>
        <div className="text-2xl font-black text-rose-700 tracking-tight">{suspendedOrFlaggedCount}</div>
        <div className="text-[11px] text-slate-500 truncate">
          Policy enforcement & trust alerts
        </div>
      </div>
    </div>
  );
};
