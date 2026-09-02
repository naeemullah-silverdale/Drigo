package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TripStatus {
    SCHEDULED,
    BOARDING,
    EN_ROUTE,
    COMPLETED,
    CANCELLED
}

enum class RecurringFrequency {
    NONE,
    DAILY,
    WEEKDAYS,
    WEEKLY
}

enum class LuggageAllowance {
    SMALL,   // Backpack only
    MEDIUM,  // Hand luggage
    LARGE    // Suitcase in trunk
}

data class Waypoint(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val estimatedMinutesFromStart: Int = 0,
    val pickupPrice: Double = 0.0
)

data class DriverProfile(
    val id: String = "driver_1",
    val name: String = "Alex Rivera",
    val avatarUrl: String = "",
    val rating: Float = 4.92f,
    val totalRides: Int = 142,
    val memberSince: String = "March 2023",
    val bio: String = "Tech commuter & weekend explorer. Safe driver with 8+ yrs clean record.",
    val verifiedIdentity: Boolean = true,
    val phone: String = "+1 (555) 234-5678"
)

data class VehicleInfo(
    val make: String = "Tesla",
    val model: String = "Model Y",
    val year: Int = 2024,
    val color: String = "Midnight Silver",
    val plate: String = "7XYZ890",
    val type: String = "Electric",
    val photoUrl: String = ""
)

data class TripPreferences(
    val allowsPets: Boolean = false,
    val allowsSmoking: Boolean = false,
    val maxTwoInBack: Boolean = true,
    val musicPreference: String = "Chill Indie & Podcasts",
    val talkative: String = "Friendly & Open",
    val temperature: String = "21°C / 70°F"
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val driverId: String,
    val driverName: String,
    val driverRating: Float,
    val driverTotalRides: Int,
    val driverPhone: String,
    val originCity: String,
    val originAddress: String,
    val originLat: Double,
    val originLon: Double,
    val destinationCity: String,
    val destinationAddress: String,
    val destinationLat: Double,
    val destinationLon: Double,
    val departureDate: String, // e.g. "2026-08-26"
    val departureTime: String, // e.g. "08:30 AM"
    val estimatedDurationHours: Double,
    val totalDistanceKm: Double,
    val pricePerSeat: Double,
    val totalSeats: Int,
    val availableSeats: Int,
    val vehicleMake: String,
    val vehicleModel: String,
    val vehicleColor: String,
    val vehiclePlate: String,
    val vehicleType: String,
    val luggageAllowance: LuggageAllowance,
    val recurringFrequency: RecurringFrequency,
    val recurringDays: String = "", // e.g. "Mon,Tue,Wed,Thu,Fri"
    val waypointsJson: String = "[]", // List<Waypoint> serialized
    val status: TripStatus = TripStatus.SCHEDULED,
    val allowsPets: Boolean = false,
    val allowsSmoking: Boolean = false,
    val maxTwoInBack: Boolean = true,
    val musicVibe: String = "Chill & Podcasts",
    val specialNotes: String = "Will make a 10-minute coffee/restroom stop at midpoint.",
    val instantBooking: Boolean = true,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CANCELLED
}

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tripId: String,
    val passengerId: String = "user_rider_1",
    val passengerName: String,
    val passengerPhone: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val seatsBooked: Int,
    val seatNumbers: String = "Front, Back-Right",
    val totalPrice: Double,
    val bookingCode: String = "CL-" + (1000..9999).random(),
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val luggageCount: Int = 1,
    val bookedAtTimestamp: Long = System.currentTimeMillis(),
    val qrToken: String = UUID.randomUUID().toString().take(12).uppercase()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tripId: String,
    val senderId: String,
    val senderName: String,
    val isDriver: Boolean,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isSystemNotice: Boolean = false
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val userId: String = "current_user",
    val userName: String = "Naeem Ullah",
    val userPhone: String = "+1 (555) 890-1234",
    val isDriverMode: Boolean = false,
    val homeCity: String = "San Francisco",
    val workCity: String = "San Jose",
    val defaultCommuteDeparture: String = "07:45 AM",
    val defaultCommuteReturn: String = "05:30 PM",
    val favoriteCorridors: String = "San Francisco to Silicon Valley, Seattle to Portland",
    val musicPreference: String = "Lo-Fi / Podcasts",
    val petAllergies: Boolean = false,
    val preferredLuggage: LuggageAllowance = LuggageAllowance.MEDIUM,
    val totalRidesAsPassenger: Int = 19,
    val totalRidesAsDriver: Int = 8,
    val totalCo2SavedKg: Double = 348.5,
    val totalMoneySavedUsd: Double = 890.0
)

