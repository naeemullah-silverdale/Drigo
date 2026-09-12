package com.example.ui.screens.driver

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.RideRequest
import com.example.ui.theme.DrigoBrandPurple
import java.text.NumberFormat
import java.util.Locale

enum class DriverContrastTheme {
    NORMAL,
    HIGH_CONTRAST_DARK,
    HIGH_CONTRAST_LIGHT
}

internal fun calculateDistanceKmHelper(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    if (lat1 == 0.0 || lon1 == 0.0 || lat2 == 0.0 || lon2 == 0.0) return 0.0
    val results = FloatArray(1)
    return try {
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        results[0] / 1000.0
    } catch (_: Exception) {
        0.0
    }
}

@Composable
fun PassengerRequestItemCard(
    request: RideRequest,
    driverLat: Double = 0.0,
    driverLon: Double = 0.0,
    contrastTheme: DriverContrastTheme = DriverContrastTheme.NORMAL,
    onSelect: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val formattedFare = NumberFormat.getNumberInstance(Locale.US).format(request.estimatedFare)
    val distKmToPickup = if (driverLat != 0.0 && driverLon != 0.0 && request.pickupLat != 0.0) {
        calculateDistanceKmHelper(driverLat, driverLon, request.pickupLat, request.pickupLon).coerceAtLeast(0.5)
    } else 0.8
    val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

    val passengerInitial = request.passengerName.trim().take(1).lowercase().ifBlank { "j" }
    val passengerName = request.passengerName.ifBlank { "jawad" }
    val ratingText = String.format(Locale.US, "%.2f", if (request.passengerRating > 0.0) request.passengerRating else 4.73)
    val totalRidesCount = if (request.passengerId.isNotBlank()) (Math.abs(request.passengerId.hashCode() % 80) + 12) else 44

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(MaterialTheme.colorScheme.surface)
            .testTag("passenger_request_item_${request.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: Avatar + Name + Rating + ETA
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(68.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF5E35B1), // Purple circle avatar from screenshot
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = passengerInitial,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = passengerName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = ratingText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "($totalRidesCount)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$etaMins min.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Center Column: Distance, Fare + Fair Price, Pickup, Destination, Category Pill
            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Distance & 3-dot overflow menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "~%.1f km", request.distanceKm).replace(".", ","),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Route Details", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    onSelect()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Skip / Ignore Request", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }

                // Fare line + Fair Price badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "PKR$formattedFare",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFCE93D8).copy(alpha = 0.2f),
                            modifier = Modifier.size(15.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = Color(0xFFCE93D8),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = "Fair price",
                            color = Color(0xFFCE93D8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Pickup location (bold)
                val pickupDisplay = buildString {
                    append(request.pickupTitle.ifBlank { "Pickup location" })
                    if (request.pickupSubtitle.isNotBlank() && !request.pickupTitle.contains(request.pickupSubtitle)) {
                        append(" (${request.pickupSubtitle})")
                    }
                }
                Text(
                    text = pickupDisplay,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Destination location
                val destDisplay = buildString {
                    append(request.destinationTitle.ifBlank { "Destination" })
                    if (request.destinationSubtitle.isNotBlank()) {
                        append(" (${request.destinationSubtitle})")
                    }
                }
                Text(
                    text = destDisplay,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category pill (e.g. Mini)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF81D4FA).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color(0xFF81D4FA).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = request.rideCategory.ifBlank { "Mini" },
                        color = Color(0xFF81D4FA),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Subtle divider between list items matching screenshot
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            thickness = 1.dp
        )
    }
}

