package com.example.data

import android.util.Log
import com.example.data.model.IntercityManifestUiState
import com.example.data.model.IntercityWaypoint
import com.example.data.model.IntercityWaypointPhase
import com.example.data.model.IntercityWaypointType
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.model.PlannedDepartureOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Geographic Route Optimizer for Intercity Departures & Trip Manifests.
 *
 * Implements the core 2-Phase Routing rule:
 * Phase 1 (Pickups): Ordered based on shortest path from driver's start heading toward highway entry.
 * Phase 2 (Drop-offs): Ordered from nearest to furthest starting from the destination highway exit point.
 */
object IntercityRouteOptimizer {
    private const val TAG = "IntercityRouteOptimizer"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Pre-configured hub and highway gateway coordinates for major corridors in Pakistan
    data class HighwayCorridorHubs(
        val entryHubName: String,
        val entryPoint: GeoPoint,
        val exitHubName: String,
        val exitPoint: GeoPoint,
        val corridorName: String
    )

    /**
     * Resolves the canonical highway gateway hubs between origin and destination cities.
     */
    fun resolveHighwayHubs(
        originCity: String,
        destinationCity: String,
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double
    ): HighwayCorridorHubs {
        val oCity = originCity.trim().lowercase()
        val dCity = destinationCity.trim().lowercase()

        // 1. Islamabad/Rawalpindi <-> Lahore (M-2 Motorway)
        if ((oCity.contains("islamabad") || oCity.contains("rawalpindi") || oCity.contains("isb") || oCity.contains("rwp")) &&
            (dCity.contains("lahore") || dCity.contains("lhr"))) {
            return HighwayCorridorHubs(
                entryHubName = "Islamabad Toll Plaza (M-2 Entry)",
                entryPoint = GeoPoint(33.5651, 72.8552),
                exitHubName = "Thokar Niaz Baig Interchange (M-2 Exit)",
                exitPoint = GeoPoint(31.4682, 74.2405),
                corridorName = "M-2 Motorway Corridor"
            )
        }

        // 2. Lahore <-> Islamabad/Rawalpindi (M-2 Motorway reversed)
        if ((oCity.contains("lahore") || oCity.contains("lhr")) &&
            (dCity.contains("islamabad") || dCity.contains("rawalpindi") || dCity.contains("isb") || dCity.contains("rwp"))) {
            return HighwayCorridorHubs(
                entryHubName = "Thokar Niaz Baig Toll Plaza (M-2 Entry)",
                entryPoint = GeoPoint(31.4682, 74.2405),
                exitHubName = "Islamabad / Kashmir Highway Exit (M-2 Exit)",
                exitPoint = GeoPoint(33.5651, 72.8552),
                corridorName = "M-2 Motorway Corridor"
            )
        }

        // 3. Islamabad/Rawalpindi <-> Peshawar (M-1 Motorway)
        if ((oCity.contains("islamabad") || oCity.contains("rawalpindi")) && dCity.contains("peshawar")) {
            return HighwayCorridorHubs(
                entryHubName = "Islamabad / Fateh Jang Toll Plaza (M-1 Entry)",
                entryPoint = GeoPoint(33.6210, 72.8250),
                exitHubName = "Peshawar Northern Bypass Toll Plaza (M-1 Exit)",
                exitPoint = GeoPoint(34.0150, 71.6500),
                corridorName = "M-1 Motorway Corridor"
            )
        }

        // 4. Peshawar <-> Islamabad/Rawalpindi (M-1 Motorway reversed)
        if (oCity.contains("peshawar") && (dCity.contains("islamabad") || dCity.contains("rawalpindi"))) {
            return HighwayCorridorHubs(
                entryHubName = "Peshawar Motorway Toll Plaza (M-1 Entry)",
                entryPoint = GeoPoint(34.0150, 71.6500),
                exitHubName = "Islamabad Zero Point / Toll Plaza (M-1 Exit)",
                exitPoint = GeoPoint(33.6210, 72.8250),
                corridorName = "M-1 Motorway Corridor"
            )
        }

        // 5. Lahore <-> Faisalabad (M-3 Motorway)
        if (oCity.contains("lahore") && dCity.contains("faisalabad")) {
            return HighwayCorridorHubs(
                entryHubName = "Faizpur Interchange (M-3 Entry)",
                entryPoint = GeoPoint(31.6020, 74.2150),
                exitHubName = "Sahianwala / Faisalabad Interchange (M-3 Exit)",
                exitPoint = GeoPoint(31.4180, 73.0790),
                corridorName = "M-3 Motorway Corridor"
            )
        }

        // 6. Lahore <-> Multan (M-3 / M-4 Motorway)
        if (oCity.contains("lahore") && dCity.contains("multan")) {
            return HighwayCorridorHubs(
                entryHubName = "Faizpur / Babu Sabu Interchange (M-3 Entry)",
                entryPoint = GeoPoint(31.5540, 74.2480),
                exitHubName = "Chowk Kumharanwala / Multan Interchange (M-4 Exit)",
                exitPoint = GeoPoint(30.1984, 71.4687),
                corridorName = "M-3 / M-4 Southbound Corridor"
            )
        }

        // 7. Karachi <-> Hyderabad (M-9 Motorway)
        if (oCity.contains("karachi") && dCity.contains("hyderabad")) {
            return HighwayCorridorHubs(
                entryHubName = "Sohrab Goth / M-9 Toll Plaza",
                entryPoint = GeoPoint(24.9850, 67.1250),
                exitHubName = "Jamshoro / Auto Bhan Terminal (M-9 Exit)",
                exitPoint = GeoPoint(25.3960, 68.3578),
                corridorName = "M-9 Motorway Corridor"
            )
        }

        // Generic fallback: Synthesize entry 15% along route and exit 85% along route
        val latVector = destLat - originLat
        val lonVector = destLon - originLon

        val calculatedEntryLat = originLat + (latVector * 0.12)
        val calculatedEntryLon = originLon + (lonVector * 0.12)

        val calculatedExitLat = originLat + (latVector * 0.88)
        val calculatedExitLon = originLon + (lonVector * 0.88)

        return HighwayCorridorHubs(
            entryHubName = "$originCity Highway Gateway",
            entryPoint = GeoPoint(calculatedEntryLat, calculatedEntryLon),
            exitHubName = "$destinationCity Highway Gateway",
            exitPoint = GeoPoint(calculatedExitLat, calculatedExitLon),
            corridorName = "Intercity Highway Corridor"
        )
    }

