import React, { useState } from 'react';
import {
  Sliders,
  X,
  ShieldCheck,
  CheckCircle2,
  Smartphone,
  Building2,
  Zap,
  DollarSign,
  Lock,
  Save
} from 'lucide-react';

interface PayoutGatewayConfigModalProps {
  onClose: () => void;
  onSave?: (config: any) => void;
}

export const PayoutGatewayConfigModal: React.FC<PayoutGatewayConfigModalProps> = ({
  onClose,
  onSave,
}) => {
  const [minWithdrawal, setMinWithdrawal] = useState('500');
  const [maxWithdrawal, setMaxWithdrawal] = useState('50000');
  const [autoApproveLimit, setAutoApproveLimit] = useState('2000');
  const [enableJazzCash, setEnableJazzCash] = useState(true);
  const [enableEasyPaisa, setEnableEasyPaisa] = useState(true);
  const [enableRaast, setEnableRaast] = useState(true);
  const [enableBankTransfer, setEnableBankTransfer] = useState(true);
  const [settlementCycle, setSettlementCycle] = useState<'daily' | 'twice_daily' | 'weekly'>('daily');
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onSave) {
      onSave({
        minWithdrawal: Number(minWithdrawal),
        maxWithdrawal: Number(maxWithdrawal),
        autoApproveLimit: Number(autoApproveLimit),
        enableJazzCash,
        enableEasyPaisa,
        enableRaast,
        enableBankTransfer,
        settlementCycle,
      });
    }
    setSavedSuccess(true);
    setTimeout(() => {
      setSavedSuccess(false);
      onClose();
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
      <div className="bg-white rounded-3xl border border-slate-200 p-6 max-w-lg w-full shadow-2xl space-y-5 my-8 relative">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center space-x-3 pr-8">
          <div className="w-10 h-10 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold border border-indigo-100 shadow-2xs">
            <Sliders className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-extrabold text-slate-900">Payout Rules & Gateway Limits</h3>
            <p className="text-xs text-slate-500">Configure Pakistani settlement corridors & auto-limits</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Thresholds */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Min. Withdrawal Limit (Rs.)
              </label>
              <input
                type="number"
                value={minWithdrawal}
                onChange={(e) => setMinWithdrawal(e.target.value)}
                min="100"
                step="50"
                className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 focus:outline-none"
                required
              />
              <span className="text-[10px] text-slate-400 mt-0.5 block">Default: Rs. 500 min</span>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Max. Single Transaction (Rs.)
              </label>
              <input
                type="number"
                value={maxWithdrawal}
                onChange={(e) => setMaxWithdrawal(e.target.value)}
                min="1000"
                step="500"
                className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 focus:outline-none"
                required
              />
              <span className="text-[10px] text-slate-400 mt-0.5 block">Cap for single withdrawal</span>
            </div>
          </div>

          {/* Auto-approval threshold */}
          <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-200 space-y-2">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs font-extrabold text-slate-900">Auto-Approval Fast Track (Rs.)</span>
                <p className="text-[11px] text-slate-500">
                  Requests below this amount with 100% matched CNIC bypass manual review
                </p>
              </div>
              <input
                type="number"
                value={autoApproveLimit}
                onChange={(e) => setAutoApproveLimit(e.target.value)}
                className="w-24 p-2 border border-slate-200 rounded-xl text-xs font-mono font-bold text-right bg-white focus:outline-none"
              />
            </div>
          </div>

          {/* Active Channels */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-2">
              Supported Payout Corridors
            </label>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <label className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition-colors ${enableJazzCash ? 'bg-amber-50/70 border-amber-300' : 'bg-white border-slate-200 opacity-60'}`}>
                <span className="font-bold text-slate-800">🟠 JazzCash</span>
                <input
                  type="checkbox"
                  checked={enableJazzCash}
                  onChange={(e) => setEnableJazzCash(e.target.checked)}
                  className="rounded text-amber-600 focus:ring-amber-500 w-4 h-4 cursor-pointer"
                />
              </label>

              <label className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition-colors ${enableEasyPaisa ? 'bg-emerald-50/70 border-emerald-300' : 'bg-white border-slate-200 opacity-60'}`}>
                <span className="font-bold text-slate-800">🟢 EasyPaisa</span>
                <input
                  type="checkbox"
                  checked={enableEasyPaisa}
                  onChange={(e) => setEnableEasyPaisa(e.target.checked)}
                  className="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4 cursor-pointer"
                />
              </label>

              <label className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition-colors ${enableRaast ? 'bg-purple-50/70 border-purple-300' : 'bg-white border-slate-200 opacity-60'}`}>
                <span className="font-bold text-slate-800">⚡ Raast IBFT</span>
                <input
                  type="checkbox"
                  checked={enableRaast}
                  onChange={(e) => setEnableRaast(e.target.checked)}
                  className="rounded text-purple-600 focus:ring-purple-500 w-4 h-4 cursor-pointer"
                />
              </label>

              <label className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition-colors ${enableBankTransfer ? 'bg-blue-50/70 border-blue-300' : 'bg-white border-slate-200 opacity-60'}`}>
                <span className="font-bold text-slate-800">🏦 1LINK Bank</span>
                <input
                  type="checkbox"
                  checked={enableBankTransfer}
                  onChange={(e) => setEnableBankTransfer(e.target.checked)}
                  className="rounded text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
                />
              </label>
            </div>
          </div>

          {/* Settlement cycle */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">
              Batch Settlement Frequency
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { id: 'twice_daily', label: 'Twice Daily', sub: '11 AM & 6 PM' },
                { id: 'daily', label: 'Daily', sub: 'Once at 5 PM' },
                { id: 'weekly', label: 'Weekly', sub: 'Every Monday' },
              ].map((cycle) => (
                <button
                  type="button"
                  key={cycle.id}
                  onClick={() => setSettlementCycle(cycle.id as any)}
                  className={`p-2.5 rounded-xl border text-center transition-all cursor-pointer ${
                    settlementCycle === cycle.id
                      ? 'bg-indigo-50 border-indigo-300 text-indigo-900 font-bold shadow-2xs'
                      : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50 font-medium'
                  }`}
                >
                  <div className="text-xs">{cycle.label}</div>
                  <div className="text-[10px] text-slate-400 mt-0.5">{cycle.sub}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Action buttons */}
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
              className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors flex items-center gap-1.5 cursor-pointer"
            >
              {savedSuccess ? (
                <>
                  <CheckCircle2 className="w-4 h-4 text-emerald-300" />
                  <span>Config Saved!</span>
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  <span>Save Configuration</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
