import React, { useState } from 'react';
import { OperationalRecommendation } from '../../types';
import {
  Zap,
  ShieldAlert,
  Car,
  Users,
  Smartphone,
  CreditCard,
  AlertTriangle,
  CheckCircle2,
  ArrowRight,
  Clock,
  Check,
  Eye,
  Sparkles,
  TrendingUp,
  Search,
  SlidersHorizontal,
  X,
  RefreshCw,
  Info
} from 'lucide-react';

interface AutonomousRecommendationsHubProps {
  recommendations: OperationalRecommendation[];
  onExecuteRecommendation: (rec: OperationalRecommendation) => void;
  onOpenSimulation: (rec: OperationalRecommendation) => void;
  onDismissRecommendation: (recId: string) => void;
  executingRecId: string | null;
  executedRecIds: Record<string, boolean>;
}

export const AutonomousRecommendationsHub: React.FC<AutonomousRecommendationsHubProps> = ({
  recommendations,
  onExecuteRecommendation,
  onOpenSimulation,
  onDismissRecommendation,
  executingRecId,
  executedRecIds,
}) => {
  const [activeCategory, setActiveCategory] = useState<string>('all');
  const [activeImpact, setActiveImpact] = useState<'all' | 'high' | 'medium' | 'low'>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [showExecuted, setShowExecuted] = useState<boolean>(false);

  // Categories list with dynamic count
  const categories = [
    { id: 'all', label: 'All Items', icon: Zap },
    { id: 'fleet', label: 'Fleet & Dispatch', icon: Car },
    { id: 'surge', label: 'Surge & Pricing', icon: TrendingUp },
    { id: 'safety', label: 'Safety & SOS', icon: ShieldAlert },
    { id: 'kyc', label: 'KYC Verification', icon: Users },
    { id: 'telemetry', label: 'Android Telemetry', icon: Smartphone },
    { id: 'finance', label: 'Finance & Payouts', icon: CreditCard },
  ];

  const getCategoryCount = (catId: string) => {
    if (catId === 'all') return recommendations.length;
    return recommendations.filter((r) => r.category === catId).length;
  };

  // Filter recommendations
  const filteredRecs = recommendations.filter((r) => {
    const isExecuted = executedRecIds[r.id] || r.executed;
    if (!showExecuted && isExecuted) return false;
    if (showExecuted && !isExecuted) return false;

    if (activeCategory !== 'all' && r.category !== activeCategory) return false;
    if (activeImpact !== 'all' && r.impact !== activeImpact) return false;

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = r.title.toLowerCase().includes(q);
      const matchDesc = r.description.toLowerCase().includes(q);
      const matchCat = r.category.toLowerCase().includes(q);
      const matchRoot = r.rootCause?.toLowerCase().includes(q) || false;
      if (!matchTitle && !matchDesc && !matchCat && !matchRoot) return false;
    }

    return true;
  });

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'fleet': return Car;
      case 'surge': return TrendingUp;
      case 'safety': return ShieldAlert;
      case 'kyc': return Users;
      case 'telemetry': return Smartphone;
      case 'finance': return CreditCard;
      default: return Zap;
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-5">
      {/* Header & Filter Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 border border-amber-200 flex items-center justify-center">
            <Zap className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-extrabold text-slate-900 flex items-center gap-2">
              <span>Operational Recommendations & Anomaly Mitigations</span>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 border border-slate-200">
                {filteredRecs.length} Available
              </span>
            </h3>
            <p className="text-[11px] text-slate-500">
              Heuristic intelligence engine evaluating supply-demand elasticity, driver battery health, and safety escalations.
            </p>
          </div>
        </div>

        {/* View Toggle: Active vs Executed History */}
        <div className="flex items-center space-x-2 shrink-0">
          <button
            onClick={() => setShowExecuted(false)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
              !showExecuted
                ? 'bg-blue-600 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            Active Queue ({recommendations.filter(r => !executedRecIds[r.id] && !r.executed).length})
          </button>
          <button
            onClick={() => setShowExecuted(true)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
              showExecuted
                ? 'bg-slate-900 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            Action History ({recommendations.filter(r => executedRecIds[r.id] || r.executed).length})
          </button>
        </div>
      </div>

      {/* Category Filter Pills & Search */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3 pt-2 border-t border-slate-100">
        <div className="flex items-center space-x-1.5 overflow-x-auto pb-1 lg:pb-0 scrollbar-none">
          {categories.map((cat) => {
            const Icon = cat.icon;
            const count = getCategoryCount(cat.id);
            const isActive = activeCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => setActiveCategory(cat.id)}
                className={`inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap border ${
                  isActive
                    ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                    : 'bg-slate-50 hover:bg-slate-100 text-slate-600 border-slate-200'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-amber-400' : 'text-slate-400'}`} />
                <span>{cat.label}</span>
                <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-extrabold ${
                  isActive ? 'bg-white/20 text-white' : 'bg-slate-200 text-slate-600'
                }`}>
                  {count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Search & Impact Filter */}
        <div className="flex items-center space-x-2 shrink-0">
          <div className="relative flex-1 sm:w-48">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              placeholder="Search anomalies..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3 h-3" />
              </button>
            )}
          </div>

          <select
            value={activeImpact}
            onChange={(e) => setActiveImpact(e.target.value as any)}
            className="px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          >
            <option value="all">All Impacts</option>
            <option value="high">High Priority</option>
            <option value="medium">Medium Priority</option>
            <option value="low">Low Priority</option>
          </select>
        </div>
      </div>

      {/* Recommendations Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {filteredRecs.length === 0 ? (
          <div className="col-span-full py-12 px-6 bg-slate-50 rounded-2xl border border-dashed border-slate-200 text-center space-y-2.5">
            <CheckCircle2 className="w-9 h-9 text-emerald-500 mx-auto" />
            <h4 className="text-sm font-extrabold text-slate-800">
              {showExecuted ? 'No Executed Actions Found' : 'Fleet Operations Nominal'}
            </h4>
            <p className="text-xs text-slate-500 max-w-md mx-auto leading-relaxed">
              {showExecuted
                ? 'No previously executed autonomous mitigation actions match the current filter criteria.'
                : 'No active anomalies or recommendations detected for this category. All Android driver and passenger client connections are operating within nominal thresholds.'}
            </p>
          </div>
        ) : (
          filteredRecs.map((rec) => {
            const isExecuted = executedRecIds[rec.id] || rec.executed;
            const isExecuting = executingRecId === rec.id;
            const CategoryIcon = getCategoryIcon(rec.category);

            return (
              <div
                key={rec.id}
                className={`bg-white rounded-2xl border p-5 shadow-xs transition-all flex flex-col justify-between space-y-4 hover:shadow-md relative overflow-hidden group ${
                  rec.impact === 'high'
                    ? 'border-rose-200/80 hover:border-rose-400'
                    : rec.impact === 'medium'
                    ? 'border-amber-200/80 hover:border-amber-400'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                {/* Top header row */}
                <div className="space-y-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center space-x-1.5 flex-wrap gap-y-1">
                      <span
                        className={`inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase border ${
                          rec.impact === 'high'
                            ? 'bg-rose-50 text-rose-700 border-rose-200'
                            : rec.impact === 'medium'
                            ? 'bg-amber-50 text-amber-700 border-amber-200'
                            : 'bg-blue-50 text-blue-700 border-blue-200'
                        }`}
                      >
                        <CategoryIcon className="w-3 h-3" />
                        <span>{rec.category.toUpperCase()} • {rec.impact.toUpperCase()} IMPACT</span>
                      </span>

                      {rec.confidenceScore && (
                        <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                          <Sparkles className="w-2.5 h-2.5" />
                          <span>{rec.confidenceScore}% AI Confidence</span>
                        </span>
                      )}
                    </div>

                    <span className="text-[10px] text-slate-400 font-mono flex items-center space-x-1 shrink-0">
                      <Clock className="w-3 h-3" />
                      <span>{rec.timestamp}</span>
                    </span>
                  </div>

                  {/* Title & Description */}
                  <div>
                    <h4 className="text-sm font-black text-slate-900 leading-snug group-hover:text-blue-600 transition-colors">
                      {rec.title}
                    </h4>
                    <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                      {rec.description}
                    </p>
                  </div>

                  {/* Root Cause & Estimated Benefit Pills */}
                  {(rec.rootCause || rec.estimatedImpactBenefit) && (
                    <div className="space-y-1.5 pt-1">
                      {rec.rootCause && (
                        <div className="bg-slate-50 p-2 rounded-xl border border-slate-100 text-[11px] text-slate-600 flex items-start space-x-1.5">
                          <Info className="w-3.5 h-3.5 text-blue-500 shrink-0 mt-0.5" />
                          <div>
                            <span className="font-bold text-slate-800">Root Cause: </span>
                            <span>{rec.rootCause}</span>
                          </div>
                        </div>
                      )}

                      {rec.estimatedImpactBenefit && (
                        <div className="bg-emerald-50/70 p-2 rounded-xl border border-emerald-100 text-[11px] text-emerald-800 flex items-center space-x-1.5 font-semibold">
                          <TrendingUp className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                          <span>Predicted Benefit: {rec.estimatedImpactBenefit}</span>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Footer Action Bar */}
                <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                  <button
                    onClick={() => onOpenSimulation(rec)}
                    className="inline-flex items-center space-x-1 px-2.5 py-1.5 rounded-xl text-xs font-bold text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-all"
                    title="Inspect simulation and parameters"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>Inspect Payload</span>
                  </button>

                  <div className="flex items-center space-x-2">
                    {!isExecuted && (
                      <button
                        onClick={() => onDismissRecommendation(rec.id)}
                        className="px-2.5 py-1.5 rounded-xl text-xs font-semibold text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                        title="Dismiss recommendation"
                      >
                        Dismiss
                      </button>
                    )}

                    <button
                      onClick={() => onExecuteRecommendation(rec)}
                      disabled={isExecuted || isExecuting}
                      className={`inline-flex items-center space-x-1.5 px-3.5 py-1.5 rounded-xl text-xs font-extrabold transition-all shadow-xs ${
                        isExecuted
                          ? 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200'
                          : isExecuting
                          ? 'bg-blue-400 text-white cursor-wait'
                          : rec.impact === 'high'
                          ? 'bg-rose-600 hover:bg-rose-700 active:scale-95 text-white shadow-rose-600/20'
                          : 'bg-blue-600 hover:bg-blue-700 active:scale-95 text-white shadow-blue-600/20'
                      }`}
                    >
                      {isExecuting ? (
                        <>
                          <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                          <span>Executing...</span>
                        </>
                      ) : isExecuted ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-emerald-600" />
                          <span>Action Executed</span>
                        </>
                      ) : (
                        <>
                          <span>{rec.actionLabel}</span>
                          <ArrowRight className="w-3.5 h-3.5" />
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
