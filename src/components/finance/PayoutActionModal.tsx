import React, { useState, useEffect } from 'react';
import {
  CheckCircle2,
  XCircle,
  CreditCard,
  Building2,
  Smartphone,
  Copy,
  Check,
  AlertTriangle,
  Info,
  ShieldCheck,
  X,
  Zap,
  DollarSign
} from 'lucide-react';
import { PayoutRequest } from '../../types';

interface PayoutActionModalProps {
  payout: PayoutRequest;
  actionType: 'approve' | 'reject';
  onClose: () => void;
  onConfirm: (
    payoutId: string,
    action: 'approve' | 'reject',
    transactionRef?: string,
    rejectionReason?: string
  ) => void;
}

const PRESET_REJECTION_REASONS = [
  'Account Title does not match Driver CNIC / Name on file',
  'Invalid or inactive JazzCash/EasyPaisa mobile account number',
  'Invalid 24-character IBAN or unsupported bank branch code',
  'Insufficient verified wallet balance at settlement time',
  'Duplicate withdrawal request submitted within 24 hours',
  'Driver profile temporarily under safety / fraud review',
  'Custom Reason'
];

export const PayoutActionModal: React.FC<PayoutActionModalProps> = ({
  payout,
  actionType,
  onClose,
  onConfirm,
}) => {
  const [txRef, setTxRef] = useState('');
  const [selectedReason, setSelectedReason] = useState(PRESET_REJECTION_REASONS[0]);
  const [customReason, setCustomReason] = useState('');
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (actionType === 'approve') {
      const prefix =
        payout.paymentMethod === 'jazzcash'
          ? 'JC'
          : payout.paymentMethod === 'easypaisa'
          ? 'EP'
          : payout.paymentMethod === 'raast'
          ? 'RAAST'
          : payout.paymentMethod === 'bank_transfer'
          ? 'IBAN'
          : 'CASH';
      const randomDigits = Math.floor(100000 + Math.random() * 900000);
      setTxRef(`${prefix}-${randomDigits}`);
    }
  }, [actionType, payout.paymentMethod]);

  const handleCopy = (text: string, fieldKey: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(fieldKey);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    if (actionType === 'approve') {
      const finalRef = txRef.trim() || `TX-${Date.now().toString().slice(-6)}`;
      onConfirm(payout.id, 'approve', finalRef);
    } else {
      const finalReason =
        selectedReason === 'Custom Reason'
          ? customReason.trim() || 'Payout request rejected after administrative audit.'
          : selectedReason;
      onConfirm(payout.id, 'reject', undefined, finalReason);
    }
  };

  const titleMatch = (payout.accountTitle || '').toLowerCase().trim() === (payout.driverName || '').toLowerCase().trim();

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="bg-white rounded-3xl border border-slate-200 p-6 max-w-lg w-full shadow-2xl space-y-5 my-8 relative">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
          title="Close dialog"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header */}
        <div className="flex items-start space-x-3.5 pr-8">
          <div
            className={`w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 border shadow-xs ${
              actionType === 'approve'
                ? 'bg-emerald-50 text-emerald-600 border-emerald-200'
                : 'bg-rose-50 text-rose-600 border-rose-200'
            }`}
          >
            {actionType === 'approve' ? (
              <CheckCircle2 className="w-6 h-6" />
            ) : (
              <XCircle className="w-6 h-6" />
            )}
          </div>
          <div>
            <h3 className="text-lg font-extrabold text-slate-900">
              {actionType === 'approve' ? 'Settle & Mark Payout as Paid' : 'Reject Driver Withdrawal'}
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Payout ID: <span className="font-mono font-bold text-slate-700">{payout.id}</span>
            </p>
          </div>
        </div>

        {/* Driver & Payout Summary Card */}
        <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200/80 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <img
                src={payout.driverAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=80'}
                alt={payout.driverName}
                className="w-10 h-10 rounded-full object-cover border border-slate-200 shadow-2xs"
              />
              <div>
                <div className="font-extrabold text-slate-900 text-sm">{payout.driverName}</div>
                <div className="text-xs text-slate-500">{payout.driverPhone}</div>
              </div>
            </div>

            <div className="text-right">
              <span className="text-[10px] font-bold text-slate-400 uppercase">Amount</span>
              <div className="text-xl font-extrabold font-mono text-emerald-700">
                Rs. {payout.amount.toLocaleString()}
              </div>
            </div>
          </div>

          {/* Account Details Box */}
          <div className="pt-3 border-t border-slate-200/80 grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
            <div className="bg-white p-2.5 rounded-xl border border-slate-200/70">
              <span className="text-[10px] font-bold text-slate-400 uppercase">Payment Channel</span>
              <div className="font-bold text-slate-800 flex items-center gap-1.5 mt-0.5">
                {payout.paymentMethod === 'jazzcash' && <span className="text-amber-600 font-extrabold">🟠 JazzCash Wallet</span>}
                {payout.paymentMethod === 'easypaisa' && <span className="text-emerald-600 font-extrabold">🟢 EasyPaisa Wallet</span>}
                {payout.paymentMethod === 'raast' && <span className="text-purple-600 font-extrabold">⚡ Raast Instant IBFT</span>}
                {payout.paymentMethod === 'bank_transfer' && <span className="text-blue-600 font-extrabold">🏦 Bank Transfer {payout.bankName ? `(${payout.bankName})` : ''}</span>}
                {payout.paymentMethod === 'cash' && <span className="text-slate-700 font-extrabold">💵 Cash Settlement</span>}
              </div>
            </div>

            <div className="bg-white p-2.5 rounded-xl border border-slate-200/70">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-slate-400 uppercase">Account Title</span>
                {!titleMatch && (
                  <span className="text-[9px] font-extrabold bg-amber-50 text-amber-800 border border-amber-200 px-1.5 py-0.2 rounded">
                    Title Mismatch
                  </span>
                )}
              </div>
              <div className="font-bold text-slate-900 mt-0.5 flex items-center justify-between">
                <span className="truncate">{payout.accountTitle}</span>
                <button
                  type="button"
                  onClick={() => handleCopy(payout.accountTitle, 'title')}
                  className="text-slate-400 hover:text-slate-700 p-0.5 rounded cursor-pointer"
                  title="Copy account title"
                >
                  {copiedField === 'title' ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            <div className="bg-white p-2.5 rounded-xl border border-slate-200/70 sm:col-span-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-slate-400 uppercase">
                  Account Number / IBAN / Raast ID
                </span>
                <button
                  type="button"
                  onClick={() => handleCopy(payout.accountNumber, 'accNo')}
                  className="text-slate-400 hover:text-slate-700 flex items-center gap-1 text-[11px] font-bold cursor-pointer"
                >
                  {copiedField === 'accNo' ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-600" />
                      <span className="text-emerald-600 text-[10px]">Copied</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" />
                      <span className="text-[10px]">Copy Number</span>
                    </>
                  )}
                </button>
              </div>
              <div className="font-mono font-bold text-sm text-slate-900 mt-1 select-all break-all">
                {payout.accountNumber}
              </div>
            </div>
          </div>
        </div>

        {/* Form Controls */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {actionType === 'approve' ? (
            <div className="space-y-3">
              <div className="bg-emerald-50/70 border border-emerald-200/80 rounded-2xl p-3.5 text-xs text-emerald-900 flex items-start space-x-2.5">
                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold">Banking & Gateway Verification</div>
                  <p className="text-[11px] text-emerald-800/90 mt-0.5">
                    Ensure the manual funds transfer of <span className="font-bold">Rs. {payout.amount.toLocaleString()}</span> has been dispatched via your {payout.paymentMethod.toUpperCase()} corporate portal before confirming.
                  </p>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Bank / Gateway Transaction Reference <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type="text"
                    value={txRef}
                    onChange={(e) => setTxRef(e.target.value)}
                    placeholder="e.g. JC-889124 or IBAN-TX-0092"
                    required
                    className="w-full p-2.5 pr-20 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      const prefix = payout.paymentMethod.slice(0, 3).toUpperCase();
                      setTxRef(`${prefix}-${Math.floor(100000 + Math.random() * 900000)}`);
                    }}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-[10px] font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 px-2 py-1 rounded-lg cursor-pointer transition-colors"
                  >
                    Regenerate
                  </button>
                </div>
                <span className="text-[10px] text-slate-400 mt-1 block">
                  Reference code will be logged for financial audit and displayed to driver.
                </span>
              </div>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="bg-rose-50 border border-rose-200 rounded-2xl p-3.5 text-xs text-rose-900 flex items-start space-x-2.5">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold">Rejection Notification</div>
                  <p className="text-[11px] text-rose-800/90 mt-0.5">
                    Rejecting this withdrawal will restore the driver's in-app wallet balance and deliver the reason code to the driver's app.
                  </p>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Reason for Rejection <span className="text-rose-500">*</span>
                </label>
                <select
                  value={selectedReason}
                  onChange={(e) => setSelectedReason(e.target.value)}
                  className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 focus:outline-none"
                >
                  {PRESET_REJECTION_REASONS.map((reason) => (
                    <option key={reason} value={reason}>
                      {reason}
                    </option>
                  ))}
                </select>
              </div>

              {selectedReason === 'Custom Reason' && (
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Detailed Reason for Driver
                  </label>
                  <textarea
                    value={customReason}
                    onChange={(e) => setCustomReason(e.target.value)}
                    rows={3}
                    placeholder="Provide specific instructions (e.g. Please update your JazzCash account title to match your CNIC name in your profile settings)."
                    className="w-full p-2.5 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 focus:outline-none"
                    required
                  />
                </div>
              )}
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-900 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className={`px-5 py-2.5 text-xs font-bold text-white rounded-xl shadow-xs transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50 ${
                actionType === 'approve'
                  ? 'bg-emerald-600 hover:bg-emerald-700'
                  : 'bg-rose-600 hover:bg-rose-700'
              }`}
            >
              {actionType === 'approve' ? (
                <>
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Confirm & Mark Settled</span>
                </>
              ) : (
                <>
                  <XCircle className="w-4 h-4" />
                  <span>Confirm Rejection</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
