import React from 'react';
import {
  TrendingUp,
  Clock,
  Wallet,
  DollarSign,
  ArrowUpRight,
  Sparkles,
  Layers,
  Banknote,
  CheckCircle2,
  Calendar
} from 'lucide-react';
import { PayoutRequest, Trip, Driver } from '../../types';

interface FinanceStatCardsProps {
  totalCommission: number;
  pendingAmount: number;
  pendingCount: number;
  totalDriverWalletBalance: number;
  totalGrossFares: number;
  completedTripsCount: number;
  driversCount: number;
  timeframe: 'all' | 'today' | 'week' | 'month';
  onTimeframeChange: (tf: 'all' | 'today' | 'week' | 'month') => void;
}

export const FinanceStatCards: React.FC<FinanceStatCardsProps> = ({
  totalCommission,
  pendingAmount,
  pendingCount,
  totalDriverWalletBalance,
  totalGrossFares,
  completedTripsCount,
  driversCount,
  timeframe,
  onTimeframeChange,
}) => {
  return (
    <div className="space-y-4">
      {/* Timeframe selector header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center space-x-2">
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
            <Calendar className="w-3.5 h-3.5 text-slate-400" />
            Financial Period
          </span>
          <div className="inline-flex p-1 bg-slate-200/70 rounded-xl">
            {(['all', 'today', 'week', 'month'] as const).map((tf) => (
              <button
                key={tf}
                onClick={() => onTimeframeChange(tf)}
                className={`px-3 py-1 text-xs font-bold rounded-lg transition-all cursor-pointer ${
                  timeframe === tf
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {tf === 'all' && 'All Time'}
                {tf === 'today' && 'Today'}
                {tf === 'week' && 'This Week'}
                {tf === 'month' && 'This Month'}
              </button>
            ))}
          </div>
        </div>

        <div className="text-[11px] font-semibold text-slate-500 bg-white px-3 py-1 rounded-xl border border-slate-200 shadow-2xs">
          Currency: <span className="text-emerald-700 font-bold font-mono">Pakistani Rupee (PKR / Rs.)</span>
        </div>
      </div>

      {/* 4 Core Financial KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Platform Commission */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs hover:shadow-md transition-shadow relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-full blur-2xl -mr-6 -mt-6 group-hover:bg-blue-500/10 transition-colors" />
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Drigo Commission Earned
            </span>
            <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold border border-blue-100 shadow-2xs">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2.5">
            <div className="text-2xl sm:text-3xl font-extrabold text-slate-900 font-mono tracking-tight">
              Rs. {Math.round(totalCommission || 0).toLocaleString('en-US')}
            </div>
            <div className="flex items-center space-x-1.5 mt-1.5">
              <span className="text-[11px] text-emerald-700 font-bold bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200/60">
                18% Platform Take
              </span>
              <span className="text-[11px] text-slate-500 font-medium">from completed rides</span>
            </div>
          </div>
        </div>

        {/* Card 2: Pending Payout Queue */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs hover:shadow-md transition-shadow relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-24 h-24 bg-amber-500/5 rounded-full blur-2xl -mr-6 -mt-6 group-hover:bg-amber-500/10 transition-colors" />
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Pending Payout Queue
            </span>
            <div className={`w-9 h-9 rounded-xl flex items-center justify-center font-bold border shadow-2xs ${
              pendingCount > 0
                ? 'bg-amber-50 text-amber-600 border-amber-200 animate-pulse'
                : 'bg-slate-50 text-slate-400 border-slate-200'
            }`}>
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2.5">
            <div className="text-2xl sm:text-3xl font-extrabold text-amber-600 font-mono tracking-tight">
              Rs. {Math.round(pendingAmount || 0).toLocaleString('en-US')}
            </div>
            <div className="flex items-center space-x-1.5 mt-1.5">
              <span className={`text-[11px] font-bold px-2 py-0.5 rounded-md border ${
                pendingCount > 0
                  ? 'bg-amber-50 text-amber-800 border-amber-200'
                  : 'bg-slate-50 text-slate-600 border-slate-200'
              }`}>
                {pendingCount} Withdrawal{pendingCount !== 1 ? 's' : ''}
              </span>
              <span className="text-[11px] text-slate-500 font-medium">awaiting settlement</span>
            </div>
          </div>
        </div>

        {/* Card 3: Driver Wallet Liability */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs hover:shadow-md transition-shadow relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-24 h-24 bg-indigo-500/5 rounded-full blur-2xl -mr-6 -mt-6 group-hover:bg-indigo-500/10 transition-colors" />
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Driver Wallet Liability
            </span>
            <div className="w-9 h-9 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold border border-indigo-100 shadow-2xs">
              <Wallet className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2.5">
            <div className="text-2xl sm:text-3xl font-extrabold text-slate-900 font-mono tracking-tight">
              Rs. {Math.round(totalDriverWalletBalance || 0).toLocaleString('en-US')}
            </div>
            <div className="flex items-center space-x-1.5 mt-1.5">
              <span className="text-[11px] text-indigo-700 font-bold bg-indigo-50 px-2 py-0.5 rounded-md border border-indigo-200/60">
                {driversCount} Driver Accounts
              </span>
              <span className="text-[11px] text-slate-500 font-medium">active balances</span>
            </div>
          </div>
        </div>

        {/* Card 4: Gross Ride Volume */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs hover:shadow-md transition-shadow relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/5 rounded-full blur-2xl -mr-6 -mt-6 group-hover:bg-emerald-500/10 transition-colors" />
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Gross Fare Volume
            </span>
            <div className="w-9 h-9 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold border border-emerald-100 shadow-2xs">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2.5">
            <div className="text-2xl sm:text-3xl font-extrabold text-slate-900 font-mono tracking-tight">
              Rs. {Math.round(totalGrossFares || 0).toLocaleString('en-US')}
            </div>
            <div className="flex items-center space-x-1.5 mt-1.5">
              <span className="text-[11px] text-emerald-700 font-bold bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200/60">
                {completedTripsCount} Completed
              </span>
              <span className="text-[11px] text-slate-500 font-medium">fares processed</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
