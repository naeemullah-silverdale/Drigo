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
                val fullAddress = if (line1.isNotBlank() && line2.isNotBlank()) "$line1\n$line2" else address.getAddressLine(0) ?: "$latitude, $longitude"

                return Triple(fullAddress, subLocality.ifBlank { "Current Location" }, adminArea.ifBlank { "Peshawar" })
            }
        } catch (_: Exception) {
        }
        return Triple("Street Number 9, Shero\nJahngi, Peshawar", "Shero Jahngi", "Peshawar")
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
