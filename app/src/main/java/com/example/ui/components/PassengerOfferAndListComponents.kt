package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannedDepartureBooking
import com.example.data.model.PlannedDepartureOffer
import com.example.ui.theme.DrigoBrandPurple

// Semantic Color Palette for Passenger Management
private val MintGreen = Color(0xFF00C853)
private val DarkGreen = Color(0xFF00897B)
private val LightMintBg = Color(0xFFE8F8EE)
private val MotorwayBlue = Color(0xFF1976D2)
private val LightBlueBg = Color(0xFFE3F2FD)
private val AmberTag = Color(0xFFFF9800)
private val LightAmberBg = Color(0xFFFFF3E0)
private val SoftRedBg = Color(0xFFFFEBEE)
private val DarkRed = Color(0xFFD32F2F)

private data class OfferTagStyle(
    val bg: Color,
    val text: Color,
    val label: String,
    val icon: ImageVector
)

/**
 * Real-time Pulsing or Solid Status Indicator Dot with smooth color transitions
 */
@Composable
fun RealtimeStatusDot(
    statusColor: Color,
    isPulsing: Boolean = false,
    size: Int = 8,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "realtimeDotColorAnim"
    )

    if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(animatedColor.copy(alpha = alpha))
        )
    } else {
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(animatedColor)
        )
    }
}

