import React, { useState } from 'react';
import { SupportTicket } from '../../types';
import {
  FileText,
  Search,
  Filter,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Clock,
  User,
  Phone,
  Tag,
  MessageSquare
} from 'lucide-react';

interface DisputeTicketsManagerProps {
  tickets: SupportTicket[];
  onUpdateTicketStatus: (ticketId: string, status: SupportTicket['status']) => void;
}

export const DisputeTicketsManager: React.FC<DisputeTicketsManagerProps> = ({
  tickets,
  onUpdateTicketStatus,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedPriority, setSelectedPriority] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [selectedType, setSelectedType] = useState<string>('ALL');

  const filteredTickets = tickets.filter((tkt) => {
    if (selectedPriority !== 'ALL' && tkt.priority !== selectedPriority) {
      return false;
    }
    if (selectedStatus !== 'ALL' && tkt.status !== selectedStatus) {
      return false;
    }
    if (selectedType !== 'ALL' && tkt.type !== selectedType) {
      return false;
    }

    if (!searchTerm.trim()) return true;
    const q = searchTerm.toLowerCase();
    return (
      tkt.title.toLowerCase().includes(q) ||
      tkt.description.toLowerCase().includes(q) ||
      tkt.userName.toLowerCase().includes(q) ||
      (tkt.tripCode && tkt.tripCode.toLowerCase().includes(q)) ||
      (tkt.userPhone && tkt.userPhone.includes(q))
    );
  });

  const getPriorityBadgeClass = (priority: string) => {
    switch (priority) {
      case 'critical':
        return 'bg-rose-100 text-rose-800 border-rose-200';
      case 'high':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'medium':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  const getTypeLabel = (type: string) => {
    switch (type) {
      case 'safety_sos':
        return '🚨 Safety & SOS';
      case 'fare_dispute':
        return '💰 Fare & Cash Dispute';
      case 'lost_item':
        return '🎒 Lost Item in Vehicle';
      case 'app_crash_bug':
        return '📱 App Bug / GPS Latency';
      case 'driver_conduct':
        return '👤 Driver Conduct';
      case 'vehicle_condition':
        return '🚗 Vehicle Condition';
      default:
        return type;
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-6 shadow-xs space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-blue-100 rounded-xl flex items-center justify-center text-blue-700">
            <FileText className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-slate-900">
              Support & Dispute Tickets Queue
            </h3>
            <p className="text-xs text-slate-500">
              Rider fare disputes, lost item inquiries, and Android app performance tickets.
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <span className="px-3 py-1 bg-blue-50 text-blue-800 border border-blue-200 text-xs font-bold rounded-full">
            {tickets.filter((t) => t.status !== 'resolved').length} OPEN DISPUTES
          </span>
        </div>
      </div>

      {/* Filter Controls */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Search */}
        <div className="relative">
          <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search ticket title, user, phone..."
            className="w-full pl-8 pr-3 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>

        {/* Priority Filter */}
        <div>
          <select
            value={selectedPriority}
            onChange={(e) => setSelectedPriority(e.target.value)}
            className="w-full p-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 font-medium text-slate-700 cursor-pointer"
          >
            <option value="ALL">All Priorities</option>
            <option value="critical">Critical Priority</option>
            <option value="high">High Priority</option>
            <option value="medium">Medium Priority</option>
            <option value="low">Low Priority</option>
          </select>
        </div>

        {/* Status Filter */}
        <div>
          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="w-full p-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 font-medium text-slate-700 cursor-pointer"
          >
            <option value="ALL">All Ticket Statuses</option>
            <option value="open">Open</option>
            <option value="investigating">Investigating</option>
            <option value="resolved">Resolved</option>
          </select>
        </div>

        {/* Type Filter */}
        <div>
          <select
            value={selectedType}
            onChange={(e) => setSelectedType(e.target.value)}
            className="w-full p-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 font-medium text-slate-700 cursor-pointer"
          >
            <option value="ALL">All Ticket Categories</option>
            <option value="safety_sos">Safety SOS</option>
            <option value="fare_dispute">Fare Dispute</option>
            <option value="lost_item">Lost Item</option>
            <option value="app_crash_bug">App Bug / GPS</option>
            <option value="driver_conduct">Driver Conduct</option>
            <option value="vehicle_condition">Vehicle Condition</option>
          </select>
        </div>
      </div>

      {/* Tickets List */}
      {filteredTickets.length === 0 ? (
        <div className="p-10 text-center text-slate-400 space-y-2 bg-slate-50 rounded-2xl border border-slate-100">
          <ShieldCheck className="w-10 h-10 text-emerald-500 mx-auto" />
          <p className="text-sm font-bold text-slate-700">No Support Tickets</p>
          <p className="text-xs">All support and customer dispute tickets have been addressed.</p>
        </div>
      ) : (
        <div className="divide-y divide-slate-100">
          {filteredTickets.map((tkt) => (
            <div
              key={tkt.id}
              className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/60 p-3 rounded-xl transition-colors"
            >
              <div className="space-y-1.5 flex-1 pr-2">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-bold text-sm text-slate-900">{tkt.title}</span>
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase border ${getPriorityBadgeClass(
                      tkt.priority
                    )}`}
                  >
                    {tkt.priority}
                  </span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-700 border border-slate-200">
                    {getTypeLabel(tkt.type)}
                  </span>
                  {tkt.tripCode && (
                    <span className="font-mono font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded text-[10px]">
                      {tkt.tripCode}
                    </span>
                  )}
                </div>

                <p className="text-slate-600 text-xs leading-relaxed">{tkt.description}</p>

                <div className="flex flex-wrap items-center gap-3 text-[11px] text-slate-500 pt-1">
                  <span className="flex items-center space-x-1">
                    <User className="w-3 h-3 text-slate-400" />
                    <span>
                      {tkt.userName} ({tkt.reportedBy})
                    </span>
                  </span>
                  {tkt.userPhone && (
                    <span className="font-mono text-slate-600">{tkt.userPhone}</span>
                  )}
                  {tkt.assignedAgent && (
                    <span className="bg-blue-50 text-blue-700 px-2 py-0.5 rounded font-medium text-[10px]">
                      Agent: {tkt.assignedAgent}
                    </span>
                  )}
                  <span className="text-slate-400 font-mono">Created: {tkt.createdAt}</span>
                </div>
              </div>

              {/* Status Update Control */}
              <div className="flex items-center space-x-2 shrink-0 sm:self-center">
                <span className="text-xs font-semibold text-slate-500">Status:</span>
                <select
                  value={tkt.status}
                  onChange={(e) =>
                    onUpdateTicketStatus(tkt.id, e.target.value as SupportTicket['status'])
                  }
                  className={`px-3 py-1.5 text-xs font-bold rounded-lg border focus:outline-none cursor-pointer transition-colors ${
                    tkt.status === 'open'
                      ? 'bg-rose-50 border-rose-300 text-rose-800'
                      : tkt.status === 'investigating'
                      ? 'bg-amber-50 border-amber-300 text-amber-800'
                      : 'bg-emerald-50 border-emerald-300 text-emerald-800'
                  }`}
                >
                  <option value="open">Open</option>
                  <option value="investigating">Investigating</option>
                  <option value="resolved">Resolved</option>
                </select>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
