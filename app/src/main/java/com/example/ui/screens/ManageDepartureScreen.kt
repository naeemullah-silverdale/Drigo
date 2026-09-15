package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IntercityRouteOptimizer
import com.example.data.model.IntercityManifestUiState
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.model.PlannedDepartureOffer
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.PassengerList
import com.example.ui.components.PassengerOfferCard
import com.example.ui.theme.DrigoBrandPurple
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

private val MintGreen = Color(0xFF00C853)
private val DarkGreen = Color(0xFF00897B)
private val LightMintBg = Color(0xFFE8F8EE)
private val AmberTag = Color(0xFFFF9800)
private val LightAmberBg = Color(0xFFFFF3E0)
private val SoftRedBg = Color(0xFFFFEBEE)
private val DarkRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDepartureScreen(
    departureId: String,
    initialDeparture: PlannedDeparture? = null,
    onBack: () -> Unit,
    initialTab: Int = 0,
    onSosClick: () -> Unit = {},
    onStartActiveRide: (PlannedDeparture) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }

    LaunchedEffect(departureId, initialDeparture) {
        initialDeparture?.let { repo.cachePlannedDeparture(it) }
        android.util.Log.d("DrigoManageDeparture", "ManageDepartureScreen initialized for departureId='$departureId', initialTab=$initialTab, initialRoute=${initialDeparture?.pickupCity}->${initialDeparture?.dropoffCity}")
    }

    // Live departure observation (specific ID observer + fallback)
    val liveDepartureFromRepo by remember(departureId) { repo.observeDepartureById(departureId) }.collectAsState(initial = initialDeparture ?: repo.getDepartureById(departureId))
    var departure by remember(departureId) {
        mutableStateOf(
            initialDeparture
                ?: repo.getDepartureById(departureId)
                ?: PlannedDeparture(id = departureId)
        )
    }

    LaunchedEffect(liveDepartureFromRepo) {
        liveDepartureFromRepo?.let {
            departure = it
            android.util.Log.d("DrigoManageDeparture", "Live departure updated for id='$departureId': Route=${it.pickupCity}->${it.dropoffCity}, Status=${it.status}, Fare=PKR ${it.farePerSeat}, Offers=${it.offersReceivedCount}")
        }
    }

    val allDepartures by remember { repo.observeAllPlannedDepartures() }.collectAsState(initial = emptyList())
    LaunchedEffect(allDepartures, departureId) {
        val found = allDepartures.find { it.id == departureId } ?: repo.getDepartureById(departureId)
        if (found != null && (departure.pickupCity.isBlank() || departure.farePerSeat == 0)) {
            android.util.Log.d("DrigoManageDeparture", "Loaded departure from allDepartures for id='$departureId': Route=${found.pickupCity}->${found.dropoffCity}")
            departure = found
        }
    }

    // Live bookings & offers
    val bookings by remember(departureId) { repo.observeDepartureBookings(departureId) }.collectAsState(initial = emptyList())
    val offers by remember(departureId) { repo.observeDepartureOffers(departureId) }.collectAsState(initial = emptyList())

    // Merged list of confirmed bookings and accepted passenger offers
    val confirmedRiders = remember(bookings, offers) {
        val list = mutableListOf<PlannedDepartureBooking>()
        list.addAll(bookings)
        val acceptedOffers = offers.filter { it.status.equals("ACCEPTED", true) }
        for (offer in acceptedOffers) {
            if (list.none { (it.passengerId.isNotBlank() && it.passengerId == offer.passengerId) || it.id == offer.id || it.id == "bk_${offer.id}" }) {
                list.add(
                    PlannedDepartureBooking(
                        id = if (offer.id.isNotBlank()) "bk_${offer.id}" else java.util.UUID.randomUUID().toString(),
                        departureId = departureId,
                        passengerId = offer.passengerId,
                        passengerName = offer.passengerName,
                        passengerPhone = offer.passengerPhone,
                        passengerRating = offer.passengerRating,
                        seatsBooked = offer.requestedSeats.coerceAtLeast(1),
                        pickupStop = offer.pickupPoint,
                        pickupLat = offer.pickupLat,
                        pickupLon = offer.pickupLon,
                        dropoffStop = offer.dropoffPoint,
                        dropoffLat = offer.dropoffLat,
                        dropoffLon = offer.dropoffLon,
                        isFullCar = (offer.bookingType == "PRIVATE"),
                        totalFarePkr = offer.offeredFare,
                        status = "CONFIRMED",
                        bookedAt = offer.createdAt
                    )
                )
            }
        }
        list
    }

    LaunchedEffect(bookings.size, confirmedRiders.size) {
        android.util.Log.d("DrigoManageDeparture", "Received ${bookings.size} direct bookings, total ${confirmedRiders.size} confirmed riders for departureId='$departureId'")
    }

    LaunchedEffect(offers.size) {
        android.util.Log.d("DrigoManageDeparture", "Received ${offers.size} passenger offers for departureId='$departureId'")
        offers.forEachIndexed { idx, off ->
            android.util.Log.d("DrigoManageDeparture", "  [$idx] Offer ID=${off.id}, Rider=${off.passengerName}, Seats=${off.requestedSeats}, Offered=PKR ${off.offeredFare}, Status=${off.status}")
        }
    }

    // Live Intercity Manifest State (Geographically sorted Pickups heading to highway & Drop-offs from highway exit)
    var manifestState by remember {
        mutableStateOf(
            IntercityManifestUiState(
                departureId = departureId,
                originCity = departure.pickupCity,
                destinationCity = departure.dropoffCity
            )
        )
    }

    LaunchedEffect(departure, confirmedRiders, offers) {
        try {
            val acceptedOffers = offers.filter { it.status.equals("ACCEPTED", true) }
            android.util.Log.d("DrigoManageDeparture", "Generating optimized manifest with ${confirmedRiders.size} confirmed riders and ${acceptedOffers.size} accepted offers...")
            val result = IntercityRouteOptimizer.generateOptimizedManifest(
                departure = departure,
                confirmedBookings = confirmedRiders,
                acceptedOffers = acceptedOffers
            )
            manifestState = result
            android.util.Log.d("DrigoManageDeparture", "Manifest generated successfully: Total Stops=${result.allOrderedStops.size}, Distance=${result.totalDistanceKm}km, Duration=${result.totalDurationMinutes}m, Earnings=PKR ${result.totalManifestEarningsPkr}")
        } catch (e: Exception) {
            android.util.Log.e("DrigoManageDeparture", "Manifest generation error for departureId='$departureId': ${e.message}", e)
        }
    }

    // Tabs: 0 -> Departure Details, 1 -> Passenger Offers, 2 -> Route Manifest
    var selectedTab by remember { mutableIntStateOf(if (initialTab in 0..2) initialTab else 0) }

    // Editable states in Quick Adjustments
    var selectedDateText by remember(departure.departureDateText) { mutableStateOf(departure.departureDateText) }
    var selectedTimeText by remember(departure.departureTimeText) { mutableStateOf(departure.departureTimeText) }
    var currentFarePerSeat by remember(departure.farePerSeat) { mutableIntStateOf(departure.farePerSeat) }
    var isFlexWindowEnabled by remember(departure.flexWindowMins) { mutableStateOf(departure.flexWindowMins > 0) }

    // Dialog states
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var counterOfferTarget by remember { mutableStateOf<PlannedDepartureOffer?>(null) }
    var counterPriceInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Origin and Destination acronyms for header
    val fromAcronym = remember(departure.pickupCity) {
        when {
            departure.pickupCity.contains("Islamabad", true) -> "ISB"
            departure.pickupCity.contains("Lahore", true) -> "LHE"
            departure.pickupCity.contains("Rawalpindi", true) -> "RWP"
            departure.pickupCity.contains("Karachi", true) -> "KHI"
            departure.pickupCity.contains("Peshawar", true) -> "PEW"
            departure.pickupCity.contains("Faisalabad", true) -> "FSD"
            departure.pickupCity.length >= 3 -> departure.pickupCity.take(3).uppercase()
            else -> "DEP"
        }
    }
    val toAcronym = remember(departure.dropoffCity) {
        when {
            departure.dropoffCity.contains("Lahore", true) -> "LHE"
            departure.dropoffCity.contains("Islamabad", true) -> "ISB"
            departure.dropoffCity.contains("Rawalpindi", true) -> "RWP"
            departure.dropoffCity.contains("Karachi", true) -> "KHI"
            departure.dropoffCity.contains("Peshawar", true) -> "PEW"
            departure.dropoffCity.contains("Faisalabad", true) -> "FSD"
            departure.dropoffCity.length >= 3 -> departure.dropoffCity.take(3).uppercase()
            else -> "ARR"
        }
    }

    val rideShortCode = remember(departure.id) {
        if (departure.id.contains("mock")) "PL902" else departure.id.takeLast(5).uppercase()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Manage Departure",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$fromAcronym → $toAcronym • Ride #DR-$rideShortCode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // SOS Pill Badge
                    Surface(
                        onClick = onSosClick,
                        shape = RoundedCornerShape(20.dp),
                        color = SoftRedBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkRed.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(DarkRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SOS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkRed
                            )
                        }
                    }

                    // Driver Profile Avatar with green online dot
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DrigoBrandPurple.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, DrigoBrandPurple.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = departure.driverName.take(2).uppercase().ifBlank { "CF" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrigoBrandPurple
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MintGreen)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Banner: "ACTIVE DEPARTURE" and "Booking Open"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (departure.status == "ACTIVE") MintGreen else AmberTag)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (departure.status == "ACTIVE") "ACTIVE DEPARTURE" else "${departure.status} DEPARTURE",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (departure.status == "ACTIVE") DarkGreen else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (departure.availableSeats > 0) LightMintBg else SoftRedBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (departure.availableSeats > 0) MintGreen.copy(alpha = 0.4f) else DarkRed.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (departure.availableSeats > 0) "Booking Open" else "Fully Booked",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (departure.availableSeats > 0) DarkGreen else DarkRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            // Segmented Tabs: Departure Details, Passenger Offers & Route Manifest
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp)
                ) {
                    // Departure Details Tab
                    Surface(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (selectedTab == 0) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 2.dp)) {
                            Text(
                                text = "Details",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Passenger Offers Tab
                    Surface(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (selectedTab == 1) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Offers",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (offers.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MintGreen,
                                    modifier = Modifier.size(17.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = offers.size.toString(),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Route Manifest Tab
                    val totalStopsCount = manifestState.phase1Pickups.size + manifestState.phase2Dropoffs.size
                    Surface(
                        onClick = { selectedTab = 2 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 2) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (selectedTab == 2) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1.1f)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Manifest",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 2) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (totalStopsCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkGreen,
                                    modifier = Modifier.height(17.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                        Text(
                                            text = "$totalStopsCount",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Content
            AnimatedContent(
                targetState = selectedTab,
                label = "ManageDepartureTabAnimation"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // DEPARTURE DETAILS TAB
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // BANNER: Pending Passenger Offers Alert (Prominent 1-tap view)
                            if (offers.isNotEmpty()) {
                                item {
                                    Surface(
                                        onClick = { selectedTab = 1 },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocalOffer,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "${offers.size} Pending Passenger ${if (offers.size == 1) "Offer" else "Offers"}",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = "Tap to review, accept, decline, or counter",
                                                        fontSize = 11.5.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "Review",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // CARD 1: Scheduled Departure & Route Details
                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        // Top row with green accent bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = LightMintBg,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.DateRange,
                                                            contentDescription = null,
                                                            tint = MintGreen,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "Scheduled Departure",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "$selectedDateText • $selectedTimeText",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Surface(
                                                onClick = { showTimePickerDialog = true },
                                                shape = RoundedCornerShape(12.dp),
                                                color = LightMintBg,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MintGreen.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = "Edit Time",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkGreen,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                        // Timeline: Pickup -> Dropoff
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            // Pickup Point
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 4.dp)
                                                        .size(12.dp)
                                                        .border(3.dp, MintGreen, CircleShape)
                                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${departure.pickupCity} (${departure.pickupHub})",
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = selectedTimeText,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                    Text(
                                                        text = "Pickup: ${departure.pickupStopDetails}",
                                                        fontSize = 11.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            // Connector Line
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 5.dp, top = 2.dp, bottom = 2.dp)
                                                    .width(2.dp)
                                                    .height(20.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                            )

                                            // Dropoff Point
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 4.dp)
                                                        .size(12.dp)
                                                        .background(AmberTag, RoundedCornerShape(3.dp))
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${departure.dropoffCity} (${departure.dropoffHub})",
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = calculateEstimatedArrival(selectedTimeText, 270),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                    Text(
                                                        text = "Dropoff: ${departure.dropoffStopDetails}",
                                                        fontSize = 11.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // Badges: Distance + Tolls Pre-cleared + Vehicle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = departure.corridorSubtitle,
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            if (departure.tollsPreCleared) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = LightMintBg
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = DarkGreen
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Tolls Pre-cleared",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = DarkGreen,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Vehicle tag
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE3F2FD),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${departure.driverVehicle} (${departure.driverPlateNumber})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF1565C0),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Bottom Fare & Capacity Box
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Fare per seat
                                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(
                                                        text = "FARE PER SEAT",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "PKR $currentFarePerSeat",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DarkGreen
                                                    )
                                                    if (departure.allowFullCarBuyout) {
                                                        Text(
                                                            text = "Buyout PKR ${departure.fullCarFare} Full Car",
                                                            fontSize = 9.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Seat capacity
                                                Column(
                                                    horizontalAlignment = Alignment.End,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Text(
                                                        text = "SEAT CAPACITY",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    // Visual seat dot indicators
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        for (i in 1..departure.totalSeats) {
                                                            val isBooked = i <= (departure.totalSeats - departure.availableSeats)
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(9.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isBooked) MintGreen else MaterialTheme.colorScheme.outlineVariant)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${departure.totalSeats - departure.availableSeats}/${departure.totalSeats} Booked (${departure.availableSeats} Left)",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // CARD 2: QUICK ADJUSTMENTS (Auto-synced)
                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        // Header
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
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = null,
                                                    tint = DarkGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "QUICK ADJUSTMENTS",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Auto-synced",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Departure Date & Time Dropdown Selectors
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Date selector
                                            Surface(
                                                onClick = { showDatePickerDialog = true },
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                                    Text(
                                                        text = "DEPARTURE DATE",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = selectedDateText,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowDown,
                                                            contentDescription = null,
                                                            tint = DarkGreen,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Time selector
                                            Surface(
                                                onClick = { showTimePickerDialog = true },
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                                    Text(
                                                        text = "DEPARTURE TIME",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = selectedTimeText,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowDown,
                                                            contentDescription = null,
                                                            tint = DarkGreen,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Fare per Seat Adjustment
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(
                                                        text = "Fare per Seat",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "Current: PKR $currentFarePerSeat",
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        onClick = {
                                                            if (currentFarePerSeat > 500) currentFarePerSeat -= 100
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        modifier = Modifier.size(width = 40.dp, height = 32.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "-100",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = LightMintBg,
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, MintGreen.copy(alpha = 0.4f)),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier.padding(horizontal = 8.dp)
                                                        ) {
                                                            Text(
                                                                text = "%,d".format(currentFarePerSeat),
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = DarkGreen
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        onClick = {
                                                            currentFarePerSeat += 100
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        modifier = Modifier.size(width = 40.dp, height = 32.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "+100",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Flexible Departure Window Switch
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Flexible Departure Window",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Allows ±15 mins pickup tolerance",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }

                                            Switch(
                                                checked = isFlexWindowEnabled,
                                                onCheckedChange = { isFlexWindowEnabled = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = MintGreen,
                                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // CARD 3: CONFIRMED PASSENGERS (Material 3 Realtime List)
                            item {
                                PassengerList(
                                    confirmedRiders = confirmedRiders,
                                    totalCapacitySeats = departure.availableSeats + confirmedRiders.sumOf { it.seatsBooked },
                                    onViewRouteManifest = { selectedTab = 2 },
                                    stopNumberProvider = { booking, index ->
                                        val pickupWp = manifestState.phase1Pickups.find {
                                            it.bookingId == booking.id || it.passengerName.equals(booking.passengerName, true)
                                        }
                                        if (pickupWp != null) "Stop ${pickupWp.stopNumber}" else "Stop ${index + 1}"
                                    },
                                    onCall = { booking ->
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking.passengerPhone}"))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Calling ${booking.passengerName} (${booking.passengerPhone})", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onMessage = { booking ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${booking.passengerPhone}"))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Messaging ${booking.passengerName}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            // CARD 4: SAVE CHANGES & CANCEL DEPARTURE ACTIONS (Screenshot 4)
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Start Ride Button
                                    val isTripActive = departure.status.equals("ACTIVE", true)
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                repo.startIntercityRide(
                                                    departure = departure,
                                                    confirmedBookings = confirmedRiders,
                                                    acceptedOffers = offers.filter { it.status.equals("ACCEPTED", true) }
                                                )
                                                onStartActiveRide(departure.copy(status = "ACTIVE"))
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTripActive) Color(0xFF00C853) else Color(0xFF0F172A),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isTripActive) Icons.Default.Navigation else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isTripActive) "Start Ride • In Progress" else "Start Ride",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Save Changes Button (Vibrant Green)
                                    Button(
                                        onClick = {
                                            isSaving = true
                                            coroutineScope.launch {
                                                repo.updateDepartureSchedule(
                                                    departureId = departureId,
                                                    dateText = selectedDateText,
                                                    timeText = selectedTimeText,
                                                    farePerSeat = currentFarePerSeat,
                                                    flexTolerance = isFlexWindowEnabled
                                                )
                                                isSaving = false
                                                Toast.makeText(context, "Schedule changes saved successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MintGreen,
                                            contentColor = Color.White
                                        ),
                                        enabled = !isSaving,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Save Changes",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Cancel This Departure Button (Light Red Background)
                                    Button(
                                        onClick = { showCancelConfirmDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SoftRedBg,
                                            contentColor = DarkRed
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = DarkRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Cancel This Departure",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // PASSENGER OFFERS TAB (Screenshot 3)
                        if (offers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No Pending Offers",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Rider booking and fare counter-offers will appear here in real-time.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 40.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(offers, key = { it.id }) { offer ->
                                    PassengerOfferCard(
                                        offer = offer,
                                        onAccept = {
                                            coroutineScope.launch {
                                                repo.acceptDepartureOffer(departureId, offer.id)
                                                Toast.makeText(context, "Offer from ${offer.passengerName} accepted!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onDecline = {
                                            coroutineScope.launch {
                                                repo.declineDepartureOffer(departureId, offer.id)
                                                Toast.makeText(context, "Offer declined.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onCounter = {
                                            counterOfferTarget = offer
                                            counterPriceInput = (offer.offeredFare + 100).toString()
                                        },
                                        onMessage = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${offer.passengerPhone}"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Messaging ${offer.passengerName}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onCall = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${offer.passengerPhone}"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Calling ${offer.passengerName} (${offer.passengerPhone})", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // ROUTE MANIFEST / INTERCITY MANIFEST TAB
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Quick Action: Launch Live Highway Radar & Active Manifest (Attachment 2)
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        if (!departure.status.equals("ACTIVE", true)) {
                                            repo.updatePlannedDepartureStatus(departureId, "ACTIVE")
                                        }
                                        onStartActiveRide(departure.copy(status = "ACTIVE"))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00C853),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Launch Live Highway Radar & Manifest",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            IntercityManifestContent(
                                manifestState = manifestState,
                                departure = departure,
                                onRecalculateRoute = {
                                    coroutineScope.launch {
                                        manifestState = manifestState.copy(isOptimizingRoute = true)
                                        manifestState = IntercityRouteOptimizer.generateOptimizedManifest(
                                            departure = departure,
                                            confirmedBookings = bookings,
                                            acceptedOffers = offers.filter { it.status.equals("ACCEPTED", true) }
                                        )
                                    }
                                },
                                offers = offers,
                                onAcceptOffer = { offer ->
                                    coroutineScope.launch {
                                        repo.acceptDepartureOffer(departureId, offer.id)
                                        Toast.makeText(context, "Offer from ${offer.passengerName} accepted!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDeclineOffer = { offer ->
                                    coroutineScope.launch {
                                        repo.declineDepartureOffer(departureId, offer.id)
                                        Toast.makeText(context, "Offer declined.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCounterOffer = { offer ->
                                    counterOfferTarget = offer
                                    counterPriceInput = (offer.offeredFare + 100).toString()
                                },
                                onMessagePassenger = { offer ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${offer.passengerPhone}"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Messaging ${offer.passengerName}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCallPassenger = { offer ->
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${offer.passengerPhone}"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Calling ${offer.passengerName} (${offer.passengerPhone})", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancel Departure Dialog
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = "Cancel This Departure?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel this scheduled trip ($fromAcronym → $toAcronym)? Any confirmed passengers and offers will be notified.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        coroutineScope.launch {
                            repo.cancelPlannedDeparture(departureId, departure.driverId)
                            Toast.makeText(context, "Departure cancelled successfully", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed, contentColor = Color.White)
                ) {
                    Text("Yes, Cancel Trip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Keep Active")
                }
            }
        )
    }

    // Counter Offer Dialog
    counterOfferTarget?.let { targetOffer ->
        AlertDialog(
            onDismissRequest = { counterOfferTarget = null },
            title = {
                Text(
                    text = "Counter Offer to ${targetOffer.passengerName}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Passenger offered PKR %,d for %d seat(s). Asking fare was PKR %,d.".format(
                            targetOffer.offeredFare,
                            targetOffer.requestedSeats,
                            targetOffer.standardAsking
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = counterPriceInput,
                        onValueChange = { counterPriceInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Counter Fare (PKR)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val counterVal = counterPriceInput.toIntOrNull() ?: targetOffer.offeredFare
                        counterOfferTarget = null
                        coroutineScope.launch {
                            repo.counterDepartureOffer(departureId, targetOffer.id, counterVal)
                            Toast.makeText(context, "Counter-offer of PKR $counterVal sent!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = Color.White)
                ) {
                    Text("Send Counter")
                }
            },
            dismissButton = {
                TextButton(onClick = { counterOfferTarget = null }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Quick Date Selection Dialog
    if (showDatePickerDialog) {
        val dateOptions = remember {
            val sdf = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
            (0..4).map { offset ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                val prefix = when (offset) {
                    0 -> "Today, "
                    1 -> "Tomorrow, "
                    else -> ""
                }
                "$prefix${sdf.format(cal.time)}"
            }
        }
        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            title = {
                Text("Select Departure Date", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    dateOptions.forEach { opt ->
                        Surface(
                            onClick = {
                                selectedDateText = opt
                                showDatePickerDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDateText == opt) LightMintBg else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt,
                                    fontWeight = if (selectedDateText == opt) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedDateText == opt) DarkGreen else MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedDateText == opt) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quick Time Selection Dialog
    if (showTimePickerDialog) {
        val timeOptions = listOf(
            "06:00 AM",
            "07:00 AM",
            "08:00 AM",
            "09:30 AM",
            "11:00 AM",
            "02:00 PM",
            "04:30 PM",
            "06:00 PM",
            "08:00 PM"
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = {
                Text("Select Departure Time", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    timeOptions.forEach { opt ->
                        Surface(
                            onClick = {
                                selectedTimeText = opt
                                showTimePickerDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTimeText == opt) LightMintBg else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt,
                                    fontWeight = if (selectedTimeText == opt) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTimeText == opt) DarkGreen else MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedTimeText == opt) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Calculates the estimated arrival time based on departure time string and corridor duration minutes.
 */
private fun calculateEstimatedArrival(departureTime: String, durationMinutes: Int = 270): String {
    return try {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val date = sdf.parse(departureTime.trim()) ?: return "~$departureTime"
        val cal = Calendar.getInstance().apply {
            time = date
            add(Calendar.MINUTE, durationMinutes)
        }
        "~" + sdf.format(cal.time)
    } catch (_: Exception) {
        "~$departureTime"
    }
}
