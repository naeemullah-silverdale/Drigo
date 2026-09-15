package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.util.NotificationPreferences
import com.example.util.NotificationPreferencesManager
import com.example.util.ThemeManager
import com.example.util.ThemeMode
import com.example.viewmodel.UserMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Ensure Notification Preferences DataStore initialized
    LaunchedEffect(Unit) {
        NotificationPreferencesManager.init(context)
    }

    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    val notifPrefs by NotificationPreferencesManager.preferences.collectAsState()

    var selectedRoleTab by remember { mutableStateOf(UserMode.PASSENGER) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Appearance & Notification Preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("settings_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= SECTION 1: APPEARANCE (THEME) =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DrigoBrandPurple.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = DrigoBrandPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose theme for light, dark, or system default mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Theme Cards Grid / Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val isSelected = currentThemeMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) DrigoBrandPurple else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) DrigoBrandPurple else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        ThemeManager.setThemeMode(context, mode)
                                    }
                                    .testTag("theme_option_${mode.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            ThemeMode.LIGHT -> Icons.Default.LightMode
                                            ThemeMode.DARK -> Icons.Default.DarkMode
                                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else DrigoBrandPurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = when (mode) {
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                            ThemeMode.SYSTEM -> "System"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= SECTION 2: NOTIFICATION PREFERENCES =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
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
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = DrigoBrandPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Notifications & Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Customize trip reminders, sound, & chat alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Master Switch
                        Switch(
                            checked = notifPrefs.masterNotificationsEnabled,
                            onCheckedChange = { isChecked ->
                                NotificationPreferencesManager.updatePreferences(context) {
                                    it.copy(masterNotificationsEnabled = isChecked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DrigoBrandPurple
                            ),
                            modifier = Modifier.testTag("master_notification_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    AnimatedVisibility(visible = notifPrefs.masterNotificationsEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Category Title
                            Text(
                                text = "CORE ALERT CATEGORIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple,
                                letterSpacing = 1.sp
                            )

                            // Trip Reminders Toggle
                            SettingsToggleItem(
                                icon = Icons.Default.DirectionsCar,
                                title = "Trip Reminders & Status Updates",
                                subtitle = "Alerts when captain accepts, arrives, or ride starts",
                                checked = notifPrefs.tripRemindersEnabled,
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) {
                                        it.copy(tripRemindersEnabled = isChecked)
                                    }
                                },
                                testTag = "toggle_trip_reminders"
                            )

                            // Chat Messages Toggle
                            SettingsToggleItem(
                                icon = Icons.Default.Chat,
                                title = "Chat & Driver Messages",
                                subtitle = "Real-time in-app conversation notifications",
                                checked = notifPrefs.chatMessagesEnabled,
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) {
                                        it.copy(chatMessagesEnabled = isChecked)
                                    }
                                },
                                testTag = "toggle_chat_messages"
                            )

                            // Promotional Alerts Toggle
                            SettingsToggleItem(
                                icon = Icons.Default.LocalOffer,
                                title = "Promotions & Discounts",
                                subtitle = "Exclusive fare discounts and promo codes",
                                checked = notifPrefs.promotionalAlertsEnabled,
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) {
                                        it.copy(promotionalAlertsEnabled = isChecked)
                                    }
                                },
                                testTag = "toggle_promo_alerts"
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Role Specific Preferences Tab Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ROLE SPECIFIC ALERTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DrigoBrandPurple,
                                    letterSpacing = 1.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = selectedRoleTab == UserMode.PASSENGER,
                                        onClick = { selectedRoleTab = UserMode.PASSENGER },
                                        label = { Text("Rider", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DrigoBrandPurple,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = selectedRoleTab == UserMode.DRIVER,
                                        onClick = { selectedRoleTab = UserMode.DRIVER },
                                        label = { Text("Captain", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DrigoBrandPurple,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            if (selectedRoleTab == UserMode.PASSENGER) {
                                SettingsToggleItem(
                                    icon = Icons.Default.PersonPinCircle,
                                    title = "Captain Arrival Alerts",
                                    subtitle = "Pop-up alert with 4-digit PIN when captain reaches pickup",
                                    checked = notifPrefs.driverArrivalAlertsEnabled,
                                    onCheckedChange = { isChecked ->
                                        NotificationPreferencesManager.updatePreferences(context) {
                                            it.copy(driverArrivalAlertsEnabled = isChecked)
                                        }
                                    },
                                    testTag = "toggle_driver_arrival"
                                )

                                SettingsToggleItem(
                                    icon = Icons.Default.PriceCheck,
                                    title = "Price Counter-Offers",
                                    subtitle = "Real-time alerts when captains offer custom fares",
                                    checked = notifPrefs.priceCounterOffersEnabled,
                                    onCheckedChange = { isChecked ->
                                        NotificationPreferencesManager.updatePreferences(context) {
                                            it.copy(priceCounterOffersEnabled = isChecked)
                                        }
                                    },
                                    testTag = "toggle_price_counter"
                                )
                            } else {
                                SettingsToggleItem(
                                    icon = Icons.Default.Radar,
                                    title = "New Ride Radar Alerts",
                                    subtitle = "Instant push alert when nearby passenger requests a ride",
                                    checked = notifPrefs.newRideRadarAlertsEnabled,
                                    onCheckedChange = { isChecked ->
                                        NotificationPreferencesManager.updatePreferences(context) {
                                            it.copy(newRideRadarAlertsEnabled = isChecked)
                                        }
                                    },
                                    testTag = "toggle_ride_radar"
                                )

                                SettingsToggleItem(
                                    icon = Icons.Default.DepartureBoard,
                                    title = "Shared & Intercity Ride Matches",
                                    subtitle = "Alerts for new seat requests on planned departures",
                                    checked = notifPrefs.sharedRideMatchesEnabled,
                                    onCheckedChange = { isChecked ->
                                        NotificationPreferencesManager.updatePreferences(context) {
                                            it.copy(sharedRideMatchesEnabled = isChecked)
                                        }
                                    },
                                    testTag = "toggle_shared_ride"
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Sound & Feedback Section
                            Text(
                                text = "SOUND & HAPTIC CHANNELS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple,
                                letterSpacing = 1.sp
                            )

                            SettingsToggleItem(
                                icon = Icons.Default.VolumeUp,
                                title = "Sound & Audio Alerts",
                                subtitle = "Play chime audio effects on high-priority alerts",
                                checked = notifPrefs.soundEnabled,
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) {
                                        it.copy(soundEnabled = isChecked)
                                    }
                                },
                                testTag = "toggle_sound_alerts"
                            )

                            SettingsToggleItem(
                                icon = Icons.Default.Vibration,
                                title = "Haptic & Vibration Feedback",
                                subtitle = "Vibrate device for incoming ride offers & chats",
                                checked = notifPrefs.vibrationEnabled,
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) {
                                        it.copy(vibrationEnabled = isChecked)
                                    }
                                },
                                testTag = "toggle_vibration_alerts"
                            )

                            SettingsToggleItem(
                                icon = Icons.Default.RecordVoiceOver,
                                title = "Voice Status Announcements",
                                subtitle = "Speak ride arrival status and PIN code out loud",
                                checked = notifPrefs.voiceAnnouncementsEnabled,
                                onCheckedChange = { isChecked ->
                                    NotificationPreferencesManager.updatePreferences(context) {
                                        it.copy(voiceAnnouncementsEnabled = isChecked)
                                    }
                                },
                                testTag = "toggle_voice_announcements"
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Reset to defaults button
                            OutlinedButton(
                                onClick = {
                                    NotificationPreferencesManager.updatePreferences(context) {
                                        NotificationPreferences()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_notification_defaults_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset All Notification Preferences", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ================= SECTION 3: APP INFO =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Drigo Ride Share",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mass-Market Ride Sharing • Version 1.2.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DrigoBrandPurple.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "v1.2.0",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DrigoBrandPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) DrigoBrandPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DrigoBrandPurple
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
