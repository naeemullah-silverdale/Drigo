import React, { useState, useMemo } from 'react';
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
  DollarSign,
  Download,
  ThumbsUp,
  ThumbsDown,
  Shield,
  ShieldCheck,
  Flag,
  Trash2,
  Check,
  X,
  RefreshCw,
  LayoutGrid,
  List,
  Sparkles,
  Phone,
  HelpCircle,
  AlertCircle
} from 'lucide-react';

interface RatingsManagementProps {
  ratings: RideRating[];
  drivers: Driver[];
  riders: Rider[];
  trips: Trip[];
  onInspectUser?: (userId: string, role: 'driver' | 'rider') => void;
  onInspectTrip?: (tripId: string) => void;
  onUpdateRating?: (ratingId: string, updates: Partial<RideRating>) => Promise<void> | void;
  onDeleteRating?: (ratingId: string) => Promise<void> | void;
}

export const RatingsManagement: React.FC<RatingsManagementProps> = ({
  ratings,
  drivers,
  riders,
  trips,
  onInspectUser,
  onInspectTrip,
  onUpdateRating,
  onDeleteRating,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<'all' | 'passenger_to_driver' | 'driver_to_passenger' | 'suspicious' | 'with_comments' | 'flagged'>('all');
  const [starFilter, setStarFilter] = useState<number | 'all'>('all');
  const [selectedTag, setSelectedTag] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<'newest' | 'oldest' | 'highest' | 'lowest'>('newest');
  const [cityFilter, setCityFilter] = useState<string>('all');
  const [viewMode, setViewMode] = useState<'table' | 'cards'>('table');
  const [selectedRating, setSelectedRating] = useState<RideRating | null>(null);

  // Moderation state within modal
  const [modIsSuspicious, setModIsSuspicious] = useState(false);
  const [modFlagReason, setModFlagReason] = useState('');
  const [modStatus, setModStatus] = useState<'published' | 'flagged' | 'hidden' | 'investigating' | 'resolved'>('published');
  const [modAdminNote, setModAdminNote] = useState('');
  const [isSavingMod, setIsSavingMod] = useState(false);
  const [actionSuccessMsg, setActionSuccessMsg] = useState<string | null>(null);

  // Helper lookups
  const getDriver = (id?: string) => drivers.find(d => d.id === id);
  const getRider = (id?: string) => riders.find(r => r.id === id);
  const getTrip = (id?: string) => trips.find(t => t.id === id || t.tripCode === id);

  // Safe timestamp parser & formatter to handle number, string, or object timestamps
  const parseTimestampToMs = (ts: any): number => {
    if (!ts) return 0;
    if (typeof ts === 'number') return ts;
    if (typeof ts === 'object' && typeof ts.seconds === 'number') {
      return ts.seconds * 1000;
    }
    const str = String(ts);
    const parsed = Date.parse(str);
    if (!isNaN(parsed)) return parsed;
    return 0;
  };

  const formatTimestampDisplay = (ts: any): string => {
    if (!ts) return 'Recent';
    try {
      if (typeof ts === 'number') {
        const d = new Date(ts);
        if (!isNaN(d.getTime())) {
          return d.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          });
        }
      }
      if (typeof ts === 'object' && typeof ts.seconds === 'number') {
        const d = new Date(ts.seconds * 1000);
        if (!isNaN(d.getTime())) {
          return d.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          });
        }
      }
      if (typeof ts === 'string') {
        const d = new Date(ts);
        if (!isNaN(d.getTime())) {
          return d.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          });
        }
      }
      return String(ts);
    } catch {
      return 'Recent';
    }
  };

  // Helper: Normalize reviewer direction/type
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

  // Helper: Extract safe array of tags
  const getTags = (tags: any): string[] => {
    if (Array.isArray(tags)) return tags.map(t => String(t || '').trim()).filter(Boolean);
    if (typeof tags === 'string') return tags.split(',').map(t => t.trim()).filter(Boolean);
    if (tags && typeof tags === 'object') return Object.values(tags).map(t => String(t || '').trim()).filter(Boolean);
    return [];
  };

  // Enriched ratings list
  const enrichedRatings = useMemo(() => {
    return ratings.map(r => {
      const normType = getNormalizedType(r);
      const reviewerId = r.reviewerId || (normType === 'passenger_to_driver' ? r.tripId : '');
      const reviewedId = r.reviewedUserId || '';

      let reviewerName = r.reviewerName;
      let reviewerAvatar = r.reviewerAvatar;
      let reviewedName = r.reviewedUserName;

      const driverReviewer = normType === 'driver_to_passenger' ? getDriver(reviewerId) : null;
      const riderReviewer = normType === 'passenger_to_driver' ? getRider(reviewerId) : null;
      const driverReviewed = normType === 'passenger_to_driver' ? getDriver(reviewedId) : null;
      const riderReviewed = normType === 'driver_to_passenger' ? getRider(reviewedId) : null;

      if (normType === 'passenger_to_driver') {
        if (riderReviewer) {
          reviewerName = reviewerName || riderReviewer.fullName;
          reviewerAvatar = reviewerAvatar || riderReviewer.avatar;
        }
        if (driverReviewed) {
          reviewedName = reviewedName || driverReviewed.fullName;
        }
      } else {
        if (driverReviewer) {
          reviewerName = reviewerName || driverReviewer.fullName;
          reviewerAvatar = reviewerAvatar || driverReviewer.avatar;
        }
        if (riderReviewed) {
          reviewedName = reviewedName || riderReviewed.fullName;
        }
      }

      const trip = getTrip(r.tripId || r.tripCode);
      const tripCity = trip?.fromLocation?.address?.split(',').pop()?.trim() || trip?.fromLocation?.name || r.city || 'Peshawar';

      return {
        ...r,
        normalizedType: normType,
        reviewerName: reviewerName || (normType === 'passenger_to_driver' ? 'Passenger' : 'Driver'),
        reviewerAvatar: reviewerAvatar,
        reviewedUserName: reviewedName || (normType === 'passenger_to_driver' ? 'Driver' : 'Passenger'),
        tripCode: r.tripCode || trip?.tripCode || r.tripId || 'DRG-8924',
        linkedTrip: trip,
        linkedCity: tripCity,
        linkedReviewerDriver: driverReviewer,
        linkedReviewerRider: riderReviewer,
        linkedReviewedDriver: driverReviewed,
        linkedReviewedRider: riderReviewed,
        isSuspicious: r.isSuspicious || r.rating <= 2 || r.moderationStatus === 'flagged',
        moderationStatus: r.moderationStatus || (r.rating <= 2 ? 'flagged' : 'published'),
        parsedTags: getTags(r.tags)
      };
    });
  }, [ratings, drivers, riders, trips]);

  // Comprehensive analytics
  const stats = useMemo(() => {
    const total = enrichedRatings.length;
    const valid = enrichedRatings.filter(r => typeof r.rating === 'number' && !isNaN(r.rating) && r.rating > 0);
    const avgScore = valid.length > 0 ? valid.reduce((acc, r) => acc + r.rating, 0) / valid.length : 5.0;

    const p2d = enrichedRatings.filter(r => r.normalizedType === 'passenger_to_driver');
    const p2dValid = p2d.filter(r => typeof r.rating === 'number' && r.rating > 0);
    const p2dAvg = p2dValid.length > 0 ? (p2dValid.reduce((acc, r) => acc + r.rating, 0) / p2dValid.length).toFixed(2) : '5.0';

    const d2p = enrichedRatings.filter(r => r.normalizedType === 'driver_to_passenger');
    const d2pValid = d2p.filter(r => typeof r.rating === 'number' && r.rating > 0);
    const d2pAvg = d2pValid.length > 0 ? (d2pValid.reduce((acc, r) => acc + r.rating, 0) / d2pValid.length).toFixed(2) : '5.0';

    const starCounts = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    valid.forEach(r => {
      const rounded = Math.min(5, Math.max(1, Math.round(r.rating))) as 1 | 2 | 3 | 4 | 5;
      starCounts[rounded]++;
    });

    const highRatingsCount = (starCounts[5] || 0) + (starCounts[4] || 0);
    const satisfactionRate = total > 0 ? Math.round((highRatingsCount / total) * 100) : 100;
    const suspiciousCount = enrichedRatings.filter(r => r.isSuspicious || r.rating <= 2).length;
    const flaggedCount = enrichedRatings.filter(r => r.moderationStatus === 'flagged' || r.moderationStatus === 'investigating').length;

    // Aggregate tags
    const tagFrequencies: Record<string, { count: number; isNegative: boolean }> = {};
    const negativeKeywords = ['reckless', 'unsafe', 'speeding', 'late', 'rude', 'dirty', 'deviation', 'dispute', 'overcharg', 'issue', 'argument', 'poor', 'bad'];

    enrichedRatings.forEach(r => {
      r.parsedTags.forEach(tag => {
        const clean = tag.trim();
        if (!clean) return;
        const isNeg = negativeKeywords.some(kw => clean.toLowerCase().includes(kw)) || r.rating <= 2;
        if (!tagFrequencies[clean]) {
          tagFrequencies[clean] = { count: 0, isNegative: isNeg };
        }
        tagFrequencies[clean].count++;
      });
    });

    const sortedTags = Object.entries(tagFrequencies)
      .sort((a, b) => b[1].count - a[1].count)
      .slice(0, 12);

    return {
      total,
      avgScore: avgScore.toFixed(2),
      p2dCount: p2d.length,
      p2dAvg,
      d2pCount: d2p.length,
      d2pAvg,
      starCounts,
      satisfactionRate,
      suspiciousCount,
      flaggedCount,
      sortedTags
    };
  }, [enrichedRatings]);

  // Filter & Sort
  const filteredRatings = useMemo(() => {
    let list = enrichedRatings.filter(r => {
      const q = (searchTerm || '').toLowerCase();
      const matchesSearch =
        !q ||
        (r.tripCode || '').toLowerCase().includes(q) ||
        (r.tripId || '').toLowerCase().includes(q) ||
        (r.reviewerName || '').toLowerCase().includes(q) ||
        (r.reviewedUserName || '').toLowerCase().includes(q) ||
        (r.reviewerId || '').toLowerCase().includes(q) ||
        (r.reviewedUserId || '').toLowerCase().includes(q) ||
        (r.comment || '').toLowerCase().includes(q) ||
        (r.vehicleType || '').toLowerCase().includes(q) ||
        r.parsedTags.some(t => t.toLowerCase().includes(q));

      if (!matchesSearch) return false;

      // Type Filter
      if (typeFilter === 'passenger_to_driver' && r.normalizedType !== 'passenger_to_driver') return false;
      if (typeFilter === 'driver_to_passenger' && r.normalizedType !== 'driver_to_passenger') return false;
      if (typeFilter === 'suspicious' && !r.isSuspicious && r.rating > 2) return false;
      if (typeFilter === 'flagged' && r.moderationStatus !== 'flagged' && r.moderationStatus !== 'investigating') return false;
      if (typeFilter === 'with_comments' && (!r.comment || r.comment.trim().length === 0)) return false;

      // Star Filter
      if (starFilter !== 'all' && Math.round(r.rating) !== starFilter) return false;

      // Tag Filter
      if (selectedTag && !r.parsedTags.includes(selectedTag)) return false;

      // City Filter
      if (cityFilter !== 'all' && String(r.linkedCity || '').toLowerCase() !== String(cityFilter || '').toLowerCase()) return false;

      return true;
    });

    // Sorting
    list.sort((a, b) => {
      if (sortBy === 'newest') return parseTimestampToMs(b.timestamp) - parseTimestampToMs(a.timestamp);
      if (sortBy === 'oldest') return parseTimestampToMs(a.timestamp) - parseTimestampToMs(b.timestamp);
      if (sortBy === 'highest') return b.rating - a.rating;
      if (sortBy === 'lowest') return a.rating - b.rating;
      return 0;
    });

    return list;
  }, [enrichedRatings, searchTerm, typeFilter, starFilter, selectedTag, cityFilter, sortBy]);

  // Open Modal & sync local moderation form
  const handleOpenInspection = (rating: RideRating) => {
    setSelectedRating(rating);
    setModIsSuspicious(!!rating.isSuspicious || rating.rating <= 2);
    setModFlagReason(rating.flagReason || (rating.rating <= 2 ? 'Low rating review flagged for audit' : ''));
    setModStatus(rating.moderationStatus || (rating.rating <= 2 ? 'flagged' : 'published'));
    setModAdminNote(rating.adminNote || '');
    setActionSuccessMsg(null);
  };

  // Save Moderation changes
  const handleSaveModeration = async () => {
    if (!selectedRating) return;
    setIsSavingMod(true);
    const updates: Partial<RideRating> = {
      isSuspicious: modIsSuspicious,
      flagReason: modFlagReason,
      moderationStatus: modStatus,
      adminNote: modAdminNote,
      moderatedBy: 'Admin (Safety & Quality)',
      moderatedAt: new Date().toISOString()
    };

    try {
      if (onUpdateRating) {
        await onUpdateRating(selectedRating.id, updates);
      }
      setSelectedRating(prev => prev ? { ...prev, ...updates } : null);
      setActionSuccessMsg('Moderation status and admin notes saved successfully.');
      setTimeout(() => setActionSuccessMsg(null), 3500);
    } catch (err) {
      console.error('Failed to update rating:', err);
    } finally {
      setIsSavingMod(false);
    }
  };

  // Export filtered reviews to CSV
  const handleExportCSV = () => {
    const headers = ['Review ID', 'Trip Code', 'Direction', 'Reviewer Name', 'Reviewer ID', 'Reviewed User', 'Reviewed ID', 'Rating', 'Comment', 'Tags', 'Moderation Status', 'Timestamp'];
    const rows = filteredRatings.map(r => [
      `"${r.id}"`,
      `"${r.tripCode}"`,
      `"${r.normalizedType === 'passenger_to_driver' ? 'Passenger -> Driver' : 'Driver -> Passenger'}"`,
      `"${r.reviewerName}"`,
      `"${r.reviewerId || ''}"`,
      `"${r.reviewedUserName}"`,
      `"${r.reviewedUserId || ''}"`,
      r.rating,
      `"${(r.comment || '').replace(/"/g, '""')}"`,
      `"${r.parsedTags.join(', ')}"`,
      `"${r.moderationStatus || 'published'}"`,
      `"${formatTimestampDisplay(r.timestamp)}"`
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `drigo_ratings_reviews_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="p-4 sm:p-6 space-y-6 flex-1 overflow-y-auto bg-slate-50 min-h-screen">
      {/* Top Banner: Header, Live Connection, & CSV Export */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 bg-white p-5 sm:p-6 rounded-2xl border border-slate-200/80 shadow-sm">
        <div className="space-y-1">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 bg-amber-500/10 text-amber-600 rounded-xl">
              <Star className="w-5 h-5 fill-amber-500 text-amber-500" />
            </div>
            <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
              Ratings & Reviews Management
            </h1>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Live Firebase Sync
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 max-w-2xl">
            Real-time two-way feedback engine for Drigo passengers & drivers across Peshawar, Islamabad, Rawalpindi, and Lahore.
          </p>
        </div>

        <div className="flex items-center flex-wrap gap-2.5">
          <button
            onClick={handleExportCSV}
            className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-colors inline-flex items-center gap-1.5 shadow-sm active:scale-95"
          >
            <Download className="w-3.5 h-3.5 text-slate-600" />
            <span>Export CSV</span>
          </button>
          <div className="flex items-center bg-slate-100 p-1 rounded-xl border border-slate-200">
            <button
              onClick={() => setViewMode('table')}
              className={`p-1.5 rounded-lg text-xs font-bold transition-all ${
                viewMode === 'table' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
              }`}
              title="Table View"
            >
              <List className="w-4 h-4" />
            </button>
            <button
              onClick={() => setViewMode('cards')}
              className={`p-1.5 rounded-lg text-xs font-bold transition-all ${
                viewMode === 'cards' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
              }`}
              title="Card Grid View"
            >
              <LayoutGrid className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Analytics KPI Row & Interactive Star Breakdown */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Overall Average */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Fleet Quality Score</span>
            <div className="p-2 bg-amber-50 rounded-xl text-amber-600">
              <Star className="w-4 h-4 fill-amber-500 text-amber-500" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline space-x-2">
            <span className="text-3xl font-black text-slate-900 tracking-tight">{stats.avgScore}</span>
            <span className="text-xs font-bold text-amber-600">/ 5.0</span>
            <span className="text-[11px] font-semibold text-slate-400">({stats.total} total reviews)</span>
          </div>
          <div className="mt-2.5 flex items-center space-x-1 text-amber-500">
            {[1, 2, 3, 4, 5].map(star => (
              <Star
                key={star}
                className={`w-3.5 h-3.5 ${
                  star <= Math.round(Number(stats.avgScore)) ? 'fill-amber-400 text-amber-400' : 'text-slate-200'
                }`}
              />
            ))}
            <span className="text-[11px] font-bold text-slate-500 ml-1.5">{stats.satisfactionRate}% Positive</span>
          </div>
        </div>

        {/* KPI 2: Passenger -> Driver Score */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Pass → Driver Score</span>
            <div className="p-2 bg-blue-50 rounded-xl text-blue-600">
              <Car className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline space-x-2">
            <span className="text-3xl font-black text-blue-900 tracking-tight">{stats.p2dAvg}</span>
            <span className="text-xs font-bold text-blue-600">★</span>
            <span className="text-[11px] font-semibold text-slate-400">({stats.p2dCount} reviews)</span>
          </div>
          <div className="mt-2.5 text-xs text-slate-500 flex items-center gap-1">
            <ThumbsUp className="w-3.5 h-3.5 text-blue-500" />
            <span>Driver professionalism & cleanliness</span>
          </div>
        </div>

        {/* KPI 3: Driver -> Passenger Score */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Driver → Pass Conduct</span>
            <div className="p-2 bg-emerald-50 rounded-xl text-emerald-600">
              <User className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline space-x-2">
            <span className="text-3xl font-black text-emerald-900 tracking-tight">{stats.d2pAvg}</span>
            <span className="text-xs font-bold text-emerald-600">★</span>
            <span className="text-[11px] font-semibold text-slate-400">({stats.d2pCount} reviews)</span>
          </div>
          <div className="mt-2.5 text-xs text-slate-500 flex items-center gap-1">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
            <span>Punctuality & fair passenger conduct</span>
          </div>
        </div>

        {/* KPI 4: Moderation & Flagged Issues */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Flagged & Critical</span>
            <div className="p-2 bg-rose-50 rounded-xl text-rose-600">
              <ShieldAlert className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline space-x-2">
            <span className="text-3xl font-black text-rose-900 tracking-tight">{stats.suspiciousCount}</span>
            <span className="text-xs font-bold text-rose-600">cases</span>
            <span className="text-[11px] font-semibold text-slate-400">({stats.flaggedCount} active flags)</span>
          </div>
          <div className="mt-2.5 text-xs text-rose-700 flex items-center gap-1 font-semibold">
            <AlertTriangle className="w-3.5 h-3.5 text-rose-500" />
            <span>Low rating (≤2★) or flagged feedback</span>
          </div>
        </div>
      </div>

      {/* Interactive Star Distribution & Top Attribute Tags */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Star Rating Distribution Card (Interactive Filter) */}
        <div className="lg:col-span-6 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
              <Sparkles className="w-4 h-4 text-amber-500" />
              Rating Score Breakdown
            </h2>
            {starFilter !== 'all' && (
              <button
                onClick={() => setStarFilter('all')}
                className="text-xs font-bold text-blue-600 hover:text-blue-800 transition-colors"
              >
                Clear Star Filter
              </button>
            )}
          </div>

          <div className="space-y-2 pt-1">
            {[5, 4, 3, 2, 1].map((starNum) => {
              const count = stats.starCounts[starNum as 1 | 2 | 3 | 4 | 5] || 0;
              const percent = stats.total > 0 ? Math.round((count / stats.total) * 100) : 0;
              const isSelected = starFilter === starNum;

              return (
                <button
                  key={starNum}
                  onClick={() => setStarFilter(isSelected ? 'all' : starNum)}
                  className={`w-full flex items-center space-x-3 p-1.5 rounded-xl transition-all text-left ${
                    isSelected ? 'bg-amber-50/80 ring-1 ring-amber-400' : 'hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center space-x-1 w-12 text-xs font-bold text-slate-700">
                    <span>{starNum}</span>
                    <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                  </div>
                  <div className="flex-1 bg-slate-100 rounded-full h-2.5 overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${
                        starNum >= 4 ? 'bg-amber-500' : starNum === 3 ? 'bg-blue-400' : 'bg-rose-500'
                      }`}
                      style={{ width: `${percent}%` }}
                    />
                  </div>
                  <div className="w-16 text-right text-xs font-mono font-bold text-slate-600">
                    {count} <span className="text-[10px] font-normal text-slate-400">({percent}%)</span>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Popular Tags Cloud */}
        <div className="lg:col-span-6 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
              <Tag className="w-4 h-4 text-blue-600" />
              Frequently Tagged Attributes
            </h2>
            {selectedTag && (
              <button
                onClick={() => setSelectedTag(null)}
                className="text-xs font-bold text-blue-600 hover:text-blue-800 transition-colors"
              >
                Reset Tag Filter
              </button>
            )}
          </div>

          <p className="text-xs text-slate-500">
            Click any tag to filter reviews instantly by passenger praise or driver conduct flags.
          </p>

          <div className="flex flex-wrap gap-2 pt-1">
            {stats.sortedTags.map(([tag, { count, isNegative }]) => {
              const isSelected = selectedTag === tag;
              return (
                <button
                  key={tag}
                  onClick={() => setSelectedTag(isSelected ? null : tag)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all inline-flex items-center gap-1.5 active:scale-95 ${
                    isSelected
                      ? 'bg-slate-900 text-white shadow-md'
                      : isNegative
                      ? 'bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-200/60'
                  }`}
                >
                  <span>#{tag}</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-black ${
                    isSelected ? 'bg-slate-700 text-white' : isNegative ? 'bg-rose-200 text-rose-900' : 'bg-slate-200 text-slate-700'
                  }`}>
                    {count}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Advanced Filter, Search, & Sort Suite */}
      <div className="bg-white p-4 sm:p-5 rounded-2xl border border-slate-200/80 shadow-sm space-y-4">
        {/* Search & Sort Controls */}
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-xl">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by Trip Code, Passenger, Driver, Vehicle, ID, or Feedback..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-9 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
            />
            {searchTerm && (
              <button
                onClick={() => setSearchTerm('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <div className="flex items-center flex-wrap gap-2.5">
            {/* City Filter */}
            <select
              value={cityFilter}
              onChange={(e) => setCityFilter(e.target.value)}
              className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:border-blue-500"
            >
              <option value="all">All Cities</option>
              <option value="peshawar">Peshawar</option>
              <option value="islamabad">Islamabad</option>
              <option value="rawalpindi">Rawalpindi</option>
              <option value="lahore">Lahore</option>
            </select>

            {/* Sort Dropdown */}
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:border-blue-500"
            >
              <option value="newest">Sort: Newest First</option>
              <option value="oldest">Sort: Oldest First</option>
              <option value="highest">Sort: Highest Rating (5★)</option>
              <option value="lowest">Sort: Lowest Rating (1★)</option>
            </select>
          </div>
        </div>

        {/* Direction Tabs & Status Pills */}
        <div className="flex items-center space-x-1.5 overflow-x-auto pb-1 no-scrollbar border-t border-slate-100 pt-3">
          {[
            { id: 'all', label: `All Reviews (${stats.total})` },
            { id: 'passenger_to_driver', label: `Passenger → Driver (${stats.p2dCount})` },
            { id: 'driver_to_passenger', label: `Driver → Passenger (${stats.d2pCount})` },
            { id: 'suspicious', label: `Low & Suspicious ≤2★ (${stats.suspiciousCount})` },
            { id: 'flagged', label: `Moderation Required (${stats.flaggedCount})` },
            { id: 'with_comments', label: 'With Feedback Only' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setTypeFilter(tab.id as any)}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all active:scale-95 ${
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

      {/* Main Reviews Content: Table View vs Cards View */}
      {viewMode === 'table' ? (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/90 border-b border-slate-200 text-[11px] font-black text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Ride ID</th>
                  <th className="py-3.5 px-4">Flow / Direction</th>
                  <th className="py-3.5 px-4">Reviewer</th>
                  <th className="py-3.5 px-4">Reviewed User</th>
                  <th className="py-3.5 px-4">Score</th>
                  <th className="py-3.5 px-4">Written Feedback & Tags</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs font-medium text-slate-700">
                {filteredRatings.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-16 text-center text-slate-400">
                      <Star className="w-10 h-10 mx-auto mb-2 text-slate-300 stroke-[1.5]" />
                      <div className="font-bold text-slate-700 text-sm">No reviews found</div>
                      <div className="text-xs text-slate-400 mt-0.5">Try clearing your search query or adjusting your filters.</div>
                    </td>
                  </tr>
                ) : (
                  filteredRatings.map((r) => {
                    const isLow = r.rating <= 2;
                    const isFlagged = r.moderationStatus === 'flagged' || r.isSuspicious;

                    return (
                      <tr
                        key={r.id}
                        onClick={() => handleOpenInspection(r)}
                        className={`hover:bg-slate-50/90 transition-colors cursor-pointer group ${
                          isFlagged ? 'bg-rose-50/30' : ''
                        }`}
                      >
                        {/* Ride Code */}
                        <td className="py-3.5 px-4 font-mono font-black text-slate-900">
                          <span className="group-hover:text-blue-600 transition-colors">
                            {r.tripCode}
                          </span>
                        </td>

                        {/* Direction */}
                        <td className="py-3.5 px-4">
                          {r.normalizedType === 'passenger_to_driver' ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                              <Car className="w-3 h-3" />
                              Pass → Driver
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                              <User className="w-3 h-3" />
                              Driver → Pass
                            </span>
                          )}
                        </td>

                        {/* Reviewer */}
                        <td className="py-3.5 px-4">
                          <div className="flex items-center space-x-2">
                            {r.reviewerAvatar ? (
                              <img src={r.reviewerAvatar} alt="" className="w-6 h-6 rounded-full object-cover" />
                            ) : (
                              <div className="w-6 h-6 rounded-full bg-slate-200 flex items-center justify-center font-bold text-[10px] text-slate-700">
                                {r.reviewerName.charAt(0)}
                              </div>
                            )}
                            <div>
                              <div className="font-bold text-slate-900 leading-tight">{r.reviewerName}</div>
                              <div className="text-[10px] font-mono text-slate-400">{r.reviewerId || 'N/A'}</div>
                            </div>
                          </div>
                        </td>

                        {/* Reviewed User */}
                        <td className="py-3.5 px-4">
                          <div className="font-bold text-slate-800 leading-tight">{r.reviewedUserName}</div>
                          <div className="text-[10px] font-mono text-slate-400">{r.reviewedUserId || 'N/A'}</div>
                        </td>

                        {/* Rating */}
                        <td className="py-3.5 px-4">
                          <div className="flex items-center space-x-1.5">
                            <span className={`px-2 py-0.5 rounded font-black text-xs ${
                              r.rating >= 4
                                ? 'bg-amber-100 text-amber-900'
                                : r.rating === 3
                                ? 'bg-blue-100 text-blue-900'
                                : 'bg-rose-100 text-rose-900'
                            }`}>
                              ★ {r.rating}.0
                            </span>
                          </div>
                        </td>

                        {/* Comment & Tags */}
                        <td className="py-3.5 px-4 max-w-sm">
                          <div className="text-slate-800 font-medium line-clamp-1" title={r.comment}>
                            {r.comment ? `"${r.comment}"` : <span className="text-slate-400 italic">No written comment</span>}
                          </div>
                          {r.parsedTags.length > 0 && (
                            <div className="flex flex-wrap gap-1 mt-1">
                              {r.parsedTags.slice(0, 3).map((tag, i) => (
                                <span key={i} className="text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded font-medium">
                                  #{tag}
                                </span>
                              ))}
                              {r.parsedTags.length > 3 && (
                                <span className="text-[10px] text-slate-400 font-mono">+{r.parsedTags.length - 3}</span>
                              )}
                            </div>
                          )}
                        </td>

                        {/* Status */}
                        <td className="py-3.5 px-4">
                          {isFlagged ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800">
                              <AlertTriangle className="w-3 h-3 text-rose-600" />
                              Flagged
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">
                              <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                              Published
                            </span>
                          )}
                        </td>

                        {/* Actions */}
                        <td className="py-3.5 px-4 text-right">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleOpenInspection(r);
                            }}
                            className="px-2.5 py-1.5 bg-slate-100 group-hover:bg-blue-600 group-hover:text-white rounded-lg text-slate-700 font-bold text-xs transition-colors inline-flex items-center gap-1"
                          >
                            <Eye className="w-3.5 h-3.5" />
                            <span>Audit</span>
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
      ) : (
        /* Card Grid View (Mobile / Tablet Optimized) */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredRatings.length === 0 ? (
            <div className="col-span-full bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-400">
              <Star className="w-10 h-10 mx-auto mb-2 text-slate-300 stroke-[1.5]" />
              <div className="font-bold text-slate-700 text-sm">No reviews found</div>
            </div>
          ) : (
            filteredRatings.map((r) => {
              const isFlagged = r.moderationStatus === 'flagged' || r.isSuspicious;

              return (
                <div
                  key={r.id}
                  onClick={() => handleOpenInspection(r)}
                  className={`bg-white rounded-2xl p-5 border transition-all cursor-pointer hover:shadow-md flex flex-col justify-between space-y-4 ${
                    isFlagged ? 'border-rose-300 bg-rose-50/20 ring-1 ring-rose-300' : 'border-slate-200/80'
                  }`}
                >
                  <div className="space-y-3">
                    {/* Top row: Trip & Score */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <span className="font-mono font-black text-sm text-slate-900">{r.tripCode}</span>
                        {r.normalizedType === 'passenger_to_driver' ? (
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                            Pass → Driver
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            Driver → Pass
                          </span>
                        )}
                      </div>
                      <div className="flex items-center space-x-1 text-amber-500 font-black text-sm bg-amber-50 px-2 py-0.5 rounded-lg border border-amber-200">
                        <span>★</span>
                        <span>{r.rating}.0</span>
                      </div>
                    </div>

                    {/* Reviewer / Reviewed user flow */}
                    <div className="flex items-center justify-between bg-slate-50 p-2.5 rounded-xl border border-slate-100 text-xs">
                      <div>
                        <span className="text-[10px] font-bold text-slate-400 uppercase block">By</span>
                        <span className="font-extrabold text-slate-900">{r.reviewerName}</span>
                      </div>
                      <div className="text-slate-300">➔</div>
                      <div className="text-right">
                        <span className="text-[10px] font-bold text-slate-400 uppercase block">To</span>
                        <span className="font-extrabold text-slate-800">{r.reviewedUserName}</span>
                      </div>
                    </div>

                    {/* Feedback Comment */}
                    <div className="bg-slate-50/80 p-3 rounded-xl border border-slate-100">
                      <p className="text-xs text-slate-800 font-medium italic leading-relaxed">
                        {r.comment ? `"${r.comment}"` : <span className="text-slate-400">No written feedback provided.</span>}
                      </p>
                    </div>

                    {/* Tags */}
                    {r.parsedTags.length > 0 && (
                      <div className="flex flex-wrap gap-1.5">
                        {r.parsedTags.map((tag, i) => (
                          <span key={i} className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md font-semibold">
                            #{tag}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Card Footer: Timestamp & Action */}
                  <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs text-slate-400">
                    <span className="flex items-center gap-1 font-mono text-[10px]">
                      <Clock className="w-3 h-3" />
                      {formatTimestampDisplay(r.timestamp)}
                    </span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleOpenInspection(r);
                      }}
                      className="text-xs font-bold text-blue-600 hover:text-blue-800 flex items-center gap-1"
                    >
                      Audit Details <ArrowUpRight className="w-3 h-3" />
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Comprehensive Review & Telemetry Inspection Modal */}
      {selectedRating && (() => {
        const normType = getNormalizedType(selectedRating);
        const trip = getTrip(selectedRating.tripId || selectedRating.tripCode);
        const reviewerDriver = normType === 'driver_to_passenger' ? getDriver(selectedRating.reviewerId) : null;
        const reviewerRider = normType === 'passenger_to_driver' ? getRider(selectedRating.reviewerId) : null;
        const reviewedDriver = normType === 'passenger_to_driver' ? getDriver(selectedRating.reviewedUserId) : null;
        const reviewedRider = normType === 'driver_to_passenger' ? getRider(selectedRating.reviewedUserId) : null;

        return (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto animate-fadeIn">
            <div className="bg-white rounded-3xl max-w-2xl w-full overflow-hidden shadow-2xl border border-slate-200 my-8">
              {/* Modal Header */}
              <div className="p-5 sm:p-6 border-b border-slate-800 flex items-center justify-between bg-slate-900 text-white">
                <div className="flex items-center space-x-3">
                  <div className="p-2.5 bg-amber-500 rounded-2xl text-white">
                    <Star className="w-5 h-5 fill-white" />
                  </div>
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="font-mono font-black text-base">Ride Review #{selectedRating.id}</span>
                      <span className="text-xs bg-slate-800 text-amber-400 px-2.5 py-0.5 rounded-full font-mono font-bold">
                        {selectedRating.tripCode || trip?.tripCode || 'DRG-8924'}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-0.5">Comprehensive audit, telematics & moderation log</p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedRating(null)}
                  className="w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center justify-center font-bold text-lg transition-colors"
                >
                  ×
                </button>
              </div>

              {/* Modal Body */}
              <div className="p-5 sm:p-6 space-y-5 text-xs max-h-[75vh] overflow-y-auto">
                {/* Success Alert */}
                {actionSuccessMsg && (
                  <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl flex items-center gap-2 text-xs font-bold animate-fadeIn">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                    <span>{actionSuccessMsg}</span>
                  </div>
                )}

                {/* Reviewer & Reviewed User Profiles */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-2xl border border-slate-200">
                  {/* Reviewer Box */}
                  <div className="space-y-1.5">
                    <div className="text-[10px] font-bold text-slate-400 uppercase flex items-center gap-1">
                      <User className="w-3.5 h-3.5 text-blue-600" />
                      Reviewer ({normType === 'passenger_to_driver' ? 'Passenger' : 'Driver'})
                    </div>
                    <div className="font-black text-slate-900 text-sm">{selectedRating.reviewerName}</div>
                    <div className="text-slate-500 font-mono text-[11px]">ID: {selectedRating.reviewerId || 'N/A'}</div>
                    <div className="text-slate-600 text-[11px] pt-1 space-y-0.5">
                      <div>Phone: {reviewerDriver?.phone || reviewerRider?.phone || '+92 300 1234567'}</div>
                      <div>Overall Rating: ★{reviewerDriver?.rating || reviewerRider?.rating || '5.0'}</div>
                    </div>
                    {onInspectUser && (
                      <button
                        onClick={() => {
                          const targetId = selectedRating.reviewerId;
                          if (targetId) {
                            onInspectUser(targetId, normType === 'passenger_to_driver' ? 'rider' : 'driver');
                            setSelectedRating(null);
                          }
                        }}
                        className="mt-2 text-xs font-bold text-blue-600 hover:text-blue-800 inline-flex items-center gap-1"
                      >
                        <span>View Reviewer Profile</span>
                        <ArrowUpRight className="w-3 h-3" />
                      </button>
                    )}
                  </div>

                  {/* Reviewed User Box */}
                  <div className="space-y-1.5 md:border-l md:border-slate-200 md:pl-4">
                    <div className="text-[10px] font-bold text-slate-400 uppercase flex items-center gap-1">
                      <User className="w-3.5 h-3.5 text-emerald-600" />
                      Reviewed User ({normType === 'passenger_to_driver' ? 'Driver' : 'Passenger'})
                    </div>
                    <div className="font-black text-slate-900 text-sm">{selectedRating.reviewedUserName}</div>
                    <div className="text-slate-500 font-mono text-[11px]">ID: {selectedRating.reviewedUserId || 'N/A'}</div>
                    <div className="text-slate-600 text-[11px] pt-1 space-y-0.5">
                      <div>Phone: {reviewedDriver?.phone || reviewedRider?.phone || '+92 321 7654321'}</div>
                      <div>Overall Rating: ★{reviewedDriver?.rating || reviewedRider?.rating || '4.9'}</div>
                    </div>
                    {onInspectUser && (
                      <button
                        onClick={() => {
                          const targetId = selectedRating.reviewedUserId;
                          if (targetId) {
                            onInspectUser(targetId, normType === 'passenger_to_driver' ? 'driver' : 'rider');
                            setSelectedRating(null);
                          }
                        }}
                        className="mt-2 text-xs font-bold text-blue-600 hover:text-blue-800 inline-flex items-center gap-1"
                      >
                        <span>View Target Profile</span>
                        <ArrowUpRight className="w-3 h-3" />
                      </button>
                    )}
                  </div>
                </div>

                {/* Ride Telemetry & Context */}
                <div className="bg-blue-50/60 p-4 rounded-2xl border border-blue-200 space-y-2.5">
                  <div className="font-black text-blue-900 flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <Car className="w-4 h-4 text-blue-600" />
                      <span>Ride Telematics Context</span>
                    </div>
                    {onInspectTrip && (
                      <button
                        onClick={() => {
                          onInspectTrip(selectedRating.tripId || trip?.id || '');
                          setSelectedRating(null);
                        }}
                        className="text-xs font-bold text-blue-700 hover:text-blue-900 inline-flex items-center gap-1 bg-white px-2.5 py-1 rounded-lg border border-blue-200 shadow-xs"
                      >
                        <span>Inspect Ride Route</span>
                        <ArrowUpRight className="w-3 h-3" />
                      </button>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-slate-700 text-xs">
                    <div><span className="font-bold text-slate-500">Trip Code:</span> <span className="font-mono font-bold text-slate-900">{trip?.tripCode || selectedRating.tripCode}</span></div>
                    <div><span className="font-bold text-slate-500">Vehicle Type:</span> <span className="font-semibold text-slate-900">{selectedRating.vehicleType || trip?.vehicleType || 'Economy Ride'}</span></div>
                    <div><span className="font-bold text-slate-500">Pickup:</span> <span className="text-slate-900">{trip?.fromLocation?.name || 'Saddar Bazaar, Peshawar'}</span></div>
                    <div><span className="font-bold text-slate-500">Destination:</span> <span className="text-slate-900">{trip?.toLocation?.name || 'University Town, Peshawar'}</span></div>
                    <div><span className="font-bold text-slate-500">Fare Amount:</span> <span className="font-mono font-black text-emerald-700">Rs. {selectedRating.fareAmount || trip?.fare?.total || 480}</span></div>
                    <div><span className="font-bold text-slate-500">Vehicle:</span> <span className="text-slate-900">{reviewedDriver?.vehicle ? `${reviewedDriver.vehicle.make} ${reviewedDriver.vehicle.model} (${reviewedDriver.vehicle.licensePlate})` : (trip?.vehiclePlate || 'Suzuki Cultus (PMA-402)')}</span></div>
                  </div>
                </div>

                {/* Score & Full Written Feedback */}
                <div className="bg-amber-50/50 p-4 rounded-2xl border border-amber-200 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-black text-amber-800 uppercase tracking-wider">Submitted Feedback</span>
                    <div className="text-amber-500 font-black text-base flex items-center space-x-1">
                      <span>{'★'.repeat(Math.min(5, Math.max(1, selectedRating.rating)))}</span>
                      <span className="text-slate-800">({selectedRating.rating}.0 / 5.0)</span>
                    </div>
                  </div>
                  <div className="text-slate-900 font-medium italic text-sm leading-relaxed pt-1">
                    "{selectedRating.comment || 'No written comment provided.'}"
                  </div>
                  <div className="flex flex-wrap gap-1.5 pt-2">
                    {getTags(selectedRating.tags).map((tag, i) => (
                      <span key={i} className="bg-amber-100 text-amber-900 px-2.5 py-1 rounded-lg font-bold text-xs">
                        #{tag}
                      </span>
                    ))}
                  </div>
                  <div className="text-[10px] text-slate-400 pt-1 font-mono flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    Timestamp: {formatTimestampDisplay(selectedRating.timestamp)}
                  </div>
                </div>

                {/* Admin Moderation Controls Section */}
                <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="font-black text-slate-900 flex items-center gap-1.5">
                      <Shield className="w-4 h-4 text-blue-600" />
                      <span>Admin Moderation & Quality Controls</span>
                    </div>
                    <span className="text-[10px] font-bold text-slate-400 uppercase">Live sync</span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="text-[10px] font-bold text-slate-500 uppercase block mb-1">
                        Review Visibility Status
                      </label>
                      <select
                        value={modStatus}
                        onChange={(e) => setModStatus(e.target.value as any)}
                        className="w-full p-2 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-800 focus:outline-none focus:border-blue-500"
                      >
                        <option value="published">Published (Public)</option>
                        <option value="flagged">Flagged (Under Audit)</option>
                        <option value="investigating">Investigating (Support Dispute)</option>
                        <option value="hidden">Hidden from Rating Calculation</option>
                        <option value="resolved">Resolved / Approved</option>
                      </select>
                    </div>

                    <div>
                      <label className="text-[10px] font-bold text-slate-500 uppercase block mb-1">
                        Suspicious / Fraud Flag
                      </label>
                      <button
                        type="button"
                        onClick={() => setModIsSuspicious(!modIsSuspicious)}
                        className={`w-full p-2 rounded-xl text-xs font-bold flex items-center justify-center gap-2 transition-all ${
                          modIsSuspicious
                            ? 'bg-rose-100 text-rose-800 border border-rose-300'
                            : 'bg-white text-slate-700 border border-slate-200 hover:bg-slate-50'
                        }`}
                      >
                        <Flag className={`w-3.5 h-3.5 ${modIsSuspicious ? 'text-rose-600 fill-rose-600' : 'text-slate-400'}`} />
                        <span>{modIsSuspicious ? 'Flagged as Suspicious' : 'Mark as Suspicious'}</span>
                      </button>
                    </div>
                  </div>

                  {modIsSuspicious && (
                    <div>
                      <label className="text-[10px] font-bold text-slate-500 uppercase block mb-1">
                        Flag Reason / Violation Code
                      </label>
                      <input
                        type="text"
                        placeholder="e.g. Reckless driving complaint, Fare dispute, Vulgar language"
                        value={modFlagReason}
                        onChange={(e) => setModFlagReason(e.target.value)}
                        className="w-full p-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-900 font-medium"
                      />
                    </div>
                  )}

                  <div>
                    <label className="text-[10px] font-bold text-slate-500 uppercase block mb-1">
                      Internal Admin Audit Notes
                    </label>
                    <textarea
                      rows={2}
                      placeholder="Add moderation notes (e.g. Warned driver, issued coupon to rider, verified dashcam footage)..."
                      value={modAdminNote}
                      onChange={(e) => setModAdminNote(e.target.value)}
                      className="w-full p-2 bg-white border border-slate-200 rounded-xl text-xs text-slate-900 font-medium"
                    />
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    {onDeleteRating && (
                      <button
                        onClick={async () => {
                          if (confirm('Are you sure you want to delete this rating record permanently?')) {
                            await onDeleteRating(selectedRating.id);
                            setSelectedRating(null);
                          }
                        }}
                        className="text-xs font-bold text-rose-600 hover:text-rose-800 inline-flex items-center gap-1"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>Delete Review</span>
                      </button>
                    )}
                    <button
                      onClick={handleSaveModeration}
                      disabled={isSavingMod}
                      className="ml-auto px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-xs inline-flex items-center gap-1.5 shadow-sm active:scale-95 disabled:opacity-50"
                    >
                      {isSavingMod ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                      <span>Save Moderation Decision</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      })()}
    </div>
  );
};
