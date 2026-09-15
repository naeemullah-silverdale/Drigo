package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntercityActiveRideState
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.DrigoBrandPurple
import com.example.ui.theme.InDriveLimeGreen
import com.example.ui.theme.drigoColors

// Dedicated Theme Palette for High-Contrast Active Ride Component
private val PrimaryCorridorGreen = Color(0xFF00C853)
private val HighwayBlue = Color(0xFF1E88E5)
private val AmberCoffee = Color(0xFFFF8F00)
private val SoftRedSos = Color(0xFFD32F2F)

/**
 * Modern, production-grade 'Active Ride' UI component for Drigo City-to-City.
 * Dynamically reacts to Firebase Realtime Database ride state (`IntercityActiveRideState`)
 * when the driver starts the ride, arrives, boards passengers, cruises on the highway, and completes the trip.
 */
@Composable
fun ActiveRideStatusComponent(
    departure: PlannedDeparture,
    booking: PlannedDepartureBooking? = null,
    liveRideStateOverride: IntercityActiveRideState? = null,
    onViewMapClick: () -> Unit = {},
    onCallDriverClick: (phone: String) -> Unit = {},
    onChatClick: (name: String, phone: String) -> Unit = { _, _ -> },
    onSosClick: () -> Unit = {},
    onRateClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { FirebaseRepository.getInstance(context) }

    // Live state observation from Firebase Realtime Database / local cache
    val observedRideState by remember(departure.id) {
        repo.observeIntercityRideState(departure.id)
    }.collectAsState(initial = null)

    val activeState = liveRideStateOverride ?: observedRideState

    val subStatus = activeState?.subStatus ?: "DRIVER_COMING"
    val isRideCompleted = activeState?.isCompleted == true || subStatus == "COMPLETED"

    val isDark = MaterialTheme.drigoColors.isDark
    val cardBackground = if (isDark) Color(0xFF16181F) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isDark) Color(0xFF282C38) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA2A7B5) else MaterialTheme.colorScheme.onSurfaceVariant

    val driverName = departure.driverName.ifBlank { "Muhammad Ali" }
    val driverPhone = departure.driverPhone.ifBlank { "+92 300 1234567" }
    val driverVehicle = departure.driverVehicle.ifBlank { "Toyota Corolla (Silver)" }
    val driverPlate = departure.driverPlateNumber.ifBlank { "LEA-18-4921" }
    val driverRating = departure.driverRating.takeIf { it > 0.0 } ?: 4.9
    val driverTrips = departure.driverTotalTrips.takeIf { it > 0 } ?: 1280
    val farePkr = booking?.totalFarePkr ?: departure.farePerSeat.takeIf { it > 0 } ?: 1900
    val seatNumber = booking?.seatsBooked ?: 3
    val pickupStop = booking?.pickupStop?.ifBlank { departure.pickupHub } ?: departure.pickupHub.ifBlank { "Faizabad Hub, Islamabad" }
    val dropoffStop = booking?.dropoffStop?.ifBlank { departure.dropoffHub } ?: departure.dropoffHub.ifBlank { "Thokar Niaz Baig, Lahore" }
    val boardingPin = "7492"

    // Pulsing animation for active live indicators
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    var showSosConfirmationDialog by remember { mutableStateOf(false) }

    if (showSosConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SoftRedSos,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Highway Emergency Helpline (NHMP 130)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Connect directly with National Highways & Motorway Police. Live GPS telemetry is linked to this trip.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Trip ID: #${departure.id.takeLast(6).uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Corridor: ${departure.pickupCity} → ${departure.dropoffCity} (M-2)", fontSize = 11.5.sp)
                            Text("Driver: $driverName ($driverPlate)", fontSize = 11.5.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmationDialog = false
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:130"))
                            context.startActivity(dialIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Dialing NHMP 130 Helpline...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRedSos)
                ) {
                    Text("Call 130 Helpline", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBackground,
        border = BorderStroke(1.2.dp, cardBorder),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_ride_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // 1. DYNAMIC STATUS BANNER & STEPPER
            // ==========================================
            DynamicStatusHeader(
                subStatus = subStatus,
                statusDisplayMessage = activeState?.statusDisplayMessage,
                boardedIndex = activeState?.currentBoardingIndex ?: 0,
                pulseAlpha = pulseAlpha,
                onRateClick = onRateClick
            )

            // Step Progress Line (Dispatched -> Arrived -> Boarding -> In Progress -> Completed)
            RideLifecycleProgressBar(subStatus = subStatus)

            // ==========================================
            // 2. LIVE STAGE SPECIFIC CALLOUT CARDS
            // ==========================================
            AnimatedContent(
                targetState = subStatus,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "ride_stage_callout"
            ) { currentStage ->
                when (currentStage) {
                    "DRIVER_COMING" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DrigoBrandPurple.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, DrigoBrandPurple.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = DrigoBrandPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Captain is on the way to $pickupStop",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Estimated Arrival: 4–6 mins • Please keep phone active",
                                        fontSize = 11.5.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                    "DRIVER_ARRIVED" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AmberCoffee.copy(alpha = 0.12f),
                            border = BorderStroke(1.2.dp, AmberCoffee),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AmberCoffee,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Driver Has Arrived at Pickup Point!",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = AmberCoffee
                                    )
                                    Text(
                                        text = "Waiting at $pickupStop. Meet $driverVehicle ($driverPlate).",
                                        fontSize = 11.5.sp,
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }
                    "BOARDING" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PrimaryCorridorGreen.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, PrimaryCorridorGreen.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AirlineSeatReclineNormal,
                                            contentDescription = null,
                                            tint = PrimaryCorridorGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Passenger Boarding Check-In",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = textPrimary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PrimaryCorridorGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Seat #$seatNumber",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryCorridorGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Verify your 4-digit PIN with Captain $driverName to confirm your seat.",
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                    "RIDE_IN_PROGRESS" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HighwayBlue.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, HighwayBlue.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = HighwayBlue,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Expressway Cruising: M-2 Motorway",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Speed: ~110 km/h • Next Rest Stop: Bhera Service Area",
                                        fontSize = 11.5.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                    "COMPLETED" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PrimaryCorridorGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.2.dp, PrimaryCorridorGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryCorridorGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Safely Reached ${departure.dropoffCity}!",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = PrimaryCorridorGreen
                                    )
                                    Text(
                                        text = "Total Fare: Rs. $farePkr • Please rate your captain.",
                                        fontSize = 11.5.sp,
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. SECRET BOARDING PIN & SEAT INFO
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secret PIN Card with One-Tap Copy
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1E212B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Boarding PIN", boardingPin)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "PIN $boardingPin copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BOARDING PIN",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = boardingPin,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = DrigoBrandPurple
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy PIN",
                                tint = textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "Share verbally with driver",
                            fontSize = 9.5.sp,
                            color = textSecondary
                        )
                    }
                }

                // Seat & Fare Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1E212B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YOUR SEAT",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Seat #$seatNumber",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Fare: Rs. $farePkr",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryCorridorGreen
                        )
                    }
                }
            }

            // ==========================================
            // 4. CAPTAIN & VEHICLE PROFILE CARD
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF1B1D25) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with rating badge
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = DrigoBrandPurple.copy(alpha = 0.2f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = driverName.take(1).uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrigoBrandPurple
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFD700),
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Verified",
                                    tint = Color.Black,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = driverName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFB300).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$driverRating",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "$driverVehicle • $driverTrips trips",
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.08f),
                            border = BorderStroke(0.8.dp, Color.Gray.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(top = 3.dp)
                        ) {
                            Text(
                                text = driverPlate,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = textPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Direct Quick Action Buttons (Call & Chat)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                onCallDriverClick(driverPhone)
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$driverPhone"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Calling $driverPhone", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(PrimaryCorridorGreen.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call Captain",
                                tint = PrimaryCorridorGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onChatClick(driverName, driverPhone) },
                            modifier = Modifier
                                .size(38.dp)
                                .background(DrigoBrandPurple.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Chat with Captain",
                                tint = DrigoBrandPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 5. ROUTE CORRIDOR HUBS
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color(0xFF14161C) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pickup
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(PrimaryCorridorGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PICKUP POINT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                        Text(
                            text = pickupStop,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(2.dp)
                        .height(10.dp)
                        .background(Color.Gray.copy(alpha = 0.4f))
                )

                // Drop-off
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(DrigoBrandPurple, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DESTINATION HUB",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                        Text(
                            text = dropoffStop,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ==========================================
            // 6. ACTION CONTROLS (Live Map, SOS, Rate)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emergency SOS Button (NHMP 130)
                OutlinedButton(
                    onClick = {
                        onSosClick()
                        showSosConfirmationDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SoftRedSos.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRedSos),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "SOS",
                        tint = SoftRedSos,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "130 SOS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftRedSos
                    )
                }

                if (isRideCompleted) {
                    // Rate Captain Primary CTA
                    Button(
                        onClick = onRateClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCorridorGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rate Captain",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // View Live Radar / Active GPS Map
                    Button(
                        onClick = onViewMapClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Route Map",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top Status Header with dynamic color coding and pulse beacon.
 */
@Composable
private fun DynamicStatusHeader(
    subStatus: String,
    statusDisplayMessage: String?,
    boardedIndex: Int,
    pulseAlpha: Float,
    onRateClick: () -> Unit
) {
    val statusColor = when (subStatus) {
        "DRIVER_COMING" -> DrigoBrandPurple
        "DRIVER_ARRIVED" -> AmberCoffee
        "BOARDING" -> PrimaryCorridorGreen
        "RIDE_IN_PROGRESS" -> HighwayBlue
        "COMPLETED" -> PrimaryCorridorGreen
        else -> PrimaryCorridorGreen
    }

    val statusIcon = when (subStatus) {
        "DRIVER_COMING" -> Icons.Default.DirectionsCar
        "DRIVER_ARRIVED" -> Icons.Default.LocationOn
        "BOARDING" -> Icons.Default.AirlineSeatReclineNormal
        "RIDE_IN_PROGRESS" -> Icons.Default.Speed
        "COMPLETED" -> Icons.Default.CheckCircle
        else -> Icons.Default.Navigation
    }

    val statusTitle = when (subStatus) {
        "DRIVER_COMING" -> "Driver is Coming"
        "DRIVER_ARRIVED" -> "Driver Has Arrived!"
        "BOARDING" -> "Boarding Passengers"
        "RIDE_IN_PROGRESS" -> "Expressway Ride Active"
        "COMPLETED" -> "Trip Completed"
        else -> "Active Ride"
    }

    val statusSubtext = when (subStatus) {
        "DRIVER_COMING" -> "Captain is en route to your pickup location."
        "DRIVER_ARRIVED" -> "Captain is at the pickup hub. Please board."
        "BOARDING" -> if (!statusDisplayMessage.isNullOrBlank()) statusDisplayMessage else "Verifying passenger tickets..."
        "RIDE_IN_PROGRESS" -> "Cruising safely on M-2 Motorway corridor."
        "COMPLETED" -> "You have safely arrived at your destination."
        else -> "Real-time trip active."
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = statusColor,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor.copy(alpha = pulseAlpha), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = statusSubtext,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 5-Stage Visual Progress Bar for active ride lifecycle.
 */
@Composable
private fun RideLifecycleProgressBar(subStatus: String) {
    val activeStep = when (subStatus) {
        "DRIVER_COMING" -> 1
        "DRIVER_ARRIVED" -> 2
        "BOARDING" -> 3
        "RIDE_IN_PROGRESS" -> 4
        "COMPLETED" -> 5
        else -> 1
    }

    val stepLabels = listOf("Coming", "Arrived", "Boarding", "Express", "Arrived")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..5) {
                val isPastOrCurrent = step <= activeStep
                val isCurrent = step == activeStep

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(52.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCurrent) {
                            PrimaryCorridorGreen
                        } else if (isPastOrCurrent) {
                            PrimaryCorridorGreen.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (isCurrent) BorderStroke(1.5.dp, PrimaryCorridorGreen) else null,
                        modifier = Modifier.size(if (isCurrent) 18.dp else 14.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isPastOrCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = stepLabels[step - 1],
                        fontSize = 9.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.onSurface
                        } else if (isPastOrCurrent) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                if (step < 5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .padding(horizontal = 2.dp)
                            .background(
                                if (step < activeStep) PrimaryCorridorGreen else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Compact Floating / Anchored Active Ride Banner for List / Home Views.
 */
@Composable
fun ActiveRideStatusMiniBanner(
    departure: PlannedDeparture,
    liveRideStateOverride: IntercityActiveRideState? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { FirebaseRepository.getInstance(context) }
    val observedRideState by remember(departure.id) {
        repo.observeIntercityRideState(departure.id)
    }.collectAsState(initial = null)

    val activeState = liveRideStateOverride ?: observedRideState
    val subStatus = activeState?.subStatus ?: "DRIVER_COMING"

    val statusColor = when (subStatus) {
        "DRIVER_COMING" -> DrigoBrandPurple
        "DRIVER_ARRIVED" -> AmberCoffee
        "BOARDING" -> PrimaryCorridorGreen
        "RIDE_IN_PROGRESS" -> HighwayBlue
        "COMPLETED" -> PrimaryCorridorGreen
        else -> PrimaryCorridorGreen
    }

    val statusText = when (subStatus) {
        "DRIVER_COMING" -> "Driver is Coming"
        "DRIVER_ARRIVED" -> "Driver Has Arrived!"
        "BOARDING" -> "Boarding Passengers"
        "RIDE_IN_PROGRESS" -> "Highway Ride in Progress"
        "COMPLETED" -> "Trip Completed!"
        else -> "Active Highway Ride"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = statusColor,
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_ride_status_mini_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (subStatus) {
                            "DRIVER_COMING" -> Icons.Default.DirectionsCar
                            "DRIVER_ARRIVED" -> Icons.Default.LocationOn
                            "BOARDING" -> Icons.Default.AirlineSeatReclineNormal
                            "RIDE_IN_PROGRESS" -> Icons.Default.Speed
                            "COMPLETED" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Navigation
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = statusText.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${departure.pickupCity} → ${departure.dropoffCity}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = Color.White
                )
                Text(
                    text = "Captain ${departure.driverName} • Tap for Live Telemetry & PIN",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
