import React, { useState } from 'react';
import { LiveActivityFeedItem } from '../../types';
import {
  Activity,
  ShieldAlert,
  AlertTriangle,
  FileCheck,
  CheckCircle2,
  Car,
  CreditCard,
  UserX,
  Search,
  Filter,
  Trash2,
  Eye,
  PlusCircle,
  Clock,
  Smartphone,
  Radio,
  ExternalLink,
  X,
  Code
} from 'lucide-react';

interface SystemMemoryStreamProps {
  feedItems: LiveActivityFeedItem[];
  onNavigateTab: (tab: any, targetId?: string) => void;
  onInjectTestEvent?: (type: string) => void;
  onClearLocalMemory?: () => void;
}

export const SystemMemoryStream: React.FC<SystemMemoryStreamProps> = ({
  feedItems,
  onNavigateTab,
  onInjectTestEvent,
  onClearLocalMemory,
}) => {
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedSeverity, setSelectedSeverity] = useState<string>('all');
  const [selectedType, setSelectedType] = useState<string>('all');
  const [inspectingItem, setInspectingItem] = useState<LiveActivityFeedItem | null>(null);
  const [showInjectModal, setShowInjectModal] = useState<boolean>(false);

  // Filtered items
  const filteredItems = feedItems.filter((item) => {
    if (selectedSeverity !== 'all' && item.severity !== selectedSeverity) return false;
    if (selectedType !== 'all' && item.type !== selectedType) return false;

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = item.title.toLowerCase().includes(q);
      const matchDesc = item.description.toLowerCase().includes(q);
      const matchType = item.type.toLowerCase().includes(q);
      if (!matchTitle && !matchDesc && !matchType) return false;
    }

    return true;
  });

  const getEventIcon = (type: string, severity: string) => {
    if (severity === 'critical') return ShieldAlert;
    if (severity === 'warning') return AlertTriangle;

    switch (type) {
      case 'trip_request':
      case 'driver_matched':
      case 'trip_completed':
        return Car;
      case 'driver_kyc':
      case 'kyc_submitted':
        return FileCheck;
      case 'payout_requested':
        return CreditCard;
      case 'account_suspended':
        return UserX;
      default:
        return CheckCircle2;
    }
  };

  const getSeverityBadgeClass = (severity: string) => {
    switch (severity) {
      case 'critical':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'warning':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'success':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      default:
        return 'bg-blue-50 text-blue-700 border-blue-200';
    }
  };

  const getEventIconClass = (severity: string) => {
    switch (severity) {
      case 'critical':
        return 'bg-rose-100 text-rose-700';
      case 'warning':
        return 'bg-amber-100 text-amber-700';
      case 'success':
        return 'bg-emerald-100 text-emerald-700';
      default:
        return 'bg-blue-100 text-blue-700';
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 border border-blue-200 flex items-center justify-center">
            <Activity className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-extrabold text-slate-900 flex items-center gap-2">
              <span>Live Database State Mutation Stream</span>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                Firebase Realtime Active
              </span>
            </h3>
            <p className="text-[11px] text-slate-500">
              Immutable memory audit stream capturing passenger requests, driver bids, GPS telemetry, and administrative actions.
            </p>
          </div>
        </div>

        {/* Secondary Actions */}
        <div className="flex items-center space-x-2 shrink-0">
          {onInjectTestEvent && (
            <button
              onClick={() => setShowInjectModal(true)}
              className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-colors"
              title="Simulate / Inject test mutation into system memory"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              <span>Simulate Mutation</span>
            </button>
          )}

          {onClearLocalMemory && (
            <button
              onClick={onClearLocalMemory}
              className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors"
              title="Clear Local Memory View"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2 border-t border-slate-100">
        <div className="relative flex-1">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            placeholder="Search state mutations by ID, user, or event..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-8 pr-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
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

        <div className="flex items-center space-x-2 shrink-0">
          <select
            value={selectedSeverity}
            onChange={(e) => setSelectedSeverity(e.target.value)}
            className="px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          >
            <option value="all">All Severities</option>
            <option value="critical">Critical Only</option>
            <option value="warning">Warnings</option>
            <option value="success">Success / Resolved</option>
            <option value="info">Info / State Pings</option>
          </select>

          <select
            value={selectedType}
            onChange={(e) => setSelectedType(e.target.value)}
            className="px-2.5 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          >
            <option value="all">All Event Types</option>
            <option value="trip_request">Trip Requests</option>
            <option value="driver_matched">Driver Matched</option>
            <option value="trip_completed">Trip Completed</option>
            <option value="trip_cancelled">Trip Cancelled</option>
            <option value="sos_alert">SOS Alerts</option>
            <option value="driver_kyc">Driver KYC</option>
            <option value="payout_requested">Payouts</option>
          </select>
        </div>
      </div>

      {/* Stream List */}
      <div className="divide-y divide-slate-100 max-h-[420px] overflow-y-auto border border-slate-100 rounded-xl bg-slate-50/50">
        {filteredItems.length === 0 ? (
          <div className="py-14 px-6 text-center text-slate-400 text-xs space-y-2">
            <Radio className="w-8 h-8 mx-auto text-blue-500 opacity-60 animate-pulse" />
            <p className="font-extrabold text-slate-700">Listening for Realtime Database State Mutations</p>
            <p className="text-[11px] text-slate-500 max-w-sm mx-auto">
              Connected to Firebase Realtime Database. New ride requests, driver verifications, GPS updates, and payout mutations from Android clients will stream here instantaneously.
            </p>
          </div>
        ) : (
          filteredItems.slice(0, 40).map((item) => {
            const Icon = getEventIcon(item.type, item.severity);

            return (
              <div
                key={item.id}
                className="p-3.5 flex items-start justify-between space-x-3 text-xs hover:bg-white transition-colors group"
              >
                <div className="flex items-start space-x-3 min-w-0">
                  <div
                    className={`w-8 h-8 rounded-xl flex items-center justify-center font-bold shrink-0 mt-0.5 ${getEventIconClass(
                      item.severity
                    )}`}
                  >
                    <Icon className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center space-x-2 flex-wrap">
                      <span className="font-extrabold text-slate-900 group-hover:text-blue-600 transition-colors">
                        {item.title}
                      </span>
                      <span
                        className={`text-[9px] px-2 py-0.2 rounded-full font-extrabold uppercase border ${getSeverityBadgeClass(
                          item.severity
                        )}`}
                      >
                        {item.severity}
                      </span>
                      <span className="text-[10px] font-mono text-slate-400">
                        #{item.id.slice(0, 10)}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-600 mt-0.5 leading-relaxed">
                      {item.description}
                    </p>
                  </div>
                </div>

                <div className="flex items-center space-x-2 shrink-0">
                  <span className="text-[10px] font-mono text-slate-400 flex items-center space-x-1">
                    <Clock className="w-3 h-3" />
                    <span>{item.timestamp}</span>
                  </span>

                  <button
                    onClick={() => setInspectingItem(item)}
                    className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    title="Inspect State Payload"
                  >
                    <Eye className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Mutation Payload Inspector Modal */}
      {inspectingItem && (
        <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center space-x-2">
                <Code className="w-5 h-5 text-blue-600" />
                <h4 className="text-sm font-extrabold text-slate-900">
                  Database State Mutation Inspector
                </h4>
              </div>
              <button
                onClick={() => setInspectingItem(null)}
                className="text-slate-400 hover:text-slate-600 text-lg font-bold"
              >
                ×
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400 block">Event Title</span>
                <p className="text-xs font-bold text-slate-900">{inspectingItem.title}</p>
              </div>

              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400 block">Description</span>
                <p className="text-xs text-slate-700">{inspectingItem.description}</p>
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-200">
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Event ID</span>
                  <span className="font-mono text-slate-800 break-all">{inspectingItem.id}</span>
                </div>
                <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-200">
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Timestamp</span>
                  <span className="font-mono text-slate-800">{inspectingItem.timestamp}</span>
                </div>
              </div>

              {/* Raw JSON viewer */}
              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1">State Payload JSON</span>
                <pre className="bg-slate-950 text-emerald-400 text-[11px] p-3 rounded-xl overflow-x-auto max-h-40 font-mono border border-slate-800">
                  {JSON.stringify(inspectingItem, null, 2)}
                </pre>
              </div>
            </div>

            {/* Modal Actions */}
            <div className="flex items-center justify-between pt-3 border-t border-slate-100">
              <div className="flex items-center space-x-2">
                {inspectingItem.tripId && (
                  <button
                    onClick={() => {
                      onNavigateTab('dispatch', inspectingItem.tripId);
                      setInspectingItem(null);
                    }}
                    className="inline-flex items-center space-x-1 px-3 py-1.5 bg-blue-50 text-blue-700 border border-blue-200 rounded-xl text-xs font-bold hover:bg-blue-100 transition-colors"
                  >
                    <span>View Trip</span>
                    <ExternalLink className="w-3 h-3" />
                  </button>
                )}
                {inspectingItem.driverId && (
                  <button
                    onClick={() => {
                      onNavigateTab('users', inspectingItem.driverId);
                      setInspectingItem(null);
                    }}
                    className="inline-flex items-center space-x-1 px-3 py-1.5 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded-xl text-xs font-bold hover:bg-indigo-100 transition-colors"
                  >
                    <span>Inspect Driver</span>
                    <ExternalLink className="w-3 h-3" />
                  </button>
                )}
              </div>

              <button
                onClick={() => setInspectingItem(null)}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold transition-colors"
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Manual Mutation Simulator Modal */}
      {showInjectModal && onInjectTestEvent && (
        <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center space-x-2">
                <PlusCircle className="w-5 h-5 text-blue-600" />
                <h4 className="text-sm font-extrabold text-slate-900">
                  Simulate Android Client State Mutation
                </h4>
              </div>
              <button
                onClick={() => setShowInjectModal(false)}
                className="text-slate-400 hover:text-slate-600 text-lg font-bold"
              >
                ×
              </button>
            </div>

            <p className="text-xs text-slate-600">
              Select a synthetic event type to inject into Drigo's System Memory to verify real-time reactivity and autonomous heuristic evaluation:
            </p>

            <div className="space-y-2">
              <button
                onClick={() => {
                  onInjectTestEvent('trip_request');
                  setShowInjectModal(false);
                }}
                className="w-full text-left p-3 rounded-xl border border-slate-200 hover:border-blue-500 hover:bg-blue-50/50 transition-all flex items-center justify-between group"
              >
                <div>
                  <div className="text-xs font-bold text-slate-900 group-hover:text-blue-700">
                    🚕 Android Passenger Ride Request
                  </div>
                  <div className="text-[11px] text-slate-500">
                    Triggers unassigned ride anomaly & dispatch recommendation
                  </div>
                </div>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">
                  Inject
                </span>
              </button>

              <button
                onClick={() => {
                  onInjectTestEvent('low_battery');
                  setShowInjectModal(false);
                }}
                className="w-full text-left p-3 rounded-xl border border-slate-200 hover:border-amber-500 hover:bg-amber-50/50 transition-all flex items-center justify-between group"
              >
                <div>
                  <div className="text-xs font-bold text-slate-900 group-hover:text-amber-700">
                    🔋 Budget Device Low Battery Warning (&lt; 15%)
                  </div>
                  <div className="text-[11px] text-slate-500">
                    Samsung Galaxy A12 driver battery threshold triggered
                  </div>
                </div>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">
                  Inject
                </span>
              </button>

              <button
                onClick={() => {
                  onInjectTestEvent('sos_alert');
                  setShowInjectModal(false);
                }}
                className="w-full text-left p-3 rounded-xl border border-slate-200 hover:border-rose-500 hover:bg-rose-50/50 transition-all flex items-center justify-between group"
              >
                <div>
                  <div className="text-xs font-bold text-slate-900 group-hover:text-rose-700">
                    🚨 Passenger Emergency SOS Trigger
                  </div>
                  <div className="text-[11px] text-slate-500">
                    Immediate critical escalation anomaly in Peshawar zone
                  </div>
                </div>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-rose-100 text-rose-700">
                  Inject
                </span>
              </button>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setShowInjectModal(false)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-colors"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
