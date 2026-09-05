import React, { useState } from 'react';
import { Driver } from '../../types';
import { DollarSign, Wallet, ArrowRight, ShieldCheck, X } from 'lucide-react';

interface AdjustWalletModalProps {
  driver: Driver;
  onClose: () => void;
  onConfirmAdjustment: (driverId: string, amount: number) => void;
}

export const AdjustWalletModal: React.FC<AdjustWalletModalProps> = ({
  driver,
  onClose,
  onConfirmAdjustment,
}) => {
  const [walletAmount, setWalletAmount] = useState<string>('');
  const [reason, setReason] = useState<string>('Operational Bonus / Adjustment');

  const currentBalance = driver.walletBalance ?? 0;
  const numAdjustment = parseFloat(walletAmount) || 0;
  const newProjectedBalance = currentBalance + numAdjustment;

  const handleQuickAdd = (amt: number) => {
    setWalletAmount(amt.toString());
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isNaN(numAdjustment) || numAdjustment === 0) return;
    onConfirmAdjustment(driver.id, numAdjustment);
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4 border border-slate-200">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 bg-emerald-50 text-emerald-600 rounded-xl">
              <Wallet className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-slate-900">
                Adjust Wallet — {driver.fullName}
              </h3>
              <p className="text-[11px] text-slate-500 font-mono">ID: {driver.id}</p>
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
              Rs. {currentBalance.toLocaleString('en-PK', { minimumFractionDigits: 2 })}
            </span>
          </div>
          <div className="border-l border-slate-200 pl-3">
            <span className="text-slate-500 block text-[10px] font-semibold">Projected Balance</span>
            <span className={`text-base font-black font-mono ${
              newProjectedBalance >= 0 ? 'text-emerald-700' : 'text-rose-600'
            }`}>
              Rs. {newProjectedBalance.toLocaleString('en-PK', { minimumFractionDigits: 2 })}
            </span>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3.5 text-xs">
          {/* Amount Input */}
          <div className="space-y-1.5">
            <label className="block font-bold text-slate-700">
              Adjustment Amount (PKR) <span className="text-slate-400 font-normal">(use negative for deductions)</span>
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 font-bold text-slate-400 text-xs">Rs.</span>
              <input
                type="number"
                step="any"
                value={walletAmount}
                onChange={(e) => setWalletAmount(e.target.value)}
                placeholder="e.g. 1000 or -500"
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
              {[-500, -1000].map((amt) => (
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

          {/* Reason / Reference */}
          <div className="space-y-1.5">
            <label className="block font-bold text-slate-700">Audit / Settlement Reason</label>
            <select
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full p-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              <option value="Weekly Performance Incentive Bonus">Weekly Performance Incentive Bonus</option>
              <option value="Cash Collection Reconciliation Settlement">Cash Collection Reconciliation Settlement</option>
              <option value="Manual Toll / Fuel Reimbursement">Manual Toll / Fuel Reimbursement</option>
              <option value="Customer Dispute Compensation Refund">Customer Dispute Compensation Refund</option>
              <option value="Admin Correction / Penalty Deduction">Admin Correction / Penalty Deduction</option>
            </select>
          </div>

          {/* Buttons */}
          <div className="flex items-center justify-end space-x-2 pt-2 border-t border-slate-100">
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
                  : 'bg-emerald-600 hover:bg-emerald-700 cursor-pointer'
              }`}
            >
              Apply Rs. {Math.abs(numAdjustment).toLocaleString()} Adjustment
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
