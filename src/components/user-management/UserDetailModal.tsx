import React from 'react';
import { Driver, Rider } from '../../types';
import {
  User,
  Phone,
  Mail,
  Star,
  Car,
  CreditCard,
  Calendar,
  Smartphone,
  ShieldCheck,
  ShieldAlert,
  Clock,
  X,
  CheckCircle,
  AlertTriangle,
  FileText,
  DollarSign
} from 'lucide-react';

interface UserDetailModalProps {
  user: Driver | Rider;
  type: 'driver' | 'rider';
  onClose: () => void;
  onToggleStatus: (id: string, status: any) => void;
  onOpenKyc?: (driver: Driver) => void;
  onOpenWalletAdjustment?: (driver: Driver) => void;
}

export const UserDetailModal: React.FC<UserDetailModalProps> = ({
  user,
  type,
  onClose,
  onToggleStatus,
  onOpenKyc,
  onOpenWalletAdjustment,
}) => {
  const isDriver = type === 'driver';
  const driver = isDriver ? (user as Driver) : null;
  const rider = !isDriver ? (user as Rider) : null;

  const statusStr = String(user.status || '').toLowerCase();
  const isSuspended = statusStr === 'suspended' || (user as any).accountStatus === 'SUSPENDED';
  const isFlagged = statusStr === 'flagged';

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-3xl max-w-xl w-full p-6 shadow-2xl space-y-5 border border-slate-200 max-h-[90vh] overflow-y-auto">
        {/* Header with Avatar and Basic Info */}
        <div className="flex items-start justify-between border-b border-slate-100 pb-4">
          <div className="flex items-center space-x-3.5">
            <img
              src={user.avatar}
              alt={user.fullName}
              className="w-14 h-14 rounded-2xl object-cover border-2 border-slate-200 shadow-xs shrink-0"
            />
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-black text-slate-900">{user.fullName}</h3>
                <span
                  className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                    isDriver
                      ? 'bg-blue-100 text-blue-800 border border-blue-200'
                      : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                  }`}
                >
                  {isDriver ? 'Driver' : 'Passenger'}
                </span>
              </div>
              <p className="text-xs text-slate-500 font-mono mt-0.5">ID: {user.id}</p>
              <div className="flex items-center gap-2 text-[11px] text-slate-500 mt-1">
                <span className="flex items-center gap-1 font-bold text-slate-800">
                  <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                  {user.rating} ★
                </span>
                <span>•</span>
                <span>{isDriver ? `${driver?.totalTrips || 0} completed rides` : `${rider?.totalRides || 0} rides taken`}</span>
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-700 p-1.5 rounded-xl hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Status Alert Banner */}
        <div className="flex items-center justify-between p-3 rounded-2xl border text-xs font-semibold bg-slate-50 border-slate-200">
          <div className="flex items-center gap-2">
            <span className="text-slate-500">Account Status:</span>
            {isSuspended ? (
              <span className="bg-rose-100 text-rose-800 border border-rose-300 px-2.5 py-0.5 rounded-full uppercase font-bold text-[10px]">
                SUSPENDED
              </span>
            ) : isFlagged ? (
              <span className="bg-orange-100 text-orange-800 border border-orange-300 px-2.5 py-0.5 rounded-full uppercase font-bold text-[10px]">
                FLAGGED
              </span>
            ) : statusStr === 'pending_verification' ? (
              <span className="bg-amber-100 text-amber-800 border border-amber-300 px-2.5 py-0.5 rounded-full uppercase font-bold text-[10px] animate-pulse">
                PENDING VERIFICATION
              </span>
            ) : (
              <span className="bg-emerald-100 text-emerald-800 border border-emerald-300 px-2.5 py-0.5 rounded-full uppercase font-bold text-[10px]">
                {statusStr.toUpperCase() || 'ACTIVE'}
              </span>
            )}
          </div>

          <div className="text-[11px] text-slate-400 font-mono">
            Joined: {user.joinedDate || '2026-01-15'}
          </div>
        </div>

        {/* Contact Information */}
        <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/80 space-y-2.5 text-xs">
          <h4 className="font-bold text-slate-900 flex items-center gap-1.5">
            <User className="w-4 h-4 text-blue-600" />
            Contact & Identity Details
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-slate-600">
            <div className="flex items-center gap-1.5">
              <Phone className="w-3.5 h-3.5 text-slate-400" />
              <span className="font-semibold text-slate-800">{user.phone}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Mail className="w-3.5 h-3.5 text-slate-400" />
              <span className="font-semibold text-slate-800">{user.email || 'N/A'}</span>
            </div>
          </div>
        </div>

        {/* Driver Specific: Vehicle & Financial Dossier */}
        {isDriver && driver && (
          <div className="space-y-3 text-xs">
            {/* Vehicle Info Card */}
            <div className="bg-blue-50/60 p-4 rounded-2xl border border-blue-200/80 space-y-2.5">
              <div className="flex items-center justify-between">
                <h4 className="font-bold text-blue-950 flex items-center gap-1.5">
                  <Car className="w-4 h-4 text-blue-600" />
                  Assigned Vehicle Specifications
                </h4>
                <span className="text-[10px] uppercase font-mono font-bold bg-blue-200/70 text-blue-900 px-2 py-0.5 rounded-md">
                  {driver.vehicle?.type || 'Sedan'}
                </span>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-blue-900 text-[11px]">
                <div>
                  <span className="text-blue-600/80 block text-[10px]">Make & Model</span>
                  <span className="font-bold">{driver.vehicle?.make} {driver.vehicle?.model}</span>
                </div>
                <div>
                  <span className="text-blue-600/80 block text-[10px]">License Plate</span>
                  <span className="font-bold font-mono bg-white/80 px-1.5 py-0.5 rounded border border-blue-200 inline-block mt-0.5">
                    {driver.vehicle?.licensePlate}
                  </span>
                </div>
                <div>
                  <span className="text-blue-600/80 block text-[10px]">Color / Year</span>
                  <span className="font-semibold">{driver.vehicle?.color || 'White'} ({driver.vehicle?.year || 2024})</span>
                </div>
              </div>
            </div>

            {/* Wallet & Earnings Summary */}
            <div className="bg-emerald-50/60 p-4 rounded-2xl border border-emerald-200/80 flex items-center justify-between">
              <div>
                <span className="text-[10px] text-emerald-800 font-bold block">Current Driver Wallet (PKR)</span>
                <div className="text-xl font-black text-emerald-700 font-mono mt-0.5">
                  Rs. {(driver.walletBalance ?? 0).toLocaleString('en-PK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>
              </div>
              {onOpenWalletAdjustment && (
                <button
                  onClick={() => {
                    onClose();
                    onOpenWalletAdjustment(driver);
                  }}
                  className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs shadow-2xs transition-all flex items-center gap-1 cursor-pointer"
                >
                  <DollarSign className="w-3.5 h-3.5" />
                  Adjust Balance
                </button>
              )}
            </div>

            {/* Android Device & Telemetry */}
            {driver.telemetry && (
              <div className="bg-slate-900 text-white p-4 rounded-2xl border border-slate-800 space-y-2 text-xs">
                <div className="flex items-center justify-between text-slate-300">
                  <span className="font-bold flex items-center gap-1.5">
                    <Smartphone className="w-3.5 h-3.5 text-emerald-400" />
                    Android Hardware Telemetry
                  </span>
                  <span className="text-[10px] bg-slate-800 text-emerald-400 px-2 py-0.5 rounded font-mono">
                    {driver.telemetry.androidVersion || 'Android 11'}
                  </span>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1 text-[10px]">
                  <div className="bg-slate-800/80 p-2 rounded-xl">
                    <div className="text-slate-400">Device Model</div>
                    <div className="font-bold text-white truncate">{driver.telemetry.deviceModel}</div>
                  </div>
                  <div className="bg-slate-800/80 p-2 rounded-xl">
                    <div className="text-slate-400">RAM Capacity</div>
                    <div className="font-bold text-emerald-400">{driver.telemetry.ramTotalGb} GB</div>
                  </div>
                  <div className="bg-slate-800/80 p-2 rounded-xl">
                    <div className="text-slate-400">Battery Level</div>
                    <div className="font-bold text-amber-400">{driver.telemetry.batteryLevel}%</div>
                  </div>
                  <div className="bg-slate-800/80 p-2 rounded-xl">
                    <div className="text-slate-400">Network Latency</div>
                    <div className="font-bold text-blue-400">{driver.telemetry.networkLatencyMs}ms ({driver.telemetry.networkType})</div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Passenger Specific: Payment & Emergency */}
        {!isDriver && rider && (
          <div className="space-y-3 text-xs">
            <div className="bg-emerald-50/60 p-4 rounded-2xl border border-emerald-200/80 space-y-2">
              <h4 className="font-bold text-emerald-950 flex items-center gap-1.5">
                <CreditCard className="w-4 h-4 text-emerald-600" />
                Payment Preferences & Security
              </h4>
              <div className="grid grid-cols-2 gap-2 text-emerald-900 text-[11px]">
                <div>
                  <span className="text-emerald-700/80 block text-[10px]">Preferred Payment</span>
                  <span className="font-bold capitalize">{rider.preferredPayment || 'Cash'}</span>
                </div>
                <div>
                  <span className="text-emerald-700/80 block text-[10px]">Wallet Balance</span>
                  <span className="font-bold font-mono">Rs. {(rider.walletBalance || 0).toFixed(2)}</span>
                </div>
              </div>
            </div>

            {rider.emergencyContact && (
              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/80 space-y-1.5">
                <h4 className="font-bold text-slate-900 flex items-center gap-1.5 text-xs">
                  <ShieldCheck className="w-4 h-4 text-blue-600" />
                  Emergency SOS Contact
                </h4>
                <div className="text-slate-700 text-xs">
                  <span className="font-bold">{rider.emergencyContact.name}</span> ({rider.emergencyContact.relationship || 'Next of Kin'}) • {rider.emergencyContact.phone}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Action Controls in Modal Footer */}
        <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2.5">
          <div className="flex items-center space-x-2">
            {isDriver && driver && onOpenKyc && (
              <button
                onClick={() => {
                  onClose();
                  onOpenKyc(driver);
                }}
                className="px-3.5 py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs rounded-xl shadow-2xs transition-all flex items-center gap-1.5 cursor-pointer"
              >
                <FileText className="w-3.5 h-3.5" />
                Inspect KYC Docs
              </button>
            )}

            {isDriver && (
              isSuspended ? (
                <button
                  onClick={() => {
                    onToggleStatus(user.id, 'online');
                    onClose();
                  }}
                  className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-2xs transition-all cursor-pointer"
                >
                  Reactivate Driver Account
                </button>
              ) : (
                <button
                  onClick={() => {
                    onToggleStatus(user.id, 'suspended');
                    onClose();
                  }}
                  className="px-3.5 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 font-bold text-xs rounded-xl transition-all cursor-pointer"
                >
                  Suspend Driver Account
                </button>
              )
            )}

            {!isDriver && (
              isSuspended ? (
                <button
                  onClick={() => {
                    onToggleStatus(user.id, 'active');
                    onClose();
                  }}
                  className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-2xs transition-all cursor-pointer"
                >
                  Reactivate Passenger
                </button>
              ) : isFlagged ? (
                <button
                  onClick={() => {
                    onToggleStatus(user.id, 'active');
                    onClose();
                  }}
                  className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-2xs transition-all cursor-pointer"
                >
                  Unflag Account
                </button>
              ) : (
                <button
                  onClick={() => {
                    onToggleStatus(user.id, 'flagged');
                    onClose();
                  }}
                  className="px-3.5 py-2 bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 font-bold text-xs rounded-xl transition-all cursor-pointer"
                >
                  Flag Passenger
                </button>
              )
            )}
          </div>

          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl transition-all ml-auto"
          >
            Close Dossier
          </button>
        </div>
      </div>
    </div>
  );
};