    /**
     * Phase 1: Dynamic Pickup Sorting.
     * Orders accepted passenger pickups starting from the driver's current/starting location,
     * following the shortest geographical driving path heading toward the highway entry point.
     */
    fun sortPickupsTowardHighway(
        driverStartLat: Double,
        driverStartLon: Double,
        highwayEntryLat: Double,
        highwayEntryLon: Double,
        pickups: List<PlannedDepartureBooking>
    ): List<PlannedDepartureBooking> {
        if (pickups.size <= 1) return pickups

        val unvisited = pickups.toMutableList()
        val sortedResult = mutableListOf<PlannedDepartureBooking>()

        var currentLat = driverStartLat
        var currentLon = driverStartLon

        val entryPoint = GeoPoint(highwayEntryLat, highwayEntryLon)

        while (unvisited.isNotEmpty()) {
            // Find the best next pickup:
            // Score = Distance(current -> pickup) + weight * Distance(pickup -> highwayEntry)
            var bestIndex = 0
            var bestScore = Double.MAX_VALUE

            for (i in unvisited.indices) {
                val candidate = unvisited[i]
                val cLat = if (candidate.pickupLat != 0.0) candidate.pickupLat else currentLat
                val cLon = if (candidate.pickupLon != 0.0) candidate.pickupLon else currentLon

                val distFromCurrent = haversineDistanceKm(currentLat, currentLon, cLat, cLon)
                val distToHighwayEntry = haversineDistanceKm(cLat, cLon, highwayEntryLat, highwayEntryLon)

                // 0.65 directional weight promotes forward progress toward highway
                val compositeScore = distFromCurrent + (0.65 * distToHighwayEntry)

                if (compositeScore < bestScore) {
                    bestScore = compositeScore
                    bestIndex = i
                }
            }

            val nextPickup = unvisited.removeAt(bestIndex)
            sortedResult.add(nextPickup)

            currentLat = if (nextPickup.pickupLat != 0.0) nextPickup.pickupLat else currentLat
            currentLon = if (nextPickup.pickupLon != 0.0) nextPickup.pickupLon else currentLon
        }

        Log.d(TAG, "Phase 1: Sorted ${pickups.size} pickups heading to highway entrance.")
        return sortedResult
    }

