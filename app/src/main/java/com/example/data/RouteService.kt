package com.example.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.*

data class DestinationSuggestion(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
) {
    fun toAppLocation() = AppLocation(title, subtitle, latitude, longitude)
}

data class AppLocation(
    val title: String,
    val subtitle: String = "",
    val latitude: Double,
    val longitude: Double
) {
    fun toGeoPoint() = GeoPoint(latitude, longitude)
    fun toDestinationSuggestion() = DestinationSuggestion(title, subtitle, latitude, longitude)
}

data class RouteResult(
    val points: List<GeoPoint>,
    val distanceKm: Double,
    val durationMinutes: Int,
    val startAddress: String,
    val destinationAddress: String,
    val startPoint: GeoPoint,
    val destinationPoint: GeoPoint
)

class RouteService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    // Popular destinations in Peshawar for instant high-speed autocomplete
    val popularSuggestions = listOf(
        DestinationSuggestion("University Town", "Jamrud Road, Peshawar", 33.9993, 71.4886),
        DestinationSuggestion("Hayatabad Phase 3", "Near Tatara Park, Peshawar", 33.9782, 71.4331),
        DestinationSuggestion("Saddar Bazaar", "The Mall / Saddar, Peshawar Cantt", 34.0044, 71.5369),
        DestinationSuggestion("Khyber Teaching Hospital (KTH)", "University Road, Peshawar", 33.9942, 71.4921),
        DestinationSuggestion("Islamia College University", "Grand Trunk Rd, Rahat Abad, Peshawar", 33.9890, 71.4797),
        DestinationSuggestion("Peshawar Bacha Khan Airport", "Civil Quarters, Peshawar", 33.9939, 71.5147),
        DestinationSuggestion("Deans Trade Center", "Saddar, Peshawar Cantt", 34.0071, 71.5348),
        DestinationSuggestion("Hayatabad Medical Complex", "Phase 4, Hayatabad, Peshawar", 33.9712, 71.4287),
        DestinationSuggestion("Shahi Qila (Bala Hissar Fort)", "Grand Trunk Rd, Peshawar City", 34.0125, 71.5678),
        DestinationSuggestion("City Railway Station", "Peshawar Cantt", 34.0150, 71.5815),
        DestinationSuggestion("Qissa Khwani Bazaar", "Old City, Peshawar", 34.0107, 71.5714),
        DestinationSuggestion("Ring Road Peshawar", "Near Hayatabad Toll Plaza, Peshawar", 33.9634, 71.4589)
    )

    /**
     * Search destination suggestions using Google Places Autocomplete API, Google Geocoding API,
     * Nominatim HTTP search fallback, and Android Geocoder with local popular places.
     */
    suspend fun searchDestinations(query: String, currentLat: Double, currentLng: Double): List<DestinationSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext popularSuggestions
        }

        val results = mutableListOf<DestinationSuggestion>()

        // 1. Local popular suggestions match
        val localMatches = popularSuggestions.filter {
            it.title.contains(trimmed, ignoreCase = true) || it.subtitle.contains(trimmed, ignoreCase = true)
        }
        results.addAll(localMatches)

        val apiKey = try {
            com.example.BuildConfig::class.java.getField("MAPS_API_KEY").get(null) as? String ?: ""
        } catch (_: Exception) { "" }

        // 2. Google Places Autocomplete API lookup if MAPS_API_KEY is configured
        if (apiKey.isNotBlank() && apiKey != "MY_MAPS_API_KEY") {
            try {
                val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
                val autocompleteUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=$encodedQuery&location=$currentLat,$currentLng&radius=50000&key=$apiKey"
                val req = Request.Builder().url(autocompleteUrl).build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val json = JSONObject(bodyStr)
                        val predictions = json.optJSONArray("predictions")
                        if (predictions != null && predictions.length() > 0) {
                            for (i in 0 until minOf(5, predictions.length())) {
                                val item = predictions.getJSONObject(i)
                                val placeId = item.optString("place_id")
                                val structured = item.optJSONObject("structured_formatting")
                                val mainText = structured?.optString("main_text") ?: item.optString("description")
                                val secondaryText = structured?.optString("secondary_text") ?: "Selected Location"

                                if (placeId.isNotBlank()) {
                                    val detailsUrl = "https://maps.googleapis.com/maps/api/place/details/json?place_id=$placeId&fields=geometry,name,formatted_address&key=$apiKey"
                                    val dReq = Request.Builder().url(detailsUrl).build()
                                    val dResp = client.newCall(dReq).execute()
                                    if (dResp.isSuccessful) {
                                        val dBody = dResp.body?.string()
                                        if (!dBody.isNullOrBlank()) {
                                            val dJson = JSONObject(dBody).optJSONObject("result")
                                            val loc = dJson?.optJSONObject("geometry")?.optJSONObject("location")
                                            if (loc != null) {
                                                val lat = loc.optDouble("lat", currentLat)
                                                val lng = loc.optDouble("lng", currentLng)
                                                val title = dJson.optString("name", mainText)
                                                val subtitle = dJson.optString("formatted_address", secondaryText)
                                                
                                                val suggestion = DestinationSuggestion(title, subtitle, lat, lng)
                                                if (results.none { abs(it.latitude - lat) < 0.0008 && abs(it.longitude - lng) < 0.0008 }) {
                                                    results.add(suggestion)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. OpenStreetMap Nominatim HTTP Search fallback
        if (results.size < 3) {
            try {
                val searchQuery = if (!trimmed.contains("pakistan", ignoreCase = true)) "$trimmed, Pakistan" else trimmed
                val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                val nomUrl = "https://nominatim.openstreetmap.org/search?format=jsonv2&q=$encodedQuery&limit=5&countrycodes=pk"
                val req = Request.Builder().url(nomUrl).header("User-Agent", "DrigoRideshareApp/1.0").build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val array = org.json.JSONArray(bodyStr)
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val lat = item.optDouble("lat", 0.0)
                            val lon = item.optDouble("lon", 0.0)
                            val displayName = item.optString("display_name", "")
                            val name = item.optString("name", "").ifBlank { displayName.split(",").firstOrNull() ?: trimmed }

                            if (lat != 0.0 && lon != 0.0) {
                                val suggestion = DestinationSuggestion(
                                    title = name,
                                    subtitle = displayName,
                                    latitude = lat,
                                    longitude = lon
                                )
                                if (results.none { abs(it.latitude - lat) < 0.0008 && abs(it.longitude - lon) < 0.0008 }) {
                                    results.add(suggestion)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Android Geocoder lookup
        if (results.size < 3) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val searchQuery = if (!trimmed.contains("peshawar", ignoreCase = true) && !trimmed.contains("pakistan", ignoreCase = true)) {
                    "$trimmed, Peshawar, Pakistan"
                } else {
                    trimmed
                }

                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var list: List<Address>? = null
                    geocoder.getFromLocationName(searchQuery, 5) { addressesList ->
                        list = addressesList
                    }
                    list
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(searchQuery, 5)
                }

                addresses?.forEach { address ->
                    val title = address.featureName ?: address.thoroughfare ?: address.subLocality ?: trimmed
                    val subtitle = address.getAddressLine(0) ?: "${address.locality ?: "Peshawar"}, ${address.countryName ?: "Pakistan"}"
                    val suggestion = DestinationSuggestion(
                        title = title,
                        subtitle = subtitle,
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                    if (results.none { abs(it.latitude - suggestion.latitude) < 0.0008 && abs(it.longitude - suggestion.longitude) < 0.0008 }) {
                        results.add(suggestion)
                    }
                }
            } catch (_: Exception) {}
        }

        // 5. If still empty, add query as a custom destination near current location
        if (results.isEmpty()) {
            results.add(
                DestinationSuggestion(
                    title = trimmed,
                    subtitle = "Custom Selected Location",
                    latitude = currentLat + 0.015,
                    longitude = currentLng + 0.015
                )
            )
        }

        results
    }

    /**
     * Calculate route between pickup and destination using OSRM with intelligent fallback
     */
    suspend fun calculateRoute(
        startPoint: GeoPoint,
        destPoint: GeoPoint,
        startAddress: String,
        destinationAddress: String
    ): RouteResult = withContext(Dispatchers.IO) {
        val osrmUrl = "https://router.project-osrm.org/route/v1/driving/${startPoint.longitude},${startPoint.latitude};${destPoint.longitude},${destPoint.latitude}?overview=full&geometries=geojson"

        try {
            val request = Request.Builder()
                .url(osrmUrl)
                .header("User-Agent", "DrigoApp/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (!bodyString.isNullOrBlank()) {
                    val json = JSONObject(bodyString)
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val distanceMeters = route.optDouble("distance", 0.0)
                        val durationSeconds = route.optDouble("duration", 0.0)

                        val geometry = route.optJSONObject("geometry")
                        val coords = geometry?.optJSONArray("coordinates")

                        val geoPoints = mutableListOf<GeoPoint>()
                        if (coords != null && coords.length() > 0) {
                            for (i in 0 until coords.length()) {
                                val coord = coords.getJSONArray(i)
                                val lng = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                geoPoints.add(GeoPoint(lat, lng))
                            }
                        }

                        if (geoPoints.isNotEmpty()) {
                            val distKm = max(0.5, (distanceMeters / 1000.0 * 10).roundToInt() / 10.0)
                            val durationMins = max(3, ceil(durationSeconds / 60.0).toInt())

                            return@withContext RouteResult(
                                points = geoPoints,
                                distanceKm = distKm,
                                durationMinutes = durationMins,
                                startAddress = startAddress,
                                destinationAddress = destinationAddress,
                                startPoint = startPoint,
                                destinationPoint = destPoint
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to interpolated road curve if offline or network timeout
        }

        // Fallback: Generate realistic road curve path between start and dest
        val fallbackPoints = generateRoadPathPoints(startPoint, destPoint)
        val haversineKm = calculateHaversineDistanceKm(startPoint, destPoint)
        val roadDistKm = max(1.2, ((haversineKm * 1.35) * 10).roundToInt() / 10.0)
        val durationMins = max(5, ceil((roadDistKm / 28.0) * 60.0).toInt())

        RouteResult(
            points = fallbackPoints,
            distanceKm = roadDistKm,
            durationMinutes = durationMins,
            startAddress = startAddress,
            destinationAddress = destinationAddress,
            startPoint = startPoint,
            destinationPoint = destPoint
        )
    }

    private fun generateRoadPathPoints(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val segments = 12
        val latDiff = end.latitude - start.latitude
        val lngDiff = end.longitude - start.longitude

        points.add(start)
        for (i in 1 until segments) {
            val fraction = i.toDouble() / segments
            // Add subtle road curvature
            val curveFactor = sin(fraction * Math.PI) * 0.003
            val lat = start.latitude + latDiff * fraction + curveFactor * (if (lngDiff > 0) 1 else -1)
            val lng = start.longitude + lngDiff * fraction + curveFactor * (if (latDiff > 0) -1 else 1)
            points.add(GeoPoint(lat, lng))
        }
        points.add(end)
        return points
    }

    private fun calculateHaversineDistanceKm(p1: GeoPoint, p2: GeoPoint): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
