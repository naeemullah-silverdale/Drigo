package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DrigoBrandPurple
import com.example.ui.theme.InDriveLimeGreen
import com.example.util.InAppNotificationItem
import com.example.util.RideNotificationType

/**
 * Bottom Sheet displaying recent notifications history for Captains and Passengers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    notifications: List<InAppNotificationItem>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14171F),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF373E4E),
                height = 4.dp,
                width = 36.dp
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = DrigoBrandPurple.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = DrigoBrandPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notification Center",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (notifications.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = DrigoBrandPurple,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${notifications.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Real-time updates, ride bids & alerts",
                        fontSize = 11.5.sp,
                        color = Color(0xFF8A93A4)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenSettings != null) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("notification_center_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Notification Settings",
                                tint = Color(0xFF8A93A4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAll,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("clear_all_notifications_button")
                        ) {
                            Text(
                                text = "Clear All",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF222632), thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            if (notifications.isEmpty()) {
                // Empty State
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1D2230),
                            border = BorderStroke(1.dp, Color(0xFF2E3547)),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = Color(0xFF6C768D),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "No Notifications Yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "You'll see real-time alerts for captain arrivals, counter-offers, trip receipts, and safety updates here.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF8A93A4),
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        NotificationHistoryCard(
                            item = item,
                            onClick = {
                                item.onActionClick?.invoke()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationHistoryCard(
    item: InAppNotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (item.type) {
        RideNotificationType.PASSENGER_DRIVER_FOUND -> Color(0xFF00E5FF)
        RideNotificationType.PASSENGER_DRIVER_ACCEPTED -> InDriveLimeGreen
        RideNotificationType.PASSENGER_DRIVER_COUNTER_OFFER -> DrigoBrandPurple
        RideNotificationType.PASSENGER_DRIVER_ARRIVING -> Color(0xFFFFB300)
        RideNotificationType.PASSENGER_DRIVER_ARRIVED -> Color(0xFF00E676)
        RideNotificationType.PASSENGER_RIDE_STARTED -> Color(0xFF29B6F6)
        RideNotificationType.PASSENGER_RIDE_COMPLETED -> Color(0xFFFFD54F)
        RideNotificationType.PASSENGER_RIDE_CANCELLED -> Color(0xFFFF5252)
        RideNotificationType.DRIVER_NEW_REQUEST -> InDriveLimeGreen
        RideNotificationType.DRIVER_OFFER_ACCEPTED -> Color(0xFF00E676)
        RideNotificationType.DRIVER_RIDE_ASSIGNED -> Color(0xFF7C4DFF)
        RideNotificationType.DRIVER_RIDE_CANCELLED -> Color(0xFFFF5252)
        RideNotificationType.DRIVER_SHARED_MATCH -> Color(0xFFFF4081)
        RideNotificationType.CHAT_MESSAGE -> DrigoBrandPurple
        RideNotificationType.PROMOTIONAL_ALERT -> Color(0xFFFF9800)
    }

    val icon = when (item.type) {
        RideNotificationType.PASSENGER_DRIVER_FOUND -> Icons.Default.PersonSearch
        RideNotificationType.PASSENGER_DRIVER_ACCEPTED -> Icons.Default.CheckCircle
        RideNotificationType.PASSENGER_DRIVER_COUNTER_OFFER -> Icons.Default.LocalOffer
        RideNotificationType.PASSENGER_DRIVER_ARRIVING -> Icons.Default.DirectionsCar
        RideNotificationType.PASSENGER_DRIVER_ARRIVED -> Icons.Default.LocationOn
        RideNotificationType.PASSENGER_RIDE_STARTED -> Icons.Default.Navigation
        RideNotificationType.PASSENGER_RIDE_COMPLETED -> Icons.Default.Star
        RideNotificationType.PASSENGER_RIDE_CANCELLED -> Icons.Default.Cancel
        RideNotificationType.DRIVER_NEW_REQUEST -> Icons.Default.NotificationsActive
        RideNotificationType.DRIVER_OFFER_ACCEPTED -> Icons.Default.ThumbUp
        RideNotificationType.DRIVER_RIDE_ASSIGNED -> Icons.Default.AssignmentTurnedIn
        RideNotificationType.DRIVER_RIDE_CANCELLED -> Icons.Default.HighlightOff
        RideNotificationType.DRIVER_SHARED_MATCH -> Icons.Default.GroupAdd
        RideNotificationType.CHAT_MESSAGE -> Icons.Default.Chat
        RideNotificationType.PROMOTIONAL_ALERT -> Icons.Default.LocalOffer
    }

    val timeAgo = remember(item.timestamp) {
        val diffMs = System.currentTimeMillis() - item.timestamp
        val diffMinutes = (diffMs / (1000 * 60)).toInt()
        when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
            else -> "${diffMinutes / 1440}d ago"
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B1E28),
        border = BorderStroke(1.dp, Color(0xFF282D3D)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = item.subText ?: item.type.categoryName,
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = timeAgo,
                        fontSize = 10.sp,
                        color = Color(0xFF6B7487)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.message,
                    fontSize = 11.5.sp,
                    color = Color(0xFFA5ACB8),
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.actionLabel != null || (item.farePkr != null && item.farePkr > 0)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (item.farePkr != null && item.farePkr > 0) {
                            Text(
                                text = "Fare: PKR ${item.farePkr}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = InDriveLimeGreen
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        if (item.actionLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.7f))
                            ) {
                                Text(
                                    text = item.actionLabel,
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
