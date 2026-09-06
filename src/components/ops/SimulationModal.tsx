import React from 'react';
import { OperationalRecommendation } from '../../types';
import {
  Sparkles,
  TrendingUp,
  Clock,
  ShieldCheck,
  Zap,
  ArrowRight,
  Code,
  X,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';

interface SimulationModalProps {
  recommendation: OperationalRecommendation | null;
  onClose: () => void;
  onExecute: (rec: OperationalRecommendation) => void;
  isExecuting: boolean;
  isExecuted: boolean;
}

export const SimulationModal: React.FC<SimulationModalProps> = ({
  recommendation,
  onClose,
  onExecute,
  isExecuting,
  isExecuted,
}) => {
  if (!recommendation) return null;

  return (
    <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-200 flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-black text-slate-900">
                Autonomous Action Simulation & Dry-Run Inspector
              </h3>
              <p className="text-[11px] text-slate-500">
                Heuristic impact projection prior to database state mutation
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

        {/* Anomaly Overview Card */}
        <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-extrabold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">
              {recommendation.category.toUpperCase()} • {recommendation.impact.toUpperCase()} IMPACT
            </span>
            <span className="text-[10px] text-slate-400 font-mono">
              Confidence: {recommendation.confidenceScore || 95}%
            </span>
          </div>
          <h4 className="text-sm font-extrabold text-slate-900">{recommendation.title}</h4>
          <p className="text-xs text-slate-600">{recommendation.description}</p>
        </div>

        {/* Projected Impact Metrics Grid */}
        <div className="space-y-2">
          <h5 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
            Projected Impact Analysis
          </h5>
          <div className="grid grid-cols-3 gap-2.5">
            <div className="bg-emerald-50/80 p-3 rounded-xl border border-emerald-100 text-center">
              <Clock className="w-4 h-4 text-emerald-600 mx-auto mb-1" />
              <div className="text-[10px] text-slate-500 uppercase font-bold">Passenger Wait Time</div>
              <div className="text-xs font-black text-emerald-700 mt-0.5">-3.8 Minutes</div>
            </div>

            <div className="bg-blue-50/80 p-3 rounded-xl border border-blue-100 text-center">
              <TrendingUp className="w-4 h-4 text-blue-600 mx-auto mb-1" />
              <div className="text-[10px] text-slate-500 uppercase font-bold">Dispatch Throughput</div>
              <div className="text-xs font-black text-blue-700 mt-0.5">+22% Efficiency</div>
            </div>

            <div className="bg-indigo-50/80 p-3 rounded-xl border border-indigo-100 text-center">
              <ShieldCheck className="w-4 h-4 text-indigo-600 mx-auto mb-1" />
              <div className="text-[10px] text-slate-500 uppercase font-bold">System Stability</div>
              <div className="text-xs font-black text-indigo-700 mt-0.5">99.8% Nominal</div>
            </div>
          </div>
        </div>

        {/* Root Cause & Target Mutation */}
        <div className="space-y-2">
          <h5 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
            Operational Root Cause
          </h5>
          <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 text-xs text-slate-700 leading-relaxed">
            {recommendation.rootCause || 'Supply-demand imbalance or device telemetry threshold crossed.'}
          </div>
        </div>

        {/* Action Payload JSON */}
        <div className="space-y-1.5">
          <div className="flex items-center space-x-1.5 text-xs font-bold text-slate-700">
            <Code className="w-3.5 h-3.5 text-slate-500" />
            <span>Target Execution Action Payload</span>
          </div>
          <pre className="bg-slate-950 text-emerald-400 text-[11px] p-3 rounded-xl overflow-x-auto max-h-36 font-mono border border-slate-800">
            {JSON.stringify({
              actionType: recommendation.actionType,
              actionLabel: recommendation.actionLabel,
              actionPayload: recommendation.actionPayload || {},
              affectedEntityId: recommendation.affectedEntityId,
              affectedEntityType: recommendation.affectedEntityType,
              timestamp: recommendation.timestamp
            }, null, 2)}
          </pre>
        </div>

        {/* Modal Actions */}
        <div className="flex items-center justify-between pt-3 border-t border-slate-100">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-colors"
          >
            Close Preview
          </button>

          <button
            onClick={() => {
              onExecute(recommendation);
              onClose();
            }}
            disabled={isExecuted || isExecuting}
            className={`inline-flex items-center space-x-1.5 px-4 py-2 rounded-xl text-xs font-extrabold transition-all shadow-md ${
              isExecuted
                ? 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200'
                : isExecuting
                ? 'bg-blue-400 text-white cursor-wait'
                : 'bg-blue-600 hover:bg-blue-700 active:scale-95 text-white shadow-blue-600/20'
            }`}
          >
            {isExecuted ? (
              <>
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>Action Executed</span>
              </>
            ) : (
              <>
                <span>Execute Action: {recommendation.actionLabel}</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
