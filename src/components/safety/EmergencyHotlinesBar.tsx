import React from 'react';
import { PhoneCall, Shield, Ambulance, Siren, PhoneForwarded } from 'lucide-react';

export const EmergencyHotlinesBar: React.FC = () => {
  const hotlines = [
    {
      name: 'Police Emergency',
      number: '15',
      tel: 'tel:15',
      badge: 'Punjab Police 15 Dispatch',
      icon: Siren,
      bg: 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100',
      iconColor: 'text-rose-600',
    },
    {
      name: 'Ambulance & Rescue',
      number: '1122',
      tel: 'tel:1122',
      badge: 'Emergency Rescue 1122',
      icon: Ambulance,
      bg: 'bg-amber-50 text-amber-800 border-amber-200 hover:bg-amber-100',
      iconColor: 'text-amber-600',
    },
    {
      name: 'Motorway Police',
      number: '130',
      tel: 'tel:130',
      badge: 'NHMP Patrol & Highway Unit',
      icon: Shield,
      bg: 'bg-blue-50 text-blue-800 border-blue-200 hover:bg-blue-100',
      iconColor: 'text-blue-600',
    },
    {
      name: 'Drigo Rapid Response',
      number: '0800-37446',
      tel: 'tel:080037446',
      badge: '24/7 Ops Emergency Hotline',
      icon: PhoneForwarded,
      bg: 'bg-emerald-50 text-emerald-800 border-emerald-200 hover:bg-emerald-100',
      iconColor: 'text-emerald-600',
    },
  ];

  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-xs">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-3">
        <div className="flex items-center space-x-2">
          <div className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
          <span className="text-xs font-bold uppercase tracking-wider text-slate-700">
            Emergency Dispatch & Priority Intercept Hotlines
          </span>
        </div>
        <span className="text-[11px] text-slate-500">
          Dedicated lines for rapid incident escalation & police location broadcasting.
        </span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-2.5">
        {hotlines.map((hl) => {
          const Icon = hl.icon;
          return (
            <a
              key={hl.name}
              href={hl.tel}
              className={`flex items-center justify-between p-3 rounded-xl border transition-colors ${hl.bg}`}
              title={`Call ${hl.name} (${hl.number})`}
            >
              <div className="flex items-center space-x-2.5 min-w-0">
                <div className="w-8 h-8 rounded-lg bg-white shadow-2xs flex items-center justify-center shrink-0">
                  <Icon className={`w-4 h-4 ${hl.iconColor}`} />
                </div>
                <div className="truncate">
                  <div className="text-[11px] font-bold text-slate-900 truncate">{hl.name}</div>
                  <div className="text-[10px] opacity-80 truncate">{hl.badge}</div>
                </div>
              </div>

              <div className="font-mono font-extrabold text-sm pl-2 shrink-0">
                {hl.number}
              </div>
            </a>
          );
        })}
      </div>
    </div>
  );
};
