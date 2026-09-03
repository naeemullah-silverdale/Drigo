package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.remote.FirebaseRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Location Service for Captain / Driver Mode.
 * Ensures continuous high-accuracy GPS tracking and Firebase synchronization
 * when the driver is ONLINE, even when the app is backgrounded or screen locked.
 */
class DriverLocationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: FirebaseRepository

    private var currentDriverId: String = ""
    private var currentDriverName: String = ""
    private var currentVehicleType: String = ""
    private var currentVehicleNumber: String = ""
    private var currentPhone: String = ""
    private var isTracking = false

    companion object {
        private const val TAG = "DriverLocationService"
        const val CHANNEL_ID = "driver_live_location_tracking"
        const val NOTIFICATION_ID = 9901

        const val ACTION_START = "com.example.service.ACTION_START_LOCATION_TRACKING"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_LOCATION_TRACKING"

        const val EXTRA_DRIVER_ID = "extra_driver_id"
        const val EXTRA_DRIVER_NAME = "extra_driver_name"
        const val EXTRA_VEHICLE_TYPE = "extra_vehicle_type"
        const val EXTRA_VEHICLE_NUMBER = "extra_vehicle_number"
        const val EXTRA_PHONE = "extra_phone"

        /**
         * Starts the continuous location tracking foreground service for an online driver.
         */
        fun start(
            context: Context,
            driverId: String,
            driverName: String = "Captain",
            vehicleType: String = "Car",
            vehicleNumber: String = "",
            phone: String = ""
        ) {
            try {
                val intent = Intent(context, DriverLocationService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_DRIVER_ID, driverId)
                    putExtra(EXTRA_DRIVER_NAME, driverName)
                    putExtra(EXTRA_VEHICLE_TYPE, vehicleType)
                    putExtra(EXTRA_VEHICLE_NUMBER, vehicleNumber)
                    putExtra(EXTRA_PHONE, phone)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to start DriverLocationService: ${t.message}", t)
            }
        }

        /**
         * Stops continuous location tracking and marks driver offline.
         */
        fun stop(context: Context, driverId: String = "") {
            try {
                val intent = Intent(context, DriverLocationService::class.java).apply {
                    action = ACTION_STOP
                    putExtra(EXTRA_DRIVER_ID, driverId)
                }
                context.startService(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to stop DriverLocationService: ${t.message}", t)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForegroundWithNotification()
            repository = FirebaseRepository.getInstance(applicationContext)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Error in onCreate: ${t.message}", t)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
                val bearing = if (location.hasBearing()) location.bearing else 0f

                if (currentDriverId.isNotBlank()) {
                    serviceScope.launch {
                        try {
                            repository.updateDriverOnlineLocation(
                                driverId = currentDriverId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                bearing = bearing,
                                speed = speedKmh,
                                driverName = currentDriverName,
                                vehicleType = currentVehicleType,
                                vehicleNumber = currentVehicleNumber,
                                phone = currentPhone
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating online driver location: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val action = intent?.action

            if (action == ACTION_STOP) {
                val driverId = intent.getStringExtra(EXTRA_DRIVER_ID) ?: currentDriverId
                stopLocationTracking(driverId)
                return START_NOT_STICKY
            }

            if (action == ACTION_START || action == null) {
                currentDriverId = intent?.getStringExtra(EXTRA_DRIVER_ID) ?: currentDriverId
                currentDriverName = intent?.getStringExtra(EXTRA_DRIVER_NAME) ?: currentDriverName
                currentVehicleType = intent?.getStringExtra(EXTRA_VEHICLE_TYPE) ?: currentVehicleType
                currentVehicleNumber = intent?.getStringExtra(EXTRA_VEHICLE_NUMBER) ?: currentVehicleNumber
                currentPhone = intent?.getStringExtra(EXTRA_PHONE) ?: currentPhone

                startForegroundWithNotification()
                startLocationTracking()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error during onStartCommand: ${t.message}", t)
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        try {
            createNotificationChannel()
            val notification = buildForegroundNotification()

            val fineGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasLocationPerm = fineGranted || coarseGranted

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocationPerm) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } catch (e: Throwable) {
                    Log.w(TAG, "Foreground location type start failed, retrying generic startForeground: ${e.message}")
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Critical startForeground error: ${t.message}", t)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_DRIVER_MODE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Drigo Captain Online")
            .setContentText("Sharing real-time location • Ready for passenger rides")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Driver Online Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Drigo Captain GPS location updated in real time while online"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startLocationTracking() {
        if (isTracking) return

        val fineGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            Log.w(TAG, "Location permissions not granted for tracking service")
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(3000L)
                .setMinUpdateDistanceMeters(5f)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
            Log.d(TAG, "Driver location tracking started for driverId=$currentDriverId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request location updates: ${e.message}", e)
        }
    }

    private fun stopLocationTracking(driverId: String) {
        if (isTracking) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing location updates: ${e.message}")
            }
            isTracking = false
        }

        val targetDriverId = driverId.ifBlank { currentDriverId }
        if (targetDriverId.isNotBlank()) {
            serviceScope.launch {
                try {
                    repository.setDriverOffline(targetDriverId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting driver offline: ${e.message}")
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {}

        stopSelf()
    }

    override fun onDestroy() {
        if (isTracking) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (_: Exception) {}
            isTracking = false
        }
        if (currentDriverId.isNotBlank()) {
            serviceScope.launch {
                try {
                    repository.setDriverOffline(currentDriverId)
                } catch (_: Exception) {}
            }
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
