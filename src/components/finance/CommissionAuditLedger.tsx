import React, { useState, useMemo } from 'react';
import {
  TrendingUp,
  Search,
  Filter,
  DollarSign,
  PieChart,
  Car,
  Calendar,
  Layers,
  ArrowUpRight,
  ShieldCheck,
  CheckCircle2,
  FileSpreadsheet
} from 'lucide-react';
import { Trip, Driver } from '../../types';

interface CommissionAuditLedgerProps {
  trips: Trip[];
  drivers: Driver[];
}

export const CommissionAuditLedger: React.FC<CommissionAuditLedgerProps> = ({
  trips,
  drivers,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [paymentFilter, setPaymentFilter] = useState<'all' | 'cash' | 'jazzcash' | 'easypaisa' | 'card' | 'wallet'>('all');
  const [vehicleFilter, setVehicleFilter] = useState<'all' | 'boda_bike' | 'tuktuk_auto' | 'sedan' | 'comfort' | 'xl_van'>('all');

  const completedTrips = useMemo(() => {
    return trips.filter((t) => t.status === 'completed');
  }, [trips]);

  // Aggregate stats
  const totalGrossFares = completedTrips.reduce((acc, t) => acc + (t.fare?.total || 0), 0);
  const totalCommission = completedTrips.reduce(
    (acc, t) => acc + (t.fare?.drigoCommissionAmount || (t.fare?.total || 0) * 0.18),
    0
  );
  const totalDriverPayouts = totalGrossFares - totalCommission;

  // Breakdown by payment method
  const cashTrips = completedTrips.filter((t) => t.paymentMethod === 'cash');
  const cashGross = cashTrips.reduce((acc, t) => acc + (t.fare?.total || 0), 0);
  const cashCommission = cashTrips.reduce(
    (acc, t) => acc + (t.fare?.drigoCommissionAmount || (t.fare?.total || 0) * 0.18),
    0
  );

  const digitalTrips = completedTrips.filter((t) => t.paymentMethod !== 'cash');
  const digitalGross = digitalTrips.reduce((acc, t) => acc + (t.fare?.total || 0), 0);
  const digitalCommission = digitalTrips.reduce(
    (acc, t) => acc + (t.fare?.drigoCommissionAmount || (t.fare?.total || 0) * 0.18),
    0
  );

  // Filtered trips
  const filteredTrips = useMemo(() => {
    return completedTrips.filter((t) => {
      const matchSearch =
        !searchTerm ||
        t.tripCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.driverName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.passengerName?.toLowerCase().includes(searchTerm.toLowerCase());

      const matchPayment = paymentFilter === 'all' || t.paymentMethod === paymentFilter;
      const matchVehicle = vehicleFilter === 'all' || t.vehicleType === vehicleFilter;

      return matchSearch && matchPayment && matchVehicle;
    });
  }, [completedTrips, searchTerm, paymentFilter, vehicleFilter]);

  const exportCommissionCSV = () => {
    const headers = ['Trip Code', 'Date', 'Driver', 'Passenger', 'Vehicle', 'Payment Method', 'Gross Fare (Rs.)', 'Drigo Cut (18%)', 'Driver Net (Rs.)'];
    const rows = filteredTrips.map((t) => {
      const gross = t.fare?.total || 0;
      const comm = t.fare?.drigoCommissionAmount || gross * 0.18;
      const net = gross - comm;
      return [
        t.tripCode || t.id,
        t.completedAt || t.requestedAt,
        `"${t.driverName || 'N/A'}"`,
        `"${t.passengerName || 'N/A'}"`,
        t.vehicleType,
        t.paymentMethod,
        gross,
        comm,
        net
      ];
    });

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `drigo_commission_audit_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-4">
      {/* Overview Breakdown Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Total Platform Take */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Total Commission Collected
            </span>
            <span className="text-xs font-extrabold text-blue-700 bg-blue-50 px-2 py-0.5 rounded-md border border-blue-100">
              18% Take Rate
            </span>
          </div>
          <div className="text-2xl font-extrabold text-slate-900 font-mono">
            Rs. {Math.round(totalCommission).toLocaleString()}
          </div>
          <div className="text-[11px] text-slate-500 flex justify-between pt-1 border-t border-slate-100">
            <span>Gross Platform GMV:</span>
            <span className="font-mono font-bold text-slate-700">Rs. {Math.round(totalGrossFares).toLocaleString()}</span>
          </div>
        </div>

        {/* Cash Rides Split */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Cash Collections
            </span>
            <span className="text-xs font-extrabold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-100">
              {cashTrips.length} Rides
            </span>
          </div>
          <div className="text-2xl font-extrabold text-slate-900 font-mono">
            Rs. {Math.round(cashCommission).toLocaleString()}
          </div>
          <div className="text-[11px] text-slate-500 flex justify-between pt-1 border-t border-slate-100">
            <span>Deducted from driver wallet:</span>
            <span className="font-mono font-bold text-amber-700">Rs. {Math.round(cashGross).toLocaleString()} (Cash Collected)</span>
          </div>
        </div>

        {/* Digital / Gateway Split */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Digital / Wallet Payments
            </span>
            <span className="text-xs font-extrabold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-100">
              {digitalTrips.length} Rides
            </span>
          </div>
          <div className="text-2xl font-extrabold text-slate-900 font-mono">
            Rs. {Math.round(digitalCommission).toLocaleString()}
          </div>
          <div className="text-[11px] text-slate-500 flex justify-between pt-1 border-t border-slate-100">
            <span>Credited to driver net:</span>
            <span className="font-mono font-bold text-emerald-700">Rs. {Math.round(digitalGross - digitalCommission).toLocaleString()}</span>
          </div>
        </div>
      </div>

      {/* Filter & Export Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row items-center justify-between gap-3">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search trip code, driver, rider..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500/20 focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          <select
            value={paymentFilter}
            onChange={(e) => setPaymentFilter(e.target.value as any)}
            className="p-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:outline-none cursor-pointer"
          >
            <option value="all">All Payment Methods</option>
            <option value="cash">Cash Settlement</option>
            <option value="jazzcash">JazzCash</option>
            <option value="easypaisa">EasyPaisa</option>
            <option value="wallet">Drigo Wallet</option>
            <option value="card">Credit / Debit Card</option>
          </select>

          <select
            value={vehicleFilter}
            onChange={(e) => setVehicleFilter(e.target.value as any)}
            className="p-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white focus:outline-none cursor-pointer"
          >
            <option value="all">All Vehicle Types</option>
            <option value="boda_bike">Moto / Bike</option>
            <option value="tuktuk_auto">Auto Rickshaw</option>
            <option value="sedan">Sedan</option>
            <option value="comfort">Executive / Comfort</option>
            <option value="xl_van">XL Van</option>
          </select>

          <button
            onClick={exportCommissionCSV}
            className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors flex items-center gap-1.5 cursor-pointer"
          >
            <FileSpreadsheet className="w-3.5 h-3.5 text-slate-600" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Commission Audit Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider text-[11px]">
              <tr>
                <th className="p-4">Trip Code</th>
                <th className="p-4">Driver</th>
                <th className="p-4">Passenger</th>
                <th className="p-4">Vehicle Tier</th>
                <th className="p-4">Payment Method</th>
                <th className="p-4 text-right">Gross Fare</th>
                <th className="p-4 text-right">Drigo Cut (18%)</th>
                <th className="p-4 text-right">Driver Net</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredTrips.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-slate-400">
                    No completed ride records matching your filters.
                  </td>
                </tr>
              ) : (
                filteredTrips.map((t) => {
                  const gross = t.fare?.total || 0;
                  const commission = t.fare?.drigoCommissionAmount || gross * 0.18;
                  const driverNet = gross - commission;

                  return (
                    <tr key={t.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="p-4 font-mono font-bold text-slate-900">
                        {t.tripCode || t.id.slice(0, 8)}
                        <div className="text-[10px] text-slate-400 font-sans font-normal">
                          {t.completedAt || t.requestedAt}
                        </div>
                      </td>

                      <td className="p-4">
                        <div className="font-bold text-slate-900">{t.driverName || 'N/A'}</div>
                        <div className="text-[11px] text-slate-500">{t.driverPhone || ''}</div>
                      </td>

                      <td className="p-4">
                        <div className="font-semibold text-slate-800">{t.passengerName}</div>
                        <div className="text-[11px] text-slate-400">{t.passengerPhone}</div>
                      </td>

                      <td className="p-4">
                        <span className="capitalize px-2 py-0.5 bg-slate-100 font-bold text-slate-700 rounded-md text-[10px]">
                          {t.vehicleType}
                        </span>
                      </td>

                      <td className="p-4">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold uppercase bg-slate-100 text-slate-700">
                          {t.paymentMethod === 'cash' && '💵 Cash'}
                          {t.paymentMethod === 'jazzcash' && '🟠 JazzCash'}
                          {t.paymentMethod === 'easypaisa' && '🟢 EasyPaisa'}
                          {t.paymentMethod === 'card' && '💳 Card'}
                          {t.paymentMethod === 'wallet' && '👛 Wallet'}
                        </span>
                      </td>

                      <td className="p-4 text-right font-mono font-bold text-slate-900">
                        Rs. {Math.round(gross).toLocaleString()}
                      </td>

                      <td className="p-4 text-right font-mono font-extrabold text-blue-600">
                        Rs. {Math.round(commission).toLocaleString()}
                      </td>

                      <td className="p-4 text-right font-mono font-extrabold text-emerald-600">
                        Rs. {Math.round(driverNet).toLocaleString()}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
