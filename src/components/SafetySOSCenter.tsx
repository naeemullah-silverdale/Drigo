import React, { useState } from 'react';
import { Trip, SupportTicket, SafetyReport } from '../types';
import {
  ShieldAlert,
  Radio,
  CheckCircle,
  AlertTriangle,
  FileText,
  Clock,
  ShieldCheck,
  MapPin,
  Volume2,
  UserX,
  AlertCircle,
  Siren,
  PhoneCall,
  Activity,
  Filter
} from 'lucide-react';
import { SafetyStatCards } from './safety/SafetyStatCards';
import { EmergencyHotlinesBar } from './safety/EmergencyHotlinesBar';
import { SosIncidentInspector } from './safety/SosIncidentInspector';
import { ConductReportsManager } from './safety/ConductReportsManager';
import { DisputeTicketsManager } from './safety/DisputeTicketsManager';

interface SafetySOSCenterProps {
  trips: Trip[];
  tickets: SupportTicket[];
  safetyReports?: SafetyReport[];
  onResolveSosAlert: (tripId: string, notes: string) => void;
  onUpdateTicketStatus: (ticketId: string, status: SupportTicket['status']) => void;
  onUpdateSafetyReportStatus?: (reportId: string, status: string) => void;
}

export type SafetyViewTab = 'all' | 'sos' | 'conduct' | 'tickets';

