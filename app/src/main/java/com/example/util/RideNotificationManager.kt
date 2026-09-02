package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Ride Notification Event Types for Passenger and Driver journeys
 */
enum class RideNotificationType(
    val channelId: String,
    val categoryName: String,
    val isDriverEvent: Boolean
) {
    // Passenger Notifications
    PASSENGER_DRIVER_FOUND("passenger_ride_updates", "Captain Found", false),
    PASSENGER_DRIVER_ACCEPTED("passenger_ride_updates", "Ride Confirmed", false),
    PASSENGER_DRIVER_COUNTER_OFFER("passenger_ride_updates", "Counter-Offer", false),
    PASSENGER_DRIVER_ARRIVING("passenger_ride_updates", "Captain Arriving", false),
    PASSENGER_DRIVER_ARRIVED("passenger_ride_updates", "Captain Arrived", false),
    PASSENGER_RIDE_STARTED("passenger_ride_updates", "Ride Started", false),
    PASSENGER_RIDE_COMPLETED("passenger_ride_updates", "Trip Completed", false),
    PASSENGER_RIDE_CANCELLED("passenger_ride_updates", "Trip Cancelled", false),

    // Driver Notifications
    DRIVER_NEW_REQUEST("driver_radar_alerts", "New Ride Request", true),
    DRIVER_OFFER_ACCEPTED("driver_radar_alerts", "Offer Accepted", true),
    DRIVER_RIDE_ASSIGNED("driver_radar_alerts", "Ride Assigned", true),
    DRIVER_RIDE_CANCELLED("driver_radar_alerts", "Ride Cancelled", true),
    DRIVER_SHARED_MATCH("driver_radar_alerts", "Shared Ride Match", true)
}

/**
 * Represents an active in-app heads-up notification event
 */
data class InAppNotificationItem(
    val id: String = "notif_${System.currentTimeMillis()}",
    val type: RideNotificationType,
    val title: String,
    val message: String,
    val subText: String? = null,
    val actionLabel: String? = null,
    val rideId: String? = null,
    val farePkr: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val onActionClick: (() -> Unit)? = null
)

/**
 * Centralized Real-time Notification Manager.
 * Dispatches Android system notifications and publishes to the in-app notification state flow.
 */
class RideNotificationManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var dismissJob: Job? = null

    private val _inAppNotification = MutableStateFlow<InAppNotificationItem?>(null)
    val inAppNotification: StateFlow<InAppNotificationItem?> = _inAppNotification.asStateFlow()

    private var audioHelper: DriverAudioHelper? = null

    init {
        createNotificationChannels()
        try {
            audioHelper = DriverAudioHelper(appContext)
        } catch (e: Exception) {
            Log.e("RideNotificationManager", "Audio helper init error: ${e.message}")
        }
    }

    companion object {
        const val CHANNEL_PASSENGER = "passenger_ride_updates"
        const val CHANNEL_DRIVER = "driver_radar_alerts"
        const val CHANNEL_EMERGENCY = "emergency_sos_alerts"

        @Volatile
        private var INSTANCE: RideNotificationManager? = null

        fun getInstance(context: Context): RideNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RideNotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Passenger updates channel
            val passengerChannel = NotificationChannel(
                CHANNEL_PASSENGER,
                "Passenger Trip Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live notifications for driver arrival, trip progress and ride completion"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 2. Driver radar alerts channel
            val driverChannel = NotificationChannel(
                CHANNEL_DRIVER,
                "Driver Radar & Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for incoming passenger requests, bids and ride assignments"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 3. Emergency SOS channel
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency & Safety SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for emergency assistance and safety updates"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }

            notificationManager.createNotificationChannels(listOf(passengerChannel, driverChannel, emergencyChannel))
        }
    }

    /**
     * Dispatches a notification for both System status bar (if permitted) and In-App floating toast banner
     */
    fun postNotification(
        type: RideNotificationType,
        title: String,
        message: String,
        subText: String? = null,
        actionLabel: String? = null,
        rideId: String? = null,
        farePkr: Int? = null,
        speakAnnouncement: Boolean = true,
        onActionClick: (() -> Unit)? = null
    ) {
        val item = InAppNotificationItem(
            type = type,
            title = title,
            message = message,
            subText = subText,
            actionLabel = actionLabel,
            rideId = rideId,
            farePkr = farePkr,
            onActionClick = onActionClick
        )

        // 1. Trigger In-App notification
        _inAppNotification.value = item
        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(5500) // Auto-dismiss after 5.5s
            if (_inAppNotification.value?.id == item.id) {
                _inAppNotification.value = null
            }
        }

        // 2. Voice announcement if enabled
        if (speakAnnouncement) {
            try {
                if (type.isDriverEvent) {
                    audioHelper?.announceRideStatus(title)
                } else {
                    audioHelper?.speakCustom(title)
                }
            } catch (e: Exception) {
                Log.e("RideNotificationManager", "Voice feedback error: ${e.message}")
            }
        }

        // 3. System Push / Status Bar Notification
        showSystemNotification(item)
    }

    fun dismissInAppNotification() {
        dismissJob?.cancel()
        _inAppNotification.value = null
    }

    private fun showSystemNotification(item: InAppNotificationItem) {
        try {
            // Check Android 13+ POST_NOTIFICATIONS permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return // In-app notification acts as fallback
                }
            }

            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NOTIFICATION_RIDE_ID", item.rideId)
                putExtra("NOTIFICATION_TYPE", item.type.name)
            }

            val pendingIntent = PendingIntent.getActivity(
                appContext,
                item.type.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(appContext, item.type.channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(item.title)
                .setContentText(item.message)
                .setSubText(item.subText ?: item.type.categoryName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            item.actionLabel?.let { label ->
                builder.addAction(android.R.drawable.ic_menu_send, label, pendingIntent)
            }

            val notificationId = (item.rideId?.hashCode() ?: System.currentTimeMillis().toInt()) and 0x7FFFFFFF
            NotificationManagerCompat.from(appContext).notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e("RideNotificationManager", "Error showing system notification: ${e.message}")
        }
    }

    // Convenience Helper Methods for Passenger

    fun notifyDriverFound(driverName: String, etaMinutes: Int, rideId: String) {
        postNotification(
            type = RideNotificationType.PASSENGER_DRIVER_FOUND,
            title = "Captain Found!",
            message = "$driverName is reviewing your ride (~$etaMinutes min away)",
            subText = "Captain Nearby",
            actionLabel = "View Status",
            rideId = rideId
        )
    }

    fun notifyDriverAccepted(driverName: String, farePkr: Int, vehicleModel: String, rideId: String, onAction: (() -> Unit)? = null) {
        postNotification(
            type = RideNotificationType.PASSENGER_DRIVER_ACCEPTED,
            title = "Ride Confirmed!",
            message = "$driverName accepted your offer for PKR $farePkr in $vehicleModel",
            subText = "Booking Active",
            actionLabel = "Track Driver",
            rideId = rideId,
            farePkr = farePkr,
            onActionClick = onAction
        )
    }

    fun notifyDriverCounterOffer(driverName: String, counterFare: Int, etaMinutes: Int, rideId: String, onAction: (() -> Unit)? = null) {
        postNotification(
            type = RideNotificationType.PASSENGER_DRIVER_COUNTER_OFFER,
            title = "New Counter-Offer Received",
            message = "$driverName offered PKR $counterFare (~$etaMinutes min away)",
            subText = "Offer Pending",
            actionLabel = "Review Offer",
            rideId = rideId,
            farePkr = counterFare,
            onActionClick = onAction
        )
    }

    fun notifyDriverArriving(driverName: String, vehicleColor: String, vehicleModel: String, plate: String, rideId: String) {
        postNotification(
            type = RideNotificationType.PASSENGER_DRIVER_ARRIVING,
            title = "Captain is Arriving",
            message = "$driverName is 2 mins away in $vehicleColor $vehicleModel ($plate)",
            subText = "Almost There",
            actionLabel = "See Map",
            rideId = rideId
        )
    }

    fun notifyDriverArrived(driverName: String, plateNumber: String, rideId: String) {
        postNotification(
            type = RideNotificationType.PASSENGER_DRIVER_ARRIVED,
            title = "Captain Has Arrived!",
            message = "$driverName ($plateNumber) is waiting at your pickup point",
            subText = "Arrived at Pickup",
            actionLabel = "Meet Captain",
            rideId = rideId
        )
    }

    fun notifyRideStarted(destination: String, rideId: String) {
        postNotification(
            type = RideNotificationType.PASSENGER_RIDE_STARTED,
            title = "Ride Started",
            message = "Heading to $destination. Have a safe journey!",
            subText = "Trip In Progress",
            actionLabel = "Safety Center",
            rideId = rideId
        )
    }

    fun notifyRideCompleted(farePkr: Int, rideId: String, onAction: (() -> Unit)? = null) {
        postNotification(
            type = RideNotificationType.PASSENGER_RIDE_COMPLETED,
            title = "Trip Completed!",
            message = "PKR $farePkr collected. Please rate your captain.",
            subText = "Trip Finished",
            actionLabel = "Rate Ride",
            rideId = rideId,
            farePkr = farePkr,
            onActionClick = onAction
        )
    }

    fun notifyPassengerRideCancelled(reason: String = "Cancelled", rideId: String) {
        postNotification(
            type = RideNotificationType.PASSENGER_RIDE_CANCELLED,
            title = "Ride Cancelled",
            message = "Your ride was cancelled ($reason). You can re-book anytime.",
            subText = "Cancelled",
            actionLabel = "Re-book",
            rideId = rideId
        )
    }

    // Convenience Helper Methods for Driver

    fun notifyNewRideRequest(pickup: String, dest: String, farePkr: Int, rideId: String, onAction: (() -> Unit)? = null) {
        postNotification(
            type = RideNotificationType.DRIVER_NEW_REQUEST,
            title = "New Ride Request! PKR $farePkr",
            message = "$pickup → $dest",
            subText = "Nearby Passenger",
            actionLabel = "View & Bid",
            rideId = rideId,
            farePkr = farePkr,
            onActionClick = onAction
        )
    }

    fun notifyPassengerAcceptedOffer(passengerName: String, farePkr: Int, pickup: String, rideId: String, onAction: (() -> Unit)? = null) {
        postNotification(
            type = RideNotificationType.DRIVER_OFFER_ACCEPTED,
            title = "Passenger Accepted Your Offer!",
            message = "$passengerName agreed to PKR $farePkr. Proceed to $pickup",
            subText = "Ride Confirmed",
            actionLabel = "Start Pickup",
            rideId = rideId,
            farePkr = farePkr,
            onActionClick = onAction
        )
    }

    fun notifyDriverRideAssigned(passengerName: String, pickup: String, rideId: String) {
        postNotification(
            type = RideNotificationType.DRIVER_RIDE_ASSIGNED,
            title = "Ride Assigned to You",
            message = "Pickup $passengerName at $pickup",
            subText = "Active Assignment",
            actionLabel = "Navigate",
            rideId = rideId
        )
    }

    fun notifyDriverRideCancelled(passengerName: String, rideId: String) {
        postNotification(
            type = RideNotificationType.DRIVER_RIDE_CANCELLED,
            title = "Ride Cancelled",
            message = "$passengerName cancelled the ride request",
            subText = "Cancelled by Passenger",
            actionLabel = "Back to Radar",
            rideId = rideId
        )
    }

    fun notifySharedRideMatch(pickup: String, dest: String, extraFare: Int, rideId: String, onAction: (() -> Unit)? = null) {
        postNotification(
            type = RideNotificationType.DRIVER_SHARED_MATCH,
            title = "Shared Route Match! +PKR $extraFare",
            message = "New passenger on your route: $pickup → $dest",
            subText = "Route Optimizer Match",
            actionLabel = "Accept Match",
            rideId = rideId,
            farePkr = extraFare,
            onActionClick = onAction
        )
    }
}
