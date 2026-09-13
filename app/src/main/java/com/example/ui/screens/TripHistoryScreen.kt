package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverHistoryItem
import com.example.data.model.PassengerOrder
import com.example.data.model.PassengerOrderStatus
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.InDriveLimeGreen
import com.example.ui.components.PostRideRatingDialog
import com.example.ui.components.SafetyReportDialog
import com.example.ui.theme.DrigoBrandFuchsia
import com.example.ui.theme.DrigoBrandMagentaBg
import com.example.ui.theme.DrigoBrandPurple
import com.example.ui.theme.DrigoBrandPurpleDark
import com.example.ui.theme.drigoColors
import com.example.viewmodel.UserMode
import com.google.firebase.auth.FirebaseUser
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TripHistoryTab {
    PASSENGER,
    DRIVER
}

enum class StatusFilter {
    ALL,
    COMPLETED,
    CANCELLED
}

/**
 * Unified Trip History View for Drigo.
 * Contextually displays completed and past trips from Firestore & Firebase Realtime Database
 * for both Passengers and Drivers with real-time reactive streams, receipt breakdowns,
 * rating submissions, and search/filtering capabilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    user: FirebaseUser?,
    initialUserMode: UserMode = UserMode.PASSENGER,
    onBackClick: () -> Unit,
    onRebookTrip: (PassengerOrder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember(context) { FirebaseRepository.getInstance(context) }
    val userId = user?.uid ?: "user_default"
    val userEmail = user?.email ?: ""
    val userName = user?.displayName ?: userEmail.substringBefore("@").ifBlank { "User" }
    val userPhone = user?.phoneNumber ?: ""

    var selectedHistoryTab by remember {
        mutableStateOf(if (initialUserMode == UserMode.DRIVER) TripHistoryTab.DRIVER else TripHistoryTab.PASSENGER)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf(StatusFilter.ALL) }

    // Dialog & Receipt States
    var selectedPassengerOrderForReceipt by remember { mutableStateOf<PassengerOrder?>(null) }
    var selectedDriverItemForReceipt by remember { mutableStateOf<DriverHistoryItem?>(null) }
    var ratingOrderTarget by remember { mutableStateOf<PassengerOrder?>(null) }
    var ratingDriverTripTarget by remember { mutableStateOf<DriverHistoryItem?>(null) }
    var reportOrderTarget by remember { mutableStateOf<PassengerOrder?>(null) }
    var driverReportTargetItem by remember { mutableStateOf<DriverHistoryItem?>(null) }

    // Passenger trips flow from Firebase (Firestore + RTDB)
    val passengerOrders by repo.listenToPassengerOrders(userId, userEmail).collectAsState(initial = emptyList())
    val pastPassengerOrders = remember(passengerOrders) {
        passengerOrders.filter {
            it.status == PassengerOrderStatus.COMPLETED || it.status == PassengerOrderStatus.CANCELLED
        }
    }

    // Driver trips and verification flow from Firebase
    val driverTrips by repo.observeDriverTripHistory(userId, userPhone).collectAsState(initial = emptyList())
    val driverVerification by repo.listenToDriverVerification(userId).collectAsState(initial = null)

    val isRegisteredDriver = remember(driverVerification, driverTrips, initialUserMode) {
        val verStatus = driverVerification?.status?.trim()?.uppercase() ?: ""
        val isConfirmed = driverVerification?.confirmtion == true
        initialUserMode == UserMode.DRIVER ||
                (verStatus.isNotBlank() && verStatus != "UNREGISTERED" && verStatus != "NOT_REGISTERED") ||
                isConfirmed ||
                driverTrips.isNotEmpty()
    }

    LaunchedEffect(isRegisteredDriver) {
        if (!isRegisteredDriver && selectedHistoryTab == TripHistoryTab.DRIVER) {
            selectedHistoryTab = TripHistoryTab.PASSENGER
        }
    }

    val isDark = MaterialTheme.drigoColors.isDark

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = DrigoBrandPurple.copy(alpha = 0.15f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = DrigoBrandPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Trip History",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (selectedHistoryTab == TripHistoryTab.PASSENGER)
                                            "${pastPassengerOrders.size} rides recorded"
                                        else
                                            "${driverTrips.size} trips recorded",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(44.dp)
                                    .testTag("trip_history_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Home",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Role Selector Segmented Control (Only visible if user is registered as a Driver)
                    if (isRegisteredDriver) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF221A2A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Passenger Tab Button
                                val isPassenger = selectedHistoryTab == TripHistoryTab.PASSENGER
                                val passengerTabBg by animateColorAsState(
                                    targetValue = if (isPassenger) DrigoBrandPurple else Color.Transparent,
                                    animationSpec = tween(200),
                                    label = "passengerTabBg"
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = passengerTabBg,
                                    shadowElevation = if (isPassenger) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedHistoryTab = TripHistoryTab.PASSENGER }
                                        .testTag("history_passenger_tab")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isPassenger) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Passenger (${pastPassengerOrders.size})",
                                            fontWeight = if (isPassenger) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = if (isPassenger) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Driver Tab Button
                                val isDriver = selectedHistoryTab == TripHistoryTab.DRIVER
                                val driverTabBg by animateColorAsState(
                                    targetValue = if (isDriver) DrigoBrandPurple else Color.Transparent,
                                    animationSpec = tween(200),
                                    label = "driverTabBg"
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = driverTabBg,
                                    shadowElevation = if (isDriver) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedHistoryTab = TripHistoryTab.DRIVER }
                                        .testTag("history_driver_tab")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = if (isDriver) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Driver (${driverTrips.size})",
                                            fontWeight = if (isDriver) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = if (isDriver) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Search Bar & Filter Chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = if (selectedHistoryTab == TripHistoryTab.PASSENGER)
                                        "Search pickup, destination, captain..."
                                    else
                                        "Search pickup, dropoff, passenger...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (searchQuery.isNotBlank()) DrigoBrandPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DrigoBrandPurple,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                focusedContainerColor = if (isDark) Color(0xFF1F1826) else MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = if (isDark) Color(0xFF1F1826) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("history_search_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedStatusFilter == StatusFilter.ALL,
                                onClick = { selectedStatusFilter = StatusFilter.ALL },
                                label = { Text("All Trips", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DrigoBrandPurple.copy(alpha = 0.16f),
                                    selectedLabelColor = DrigoBrandPurple
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedStatusFilter == StatusFilter.ALL) DrigoBrandPurple else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("filter_all")
                            )

                            FilterChip(
                                selected = selectedStatusFilter == StatusFilter.COMPLETED,
                                onClick = { selectedStatusFilter = StatusFilter.COMPLETED },
                                label = { Text("Completed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853),
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00C853).copy(alpha = 0.14f),
                                    selectedLabelColor = if (isDark) Color(0xFF69F0AE) else Color(0xFF1B5E20)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedStatusFilter == StatusFilter.COMPLETED) Color(0xFF00C853) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("filter_completed")
                            )

                            FilterChip(
                                selected = selectedStatusFilter == StatusFilter.CANCELLED,
                                onClick = { selectedStatusFilter = StatusFilter.CANCELLED },
                                label = { Text("Cancelled", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEF5350).copy(alpha = 0.14f),
                                    selectedLabelColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedStatusFilter == StatusFilter.CANCELLED) Color(0xFFEF5350) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("filter_cancelled")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        },
        modifier = modifier.testTag("trip_history_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedHistoryTab) {
                TripHistoryTab.PASSENGER -> {
                    PassengerTripHistoryContent(
                        orders = pastPassengerOrders,
                        searchQuery = searchQuery,
                        statusFilter = selectedStatusFilter,
                        onViewReceipt = { selectedPassengerOrderForReceipt = it },
                        onRateDriver = { ratingOrderTarget = it },
                        onReportSafety = { reportOrderTarget = it },
                        onRebookTrip = {
                            onRebookTrip(it)
                            Toast.makeText(context, "Locations pre-filled for rebooking!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                TripHistoryTab.DRIVER -> {
                    DriverTripHistoryContent(
                        trips = driverTrips,
                        searchQuery = searchQuery,
                        statusFilter = selectedStatusFilter,
                        onViewDetails = { selectedDriverItemForReceipt = it },
                        onRatePassenger = { ratingDriverTripTarget = it },
                        onReportIncident = { driverReportTargetItem = it }
                    )
                }
            }
        }
    }

    // Passenger Trip Receipt / Breakdown Dialog
    selectedPassengerOrderForReceipt?.let { order ->
        PassengerReceiptDialog(
            order = order,
            onDismiss = { selectedPassengerOrderForReceipt = null }
        )
    }

    // Driver Trip Receipt / Details Dialog
    selectedDriverItemForReceipt?.let { trip ->
        DriverTripReceiptDialog(
            trip = trip,
            onDismiss = { selectedDriverItemForReceipt = null }
        )
    }

    // Passenger Rating Captain Dialog
    ratingOrderTarget?.let { order ->
        PostRideRatingDialog(
            rideId = order.id.ifBlank { order.requestId },
            currentUserId = userId,
            currentUserName = userName,
            isDriver = false,
            targetId = order.assignedDriverId.ifBlank { "driver_default" },
            targetName = order.driverName.ifBlank { "Captain" },
            targetPhone = order.driverPhone,
            targetVehicleSummary = "${order.driverVehicleMake} ${order.driverVehicleModel}".trim(),
            targetPlateNumber = order.driverPlateNumber,
            targetRating = order.driverRating,
            pickupTitle = order.pickupTitle,
            destinationTitle = order.destinationTitle,
            farePkr = order.agreedFare,
            onDismiss = { ratingOrderTarget = null },
            onRatingSubmitted = {
                ratingOrderTarget = null
            },
            onOpenSafetyReport = {
                val target = ratingOrderTarget
                ratingOrderTarget = null
                reportOrderTarget = target
            }
        )
    }

    // Driver Rating Passenger Dialog
    ratingDriverTripTarget?.let { trip ->
        PostRideRatingDialog(
            rideId = trip.id.ifBlank { trip.tripId.ifBlank { trip.requestId } },
            currentUserId = userId,
            currentUserName = userName,
            isDriver = true,
            targetId = trip.passengerId.ifBlank { "passenger_${trip.id}" },
            targetName = trip.passengerName.ifBlank { "Passenger" },
            targetPhone = "",
            targetVehicleSummary = "",
            targetPlateNumber = "",
            targetRating = trip.passengerRating,
            pickupTitle = trip.pickupTitle.ifBlank { trip.pickupAddress },
            destinationTitle = trip.destinationTitle.ifBlank { trip.destinationAddress },
            farePkr = (trip.agreedFare ?: 0).takeIf { it > 0 } ?: trip.farePkr,
            onDismiss = { ratingDriverTripTarget = null },
            onRatingSubmitted = {
                ratingDriverTripTarget = null
            },
            onOpenSafetyReport = {
                val target = ratingDriverTripTarget
                ratingDriverTripTarget = null
                driverReportTargetItem = target
            }
        )
    }

    // Safety / Incident Report Dialog (Passenger)
    reportOrderTarget?.let { order ->
        SafetyReportDialog(
            rideId = order.id.ifBlank { order.requestId },
            reporterId = userId,
            reporterName = userName,
            reporterPhone = userPhone,
            isReporterDriver = false,
            reportedUserId = order.assignedDriverId.ifBlank { "driver_default" },
            reportedUserName = order.driverName.ifBlank { "Captain" },
            driverPlateNumber = order.driverPlateNumber,
            pickupTitle = order.pickupTitle,
            destinationTitle = order.destinationTitle,
            onDismiss = { reportOrderTarget = null },
            onReportSubmitted = {
                reportOrderTarget = null
                Toast.makeText(context, "Safety report lodged with Drigo Safety Team", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Safety / Incident Report Dialog (Driver)
    driverReportTargetItem?.let { item ->
        SafetyReportDialog(
            rideId = item.id.ifBlank { item.tripId },
            reporterId = userId,
            reporterName = userName,
            reporterPhone = userPhone,
            isReporterDriver = true,
            reportedUserId = item.passengerId.ifBlank { "passenger_default" },
            reportedUserName = item.passengerName.ifBlank { "Passenger" },
            driverPlateNumber = "",
            pickupTitle = item.pickupTitle,
            destinationTitle = item.destinationTitle,
            onDismiss = { driverReportTargetItem = null },
            onReportSubmitted = {
                driverReportTargetItem = null
                Toast.makeText(context, "Incident report submitted to Admin", Toast.LENGTH_LONG).show()
            }
        )
    }
}

/**
 * Passenger History List with Search, Status filtering, Analytics Banner and Interactive Cards
 */
