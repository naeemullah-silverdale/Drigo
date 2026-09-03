package com.example.data.remote

import android.util.Log
import com.example.data.model.RideRequest
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * RideRequestRepository manages the real-time stream of real passenger ride requests
 * directly from Firebase Realtime Database node "ride_requests".
 *
 * No hardcoded, mock, or demo requests are returned.
 */
class RideRequestRepository private constructor() {

    companion object {
        private const val TAG = "RideRequestRepo"
        private const val RTDB_URL = "https://drigo-8b15c-default-rtdb.firebaseio.com"
        private const val NODE_RIDE_REQUESTS = "ride_requests"

        @Volatile
        private var instance: RideRequestRepository? = null

        fun getInstance(): RideRequestRepository {
            return instance ?: synchronized(this) {
                instance ?: RideRequestRepository().also { instance = it }
            }
        }
    }

    private fun getDatabase(): FirebaseDatabase? {
        return try {
            FirebaseDatabase.getInstance(RTDB_URL)
        } catch (_: Exception) {
            try {
                FirebaseDatabase.getInstance()
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Attaches a ChildEventListener to the existing passenger ride-request node ("ride_requests").
     * Uses callbackFlow and ensures proper cleanup via awaitClose { removeEventListener(...) }.
     * Emits only real, valid, active passenger requests with status PENDING or SEARCHING.
     */
    fun getLiveRideRequests(): Flow<List<RideRequest>> = callbackFlow {
        val db = getDatabase()
        if (db == null) {
            Log.w(TAG, "FirebaseDatabase instance unavailable. Emitting empty list.")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val requestsMap = ConcurrentHashMap<String, RideRequest>()

        fun emitFilteredRequests() {
            val validList = requestsMap.values
                .filter { isEligibleActiveRequest(it) }
                .sortedByDescending { it.timestamp }
            trySend(validList)
        }

        val ref = db.getReference(NODE_RIDE_REQUESTS)

        val childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val req = parseSnapshotToRideRequest(snapshot)
                if (req != null && isEligibleActiveRequest(req)) {
                    requestsMap[req.id] = req
                    emitFilteredRequests()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val req = parseSnapshotToRideRequest(snapshot)
                if (req != null) {
                    if (isEligibleActiveRequest(req)) {
                        requestsMap[req.id] = req
                    } else {
                        // Request is no longer eligible (e.g. status CANCELLED, ACCEPTED, or assigned)
                        requestsMap.remove(req.id)
                    }
                    emitFilteredRequests()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val key = snapshot.key ?: snapshot.child("id").getValue(String::class.java)
                if (!key.isNullOrBlank()) {
                    requestsMap.remove(key)
                    emitFilteredRequests()
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Not used for unordered list
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "ChildEventListener on node '$NODE_RIDE_REQUESTS' cancelled: ${error.message}")
            }
        }

        // Initial emission so UI does not hang when node has no items
        trySend(emptyList())

        // Attach real-time ChildEventListener
        ref.addChildEventListener(childListener)
        Log.d(TAG, "Attached ChildEventListener to Realtime Database node '$NODE_RIDE_REQUESTS'")

        awaitClose {
            Log.d(TAG, "Detaching ChildEventListener from Realtime Database node '$NODE_RIDE_REQUESTS'")
            ref.removeEventListener(childListener)
            requestsMap.clear()
        }
    }

    /**
     * Safely parse a Firebase DataSnapshot to a RideRequest without crashing on missing/invalid fields.
     */
    private fun parseSnapshotToRideRequest(snapshot: DataSnapshot): RideRequest? {
        return try {
            val id = snapshot.child("id").getValue(String::class.java)?.ifBlank { null }
                ?: snapshot.key?.ifBlank { null }
                ?: return null

            // Exclude any mock/seed requests that may have been saved in test runs
            if (id.startsWith("req_seed_", ignoreCase = true) || id.startsWith("demo_", ignoreCase = true) || id.startsWith("mock_", ignoreCase = true)) {
                return null
            }

            val passengerId = snapshot.child("passengerId").getValue(String::class.java) ?: ""
            val passengerName = snapshot.child("passengerName").getValue(String::class.java) ?: "Passenger"
            val passengerEmail = snapshot.child("passengerEmail").getValue(String::class.java) ?: ""
            val passengerPhone = snapshot.child("passengerPhone").getValue(String::class.java) ?: ""
            val passengerPhotoUrl = snapshot.child("passengerPhotoUrl").getValue(String::class.java) ?: ""

            val ratingVal = snapshot.child("passengerRating").value
            val passengerRating = when (ratingVal) {
                is Number -> ratingVal.toDouble()
                is String -> ratingVal.toDoubleOrNull() ?: 4.9
                else -> 4.9
            }

            val paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "Cash"
            val pickupTitle = snapshot.child("pickupTitle").getValue(String::class.java) ?: ""
            val pickupSubtitle = snapshot.child("pickupSubtitle").getValue(String::class.java) ?: ""

            val pLatVal = snapshot.child("pickupLat").value
            val pickupLat = when (pLatVal) {
                is Number -> pLatVal.toDouble()
                is String -> pLatVal.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val pLonVal = snapshot.child("pickupLon").value
            val pickupLon = when (pLonVal) {
                is Number -> pLonVal.toDouble()
                is String -> pLonVal.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val destinationTitle = snapshot.child("destinationTitle").getValue(String::class.java) ?: ""
            val destinationSubtitle = snapshot.child("destinationSubtitle").getValue(String::class.java) ?: ""

            val dLatVal = snapshot.child("destinationLat").value
            val destinationLat = when (dLatVal) {
                is Number -> dLatVal.toDouble()
                is String -> dLatVal.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val dLonVal = snapshot.child("destinationLon").value
            val destinationLon = when (dLonVal) {
                is Number -> dLonVal.toDouble()
                is String -> dLonVal.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val rideCategory = snapshot.child("rideCategory").getValue(String::class.java) ?: "Ride A/C"
            val vehicleType = snapshot.child("vehicleType").getValue(String::class.java) ?: "Car"

            val hasAcVal = snapshot.child("hasAc").value
            val hasAc = when (hasAcVal) {
                is Boolean -> hasAcVal
                is String -> hasAcVal.toBoolean()
                is Number -> hasAcVal.toInt() != 0
                else -> rideCategory.contains("AC", true) || rideCategory.contains("A/C", true)
            }

            val fareVal = snapshot.child("estimatedFare").value
            val estimatedFare = when (fareVal) {
                is Number -> fareVal.toInt()
                is String -> fareVal.toIntOrNull() ?: 0
                else -> 0
            }

            val distVal = snapshot.child("distanceKm").value
            val distanceKm = when (distVal) {
                is Number -> distVal.toDouble()
                is String -> distVal.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val durVal = snapshot.child("durationMinutes").value
            val durationMinutes = when (durVal) {
                is Number -> durVal.toInt()
                is String -> durVal.toIntOrNull() ?: 0
                else -> 0
            }

            val status = snapshot.child("status").getValue(String::class.java) ?: "SEARCHING_DRIVERS"
            val assignedDriverId = snapshot.child("assignedDriverId").getValue(String::class.java) ?: ""

            val timeVal = snapshot.child("timestamp").value
            val timestamp = when (timeVal) {
                is Number -> timeVal.toLong()
                is String -> timeVal.toLongOrNull() ?: System.currentTimeMillis()
                else -> System.currentTimeMillis()
            }

            val expVal = snapshot.child("expiresAt").value
            val expiresAt = when (expVal) {
                is Number -> expVal.toLong()
                is String -> expVal.toLongOrNull() ?: 0L
                else -> 0L
            }

            RideRequest(
                id = id,
                passengerId = passengerId,
                passengerName = passengerName,
                passengerEmail = passengerEmail,
                passengerPhone = passengerPhone,
                passengerPhotoUrl = passengerPhotoUrl,
                passengerRating = passengerRating,
                paymentMethod = paymentMethod,
                pickupTitle = pickupTitle,
                pickupSubtitle = pickupSubtitle,
                pickupLat = pickupLat,
                pickupLon = pickupLon,
                destinationTitle = destinationTitle,
                destinationSubtitle = destinationSubtitle,
                destinationLat = destinationLat,
                destinationLon = destinationLon,
                rideCategory = rideCategory,
                vehicleType = vehicleType,
                hasAc = hasAc,
                estimatedFare = estimatedFare,
                distanceKm = distanceKm,
                durationMinutes = durationMinutes,
                status = status,
                assignedDriverId = assignedDriverId,
                timestamp = timestamp,
                expiresAt = expiresAt
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing ride request: ${e.message}")
            null
        }
    }

    /**
     * Filters real requests to ensure:
     * - Status is PENDING or SEARCHING.
     * - Not cancelled, completed, expired, or already assigned.
     * - Valid pickup or destination.
     */
    private fun isEligibleActiveRequest(req: RideRequest): Boolean {
        val status = req.status.trim()

        // 1. Status must be PENDING or SEARCHING (or SEARCHING_DRIVERS)
        val isPendingOrSearching = status.equals("PENDING", ignoreCase = true) ||
                status.startsWith("SEARCHING", ignoreCase = true) ||
                status.equals("OPEN", ignoreCase = true) ||
                status.equals("NEW", ignoreCase = true)

        if (!isPendingOrSearching) return false

        // 2. Must not be CANCELLED, COMPLETED, ACCEPTED, REJECTED, IN_TRIP, or DRIVER_COMING
        val isTerminalOrTaken = status.equals("CANCELLED", ignoreCase = true) ||
                status.equals("CANCELED", ignoreCase = true) ||
                status.equals("COMPLETED", ignoreCase = true) ||
                status.equals("FINISHED", ignoreCase = true) ||
                status.equals("REJECTED", ignoreCase = true) ||
                status.equals("ACCEPTED", ignoreCase = true) ||
                status.equals("IN_TRIP", ignoreCase = true) ||
                status.equals("DRIVER_ARRIVED", ignoreCase = true) ||
                status.equals("DRIVER_COMING", ignoreCase = true)

        if (isTerminalOrTaken) return false

        // 3. Not already assigned to a driver
        val isAssigned = req.assignedDriverId.isNotBlank() &&
                !req.assignedDriverId.equals("null", ignoreCase = true) &&
                !req.assignedDriverId.equals("none", ignoreCase = true) &&
                req.assignedDriverId != "0"

        if (isAssigned) return false

        // 4. Must not be expired
        val now = System.currentTimeMillis()
        if (req.expiresAt > 0L && req.expiresAt < (now - 5 * 60 * 1000L)) {
            return false
        }
        // Timestamp must be within last 24 hours
        if (req.timestamp > 0L && (now - req.timestamp > 24 * 60 * 60 * 1000L)) {
            return false
        }

        // 5. Must have at least a pickup or destination title
        if (req.pickupTitle.isBlank() && req.destinationTitle.isBlank()) {
            return false
        }

        return true
    }
}
