import React from 'react';
import { Trip, SupportTicket, SafetyReport } from '../../types';
import {
  ShieldAlert,
  Radio,
  AlertTriangle,
  FileCheck2,
  Clock,
  ShieldCheck,
  Headphones
} from 'lucide-react';

interface SafetyStatCardsProps {
  trips: Trip[];
  tickets: SupportTicket[];
  safetyReports: SafetyReport[];
}

export const SafetyStatCards: React.FC<SafetyStatCardsProps> = ({
  trips,
  tickets,
  safetyReports,
}) => {
  const activeSosTrips = trips.filter(
    (t) => (t.status === 'sos_alert' || t.sosAlert?.isTriggered) && !t.sosAlert?.resolved
  );
  const liveAudioFeeds = activeSosTrips.filter((t) => t.sosAlert?.liveAudioActive);
  const pendingConductReports = safetyReports.filter(
    (r) => r.status === 'PENDING_ADMIN_REVIEW'
  );
  const criticalTickets = tickets.filter(
    (t) => t.priority === 'critical' && t.status !== 'resolved'
  );
  const resolvedToday = safetyReports.filter((r) => r.status === 'RESOLVED').length +
    trips.filter((t) => t.sosAlert?.resolved).length;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* 1. Active SOS Emergencies */}
      <div
        className={`p-4 sm:p-5 rounded-2xl border transition-all ${
          activeSosTrips.length > 0
            ? 'bg-rose-50/90 border-rose-300 ring-2 ring-rose-500/20 shadow-sm'
            : 'bg-white border-slate-200/80 shadow-xs'
        }`}
      >
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Active SOS Emergencies
          </span>
          <div
            className={`w-9 h-9 rounded-xl flex items-center justify-center ${
              activeSosTrips.length > 0
                ? 'bg-rose-600 text-white animate-pulse shadow-md shadow-rose-600/30'
                : 'bg-slate-100 text-slate-600'
            }`}
          >
            <ShieldAlert className="w-5 h-5" />
          </div>
        </div>

        <div className="mt-2.5">
          <div className="text-2xl sm:text-3xl font-extrabold font-mono tracking-tight text-slate-900">
            {activeSosTrips.length}
          </div>
          <div className="flex items-center space-x-1.5 mt-1.5">
            {activeSosTrips.length > 0 ? (
              <span className="text-[11px] text-rose-700 font-bold bg-rose-100 px-2 py-0.5 rounded-md border border-rose-200 animate-pulse">
                🚨 Immediate Dispatch Required
              </span>
            ) : (
              <span className="text-[11px] text-emerald-700 font-bold bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                ✓ All Rides Clear & Safe
              </span>
            )}
          </div>
        </div>
      </div>

      {/* 2. Live Audio Intercept Feeds */}
      <div className="p-4 sm:p-5 rounded-2xl bg-white border border-slate-200/80 shadow-xs">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Encrypted Audio Streams
          </span>
          <div className="w-9 h-9 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
            <Headphones className="w-5 h-5" />
          </div>
        </div>

        <div className="mt-2.5">
          <div className="text-2xl sm:text-3xl font-extrabold font-mono tracking-tight text-slate-900">
            {liveAudioFeeds.length} <span className="text-xs font-semibold text-slate-400">Live</span>
          </div>
          <div className="flex items-center space-x-1.5 mt-1.5">
            <span className="text-[11px] text-purple-700 font-bold bg-purple-50 px-2 py-0.5 rounded-md border border-purple-200">
              {liveAudioFeeds.length > 0 ? 'Secure Channel In-Flight' : 'Standby Monitoring'}
            </span>
          </div>
        </div>
      </div>

      {/* 3. Behavior & Conduct Violations */}
      <div className="p-4 sm:p-5 rounded-2xl bg-white border border-slate-200/80 shadow-xs">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Pending Incident Reports
          </span>
          <div className="w-9 h-9 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
            <AlertTriangle className="w-5 h-5" />
          </div>
        </div>

        <div className="mt-2.5">
          <div className="text-2xl sm:text-3xl font-extrabold font-mono tracking-tight text-amber-600">
            {pendingConductReports.length}
          </div>
          <div className="flex items-center space-x-1.5 mt-1.5">
            <span className="text-[11px] text-amber-800 font-bold bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
              {pendingConductReports.filter((r) => r.blockUser).length} with Block Requested
            </span>
          </div>
        </div>
      </div>

      {/* 4. Critical Tickets & SLA */}
      <div className="p-4 sm:p-5 rounded-2xl bg-white border border-slate-200/80 shadow-xs">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Dispute & Safety SLA
          </span>
          <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
            <Clock className="w-5 h-5" />
          </div>
        </div>

        <div className="mt-2.5">
          <div className="text-2xl sm:text-3xl font-extrabold font-mono tracking-tight text-slate-900">
            1.8 <span className="text-xs font-semibold text-slate-500">min avg</span>
          </div>
          <div className="flex items-center space-x-1.5 mt-1.5">
            <span className="text-[11px] text-emerald-700 font-bold bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
              {criticalTickets.length} Critical Tickets Open
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
