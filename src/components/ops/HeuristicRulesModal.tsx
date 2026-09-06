import React, { useState } from 'react';
import {
  Sliders,
  CheckCircle2,
  X,
  Battery,
  Clock,
  TrendingUp,
  ShieldAlert,
  Smartphone,
  CreditCard,
  RotateCcw
} from 'lucide-react';

interface HeuristicRulesModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaveRules: (rules: HeuristicRules) => void;
}

export interface HeuristicRules {
  unassignedTripTimeoutMinutes: number;
  lowBatteryThresholdPercent: number;
  highRamThresholdPercent: number;
  autoSurgeDemandRatio: number;
  sosAutoEscalateSeconds: number;
  payoutFlagThresholdPkr: number;
}

export const DEFAULT_HEURISTIC_RULES: HeuristicRules = {
  unassignedTripTimeoutMinutes: 3,
  lowBatteryThresholdPercent: 25,
  highRamThresholdPercent: 80,
  autoSurgeDemandRatio: 2.0,
  sosAutoEscalateSeconds: 30,
  payoutFlagThresholdPkr: 15000,
};

export const HeuristicRulesModal: React.FC<HeuristicRulesModalProps> = ({
  isOpen,
  onClose,
  onSaveRules,
}) => {
  const [rules, setRules] = useState<HeuristicRules>(() => {
    try {
      const saved = localStorage.getItem('DRIGO_HEURISTIC_RULES');
      return saved ? JSON.parse(saved) : DEFAULT_HEURISTIC_RULES;
    } catch {
      return DEFAULT_HEURISTIC_RULES;
    }
  });

  if (!isOpen) return null;

  const handleSave = () => {
    try {
      localStorage.setItem('DRIGO_HEURISTIC_RULES', JSON.stringify(rules));
    } catch (e) {
      console.warn('Failed to save rules to localStorage:', e);
    }
    onSaveRules(rules);
    onClose();
  };

  const handleReset = () => {
    setRules(DEFAULT_HEURISTIC_RULES);
  };

  return (
    <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 border border-blue-200 flex items-center justify-center">
              <Sliders className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-black text-slate-900">
                Autonomous Heuristics & Anomaly Thresholds
              </h3>
              <p className="text-[11px] text-slate-500">
                Fine-tune trigger sensitivities for background fleet observation
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 text-lg font-bold"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Fields */}
        <div className="space-y-4">
          {/* Unassigned Trip Timeout */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <label className="font-extrabold text-slate-800 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-blue-600" />
                <span>Unassigned Ride Anomaly Threshold</span>
              </label>
              <span className="font-mono font-bold text-blue-600">
                {rules.unassignedTripTimeoutMinutes} Minutes
              </span>
            </div>
            <input
              type="range"
              min="1"
              max="10"
              step="1"
              value={rules.unassignedTripTimeoutMinutes}
              onChange={(e) =>
                setRules({ ...rules, unassignedTripTimeoutMinutes: Number(e.target.value) })
              }
              className="w-full accent-blue-600 cursor-pointer"
            />
            <p className="text-[10px] text-slate-400">
              Flags unassigned passenger requests as high priority if no driver accepts within this duration.
            </p>
          </div>

          {/* Low Battery Warning */}
          <div className="space-y-1.5 pt-2 border-t border-slate-100">
            <div className="flex items-center justify-between text-xs">
              <label className="font-extrabold text-slate-800 flex items-center gap-1.5">
                <Battery className="w-3.5 h-3.5 text-rose-600" />
                <span>Android Driver Low Battery Warning</span>
              </label>
              <span className="font-mono font-bold text-rose-600">
                &lt; {rules.lowBatteryThresholdPercent}%
              </span>
            </div>
            <input
              type="range"
              min="10"
              max="40"
              step="5"
              value={rules.lowBatteryThresholdPercent}
              onChange={(e) =>
                setRules({ ...rules, lowBatteryThresholdPercent: Number(e.target.value) })
              }
              className="w-full accent-rose-600 cursor-pointer"
            />
            <p className="text-[10px] text-slate-400">
              Warns dispatch when budget phone batteries approach critical levels to prevent mid-ride dropouts.
            </p>
          </div>

          {/* High RAM Usage Threshold */}
          <div className="space-y-1.5 pt-2 border-t border-slate-100">
            <div className="flex items-center justify-between text-xs">
              <label className="font-extrabold text-slate-800 flex items-center gap-1.5">
                <Smartphone className="w-3.5 h-3.5 text-amber-600" />
                <span>Low RAM (2GB/3GB) Load Pressure</span>
              </label>
              <span className="font-mono font-bold text-amber-600">
                {rules.highRamThresholdPercent}% RAM Load
              </span>
            </div>
            <input
              type="range"
              min="60"
              max="95"
              step="5"
              value={rules.highRamThresholdPercent}
              onChange={(e) =>
                setRules({ ...rules, highRamThresholdPercent: Number(e.target.value) })
              }
              className="w-full accent-amber-600 cursor-pointer"
            />
            <p className="text-[10px] text-slate-400">
              Triggers memory compression alerts on budget Android hardware running under heavy OS pressure.
            </p>
          </div>

          {/* Auto Surge Ratio */}
          <div className="space-y-1.5 pt-2 border-t border-slate-100">
            <div className="flex items-center justify-between text-xs">
              <label className="font-extrabold text-slate-800 flex items-center gap-1.5">
                <TrendingUp className="w-3.5 h-3.5 text-emerald-600" />
                <span>Surge Multiplier Sensitivity (Demand/Supply)</span>
              </label>
              <span className="font-mono font-bold text-emerald-600">
                {rules.autoSurgeDemandRatio.toFixed(1)}x Demand
              </span>
            </div>
            <input
              type="range"
              min="1.2"
              max="3.5"
              step="0.1"
              value={rules.autoSurgeDemandRatio}
              onChange={(e) =>
                setRules({ ...rules, autoSurgeDemandRatio: Number(e.target.value) })
              }
              className="w-full accent-emerald-600 cursor-pointer"
            />
            <p className="text-[10px] text-slate-400">
              Recommends surge pricing activation when pending passenger ride requests exceed available online drivers by this multiplier.
            </p>
          </div>

          {/* Emergency SOS Escalation */}
          <div className="space-y-1.5 pt-2 border-t border-slate-100">
            <div className="flex items-center justify-between text-xs">
              <label className="font-extrabold text-slate-800 flex items-center gap-1.5">
                <ShieldAlert className="w-3.5 h-3.5 text-rose-600" />
                <span>Emergency SOS Auto-Escalation Window</span>
              </label>
              <span className="font-mono font-bold text-rose-600">
                {rules.sosAutoEscalateSeconds} Seconds
              </span>
            </div>
            <input
              type="range"
              min="10"
              max="90"
              step="5"
              value={rules.sosAutoEscalateSeconds}
              onChange={(e) =>
                setRules({ ...rules, sosAutoEscalateSeconds: Number(e.target.value) })
              }
              className="w-full accent-rose-600 cursor-pointer"
            />
            <p className="text-[10px] text-slate-400">
              Auto-escalates unacknowledged safety SOS triggers directly to priority dispatch center.
            </p>
          </div>
        </div>

        {/* Modal Actions */}
        <div className="flex items-center justify-between pt-3 border-t border-slate-100">
          <button
            onClick={handleReset}
            className="inline-flex items-center space-x-1 px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-xl text-xs font-bold transition-colors"
          >
            <RotateCcw className="w-3 h-3" />
            <span>Restore Defaults</span>
          </button>

          <div className="flex items-center space-x-2">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 active:scale-95 text-white rounded-xl text-xs font-extrabold transition-all shadow-md shadow-blue-600/20"
            >
              Save & Apply Rules
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
