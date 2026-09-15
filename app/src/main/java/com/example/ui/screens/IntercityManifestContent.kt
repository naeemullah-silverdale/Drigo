package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntercityManifestUiState
import com.example.data.model.IntercityWaypoint
import com.example.data.model.IntercityWaypointPhase
import com.example.data.model.IntercityWaypointType
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureOffer
import com.example.ui.components.PassengerOfferCard
import com.example.ui.theme.DrigoBrandPurple

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
 * Responsive, production-grade Jetpack Compose UI for the Intercity Manifest screen.
 * Displays Phase 1 (Pickups heading toward highway) and Phase 2 (Drop-offs from highway exit),
 * sequentially numbered as Stop 1, Stop 2, Stop 3...
 * Also integrates Material3 passenger offer cards with dedicated Accept, Reject, and Counter actions.
 */
@Composable
fun IntercityManifestContent(
    manifestState: IntercityManifestUiState,
    departure: PlannedDeparture,
    onRecalculateRoute: () -> Unit,
    offers: List<PlannedDepartureOffer> = emptyList(),
    onAcceptOffer: (PlannedDepartureOffer) -> Unit = {},
    onDeclineOffer: (PlannedDepartureOffer) -> Unit = {},
    onCounterOffer: (PlannedDepartureOffer) -> Unit = {},
    onMessagePassenger: (PlannedDepartureOffer) -> Unit = {},
    onCallPassenger: (PlannedDepartureOffer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val completedStopIds = remember { mutableStateListOf<String>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. ROUTE OVERVIEW HERO CARD
        item {
            RouteManifestOverviewCard(
                manifestState = manifestState,
                departure = departure,
                onRecalculate = onRecalculateRoute
            )
        }

        // 2. PASSENGER OFFERS & BOOKING REQUESTS (IF PRESENT IN MANIFEST)
        if (offers.isNotEmpty()) {
            val pendingOffersCount = offers.count { it.status.equals("PENDING", true) || it.status.equals("COUNTERED", true) }
            val acceptedOffersCount = offers.count { it.status.equals("ACCEPTED", true) }
            item {
                PhaseHeader(
                    phaseNumber = 0,
                    title = "PASSENGER OFFERS & REQUESTS",
                    subtitle = "$pendingOffersCount pending review • $acceptedOffersCount confirmed",
                    containerColor = LightAmberBg,
                    accentColor = AmberTag,
                    icon = Icons.Default.LocalOffer
                )
            }
            items(offers, key = { "manifest_offer_${it.id}" }) { offer ->
                PassengerOfferCard(
                    offer = offer,
                    onAccept = { onAcceptOffer(offer) },
                    onDecline = { onDeclineOffer(offer) },
                    onCounter = { onCounterOffer(offer) },
                    onMessage = { onMessagePassenger(offer) },
                    onCall = { onCallPassenger(offer) }
                )
            }
        }

        // 3. PHASE 1: LOCAL PICKUPS HEADING TOWARD HIGHWAY
        item {
            PhaseHeader(
                phaseNumber = 1,
                title = "PHASE 1: LOCAL PICKUPS",
                subtitle = "Ordered shortest path heading to ${manifestState.highwayEntryHub}",
                containerColor = LightMintBg,
                accentColor = DarkGreen,
                icon = Icons.Default.DirectionsCar
            )
        }

        // Driver Start Hub
        manifestState.driverStartWaypoint?.let { driverStart ->
            item {
                DriverStartWaypointCard(waypoint = driverStart)
            }
        }

        // Sequenced Pickups (Stop 1, Stop 2...)
        if (manifestState.phase1Pickups.isEmpty()) {
            item {
                EmptyPhasePlaceholder(message = "No passenger pickups confirmed yet.")
            }
        } else {
            items(manifestState.phase1Pickups, key = { it.id }) { waypoint ->
                val isDone = completedStopIds.contains(waypoint.id)
                WaypointCard(
                    waypoint = waypoint,
                    isDone = isDone,
                    onToggleDone = {
                        if (isDone) completedStopIds.remove(waypoint.id) else completedStopIds.add(waypoint.id)
                    },
                    accentColor = DarkGreen,
                    badgeBgColor = LightMintBg
                )
            }
        }

        // 3. INTERCITY HIGHWAY CORRIDOR TRANSIT
        item {
            HighwayCorridorCard(
                manifestState = manifestState,
                departure = departure
            )
        }

        // 4. PHASE 2: DESTINATION DROP-OFFS FROM HIGHWAY EXIT
        item {
            PhaseHeader(
                phaseNumber = 2,
                title = "PHASE 2: DESTINATION DROP-OFFS",
                subtitle = "Ordered nearest to furthest from ${manifestState.highwayExitHub}",
                containerColor = LightBlueBg,
                accentColor = MotorwayBlue,
                icon = Icons.Default.LocationOn
            )
        }

        // Sequenced Drop-offs (Stop K, Stop K+1...)
        if (manifestState.phase2Dropoffs.isEmpty()) {
            item {
                EmptyPhasePlaceholder(message = "No passenger drop-offs registered yet.")
            }
        } else {
            items(manifestState.phase2Dropoffs, key = { it.id }) { waypoint ->
                val isDone = completedStopIds.contains(waypoint.id)
                WaypointCard(
                    waypoint = waypoint,
                    isDone = isDone,
                    onToggleDone = {
                        if (isDone) completedStopIds.remove(waypoint.id) else completedStopIds.add(waypoint.id)
                    },
                    accentColor = MotorwayBlue,
                    badgeBgColor = LightBlueBg
                )
            }
        }

        // Final Destination Terminal
        manifestState.destinationTerminalWaypoint?.let { destTerminal ->
            item {
                DestinationTerminalWaypointCard(waypoint = destTerminal)
            }
        }

        // 5. DRIVER NAVIGATION & SHARE ACTIONS
        item {
            ManifestActionFooter(
                manifestState = manifestState,
                departure = departure,
                context = context
            )
        }
    }
}

@Composable
private fun RouteManifestOverviewCard(
    manifestState: IntercityManifestUiState,
    departure: PlannedDeparture,
    onRecalculate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row with Corridor Name & Live Optimization Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${manifestState.originCity} ➔ ${manifestState.destinationCity}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = manifestState.corridorName,
                            fontSize = 11.sp,
                            color = DarkGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    onClick = onRecalculate,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Re-optimize",
                            tint = DarkGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (manifestState.isOptimizingRoute) "Routing..." else "Optimize",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Stats Metric Bar (Distance, Duration, Seats, Fare)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ManifestMetricItem(
                    label = "DISTANCE",
                    value = "${manifestState.totalDistanceKm} km",
                    icon = Icons.Outlined.Straighten
                )
                ManifestMetricItem(
                    label = "EST. DURATION",
                    value = formatMinutesToHours(manifestState.totalDurationMinutes),
                    icon = Icons.Outlined.Schedule
                )
                ManifestMetricItem(
                    label = "SEATS",
                    value = "${manifestState.totalBookedSeats} Booked",
                    icon = Icons.Outlined.AirlineSeatReclineNormal
                )
                ManifestMetricItem(
                    label = "TOTAL FARE",
                    value = "PKR %,d".format(manifestState.totalManifestEarningsPkr),
                    icon = Icons.Outlined.Payments,
                    valueColor = DarkGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Algorithm Method Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MintGreen,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${manifestState.routingMethod} • 2-Phase Highway Sequence",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ManifestMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun PhaseHeader(
    phaseNumber: Int,
    title: String,
    subtitle: String,
    containerColor: Color,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "P$phaseNumber",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DriverStartWaypointCard(waypoint: IntercityWaypoint) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "DEPARTURE START",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = waypoint.estimatedArrivalText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreen
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = waypoint.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = waypoint.addressDetails,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DestinationTerminalWaypointCard(waypoint: IntercityWaypoint) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MotorwayBlue.copy(alpha = 0.15f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MotorwayBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = LightBlueBg
                ) {
                    Text(
                        text = "FINAL TERMINUS",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotorwayBlue,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = waypoint.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = waypoint.addressDetails,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Clean, modern Waypoint Card with explicit Stop 1, Stop 2... sequence number,
 * customer contact shortcuts, and status toggle.
 */
@Composable
private fun WaypointCard(
    waypoint: IntercityWaypoint,
    isDone: Boolean,
    onToggleDone: () -> Unit,
    accentColor: Color,
    badgeBgColor: Color
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.5.dp),
        border = BorderStroke(
            1.dp,
            if (isDone) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else accentColor.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Stop Number Badge, Passenger Name, Done Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // STOP NUMBER BADGE (e.g. Stop 1, Stop 2)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDone) MaterialTheme.colorScheme.outlineVariant else badgeBgColor,
                        border = BorderStroke(1.dp, if (isDone) Color.Gray else accentColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Stop ${waypoint.stopNumber}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) Color.DarkGray else accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = waypoint.passengerName.ifBlank { waypoint.title },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (waypoint.passengerRating > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "%.1f".format(waypoint.passengerRating),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Done checkmark toggle
                Surface(
                    onClick = onToggleDone,
                    shape = CircleShape,
                    color = if (isDone) MintGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark done",
                            tint = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address & Stop details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (waypoint.phase == IntercityWaypointPhase.PICKUP) Icons.Outlined.PersonPinCircle else Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = waypoint.addressDetails.ifBlank { "Designated Stop Location" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (waypoint.distanceKmFromPrevious > 0) {
                        Text(
                            text = "+${waypoint.distanceKmFromPrevious} km from prior stop",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info: Seats, Fare, and Quick Contact Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${waypoint.seatsCount} Seat(s)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (waypoint.farePkr > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PKR %,d".format(waypoint.farePkr),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                    }
                }

                // Call & Message Shortcuts
                if (waypoint.passengerPhone.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // SMS button
                        Surface(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${waypoint.passengerPhone}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Messaging ${waypoint.passengerName}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Message passenger",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Call button
                        Surface(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${waypoint.passengerPhone}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Calling ${waypoint.passengerName}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = CircleShape,
                            color = MintGreen.copy(alpha = 0.18f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call passenger",
                                    tint = DarkGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighwayCorridorCard(
    manifestState: IntercityManifestUiState,
    departure: PlannedDeparture
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, MotorwayBlue.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            LightBlueBg.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MotorwayBlue,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Commute,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = manifestState.corridorName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotorwayBlue
                    )
                    Text(
                        text = "Express Intercity Motorway Corridor",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Entry & Exit Interchange Gateways
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HIGHWAY ENTRY",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    Text(
                        text = manifestState.highwayEntryHub,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MotorwayBlue,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(horizontal = 2.dp)
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "HIGHWAY EXIT",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotorwayBlue
                    )
                    Text(
                        text = manifestState.highwayExitHub,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPhasePlaceholder(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ManifestActionFooter(
    manifestState: IntercityManifestUiState,
    departure: PlannedDeparture,
    context: Context
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Start Navigation Button
        Button(
            onClick = {
                try {
                    // Open Google Maps / Navigation intent with destination
                    val destLat = manifestState.destinationTerminalWaypoint?.latitude ?: departure.dropoffLat
                    val destLon = manifestState.destinationTerminalWaypoint?.longitude ?: departure.dropoffLon
                    val navUri = Uri.parse("google.navigation:q=$destLat,$destLon&mode=d")
                    val mapIntent = Intent(Intent.ACTION_VIEW, navUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$destLat,$destLon?q=$destLat,$destLon(${departure.dropoffCity})"))
                        context.startActivity(genericIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Opening GPS Navigation for ${departure.dropoffCity}", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Optimized GPS Navigation",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Share Manifest Button
        OutlinedButton(
            onClick = {
                try {
                    val shareText = buildString {
                        appendLine("🚗 DRIGO INTERCITY TRIP MANIFEST")
                        appendLine("Route: ${manifestState.originCity} ➔ ${manifestState.destinationCity}")
                        appendLine("Corridor: ${manifestState.corridorName}")
                        appendLine("Total Distance: ${manifestState.totalDistanceKm} km (${formatMinutesToHours(manifestState.totalDurationMinutes)})")
                        appendLine()
                        appendLine("📍 PHASE 1 PICKUPS (Heading to ${manifestState.highwayEntryHub}):")
                        manifestState.phase1Pickups.forEach {
                            appendLine("  Stop ${it.stopNumber}: ${it.passengerName} (${it.seatsCount} seat) - ${it.addressDetails}")
                        }
                        appendLine()
                        appendLine("🛣️ MOTORWAY TRANSIT: ${manifestState.highwayEntryHub} ➔ ${manifestState.highwayExitHub}")
                        appendLine()
                        appendLine("📍 PHASE 2 DROP-OFFS (From ${manifestState.highwayExitHub}):")
                        manifestState.phase2Dropoffs.forEach {
                            appendLine("  Stop ${it.stopNumber}: ${it.passengerName} - ${it.addressDetails}")
                        }
                        appendLine()
                        appendLine("Total Confirmed Seats: ${manifestState.totalBookedSeats} | Total Fare: PKR ${manifestState.totalManifestEarningsPkr}")
                    }

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Trip Manifest"))
                } catch (_: Exception) {
                    Toast.makeText(context, "Manifest summary ready", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Share Route Manifest",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatMinutesToHours(mins: Int): String {
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) "${h}h ${m}m" else "${m} mins"
}

/**
 * Production-grade Material 3 Passenger Offer Card for City to City manifest and departures.
 * Supports consistent spacing and layout regardless of seat count (1 to 4+ seats or full car buyout).
 * Implements prominent, unambiguous visual states for 'Accept', 'Reject', and 'Counter' actions.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val isAccepted = offer.status.equals("ACCEPTED", ignoreCase = true)
    val isCountered = offer.status.equals("COUNTERED", ignoreCase = true)
    val isDeclined = offer.status.equals("DECLINED", ignoreCase = true) || offer.status.equals("REJECTED", ignoreCase = true)
    val isMultiSeat = offer.requestedSeats > 1

    val statusDotColor = when {
        isAccepted -> MintGreen
        isCountered -> AmberTag
        isDeclined -> DarkRed
        offer.differencePkr < 0 -> Color(0xFFFB8C00)
        else -> MintGreen
    }

    val cardBorderColor = when {
        isAccepted -> MintGreen.copy(alpha = 0.6f)
        isCountered -> AmberTag.copy(alpha = 0.6f)
        isDeclined -> DarkRed.copy(alpha = 0.35f)
        offer.isFullFare -> MintGreen.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    val cardContainerColor = MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp, pressedElevation = 3.dp),
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
            // SECTION 1: HEADER STRIP (SEATS & BOOKING TYPE + STATUS BADGE)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Seat Count & Booking Type
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

                // Right: Clear Status Pill with matching dot
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
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(tagStyle.text)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tagStyle.label,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tagStyle.text,
                            maxLines = 1,
                            softWrap = false
                        )
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
                    // Passenger Initials Circle Avatar
                    Surface(
                        shape = CircleShape,
                        color = DrigoBrandPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, DrigoBrandPurple.copy(alpha = 0.3f)),
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
                                text = "%.1f".format(offer.passengerRating),
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

                // Quick Contact Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Passenger",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onMessage,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Message Passenger",
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
            // SECTION 3: ROUTE TIMELINE (PICKUP & DROPOFF)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pickup Point Row
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

                // Drop-off Point Row
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
            // SECTION 4 & 5: FARE & ACTIONS (ACCEPTED vs NEGOTIATING)
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

