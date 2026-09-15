package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DrigoBrandPurple
import com.example.ui.theme.InDriveLimeGreen
import com.example.util.NotificationPreferences
import com.example.util.NotificationPreferencesManager
import com.example.viewmodel.UserMode

/**
 * User Settings Sheet allowing passengers and drivers to toggle specific
 * notification types such as Trip Reminders, Chat Messages, Promotional Alerts,
 * Radar alerts, and Sound/Haptic feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSheet(
    userMode: UserMode = UserMode.PASSENGER,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        NotificationPreferencesManager.init(context)
    }

    val prefs by NotificationPreferencesManager.preferences.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        },
        modifier = modifier.testTag("notification_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DrigoBrandPurple.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = DrigoBrandPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Notification Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (userMode == UserMode.DRIVER) "Driver Mode Preferences" else "Passenger Mode Preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("close_notification_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Master Push Notifications Switch
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (prefs.masterNotificationsEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                border = BorderStroke(
                    1.dp,
                    if (prefs.masterNotificationsEnabled) DrigoBrandPurple.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (prefs.masterNotificationsEnabled) DrigoBrandPurple else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (prefs.masterNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "All Notifications",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (prefs.masterNotificationsEnabled) "Enabled for status bar & alerts" else "All alerts muted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = prefs.masterNotificationsEnabled,
                        onCheckedChange = { isChecked ->
                            NotificationPreferencesManager.updatePreferences(context) { current ->
                                current.copy(masterNotificationsEnabled = isChecked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DrigoBrandPurple
                        ),
                        modifier = Modifier.testTag("toggle_master_notifications")
                    )
                }
            }

            AnimatedVisibility(visible = prefs.masterNotificationsEnabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 1: CORE NOTIFICATION CHANNELS (Trip Reminders, Chat Messages, Promotional Alerts)
                    Text(
                        text = "NOTIFICATION CATEGORIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DrigoBrandPurple,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            // 1. Trip Reminders (MANDATORY REQUIREMENT)
                            NotificationToggleItem(
                                icon = Icons.Default.AccessTimeFilled,
                                iconTint = Color(0xFF00A859),
                                title = "Trip Reminders",
                                subtitle = "Active status updates, ride ETA, arrival alerts & completion notices",
                                isChecked = prefs.tripRemindersEnabled,
                                testTag = "toggle_trip_reminders",
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) { it.copy(tripRemindersEnabled = isChecked) }
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // 2. Chat Messages (MANDATORY REQUIREMENT)
                            NotificationToggleItem(
                                icon = Icons.Default.Chat,
                                iconTint = DrigoBrandPurple,
                                title = "Chat Messages",
                                subtitle = "In-ride messages, quick responses & coordination chats",
                                isChecked = prefs.chatMessagesEnabled,
                                testTag = "toggle_chat_messages",
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) { it.copy(chatMessagesEnabled = isChecked) }
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // 3. Promotional Alerts (MANDATORY REQUIREMENT)
                            NotificationToggleItem(
                                icon = Icons.Default.LocalOffer,
                                iconTint = Color(0xFFFF9800),
                                title = "Promotional Alerts",
                                subtitle = "Discounts, seasonal promo codes & ride cashback offers",
                                isChecked = prefs.promotionalAlertsEnabled,
                                testTag = "toggle_promotional_alerts",
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) { it.copy(promotionalAlertsEnabled = isChecked) }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: ROLE SPECIFIC NOTIFICATIONS
                    if (userMode == UserMode.DRIVER) {
                        Text(
                            text = "DRIVER RADAR & RIDE OFFERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = InDriveLimeGreen,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                NotificationToggleItem(
                                    icon = Icons.Default.Sensors,
                                    iconTint = InDriveLimeGreen,
                                    title = "Nearby Ride Radar Requests",
                                    subtitle = "Sound & push alert whenever a passenger requests a ride nearby",
                                    isChecked = prefs.newRideRadarAlertsEnabled,
                                    testTag = "toggle_driver_radar",
                                    onCheckedChange = { isChecked ->
                                        NotificationPreferencesManager.updatePreferences(context) { it.copy(newRideRadarAlertsEnabled = isChecked) }
                                    }
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                NotificationToggleItem(
                                    icon = Icons.Default.GroupAdd,
                                    iconTint = Color(0xFF00B0FF),
                                    title = "Shared Route Matches",
                                    subtitle = "Alerts for secondary pickups along your existing route",
                                    isChecked = prefs.sharedRideMatchesEnabled,
                                    testTag = "toggle_driver_shared_route",
                                    onCheckedChange = { isChecked ->
                                        NotificationPreferencesManager.updatePreferences(context) { it.copy(sharedRideMatchesEnabled = isChecked) }
                                    }
                                )
                            }
                        }
                    } else {
                        // Passenger specifics
                        Text(
                            text = "PASSENGER RIDE UPDATES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DrigoBrandPurple,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                NotificationToggleItem(
                                    icon = Icons.Default.DirectionsCar,
                                    iconTint = Color(0xFF00E676),
                                    title = "Captain Arrival Alerts",
                                    subtitle = "Alert when your driver is 2 minutes away and has arrived",
                                    isChecked = prefs.driverArrivalAlertsEnabled,
                                    testTag = "toggle_driver_arrival",
                                    onCheckedChange = { isChecked ->
                                        NotificationPreferencesManager.updatePreferences(context) { it.copy(driverArrivalAlertsEnabled = isChecked) }
                                    }
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                NotificationToggleItem(
                                    icon = Icons.Default.PriceChange,
                                    iconTint = Color(0xFFB642F5),
                                    title = "Driver Counter-Offers & Bids",
                                    subtitle = "Instant alerts when drivers propose alternative fares",
                                    isChecked = prefs.priceCounterOffersEnabled,
                                    testTag = "toggle_price_counter_offers",
                                    onCheckedChange = { isChecked ->
                                        NotificationPreferencesManager.updatePreferences(context) { it.copy(priceCounterOffersEnabled = isChecked) }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 3: SOUND & VIBRATION
                    Text(
                        text = "ALERT FEEDBACK & AUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            NotificationToggleItem(
                                icon = Icons.Default.VolumeUp,
                                iconTint = Color(0xFF4FC3F7),
                                title = "Notification Sounds",
                                subtitle = "Play chime tones for ride alerts",
                                isChecked = prefs.soundEnabled,
                                testTag = "toggle_notif_sound",
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) { it.copy(soundEnabled = isChecked) }
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            NotificationToggleItem(
                                icon = Icons.Default.Vibration,
                                iconTint = Color(0xFFFFB74D),
                                title = "Vibration",
                                subtitle = "Haptic feedback pattern for incoming notifications",
                                isChecked = prefs.vibrationEnabled,
                                testTag = "toggle_notif_vibration",
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) { it.copy(vibrationEnabled = isChecked) }
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            NotificationToggleItem(
                                icon = Icons.Default.RecordVoiceOver,
                                iconTint = DrigoBrandPurple,
                                title = "Voice Status Announcements",
                                subtitle = "Spoken Urdu/English audio updates during active trips",
                                isChecked = prefs.voiceAnnouncementsEnabled,
                                testTag = "toggle_notif_voice",
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) { it.copy(voiceAnnouncementsEnabled = isChecked) }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DrigoBrandPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_notification_settings_btn")
            ) {
                Text(
                    text = "Save Preferences",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NotificationToggleItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DrigoBrandPurple
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