data class AiMatchRecommendation(
    val tripId: String,
    val matchScorePercent: Int,
    val matchReasoning: String,
    val estimatedArrival: String,
    val co2SavingsKg: Double,
    val corridorName: String
)

data class RideRequest(
    val id: String = UUID.randomUUID().toString(),
    val passengerId: String = "",
    val passengerName: String = "",
    val passengerEmail: String = "",
    val passengerPhotoUrl: String = "",
    val passengerRating: Double = 4.9,
    val paymentMethod: String = "Cash",
    val pickupTitle: String = "",
    val pickupSubtitle: String = "",
    val pickupLat: Double = 0.0,
    val pickupLon: Double = 0.0,
    val destinationTitle: String = "",
    val destinationSubtitle: String = "",
    val destinationLat: Double = 0.0,
    val destinationLon: Double = 0.0,
    val rideCategory: String = "Share Ride",
    val vehicleType: String = "Car",
    val hasAc: Boolean = false,
    val estimatedFare: Int = 0,
    val distanceKm: Double = 0.0,
    val durationMinutes: Int = 0,
    val status: String = "SEARCHING_DRIVERS",
    val assignedDriverId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L
)

enum class TransactionType {
    TOP_UP,
    RIDE_PAYMENT,
    REFUND,
    ADJUSTMENT
}

enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED
}

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val userId: String = "",
    val walletId: String = UUID.randomUUID().toString(),
    val balance: Double = 0.0,
    val currency: String = "PKR",
    val userRole: String = "PASSENGER", // "PASSENGER" or "DRIVER"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val transactionId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val walletId: String = "",
    val type: TransactionType = TransactionType.TOP_UP,
    val amount: Double = 0.0,
    val balanceBefore: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val paymentMethod: String = "EASYPAISA",
    val referenceId: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class EasypaisaPaymentRequest(
    val orderId: String,
    val transactionId: String,
    val amount: Double,
    val mobileNumber: String,
    val userRole: String,
    val storeId: String = "DRIGO_EASYPAISA_STORE",
    val description: String = "Drigo Wallet Top-up via Easypaisa",
    val timestamp: Long = System.currentTimeMillis()
)

data class EasypaisaPaymentResult(
    val success: Boolean,
    val orderId: String,
    val transactionId: String,
    val responseCode: String,
    val responseMessage: String,
    val updatedTransaction: WalletTransactionEntity? = null
)

