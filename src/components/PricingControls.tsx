import React, { useState } from 'react';
import { PricingConfig, SurgeZone, VehicleType } from '../types';
import { DEFAULT_PRICING_CONFIGS } from '../mockData';
import { Zap, Save, CheckCircle, Info } from 'lucide-react';

interface PricingControlsProps {
  configs?: PricingConfig[];
  surgeZones?: SurgeZone[];
  onUpdatePricingConfig: (updated: PricingConfig) => void;
  onUpdateSurgeMultiplier: (zoneId: string, multiplier: number) => void;
}

const FALLBACK_PRICING_CONFIG: PricingConfig = {
  vehicleType: 'sedan',
  name: 'Drigo Mini / Go (Alto / Cultus)',
  baseFare: 120.0,
  perKmRate: 50.0,
  perMinuteRate: 5.0,
  minimumFare: 200.0,
  commissionPercentage: 18,
  cancellationFee: 60.0,
  currency: 'Rs.',
};

export const PricingControls: React.FC<PricingControlsProps> = ({
  configs = DEFAULT_PRICING_CONFIGS,
  surgeZones = [],
  onUpdatePricingConfig,
  onUpdateSurgeMultiplier,
}) => {
  const [selectedVehicle, setSelectedVehicle] = useState<VehicleType>('sedan');
  
  const safeConfigs = Array.isArray(configs) && configs.length > 0 ? configs : DEFAULT_PRICING_CONFIGS;
  const safeSurgeZones = Array.isArray(surgeZones) ? surgeZones : [];

  const activeConfig =
    safeConfigs.find((c) => c?.vehicleType === selectedVehicle) ||
    safeConfigs[0] ||
    FALLBACK_PRICING_CONFIG;

  const [formConfig, setFormConfig] = useState<PricingConfig>(activeConfig || FALLBACK_PRICING_CONFIG);
  const [savedSuccess, setSavedSuccess] = useState(false);

  React.useEffect(() => {
    if (activeConfig) {
      setFormConfig(activeConfig);
    }
  }, [selectedVehicle, activeConfig]);

  const handleSave = () => {
    onUpdatePricingConfig(formConfig);
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 2000);
  };

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50">
      {/* Title */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <h2 className="text-xl font-extrabold text-slate-900">Fare Matrix & Surge Pricing Controls</h2>
          <p className="text-xs text-slate-500">Configure base fares, per-km rates, Drigo commission %, and real-time surge multiplier zones.</p>
        </div>
        {savedSuccess && (
          <div className="flex items-center space-x-1.5 bg-emerald-100 text-emerald-800 px-3 py-1 rounded-lg text-xs font-bold animate-fadeIn">
            <CheckCircle className="w-4 h-4 text-emerald-600" />
            <span>Pricing Config Saved</span>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Fare Matrix Configuration Form */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-5">
          {/* Vehicle Category Selector Tabs */}
          <div className="flex space-x-1 bg-slate-100 p-1 rounded-xl text-xs font-bold overflow-x-auto">
            {safeConfigs.map((cfg, idx) => {
              const vType = cfg?.vehicleType || `vehicle-${idx}`;
              const tabKey = `pricing-tab-${vType}-${idx}`;
              return (
                <button
                  key={tabKey}
                  type="button"
                  onClick={() => cfg?.vehicleType && setSelectedVehicle(cfg.vehicleType)}
                  className={`px-3 py-2 rounded-lg transition-all shrink-0 ${
                    selectedVehicle === cfg?.vehicleType
                      ? 'bg-blue-600 text-white shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {cfg?.name || cfg?.vehicleType || `Tier ${idx + 1}`}
                </button>
              );
            })}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Base Fare ({formConfig?.currency || 'Rs.'})</label>
              <input
                type="number"
                step="5"
                value={formConfig?.baseFare ?? 0}
                onChange={(e) => setFormConfig({ ...formConfig, baseFare: parseFloat(e.target.value) || 0 })}
                className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Per Kilometer Rate ({formConfig?.currency || 'Rs.'}/km)</label>
              <input
                type="number"
                step="1"
                value={formConfig?.perKmRate ?? 0}
                onChange={(e) => setFormConfig({ ...formConfig, perKmRate: parseFloat(e.target.value) || 0 })}
                className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Per Minute Time Rate ({formConfig?.currency || 'Rs.'}/min)</label>
              <input
                type="number"
                step="0.5"
                value={formConfig?.perMinuteRate ?? 0}
                onChange={(e) => setFormConfig({ ...formConfig, perMinuteRate: parseFloat(e.target.value) || 0 })}
                className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Minimum Ride Fare ({formConfig?.currency || 'Rs.'})</label>
              <input
                type="number"
                step="10"
                value={formConfig?.minimumFare ?? 0}
                onChange={(e) => setFormConfig({ ...formConfig, minimumFare: parseFloat(e.target.value) || 0 })}
                className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Drigo Commission Rate (%)</label>
              <input
                type="number"
                step="1"
                min="5"
                max="30"
                value={formConfig?.commissionPercentage ?? 18}
                onChange={(e) => setFormConfig({ ...formConfig, commissionPercentage: parseFloat(e.target.value) || 18 })}
                className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold text-blue-600 focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Cancellation Fee ({formConfig?.currency || 'Rs.'})</label>
              <input
                type="number"
                step="10"
                value={formConfig?.cancellationFee ?? 0}
                onChange={(e) => setFormConfig({ ...formConfig, cancellationFee: parseFloat(e.target.value) || 0 })}
                className="w-full p-2.5 border border-slate-200 rounded-xl text-xs font-mono font-bold focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
              />
            </div>
          </div>

          <div className="flex justify-end pt-3 border-t border-slate-100">
            <button
              onClick={handleSave}
              className="flex items-center space-x-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl text-xs shadow-md transition-all"
            >
              <Save className="w-4 h-4" />
              <span>Save & Publish Fare Matrix</span>
            </button>
          </div>
        </div>

        {/* Real-time Surge Zones Multipliers */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Zap className="w-4 h-4 text-amber-500 fill-amber-500" />
              <h3 className="text-sm font-bold text-slate-900">Active Surge Multiplier Zones</h3>
            </div>
            <span className="text-[10px] bg-amber-100 text-amber-800 font-bold px-2 py-0.5 rounded-full">
              Live Demand Engine
            </span>
          </div>

          <div className="space-y-3">
            {safeSurgeZones.length === 0 ? (
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 text-center space-y-1.5">
                <Info className="w-5 h-5 text-slate-400 mx-auto" />
                <div className="text-xs font-bold text-slate-700">No Active Surge Zones</div>
                <p className="text-[11px] text-slate-500">
                  Standard 1.0x baseline fares currently apply across all active regions.
                </p>
              </div>
            ) : (
              safeSurgeZones.map((zone, idx) => {
                const zoneId = zone?.id || `zone-${idx}`;
                const cardKey = `surge-zone-card-${zoneId}-${idx}`;
                return (
                  <div key={cardKey} className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-900">{zone?.name || `Surge Zone ${idx + 1}`}</span>
                      <span className="text-xs font-mono font-extrabold text-amber-600 bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                        {zone?.surgeMultiplier ?? 1.0}x
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-slate-500">
                      <span>Demand: {zone?.activeDemand ?? 0} rides</span>
                      <span>Drivers: {zone?.availableDrivers ?? 0} online</span>
                    </div>

                    {/* Multiplier Slider */}
                    <div className="pt-1">
                      <input
                        type="range"
                        min="1.0"
                        max="3.0"
                        step="0.1"
                        value={zone?.surgeMultiplier ?? 1.0}
                        onChange={(e) => (zone?.id ? onUpdateSurgeMultiplier(zone.id, parseFloat(e.target.value)) : null)}
                        className="w-full accent-amber-500 cursor-pointer"
                      />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