@Composable
private fun PassengerTripHistoryContent(
    orders: List<PassengerOrder>,
    searchQuery: String,
    statusFilter: StatusFilter,
    onViewReceipt: (PassengerOrder) -> Unit,
    onRateDriver: (PassengerOrder) -> Unit,
    onReportSafety: (PassengerOrder) -> Unit,
    onRebookTrip: (PassengerOrder) -> Unit
) {
    val filteredOrders = remember(orders, searchQuery, statusFilter) {
        orders.filter { order ->
            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.COMPLETED -> order.status == PassengerOrderStatus.COMPLETED
                StatusFilter.CANCELLED -> order.status == PassengerOrderStatus.CANCELLED
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                order.pickupTitle.contains(searchQuery, ignoreCase = true) ||
                        order.destinationTitle.contains(searchQuery, ignoreCase = true) ||
                        order.driverName.contains(searchQuery, ignoreCase = true) ||
                        order.driverVehicleMake.contains(searchQuery, ignoreCase = true) ||
                        order.driverVehicleModel.contains(searchQuery, ignoreCase = true) ||
                        order.rideCategory.contains(searchQuery, ignoreCase = true)
            }
            matchesStatus && matchesSearch
        }
    }

    val totalCompleted = remember(orders) { orders.count { it.status == PassengerOrderStatus.COMPLETED } }
    val totalSpentPkr = remember(orders) { orders.filter { it.status == PassengerOrderStatus.COMPLETED }.sumOf { it.agreedFare } }
    val totalDistanceKm = remember(orders) { orders.filter { it.status == PassengerOrderStatus.COMPLETED }.sumOf { it.distanceKm } }

    if (orders.isEmpty()) {
        EmptyTripHistoryView(
            isDriver = false,
            searchQuery = searchQuery
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Passenger Analytics Banner (Only when not filtering by a search term)
            if (searchQuery.isBlank() && statusFilter == StatusFilter.ALL && totalCompleted > 0) {
                item {
                    PassengerAnalyticsHeroBanner(
                        totalCompleted = totalCompleted,
                        totalSpentPkr = totalSpentPkr,
                        totalDistanceKm = totalDistanceKm
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                item {
                    EmptyTripHistoryView(
                        isDriver = false,
                        searchQuery = searchQuery
                    )
                }
            } else {
                items(filteredOrders, key = { it.id.ifBlank { it.requestId } }) { order ->
                    PassengerTripCard(
                        order = order,
                        onViewReceipt = { onViewReceipt(order) },
                        onRateDriver = { onRateDriver(order) },
                        onReportSafety = { onReportSafety(order) },
                        onRebookTrip = { onRebookTrip(order) }
                    )
                }
            }
        }
    }
}

/**
 * Passenger Analytics Hero Banner
 */
@Composable
private fun PassengerAnalyticsHeroBanner(
    totalCompleted: Int,
    totalSpentPkr: Int,
    totalDistanceKm: Double
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            DrigoBrandMagentaBg,
                            DrigoBrandPurple,
                            DrigoBrandPurpleDark
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your Riding Journey",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Drigo Passenger",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Metric 1: Total Rides
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rides Taken",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalCompleted",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Metric 2: Total Distance
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = "Distance",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", totalDistanceKm)} km",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Metric 3: Total Spent
                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Spent",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PKR ${NumberFormat.getNumberInstance(Locale.US).format(totalSpentPkr)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = InDriveLimeGreen
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Passenger Trip Card with Route Dots, Fare, and Action Buttons
 */
@Composable
private fun PassengerTripCard(
    order: PassengerOrder,
    onViewReceipt: () -> Unit,
    onRateDriver: () -> Unit,
    onReportSafety: () -> Unit,
    onRebookTrip: () -> Unit
) {
    val isCompleted = order.status == PassengerOrderStatus.COMPLETED
    val formattedDate = remember(order.createdAt) {
        if (order.createdAt > 0) {
            SimpleDateFormat("EEE, MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
        } else {
            order.scheduledTimeText?.ifBlank { "Recently" } ?: "Recently"
        }
    }

    val isDark = MaterialTheme.drigoColors.isDark

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passenger_trip_card_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Category Pill, Distance, Date/Time & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val categoryIcon = when (order.rideCategory.lowercase(Locale.ROOT)) {
                        "moto", "bike" -> Icons.Default.TwoWheeler
                        "auto", "rickshaw" -> Icons.Default.ElectricRickshaw
                        else -> Icons.Default.DirectionsCar
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DrigoBrandPurple.copy(alpha = 0.14f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = DrigoBrandPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = order.rideCategory.ifBlank { "Ride A/C" },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple
                            )
                        }
                    }

                    if (order.distanceKm > 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${order.distanceKm} km",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCompleted) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFEF5350).copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) Color(0xFF00C853).copy(alpha = 0.4f) else Color(0xFFEF5350).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isCompleted) Color(0xFF00C853) else Color(0xFFEF5350),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCompleted) "Completed" else "Cancelled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) (if (isDark) Color(0xFF69F0AE) else Color(0xFF1B5E20)) else (if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formattedDate,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(12.dp))

            // Route Section: Pickup -> Destination
            Row(modifier = Modifier.fillMaxWidth()) {
                // Route Visual Indicator (Green dot -> Line -> Red Pin)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp, end = 12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00C853),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF00C853), Color(0xFFE53935))
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Locations text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.pickupTitle.ifBlank { "Pickup Location" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = order.destinationTitle.ifBlank { "Destination Location" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Agreed Fare & Payment Method
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PKR ${NumberFormat.getNumberInstance(Locale.US).format(order.agreedFare)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (isCompleted) (if (isDark) Color(0xFFD8B4FE) else DrigoBrandPurple) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF282030) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = order.paymentMethod.ifBlank { "Cash" },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFFFD54F) else Color(0xFFF57F17),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Driver Information Snippet (if available)
            if (order.driverName.isNotBlank() && isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF231B2C) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = DrigoBrandPurple.copy(alpha = 0.18f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = order.driverName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DrigoBrandPurple
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Captain ${order.driverName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (order.driverVehicleMake.isNotBlank()) {
                                    Text(
                                        text = "${order.driverVehicleMake} ${order.driverVehicleModel} • ${order.driverPlateNumber}".trim(),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", order.driverRating),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Rebook (Lime Green), View Receipt, Rate Driver
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rebook Button (Primary CTA)
                Button(
                    onClick = onRebookTrip,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InDriveLimeGreen,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(40.dp)
                        .testTag("btn_rebook_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rebook", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }

                // View Receipt Button
                OutlinedButton(
                    onClick = onViewReceipt,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("btn_view_receipt_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receipt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                if (isCompleted) {
                    // Rate Driver Button
                    OutlinedButton(
                        onClick = onRateDriver,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB300)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(40.dp)
                            .testTag("btn_rate_driver_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rate", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Driver Trip History Content with Total Net Income Summary, Filtering & Search
 */
@Composable
private fun DriverTripHistoryContent(
    trips: List<DriverHistoryItem>,
    searchQuery: String,
    statusFilter: StatusFilter,
    onViewDetails: (DriverHistoryItem) -> Unit,
    onRatePassenger: (DriverHistoryItem) -> Unit,
    onReportIncident: (DriverHistoryItem) -> Unit
) {
    val filteredTrips = remember(trips, searchQuery, statusFilter) {
        trips.filter { trip ->
            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.COMPLETED -> trip.status.equals("COMPLETED", true) || trip.tripStatus.equals("COMPLETED", true)
                StatusFilter.CANCELLED -> trip.status.equals("CANCELLED", true) || trip.tripStatus.equals("CANCELLED", true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                trip.pickupTitle.contains(searchQuery, ignoreCase = true) ||
                        trip.destinationTitle.contains(searchQuery, ignoreCase = true) ||
                        trip.passengerName.contains(searchQuery, ignoreCase = true) ||
                        trip.category.contains(searchQuery, ignoreCase = true) ||
                        trip.paymentMethod.contains(searchQuery, ignoreCase = true)
            }
            matchesStatus && matchesSearch
        }
    }

    val completedTripsCount = remember(trips) {
        trips.count { it.status.equals("COMPLETED", true) || it.tripStatus.equals("COMPLETED", true) }
    }
    val totalEarnings = remember(trips) {
        trips.filter { it.status.equals("COMPLETED", true) || it.tripStatus.equals("COMPLETED", true) }
            .sumOf {
                if (it.netEarningsPkr > 0) it.netEarningsPkr else {
                    val fare = (it.agreedFare ?: 0).takeIf { f -> f > 0 } ?: it.farePkr
                    val fee = if (it.platformFeePkr > 0) it.platformFeePkr else (fare * 0.10).toInt()
                    fare - fee
                }
            }
    }
    val totalDistanceKm = remember(trips) {
        trips.filter { it.status.equals("COMPLETED", true) || it.tripStatus.equals("COMPLETED", true) }
            .sumOf { it.distanceKm }
    }

    if (trips.isEmpty()) {
        EmptyTripHistoryView(
            isDriver = true,
            searchQuery = searchQuery
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Driver Earnings & Completed Rides Summary Card (Only when not actively searching)
            if (searchQuery.isBlank() && statusFilter == StatusFilter.ALL && completedTripsCount > 0) {
                item {
                    DriverEarningsHeroCard(
                        totalEarnings = totalEarnings,
                        completedTripsCount = completedTripsCount,
                        totalDistanceKm = totalDistanceKm
                    )
                }
            }

            if (filteredTrips.isEmpty()) {
                item {
                    EmptyTripHistoryView(
                        isDriver = true,
                        searchQuery = searchQuery
                    )
                }
            } else {
                items(filteredTrips, key = { it.id.ifBlank { it.tripId } }) { trip ->
                    DriverTripCard(
                        trip = trip,
                        onViewDetails = { onViewDetails(trip) },
                        onRatePassenger = { onRatePassenger(trip) },
                        onReportIncident = { onReportIncident(trip) }
                    )
                }
            }
        }
    }
}

/**
 * Driver Earnings Hero Card with High-Impact Gradient & Metrics
 */
@Composable
private fun DriverEarningsHeroCard(
    totalEarnings: Int,
    completedTripsCount: Int,
    totalDistanceKm: Double
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            DrigoBrandPurpleDark,
                            DrigoBrandPurple,
                            DrigoBrandMagentaBg
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Driver Net Payout",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "90% Net Take-home",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = InDriveLimeGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "PKR ${NumberFormat.getNumberInstance(Locale.US).format(totalEarnings)}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = InDriveLimeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Completed", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.75f))
                            Text("$completedTripsCount Rides", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Distance", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.75f))
                            Text("${String.format(Locale.US, "%.1f", totalDistanceKm)} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Driver Trip Card with Net Income, Route breakdown, and Action Buttons
 */
@Composable
private fun DriverTripCard(
    trip: DriverHistoryItem,
    onViewDetails: () -> Unit,
    onRatePassenger: () -> Unit,
    onReportIncident: () -> Unit
) {
    val isCompleted = trip.status.equals("COMPLETED", true) || trip.tripStatus.equals("COMPLETED", true)
    val fare = (trip.agreedFare ?: 0).takeIf { it > 0 } ?: trip.farePkr
    val commission = if (trip.platformFeePkr > 0) trip.platformFeePkr else (fare * 0.10).toInt()
    val netEarnings = if (trip.netEarningsPkr > 0) trip.netEarningsPkr else (fare - commission)
    val formattedDate = remember(trip.timestamp) {
        if (trip.timestamp > 0) {
            SimpleDateFormat("EEE, MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(trip.timestamp))
        } else {
            trip.dateFormatted.ifBlank { "Recently" }
        }
    }

    val isDark = MaterialTheme.drigoColors.isDark

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_trip_card_${trip.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Category, Distance & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val categoryIcon = when (trip.category.lowercase(Locale.ROOT)) {
                        "moto", "bike" -> Icons.Default.TwoWheeler
                        "auto", "rickshaw" -> Icons.Default.ElectricRickshaw
                        else -> Icons.Default.DirectionsCar
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DrigoBrandPurple.copy(alpha = 0.14f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = DrigoBrandPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = trip.category.ifBlank { "Ride" },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple
                            )
                        }
                    }

                    if (trip.distanceKm > 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${trip.distanceKm} km" + if (trip.durationMins > 0) " • ${trip.durationMins}m" else "",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCompleted) Color(0xFF00C853).copy(alpha = 0.12f) else Color(0xFFEF5350).copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) Color(0xFF00C853).copy(alpha = 0.4f) else Color(0xFFEF5350).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isCompleted) Color(0xFF00C853) else Color(0xFFEF5350),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCompleted) "Completed" else "Cancelled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) (if (isDark) Color(0xFF69F0AE) else Color(0xFF1B5E20)) else (if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(12.dp))

            // Passenger Info & Earnings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = InDriveLimeGreen.copy(alpha = 0.22f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = trip.passengerName.take(1).uppercase().ifBlank { "P" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) InDriveLimeGreen else Color(0xFF1B5E20)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = trip.passengerName.ifBlank { "Passenger" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (trip.passengerRating > 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", trip.passengerRating),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Fare & Net Earnings
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net: PKR ${NumberFormat.getNumberInstance(Locale.US).format(netEarnings)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isCompleted) (if (isDark) Color(0xFF69F0AE) else Color(0xFF00C853)) else Color.Gray
                    )
                    Text(
                        text = "Gross: PKR $fare (${trip.paymentMethod.ifBlank { "Cash" }})",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route Breakdown
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp, end = 12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00C853),
                        modifier = Modifier.size(9.dp)
                    ) {}
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(26.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF00C853), Color(0xFFE53935))
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(13.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.pickupTitle.ifBlank { "Pickup Address" },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = trip.destinationTitle.ifBlank { "Destination Address" },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_driver_trip_details_${trip.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Breakdown", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }

                if (isCompleted) {
                    OutlinedButton(
                        onClick = onRatePassenger,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB300)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(38.dp)
                            .testTag("btn_driver_rate_${trip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rate", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = onReportIncident,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                    border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(38.dp)
                        .testTag("btn_driver_report_${trip.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Report,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Empty Trip History View
 */
@Composable
private fun EmptyTripHistoryView(
    isDriver: Boolean,
    searchQuery: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = DrigoBrandPurple.copy(alpha = 0.12f),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDriver) Icons.Default.DirectionsCar else Icons.Default.History,
                        contentDescription = null,
                        tint = DrigoBrandPurple,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No Matching Trips Found" else if (isDriver) "No Driver Trips Yet" else "No Completed Rides Yet",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (searchQuery.isNotBlank())
                    "Try adjusting your search terms or filter criteria to find specific past rides."
                else if (isDriver)
                    "When you accept and complete passenger ride requests, your full earnings and trip logs will appear here."
                else
                    "Your completed rides, fare receipts, and driver feedback records will be organized right here.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Passenger Digital Receipt Modal
 */
@Composable
private fun PassengerReceiptDialog(
    order: PassengerOrder,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val total = order.agreedFare
    val dist = order.distanceKm
    val baseFare = when (order.rideCategory.lowercase(Locale.ROOT)) {
        "moto", "bike" -> 60
        "auto", "rickshaw" -> 80
        "ride mini", "mini" -> 120
        "comfort", "ac", "ride a/c" -> 160
        else -> 100
    }.coerceAtMost((total * 0.35).toInt().coerceAtLeast(30))
    val distanceFare = if (dist > 0.0) {
        (total - baseFare - (total * 0.08).toInt()).coerceAtLeast(0)
    } else {
        (total * 0.55).toInt()
    }
    val taxesAndSurge = (total - baseFare - distanceFare).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("passenger_receipt_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = DrigoBrandPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trip Receipt",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Total Fare Header
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DrigoBrandPurple.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Paid",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PKR ${NumberFormat.getNumberInstance(Locale.US).format(order.agreedFare)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DrigoBrandPurple
                        )
                        Text(
                            text = "Paid via ${order.paymentMethod.ifBlank { "Cash" }}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ride Breakdown Lines
                ReceiptLine(label = "Base Booking Fare", value = "PKR $baseFare")
                ReceiptLine(label = "Distance Charge (${order.distanceKm} km)", value = "PKR $distanceFare")
                ReceiptLine(label = "Service Fee & Surcharges", value = "PKR $taxesAndSurge")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ReceiptLine(label = "Final Amount", value = "PKR ${order.agreedFare}", isBold = true)

                Spacer(modifier = Modifier.height(16.dp))

                // Route Details
                Text(
                    text = "Trip Summary",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pickup: ${order.pickupTitle}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Dropoff: ${order.destinationTitle}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (order.driverName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Captain: ${order.driverName} (${order.driverVehicleMake} ${order.driverVehicleModel})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val summary = "Drigo Ride Receipt\nTotal: PKR ${order.agreedFare}\nFrom: ${order.pickupTitle}\nTo: ${order.destinationTitle}\nCaptain: ${order.driverName}\nStatus: ${order.status.name}"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Drigo Receipt", summary))
                    Toast.makeText(context, "Receipt copied to clipboard!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Receipt Details", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Driver Digital Trip Details Modal
 */
@Composable
private fun DriverTripReceiptDialog(
    trip: DriverHistoryItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fare = (trip.agreedFare ?: 0).takeIf { it > 0 } ?: trip.farePkr
    val commission = if (trip.platformFeePkr > 0) trip.platformFeePkr else (fare * 0.10).toInt()
    val netEarnings = if (trip.netEarningsPkr > 0) trip.netEarningsPkr else (fare - commission)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_receipt_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = DrigoBrandPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Driver Trip Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00C853).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Driver Net Payout",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PKR ${NumberFormat.getNumberInstance(Locale.US).format(netEarnings)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00C853)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ReceiptLine(label = "Gross Ride Fare", value = "PKR $fare")
                ReceiptLine(label = "Drigo Platform Fee (10%)", value = "- PKR $commission")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ReceiptLine(label = "Net Credited to Driver", value = "PKR $netEarnings", isBold = true)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Passenger: ${trip.passengerName.ifBlank { "Passenger" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "From: ${trip.pickupTitle}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "To: ${trip.destinationTitle}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Distance: ${trip.distanceKm} km • ${trip.durationMins} mins",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Payment: ${trip.paymentMethod.ifBlank { "Cash" }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ReceiptLine(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
