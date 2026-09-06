export interface LiveDriverLocation {
  driverId: string;
  driverName?: string;
  phone?: string;
  lat: number;
  lng: number;
  heading?: number;
  speedKmh?: number;
  updatedAt?: string | number;
  status?: string; // 'online' | 'on_trip' | 'offline'
  vehicleType?: VehicleType | string;
  licensePlate?: string;
  fareDisplay?: string;
}

export type VehicleType = 'sedan' | 'boda_bike' | 'tuktuk_auto' | 'comfort' | 'xl_van';

export type DriverVerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type DriverAccountStatus =
  | 'PENDING_REVIEW'
  | 'ACTIVE'
  | 'ONLINE'
  | 'ON_TRIP'
  | 'SUSPENDED'
  | 'FLAGGED';

export type PassengerAccountStatus =
  | 'ACTIVE'
  | 'ON_TRIP'
  | 'SUSPENDED'
  | 'FLAGGED'
  | 'INACTIVE'
  | 'DEACTIVATED';

export type DriverStatus = 'online' | 'on_trip' | 'offline' | 'suspended' | 'pending_verification';

export type RiderStatus = 'active' | 'suspended' | 'flagged' | 'under_review' | 'inactive' | 'deactivated';

export type TripStatus =
  | 'requested'
  | 'searching'
  | 'matching'
  | 'offer_received'
  | 'accepted'
  | 'driver_arriving'
  | 'driver_arrived'
  | 'in_progress'
  | 'completed'
  | 'cancelled'
  | 'sos_alert'
  | string;

export type PaymentMethod = 'cash' | 'jazzcash' | 'easypaisa' | 'mobile_money' | 'wallet' | 'card';

export type PaymentStatus = 'paid' | 'pending' | 'failed' | 'refunded';

export interface LocationPoint {
  name: string;
  address: string;
  lat: number;
  lng: number;
  landmark?: string;
}

export interface DocumentVerification {
  id: string;
  type: 'national_id' | 'driver_license' | 'vehicle_registration' | 'commercial_insurance' | 'vehicle_inspection' | string;
  docType?: string;
  category?: string;
  title: string;
  documentNumber: string;
  issueDate: string;
  expiryDate: string;
  status: 'verified' | 'pending' | 'rejected' | 'expired';
  fileUrl: string;
  driveWebLink?: string;
  rejectionReason?: string;
  lastReviewedBy?: string;
  lastReviewedAt?: string;
}

export interface VehicleInfo {
  make: string;
  model: string;
  year: number;
  color: string;
  licensePlate: string;
  type: VehicleType;
  seatingCapacity: number;
  photoUrl: string;
  inspectionPassed: boolean;
}

export interface DeviceTelemetry {
  deviceModel: string;
  manufacturer: string;
  androidVersion: string; // e.g. "Android 11 (API 30)" or "Android 8.1 (API 27)"
  apiLevel: number;
  ramTotalGb: number; // e.g. 2, 3, 4, 6
  ramUsagePercent: number;
  batteryLevel: number;
  isBatterySaver: boolean;
  appVersion: string;
  networkType: '2G' | '3G' | '4G' | '5G' | 'WiFi' | 'Offline';
  networkLatencyMs: number;
  gpsAccuracyMeters: number;
  offlineQueuedPackets: number;
  lastPingAt: string;
}

export interface Driver {
  id: string;
  fullName: string;
  phone: string;
  email: string;
  avatar: string;
  rating: number;
  totalTrips: number;
  acceptanceRate: number; // 0-100
  completionRate: number; // 0-100
  cancellationRate: number; // 0-100
  status: DriverStatus;
  accountStatus: DriverAccountStatus;
  verificationStatus: DriverVerificationStatus;
  rawStatus?: string;
  walletBalance: number;
  todayEarnings: number;
  joinedDate: string;
  currentLocation: {
    lat: number;
    lng: number;
    heading: number; // degrees 0-360
    speedKmh: number;
  };
  currentTripId?: string;
  vehicle: VehicleInfo;
  documents: DocumentVerification[];
  telemetry: DeviceTelemetry;
  city: string;
}

export interface Rider {
  id: string;
  fullName: string;
  phone: string;
  email: string;
  avatar: string;
  rating: number;
  totalRides: number;
  totalSpend: number;
  walletBalance: number;
  status: RiderStatus;
  accountStatus: PassengerAccountStatus;
  joinedDate: string;
  preferredPayment: PaymentMethod;
  deviceModel: string;
  androidVersion: string;
  emergencyContact: {
    name: string;
    phone: string;
    relationship: string;
  };
  reportedIncidentsCount: number;
  city: string;
}

