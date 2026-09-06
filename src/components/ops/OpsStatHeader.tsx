import React from 'react';
import {
  BrainCircuit,
  Activity,
  Zap,
  ShieldCheck,
  AlertTriangle,
  Smartphone,
  Sliders,
  Download,
  Play,
  Pause,
  RefreshCw,
  Server
} from 'lucide-react';

interface OpsStatHeaderProps {
  fleetHealthScore: number;
  criticalCount: number;
  highCount: number;
  totalRecommendations: number;
  mutationStreamCount: number;
  lowBatteryDriverCount: number;
  lowRamDriverCount: number;
  isStreamPaused: boolean;
  onToggleStreamPause: () => void;
  onRunDeepDiagnostic: () => void;
  onExportSystemMemory: () => void;
  onOpenRulesConfig: () => void;
  isScanning: boolean;
  totalMonitoredNodes: number;
}

export const OpsStatHeader: React.FC<OpsStatHeaderProps> = ({
  fleetHealthScore,
  criticalCount,
  highCount,
  totalRecommendations,
  mutationStreamCount,
  lowBatteryDriverCount,
  lowRamDriverCount,
  isStreamPaused,
  onToggleStreamPause,
  onRunDeepDiagnostic,
  onExportSystemMemory,
  onOpenRulesConfig,
  isScanning,
  totalMonitoredNodes,
}) => {
  return (
    <div className="space-y-4">
      {/* Top Hero Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-900 to-indigo-950 rounded-2xl p-6 text-white border border-slate-800 shadow-xl relative overflow-hidden">
        {/* Glow background accent */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl -mr-20 -mt-20 pointer-events-none" />
        <div className="absolute bottom-0 left-1/3 w-64 h-64 bg-indigo-500/10 rounded-full blur-2xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-5">
          <div className="flex items-start space-x-4">
            <div className="w-12 h-12 bg-gradient-to-tr from-blue-600 to-indigo-600 text-white rounded-2xl flex items-center justify-center border border-white/20 shadow-lg shadow-blue-500/20 shrink-0">
              <BrainCircuit className={`w-6 h-6 ${isScanning ? 'animate-spin' : 'animate-pulse'}`} />
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-xl font-black tracking-tight text-white">
                  Autonomous Fleet Intelligence & System Memory
                </h2>
                <span className="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] uppercase font-extrabold px-2.5 py-0.5 rounded-full flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                  Neural Observer Active
                </span>
                <span className="bg-blue-500/20 text-blue-300 border border-blue-500/30 text-[10px] uppercase font-bold px-2 py-0.5 rounded-full">
                  Android minSdk 23 Opt
                </span>
              </div>
              <p className="text-xs text-slate-300 mt-1.5 max-w-3xl leading-relaxed">
                Autonomous real-time supervisor monitoring database state mutations, budget Android client telemetry (Samsung A12 / Redmi / Tecno), surge balancing heuristics, and emergency escalations.
              </p>
            </div>
          </div>

          {/* Action Toolbar */}
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={onRunDeepDiagnostic}
              disabled={isScanning}
              className="inline-flex items-center space-x-1.5 px-3 py-2 bg-blue-600 hover:bg-blue-500 active:scale-95 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-blue-600/20 disabled:opacity-50"
              title="Run a heuristic anomaly scan across all connected nodes"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isScanning ? 'animate-spin' : ''}`} />
              <span>{isScanning ? 'Analyzing Fleet...' : 'Deep Heuristic Scan'}</span>
            </button>

            <button
              onClick={onToggleStreamPause}
              className={`inline-flex items-center space-x-1.5 px-3 py-2 rounded-xl text-xs font-bold transition-all border ${
                isStreamPaused
                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/30 hover:bg-amber-500/30'
                  : 'bg-white/10 hover:bg-white/20 text-slate-200 border-white/10'
              }`}
              title={isStreamPaused ? 'Resume live mutation stream' : 'Pause live mutation stream'}
            >
              {isStreamPaused ? (
                <>
                  <Play className="w-3.5 h-3.5 text-amber-400" />
                  <span>Stream Paused</span>
                </>
              ) : (
                <>
                  <Pause className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Stream Live</span>
                </>
              )}
            </button>

            <button
              onClick={onExportSystemMemory}
              className="inline-flex items-center space-x-1.5 px-3 py-2 bg-white/10 hover:bg-white/20 text-slate-200 rounded-xl text-xs font-bold transition-all border border-white/10"
              title="Export system memory audit stream as JSON"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Export Audit</span>
            </button>

            <button
              onClick={onOpenRulesConfig}
              className="p-2 bg-white/10 hover:bg-white/20 text-slate-200 rounded-xl transition-all border border-white/10"
              title="Configure Autonomous Heuristic Rules & Thresholds"
            >
              <Sliders className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5">
        {/* Fleet Health Index */}
        <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-xs flex items-center space-x-3.5">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100 flex items-center justify-center shrink-0">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Fleet Health Index</div>
            <div className="flex items-baseline space-x-1.5 mt-0.5">
              <span className="text-xl font-black text-slate-900">{fleetHealthScore}%</span>
              <span className={`text-[10px] font-bold px-1.5 py-0.2 rounded-full ${
                fleetHealthScore >= 90 ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
              }`}>
                {fleetHealthScore >= 90 ? 'Optimal' : 'Attention'}
              </span>
            </div>
            <div className="text-[10px] text-slate-400 mt-0.5">
              {totalMonitoredNodes} Connected Nodes
            </div>
          </div>
        </div>

        {/* Operational Anomalies */}
        <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-xs flex items-center space-x-3.5">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 border ${
            criticalCount > 0
              ? 'bg-rose-50 text-rose-600 border-rose-100 animate-pulse'
              : 'bg-amber-50 text-amber-600 border-amber-100'
          }`}>
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Active Anomalies</div>
            <div className="flex items-baseline space-x-1.5 mt-0.5">
              <span className="text-xl font-black text-slate-900">{totalRecommendations}</span>
              {criticalCount > 0 && (
                <span className="text-[10px] font-extrabold px-1.5 py-0.2 rounded-full bg-rose-100 text-rose-700">
                  {criticalCount} Critical
                </span>
              )}
            </div>
            <div className="text-[10px] text-slate-400 mt-0.5">
              {highCount} High Priority Action Items
            </div>
          </div>
        </div>

        {/* System Memory Mutations */}
        <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-xs flex items-center space-x-3.5">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 border border-blue-100 flex items-center justify-center shrink-0">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">System Memory Stream</div>
            <div className="flex items-baseline space-x-1.5 mt-0.5">
              <span className="text-xl font-black text-slate-900">{mutationStreamCount}</span>
              <span className="text-[10px] font-bold px-1.5 py-0.2 rounded-full bg-blue-100 text-blue-700">
                Mutations
              </span>
            </div>
            <div className="text-[10px] text-slate-400 mt-0.5">
              Realtime Socket 12ms Latency
            </div>
          </div>
        </div>

        {/* Android Device Guard */}
        <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-xs flex items-center space-x-3.5">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 border ${
            lowBatteryDriverCount > 0 || lowRamDriverCount > 0
              ? 'bg-amber-50 text-amber-600 border-amber-100'
              : 'bg-indigo-50 text-indigo-600 border-indigo-100'
          }`}>
            <Smartphone className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Android Device Telemetry</div>
            <div className="flex items-baseline space-x-1.5 mt-0.5">
              <span className="text-xl font-black text-slate-900">
                {lowBatteryDriverCount + lowRamDriverCount}
              </span>
              <span className="text-[10px] font-bold px-1.5 py-0.2 rounded-full bg-amber-100 text-amber-800">
                Warnings
              </span>
            </div>
            <div className="text-[10px] text-slate-400 mt-0.5">
              {lowBatteryDriverCount} Low Battery • {lowRamDriverCount} High RAM
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
