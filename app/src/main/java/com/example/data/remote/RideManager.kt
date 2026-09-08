package com.example.data.remote

import android.util.Log
import com.example.data.model.PassengerOrder
import com.example.data.model.PassengerOrderStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * RideManager singleton enforcing a unified, reactive state model for active trips.
 * Both Passenger and Driver modes observe the same 'active_trips/{tripId}' document in Firestore
 * and Realtime DB to ensure 100% synchronous state transitions across both apps.
 */
object RideManager {
    private const val TAG = "RideManager"
    private const val ACTIVE_TRIPS_COLLECTION = "active_trips"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeTrip = MutableStateFlow<PassengerOrder?>(null)
    val activeTrip: StateFlow<PassengerOrder?> = _activeTrip.asStateFlow()

    private var activeTripId: String? = null
    private var firestoreRegistration: ListenerRegistration? = null
    private var rtdbListener: ValueEventListener? = null
    private var rtdbRef: com.google.firebase.database.DatabaseReference? = null

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not available: ${e.message}")
            null
        }
    }

    private fun getRtdb(): FirebaseDatabase? {
        return try {
            FirebaseDatabase.getInstance("https://drigo-8b15c-default-rtdb.firebaseio.com")
        } catch (_: Exception) {
            try {
                FirebaseDatabase.getInstance()
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Start observing active_trips/{tripId} document in Firestore and Realtime Database.
     * Both Passenger and Driver UIs observe this exact same reactive flow.
     */
    fun observeActiveTrip(tripId: String) {
        val cleanTripId = tripId.trim()
        if (cleanTripId.isBlank()) return
        if (activeTripId == cleanTripId && firestoreRegistration != null) {
            return
        }

        stopObserving()
        activeTripId = cleanTripId

        // 1. Listen to Firestore active_trips/{tripId}
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                firestoreRegistration = firestore.collection(ACTIVE_TRIPS_COLLECTION)
                    .document(cleanTripId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Firestore active_trips listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val order = parseFirestoreTrip(snapshot)
                            if (order != null) {
                                Log.d(TAG, "Reactive active_trips update from Firestore: id=${order.id}, status=${order.status}")
                                _activeTrip.value = order
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to attach Firestore snapshot listener: ${e.message}")
            }
        }

        // 2. Listen to Realtime Database active_trips/{tripId} for dual fallback/instant sync
        val rtdb = getRtdb()
        if (rtdb != null) {
            try {
                rtdbRef = rtdb.getReference(ACTIVE_TRIPS_COLLECTION).child(cleanTripId)
                rtdbListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val order = parseRtdbTrip(snapshot)
                            if (order != null) {
                                Log.d(TAG, "Reactive active_trips update from RTDB: id=${order.id}, status=${order.status}")
                                _activeTrip.value = order
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB active_trips listener cancelled: ${error.message}")
                    }
                }
                rtdbRef?.addValueEventListener(rtdbListener!!)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to attach RTDB listener: ${e.message}")
            }
        }
    }

    /**
     * Stop active observation listeners.
     */
    fun stopObserving() {
        firestoreRegistration?.remove()
        firestoreRegistration = null

        rtdbListener?.let { listener ->
            rtdbRef?.removeEventListener(listener)
        }
        rtdbListener = null
        rtdbRef = null
        activeTripId = null
    }

    /**
     * Save/Publish a trip to active_trips/{tripId} in Firestore and Realtime Database.
     */
    suspend fun saveActiveTrip(order: PassengerOrder): Result<Unit> {
        val tripId = order.id.ifBlank { order.requestId }
        if (tripId.isBlank()) return Result.failure(IllegalArgumentException("Trip ID cannot be blank"))

        _activeTrip.value = order
        observeActiveTrip(tripId)

        val map = mapOf(
            "id" to order.id,
            "requestId" to order.requestId,
            "passengerId" to order.passengerId,
            "passengerName" to order.passengerName,
            "passengerEmail" to order.passengerEmail,
            "passengerPhone" to order.passengerPhone,
            "pickupTitle" to order.pickupTitle,
            "pickupSubtitle" to order.pickupSubtitle,
            "pickupLat" to order.pickupLat,
            "pickupLon" to order.pickupLon,
            "destinationTitle" to order.destinationTitle,
            "destinationSubtitle" to order.destinationSubtitle,
            "destinationLat" to order.destinationLat,
            "destinationLon" to order.destinationLon,
            "distanceKm" to order.distanceKm,
            "durationMinutes" to order.durationMinutes,
            "rideCategory" to order.rideCategory,
            "agreedFare" to order.agreedFare,
            "paymentMethod" to order.paymentMethod,
            "driverName" to order.driverName,
            "driverPhone" to order.driverPhone,
            "driverRating" to order.driverRating,
            "driverTotalRides" to order.driverTotalRides,
            "driverVehicleMake" to order.driverVehicleMake,
            "driverVehicleModel" to order.driverVehicleModel,
            "driverVehicleColor" to order.driverVehicleColor,
            "driverPlateNumber" to order.driverPlateNumber,
            "assignedDriverId" to order.assignedDriverId,
            "status" to order.status.name,
            "statusLabel" to order.status.label,
            "etaMinutes" to order.etaMinutes,
            "createdAt" to order.createdAt,
            "updatedAt" to System.currentTimeMillis()
        )

        // Write to Firestore active_trips/{tripId}
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                firestore.collection(ACTIVE_TRIPS_COLLECTION).document(tripId).set(map).await()
                if (order.requestId.isNotBlank() && order.requestId != tripId) {
                    firestore.collection(ACTIVE_TRIPS_COLLECTION).document(order.requestId).set(map).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error writing active_trips to Firestore: ${e.message}")
            }
        }

        // Write to RTDB active_trips/{tripId}
        val rtdb = getRtdb()
        if (rtdb != null) {
            try {
                rtdb.getReference(ACTIVE_TRIPS_COLLECTION).child(tripId).setValue(map).await()
                if (order.requestId.isNotBlank() && order.requestId != tripId) {
                    rtdb.getReference(ACTIVE_TRIPS_COLLECTION).child(order.requestId).setValue(map).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error writing active_trips to RTDB: ${e.message}")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Update active trip status in active_trips/{tripId} across Firestore and Realtime Database.
     * Both Passenger and Driver listeners observe this change instantly.
     */
    suspend fun updateTripStatus(
        orderId: String,
        status: PassengerOrderStatus,
        requestId: String = "",
        passengerId: String = "",
        driverId: String = "",
        finalFare: Int? = null
    ): Result<Unit> {
        val tripId = orderId.ifBlank { requestId }
        if (tripId.isBlank()) return Result.failure(IllegalArgumentException("Order ID cannot be blank"))

        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any>(
            "status" to status.name,
            "statusLabel" to status.label,
            "updatedAt" to now
        )
        if (finalFare != null && finalFare > 0) {
            updates["agreedFare"] = finalFare
        }
        if (status == PassengerOrderStatus.COMPLETED) {
            updates["completedAt"] = now
        } else if (status == PassengerOrderStatus.CANCELLED) {
            updates["cancelledAt"] = now
        }

        // Immediately update in-memory active trip state so local UI updates immediately
        _activeTrip.value?.let { current ->
            if (current.id == tripId || current.requestId == tripId || current.id == orderId || current.requestId == requestId) {
                _activeTrip.value = current.copy(
                    status = status,
                    agreedFare = finalFare ?: current.agreedFare
                )
            }
        }

        // 1. Update Firestore active_trips/{tripId}
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                firestore.collection(ACTIVE_TRIPS_COLLECTION).document(tripId).update(updates).await()
                if (orderId.isNotBlank() && orderId != tripId) {
                    firestore.collection(ACTIVE_TRIPS_COLLECTION).document(orderId).update(updates).await()
                }
                if (requestId.isNotBlank() && requestId != tripId) {
                    firestore.collection(ACTIVE_TRIPS_COLLECTION).document(requestId).update(updates).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore active_trips update status error: ${e.message}")
            }
        }

        // 2. Update RTDB active_trips/{tripId}
        val rtdb = getRtdb()
        if (rtdb != null) {
            try {
                rtdb.getReference(ACTIVE_TRIPS_COLLECTION).child(tripId).updateChildren(updates).await()
                if (orderId.isNotBlank() && orderId != tripId) {
                    rtdb.getReference(ACTIVE_TRIPS_COLLECTION).child(orderId).updateChildren(updates).await()
                }
                if (requestId.isNotBlank() && requestId != tripId) {
                    rtdb.getReference(ACTIVE_TRIPS_COLLECTION).child(requestId).updateChildren(updates).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "RTDB active_trips update status error: ${e.message}")
            }
        }

        // Delayed cleanup of active_trips node so both Passenger and Driver apps process COMPLETED/CANCELLED event first
        if (status == PassengerOrderStatus.COMPLETED || status == PassengerOrderStatus.CANCELLED) {
            scope.launch {
                try {
                    kotlinx.coroutines.delay(5000L)
                    firestore?.collection(ACTIVE_TRIPS_COLLECTION)?.document(tripId)?.delete()?.await()
                    rtdb?.getReference(ACTIVE_TRIPS_COLLECTION)?.child(tripId)?.removeValue()?.await()
                } catch (_: Exception) {}
            }
        }

        return Result.success(Unit)
    }

    /**
     * Clear current active trip state.
     */
    fun clearActiveTrip() {
        _activeTrip.value = null
        stopObserving()
    }

    private fun parseFirestoreTrip(snapshot: com.google.firebase.firestore.DocumentSnapshot): PassengerOrder? {
        return try {
            val statusStr = snapshot.getString("status") ?: "SEARCHING"
            val status = try {
                PassengerOrderStatus.valueOf(statusStr)
            } catch (_: Exception) {
                PassengerOrderStatus.SEARCHING
            }

            PassengerOrder(
                id = snapshot.getString("id") ?: snapshot.id,
                requestId = snapshot.getString("requestId") ?: snapshot.id,
                passengerId = snapshot.getString("passengerId") ?: "",
                passengerName = snapshot.getString("passengerName") ?: "",
                passengerEmail = snapshot.getString("passengerEmail") ?: "",
                passengerPhone = snapshot.getString("passengerPhone") ?: "",
                pickupTitle = snapshot.getString("pickupTitle") ?: "",
                pickupSubtitle = snapshot.getString("pickupSubtitle") ?: "",
                pickupLat = snapshot.getDouble("pickupLat") ?: 0.0,
                pickupLon = snapshot.getDouble("pickupLon") ?: 0.0,
                destinationTitle = snapshot.getString("destinationTitle") ?: "",
                destinationSubtitle = snapshot.getString("destinationSubtitle") ?: "",
                destinationLat = snapshot.getDouble("destinationLat") ?: 0.0,
                destinationLon = snapshot.getDouble("destinationLon") ?: 0.0,
                distanceKm = snapshot.getDouble("distanceKm") ?: 0.0,
                durationMinutes = (snapshot.getLong("durationMinutes") ?: 0L).toInt(),
                rideCategory = snapshot.getString("rideCategory") ?: "Ride A/C",
                agreedFare = (snapshot.getLong("agreedFare") ?: 0L).toInt(),
                paymentMethod = snapshot.getString("paymentMethod") ?: "Cash",
                driverName = snapshot.getString("driverName") ?: "",
                driverPhone = snapshot.getString("driverPhone") ?: "",
                driverRating = snapshot.getDouble("driverRating") ?: 5.0,
                driverTotalRides = (snapshot.getLong("driverTotalRides") ?: 0L).toInt(),
                driverVehicleMake = snapshot.getString("driverVehicleMake") ?: "",
                driverVehicleModel = snapshot.getString("driverVehicleModel") ?: "",
                driverVehicleColor = snapshot.getString("driverVehicleColor") ?: "",
                driverPlateNumber = snapshot.getString("driverPlateNumber") ?: "",
                assignedDriverId = snapshot.getString("assignedDriverId") ?: "",
                status = status,
                etaMinutes = (snapshot.getLong("etaMinutes") ?: 0L).toInt(),
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing Firestore trip: ${e.message}")
            null
        }
    }

    private fun parseRtdbTrip(snapshot: DataSnapshot): PassengerOrder? {
        return try {
            val statusStr = snapshot.child("status").getValue(String::class.java) ?: "SEARCHING"
            val status = try {
                PassengerOrderStatus.valueOf(statusStr)
            } catch (_: Exception) {
                PassengerOrderStatus.SEARCHING
            }

            PassengerOrder(
                id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: "",
                requestId = snapshot.child("requestId").getValue(String::class.java) ?: snapshot.key ?: "",
                passengerId = snapshot.child("passengerId").getValue(String::class.java) ?: "",
                passengerName = snapshot.child("passengerName").getValue(String::class.java) ?: "",
                passengerEmail = snapshot.child("passengerEmail").getValue(String::class.java) ?: "",
                passengerPhone = snapshot.child("passengerPhone").getValue(String::class.java) ?: "",
                pickupTitle = snapshot.child("pickupTitle").getValue(String::class.java) ?: "",
                pickupSubtitle = snapshot.child("pickupSubtitle").getValue(String::class.java) ?: "",
                pickupLat = snapshot.child("pickupLat").getValue(Double::class.java) ?: 0.0,
                pickupLon = snapshot.child("pickupLon").getValue(Double::class.java) ?: 0.0,
                destinationTitle = snapshot.child("destinationTitle").getValue(String::class.java) ?: "",
                destinationSubtitle = snapshot.child("destinationSubtitle").getValue(String::class.java) ?: "",
                destinationLat = snapshot.child("destinationLat").getValue(Double::class.java) ?: 0.0,
                destinationLon = snapshot.child("destinationLon").getValue(Double::class.java) ?: 0.0,
                distanceKm = snapshot.child("distanceKm").getValue(Double::class.java) ?: 0.0,
                durationMinutes = snapshot.child("durationMinutes").getValue(Int::class.java)
                    ?: snapshot.child("durationMinutes").getValue(Long::class.java)?.toInt() ?: 0,
                rideCategory = snapshot.child("rideCategory").getValue(String::class.java) ?: "Ride A/C",
                agreedFare = snapshot.child("agreedFare").getValue(Int::class.java)
                    ?: snapshot.child("agreedFare").getValue(Long::class.java)?.toInt() ?: 0,
                paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "Cash",
                driverName = snapshot.child("driverName").getValue(String::class.java) ?: "",
                driverPhone = snapshot.child("driverPhone").getValue(String::class.java) ?: "",
                driverRating = snapshot.child("driverRating").getValue(Double::class.java) ?: 5.0,
                driverTotalRides = snapshot.child("driverTotalRides").getValue(Int::class.java)
                    ?: snapshot.child("driverTotalRides").getValue(Long::class.java)?.toInt() ?: 0,
                driverVehicleMake = snapshot.child("driverVehicleMake").getValue(String::class.java) ?: "",
                driverVehicleModel = snapshot.child("driverVehicleModel").getValue(String::class.java) ?: "",
                driverVehicleColor = snapshot.child("driverVehicleColor").getValue(String::class.java) ?: "",
                driverPlateNumber = snapshot.child("driverPlateNumber").getValue(String::class.java) ?: "",
                assignedDriverId = snapshot.child("assignedDriverId").getValue(String::class.java) ?: "",
                status = status,
                etaMinutes = snapshot.child("etaMinutes").getValue(Int::class.java)
                    ?: snapshot.child("etaMinutes").getValue(Long::class.java)?.toInt() ?: 0,
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing RTDB trip: ${e.message}")
            null
        }
    }
}
