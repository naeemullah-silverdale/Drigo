import React, { useState } from 'react';
import { Trip, SupportTicket, SafetyReport } from '../types';
import {
  ShieldAlert,
  Phone,
  Radio,
  CheckCircle,
  AlertTriangle,
  UserCheck,
  FileText,
  Clock,
  ShieldCheck,
  MapPin,
  Volume2,
  UserX,
  AlertCircle,
  ShieldX,
  MessageSquareOff
} from 'lucide-react';

interface SafetySOSCenterProps {
  trips: Trip[];
  tickets: SupportTicket[];
  safetyReports?: SafetyReport[];
  onResolveSosAlert: (tripId: string, notes: string) => void;
  onUpdateTicketStatus: (ticketId: string, status: SupportTicket['status']) => void;
  onUpdateSafetyReportStatus?: (reportId: string, status: string) => void;
}

export const SafetySOSCenter: React.FC<SafetySOSCenterProps> = ({
  trips,
  tickets,
  safetyReports = [],
  onResolveSosAlert,
  onUpdateTicketStatus,
  onUpdateSafetyReportStatus,
}) => {
  const sosTrips = trips.filter(t => t.status === 'sos_alert' || (t.sosAlert && t.sosAlert.isTriggered));
  const [selectedSosTrip, setSelectedSosTrip] = useState<Trip | null>(sosTrips[0] || null);
  const [resolutionNotes, setResolutionNotes] = useState('');

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50">
      {/* Title */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-rose-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-rose-600/30 animate-pulse">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-extrabold text-slate-900">Safety & SOS Emergency Desk</h2>
            <p className="text-xs text-slate-500">Real-time emergency tracking, route deviation alerts, and live audio intercepts.</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <span className="px-3 py-1 bg-rose-100 text-rose-800 rounded-full text-xs font-bold border border-rose-200">
            {sosTrips.filter(t => !t.sosAlert?.resolved).length} UNRESOLVED SOS ALERTS
          </span>
        </div>
      </div>

      {/* SOS Alert Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Active SOS List */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-3">
          <h3 className="text-sm font-bold text-slate-900 mb-2">Active Emergency Incident Queue</h3>
          {sosTrips.length === 0 ? (
            <div className="p-8 text-center text-slate-400 space-y-2">
              <ShieldCheck className="w-8 h-8 text-emerald-500 mx-auto" />
              <p className="text-xs font-bold text-slate-700">All Rides Safe</p>
              <p className="text-[11px]">No active SOS emergency triggers reported in the system.</p>
            </div>
          ) : (
            sosTrips.map((trip) => {
              const isSelected = selectedSosTrip?.id === trip.id;
              return (
                <button
                  key={trip.id}
                  onClick={() => setSelectedSosTrip(trip)}
                  className={`w-full text-left p-3 rounded-xl border transition-all text-xs space-y-1.5 ${
                    isSelected
                      ? 'bg-rose-50 border-rose-500 ring-2 ring-rose-500/20'
                      : 'bg-slate-50 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-mono font-bold text-rose-700 bg-rose-100 px-2 py-0.5 rounded">
                      {trip.tripCode}
                    </span>
                    <span className="text-[10px] font-bold text-rose-600 animate-pulse">
                      {trip.sosAlert?.triggeredAt || 'NOW'}
                    </span>
                  </div>

                  <div className="font-bold text-slate-900">{trip.passengerName} (Passenger)</div>
                  <p className="text-[11px] text-slate-600 line-clamp-2">{trip.sosAlert?.reason}</p>
                </button>
              );
            })
          )}
        </div>

        {/* Selected Incident Control Inspector */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
          {selectedSosTrip ? (
            <>
              <div className="flex items-center justify-between pb-3 border-b border-slate-200">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-mono text-sm font-bold text-rose-700 bg-rose-100 px-2.5 py-0.5 rounded">
                      {selectedSosTrip.tripCode}
                    </span>
                    <h3 className="text-base font-extrabold text-slate-900">
                      Emergency Alert on {selectedSosTrip.fromLocation.name}
                    </h3>
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Triggered by {String(selectedSosTrip.sosAlert?.triggeredBy || '').toUpperCase()} at {selectedSosTrip.sosAlert?.triggeredAt}
                  </p>
                </div>

                <span className={`px-3 py-1 rounded-full text-xs font-bold uppercase ${
                  selectedSosTrip.sosAlert?.resolved
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-rose-600 text-white animate-bounce'
                }`}>
                  {selectedSosTrip.sosAlert?.resolved ? 'RESOLVED' : 'ACTIVE EMERGENCY'}
                </span>
              </div>

              {/* Live Audio & Escalation Bar */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl flex items-center space-x-3">
                  <Volume2 className="w-5 h-5 text-rose-600 animate-pulse" />
                  <div>
                    <div className="text-[10px] text-rose-700 uppercase font-bold">LIVE AUDIO FEED</div>
                    <div className="text-xs font-bold text-slate-900">
                      {selectedSosTrip.sosAlert?.liveAudioActive ? 'Channel Active (Encrypted)' : 'Inactive'}
                    </div>
                  </div>
                </div>

                <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl flex items-center space-x-3">
                  <ShieldCheck className="w-5 h-5 text-blue-600" />
                  <div>
                    <div className="text-[10px] text-blue-700 uppercase font-bold">POLICE DISPATCH</div>
                    <div className="text-xs font-bold text-slate-900">
                      {selectedSosTrip.sosAlert?.policeNotified ? 'Notified & Transmitted' : 'Pending'}
                    </div>
                  </div>
                </div>

                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center space-x-3">
                  <Phone className="w-5 h-5 text-emerald-600" />
                  <div>
                    <div className="text-[10px] text-emerald-700 uppercase font-bold">EMERGENCY CONTACTS</div>
                    <div className="text-xs font-bold text-slate-900">SMS Alert Dispatched</div>
                  </div>
                </div>
              </div>

              {/* Trip Participants */}
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs">
                <div>
                  <div className="text-[10px] uppercase font-bold text-slate-400">PASSENGER</div>
                  <div className="font-bold text-slate-900">{selectedSosTrip.passengerName}</div>
                  <div className="text-slate-500">{selectedSosTrip.passengerPhone}</div>
                </div>
                <div>
                  <div className="text-[10px] uppercase font-bold text-slate-400">DRIVER</div>
                  <div className="font-bold text-slate-900">{selectedSosTrip.driverName || 'N/A'}</div>
                  <div className="text-slate-500">{selectedSosTrip.driverPhone || 'N/A'} • {selectedSosTrip.vehiclePlate}</div>
                </div>
              </div>

              {/* Resolution Notes Action */}
              {!selectedSosTrip.sosAlert?.resolved && (
                <div className="p-4 bg-slate-900 text-white rounded-xl space-y-3">
                  <h4 className="text-xs font-bold text-slate-200">Resolve Safety Incident & File Report</h4>
                  <textarea
                    value={resolutionNotes}
                    onChange={(e) => setResolutionNotes(e.target.value)}
                    placeholder="Enter resolution details, police dispatch response notes, and driver contact verification..."
                    className="w-full p-2.5 bg-slate-800 border border-slate-700 rounded-lg text-xs text-white placeholder-slate-400 focus:outline-none"
                    rows={2}
                  />
                  <div className="flex justify-end">
                    <button
                      onClick={() => {
                        onResolveSosAlert(selectedSosTrip.id, resolutionNotes || 'Verified route deviation resolved safely via phone call.');
                        setResolutionNotes('');
                      }}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-lg transition-colors shadow-md"
                    >
                      Mark Incident Resolved
                    </button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="p-8 text-center text-slate-400">Select an emergency incident to view telemetry.</div>
          )}
        </div>
      </div>

      {/* Passenger Conduct & Inappropriate Behavior Reports */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-amber-100 rounded-lg flex items-center justify-center text-amber-600">
              <AlertCircle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">Passenger Conduct & Inappropriate Behavior Reports</h3>
              <p className="text-[11px] text-slate-500">Real-time alerts filed by passengers or drivers, requiring immediate administrative review.</p>
            </div>
          </div>
          <span className="px-2.5 py-0.5 bg-amber-50 text-amber-800 border border-amber-200 text-[10px] font-bold rounded-full w-fit">
            {safetyReports.filter(r => r.status === 'PENDING_ADMIN_REVIEW').length} PENDING REVIEW
          </span>
        </div>

        {safetyReports.length === 0 ? (
          <div className="p-8 text-center text-slate-400 space-y-2">
            <ShieldCheck className="w-8 h-8 text-emerald-500 mx-auto" />
            <p className="text-xs font-bold text-slate-700">No Behaviour Reports</p>
            <p className="text-[11px]">No inappropriate conduct or safety violation reports have been lodged.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {safetyReports.map((report, idx) => {
              const repId = report?.id || `safety-rep-${idx}`;
              const dateStr = report?.timestamp ? new Date(report.timestamp).toLocaleString() : 'Recent';
              return (
                <div key={repId} className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-3 hover:border-slate-300 transition-colors">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="px-2.5 py-1 bg-rose-100 text-rose-800 border border-rose-200 text-[10px] font-extrabold uppercase rounded">
                        {report.categoryLabel}
                      </span>
                      {report.blockUser && (
                        <span className="flex items-center px-2.5 py-1 bg-amber-100 text-amber-800 border border-amber-200 text-[10px] font-bold rounded">
                          <UserX className="w-3.5 h-3.5 mr-1" />
                          Block Requested
                        </span>
                      )}
                    </div>
                    <span className="text-[11px] text-slate-400 font-mono">
                      {dateStr}
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div className="space-y-2">
                      <div className="p-2.5 bg-white rounded-lg border border-slate-100 space-y-1">
                        <div className="text-[10px] text-slate-400 font-bold uppercase">REPORTER</div>
                        <div className="font-bold text-slate-800">
                          {report.reporterName} <span className="font-medium text-slate-500 text-[10px]">({report.reporterRole})</span>
                        </div>
                        {report.reporterPhone && <div className="text-slate-500 text-[11px]">{report.reporterPhone}</div>}
                      </div>

                      <div className="p-2.5 bg-white rounded-lg border border-slate-100 space-y-1">
                        <div className="text-[10px] text-slate-400 font-bold uppercase">REPORTED INDIVIDUAL</div>
                        <div className="font-bold text-rose-700">
                          {report.reportedUserName} <span className="font-medium text-slate-500 text-[10px]">({report.reportedUserRole})</span>
                        </div>
                        {report.driverPlateNumber && <div className="text-slate-500 text-[11px]">Plate: {report.driverPlateNumber}</div>}
                      </div>
                    </div>

                    <div className="p-3 bg-white rounded-lg border border-slate-100 space-y-2 flex flex-col justify-between">
                      <div>
                        <div className="text-[10px] text-slate-400 font-bold uppercase mb-1">INCIDENT DESCRIPTION</div>
                        <p className="text-slate-700 italic text-[11px] font-medium bg-slate-50 p-2 rounded-md border border-slate-100 leading-relaxed">
                          "{report.description || 'No description provided.'}"
                        </p>
                      </div>

                      <div className="text-[11px] text-slate-500 flex items-center space-x-1.5 pt-2">
                        <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        <span className="truncate">
                          <strong>Pickup:</strong> {report.ridePickupTitle || 'N/A'}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2 border-t border-slate-200">
                    <div className="text-[11px] text-slate-400">
                      Report ID: <span className="font-mono bg-slate-200 px-1.5 py-0.5 rounded text-slate-600">{report.id}</span> • Ride: <span className="font-mono bg-slate-200 px-1.5 py-0.5 rounded text-slate-600">{report.rideId}</span>
                    </div>

                    <div className="flex items-center space-x-2">
                      <span className="text-xs text-slate-500 font-bold">Action / Status:</span>
                      <select
                        value={report.status}
                        onChange={(e) => onUpdateSafetyReportStatus && onUpdateSafetyReportStatus(report.id, e.target.value)}
                        className={`px-3 py-1.5 text-xs font-bold rounded-lg border focus:outline-none cursor-pointer ${
                          report.status === 'PENDING_ADMIN_REVIEW'
                            ? 'bg-amber-100 border-amber-300 text-amber-800'
                            : report.status === 'UNDER_INVESTIGATION'
                            ? 'bg-blue-100 border-blue-300 text-blue-800'
                            : 'bg-emerald-100 border-emerald-300 text-emerald-800'
                        }`}
                      >
                        <option value="PENDING_ADMIN_REVIEW">Pending Review</option>
                        <option value="UNDER_INVESTIGATION">Under Investigation</option>
                        <option value="RESOLVED">Resolved / Actions Completed</option>
                        <option value="DISMISSED">Dismissed / No Violation</option>
                      </select>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Support Tickets Queue */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
        <h3 className="text-sm font-bold text-slate-900">Support & Dispute Tickets Queue</h3>
        <div className="divide-y divide-slate-100">
          {tickets.map((tkt) => (
            <div key={tkt.id} className="py-3 flex items-center justify-between text-xs">
              <div>
                <div className="flex items-center space-x-2">
                  <span className="font-bold text-slate-900">{tkt.title}</span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                    tkt.priority === 'critical' ? 'bg-rose-100 text-rose-800' : 'bg-slate-100 text-slate-700'
                  }`}>
                    {tkt.priority}
                  </span>
                </div>
                <p className="text-slate-500 text-[11px] mt-0.5">{tkt.description}</p>
              </div>

              <div className="flex items-center space-x-2">
                <span className="text-slate-400 font-mono">{tkt.createdAt}</span>
                <select
                  value={tkt.status}
                  onChange={(e) => onUpdateTicketStatus(tkt.id, e.target.value as SupportTicket['status'])}
                  className="bg-slate-100 border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-700 cursor-pointer"
                >
                  <option value="open">Open</option>
                  <option value="investigating">Investigating</option>
                  <option value="resolved">Resolved</option>
                </select>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
