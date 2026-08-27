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
