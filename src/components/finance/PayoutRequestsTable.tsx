import React, { useState, useMemo } from 'react';
import {
  CreditCard,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  Clock,
  ArrowDownToLine,
  FileText,
  Copy,
  Check,
  AlertTriangle,
  User,
  Building2,
  Smartphone,
  ExternalLink,
  Layers,
  Sparkles,
  Zap
} from 'lucide-react';
import { PayoutRequest, Driver } from '../../types';

interface PayoutRequestsTableProps {
  payoutRequests: PayoutRequest[];
  drivers: Driver[];
  onOpenSettleModal: (payout: PayoutRequest) => void;
  onOpenRejectModal: (payout: PayoutRequest) => void;
  onOpenReceiptModal: (payout: PayoutRequest) => void;
  onBatchSettle: (payoutIds: string[]) => void;
}

export const PayoutRequestsTable: React.FC<PayoutRequestsTableProps> = ({
  payoutRequests,
  drivers,
  onOpenSettleModal,
  onOpenRejectModal,
  onOpenReceiptModal,
  onBatchSettle,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'pending' | 'approved' | 'processed' | 'rejected'>('all');
  const [methodFilter, setMethodFilter] = useState<'all' | 'jazzcash' | 'easypaisa' | 'raast' | 'bank_transfer' | 'cash'>('all');
  const [selectedPayoutIds, setSelectedPayoutIds] = useState<string[]>([]);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Filtered list
  const filteredPayouts = useMemo(() => {
    return payoutRequests.filter((p) => {
      const q = (searchTerm || '').toLowerCase().trim();
      const matchSearch =
        !q ||
        (p.driverName || '').toLowerCase().includes(q) ||
        (p.driverPhone || '').includes(q) ||
        (p.accountTitle || '').toLowerCase().includes(q) ||
        (p.accountNumber || '').includes(q) ||
        (p.id || '').toLowerCase().includes(q) ||
        (p.transactionRef ? p.transactionRef.toLowerCase().includes(q) : false);

      const matchStatus = statusFilter === 'all' || p.status === statusFilter;
      const matchMethod = methodFilter === 'all' || p.paymentMethod === methodFilter;

      return matchSearch && matchStatus && matchMethod;
    });
  }, [payoutRequests, searchTerm, statusFilter, methodFilter]);

  const pendingPayouts = useMemo(() => {
    return filteredPayouts.filter((p) => p.status === 'pending');
  }, [filteredPayouts]);

  const handleSelectAllPending = () => {
    if (selectedPayoutIds.length === pendingPayouts.length) {
      setSelectedPayoutIds([]);
    } else {
      setSelectedPayoutIds(pendingPayouts.map((p) => p.id));
    }
  };

  const handleToggleSelect = (id: string) => {
    setSelectedPayoutIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const exportPayoutsCSV = () => {
    const headers = ['Payout ID', 'Driver Name', 'Phone', 'Amount (Rs.)', 'Method', 'Account Title', 'Account Number', 'Bank Name', 'Status', 'Requested At', 'Transaction Ref'];
    const rows = filteredPayouts.map((p) => [
      p.id,
      `"${p.driverName}"`,
      p.driverPhone,
      p.amount,
      p.paymentMethod,
      `"${p.accountTitle}"`,
      p.accountNumber,
      `"${p.bankName || 'N/A'}"`,
      p.status,
      p.requestedAt,
      p.transactionRef || 'N/A'
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `drigo_payouts_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-4">
      {/* Search & Filter Header Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Search */}
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search driver, phone, IBAN, Ref..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
          />
        </div>

        {/* Filter dropdowns & Export */}
        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as any)}
            className="p-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:outline-none cursor-pointer"
          >
            <option value="all">All Statuses</option>
            <option value="pending">Pending Approval</option>
            <option value="processed">Settled / Paid</option>
            <option value="approved">Approved</option>
            <option value="rejected">Rejected</option>
          </select>

          <select
            value={methodFilter}
            onChange={(e) => setMethodFilter(e.target.value as any)}
            className="p-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:outline-none cursor-pointer"
          >
            <option value="all">All Channels</option>
            <option value="jazzcash">🟠 JazzCash</option>
            <option value="easypaisa">🟢 EasyPaisa</option>
            <option value="raast">⚡ Raast IBFT</option>
            <option value="bank_transfer">🏦 1LINK Bank</option>
            <option value="cash">💵 Cash Counter</option>
          </select>

          <button
            onClick={exportPayoutsCSV}
            className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors flex items-center gap-1.5 cursor-pointer"
            title="Export filtered records to CSV"
          >
            <ArrowDownToLine className="w-3.5 h-3.5 text-slate-600" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Batch Actions Bar (when pending items are selected) */}
      {selectedPayoutIds.length > 0 && (
        <div className="bg-blue-600 text-white p-3.5 rounded-2xl shadow-md flex items-center justify-between animate-fadeIn text-xs">
          <div className="flex items-center space-x-2">
            <span className="font-extrabold bg-blue-800 px-2 py-0.5 rounded-md font-mono">
              {selectedPayoutIds.length} Selected
            </span>
            <span className="font-semibold">Pending Payout Requests</span>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                onBatchSettle(selectedPayoutIds);
                setSelectedPayoutIds([]);
              }}
              className="px-3 py-1.5 bg-emerald-500 hover:bg-emerald-600 text-white font-bold rounded-xl transition-colors shadow-xs flex items-center gap-1 cursor-pointer"
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Batch Settle ({selectedPayoutIds.length})</span>
            </button>
            <button
              onClick={() => setSelectedPayoutIds([])}
              className="px-3 py-1.5 bg-blue-700 hover:bg-blue-800 text-white font-bold rounded-xl transition-colors cursor-pointer"
            >
              Deselect
            </button>
          </div>
        </div>
      )}

      {/* Desktop & Tablet Table View */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden hidden md:block">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
              <tr>
                <th className="p-4 w-10">
                  {pendingPayouts.length > 0 && (
                    <input
                      type="checkbox"
                      checked={selectedPayoutIds.length === pendingPayouts.length && pendingPayouts.length > 0}
                      onChange={handleSelectAllPending}
                      className="rounded text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
                      title="Select all pending"
                    />
                  )}
                </th>
                <th className="p-4">Driver Beneficiary</th>
                <th className="p-4">Amount</th>
                <th className="p-4">Payment Channel</th>
                <th className="p-4">Account Target</th>
                <th className="p-4">Requested Date</th>
                <th className="p-4">Status & Audit</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredPayouts.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-slate-400">
                    No withdrawal payout requests found matching your filter criteria.
                  </td>
                </tr>
              ) : (
                filteredPayouts.map((p, idx) => {
                  const isTitleMatch =
                    (p.accountTitle || '').toLowerCase().trim() === (p.driverName || '').toLowerCase().trim();
                  const isPending = p.status === 'pending';
                  const isSelected = selectedPayoutIds.includes(p.id);

                  return (
                    <tr
                      key={p.id ? `payout-tr-${p.id}-${idx}` : `payout-tr-${idx}`}
                      className={`hover:bg-slate-50/80 transition-colors ${
                        isSelected ? 'bg-blue-50/60' : ''
                      }`}
                    >
                      <td className="p-4">
                        {isPending ? (
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => handleToggleSelect(p.id)}
                            className="rounded text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
                          />
                        ) : (
                          <span className="text-slate-300">•</span>
                        )}
                      </td>

                      <td className="p-4">
                        <div className="flex items-center space-x-3">
                          <img
                            src={p.driverAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80'}
                            alt={p.driverName}
                            className="w-9 h-9 rounded-full object-cover border border-slate-200 shadow-2xs"
                          />
                          <div>
                            <div className="font-extrabold text-slate-900">{p.driverName}</div>
                            <div className="text-[11px] text-slate-500">{p.driverPhone}</div>
                          </div>
                        </div>
                      </td>

                      <td className="p-4">
                        <span className="font-mono font-extrabold text-sm text-slate-900">
                          Rs. {(p.amount ?? 0).toLocaleString()}
                        </span>
                      </td>

                      <td className="p-4">
                        <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-[11px] font-bold uppercase bg-slate-100 text-slate-700 border border-slate-200">
                          {p.paymentMethod === 'jazzcash' && '🟠 JazzCash'}
                          {p.paymentMethod === 'easypaisa' && '🟢 EasyPaisa'}
                          {p.paymentMethod === 'raast' && '⚡ Raast IBFT'}
                          {p.paymentMethod === 'bank_transfer' && '🏦 1LINK Bank'}
                          {p.paymentMethod === 'cash' && '💵 Cash Counter'}
                        </span>
                      </td>

                      <td className="p-4">
                        <div>
                          <div className="flex items-center space-x-1 font-semibold text-slate-800">
                            <span className="truncate max-w-[130px]">{p.accountTitle}</span>
                            {!isTitleMatch && (
                              <span
                                className="text-amber-500 hover:text-amber-600 cursor-help"
                                title="Warning: Account Title differs from driver name on CNIC"
                              >
                                <AlertTriangle className="w-3.5 h-3.5" />
                              </span>
                            )}
                          </div>
                          <div className="font-mono text-[11px] text-slate-500 flex items-center space-x-1">
                            <span>{p.accountNumber}</span>
                            <button
                              onClick={() => handleCopy(p.accountNumber, p.id)}
                              className="text-slate-400 hover:text-slate-700 p-0.5 rounded cursor-pointer"
                              title="Copy account number"
                            >
                              {copiedId === p.id ? (
                                <Check className="w-3 h-3 text-emerald-600" />
                              ) : (
                                <Copy className="w-3 h-3" />
                              )}
                            </button>
                          </div>
                          {p.bankName && (
                            <div className="text-[10px] text-blue-600 font-semibold">{p.bankName}</div>
                          )}
                        </div>
                      </td>

                      <td className="p-4 text-slate-500 text-[11px] whitespace-nowrap">
                        {p.requestedAt}
                      </td>

                      <td className="p-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${
                            p.status === 'pending'
                              ? 'bg-amber-50 text-amber-800 border-amber-200 animate-pulse'
                              : p.status === 'processed' || p.status === 'approved'
                              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                              : 'bg-rose-50 text-rose-800 border-rose-200'
                          }`}
                        >
                          {p.status === 'pending' && 'Pending'}
                          {p.status === 'processed' && 'Settled & Paid'}
                          {p.status === 'approved' && 'Approved'}
                          {p.status === 'rejected' && 'Rejected'}
                        </span>
                        {p.transactionRef && (
                          <div className="text-[10px] font-mono text-slate-400 mt-0.5">
                            Ref: {p.transactionRef}
                          </div>
                        )}
                        {p.rejectionReason && (
                          <div className="text-[10px] text-rose-600 mt-0.5 max-w-[150px] truncate" title={p.rejectionReason}>
                            {p.rejectionReason}
                          </div>
                        )}
                      </td>

                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end space-x-1.5">
                          {isPending ? (
                            <>
                              <button
                                onClick={() => onOpenSettleModal(p)}
                                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors cursor-pointer"
                              >
                                Settle
                              </button>
                              <button
                                onClick={() => onOpenRejectModal(p)}
                                className="px-2.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-xl text-xs font-bold transition-colors cursor-pointer"
                              >
                                Reject
                              </button>
                            </>
                          ) : (
                            <button
                              onClick={() => onOpenReceiptModal(p)}
                              className="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-colors flex items-center gap-1 cursor-pointer"
                              title="View Official Drigo Voucher"
                            >
                              <FileText className="w-3.5 h-3.5 text-slate-500" />
                              <span>Voucher</span>
                            </button>
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

      {/* Mobile Card View (for compact phone screens 320dp - 360dp) */}
      <div className="block md:hidden space-y-3">
        {filteredPayouts.length === 0 ? (
          <div className="bg-white p-6 rounded-2xl border border-slate-200 text-center text-slate-400 text-xs">
            No withdrawal requests matching your filters.
          </div>
        ) : (
          filteredPayouts.map((p, idx) => {
            const isTitleMatch =
              (p.accountTitle || '').toLowerCase().trim() === (p.driverName || '').toLowerCase().trim();
            const isPending = p.status === 'pending';

            return (
              <div
                key={p.id ? `payout-card-${p.id}-${idx}` : `payout-card-${idx}`}
                className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs space-y-3"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <img
                      src={p.driverAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80'}
                      alt={p.driverName}
                      className="w-10 h-10 rounded-full object-cover border border-slate-200"
                    />
                    <div>
                      <div className="font-extrabold text-slate-900 text-xs">{p.driverName}</div>
                      <div className="text-[11px] text-slate-500">{p.driverPhone}</div>
                      <span className="inline-block mt-0.5 text-[9px] font-bold uppercase bg-slate-100 text-slate-700 px-1.5 py-0.2 rounded">
                        {p.paymentMethod}
                      </span>
                    </div>
                  </div>

                  <div className="text-right">
                    <span className="font-mono font-extrabold text-sm text-slate-900 block">
                      Rs. {(p.amount ?? 0).toLocaleString()}
                    </span>
                    <span
                      className={`inline-block text-[9px] font-bold px-2 py-0.5 rounded-full mt-1 border ${
                        p.status === 'pending'
                          ? 'bg-amber-50 text-amber-800 border-amber-200'
                          : p.status === 'processed' || p.status === 'approved'
                          ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                          : 'bg-rose-50 text-rose-800 border-rose-200'
                      }`}
                    >
                      {p.status}
                    </span>
                  </div>
                </div>

                {/* Account details box */}
                <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100 text-xs space-y-1">
                  <div className="flex justify-between items-center">
                    <span className="text-[10px] text-slate-400 font-bold uppercase">Account Title</span>
                    <span className="font-semibold text-slate-800 flex items-center gap-1">
                      {p.accountTitle}
                      {!isTitleMatch && <AlertTriangle className="w-3 h-3 text-amber-500" />}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[10px] text-slate-400 font-bold uppercase">Number</span>
                    <span className="font-mono font-bold text-slate-700 select-all">{p.accountNumber}</span>
                  </div>
                  {p.transactionRef && (
                    <div className="flex justify-between items-center text-[10px] text-slate-400 pt-1 border-t border-slate-200/60 font-mono">
                      <span>Ref:</span>
                      <span>{p.transactionRef}</span>
                    </div>
                  )}
                </div>

                {/* Mobile Actions */}
                <div className="flex items-center justify-end space-x-2 pt-1 border-t border-slate-100">
                  {isPending ? (
                    <>
                      <button
                        onClick={() => onOpenRejectModal(p)}
                        className="px-3 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-xl text-xs font-bold transition-colors cursor-pointer min-h-[44px]"
                      >
                        Reject
                      </button>
                      <button
                        onClick={() => onOpenSettleModal(p)}
                        className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors cursor-pointer min-h-[44px] flex items-center justify-center gap-1"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Settle Rs. {p.amount.toLocaleString()}</span>
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => onOpenReceiptModal(p)}
                      className="w-full py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-colors flex items-center justify-center gap-1.5 cursor-pointer min-h-[44px]"
                    >
                      <FileText className="w-4 h-4 text-slate-500" />
                      <span>View Voucher</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
