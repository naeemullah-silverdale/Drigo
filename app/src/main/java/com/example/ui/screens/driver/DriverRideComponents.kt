package com.example.ui.screens.driver

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
fun DriverRideRequestCard(
    request: RideRequest,
    isSelected: Boolean,
    isOfferSent: Boolean,
    driverLat: Double = 0.0,
    driverLon: Double = 0.0,
    onSelect: () -> Unit,
    onAcceptOffer: () -> Unit,
    onCounterOffer: (Int) -> Unit
) {
    var expandedBidding by remember { mutableStateOf(false) }
    var customBidText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

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
        color = if (isSelected) Color(0xFF222634) else Color(0xFF191B24),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) DrigoBrandPurple else Color(0xFF2D3244)
        ),
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
                        color = DrigoBrandPurple.copy(alpha = 0.25f),
                        border = BorderStroke(1.5.dp, DrigoBrandPurple),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = request.passengerName.take(1).uppercase().ifBlank { "P" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = request.passengerName.ifBlank { "Passenger" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2A281E),
                                border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "4.9",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = Color(0xFF81D4FA),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = request.rideCategory.ifBlank { "Ride" },
                                fontSize = 11.5.sp,
                                color = Color(0xFF81D4FA),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (distAwayKm > 0.0) {
                                Text(
                                    text = " • %.1f km away".format(Locale.US, distAwayKm),
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                            }
                        }
                    }
                }

                // Fare Display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B2C24),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "PKR $formattedFare",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFF00E676)
                        )
                        Text(
                            text = request.paymentMethod.ifBlank { "Cash" },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB9F6CA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF262C3E))
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
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(Color(0xFF37474F))
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF5350),
                        modifier = Modifier.size(8.dp)
                    ) {}
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.pickupTitle.ifBlank { "Pickup Location" },
                        fontSize = 12.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = request.destinationTitle.ifBlank { "Destination Location" },
                        fontSize = 12.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Accept Or Expand Counter Bidding
            if (isOfferSent) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DrigoBrandPurple.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, DrigoBrandPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = DrigoBrandPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offer Sent • Waiting for passenger...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Direct Accept Fare Button
                    Button(
                        onClick = onAcceptOffer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("driver_accept_fare_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accept PKR $formattedFare",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }

                    // Raise / Counter Bidding Toggle Button
                    OutlinedButton(
                        onClick = { expandedBidding = !expandedBidding },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DrigoBrandPurple),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (expandedBidding) DrigoBrandPurple.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (expandedBidding) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = DrigoBrandPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expandedBidding) "Close" else "Offer Higher",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = DrigoBrandPurple
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
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
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
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF282138),
                                    border = BorderStroke(1.dp, DrigoBrandPurple),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = "+$increment",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFFD54F)
                                        )
                                        Text(
                                            text = "$counterFare",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
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
                                placeholder = { Text("Custom PKR...", fontSize = 11.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Text(
                                        "PKR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF13151D),
                                    unfocusedContainerColor = Color(0xFF13151D),
                                    focusedBorderColor = DrigoBrandPurple,
                                    unfocusedBorderColor = Color(0xFF2E3547),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
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
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Send", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    onSelect: () -> Unit,
    onAcceptOffer: () -> Unit,
    onCounterOffer: (Int) -> Unit
) {
    var expandedBidding by remember { mutableStateOf(false) }

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
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF1E2230) else Color(0xFF161822),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) DrigoBrandPurple else Color(0xFF2A2E3E)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_compact_ride_card_${request.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
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
                        tint = Color(0xFF81D4FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = request.passengerName.ifBlank { "Passenger" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (distAwayKm > 0.0) {
                        Text(
                            text = " • %.1f km".format(Locale.US, distAwayKm),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    text = "PKR $formattedFare",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color(0xFF00E676)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${request.pickupTitle} ➔ ${request.destinationTitle}",
                fontSize = 11.sp,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isOfferSent) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DrigoBrandPurple.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Offer Sent • Pending...",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onAcceptOffer,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Accept PKR $formattedFare", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = { expandedBidding = !expandedBidding },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DrigoBrandPurple),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text(if (expandedBidding) "Close" else "Raise", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF282138),
                                    border = BorderStroke(1.dp, DrigoBrandPurple),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "+${fare - base}\nPKR $fare",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
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
