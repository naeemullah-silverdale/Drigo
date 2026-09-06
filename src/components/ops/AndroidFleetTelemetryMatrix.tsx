import React, { useState } from 'react';
import { Driver } from '../../types';
import {
  Smartphone,
  Battery,
  BatteryCharging,
  Cpu,
  Wifi,
  WifiOff,
  Signal,
  AlertTriangle,
  CheckCircle2,
  Send,
  ExternalLink,
  Shield,
  Clock,
  Layers
} from 'lucide-react';

interface AndroidFleetTelemetryMatrixProps {
  drivers: Driver[];
  onInspectDriver: (driverId: string) => void;
  onSendBatteryAlert: (driverId: string, driverName: string) => void;
}

export const AndroidFleetTelemetryMatrix: React.FC<AndroidFleetTelemetryMatrixProps> = ({
  drivers,
  onInspectDriver,
  onSendBatteryAlert,
}) => {
  const [selectedTab, setSelectedTab] = useState<'all' | 'low_battery' | 'high_ram' | 'unstable_net'>('all');

  // Compute telemetry statistics
  const lowBatteryDrivers = drivers.filter(
    (d) => (d.telemetry?.batteryLevel ?? 100) < 25
  );

  const highRamDrivers = drivers.filter(
    (d) => (d.telemetry?.ramUsagePercent ?? 0) > 75 || (d.telemetry?.ramTotalGb ?? 4) <= 2
  );

  const highLatencyDrivers = drivers.filter(
    (d) => (d.telemetry?.networkLatencyMs ?? 0) > 150 || d.telemetry?.networkType === '2G' || d.telemetry?.networkType === '3G'
  );

  // Filtered list
  const displayDrivers = drivers.filter((d) => {
    if (selectedTab === 'low_battery') return (d.telemetry?.batteryLevel ?? 100) < 25;
    if (selectedTab === 'high_ram') return (d.telemetry?.ramUsagePercent ?? 0) > 75 || (d.telemetry?.ramTotalGb ?? 4) <= 2;
    if (selectedTab === 'unstable_net') return (d.telemetry?.networkLatencyMs ?? 0) > 150 || d.telemetry?.networkType === '2G' || d.telemetry?.networkType === '3G';
    return true;
  });

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center space-x-2.5">
          <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-200 flex items-center justify-center">
            <Smartphone className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-extrabold text-slate-900 flex items-center gap-2">
              <span>Low-End Android Device Telemetry & Fleet Matrix</span>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                minSdk 23 Target
              </span>
            </h3>
            <p className="text-[11px] text-slate-500">
              Hardware-level observer optimizing memory allocation, battery preservation, and weak 3G/4G connectivity on budget phones (Samsung A12 / Redmi / Tecno).
            </p>
          </div>
        </div>

        {/* Telemetry Filter Tabs */}
        <div className="flex items-center space-x-1 overflow-x-auto bg-slate-50 p-1 rounded-xl border border-slate-200 text-xs font-bold">
          <button
            onClick={() => setSelectedTab('all')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              selectedTab === 'all'
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            All Hardware ({drivers.length})
          </button>
          <button
            onClick={() => setSelectedTab('low_battery')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              selectedTab === 'low_battery'
                ? 'bg-rose-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Low Battery ({lowBatteryDrivers.length})
          </button>
          <button
            onClick={() => setSelectedTab('high_ram')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              selectedTab === 'high_ram'
                ? 'bg-amber-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Low RAM / High Load ({highRamDrivers.length})
          </button>
          <button
            onClick={() => setSelectedTab('unstable_net')}
            className={`px-3 py-1.5 rounded-lg transition-all ${
              selectedTab === 'unstable_net'
                ? 'bg-blue-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Weak Signal ({highLatencyDrivers.length})
          </button>
        </div>
      </div>

      {/* Hardware Telemetry Cards List */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
        {displayDrivers.length === 0 ? (
          <div className="col-span-full py-10 px-6 bg-slate-50 rounded-2xl border border-dashed border-slate-200 text-center space-y-2">
            <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto" />
            <h4 className="text-sm font-extrabold text-slate-800">All Device Telemetry Nominal</h4>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              No devices currently exceed the specified telemetry alert thresholds for this hardware view.
            </p>
          </div>
        ) : (
          displayDrivers.map((driver) => {
            const tel = driver.telemetry || {
              deviceModel: 'Android Smartphone',
              manufacturer: 'Samsung',
              androidVersion: 'Android 11 (API 30)',
              apiLevel: 30,
              ramTotalGb: 3,
              ramUsagePercent: 50,
              batteryLevel: 80,
              isBatterySaver: false,
              appVersion: 'v2.4.1',
              networkType: '4G',
              networkLatencyMs: 45,
              gpsAccuracyMeters: 4.0,
              offlineQueuedPackets: 0,
              lastPingAt: 'Just now'
            };

            const isLowBattery = tel.batteryLevel < 25;
            const isHighRam = tel.ramUsagePercent > 75 || tel.ramTotalGb <= 2;
            const isWeakNet = tel.networkLatencyMs > 150 || tel.networkType === '2G' || tel.networkType === '3G';

            return (
              <div
                key={driver.id}
                className={`bg-white rounded-2xl border p-4 shadow-xs flex flex-col justify-between space-y-3 transition-all hover:shadow-md ${
                  isLowBattery
                    ? 'border-rose-200 hover:border-rose-400'
                    : isHighRam
                    ? 'border-amber-200 hover:border-amber-400'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                {/* Driver & Device Header */}
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center space-x-2.5 min-w-0">
                    <img
                      src={driver.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}
                      alt={driver.fullName}
                      className="w-9 h-9 rounded-xl object-cover border border-slate-200 shrink-0"
                    />
                    <div className="min-w-0">
                      <div className="font-extrabold text-xs text-slate-900 truncate">
                        {driver.fullName}
                      </div>
                      <div className="text-[10px] text-slate-500 truncate flex items-center space-x-1">
                        <Smartphone className="w-3 h-3 text-slate-400 shrink-0" />
                        <span>{tel.deviceModel || 'Budget Android'}</span>
                      </div>
                    </div>
                  </div>

                  <span
                    className={`text-[9px] font-extrabold px-2 py-0.5 rounded-full uppercase border shrink-0 ${
                      driver.status === 'online' || driver.status === 'on_trip'
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        : 'bg-slate-100 text-slate-600 border-slate-200'
                    }`}
                  >
                    {driver.status.replace('_', ' ')}
                  </span>
                </div>

                {/* Telemetry Stats Row */}
                <div className="grid grid-cols-3 gap-2 bg-slate-50 p-2.5 rounded-xl border border-slate-100 text-center">
                  {/* Battery */}
                  <div className="space-y-0.5">
                    <span className="text-[9px] font-bold text-slate-400 uppercase block">Battery</span>
                    <div className={`text-xs font-black flex items-center justify-center space-x-1 ${
                      isLowBattery ? 'text-rose-600 animate-pulse' : 'text-slate-800'
                    }`}>
                      <Battery className="w-3 h-3" />
                      <span>{tel.batteryLevel}%</span>
                    </div>
                  </div>

                  {/* RAM Total & Load */}
                  <div className="space-y-0.5">
                    <span className="text-[9px] font-bold text-slate-400 uppercase block">RAM ({tel.ramTotalGb}GB)</span>
                    <div className={`text-xs font-black flex items-center justify-center space-x-1 ${
                      isHighRam ? 'text-amber-600' : 'text-slate-800'
                    }`}>
                      <Cpu className="w-3 h-3" />
                      <span>{tel.ramUsagePercent}%</span>
                    </div>
                  </div>

                  {/* Network */}
                  <div className="space-y-0.5">
                    <span className="text-[9px] font-bold text-slate-400 uppercase block">Network</span>
                    <div className={`text-xs font-black flex items-center justify-center space-x-1 ${
                      isWeakNet ? 'text-amber-600' : 'text-slate-800'
                    }`}>
                      <Signal className="w-3 h-3" />
                      <span>{tel.networkType} ({tel.networkLatencyMs}ms)</span>
                    </div>
                  </div>
                </div>

                {/* Footer Actions */}
                <div className="flex items-center justify-between pt-1 border-t border-slate-100 text-xs">
                  <span className="text-[10px] text-slate-400 font-mono">
                    {tel.androidVersion || 'API 23+'}
                  </span>

                  <div className="flex items-center space-x-1.5">
                    {isLowBattery && (
                      <button
                        onClick={() => onSendBatteryAlert(driver.id, driver.fullName)}
                        className="inline-flex items-center space-x-1 px-2 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg text-[10px] font-bold transition-colors"
                        title="Send low battery saver push alert to driver"
                      >
                        <Send className="w-2.5 h-2.5" />
                        <span>Push Alert</span>
                      </button>
                    )}

                    <button
                      onClick={() => onInspectDriver(driver.id)}
                      className="inline-flex items-center space-x-1 px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-[10px] font-bold transition-colors"
                      title="Inspect driver profile"
                    >
                      <span>Inspect</span>
                      <ExternalLink className="w-2.5 h-2.5" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
