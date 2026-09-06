import React, { useState } from 'react';
import { SafetyReport } from '../../types';
import {
  AlertTriangle,
  UserX,
  MapPin,
  Search,
  Filter,
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  FileText,
  Clock,
  Phone,
  MessageSquare,
  AlertCircle
} from 'lucide-react';

interface ConductReportsManagerProps {
  safetyReports: SafetyReport[];
  onUpdateSafetyReportStatus?: (reportId: string, status: string) => void;
}

export const ConductReportsManager: React.FC<ConductReportsManagerProps> = ({
  safetyReports,
  onUpdateSafetyReportStatus,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [onlyBlockRequests, setOnlyBlockRequests] = useState<boolean>(false);
  const [investigationNotes, setInvestigationNotes] = useState<Record<string, string>>({});
  const [activeNoteModalReportId, setActiveNoteModalReportId] = useState<string | null>(null);
  const [noteInput, setNoteInput] = useState('');
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 3500);
  };

  // Filter reports
  const filteredReports = safetyReports.filter((report) => {
    if (selectedStatus !== 'ALL' && report.status !== selectedStatus) {
      return false;
    }

    if (selectedCategory !== 'ALL' && report.category !== selectedCategory) {
      return false;
    }

    if (onlyBlockRequests && !report.blockUser) {
      return false;
    }

    if (!searchTerm.trim()) return true;
    const q = searchTerm.toLowerCase();
    return (
      (report.reporterName && report.reporterName.toLowerCase().includes(q)) ||
      (report.reportedUserName && report.reportedUserName.toLowerCase().includes(q)) ||
      (report.description && report.description.toLowerCase().includes(q)) ||
      (report.categoryLabel && report.categoryLabel.toLowerCase().includes(q)) ||
      (report.driverPlateNumber && report.driverPlateNumber.toLowerCase().includes(q)) ||
      (report.rideId && report.rideId.toLowerCase().includes(q))
    );
  });

  const getCategoryBadgeClass = (category: string) => {
    switch (category) {
      case 'harassment':
        return 'bg-rose-100 text-rose-800 border-rose-200';
      case 'reckless_driving':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'overcharging':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'passenger_misconduct':
        return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'route_refusal':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-slate-100 text-slate-800 border-slate-200';
    }
  };

  const handleSaveNote = () => {
    if (!activeNoteModalReportId || !noteInput.trim()) return;
    setInvestigationNotes((prev) => ({
      ...prev,
      [activeNoteModalReportId]: noteInput.trim(),
    }));
    setActiveNoteModalReportId(null);
    setNoteInput('');
    showToast('Investigation note successfully recorded on report audit trail.');
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-6 shadow-xs space-y-5">
      {/* Toast Notification */}
      {toastMsg && (
        <div className="p-3 bg-slate-900 text-white rounded-xl text-xs flex items-center justify-between shadow-md">
          <div className="flex items-center space-x-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{toastMsg}</span>
          </div>
          <button onClick={() => setToastMsg(null)} className="text-slate-400 hover:text-white px-2">
            ✕
          </button>
        </div>
      )}

      {/* Header & Pending Count */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-amber-100 rounded-xl flex items-center justify-center text-amber-700">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-slate-900">
              Passenger Conduct & Inappropriate Behavior Reports
            </h3>
            <p className="text-xs text-slate-500">
              Real-time violation alerts logged by passengers or drivers during active or completed trips.
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <span className="px-3 py-1 bg-amber-50 text-amber-800 border border-amber-200 text-xs font-bold rounded-full">
            {safetyReports.filter((r) => r.status === 'PENDING_ADMIN_REVIEW').length} PENDING REVIEW
          </span>
        </div>
      </div>

      {/* Search & Filter Controls */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Search */}
        <div className="relative">
          <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search person, plate, ride ID, keywords..."
            className="w-full pl-8 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
          />
        </div>

        {/* Category Filter */}
        <div>
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="w-full p-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-amber-500/20 font-medium text-slate-700 cursor-pointer"
          >
            <option value="ALL">All Violation Categories</option>
            <option value="harassment">Inappropriate Remarks & Harassment</option>
            <option value="reckless_driving">Dangerous & Reckless Speeding</option>
            <option value="overcharging">Cash Extortion / Overcharging</option>
            <option value="passenger_misconduct">Passenger Misconduct & Abuse</option>
            <option value="route_refusal">Refusal of Destination Drop-off</option>
          </select>
        </div>

        {/* Status Filter */}
        <div>
          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="w-full p-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-amber-500/20 font-medium text-slate-700 cursor-pointer"
          >
            <option value="ALL">All Report Statuses</option>
            <option value="PENDING_ADMIN_REVIEW">Pending Review</option>
            <option value="UNDER_INVESTIGATION">Under Investigation</option>
            <option value="RESOLVED">Resolved / Actions Completed</option>
            <option value="DISMISSED">Dismissed / No Violation</option>
          </select>
        </div>

        {/* Block Requested Toggle */}
        <div className="flex items-center">
          <label className="flex items-center space-x-2 text-xs font-semibold text-slate-700 cursor-pointer p-2 bg-slate-50 rounded-xl border border-slate-200 w-full hover:bg-slate-100 transition-colors">
            <input
              type="checkbox"
              checked={onlyBlockRequests}
              onChange={(e) => setOnlyBlockRequests(e.target.checked)}
              className="rounded text-amber-600 focus:ring-amber-500 w-4 h-4 cursor-pointer"
            />
            <span className="flex items-center space-x-1">
              <UserX className="w-3.5 h-3.5 text-rose-600" />
              <span>Block Requested Only</span>
            </span>
          </label>
        </div>
      </div>

      {/* Reports List */}
      {filteredReports.length === 0 ? (
        <div className="p-10 text-center text-slate-400 space-y-2 bg-slate-50 rounded-2xl border border-slate-100">
          <ShieldCheck className="w-10 h-10 text-emerald-500 mx-auto" />
          <p className="text-sm font-bold text-slate-700">No Incident Reports</p>
          <p className="text-xs">No conduct violation reports match your active search and filter filters.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredReports.map((report, idx) => {
            const repId = report?.id || `safety-rep-${idx}`;
            let dateStr = 'Recent';
            if (report?.timestamp) {
              try {
                const d = new Date(report.timestamp);
                if (!isNaN(d.getTime())) {
                  dateStr = d.toLocaleString('en-US', {
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  });
                }
              } catch {
                dateStr = 'Recent';
              }
            }

            const attachedNote = investigationNotes[repId];

            return (
              <div
                key={repId}
                className="p-4 sm:p-5 bg-slate-50/70 rounded-2xl border border-slate-200 space-y-4 hover:border-slate-300 transition-colors"
              >
                {/* Header info */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <span
                      className={`px-3 py-1 border text-xs font-extrabold uppercase rounded-lg ${getCategoryBadgeClass(
                        report.category
                      )}`}
                    >
                      {report.categoryLabel || 'Safety Report'}
                    </span>

                    {report.blockUser && (
                      <span className="flex items-center px-2.5 py-1 bg-rose-100 text-rose-800 border border-rose-200 text-[11px] font-bold rounded-lg">
                        <UserX className="w-3.5 h-3.5 mr-1" />
                        Block Requested
                      </span>
                    )}
                  </div>

                  <div className="text-xs text-slate-400 font-mono flex items-center space-x-1">
                    <Clock className="w-3.5 h-3.5" />
                    <span>{dateStr}</span>
                  </div>
                </div>

                {/* Participants & Description Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 text-xs">
                  {/* Reporter & Reported User */}
                  <div className="lg:col-span-5 space-y-2.5">
                    {/* Reporter */}
                    <div className="p-3 bg-white rounded-xl border border-slate-100 space-y-1">
                      <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                        REPORTING PARTY
                      </div>
                      <div className="font-bold text-slate-800 text-xs flex items-center justify-between">
                        <span>
                          {report.reporterName}{' '}
                          <span className="font-medium text-slate-500 text-[10px]">
                            ({report.reporterRole})
                          </span>
                        </span>
                        {report.reporterPhone && (
                          <a
                            href={`tel:${report.reporterPhone}`}
                            className="text-emerald-600 hover:text-emerald-700 flex items-center space-x-1 text-[11px]"
                          >
                            <Phone className="w-3 h-3" />
                            <span>Call</span>
                          </a>
                        )}
                      </div>
                      {report.reporterPhone && (
                        <div className="text-slate-500 font-mono text-[11px]">{report.reporterPhone}</div>
                      )}
                    </div>

                    {/* Reported Person */}
                    <div className="p-3 bg-white rounded-xl border border-slate-100 space-y-1">
                      <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                        REPORTED INDIVIDUAL
                      </div>
                      <div className="font-bold text-rose-700 text-xs">
                        {report.reportedUserName}{' '}
                        <span className="font-medium text-slate-500 text-[10px]">
                          ({report.reportedUserRole})
                        </span>
                      </div>
                      {report.driverPlateNumber && (
                        <div className="text-slate-600 text-[11px]">
                          Vehicle Plate: <strong className="font-mono text-slate-800">{report.driverPlateNumber}</strong>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Incident Description & Route */}
                  <div className="lg:col-span-7 p-3.5 bg-white rounded-xl border border-slate-100 space-y-3 flex flex-col justify-between">
                    <div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-1.5">
                        INCIDENT STATEMENT
                      </div>
                      <p className="text-slate-800 italic text-xs font-medium bg-slate-50 p-3 rounded-lg border border-slate-100 leading-relaxed">
                        "{report.description || 'No detailed statement provided.'}"
                      </p>
                    </div>

                    {attachedNote && (
                      <div className="p-2.5 bg-blue-50 border border-blue-200 rounded-lg text-xs space-y-1">
                        <div className="text-[10px] text-blue-700 font-bold uppercase">
                          SUPERVISOR AUDIT LOG NOTE
                        </div>
                        <p className="text-blue-900 font-medium text-[11px]">{attachedNote}</p>
                      </div>
                    )}

                    <div className="text-[11px] text-slate-500 flex flex-wrap items-center gap-3 pt-2 border-t border-slate-100">
                      <div className="flex items-center space-x-1 truncate">
                        <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        <span className="truncate">
                          <strong>Pickup:</strong> {report.ridePickupTitle || 'Lahore'}
                        </span>
                      </div>

                      {report.rideDestinationTitle && (
                        <div className="flex items-center space-x-1 truncate">
                          <MapPin className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                          <span className="truncate">
                            <strong>Drop-off:</strong> {report.rideDestinationTitle}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Footer Controls */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-3 border-t border-slate-200">
                  <div className="text-[11px] text-slate-400">
                    Report ID: <span className="font-mono bg-slate-200 px-2 py-0.5 rounded text-slate-700">{report.id}</span> • Ride ID: <span className="font-mono bg-slate-200 px-2 py-0.5 rounded text-slate-700">{report.rideId}</span>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      onClick={() => {
                        setActiveNoteModalReportId(repId);
                        setNoteInput(attachedNote || '');
                      }}
                      className="px-3 py-1.5 bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 rounded-lg text-xs font-bold transition-colors flex items-center space-x-1"
                    >
                      <FileText className="w-3.5 h-3.5 text-slate-500" />
                      <span>{attachedNote ? 'Edit Note' : 'Add Note'}</span>
                    </button>

                    <div className="flex items-center space-x-1.5">
                      <span className="text-xs text-slate-500 font-bold">Status:</span>
                      <select
                        value={report.status}
                        onChange={(e) =>
                          onUpdateSafetyReportStatus &&
                          onUpdateSafetyReportStatus(report.id, e.target.value)
                        }
                        className={`px-3 py-1.5 text-xs font-bold rounded-lg border focus:outline-none cursor-pointer transition-colors ${
                          report.status === 'PENDING_ADMIN_REVIEW'
                            ? 'bg-amber-100 border-amber-300 text-amber-900'
                            : report.status === 'UNDER_INVESTIGATION'
                            ? 'bg-blue-100 border-blue-300 text-blue-900'
                            : report.status === 'RESOLVED'
                            ? 'bg-emerald-100 border-emerald-300 text-emerald-900'
                            : 'bg-slate-200 border-slate-300 text-slate-800'
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
              </div>
            );
          })}
        </div>
      )}

      {/* Add Investigation Note Modal */}
      {activeNoteModalReportId && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 space-y-4 shadow-xl border border-slate-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center space-x-2">
                <FileText className="w-5 h-5 text-blue-600" />
                <h4 className="text-sm font-bold text-slate-900">
                  Add Supervisor Investigation Note
                </h4>
              </div>
              <button
                onClick={() => setActiveNoteModalReportId(null)}
                className="text-slate-400 hover:text-slate-700 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-700">
                Inquiry details, driver statements, and disciplinary actions taken:
              </label>
              <textarea
                value={noteInput}
                onChange={(e) => setNoteInput(e.target.value)}
                placeholder="Enter notes on phone investigation with passenger and driver, warning issued, or account action..."
                className="w-full p-3 text-xs border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={4}
              />
            </div>

            <div className="flex justify-end space-x-2 pt-2">
              <button
                onClick={() => setActiveNoteModalReportId(null)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleSaveNote}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl transition-colors shadow-sm"
              >
                Save Investigation Note
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
