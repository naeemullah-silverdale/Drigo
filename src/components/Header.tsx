import React from 'react';
import { Search, Bell, ShieldAlert, RefreshCw, Smartphone, Globe, Database } from 'lucide-react';
import { AdminNotification } from '../types';
import { AdminNotificationsDropdown } from './AdminNotificationsDropdown';

interface HeaderProps {
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  selectedCity: string;
  setSelectedCity: (city: string) => void;
  activeSosCount: number;
  onRefreshData: () => void;
  isRefreshing: boolean;
  onOpenSosCenter: () => void;
  onOpenFirebaseModal: () => void;
  notifications: AdminNotification[];
  onMarkAsRead: (id: string) => void;
  onMarkAllAsRead: () => void;
  onNavigate: (tab: 'dashboard' | 'dispatch' | 'rides' | 'ratings' | 'users' | 'analytics' | 'safety' | 'pricing', targetId?: string) => void;
}

export const Header: React.FC<HeaderProps> = ({
  searchTerm,
  setSearchTerm,
  selectedCity,
  setSelectedCity,
  activeSosCount,
  onRefreshData,
  isRefreshing,
  onOpenSosCenter,
  onOpenFirebaseModal,
  notifications,
  onMarkAsRead,
  onMarkAllAsRead,
  onNavigate,
}) => {
  return (
    <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 shrink-0 z-10 shadow-xs">
      {/* Search Bar */}
      <div className="flex items-center space-x-4 flex-1 max-w-md">
        <div className="relative w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search driver, passenger, trip code (e.g. DRG-8924), phone, plate..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 font-bold"
            >
              ×
            </button>
          )}
        </div>
      </div>

      {/* Right Controls */}
      <div className="flex items-center space-x-2.5">
        {/* Firebase Config Trigger Button */}
        <button
          onClick={onOpenFirebaseModal}
          className="flex items-center space-x-1.5 px-3 py-1.5 bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-800 rounded-xl text-xs font-bold transition-all shadow-2xs"
          title="Configure Firebase Realtime Database & Auth"
        >
          <Database className="w-3.5 h-3.5 text-amber-600" />
          <span className="hidden md:inline">Firebase Sync</span>
        </button>

        {/* City Filter Selector */}
        <div className="flex items-center space-x-2 bg-slate-50 border border-slate-200 rounded-xl px-3 py-1.5 text-xs text-slate-700">
          <Globe className="w-3.5 h-3.5 text-slate-500" />
          <select
            value={selectedCity}
            onChange={(e) => setSelectedCity(e.target.value)}
            className="bg-transparent font-medium text-xs text-slate-700 focus:outline-none cursor-pointer"
          >
            <option value="All">All Metro Cities</option>
            <option value="Nairobi Central">Nairobi Central</option>
            <option value="Westlands">Westlands</option>
            <option value="Kilimani">Kilimani</option>
            <option value="South B">South B</option>
          </select>
        </div>

        {/* Live Refresh Trigger */}
        <button
          onClick={onRefreshData}
          disabled={isRefreshing}
          className="flex items-center space-x-1.5 px-3 py-1.5 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 transition-colors"
          title="Force telemetry refresh"
        >
          <RefreshCw className={`w-3.5 h-3.5 text-slate-600 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span className="hidden sm:inline">Refresh</span>
        </button>

        {/* SOS Emergency Trigger Button */}
        {activeSosCount > 0 ? (
          <button
            onClick={onOpenSosCenter}
            className="flex items-center space-x-2 bg-rose-600 hover:bg-rose-700 text-white px-3.5 py-1.5 rounded-xl text-xs font-bold shadow-md shadow-rose-600/30 transition-all animate-bounce cursor-pointer"
          >
            <ShieldAlert className="w-4 h-4" />
            <span>{activeSosCount} SOS EMERGENCY</span>
          </button>
        ) : (
          <button
            onClick={onOpenSosCenter}
            className="flex items-center space-x-1.5 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 text-emerald-700 px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors"
          >
            <ShieldAlert className="w-3.5 h-3.5 text-emerald-600" />
            <span className="hidden sm:inline">Safety Desk</span>
          </button>
        )}

        {/* Admin Notifications Dropdown Center */}
        <AdminNotificationsDropdown
          notifications={notifications}
          onMarkAsRead={onMarkAsRead}
          onMarkAllAsRead={onMarkAllAsRead}
          onNavigate={onNavigate}
        />

        {/* Live Badge */}
        <div className="flex items-center space-x-1.5 bg-emerald-100 text-emerald-800 px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>LIVE TELEMETRY</span>
        </div>
      </div>
    </header>
  );
};

