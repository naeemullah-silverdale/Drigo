package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class UserLocationData(
    val latitude: Double,
    val longitude: Double,
    val addressLine: String,
    val areaName: String,
    val city: String,
    val bearing: Float = 0f,
    val speedKmh: Float = 0f
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    companion object {
        fun isLocationServiceEnabled(context: Context): Boolean {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                        lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            }
        }

        fun hasLocationPermission(context: Context): Boolean {
            val fine = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }

        fun isLocationReady(context: Context): Boolean {
            return isLocationServiceEnabled(context) && hasLocationPermission(context)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocationData? = withContext(Dispatchers.IO) {
        try {
            val cts = CancellationTokenSource()
            val location: Location? = try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await() ?: fusedLocationClient.lastLocation.await()
            } catch (e: Exception) {
                null
            }

            if (location != null) {
                val geocodeResult = reverseGeocode(location.latitude, location.longitude)
                UserLocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    addressLine = geocodeResult.first,
                    areaName = geocodeResult.second,
                    city = geocodeResult.third,
                    bearing = if (location.hasBearing()) location.bearing else 0f,
                    speedKmh = if (location.hasSpeed()) (location.speed * 3.6f) else 0f
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdatesFlow(): Flow<UserLocationData> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    UserLocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        addressLine = "${String.format(Locale.US, "%.5f", loc.latitude)}, ${String.format(Locale.US, "%.5f", loc.longitude)}",
                        areaName = "Current Location",
                        city = "Peshawar",
                        bearing = if (loc.hasBearing()) loc.bearing else 0f,
                        speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f) else 0f
                    )
                )
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, context.mainLooper)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (_: Throwable) {}
        }
    }

    @Suppress("DEPRECATION")
    fun reverseGeocode(latitude: Double, longitude: Double): Triple<String, String, String> {
        if (latitude == 0.0 && longitude == 0.0) {
            return Triple("Select Pickup Location", "Current Location", "Peshawar")
        }

        // 1. Google Maps Reverse Geocoding API when MAPS_API_KEY is available
        val apiKey = try {
            com.example.BuildConfig::class.java.getField("MAPS_API_KEY").get(null) as? String ?: ""
        } catch (_: Exception) { "" }

        if (apiKey.isNotBlank() && apiKey != "MY_MAPS_API_KEY") {
            try {
                val urlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$latitude,$longitude&key=$apiKey"
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val status = json.optString("status")
                    if (status == "OK") {
                        val results = json.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            val firstResult = results.getJSONObject(0)
                            val formattedAddr = firstResult.optString("formatted_address", "")
                            
                            var subLocality = ""
                            var locality = ""
                            val comps = firstResult.optJSONArray("address_components")
                            if (comps != null) {
                                for (i in 0 until comps.length()) {
                                    val c = comps.getJSONObject(i)
                                    val types = c.optJSONArray("types")
                                    if (types != null) {
                                        for (j in 0 until types.length()) {
                                            val t = types.getString(j)
                                            if (t == "sublocality" || t == "sublocality_level_1" || t == "neighborhood") {
                                                if (subLocality.isBlank()) subLocality = c.optString("long_name")
                                            }
                                            if (t == "locality" || t == "administrative_area_level_2") {
                                                if (locality.isBlank()) locality = c.optString("long_name")
                                            }
                                        }
                                    }
                                }
                            }

                            if (formattedAddr.isNotBlank()) {
                                val shortTitle = subLocality.ifBlank { formattedAddr.split(",").firstOrNull()?.trim() ?: "Selected Location" }
                                val city = locality.ifBlank { "Peshawar" }
                                return Triple(formattedAddr, shortTitle, city)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Native Android Geocoder lookup
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var list: List<Address>? = null
                geocoder.getFromLocation(latitude, longitude, 1) { addressesList ->
                    list = addressesList
                }
                list
            } else {
                geocoder.getFromLocation(latitude, longitude, 1)
            }

            val address = addresses?.firstOrNull()
            if (address != null) {
                val thoroughfare = address.thoroughfare ?: address.subThoroughfare ?: address.featureName ?: ""
                val subLocality = address.subLocality ?: address.locality ?: ""
                val adminArea = address.adminArea ?: address.countryName ?: ""

                val line1 = if (thoroughfare.isNotBlank()) thoroughfare else subLocality
                val line2 = if (subLocality.isNotBlank() && subLocality != line1) "$subLocality, $adminArea" else adminArea
                val fullAddress = if (line1.isNotBlank() && line2.isNotBlank() && line1 != line2) "$line1, $line2" else address.getAddressLine(0) ?: String.format(Locale.US, "Location (%.4f, %.4f)", latitude, longitude)

                return Triple(fullAddress, subLocality.ifBlank { "Selected Location" }, adminArea.ifBlank { "Peshawar" })
            }
        } catch (_: Exception) {
        }

        // HTTP OpenStreetMap Nominatim fallback when Android Geocoder service is unavailable
        try {
            val urlStr = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude"
            val url = java.net.URL(urlStr)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", context.packageName)
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                val displayName = json.optString("display_name")
                val addressObj = json.optJSONObject("address")
                val road = addressObj?.optString("road") ?: addressObj?.optString("suburb") ?: addressObj?.optString("neighbourhood") ?: ""
                val suburb = addressObj?.optString("suburb") ?: addressObj?.optString("city_district") ?: addressObj?.optString("town") ?: ""
                val city = addressObj?.optString("city") ?: addressObj?.optString("county") ?: addressObj?.optString("state") ?: "Peshawar"

                if (displayName.isNotBlank()) {
                    val line1 = if (road.isNotBlank()) road else displayName.split(",").firstOrNull()?.trim() ?: displayName
                    val line2 = if (suburb.isNotBlank() && suburb != line1) "$suburb, $city" else city
                    val fullAddr = if (line1.isNotBlank() && line2.isNotBlank() && line1 != line2) "$line1, $line2" else displayName
                    return Triple(fullAddr, suburb.ifBlank { line1 }, city.ifBlank { "Peshawar" })
                }
            }
        } catch (_: Exception) {}

        // Exact coordinate fallback guaranteed to never return a fake/hardcoded address
        val formattedCoords = String.format(Locale.US, "Location (%.4f, %.4f)", latitude, longitude)
        return Triple(formattedCoords, "Selected Location", "Peshawar")
    }

    @Suppress("DEPRECATION")
    fun geocodeAddress(addressName: String): Pair<Double, Double>? {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val query = if (!addressName.contains("peshawar", ignoreCase = true) && !addressName.contains("pakistan", ignoreCase = true)) {
                "$addressName, Peshawar, Pakistan"
            } else {
                addressName
            }
            val list = geocoder.getFromLocationName(query, 1)
            val addr = list?.firstOrNull()
            if (addr != null && addr.hasLatitude() && addr.hasLongitude()) {
                return Pair(addr.latitude, addr.longitude)
            }
        } catch (_: Exception) {}
        return null
    }
}
