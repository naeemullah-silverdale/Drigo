import React from 'react';
import { Search, Filter, Download, LayoutGrid, List, RefreshCw, X } from 'lucide-react';

interface UserFiltersProps {
  searchTerm: string;
  onSearchChange: (val: string) => void;
  roleFilter: 'all' | 'driver' | 'rider';
  onRoleFilterChange: (role: 'all' | 'driver' | 'rider') => void;
  statusFilter: 'all' | 'active' | 'pending' | 'suspended_flagged';
  onStatusFilterChange: (status: 'all' | 'active' | 'pending' | 'suspended_flagged') => void;
  viewMode: 'table' | 'cards';
  onViewModeChange: (mode: 'table' | 'cards') => void;
  onExportCsv: () => void;
  totalFilteredCount: number;
  totalAllCount: number;
  driversCount: number;
  ridersCount: number;
  activeCount: number;
  pendingCount: number;
  suspendedCount: number;
}

export const UserFilters: React.FC<UserFiltersProps> = ({
  searchTerm,
  onSearchChange,
  roleFilter,
  onRoleFilterChange,
  statusFilter,
  onStatusFilterChange,
  viewMode,
  onViewModeChange,
  onExportCsv,
  totalFilteredCount,
  totalAllCount,
  driversCount,
  ridersCount,
  activeCount,
  pendingCount,
  suspendedCount,
}) => {
  const isFiltered = searchTerm !== '' || roleFilter !== 'all' || statusFilter !== 'all';

  return (
    <div className="bg-white p-4 rounded-2xl border border-slate-200/90 shadow-2xs space-y-3.5">
      {/* Top Search & Actions Row */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5">
        {/* Search Bar */}
        <div className="relative flex-1 min-w-[240px]">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Search by name, phone (+92), email, vehicle plate, or ID..."
            className="w-full pl-9 pr-8 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none focus:bg-white transition-all"
          />
          {searchTerm && (
            <button
              onClick={() => onSearchChange('')}
              className="absolute right-2.5 top-2 text-slate-400 hover:text-slate-600 p-0.5 rounded-full"
              title="Clear search"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* View Mode Toggle & CSV Export */}
        <div className="flex items-center space-x-2 shrink-0 self-end sm:self-auto">
          {/* Layout Mode */}
          <div className="flex items-center bg-slate-100 p-1 rounded-xl border border-slate-200/70">
            <button
              onClick={() => onViewModeChange('table')}
              className={`p-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1 ${
                viewMode === 'table'
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
              title="Table View"
            >
              <List className="w-3.5 h-3.5" />
              <span className="hidden md:inline text-[11px]">Table</span>
            </button>
            <button
              onClick={() => onViewModeChange('cards')}
              className={`p-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1 ${
                viewMode === 'cards'
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
              title="Card Grid View (Optimal for mobile screens)"
            >
              <LayoutGrid className="w-3.5 h-3.5" />
              <span className="hidden md:inline text-[11px]">Cards</span>
            </button>
          </div>

          {/* Export CSV */}
          <button
            onClick={onExportCsv}
            className="px-3 py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 shadow-2xs transition-all cursor-pointer"
            title="Export filtered directory to CSV report"
          >
            <Download className="w-3.5 h-3.5 text-blue-400" />
            <span className="text-[11px]">Export CSV</span>
          </button>
        </div>
      </div>

      {/* Role & Status Filter Chips */}
      <div className="flex flex-wrap items-center justify-between gap-2.5 pt-2 border-t border-slate-100 text-xs">
        {/* Role Tabs */}
        <div className="flex items-center space-x-1 bg-slate-100 p-1 rounded-xl">
          <button
            onClick={() => onRoleFilterChange('all')}
            className={`px-3 py-1 rounded-lg font-bold text-[11px] transition-all ${
              roleFilter === 'all'
                ? 'bg-white text-slate-900 shadow-2xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            All Roles ({totalAllCount})
          </button>
          <button
            onClick={() => onRoleFilterChange('driver')}
            className={`px-3 py-1 rounded-lg font-bold text-[11px] transition-all ${
              roleFilter === 'driver'
                ? 'bg-blue-600 text-white shadow-2xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Drivers ({driversCount})
          </button>
          <button
            onClick={() => onRoleFilterChange('rider')}
            className={`px-3 py-1 rounded-lg font-bold text-[11px] transition-all ${
              roleFilter === 'rider'
                ? 'bg-emerald-600 text-white shadow-2xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Passengers ({ridersCount})
          </button>
        </div>

        {/* Status Filter Badges */}
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-slate-400 text-[11px] font-bold flex items-center gap-1 mr-1">
            <Filter className="w-3 h-3" /> Status:
          </span>
          <button
            onClick={() => onStatusFilterChange('all')}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all ${
              statusFilter === 'all'
                ? 'bg-slate-900 text-white'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            All
          </button>
          <button
            onClick={() => onStatusFilterChange('active')}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all ${
              statusFilter === 'active'
                ? 'bg-emerald-600 text-white'
                : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
            }`}
          >
            Active / Online ({activeCount})
          </button>
          <button
            onClick={() => onStatusFilterChange('pending')}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all ${
              statusFilter === 'pending'
                ? 'bg-amber-600 text-white'
                : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
            }`}
          >
            Pending KYC ({pendingCount})
          </button>
          <button
            onClick={() => onStatusFilterChange('suspended_flagged')}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all ${
              statusFilter === 'suspended_flagged'
                ? 'bg-rose-600 text-white'
                : 'bg-rose-50 text-rose-700 hover:bg-rose-100'
            }`}
          >
            Restricted ({suspendedCount})
          </button>

          {isFiltered && (
            <button
              onClick={() => {
                onSearchChange('');
                onRoleFilterChange('all');
                onStatusFilterChange('all');
              }}
              className="px-2 py-1 bg-slate-200 hover:bg-slate-300 text-slate-700 rounded-lg text-[10px] font-bold flex items-center gap-1 transition-all"
              title="Reset all filters"
            >
              <RefreshCw className="w-2.5 h-2.5" />
              Reset
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