export interface RouteWaypoint {
  lat: number;
  lng: number;
}

export interface TripFareBreakdown {
  baseFare: number;
  distanceFare: number;
  timeFare: number;
  surgeMultiplier: number;
  surgeAmount: number;
  discount: number;
  drigoCommissionRate: number; // e.g. 0.18 (18%)
  drigoCommissionAmount: number;
  driverEarnings: number;
  total: number;
  currency: string;
}

export interface Trip {
  id: string;
  tripCode: string; // e.g. "DRG-8924"
  passengerId: string;
  passengerName: string;
  passengerPhone: string;
  passengerAvatar: string;
  passengerRating: number;
  driverId?: string;
  driverName?: string;
  driverPhone?: string;
  driverAvatar?: string;
  driverRating?: number;
  vehicleType: VehicleType;
  vehiclePlate?: string;
  vehicleModel?: string;
  vehicleColor?: string;
  status: TripStatus;
  fromLocation: LocationPoint; // Pick-up (Mandatory & Independent)
  toLocation: LocationPoint;   // Drop-off (Mandatory & Independent)
  distanceKm: number;
  estimatedDurationMinutes: number;
  actualDurationMinutes?: number;
  routePolyline: RouteWaypoint[];
  fare: TripFareBreakdown;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  requestedAt: string;
  startedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  cancelledBy?: 'passenger' | 'driver' | 'admin' | 'system_timeout';
  sosAlert?: {
    isTriggered: boolean;
    triggeredAt?: string;
    triggeredBy?: 'passenger' | 'driver';
    reason?: string;
    liveAudioActive?: boolean;
    policeNotified?: boolean;
    emergencyContactsSent?: boolean;
    resolved?: boolean;
    resolvedAt?: string;
    resolutionNotes?: string;
  };
  passengerTelemetry?: {
    network: string;
    battery: number;
    device: string;
  };
  driverTelemetry?: {
    network: string;
    battery: number;
    gpsAccuracy: number;
    device: string;
  };
  offerAmount?: number;
  counterOfferAmount?: number;
  bids?: Array<{
    driverId: string;
    driverName: string;
    driverPhone?: string;
    driverAvatar?: string;
    driverRating?: number;
    offerAmount: number;
    bidAmount?: number;
    vehicleModel?: string;
    vehiclePlate?: string;
    etaMinutes?: number;
    createdAt?: string;
    status?: 'pending' | 'accepted' | 'rejected';
  }>;
  rideCategory?: string;
  notes?: string;
  rating?: number;
  review?: string;
}

export interface SurgeZone {
  id: string;
  name: string;
  city: string;
  center: { lat: number; lng: number };
  radiusMeters: number;
  surgeMultiplier: number; // e.g. 1.4x
  activeDemand: number; // pending ride requests
  availableDrivers: number;
  status: 'active' | 'scheduled' | 'disabled';
  baseFareBonus: number;
  color: string;
}

export interface PricingConfig {
  vehicleType: VehicleType;
  name: string;
  baseFare: number;
  perKmRate: number;
  perMinuteRate: number;
  minimumFare: number;
  commissionPercentage: number;
  cancellationFee: number;
  currency: string;
}

export interface SupportTicket {
  id: string;
  tripId?: string;
  tripCode?: string;
  reportedBy: 'passenger' | 'driver';
  userName: string;
  userPhone: string;
  type: 'safety_sos' | 'fare_dispute' | 'lost_item' | 'app_crash_bug' | 'driver_conduct' | 'vehicle_condition';
  priority: 'critical' | 'high' | 'medium' | 'low';
  status: 'open' | 'investigating' | 'resolved';
  title: string;
  description: string;
  createdAt: string;
  assignedAgent?: string;
  resolutionNotes?: string;
}

export interface LiveActivityFeedItem {
  id: string;
  timestamp: string;
  type: 'trip_request' | 'trip_assigned' | 'trip_completed' | 'sos_alert' | 'driver_online' | 'driver_kyc' | 'payout_requested' | 'trip_cancelled' | 'driver_matched' | 'kyc_submitted' | 'account_suspended';
  title: string;
  description: string;
  severity: 'info' | 'success' | 'warning' | 'critical';
  tripId?: string;
  driverId?: string;
  riderId?: string;
}

export interface DriverVerification {
  id: string;
  driverId: string;
  driverName: string;
  fullName?: string;
  profileImage?: string;
  avatar?: string;
  documentId: string;
  role: string;
  phone: string;
  email: string;
  vehicleMake: string;
  vehicleModel: string;
  licensePlate: string;
  status: 'PENDING' | 'REVIEW' | 'APPROVED' | 'REJECTED' | 'SUSPENDED' | string;
  accountStatus?: 'ACTIVE' | 'SUSPENDED' | string;
  verificationStatus?: 'APPROVED' | 'PENDING' | 'REJECTED' | 'UNDER_REVIEW' | string;
  submittedAt?: string;
  createdAt?: string;
  documents?: DocumentVerification[];
}

