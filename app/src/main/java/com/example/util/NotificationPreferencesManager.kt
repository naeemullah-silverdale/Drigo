package com.example.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "drigo_notification_preferences")

/**
 * Data model representing fine-grained notification toggles for Passengers & Drivers.
 */
data class NotificationPreferences(
    // Master switch
    val masterNotificationsEnabled: Boolean = true,

    // Core Categories
    val tripRemindersEnabled: Boolean = true,
    val chatMessagesEnabled: Boolean = true,
    val promotionalAlertsEnabled: Boolean = false,

    // Passenger specifics
    val driverArrivalAlertsEnabled: Boolean = true,
    val priceCounterOffersEnabled: Boolean = true,

    // Driver specifics
    val newRideRadarAlertsEnabled: Boolean = true,
    val sharedRideMatchesEnabled: Boolean = true,

    // Audio & Feedback
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val voiceAnnouncementsEnabled: Boolean = true
)

class NotificationPreferenceStore(private val context: Context) {

    companion object {
        private val KEY_MASTER = booleanPreferencesKey("notif_master_enabled")
        private val KEY_TRIP_REMINDERS = booleanPreferencesKey("notif_trip_reminders_enabled")
        private val KEY_CHAT_MESSAGES = booleanPreferencesKey("notif_chat_messages_enabled")
        private val KEY_PROMO_ALERTS = booleanPreferencesKey("notif_promo_alerts_enabled")

        private val KEY_DRIVER_ARRIVAL = booleanPreferencesKey("notif_driver_arrival_enabled")
        private val KEY_PRICE_COUNTER = booleanPreferencesKey("notif_price_counter_enabled")

        private val KEY_NEW_RIDE_RADAR = booleanPreferencesKey("notif_new_ride_radar_enabled")
        private val KEY_SHARED_RIDE = booleanPreferencesKey("notif_shared_ride_enabled")

        private val KEY_SOUND = booleanPreferencesKey("notif_sound_enabled")
        private val KEY_VIBRATION = booleanPreferencesKey("notif_vibration_enabled")
        private val KEY_VOICE = booleanPreferencesKey("notif_voice_enabled")
    }

    val preferencesFlow: Flow<NotificationPreferences> = context.notificationDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            NotificationPreferences(
                masterNotificationsEnabled = prefs[KEY_MASTER] ?: true,
                tripRemindersEnabled = prefs[KEY_TRIP_REMINDERS] ?: true,
                chatMessagesEnabled = prefs[KEY_CHAT_MESSAGES] ?: true,
                promotionalAlertsEnabled = prefs[KEY_PROMO_ALERTS] ?: false,
                driverArrivalAlertsEnabled = prefs[KEY_DRIVER_ARRIVAL] ?: true,
                priceCounterOffersEnabled = prefs[KEY_PRICE_COUNTER] ?: true,
                newRideRadarAlertsEnabled = prefs[KEY_NEW_RIDE_RADAR] ?: true,
                sharedRideMatchesEnabled = prefs[KEY_SHARED_RIDE] ?: true,
                soundEnabled = prefs[KEY_SOUND] ?: true,
                vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
                voiceAnnouncementsEnabled = prefs[KEY_VOICE] ?: true
            )
        }

    suspend fun updatePreferences(update: (NotificationPreferences) -> NotificationPreferences) {
        context.notificationDataStore.edit { prefs ->
            val current = NotificationPreferences(
                masterNotificationsEnabled = prefs[KEY_MASTER] ?: true,
                tripRemindersEnabled = prefs[KEY_TRIP_REMINDERS] ?: true,
                chatMessagesEnabled = prefs[KEY_CHAT_MESSAGES] ?: true,
                promotionalAlertsEnabled = prefs[KEY_PROMO_ALERTS] ?: false,
                driverArrivalAlertsEnabled = prefs[KEY_DRIVER_ARRIVAL] ?: true,
                priceCounterOffersEnabled = prefs[KEY_PRICE_COUNTER] ?: true,
                newRideRadarAlertsEnabled = prefs[KEY_NEW_RIDE_RADAR] ?: true,
                sharedRideMatchesEnabled = prefs[KEY_SHARED_RIDE] ?: true,
                soundEnabled = prefs[KEY_SOUND] ?: true,
                vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
                voiceAnnouncementsEnabled = prefs[KEY_VOICE] ?: true
            )
            val updated = update(current)

            prefs[KEY_MASTER] = updated.masterNotificationsEnabled
            prefs[KEY_TRIP_REMINDERS] = updated.tripRemindersEnabled
            prefs[KEY_CHAT_MESSAGES] = updated.chatMessagesEnabled
            prefs[KEY_PROMO_ALERTS] = updated.promotionalAlertsEnabled
            prefs[KEY_DRIVER_ARRIVAL] = updated.driverArrivalAlertsEnabled
            prefs[KEY_PRICE_COUNTER] = updated.priceCounterOffersEnabled
            prefs[KEY_NEW_RIDE_RADAR] = updated.newRideRadarAlertsEnabled
            prefs[KEY_SHARED_RIDE] = updated.sharedRideMatchesEnabled
            prefs[KEY_SOUND] = updated.soundEnabled
            prefs[KEY_VIBRATION] = updated.vibrationEnabled
            prefs[KEY_VOICE] = updated.voiceAnnouncementsEnabled
        }
    }
}

