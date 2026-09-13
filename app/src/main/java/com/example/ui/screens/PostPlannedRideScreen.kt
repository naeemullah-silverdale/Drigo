package com.example.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannedDeparture
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class CorridorOption(
    val title: String,
    val distanceSubtitle: String,
    val tag: String,
    val distanceKm: Double,
    val durationMinutes: Int
) {
    MOTORWAY_M2("Via M-2 Motorway", "375 km • 4h 15m", "Recommended", 375.0, 255),
    GT_ROAD("Via GT Road", "290 km • 6h 10m", "Slower Route", 290.0, 370),
    MOTORWAY_M1("Via M-1 Motorway", "155 km • 1h 45m", "Fast Route", 155.0, 105),
    MOTORWAY_M3("Via M-3 Motorway", "180 km • 2h 10m", "Recommended", 180.0, 130),
    MOTORWAY_M9("Via M-9 Super Highway", "160 km • 2h 00m", "Express", 160.0, 120)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostPlannedRideScreen(
    driverId: String,
    driverName: String,
    driverPhone: String,
    onBackClick: () -> Unit,
    onPublishedSuccess: (PlannedDeparture) -> Unit,
    onOpenSos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }

    BackHandler(onBack = onBackClick)

    // Form State
    var pickupCity by remember { mutableStateOf("Islamabad") }
    var pickupHub by remember { mutableStateOf("G-9 Markaz Hub") }
    var pickupStopDetails by remember { mutableStateOf("Near Karachi Company Taxi Stand") }
    var pickupLat by remember { mutableDoubleStateOf(33.6938) }
    var pickupLon by remember { mutableDoubleStateOf(73.0336) }

    var dropoffCity by remember { mutableStateOf("Lahore") }
    var dropoffHub by remember { mutableStateOf("DHA Phase 5 / Ring Road") }
    var dropoffStopDetails by remember { mutableStateOf("Via Thokar Interchange Exit") }
    var dropoffLat by remember { mutableDoubleStateOf(31.4707) }
    var dropoffLon by remember { mutableDoubleStateOf(74.4098) }

    var selectedCorridor by remember { mutableStateOf(CorridorOption.MOTORWAY_M2) }
    var tollsPreCleared by remember { mutableStateOf(true) }

    // Date & Time
    val calendar = remember { Calendar.getInstance() }
    val todayDateStr = remember {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(calendar.time)
    }
    val tomorrowDateStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(cal.time)
    }

    var selectedDateLabel by remember { mutableStateOf("Tomorrow, 25 Oct") }
    var selectedTimeLabel by remember { mutableStateOf("09:30 AM") }
    var flexWindowMins by remember { mutableIntStateOf(15) }

    // Pricing & Seats
    var farePerSeat by remember { mutableIntStateOf(1900) }
    var totalSeats by remember { mutableIntStateOf(4) }
    var allowFullCarBuyout by remember { mutableStateOf(true) }
    var fullCarFare by remember { mutableIntStateOf(7500) }

    // Policies
    var isInstantBooking by remember { mutableStateOf(true) }
    var allowCounterOffers by remember { mutableStateOf(true) }
    var isLadiesOnly by remember { mutableStateOf(false) }

    var isPublishing by remember { mutableStateOf(false) }
    var showCityChangeDialog by remember { mutableStateOf<String?>(null) } // "PICKUP" or "DROPOFF"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("post_planned_ride_screen"),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Drigo Logo / Tag Pill
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = DrigoBrandPurple.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, DrigoBrandPurple.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drigo • Intercity",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Post Planned Ride",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$pickupCity → $dropoffCity",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00C853),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Red SOS button
                    Surface(
                        onClick = onOpenSos,
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "SOS",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Avatar
                    Surface(
                        shape = CircleShape,
                        color = DrigoBrandPurple.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, Color(0xFF00C853)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = DrigoBrandPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isPublishing) return@Button
                            isPublishing = true
                            scope.launch {
                                val departure = PlannedDeparture(
                                    driverId = driverId,
                                    driverName = driverName.ifBlank { "Captain Farhan" },
                                    driverPhone = driverPhone.ifBlank { "+92 300 1234567" },
                                    driverRating = 4.92,
                                    driverVehicle = "Toyota Corolla (White)",
                                    driverPlateNumber = "LEA-18-4921",
                                    pickupCity = pickupCity,
                                    pickupHub = pickupHub,
                                    pickupStopDetails = pickupStopDetails,
                                    pickupLat = pickupLat,
                                    pickupLon = pickupLon,
                                    dropoffCity = dropoffCity,
                                    dropoffHub = dropoffHub,
                                    dropoffStopDetails = dropoffStopDetails,
                                    dropoffLat = dropoffLat,
                                    dropoffLon = dropoffLon,
                                    corridorName = selectedCorridor.title,
                                    corridorSubtitle = "${selectedCorridor.distanceSubtitle} ${selectedCorridor.title}",
                                    distanceKm = selectedCorridor.distanceKm,
                                    durationMinutes = selectedCorridor.durationMinutes,
                                    tollsPreCleared = tollsPreCleared,
                                    departureDateText = selectedDateLabel,
                                    departureTimeText = selectedTimeLabel,
                                    flexWindowMins = flexWindowMins,
                                    pickupWindowText = "Window: ±$flexWindowMins mins around $selectedTimeLabel",
                                    farePerSeat = farePerSeat,
                                    totalSeats = totalSeats,
                                    availableSeats = totalSeats,
                                    allowFullCarBuyout = allowFullCarBuyout,
                                    fullCarFare = fullCarFare,
                                    isInstantBooking = isInstantBooking,
                                    allowCounterOffers = allowCounterOffers,
                                    isLadiesOnly = isLadiesOnly,
                                    status = "ACTIVE"
                                )

                                val res = repo.savePlannedDeparture(departure)
                                isPublishing = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Departure published successfully! Riders can now book.", Toast.LENGTH_LONG).show()
                                    onPublishedSuccess(departure)
                                } else {
                                    Toast.makeText(context, "Departure saved locally.", Toast.LENGTH_SHORT).show()
                                    onPublishedSuccess(departure)
                                }
                            }
                        },
                        enabled = !isPublishing,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("publish_departure_button")
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Publish Departure →",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Free cancellation or edits up to 2 hours before $selectedTimeLabel departure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 10.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8FB))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Header Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00C853),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "1",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Departure Plan & Seats",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 14.5.sp
                        )
                        Text(
                            text = "Intercity Motorway Corridor Configuration",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // =========================================================================
            // CARD 1: TRIP CORRIDOR & ROUTE
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trip Corridor & Route",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(0.5.dp, Color(0xFF81C784))
                        ) {
                            Text(
                                text = "• High Demand Route",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pickup Origin block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                                .background(Color(0xFF00C853), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PICKUP ORIGIN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$pickupCity • $pickupHub",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "📍 $pickupStopDetails",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        TextButton(
                            onClick = { showCityChangeDialog = "PICKUP" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Change",
                                color = DrigoBrandPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    // Vertical connector line
                    Box(
                        modifier = Modifier
                            .padding(start = 7.dp)
                            .height(16.dp)
                            .width(2.dp)
                            .background(Color(0xFFCBD5E1))
                    )

                    // Dropoff Destination block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                                .background(Color(0xFFD32F2F), RoundedCornerShape(3.dp))
                                .border(2.dp, Color.White, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DROPOFF DESTINATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$dropoffCity • $dropoffHub",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "⚑ $dropoffStopDetails",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        TextButton(
                            onClick = { showCityChangeDialog = "DROPOFF" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Change",
                                color = DrigoBrandPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Select Preferred Corridor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Preferred Corridor",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(0.5.dp, Color(0xFF93C5FD))
                        ) {
                            Text(
                                text = "E-Tag FastPay Supported",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option A: Motorway M-2
                    val isM2Selected = selectedCorridor == CorridorOption.MOTORWAY_M2
                    Surface(
                        onClick = { selectedCorridor = CorridorOption.MOTORWAY_M2 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isM2Selected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            if (isM2Selected) 1.5.dp else 1.dp,
                            if (isM2Selected) Color(0xFF00C853) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isM2Selected,
                                onClick = { selectedCorridor = CorridorOption.MOTORWAY_M2 },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00C853))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Via M-2 Motorway",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF00C853).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Recommended",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "375 km • 4h 15m (Fast & Safe Toll Highway)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option B: GT Road
                    val isGtSelected = selectedCorridor == CorridorOption.GT_ROAD
                    Surface(
                        onClick = { selectedCorridor = CorridorOption.GT_ROAD },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGtSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            if (isGtSelected) 1.5.dp else 1.dp,
                            if (isGtSelected) Color(0xFF00C853) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isGtSelected,
                                onClick = { selectedCorridor = CorridorOption.GT_ROAD },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00C853))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Via GT Road (N-5)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF64748B).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Slower Route",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "290 km • 6h 10m (Multiple City Crossings)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // CARD 2: SCHEDULE DEPARTURE
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Schedule Departure",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            onClick = {
                                flexWindowMins = if (flexWindowMins == 15) 30 else if (flexWindowMins == 30) 45 else 15
                            },
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFF00897B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "±${flexWindowMins}m Flex Window ⌄",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF00897B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Selector Row
                    Text(
                        text = "SELECT DEPARTURE DATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Today Chip
                        val isToday = selectedDateLabel.startsWith("Today")
                        Surface(
                            onClick = { selectedDateLabel = "Today, $todayDateStr" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isToday) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isToday) 1.5.dp else 1.dp,
                                if (isToday) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Today, $todayDateStr",
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) Color(0xFF047857) else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        // Tomorrow Chip
                        val isTomorrow = selectedDateLabel.startsWith("Tomorrow")
                        Surface(
                            onClick = { selectedDateLabel = "Tomorrow, $tomorrowDateStr" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isTomorrow) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isTomorrow) 1.5.dp else 1.dp,
                                if (isTomorrow) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Tomorrow, 25 Oct",
                                fontSize = 12.sp,
                                fontWeight = if (isTomorrow) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTomorrow) Color(0xFF047857) else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        // Custom Calendar Button
                        Surface(
                            onClick = {
                                selectedDateLabel = "26 Oct • Next Day"
                                Toast.makeText(context, "Date set for 26 Oct", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Custom Date",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Departure Time Selector
                    Text(
                        text = "SELECT DEPARTURE TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset time chips
                    val timePresets = listOf(
                        "08:00 AM" to "Morning Rush",
                        "09:30 AM" to "Prime Booking",
                        "02:30 PM" to "Afternoon",
                        "06:00 PM" to "Evening"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timePresets.take(3).forEach { (time, note) ->
                            val isSelected = selectedTimeLabel == time
                            Surface(
                                onClick = { selectedTimeLabel = time },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF00C853) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = time,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFF047857) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = note,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color(0xFF047857) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Info Callout Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pickup Window: ±${flexWindowMins}m around $selectedTimeLabel (allows accommodating nearby passenger stops along route)",
                                fontSize = 11.5.sp,
                                color = Color(0xFF1E40AF),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // CARD 3: SEAT FARE & BUYOUT
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seat Fare & Buyout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(0.5.dp, Color(0xFFA7F3D0))
                        ) {
                            Text(
                                text = "M-2 Tolls Included",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Benchmark banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📈 Corridor Market Benchmark: PKR 1,800 – 2,100",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fare per seat Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FARE PER SEAT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "PKR $farePerSeat",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00796B)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // -100 button
                            Surface(
                                onClick = { if (farePerSeat > 500) farePerSeat -= 100 },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "-100",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            // +100 button
                            Surface(
                                onClick = { farePerSeat += 100 },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00C853).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF00C853))
                            ) {
                                Text(
                                    text = "+100",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Available Seats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE SEATS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$totalSeats passenger seats",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..4).forEach { count ->
                                val isSelected = totalSeats == count
                                Surface(
                                    onClick = { totalSeats = count },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF00C853) else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$count",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Allow Full-Car Buyout Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color(0xFF00796B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Full-Car Buyout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Single rider pays PKR $fullCarFare for entire car",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = allowFullCarBuyout,
                            onCheckedChange = { allowFullCarBuyout = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00C853)
                            )
                        )
                    }
                }
            }

            // =========================================================================
            // CARD 4: CAPTAIN RIDE POLICIES (FROM 3RD SCREENSHOT)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Captain Ride Policies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Booking Type Segmented Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Instant Booking
                        Surface(
                            onClick = { isInstantBooking = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isInstantBooking) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isInstantBooking) 1.5.dp else 1.dp,
                                if (isInstantBooking) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡ Instant",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isInstantBooking) Color(0xFF047857) else Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "Auto-confirms verified passenger bookings",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Driver Approval
                        Surface(
                            onClick = { isInstantBooking = false },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isInstantBooking) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (!isInstantBooking) 1.5.dp else 1.dp,
                                if (!isInstantBooking) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "👤 Approval",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (!isInstantBooking) Color(0xFF047857) else Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "Review passenger rating before accepting",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle 1: Allow Fare Counter-Offers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriceChange,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Fare Counter-Offers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Riders can negotiate within ± PKR 300 of your price",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = allowCounterOffers,
                            onCheckedChange = { allowCounterOffers = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00C853)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle 2: Ladies Only Corridor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFCE4EC),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "♀",
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE91E63),
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ladies Only Corridor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFCE4EC)
                                ) {
                                    Text(
                                        text = "Verified",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2185B),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Exclusive for verified female passengers only",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = isLadiesOnly,
                            onCheckedChange = { isLadiesOnly = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE91E63)
                            )
                        )
                    }
                }
            }
        }
    }

    // City & Hub Picker Dialog
    if (showCityChangeDialog != null) {
        val isPickupPicker = showCityChangeDialog == "PICKUP"
        val pakCities = listOf(
            "Islamabad" to "G-9 Markaz Hub / Faizabad Stop",
            "Lahore" to "DHA Phase 5 / Thokar Niaz Baig",
            "Rawalpindi" to "Saddar Metro / Pirwadhai",
            "Peshawar" to "University Road / Motorway Toll Plaza",
            "Faisalabad" to "D-Ground / Motorway M-4 Interchange",
            "Multan" to "Chowk Kumharan / M-4 Exit",
            "Karachi" to "Sohrab Goth / Toll Plaza M-9",
            "Hyderabad" to "Auto Bhan Road / M-9 Terminal",
            "Sialkot" to "Kashmir Road / Sambrial Interchange",
            "Gujranwala" to "Chanda Qila / GT Road Hub"
        )

        AlertDialog(
            onDismissRequest = { showCityChangeDialog = null },
            title = {
                Text(
                    text = if (isPickupPicker) "Select Pickup Hub" else "Select Dropoff Hub",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pakCities.forEach { (city, hub) ->
                        Surface(
                            onClick = {
                                if (isPickupPicker) {
                                    pickupCity = city
                                    pickupHub = hub.substringBefore(" /")
                                    pickupStopDetails = "Near $hub"
                                } else {
                                    dropoffCity = city
                                    dropoffHub = hub.substringBefore(" /")
                                    dropoffStopDetails = "Near $hub"
                                }
                                showCityChangeDialog = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = city, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = hub, fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityChangeDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}
