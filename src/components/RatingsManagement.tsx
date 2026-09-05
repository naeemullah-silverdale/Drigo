import React, { useState } from 'react';
import { RideRating, Driver, Rider, Trip } from '../types';
import {
  Star,
  Search,
  Filter,
  ShieldAlert,
  User,
  Car,
  CheckCircle2,
  AlertTriangle,
  MessageSquare,
  Tag,
  Clock,
  Eye,
  ArrowUpRight,
  TrendingUp,
  MapPin,
  DollarSign
} from 'lucide-react';

interface RatingsManagementProps {
  ratings: RideRating[];
  drivers: Driver[];
  riders: Rider[];
  trips: Trip[];
  onInspectUser?: (userId: string, role: 'driver' | 'rider') => void;
  onInspectTrip?: (tripId: string) => void;
}

export const RatingsManagement: React.FC<RatingsManagementProps> = ({
  ratings,
  drivers,
  riders,
  trips,
  onInspectUser,
  onInspectTrip,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<'all' | 'passenger_to_driver' | 'driver_to_passenger' | 'suspicious'>('all');
  const [selectedRating, setSelectedRating] = useState<RideRating | null>(null);

  // Helper lookups
  const getDriver = (id?: string) => drivers.find(d => d.id === id);
  const getRider = (id?: string) => riders.find(r => r.id === id);
  const getTrip = (id?: string) => trips.find(t => t.id === id || t.tripCode === id);

  // Normalize reviewer direction/type
  const getNormalizedType = (r: RideRating): 'passenger_to_driver' | 'driver_to_passenger' => {
    const typeStr = String(r.reviewerType || '').toLowerCase();
    if (typeStr.includes('passenger_to_driver') || typeStr.includes('passenger->driver')) return 'passenger_to_driver';
    if (typeStr.includes('driver_to_passenger') || typeStr.includes('driver->passenger')) return 'driver_to_passenger';

    const reviewerRole = (r as any).reviewerRole;
    if (reviewerRole) {
      const role = String(reviewerRole).toLowerCase();
      if (role.includes('driver')) return 'driver_to_passenger';
      if (role.includes('passenger') || role.includes('rider')) return 'passenger_to_driver';
    }

    if (r.reviewerId) {
      if (r.reviewerId.startsWith('drv')) return 'driver_to_passenger';
      if (r.reviewerId.startsWith('rdr') || r.reviewerId.startsWith('usr')) return 'passenger_to_driver';
    }

    if (r.reviewedUserId) {
      if (r.reviewedUserId.startsWith('drv')) return 'passenger_to_driver';
      if (r.reviewedUserId.startsWith('rdr') || r.reviewedUserId.startsWith('usr')) return 'driver_to_passenger';
    }

    return 'passenger_to_driver';
  };

  // Enriched ratings list
  const enrichedRatings = ratings.map(r => {
    const normType = getNormalizedType(r);
    const reviewerId = r.reviewerId || (normType === 'passenger_to_driver' ? r.tripId : '');
    const reviewedId = r.reviewedUserId || '';

    let reviewerName = r.reviewerName;
    let reviewerAvatar = r.reviewerAvatar;
    let reviewedName = r.reviewedUserName;

    if (normType === 'passenger_to_driver') {
      const rider = getRider(reviewerId);
      if (rider) {
        reviewerName = reviewerName || rider.fullName;
        reviewerAvatar = reviewerAvatar || rider.avatar;
      }
      const driver = getDriver(reviewedId);
      if (driver) {
        reviewedName = reviewedName || driver.fullName;
      }
    } else {
      const driver = getDriver(reviewerId);
      if (driver) {
        reviewerName = reviewerName || driver.fullName;
        reviewerAvatar = reviewerAvatar || driver.avatar;
      }
      const rider = getRider(reviewedId);
      if (rider) {
        reviewedName = reviewedName || rider.fullName;
      }
    }

    const trip = getTrip(r.tripId || r.tripCode);

    return {
      ...r,
      normalizedType: normType,
      reviewerName: reviewerName || (normType === 'passenger_to_driver' ? 'Passenger' : 'Driver'),
      reviewedUserName: reviewedName || (normType === 'passenger_to_driver' ? 'Driver' : 'Passenger'),
      tripCode: r.tripCode || trip?.tripCode || r.tripId || 'DRG-8924',
      linkedTrip: trip,
      linkedReviewerDriver: normType === 'driver_to_passenger' ? getDriver(reviewerId) : null,
      linkedReviewerRider: normType === 'passenger_to_driver' ? getRider(reviewerId) : null,
      linkedReviewedDriver: normType === 'passenger_to_driver' ? getDriver(reviewedId) : null,
      linkedReviewedRider: normType === 'driver_to_passenger' ? getRider(reviewedId) : null,
    };
  });

  // Calculate stats
  const totalReviewsCount = enrichedRatings.length;
  const validRatings = enrichedRatings.filter(r => typeof r.rating === 'number' && !isNaN(r.rating) && r.rating > 0);
  const overallAverageRating = validRatings.length > 0
    ? (validRatings.reduce((acc, r) => acc + r.rating, 0) / validRatings.length).toFixed(2)
    : '0.0';

  const p2dRatings = enrichedRatings.filter(r => r.normalizedType === 'passenger_to_driver');
  const d2pRatings = enrichedRatings.filter(r => r.normalizedType === 'driver_to_passenger');
  const suspiciousCount = enrichedRatings.filter(r => r.isSuspicious || r.rating <= 2).length;

  // Filtered ratings
  const getTags = (tags: any): string[] => {
    if (Array.isArray(tags)) return tags.map(t => String(t || '').trim()).filter(Boolean);
    if (typeof tags === 'string') return tags.split(',').map(t => t.trim()).filter(Boolean);
    if (tags && typeof tags === 'object') return Object.values(tags).map(t => String(t || '').trim()).filter(Boolean);
    return [];
  };

  const filteredRatings = enrichedRatings.filter((r) => {
    const q = (searchTerm || '').toLowerCase();
    const matchesSearch =
      (r.tripCode || '').toLowerCase().includes(q) ||
      (r.tripId || '').toLowerCase().includes(q) ||
      (r.reviewerName || '').toLowerCase().includes(q) ||
      (r.reviewedUserName || '').toLowerCase().includes(q) ||
      (r.reviewerId || '').toLowerCase().includes(q) ||
      (r.reviewedUserId || '').toLowerCase().includes(q) ||
      (r.comment || '').toLowerCase().includes(q);

    if (!matchesSearch) return false;

    if (typeFilter === 'passenger_to_driver') return r.normalizedType === 'passenger_to_driver';
    if (typeFilter === 'driver_to_passenger') return r.normalizedType === 'driver_to_passenger';
    if (typeFilter === 'suspicious') return r.isSuspicious || r.rating <= 2;
    return true;
  });

  return (
    <div className="p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50">
      {/* Header & Stats Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">Ratings & Reviews Management</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Monitor real-time passenger and driver reviews synced from <code className="text-blue-600 font-mono">/ride_ratings</code>.
          </p>
        </div>
        <div className="flex items-center flex-wrap gap-3">
          <div className="bg-amber-50 border border-amber-100 px-3.5 py-2 rounded-xl text-center">
            <div className="text-[10px] font-bold text-amber-600 uppercase tracking-wider">Overall Average</div>
            <div className="text-base font-black text-amber-900 flex items-center justify-center space-x-1">
              <span>★</span>
              <span>{overallAverageRating}</span>
            </div>
          </div>
          <div className="bg-blue-50 border border-blue-100 px-3.5 py-2 rounded-xl text-center">
            <div className="text-[10px] font-bold text-blue-600 uppercase tracking-wider">Total Reviews</div>
            <div className="text-base font-black text-blue-900">{totalReviewsCount}</div>
          </div>
          <div className="bg-indigo-50 border border-indigo-100 px-3.5 py-2 rounded-xl text-center">
            <div className="text-[10px] font-bold text-indigo-600 uppercase tracking-wider">Pass → Drv</div>
            <div className="text-base font-black text-indigo-900">{p2dRatings.length}</div>
          </div>
          <div className="bg-emerald-50 border border-emerald-100 px-3.5 py-2 rounded-xl text-center">
            <div className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider">Drv → Pass</div>
            <div className="text-base font-black text-emerald-900">{d2pRatings.length}</div>
          </div>
          <div className="bg-rose-50 border border-rose-100 px-3.5 py-2 rounded-xl text-center">
            <div className="text-[10px] font-bold text-rose-600 uppercase tracking-wider">Suspicious / Low</div>
            <div className="text-base font-black text-rose-900">{suspiciousCount}</div>
          </div>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by Trip ID, Reviewer, Reviewed User, User ID, or Comment..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs font-medium text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>

        {/* Type Filter Tabs */}
        <div className="flex items-center space-x-1.5 overflow-x-auto pb-1 md:pb-0">
          {[
            { id: 'all', label: 'All Reviews' },
            { id: 'passenger_to_driver', label: 'Passenger → Driver' },
            { id: 'driver_to_passenger', label: 'Driver → Passenger' },
            { id: 'suspicious', label: 'Suspicious / Low (≤2★)' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setTypeFilter(tab.id as any)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                typeFilter === tab.id
                  ? 'bg-slate-900 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Ratings Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                <th className="py-3.5 px-4">Ride ID</th>
                <th className="py-3.5 px-4">Direction</th>
                <th className="py-3.5 px-4">Reviewer</th>
                <th className="py-3.5 px-4">Reviewed User</th>
                <th className="py-3.5 px-4">Rating</th>
                <th className="py-3.5 px-4">Comment & Tags</th>
                <th className="py-3.5 px-4">Timestamp</th>
                <th className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs font-medium text-slate-700">
              {filteredRatings.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <Star className="w-8 h-8 mx-auto mb-2 opacity-30 text-slate-400" />
                    No reviews found matching the current filter.
                  </td>
                </tr>
              ) : (
                filteredRatings.map((r) => {
                  return (
                    <tr
                      key={r.id}
                      onClick={() => setSelectedRating(r)}
                      className="hover:bg-slate-50/80 transition-colors cursor-pointer group"
                    >
                      <td className="py-3.5 px-4 font-mono font-bold text-slate-900">
                        {r.tripCode}
                      </td>
                      <td className="py-3.5 px-4">
                        {r.normalizedType === 'passenger_to_driver' ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                            Passenger → Driver
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            Driver → Passenger
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-900">{r.reviewerName}</div>
                        <div className="text-[10px] font-mono text-slate-400">{r.reviewerId || 'N/A'}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-800">{r.reviewedUserName}</div>
                        <div className="text-[10px] font-mono text-slate-400">{r.reviewedUserId || 'N/A'}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="flex items-center space-x-1 text-amber-500 font-black">
                          <span>{'★'.repeat(Math.min(5, Math.max(1, r.rating)))}</span>
                          <span className="text-slate-400 font-normal">({r.rating}.0)</span>
                          {(r.isSuspicious || r.rating <= 2) && (
                            <span className="ml-1 px-1.5 py-0.2 bg-rose-100 text-rose-700 rounded text-[9px] font-bold">
                              {r.rating <= 2 ? 'LOW' : 'SUSPICIOUS'}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="py-3.5 px-4 max-w-xs">
                        <div className="truncate text-slate-800 font-medium" title={r.comment}>
                          "{r.comment || 'No comment provided.'}"
                        </div>
                        <div className="flex flex-wrap gap-1 mt-1">
                          {getTags(r.tags).map((tag, i) => (
                            <span key={i} className="text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded font-medium">
                              #{tag}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="py-3.5 px-4 font-mono text-[11px] text-slate-500">
                        {r.timestamp || 'Recent'}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedRating(r);
                          }}
                          className="p-1.5 bg-slate-100 hover:bg-blue-600 hover:text-white rounded-lg text-slate-600 transition-colors inline-flex items-center space-x-1 text-xs font-semibold"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Inspect</span>
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Rich Detailed Inspection Modal */}
      {selectedRating && (() => {
        const normType = getNormalizedType(selectedRating);
        const trip = getTrip(selectedRating.tripId || selectedRating.tripCode);
        const reviewerDriver = normType === 'driver_to_passenger' ? getDriver(selectedRating.reviewerId) : null;
        const reviewerRider = normType === 'passenger_to_driver' ? getRider(selectedRating.reviewerId) : null;
        const reviewedDriver = normType === 'passenger_to_driver' ? getDriver(selectedRating.reviewedUserId) : null;
        const reviewedRider = normType === 'driver_to_passenger' ? getRider(selectedRating.reviewedUserId) : null;

        return (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto animate-fadeIn">
            <div className="bg-white rounded-2xl max-w-2xl w-full overflow-hidden shadow-2xl border border-slate-200">
              {/* Modal Header */}
              <div className="p-6 border-b border-slate-200 flex items-center justify-between bg-slate-900 text-white">
                <div className="flex items-center space-x-3">
                  <div className="p-2.5 bg-amber-500 rounded-xl">
                    <Star className="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="font-mono font-extrabold text-base">Review Details #{selectedRating.id}</span>
                      <span className="text-xs bg-slate-800 text-slate-300 px-2 py-0.5 rounded font-mono">
                        {selectedRating.tripCode || trip?.tripCode || 'DRG-8924'}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-0.5">Comprehensive audit & ride telemetry record</p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedRating(null)}
                  className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center justify-center font-bold text-lg"
                >
                  ×
                </button>
              </div>

              {/* Modal Body */}
              <div className="p-6 space-y-5 text-xs max-h-[80vh] overflow-y-auto">
                {/* Reviewer & Reviewed Profiles */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-200">
                  <div className="space-y-1">
                    <div className="text-[10px] font-bold text-slate-400 uppercase flex items-center gap-1">
                      <User className="w-3 h-3 text-blue-600" />
                      Reviewer ({normType === 'passenger_to_driver' ? 'Passenger' : 'Driver'})
                    </div>
                    <div className="font-extrabold text-slate-900 text-sm">
                      {selectedRating.reviewerName}
                    </div>
                    <div className="text-slate-500 text-[11px] font-mono">ID: {selectedRating.reviewerId || 'N/A'}</div>
                    {(reviewerDriver || reviewerRider) && (
                      <div className="text-slate-600 text-[11px] pt-1">
                        Phone: {reviewerDriver?.phone || reviewerRider?.phone || 'N/A'} | Rating: ★{reviewerDriver?.rating || reviewerRider?.rating || '5.0'}
                      </div>
                    )}
                  </div>

                  <div className="space-y-1 md:border-l md:border-slate-200 md:pl-4">
                    <div className="text-[10px] font-bold text-slate-400 uppercase flex items-center gap-1">
                      <User className="w-3 h-3 text-emerald-600" />
                      Reviewed User ({normType === 'passenger_to_driver' ? 'Driver' : 'Passenger'})
                    </div>
                    <div className="font-extrabold text-slate-900 text-sm">
                      {selectedRating.reviewedUserName}
                    </div>
                    <div className="text-slate-500 text-[11px] font-mono">ID: {selectedRating.reviewedUserId || 'N/A'}</div>
                    {(reviewedDriver || reviewedRider) && (
                      <div className="text-slate-600 text-[11px] pt-1">
                        Phone: {reviewedDriver?.phone || reviewedRider?.phone || 'N/A'} | Rating: ★{reviewedDriver?.rating || reviewedRider?.rating || '5.0'}
                      </div>
                    )}
                  </div>
                </div>

                {/* Ride Context / Telemetry */}
                <div className="bg-blue-50/60 p-4 rounded-xl border border-blue-200 space-y-2">
                  <div className="font-bold text-blue-900 flex items-center gap-1.5">
                    <Car className="w-4 h-4 text-blue-600" />
                    Ride Context & Telemetry
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-blue-900">
                    <div><span className="font-semibold text-slate-600">Ride ID:</span> {trip?.tripCode || selectedRating.tripCode}</div>
                    <div><span className="font-semibold text-slate-600">Ride Type:</span> {trip?.vehicleType || 'Standard Sedan'}</div>
                    <div><span className="font-semibold text-slate-600">Pickup:</span> {trip?.fromLocation?.name || 'City Center'}</div>
                    <div><span className="font-semibold text-slate-600">Destination:</span> {trip?.toLocation?.name || 'Airport Terminal'}</div>
                    <div><span className="font-semibold text-slate-600">Agreed Fare:</span> ${trip?.fare?.total ?? '18.50'}</div>
                    <div><span className="font-semibold text-slate-600">Vehicle:</span> {reviewedDriver?.vehicle ? `${reviewedDriver.vehicle.make} ${reviewedDriver.vehicle.model} (${reviewedDriver.vehicle.licensePlate})` : (trip?.vehiclePlate || 'Suzuki Cultus (LHR-789)')}</div>
                  </div>
                </div>

                {/* Rating & Tags */}
                <div className="bg-white p-4 rounded-xl border border-slate-200 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-600 uppercase tracking-wider text-[11px]">Rating Score</span>
                    <div className="text-amber-500 font-black text-base flex items-center space-x-1">
                      <span>{'★'.repeat(Math.min(5, Math.max(1, selectedRating.rating)))}</span>
                      <span>({selectedRating.rating}.0 / 5.0)</span>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-1.5 pt-2 border-t border-slate-100">
                    {getTags(selectedRating.tags).map((tag, i) => (
                      <span key={i} className="bg-blue-50 text-blue-700 px-2.5 py-1 rounded-lg font-semibold text-xs">
                        #{tag}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Original Review Comment */}
                <div className="bg-amber-50/60 p-4 rounded-xl border border-amber-200 space-y-1.5">
                  <div className="text-[10px] font-bold text-amber-900 uppercase">Written Feedback Comment</div>
                  <div className="text-slate-800 font-medium italic text-sm">
                    "{selectedRating.comment || 'No comment provided.'}"
                  </div>
                  <div className="text-[10px] text-slate-400 pt-1 font-mono flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    Timestamp: {selectedRating.timestamp || trip?.requestedAt || 'Recent'}
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center justify-end space-x-3 pt-2">
                  {onInspectTrip && (
                    <button
                      onClick={() => {
                        onInspectTrip(selectedRating.tripId || trip?.id || '');
                        setSelectedRating(null);
                      }}
                      className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-xl font-bold flex items-center space-x-1.5"
                    >
                      <span>View Ride History</span>
                      <ArrowUpRight className="w-3.5 h-3.5" />
                    </button>
                  )}
                  {onInspectUser && (
                    <button
                      onClick={() => {
                        const targetUserId = selectedRating.reviewedUserId;
                        const targetRole = targetUserId?.startsWith('drv') ? 'driver' : 'rider';
                        if (targetUserId && onInspectUser) {
                          onInspectUser(targetUserId, targetRole);
                          setSelectedRating(null);
                        }
                      }}
                      className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold"
                    >
                      Inspect User Profile
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        );
      })()}
    </div>
  );
};
