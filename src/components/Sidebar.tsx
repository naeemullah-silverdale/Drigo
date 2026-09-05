import React, { useState, useEffect } from 'react';
import {
  LayoutDashboard,
  MapPin,
  Users,
  BarChart3,
  ShieldAlert,
  Sliders,
  Smartphone,
  CheckCircle2,
  AlertTriangle,
  Radio,
  Star,
  LogOut,
  ChevronLeft,
  ChevronRight,
  CreditCard,
  BrainCircuit
} from 'lucide-react';

export type ActiveTab = 'dashboard' | 'dispatch' | 'rides' | 'ratings' | 'users' | 'analytics' | 'safety' | 'pricing' | 'finance' | 'ops_center';

interface SidebarProps {
  activeTab: ActiveTab;
  setActiveTab: (tab: ActiveTab) => void;
  pendingKycCount: number;
  activeSosCount: number;
  activeTripsCount: number;
  pendingPayoutCount?: number;
  adminEmail?: string | null;
  onLogout?: () => void;
  isCollapsed?: boolean;
  onToggleCollapse?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  setActiveTab,
  pendingKycCount,
  activeSosCount,
  activeTripsCount,
  pendingPayoutCount = 0,
  adminEmail,
  onLogout,
  isCollapsed: controlledIsCollapsed,
  onToggleCollapse: controlledOnToggleCollapse,
}) => {
  // Persistence using localStorage & auto-collapsing on smaller screen widths
  const [internalIsCollapsed, setInternalIsCollapsed] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('DRIGO_SIDEBAR_COLLAPSED');
      if (saved !== null) {
        return saved === 'true';
      }
      return window.innerWidth < 1024;
    }
    return false;
  });

  const isCollapsed = controlledIsCollapsed !== undefined ? controlledIsCollapsed : internalIsCollapsed;

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 1024) {
        if (controlledOnToggleCollapse && !controlledIsCollapsed) {
          controlledOnToggleCollapse();
        } else {
          setInternalIsCollapsed(true);
        }
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [controlledIsCollapsed, controlledOnToggleCollapse]);

  const toggleCollapse = () => {
    if (controlledOnToggleCollapse) {
      controlledOnToggleCollapse();
    } else {
      const nextState = !isCollapsed;
      setInternalIsCollapsed(nextState);
      localStorage.setItem('DRIGO_SIDEBAR_COLLAPSED', String(nextState));
    }
  };

  const navItems = [
    {
      id: 'dashboard' as ActiveTab,
      label: 'Dashboard',
      icon: LayoutDashboard,
      badge: null,
    },
    {
      id: 'dispatch' as ActiveTab,
      label: 'Fleet & Live Dispatch',
      icon: MapPin,
      badge: activeTripsCount > 0 ? `${activeTripsCount} active` : null,
      badgeColor: 'bg-blue-500/20 text-blue-400 border border-blue-500/30',
    },
    {
      id: 'rides' as ActiveTab,
      label: 'Ride Management',
      icon: Radio,
      badge: null,
    },
    {
      id: 'ratings' as ActiveTab,
      label: 'Ratings & Reviews',
      icon: Star,
      badge: null,
    },
    {
      id: 'users' as ActiveTab,
      label: 'User & KYC Management',
      icon: Users,
      badge: pendingKycCount > 0 ? `${pendingKycCount} KYC` : null,
      badgeColor: 'bg-amber-500/20 text-amber-400 border border-amber-500/30',
    },
    {
      id: 'analytics' as ActiveTab,
      label: 'Real-time Analytics',
      icon: BarChart3,
      badge: null,
    },
    {
      id: 'safety' as ActiveTab,
      label: 'Safety & SOS Center',
      icon: ShieldAlert,
      badge: activeSosCount > 0 ? `${activeSosCount} SOS` : null,
      badgeColor: 'bg-rose-500 text-white font-bold animate-pulse',
    },
    {
      id: 'pricing' as ActiveTab,
      label: 'Fare & Surge Config',
      icon: Sliders,
      badge: null,
    },
    {
      id: 'finance' as ActiveTab,
      label: 'Finance & Payouts',
      icon: CreditCard,
      badge: pendingPayoutCount > 0 ? `${pendingPayoutCount} pending` : null,
      badgeColor: 'bg-amber-500/20 text-amber-400 border border-amber-500/30',
    },
    {
      id: 'ops_center' as ActiveTab,
      label: 'AI Ops & Memory',
      icon: BrainCircuit,
      badge: 'Live',
      badgeColor: 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30',
    },
  ];

  return (
    <aside 
      className={`bg-slate-900 flex flex-col shrink-0 border-r border-slate-800 select-none transition-all duration-300 ease-in-out ${
        isCollapsed ? 'w-[72px]' : 'w-64'
      }`}
    >
      {/* Brand Header */}
      {isCollapsed ? (
        <div className="p-4 flex items-center justify-center border-b border-slate-800 h-[77px] shrink-0">
          <div className="w-9 h-9 bg-gradient-to-tr from-blue-600 to-indigo-500 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 ring-1 ring-white/20 shrink-0">
            <span className="text-white font-extrabold text-xl tracking-wider">D</span>
          </div>
        </div>
      ) : (
        <div className="p-5 flex items-center justify-between border-b border-slate-800 h-[77px] shrink-0 overflow-hidden whitespace-nowrap">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 bg-gradient-to-tr from-blue-600 to-indigo-500 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 ring-1 ring-white/20 shrink-0">
              <span className="text-white font-extrabold text-xl tracking-wider">D</span>
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="text-white font-extrabold text-lg tracking-tight">DRIGO</span>
                <span className="bg-blue-500/20 text-blue-400 text-[10px] font-bold px-1.5 py-0.5 rounded border border-blue-500/30">
                  ADMIN
                </span>
              </div>
              <p className="text-[11px] text-slate-400">Ride-Sharing Operations</p>
            </div>
          </div>
        </div>
      )}

      {/* Android Fleet Status Banner */}
      {isCollapsed ? (
        <div 
          className="mx-3 mt-4 p-2.5 rounded-lg bg-slate-800/80 border border-slate-700/60 flex items-center justify-center cursor-help shrink-0 group/banner-tooltip relative"
        >
          <Smartphone className="w-4 h-4 text-emerald-400 shrink-0" />
          {/* Tooltip */}
          <div className="absolute left-full ml-3 top-1/2 -translate-y-1/2 hidden group-hover/banner-tooltip:flex items-center z-50 pointer-events-none">
            <div className="w-1.5 h-1.5 bg-slate-950 border-l border-b border-slate-800 transform rotate-45 translate-x-1" />
            <div className="bg-slate-950 text-white text-[11px] font-bold px-3 py-1.5 rounded-lg shadow-xl border border-slate-800 whitespace-nowrap">
              Android Fleet Engine (OK)
            </div>
          </div>
        </div>
      ) : (
        <div className="mx-3 mt-4 p-2.5 rounded-lg bg-slate-800/80 border border-slate-700/60 flex items-center justify-between overflow-hidden whitespace-nowrap shrink-0">
          <div className="flex items-center space-x-2">
            <Smartphone className="w-4 h-4 text-emerald-400 shrink-0" />
            <div>
              <div className="text-[11px] font-semibold text-slate-200">Android Fleet Engine</div>
              <div className="text-[10px] text-slate-400">minSdk 23 • Low-RAM Opt</div>
            </div>
          </div>
          <div className="flex items-center space-x-1">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
            <span className="text-[10px] font-bold text-emerald-400">OK</span>
          </div>
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-2 overflow-y-auto overflow-x-hidden">
        {isCollapsed ? (
          <div className="h-px bg-slate-800 my-2 mx-1 shrink-0" />
        ) : (
          <div className="px-3 pb-2 text-[10px] font-bold text-slate-400 uppercase tracking-wider overflow-hidden whitespace-nowrap shrink-0">
            Core Management
          </div>
        )}
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return isCollapsed ? (
            <div key={item.id} className="relative group/tooltip">
              <button
                onClick={() => setActiveTab(item.id)}
                className={`w-full flex items-center justify-center p-3 rounded-xl transition-all duration-150 relative ${
                  isActive
                    ? 'bg-blue-600 text-white shadow-md shadow-blue-600/30 font-semibold'
                    : 'text-slate-400 hover:bg-slate-800/80 hover:text-slate-200'
                }`}
              >
                <Icon
                  className={`w-5 h-5 shrink-0 transition-colors ${
                    isActive ? 'text-white' : 'text-slate-400 group-hover:text-slate-200'
                  }`}
                />
                {item.badge && (
                  <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 rounded-full bg-rose-500 ring-2 ring-slate-900 animate-pulse" />
                )}
              </button>
              {/* Absolute CSS Tooltip */}
              <div className="absolute left-full ml-3 top-1/2 -translate-y-1/2 hidden group-hover/tooltip:flex items-center z-50 pointer-events-none">
                <div className="w-1.5 h-1.5 bg-slate-950 border-l border-b border-slate-800 transform rotate-45 translate-x-1" />
                <div className="bg-slate-950 text-white text-[11px] font-bold px-3 py-1.5 rounded-lg shadow-xl border border-slate-800 whitespace-nowrap">
                  {item.label}
                  {item.badge && <span className="ml-1.5 text-[9px] text-amber-400 font-extrabold">({item.badge})</span>}
                </div>
              </div>
            </div>
          ) : (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150 group overflow-hidden whitespace-nowrap ${
                isActive
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-600/30 font-semibold'
                  : 'text-slate-400 hover:bg-slate-800/80 hover:text-slate-200'
              }`}
            >
              <div className="flex items-center space-x-3 min-w-0">
                <Icon
                  className={`w-4 h-4 shrink-0 transition-colors ${
                    isActive ? 'text-white' : 'text-slate-400 group-hover:text-slate-200'
                  }`}
                />
                <span className="truncate text-[13px]">{item.label}</span>
              </div>
              {item.badge && (
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold shrink-0 ${item.badgeColor}`}>
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </nav>

      {/* Live Sync Status */}
      {isCollapsed ? (
        <div 
          className="p-3 border-t border-slate-800 bg-slate-900/60 flex items-center justify-center cursor-help shrink-0 group/sync-tooltip relative"
        >
          <Radio className="w-4 h-4 text-emerald-400 animate-pulse shrink-0" />
          {/* Tooltip */}
          <div className="absolute left-full ml-3 top-1/2 -translate-y-1/2 hidden group-hover/sync-tooltip:flex items-center z-50 pointer-events-none">
            <div className="w-1.5 h-1.5 bg-slate-950 border-l border-b border-slate-800 transform rotate-45 translate-x-1" />
            <div className="bg-slate-950 text-white text-[11px] font-bold px-3 py-1.5 rounded-lg shadow-xl border border-slate-800 whitespace-nowrap">
              Real-time Socket (12ms)
            </div>
          </div>
        </div>
      ) : (
        <div className="p-3 border-t border-slate-800 bg-slate-900/60 overflow-hidden whitespace-nowrap shrink-0">
          <div className="flex items-center justify-between px-2 py-1.5 rounded bg-slate-950/60 border border-slate-800 text-[11px] text-slate-400">
            <div className="flex items-center space-x-2">
              <Radio className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
              <span>Real-time Socket</span>
            </div>
            <span className="text-emerald-400 font-mono font-medium">12ms</span>
          </div>
        </div>
      )}

      {/* Collapse Toggle Row */}
      <div className="p-3 border-t border-slate-800 bg-slate-950/20 flex items-center justify-center shrink-0">
        <button
          onClick={toggleCollapse}
          className="w-full py-2 px-3 rounded-lg text-slate-500 hover:text-slate-200 hover:bg-slate-800/60 transition-all flex items-center justify-center gap-2 text-xs font-semibold"
          title={isCollapsed ? "Expand Sidebar" : "Collapse Sidebar"}
        >
          {isCollapsed ? (
            <ChevronRight className="w-4 h-4 animate-pulse" />
          ) : (
            <>
              <ChevronLeft className="w-4 h-4" />
              <span className="truncate text-[11px]">Collapse Sidebar</span>
            </>
          )}
        </button>
      </div>

      {/* Admin Profile */}
      {isCollapsed ? (
        <div className="p-4 border-t border-slate-800 bg-slate-900 flex flex-col items-center space-y-3 shrink-0">
          <div className="relative shrink-0 group/profile-tooltip">
            <img
              src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&auto=format&fit=crop&q=80"
              alt={adminEmail || "Alex Sterling"}
              className="w-9 h-9 rounded-full object-cover border-2 border-slate-700"
            />
            <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-emerald-500 rounded-full border-2 border-slate-900"></span>
            {/* Absolute Tooltip */}
            <div className="absolute left-full ml-3 top-1/2 -translate-y-1/2 hidden group-hover/profile-tooltip:flex items-center z-50 pointer-events-none">
              <div className="w-1.5 h-1.5 bg-slate-950 border-l border-b border-slate-800 transform rotate-45 translate-x-1" />
              <div className="bg-slate-950 text-white text-[11px] font-bold px-3 py-1.5 rounded-lg shadow-xl border border-slate-800 whitespace-nowrap">
                {adminEmail || "Senior Fleet Ops Admin"}
              </div>
            </div>
          </div>
          {onLogout && (
            <button
              onClick={onLogout}
              title="Secure Logout"
              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition-colors shrink-0"
            >
              <LogOut className="w-4 h-4" />
            </button>
          )}
        </div>
      ) : (
        <div className="p-4 border-t border-slate-800 bg-slate-900 flex items-center justify-between overflow-hidden whitespace-nowrap shrink-0">
          <div className="flex items-center space-x-3 min-w-0 flex-1">
            <div className="relative shrink-0">
              <img
                src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&auto=format&fit=crop&q=80"
                alt={adminEmail || "Alex Sterling"}
                className="w-9 h-9 rounded-full object-cover border-2 border-slate-700"
              />
              <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-emerald-500 rounded-full border-2 border-slate-900"></span>
            </div>
            <div className="flex flex-col min-w-0 flex-1">
              <span className="text-xs text-white font-bold truncate">
                {adminEmail ? adminEmail.split('@')[0] : "Alex Sterling"}
              </span>
              <span className="text-[10px] text-slate-400 truncate">
                {adminEmail || "Senior Fleet Ops Admin"}
              </span>
            </div>
          </div>
          {onLogout && (
            <button
              onClick={onLogout}
              title="Secure Logout"
              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition-colors shrink-0 ml-1"
            >
              <LogOut className="w-4 h-4" />
            </button>
          )}
        </div>
      )}
    </aside>
  );
};
