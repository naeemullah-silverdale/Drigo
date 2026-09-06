import React, { useState, useRef, useEffect } from 'react';
import { AdminNotification } from '../types';
import {
  Bell,
  Check,
  CheckCheck,
  ShieldAlert,
  UserCheck,
  Car,
  XCircle,
  AlertTriangle,
  CreditCard,
  Wallet,
  UserX,
  ExternalLink,
  ChevronRight
} from 'lucide-react';

interface AdminNotificationsDropdownProps {
  notifications: AdminNotification[];
  onMarkAsRead: (id: string) => void;
  onMarkAllAsRead: () => void;
  onNavigate: (tab: 'dashboard' | 'dispatch' | 'rides' | 'ratings' | 'users' | 'analytics' | 'safety' | 'pricing' | 'finance', targetId?: string) => void;
}

export const AdminNotificationsDropdown: React.FC<AdminNotificationsDropdownProps> = ({
  notifications,
  onMarkAsRead,
  onMarkAllAsRead,
  onNavigate,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'sos_trigger':
        return <ShieldAlert className="w-4 h-4 text-rose-600" />;
      case 'kyc_submission':
      case 'kyc_completion':
        return <UserCheck className="w-4 h-4 text-emerald-600" />;
      case 'kyc_rejection':
        return <XCircle className="w-4 h-4 text-rose-600" />;
      case 'new_passenger_ride_request':
      case 'driver_matched':
      case 'ride_cancellation':
        return <Car className="w-4 h-4 text-blue-600" />;
      case 'incident_report':
        return <AlertTriangle className="w-4 h-4 text-amber-600" />;
      case 'suspended_account':
        return <UserX className="w-4 h-4 text-rose-600" />;
      case 'failed_payment':
        return <CreditCard className="w-4 h-4 text-rose-600" />;
      case 'wallet_issue':
        return <Wallet className="w-4 h-4 text-amber-600" />;
      default:
        return <Bell className="w-4 h-4 text-slate-600" />;
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell Button with Badge */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-xl bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-700 transition-colors flex items-center justify-center cursor-pointer"
        title="Admin Notifications"
      >
        <Bell className="w-4 h-4 text-slate-600" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 bg-rose-600 text-white text-[10px] font-black w-5 h-5 rounded-full flex items-center justify-center shadow-md animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown Panel */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-96 max-w-[calc(100vw-2rem)] bg-white rounded-2xl shadow-2xl border border-slate-200/90 z-50 overflow-hidden animate-fadeIn">
          {/* Header */}
          <div className="px-4 py-3 bg-slate-900 text-white flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Bell className="w-4 h-4 text-amber-400" />
              <span className="font-bold text-xs">Admin Activity Center</span>
              {unreadCount > 0 && (
                <span className="bg-rose-500 text-white px-2 py-0.5 rounded-full text-[10px] font-bold">
                  {unreadCount} unread
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                onClick={() => onMarkAllAsRead()}
                className="text-[11px] text-slate-300 hover:text-white font-semibold flex items-center space-x-1 transition-colors"
              >
                <CheckCheck className="w-3.5 h-3.5" />
                <span>Mark all read</span>
              </button>
            )}
          </div>

          {/* Notifications List */}
          <div className="max-h-96 overflow-y-auto divide-y divide-slate-100">
            {notifications.length === 0 ? (
              <div className="p-8 text-center text-slate-400 text-xs">
                No notifications available.
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => {
                    if (!n.isRead) {
                      onMarkAsRead(n.id);
                    }
                    setIsOpen(false);
                    onNavigate(n.targetTab, n.targetId);
                  }}
                  className={`p-3.5 hover:bg-slate-50 transition-colors cursor-pointer flex items-start space-x-3 ${
                    !n.isRead ? 'bg-blue-50/40' : ''
                  }`}
                >
                  <div className={`p-2 rounded-xl mt-0.5 shrink-0 ${!n.isRead ? 'bg-blue-100' : 'bg-slate-100'}`}>
                    {getNotificationIcon(n.type)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold text-slate-900 truncate">{n.title}</h4>
                      <span className="text-[10px] text-slate-400 font-mono shrink-0 ml-2">{n.timestamp}</span>
                    </div>
                    <p className="text-xs text-slate-600 mt-0.5 line-clamp-2 leading-relaxed">{n.message}</p>
                    <div className="flex items-center justify-between mt-2">
                      <span className="text-[10px] uppercase font-bold text-blue-600 tracking-wider bg-blue-50 px-2 py-0.5 rounded">
                        {n.targetTab} view
                      </span>
                      {!n.isRead && (
                        <span className="w-2 h-2 rounded-full bg-blue-600 animate-ping"></span>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          <div className="p-2.5 bg-slate-50 border-t border-slate-200 text-center text-[11px] text-slate-500 font-medium">
            Real-time backend events synchronized from Firebase
          </div>
        </div>
      )}
    </div>
  );
};
