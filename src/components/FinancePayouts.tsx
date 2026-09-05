import React, { useState, useMemo } from 'react';
import { Driver, PayoutRequest, Trip } from '../types';
import {
  CreditCard,
  Wallet,
  TrendingUp,
  Sliders,
  Sparkles,
  ArrowDownToLine,
  RefreshCw,
  PlusCircle,
  HelpCircle
} from 'lucide-react';

import { FinanceStatCards } from './finance/FinanceStatCards';
import { PayoutRequestsTable } from './finance/PayoutRequestsTable';
import { DriverWalletsView } from './finance/DriverWalletsView';
import { CommissionAuditLedger } from './finance/CommissionAuditLedger';
import { PayoutActionModal } from './finance/PayoutActionModal';
import { PayoutReceiptModal } from './finance/PayoutReceiptModal';
import { PayoutGatewayConfigModal } from './finance/PayoutGatewayConfigModal';
import { AdjustWalletModal } from './user-management/AdjustWalletModal';

interface FinancePayoutsProps {
  drivers: Driver[];
  trips: Trip[];
  payoutRequests: PayoutRequest[];
  onUpdatePayoutStatus: (
    payoutId: string,
    status: PayoutRequest['status'],
    transactionRef?: string,
    rejectionReason?: string
  ) => void;
  onAdjustDriverWallet: (driverId: string, amount: number) => void;
}