/**
 * Production-grade Material 3 Passenger Offer Card for City-to-City and Urban Rides.
 * Strictly maps all Firestore fields from `PlannedDepartureOffer` to UI components.
 * Supports adaptive layouts across low-end (320dp–360dp) to large screens with zero overlap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassengerOfferCard(
    offer: PlannedDepartureOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCounter: () -> Unit,
    onMessage: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAccepted = offer.status.equals("ACCEPTED", ignoreCase = true)
    val isCountered = offer.status.equals("COUNTERED", ignoreCase = true)
    val isDeclined = offer.status.equals("DECLINED", ignoreCase = true) || offer.status.equals("REJECTED", ignoreCase = true)
    val isPending = !isAccepted && !isCountered && !isDeclined
    val isMultiSeat = offer.requestedSeats > 1

    // Data-driven dynamic border and status styling
    val statusColor by animateColorAsState(
        targetValue = when {
            isAccepted -> MintGreen
            isCountered -> AmberTag
            isDeclined -> DarkRed
            offer.differencePkr < 0 -> Color(0xFFFB8C00)
            else -> MintGreen
        },
        label = "statusColor"
    )

    val cardBorderColor = when {
        isAccepted -> MintGreen.copy(alpha = 0.6f)
        isCountered -> AmberTag.copy(alpha = 0.6f)
        isDeclined -> DarkRed.copy(alpha = 0.35f)
        offer.isFullFare -> MintGreen.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
        border = BorderStroke(1.2.dp, cardBorderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // -------------------------------------------------------------
            // SECTION 1: HEADER STRIP (SEATS, BOOKING TYPE, & LIVE STATUS)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Seat Count & Booking Type Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (isMultiSeat) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DrigoBrandPurple,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${offer.requestedSeats} SEATS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "1 SEAT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (offer.bookingType.equals("PRIVATE", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Full Car",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Clear Status Tag with Realtime Status Dot
                val tagStyle = when {
                    isAccepted -> OfferTagStyle(LightMintBg, DarkGreen, "ACCEPTED", Icons.Default.CheckCircle)
                    isCountered -> OfferTagStyle(LightAmberBg, AmberTag, "COUNTER SENT", Icons.Default.Schedule)
                    isDeclined -> OfferTagStyle(SoftRedBg, DarkRed, "REJECTED", Icons.Outlined.Cancel)
                    offer.differencePkr < 0 -> OfferTagStyle(LightAmberBg, Color(0xFFD84315), "PKR ${-offer.differencePkr} LESS", Icons.Outlined.TrendingDown)
                    offer.differencePkr > 0 -> OfferTagStyle(LightMintBg, DarkGreen, "+PKR ${offer.differencePkr} BONUS", Icons.Outlined.TrendingUp)
                    else -> OfferTagStyle(LightMintBg, DarkGreen, "FULL FARE", Icons.Default.Done)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tagStyle.bg,
                    border = BorderStroke(1.dp, tagStyle.text.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AnimatedContent(
                            targetState = offer.status,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                 slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> -height / 2 })
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> height / 2 }
                                    )
                            },
                            label = "offerStatusDotTransition"
                        ) { _ ->
                            RealtimeStatusDot(
                                statusColor = tagStyle.text,
                                isPulsing = isPending,
                                size = 7
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        AnimatedContent(
                            targetState = tagStyle.label,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                 slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> -height / 2 })
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> height / 2 }
                                    )
                            },
                            label = "offerStatusLabelTransition"
                        ) { label ->
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tagStyle.text,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 2: PASSENGER PROFILE & QUICK CONTACTS
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Passenger Initials Circle Avatar with Status Ring
                    Surface(
                        shape = CircleShape,
                        color = DrigoBrandPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, DrigoBrandPurple.copy(alpha = 0.35f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = offer.passengerName.trim().take(2).uppercase().ifBlank { "PS" },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = offer.passengerName.ifBlank { "Passenger" },
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (offer.isVerified) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LightMintBg
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Verified",
                                            tint = DarkGreen,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Verified",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkGreen,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "%.1f".format(if (offer.passengerRating > 0) offer.passengerRating else 4.9),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "•",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${offer.passengerRidesCompleted} rides",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Quick Contact Buttons (Phone & Chat with 48dp touch targets)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call ${offer.passengerName}",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onMessage,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Message ${offer.passengerName}",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // -------------------------------------------------------------
            // SECTION 3: ROUTE TIMELINE (PICKUP & DROP-OFF)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pickup Point
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MintGreen)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PICKUP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            letterSpacing = 0.5.sp,
                            softWrap = false
                        )
                        Text(
                            text = offer.pickupPoint.ifBlank { "Pickup Location" },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Drop-off Point
                if (offer.dropoffPoint.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFD84315))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DROP-OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315),
                                letterSpacing = 0.5.sp,
                                softWrap = false
                            )
                            Text(
                                text = offer.dropoffPoint,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 3B: RESPONSIVE BADGES (LUGGAGE & PAYMENT VIA FLOWROW)
            // -------------------------------------------------------------
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (offer.luggageDetails.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Luggage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = offer.luggageDetails,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (offer.paymentMethod.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = offer.paymentMethod,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 4 & 5: FARE ECONOMICS & ACTIONS (ACCEPTED vs NEGOTIATING)
            // -------------------------------------------------------------
            if (isAccepted) {
                // CONFIRMED / ACCEPTED STATE
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightMintBg.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MintGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Agreed Fare (Confirmed)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkGreen
                                )
                                val perSeat = if (offer.requestedSeats > 0) offer.offeredFare / offer.requestedSeats else offer.offeredFare
                                Text(
                                    text = if (isMultiSeat) "PKR %,d / seat • %d seats".format(perSeat, offer.requestedSeats) else "1 Seat • ${if (offer.paymentMethod.isNotBlank()) offer.paymentMethod else "Cash on Boarding"}",
                                    fontSize = 10.5.sp,
                                    color = DarkGreen.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = "PKR %,d".format(offer.offeredFare),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkGreen
                            )
                        }

                        HorizontalDivider(
                            color = MintGreen.copy(alpha = 0.25f),
                            thickness = 0.8.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = DarkGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Confirmed in Route Manifest",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MintGreen.copy(alpha = 0.18f),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = "Rider Notified",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                // FARE BREAKDOWN FOR PENDING / COUNTERED / DECLINED
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Standard Asking Row
                        if (offer.standardAsking > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Asking Fare:",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isMultiSeat) {
                                        val perSeatAsking = if (offer.requestedSeats > 0) offer.standardAsking / offer.requestedSeats else offer.standardAsking
                                        Text(
                                            text = " • PKR %,d / seat".format(perSeatAsking),
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "PKR %,d".format(offer.standardAsking),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }

                        // Passenger Offer Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Passenger Offer:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isMultiSeat && offer.offeredFare > 0) {
                                    val perSeatOffer = if (offer.requestedSeats > 0) offer.offeredFare / offer.requestedSeats else offer.offeredFare
                                    Text(
                                        text = "PKR %,d / seat • %d seats".format(perSeatOffer, offer.requestedSeats),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (offer.differencePkr < 0) AmberTag else DarkGreen
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "PKR %,d".format(offer.offeredFare),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (offer.differencePkr < 0) Color(0xFFD84315) else DarkGreen
                                )
                                if (offer.differencePkr != 0) {
                                    Text(
                                        text = if (offer.differencePkr < 0) "-PKR %,d".format(-offer.differencePkr) else "+PKR %,d".format(offer.differencePkr),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (offer.differencePkr < 0) AmberTag else DarkGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Note Bubble if rider included a note
                if (offer.note.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LightAmberBg,
                        border = BorderStroke(1.dp, AmberTag.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Chat,
                                contentDescription = null,
                                tint = AmberTag,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "\"${offer.note}\"",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4E342E),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ACTIONS FOR PENDING / COUNTERED / DECLINED
                when {
                    // VISUAL STATE: REJECTED / DECLINED
                    isDeclined -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftRedBg,
                            border = BorderStroke(1.2.dp, DarkRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Cancel,
                                        contentDescription = null,
                                        tint = DarkRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Offer Declined",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkRed
                                        )
                                        Text(
                                            text = "Passenger notified",
                                            fontSize = 10.5.sp,
                                            color = DarkRed.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = onCounter,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Make Counter",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberTag
                                    )
                                }
                            }
                        }
                    }

                    // VISUAL STATE: COUNTER SENT
                    isCountered -> {
                        val counterAmount = offer.counterOfferPkr ?: (offer.offeredFare + 100)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LightAmberBg,
                            border = BorderStroke(1.2.dp, AmberTag),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color(0xFFD84315),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Counter-Offer Sent:",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD84315)
                                        )
                                    }
                                    Text(
                                        text = "PKR %,d".format(counterAmount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD84315)
                                    )
                                }

                                Text(
                                    text = "Awaiting rider confirmation. You can adjust counter fare or accept original.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6D4C41)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = onDecline,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        border = BorderStroke(1.dp, DarkRed.copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkRed),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Text("Reject", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }

                                    Button(
                                        onClick = onCounter,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LightAmberBg,
                                            contentColor = Color(0xFFD84315)
                                        ),
                                        border = BorderStroke(1.dp, AmberTag.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(40.dp)
                                    ) {
                                        Text("Edit Counter", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }

                                    Button(
                                        onClick = onAccept,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MintGreen,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(40.dp)
                                    ) {
                                        Text("Accept Orig", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    // VISUAL STATE: PENDING REVIEW
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. REJECT / DECLINE (Red)
                            OutlinedButton(
                                onClick = onDecline,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                border = BorderStroke(1.dp, DarkRed.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SoftRedBg.copy(alpha = 0.5f),
                                    contentColor = DarkRed
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Cancel,
                                        contentDescription = null,
                                        tint = DarkRed,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reject",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 2. COUNTER (Amber)
                            Button(
                                onClick = onCounter,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LightAmberBg,
                                    contentColor = Color(0xFFD84315)
                                ),
                                border = BorderStroke(1.dp, AmberTag.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1.05f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFFD84315),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Counter",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 3. ACCEPT (Solid Mint Green)
                            Button(
                                onClick = onAccept,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintGreen,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Accept",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
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

/**
 * Backward compatibility wrapper for `Material3PassengerOfferCard`.
 */
