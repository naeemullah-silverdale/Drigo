import React, { useState } from 'react';
import { Driver, Rider, WalletTransaction } from '../../types';
import {
  DollarSign,
  Wallet,
  ArrowRight,
  ShieldCheck,
  X,
  CreditCard,
  PlusCircle,
  MinusCircle,
  FileText,
  User
} from 'lucide-react';

interface AdjustWalletModalProps {
  driver?: Driver | null;
  rider?: Rider | null;
  user?: Driver | Rider | null;
  userType?: 'driver' | 'rider';
  onClose: () => void;
  onConfirmAdjustment: (
    userId: string,
    amount: number,
    transactionMeta?: Partial<WalletTransaction>
  ) => void;
}

export const AdjustWalletModal: React.FC<AdjustWalletModalProps> = ({
  driver,
  rider,
  user,
  userType: propUserType,
  onClose,
  onConfirmAdjustment,
}) => {
  const targetUser = user || driver || rider;
  const isDriver = propUserType ? propUserType === 'driver' : !!driver;
  const userType = isDriver ? 'driver' : 'rider';

  const [walletAmount, setWalletAmount] = useState<string>('');
  const [transactionType, setTransactionType] = useState<
    'topup' | 'bonus' | 'refund' | 'deduction' | 'admin_adjustment'
  >(isDriver ? 'bonus' : 'topup');
  const [paymentMethod, setPaymentMethod] = useState<
    'jazzcash' | 'easypaisa' | 'bank_transfer' | 'cash' | 'raast' | 'admin_manual'
  >('admin_manual');
  const [reason, setReason] = useState<string>(
    isDriver ? 'Weekly Performance Incentive Bonus' : 'Promotional Wallet Top-up Voucher'
  );
  const [referenceId, setReferenceId] = useState<string>('');

  if (!targetUser) return null;

  const currentBalance = targetUser.walletBalance ?? 0;
  const rawNum = parseFloat(walletAmount) || 0;
  // If transactionType is deduction, make it negative if entered positive
  const numAdjustment =
    transactionType === 'deduction' ? -Math.abs(rawNum) : rawNum;
  const newProjectedBalance = currentBalance + numAdjustment;

  const handleQuickAdd = (amt: number) => {
    setWalletAmount(Math.abs(amt).toString());
    if (amt < 0) {
      setTransactionType('deduction');
    } else {
      if (transactionType === 'deduction') setTransactionType('topup');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isNaN(numAdjustment) || numAdjustment === 0) return;

    const txMeta: Partial<WalletTransaction> = {
      userType,
      userId: targetUser.id,
      userName: targetUser.fullName,
      userPhone: targetUser.phone,
      type: transactionType,
      amount: numAdjustment,
      previousBalance: currentBalance,
      newBalance: newProjectedBalance,
      currency: 'PKR',
      paymentMethod,
      reason,
      referenceId: referenceId.trim() || undefined,
      administeredBy: 'Admin Ops Manager',
      timestamp: new Date().toISOString()
    };

    onConfirmAdjustment(targetUser.id, numAdjustment, txMeta);
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-4 border border-slate-200 max-h-[92vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2.5">
            <div className={`p-2 rounded-xl ${isDriver ? 'bg-emerald-50 text-emerald-600' : 'bg-blue-50 text-blue-600'}`}>
              <Wallet className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-sm text-slate-900 flex items-center gap-2">
                <span>Adjust Wallet Balance</span>
                <span className={`text-[10px] uppercase font-extrabold px-2 py-0.5 rounded-full ${
                  isDriver ? 'bg-emerald-100 text-emerald-800' : 'bg-blue-100 text-blue-800'
                }`}>
                  {isDriver ? 'Driver Captain' : 'Passenger Rider'}
                </span>
              </h3>
              <p className="text-[11px] text-slate-500 font-mono">
                {targetUser.fullName} • ID: {targetUser.id}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-700 text-lg font-bold p-1 rounded-lg"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Current & Projected Balance Cards */}
        <div className="grid grid-cols-2 gap-2.5 bg-slate-50 p-3.5 rounded-2xl border border-slate-200/80 text-xs">
          <div>
            <span className="text-slate-500 block text-[10px] font-semibold">Current Balance</span>
            <span className="text-base font-black text-slate-900 font-mono">
              Rs. {(currentBalance ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })}
            </span>
          </div>
          <div className="border-l border-slate-200 pl-3">
            <span className="text-slate-500 block text-[10px] font-semibold">Projected Balance</span>
            <span className={`text-base font-black font-mono ${
              newProjectedBalance >= 0 ? 'text-emerald-700' : 'text-rose-600'
            }`}>
              Rs. {(newProjectedBalance ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2 })}
            </span>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3.5 text-xs">
          {/* Transaction Type */}
          <div className="space-y-1.5">
            <label className="block font-bold text-slate-700">Transaction Type</label>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5">
              <button
                type="button"
                onClick={() => {
                  setTransactionType('topup');
                  setReason(isDriver ? 'Manual Fuel / Cash Top-up' : 'Prepaid Wallet Credit Top-up');
                }}
                className={`p-2 rounded-xl border text-left font-bold transition-all ${
                  transactionType === 'topup'
                    ? 'bg-blue-50 border-blue-500 text-blue-700 shadow-2xs'
                    : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center space-x-1">
                  <PlusCircle className="w-3.5 h-3.5 text-blue-600" />
                  <span>Top-Up</span>
                </div>
                <div className="text-[10px] text-slate-400 font-normal">Add credit balance</div>
              </button>

              <button
                type="button"
                onClick={() => {
                  setTransactionType('bonus');
                  setReason(isDriver ? 'Weekly Performance Incentive Bonus' : 'Promotional Promo Voucher');
                }}
                className={`p-2 rounded-xl border text-left font-bold transition-all ${
                  transactionType === 'bonus'
                    ? 'bg-emerald-50 border-emerald-500 text-emerald-700 shadow-2xs'
                    : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center space-x-1">
                  <DollarSign className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Bonus / Promo</span>
                </div>
                <div className="text-[10px] text-slate-400 font-normal">Incentive reward</div>
              </button>

              <button
                type="button"
                onClick={() => {
                  setTransactionType('refund');
                  setReason('Trip Dispute / Cancellation Compensation Refund');
                }}
                className={`p-2 rounded-xl border text-left font-bold transition-all ${
                  transactionType === 'refund'
                    ? 'bg-indigo-50 border-indigo-500 text-indigo-700 shadow-2xs'
                    : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center space-x-1">
                  <CreditCard className="w-3.5 h-3.5 text-indigo-600" />
                  <span>Refund</span>
                </div>
                <div className="text-[10px] text-slate-400 font-normal">Dispute reimbursement</div>
              </button>

              <button
                type="button"
                onClick={() => {
                  setTransactionType('deduction');
                  setReason(isDriver ? 'Cash Commission Reconciliation' : 'Dispute Chargeback Deduction');
                }}
                className={`p-2 rounded-xl border text-left font-bold transition-all ${
                  transactionType === 'deduction'
                    ? 'bg-rose-50 border-rose-500 text-rose-700 shadow-2xs'
                    : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center space-x-1">
                  <MinusCircle className="w-3.5 h-3.5 text-rose-600" />
                  <span>Deduction</span>
                </div>
                <div className="text-[10px] text-slate-400 font-normal">Penalty / Fee Debit</div>
              </button>

              <button
                type="button"
                onClick={() => {
                  setTransactionType('admin_adjustment');
                  setReason('Administrative Balance Correction');
                }}
                className={`p-2 rounded-xl border text-left font-bold transition-all ${
                  transactionType === 'admin_adjustment'
                    ? 'bg-amber-50 border-amber-500 text-amber-700 shadow-2xs'
                    : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                }`}
              >
                <div className="flex items-center space-x-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-amber-600" />
                  <span>Admin Adjust</span>
                </div>
                <div className="text-[10px] text-slate-400 font-normal">Direct balance override</div>
              </button>
            </div>
          </div>

          {/* Amount Input */}
          <div className="space-y-1.5">
            <label className="block font-bold text-slate-700">
              Adjustment Amount (PKR)
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 font-bold text-slate-400 text-xs">Rs.</span>
              <input
                type="number"
                step="any"
                value={walletAmount}
                onChange={(e) => setWalletAmount(e.target.value)}
                placeholder="e.g. 1000"
                className="w-full pl-10 pr-3 py-2.5 bg-slate-50 border border-slate-300 rounded-xl font-mono text-sm font-bold focus:ring-2 focus:ring-blue-500 focus:outline-none focus:bg-white"
                autoFocus
              />
            </div>
          </div>

          {/* Quick Preset Buttons */}
          <div className="space-y-1">
            <span className="text-[11px] text-slate-500 font-semibold">Quick Presets:</span>
            <div className="flex flex-wrap gap-1.5">
              {[500, 1000, 2500, 5000].map((amt) => (
                <button
                  type="button"
                  key={amt}
                  onClick={() => handleQuickAdd(amt)}
                  className="px-2.5 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 font-mono text-[11px] font-bold rounded-lg border border-emerald-200 transition-colors cursor-pointer"
                >
                  +Rs. {amt.toLocaleString()}
                </button>
              ))}
              {[-500, -1000, -2000].map((amt) => (
                <button
                  type="button"
                  key={amt}
                  onClick={() => handleQuickAdd(amt)}
                  className="px-2.5 py-1 bg-rose-50 hover:bg-rose-100 text-rose-800 font-mono text-[11px] font-bold rounded-lg border border-rose-200 transition-colors cursor-pointer"
                >
                  -Rs. {Math.abs(amt).toLocaleString()}
                </button>
              ))}
            </div>
          </div>

          {/* Payment Method / Channel */}
          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700">Payment Channel</label>
              <select
                value={paymentMethod}
                onChange={(e) => setPaymentMethod(e.target.value as any)}
                className="w-full p-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="admin_manual">Admin Manual Override</option>
                <option value="jazzcash">JazzCash Mobile Account</option>
                <option value="easypaisa">EasyPaisa Mobile Account</option>
                <option value="bank_transfer">Pakistani Bank Transfer</option>
                <option value="raast">Raast Instant Payment</option>
                <option value="cash">Cash Office Deposit</option>
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700">Reference / Voucher #</label>
              <input
                type="text"
                value={referenceId}
                onChange={(e) => setReferenceId(e.target.value)}
                placeholder="e.g. TXN-8921 / Voucher"
                className="w-full p-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-mono focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Reason / Memo */}
          <div className="space-y-1.5">
            <label className="block font-bold text-slate-700">Audit / Settlement Memo</label>
            <input
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Audit explanation for this balance adjustment"
              className="w-full p-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>

          {/* Buttons */}
          <div className="flex items-center justify-end space-x-2 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={numAdjustment === 0}
              className={`px-5 py-2 text-white text-xs font-bold rounded-xl shadow-2xs transition-all ${
                numAdjustment === 0
                  ? 'bg-slate-300 cursor-not-allowed'
                  : numAdjustment < 0
                  ? 'bg-rose-600 hover:bg-rose-700 cursor-pointer'
                  : 'bg-emerald-600 hover:bg-emerald-700 cursor-pointer'
              }`}
            >
              Apply {numAdjustment < 0 ? '-' : '+'}Rs. {Math.abs(numAdjustment).toLocaleString()} Adjustment
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
