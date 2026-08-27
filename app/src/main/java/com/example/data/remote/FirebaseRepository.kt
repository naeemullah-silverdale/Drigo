package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class FirebaseUserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false
)

enum class CloudSyncStatus {
    CONNECTED,
    SYNCING,
    OFFLINE_LOCAL,
    ERROR
}

data class LiveDriverTelemetry(
    val tripId: String = "",
    val driverId: String = "",
    val progressRatio: Float = 0f,
    val speedKmH: Int = 0,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

class FirebaseRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseRepository"
        private const val TRIPS_COLLECTION = "trips"
        private const val BOOKINGS_COLLECTION = "bookings"
        private const val CHAT_COLLECTION = "chat_messages"
        private const val PROFILES_COLLECTION = "user_profiles"
        private const val TELEMETRY_COLLECTION = "driver_telemetry"
        private const val RIDE_REQUESTS_COLLECTION = "ride_requests"

        @Volatile
        private var INSTANCE: FirebaseRepository? = null

        fun getInstance(context: Context): FirebaseRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var isFirebaseInitialized: Boolean = false
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private val _syncStatus = MutableStateFlow(CloudSyncStatus.OFFLINE_LOCAL)
    val syncStatus = _syncStatus.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<FirebaseUserProfile?>(null)
    val currentUserProfile = _currentUserProfile.asStateFlow()

    init {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                isFirebaseInitialized = true
                auth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                _syncStatus.value = CloudSyncStatus.CONNECTED
                checkCurrentUser()
                Log.d(TAG, "Firebase initialized successfully with default app.")
            } else {
                Log.w(TAG, "No FirebaseApp instances found. Running in offline/local-first mode.")
                _syncStatus.value = CloudSyncStatus.OFFLINE_LOCAL
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization fallback: ${e.message}")
            _syncStatus.value = CloudSyncStatus.OFFLINE_LOCAL
        }
    }

    fun isAvailable(): Boolean = isFirebaseInitialized && firestore != null

    private fun checkCurrentUser() {
        auth?.currentUser?.let { user ->
            _currentUserProfile.value = mapFirebaseUser(user)
        }
    }

    private fun mapFirebaseUser(user: FirebaseUser): FirebaseUserProfile {
        return FirebaseUserProfile(
            uid = user.uid,
            displayName = user.displayName ?: "Naeem Ullah",
            email = user.email ?: "naeemullahsilverdale@gmail.com",
            photoUrl = user.photoUrl?.toString(),
            isAnonymous = user.isAnonymous
        )
    }

    // --- Authentication ---
    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUserProfile> {
        return try {
            val authInstance = auth ?: throw IllegalStateException("Firebase Auth not initialized")
            _syncStatus.value = CloudSyncStatus.SYNCING
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = authInstance.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("User null after Google sign in")
            val profile = mapFirebaseUser(user)
            _currentUserProfile.value = profile
            _syncStatus.value = CloudSyncStatus.CONNECTED
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with Google: ${e.message}", e)
            _syncStatus.value = CloudSyncStatus.ERROR
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            _currentUserProfile.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}")
        }
    }

    // --- Trips Firestore Sync ---

    suspend fun pushTripToCloud(trip: TripEntity): Result<Unit> {
        if (!isAvailable()) return Result.success(Unit)
        return try {
            _syncStatus.value = CloudSyncStatus.SYNCING
            val tripMap = mapOf(
                "id" to trip.id,
                "driverId" to trip.driverId,
                "driverName" to trip.driverName,
                "driverRating" to trip.driverRating.toDouble(),
                "driverTotalRides" to trip.driverTotalRides,
                "driverPhone" to trip.driverPhone,
                "originCity" to trip.originCity,
                "originAddress" to trip.originAddress,
                "originLat" to trip.originLat,
                "originLon" to trip.originLon,
                "destinationCity" to trip.destinationCity,
                "destinationAddress" to trip.destinationAddress,
                "destinationLat" to trip.destinationLat,
                "destinationLon" to trip.destinationLon,
                "departureDate" to trip.departureDate,
                "departureTime" to trip.departureTime,
                "estimatedDurationHours" to trip.estimatedDurationHours,
                "totalDistanceKm" to trip.totalDistanceKm,
                "pricePerSeat" to trip.pricePerSeat,
                "totalSeats" to trip.totalSeats,
                "availableSeats" to trip.availableSeats,
                "vehicleMake" to trip.vehicleMake,
                "vehicleModel" to trip.vehicleModel,
                "vehicleColor" to trip.vehicleColor,
                "vehiclePlate" to trip.vehiclePlate,
                "vehicleType" to trip.vehicleType,
                "luggageAllowance" to trip.luggageAllowance.name,
                "recurringFrequency" to trip.recurringFrequency.name,
                "recurringDays" to trip.recurringDays,
                "waypointsJson" to trip.waypointsJson,
                "status" to trip.status.name,
                "allowsPets" to trip.allowsPets,
                "allowsSmoking" to trip.allowsSmoking,
                "maxTwoInBack" to trip.maxTwoInBack,
                "musicVibe" to trip.musicVibe,
                "specialNotes" to trip.specialNotes,
                "instantBooking" to trip.instantBooking,
                "createdAtTimestamp" to trip.createdAtTimestamp
            )
            firestore!!.collection(TRIPS_COLLECTION).document(trip.id).set(tripMap).await()
            _syncStatus.value = CloudSyncStatus.CONNECTED
            Log.d(TAG, "Trip synced to Firestore: ${trip.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push trip to Firestore: ${e.message}")
            _syncStatus.value = CloudSyncStatus.CONNECTED
            Result.failure(e)
        }
    }

    suspend fun updateTripSeatsInCloud(tripId: String, availableSeats: Int) {
        if (!isAvailable()) return
        try {
            firestore!!.collection(TRIPS_COLLECTION).document(tripId)
                .update("availableSeats", availableSeats).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update available seats in cloud: ${e.message}")
        }
    }

    fun listenToCloudTrips(): Flow<List<TripEntity>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = firestore!!.collection(TRIPS_COLLECTION)
            .orderBy("createdAtTimestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to trips failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val tripList = snapshot.documents.mapNotNull { doc ->
                        try {
                            TripEntity(
                                id = doc.getString("id") ?: doc.id,
                                driverId = doc.getString("driverId") ?: "driver_1",
                                driverName = doc.getString("driverName") ?: "Driver",
                                driverRating = (doc.getDouble("driverRating") ?: 4.9).toFloat(),
                                driverTotalRides = (doc.getLong("driverTotalRides") ?: 10).toInt(),
                                driverPhone = doc.getString("driverPhone") ?: "+92 300 1234567",
                                originCity = doc.getString("originCity") ?: "Islamabad",
                                originAddress = doc.getString("originAddress") ?: "Zero Point",
                                originLat = doc.getDouble("originLat") ?: 33.6844,
                                originLon = doc.getDouble("originLon") ?: 73.0479,
                                destinationCity = doc.getString("destinationCity") ?: "Lahore",
                                destinationAddress = doc.getString("destinationAddress") ?: "Thokar Niaz Baig",
                                destinationLat = doc.getDouble("destinationLat") ?: 31.4697,
                                destinationLon = doc.getDouble("destinationLon") ?: 74.2498,
                                departureDate = doc.getString("departureDate") ?: "Today",
                                departureTime = doc.getString("departureTime") ?: "07:30 AM",
                                estimatedDurationHours = doc.getDouble("estimatedDurationHours") ?: 4.0,
                                totalDistanceKm = doc.getDouble("totalDistanceKm") ?: 375.0,
                                pricePerSeat = doc.getDouble("pricePerSeat") ?: 1800.0,
                                totalSeats = (doc.getLong("totalSeats") ?: 3).toInt(),
                                availableSeats = (doc.getLong("availableSeats") ?: 3).toInt(),
                                vehicleMake = doc.getString("vehicleMake") ?: "Toyota",
                                vehicleModel = doc.getString("vehicleModel") ?: "Corolla",
                                vehicleColor = doc.getString("vehicleColor") ?: "White",
                                vehiclePlate = doc.getString("vehiclePlate") ?: "ICT-8821",
                                vehicleType = doc.getString("vehicleType") ?: "Sedan",
                                luggageAllowance = try {
                                    LuggageAllowance.valueOf(doc.getString("luggageAllowance") ?: "MEDIUM")
                                } catch (e: Exception) { LuggageAllowance.MEDIUM },
                                recurringFrequency = try {
                                    RecurringFrequency.valueOf(doc.getString("recurringFrequency") ?: "NONE")
                                } catch (e: Exception) { RecurringFrequency.NONE },
                                recurringDays = doc.getString("recurringDays") ?: "",
                                waypointsJson = doc.getString("waypointsJson") ?: "[]",
                                status = try {
                                    TripStatus.valueOf(doc.getString("status") ?: "SCHEDULED")
                                } catch (e: Exception) { TripStatus.SCHEDULED },
                                allowsPets = doc.getBoolean("allowsPets") ?: false,
                                allowsSmoking = doc.getBoolean("allowsSmoking") ?: false,
                                maxTwoInBack = doc.getBoolean("maxTwoInBack") ?: true,
                                musicVibe = doc.getString("musicVibe") ?: "Coke Studio & Chill",
                                specialNotes = doc.getString("specialNotes") ?: "",
                                instantBooking = doc.getBoolean("instantBooking") ?: true,
                                createdAtTimestamp = doc.getLong("createdAtTimestamp") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing trip document: ${doc.id}", e)
                            null
                        }
                    }
                    trySend(tripList)
                }
            }

        awaitClose { registration.remove() }
    }

    // --- Bookings Firestore Sync ---

    suspend fun pushBookingToCloud(booking: BookingEntity): Result<Unit> {
        if (!isAvailable()) return Result.success(Unit)
        return try {
            val bookingMap = mapOf(
                "id" to booking.id,
                "tripId" to booking.tripId,
                "passengerId" to booking.passengerId,
                "passengerName" to booking.passengerName,
                "passengerPhone" to booking.passengerPhone,
                "pickupLocation" to booking.pickupLocation,
                "dropoffLocation" to booking.dropoffLocation,
                "seatsBooked" to booking.seatsBooked,
                "seatNumbers" to booking.seatNumbers,
                "totalPrice" to booking.totalPrice,
                "bookingCode" to booking.bookingCode,
                "status" to booking.status.name,
                "luggageCount" to booking.luggageCount,
                "bookedAtTimestamp" to booking.bookedAtTimestamp,
                "qrToken" to booking.qrToken
            )
            firestore!!.collection(BOOKINGS_COLLECTION).document(booking.id).set(bookingMap).await()
            Log.d(TAG, "Booking synced to Firestore: ${booking.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push booking to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Chat Firestore Sync ---

    suspend fun pushChatMessageToCloud(message: ChatMessageEntity): Result<Unit> {
        if (!isAvailable()) return Result.success(Unit)
        return try {
            val messageMap = mapOf(
                "id" to message.id,
                "tripId" to message.tripId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "isDriver" to message.isDriver,
                "messageText" to message.messageText,
                "timestamp" to message.timestamp,
                "isRead" to message.isRead,
                "isSystemNotice" to message.isSystemNotice
            )
            firestore!!.collection(CHAT_COLLECTION).document(message.id).set(messageMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push message to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    fun listenToCloudMessages(tripId: String): Flow<List<ChatMessageEntity>> = callbackFlow {
        if (!isAvailable() || tripId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore!!.collection(CHAT_COLLECTION)
            .whereEqualTo("tripId", tripId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to chat messages failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            ChatMessageEntity(
                                id = doc.getString("id") ?: doc.id,
                                tripId = doc.getString("tripId") ?: tripId,
                                senderId = doc.getString("senderId") ?: "user",
                                senderName = doc.getString("senderName") ?: "User",
                                isDriver = doc.getBoolean("isDriver") ?: false,
                                messageText = doc.getString("messageText") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                isRead = doc.getBoolean("isRead") ?: true,
                                isSystemNotice = doc.getBoolean("isSystemNotice") ?: false
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(messages)
                }
            }

        awaitClose { registration.remove() }
    }

    // --- Driver Realtime Telemetry Broadcast ---

    suspend fun broadcastTelemetry(telemetry: LiveDriverTelemetry) {
        if (!isAvailable()) return
        try {
            firestore!!.collection(TELEMETRY_COLLECTION).document(telemetry.tripId).set(telemetry).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error broadcasting telemetry: ${e.message}")
        }
    }

    fun listenToDriverTelemetry(tripId: String): Flow<LiveDriverTelemetry?> = callbackFlow {
        if (!isAvailable() || tripId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = firestore!!.collection(TELEMETRY_COLLECTION).document(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val telemetry = try {
                        LiveDriverTelemetry(
                            tripId = snapshot.getString("tripId") ?: tripId,
                            driverId = snapshot.getString("driverId") ?: "",
                            progressRatio = (snapshot.getDouble("progressRatio") ?: 0.0).toFloat(),
                            speedKmH = (snapshot.getLong("speedKmH") ?: 0).toInt(),
                            lat = snapshot.getDouble("lat") ?: 0.0,
                            lon = snapshot.getDouble("lon") ?: 0.0,
                            timestamp = snapshot.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) { null }
                    trySend(telemetry)
                }
            }

        awaitClose { registration.remove() }
    }

    // --- User Profile Sync ---

    suspend fun pushUserProfileToCloud(profile: UserPreferenceEntity) {
        if (!isAvailable()) return
        try {
            val userMap = mapOf(
                "userId" to profile.userId,
                "userName" to profile.userName,
                "userPhone" to profile.userPhone,
                "isDriverMode" to profile.isDriverMode,
                "homeCity" to profile.homeCity,
                "workCity" to profile.workCity,
                "defaultCommuteDeparture" to profile.defaultCommuteDeparture,
                "musicPreference" to profile.musicPreference,
                "totalRidesAsPassenger" to profile.totalRidesAsPassenger,
                "totalRidesAsDriver" to profile.totalRidesAsDriver,
                "totalCo2SavedKg" to profile.totalCo2SavedKg,
                "totalMoneySavedUsd" to profile.totalMoneySavedUsd
            )
            firestore!!.collection(PROFILES_COLLECTION).document(profile.userId).set(userMap).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing user profile: ${e.message}")
        }
    }

    // --- Passenger Ride Requests (Realtime Database & Firestore Sync) ---

    suspend fun createRideRequest(request: RideRequest): Result<String> {
        return try {
            val requestMap = mapOf(
                "id" to request.id,
                "passengerId" to request.passengerId,
                "passengerName" to request.passengerName,
                "passengerEmail" to request.passengerEmail,
                "pickupTitle" to request.pickupTitle,
                "pickupSubtitle" to request.pickupSubtitle,
                "pickupLat" to request.pickupLat,
                "pickupLon" to request.pickupLon,
                "destinationTitle" to request.destinationTitle,
                "destinationSubtitle" to request.destinationSubtitle,
                "destinationLat" to request.destinationLat,
                "destinationLon" to request.destinationLon,
                "rideCategory" to request.rideCategory,
                "estimatedFare" to request.estimatedFare,
                "distanceKm" to request.distanceKm,
                "durationMinutes" to request.durationMinutes,
                "status" to request.status,
                "timestamp" to request.timestamp
            )

            // 1. Write to Firebase Realtime Database
            try {
                val db = FirebaseDatabase.getInstance("https://drigo-8b15c-default-rtdb.firebaseio.com")
                val reqRef = db.getReference("ride_requests").child(request.id)
                reqRef.setValue(requestMap)
                if (request.passengerId.isNotBlank()) {
                    db.getReference("users").child(request.passengerId).child("active_ride_request")
                        .setValue(requestMap)
                }
                Log.d(TAG, "Ride request created in Firebase Realtime Database: ${request.id}")
            } catch (rtdbErr: Exception) {
                Log.w(TAG, "Realtime Database write notice: ${rtdbErr.message}")
            }

            // 2. Write to Cloud Firestore if available
            if (isAvailable()) {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(4000L) {
                        firestore!!.collection(RIDE_REQUESTS_COLLECTION).document(request.id).set(requestMap).await()
                    }
                    Log.d(TAG, "Ride request synced to Firestore collection '${RIDE_REQUESTS_COLLECTION}': ${request.id}")
                } catch (fsErr: Exception) {
                    Log.w(TAG, "Firestore sync notice: ${fsErr.message}")
                }
            }

            Result.success(request.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ride request in Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun listenToRideRequests(): Flow<List<RideRequest>> = callbackFlow {
        if (!isAvailable()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore!!.collection(RIDE_REQUESTS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to ride requests error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        try {
                            RideRequest(
                                id = doc.getString("id") ?: doc.id,
                                passengerId = doc.getString("passengerId") ?: "",
                                passengerName = doc.getString("passengerName") ?: "Passenger",
                                passengerEmail = doc.getString("passengerEmail") ?: "",
                                pickupTitle = doc.getString("pickupTitle") ?: "",
                                pickupSubtitle = doc.getString("pickupSubtitle") ?: "",
                                pickupLat = doc.getDouble("pickupLat") ?: 0.0,
                                pickupLon = doc.getDouble("pickupLon") ?: 0.0,
                                destinationTitle = doc.getString("destinationTitle") ?: "",
                                destinationSubtitle = doc.getString("destinationSubtitle") ?: "",
                                destinationLat = doc.getDouble("destinationLat") ?: 0.0,
                                destinationLon = doc.getDouble("destinationLon") ?: 0.0,
                                rideCategory = doc.getString("rideCategory") ?: "Share Ride",
                                estimatedFare = (doc.getLong("estimatedFare") ?: 0).toInt(),
                                distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                                durationMinutes = (doc.getLong("durationMinutes") ?: 0).toInt(),
                                status = doc.getString("status") ?: "SEARCHING_DRIVERS",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(requests)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateRideRequestStatus(requestId: String, status: String) {
        try {
            FirebaseDatabase.getInstance().getReference("ride_requests")
                .child(requestId).child("status").setValue(status).await()
        } catch (_: Exception) {}

        if (isAvailable()) {
            try {
                firestore!!.collection(RIDE_REQUESTS_COLLECTION).document(requestId)
                    .update("status", status).await()
            } catch (_: Exception) {}
        }
    }
}
