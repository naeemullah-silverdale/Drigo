import React, { useState } from 'react';
import {
  DollarSign,
  Send,
  X,
  CreditCard,
  Building2,
  Smartphone,
  ShieldCheck,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { Driver, PayoutRequest } from '../../types';

interface InitiatePayoutModalProps {
  driver: Driver;
  onClose: () => void;
  onSubmitPayout: (payout: Partial<PayoutRequest>) => void;
}

export const InitiatePayoutModal: React.FC<InitiatePayoutModalProps> = ({
  driver,
  onClose,
  onSubmitPayout,
}) => {
  const maxAvailable = Math.max(0, driver.walletBalance || 0);
  const [amount, setAmount] = useState<number>(Math.min(maxAvailable, 2500));
  const [paymentMethod, setPaymentMethod] = useState<'jazzcash' | 'easypaisa' | 'raast' | 'bank_transfer' | 'cash'>('jazzcash');
  const [accountTitle, setAccountTitle] = useState(driver.fullName || '');
  const [accountNumber, setAccountNumber] = useState(driver.phone || '');
  const [bankName, setBankName] = useState('HBL (Habib Bank Limited)');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (amount <= 0 || amount > maxAvailable) return;

    setIsSubmitting(true);
    const newPayout: Partial<PayoutRequest> = {
      id: `PO-${Date.now().toString().slice(-6)}`,
      driverId: driver.id,
      driverName: driver.fullName,
      driverPhone: driver.phone,
      driverAvatar: driver.avatar,
      amount: Number(amount),
      currency: 'PKR',
      paymentMethod,
      accountTitle: accountTitle.trim() || driver.fullName,
      accountNumber: accountNumber.trim(),
      bankName: paymentMethod === 'bank_transfer' ? bankName : undefined,
      requestedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
      status: 'pending',
    };

    onSubmitPayout(newPayout);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="bg-white rounded-3xl border border-slate-200 p-6 max-w-lg w-full shadow-2xl space-y-4 my-8 relative">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header */}
        <div className="flex items-center space-x-3.5 pr-8">
          <div className="w-11 h-11 rounded-2xl bg-emerald-50 text-emerald-600 border border-emerald-200 flex items-center justify-center font-bold">
            <Send className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-slate-900">Initiate Driver Payout Request</h3>
            <p className="text-xs text-slate-500">
              Create a withdrawal record for <span className="font-bold text-slate-800">{driver.fullName}</span>
            </p>
          </div>
        </div>

        {/* Available Balance Box */}
        <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Available Wallet Balance
            </span>
            <div className="text-xl font-extrabold font-mono text-emerald-700 mt-0.5">
              Rs. {maxAvailable.toLocaleString('en-PK', { minimumFractionDigits: 2 })}
            </div>
          </div>

          <button
            type="button"
            onClick={() => setAmount(maxAvailable)}
            className="px-3 py-1.5 bg-emerald-100 hover:bg-emerald-200 text-emerald-800 text-xs font-bold rounded-xl transition-colors cursor-pointer"
          >
            Withdraw All
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Amount input */}
          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1">
              Withdrawal Amount (PKR / Rs.)
            </label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 font-mono font-bold text-slate-400 text-xs">
                Rs.
              </span>
              <input
                type="number"
                min={100}
                max={maxAvailable}
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-mono font-bold text-slate-900 focus:ring-2 focus:ring-emerald-500/20 focus:outline-none"
                required
              />
            </div>
            {amount > maxAvailable && (
              <p className="text-[11px] text-rose-600 font-bold mt-1 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" />
                Amount exceeds driver available wallet balance of Rs. {maxAvailable.toLocaleString()}
              </p>
            )}
          </div>

          {/* Payment Method Selector */}
          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1">
              Disbursement Channel
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[
                { id: 'jazzcash', label: 'JazzCash', icon: Smartphone },
                { id: 'easypaisa', label: 'EasyPaisa', icon: Smartphone },
                { id: 'raast', label: 'Raast P2P', icon: Building2 },
                { id: 'bank_transfer', label: 'Bank / IBAN', icon: CreditCard },
              ].map((m) => {
                const Icon = m.icon;
                const isSelected = paymentMethod === m.id;
                return (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => setPaymentMethod(m.id as any)}
                    className={`p-2.5 rounded-xl border text-center transition-all cursor-pointer flex flex-col items-center justify-center gap-1 ${
                      isSelected
                        ? 'border-emerald-600 bg-emerald-50/70 text-emerald-800 shadow-2xs font-bold'
                        : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    <Icon className={`w-4 h-4 ${isSelected ? 'text-emerald-600' : 'text-slate-400'}`} />
                    <span className="text-[11px]">{m.label}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Destination Account Details */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">Account Title</label>
              <input
                type="text"
                value={accountTitle}
                onChange={(e) => setAccountTitle(e.target.value)}
                placeholder="e.g. Muhammad Bilal"
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-emerald-500/20 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">
                {paymentMethod === 'bank_transfer' ? 'IBAN (PK..)' : 'Account Number / Mobile'}
              </label>
              <input
                type="text"
                value={accountNumber}
                onChange={(e) => setAccountNumber(e.target.value)}
                placeholder={paymentMethod === 'bank_transfer' ? 'PK36MEZN0000123456789' : '03001234567'}
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs font-mono font-medium focus:ring-2 focus:ring-emerald-500/20 focus:outline-none"
                required
              />
            </div>
          </div>

          {paymentMethod === 'bank_transfer' && (
            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">Bank Name</label>
              <select
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none cursor-pointer"
              >
                <option value="HBL (Habib Bank Limited)">HBL (Habib Bank Limited)</option>
                <option value="Meezan Bank Ltd">Meezan Bank Ltd</option>
                <option value="UBL (United Bank Limited)">UBL (United Bank Limited)</option>
                <option value="MCB Bank Ltd">MCB Bank Ltd</option>
                <option value="Bank Alfalah">Bank Alfalah</option>
                <option value="Standard Chartered Pakistan">Standard Chartered Pakistan</option>
                <option value="Faysal Bank">Faysal Bank</option>
                <option value="Allied Bank (ABL)">Allied Bank (ABL)</option>
                <option value="Nayapay / Sadapay">Nayapay / Sadapay</option>
              </select>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex items-center justify-end space-x-2 pt-2 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || amount <= 0 || amount > maxAvailable}
              className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center space-x-1.5 cursor-pointer"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Submit Withdrawal Request</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