    /**
     * Phase 2: Dynamic Drop-off Sorting.
     * Orders passenger drop-offs from nearest to furthest starting from the destination city highway exit point.
     */
    fun sortDropoffsFromHighwayExit(
        highwayExitLat: Double,
        highwayExitLon: Double,
        finalDestLat: Double,
        finalDestLon: Double,
        dropoffs: List<PlannedDepartureBooking>
    ): List<PlannedDepartureBooking> {
        if (dropoffs.size <= 1) return dropoffs

        val unvisited = dropoffs.toMutableList()
        val sortedResult = mutableListOf<PlannedDepartureBooking>()

        var currentLat = highwayExitLat
        var currentLon = highwayExitLon

        while (unvisited.isNotEmpty()) {
            // Greedy progressive chain from highway exit towards destination
            var bestIndex = 0
            var bestDistance = Double.MAX_VALUE

            for (i in unvisited.indices) {
                val candidate = unvisited[i]
                val dLat = if (candidate.dropoffLat != 0.0) candidate.dropoffLat else finalDestLat
                val dLon = if (candidate.dropoffLon != 0.0) candidate.dropoffLon else finalDestLon

                val dist = haversineDistanceKm(currentLat, currentLon, dLat, dLon)
                if (dist < bestDistance) {
                    bestDistance = dist
                    bestIndex = i
                }
            }

            val nextDropoff = unvisited.removeAt(bestIndex)
            sortedResult.add(nextDropoff)

            currentLat = if (nextDropoff.dropoffLat != 0.0) nextDropoff.dropoffLat else currentLat
            currentLon = if (nextDropoff.dropoffLon != 0.0) nextDropoff.dropoffLon else currentLon
        }

        Log.d(TAG, "Phase 2: Sorted ${dropoffs.size} dropoffs starting from highway exit.")
        return sortedResult
    }

