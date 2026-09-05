import React, { useState } from 'react';
import { Driver, PayoutRequest, Trip } from '../types';
import {
  DollarSign,
  CreditCard,
  CheckCircle2,
  XCircle,
  Clock,
  Search,
  Filter,
  ArrowDownToLine,
  ArrowUpRight,
  TrendingUp,
  Wallet,
  Building2,
  Smartphone,
  AlertCircle,
  FileText,
  User,
  Check,
  X,
  ExternalLink
} from 'lucide-react';

interface FinancePayoutsProps {
  drivers: Driver[];
  trips: Trip[];
  payoutRequests: PayoutRequest[];
  onUpdatePayoutStatus: (
    payoutId: string,
    status: PayoutRequest['status'],
    transactionRef?: string,
    rejectionReason?: string
  ) => void;
  onAdjustDriverWallet: (driverId: string, amount: number) => void;
}

export const FinancePayouts: React.FC<FinancePayoutsProps> = ({
  drivers,
  trips,
  payoutRequests,
  onUpdatePayoutStatus,
  onAdjustDriverWallet,
}) => {
  const [activeTab, setActiveTab] = useState<'payout_requests' | 'driver_wallets' | 'commission_breakdown'>('payout_requests');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'pending' | 'approved' | 'processed' | 'rejected'>('all');
  const [methodFilter, setMethodFilter] = useState<'all' | 'jazzcash' | 'easypaisa' | 'bank_transfer' | 'cash'>('all');

  // Modal states
  const [selectedPayout, setSelectedPayout] = useState<PayoutRequest | null>(null);
  const [actionType, setActionType] = useState<'approve' | 'reject' | null>(null);
  const [txRefInput, setTxRefInput] = useState('');
  const [rejectReasonInput, setRejectReasonInput] = useState('');

  // Wallet manual adjustment modal
  const [selectedDriverForAdjust, setSelectedDriverForAdjust] = useState<Driver | null>(null);
  const [adjustAmount, setAdjustAmount] = useState('');
  const [adjustNote, setAdjustNote] = useState('');

  // Calculations
  const completedTrips = trips.filter((t) => t.status === 'completed');
  const totalGrossFares = completedTrips.reduce((acc, t) => acc + (t.fare?.total || 0), 0);
  const totalCommission = completedTrips.reduce(
    (acc, t) => acc + (t.fare?.drigoCommissionAmount || (t.fare?.total || 0) * 0.18),
    0
  );
  const totalDriverWalletBalance = drivers.reduce((acc, d) => acc + (d.walletBalance || 0), 0);
  const pendingRequests = payoutRequests.filter((p) => p.status === 'pending');
  const pendingAmount = pendingRequests.reduce((acc, p) => acc + p.amount, 0);

  // Filtered Payouts
  const filteredPayouts = payoutRequests.filter((p) => {
    const matchesSearch =
      !searchTerm ||
      p.driverName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.driverPhone.includes(searchTerm) ||
      p.accountTitle.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.accountNumber.includes(searchTerm) ||
      p.id.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesStatus = statusFilter === 'all' || p.status === statusFilter;
    const matchesMethod = methodFilter === 'all' || p.paymentMethod === methodFilter;

    return matchesSearch && matchesStatus && matchesMethod;
  });

  // Filtered Drivers
  const filteredDrivers = drivers.filter((d) => {
    return (
      !searchTerm ||
      d.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      d.phone.includes(searchTerm) ||
      d.id.toLowerCase().includes(searchTerm.toLowerCase())
    );
  });

  const handleConfirmAction = () => {
    if (!selectedPayout || !actionType) return;

    if (actionType === 'approve') {
      onUpdatePayoutStatus(selectedPayout.id, 'processed', txRefInput || `TX-${Date.now().toString().slice(-6)}`);
    } else {
      onUpdatePayoutStatus(
        selectedPayout.id,
        'rejected',
        undefined,
        rejectReasonInput || 'Account verification failed or insufficient verified balance.'
      );
    }

    setSelectedPayout(null);
    setActionType(null);
    setTxRefInput('');
    setRejectReasonInput('');
  };

  const handleWalletAdjustSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDriverForAdjust) return;
    const val = parseFloat(adjustAmount);
    if (!isNaN(val) && val !== 0) {
      onAdjustDriverWallet(selectedDriverForAdjust.id, val);
      setSelectedDriverForAdjust(null);
      setAdjustAmount('');
      setAdjustNote('');
    }
  };

  const exportPayoutsCSV = () => {
    const headers = ['Payout ID', 'Driver Name', 'Phone', 'Amount (Rs.)', 'Method', 'Account Title', 'Account Number', 'Status', 'Requested At', 'Transaction Ref'];
    const rows = filteredPayouts.map((p) => [
      p.id,
      `"${p.driverName}"`,
      p.driverPhone,
      p.amount,
      p.paymentMethod,
      `"${p.accountTitle}"`,
      p.accountNumber,
      p.status,
      p.requestedAt,
      p.transactionRef || 'N/A'
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `drigo_payouts_export_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50">
      {/* Page Title & Export */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <h2 className="text-xl font-extrabold text-slate-900 flex items-center space-x-2">
            <CreditCard className="w-6 h-6 text-blue-600" />
            <span>Finance & Driver Payouts Hub</span>
          </h2>
          <p className="text-xs text-slate-500 mt-1">
            Manage driver withdrawal requests, settle JazzCash & EasyPaisa payouts, and audit Drigo platform commission.
          </p>
        </div>

        <button
          onClick={exportPayoutsCSV}
          className="inline-flex items-center space-x-1.5 px-3 py-2 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold rounded-xl border border-slate-200 shadow-xs transition-colors self-start sm:self-auto"
        >
          <ArrowDownToLine className="w-4 h-4 text-slate-500" />
          <span>Export Payouts CSV</span>
        </button>
      </div>

      {/* Financial Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Total Commission Earned</span>
            <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <h3 className="text-2xl font-extrabold text-slate-900 mt-2 font-mono">
            Rs. {(totalCommission ?? 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
          </h3>
          <p className="text-[11px] text-emerald-600 font-semibold mt-1">18% Standard Take Rate</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Pending Payout Queue</span>
            <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center font-bold">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <h3 className="text-2xl font-extrabold text-amber-600 mt-2 font-mono">
            Rs. {(pendingAmount ?? 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
          </h3>
          <p className="text-[11px] text-slate-500 mt-1">{pendingRequests.length} driver withdrawals awaiting approval</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Driver Wallet Liability</span>
            <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
              <Wallet className="w-4 h-4" />
            </div>
          </div>
          <h3 className="text-2xl font-extrabold text-slate-900 mt-2 font-mono">
            Rs. {(totalDriverWalletBalance ?? 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
          </h3>
          <p className="text-[11px] text-slate-500 mt-1">Stored across {drivers.length} active partner accounts</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Gross Ride Value</span>
            <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <h3 className="text-2xl font-extrabold text-slate-900 mt-2 font-mono">
            Rs. {(totalGrossFares ?? 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
          </h3>
          <p className="text-[11px] text-slate-500 mt-1">{completedTrips.length} completed transactions</p>
        </div>
      </div>

      {/* Tabs Switcher */}
      <div className="flex border-b border-slate-200 space-x-4">
        <button
          onClick={() => setActiveTab('payout_requests')}
          className={`pb-3 text-xs font-bold transition-all border-b-2 flex items-center space-x-2 ${
            activeTab === 'payout_requests'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <CreditCard className="w-4 h-4" />
          <span>Driver Payout Requests</span>
          {pendingRequests.length > 0 && (
            <span className="bg-amber-100 text-amber-700 text-[10px] font-extrabold px-2 py-0.5 rounded-full">
              {pendingRequests.length}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveTab('driver_wallets')}
          className={`pb-3 text-xs font-bold transition-all border-b-2 flex items-center space-x-2 ${
            activeTab === 'driver_wallets'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Wallet className="w-4 h-4" />
          <span>Driver Wallet Balances & Adjustments</span>
        </button>
      </div>

      {/* Payout Requests Tab Content */}
      {activeTab === 'payout_requests' && (
        <div className="space-y-4">
          {/* Filters Bar */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row items-center justify-between gap-3">
            <div className="relative w-full md:w-80">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search driver, phone, account #..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>

            <div className="flex items-center space-x-2 w-full md:w-auto overflow-x-auto">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as any)}
                className="p-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:outline-none"
              >
                <option value="all">All Statuses</option>
                <option value="pending">Pending</option>
                <option value="processed">Processed / Paid</option>
                <option value="rejected">Rejected</option>
              </select>

              <select
                value={methodFilter}
                onChange={(e) => setMethodFilter(e.target.value as any)}
                className="p-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:outline-none"
              >
                <option value="all">All Methods</option>
                <option value="jazzcash">JazzCash</option>
                <option value="easypaisa">EasyPaisa</option>
                <option value="bank_transfer">Bank Transfer</option>
                <option value="cash">Cash Settlement</option>
              </select>
            </div>
          </div>

          {/* Table */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-700">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
                  <tr>
                    <th className="p-4">Driver Details</th>
                    <th className="p-4">Amount</th>
                    <th className="p-4">Payout Method</th>
                    <th className="p-4">Account Target</th>
                    <th className="p-4">Requested Date</th>
                    <th className="p-4">Status</th>
                    <th className="p-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredPayouts.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="p-8 text-center text-slate-400">
                        No payout withdrawal requests found matching your filters.
                      </td>
                    </tr>
                  ) : (
                    filteredPayouts.map((p) => {
                      return (
                        <tr key={p.id} className="hover:bg-slate-50/80 transition-colors">
                          <td className="p-4">
                            <div className="flex items-center space-x-3">
                              <img
                                src={p.driverAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80'}
                                alt={p.driverName}
                                className="w-9 h-9 rounded-full object-cover border border-slate-200"
                              />
                              <div>
                                <div className="font-bold text-slate-900">{p.driverName}</div>
                                <div className="text-[11px] text-slate-500">{p.driverPhone}</div>
                              </div>
                            </div>
                          </td>

                          <td className="p-4">
                            <span className="font-mono font-bold text-sm text-slate-900">
                              Rs. {(p.amount ?? 0).toLocaleString()}
                            </span>
                          </td>

                          <td className="p-4">
                            <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-[11px] font-bold uppercase bg-slate-100 text-slate-700 border border-slate-200">
                              {p.paymentMethod === 'jazzcash' && '🟠 JazzCash'}
                              {p.paymentMethod === 'easypaisa' && '🟢 EasyPaisa'}
                              {p.paymentMethod === 'bank_transfer' && '🏦 Bank Transfer'}
                              {p.paymentMethod === 'cash' && '💵 Cash Settlement'}
                            </span>
                          </td>

                          <td className="p-4">
                            <div>
                              <div className="font-semibold text-slate-800">{p.accountTitle}</div>
                              <div className="font-mono text-[11px] text-slate-500">{p.accountNumber}</div>
                              {p.bankName && <div className="text-[10px] text-blue-600 font-semibold">{p.bankName}</div>}
                            </div>
                          </td>

                          <td className="p-4 text-slate-500 text-[11px]">
                            {p.requestedAt}
                          </td>

                          <td className="p-4">
                            <span
                              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${
                                p.status === 'pending'
                                  ? 'bg-amber-50 text-amber-700 border-amber-200 animate-pulse'
                                  : p.status === 'processed' || p.status === 'approved'
                                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                  : 'bg-rose-50 text-rose-700 border-rose-200'
                              }`}
                            >
                              {p.status === 'pending' && 'Pending Approval'}
                              {p.status === 'processed' && 'Settled & Transferred'}
                              {p.status === 'approved' && 'Approved'}
                              {p.status === 'rejected' && 'Rejected'}
                            </span>
                            {p.transactionRef && (
                              <div className="text-[10px] font-mono text-slate-400 mt-0.5">
                                Ref: {p.transactionRef}
                              </div>
                            )}
                          </td>

                          <td className="p-4 text-right">
                            {p.status === 'pending' ? (
                              <div className="flex items-center justify-end space-x-2">
                                <button
                                  onClick={() => {
                                    setSelectedPayout(p);
                                    setActionType('approve');
                                    setTxRefInput(`TX-${Date.now().toString().slice(-6)}`);
                                  }}
                                  className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold shadow-xs transition-colors"
                                >
                                  Settle / Pay
                                </button>
                                <button
                                  onClick={() => {
                                    setSelectedPayout(p);
                                    setActionType('reject');
                                  }}
                                  className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg text-xs font-bold transition-colors"
                                >
                                  Reject
                                </button>
                              </div>
                            ) : (
                              <span className="text-[11px] text-slate-400 font-semibold">Settled</span>
                            )}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Driver Wallets Tab Content */}
      {activeTab === 'driver_wallets' && (
        <div className="space-y-4">
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
            <div className="relative w-full max-w-sm">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search driver by name or phone..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>
            <p className="text-xs text-slate-500 font-semibold">
              Showing {filteredDrivers.length} verified drivers
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredDrivers.map((d) => (
              <div key={d.id} className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between space-y-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <img
                      src={d.avatar}
                      alt={d.fullName}
                      className="w-12 h-12 rounded-full object-cover border border-slate-200"
                    />
                    <div>
                      <h4 className="font-bold text-slate-900 text-sm">{d.fullName}</h4>
                      <p className="text-xs text-slate-500">{d.phone}</p>
                      <span className="inline-block mt-1 text-[10px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                        {d.vehicle.make} {d.vehicle.model} ({d.vehicle.licensePlate})
                      </span>
                    </div>
                  </div>
                </div>

                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-between">
                  <div>
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Wallet Balance</span>
                    <div className="text-lg font-extrabold font-mono text-slate-900">
                      Rs. {d.walletBalance?.toLocaleString() || '0'}
                    </div>
                  </div>
                  <div>
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Today's Earnings</span>
                    <div className="text-sm font-bold font-mono text-emerald-600">
                      +Rs. {d.todayEarnings?.toLocaleString() || '0'}
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => {
                    setSelectedDriverForAdjust(d);
                    setAdjustAmount('');
                    setAdjustNote('');
                  }}
                  className="w-full py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 rounded-xl text-xs font-bold transition-colors"
                >
                  Adjust Balance (Credit / Debit)
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Modal: Payout Action Confirmation */}
      {selectedPayout && actionType && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-base font-extrabold text-slate-900">
              {actionType === 'approve' ? 'Confirm Payout Settlement' : 'Reject Payout Request'}
            </h3>
            <p className="text-xs text-slate-600">
              {actionType === 'approve'
                ? `You are marking Rs. ${selectedPayout.amount} as transferred to ${selectedPayout.driverName} via ${selectedPayout.paymentMethod.toUpperCase()}.`
                : `Specify a valid operational reason for rejecting ${selectedPayout.driverName}'s payout request.`}
            </p>

            {actionType === 'approve' ? (
              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-700">Bank / Gateway Transaction Reference</label>
                <input
                  type="text"
                  value={txRefInput}
                  onChange={(e) => setTxRefInput(e.target.value)}
                  placeholder="e.g. JC-88912401 or IBAN-TX-009"
                  className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
                />
              </div>
            ) : (
              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-700">Rejection Reason</label>
                <textarea
                  value={rejectReasonInput}
                  onChange={(e) => setRejectReasonInput(e.target.value)}
                  rows={3}
                  placeholder="e.g. Account number title does not match CNIC on driver profile."
                  className="w-full p-2.5 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-rose-500/20 focus:outline-none"
                />
              </div>
            )}

            <div className="flex items-center justify-end space-x-3 pt-2">
              <button
                type="button"
                onClick={() => {
                  setSelectedPayout(null);
                  setActionType(null);
                }}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-900"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmAction}
                className={`px-4 py-2 text-xs font-bold text-white rounded-xl shadow-xs ${
                  actionType === 'approve' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'
                }`}
              >
                {actionType === 'approve' ? 'Confirm & Mark Paid' : 'Confirm Rejection'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Manual Driver Wallet Adjustment */}
      {selectedDriverForAdjust && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-base font-extrabold text-slate-900">
              Adjust Wallet: {selectedDriverForAdjust.fullName}
            </h3>
            <p className="text-xs text-slate-500">
              Current Balance: <span className="font-bold text-slate-900 font-mono">Rs. {selectedDriverForAdjust.walletBalance}</span>
            </p>

            <form onSubmit={handleWalletAdjustSubmit} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700">Adjustment Amount (Rs.)</label>
                <input
                  type="number"
                  step="50"
                  placeholder="Enter positive to credit, negative to debit (e.g. 500 or -200)"
                  value={adjustAmount}
                  onChange={(e) => setAdjustAmount(e.target.value)}
                  className="w-full mt-1 p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
                  required
                />
                <span className="text-[10px] text-slate-400 mt-1 block">
                  Positive value increases wallet; negative value decreases wallet.
                </span>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700">Audit Memo / Note</label>
                <input
                  type="text"
                  placeholder="e.g. Weekly incentive bonus, Fuel reimbursement, Commission deduction"
                  value={adjustNote}
                  onChange={(e) => setAdjustNote(e.target.value)}
                  className="w-full mt-1 p-2.5 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setSelectedDriverForAdjust(null)}
                  className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-900"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl shadow-xs"
                >
                  Save Adjustment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