export const SafetySOSCenter: React.FC<SafetySOSCenterProps> = ({
  trips,
  tickets,
  safetyReports = [],
  onResolveSosAlert,
  onUpdateTicketStatus,
  onUpdateSafetyReportStatus,
}) => {
  const [activeTab, setActiveTab] = useState<SafetyViewTab>('all');

  const sosTrips = trips.filter(
    (t) => t.status === 'sos_alert' || (t.sosAlert && t.sosAlert.isTriggered)
  );
  const unresolvedSosCount = sosTrips.filter((t) => !t.sosAlert?.resolved).length;
  const pendingConductCount = safetyReports.filter(
    (r) => r.status === 'PENDING_ADMIN_REVIEW'
  ).length;
  const openTicketsCount = tickets.filter((t) => t.status !== 'resolved').length;

  const [selectedSosTrip, setSelectedSosTrip] = useState<Trip | null>(
    sosTrips.find((t) => !t.sosAlert?.resolved) || sosTrips[0] || null
  );

  return (
    <div className="p-4 sm:p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50/50">
      {/* Title & Rapid Status Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200/80 pb-5">
        <div className="flex items-center space-x-3.5">
          <div className="w-11 h-11 bg-rose-600 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-rose-600/25 shrink-0">
            <ShieldAlert className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900 tracking-tight">
                Safety & SOS Emergency Desk
              </h2>
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5 animate-ping" />
                Live Telemetry Active
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Real-time in-flight emergency tracking, route deviation alerts, encrypted audio intercepts & passenger safety moderation.
            </p>
          </div>
        </div>

        {/* Action / Emergency State Pills */}
        <div className="flex items-center space-x-2 shrink-0">
          <span
            className={`px-3.5 py-1.5 rounded-xl text-xs font-extrabold border ${
              unresolvedSosCount > 0
                ? 'bg-rose-100 text-rose-800 border-rose-300 animate-pulse shadow-xs'
                : 'bg-emerald-50 text-emerald-800 border-emerald-200'
            }`}
          >
            {unresolvedSosCount > 0
              ? `🚨 ${unresolvedSosCount} UNRESOLVED SOS ALERTS`
              : '✓ NO ACTIVE EMERGENCIES'}
          </span>
        </div>
      </div>

      {/* Emergency Hotlines Bar */}
      <EmergencyHotlinesBar />

      {/* Operational KPI Metric Cards */}
      <SafetyStatCards
        trips={trips}
        tickets={tickets}
        safetyReports={safetyReports}
      />

      {/* View Switcher Tabs */}
      <div className="flex items-center space-x-1.5 p-1.5 bg-slate-200/70 rounded-2xl overflow-x-auto">
        <button
          onClick={() => setActiveTab('all')}
          className={`px-4 py-2 text-xs font-extrabold rounded-xl transition-all whitespace-nowrap flex items-center space-x-2 ${
            activeTab === 'all'
              ? 'bg-white text-slate-900 shadow-xs'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <Activity className="w-3.5 h-3.5 text-blue-600" />
          <span>All Safety Operations</span>
        </button>

        <button
          onClick={() => setActiveTab('sos')}
          className={`px-4 py-2 text-xs font-extrabold rounded-xl transition-all whitespace-nowrap flex items-center space-x-2 ${
            activeTab === 'sos'
              ? 'bg-rose-600 text-white shadow-xs'
              : 'text-slate-600 hover:text-rose-700'
          }`}
        >
          <ShieldAlert className="w-3.5 h-3.5" />
          <span>Emergency SOS In-Flight</span>
          {unresolvedSosCount > 0 && (
            <span
              className={`px-1.5 py-0.2 rounded-full text-[10px] font-mono font-bold ${
                activeTab === 'sos' ? 'bg-white text-rose-700' : 'bg-rose-100 text-rose-800'
              }`}
            >
              {unresolvedSosCount}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveTab('conduct')}
          className={`px-4 py-2 text-xs font-extrabold rounded-xl transition-all whitespace-nowrap flex items-center space-x-2 ${
            activeTab === 'conduct'
              ? 'bg-amber-600 text-white shadow-xs'
              : 'text-slate-600 hover:text-amber-700'
          }`}
        >
          <AlertTriangle className="w-3.5 h-3.5" />
          <span>Behavior & Conduct Reports</span>
          {pendingConductCount > 0 && (
            <span
              className={`px-1.5 py-0.2 rounded-full text-[10px] font-mono font-bold ${
                activeTab === 'conduct' ? 'bg-white text-amber-700' : 'bg-amber-100 text-amber-800'
              }`}
            >
              {pendingConductCount}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveTab('tickets')}
          className={`px-4 py-2 text-xs font-extrabold rounded-xl transition-all whitespace-nowrap flex items-center space-x-2 ${
            activeTab === 'tickets'
              ? 'bg-blue-600 text-white shadow-xs'
              : 'text-slate-600 hover:text-blue-700'
          }`}
        >
          <FileText className="w-3.5 h-3.5" />
          <span>Support & Dispute Tickets</span>
          {openTicketsCount > 0 && (
            <span
              className={`px-1.5 py-0.2 rounded-full text-[10px] font-mono font-bold ${
                activeTab === 'tickets' ? 'bg-white text-blue-700' : 'bg-blue-100 text-blue-800'
              }`}
            >
              {openTicketsCount}
            </span>
          )}
        </button>
      </div>

      {/* Main Content Sections based on Active Tab */}
      {(activeTab === 'all' || activeTab === 'sos') && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <ShieldAlert className="w-5 h-5 text-rose-600" />
              <h3 className="text-base font-extrabold text-slate-900">
                🚨 In-Flight Emergency SOS Command Center
              </h3>
            </div>
          </div>
          <SosIncidentInspector
            trips={trips}
            selectedTrip={selectedSosTrip}
            onSelectTrip={(trip) => setSelectedSosTrip(trip)}
            onResolveSosAlert={onResolveSosAlert}
          />
        </div>
      )}

      {(activeTab === 'all' || activeTab === 'conduct') && (
        <ConductReportsManager
          safetyReports={safetyReports}
          onUpdateSafetyReportStatus={onUpdateSafetyReportStatus}
        />
      )}

      {(activeTab === 'all' || activeTab === 'tickets') && (
        <DisputeTicketsManager
          tickets={tickets}
          onUpdateTicketStatus={onUpdateTicketStatus}
        />
      )}
    </div>
  );
};