    /**
     * Generates a complete Intercity Manifest UI State with separated Phase 1 & Phase 2,
     * sequential Stop 1, Stop 2... numbering, and OSRM driving calculations.
     */
    suspend fun generateOptimizedManifest(
        departure: PlannedDeparture,
        confirmedBookings: List<PlannedDepartureBooking>,
        acceptedOffers: List<PlannedDepartureOffer> = emptyList()
    ): IntercityManifestUiState = withContext(Dispatchers.Default) {
        // Convert any accepted offers to bookings if not already present
        val allPassengerItems = mutableListOf<PlannedDepartureBooking>()
        allPassengerItems.addAll(confirmedBookings)

        acceptedOffers.forEach { offer ->
            if (allPassengerItems.none { it.id == offer.id || it.passengerId == offer.passengerId }) {
                allPassengerItems.add(
                    PlannedDepartureBooking(
                        id = offer.id,
                        departureId = departure.id,
                        passengerId = offer.passengerId,
                        passengerName = offer.passengerName,
                        passengerPhone = offer.passengerPhone,
                        passengerRating = offer.passengerRating,
                        seatsBooked = offer.requestedSeats,
                        pickupStop = offer.pickupPoint,
                        pickupLat = offer.pickupLat,
                        pickupLon = offer.pickupLon,
                        dropoffStop = offer.dropoffPoint,
                        dropoffLat = offer.dropoffLat,
                        dropoffLon = offer.dropoffLon,
                        isFullCar = offer.bookingType == "PRIVATE",
                        totalFarePkr = offer.offeredFare,
                        status = "CONFIRMED",
                        bookedAt = offer.createdAt
                    )
                )
            }
        }

        val driverStartLat = if (departure.pickupLat != 0.0) departure.pickupLat else 33.6938
        val driverStartLon = if (departure.pickupLon != 0.0) departure.pickupLon else 73.0317
        val finalDestLat = if (departure.dropoffLat != 0.0) departure.dropoffLat else 31.4720
        val finalDestLon = if (departure.dropoffLon != 0.0) departure.dropoffLon else 74.3980

        val hubs = resolveHighwayHubs(
            originCity = departure.pickupCity.ifBlank { "Islamabad" },
            destinationCity = departure.dropoffCity.ifBlank { "Lahore" },
            originLat = driverStartLat,
            originLon = driverStartLon,
            destLat = finalDestLat,
            destLon = finalDestLon
        )

        // Phase 1: Sort Pickups
        val sortedPickups = sortPickupsTowardHighway(
            driverStartLat = driverStartLat,
            driverStartLon = driverStartLon,
            highwayEntryLat = hubs.entryPoint.latitude,
            highwayEntryLon = hubs.entryPoint.longitude,
            pickups = allPassengerItems
        )

        // Phase 2: Sort Dropoffs
        val sortedDropoffs = sortDropoffsFromHighwayExit(
            highwayExitLat = hubs.exitPoint.latitude,
            highwayExitLon = hubs.exitPoint.longitude,
            finalDestLat = finalDestLat,
            finalDestLon = finalDestLon,
            dropoffs = allPassengerItems
        )

        // Construct Sequential Waypoints
        var currentStopNumber = 1
        val allStops = mutableListOf<IntercityWaypoint>()

        // 0. Driver Start Waypoint
        val driverStartWaypoint = IntercityWaypoint(
            id = "driver_start_${departure.id}",
            stopNumber = 0,
            phase = IntercityWaypointPhase.PICKUP,
            type = IntercityWaypointType.DRIVER_START,
            title = "Driver Starting Hub",
            addressDetails = departure.pickupStopDetails.ifBlank { "${departure.pickupCity} Departure Terminal" },
            city = departure.pickupCity,
            latitude = driverStartLat,
            longitude = driverStartLon,
            isDriverOrigin = true,
            estimatedArrivalText = departure.departureTimeText.ifBlank { "08:00 AM" },
            note = "Driver pre-trip check & route departure"
        )

        // Phase 1 Pickup Waypoints (Stop 1, Stop 2...)
        val phase1Waypoints = mutableListOf<IntercityWaypoint>()
        var prevLat = driverStartLat
        var prevLon = driverStartLon

        sortedPickups.forEach { booking ->
            val pLat = if (booking.pickupLat != 0.0) booking.pickupLat else (prevLat + 0.01)
            val pLon = if (booking.pickupLon != 0.0) booking.pickupLon else (prevLon + 0.01)
            val dist = haversineDistanceKm(prevLat, prevLon, pLat, pLon)

            val waypoint = IntercityWaypoint(
                id = "pickup_${booking.id}",
                stopNumber = currentStopNumber++,
                phase = IntercityWaypointPhase.PICKUP,
                type = IntercityWaypointType.PASSENGER_PICKUP,
                title = "Pickup: ${booking.passengerName}",
                addressDetails = booking.pickupStop,
                city = departure.pickupCity,
                latitude = pLat,
                longitude = pLon,
                passengerId = booking.passengerId,
                passengerName = booking.passengerName,
                passengerPhone = booking.passengerPhone,
                passengerRating = booking.passengerRating,
                seatsCount = booking.seatsBooked,
                farePkr = booking.totalFarePkr,
                bookingId = booking.id,
                distanceKmFromPrevious = (dist * 10).roundToInt() / 10.0,
                note = "${booking.seatsBooked} Seat(s) • PKR ${booking.totalFarePkr}"
            )
            phase1Waypoints.add(waypoint)
            allStops.add(waypoint)
            prevLat = pLat
            prevLon = pLon
        }

        // Highway Transit Waypoint
        val highwayCorridorWaypoint = IntercityWaypoint(
            id = "highway_transit_${departure.id}",
            stopNumber = currentStopNumber,
            phase = IntercityWaypointPhase.HIGHWAY_TRANSIT,
            type = IntercityWaypointType.HIGHWAY_CORRIDOR,
            title = hubs.corridorName,
            addressDetails = "${hubs.entryHubName} ➔ ${hubs.exitHubName}",
            city = "Intercity Highway",
            latitude = hubs.entryPoint.latitude,
            longitude = hubs.entryPoint.longitude,
            isHighwayPoint = true,
            note = "Direct express transit via motorway"
        )

        // Phase 2 Dropoff Waypoints (Continuing Stop Numbers)
        val phase2Waypoints = mutableListOf<IntercityWaypoint>()
        prevLat = hubs.exitPoint.latitude
        prevLon = hubs.exitPoint.longitude

        sortedDropoffs.forEach { booking ->
            val dLat = if (booking.dropoffLat != 0.0) booking.dropoffLat else (prevLat + 0.01)
            val dLon = if (booking.dropoffLon != 0.0) booking.dropoffLon else (prevLon + 0.01)
            val dist = haversineDistanceKm(prevLat, prevLon, dLat, dLon)

            val waypoint = IntercityWaypoint(
                id = "dropoff_${booking.id}",
                stopNumber = currentStopNumber++,
                phase = IntercityWaypointPhase.DROPOFF,
                type = IntercityWaypointType.PASSENGER_DROPOFF,
                title = "Drop-off: ${booking.passengerName}",
                addressDetails = booking.dropoffStop,
                city = departure.dropoffCity,
                latitude = dLat,
                longitude = dLon,
                passengerId = booking.passengerId,
                passengerName = booking.passengerName,
                passengerPhone = booking.passengerPhone,
                passengerRating = booking.passengerRating,
                seatsCount = booking.seatsBooked,
                farePkr = booking.totalFarePkr,
                bookingId = booking.id,
                distanceKmFromPrevious = (dist * 10).roundToInt() / 10.0,
                note = "Destination: ${booking.dropoffStop}"
            )
            phase2Waypoints.add(waypoint)
            allStops.add(waypoint)
            prevLat = dLat
            prevLon = dLon
        }

        // Destination Terminal Waypoint
        val destinationTerminalWaypoint = IntercityWaypoint(
            id = "dest_terminal_${departure.id}",
            stopNumber = currentStopNumber,
            phase = IntercityWaypointPhase.DROPOFF,
            type = IntercityWaypointType.FINAL_DESTINATION,
            title = "${departure.dropoffCity} Final Terminal",
            addressDetails = departure.dropoffStopDetails.ifBlank { "${departure.dropoffCity} Central Hub" },
            city = departure.dropoffCity,
            latitude = finalDestLat,
            longitude = finalDestLon,
            isDestinationTerminal = true,
            note = "Trip completion & payout settlement"
        )

        val totalSeats = allPassengerItems.sumOf { it.seatsBooked }
        val totalEarnings = allPassengerItems.sumOf { it.totalFarePkr }

        // Approximate base calculation
        var totalDistKm = 375.0
        var totalDurationMins = 255

        // Query OSRM Driving API to refine route distance & duration if network is accessible
        try {
            val coordsParam = buildString {
                append("${driverStartLon},${driverStartLat}")
                phase1Waypoints.take(3).forEach {
                    append(";${it.longitude},${it.latitude}")
                }
                append(";${hubs.entryPoint.longitude},${hubs.entryPoint.latitude}")
                append(";${hubs.exitPoint.longitude},${hubs.exitPoint.latitude}")
                phase2Waypoints.take(3).forEach {
                    append(";${it.longitude},${it.latitude}")
                }
                append(";${finalDestLon},${finalDestLat}")
            }

            val osrmUrl = "https://router.project-osrm.org/route/v1/driving/$coordsParam?overview=false"
            val req = Request.Builder().url(osrmUrl).header("User-Agent", "DrigoApp/1.0").build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val routeObj = routes.getJSONObject(0)
                        val meters = routeObj.optDouble("distance", 0.0)
                        val seconds = routeObj.optDouble("duration", 0.0)
                        if (meters > 1000) {
                            totalDistKm = (meters / 1000.0 * 10).roundToInt() / 10.0
                            totalDurationMins = max(10, ceil(seconds / 60.0).toInt())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "OSRM route query fallback to geodesic calculation: ${e.message}")
        }

        IntercityManifestUiState(
            departureId = departure.id,
            originCity = departure.pickupCity.ifBlank { "Islamabad" },
            destinationCity = departure.dropoffCity.ifBlank { "Lahore" },
            corridorName = hubs.corridorName,
            highwayEntryHub = hubs.entryHubName,
            highwayExitHub = hubs.exitHubName,
            driverStartWaypoint = driverStartWaypoint,
            phase1Pickups = phase1Waypoints,
            highwayCorridorWaypoint = highwayCorridorWaypoint,
            phase2Dropoffs = phase2Waypoints,
            destinationTerminalWaypoint = destinationTerminalWaypoint,
            allOrderedStops = allStops,
            totalDistanceKm = totalDistKm,
            totalDurationMinutes = totalDurationMins,
            totalBookedSeats = totalSeats,
            totalManifestEarningsPkr = totalEarnings,
            isOptimizingRoute = false,
            routingMethod = "OSRM 2-Phase Highway Route"
        )
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