@Composable
fun Material3PassengerOfferCard(
    offer: PlannedDepartureOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCounter: () -> Unit,
    onMessage: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    PassengerOfferCard(
        offer = offer,
        onAccept = onAccept,
        onDecline = onDecline,
        onCounter = onCounter,
        onMessage = onMessage,
        onCall = onCall,
        modifier = modifier
    )
}

/**
 * Individual Card representation for Confirmed / Booked Passenger in the PassengerList.
 * Strictly maps `PlannedDepartureBooking` Firestore fields with live status dot.
 */
@Composable
fun PassengerCard(
    booking: PlannedDepartureBooking,
    index: Int,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onToggleBoarded: (() -> Unit)? = null,
    stopNumberLabel: String = "Stop ${index + 1}",
    modifier: Modifier = Modifier
) {
    val isBoarded = booking.status.equals("BOARDED", ignoreCase = true)
    val isCancelled = booking.status.equals("CANCELLED", ignoreCase = true)
    val isPending = booking.status.equals("PENDING_APPROVAL", ignoreCase = true) || booking.status.equals("PENDING", ignoreCase = true)

    val bookingState = when {
        isBoarded -> "BOARDED"
        isCancelled -> "CANCELLED"
        isPending -> "PENDING"
        else -> "ACCEPTED" // CONFIRMED
    }

    val targetDotColor = when (bookingState) {
        "BOARDED" -> MotorwayBlue
        "CANCELLED" -> DarkRed
        "PENDING" -> AmberTag
        else -> MintGreen // CONFIRMED / ACCEPTED
    }

    val targetStatusText = when (bookingState) {
        "BOARDED" -> "BOARDED"
        "CANCELLED" -> "CANCELLED"
        "PENDING" -> "PENDING"
        else -> "CONFIRMED"
    }

    val targetBg = when (bookingState) {
        "BOARDED" -> LightBlueBg
        "CANCELLED" -> SoftRedBg
        "PENDING" -> LightAmberBg
        else -> LightMintBg
    }

    val animatedDotColor by animateColorAsState(
        targetValue = targetDotColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "passengerDotColorAnim"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "passengerBgAnim"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp, pressedElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Stop # / Index Pill + Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LightMintBg
                ) {
                    Text(
                        text = stopNumberLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = animatedBgColor
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        AnimatedContent(
                            targetState = bookingState,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                 slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> -height / 2 })
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> height / 2 }
                                    )
                            },
                            label = "statusDotTransition"
                        ) { state ->
                            RealtimeStatusDot(
                                statusColor = animatedDotColor,
                                isPulsing = state == "PENDING",
                                size = 6
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        AnimatedContent(
                            targetState = targetStatusText,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                 slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> -height / 2 })
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> height / 2 }
                                    )
                            },
                            label = "statusTextTransition"
                        ) { text ->
                            Text(
                                text = text,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = animatedDotColor
                            )
                        }
                    }
                }
            }

            // Passenger Profile & Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Initials Avatar with Animated Status Dot
                    Box(modifier = Modifier.size(40.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = DrigoBrandPurple.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, DrigoBrandPurple.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = booking.passengerName.trim().take(2).uppercase().ifBlank { "PS" },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrigoBrandPurple
                                )
                            }
                        }

                        // Subtle slide + cross-fade animation for status dot on state transitions
                        AnimatedContent(
                            targetState = bookingState,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                 slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { h -> -h / 2 })
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { h -> h / 2 }
                                    )
                            },
                            modifier = Modifier.align(Alignment.BottomEnd),
                            label = "avatarStatusDotTransition"
                        ) { _ ->
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(1.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(animatedDotColor)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = booking.passengerName.ifBlank { "Passenger" },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "%.1f".format(if (booking.passengerRating > 0) booking.passengerRating else 5.0),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${booking.seatsBooked} Seat${if (booking.seatsBooked > 1) "s" else ""} • PKR %,d".format(booking.totalFarePkr),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Action icons: Call & SMS
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call ${booking.passengerName}",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onMessage,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Message ${booking.passengerName}",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Route Stops
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MintGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pickup: ${booking.pickupStop.ifBlank { "Pickup Location" }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (booking.dropoffStop.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color(0xFFD84315))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Drop-off: ${booking.dropoffStop}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Optional Boarding Toggle Action
            if (onToggleBoarded != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBoarded) "Passenger is on board" else "Ready for boarding check-in",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = onToggleBoarded,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = if (isBoarded) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = LightMintBg,
                                contentColor = DarkGreen
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = LightBlueBg,
                                contentColor = MotorwayBlue
                            )
                        },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBoarded) Icons.Default.Check else Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBoarded) "Boarded ✓" else "Check In",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Production-grade Material 3 PassengerList Component.
 * Renders confirmed riders and offers with live real-time sync status,
 * summary metrics header, empty state handling, and interactive contact triggers.
 */
