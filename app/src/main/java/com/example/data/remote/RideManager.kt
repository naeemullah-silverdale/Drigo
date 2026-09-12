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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * RideManager singleton acting as a reactive mirror/cache of the authoritative Firebase
 * ride record at `/ride_requests/{requestId}` and `/active_trips/{requestId}`.
 * Local state caches and renders Firebase data without acting as an independent source of truth.
 */
object RideManager {
    private const val TAG = "RideManager"
    private const val RIDE_REQUESTS_COLLECTION = "ride_requests"
    private const val ACTIVE_TRIPS_COLLECTION = "active_trips"

    private val _activeTrip = MutableStateFlow<PassengerOrder?>(null)
    val activeTrip: StateFlow<PassengerOrder?> = _activeTrip.asStateFlow()

    private var activeObservedId: String? = null
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
     * Start observing the authoritative ride request at `/ride_requests/{requestId}`
     * and mirror at `/active_trips/{requestId}`.
     */
    fun observeActiveTrip(requestId: String) {
        val cleanId = requestId.trim()
        if (cleanId.isBlank()) return
        if (activeObservedId == cleanId && rtdbListener != null) {
            return
        }

        stopObserving()
        activeObservedId = cleanId

        val db = getRtdb()
        if (db != null) {
            try {
                rtdbRef = db.getReference(RIDE_REQUESTS_COLLECTION).child(cleanId)
                rtdbListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val order = parseRtdbTrip(snapshot)
                            if (order != null) {
                                Log.d(TAG, "Authoritative ride update: id=${order.id}, reqId=${order.requestId}, status=${order.status}")
                                _activeTrip.value = order
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB ride listener cancelled: ${error.message}")
                    }
                }
                rtdbRef?.addValueEventListener(rtdbListener!!)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to attach RTDB listener: ${e.message}")
            }
        }

        val firestore = getFirestore()
        if (firestore != null) {
            try {
                firestoreRegistration = firestore.collection(RIDE_REQUESTS_COLLECTION)
                    .document(cleanId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null || !snapshot.exists()) {
                            return@addSnapshotListener
                        }
                        val order = parseFirestoreTrip(snapshot)
                        if (order != null) {
                            _activeTrip.value = order
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to attach Firestore snapshot listener: ${e.message}")
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
        activeObservedId = null
    }

    /**
     * Cache and mirror an active trip locally and in Firebase.
     */
    suspend fun saveActiveTrip(order: PassengerOrder): Result<Unit> {
        val reqId = order.requestId.ifBlank { order.id }
        if (reqId.isBlank()) return Result.failure(IllegalArgumentException("Request ID cannot be blank"))

        _activeTrip.value = order
        observeActiveTrip(reqId)
        return Result.success(Unit)
    }

    /**
     * Updates trip status locally and mirrors to authoritative path.
     */
    suspend fun updateTripStatus(
        orderId: String,
        status: PassengerOrderStatus,
        requestId: String = "",
        passengerId: String = "",
        driverId: String = "",
        finalFare: Int? = null,
        tripDetails: PassengerOrder? = null
    ): Result<Unit> {
        val reqId = requestId.ifBlank { orderId }
        if (reqId.isBlank()) return Result.failure(IllegalArgumentException("Request ID cannot be blank"))

        // Update local StateFlow mirror
        val current = _activeTrip.value
        val updated = (current ?: tripDetails)?.copy(
            status = status,
            agreedFare = finalFare ?: (current?.agreedFare ?: (tripDetails?.agreedFare ?: 0))
        )
        if (updated != null) {
            _activeTrip.value = updated
        }

        return Result.success(Unit)
    }

    /**
     * Completes trip locally and updates StateFlow mirror.
     */
    suspend fun completeTrip(
        orderId: String,
        requestId: String = "",
        passengerId: String = "",
        driverId: String = "",
        finalFare: Int? = null,
        tripDetails: PassengerOrder? = null
    ): Result<Unit> {
        val res = updateTripStatus(
            orderId = orderId,
            status = PassengerOrderStatus.COMPLETED,
            requestId = requestId,
            passengerId = passengerId,
            driverId = driverId,
            finalFare = finalFare,
            tripDetails = tripDetails
        )
        return res
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
                agreedFare = (snapshot.getLong("agreedFare") ?: (snapshot.getLong("assignedFare") ?: 0L)).toInt(),
                paymentMethod = snapshot.getString("paymentMethod") ?: "Cash",
                driverName = snapshot.getString("driverName") ?: (snapshot.getString("assignedDriverName") ?: ""),
                driverPhone = snapshot.getString("driverPhone") ?: "",
                driverRating = snapshot.getDouble("driverRating") ?: 5.0,
                driverTotalRides = (snapshot.getLong("driverTotalRides") ?: 0L).toInt(),
                driverVehicleMake = snapshot.getString("driverVehicleMake") ?: "",
                driverVehicleModel = snapshot.getString("driverVehicleModel") ?: "",
                driverVehicleColor = snapshot.getString("driverVehicleColor") ?: "",
                driverPlateNumber = snapshot.getString("driverPlateNumber") ?: "",
                assignedDriverId = snapshot.getString("assignedDriverId") ?: (snapshot.getString("driverId") ?: ""),
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
                    ?: (snapshot.child("assignedFare").getValue(Int::class.java)
                    ?: snapshot.child("agreedFare").getValue(Long::class.java)?.toInt() ?: (snapshot.child("assignedFare").getValue(Long::class.java)?.toInt() ?: 0)),
                paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "Cash",
                driverName = snapshot.child("driverName").getValue(String::class.java)
                    ?: (snapshot.child("assignedDriverName").getValue(String::class.java) ?: ""),
                driverPhone = snapshot.child("driverPhone").getValue(String::class.java) ?: "",
                driverRating = snapshot.child("driverRating").getValue(Double::class.java) ?: 5.0,
                driverTotalRides = snapshot.child("driverTotalRides").getValue(Int::class.java)
                    ?: snapshot.child("driverTotalRides").getValue(Long::class.java)?.toInt() ?: 0,
                driverVehicleMake = snapshot.child("driverVehicleMake").getValue(String::class.java) ?: "",
                driverVehicleModel = snapshot.child("driverVehicleModel").getValue(String::class.java) ?: "",
                driverVehicleColor = snapshot.child("driverVehicleColor").getValue(String::class.java) ?: "",
                driverPlateNumber = snapshot.child("driverPlateNumber").getValue(String::class.java) ?: "",
                assignedDriverId = snapshot.child("assignedDriverId").getValue(String::class.java)
                    ?: (snapshot.child("driverId").getValue(String::class.java) ?: ""),
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
