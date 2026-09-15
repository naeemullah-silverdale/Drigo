package com.example.ui.screens

import android.app.DatePickerDialog
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
import com.example.ui.components.IntercityMapWaypoint
import com.example.ui.components.IntercityOsmMapView
import com.example.ui.components.IntercityWaypointStatus
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

enum class CorridorOption(
    val title: String,
    val distanceSubtitle: String,
    val tag: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val tollEstimate: String = "PKR 1,100 M-Tag Pre-cleared"
) {
    MOTORWAY_M2("Via M-2 Motorway", "375 km • 4h 15m", "Recommended", 375.0, 255, "PKR 1,100 M-Tag"),
    GT_ROAD("Via GT Road (N-5)", "290 km • 6h 10m", "Slower Route", 290.0, 370, "PKR 240 Tolls"),
    MOTORWAY_M1("Via M-1 Motorway", "155 km • 1h 45m", "Fast Route", 155.0, 105, "PKR 450 M-Tag"),
    MOTORWAY_M3("Via M-3 Motorway", "180 km • 2h 10m", "Recommended", 180.0, 130, "PKR 550 M-Tag"),
    MOTORWAY_M9("Via M-9 Super Highway", "160 km • 2h 00m", "Express", 160.0, 120, "PKR 350 M-Tag")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostPlannedRideScreen(
    driverId: String,
    driverName: String,
    driverPhone: String,
    driverRating: Double = 5.0,
    driverTotalTrips: Int = 0,
    driverVehicle: String = "",
    driverPlateNumber: String = "",
    driverVehicleType: String = "Sedan",
    onBackClick: () -> Unit,
    onPublishedSuccess: (PlannedDeparture) -> Unit,
    onOpenSos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }

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
    var showMapPreview by remember { mutableStateOf(false) }

    LaunchedEffect(driverId) {
        android.util.Log.d(
            "DrigoPostPlannedRide",
            "PostPlannedRideScreen initialized: driverId='$driverId', driverName='$driverName', driverPhone='$driverPhone', driverVehicle='$driverVehicle', Corridor=${selectedCorridor.title}"
        )
    }

    // Date & Time
    val calendar = remember { Calendar.getInstance() }
    val todayDateStr = remember {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(calendar.time)
    }
    val tomorrowDateStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(cal.time)
    }
    val dayAfterDateStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(cal.time)
    }

    var selectedDateLabel by remember { mutableStateOf("Tomorrow, $tomorrowDateStr") }
    var selectedTimeLabel by remember { mutableStateOf("09:30 AM") }
    var flexWindowMins by remember { mutableIntStateOf(15) }

    // Pricing & Seats
    var farePerSeat by remember { mutableIntStateOf(1900) }
    var totalSeats by remember { mutableIntStateOf(4) }
    var allowFullCarBuyout by remember { mutableStateOf(true) }
    val calculatedBuyoutFare = remember(farePerSeat, totalSeats) {
        // 5% bundle discount for full car buyout
        (farePerSeat * totalSeats * 0.95).toInt().coerceAtLeast(1000)
    }
    var fullCarFare by remember { mutableIntStateOf(7200) }

    LaunchedEffect(calculatedBuyoutFare) {
        fullCarFare = calculatedBuyoutFare
    }

    // Policies & Amenities
    var isInstantBooking by remember { mutableStateOf(true) }
    var allowCounterOffers by remember { mutableStateOf(true) }
    var isLadiesOnly by remember { mutableStateOf(false) }
    var hasAirConditioning by remember { mutableStateOf(true) }
    var hasLuggageSpace by remember { mutableStateOf(true) }
    var isSmokeFree by remember { mutableStateOf(true) }
    var hasMTagFastPay by remember { mutableStateOf(true) }

    var isPublishing by remember { mutableStateOf(false) }
    var showCityChangeDialog by remember { mutableStateOf<String?>(null) } // "PICKUP" or "DROPOFF"

    BackHandler(enabled = !isPublishing) {
        onBackClick()
    }

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
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Publish Departure",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$pickupCity ➔ $dropoffCity",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00C853),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
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
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "SOS",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Avatar
                    Surface(
                        shape = CircleShape,
                        color = DrigoBrandPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, Color(0xFF00C853)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = DrigoBrandPurple,
                                modifier = Modifier.size(18.dp)
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
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Estimated Earnings Preview Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Est. Gross Revenue:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "PKR ${farePerSeat * totalSeats}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00796B)
                        )
                    }

                    Button(
                        onClick = {
                            if (isPublishing) return@Button
                            isPublishing = true
                            scope.launch {
                                val departure = PlannedDeparture(
                                    driverId = driverId,
                                    driverName = driverName.ifBlank { "Driver" },
                                    driverPhone = driverPhone,
                                    driverRating = driverRating,
                                    driverTotalTrips = driverTotalTrips,
                                    driverVehicle = driverVehicle.ifBlank { "Toyota Corolla" },
                                    driverPlateNumber = driverPlateNumber.ifBlank { "LEB-4521" },
                                    driverVehicleType = driverVehicleType,
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
                                    isClimateControlled = hasAirConditioning,
                                    luggagePolicy = if (hasLuggageSpace) "Standard (1 bag/seat)" else "Light Backpack Only",
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("publish_departure_button")
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Publish Departure →",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Free edits & cancellation up to 2 hours before departure.",
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
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step Header Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00C853),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "1",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Step 1 of 2: Configure Departure Plan",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Motorway Corridor & Capacity Matrix",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Responsive Telemetry Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Express M-2", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("NHMP Verified", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("M-Tag Ready", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // CARD 1: TRIP CORRIDOR & ROUTE
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.5.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header Row with robust flex wrapping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trip Corridor & Route",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(0.8.dp, Color(0xFF81C784))
                        ) {
                            Text(
                                text = "🔥 High Demand",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Route Nodes Container with clean connected timeline
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            // Pickup Origin block
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color(0xFF00C853), CircleShape)
                                            .border(2.dp, Color.White, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(30.dp)
                                            .background(Color(0xFFCBD5E1))
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PICKUP ORIGIN",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF64748B),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Change",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DrigoBrandPurple,
                                            modifier = Modifier
                                                .clickable { showCityChangeDialog = "PICKUP" }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "$pickupCity • $pickupHub",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "📍 $pickupStopDetails",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Dropoff Destination block
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color(0xFFD32F2F), RoundedCornerShape(3.dp))
                                            .border(2.dp, Color.White, RoundedCornerShape(3.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DROPOFF DESTINATION",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF64748B),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Change",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DrigoBrandPurple,
                                            modifier = Modifier
                                                .clickable { showCityChangeDialog = "DROPOFF" }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "$dropoffCity • $dropoffHub",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "⚑ $dropoffStopDetails",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Optional Interactive Map Preview toggle
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMapPreview = !showMapPreview }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = DrigoBrandPurple, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showMapPreview) "Hide Live Highway Map" else "Preview Highway Route on Map",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (showMapPreview) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = DrigoBrandPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showMapPreview) {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                IntercityOsmMapView(
                                    modifier = Modifier.fillMaxSize(),
                                    waypoints = listOf(
                                        IntercityMapWaypoint(
                                            stopNumber = 1,
                                            title = pickupCity,
                                            subtitle = pickupHub,
                                            point = GeoPoint(pickupLat, pickupLon),
                                            status = IntercityWaypointStatus.ACTIVE_NEXT,
                                            isPickup = true
                                        ),
                                        IntercityMapWaypoint(
                                            stopNumber = 2,
                                            title = dropoffCity,
                                            subtitle = dropoffHub,
                                            point = GeoPoint(dropoffLat, dropoffLon),
                                            status = IntercityWaypointStatus.UPCOMING,
                                            isPickup = false
                                        )
                                    ),
                                    routePoints = listOf(
                                        GeoPoint(pickupLat, pickupLon),
                                        GeoPoint(32.4833, 73.1333), // Bhera rest stop
                                        GeoPoint(31.8500, 73.5000), // Sukheke rest stop
                                        GeoPoint(dropoffLat, dropoffLon)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Select Preferred Corridor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Preferred Corridor",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(0.5.dp, Color(0xFF93C5FD))
                        ) {
                            Text(
                                text = "M-Tag FastPay",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option A: Motorway M-2
                    val isM2Selected = selectedCorridor == CorridorOption.MOTORWAY_M2
                    Surface(
                        onClick = { selectedCorridor = CorridorOption.MOTORWAY_M2 },
                        shape = RoundedCornerShape(10.dp),
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
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isM2Selected,
                                onClick = { selectedCorridor = CorridorOption.MOTORWAY_M2 },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00C853)),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Via M-2 Motorway",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF00C853).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Recommended",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "375 km • 4h 15m (Fast Toll Highway • PKR 1,100)",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option B: GT Road
                    val isGtSelected = selectedCorridor == CorridorOption.GT_ROAD
                    Surface(
                        onClick = { selectedCorridor = CorridorOption.GT_ROAD },
                        shape = RoundedCornerShape(10.dp),
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
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isGtSelected,
                                onClick = { selectedCorridor = CorridorOption.GT_ROAD },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00C853)),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Via GT Road (N-5)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF64748B).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Slower",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "290 km • 6h 10m (City Crossings • PKR 240)",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.5.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Schedule Departure",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFF00897B),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "±${flexWindowMins}m ⌄",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF00897B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date Selector Row
                    Text(
                        text = "SELECT DEPARTURE DATE",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Today Chip
                        val isToday = selectedDateLabel.startsWith("Today")
                        Surface(
                            onClick = { selectedDateLabel = "Today, $todayDateStr" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isToday) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isToday) 1.5.dp else 1.dp,
                                if (isToday) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Today",
                                fontSize = 11.5.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) Color(0xFF047857) else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                                maxLines = 1
                            )
                        }

                        // Tomorrow Chip
                        val isTomorrow = selectedDateLabel.startsWith("Tomorrow")
                        Surface(
                            onClick = { selectedDateLabel = "Tomorrow, $tomorrowDateStr" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTomorrow) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isTomorrow) 1.5.dp else 1.dp,
                                if (isTomorrow) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = "Tomorrow",
                                fontSize = 11.5.sp,
                                fontWeight = if (isTomorrow) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTomorrow) Color(0xFF047857) else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                                maxLines = 1
                            )
                        }

                        // Day After Chip
                        val isDayAfter = selectedDateLabel.contains(dayAfterDateStr)
                        Surface(
                            onClick = { selectedDateLabel = dayAfterDateStr },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDayAfter) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isDayAfter) 1.5.dp else 1.dp,
                                if (isDayAfter) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Next Day",
                                fontSize = 11.5.sp,
                                fontWeight = if (isDayAfter) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDayAfter) Color(0xFF047857) else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                                maxLines = 1
                            )
                        }

                        // Custom Calendar Button
                        Surface(
                            onClick = {
                                val now = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val pickedCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        val sdf = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                                        selectedDateLabel = sdf.format(pickedCal.time)
                                    },
                                    now.get(Calendar.YEAR),
                                    now.get(Calendar.MONTH),
                                    now.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Custom Date",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Departure Time Selector
                    Text(
                        text = "SELECT DEPARTURE TIME",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val timePresets = listOf(
                        "08:00 AM" to "Morning",
                        "09:30 AM" to "Prime",
                        "02:30 PM" to "Noon"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        timePresets.forEach { (time, note) ->
                            val isSelected = selectedTimeLabel == time
                            Surface(
                                onClick = { selectedTimeLabel = time },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF00C853) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = time,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFF047857) else Color(0xFF0F172A),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = note,
                                        fontSize = 8.5.sp,
                                        color = if (isSelected) Color(0xFF047857) else Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Custom Time Picker Button
                        Surface(
                            onClick = {
                                val now = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val amPm = if (hourOfDay < 12) "AM" else "PM"
                                        val formattedHour = if (hourOfDay == 0 || hourOfDay == 12) 12 else hourOfDay % 12
                                        selectedTimeLabel = String.format(Locale.getDefault(), "%02d:%02d %s", formattedHour, minute, amPm)
                                    },
                                    now.get(Calendar.HOUR_OF_DAY),
                                    now.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Custom Time",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Info Callout Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Departure Window: $selectedDateLabel at $selectedTimeLabel (±${flexWindowMins}m check-in window)",
                                fontSize = 10.5.sp,
                                color = Color(0xFF1E40AF),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // CARD 3: CAR CABIN & SEAT MATRIX VISUALIZER
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.5.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seat Fare & Vehicle Cabin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(0.5.dp, Color(0xFFA7F3D0))
                        ) {
                            Text(
                                text = "M-2 Tolls Included",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Benchmark banner
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📈 Corridor Benchmark: PKR 1,800 – 2,100 / seat",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fare per seat Stepper Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FARE PER SEAT",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "PKR $farePerSeat",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00796B)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // -100 button
                            Surface(
                                onClick = { if (farePerSeat > 500) farePerSeat -= 100 },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "-100",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }

                            // +100 button
                            Surface(
                                onClick = { farePerSeat += 100 },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF00C853).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF00C853))
                            ) {
                                Text(
                                    text = "+100",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Available Seats Row & Matrix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "OFFERED SEATS",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$totalSeats seats available",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..4).forEach { count ->
                                val isSelected = totalSeats == count
                                Surface(
                                    onClick = { totalSeats = count },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF00C853) else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$count",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Allow Full-Car Buyout Switch & Calculated Discount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color(0xFF00796B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Full-Car Buyout",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "5% Off",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Single rider books all $totalSeats seats for PKR $fullCarFare",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
            // CARD 4: VEHICLE DETAILS & ONBOARD AMENITIES
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.5.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vehicle & Amenities",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(0.8.dp, Color(0xFF86EFAC))
                        ) {
                            Text(
                                text = "✓ Verified Captain",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vehicle Profile Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00796B).copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (driverVehicle.isNotBlank()) driverVehicle else "Toyota Corolla GLi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${if (driverPlateNumber.isNotBlank()) driverPlateNumber else "LEB-4521"} • $driverVehicleType",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("5.0", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Responsive 2x2 Amenity Checklist Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AmenityToggleChip(
                            label = "AC On",
                            icon = Icons.Default.AcUnit,
                            isSelected = hasAirConditioning,
                            onToggle = { hasAirConditioning = !hasAirConditioning },
                            modifier = Modifier.weight(1f)
                        )
                        AmenityToggleChip(
                            label = "Luggage",
                            icon = Icons.Default.Luggage,
                            isSelected = hasLuggageSpace,
                            onToggle = { hasLuggageSpace = !hasLuggageSpace },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AmenityToggleChip(
                            label = "No Smoking",
                            icon = Icons.Default.SmokeFree,
                            isSelected = isSmokeFree,
                            onToggle = { isSmokeFree = !isSmokeFree },
                            modifier = Modifier.weight(1f)
                        )
                        AmenityToggleChip(
                            label = "M-Tag",
                            icon = Icons.Default.Speed,
                            isSelected = hasMTagFastPay,
                            onToggle = { hasMTagFastPay = !hasMTagFastPay },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // =========================================================================
            // CARD 5: CAPTAIN RIDE POLICIES
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.5.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Captain Ride Policies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Booking Type Segmented Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Instant Booking
                        Surface(
                            onClick = { isInstantBooking = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isInstantBooking) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (isInstantBooking) 1.5.dp else 1.dp,
                                if (isInstantBooking) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡ Instant",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isInstantBooking) Color(0xFF047857) else Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "Auto-confirms bookings",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Driver Approval
                        Surface(
                            onClick = { isInstantBooking = false },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isInstantBooking) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (!isInstantBooking) 1.5.dp else 1.dp,
                                if (!isInstantBooking) Color(0xFF00C853) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "👤 Approval",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (!isInstantBooking) Color(0xFF047857) else Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "Review riders first",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle 1: Allow Fare Counter-Offers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriceChange,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fare Counter-Offers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Riders can negotiate within ± PKR 300",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle 2: Ladies Only Corridor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFCE4EC),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "♀",
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE91E63),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Ladies Only",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFCE4EC)
                                ) {
                                    Text(
                                        text = "Verified",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2185B),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Exclusive for verified female passengers",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
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
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = city, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(text = hub, fontSize = 11.sp, color = Color(0xFF64748B))
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

@Composable
private fun AmenityToggleChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            if (isSelected) 1.2.dp else 1.dp,
            if (isSelected) Color(0xFF00C853) else Color(0xFFCBD5E1)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF047857) else Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF047857) else Color(0xFF475569),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
