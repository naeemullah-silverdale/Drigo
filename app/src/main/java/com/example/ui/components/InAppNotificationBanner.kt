package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
 * Animated In-App Heads-Up Notification Banner.
 * Displays on top of the screen whenever an important real-time ride event occurs.
 */
@Composable
fun InAppNotificationBanner(
    notification: InAppNotificationItem?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 350)
        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 250)
        ) + fadeOut(animationSpec = tween(durationMillis = 200)),
        modifier = modifier
    ) {
        if (notification != null) {
            val (icon, accentColor, bgGradient) = getNotificationVisuals(notification.type)

            var offsetY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (delta < -10) {
                                onDismiss()
                            }
                        }
                    )
                    .testTag("in_app_notification_banner")
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF16181F),
                    border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = accentColor)
                        .clickable {
                            notification.onActionClick?.invoke()
                            onDismiss()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.18f),
                                        Color(0xFF16181F),
                                        Color(0xFF181B24)
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Category Icon Bubble
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, accentColor),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = notification.title,
                                        tint = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Text Content
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = accentColor.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = notification.subText ?: notification.type.categoryName,
                                            color = accentColor,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                    if (notification.farePkr != null && notification.farePkr > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PKR ${notification.farePkr}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = notification.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = notification.message,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA0A6B5),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action or Dismiss
                            if (notification.actionLabel != null) {
                                Surface(
                                    onClick = {
                                        notification.onActionClick?.invoke()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = accentColor,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = notification.actionLabel,
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF788090),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NotificationVisuals(
    val icon: ImageVector,
    val accentColor: Color,
    val bgTint: Color
)

private fun getNotificationVisuals(type: RideNotificationType): NotificationVisuals {
    return when (type) {
        RideNotificationType.PASSENGER_DRIVER_FOUND -> NotificationVisuals(
            icon = Icons.Default.PersonSearch,
            accentColor = Color(0xFF00E5FF),
            bgTint = Color(0xFF00E5FF)
        )
        RideNotificationType.PASSENGER_DRIVER_ACCEPTED -> NotificationVisuals(
            icon = Icons.Default.CheckCircle,
            accentColor = InDriveLimeGreen,
            bgTint = InDriveLimeGreen
        )
        RideNotificationType.PASSENGER_DRIVER_COUNTER_OFFER -> NotificationVisuals(
            icon = Icons.Default.LocalOffer,
            accentColor = DrigoBrandPurple,
            bgTint = DrigoBrandPurple
        )
        RideNotificationType.PASSENGER_DRIVER_ARRIVING -> NotificationVisuals(
            icon = Icons.Default.DirectionsCar,
            accentColor = Color(0xFFFFB300),
            bgTint = Color(0xFFFFB300)
        )
        RideNotificationType.PASSENGER_DRIVER_ARRIVED -> NotificationVisuals(
            icon = Icons.Default.LocationOn,
            accentColor = Color(0xFF00E676),
            bgTint = Color(0xFF00E676)
        )
        RideNotificationType.PASSENGER_RIDE_STARTED -> NotificationVisuals(
            icon = Icons.Default.Navigation,
            accentColor = Color(0xFF29B6F6),
            bgTint = Color(0xFF29B6F6)
        )
        RideNotificationType.PASSENGER_RIDE_COMPLETED -> NotificationVisuals(
            icon = Icons.Default.Star,
            accentColor = Color(0xFFFFD54F),
            bgTint = Color(0xFFFFD54F)
        )
        RideNotificationType.PASSENGER_RIDE_CANCELLED -> NotificationVisuals(
            icon = Icons.Default.Cancel,
            accentColor = Color(0xFFFF5252),
            bgTint = Color(0xFFFF5252)
        )
        RideNotificationType.DRIVER_NEW_REQUEST -> NotificationVisuals(
            icon = Icons.Default.NotificationsActive,
            accentColor = InDriveLimeGreen,
            bgTint = InDriveLimeGreen
        )
        RideNotificationType.DRIVER_OFFER_ACCEPTED -> NotificationVisuals(
            icon = Icons.Default.ThumbUp,
            accentColor = Color(0xFF00E676),
            bgTint = Color(0xFF00E676)
        )
        RideNotificationType.DRIVER_RIDE_ASSIGNED -> NotificationVisuals(
            icon = Icons.Default.AssignmentTurnedIn,
            accentColor = Color(0xFF7C4DFF),
            bgTint = Color(0xFF7C4DFF)
        )
        RideNotificationType.DRIVER_RIDE_CANCELLED -> NotificationVisuals(
            icon = Icons.Default.HighlightOff,
            accentColor = Color(0xFFFF5252),
            bgTint = Color(0xFFFF5252)
        )
        RideNotificationType.DRIVER_SHARED_MATCH -> NotificationVisuals(
            icon = Icons.Default.GroupAdd,
            accentColor = Color(0xFFFF4081),
            bgTint = Color(0xFFFF4081)
        )
    }
}
