package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.DrigoBrandPurple
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
    onOpenSos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }

    val departures by repo.observeDriverPlannedDepartures(driverId).collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = History

    val activeRides = remember(departures) {
        departures.filter { it.status.equals("ACTIVE", true) || it.status.equals("FULL", true) || it.status.equals("IN_PROGRESS", true) }
    }
    val pastRides = remember(departures) {
        departures.filter { it.status.equals("COMPLETED", true) || it.status.equals("CANCELLED", true) }
    }

    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_planned_departures_screen"),
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
                            maxLines = 1
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
                        color = DrigoBrandPurple.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, Color(0xFF00C853)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = DrigoBrandPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPostRideClick,
                containerColor = Color(0xFF00C853),
                contentColor = Color.White,
                shape = RoundedCornerShape(100.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, end = 4.dp)
                    .testTag("post_planned_ride_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Post Ride",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF4F6F9))
        ) {
            // Segmented Tab Bar (Active / History)
            Surface(
                color = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (selectedTab == 0) Color(0xFFE8F5E9) else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "${activeRides.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) Color(0xFF2E7D32) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // History / Past Tab
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
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (selectedTab == 1) Color(0xFFE0E7FF) else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "${pastRides.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) Color(0xFF3730A3) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // List of Planned Departures
            val displayedRides = if (selectedTab == 0) activeRides else pastRides

            if (displayedRides.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DrigoBrandPurple.copy(alpha = 0.1f),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = DrigoBrandPurple,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (selectedTab == 0) "No Active Departures" else "No Completed Departures",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedTab == 0)
                                "Post your intercity departure to allow passengers along the corridor to book seats or full car buyout."
                            else
                                "Your past and completed city-to-city trips will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            lineHeight = 18.sp
                        )
                        if (selectedTab == 0) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onPostRideClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Post Scheduled Ride", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedRides, key = { it.id }) { departure ->
                        PlannedDepartureCard(
                            departure = departure,
                            onManageClick = { onManageDeparture(departure) },
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
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onManageClick() }
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
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${departure.departureDateText} • ${departure.departureTimeText}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            // Optional: Offers badge banner if riders placed offers
            if (departure.offersReceivedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏷️ ${departure.offersReceivedCount} Rider ${if (departure.offersReceivedCount == 1) "Offer" else "Offers"} Received",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            .size(12.dp)
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "• ${departure.pickupHub}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (departure.pickupStopDetails.isNotBlank()) {
                            Text(
                                text = "Pickup: ${departure.pickupStopDetails}",
                                fontSize = 11.sp,
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
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .height(18.dp)
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                            .size(12.dp)
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "• ${departure.dropoffHub}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (departure.dropoffStopDetails.isNotBlank()) {
                            Text(
                                text = "Dropoff: ${departure.dropoffStopDetails}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "PKR ${"%,d".format(departure.farePerSeat)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857)
                            )
                        }

                        // Seat occupancy badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (departure.availableSeats > 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            border = BorderStroke(
                                0.5.dp,
                                if (departure.availableSeats > 0) Color(0xFF86EFAC) else Color(0xFFFECACA)
                            )
                        ) {
                            Text(
                                text = "💺 ${departure.availableSeats} of ${departure.totalSeats} seats free",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (departure.availableSeats > 0) Color(0xFF166534) else Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (departure.allowFullCarBuyout) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Full Car Buyout: PKR ${"%,d".format(departure.fullCarFare)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
                            fontSize = 10.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row: Manage Trip & Share Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share Route button
                OutlinedButton(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E)),
                    border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share Route",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F766E)
                    )
                }

                // Manage / Details button
                Button(
                    onClick = onManageClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(
                        text = "Manage Trip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