export interface SafetyReport {
  id: string;
  rideId: string;
  category: string;
  categoryLabel: string;
  description: string;
  reporterId: string;
  reporterName: string;
  reporterPhone?: string;
  reporterRole: 'PASSENGER' | 'DRIVER' | string;
  reportedUserId: string;
  reportedUserName: string;
  reportedUserRole: 'PASSENGER' | 'DRIVER' | string;
  ridePickupTitle?: string;
  rideDestinationTitle?: string;
  driverPlateNumber?: string;
  status: 'PENDING_ADMIN_REVIEW' | 'RESOLVED' | string;
  timestamp: number;
  blockUser?: boolean;
}

export interface RideRating {
  id: string;
  tripId: string;
  tripCode: string;
  reviewerType: 'passenger_to_driver' | 'driver_to_passenger';
  reviewerId: string;
  reviewerName: string;
  reviewerAvatar?: string;
  reviewedUserId: string;
  reviewedUserName: string;
  rating: number;
  comment: string;
  tags: string[];
  timestamp: string;
  isSuspicious?: boolean;
  flagReason?: string;
  moderationStatus?: 'published' | 'flagged' | 'hidden' | 'investigating' | 'resolved';
  adminNote?: string;
  moderatedBy?: string;
  moderatedAt?: string;
  city?: string;
  vehicleType?: string;
  fareAmount?: number;
}

export interface AdminNotification {
  id: string;
  type: 
    | 'new_driver_registration'
    | 'kyc_submission'
    | 'kyc_rejection'
    | 'kyc_completion'
    | 'new_passenger_ride_request'
    | 'driver_matched'
    | 'ride_cancellation'
    | 'sos_trigger'
    | 'incident_report'
    | 'suspended_account'
    | 'failed_payment'
    | 'wallet_issue'
    | string;
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  targetTab: 'dashboard' | 'dispatch' | 'rides' | 'ratings' | 'users' | 'analytics' | 'safety' | 'pricing' | 'finance';
  targetId?: string;
}

export interface PayoutRequest {
  id: string;
  driverId: string;
  driverName: string;
  driverPhone: string;
  driverAvatar?: string;
  amount: number;
  currency: string;
  paymentMethod: 'jazzcash' | 'easypaisa' | 'bank_transfer' | 'cash' | 'raast';
  accountTitle: string;
  accountNumber: string;
  bankName?: string;
  status: 'pending' | 'approved' | 'rejected' | 'processed';
  requestedAt: string;
  processedAt?: string;
  processedBy?: string;
  transactionRef?: string;
  rejectionReason?: string;
  gatewayFee?: number;
  note?: string;
}

export interface WalletTransaction {
  id: string;
  userType: 'driver' | 'rider';
  userId: string;
  userName: string;
  userPhone?: string;
  type: 'admin_adjustment' | 'bonus' | 'topup' | 'refund' | 'deduction' | 'fare_earning' | 'commission_deduction' | 'payout_withdrawal';
  amount: number; // positive for credit, negative for debit
  previousBalance: number;
  newBalance: number;
  currency: string;
  paymentMethod?: 'jazzcash' | 'easypaisa' | 'bank_transfer' | 'cash' | 'raast' | 'wallet' | 'admin_manual';
  reason: string;
  referenceId?: string; // e.g. tripId, payoutId, bank reference
  administeredBy?: string;
  timestamp: string;
}

export interface OperationalRecommendation {
  id: string;
  category: 'surge' | 'fleet' | 'safety' | 'kyc' | 'telemetry' | 'finance' | 'fraud' | 'dispatch';
  title: string;
  description: string;
  impact: 'high' | 'medium' | 'low';
  timestamp: string;
  actionLabel: string;
  actionType: 'activate_surge' | 'view_kyc' | 'view_safety' | 'inspect_driver' | 'approve_payout' | 'open_dispatch' | 'rebalance_fleet' | 'push_battery_alert' | 'escalate_sos' | 'dismiss_recommendation' | string;
  actionPayload?: any;
  confidenceScore?: number;
  rootCause?: string;
  estimatedImpactBenefit?: string;
  affectedEntityId?: string;
  affectedEntityType?: 'trip' | 'driver' | 'rider' | 'zone' | 'payout' | 'safety_report';
  executed?: boolean;
  executedAt?: string;
  executedBy?: string;
}



