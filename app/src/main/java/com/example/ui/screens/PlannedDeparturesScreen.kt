package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.PlannedDepartureCardSkeleton
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedDeparturesScreen(
    driverId: String,
    driverName: String,
    driverPhone: String,
    onBackClick: () -> Unit,
    onPostRideClick: () -> Unit,
    onManageDeparture: (PlannedDeparture) -> Unit = {},
    onStartActiveRide: (PlannedDeparture) -> Unit = {},
    onOpenSos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }

    val departures by remember(driverId) { repo.observeDriverPlannedDepartures(driverId) }.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = History
    var isInitialLoading by remember { mutableStateOf(true) }

    LaunchedEffect(driverId) {
        android.util.Log.d("DrigoPlannedDepartures", "PlannedDeparturesScreen initialized: driverId='$driverId', driverName='$driverName', driverPhone='$driverPhone'")
        delay(400L)
        isInitialLoading = false
    }

    LaunchedEffect(departures.size) {
        if (departures.isNotEmpty()) {
            isInitialLoading = false
        }
    }

    val activeRides by remember(departures) {
        derivedStateOf {
            departures
                .filter {
                    it.status.equals("ACTIVE", true) ||
                    it.status.equals("FULL", true) ||
                    it.status.equals("IN_PROGRESS", true) ||
                    it.status.equals("SCHEDULED", true) ||
                    it.status.equals("OPEN", true)
                }
                .distinctBy { it.id }
        }
    }
    val pastRides by remember(departures) {
        derivedStateOf {
            departures
                .filter { it.status.equals("COMPLETED", true) || it.status.equals("CANCELLED", true) }
                .distinctBy { it.id }
        }
    }

    val uniqueDisplayedRides by remember(selectedTab, activeRides, pastRides) {
        derivedStateOf {
            if (selectedTab == 0) activeRides else pastRides
        }
    }

    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_planned_departures_screen"),
        floatingActionButtonPosition = FabPosition.End,
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
                            text = "Planned Departures",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeRides.size} Active scheduled ${if (activeRides.size == 1) "ride" else "rides"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                    // Avatar with online status
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
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPostRideClick,
                containerColor = Color(0xFF00C853),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = {
                    Text(
                        text = "Schedule Ride",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .testTag("post_planned_ride_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8FB))
        ) {
            // Segmented Tab Bar (Active / History)
            Surface(
                color = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp)
                ) {
                    // Active Tab
                    Surface(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 0) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == 0) 1.5.dp else 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color(0xFF0F172A) else Color(0xFF64748B),
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (selectedTab == 0) Color(0xFFE8F5E9) else Color(0xFFCBD5E1)
                            ) {
                                Text(
                                    text = "${activeRides.size}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedTab == 0) Color(0xFF2E7D32) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // History Tab
                    Surface(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 1) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == 1) 1.5.dp else 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "History",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color(0xFF0F172A) else Color(0xFF64748B),
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (selectedTab == 1) Color(0xFFE0E7FF) else Color(0xFFCBD5E1)
                            ) {
                                Text(
                                    text = "${pastRides.size}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedTab == 1) Color(0xFF3730A3) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Body Content: Skeletons vs Empty State vs Clean List
            if (isInitialLoading && uniqueDisplayedRides.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlannedDepartureCardSkeleton()
                    PlannedDepartureCardSkeleton()
                    PlannedDepartureCardSkeleton()
                }
            } else if (uniqueDisplayedRides.isEmpty()) {
                // Clean and Polished Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF3E8FF),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = DrigoBrandPurple,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (selectedTab == 0) "No Active Departures" else "No Completed Departures",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontSize = 17.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (selectedTab == 0)
                                "Post your intercity departure to allow passengers along the corridor to book seats or full car buyout."
                            else
                                "Your completed and past scheduled rides will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uniqueDisplayedRides, key = { it.id }) { departure ->
                        PlannedDepartureCard(
                            departure = departure,
                            onManageClick = { onManageDeparture(departure) },
                            onStartActiveRide = {
                                if (!departure.status.equals("ACTIVE", true)) {
                                    scope.launch {
                                        repo.updatePlannedDepartureStatus(departure.id, "ACTIVE")
                                    }
                                }
                                onStartActiveRide(departure.copy(status = "ACTIVE"))
                            },
                            onShareClick = {
                                val shareText = "🚗 Drigo City-to-City Departure\n" +
                                        "Route: ${departure.pickupCity} → ${departure.dropoffCity}\n" +
                                        "Pickup: ${departure.pickupHub} (${departure.pickupStopDetails})\n" +
                                        "Dropoff: ${departure.dropoffHub} (${departure.dropoffStopDetails})\n" +
                                        "Date & Time: ${departure.departureDateText} • ${departure.departureTimeText}\n" +
                                        "Corridor: ${departure.corridorName}\n" +
                                        "Fare: PKR ${departure.farePerSeat} / Seat (${departure.availableSeats} of ${departure.totalSeats} seats available)\n" +
                                        (if (departure.allowFullCarBuyout) "Full Car Buyout: PKR ${departure.fullCarFare}\n" else "") +
                                        "Book instantly on the Drigo App!"

                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Drigo Route: ${departure.pickupCity} → ${departure.dropoffCity}")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Departure Route"))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlannedDepartureCard(
    departure: PlannedDeparture,
    onManageClick: () -> Unit,
    onShareClick: () -> Unit,
    onStartActiveRide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTripActive = departure.status.equals("ACTIVE", true)
    // Derive actual seat count strictly based on the Firestore model
    val actualAvailableSeats = remember(departure) {
        when {
            departure.isFullCarBooked -> 0
            departure.bookedSeatsCount > 0 -> (departure.totalSeats - departure.bookedSeatsCount).coerceIn(0, departure.totalSeats)
            departure.availableSeats in 0..departure.totalSeats -> departure.availableSeats
            else -> (departure.totalSeats - departure.bookedSeatsCount).coerceIn(0, departure.totalSeats)
        }
    }
    val isFull = actualAvailableSeats == 0 || departure.status.equals("FULL", true)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isTripActive) 3.dp else 1.5.dp,
        border = BorderStroke(
            if (isTripActive) 1.5.dp else 1.dp,
            if (isTripActive) Color(0xFF00C853) else Color(0xFFE2E8F0)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onManageClick()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Status Pill (Left) & Scheduled Date/Time Badge (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = when {
                        departure.status.equals("FULL", true) -> Color(0xFFEDE9FE)
                        departure.status.equals("COMPLETED", true) -> Color(0xFFF1F5F9)
                        departure.status.equals("CANCELLED", true) -> Color(0xFFFEE2E2)
                        else -> Color(0xFFE8F5E9)
                    },
                    border = BorderStroke(
                        0.5.dp,
                        when {
                            departure.status.equals("FULL", true) -> Color(0xFFDDD6FE)
                            departure.status.equals("COMPLETED", true) -> Color(0xFFCBD5E1)
                            departure.status.equals("CANCELLED", true) -> Color(0xFFFECACA)
                            else -> Color(0xFF81C784)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    when {
                                        departure.status.equals("FULL", true) -> Color(0xFF7C3AED)
                                        departure.status.equals("COMPLETED", true) -> Color(0xFF64748B)
                                        departure.status.equals("CANCELLED", true) -> Color(0xFFDC2626)
                                        else -> Color(0xFF00C853)
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                departure.status.equals("FULL", true) -> "Full"
                                departure.status.equals("COMPLETED", true) -> "Completed"
                                departure.status.equals("CANCELLED", true) -> "Cancelled"
                                else -> "Active"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                departure.status.equals("FULL", true) -> Color(0xFF6D28D9)
                                departure.status.equals("COMPLETED", true) -> Color(0xFF475569)
                                departure.status.equals("CANCELLED", true) -> Color(0xFFB91C1C)
                                else -> Color(0xFF2E7D32)
                            }
                        )
                    }
                }

                // Date & Time Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(0.5.dp, Color(0xFFBBF7D0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${departure.departureDateText} • ${departure.departureTimeText}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Offers badge banner if riders placed offers
            if (departure.offersReceivedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🏷️ ${departure.offersReceivedCount} Rider ${if (departure.offersReceivedCount == 1) "Offer" else "Offers"} Received",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "Review →",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Full-Width Route Stack: Pickup -> Connector -> Dropoff
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                // Origin (Pickup)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(11.dp)
                            .background(Color(0xFF00C853), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = departure.pickupCity,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "• ${departure.pickupHub}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (departure.pickupStopDetails.isNotBlank()) {
                            Text(
                                text = "Pickup: ${departure.pickupStopDetails}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Connector line with route distance tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.5.dp)
                            .height(16.dp)
                            .width(2.dp)
                            .background(Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = departure.corridorSubtitle.ifBlank { departure.corridorName },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Destination (Dropoff)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(11.dp)
                            .background(Color(0xFFD32F2F), RoundedCornerShape(2.dp))
                            .border(1.5.dp, Color.White, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = departure.dropoffCity,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "• ${departure.dropoffHub}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (departure.dropoffStopDetails.isNotBlank()) {
                            Text(
                                text = "Dropoff: ${departure.dropoffStopDetails}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing & Seats Banner (Full Width Tinted Box)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FARE / SEAT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "PKR ${"%,d".format(departure.farePerSeat)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857)
                            )
                        }

                        // Seat occupancy badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (!isFull) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            border = BorderStroke(
                                0.5.dp,
                                if (!isFull) Color(0xFF86EFAC) else Color(0xFFFECACA)
                            )
                        ) {
                            Text(
                                text = if (isFull) "💺 Full (0 seats free)" else "💺 $actualAvailableSeats of ${departure.totalSeats} seats free",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isFull) Color(0xFF166534) else Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (departure.allowFullCarBuyout) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Full Car Buyout: PKR ${"%,d".format(departure.fullCarFare)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Feature Chips Row (Tolls Pre-cleared, Vehicle, etc.)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (departure.tollsPreCleared) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "✓ Tolls Pre-cleared",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }
                }

                if (departure.driverVehicle.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "🚗 ${departure.driverVehicle}",
                            fontSize = 9.5.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions Row: Share Route, Manage Trip & Start Ride
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Share Route button
                OutlinedButton(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E)),
                    border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(0.85f).height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Share",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF0F766E)
                    )
                }

                // Manage / Details button (3 Tabs: Details, Offers, Manifest)
                Button(
                    onClick = onManageClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.05f).height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Manage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                // Start Ride / Live Radar button
                Button(
                    onClick = onStartActiveRide,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTripActive) Color(0xFF00C853) else Color(0xFF16A34A)
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.1f).height(38.dp)
                ) {
                    Icon(
                        imageVector = if (isTripActive) Icons.Default.Navigation else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isTripActive) "Live Radar" else "Start Ride",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
