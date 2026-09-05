import React, { useState, useMemo } from 'react';
import { Trip, Driver, Rider, SupportTicket, LiveActivityFeedItem, SurgeZone, SafetyReport, PayoutRequest } from '../types';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { REVENUE_HOURLY_DATA, HARDWARE_DEVICE_STATS } from '../mockData';
import {
  Car,
  Users,
  DollarSign,
  ShieldAlert,
  Smartphone,
  TrendingUp,
  Activity,
  ArrowUpRight,
  ChevronRight,
  CheckCircle,
  AlertTriangle,
  CreditCard,
  Gauge,
  Layers,
  Cpu,
  Radio,
  Clock,
  SlidersHorizontal,
} from 'lucide-react';

interface DashboardOverviewProps {
  trips: Trip[];
  drivers: Driver[];
  riders: Rider[];
  tickets: SupportTicket[];
  feed: LiveActivityFeedItem[];
  surgeZones?: SurgeZone[];
  safetyReports?: SafetyReport[];
  payoutRequests?: PayoutRequest[];
  isSidebarCollapsed?: boolean;
  onSelectTrip: (tripId: string) => void;
  onNavigateTab: (tab: any, targetId?: string) => void;
}

type DensityMode = 'auto' | 'dense' | 'comfortable';

export const DashboardOverview: React.FC<DashboardOverviewProps> = ({
  trips,
  drivers,
  riders,
  tickets,
  feed,
  surgeZones = [],
  safetyReports = [],
  payoutRequests = [],
  isSidebarCollapsed = false,
  onSelectTrip,
  onNavigateTab,
}) => {
  // Manual density override option; defaults to 'auto' (synced with sidebar)
  const [densityPreference, setDensityPreference] = useState<DensityMode>('auto');
  // Chart metric view filter
  const [chartMetric, setChartMetric] = useState<'all' | 'revenue' | 'trips'>('all');

  // Effective density: if auto, true when sidebar is collapsed (more horizontal room)
  const isDense = useMemo(() => {
    if (densityPreference === 'dense') return true;
    if (densityPreference === 'comfortable') return false;
    return isSidebarCollapsed;
  }, [densityPreference, isSidebarCollapsed]);

  // Performance-optimized memoized stats calculations
  const activeTrips = useMemo(
    () =>
      trips.filter(
        (t) =>
          t.status === 'in_progress' ||
          t.status === 'driver_arriving' ||
          t.status === 'matching' ||
          t.status === 'accepted'
      ),
    [trips]
  );

  const onlineDrivers = useMemo(
    () =>
      drivers.filter(
        (d) => d.status === 'online' || d.status === 'on_trip' || d.accountStatus === 'ONLINE'
      ),
    [drivers]
  );

  const activeSos = useMemo(
    () => [
      ...trips.filter(
        (t) => t.status === 'sos_alert' || (t.sosAlert && t.sosAlert.isTriggered && !t.sosAlert.resolved)
      ),
      ...safetyReports.filter(
        (r) => r.status === 'pending' || r.status === 'investigating' || r.status === 'active'
      ),
    ],
    [trips, safetyReports]
  );

  const totalRevenueToday = useMemo(
    () =>
      trips
        .filter((t) => t.status === 'completed')
        .reduce((acc, t) => acc + (t.fare?.total || 0), 0),
    [trips]
  );

  const driversMatchedCount = useMemo(
    () =>
      drivers.filter(
        (d) =>
          d.status === 'on_trip' ||
          d.currentTripId ||
          trips.some(
            (t) => t.driverId === d.id && (t.status === 'in_progress' || t.status === 'driver_arriving')
          )
      ).length,
    [drivers, trips]
  );

  const pendingKycCount = useMemo(
    () =>
      drivers.filter(
        (d) =>
          d.status === 'pending_verification' ||
          d.verificationStatus === 'PENDING' ||
          d.documents?.some((doc) => doc.status === 'pending')
      ).length,
    [drivers]
  );

  const activeSurgeCount = useMemo(
    () => surgeZones.filter((z) => z.status === 'active' || (z.surgeMultiplier ?? 1) > 1).length,
    [surgeZones]
  );

  const pendingPayouts = useMemo(
    () => payoutRequests.filter((p) => p.status === 'pending'),
    [payoutRequests]
  );

  const totalPendingPayoutAmount = useMemo(
    () => pendingPayouts.reduce((sum, p) => sum + p.amount, 0),
    [pendingPayouts]
  );

  return (
    <div
      className={`flex-1 overflow-y-auto transition-all duration-300 ease-in-out ${
        isDense ? 'p-4 sm:p-5 space-y-4' : 'p-5 sm:p-6 space-y-5'
      }`}
    >
      {/* Density & Fleet Health Bar */}
      <div className="bg-white rounded-xl border border-slate-200 px-4 py-2.5 shadow-xs flex flex-wrap items-center justify-between gap-3 shrink-0">
        <div className="flex items-center space-x-3 min-w-0">
          <div className="flex items-center space-x-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse shrink-0" />
            <span className="text-xs font-bold text-slate-800 tracking-tight whitespace-nowrap">
              Fleet Engine Online
            </span>
          </div>
          <span className="hidden sm:inline-block text-slate-300">|</span>
          <div className="hidden sm:flex items-center space-x-1.5 text-xs text-slate-500">
            <Radio className="w-3.5 h-3.5 text-blue-500 animate-pulse shrink-0" />
            <span className="truncate">Lahore Metro Grid: 14 Active Sectors</span>
          </div>
        </div>

        <div className="flex items-center space-x-2 shrink-0">
          <div className="flex items-center space-x-1 bg-slate-100 p-0.5 rounded-lg border border-slate-200 text-[11px]">
            <span className="px-2 py-0.5 text-slate-500 font-medium flex items-center space-x-1">
              <Layers className="w-3 h-3" />
              <span className="hidden md:inline">Density:</span>
            </span>
            <button
              onClick={() => setDensityPreference('auto')}
              className={`px-2 py-0.5 rounded-md font-semibold transition-colors ${
                densityPreference === 'auto'
                  ? 'bg-white text-blue-700 shadow-xs font-bold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
              title="Automatically adjusts data density to sidebar collapsed state"
            >
              Auto {isSidebarCollapsed ? '(Dense)' : '(Standard)'}
            </button>
            <button
              onClick={() => setDensityPreference('dense')}
              className={`px-2 py-0.5 rounded-md font-semibold transition-colors ${
                densityPreference === 'dense'
                  ? 'bg-white text-blue-700 shadow-xs font-bold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
              title="Force high-density layout"
            >
              Dense
            </button>
            <button
              onClick={() => setDensityPreference('comfortable')}
              className={`px-2 py-0.5 rounded-md font-semibold transition-colors ${
                densityPreference === 'comfortable'
                  ? 'bg-white text-blue-700 shadow-xs font-bold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
              title="Force standard comfortable layout"
            >
              Comfortable
            </button>
          </div>
        </div>
      </div>

      {/* Dynamic Metric Cards Grid (Zero-shift fixed height) */}
      <div
        className={`grid transition-all duration-300 ease-in-out ${
          isDense
            ? 'grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-5 gap-3.5'
            : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4'
        }`}
      >
        {/* Card 1: Active Rides */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between h-[148px] overflow-hidden">
          <div className="flex items-center justify-between shrink-0">
            <p className="text-[11px] font-bold text-slate-500 uppercase tracking-wider truncate">
              Active Rides Now
            </p>
            <div className="w-8 h-8 bg-blue-50 text-blue-600 rounded-lg flex items-center justify-center font-bold shrink-0">
              <Car className="w-4 h-4" />
            </div>
          </div>
          <div className="my-auto">
            <h3 className="text-2xl font-extrabold text-slate-900 font-mono tracking-tight">
              {activeTrips.length}
            </h3>
            <div className="flex items-center space-x-1 mt-1 text-[11px] text-emerald-600 font-bold whitespace-nowrap">
              <TrendingUp className="w-3 h-3 shrink-0" />
              <span>+18.4%</span>
              <span className="text-slate-400 font-normal">vs morning peak</span>
            </div>
          </div>
          <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500 shrink-0">
            <span className="truncate">Matched: {driversMatchedCount}</span>
            <button
              onClick={() => onNavigateTab('dispatch')}
              className="text-blue-600 hover:text-blue-700 font-bold flex items-center space-x-0.5 shrink-0 ml-1"
            >
              <span>Dispatch</span>
              <ChevronRight className="w-3 h-3" />
            </button>
          </div>
        </div>

        {/* Card 2: Online Fleet Drivers */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between h-[148px] overflow-hidden">
          <div className="flex items-center justify-between shrink-0">
            <p className="text-[11px] font-bold text-slate-500 uppercase tracking-wider truncate">
              Online Fleet
            </p>
            <div className="w-8 h-8 bg-emerald-50 text-emerald-600 rounded-lg flex items-center justify-center font-bold shrink-0">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="my-auto">
            <h3 className="text-2xl font-extrabold text-slate-900 font-mono tracking-tight">
              {onlineDrivers.length}
            </h3>
            <div className="flex items-center space-x-1 mt-1 text-[11px] text-emerald-600 font-bold whitespace-nowrap">
              <ArrowUpRight className="w-3 h-3 shrink-0" />
              <span>94% Acceptance</span>
            </div>
          </div>
          <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500 shrink-0">
            <span className="truncate">Pending KYC: {pendingKycCount}</span>
            <button
              onClick={() => onNavigateTab('users')}
              className="text-blue-600 hover:text-blue-700 font-bold flex items-center space-x-0.5 shrink-0 ml-1"
            >
              <span>Review</span>
              <ChevronRight className="w-3 h-3" />
            </button>
          </div>
        </div>

        {/* Card 3: Gross Ride Revenue */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between h-[148px] overflow-hidden">
          <div className="flex items-center justify-between shrink-0">
            <p className="text-[11px] font-bold text-slate-500 uppercase tracking-wider truncate">
              Gross Ride GMV
            </p>
            <div className="w-8 h-8 bg-amber-50 text-amber-600 rounded-lg flex items-center justify-center font-bold shrink-0">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <div className="my-auto">
            <h3 className="text-2xl font-extrabold text-slate-900 font-mono tracking-tight truncate">
              Rs. {(totalRevenueToday ?? 0).toLocaleString('en-PK', { maximumFractionDigits: 0 })}
            </h3>
            <div className="flex items-center space-x-1 mt-1 text-[11px] text-emerald-600 font-bold whitespace-nowrap">
              <TrendingUp className="w-3 h-3 shrink-0" />
              <span>18% Drigo Margin</span>
            </div>
          </div>
          <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500 shrink-0">
            <span className="truncate">Surge Zones: {activeSurgeCount}</span>
            <button
              onClick={() => onNavigateTab('pricing')}
              className="text-blue-600 hover:text-blue-700 font-bold flex items-center space-x-0.5 shrink-0 ml-1"
            >
              <span>Fares</span>
              <ChevronRight className="w-3 h-3" />
            </button>
          </div>
        </div>

        {/* Card 4: Driver Payout Settlements (Featured when dense or integrated) */}
        {isDense ? (
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between h-[148px] overflow-hidden">
            <div className="flex items-center justify-between shrink-0">
              <p className="text-[11px] font-bold text-slate-500 uppercase tracking-wider truncate">
                Payout Requests
              </p>
              <div className="w-8 h-8 bg-indigo-50 text-indigo-600 rounded-lg flex items-center justify-center font-bold shrink-0">
                <CreditCard className="w-4 h-4" />
              </div>
            </div>
            <div className="my-auto">
              <h3 className="text-2xl font-extrabold text-slate-900 font-mono tracking-tight">
                {pendingPayouts.length}{' '}
                <span className="text-xs font-semibold text-slate-400">
                  (Rs. {(totalPendingPayoutAmount ?? 0).toLocaleString()})
                </span>
              </h3>
              <div className="flex items-center space-x-1 mt-1 text-[11px] text-indigo-600 font-bold whitespace-nowrap">
                <CheckCircle className="w-3 h-3 shrink-0" />
                <span>JazzCash & EasyPaisa</span>
              </div>
            </div>
            <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500 shrink-0">
              <span className="truncate">Auto-audited</span>
              <button
                onClick={() => onNavigateTab('finance')}
                className="text-blue-600 hover:text-blue-700 font-bold flex items-center space-x-0.5 shrink-0 ml-1"
              >
                <span>Settle</span>
                <ChevronRight className="w-3 h-3" />
              </button>
            </div>
          </div>
        ) : null}

        {/* Card 5: Safety Desk & SOS Alert Incident Status */}
        <div
          className={`p-4 rounded-2xl border shadow-xs transition-all duration-200 flex flex-col justify-between h-[148px] overflow-hidden ${
            activeSos.length > 0
              ? 'bg-rose-50/90 border-rose-300 ring-2 ring-rose-500/20'
              : 'bg-white border-slate-200 hover:shadow-md'
          }`}
        >
          <div className="flex items-center justify-between shrink-0">
            <p
              className={`text-[11px] font-bold uppercase tracking-wider truncate ${
                activeSos.length > 0 ? 'text-rose-700 font-bold' : 'text-slate-500'
              }`}
            >
              Safety Desk & SOS
            </p>
            <div
              className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold shrink-0 ${
                activeSos.length > 0
                  ? 'bg-rose-600 text-white animate-pulse'
                  : 'bg-slate-100 text-slate-600'
              }`}
            >
              <ShieldAlert className="w-4 h-4" />
            </div>
          </div>
          <div className="my-auto">
            <h3
              className={`text-2xl font-extrabold font-mono tracking-tight ${
                activeSos.length > 0 ? 'text-rose-900' : 'text-slate-900'
              }`}
            >
              {activeSos.length > 0 ? `${activeSos.length} ALERTS` : '0 Alerts'}
            </h3>
            <p
              className={`text-[11px] truncate mt-1 ${
                activeSos.length > 0 ? 'text-rose-700 font-bold' : 'text-slate-500'
              }`}
            >
              {activeSos.length > 0 ? 'Urgent route anomaly' : 'All ride protocols nominal'}
            </p>
          </div>
          <div className="pt-2 border-t border-slate-200/60 flex items-center justify-between text-[11px] shrink-0">
            <span className="text-slate-500 truncate">Telemetry: 99.8% OK</span>
            <button
              onClick={() => onNavigateTab('safety')}
              className={`font-bold flex items-center space-x-0.5 shrink-0 ml-1 ${
                activeSos.length > 0 ? 'text-rose-700 hover:text-rose-800' : 'text-blue-600 hover:text-blue-700'
              }`}
            >
              <span>Desk</span>
              <ChevronRight className="w-3 h-3" />
            </button>
          </div>
        </div>
      </div>

      {/* Main Charts & Activity Row (12-Column Responsive Layout) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-stretch">
        {/* Hourly Traffic & Revenue Trend Chart (8 cols when dense, 7-8 cols standard) */}
        <div
          className={`bg-white rounded-2xl border border-slate-200 p-5 shadow-xs flex flex-col justify-between h-[370px] transition-all duration-300 ${
            isDense ? 'lg:col-span-8' : 'lg:col-span-7 xl:col-span-8'
          }`}
        >
          <div className="flex flex-wrap items-center justify-between gap-2 shrink-0 mb-2">
            <div>
              <div className="flex items-center space-x-2">
                <h4 className="text-sm font-bold text-slate-800 tracking-tight">
                  Real-time Ride Demand & Revenue
                </h4>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                  Today
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Hourly gross booking revenue (PKR) and completed trip count
              </p>
            </div>

            <div className="flex items-center space-x-1.5 bg-slate-100 p-1 rounded-lg text-xs">
              <button
                onClick={() => setChartMetric('all')}
                className={`px-2 py-1 rounded-md font-semibold transition-colors ${
                  chartMetric === 'all'
                    ? 'bg-white text-blue-700 shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                All
              </button>
              <button
                onClick={() => setChartMetric('revenue')}
                className={`px-2 py-1 rounded-md font-semibold transition-colors ${
                  chartMetric === 'revenue'
                    ? 'bg-white text-blue-700 shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Revenue
              </button>
              <button
                onClick={() => setChartMetric('trips')}
                className={`px-2 py-1 rounded-md font-semibold transition-colors ${
                  chartMetric === 'trips'
                    ? 'bg-white text-blue-700 shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Rides
              </button>
            </div>
          </div>

          {/* Zero-shift fixed height chart container */}
          <div className="w-full h-[270px] min-w-0">
            <ResponsiveContainer width="100%" height="100%" debounce={50}>
              <AreaChart data={REVENUE_HOURLY_DATA} margin={{ top: 10, right: 10, left: -15, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorGmv" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#2563EB" stopOpacity={0.25} />
                    <stop offset="95%" stopColor="#2563EB" stopOpacity={0.0} />
                  </linearGradient>
                  <linearGradient id="colorTrips" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10B981" stopOpacity={0.25} />
                    <stop offset="95%" stopColor="#10B981" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                <XAxis dataKey="hour" tick={{ fontSize: 11, fill: '#64748B' }} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#64748B' }} tickLine={false} axisLine={false} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0F172A',
                    borderColor: '#334155',
                    borderRadius: '12px',
                    color: '#F8FAFC',
                    fontSize: '12px',
                    boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.2)',
                  }}
                  itemStyle={{ color: '#F8FAFC' }}
                  formatter={(value: any, name: any) => [
                    name === 'Gross Revenue (Rs.)' ? `Rs. ${(Number(value) || 0).toLocaleString()}` : (value ?? ''),
                    name,
                  ]}
                />
                {(chartMetric === 'all' || chartMetric === 'revenue') && (
                  <Area
                    type="monotone"
                    dataKey="gmv"
                    name="Gross Revenue (Rs.)"
                    stroke="#2563EB"
                    strokeWidth={2.5}
                    fillOpacity={1}
                    fill="url(#colorGmv)"
                  />
                )}
                {(chartMetric === 'all' || chartMetric === 'trips') && (
                  <Area
                    type="monotone"
                    dataKey="trips"
                    name="Completed Rides"
                    stroke="#10B981"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#colorTrips)"
                  />
                )}
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Live Fleet Activity Feed Stream (4 cols when dense, 4-5 cols standard) */}
        <div
          className={`bg-white rounded-2xl border border-slate-200 p-5 shadow-xs flex flex-col justify-between h-[370px] transition-all duration-300 ${
            isDense ? 'lg:col-span-4' : 'lg:col-span-5 xl:col-span-4'
          }`}
        >
          <div className="flex items-center justify-between mb-3 shrink-0">
            <div className="flex items-center space-x-2">
              <Activity className="w-4 h-4 text-blue-600 animate-pulse" />
              <h4 className="text-sm font-bold text-slate-800">Live Activity Feed</h4>
            </div>
            <span className="text-[10px] bg-emerald-50 text-emerald-700 font-bold px-2 py-0.5 rounded-full border border-emerald-200 flex items-center space-x-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping" />
              <span>Real-time</span>
            </span>
          </div>

          <div className="space-y-2.5 flex-1 overflow-y-auto pr-1">
            {feed.map((item) => (
              <div
                key={item.id}
                className="p-2.5 rounded-xl bg-slate-50 border border-slate-100 hover:bg-blue-50/50 hover:border-blue-200 transition-all cursor-pointer flex items-start space-x-3"
                onClick={() => {
                  if (item.tripId) onSelectTrip(item.tripId);
                }}
              >
                <div
                  className={`p-1.5 rounded-lg shrink-0 mt-0.5 ${
                    item.severity === 'critical'
                      ? 'bg-rose-100 text-rose-600'
                      : item.severity === 'warning'
                      ? 'bg-amber-100 text-amber-600'
                      : item.severity === 'success'
                      ? 'bg-emerald-100 text-emerald-600'
                      : 'bg-blue-100 text-blue-600'
                  }`}
                >
                  {item.type === 'sos_alert' ? (
                    <AlertTriangle className="w-3.5 h-3.5" />
                  ) : item.type === 'trip_completed' ? (
                    <CheckCircle className="w-3.5 h-3.5" />
                  ) : (
                    <Car className="w-3.5 h-3.5" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-bold text-slate-800 truncate">{item.title}</p>
                    <span className="text-[10px] text-slate-400 font-mono shrink-0 ml-1">
                      {item.timestamp}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-600 line-clamp-1 mt-0.5">{item.description}</p>
                  {/* High density micro-chips */}
                  {isDense && (
                    <div className="flex items-center space-x-2 mt-1 text-[9px] text-slate-400 font-medium">
                      <span className="bg-slate-200/70 px-1.5 py-0.2 rounded text-slate-600">
                        LTE 84ms
                      </span>
                      <span>•</span>
                      <span>Verified GPS</span>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom Section: Active Trips Highlights + Android Low-RAM Hardware Diagnostics */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-stretch">
        {/* Active Trips In-Flight Panel */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs flex flex-col justify-between min-h-[355px] lg:col-span-6 transition-all duration-300">
          <div>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <Car className="w-4 h-4 text-blue-600" />
                <h4 className="text-sm font-bold text-slate-800">Active Drigo Trips In-Flight</h4>
              </div>
              <button
                onClick={() => onNavigateTab('dispatch')}
                className="text-xs text-blue-600 hover:text-blue-700 font-bold flex items-center space-x-0.5"
              >
                <span>Dispatch Map</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="space-y-2.5">
              {trips.slice(0, 3).map((trip) => (
                <div
                  key={trip.id}
                  onClick={() => {
                    onSelectTrip(trip.id);
                    onNavigateTab('dispatch');
                  }}
                  className="p-3 bg-slate-50 border border-slate-200/80 rounded-xl hover:border-blue-300 hover:bg-blue-50/20 transition-all cursor-pointer flex flex-col space-y-2"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2 min-w-0">
                      <span className="font-mono text-xs font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded border border-blue-200 shrink-0">
                        {trip.tripCode}
                      </span>
                      <span className="text-xs font-bold text-slate-800 truncate">
                        {trip.passengerName}
                      </span>
                      <span className="text-[11px] text-slate-500 truncate hidden sm:inline">
                        • {trip.driverName || 'Matching Driver'}
                      </span>
                    </div>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase shrink-0 whitespace-nowrap ${
                        trip.status === 'in_progress'
                          ? 'bg-emerald-100 text-emerald-800'
                          : trip.status === 'sos_alert'
                          ? 'bg-rose-100 text-rose-800 animate-pulse'
                          : 'bg-blue-100 text-blue-800'
                      }`}
                    >
                      {String(trip.status || '').replace('_', ' ')}
                    </span>
                  </div>

                  {/* Dual pickup & destination route preview */}
                  <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-600 bg-white p-2 rounded-lg border border-slate-100">
                    <div className="truncate">
                      <span className="text-emerald-600 font-bold">FROM: </span>
                      <span className="text-slate-700 font-medium">{trip.fromLocation.name}</span>
                    </div>
                    <div className="truncate">
                      <span className="text-rose-600 font-bold">TO: </span>
                      <span className="text-slate-700 font-medium">{trip.toLocation.name}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Real-time GPS Polling: 2-3s interval</span>
            <span className="text-emerald-600 font-semibold flex items-center space-x-1">
              <CheckCircle className="w-3.5 h-3.5" />
              <span>Polyline Smooth</span>
            </span>
          </div>
        </div>

        {/* Android Device & Low-RAM Hardware Compatibility Panel */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs flex flex-col justify-between min-h-[355px] lg:col-span-6 transition-all duration-300">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center space-x-2">
                <Smartphone className="w-4 h-4 text-indigo-600" />
                <h4 className="text-sm font-bold text-slate-800">Android Hardware & Compatibility</h4>
              </div>
              <span className="text-[10px] bg-indigo-50 text-indigo-700 font-bold px-2 py-0.5 rounded-full border border-indigo-200">
                minSdk 23 (Android 6.0+)
              </span>
            </div>

            <p className="text-xs text-slate-500 mb-3">
              Driver & Passenger apps are engineered for mass-market 2GB/3GB phones (Samsung A12, Tecno, Redmi) with offline packet recovery.
            </p>

            {/* Dynamic RAM Tier Grid: 4-cols when dense, 2x2 when standard */}
            <div
              className={`grid transition-all duration-300 ${
                isDense ? 'grid-cols-2 md:grid-cols-4 gap-2.5' : 'grid-cols-2 gap-3'
              }`}
            >
              {HARDWARE_DEVICE_STATS.ramTiers.map((tier) => (
                <div
                  key={tier.name}
                  className="p-2.5 bg-slate-50 rounded-xl border border-slate-200/80 hover:bg-slate-100/60 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-700 truncate">
                      {tier.name.split(' ')[0]} {tier.name.split(' ')[1]}
                    </span>
                    <span className="text-xs font-mono font-extrabold text-blue-600">
                      {tier.percentage}%
                    </span>
                  </div>
                  <div className="w-full bg-slate-200 h-1.5 rounded-full mt-2 overflow-hidden">
                    <div
                      className="bg-blue-600 h-full rounded-full transition-all duration-500"
                      style={{ width: `${tier.percentage}%` }}
                    />
                  </div>
                  <div className="flex items-center justify-between mt-1.5 text-[10px] text-slate-500">
                    <span>{tier.count} devices</span>
                    <span className="font-mono text-emerald-600 font-semibold">{tier.avgFps} FPS</span>
                  </div>
                </div>
              ))}
            </div>

            {/* Micro-telemetry chips in dense view */}
            {isDense && (
              <div className="mt-3 pt-2.5 border-t border-slate-100 grid grid-cols-3 gap-2 text-center text-[10px]">
                <div className="bg-slate-50 p-1.5 rounded-lg border border-slate-200/60">
                  <span className="text-slate-400 block">Samsung A12</span>
                  <span className="font-bold text-slate-700">742 Active</span>
                </div>
                <div className="bg-slate-50 p-1.5 rounded-lg border border-slate-200/60">
                  <span className="text-slate-400 block">Redmi 9A/9C</span>
                  <span className="font-bold text-slate-700">688 Active</span>
                </div>
                <div className="bg-slate-50 p-1.5 rounded-lg border border-slate-200/60">
                  <span className="text-slate-400 block">Infinix Hot 10</span>
                  <span className="font-bold text-slate-700">612 Active</span>
                </div>
              </div>
            )}
          </div>

          <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-xs">
            <span className="text-slate-500">Target SDK: 36 (Android 16 Ready)</span>
            <span className="text-emerald-600 font-bold flex items-center space-x-1">
              <CheckCircle className="w-3.5 h-3.5" />
              <span>Offline Packet Recovery 99.4%</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