@Composable
fun PassengerList(
    confirmedRiders: List<PlannedDepartureBooking>,
    onCall: (PlannedDepartureBooking) -> Unit,
    onMessage: (PlannedDepartureBooking) -> Unit,
    onToggleBoarded: ((PlannedDepartureBooking) -> Unit)? = null,
    totalCapacitySeats: Int = 4,
    onViewRouteManifest: (() -> Unit)? = null,
    stopNumberProvider: ((PlannedDepartureBooking, Int) -> String)? = null,
    modifier: Modifier = Modifier
) {
    val totalSeatsBooked = confirmedRiders.sumOf { it.seatsBooked }
    val totalRevenuePkr = confirmedRiders.sumOf { it.totalFarePkr }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // -------------------------------------------------------------
        // HEADER: METRICS & REALTIME SYNC INDICATOR
        // -------------------------------------------------------------
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    AnimatedContent(
                        targetState = confirmedRiders.isNotEmpty(),
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                             slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> -height / 2 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                    slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> height / 2 }
                                )
                        },
                        label = "manifestSyncDotTransition"
                    ) { hasRiders ->
                        RealtimeStatusDot(
                            statusColor = if (hasRiders) MintGreen else AmberTag,
                            isPulsing = true,
                            size = 8
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    AnimatedContent(
                        targetState = if (confirmedRiders.isNotEmpty()) "${confirmedRiders.size} CONFIRMED" else "0 BOOKED",
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                             slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> -height / 2 })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                    slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { height -> height / 2 }
                                )
                        },
                        label = "manifestSyncTextTransition"
                    ) { statusLabel ->
                        Text(
                            text = statusLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (confirmedRiders.isNotEmpty()) LightMintBg else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "$totalSeatsBooked/$totalCapacitySeats Seats • PKR %,d".format(totalRevenuePkr),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (confirmedRiders.isNotEmpty()) DarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }

                    if (onViewRouteManifest != null) {
                        Surface(
                            onClick = onViewRouteManifest,
                            shape = RoundedCornerShape(6.dp),
                            color = LightMintBg
                        ) {
                            Text(
                                text = "View Route ➔",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PASSENGER CARDS OR EMPTY STATE
        // -------------------------------------------------------------
        if (confirmedRiders.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Passengers Confirmed Yet",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Accepted rider offers will appear in this manifest automatically in real-time.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                confirmedRiders.forEachIndexed { index, booking ->
                    val stopLabel = stopNumberProvider?.invoke(booking, index) ?: "Stop ${index + 1}"
                    PassengerCard(
                        booking = booking,
                        index = index,
                        onCall = { onCall(booking) },
                        onMessage = { onMessage(booking) },
                        onToggleBoarded = onToggleBoarded?.let { { it(booking) } },
                        stopNumberLabel = stopLabel
                    )
                }
            }
        }
    }
}