/**
 * Global singleton manager for observing and toggling user notification settings.
 */
object NotificationPreferencesManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var store: NotificationPreferenceStore? = null

    private val _preferences = MutableStateFlow(NotificationPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    fun init(context: Context) {
        if (store == null) {
            val s = NotificationPreferenceStore(context.applicationContext)
            store = s
            scope.launch {
                s.preferencesFlow.collect { updated ->
                    _preferences.value = updated
                }
            }
        }
    }

    fun updatePreferences(context: Context, update: (NotificationPreferences) -> NotificationPreferences) {
        init(context)
        val updated = update(_preferences.value)
        _preferences.value = updated
        scope.launch {
            store?.updatePreferences(update)
        }
    }

    /**
     * Helper to verify whether a given notification type or category is allowed.
     */
    fun isNotificationAllowed(type: RideNotificationType): Boolean {
        val prefs = _preferences.value
        if (!prefs.masterNotificationsEnabled) return false

        return when (type) {
            // Promotional
            RideNotificationType.PROMOTIONAL_ALERT -> prefs.promotionalAlertsEnabled

            // Chat Messages
            RideNotificationType.CHAT_MESSAGE -> prefs.chatMessagesEnabled

            // Passenger Trip Reminders & Life-cycle
            RideNotificationType.PASSENGER_DRIVER_ARRIVING,
            RideNotificationType.PASSENGER_DRIVER_ARRIVED -> prefs.tripRemindersEnabled && prefs.driverArrivalAlertsEnabled

            RideNotificationType.PASSENGER_DRIVER_COUNTER_OFFER -> prefs.tripRemindersEnabled && prefs.priceCounterOffersEnabled

            RideNotificationType.PASSENGER_DRIVER_FOUND,
            RideNotificationType.PASSENGER_DRIVER_ACCEPTED,
            RideNotificationType.PASSENGER_RIDE_STARTED,
            RideNotificationType.PASSENGER_RIDE_COMPLETED,
            RideNotificationType.PASSENGER_RIDE_CANCELLED -> prefs.tripRemindersEnabled

            // Driver radar alerts & matches
            RideNotificationType.DRIVER_NEW_REQUEST -> prefs.tripRemindersEnabled && prefs.newRideRadarAlertsEnabled
            RideNotificationType.DRIVER_SHARED_MATCH -> prefs.tripRemindersEnabled && prefs.sharedRideMatchesEnabled

            RideNotificationType.DRIVER_OFFER_ACCEPTED,
            RideNotificationType.DRIVER_RIDE_ASSIGNED,
            RideNotificationType.DRIVER_RIDE_CANCELLED -> prefs.tripRemindersEnabled
        }
    }
}
