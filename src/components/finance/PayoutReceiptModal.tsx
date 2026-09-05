import React, { useRef } from 'react';
import {
  FileText,
  Printer,
  Download,
  X,
  CheckCircle2,
  Building2,
  Smartphone,
  ShieldCheck,
  Calendar,
  User,
  QrCode,
  Zap,
  CreditCard
} from 'lucide-react';
import { PayoutRequest } from '../../types';

interface PayoutReceiptModalProps {
  payout: PayoutRequest;
  onClose: () => void;
}

export const PayoutReceiptModal: React.FC<PayoutReceiptModalProps> = ({ payout, onClose }) => {
  const receiptRef = useRef<HTMLDivElement>(null);

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="bg-white rounded-3xl border border-slate-200 p-6 max-w-lg w-full shadow-2xl space-y-5 my-8 relative">
        {/* Top Action Bar */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
              <FileText className="w-4 h-4" />
            </div>
            <span className="text-sm font-extrabold text-slate-900">Drigo Official Settlement Voucher</span>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={handlePrint}
              className="p-1.5 text-slate-600 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition-colors cursor-pointer"
              title="Print Receipt"
            >
              <Printer className="w-4 h-4" />
            </button>
            <button
              onClick={onClose}
              className="p-1.5 text-slate-400 hover:text-slate-700 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Printable Receipt Canvas */}
        <div ref={receiptRef} className="space-y-4 p-5 bg-slate-50/70 rounded-2xl border border-slate-200 text-slate-800 text-xs">
          {/* Header */}
          <div className="flex items-start justify-between border-b border-slate-200 pb-3">
            <div>
              <div className="flex items-center space-x-1.5">
                <div className="w-6 h-6 rounded-lg bg-blue-600 text-white font-extrabold flex items-center justify-center text-xs">
                  D
                </div>
                <span className="font-extrabold text-base tracking-tight text-slate-900">DRIGO</span>
                <span className="text-[10px] font-bold text-blue-600 bg-blue-50 px-1.5 py-0.2 rounded border border-blue-200">
                  FINANCE
                </span>
              </div>
              <p className="text-[10px] text-slate-500 mt-1">Drigo Ride-Sharing Technologies (Pvt.) Ltd.</p>
              <p className="text-[10px] text-slate-400">NTN: 8941204-7 | Islamabad, Pakistan</p>
            </div>

            <div className="text-right">
              <span
                className={`inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider ${
                  payout.status === 'processed' || payout.status === 'approved'
                    ? 'bg-emerald-100 text-emerald-800'
                    : payout.status === 'pending'
                    ? 'bg-amber-100 text-amber-800'
                    : 'bg-rose-100 text-rose-800'
                }`}
              >
                {payout.status === 'processed' ? 'Settled & Paid' : payout.status}
              </span>
              <div className="font-mono text-[10px] text-slate-500 mt-1">VOUCHER #{payout.id}</div>
            </div>
          </div>

          {/* Details Grid */}
          <div className="grid grid-cols-2 gap-3 py-1">
            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase">Driver / Beneficiary</span>
              <div className="font-bold text-slate-900 mt-0.5">{payout.driverName}</div>
              <div className="text-[11px] text-slate-500">{payout.driverPhone}</div>
              <div className="text-[10px] font-mono text-slate-400">ID: {payout.driverId}</div>
            </div>

            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase">Settlement Channel</span>
              <div className="font-bold text-slate-900 mt-0.5 flex items-center gap-1">
                {payout.paymentMethod === 'jazzcash' && '🟠 JazzCash Mobile Wallet'}
                {payout.paymentMethod === 'easypaisa' && '🟢 EasyPaisa Mobile Wallet'}
                {payout.paymentMethod === 'raast' && '⚡ Raast Instant IBFT'}
                {payout.paymentMethod === 'bank_transfer' && `🏦 Bank Transfer (${payout.bankName || 'IBAN'})`}
                {payout.paymentMethod === 'cash' && '💵 Cash Settlement'}
              </div>
              <div className="text-[11px] font-semibold text-slate-600 mt-0.5">Title: {payout.accountTitle}</div>
              <div className="font-mono text-[10px] text-slate-500 break-all">{payout.accountNumber}</div>
            </div>

            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase">Requested At</span>
              <div className="font-semibold text-slate-700 mt-0.5">{payout.requestedAt}</div>
            </div>

            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase">Gateway Reference</span>
              <div className="font-mono font-bold text-slate-900 mt-0.5">
                {payout.transactionRef || 'Pending Settlement'}
              </div>
            </div>
          </div>

          {/* Financial Breakdown Box */}
          <div className="bg-white rounded-xl p-3 border border-slate-200/90 space-y-2">
            <div className="flex justify-between text-xs text-slate-600">
              <span>Gross Payout Requested:</span>
              <span className="font-mono font-semibold">Rs. {payout.amount.toLocaleString()}</span>
            </div>
            <div className="flex justify-between text-xs text-slate-600">
              <span>Gateway Transfer Fee:</span>
              <span className="font-mono font-semibold text-emerald-600">Rs. 0 (Waived by Drigo)</span>
            </div>
            <div className="pt-2 border-t border-slate-100 flex justify-between text-sm font-extrabold text-slate-900">
              <span>Total Dispatched Amount:</span>
              <span className="font-mono text-emerald-700">Rs. {payout.amount.toLocaleString()}</span>
            </div>
          </div>

          {/* Footer & Audit signature */}
          <div className="pt-2 border-t border-slate-200 flex items-center justify-between text-[10px] text-slate-400">
            <div>
              <div>Processed By: {payout.processedBy || 'Admin System'}</div>
              <div>Timestamp: {payout.processedAt || payout.requestedAt}</div>
            </div>
            <div className="flex items-center space-x-1 text-emerald-700 font-bold">
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Verified Financial Record</span>
            </div>
          </div>
        </div>

        {/* Buttons */}
        <div className="flex items-center justify-end space-x-2">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors cursor-pointer"
          >
            Close
          </button>
          <button
            onClick={handlePrint}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors flex items-center gap-1.5 cursor-pointer"
          >
            <Printer className="w-3.5 h-3.5" />
            <span>Print Voucher</span>
          </button>
        </div>
      </div>
    </div>
  );
};
