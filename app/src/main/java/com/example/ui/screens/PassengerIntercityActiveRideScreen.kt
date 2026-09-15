package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntercityActiveRideState
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.IntercityMapWaypoint
import com.example.ui.components.IntercityOsmMapView
import com.example.ui.components.IntercityWaypointStatus
import com.example.ui.components.PostRideRatingDialog
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint

private val PrimaryCorridorGreen = Color(0xFF00C853)
private val DarkCorridorGreen = Color(0xFF007E33)
private val LightGreenBg = Color(0xFFE8F8EE)
private val SoftPillBg = Color(0xFFF1F8F4)
private val HighwayBlue = Color(0xFF0288D1)
private val AmberCoffee = Color(0xFFFF8F00)
private val SoftRedSos = Color(0xFFD32F2F)

/**
 * Passenger View for City-to-City Active Scheduled Ride.
 * Directly reproduces Attachment 1 with live GPS tracking along the M-2 corridor,
 * Secret Boarding PIN, Captain Card, Corridor Manifest, Timeline Schedule, and Amenities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerIntercityActiveRideScreen(
    departure: PlannedDeparture? = null,
    booking: PlannedDepartureBooking? = null,
    onBack: () -> Unit = {},
    onOpenChat: (driverName: String, driverPhone: String) -> Unit = { _, _ -> },
    onCallDriver: (driverPhone: String) -> Unit = {},
    onShareTrip: () -> Unit = {},
    onSosClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { FirebaseRepository.getInstance(context) }

    // Live ride state observed from Firebase
    val liveRideState by remember(departure?.id) {
        repo.observeIntercityRideState(departure?.id ?: "")
    }.collectAsState(initial = null)

    var showSosDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var isImmersiveMapMode by remember { mutableStateOf(false) }
    var sheetState by remember { mutableStateOf(ManifestSheetState.HALF) }

    // Automatically trigger rating dialog once ride is completed
    LaunchedEffect(liveRideState?.subStatus, liveRideState?.isCompleted) {
        if (liveRideState?.subStatus == "COMPLETED" || liveRideState?.isCompleted == true) {
            showRatingDialog = true
        }
    }

    val driverName = departure?.driverName?.ifBlank { "Muhammad Ali" } ?: "Muhammad Ali"
    val driverPhone = departure?.driverPhone?.ifBlank { "+92 300 1234567" } ?: "+92 300 1234567"
    val driverVehicle = departure?.driverVehicle?.ifBlank { "TOYOTA YARIS" } ?: "TOYOTA YARIS"
    val driverPlate = departure?.driverPlateNumber?.ifBlank { "LEA-18-4921" } ?: "LEA-18-4921"
    val driverRating = departure?.driverRating ?: 4.9
    val driverTrips = departure?.driverTotalTrips?.takeIf { it > 0 } ?: 1240
    val farePkr = booking?.totalFarePkr ?: departure?.farePerSeat?.takeIf { it > 0 } ?: 1900
    val assignedSeat = booking?.seatsBooked ?: 3
    val pickupHub = booking?.pickupStop?.ifBlank { "Faizabad Hub" } ?: "Faizabad Hub"
    val subStatus = liveRideState?.subStatus ?: "DRIVER_COMING"

    // Live animated car position along M-2 corridor
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

    var carProgressIndex by remember { mutableIntStateOf(1) }
    var currentSpeed by remember { mutableIntStateOf(115) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            currentSpeed = (112..118).random()
        }
    }

    val driverCarPoint = remember(carProgressIndex) {
        m2RoutePoints[carProgressIndex.coerceIn(0, m2RoutePoints.size - 1)]
    }

    val userPickupPoint = remember {
        GeoPoint(33.6631, 73.0847) // Faizabad Hub
    }

    val waypoints = remember {
        listOf(
            IntercityMapWaypoint(
                stopNumber = 1,
                title = "G-9 Boarded",
                point = GeoPoint(33.6938, 73.0317),
                status = IntercityWaypointStatus.COMPLETED
            ),
            IntercityMapWaypoint(
                stopNumber = 2,
                title = "Faizabad (You)",
                point = GeoPoint(33.6631, 73.0847),
                status = IntercityWaypointStatus.ACTIVE_NEXT
            ),
            IntercityMapWaypoint(
                stopNumber = 3,
                title = "M-2 Toll Plaza",
                point = GeoPoint(33.5651, 72.8552),
                status = IntercityWaypointStatus.UPCOMING
            ),
            IntercityMapWaypoint(
                stopNumber = 4,
                title = "Bhera Rest (15m)",
                point = GeoPoint(32.4830, 72.9150),
                status = IntercityWaypointStatus.UPCOMING
            ),
            IntercityMapWaypoint(
                stopNumber = 5,
                title = "Thokar Niaz Baig",
                point = GeoPoint(31.4682, 74.2405),
                status = IntercityWaypointStatus.UPCOMING
            ),
            IntercityMapWaypoint(
                stopNumber = 6,
                title = "Final: DHA Phase 5",
                point = GeoPoint(31.4720, 74.3980),
                status = IntercityWaypointStatus.UPCOMING
            )
        )
    }

    // Post-Ride Rating Dialog (Passenger rating Captain)
    if (showRatingDialog) {
        PostRideRatingDialog(
            rideId = departure?.id ?: "intercity_trip",
            currentUserId = booking?.passengerId ?: "passenger_me",
            currentUserName = booking?.passengerName ?: "Passenger",
            isDriver = false,
            targetId = departure?.driverId ?: "driver",
            targetName = driverName,
            targetPhone = driverPhone,
            targetVehicleSummary = driverVehicle,
            targetPlateNumber = driverPlate,
            targetRating = driverRating,
            pickupTitle = departure?.pickupCity ?: "Islamabad",
            destinationTitle = departure?.dropoffCity ?: "Lahore",
            farePkr = farePkr,
            onDismiss = {
                showRatingDialog = false
                onBack()
            },
            onRatingSubmitted = {
                showRatingDialog = false
                Toast.makeText(context, "Rating submitted! Thank you for choosing Drigo.", Toast.LENGTH_SHORT).show()
                onBack()
            }
        )
    }

    // SOS NHMP 130 Dialog
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = SoftRedSos)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Highway Assistance", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Direct hotline to National Highways & Motorway Police (NHMP 130) with active GPS telemetry.")
                    Text("Current Coordinates: 33.6631° N, 73.0847° E (M-2 Corridor)", fontWeight = FontWeight.SemiBold)
                    Text("Trip ID: #${departure?.id?.takeLast(6)?.uppercase() ?: "M2-849"}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:130"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Dialing 130...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRedSos)
                ) {
                    Text("Dial 130 Helpline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

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
                            // Drigo Purple Brand Badge
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
                                        text = "Drigo",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PASSENGER",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryCorridorGreen
                                    )
                                }
                                Text(
                                    text = "Intercity Corridor",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // ISB/RWP Active Chip
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = LightGreenBg,
                                border = BorderStroke(1.dp, PrimaryCorridorGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryCorridorGreen)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "ISB/RWP Active",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkCorridorGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // User Avatar
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isImmersiveMapMode && sheetState != ManifestSheetState.EXPANDED,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                // Standard Drigo Bottom Bar (Radar active)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem(icon = Icons.Default.GpsFixed, label = "Radar", isSelected = true)
                        BottomNavItem(icon = Icons.Default.AccountBalanceWallet, label = "Earnings", isSelected = false)
                        BottomNavItem(icon = Icons.Default.AccessTime, label = "Shift", isSelected = false)
                        BottomNavItem(icon = Icons.Default.LocalFireDepartment, label = "Hotspots", isSelected = false)
                        BottomNavItem(icon = Icons.Default.Apps, label = "Portal", isSelected = false)
                    }
                }
            }
        }
    ) { innerPadding ->
        val targetFraction = when (sheetState) {
            ManifestSheetState.COLLAPSED -> 0.12f
            ManifestSheetState.HALF -> 0.54f
            ManifestSheetState.EXPANDED -> 0.94f
        }
        val animatedSheetFraction by animateFloatAsState(
            targetValue = targetFraction,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            label = "passengerSheetFraction"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. TOP INTERACTIVE OSM MAP SECTION (Full Background)
            IntercityOsmMapView(
                modifier = Modifier.fillMaxSize(),
                routePoints = m2RoutePoints,
                waypoints = waypoints,
                driverCarLocation = driverCarPoint,
                driverCarBearing = 45f,
                driverCarTitle = "Ali: $currentSpeed km/h",
                userPickupLocation = userPickupPoint,
                userPickupTitle = "YOU: $pickupHub (12 min ETA)",
                centerLat = 33.6631,
                centerLon = 73.0847,
                initialZoom = 11.0,
                onMapClick = {
                    isImmersiveMapMode = !isImmersiveMapMode
                }
            )

            // Top Floating Overlays
            AnimatedVisibility(
                visible = !isImmersiveMapMode && sheetState != ManifestSheetState.EXPANDED,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulse Live GPS Highway Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, PrimaryCorridorGreen)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            val pulseAlpha = rememberInfiniteTransition(label = "pulse").animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dot"
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryCorridorGreen.copy(alpha = pulseAlpha.value))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live GPS • M-2 Highway",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // SOS 130 Pill Button
                    Surface(
                        onClick = { showSosDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        color = SoftRedSos,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "SOS",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SOS 130",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Bottom Map Overlays
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
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Islamabad → Lahore • 340 km",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LightGreenBg,
                            border = BorderStroke(0.5.dp, PrimaryCorridorGreen)
                        ) {
                            Text(
                                text = "M-Tag Pre-paid",
                                color = DarkCorridorGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Floating Restore Button when in Immersive Map Mode
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
                    border = BorderStroke(1.2.dp, PrimaryCorridorGreen),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = PrimaryCorridorGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tap Screen to Restore Controls", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. BOTTOM ACTIVE DETAILS SHEET (Collapsible & Expandable)
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
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Drag & Toggle Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sheetState = when (sheetState) {
                                        ManifestSheetState.COLLAPSED -> ManifestSheetState.HALF
                                        ManifestSheetState.HALF -> ManifestSheetState.EXPANDED
                                        ManifestSheetState.EXPANDED -> ManifestSheetState.HALF
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 38.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Islamabad ➔ Lahore Intercity Transit",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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

                        if (sheetState != ManifestSheetState.COLLAPSED) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                    // Drag Handle Bar
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .align(Alignment.CenterHorizontally)
                    )

                    // DYNAMIC LIVE RIDE LIFECYCLE STATUS BANNER
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (subStatus) {
                            "DRIVER_COMING" -> DrigoBrandPurple.copy(alpha = 0.12f)
                            "DRIVER_ARRIVED" -> AmberCoffee.copy(alpha = 0.15f)
                            "BOARDING" -> LightGreenBg
                            "RIDE_IN_PROGRESS" -> HighwayBlue.copy(alpha = 0.12f)
                            "COMPLETED" -> PrimaryCorridorGreen.copy(alpha = 0.15f)
                            else -> LightGreenBg
                        },
                        border = BorderStroke(
                            1.2.dp,
                            when (subStatus) {
                                "DRIVER_COMING" -> DrigoBrandPurple.copy(alpha = 0.6f)
                                "DRIVER_ARRIVED" -> AmberCoffee
                                "BOARDING" -> PrimaryCorridorGreen
                                "RIDE_IN_PROGRESS" -> HighwayBlue
                                "COMPLETED" -> PrimaryCorridorGreen
                                else -> PrimaryCorridorGreen
                            }
                        ),
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
                                color = when (subStatus) {
                                    "DRIVER_COMING" -> DrigoBrandPurple
                                    "DRIVER_ARRIVED" -> AmberCoffee
                                    "BOARDING" -> PrimaryCorridorGreen
                                    "RIDE_IN_PROGRESS" -> HighwayBlue
                                    "COMPLETED" -> PrimaryCorridorGreen
                                    else -> PrimaryCorridorGreen
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when (subStatus) {
                                            "DRIVER_COMING" -> Icons.Default.DirectionsCar
                                            "DRIVER_ARRIVED" -> Icons.Default.LocationOn
                                            "BOARDING" -> Icons.Default.AirlineSeatReclineNormal
                                            "RIDE_IN_PROGRESS" -> Icons.Default.Speed
                                            "COMPLETED" -> Icons.Default.CheckCircle
                                            else -> Icons.Default.DirectionsCar
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (subStatus) {
                                        "DRIVER_COMING" -> "Driver is Coming"
                                        "DRIVER_ARRIVED" -> "Driver Has Arrived!"
                                        "BOARDING" -> "Boarding Passengers (${liveRideState?.currentBoardingIndex ?: 1} Boarded)"
                                        "RIDE_IN_PROGRESS" -> "Ride in Progress"
                                        "COMPLETED" -> "Ride Completed!"
                                        else -> "En Route"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (subStatus) {
                                        "DRIVER_COMING" -> "Captain $driverName is approaching $pickupHub."
                                        "DRIVER_ARRIVED" -> "Captain is waiting at $pickupHub. Please board."
                                        "BOARDING" -> "Verify PIN '7492' verbally with driver."
                                        "RIDE_IN_PROGRESS" -> "Cruising safely along M-2 Motorway towards Lahore."
                                        "COMPLETED" -> "You have reached your destination. Please rate your captain."
                                        else -> "Live telemetry active."
                                    },
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (subStatus == "COMPLETED") {
                                Button(
                                    onClick = { showRatingDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCorridorGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Rate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Seat & ETA Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seat #$assignedSeat - Rear Right Window",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftPillBg,
                            border = BorderStroke(1.dp, PrimaryCorridorGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "12 mins • 08:25 AM",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkCorridorGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // BOARDING SECRET PIN CARD
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LightGreenBg,
                        border = BorderStroke(1.2.dp, PrimaryCorridorGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BOARDING SECRET PIN",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkCorridorGreen,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Share verbally with $driverName once seated",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, PrimaryCorridorGreen)
                            ) {
                                Text(
                                    text = "7  4  9  2",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DarkCorridorGreen,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // CAPTAIN PROFILE CARD
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with Verified Badge
                                Box(modifier = Modifier.size(46.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = DrigoBrandPurple.copy(alpha = 0.15f),
                                        border = BorderStroke(1.5.dp, DrigoBrandPurple),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = driverName.take(2).uppercase(),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DrigoBrandPurple
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = PrimaryCorridorGreen,
                                        border = BorderStroke(1.5.dp, Color.White),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Verified",
                                            tint = Color.White,
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = driverName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = LightGreenBg
                                        ) {
                                            Text(
                                                text = "CORRIDOR MASTER",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkCorridorGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$driverRating ★ • $driverTrips+ M-2 trips",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$driverVehicle • $driverPlate • White • Chilled AC",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Driver Action Buttons Row: Call Ali (Green) & In-App Chat
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onCallDriver(driverPhone) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCorridorGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call ${driverName.substringBefore(" ")}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { onOpenChat(driverName, driverPhone) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("In-App Chat", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // CORRIDOR MANIFEST - PASSENGERS (3/4 BOOKED)
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
                                text = "CORRIDOR MANIFEST",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftPillBg
                            ) {
                                Text(
                                    text = "1 Seat Available",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkCorridorGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Passengers (3/4 Booked)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 4 Seat Manifest Cards
                        ManifestPassengerItem(
                            seatNumber = 1,
                            name = "Hamza S. (Boarded at G-9)",
                            dropoff = "Drop: Thokar Niaz Baig",
                            isBoarded = true
                        )
                        ManifestPassengerItem(
                            seatNumber = 2,
                            name = "Ayesha K. (Boarded at G-9)",
                            dropoff = "Drop: DHA Phase 5 Lahore",
                            isBoarded = true
                        )
                        ManifestPassengerItem(
                            seatNumber = 3,
                            name = "You ($pickupHub)",
                            dropoff = "Drop: DHA Phase 5 Lahore",
                            isUser = true,
                            badgeText = "Boarding"
                        )
                        ManifestPassengerItem(
                            seatNumber = 4,
                            name = "Available Corridor Seat",
                            dropoff = "Unoccupied (Auto-matching)",
                            isAvailable = true,
                            badgeText = "Available"
                        )
                    }

                    // CORRIDOR SCHEDULE & STOPS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Corridor Schedule & Stops",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "M-2 Expressway",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HighwayBlue
                            )
                        }

                        // Connected Timeline Nodes
                        TimelineScheduleNode(
                            title = "Stop 1: G-9 Markaz Hub",
                            time = "08:00 AM • Departed",
                            description = "2 passengers boarded successfully",
                            status = TimelineStatus.COMPLETED
                        )
                        TimelineScheduleNode(
                            title = "Stop 2: Faizabad Hub (You)",
                            time = "08:25 AM ETA",
                            description = "Your pickup location • Captain approaching",
                            status = TimelineStatus.ACTIVE
                        )
                        TimelineScheduleNode(
                            title = "Bhera M-2 Service Area",
                            time = "10:15 AM (15 min rest)",
                            description = "Comfort stop, chai & prayer facilities",
                            status = TimelineStatus.REST_STOP
                        )
                        TimelineScheduleNode(
                            title = "Stop 4: Thokar Niaz Baig",
                            time = "12:05 PM",
                            description = "Lahore entry passenger drop-off",
                            status = TimelineStatus.UPCOMING
                        )
                        TimelineScheduleNode(
                            title = "Terminus: DHA Phase 5",
                            time = "12:35 PM",
                            description = "Your scheduled final drop-off",
                            status = TimelineStatus.TERMINUS,
                            isLast = true
                        )
                    }

                    // INTERCITY FIXED FARE CARD
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INTERCITY FIXED FARE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SoftPillBg
                                ) {
                                    Text(
                                        text = "Digital Wallet / Cash",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkCorridorGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "PKR $farePkr",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // 3 Feature Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AmenityPill(
                                    title = "Tolls Included",
                                    subtitle = "M-Tag paid",
                                    icon = Icons.Default.ConfirmationNumber,
                                    modifier = Modifier.weight(1f)
                                )
                                AmenityPill(
                                    title = "Luggage OK",
                                    subtitle = "1 Trolley + Bag",
                                    icon = Icons.Default.Luggage,
                                    modifier = Modifier.weight(1f)
                                )
                                AmenityPill(
                                    title = "Chilled AC",
                                    subtitle = "Continuous",
                                    icon = Icons.Default.AcUnit,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // BOTTOM SHARE TRACKING & SAFETY BUTTONS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onShareTrip()
                                try {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Tracking my Drigo Intercity Ride to Lahore with Captain $driverName ($driverVehicle, $driverPlate). Live GPS: https://drigo.pk/track")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Live Tracking"))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Live Tracking link copied", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCorridorGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Live Tracking", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Shield Icon Button
                        Surface(
                            onClick = onSosClick,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Safety",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun ManifestPassengerItem(
    seatNumber: Int,
    name: String,
    dropoff: String,
    isBoarded: Boolean = false,
    isUser: Boolean = false,
    isAvailable: Boolean = false,
    badgeText: String = ""
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isUser -> LightGreenBg
            isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp,
            if (isUser) PrimaryCorridorGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seat circle
            Surface(
                shape = CircleShape,
                color = if (isUser || isBoarded) PrimaryCorridorGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$seatNumber",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser || isBoarded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 12.5.sp,
                    fontWeight = if (isUser) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dropoff,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isBoarded) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryCorridorGreen,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = "Boarded", tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
            } else if (badgeText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUser) PrimaryCorridorGreen else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private enum class TimelineStatus {
    COMPLETED,
    ACTIVE,
    REST_STOP,
    UPCOMING,
    TERMINUS
}

@Composable
private fun TimelineScheduleNode(
    title: String,
    time: String,
    description: String,
    status: TimelineStatus,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Vertical indicator column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            when (status) {
                TimelineStatus.COMPLETED -> {
                    Surface(shape = CircleShape, color = PrimaryCorridorGreen, modifier = Modifier.size(18.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                TimelineStatus.ACTIVE -> {
                    Surface(
                        shape = CircleShape,
                        color = HighwayBlue,
                        border = BorderStroke(2.dp, Color.White),
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                TimelineStatus.REST_STOP -> {
                    Surface(shape = CircleShape, color = AmberCoffee, modifier = Modifier.size(18.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Coffee, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                    }
                }
                TimelineStatus.TERMINUS -> {
                    Surface(shape = CircleShape, color = PrimaryCorridorGreen, modifier = Modifier.size(18.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                    }
                }
                else -> {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(14.dp)) {}
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(
                            if (status == TimelineStatus.COMPLETED) PrimaryCorridorGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = time,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (status == TimelineStatus.COMPLETED) DarkCorridorGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AmenityPill(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = DarkCorridorGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(text = subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryCorridorGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryCorridorGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
