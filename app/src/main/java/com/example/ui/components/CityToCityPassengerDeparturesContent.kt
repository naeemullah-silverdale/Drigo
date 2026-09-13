package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PlannedDeparture
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.InDriveLimeGreen
import com.example.ui.theme.drigoColors
import kotlinx.coroutines.launch

/**
 * Full Passenger City-to-City departures screen content matching Screenshot 1 & 2.
 */
@Composable
fun CityToCityPassengerDeparturesContent(
    onBackClick: () -> Unit,
    onSosClick: () -> Unit = {},
    initialFromCity: String = "Islamabad",
    initialToCity: String = "Lahore",
    isSheetMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { FirebaseRepository.getInstance(context) }
    val allDepartures by repo.observeAllPlannedDepartures().collectAsState(initial = emptyList())

    val isDark = MaterialTheme.drigoColors.isDark
    val screenBg = if (isDark) Color(0xFF0F1116) else MaterialTheme.colorScheme.background
    val cardBg = if (isDark) Color(0xFF181B23) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color(0xFF282C3A) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant

    var fromCity by remember { mutableStateOf(initialFromCity) }
    var fromHub by remember { mutableStateOf("G-9 & F-10 Hubs") }
    var toCity by remember { mutableStateOf(initialToCity) }
    var toHub by remember { mutableStateOf("Motorway Direct") }

    var selectedDateText by remember { mutableStateOf("Tomorrow, 25 Oct") }
    var selectedWindowText by remember { mutableStateOf("Morning departures") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var selectedSortOption by remember { mutableStateOf("Earliest Departure") }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedDepartureForOffer by remember { mutableStateOf<PlannedDeparture?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showWindowDialog by remember { mutableStateOf(false) }

    val quickCities = remember {
        listOf("Peshawar", "Faisalabad", "Multan", "Rawalpindi", "Gujranwala", "Sialkot")
    }

    val filteredDepartures = remember(allDepartures, fromCity, toCity, selectedFilterIndex, selectedSortOption) {
        val base = allDepartures.filter { dep ->
            dep.status.equals("ACTIVE", ignoreCase = true) &&
                    (fromCity.isBlank() || dep.pickupCity.contains(fromCity, ignoreCase = true) || fromCity.contains(dep.pickupCity, ignoreCase = true)) &&
                    (toCity.isBlank() || dep.dropoffCity.contains(toCity, ignoreCase = true) || toCity.contains(dep.dropoffCity, ignoreCase = true)) &&
                    when (selectedFilterIndex) {
                        1 -> dep.corridorName.contains("M-2", ignoreCase = true) || dep.corridorSubtitle.contains("M-2", ignoreCase = true)
                        2 -> dep.allowFullCarBuyout
                        else -> true
                    }
        }
        when (selectedSortOption) {
            "Highest Rating" -> base.sortedByDescending { it.driverRating }
            "Lowest Price" -> base.sortedBy { it.farePerSeat }
            else -> base.sortedBy { it.departureTimeText }
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

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF00E676), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "From (Departure City)",
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

                                    HorizontalDivider(
                                        color = borderCol.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(start = 20.dp),
                                        thickness = 0.8.dp
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFFFF5252), RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "To (Drop-off City)",
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "QUICK:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                quickCities.forEach { city ->
                                    val isSelected = toCity.equals(city, ignoreCase = true)
                                    Surface(
                                        onClick = {
                                            toCity = city
                                            toHub = "Direct Hub"
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
                        "All Rides (${filteredDepartures.size.coerceAtLeast(3)})",
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
                            text = "AVAILABLE CAPTAINS (${filteredDepartures.size.coerceAtLeast(3)})",
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
                items(filteredDepartures, key = { it.id }) { departure ->
                    ModernPassengerDepartureCard(
                        departure = departure,
                        onSendOffer = {
                            selectedDepartureForOffer = departure
                        }
                    )
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

        // SEND BOOKING OFFER MODAL SHEET (Screenshot 2)
        if (selectedDepartureForOffer != null) {
            ModernPassengerMakeOfferSheet(
                departure = selectedDepartureForOffer!!,
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
    onSendOffer: () -> Unit
) {
    val isDark = MaterialTheme.drigoColors.isDark
    val cardBg = if (isDark) Color(0xFF181B23) else MaterialTheme.colorScheme.surface
    val routeBoxBg = if (isDark) Color(0xFF1F2330) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderCol = if (isDark) Color(0xFF282C3A) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant
    val availableSeats = (departure.totalSeats - departure.bookedSeatsCount).coerceAtLeast(0)
    val isFull = availableSeats <= 0

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
            // Row 0: Top Header Badges (Screenshot 1: SUPER CAPTAIN, LADIES FRIENDLY, ECONOMY CHOICE, LAST 1 SEAT LEFT)
            val headerBadges = remember(departure) {
                val list = mutableListOf<Triple<String, Color, Color>>()
                if (departure.driverBadges.any { it.contains("Super Captain", ignoreCase = true) }) {
                    list.add(Triple("🚹 SUPER CAPTAIN", Color(0xFF1E88E5).copy(alpha = 0.15f), Color(0xFF29B6F6)))
                }
                if (departure.driverBadges.any { it.contains("Ladies Friendly", ignoreCase = true) }) {
                    list.add(Triple("🚺 LADIES FRIENDLY", Color(0xFFFF4081).copy(alpha = 0.15f), Color(0xFFFF4081)))
                }
                if (departure.driverBadges.any { it.contains("Economy Choice", ignoreCase = true) }) {
                    list.add(Triple("🌱 ECONOMY CHOICE", Color(0xFF7C4DFF).copy(alpha = 0.15f), Color(0xFFB388FF)))
                }
                if (availableSeats == 1) {
                    list.add(Triple("🔴 LAST 1 SEAT LEFT!", Color(0xFFFF5252).copy(alpha = 0.15f), Color(0xFFFF5252)))
                }
                if (departure.approvalWindowText.isNotBlank()) {
                    list.add(Triple("⏱ ${departure.approvalWindowText}", Color(0xFF00E676).copy(alpha = 0.12f), Color(0xFF00E676)))
                }
                list
            }

            if (headerBadges.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    headerBadges.forEach { (text, bg, textColor) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            border = BorderStroke(0.8.dp, textColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = text,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Row 1: Captain Avatar, Info + Fare Display
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
                    Box(modifier = Modifier.size(46.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = if (departure.driverName.contains("Tariq", ignoreCase = true)) Color(0xFF5C6BC0) else if (departure.driverName.contains("Zubair", ignoreCase = true)) Color(0xFF26A69A) else Color(0xFF1E88E5),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = departure.driverName.take(1).uppercase(),
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
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(10.dp).align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = departure.driverName,
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
                                text = "★ ${departure.driverRating}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFFFB300)
                            )
                            Text(
                                text = " • ${if (departure.driverTotalTrips > 0) departure.driverTotalTrips else 1240} trips",
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }

                        Text(
                            text = "${departure.driverVehicle} (${departure.driverPlateNumber})",
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Fare Display (Top Right)
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "PKR ${"%,d".format(departure.farePerSeat)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF00E676)
                        )
                        Text(
                            text = " / seat",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    if (departure.allowFullCarBuyout) {
                        Text(
                            text = "Buyout: PKR ${"%,d".format(departure.fullCarFare)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF29B6F6)
                        )
                    } else {
                        Text(
                            text = "Single seat only (no buyout)",
                            fontSize = 10.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            // Row 2: Verification / Feature Badges (Verified Captain, Top Rated, Instant Booking)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, Color(0xFF00E676).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Verified Captain",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                }

                if (departure.driverRating >= 4.8) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E88E5).copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, Color(0xFF29B6F6).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Top Rated",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF29B6F6)
                            )
                        }
                    }
                }

                if (departure.isInstantBooking) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00C853).copy(alpha = 0.18f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Instant Booking",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }
            }

            // Row 3: Route & Timeline Box Container (Screenshot 1)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = routeBoxBg,
                border = BorderStroke(1.dp, borderCol.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pickup Hub & Departure Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "DEPARTURE HUB",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    letterSpacing = 0.4.sp
                                )
                                Text(
                                    text = "${departure.pickupCity} (${departure.pickupHub.ifBlank { "G-9 Markaz Hub" }})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = departure.departureTimeText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                            Text(
                                text = "ON TIME",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00E676)
                            )
                        }
                    }

                    // Connecting vertical divider line
                    HorizontalDivider(
                        color = borderCol.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        thickness = 0.8.dp
                    )

                    // Dropoff Hub & Estimated Arrival Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(Color(0xFFFF5252), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "DESTINATION DROP-OFF",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    letterSpacing = 0.4.sp
                                )
                                Text(
                                    text = "${departure.dropoffCity} (${departure.dropoffHub.ifBlank { "DHA Phase 5 / Ring Rd" }})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = departure.estimatedArrival.ifBlank { "~12:15 PM" },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF5252)
                            )
                            Text(
                                text = "Est. Arrival",
                                fontSize = 9.5.sp,
                                color = textSecondary
                            )
                        }
                    }

                    // Info row inside route container: Distance & Toll Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Navigation,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = departure.corridorSubtitle.ifBlank { "375 km • ~4h 15m via M-2" },
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E88E5).copy(alpha = 0.15f),
                            border = BorderStroke(0.8.dp, Color(0xFF29B6F6).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF29B6F6),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (departure.tollsPreCleared) "TOLLS PRE-CLEARED" else "TOLLS INCLUDED",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF29B6F6)
                                )
                            }
                        }
                    }
                }
            }

            // Row 4: Seat Availability Dots & Luggage Badges (Screenshot 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot seat availability indicator box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF222634) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.8.dp, borderCol)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Render green dots for available, grey for booked
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(departure.totalSeats) { index ->
                                val isSeatAvailable = index < availableSeats
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isSeatAvailable) Color(0xFF00E676) else Color.Gray.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                )
                            }
                        }
                        Text(
                            text = if (isFull) "Full" else "$availableSeats of ${departure.totalSeats} Seats Open",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFull) Color(0xFFFF5252) else textPrimary
                        )
                    }
                }

                // Bag Policy Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF222634) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Luggage,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = departure.luggagePolicy.ifBlank { "2 Bags max / rider" },
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            // Row 5: Select & Send Offer Primary Button (Screenshot 1)
            Button(
                onClick = onSendOffer,
                enabled = !isFull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006837),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF2A2D37),
                    disabledContentColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isFull) "Departure Full" else "Select & Send Offer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
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

    var passengerName by remember { mutableStateOf("") }
    var passengerPhone by remember { mutableStateOf("") }
    var pickupPoint by remember { mutableStateOf(departure.pickupHub.ifBlank { "G-9 Markaz Islamabad" }) }
    var dropoffPoint by remember { mutableStateOf(departure.dropoffHub.ifBlank { "Thokar Niaz Baig Lahore" }) }
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
                                        modifier = Modifier.size(36.dp)
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
                                        modifier = Modifier.padding(horizontal = 6.dp)
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
                                        modifier = Modifier.size(36.dp)
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
                                        modifier = Modifier.weight(1f).height(32.dp)
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
                    Column {
                        Text(
                            text = "Total Offer:",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                        Text(
                            text = "PKR ${"%,d".format(proposedFare)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E676)
                        )
                    }

                    Button(
                        onClick = {
                            if (passengerName.isBlank() || passengerPhone.isBlank()) {
                                Toast.makeText(context, "Please enter your name and phone number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            scope.launch {
                                val result = repo.submitDepartureOffer(
                                    departureId = departure.id,
                                    passengerId = "pass_${System.currentTimeMillis().toString().takeLast(6)}",
                                    passengerName = passengerName.trim(),
                                    passengerPhone = passengerPhone.trim(),
                                    requestedSeats = if (isFullCarBuyout) departure.totalSeats else requestedSeats,
                                    offeredFare = proposedFare,
                                    standardAsking = if (isFullCarBuyout) departure.fullCarFare else (departure.farePerSeat * requestedSeats),
                                    pickupPoint = pickupPoint.trim(),
                                    luggageDetails = luggageNote.trim().ifBlank { "1 Medium Bag" },
                                    bookingType = if (isFullCarBuyout) "PRIVATE" else "SHARED",
                                    note = passengerNote.trim(),
                                    paymentMethod = selectedPaymentMethod,
                                    dropoffPoint = dropoffPoint.trim()
                                )
                                isSubmitting = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Offer of PKR ${"%,d".format(proposedFare)} sent to Captain ${departure.driverName}!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Could not send offer. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .padding(start = 12.dp)
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
                                    text = "Send Offer to Captain",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