@Composable
fun DriverRideRequestCard(
    request: RideRequest,
    isSelected: Boolean,
    isOfferSent: Boolean,
    driverLat: Double = 0.0,
    driverLon: Double = 0.0,
    contrastTheme: DriverContrastTheme = DriverContrastTheme.NORMAL,
    onSelect: () -> Unit,
    onAcceptOffer: () -> Unit,
    onCounterOffer: (Int) -> Unit
) {
    var expandedBidding by remember { mutableStateOf(false) }
    var customBidText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val isHighContrastDark = contrastTheme == DriverContrastTheme.HIGH_CONTRAST_DARK
    val isHighContrastLight = contrastTheme == DriverContrastTheme.HIGH_CONTRAST_LIGHT
    val isHighContrast = isHighContrastDark || isHighContrastLight

    val cardBg = when {
        isHighContrastLight -> Color(0xFFFFFFFF)
        isHighContrastDark -> Color(0xFF000000)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    val cardBorder = when {
        isHighContrastLight -> BorderStroke(if (isSelected) 3.dp else 2.dp, Color(0xFF111827))
        isHighContrastDark -> BorderStroke(if (isSelected) 3.dp else 2.dp, Color(0xFF00E676))
        isSelected -> BorderStroke(2.dp, DrigoBrandPurple)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val primaryTextColor = when {
        isHighContrastLight -> Color(0xFF111827)
        isHighContrastDark -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val secondaryTextColor = when {
        isHighContrastLight -> Color(0xFF374151)
        isHighContrastDark -> Color(0xFFE0E0E0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val categoryIcon = when {
        request.rideCategory.contains("Share", ignoreCase = true) -> Icons.Default.Groups
        request.rideCategory.contains("Private", ignoreCase = true) -> Icons.Default.DirectionsCar
        request.rideCategory.contains("Bike", ignoreCase = true) -> Icons.Default.TwoWheeler
        request.rideCategory.contains("Mini", ignoreCase = true) -> Icons.Default.DirectionsCar
        request.rideCategory.contains("A/C", ignoreCase = true) || request.rideCategory.contains("AC", ignoreCase = true) -> Icons.Default.AcUnit
        request.rideCategory.contains("Courier", ignoreCase = true) || request.rideCategory.contains("Parcel", ignoreCase = true) -> Icons.Default.LocalShipping
        request.rideCategory.contains("City", ignoreCase = true) -> Icons.Default.AltRoute
        else -> Icons.Default.LocalTaxi
    }

    val distAwayKm = if (driverLat != 0.0 && driverLon != 0.0 && request.pickupLat != 0.0) {
        calculateDistanceKmHelper(driverLat, driverLon, request.pickupLat, request.pickupLon)
    } else 0.0

    val formattedFare = NumberFormat.getNumberInstance(Locale.US).format(request.estimatedFare)

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = cardBorder,
        shadowElevation = if (isSelected) 8.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_ride_card_${request.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Passenger Profile & Rating + Offered Fare Container
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Passenger Avatar & Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isHighContrastLight -> Color(0xFF111827)
                            isHighContrastDark -> Color(0xFF00E676)
                            else -> DrigoBrandPurple.copy(alpha = 0.25f)
                        },
                        border = BorderStroke(1.5.dp, if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else DrigoBrandPurple),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.passengerName.take(1).uppercase().ifBlank { "P" },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (isHighContrastDark) Color(0xFF000000) else Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = request.passengerName.ifBlank { "Passenger" },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = primaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isHighContrastLight) Color(0xFFFEF3C7) else Color(0xFF2A281E),
                                border = BorderStroke(1.dp, if (isHighContrastLight) Color(0xFFD97706) else Color(0xFFFFD54F).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isHighContrastLight) Color(0xFFB45309) else Color(0xFFFFD54F),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "4.9",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isHighContrastLight) Color(0xFF92400E) else Color(0xFFFFD54F)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = if (isHighContrastLight) Color(0xFF0284C7) else Color(0xFF81D4FA),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = request.rideCategory.ifBlank { "Ride" },
                                fontSize = 12.sp,
                                color = if (isHighContrastLight) Color(0xFF0369A1) else Color(0xFF81D4FA),
                                fontWeight = FontWeight.Bold
                            )
                            if (distAwayKm > 0.0) {
                                Text(
                                    text = " • %.1f km away".format(Locale.US, distAwayKm),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }
                }

                // Fare Display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isHighContrastLight -> Color(0xFFDCFCE7)
                        isHighContrastDark -> Color(0xFF000000)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        if (isHighContrast) 2.dp else 1.dp,
                        if (isHighContrastLight) Color(0xFF15803D) else Color(0xFF00E676)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "PKR $formattedFare",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = if (isHighContrastLight) Color(0xFF14532D) else Color(0xFF00E676)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (request.paymentMethod.contains("Wallet", ignoreCase = true)) Icons.Default.AccountBalanceWallet else Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isHighContrastLight) Color(0xFF15803D) else Color(0xFFB9F6CA),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = request.paymentMethod.ifBlank { "Cash" },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHighContrastLight) Color(0xFF15803D) else Color(0xFFB9F6CA)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isHighContrastLight) Color(0xFFE5E7EB) else if (isHighContrastDark) Color(0xFF333333) else MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Route Visualization (Pickup & Destination)
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00E676),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(28.dp)
                            .background(if (isHighContrastLight) Color(0xFF9CA3AF) else Color(0xFF546E7A))
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF5350),
                        modifier = Modifier.size(10.dp)
                    ) {}
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.pickupTitle.ifBlank { "Pickup Location" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = request.destinationTitle.ifBlank { "Destination Location" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Oversized Action Buttons: Accept Or Expand Counter Bidding
            if (isOfferSent) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isHighContrastLight) Color(0xFFEEF2FF) else DrigoBrandPurple.copy(alpha = 0.25f),
                    border = BorderStroke(2.dp, if (isHighContrastLight) Color(0xFF4F46E5) else DrigoBrandPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = if (isHighContrastLight) Color(0xFF4F46E5) else DrigoBrandPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Offer Sent • Waiting for passenger...",
                            color = if (isHighContrastLight) Color(0xFF1E1B4B) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Oversized Direct Accept Fare Button (58dp height, 17sp bold text)
                    Button(
                        onClick = onAcceptOffer,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHighContrastDark) Color(0xFF00E676) else Color(0xFF00C853)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(58.dp)
                            .testTag("driver_accept_fare_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isHighContrastDark) Color(0xFF000000) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accept PKR $formattedFare",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = if (isHighContrastDark) Color(0xFF000000) else Color.White
                        )
                    }

                    // Oversized Raise / Counter Bidding Toggle Button (58dp height)
                    OutlinedButton(
                        onClick = { expandedBidding = !expandedBidding },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            2.dp,
                            if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else DrigoBrandPurple
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (expandedBidding) {
                                if (isHighContrastLight) Color(0xFFF3F4F6) else DrigoBrandPurple.copy(alpha = 0.25f)
                            } else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(58.dp)
                    ) {
                        Icon(
                            imageVector = if (expandedBidding) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else DrigoBrandPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expandedBidding) "Close" else "Offer Higher",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else DrigoBrandPurple
                        )
                    }
                }

                // In-Drive Fast Counter Bidding Chips & Custom Input
                AnimatedVisibility(
                    visible = expandedBidding,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = "Quick Counter Offer:",
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val baseFare = request.estimatedFare
                            val increments = listOf(50, 100, 150, 200)
                            increments.forEach { increment ->
                                val counterFare = baseFare + increment
                                Surface(
                                    onClick = {
                                        onCounterOffer(counterFare)
                                        expandedBidding = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isHighContrastLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else DrigoBrandPurple
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = "+$increment",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isHighContrastLight) Color(0xFFB45309) else Color(0xFFFFD54F)
                                        )
                                        Text(
                                            text = "$counterFare",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryTextColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Counter Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customBidText,
                                onValueChange = { if (it.all { char -> char.isDigit() }) customBidText = it },
                                placeholder = { Text("Custom PKR...", fontSize = 13.sp, color = secondaryTextColor) },
                                leadingIcon = {
                                    Text(
                                        "PKR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryTextColor,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = if (isHighContrastLight) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = if (isHighContrastLight) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = if (isHighContrastLight) Color(0xFF111827) else DrigoBrandPurple,
                                    unfocusedBorderColor = if (isHighContrastLight) Color(0xFF9CA3AF) else MaterialTheme.colorScheme.outlineVariant,
                                    focusedTextColor = primaryTextColor,
                                    unfocusedTextColor = primaryTextColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            )

                            Button(
                                onClick = {
                                    val customFare = customBidText.toIntOrNull()
                                    if (customFare != null && customFare > 0) {
                                        onCounterOffer(customFare)
                                        expandedBidding = false
                                        customBidText = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else DrigoBrandPurple
                                ),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(
                                    "Send",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isHighContrastDark) Color(0xFF000000) else Color.White
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
fun DriverCompactRideRequestItem(
    request: RideRequest,
    isSelected: Boolean,
    isOfferSent: Boolean,
    driverLat: Double = 0.0,
    driverLon: Double = 0.0,
    contrastTheme: DriverContrastTheme = DriverContrastTheme.NORMAL,
    onSelect: () -> Unit,
    onAcceptOffer: () -> Unit,
    onCounterOffer: (Int) -> Unit
) {
    var expandedBidding by remember { mutableStateOf(false) }

    val isHighContrastDark = contrastTheme == DriverContrastTheme.HIGH_CONTRAST_DARK
    val isHighContrastLight = contrastTheme == DriverContrastTheme.HIGH_CONTRAST_LIGHT
    val isHighContrast = isHighContrastDark || isHighContrastLight

    val cardBg = when {
        isHighContrastLight -> Color(0xFFFFFFFF)
        isHighContrastDark -> Color(0xFF000000)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    val cardBorder = when {
        isHighContrastLight -> BorderStroke(if (isSelected) 2.5.dp else 1.5.dp, Color(0xFF111827))
        isHighContrastDark -> BorderStroke(if (isSelected) 2.5.dp else 1.5.dp, Color(0xFF00E676))
        isSelected -> BorderStroke(1.5.dp, DrigoBrandPurple)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val primaryTextColor = when {
        isHighContrastLight -> Color(0xFF111827)
        isHighContrastDark -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val secondaryTextColor = when {
        isHighContrastLight -> Color(0xFF374151)
        isHighContrastDark -> Color(0xFFE0E0E0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val categoryIcon = when {
        request.rideCategory.contains("Share", ignoreCase = true) -> Icons.Default.Groups
        request.rideCategory.contains("Private", ignoreCase = true) -> Icons.Default.DirectionsCar
        request.rideCategory.contains("Bike", ignoreCase = true) -> Icons.Default.TwoWheeler
        request.rideCategory.contains("Mini", ignoreCase = true) -> Icons.Default.DirectionsCar
        request.rideCategory.contains("A/C", ignoreCase = true) || request.rideCategory.contains("AC", ignoreCase = true) -> Icons.Default.AcUnit
        request.rideCategory.contains("Courier", ignoreCase = true) || request.rideCategory.contains("Parcel", ignoreCase = true) -> Icons.Default.LocalShipping
        request.rideCategory.contains("City", ignoreCase = true) -> Icons.Default.AltRoute
        else -> Icons.Default.LocalTaxi
    }

    val distAwayKm = if (driverLat != 0.0 && driverLon != 0.0 && request.pickupLat != 0.0) {
        calculateDistanceKmHelper(driverLat, driverLon, request.pickupLat, request.pickupLon)
    } else 0.0

    val formattedFare = NumberFormat.getNumberInstance(Locale.US).format(request.estimatedFare)

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_compact_ride_card_${request.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = if (isHighContrastLight) Color(0xFF0284C7) else Color(0xFF81D4FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = request.passengerName.ifBlank { "Passenger" },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (distAwayKm > 0.0) {
                        Text(
                            text = " • %.1f km".format(Locale.US, distAwayKm),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryTextColor
                        )
                    }
                }

                Text(
                    text = "PKR $formattedFare",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (isHighContrastLight) Color(0xFF15803D) else Color(0xFF00E676)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${request.pickupTitle} ➔ ${request.destinationTitle}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isOfferSent) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isHighContrastLight) Color(0xFFEEF2FF) else DrigoBrandPurple.copy(alpha = 0.25f),
                    border = BorderStroke(1.5.dp, if (isHighContrastLight) Color(0xFF4F46E5) else DrigoBrandPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Offer Sent • Pending...",
                            color = if (isHighContrastLight) Color(0xFF1E1B4B) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Oversized Primary Accept Button
                    Button(
                        onClick = onAcceptOffer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHighContrastDark) Color(0xFF00E676) else Color(0xFF00C853)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(56.dp)
                    ) {
                        Text(
                            "Accept PKR $formattedFare",
                            color = if (isHighContrastDark) Color(0xFF000000) else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { expandedBidding = !expandedBidding },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else DrigoBrandPurple
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(0.7f)
                            .height(56.dp)
                    ) {
                        Text(
                            if (expandedBidding) "Close" else "Raise",
                            color = if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }

                AnimatedVisibility(
                    visible = expandedBidding,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val base = request.estimatedFare
                            listOf(base + 50, base + 100, base + 150).forEach { fare ->
                                Surface(
                                    onClick = {
                                        onCounterOffer(fare)
                                        expandedBidding = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isHighContrastLight) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isHighContrastLight) Color(0xFF111827) else if (isHighContrastDark) Color(0xFF00E676) else DrigoBrandPurple
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "+${fare - base}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isHighContrastLight) Color(0xFFB45309) else Color(0xFFFFD54F)
                                        )
                                        Text(
                                            text = "$fare",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryTextColor
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
}
