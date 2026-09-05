import React, { useState } from 'react';
import { Driver } from '../../types';
import {
  Smartphone,
  Cpu,
  Battery,
  Wifi,
  Activity,
  AlertTriangle,
  CheckCircle2,
  ShieldCheck,
  Search,
  Filter,
  Sparkles,
  RefreshCw
} from 'lucide-react';

interface AndroidTelemetryViewProps {
  drivers: Driver[];
  onInspectDriver: (driver: Driver) => void;
}

export const AndroidTelemetryView: React.FC<AndroidTelemetryViewProps> = ({
  drivers,
  onInspectDriver,
}) => {
  const [filterBrand, setFilterBrand] = useState<string>('all');
  const [filterHealth, setFilterHealth] = useState<'all' | 'low_ram' | 'low_battery' | 'high_latency'>('all');
  const [searchDevice, setSearchDevice] = useState<string>('');

  const driversWithTelemetry = drivers.map((d) => ({
    driver: d,
    telemetry: d.telemetry || {
      deviceModel: 'Samsung Galaxy A12',
      manufacturer: 'Samsung',
      androidVersion: 'Android 11 (API 30)',
      apiLevel: 30,
      ramTotalGb: 3,
      ramUsagePercent: 68,
      batteryLevel: 65,
      isBatterySaver: false,
      appVersion: 'v2.4.1',
      networkType: '4G',
      networkLatencyMs: 45,
      gpsAccuracyMeters: 4.2,
      offlineQueuedPackets: 0,
      lastPingAt: 'Just now',
    },
  }));

  // Filtering
  const filtered = driversWithTelemetry.filter(({ driver, telemetry }) => {
    if (filterBrand !== 'all') {
      const mfg = (telemetry.manufacturer || '').toLowerCase();
      if (!mfg.includes(filterBrand.toLowerCase())) return false;
    }

    if (filterHealth === 'low_ram' && telemetry.ramTotalGb > 3) return false;
    if (filterHealth === 'low_battery' && telemetry.batteryLevel > 25) return false;
    if (filterHealth === 'high_latency' && telemetry.networkLatencyMs < 120) return false;

    if (searchDevice) {
      const q = searchDevice.toLowerCase();
      const matchName = driver.fullName.toLowerCase().includes(q);
      const matchDevice = telemetry.deviceModel.toLowerCase().includes(q);
      const matchPlate = (driver.vehicle?.licensePlate || '').toLowerCase().includes(q);
      if (!matchName && !matchDevice && !matchPlate) return false;
    }

    return true;
  });

  // Calculate aggregates
  const totalTracked = driversWithTelemetry.length;
  const avgLatency = Math.round(
    driversWithTelemetry.reduce((acc, curr) => acc + curr.telemetry.networkLatencyMs, 0) / (totalTracked || 1)
  );
  const lowRamDevices = driversWithTelemetry.filter(d => d.telemetry.ramTotalGb <= 3).length;
  const batteryHealthAlerts = driversWithTelemetry.filter(d => d.telemetry.batteryLevel <= 20).length;

  return (
    <div className="space-y-4">
      {/* Target Compatibility Benchmark Card */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 text-white p-5 rounded-3xl border border-slate-700/80 shadow-md space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-blue-500/20 text-blue-400 rounded-2xl border border-blue-400/30">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-black text-sm text-white flex items-center gap-2">
                Android Device Compatibility & Low-End Telemetry
                <span className="text-[10px] bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 px-2 py-0.5 rounded-full font-mono">
                  minSdk 23 • targetSdk 36
                </span>
              </h3>
              <p className="text-xs text-slate-300 mt-0.5">
                Real-time performance diagnostics tuned for budget phones (2GB–4GB RAM, Samsung A12, Redmi, Infinix, Tecno)
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2 text-xs">
            <span className="px-2.5 py-1 bg-slate-800/80 rounded-xl border border-slate-700 text-slate-300 font-mono text-[11px]">
              {totalTracked} Active Connected Terminals
            </span>
          </div>
        </div>

        {/* Telemetry Metric Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-1 text-xs">
          <div className="bg-slate-800/60 p-3 rounded-2xl border border-slate-700/70 space-y-1">
            <div className="text-slate-400 text-[11px] flex items-center justify-between">
              <span>Avg Latency</span>
              <Wifi className="w-3.5 h-3.5 text-blue-400" />
            </div>
            <div className="text-lg font-black text-white font-mono">{avgLatency} ms</div>
            <div className="text-[10px] text-emerald-400">Stable on 3G/4G packet queue</div>
          </div>

          <div className="bg-slate-800/60 p-3 rounded-2xl border border-slate-700/70 space-y-1">
            <div className="text-slate-400 text-[11px] flex items-center justify-between">
              <span>Budget RAM Tier (≤3GB)</span>
              <Cpu className="w-3.5 h-3.5 text-purple-400" />
            </div>
            <div className="text-lg font-black text-purple-300 font-mono">{lowRamDevices} Devices</div>
            <div className="text-[10px] text-purple-300">Memory footprint optimized</div>
          </div>

          <div className="bg-slate-800/60 p-3 rounded-2xl border border-slate-700/70 space-y-1">
            <div className="text-slate-400 text-[11px] flex items-center justify-between">
              <span>Low Battery Risk (&le;20%)</span>
              <Battery className="w-3.5 h-3.5 text-amber-400" />
            </div>
            <div className="text-lg font-black text-amber-300 font-mono">{batteryHealthAlerts} Drivers</div>
            <div className="text-[10px] text-slate-300">Power saver monitoring</div>
          </div>

          <div className="bg-slate-800/60 p-3 rounded-2xl border border-slate-700/70 space-y-1">
            <div className="text-slate-400 text-[11px] flex items-center justify-between">
              <span>GPS Precision</span>
              <Activity className="w-3.5 h-3.5 text-emerald-400" />
            </div>
            <div className="text-lg font-black text-emerald-300 font-mono">&plusmn; 3.8m</div>
            <div className="text-[10px] text-emerald-400">High-accuracy GNSS fix</div>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-3.5 rounded-2xl border border-slate-200/90 shadow-2xs flex flex-wrap items-center justify-between gap-2.5 text-xs">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            value={searchDevice}
            onChange={(e) => setSearchDevice(e.target.value)}
            placeholder="Search driver, device model (e.g. A12, Redmi)..."
            className="w-full pl-9 pr-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-slate-400 text-[11px] font-bold">Brand:</span>
          {['all', 'samsung', 'xiaomi', 'infinix', 'tecno'].map((brand) => (
            <button
              key={brand}
              onClick={() => setFilterBrand(brand)}
              className={`px-2.5 py-1 rounded-lg text-[11px] font-bold uppercase transition-all ${
                filterBrand === brand
                  ? 'bg-slate-900 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {brand}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-slate-400 text-[11px] font-bold">Health Filter:</span>
          <button
            onClick={() => setFilterHealth('all')}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold ${
              filterHealth === 'all' ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600'
            }`}
          >
            All
          </button>
          <button
            onClick={() => setFilterHealth('low_ram')}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold ${
              filterHealth === 'low_ram' ? 'bg-purple-600 text-white' : 'bg-purple-50 text-purple-700'
            }`}
          >
            Budget RAM (≤3GB)
          </button>
          <button
            onClick={() => setFilterHealth('low_battery')}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold ${
              filterHealth === 'low_battery' ? 'bg-amber-600 text-white' : 'bg-amber-50 text-amber-700'
            }`}
          >
            Low Battery
          </button>
        </div>
      </div>

      {/* Device Telemetry Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
        {filtered.map(({ driver, telemetry }) => {
          const isLowRam = telemetry.ramTotalGb <= 3;
          const isLowBattery = telemetry.batteryLevel <= 20;
          const isHighLatency = telemetry.networkLatencyMs >= 150;

          return (
            <div
              key={driver.id}
              onClick={() => onInspectDriver(driver)}
              className="bg-white p-4 rounded-3xl border border-slate-200/90 shadow-2xs hover:shadow-md hover:border-blue-300 transition-all cursor-pointer space-y-3"
            >
              {/* Top Driver & Device Header */}
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2.5">
                  <img
                    src={driver.avatar}
                    alt={driver.fullName}
                    className="w-10 h-10 rounded-xl object-cover border border-slate-200"
                  />
                  <div>
                    <h4 className="font-extrabold text-xs text-slate-900">{driver.fullName}</h4>
                    <p className="text-[10px] text-slate-500 font-mono">{driver.vehicle.licensePlate} • {driver.vehicle.make}</p>
                  </div>
                </div>

                <span className="text-[10px] bg-slate-100 text-slate-700 px-2 py-0.5 rounded-md font-mono font-bold">
                  {telemetry.androidVersion.replace('Android ', 'v')}
                </span>
              </div>

              {/* Hardware Spec Box */}
              <div className="bg-slate-50 p-3 rounded-2xl border border-slate-200/70 space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-slate-800 text-[11px] flex items-center gap-1">
                    <Smartphone className="w-3.5 h-3.5 text-blue-600" />
                    {telemetry.deviceModel}
                  </span>
                  <span className="text-[10px] text-slate-500 uppercase font-mono font-semibold">
                    {telemetry.manufacturer}
                  </span>
                </div>

                <div className="grid grid-cols-3 gap-1.5 text-[10px]">
                  <div className={`p-1.5 rounded-xl border ${
                    isLowRam ? 'bg-purple-50 text-purple-900 border-purple-200 font-bold' : 'bg-white text-slate-700 border-slate-100'
                  }`}>
                    <span className="text-slate-400 block text-[9px]">RAM</span>
                    {telemetry.ramTotalGb} GB ({telemetry.ramUsagePercent}%)
                  </div>

                  <div className={`p-1.5 rounded-xl border ${
                    isLowBattery ? 'bg-rose-50 text-rose-900 border-rose-200 font-bold animate-pulse' : 'bg-white text-slate-700 border-slate-100'
                  }`}>
                    <span className="text-slate-400 block text-[9px]">Battery</span>
                    {telemetry.batteryLevel}% {telemetry.isBatterySaver ? '⚡' : ''}
                  </div>

                  <div className={`p-1.5 rounded-xl border ${
                    isHighLatency ? 'bg-amber-50 text-amber-900 border-amber-200 font-bold' : 'bg-white text-slate-700 border-slate-100'
                  }`}>
                    <span className="text-slate-400 block text-[9px]">Ping ({telemetry.networkType})</span>
                    {telemetry.networkLatencyMs} ms
                  </div>
                </div>
              </div>

              {/* Performance Footer */}
              <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1">
                <span className="flex items-center gap-1 font-mono text-[10px]">
                  <Activity className="w-3 h-3 text-emerald-500" />
                  GPS: ±{telemetry.gpsAccuracyMeters}m
                </span>
                <span className="text-[10px] text-blue-600 font-bold hover:underline">
                  View Full Dossier →
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