enum class PassengerOrderStatus(val label: String) {
    SEARCHING("Searching Drivers"),
    OFFER_RECEIVED("Offer Received"),
    ACCEPTED("Offer Accepted"),
    DRIVER_ARRIVED("Driver Arrived"),
    IN_TRIP("On Trip"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

data class PassengerOrder(
    val id: String = UUID.randomUUID().toString(),
    val requestId: String = "",
    val passengerId: String = "",
    val passengerEmail: String = "",
    val pickupTitle: String = "",
    val pickupSubtitle: String = "",
    val pickupLat: Double = 0.0,
    val pickupLon: Double = 0.0,
    val destinationTitle: String = "",
    val destinationSubtitle: String = "",
    val destinationLat: Double = 0.0,
    val destinationLon: Double = 0.0,
    val distanceKm: Double = 0.0,
    val durationMinutes: Int = 0,
    val rideCategory: String = "Ride A/C",
    val agreedFare: Int = 650,
    val paymentMethod: String = "Cash",
    val driverName: String = "Captain Farhan",
    val driverRating: Double = 4.9,
    val driverTotalRides: Int = 1420,
    val driverVehicleMake: String = "Toyota",
    val driverVehicleModel: String = "Corolla",
    val driverVehicleColor: String = "White",
    val driverPlateNumber: String = "LEA-4521",
    val driverPhone: String = "+92 300 1234567",
    val status: PassengerOrderStatus = PassengerOrderStatus.ACCEPTED,
    val etaMinutes: Int = 4,
    val scheduledTimeText: String? = null,
    val passengerCount: Int = 1,
    val comments: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class DriverOffer(
    val id: String = UUID.randomUUID().toString(),
    val requestId: String = "",
    val driverId: String = "",
    val driverName: String = "Captain Farhan",
    val driverRating: Double = 4.9,
    val driverTotalRides: Int = 1420,
    val driverVehicleMake: String = "Toyota",
    val driverVehicleModel: String = "Corolla",
    val driverVehicleColor: String = "White",
    val driverPlateNumber: String = "LEA-4521",
    val driverPhone: String = "+92 300 1234567",
    val offeredFare: Int = 0,
    val etaMinutes: Int = 4,
    val distanceKmAway: Double = 1.2,
    val driverLat: Double = 33.6844,
    val driverLon: Double = 73.0479,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveDriverLocation(
    val rideId: String = "",
    val driverId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val bearing: Float = 0f,
    val speedKmh: Float = 35f,
    val etaMinutes: Int = 0,
    val distanceRemainingKm: Double = 0.0,
    val status: String = "EN_ROUTE_TO_PICKUP", // EN_ROUTE_TO_PICKUP, ARRIVED, IN_TRIP, COMPLETED
    val updatedAt: Long = System.currentTimeMillis()
)

enum class VerificationStatus(val label: String) {
    PENDING("PENDING"),
    UNDER_REVIEW("UNDER_REVIEW"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED")
}

enum class DriverVerificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class DriverAccountStatus {
    PENDING_REVIEW,
    ACTIVE,
    ONLINE,
    ON_TRIP,
    SUSPENDED,
    FLAGGED
}

enum class PassengerAccountStatus {
    ACTIVE,
    ON_TRIP,
    SUSPENDED,
    FLAGGED,
    INACTIVE,
    DEACTIVATED
}

data class UserRecord(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "PASSENGER", // "DRIVER" or "PASSENGER"
    val accountStatus: String = "ACTIVE", // Role-specific enum string
    val verificationStatus: String = "PENDING", // For Drivers: PENDING, APPROVED, REJECTED
    val isOnline: Boolean = false,
    val mode: String = "PASSENGER",
    val updatedAt: Long = System.currentTimeMillis()
)

fun parseDriverVerificationStatus(
    rawVerStatus: String?,
    rawStatus: String?,
    isConfirmation: Boolean
): DriverVerificationStatus {
    val ver = rawVerStatus?.uppercase()?.trim() ?: ""
    val st = rawStatus?.uppercase()?.trim() ?: ""
    
    if (ver == "APPROVED" || ver == "VERIFIED" || st == "APPROVED" || st == "VERIFIED" || isConfirmation) {
        return DriverVerificationStatus.APPROVED
    }
    if (ver == "REJECTED" || st == "REJECTED") {
        return DriverVerificationStatus.REJECTED
    }
    return DriverVerificationStatus.PENDING
}

fun parseDriverAccountStatus(
    rawAccountStatus: String?,
    rawStatus: String?,
    verificationStatus: DriverVerificationStatus,
    isOnline: Boolean
): DriverAccountStatus {
    val acc = rawAccountStatus?.uppercase()?.trim() ?: ""
    val st = rawStatus?.uppercase()?.trim() ?: ""
    
    if (acc == "SUSPENDED" || st == "SUSPENDED") return DriverAccountStatus.SUSPENDED
    if (acc == "FLAGGED" || st == "FLAGGED") return DriverAccountStatus.FLAGGED
    if (acc == "ON_TRIP" || st == "ON_TRIP" || st == "IN_TRIP") return DriverAccountStatus.ON_TRIP
    if (acc == "ONLINE" || st == "ONLINE" || (isOnline && verificationStatus == DriverVerificationStatus.APPROVED)) return DriverAccountStatus.ONLINE
    if (acc == "ACTIVE" || st == "ACTIVE" || st == "APPROVED") {
        return if (verificationStatus == DriverVerificationStatus.APPROVED) {
            if (isOnline) DriverAccountStatus.ONLINE else DriverAccountStatus.ACTIVE
        } else {
            DriverAccountStatus.PENDING_REVIEW
        }
    }
    if (acc == "PENDING_REVIEW" || st == "PENDING_VERIFICATION" || st == "PENDING" || st == "UNDER_REVIEW") {
        return DriverAccountStatus.PENDING_REVIEW
    }
    
    return if (verificationStatus == DriverVerificationStatus.APPROVED) DriverAccountStatus.ACTIVE else DriverAccountStatus.PENDING_REVIEW
}

fun parsePassengerAccountStatus(
    rawAccountStatus: String?,
    rawStatus: String?
): PassengerAccountStatus {
    val acc = rawAccountStatus?.uppercase()?.trim() ?: ""
    val st = rawStatus?.uppercase()?.trim() ?: ""
    
    if (acc == "SUSPENDED" || st == "SUSPENDED") return PassengerAccountStatus.SUSPENDED
    if (acc == "DEACTIVATED" || st == "DEACTIVATED") return PassengerAccountStatus.DEACTIVATED
    if (acc == "FLAGGED" || st == "FLAGGED") return PassengerAccountStatus.FLAGGED
    if (acc == "ON_TRIP" || st == "ON_TRIP" || st == "IN_TRIP") return PassengerAccountStatus.ON_TRIP
    if (acc == "INACTIVE" || st == "INACTIVE") return PassengerAccountStatus.INACTIVE
    
    return PassengerAccountStatus.ACTIVE
}

data class GoogleDriveFileRecord(
    val fileId: String = "",
    val fileName: String = "",
    val mimeType: String = "image/jpeg",
    val driveFolderId: String = "",
    val webViewLink: String = "",
    val webContentLink: String = "",
    val directDownloadUrl: String = "",
    val thumbnailLink: String = "",
    val fileSize: Long = 0L,
    val userId: String = "",
    val userEmail: String = "",
    val docType: String = "GENERAL_IMAGE",
    val category: String = "documents", // "driver_documents", "profile", "receipts", "general"
    val uploadedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class DriverDocumentItem(
    val docType: String = "", // CNIC_FRONT, CNIC_BACK, LICENSE_FRONT, LICENSE_BACK, DRIVER_PHOTO, VEHICLE_FRONT, VEHICLE_BACK, VEHICLE_SIDE, VEHICLE_REGISTRATION, ADDITIONAL_DOC
    val title: String = "",
    val category: String = "documents", // "profile", "identity", "license", "vehicle", "documents"
    val storagePath: String = "", // Drigo_Rideshare_Documents/Drivers/{driverId}/category/filename.jpg
    val fileUrl: String = "",
    val fileType: String = "image/jpeg",
    val fileSize: Long = 0L,
    val uploadedAt: Long = System.currentTimeMillis(),
    val driverId: String = "",
    val isRequired: Boolean = true,
    val status: String = "PENDING", // PENDING, UNDER_REVIEW, APPROVED, REJECTED
    val rejectionReason: String = "",
    val storageProvider: String = "GOOGLE_DRIVE", // GOOGLE_DRIVE
    val googleDriveFileId: String = "",
    val googleDriveWebViewLink: String = "",
    val driveFolderId: String = "",
    val fileName: String = ""
)

data class DriverVerification(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    // Personal Details & Profile Photo
    val driverPhotoUri: String = "",
    val cnicFrontUri: String = "",
    val cnicBackUri: String = "",
    // Vehicle Details & Multi-Angle Photos
    val vehiclePictureUri: String = "",
    val vehicleFrontUri: String = "",
    val vehicleBackUri: String = "",
    val vehicleSideUri: String = "",
    val vehicleCardDocFrontUri: String = "",
    val vehicleCardDocBackUri: String = "",
    val vehicleRegistrationDocUri: String = "",
    val vehicleCompany: String = "",
    val vehicleModel: String = "",
    val vehicleNumber: String = "",
    // Driving Licence
    val drivingLicenseFrontUri: String = "",
    val drivingLicenseBackUri: String = "",
    // Additional Document
    val additionalDocUri: String = "",
    // Detailed list of verification documents
    val documents: List<DriverDocumentItem> = emptyList(),
    // Confirmation status field explicitly named "confirmtion"
    val confirmtion: Boolean = false,
    val status: String = "PENDING", // PENDING, UNDER_REVIEW, APPROVED, REJECTED
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewNotes: String = "Your registration is under review. It will take around 24 hrs for confirmation.",
    val rejectionReason: String = "",
    // User & KYC Management System Aligned Fields
    val accountStatus: String = "PENDING_REVIEW", // PENDING_REVIEW, ACTIVE, ONLINE, ON_TRIP, SUSPENDED, FLAGGED
    val verificationStatus: String = "PENDING", // PENDING, APPROVED, REJECTED
    val isVerified: Boolean = false,
    val isOnline: Boolean = false
)

enum class ReportCategory(val label: String) {
    INAPPROPRIATE_BEHAVIOR("Inappropriate Behavior / Harassment"),
    RECKLESS_DRIVING("Reckless / Dangerous Driving"),
    ROUTE_DEVIATION("Significant Route Deviation"),
    OVERCHARGING("Fare Dispute / Overcharging"),
    VEHICLE_CONDITION("Vehicle Condition / Cleanliness"),
    UNSAFE_EXPERIENCE("Felt Unsafe / Security Concern"),
    PASSENGER_MISCONDUCT("Passenger Misconduct / Rude Behavior"),
    FRAUD_SCAM("Fraud / Scam / Fake Profile"),
    OTHER("Other Issue")
}

@Entity(tableName = "ride_ratings")
data class RideRatingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val rideId: String = "",
    val raterId: String = "",
    val raterRole: String = "PASSENGER", // "PASSENGER" or "DRIVER"
    val raterName: String = "",
    val targetId: String = "",
    val targetName: String = "",
    val stars: Int = 5,
    val reviewText: String = "",
    val tags: String = "", // Comma-separated quick tags e.g. "Safe Driver,Punctual,Clean Car"
    val tipAmount: Double = 0.0,
    val isBlocked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "safety_reports")
data class SafetyReportEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val rideId: String = "",
    val reporterId: String = "",
    val reporterRole: String = "PASSENGER", // "PASSENGER" or "DRIVER"
    val reporterName: String = "",
    val reporterPhone: String = "",
    val reportedUserId: String = "",
    val reportedUserName: String = "",
    val reportedUserRole: String = "DRIVER",
    val category: ReportCategory = ReportCategory.INAPPROPRIATE_BEHAVIOR,
    val description: String = "",
    val blockUser: Boolean = true,
    val ridePickupTitle: String = "",
    val rideDestinationTitle: String = "",
    val driverPlateNumber: String = "",
    val status: String = "PENDING_ADMIN_REVIEW", // PENDING_ADMIN_REVIEW, UNDER_INVESTIGATION, RESOLVED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val blockerUserId: String = "",
    val blockedUserId: String = "",
    val blockedUserName: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)



