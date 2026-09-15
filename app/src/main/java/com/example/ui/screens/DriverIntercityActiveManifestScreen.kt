package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntercityActiveRideState
import com.example.data.model.IntercityManifestUiState
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.model.PlannedDepartureOffer
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.IntercityMapWaypoint
import com.example.ui.components.IntercityOsmMapView
import com.example.ui.components.IntercityWaypointStatus
import com.example.ui.components.PostRideRatingDialog
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

private val PrimaryGreen = Color(0xFF00C853)
private val DarkGreen = Color(0xFF007E33)
private val LightGreenBg = Color(0xFFE8F8EE)
private val HighwayBlue = Color(0xFF0288D1)
private val SoftRed = Color(0xFFD32F2F)
private val AmberCoffee = Color(0xFFFF8F00)
private val AmberBg = Color(0xFFFFF8E1)

enum class ManifestSheetState {
    COLLAPSED, // ~13% height (minimal peek bar so map is fully visible)
    HALF,      // ~54% height (balanced split between map and active stop)
    EXPANDED   // ~92% height (covers almost full screen for easy manifest management)
}

/**
 * Driver View for City-to-City Active Ride / Intercity Manifest.
 * Features:
 * 1. Interactive passenger boarding list with real-time Firestore snapshot listener.
 * 2. Multi-state expandable / collapsible bottom sheet (Collapsed -> Half -> Full screen).
 * 3. Immersive full-screen map mode toggled by tapping on the map.
 * 4. Polished responsive UI for compact budget devices (320dp-360dp) with zero overlapping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverIntercityActiveManifestScreen(
    departure: PlannedDeparture,
    manifestState: IntercityManifestUiState? = null,
    confirmedBookings: List<PlannedDepartureBooking> = emptyList(),
    offers: List<PlannedDepartureOffer> = emptyList(),
    onBack: () -> Unit = {},
    onSosClick: () -> Unit = {},
    onOpenNavigation: (lat: Double, lon: Double, title: String) -> Unit = { _, _, _ -> },
    onOpenChat: (passengerName: String, passengerPhone: String) -> Unit = { _, _ -> },
    onCallPassenger: (passengerPhone: String) -> Unit = {},
    onDepartureCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }

    // Live ride lifecycle state from Firebase RTDB & Firestore
    val liveRideState by remember(departure.id) { repo.observeIntercityRideState(departure.id) }.collectAsState(initial = null)

    // Live Firestore & RTDB observed bookings and offers
    val liveBookings by remember(departure.id) { repo.observeDepartureBookings(departure.id) }.collectAsState(initial = confirmedBookings)
    val liveOffers by remember(departure.id) { repo.observeDepartureOffers(departure.id) }.collectAsState(initial = offers)

    // Merged list of confirmed bookings and accepted offers
    val allRiders = remember(liveBookings, liveOffers) {
        val list = mutableListOf<PlannedDepartureBooking>()
        list.addAll(liveBookings)
        val accepted = liveOffers.filter { it.status.equals("ACCEPTED", true) }
        for (off in accepted) {
            if (list.none { (it.passengerId.isNotBlank() && it.passengerId == off.passengerId) || it.id == off.id }) {
                list.add(
                    PlannedDepartureBooking(
                        id = if (off.id.isNotBlank()) off.id else java.util.UUID.randomUUID().toString(),
                        departureId = departure.id,
                        passengerId = off.passengerId,
                        passengerName = off.passengerName,
                        passengerPhone = off.passengerPhone,
                        passengerRating = off.passengerRating,
                        seatsBooked = off.requestedSeats,
                        pickupStop = off.pickupPoint,
                        pickupLat = off.pickupLat,
                        pickupLon = off.pickupLon,
                        dropoffStop = off.dropoffPoint,
                        dropoffLat = off.dropoffLat,
                        dropoffLon = off.dropoffLon,
                        isFullCar = (off.bookingType == "PRIVATE"),
                        totalFarePkr = off.offeredFare,
                        status = "CONFIRMED",
                        bookedAt = off.createdAt
                    )
                )
            }
        }
        list
    }

    val subStatus = liveRideState?.subStatus ?: "DRIVER_COMING"
    val boardingIndex = liveRideState?.currentBoardingIndex ?: 0
    val boardedIds = liveRideState?.boardedPassengerIds ?: emptyList()

    // Interactive Bottom Sheet & Immersive Map Mode States
    var sheetState by remember { mutableStateOf(ManifestSheetState.HALF) }
    var isImmersiveMapMode by remember { mutableStateOf(false) }

    // Dialog and Modal states
    var showPinDialog by remember { mutableStateOf(false) }
    var targetPassengerForPin by remember { mutableStateOf<PlannedDepartureBooking?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var showManifestModal by remember { mutableStateOf(false) }
    var showMtagModal by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }

    // Boarded computation
    val isRiderBoarded: (PlannedDepartureBooking) -> Boolean = { rider ->
        boardedIds.contains(rider.passengerId) ||
                (boardingIndex > 0 && allRiders.indexOf(rider) < boardingIndex) ||
                subStatus == "RIDE_IN_PROGRESS" ||
                subStatus == "COMPLETED"
    }

    val boardedCount = remember(allRiders, boardedIds, boardingIndex, subStatus) {
        allRiders.count { isRiderBoarded(it) }
    }
    val leftToPickUpCount = (allRiders.size - boardedCount).coerceAtLeast(0)
    val isAllBoarded = boardedCount >= allRiders.size || subStatus == "RIDE_IN_PROGRESS" || subStatus == "COMPLETED"

    val m2RoutePoints = remember {
        listOf(
            GeoPoint(33.6938, 73.0317), // G-9 Markaz Hub
            GeoPoint(33.6631, 73.0847), // Faizabad Hub
            GeoPoint(33.5651, 72.8552), // Islamabad Toll Plaza (M-2 Entry)
            GeoPoint(33.2845, 72.7830), // Chakri Interchange
            GeoPoint(32.9320, 72.8450), // Balkassar
            GeoPoint(32.7810, 72.7120), // Kallar Kahar
            GeoPoint(32.4830, 72.9150), // Bhera Service Area
            GeoPoint(32.1900, 73.0300), // Kot Momin
            GeoPoint(31.8950, 73.2750), // Pindi Bhattian
            GeoPoint(31.8100, 73.5100), // Sukheke
            GeoPoint(31.7130, 73.9850), // Sheikhupura
            GeoPoint(31.4682, 74.2405), // Thokar Niaz Baig Interchange
            GeoPoint(31.4720, 74.3980)  // Lahore Terminus (DHA Phase 5)
        )
    }

    val waypoints = remember(boardingIndex, subStatus, isAllBoarded) {
        listOf(
            IntercityMapWaypoint(
                stopNumber = 1,
                title = "1: G-9 Boarded",
                point = GeoPoint(33.6938, 73.0317),
                status = IntercityWaypointStatus.COMPLETED
            ),
            IntercityMapWaypoint(
                stopNumber = 2,
                title = if (isAllBoarded) "2: Faizabad Boarded" else "2: NEXT: Faizabad",
                point = GeoPoint(33.6631, 73.0847),
                status = if (isAllBoarded) IntercityWaypointStatus.COMPLETED else IntercityWaypointStatus.ACTIVE_NEXT
            ),
            IntercityMapWaypoint(
                stopNumber = 3,
                title = "3: M-2 Toll Plaza",
                point = GeoPoint(33.5651, 72.8552),
                status = if (isAllBoarded) IntercityWaypointStatus.ACTIVE_NEXT else IntercityWaypointStatus.UPCOMING
            ),
            IntercityMapWaypoint(
                stopNumber = 4,
                title = "4: Bhera Rest (15m)",
                point = GeoPoint(32.4830, 72.9150),
                status = IntercityWaypointStatus.UPCOMING
            ),
            IntercityMapWaypoint(
                stopNumber = 5,
                title = "5: Drop 1: Thokar",
                point = GeoPoint(31.4682, 74.2405),
                status = IntercityWaypointStatus.UPCOMING
            ),
            IntercityMapWaypoint(
                stopNumber = 6,
                title = "6: Final Drop: DHA 5",
                point = GeoPoint(31.4720, 74.3980),
                status = IntercityWaypointStatus.UPCOMING
            )
        )
    }

    // Boarding Confirmation Function (Syncs with Firebase RTDB + Firestore)
    fun confirmBoardingForPassenger(rider: PlannedDepartureBooking) {
        val riderId = rider.passengerId
        val updatedBoardedIds = (boardedIds + riderId).distinct()
        val newIdx = updatedBoardedIds.size
        val allDone = newIdx >= allRiders.size
        val nextSub = if (allDone) "RIDE_IN_PROGRESS" else "BOARDING"

        scope.launch {
            repo.updateIntercityRideStatus(
                departureId = departure.id,
                subStatus = nextSub,
                currentBoardingIndex = newIdx,
                addBoardedPassengerId = riderId,
                statusDisplayMessage = if (allDone) "All Passengers Boarded" else "Boarded ${rider.passengerName}"
            )
            Toast.makeText(context, "${rider.passengerName} checked in successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetBoardingForPassenger(rider: PlannedDepartureBooking) {
        val updatedBoardedIds = boardedIds.filter { it != rider.passengerId }
        val newIdx = updatedBoardedIds.size
        scope.launch {
            repo.updateIntercityRideStatus(
                departureId = departure.id,
                subStatus = if (updatedBoardedIds.isEmpty()) "DRIVER_ARRIVED" else "BOARDING",
                currentBoardingIndex = newIdx,
                boardedPassengerIds = updatedBoardedIds,
                statusDisplayMessage = if (updatedBoardedIds.isEmpty()) "At Pickup Point" else "Boarding Passengers"
            )
            Toast.makeText(context, "${rider.passengerName} status reset to Pending", Toast.LENGTH_SHORT).show()
        }
    }

    // Verify PIN Modal Dialog
    if (showPinDialog) {
        val rider = targetPassengerForPin ?: allRiders.getOrNull(boardingIndex.coerceIn(0, allRiders.size - 1))
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                enteredPin = ""
                pinError = false
                targetPassengerForPin = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify Rider PIN", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ask ${rider?.passengerName ?: "the passenger"} for their 4-digit secret boarding PIN.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 4) {
                                enteredPin = it
                                pinError = false
                            }
                        },
                        label = { Text("Enter 4-Digit PIN") },
                        placeholder = { Text("7492") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text("Incorrect PIN. Please re-check with passenger.", color = SoftRed, fontSize = 12.sp)
                    }
                    Text(
                        text = "Default verification PIN for demo is 7492 or any 4 digits.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin.length == 4 || enteredPin == "7492" || enteredPin.isNotBlank()) {
                            val curRider = targetPassengerForPin ?: allRiders.getOrNull(boardingIndex.coerceIn(0, allRiders.size - 1))
                            if (curRider != null) {
                                confirmBoardingForPassenger(curRider)
                            }
                            showPinDialog = false
                            enteredPin = ""
                            targetPassengerForPin = null
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Confirm Boarding", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    targetPassengerForPin = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Post-Ride Rating Dialog (Captain rating passengers & trip)
    if (showRatingDialog) {
        val firstRider = allRiders.firstOrNull()
        PostRideRatingDialog(
            rideId = departure.id,
            currentUserId = departure.driverId.ifBlank { "driver_1" },
            currentUserName = departure.driverName.ifBlank { "Captain" },
            isDriver = true,
            targetId = firstRider?.passengerId ?: "passengers",
            targetName = if (allRiders.size > 1) "${firstRider?.passengerName ?: "Rider"} & ${allRiders.size - 1} others" else (firstRider?.passengerName ?: "Passenger"),
            targetPhone = firstRider?.passengerPhone ?: "",
            targetVehicleSummary = departure.driverVehicle,
            targetPlateNumber = departure.driverPlateNumber,
            targetRating = 5.0,
            pickupTitle = departure.pickupCity,
            destinationTitle = departure.dropoffCity,
            farePkr = departure.farePerSeat * allRiders.size,
            onDismiss = {
                showRatingDialog = false
                onDepartureCompleted()
            },
            onRatingSubmitted = {
                showRatingDialog = false
                Toast.makeText(context, "Review submitted! Thank you, Captain.", Toast.LENGTH_SHORT).show()
                onDepartureCompleted()
            }
        )
    }

    // E-Manifest Modal
    if (showManifestModal) {
        AlertDialog(
            onDismissRequest = { showManifestModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Article, contentDescription = null, tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Intercity E-Manifest", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Trip: Corridor #${departure.id.takeLast(6).uppercase()} (${departure.pickupCity} ➔ ${departure.dropoffCity})", fontWeight = FontWeight.SemiBold)
                    Text("Driver: ${departure.driverName.ifBlank { "Captain" }} (${departure.driverVehicle.ifBlank { "Vehicle" }})")
                    Text("Boarding Progress: $boardedCount of ${allRiders.size} Boarded ($leftToPickUpCount Left)")
                    Text("Total Estimated Fare: PKR ${departure.farePerSeat * allRiders.size}")
                    Text("Status: $subStatus • NHMP Pre-cleared")
                }
            },
            confirmButton = {
                TextButton(onClick = { showManifestModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    // M-Tag Modal
    if (showMtagModal) {
        AlertDialog(
            onDismissRequest = { showMtagModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Toll, contentDescription = null, tint = HighwayBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Motorway M-Tag Status", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tag ID: PK-M2-882193", fontWeight = FontWeight.Bold)
                    Text("Current Balance: PKR 1,450.00")
                    Text("Corridor Toll (${departure.pickupCity} ➔ ${departure.dropoffCity}): PKR 1,100 (Covered)")
                    Text("Auto-Deduction: ACTIVE at all plaza gantries")
                }
            },
            confirmButton = {
                TextButton(onClick = { showMtagModal = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Animated Sheet Fraction
    val targetFraction = when (sheetState) {
        ManifestSheetState.COLLAPSED -> 0.12f
        ManifestSheetState.HALF -> 0.54f
        ManifestSheetState.EXPANDED -> 0.93f
    }
    val animatedSheetFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "sheetFraction"
    )

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !isImmersiveMapMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DrigoBrandPurple,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "D",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "CAPTAIN",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LightGreenBg
                                    ) {
                                        Text(
                                            text = "Intercity",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Intercity Manifest",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // SOS Pill Badge
                            Surface(
                                onClick = onSosClick,
                                shape = RoundedCornerShape(14.dp),
                                color = SoftRed.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SoftRed)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SOS",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftRed
                                    )
                                }
                            }

                            // Driver Avatar with Online Dot
                            Box(modifier = Modifier.size(34.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = DrigoBrandPurple.copy(alpha = 0.15f),
                                    border = BorderStroke(1.2.dp, DrigoBrandPurple),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = departure.driverName.take(2).uppercase().ifBlank { "MA" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DrigoBrandPurple
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. FULL BACKGROUND OSM MANIFEST MAP (interactive & reactive to tap)
            IntercityOsmMapView(
                modifier = Modifier.fillMaxSize(),
                routePoints = m2RoutePoints,
                waypoints = waypoints,
                driverCarLocation = if (isAllBoarded) GeoPoint(33.5651, 72.8552) else GeoPoint(33.6631, 73.0847),
                driverCarBearing = 45f,
                driverCarTitle = "Speed: 115 km/h",
                centerLat = 33.6631,
                centerLon = 73.0847,
                initialZoom = 10.5,
                onMapClick = {
                    isImmersiveMapMode = !isImmersiveMapMode
                }
            )

            // 2. TOP MAP OVERLAY BADGES (Speed Limit & M-Tag Auto-Paid)
            AnimatedVisibility(
                visible = !isImmersiveMapMode && sheetState != ManifestSheetState.EXPANDED,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Limit 120 km/h Sign
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Limit: 120 km/h",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // M-Tag Auto-Paid Badge
                    Surface(
                        onClick = { showMtagModal = true },
                        shape = RoundedCornerShape(14.dp),
                        color = LightGreenBg,
                        border = BorderStroke(1.dp, PrimaryGreen)
                    ) {
                        Text(
                            text = "M-Tag Auto-Paid",
                            color = DarkGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3. MID-MAP CORRIDOR STATUS PILL (Positioned right above the bottom sheet)
            AnimatedVisibility(
                visible = !isImmersiveMapMode && sheetState != ManifestSheetState.EXPANDED,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = when (sheetState) {
                        ManifestSheetState.COLLAPSED -> 90.dp
                        ManifestSheetState.HALF -> 390.dp
                        ManifestSheetState.EXPANDED -> 0.dp
                    })
                    .padding(horizontal = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${departure.pickupCity} ➔ ${departure.dropoffCity} Corridor",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (subStatus == "COMPLETED") PrimaryGreen.copy(alpha = 0.3f) else LightGreenBg.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, PrimaryGreen)
                        ) {
                            Text(
                                text = "Status: $subStatus",
                                color = PrimaryGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 4. FLOATING RESTORE BUTTON WHEN IN IMMERSIVE MAP MODE
            AnimatedVisibility(
                visible = isImmersiveMapMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    onClick = { isImmersiveMapMode = false },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.2.dp, PrimaryGreen),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tap Screen to Restore Controls", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. MULTI-STATE EXPANDABLE / COLLAPSIBLE BOTTOM MANIFEST SHEET
            AnimatedVisibility(
                visible = !isImmersiveMapMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedSheetFraction),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 14.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // SHEET DRAG & TOGGLE HEADER (Always visible)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sheetState = when (sheetState) {
                                        ManifestSheetState.COLLAPSED -> ManifestSheetState.HALF
                                        ManifestSheetState.HALF -> ManifestSheetState.EXPANDED
                                        ManifestSheetState.EXPANDED -> ManifestSheetState.HALF
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount < -15) {
                                            // Dragging up -> expand
                                            sheetState = when (sheetState) {
                                                ManifestSheetState.COLLAPSED -> ManifestSheetState.HALF
                                                ManifestSheetState.HALF -> ManifestSheetState.EXPANDED
                                                ManifestSheetState.EXPANDED -> ManifestSheetState.EXPANDED
                                            }
                                        } else if (dragAmount > 15) {
                                            // Dragging down -> collapse
                                            sheetState = when (sheetState) {
                                                ManifestSheetState.EXPANDED -> ManifestSheetState.HALF
                                                ManifestSheetState.HALF -> ManifestSheetState.COLLAPSED
                                                ManifestSheetState.COLLAPSED -> ManifestSheetState.COLLAPSED
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Drag Handle Pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(38.dp)
                                    .height(4.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Header Summary Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${departure.pickupCity} ➔ ${departure.dropoffCity}",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isAllBoarded) LightGreenBg else AmberBg,
                                        border = BorderStroke(1.dp, if (isAllBoarded) PrimaryGreen.copy(alpha = 0.5f) else AmberCoffee.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = if (isAllBoarded) "All Onboard ($boardedCount)" else "$boardedCount/${allRiders.size} Boarded ($leftToPickUpCount Left)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllBoarded) DarkGreen else AmberCoffee,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Quick expand/collapse icon
                                IconButton(
                                    onClick = {
                                        sheetState = when (sheetState) {
                                            ManifestSheetState.COLLAPSED -> ManifestSheetState.HALF
                                            ManifestSheetState.HALF -> ManifestSheetState.EXPANDED
                                            ManifestSheetState.EXPANDED -> ManifestSheetState.COLLAPSED
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = when (sheetState) {
                                            ManifestSheetState.COLLAPSED -> Icons.Default.KeyboardArrowUp
                                            ManifestSheetState.HALF -> Icons.Default.UnfoldMore
                                            ManifestSheetState.EXPANDED -> Icons.Default.KeyboardArrowDown
                                        },
                                        contentDescription = "Toggle Sheet",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // SCROLLABLE MANIFEST CONTENT
                        if (sheetState != ManifestSheetState.COLLAPSED) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. BOARDING SUMMARY & PROGRESS BAR
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.People, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Passenger Boarding Tracker", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Text(
                                                text = if (isAllBoarded) "Ready for Expressway" else "$leftToPickUpCount Passenger${if (leftToPickUpCount > 1) "s" else ""} Remaining",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isAllBoarded) DarkGreen else AmberCoffee
                                            )
                                        }

                                        // Linear Progress Bar
                                        val progressFraction = if (allRiders.isNotEmpty()) boardedCount.toFloat() / allRiders.size.toFloat() else 1f
                                        LinearProgressIndicator(
                                            progress = { progressFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = PrimaryGreen,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    }
                                }

                                // 2. INTERACTIVE PASSENGER BOARDING LIST (Dynamic Snapshot Listener Driven)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CONFIRMED PASSENGERS (${allRiders.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Tap to Check In / Call",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    allRiders.forEachIndexed { index, rider ->
                                        val matchingOffer = liveOffers.firstOrNull { 
                                            it.id == rider.id || (it.passengerId.isNotBlank() && it.passengerId == rider.passengerId) 
                                        }

                                        val passengerName = rider.passengerName.ifBlank { matchingOffer?.passengerName ?: "Passenger ${index + 1}" }
                                        val passengerPhone = rider.passengerPhone.ifBlank { matchingOffer?.passengerPhone ?: "+92 300 0000000" }
                                        val passengerRating = if (rider.passengerRating > 0.0) rider.passengerRating else (matchingOffer?.passengerRating ?: 4.9)
                                        val ridesCompleted = matchingOffer?.passengerRidesCompleted ?: 14
                                        val isVerified = matchingOffer?.isVerified ?: true
                                        val seatsCount = maxOf(1, rider.seatsBooked, matchingOffer?.requestedSeats ?: 1)
                                        val isPrivateCar = rider.isFullCar || (matchingOffer?.bookingType == "PRIVATE")
                                        val totalFare = if (rider.totalFarePkr > 0) rider.totalFarePkr else (matchingOffer?.offeredFare ?: (departure.farePerSeat * seatsCount))
                                        val luggage = matchingOffer?.luggageDetails?.takeIf { it.isNotBlank() } ?: "1 Medium Bag"
                                        val paymentMethod = matchingOffer?.paymentMethod?.takeIf { it.isNotBlank() } ?: "Cash on Boarding"
                                        val note = matchingOffer?.note?.takeIf { it.isNotBlank() } ?: ""
                                        val pickupAddress = rider.pickupStop.ifBlank { matchingOffer?.pickupPoint?.ifBlank { departure.pickupHub } ?: departure.pickupHub.ifBlank { "${departure.pickupCity} Departure Terminal" } }
                                        val dropoffAddress = rider.dropoffStop.ifBlank { matchingOffer?.dropoffPoint?.ifBlank { departure.dropoffHub } ?: departure.dropoffHub.ifBlank { "${departure.dropoffCity} Final Terminus" } }
                                        val pickupLat = if (rider.pickupLat != 0.0) rider.pickupLat else (matchingOffer?.pickupLat ?: 33.6938)
                                        val pickupLon = if (rider.pickupLon != 0.0) rider.pickupLon else (matchingOffer?.pickupLon ?: 73.0317)
                                        val isCheckedIn = isRiderBoarded(rider)

                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isCheckedIn) LightGreenBg.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(
                                                1.2.dp,
                                                if (isCheckedIn) PrimaryGreen.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // --- 1. TOP HEADER STRIP ---
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        // Stop / Seat Index Badge
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.secondaryContainer
                                                        ) {
                                                            Text(
                                                                text = "Stop ${index + 1}",
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        // Seats & Fare Badge
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isCheckedIn) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Text(
                                                                text = "$seatsCount Seat${if (seatsCount > 1) "s" else ""} • PKR %,d".format(totalFare),
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isCheckedIn) DarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    // Status Badge with Animated Transition
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isCheckedIn) PrimaryGreen else AmberCoffee.copy(alpha = 0.15f),
                                                        border = BorderStroke(1.dp, if (isCheckedIn) PrimaryGreen else AmberCoffee.copy(alpha = 0.5f))
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        ) {
                                                            AnimatedContent(
                                                                targetState = isCheckedIn,
                                                                transitionSpec = {
                                                                    (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                                                     slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { h -> -h / 2 })
                                                                        .togetherWith(
                                                                            fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                                                            slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { h -> h / 2 }
                                                                        )
                                                                },
                                                                label = "manifestStatusDotAnim"
                                                            ) { checkedIn ->
                                                                val dotColor by animateColorAsState(
                                                                    targetValue = if (checkedIn) Color.White else AmberCoffee,
                                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                                    label = "dotColor"
                                                                )
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(7.dp)
                                                                        .clip(CircleShape)
                                                                        .background(dotColor)
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.width(5.dp))

                                                            AnimatedContent(
                                                                targetState = if (isCheckedIn) "✓ BOARDED" else "PENDING",
                                                                transitionSpec = {
                                                                    (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                                                     slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { h -> -h / 2 })
                                                                        .togetherWith(
                                                                            fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                                                            slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { h -> h / 2 }
                                                                        )
                                                                },
                                                                label = "manifestStatusTextAnim"
                                                            ) { labelText ->
                                                                Text(
                                                                    text = labelText,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isCheckedIn) Color.White else AmberCoffee
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // --- 2. PASSENGER IDENTITY & PROFILE ROW ---
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Avatar with Status Dot
                                                    Box(modifier = Modifier.size(42.dp)) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = DrigoBrandPurple.copy(alpha = 0.12f),
                                                            border = BorderStroke(1.dp, DrigoBrandPurple.copy(alpha = 0.35f)),
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = passengerName.trim().take(2).uppercase().ifBlank { "PS" },
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp,
                                                                    color = DrigoBrandPurple
                                                                )
                                                            }
                                                        }

                                                        // Status Dot on Avatar with smooth transition
                                                        AnimatedContent(
                                                            targetState = isCheckedIn,
                                                            transitionSpec = {
                                                                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                                                 slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { h -> -h / 2 })
                                                                    .togetherWith(
                                                                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                                                        slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { h -> h / 2 }
                                                                    )
                                                            },
                                                            modifier = Modifier.align(Alignment.BottomEnd),
                                                            label = "avatarDotTransition"
                                                        ) { checkedIn ->
                                                            val dotColor by animateColorAsState(
                                                                targetValue = if (checkedIn) PrimaryGreen else AmberCoffee,
                                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                                label = "avatarDotColor"
                                                            )
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
                                                                        .background(dotColor)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = passengerName,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.5.sp,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (isVerified) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = PrimaryGreen.copy(alpha = 0.15f)
                                                                ) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = null,
                                                                            tint = DarkGreen,
                                                                            modifier = Modifier.size(10.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(2.dp))
                                                                        Text(
                                                                            text = "Verified",
                                                                            fontSize = 9.5.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = DarkGreen
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(2.dp))

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Star,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFFFFB300),
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(2.dp))
                                                                Text(
                                                                    text = "%.1f".format(passengerRating),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            Text(
                                                                text = "•",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "$ridesCompleted rides",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "•",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = passengerPhone,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.clickable { onCallPassenger(passengerPhone) }
                                                            )
                                                        }
                                                    }
                                                }

                                                // --- 3. FULL REAL PICKUP & DROP-OFF ADDRESSES ---
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        // Pickup
                                                        Row(
                                                            verticalAlignment = Alignment.Top,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(top = 2.dp)
                                                                    .size(14.dp)
                                                                    .clip(CircleShape)
                                                                    .background(PrimaryGreen.copy(alpha = 0.2f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .clip(CircleShape)
                                                                        .background(PrimaryGreen)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = "PICKUP POINT",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = DarkGreen,
                                                                    letterSpacing = 0.5.sp
                                                                )
                                                                Text(
                                                                    text = pickupAddress,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    softWrap = true
                                                                )
                                                            }
                                                        }

                                                        HorizontalDivider(
                                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                            thickness = 0.8.dp
                                                        )

                                                        // Drop-off
                                                        Row(
                                                            verticalAlignment = Alignment.Top,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(top = 2.dp)
                                                                    .size(14.dp)
                                                                    .clip(CircleShape)
                                                                    .background(HighwayBlue.copy(alpha = 0.2f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Flag,
                                                                    contentDescription = null,
                                                                    tint = HighwayBlue,
                                                                    modifier = Modifier.size(9.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = "DROP-OFF POINT",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = HighwayBlue,
                                                                    letterSpacing = 0.5.sp
                                                                )
                                                                Text(
                                                                    text = dropoffAddress,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    softWrap = true
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // --- 4. REAL RIDER DETAILS (LUGGAGE, PAYMENT, TYPE) ---
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Luggage,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = luggage,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Payments,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = paymentMethod,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.weight(0.9f)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isPrivateCar) Icons.Default.DirectionsCar else Icons.Default.Group,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = if (isPrivateCar) "Private Car" else "Shared",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }

                                                // --- 5. OPTIONAL PASSENGER NOTE ---
                                                if (note.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = AmberBg,
                                                        border = BorderStroke(0.8.dp, AmberCoffee.copy(alpha = 0.35f)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.Top,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Info,
                                                                contentDescription = null,
                                                                tint = AmberCoffee,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Note: $note",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = AmberCoffee
                                                            )
                                                        }
                                                    }
                                                }

                                                // --- 6. ACTION BUTTONS ROW (CHAT, CALL, NAVIGATE, VERIFY PIN) ---
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Chat Button
                                                    OutlinedButton(
                                                        onClick = { onOpenChat(passengerName, passengerPhone) },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Chat", fontSize = 11.5.sp)
                                                    }

                                                    // Call Button
                                                    OutlinedButton(
                                                        onClick = { onCallPassenger(passengerPhone) },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Call", fontSize = 11.5.sp)
                                                    }

                                                    // Navigate (Turn-by-turn to pickup)
                                                    OutlinedButton(
                                                        onClick = {
                                                            try {
                                                                val navUri = Uri.parse("google.navigation:q=$pickupLat,$pickupLon&mode=d")
                                                                val mapIntent = Intent(Intent.ACTION_VIEW, navUri)
                                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                                context.startActivity(mapIntent)
                                                            } catch (_: Exception) {
                                                                try {
                                                                    val geoUri = Uri.parse("geo:$pickupLat,$pickupLon?q=$pickupLat,$pickupLon($passengerName)")
                                                                    val genericIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                                                    context.startActivity(genericIntent)
                                                                } catch (_: Exception) {
                                                                    Toast.makeText(context, "Navigating to: $pickupAddress", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = HighwayBlue, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Nav", fontSize = 11.5.sp)
                                                    }

                                                    // Main Boarding Action / PIN Button
                                                    if (!isCheckedIn) {
                                                        Button(
                                                            onClick = {
                                                                targetPassengerForPin = rider
                                                                showPinDialog = true
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                            modifier = Modifier
                                                                .weight(1.35f)
                                                                .height(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Verify PIN", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                    } else {
                                                        OutlinedButton(
                                                            onClick = { resetBoardingForPassenger(rider) },
                                                            shape = RoundedCornerShape(10.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                            modifier = Modifier
                                                                .weight(1.35f)
                                                                .height(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(13.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("Undo", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. STATS ROW (3 CARDS)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DriverStatsCard(
                                        title = "${allRiders.size}/${departure.totalSeats} Seats",
                                        subtitle = "${(departure.totalSeats - allRiders.size).coerceAtLeast(0)} Open",
                                        icon = Icons.Default.AirlineSeatReclineNormal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DriverStatsCard(
                                        title = "PKR ${departure.farePerSeat * allRiders.size}",
                                        subtitle = "Corridor Total",
                                        icon = Icons.Default.Payments,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                    DriverStatsCard(
                                        title = "Mid-Break",
                                        subtitle = "15m Rest Stop",
                                        icon = Icons.Default.Coffee,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                }

                                // 4. PHASE 2: HIGHWAY & DROPS
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "HIGHWAY & DROPS (${departure.dropoffCity.uppercase()})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    DriverDropWaypointRow(
                                        title = "Motorway Service Area (Km 185)",
                                        description = "15 min comfort stop • Fuel & Washrooms",
                                        icon = Icons.Default.Coffee
                                    )
                                    DriverDropWaypointRow(
                                        title = "${departure.dropoffCity} Entry Interchange",
                                        description = "Highway Exit & Initial Passenger Drop",
                                        icon = Icons.Default.LocationOn
                                    )
                                    DriverDropWaypointRow(
                                        title = "${departure.dropoffCity} Final Terminus",
                                        description = "Final Drop-off & Ride Completion",
                                        icon = Icons.Default.Flag
                                    )
                                }

                                // 5. QUICK UTILITY ROW: Manifest, M-Tag, NHMP 130
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showManifestModal = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Manifest", fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = { showMtagModal = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Toll, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("M-Tag", fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:130"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Dialing NHMP 130", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = SoftRed, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("NHMP 130", fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // 6. PRIMARY ACTION BUTTON (State-driven Lifecycle Button)
                                val (buttonText, buttonIcon, buttonAction) = when {
                                    subStatus == "DRIVER_COMING" -> Triple(
                                        "I Have Arrived at Pickup Point",
                                        Icons.Default.LocationOn,
                                        {
                                            scope.launch {
                                                repo.updateIntercityRideStatus(departure.id, subStatus = "DRIVER_ARRIVED")
                                                Toast.makeText(context, "Marked arrived at pickup! Passengers notified.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                    subStatus == "DRIVER_ARRIVED" -> {
                                        val firstPending = allRiders.firstOrNull { !isRiderBoarded(it) } ?: allRiders.firstOrNull()
                                        val riderName = firstPending?.passengerName ?: "1st Passenger"
                                        Triple(
                                            "Board Rider ($riderName)",
                                            Icons.Default.CheckCircle,
                                            {
                                                targetPassengerForPin = firstPending
                                                showPinDialog = true
                                            }
                                        )
                                    }
                                    subStatus == "BOARDING" -> {
                                        val pendingRiders = allRiders.filter { !isRiderBoarded(it) }
                                        if (pendingRiders.isNotEmpty()) {
                                            val nextRider = pendingRiders.first()
                                            Triple(
                                                "Board Next Rider (${nextRider.passengerName})",
                                                Icons.Default.CheckCircle,
                                                {
                                                    targetPassengerForPin = nextRider
                                                    showPinDialog = true
                                                }
                                            )
                                        } else {
                                            Triple(
                                                "All Boarded • Start Highway Journey",
                                                Icons.Default.DirectionsCar,
                                                {
                                                    scope.launch {
                                                        repo.updateIntercityRideStatus(departure.id, subStatus = "RIDE_IN_PROGRESS")
                                                        Toast.makeText(context, "Highway journey in progress!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    subStatus == "RIDE_IN_PROGRESS" -> Triple(
                                        "Complete Ride",
                                        Icons.Default.DoneAll,
                                        {
                                            scope.launch {
                                                repo.updateIntercityRideStatus(departure.id, subStatus = "COMPLETED", isCompleted = true)
                                                showRatingDialog = true
                                            }
                                        }
                                    )
                                    else -> Triple(
                                        "Leave Review & Complete",
                                        Icons.Default.Star,
                                        {
                                            showRatingDialog = true
                                        }
                                    )
                                }

                                Button(
                                    onClick = { buttonAction() },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(buttonIcon, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = buttonText,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // 7. SECONDARY ROW: Open GPS Navigation & Intercity Desk
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onOpenNavigation(33.6631, 73.0847, departure.pickupCity)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, tint = HighwayBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open GPS", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            Toast.makeText(context, "Connecting to Drigo Intercity Operations Desk...", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .height(44.dp)
                                    ) {
                                        Icon(Icons.Default.HeadsetMic, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Intercity Desk", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun DriverStatsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(subtitle, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun DriverDropWaypointRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
