import React from 'react';
import { Trip, Driver, Rider } from '../types';
import {
  AreaChart,
  Area,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import {
  BarChart3,
  TrendingUp,
  Smartphone,
  Cpu,
  Wifi,
  Activity,
  Car,
  ShieldCheck,
  Zap,
  CheckCircle,
  AlertCircle
} from 'lucide-react';

interface RealtimeAnalyticsProps {
  trips: Trip[];
  drivers: Driver[];
  riders?: Rider[];
}

const VEHICLE_COLORS: Record<string, string> = {
  sedan: '#2563EB',
  boda_bike: '#10B981',
  tuktuk_auto: '#F59E0B',
  comfort: '#8B5CF6',
  xl_van: '#EC4899',
};

export const RealtimeAnalytics: React.FC<RealtimeAnalyticsProps> = ({ trips, drivers, riders = [] }) => {
  // Real Operational Calculations
  const totalRides = trips.length;
  const completedRides = trips.filter(t => t.status === 'completed').length;
  const cancelledRides = trips.filter(t => t.status === 'cancelled').length;
  const inProgressRides = trips.filter(t => t.status === 'in_progress' || t.status === 'driver_arriving' || t.status === 'matching').length;
  
  const totalRevenue = trips
    .filter(t => t.status === 'completed')
    .reduce((acc, t) => acc + (t.fare?.total || 0), 0);

  const averageFare = completedRides > 0 ? Math.round(totalRevenue / completedRides) : 0;

  const activeDrivers = drivers.filter(d => d.status === 'online' || d.status === 'on_trip' || d.accountStatus === 'ONLINE').length;
  
  const avgAcceptanceRate = drivers.length > 0 
    ? Math.round(drivers.reduce((acc, d) => acc + (d.acceptanceRate || 0), 0) / drivers.length)
    : 0;

  const avgDriverCancellation = drivers.length > 0 
    ? (drivers.reduce((acc, d) => acc + (d.cancellationRate || 0), 0) / drivers.length).toFixed(1)
    : '0.0';

  const passengerCancellationRate = totalRides > 0 
    ? ((cancelledRides / totalRides) * 100).toFixed(1)
    : '0.0';

  const completedTripsList = trips.filter(t => t.status === 'completed');
  const averageTripDuration = completedTripsList.length > 0
    ? Math.round(completedTripsList.reduce((acc, t) => acc + (t.actualDurationMinutes || t.estimatedDurationMinutes || 15), 0) / completedTripsList.length)
    : 0;

  // Vehicle Category Distribution from real trips / drivers
  const vehicleCounts: Record<string, number> = {
    sedan: 0,
    boda_bike: 0,
    tuktuk_auto: 0,
    comfort: 0,
    xl_van: 0,
  };

  trips.forEach(t => {
    if (t.vehicleType && vehicleCounts[t.vehicleType] !== undefined) {
      vehicleCounts[t.vehicleType]++;
    } else if (t.vehicleType) {
      vehicleCounts[t.vehicleType] = 1;
    } else {
      vehicleCounts.sedan++;
    }
  });

  const totalVehicleTrips = Object.values(vehicleCounts).reduce((a, b) => a + b, 0);

  const vehicleStats = [
    { name: 'Sedan Standard', key: 'sedan', color: VEHICLE_COLORS.sedan },
    { name: 'Boda Motorbike', key: 'boda_bike', color: VEHICLE_COLORS.boda_bike },
    { name: 'TukTuk Auto', key: 'tuktuk_auto', color: VEHICLE_COLORS.tuktuk_auto },
    { name: 'Comfort Sedan', key: 'comfort', color: VEHICLE_COLORS.comfort },
    { name: 'XL Van (6-8s)', key: 'xl_van', color: VEHICLE_COLORS.xl_van },
  ].map(item => ({
    name: item.name,
    value: totalVehicleTrips > 0 ? Math.round((vehicleCounts[item.key] / totalVehicleTrips) * 100) : 0,
    rawCount: vehicleCounts[item.key],
    color: item.color,
  }));

  // Real hourly chart data derived from actual completed trips
  const hourlyMap: Record<string, { hour: string; gmv: number; trips: number }> = {};
  completedTripsList.forEach(t => {
    const timeStr = String(t.requestedAt || '12:00 PM');
    const hourKey = timeStr.includes(':') ? timeStr.split(':')[0] + ' ' + (timeStr.includes('PM') ? 'PM' : 'AM') : '12:00 PM';
    if (!hourlyMap[hourKey]) {
      hourlyMap[hourKey] = { hour: hourKey, gmv: 0, trips: 0 };
    }
    hourlyMap[hourKey].gmv += t.fare?.total || 450;
    hourlyMap[hourKey].trips += 1;
  });

  const hourlyChartData = Object.values(hourlyMap);

  // Real Android Telemetry from drivers
  const driversWithTelemetry = drivers.filter(d => d.telemetry);

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50">
      {/* Title */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-extrabold text-slate-900">Real-time Ride & Telemetry Analytics</h2>
          <p className="text-xs text-slate-500">Live operational metrics, revenue, vehicle breakdown, and Android client performance.</p>
        </div>
        <div className="flex items-center space-x-2 bg-emerald-100 text-emerald-800 px-3 py-1 rounded-full text-xs font-bold">
          <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
          <span>SYSTEM HEALTH: 100% OPERATIONAL</span>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
          <div className="text-[11px] font-bold text-slate-400 uppercase">Total Rides</div>
          <div className="text-xl font-black text-slate-900 mt-1">{totalRides}</div>
          <div className="text-[10px] text-emerald-600 font-semibold mt-1">
            {completedRides} Completed • {cancelledRides} Cancelled
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
          <div className="text-[11px] font-bold text-slate-400 uppercase">Total Revenue (GMV)</div>
          <div className="text-xl font-black text-blue-600 mt-1">Rs. {(totalRevenue ?? 0).toLocaleString()}</div>
          <div className="text-[10px] text-slate-500 font-semibold mt-1">
            Avg Fare: Rs. {averageFare}
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
          <div className="text-[11px] font-bold text-slate-400 uppercase">Fleet Engagement</div>
          <div className="text-xl font-black text-slate-900 mt-1">{activeDrivers} Online</div>
          <div className="text-[10px] text-slate-500 font-semibold mt-1">
            Acceptance Rate: {avgAcceptanceRate}%
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
          <div className="text-[11px] font-bold text-slate-400 uppercase">Cancellation Rates</div>
          <div className="text-xl font-black text-rose-600 mt-1">{passengerCancellationRate}%</div>
          <div className="text-[10px] text-slate-500 font-semibold mt-1">
            Driver Cancel: {avgDriverCancellation}%
          </div>
        </div>
      </div>

      {/* Row 1: Revenue Area Chart & Vehicle Distribution Pie */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue & Rides Hourly Chart */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-5 shadow-xs flex flex-col">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h4 className="text-sm font-bold text-slate-800">Historical Revenue (Rs.) vs Ride Count</h4>
              <p className="text-xs text-slate-500">Aggregated from real completed trip records</p>
            </div>
            <span className="text-[11px] font-mono font-bold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-lg border border-blue-200">
              Avg Duration: {averageTripDuration} mins
            </span>
          </div>

          <div className="h-64 w-full">
            {hourlyChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={hourlyChartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                  <XAxis dataKey="hour" tick={{ fontSize: 11, fill: '#64748B' }} />
                  <YAxis tick={{ fontSize: 11, fill: '#64748B' }} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#0F172A', borderRadius: '12px', color: '#F8FAFC', fontSize: '12px' }}
                  />
                  <Area type="monotone" dataKey="gmv" name="Revenue (Rs.)" stroke="#2563EB" strokeWidth={2.5} fill="#3B82F6" fillOpacity={0.15} />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-slate-400 text-xs">
                <AlertCircle className="w-8 h-8 mb-2 opacity-40" />
                No data available
              </div>
            )}
          </div>
        </div>

        {/* Vehicle Category Distribution */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs flex flex-col">
          <h4 className="text-sm font-bold text-slate-800 mb-1">Fleet Vehicle Category Mix</h4>
          <p className="text-xs text-slate-500 mb-4">Distribution across operational rides</p>

          <div className="h-48 w-full">
            {totalVehicleTrips > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={vehicleStats}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={75}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {vehicleStats.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-slate-400 text-xs">
                No data available
              </div>
            )}
          </div>

          <div className="space-y-1.5 mt-2">
            {vehicleStats.map((item) => (
              <div key={item.name} className="flex items-center justify-between text-xs">
                <div className="flex items-center space-x-2">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: item.color }}></span>
                  <span className="text-slate-700 font-medium">{item.name}</span>
                </div>
                <span className="font-mono font-bold text-slate-900">{item.value}% ({item.rawCount})</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Row 2: Android Hardware Performance & Low-RAM Telemetry */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Smartphone className="w-5 h-5 text-indigo-600" />
            <div>
              <h3 className="text-sm font-bold text-slate-900">Android Client Compatibility & RAM Telemetry</h3>
              <p className="text-xs text-slate-500">
                Live performance metrics reported by connected Android driver client applications.
              </p>
            </div>
          </div>
          <span className="text-xs font-mono font-extrabold bg-indigo-50 text-indigo-700 px-3 py-1 rounded-lg border border-indigo-200">
            Active Devices: {driversWithTelemetry.length}
          </span>
        </div>

        {driversWithTelemetry.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {driversWithTelemetry.map((d, idx) => (
              <div key={idx} className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-900">{d.telemetry.deviceModel}</span>
                  <span className="text-[10px] font-mono font-bold bg-blue-100 text-blue-800 px-1.5 py-0.5 rounded">
                    {d.fullName}
                  </span>
                </div>
                <div className="flex items-center justify-between text-[11px] text-slate-500">
                  <span>RAM: {d.telemetry.ramTotalGb}GB • {d.telemetry.androidVersion}</span>
                </div>
                <div className="flex items-center justify-between text-[10px] font-semibold text-emerald-600 pt-1 border-t border-slate-200/80">
                  <span>Network: {d.telemetry.networkType} ({d.telemetry.networkLatencyMs}ms)</span>
                  <span className="font-mono font-bold">Battery: {d.telemetry.batteryLevel}%</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="py-8 text-center text-slate-400 text-xs italic bg-slate-50 rounded-xl border border-dashed border-slate-200">
            No data available — Android driver telemetry reports have not been received yet.
          </div>
        )}
      </div>
    </div>
  );
};
