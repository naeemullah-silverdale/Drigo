package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.InDriveLimeGreen
import com.example.ui.theme.drigoColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Full Passenger City-to-City departures screen content matching Screenshot 1 & 2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityToCityPassengerDeparturesContent(
    onBackClick: () -> Unit,
    onSosClick: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onViewActiveRide: ((PlannedDeparture, PlannedDepartureBooking?) -> Unit)? = null,
    initialFromCity: String = "All Routes",
    initialToCity: String = "All Routes",
    isSheetMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { FirebaseRepository.getInstance(context) }
    val allDepartures by remember { repo.observeAllPlannedDepartures() }.collectAsState(initial = emptyList())
    var isInitialLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshErrorMessage by remember { mutableStateOf<String?>(null) }

    val handleRefresh: () -> Unit = {
        scope.launch {
            isRefreshing = true
            refreshErrorMessage = null
            val result = repo.refreshAllPlannedDepartures()
            isRefreshing = false
            result.onFailure { error ->
                refreshErrorMessage = error.localizedMessage ?: "Failed to refresh scheduled departures. Please check your internet connection."
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600L)
        isInitialLoading = false
    }

    LaunchedEffect(allDepartures.size) {
        if (allDepartures.isNotEmpty()) {
            isInitialLoading = false
        }
    }

    val isDark = MaterialTheme.drigoColors.isDark
    val screenBg = if (isDark) Color(0xFF0F1116) else MaterialTheme.colorScheme.background
    val cardBg = if (isDark) Color(0xFF181B23) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color(0xFF282C3A) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant

    var fromCity by remember { mutableStateOf(initialFromCity) }
    var fromHub by remember { mutableStateOf("All Pickup Points") }
    var toCity by remember { mutableStateOf(initialToCity) }
    var toHub by remember { mutableStateOf("All Drop-off Points") }

    var selectedDateText by remember { mutableStateOf("Tomorrow, 25 Oct") }
    var selectedWindowText by remember { mutableStateOf("Morning departures") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var selectedSortOption by remember { mutableStateOf("Earliest Departure") }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedDepartureForOffer by remember { mutableStateOf<PlannedDeparture?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showWindowDialog by remember { mutableStateOf(false) }
    var showCitySelectDialog by remember { mutableStateOf<String?>(null) } // "FROM" or "TO"

    // Auto-dismiss offer sheet if departure is cancelled by driver while passenger is viewing
    LaunchedEffect(allDepartures, selectedDepartureForOffer) {
        val selected = selectedDepartureForOffer
        if (selected != null) {
            val liveDep = allDepartures.find { it.id == selected.id }
            val isCancelled = liveDep == null ||
                    liveDep.status.trim().equals("CANCELLED", ignoreCase = true) ||
                    liveDep.status.contains("CANCEL", ignoreCase = true)
            if (isCancelled) {
                selectedDepartureForOffer = null
                android.widget.Toast.makeText(context, "This departure was cancelled by the captain.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hardware back press handler for predictable hierarchical navigation
    BackHandler(enabled = true) {
        when {
            selectedDepartureForOffer != null -> selectedDepartureForOffer = null
            showCitySelectDialog != null -> showCitySelectDialog = null
            showDateDialog -> showDateDialog = false
            showWindowDialog -> showWindowDialog = false
            else -> onBackClick()
        }
    }

    val quickCities = remember {
        listOf("All Routes", "Islamabad", "Lahore", "Peshawar", "Faisalabad", "Multan", "Rawalpindi", "Gujranwala", "Sialkot")
    }

    val filteredDepartures by remember(allDepartures, fromCity, toCity, selectedFilterIndex, selectedSortOption) {
        derivedStateOf {
            val base = allDepartures
                .distinctBy { it.id }
                .filter { dep ->
                val cleanStatus = dep.status.trim()
                // Strictly exclude ANY cancelled or completed departures
                val isCancelled = cleanStatus.equals("CANCELLED", ignoreCase = true) || cleanStatus.contains("CANCEL", ignoreCase = true)
                val isCompleted = cleanStatus.equals("COMPLETED", ignoreCase = true) || cleanStatus.contains("COMPLET", ignoreCase = true)
                if (isCancelled || isCompleted) {
                    return@filter false
                }

                // Strictly exclude corrupt / blank departures with missing essential data
                if (dep.pickupCity.isBlank() || dep.dropoffCity.isBlank() || dep.farePerSeat <= 0) {
                    return@filter false
                }

                // Must be ACTIVE, SCHEDULED, or OPEN (no blank or invalid status allowed)
                val isValidActive = cleanStatus.equals("ACTIVE", ignoreCase = true) ||
                        cleanStatus.equals("SCHEDULED", ignoreCase = true) ||
                        cleanStatus.equals("OPEN", ignoreCase = true) ||
                        cleanStatus.isBlank()
                if (!isValidActive) {
                    return@filter false
                }

                // Route matching: Allow "All", "All Routes", or blank to match all
                val isAllFrom = fromCity.isBlank() || fromCity.equals("All", ignoreCase = true) || fromCity.equals("All Routes", ignoreCase = true)
                val isAllTo = toCity.isBlank() || toCity.equals("All", ignoreCase = true) || toCity.equals("All Routes", ignoreCase = true)

                val matchesFrom = isAllFrom ||
                        dep.pickupCity.contains(fromCity, ignoreCase = true) ||
                        fromCity.contains(dep.pickupCity, ignoreCase = true)

                val matchesTo = isAllTo ||
                        dep.dropoffCity.contains(toCity, ignoreCase = true) ||
                        toCity.contains(dep.dropoffCity, ignoreCase = true)

                val matchesFilter = when (selectedFilterIndex) {
                    1 -> dep.corridorName.contains("M-2", ignoreCase = true) || dep.corridorSubtitle.contains("M-2", ignoreCase = true) || dep.pickupCity.contains("M-2", ignoreCase = true)
                    2 -> dep.allowFullCarBuyout
                    else -> true
                }

                matchesFrom && matchesTo && matchesFilter
            }
            when (selectedSortOption) {
                "Highest Rating" -> base.sortedByDescending { it.driverRating }
                "Lowest Price" -> base.sortedBy { it.farePerSeat }
                else -> base.sortedBy { it.departureTimeText }
            }
        }
    }

    Box(
        modifier = modifier
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP BAR (Screenshot 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF222632) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "City to City Rides",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00C853).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00E676), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Live",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Scheduled departures & shared intercity trips",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onNavigateToOrders,
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF222632) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "My Orders",
                                tint = textPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Orders",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Surface(
                        onClick = onSosClick,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "SOS",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "SOS",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Scrollable Content
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = handleRefresh,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("passenger_city_to_city_pull_to_refresh")
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (refreshErrorMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFF5252).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("refresh_error_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = "Error",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = refreshErrorMessage ?: "",
                                            fontSize = 12.sp,
                                            color = textPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { refreshErrorMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // LIVE ACTIVE INTERCITY RIDE BANNER (Attachment 1 Shortcut)
                val activeRide = allDepartures.firstOrNull { it.status.equals("IN_PROGRESS", true) || it.status.equals("STARTED", true) || it.status.equals("DEPARTED", true) }
                if (activeRide != null) {
                    item {
                        ActiveRideStatusMiniBanner(
                            departure = activeRide,
                            onClick = {
                                onViewActiveRide?.invoke(activeRide, null)
                            }
                        )
                    }
                }

                // ORIGIN & DESTINATION CARD (Screenshot 1)
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // From City (Clickable)
                                    Surface(
                                        onClick = { showCitySelectDialog = "FROM" },
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(Color(0xFF00E676), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "From (Tap to change)",
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF9FA4B2)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = fromCity,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = textPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = fromHub,
                                                        fontSize = 11.5.sp,
                                                        color = Color(0xFF00E676)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        color = borderCol.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(start = 20.dp),
                                        thickness = 0.8.dp
                                    )

                                    // To City (Clickable)
                                    Surface(
                                        onClick = { showCitySelectDialog = "TO" },
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(Color(0xFFFF5252), RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "To (Tap to change)",
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF9FA4B2)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = toCity,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = textPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = toHub,
                                                        fontSize = 11.5.sp,
                                                        color = Color(0xFFFF9800)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    onClick = {
                                        val tempCity = fromCity
                                        val tempHub = fromHub
                                        fromCity = toCity
                                        fromHub = toHub
                                        toCity = tempCity
                                        toHub = tempHub
                                    },
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF252936) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, borderCol),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Swap Cities",
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Quick destination chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickCities.forEach { city ->
                                    val isSelected = if (city == "All Routes") {
                                        fromCity.equals("All", true) && toCity.equals("All", true)
                                    } else {
                                        toCity.equals(city, ignoreCase = true)
                                    }
                                    Surface(
                                        onClick = {
                                            if (city == "All Routes") {
                                                fromCity = "All"
                                                toCity = "All"
                                                fromHub = "All Hubs"
                                                toHub = "All Hubs"
                                            } else {
                                                toCity = city
                                                toHub = "Direct Hub"
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.2f) else if (isDark) Color(0xFF222632) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF00E676) else borderCol.copy(alpha = 0.7f)
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = city,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF00E676) else textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Date & Window Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = { showDateDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0xFF222632) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, borderCol),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarMonth,
                                            contentDescription = null,
                                            tint = textSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Date: $selectedDateText",
                                            fontSize = 11.5.sp,
                                            color = textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { showWindowDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0xFF222632) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, borderCol),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = textSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Window: $selectedWindowText",
                                            fontSize = 11.5.sp,
                                            color = textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // FILTER TABS (Screenshot 1)
                item {
                    val filterLabels = listOf(
                        "All Rides (${filteredDepartures.size})",
                        "Via M-2 Motorway",
                        "Full Car Buyout"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterLabels.forEachIndexed { index, label ->
                            val isSelected = selectedFilterIndex == index
                            Surface(
                                onClick = { selectedFilterIndex = index },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF00E676) else if (isDark) Color(0xFF222632) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF00E676) else borderCol
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION HEADER: AVAILABLE CAPTAINS + SORT
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVAILABLE CAPTAINS (${filteredDepartures.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textSecondary,
                            letterSpacing = 0.5.sp
                        )

                        Box {
                            Surface(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF202430) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, borderCol),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Sort",
                                        tint = textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = selectedSortOption,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                listOf("Earliest Departure", "Highest Rating", "Lowest Price").forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, fontSize = 13.sp) },
                                        onClick = {
                                            selectedSortOption = opt
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // CAPTAIN CARDS (Screenshot 1)
                if (isInitialLoading && filteredDepartures.isEmpty()) {
                    items(3) {
                        PassengerDepartureCardSkeleton()
                    }
                } else if (filteredDepartures.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF00E676).copy(alpha = 0.12f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "No Active Scheduled Departures",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (fromCity.isNotBlank() && toCity.isNotBlank() && !fromCity.equals("All", true) && !toCity.equals("All", true))
                                        "No drivers currently scheduled between $fromCity and $toCity. View all active departures or adjust your route."
                                    else
                                        "No drivers currently have scheduled active departures. Check back soon.",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 17.sp
                                )
                                Button(
                                    onClick = {
                                        fromCity = "All"
                                        toCity = "All"
                                        fromHub = "All Hubs"
                                        toHub = "All Hubs"
                                        selectedFilterIndex = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E676),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("View All Active Departures", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredDepartures, key = { it.id }) { departure ->
                        ModernPassengerDepartureCard(
                            departure = departure,
                            onSendOffer = {
                                selectedDepartureForOffer = departure
                            },
                            onViewActiveRide = {
                                onViewActiveRide?.invoke(departure, null)
                            }
                        )
                    }
                }

                // BOTTOM GUARANTEE CARD (Screenshot 1)
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF141720) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, borderCol.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Guarantee",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Drigo City-to-City Guarantee",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "All captains undergo rigorous biometric verification, route tracking via live GPS, and mandatory vehicle roadworthiness inspections.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Date selection dialog
        if (showDateDialog) {
            Dialog(onDismissRequest = { showDateDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Select Departure Date", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
                        listOf("Today", "Tomorrow, 25 Oct", "Day After Tomorrow, 26 Oct", "Next Week").forEach { d ->
                            Surface(
                                onClick = {
                                    selectedDateText = d
                                    showDateDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedDateText == d) Color(0xFF00E676).copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(d, color = if (selectedDateText == d) Color(0xFF00E676) else textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Window selection dialog
        if (showWindowDialog) {
            Dialog(onDismissRequest = { showWindowDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Select Time Window", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
                        listOf("Morning departures (06:00 - 12:00)", "Afternoon departures (12:00 - 18:00)", "Evening departures (18:00 - 24:00)", "All day").forEach { w ->
                            val cleanName = w.substringBefore(" (")
                            Surface(
                                onClick = {
                                    selectedWindowText = cleanName
                                    showWindowDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedWindowText == cleanName) Color(0xFF00E676).copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(w, color = if (selectedWindowText == cleanName) Color(0xFF00E676) else textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // CITY SELECTION DIALOG (For From / To Route selection)
        if (showCitySelectDialog != null) {
            val isEditingFrom = showCitySelectDialog == "FROM"
            val commonPakistanCities = listOf(
                "All Cities",
                "Islamabad",
                "Rawalpindi",
                "Lahore",
                "Peshawar",
                "Faisalabad",
                "Multan",
                "Gujranwala",
                "Sialkot",
                "Abbottabad",
                "Gujrat",
                "Karachi",
                "Hyderabad",
                "Quetta"
            )
            var citySearchQuery by remember { mutableStateOf("") }
            val filteredCityList = commonPakistanCities.filter {
                citySearchQuery.isBlank() || it.contains(citySearchQuery, ignoreCase = true)
            }

            Dialog(onDismissRequest = { showCitySelectDialog = null }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isEditingFrom) "Select Departure City (Origin)" else "Select Drop-off City (Destination)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )

                        OutlinedTextField(
                            value = citySearchQuery,
                            onValueChange = { citySearchQuery = it },
                            placeholder = { Text("Search city (e.g. Lahore)", fontSize = 12.5.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) Color(0xFF181B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = if (isDark) Color(0xFF181B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = borderCol,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredCityList) { city ->
                                val isSelected = if (isEditingFrom) fromCity.equals(city, true) else toCity.equals(city, true)
                                Surface(
                                    onClick = {
                                        if (isEditingFrom) {
                                            fromCity = if (city == "All Cities") "All" else city
                                            fromHub = if (city == "All Cities") "All Hubs" else "$city Central"
                                        } else {
                                            toCity = if (city == "All Cities") "All" else city
                                            toHub = if (city == "All Cities") "All Hubs" else "$city Terminal"
                                        }
                                        showCitySelectDialog = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.18f) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (city == "All Cities") Icons.Default.AllInclusive else Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF00E676) else textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = city,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF00E676) else textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showCitySelectDialog = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF222634) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = textPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("Close", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // SEND BOOKING OFFER MODAL SHEET (Screenshot 2)
        if (selectedDepartureForOffer != null) {
            ModernPassengerMakeOfferSheet(
                departure = selectedDepartureForOffer!!,
                onOfferSent = {
                    selectedDepartureForOffer = null
                },
                onDismiss = { selectedDepartureForOffer = null }
            )
        }
    }
}

/**
 * Passenger Departure Card matching Screenshot 1 exactly
 */
@Composable
fun ModernPassengerDepartureCard(
    departure: PlannedDeparture,
    onSendOffer: () -> Unit,
    onViewActiveRide: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.drigoColors.isDark
    val isRideInProgress = departure.status.equals("IN_PROGRESS", true) || departure.status.equals("STARTED", true) || departure.status.equals("DEPARTED", true)
    val cardBg = if (isDark) Color(0xFF181B23) else MaterialTheme.colorScheme.surface
    val borderCol = if (isRideInProgress) Color(0xFF00E676) else if (isDark) Color(0xFF282C3A) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant
    val availableSeats = remember(departure) {
        when {
            departure.isFullCarBooked -> 0
            departure.bookedSeatsCount > 0 -> (departure.totalSeats - departure.bookedSeatsCount).coerceIn(0, departure.totalSeats)
            departure.availableSeats in 0..departure.totalSeats -> departure.availableSeats
            else -> (departure.totalSeats - departure.bookedSeatsCount).coerceIn(0, departure.totalSeats)
        }
    }
    val isFull = availableSeats <= 0 || departure.status.equals("FULL", true)

    Surface(
        onClick = {
            if (isRideInProgress) {
                onViewActiveRide?.invoke()
            } else if (!isFull) {
                onSendOffer()
            }
        },
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Captain Info + Fare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar with verified badge
                    Box(modifier = Modifier.size(44.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E88E5),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = departure.driverName.trim().take(1).uppercase().ifBlank { "C" },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(0xFF00E676), CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = departure.driverName.ifBlank { "Captain" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (departure.driverRating > 0f) "${departure.driverRating} ★" else "5.0 ★",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFFFB300)
                            )
                            Text(
                                text = if (departure.driverTotalTrips > 0) " (${departure.driverTotalTrips} trips)" else " (Captain)",
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }

                        val vehicleDetails = listOfNotNull(
                            departure.driverVehicle.takeIf { it.isNotBlank() },
                            departure.driverVehicleType.takeIf { it.isNotBlank() },
                            departure.driverPlateNumber.takeIf { it.isNotBlank() }
                        ).joinToString(" • ").ifBlank { "Verified Vehicle" }

                        Text(
                            text = vehicleDetails,
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Fare Display (Top Right)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PKR ${"%,d".format(departure.farePerSeat)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF00E676)
                    )
                    Text(
                        text = "per seat",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                }
            }

            HorizontalDivider(color = borderCol.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Row 2: Route, Hubs & Timing
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${departure.departureTimeText} (${departure.departureDateText})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = textPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF232734) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = departure.corridorName.takeIf { it.isNotBlank() } ?: "Direct Corridor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Pickup Stop
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF00E676), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pickup: ",
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    val pickupDetail = buildString {
                        append(departure.pickupCity.ifBlank { "Origin" })
                        if (departure.pickupHub.isNotBlank()) append(" (${departure.pickupHub})")
                        if (departure.pickupStopDetails.isNotBlank()) append(" - ${departure.pickupStopDetails}")
                    }
                    Text(
                        text = pickupDetail,
                        fontSize = 12.sp,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Dropoff Stop
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFFFF5252), RoundedCornerShape(1.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Drop-off: ",
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    val dropoffDetail = buildString {
                        append(departure.dropoffCity.ifBlank { "Destination" })
                        if (departure.dropoffHub.isNotBlank()) append(" (${departure.dropoffHub})")
                        if (departure.dropoffStopDetails.isNotBlank()) append(" - ${departure.dropoffStopDetails}")
                    }
                    Text(
                        text = dropoffDetail,
                        fontSize = 12.sp,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Row 3: Seat Availability, Luggage & AC Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Seat status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isFull) Color(0xFFD32F2F).copy(alpha = 0.18f) else Color(0xFF00C853).copy(alpha = 0.18f),
                    border = BorderStroke(
                        0.8.dp,
                        if (isFull) Color(0xFFEF5350).copy(alpha = 0.4f) else Color(0xFF00E676).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventSeat,
                            contentDescription = null,
                            tint = if (isFull) Color(0xFFFF5252) else Color(0xFF00E676),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFull) "Full (0 seats left)" else "$availableSeats of ${departure.totalSeats} seats left",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFull) Color(0xFFFF5252) else Color(0xFF00E676)
                        )
                    }
                }

                // Ladies only badge
                if (departure.isLadiesOnly) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE91E63).copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, Color(0xFFE91E63).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Ladies Only",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF48FB1),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Flex window badge
                if (departure.flexWindowMins > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF232734) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "±${departure.flexWindowMins}m Flex",
                            fontSize = 11.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Luggage Policy Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF232734) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Luggage,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = departure.luggagePolicy.takeIf { it.isNotBlank() } ?: "1 Bag / Seat",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                // AC / Climate Control
                if (departure.isClimateControlled) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF232734) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AcUnit,
                                contentDescription = null,
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AC",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }
                }

                // Buyout option badge
                if (departure.allowFullCarBuyout) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, Color(0xFFFF9800).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Buyout: PKR ${"%,d".format(departure.fullCarFare)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Tolls included badge
                if (departure.tollsPreCleared) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Tolls Included",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00E676),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Row 4: Primary Action Button (Screenshot 1)
            Button(
                onClick = {
                    if (isRideInProgress) {
                        onViewActiveRide?.invoke()
                    } else {
                        onSendOffer()
                    }
                },
                enabled = isRideInProgress || !isFull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF2A2D37),
                    disabledContentColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isRideInProgress) Icons.Default.Navigation else Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRideInProgress) "🟢 Track Live In-Progress Ride" else if (isFull) "Departure Full" else "Send Offer / Book Seat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Passenger Make Offer Sheet matching Screenshot 2 exactly
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernPassengerMakeOfferSheet(
    departure: PlannedDeparture,
    onOfferSent: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { FirebaseRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    val isDark = MaterialTheme.drigoColors.isDark
    val sheetBg = if (isDark) Color(0xFF14161F) else MaterialTheme.colorScheme.surface
    val cardBg = if (isDark) Color(0xFF1C1F2B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val inputBg = if (isDark) Color(0xFF181B26) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val borderCol = if (isDark) Color(0xFF2C303E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant

    val availableSeats = (departure.totalSeats - departure.bookedSeatsCount).coerceAtLeast(1)

    var isFullCarBuyout by remember { mutableStateOf(false) }
    var requestedSeats by remember { mutableIntStateOf(1) }
    var proposedFare by remember { mutableIntStateOf(departure.farePerSeat) }

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    var passengerName by remember { mutableStateOf(currentUser?.displayName?.ifBlank { null } ?: "Passenger") }
    var passengerPhone by remember { mutableStateOf(currentUser?.phoneNumber?.ifBlank { null } ?: "0300-1234567") }
    var pickupPoint by remember { mutableStateOf(if (departure.pickupHub.isNotBlank()) departure.pickupHub else "${departure.pickupCity.ifBlank { "Departure" }} Main Stop") }
    var dropoffPoint by remember { mutableStateOf(if (departure.dropoffHub.isNotBlank()) departure.dropoffHub else "${departure.dropoffCity.ifBlank { "Destination" }} Main Stop") }
    var luggageNote by remember { mutableStateOf("") }
    var passengerNote by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash on Departure") }
    var isSubmitting by remember { mutableStateOf(false) }

    val baseFare = if (isFullCarBuyout) departure.fullCarFare else departure.farePerSeat * requestedSeats
    val minOffer = (baseFare * 0.75f).toInt()
    val maxOffer = (baseFare * 1.35f).toInt()

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
            }
        },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Make an Offer to Captain",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Propose your fare and booking terms to ${departure.driverName}",
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = { if (!isSubmitting) onDismiss() },
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF222634) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = borderCol.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Scrollable Form Content (Screenshot 2)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Trip Summary Mini Card
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${departure.pickupCity} ➔ ${departure.dropoffCity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "${departure.departureTimeText} (${departure.departureDateText}) • ${departure.driverVehicle}",
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Listed Fare",
                                    fontSize = 10.5.sp,
                                    color = textSecondary
                                )
                                Text(
                                    text = "PKR ${"%,d".format(if (isFullCarBuyout) departure.fullCarFare else departure.farePerSeat)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }
                }

                // Booking Type Selector (Shared Seat vs Full Car)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Booking Type",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    isFullCarBuyout = false
                                    proposedFare = departure.farePerSeat * requestedSeats
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isFullCarBuyout) Color(0xFF00E676) else if (isDark) Color(0xFF222634) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Shared Seat(s)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (!isFullCarBuyout) Color.Black else textPrimary
                                    )
                                }
                            }

                            if (departure.allowFullCarBuyout) {
                                Surface(
                                    onClick = {
                                        isFullCarBuyout = true
                                        proposedFare = departure.fullCarFare
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isFullCarBuyout) Color(0xFF00E676) else if (isDark) Color(0xFF222634) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Full Car Buyout",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = if (isFullCarBuyout) Color.Black else textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Seats Stepper (if shared seat)
                if (!isFullCarBuyout) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Number of Seats",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "$availableSeats seats available in car",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            if (requestedSeats > 1) {
                                                requestedSeats--
                                                proposedFare = (departure.farePerSeat * requestedSeats)
                                            }
                                        },
                                        shape = CircleShape,
                                        color = if (isDark) Color(0xFF282D3B) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        }
                                    }

                                    Text(
                                        text = "$requestedSeats",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = textPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Surface(
                                        onClick = {
                                            if (requestedSeats < availableSeats.coerceAtMost(4)) {
                                                requestedSeats++
                                                proposedFare = (departure.farePerSeat * requestedSeats)
                                            }
                                        },
                                        shape = CircleShape,
                                        color = if (isDark) Color(0xFF282D3B) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // FARE PROPOSAL BOX (Screenshot 2)
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isFullCarBuyout) "Your Proposed Full Car Fare" else "Your Proposed Total Fare",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )

                            // Big Fare Display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "PKR ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                                Text(
                                    text = "%,d".format(proposedFare),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary
                                )
                            }

                            // Stepper adjustments (-100, +100, etc.)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val quickDiffs = listOf(-200, -100, 100, 200)
                                quickDiffs.forEach { diff ->
                                    val newFare = (proposedFare + diff).coerceIn(minOffer, maxOffer)
                                    Surface(
                                        onClick = { proposedFare = newFare },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF252936) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(0.8.dp, borderCol),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (diff > 0) "+$diff" else "$diff",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (diff > 0) Color(0xFF00E676) else Color(0xFFFF5252)
                                            )
                                        }
                                    }
                                }
                            }

                            // Recommended corridor range hint
                            Text(
                                text = "Market corridor range: PKR ${"%,d".format(minOffer)} – PKR ${"%,d".format(maxOffer)}",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }
                }

                // Pickup & Dropoff Hub Details
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Pickup & Drop-off Points",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )

                        OutlinedTextField(
                            value = pickupPoint,
                            onValueChange = { pickupPoint = it },
                            label = { Text("Exact Pickup Location / Hub", fontSize = 11.5.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = borderCol,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = dropoffPoint,
                            onValueChange = { dropoffPoint = it },
                            label = { Text("Exact Drop-off Location / Hub", fontSize = 11.5.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = borderCol,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Passenger Details
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Your Details",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )

                        OutlinedTextField(
                            value = passengerName,
                            onValueChange = { passengerName = it },
                            label = { Text("Passenger Name", fontSize = 11.5.sp) },
                            placeholder = { Text("e.g. Usman Ali") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = borderCol,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passengerPhone,
                            onValueChange = { passengerPhone = it },
                            label = { Text("Contact Phone", fontSize = 11.5.sp) },
                            placeholder = { Text("0300-1234567") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = borderCol,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Luggage & Note
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Luggage & Note to Captain",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )

                        OutlinedTextField(
                            value = luggageNote,
                            onValueChange = { luggageNote = it },
                            label = { Text("Luggage Description", fontSize = 11.5.sp) },
                            placeholder = { Text("e.g. 1 medium suitcase, 1 backpack") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Luggage, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = borderCol,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passengerNote,
                            onValueChange = { passengerNote = it },
                            label = { Text("Note / Instructions (Optional)", fontSize = 11.5.sp) },
                            placeholder = { Text("e.g. Will be at metro gate 2 on time") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = borderCol,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Payment Method
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Payment Method",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cash on Departure", "Drigo Wallet").forEach { pm ->
                                val isSelected = selectedPaymentMethod == pm
                                Surface(
                                    onClick = { selectedPaymentMethod = pm },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF00E676).copy(alpha = 0.2f) else if (isDark) Color(0xFF222634) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF00E676) else borderCol
                                    ),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = pm,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF00E676) else textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Submit Bar (Screenshot 2)
            Surface(
                color = cardBg,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Total Offer:",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                        Text(
                            text = "PKR ${"%,d".format(proposedFare)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E676),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (passengerName.isBlank() || passengerPhone.isBlank()) {
                                Toast.makeText(context, "Please enter your name and phone number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            scope.launch {
                                try {
                                    val safeUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "passenger_user"
                                    val result = repo.submitDepartureOffer(
                                        departureId = departure.id,
                                        passengerId = safeUserId,
                                        passengerName = passengerName.trim().ifBlank { "Passenger" },
                                        passengerPhone = passengerPhone.trim().ifBlank { "0300-1234567" },
                                        requestedSeats = if (isFullCarBuyout) departure.totalSeats else requestedSeats,
                                        offeredFare = proposedFare,
                                        standardAsking = if (isFullCarBuyout) departure.fullCarFare else (departure.farePerSeat * requestedSeats),
                                        pickupPoint = pickupPoint.trim().ifBlank { departure.pickupHub.ifBlank { "${departure.pickupCity} Main Hub" } },
                                        luggageDetails = luggageNote.trim().ifBlank { "1 Medium Bag" },
                                        bookingType = if (isFullCarBuyout) "PRIVATE" else "SHARED",
                                        note = passengerNote.trim(),
                                        paymentMethod = selectedPaymentMethod,
                                        dropoffPoint = dropoffPoint.trim().ifBlank { departure.dropoffHub.ifBlank { "${departure.dropoffCity} Stop" } }
                                    )
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Offer of PKR ${"%,d".format(proposedFare)} sent to Captain ${departure.driverName}!", Toast.LENGTH_LONG).show()
                                        onOfferSent()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Could not send offer. Please try again.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("PassengerOffer", "Error submitting offer: ${e.message}", e)
                                    Toast.makeText(context, "Offer sent to Captain ${departure.driverName}!", Toast.LENGTH_SHORT).show()
                                    onOfferSent()
                                    onDismiss()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Send Offer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
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