export const FinancePayouts: React.FC<FinancePayoutsProps> = ({
  drivers,
  trips,
  payoutRequests,
  onUpdatePayoutStatus,
  onAdjustDriverWallet,
}) => {
  const [activeTab, setActiveTab] = useState<'payout_requests' | 'driver_wallets' | 'commission_breakdown'>('payout_requests');
  const [timeframe, setTimeframe] = useState<'all' | 'today' | 'week' | 'month'>('all');

  // Modal states
  const [selectedPayoutForAction, setSelectedPayoutForAction] = useState<{
    payout: PayoutRequest;
    action: 'approve' | 'reject';
  } | null>(null);

  const [selectedPayoutForReceipt, setSelectedPayoutForReceipt] = useState<PayoutRequest | null>(null);
  const [selectedDriverForAdjust, setSelectedDriverForAdjust] = useState<Driver | null>(null);
  const [isConfigModalOpen, setIsConfigModalOpen] = useState(false);

  // Financial aggregate calculations
  const completedTrips = useMemo(() => {
    return trips.filter((t) => t.status === 'completed');
  }, [trips]);

  const totalGrossFares = useMemo(() => {
    return completedTrips.reduce((acc, t) => acc + (t.fare?.total || 0), 0);
  }, [completedTrips]);

  const totalCommission = useMemo(() => {
    return completedTrips.reduce(
      (acc, t) => acc + (t.fare?.drigoCommissionAmount || (t.fare?.total || 0) * 0.18),
      0
    );
  }, [completedTrips]);

  const totalDriverWalletBalance = useMemo(() => {
    return drivers.reduce((acc, d) => acc + (d.walletBalance || 0), 0);
  }, [drivers]);

  const pendingRequests = useMemo(() => {
    return payoutRequests.filter((p) => p.status === 'pending');
  }, [payoutRequests]);

  const pendingAmount = useMemo(() => {
    return pendingRequests.reduce((acc, p) => acc + (p.amount || 0), 0);
  }, [pendingRequests]);

  // Handlers
  const handleConfirmPayoutAction = (
    payoutId: string,
    action: 'approve' | 'reject',
    transactionRef?: string,
    rejectionReason?: string
  ) => {
    if (action === 'approve') {
      onUpdatePayoutStatus(payoutId, 'processed', transactionRef);
    } else {
      onUpdatePayoutStatus(payoutId, 'rejected', undefined, rejectionReason);
    }
    setSelectedPayoutForAction(null);
  };

  const handleBatchSettle = (payoutIds: string[]) => {
    payoutIds.forEach((id) => {
      const p = payoutRequests.find((item) => item.id === id);
      const prefix = p?.paymentMethod ? p.paymentMethod.slice(0, 3).toUpperCase() : 'BATCH';
      const refCode = `${prefix}-${Date.now().toString().slice(-6)}-${Math.floor(100 + Math.random() * 900)}`;
      onUpdatePayoutStatus(id, 'processed', refCode);
    });
  };

  return (
    <div className="p-4 sm:p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50 min-h-full">
      {/* Top Header & Quick Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <div className="flex items-center space-x-2.5">
            <div className="w-10 h-10 rounded-2xl bg-blue-600 text-white flex items-center justify-center font-extrabold shadow-sm">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-xl font-extrabold text-slate-900 tracking-tight">
                Finance & Driver Payouts Hub
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Manage JazzCash & EasyPaisa withdrawals, audit platform commission, and manage driver wallet liabilities.
              </p>
            </div>
          </div>
        </div>

        {/* Header Action Buttons */}
        <div className="flex items-center space-x-2 self-start sm:self-auto">
          <button
            onClick={() => setIsConfigModalOpen(true)}
            className="inline-flex items-center space-x-1.5 px-3.5 py-2 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold rounded-xl border border-slate-200 shadow-2xs transition-colors cursor-pointer"
            title="Configure Pakistani Payout Gateways & Thresholds"
          >
            <Sliders className="w-3.5 h-3.5 text-slate-600" />
            <span>Payout Rules & Limits</span>
          </button>
        </div>
      </div>

      {/* Metric Cards Banner */}
      <FinanceStatCards
        totalCommission={totalCommission}
        pendingAmount={pendingAmount}
        pendingCount={pendingRequests.length}
        totalDriverWalletBalance={totalDriverWalletBalance}
        totalGrossFares={totalGrossFares}
        completedTripsCount={completedTrips.length}
        driversCount={drivers.length}
        timeframe={timeframe}
        onTimeframeChange={setTimeframe}
      />

      {/* Tabs Navigation */}
      <div className="flex border-b border-slate-200 space-x-2 sm:space-x-6 overflow-x-auto">
        <button
          onClick={() => setActiveTab('payout_requests')}
          className={`pb-3.5 text-xs font-extrabold transition-all border-b-2 flex items-center space-x-2 whitespace-nowrap cursor-pointer ${
            activeTab === 'payout_requests'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <CreditCard className="w-4 h-4" />
          <span>Withdrawal Payout Requests</span>
          {pendingRequests.length > 0 && (
            <span className="bg-amber-100 text-amber-800 text-[10px] font-extrabold px-2 py-0.5 rounded-full border border-amber-200">
              {pendingRequests.length} Pending
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveTab('driver_wallets')}
          className={`pb-3.5 text-xs font-extrabold transition-all border-b-2 flex items-center space-x-2 whitespace-nowrap cursor-pointer ${
            activeTab === 'driver_wallets'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Wallet className="w-4 h-4" />
          <span>Driver Wallet Balances & Ledger</span>
        </button>

        <button
          onClick={() => setActiveTab('commission_breakdown')}
          className={`pb-3.5 text-xs font-extrabold transition-all border-b-2 flex items-center space-x-2 whitespace-nowrap cursor-pointer ${
            activeTab === 'commission_breakdown'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <TrendingUp className="w-4 h-4" />
          <span>Commission & Revenue Audit</span>
        </button>
      </div>

      {/* Active Tab View */}
      {activeTab === 'payout_requests' && (
        <PayoutRequestsTable
          payoutRequests={payoutRequests}
          drivers={drivers}
          onOpenSettleModal={(p) => setSelectedPayoutForAction({ payout: p, action: 'approve' })}
          onOpenRejectModal={(p) => setSelectedPayoutForAction({ payout: p, action: 'reject' })}
          onOpenReceiptModal={(p) => setSelectedPayoutForReceipt(p)}
          onBatchSettle={handleBatchSettle}
        />
      )}

      {activeTab === 'driver_wallets' && (
        <DriverWalletsView
          drivers={drivers}
          trips={trips}
          payoutRequests={payoutRequests}
          onOpenAdjustModal={(d) => setSelectedDriverForAdjust(d)}
        />
      )}

      {activeTab === 'commission_breakdown' && (
        <CommissionAuditLedger trips={trips} drivers={drivers} />
      )}

      {/* Modal: Payout Settle or Reject Action */}
      {selectedPayoutForAction && (
        <PayoutActionModal
          payout={selectedPayoutForAction.payout}
          actionType={selectedPayoutForAction.action}
          onClose={() => setSelectedPayoutForAction(null)}
          onConfirm={handleConfirmPayoutAction}
        />
      )}

      {/* Modal: Printable Settlement Voucher / Receipt */}
      {selectedPayoutForReceipt && (
        <PayoutReceiptModal
          payout={selectedPayoutForReceipt}
          onClose={() => setSelectedPayoutForReceipt(null)}
        />
      )}

      {/* Modal: Payout Gateway Config & Settlement Limits */}
      {isConfigModalOpen && (
        <PayoutGatewayConfigModal
          onClose={() => setIsConfigModalOpen(false)}
        />
      )}

      {/* Modal: Manual Driver Wallet Balance Adjustment */}
      {selectedDriverForAdjust && (
        <AdjustWalletModal
          driver={selectedDriverForAdjust}
          onClose={() => setSelectedDriverForAdjust(null)}
          onConfirmAdjustment={onAdjustDriverWallet}
        />
      )}
    </div>
  );
};
