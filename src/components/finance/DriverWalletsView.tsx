import React, { useState, useMemo } from 'react';
import {
  Wallet,
  Search,
  Filter,
  DollarSign,
  TrendingUp,
  History,
  ArrowUpRight,
  ArrowDownLeft,
  AlertTriangle,
  User,
  Phone,
  Car,
  ChevronRight,
  X,
  CreditCard,
  PlusCircle,
  Clock
} from 'lucide-react';
import { Driver, Trip, PayoutRequest } from '../../types';

interface DriverWalletsViewProps {
  drivers: Driver[];
  trips: Trip[];
  payoutRequests: PayoutRequest[];
  onOpenAdjustModal: (driver: Driver) => void;
  onRequestPayoutForDriver?: (driver: Driver) => void;
}

export const DriverWalletsView: React.FC<DriverWalletsViewProps> = ({
  drivers,
  trips,
  payoutRequests,
  onOpenAdjustModal,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [balanceFilter, setBalanceFilter] = useState<'all' | 'positive' | 'negative' | 'high'>('all');
  const [vehicleFilter, setVehicleFilter] = useState<'all' | 'boda_bike' | 'tuktuk_auto' | 'sedan' | 'comfort' | 'xl_van'>('all');
  
  // Selected driver for ledger inspection
  const [selectedDriverForHistory, setSelectedDriverForHistory] = useState<Driver | null>(null);

  // Filter drivers
  const filteredDrivers = useMemo(() => {
    return drivers.filter((d) => {
      const q = (searchTerm || '').toLowerCase().trim();
      const matchSearch =
        !q ||
        (d.fullName || '').toLowerCase().includes(q) ||
        (d.phone || '').includes(q) ||
        (d.id || '').toLowerCase().includes(q) ||
        (d.vehicle?.licensePlate ? d.vehicle.licensePlate.toLowerCase().includes(q) : false);

      let matchBalance = true;
      const balance = d.walletBalance || 0;
      if (balanceFilter === 'positive') matchBalance = balance > 0;
      else if (balanceFilter === 'negative') matchBalance = balance < 0;
      else if (balanceFilter === 'high') matchBalance = balance >= 5000;

      let matchVehicle = true;
      if (vehicleFilter !== 'all') {
        const vType = d.vehicle?.type;
        matchVehicle = vType === vehicleFilter;
      }

      return matchSearch && matchBalance && matchVehicle;
    });
  }, [drivers, searchTerm, balanceFilter, vehicleFilter]);

  // Aggregate stats
  const totalBalance = drivers.reduce((sum, d) => sum + (d.walletBalance || 0), 0);
  const positiveDriversCount = drivers.filter((d) => (d.walletBalance || 0) > 0).length;
  const negativeDriversCount = drivers.filter((d) => (d.walletBalance || 0) < 0).length;

  // Selected driver's transaction ledger history
  const driverTransactions = useMemo(() => {
    if (!selectedDriverForHistory) return [];
    const dId = selectedDriverForHistory.id;

    // Completed trips for this driver
    const driverTrips = trips
      .filter((t) => t.driverId === dId && t.status === 'completed')
      .map((t) => {
        const isCash = t.paymentMethod === 'cash';
        const grossFare = t.fare?.total || 0;
        const commission = t.fare?.drigoCommissionAmount || grossFare * 0.18;
        const netEarning = grossFare - commission;

        return {
          id: `trip-${t.id}`,
          type: isCash ? 'cash_trip_deduction' : 'digital_trip_credit',
          title: `Trip ${t.tripCode || t.id.slice(0, 8)} (${t.paymentMethod.toUpperCase()})`,
          subtitle: `${t.fromLocation?.address || 'Pickup'} → ${t.toLocation?.address || 'Drop-off'}`,
          date: t.completedAt || t.requestedAt,
          grossFare,
          commission,
          netImpact: isCash ? -commission : netEarning,
          status: 'completed',
        };
      });

    // Payout requests for this driver
    const driverPayouts = payoutRequests
      .filter((p) => p.driverId === dId)
      .map((p) => ({
        id: `payout-${p.id}`,
        type: 'payout_withdrawal',
        title: `Withdrawal via ${p.paymentMethod.toUpperCase()}`,
        subtitle: `To: ${p.accountTitle} (${p.accountNumber})`,
        date: p.processedAt || p.requestedAt,
        grossFare: 0,
        commission: 0,
        netImpact: -p.amount,
        status: p.status,
      }));

    // Combine and sort by date descending
    const combined = [...driverTrips, ...driverPayouts];
    return combined.slice(0, 30);
  }, [selectedDriverForHistory, trips, payoutRequests]);

  return (
    <div className="space-y-4">
      {/* Top Controls & Metrics Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Search input */}
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search driver name, phone, plate #..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
          />
        </div>

        {/* Filter chips */}
        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          <select
            value={balanceFilter}
            onChange={(e) => setBalanceFilter(e.target.value as any)}
            className="p-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:outline-none cursor-pointer"
          >
            <option value="all">All Balances</option>
            <option value="positive">In Credit (&gt; Rs. 0)</option>
            <option value="high">High Balance (&gt; Rs. 5,000)</option>
            <option value="negative">Negative / Debt (&lt; Rs. 0)</option>
          </select>

          <select
            value={vehicleFilter}
            onChange={(e) => setVehicleFilter(e.target.value as any)}
            className="p-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:outline-none cursor-pointer"
          >
            <option value="all">All Vehicles</option>
            <option value="boda_bike">Moto / Bike</option>
            <option value="tuktuk_auto">Auto Rickshaw</option>
            <option value="sedan">Sedan</option>
            <option value="comfort">Executive / Comfort</option>
            <option value="xl_van">XL Van</option>
          </select>
        </div>
      </div>

      {/* Driver Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredDrivers.length === 0 ? (
          <div className="col-span-full bg-white p-8 rounded-2xl border border-slate-200 text-center text-slate-400">
            No driver wallets found matching the selected filters.
          </div>
        ) : (
          filteredDrivers.map((d, idx) => {
            const balance = d.walletBalance || 0;
            const isNegative = balance < 0;
            const isHigh = balance >= 5000;

            return (
              <div
                key={d.id ? `driver-wallet-${d.id}-${idx}` : `driver-wallet-${idx}`}
                className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-all flex flex-col justify-between space-y-4 relative overflow-hidden"
              >
                {/* Top Driver Info */}
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <img
                      src={d.avatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80'}
                      alt={d.fullName}
                      className="w-12 h-12 rounded-full object-cover border border-slate-200 shadow-2xs"
                    />
                    <div>
                      <h4 className="font-extrabold text-slate-900 text-sm">{d.fullName}</h4>
                      <p className="text-xs text-slate-500 font-medium">{d.phone}</p>
                      <div className="flex items-center gap-1.5 mt-1">
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                          {d.vehicle?.make} {d.vehicle?.model}
                        </span>
                        <span className="text-[10px] font-mono font-bold text-slate-600 bg-slate-100 px-1.5 py-0.5 rounded">
                          {d.vehicle?.licensePlate || 'N/A'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Balance & Today Earnings Container */}
                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                      Wallet Balance
                    </span>
                    <div
                      className={`text-lg font-extrabold font-mono mt-0.5 ${
                        isNegative
                          ? 'text-rose-600'
                          : isHigh
                          ? 'text-emerald-700'
                          : 'text-slate-900'
                      }`}
                    >
                      Rs. {balance.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
                    </div>
                  </div>

                  <div className="text-right">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                      Today's Earnings
                    </span>
                    <div className="text-sm font-extrabold font-mono text-emerald-600 mt-0.5">
                      +Rs. {(d.todayEarnings || 0).toLocaleString('en-US')}
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="grid grid-cols-2 gap-2 pt-1 border-t border-slate-100 text-xs font-bold">
                  <button
                    onClick={() => setSelectedDriverForHistory(d)}
                    className="py-2.5 px-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <History className="w-3.5 h-3.5 text-slate-500" />
                    <span>Ledger Log</span>
                  </button>

                  <button
                    onClick={() => onOpenAdjustModal(d)}
                    className="py-2.5 px-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <DollarSign className="w-3.5 h-3.5" />
                    <span>Adjust PKR</span>
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Driver Ledger History Modal */}
      {selectedDriverForHistory && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
          <div className="bg-white rounded-3xl border border-slate-200 p-6 max-w-xl w-full shadow-2xl space-y-4 my-8 relative">
            <button
              onClick={() => setSelectedDriverForHistory(null)}
              className="absolute top-5 right-5 p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Header */}
            <div className="flex items-center space-x-3.5 pr-8">
              <img
                src={selectedDriverForHistory.avatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80'}
                alt={selectedDriverForHistory.fullName}
                className="w-12 h-12 rounded-full object-cover border border-slate-200 shadow-2xs"
              />
              <div>
                <h3 className="text-base font-extrabold text-slate-900">
                  {selectedDriverForHistory.fullName}
                </h3>
                <p className="text-xs text-slate-500">
                  Phone: {selectedDriverForHistory.phone} | Wallet:{' '}
                  <span className="font-mono font-bold text-slate-800">
                    Rs. {selectedDriverForHistory.walletBalance.toLocaleString()}
                  </span>
                </p>
              </div>
            </div>

            <div className="border-t border-slate-100 pt-3">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-extrabold text-slate-900">
                  Recent Wallet Ledger & Trips Log
                </span>
                <span className="text-[11px] text-slate-400">Showing last 30 activities</span>
              </div>

              <div className="max-h-80 overflow-y-auto divide-y divide-slate-100 border border-slate-100 rounded-2xl bg-slate-50/50">
                {driverTransactions.length === 0 ? (
                  <div className="p-6 text-center text-xs text-slate-400">
                    No recent trip transactions or payout records logged for this driver.
                  </div>
                ) : (
                  driverTransactions.map((tx) => (
                    <div key={tx.id} className="p-3 flex items-center justify-between text-xs hover:bg-white transition-colors">
                      <div className="space-y-0.5">
                        <div className="font-bold text-slate-800">{tx.title}</div>
                        <div className="text-[11px] text-slate-500 truncate max-w-xs">{tx.subtitle}</div>
                        <div className="text-[10px] text-slate-400">{tx.date}</div>
                      </div>

                      <div className="text-right">
                        <div
                          className={`font-mono font-extrabold text-sm ${
                            tx.netImpact > 0 ? 'text-emerald-600' : 'text-rose-600'
                          }`}
                        >
                          {tx.netImpact > 0 ? `+Rs. ${tx.netImpact.toLocaleString()}` : `-Rs. ${Math.abs(tx.netImpact).toLocaleString()}`}
                        </div>
                        {tx.commission > 0 && (
                          <div className="text-[10px] text-slate-400 font-mono">
                            Drigo cut: Rs. {Math.round(tx.commission)}
                          </div>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <button
                onClick={() => {
                  const d = selectedDriverForHistory;
                  setSelectedDriverForHistory(null);
                  onOpenAdjustModal(d);
                }}
                className="px-4 py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-bold rounded-xl transition-colors cursor-pointer"
              >
                Adjust Balance Directly
              </button>

              <button
                onClick={() => setSelectedDriverForHistory(null)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors cursor-pointer"
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
