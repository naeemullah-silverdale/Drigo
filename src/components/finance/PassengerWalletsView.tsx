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
  User,
  Phone,
  CreditCard,
  PlusCircle,
  Clock,
  X,
  Sparkles,
  Download,
  Gift,
  ShieldCheck
} from 'lucide-react';
import { Rider, Trip, WalletTransaction } from '../../types';

interface PassengerWalletsViewProps {
  riders: Rider[];
  trips: Trip[];
  walletTransactions?: WalletTransaction[];
  onOpenAdjustModal: (rider: Rider) => void;
}

export const PassengerWalletsView: React.FC<PassengerWalletsViewProps> = ({
  riders,
  trips,
  walletTransactions = [],
  onOpenAdjustModal,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [balanceFilter, setBalanceFilter] = useState<'all' | 'positive' | 'zero' | 'high'>('all');
  const [selectedRiderForHistory, setSelectedRiderForHistory] = useState<Rider | null>(null);

  // Filter riders
  const filteredRiders = useMemo(() => {
    return riders.filter((r) => {
      const q = (searchTerm || '').toLowerCase().trim();
      const matchSearch =
        !q ||
        (r.fullName || '').toLowerCase().includes(q) ||
        (r.phone || '').includes(q) ||
        (r.id || '').toLowerCase().includes(q) ||
        (r.email ? r.email.toLowerCase().includes(q) : false);

      let matchBalance = true;
      const balance = r.walletBalance || 0;
      if (balanceFilter === 'positive') matchBalance = balance > 0;
      else if (balanceFilter === 'zero') matchBalance = balance === 0;
      else if (balanceFilter === 'high') matchBalance = balance >= 2000;

      return matchSearch && matchBalance;
    });
  }, [riders, searchTerm, balanceFilter]);

  // Aggregate stats
  const totalPassengerBalance = useMemo(() => {
    return riders.reduce((sum, r) => sum + (r.walletBalance || 0), 0);
  }, [riders]);

  const positiveRidersCount = useMemo(() => {
    return riders.filter((r) => (r.walletBalance || 0) > 0).length;
  }, [riders]);

  // Selected rider's transaction ledger history
  const riderTransactions = useMemo(() => {
    if (!selectedRiderForHistory) return [];
    const rId = selectedRiderForHistory.id;

    // Explicit wallet transactions from Firebase
    const explicitTxs = walletTransactions
      .filter((tx) => tx.userId === rId || tx.userType === 'rider')
      .map((tx) => ({
        id: tx.id,
        type: tx.type,
        title: tx.reason || 'Wallet Top-Up / Adjustment',
        subtitle: `Channel: ${(tx.paymentMethod || 'admin_manual').toUpperCase()} | Ref: ${tx.referenceId || 'N/A'}`,
        date: tx.timestamp,
        netImpact: tx.amount,
        status: 'completed',
      }));

    // Completed trips paid by wallet
    const riderTrips = trips
      .filter((t) => t.passengerId === rId && t.status === 'completed' && t.paymentMethod === 'wallet')
      .map((t) => ({
        id: `trip-${t.id}`,
        type: 'fare_deduction',
        title: `Ride Fare Paid (${t.tripCode || t.id.slice(0, 8)})`,
        subtitle: `${t.fromLocation?.address || 'Pickup'} → ${t.toLocation?.address || 'Drop-off'}`,
        date: t.completedAt || t.requestedAt,
        netImpact: -(t.fare?.total || 0),
        status: 'completed',
      }));

    const combined = [...explicitTxs, ...riderTrips];
    return combined.sort((a, b) => (String(b.date || '') > String(a.date || '') ? 1 : -1)).slice(0, 30);
  }, [selectedRiderForHistory, trips, walletTransactions]);

  const exportPassengerLedgerCSV = () => {
    if (!selectedRiderForHistory) return;
    const headers = ['Transaction ID', 'Date', 'Type', 'Description', 'Channel / Notes', 'Amount (Rs.)'];
    const rows = riderTransactions.map((tx) => [
      tx.id,
      tx.date,
      tx.type,
      `"${tx.title}"`,
      `"${tx.subtitle}"`,
      tx.netImpact
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `passenger_${selectedRiderForHistory.id}_ledger.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-4">
      {/* Top Banner KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
              Passenger Wallet Liabilities
            </span>
            <div className="text-xl font-extrabold font-mono text-slate-900 mt-0.5">
              Rs. {totalPassengerBalance.toLocaleString('en-PK', { minimumFractionDigits: 2 })}
            </div>
            <span className="text-[10px] text-slate-500 font-medium">Prepaid passenger balances</span>
          </div>
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
            <Wallet className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
              Active Funded Wallets
            </span>
            <div className="text-xl font-extrabold font-mono text-emerald-600 mt-0.5">
              {positiveRidersCount} Passengers
            </div>
            <span className="text-[10px] text-slate-500 font-medium">with credit balance &gt; 0</span>
          </div>
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
            <Gift className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
              Average Passenger Balance
            </span>
            <div className="text-xl font-extrabold font-mono text-indigo-600 mt-0.5">
              Rs. {positiveRidersCount > 0 ? Math.round(totalPassengerBalance / positiveRidersCount).toLocaleString() : 0}
            </div>
            <span className="text-[10px] text-slate-500 font-medium">per funded rider account</span>
          </div>
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
            <Sparkles className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Controls Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Search */}
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search passenger name, phone, email, ID..."
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
            <option value="all">All Passenger Balances</option>
            <option value="positive">Funded Wallets (&gt; Rs. 0)</option>
            <option value="high">High Balance (&gt; Rs. 2,000)</option>
            <option value="zero">Zero Balance (Rs. 0)</option>
          </select>
        </div>
      </div>

      {/* Passenger Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredRiders.length === 0 ? (
          <div className="col-span-full bg-white p-8 rounded-2xl border border-slate-200 text-center text-slate-400">
            No passenger wallets found matching your search filters.
          </div>
        ) : (
          filteredRiders.map((r, idx) => {
            const balance = r.walletBalance || 0;
            const isFunded = balance > 0;

            return (
              <div
                key={r.id ? `rider-wallet-${r.id}-${idx}` : `rider-wallet-${idx}`}
                className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-all flex flex-col justify-between space-y-4 relative overflow-hidden"
              >
                {/* Top Passenger Info */}
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <img
                      src={r.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80'}
                      alt={r.fullName}
                      className="w-12 h-12 rounded-full object-cover border border-slate-200 shadow-2xs"
                    />
                    <div>
                      <h4 className="font-extrabold text-slate-900 text-sm">{r.fullName}</h4>
                      <p className="text-xs text-slate-500 font-medium">{r.phone}</p>
                      <div className="flex items-center gap-1.5 mt-1">
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700">
                          {r.city || 'Pakistan'}
                        </span>
                        <span className="text-[10px] font-mono text-slate-500">
                          {r.totalTrips || 0} Trips Completed
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Balance Card */}
                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                      Wallet Credit Balance
                    </span>
                    <div
                      className={`text-lg font-extrabold font-mono mt-0.5 ${
                        isFunded ? 'text-blue-700' : 'text-slate-500'
                      }`}
                    >
                      Rs. {balance.toLocaleString('en-PK', { minimumFractionDigits: 2 })}
                    </div>
                  </div>

                  <div className="text-right">
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                      Rider Rating
                    </span>
                    <div className="text-sm font-extrabold font-mono text-amber-600 mt-0.5">
                      ⭐ {(r.rating ?? 5.0).toFixed(1)}
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="grid grid-cols-2 gap-2 pt-1 border-t border-slate-100 text-xs font-bold">
                  <button
                    onClick={() => setSelectedRiderForHistory(r)}
                    className="py-2.5 px-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <History className="w-3.5 h-3.5 text-slate-500" />
                    <span>Ledger Log</span>
                  </button>

                  <button
                    onClick={() => onOpenAdjustModal(r)}
                    className="py-2.5 px-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <PlusCircle className="w-3.5 h-3.5" />
                    <span>Top-Up / Refund</span>
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Passenger Ledger Modal */}
      {selectedRiderForHistory && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
          <div className="bg-white rounded-3xl border border-slate-200 p-6 max-w-xl w-full shadow-2xl space-y-4 my-8 relative">
            <button
              onClick={() => setSelectedRiderForHistory(null)}
              className="absolute top-5 right-5 p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Header */}
            <div className="flex items-center space-x-3.5 pr-8">
              <img
                src={selectedRiderForHistory.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80'}
                alt={selectedRiderForHistory.fullName}
                className="w-12 h-12 rounded-full object-cover border border-slate-200 shadow-2xs"
              />
              <div>
                <h3 className="text-base font-extrabold text-slate-900">
                  {selectedRiderForHistory.fullName}
                </h3>
                <p className="text-xs text-slate-500">
                  Phone: {selectedRiderForHistory.phone} | Wallet:{' '}
                  <span className="font-mono font-bold text-blue-700">
                    Rs. {(selectedRiderForHistory.walletBalance ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })}
                  </span>
                </p>
              </div>
            </div>

            <div className="border-t border-slate-100 pt-3">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-extrabold text-slate-900">
                  Passenger Wallet Ledger History
                </span>
                <button
                  onClick={exportPassengerLedgerCSV}
                  className="inline-flex items-center gap-1 text-[11px] font-bold text-blue-600 hover:text-blue-800 cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Export CSV</span>
                </button>
              </div>

              <div className="max-h-80 overflow-y-auto divide-y divide-slate-100 border border-slate-100 rounded-2xl bg-slate-50/50">
                {riderTransactions.length === 0 ? (
                  <div className="p-6 text-center text-xs text-slate-400">
                    No recent top-ups, promo vouchers, or wallet trip transactions found for this passenger.
                  </div>
                ) : (
                  riderTransactions.map((tx) => (
                    <div key={tx.id} className="p-3 flex items-center justify-between text-xs hover:bg-white transition-colors">
                      <div className="space-y-0.5">
                        <div className="font-bold text-slate-800">{tx.title}</div>
                        <div className="text-[11px] text-slate-500 truncate max-w-xs">{tx.subtitle}</div>
                        <div className="text-[10px] text-slate-400">{tx.date}</div>
                      </div>

                      <div className="text-right">
                        <div
                          className={`font-mono font-extrabold text-sm ${
                            (tx.netImpact || 0) >= 0 ? 'text-emerald-600' : 'text-rose-600'
                          }`}
                        >
                          {(tx.netImpact || 0) >= 0
                            ? `+Rs. ${(tx.netImpact || 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })}`
                            : `-Rs. ${Math.abs(tx.netImpact || 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })}`}
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <button
                onClick={() => {
                  const r = selectedRiderForHistory;
                  setSelectedRiderForHistory(null);
                  onOpenAdjustModal(r);
                }}
                className="px-4 py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-bold rounded-xl transition-colors cursor-pointer"
              >
                Top-Up / Refund Balance
              </button>

              <button
                onClick={() => setSelectedRiderForHistory(null)}
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
