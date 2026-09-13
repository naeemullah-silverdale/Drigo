package com.example.ui.screens.driver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.DriverHistoryItem
import com.example.ui.theme.DrigoBrandPurple
import com.example.ui.theme.drigoColors
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverTripHistoryScreen(
    driverName: String,
    completedTrips: List<DriverHistoryItem>,
    onOpenDrawer: () -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTripForDetail by remember { mutableStateOf<DriverHistoryItem?>(null) }
    var showReportIssueDialog by remember { mutableStateOf<DriverHistoryItem?>(null) }
    val context = LocalContext.current

    val filteredList = remember(searchQuery, selectedFilter, completedTrips) {
        completedTrips.filter { trip ->
            val matchesFilter = when (selectedFilter) {
                "Completed" -> trip.status == "COMPLETED"
                "Cancelled" -> trip.status == "CANCELLED"
                "Cash" -> trip.paymentMethod.contains("Cash", true)
                "Digital" -> !trip.paymentMethod.contains("Cash", true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    trip.passengerName.contains(searchQuery, ignoreCase = true) ||
                    trip.pickupTitle.contains(searchQuery, ignoreCase = true) ||
                    trip.destinationTitle.contains(searchQuery, ignoreCase = true) ||
                    trip.category.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val totalNetEarnings = remember(completedTrips) {
        completedTrips.filter { it.status == "COMPLETED" }.sumOf { it.netEarningsPkr }
    }
    val totalRidesCount = remember(completedTrips) {
        completedTrips.count { it.status == "COMPLETED" }
    }
    val isDark = MaterialTheme.drigoColors.isDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF222838) else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("driver_history_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Requests",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF222838) else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("driver_history_drawer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Drawer",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trip History",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$totalRidesCount Completed • PKR ${NumberFormat.getNumberInstance(Locale.US).format(totalNetEarnings)} Net Earned",
                                fontSize = 12.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary Analytics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 1: Completed Rides
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.drigoColors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Total Rides", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$totalRidesCount",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Card 2: Net Earnings
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.drigoColors.cardBorder),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Net Earned", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "PKR ${NumberFormat.getNumberInstance(Locale.US).format(totalNetEarnings)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }

                        // Card 3: Avg Fare
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.drigoColors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Avg / Ride", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val avg = if (totalRidesCount > 0) totalNetEarnings / totalRidesCount else 0
                                Text(
                                    text = "PKR $avg",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF81D4FA) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search passenger or location...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = DrigoBrandPurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("driver_history_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("All", "Completed", "Cancelled", "Cash", "Digital").forEach { filter ->
                            item {
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DrigoBrandPurple,
                                        selectedLabelColor = Color.White,
                                        containerColor = if (isDark) Color(0xFF1E2433) else MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = if (isDark) Color(0xFFB0BEC5) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedFilter == filter,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                                        selectedBorderColor = DrigoBrandPurple
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Trips List
            if (filteredList.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No trips found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Try adjusting your search query or filter.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredList) { trip ->
                        val isCompleted = trip.status == "COMPLETED"
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.drigoColors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header: Passenger Name & Ride Category Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isDark) Color(0xFF2A3144) else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFF81D4FA) else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = trip.passengerName,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.5.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "★ ${trip.passengerRating}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFFFD54F),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = trip.dateFormatted,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    // Category Pill + Status
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCompleted) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFFEF5350).copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, if (isCompleted) Color(0xFF00E676).copy(alpha = 0.5f) else Color(0xFFEF5350).copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = if (isCompleted) "✓ Completed" else "✕ Cancelled",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCompleted) Color(0xFF00E676) else Color(0xFFEF5350),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = trip.category,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF90A4AE)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Route Details (Pickup -> Destination)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Surface(shape = CircleShape, color = Color(0xFF00E676), modifier = Modifier.size(8.dp)) {}
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(24.dp)
                                                .background(if (isDark) Color(0xFF37474F) else MaterialTheme.colorScheme.outlineVariant)
                                        )
                                        Surface(shape = CircleShape, color = Color(0xFFEF5350), modifier = Modifier.size(8.dp)) {}
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = trip.pickupTitle,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = trip.destinationTitle,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Fare & Details Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) Color(0xFF222838) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Text(
                                                text = trip.paymentMethod,
                                                fontSize = 11.sp,
                                                color = if (isDark) Color(0xFFFFD54F) else Color(0xFFF57F17),
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${trip.distanceKm} km • ${trip.durationMins} mins",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "PKR ${NumberFormat.getNumberInstance(Locale.US).format(trip.farePkr)}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isCompleted) Color(0xFF00E676) else Color.Gray
                                        )
                                        if (isCompleted) {
                                            Text(
                                                text = "Net: PKR ${NumberFormat.getNumberInstance(Locale.US).format(trip.netEarningsPkr)}",
                                                fontSize = 10.5.sp,
                                                color = if (isDark) Color(0xFF81D4FA) else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedTripForDetail = trip },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = if (isDark) Color(0xFF81D4FA) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Receipt & Details", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    OutlinedButton(
                                        onClick = { showReportIssueDialog = trip },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Report Issue", fontSize = 11.5.sp, color = Color(0xFFEF5350))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Receipt Modal Bottom Sheet
        if (selectedTripForDetail != null) {
            val trip = selectedTripForDetail!!
            ModalBottomSheet(
                onDismissRequest = { selectedTripForDetail = null },
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Trip Receipt & Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Trip ID: ${trip.id}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { selectedTripForDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Passenger", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(trip.passengerName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Date & Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(trip.dateFormatted, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ride Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(trip.category, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF81D4FA) else MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payment Method", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(trip.paymentMethod, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFFD54F) else Color(0xFFF57F17))
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Fare Breakdown", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF81D4FA) else MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Base Booking Fare", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("PKR ${trip.baseFarePkr}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Distance Charge (${trip.distanceKm} km)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("PKR ${trip.distanceFarePkr}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (trip.tollPkr > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tolls & Extra Surcharges", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("PKR ${trip.tollPkr}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Fare Collected", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("PKR ${trip.farePkr}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Drigo Platform Fee (10%)", fontSize = 12.sp, color = Color(0xFFEF5350))
                                Text("- PKR ${trip.platformFeePkr}", fontSize = 12.sp, color = Color(0xFFEF5350))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Driver Net Payout", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676))
                                Text("PKR ${trip.netEarningsPkr}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Receipt exported / shared successfully!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Receipt", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { selectedTripForDetail = null },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Close", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Report Issue Dialog
        if (showReportIssueDialog != null) {
            val trip = showReportIssueDialog!!
            var selectedIssueReason by remember { mutableStateOf("Fare mismatch") }

            AlertDialog(
                onDismissRequest = { showReportIssueDialog = null },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                title = { Text("Report Issue for Trip #${trip.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text("Select problem encountered during trip with ${trip.passengerName}:", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        listOf("Fare mismatch / unpaid cash", "Passenger behavior", "Safety concerns", "Route discrepancy / wrong location").forEach { reason ->
                            Surface(
                                onClick = { selectedIssueReason = reason },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedIssueReason == reason) Color(0xFFEF5350).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (selectedIssueReason == reason) Color(0xFFEF5350) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = reason,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showReportIssueDialog = null
                            Toast.makeText(context, "Support ticket submitted for Trip #${trip.id}!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Submit Ticket", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportIssueDialog = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
