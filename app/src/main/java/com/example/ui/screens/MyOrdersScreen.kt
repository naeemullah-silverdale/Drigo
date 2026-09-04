package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PassengerOrder
import com.example.data.model.PassengerOrderStatus
import com.example.data.local.AppDatabase
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.InDriveLimeGreen
import com.example.ui.components.PostRideRatingDialog
import com.example.ui.components.SafetyReportDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val PassengerOrderStatus.badgeColor: Color
    get() = when (this) {
        PassengerOrderStatus.SEARCHING -> Color(0xFFFFB300)
        PassengerOrderStatus.OFFER_RECEIVED -> InDriveLimeGreen
        PassengerOrderStatus.ACCEPTED, PassengerOrderStatus.DRIVER_COMING -> InDriveLimeGreen
        PassengerOrderStatus.DRIVER_ARRIVED -> Color(0xFF00E676)
        PassengerOrderStatus.IN_TRIP -> Color(0xFF29B6F6)
        PassengerOrderStatus.COMPLETED -> Color(0xFF81C784)
        PassengerOrderStatus.CANCELLED -> Color(0xFFE57373)
    }

val PassengerOrderStatus.displayName: String
    get() = when (this) {
        PassengerOrderStatus.SEARCHING -> "Searching Drivers"
        PassengerOrderStatus.OFFER_RECEIVED -> "Offer Received"
        PassengerOrderStatus.ACCEPTED, PassengerOrderStatus.DRIVER_COMING -> "Driver on the way"
        PassengerOrderStatus.DRIVER_ARRIVED -> "Driver Arrived"
        PassengerOrderStatus.IN_TRIP -> "On Trip"
        PassengerOrderStatus.COMPLETED -> "Completed"
        PassengerOrderStatus.CANCELLED -> "Cancelled"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    orders: List<PassengerOrder>,
    currentUserId: String = "passenger_user",
    currentUserName: String = "Passenger",
    onTrackOnMap: (PassengerOrder) -> Unit,
    onOpenChat: (PassengerOrder) -> Unit,
    onCancelOrder: (PassengerOrder) -> Unit,
    onRebookTrip: (PassengerOrder) -> Unit,
    onRequestNewRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = History

    // Rating & Safety Dialog State
    var ratingOrderTarget by remember { mutableStateOf<PassengerOrder?>(null) }
    var reportOrderTarget by remember { mutableStateOf<PassengerOrder?>(null) }

    val activeOrders = remember(orders) {
        orders.filter { it.status != PassengerOrderStatus.COMPLETED && it.status != PassengerOrderStatus.CANCELLED }
    }
    val pastOrders = remember(orders) {
        orders.filter { it.status == PassengerOrderStatus.COMPLETED || it.status == PassengerOrderStatus.CANCELLED }
    }

    Scaffold(
        containerColor = Color(0xFF14161B),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Orders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14161B),
                    titleContentColor = Color.White
                ),
                actions = {
                    if (activeOrders.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = InDriveLimeGreen.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, InDriveLimeGreen),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "${activeOrders.size} Active",
                                color = InDriveLimeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier.testTag("my_orders_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: Active Orders (with count badge) vs History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Tab
                Surface(
                    onClick = { selectedFilterTab = 0 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedFilterTab == 0) Color(0xFF232732) else Color(0xFF1A1C23),
                    border = BorderStroke(
                        1.dp,
                        if (selectedFilterTab == 0) InDriveLimeGreen else Color(0xFF2C303B)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Active Orders",
                            fontWeight = if (selectedFilterTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedFilterTab == 0) Color.White else Color(0xFFA0A6B5),
                            fontSize = 14.sp
                        )
                        if (activeOrders.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = InDriveLimeGreen,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${activeOrders.size}",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // History Tab
                Surface(
                    onClick = { selectedFilterTab = 1 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedFilterTab == 1) Color(0xFF232732) else Color(0xFF1A1C23),
                    border = BorderStroke(
                        1.dp,
                        if (selectedFilterTab == 1) InDriveLimeGreen else Color(0xFF2C303B)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Past Trips",
                                fontWeight = if (selectedFilterTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedFilterTab == 1) Color.White else Color(0xFFA0A6B5),
                                fontSize = 14.sp
                            )
                            if (pastOrders.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF383D4E),
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${pastOrders.size}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val displayList = if (selectedFilterTab == 0) activeOrders else pastOrders

            if (displayList.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E212B),
                            border = BorderStroke(1.dp, Color(0xFF2C303B)),
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedFilterTab == 0) Icons.Outlined.DirectionsCar else Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = InDriveLimeGreen,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Text(
                            text = if (selectedFilterTab == 0) "No Active Orders" else "No Trip History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )

                        Text(
                            text = if (selectedFilterTab == 0)
                                "When you request a ride or accept a driver offer, your live trip details will appear here."
                            else
                                "Your completed or past trips in the Drigo network will be saved here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA0A6B5),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        Button(
                            onClick = onRequestNewRide,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InDriveLimeGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Book a Ride",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayList, key = { it.id }) { order ->
                        if (selectedFilterTab == 0) {
                            AcceptedOrderCard(
                                order = order,
                                onTrackOnMap = { onTrackOnMap(order) },
                                onOpenChat = { onOpenChat(order) },
                                onCallDriver = {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${order.driverPhone}")
                                        }
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {}
                                },
                                onCancelOrder = { onCancelOrder(order) }
                            )
                        } else {
                            PastTripCard(
                                order = order,
                                onRebookTrip = { onRebookTrip(order) },
                                onRateTrip = { ratingOrderTarget = order },
                                onReportTrip = { reportOrderTarget = order }
                            )
                        }
                    }
                }
            }
        }

        // Post-Ride Rating Dialog Overlay
        if (ratingOrderTarget != null) {
            val order = ratingOrderTarget!!
            PostRideRatingDialog(
                rideId = order.id.ifBlank { order.requestId },
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                isDriver = false,
                targetId = order.driverId.ifBlank { order.driverPhone.ifBlank { "driver_${order.id}" } },
                targetName = order.driverName.ifBlank { "Driver Captain" },
                targetPhone = order.driverPhone,
                targetVehicleSummary = "${order.driverVehicleMake} ${order.driverVehicleModel}".trim(),
                targetPlateNumber = order.driverPlateNumber,
                targetRating = if (order.driverRating > 0) order.driverRating else 5.0,
                pickupTitle = order.pickupTitle,
                destinationTitle = order.destinationTitle,
                farePkr = order.agreedFare,
                onDismiss = { ratingOrderTarget = null },
                onRatingSubmitted = {
                    ratingOrderTarget = null
                },
                onOpenSafetyReport = {
                    reportOrderTarget = order
                }
            )
        }

        // Safety Incident Report Dialog Overlay
        if (reportOrderTarget != null) {
            val order = reportOrderTarget!!
            SafetyReportDialog(
                rideId = order.id,
                reporterId = currentUserId,
                reporterName = currentUserName,
                reporterPhone = "",
                isReporterDriver = false,
                reportedUserId = order.driverPhone.ifBlank { "driver_${order.id}" },
                reportedUserName = order.driverName.ifBlank { "Driver Captain" },
                driverPlateNumber = order.driverPlateNumber,
                pickupTitle = order.pickupTitle,
                destinationTitle = order.destinationTitle,
                onDismiss = { reportOrderTarget = null },
                onReportSubmitted = {
                    reportOrderTarget = null
                }
            )
        }
    }
}

@Composable
fun AcceptedOrderCard(
    order: PassengerOrder,
    onTrackOnMap: () -> Unit,
    onOpenChat: () -> Unit,
    onCallDriver: () -> Unit,
    onCancelOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E212B),
        border = BorderStroke(1.dp, Color(0xFF2E3342)),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Top Header: Status Badge + Live ETA / Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status chip with pulsating dot
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = order.status.badgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, order.status.badgeColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (order.status == PassengerOrderStatus.ACCEPTED || order.status == PassengerOrderStatus.IN_TRIP) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(pulseAlpha)
                                    .background(order.status.badgeColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = order.status.displayName.uppercase(),
                            color = order.status.badgeColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                // ETA or Scheduled Time
                if (order.scheduledTimeText != null) {
                    Text(
                        text = "📅 ${order.scheduledTimeText}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFA0A6B5),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "ETA: ~${order.etaMinutes} mins",
                        style = MaterialTheme.typography.labelMedium,
                        color = InDriveLimeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // 2. Driver Info Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF161820),
                border = BorderStroke(1.dp, Color(0xFF282C38)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Driver Avatar
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2E3342),
                        border = BorderStroke(1.5.dp, InDriveLimeGreen),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Driver Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Name, Rating & Vehicle
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.driverName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF2D323F)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${order.driverRating}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${order.driverVehicleColor} ${order.driverVehicleMake} ${order.driverVehicleModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A6B5),
                            fontSize = 12.sp
                        )
                    }

                    // Plate Number Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF252834),
                        border = BorderStroke(1.dp, Color(0xFF3B4050))
                    ) {
                        Text(
                            text = order.driverPlateNumber,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3. Route Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pickup
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = InDriveLimeGreen,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = order.pickupTitle.ifBlank { "Pickup Location" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Destination
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF5252),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = order.destinationTitle.ifBlank { "Destination Location" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Fare & Ride Type Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.rideCategory,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFA0A6B5),
                        fontSize = 12.sp
                    )
                    if (order.comments.isNotBlank()) {
                        Text(
                            text = "Note: ${order.comments}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "PKR ${"%,d".format(order.agreedFare)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            HorizontalDivider(color = Color(0xFF282C38), thickness = 1.dp)

            // 5. Action Buttons Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Track on Map
                Button(
                    onClick = onTrackOnMap,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InDriveLimeGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f).height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Track on Map",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Track",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                // Chat with Driver
                OutlinedButton(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF383D4E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = InDriveLimeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Call Driver
                Surface(
                    onClick = onCallDriver,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF252936),
                    border = BorderStroke(1.dp, Color(0xFF383D4E)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Cancel Order
                Surface(
                    onClick = onCancelOrder,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF252936),
                    border = BorderStroke(1.dp, Color(0xFF383D4E)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Ride",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PastTripCard(
    order: PassengerOrder,
    onRebookTrip: () -> Unit,
    onRateTrip: () -> Unit,
    onReportTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val dateString = remember(order.createdAt) {
        try {
            dateFormat.format(Date(order.createdAt))
        } catch (_: Exception) {
            "Recent Trip"
        }
    }

    // Check if this passenger already rated this ride
    var hasRated by remember { mutableStateOf(false) }
    LaunchedEffect(order.id) {
        launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context, this)
                val isRated = db.safetyDao().hasRatedRide(order.id, "PASSENGER")
                hasRated = isRated
            } catch (_: Exception) {}
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1B1D24),
        border = BorderStroke(1.dp, Color(0xFF292C37)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("past_order_card_${order.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Date & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8E95A5),
                    fontSize = 11.sp
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = order.status.badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, order.status.badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = order.status.displayName.uppercase(),
                        color = order.status.badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Route points
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = InDriveLimeGreen,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = order.pickupTitle.ifBlank { "Pickup Location" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF5252),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = order.destinationTitle.ifBlank { "Destination Location" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Driver and Fare Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFA0A6B5),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.driverName.ifBlank { "Driver Captain" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A6B5),
                        fontSize = 12.sp
                    )
                    if (order.driverPlateNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${order.driverPlateNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "PKR ${"%,d".format(order.agreedFare)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            HorizontalDivider(color = Color(0xFF262933), thickness = 1.dp)

            // Bottom Action Row: Report Button, Rate Ride / Rated Badge, Rebook Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Report / Safety Icon Button
                IconButton(
                    onClick = onReportTrip,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = "Report Ride",
                        tint = Color(0xFFFF7043),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rate Driver Button or "Rated" Badge
                    if (order.status == PassengerOrderStatus.COMPLETED) {
                        if (hasRated) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF252A36),
                                border = BorderStroke(1.dp, Color(0xFF3B4356)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Rated",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFA0A6B5)
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = onRateTrip,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFFB300)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rate Driver",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Rate Driver",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFB300)
                                )
                            }
                        }
                    }

                    // Rebook Ride Button
                    Button(
                        onClick = onRebookTrip,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF272B38),
                            contentColor = InDriveLimeGreen
                        ),
                        border = BorderStroke(1.dp, InDriveLimeGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rebook",
                            tint = InDriveLimeGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rebook",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = InDriveLimeGreen
                        )
                    }
                }
            }
        }
    }
}

/**
 * inDrive Style Bottom Navigation Bar matching the exact screenshot:
 * 1. "Ride" with squiggly road path icon
 * 2. "My orders" with layered sheets + checkmark icon and notification badge
 */
@Composable
fun InDrivePassengerBottomNav(
    selectedTab: Int, // 0 = Ride, 1 = My orders
    activeOrdersCount: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF14161B),
        border = BorderStroke(1.dp, Color(0xFF222632)),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("indrive_bottom_nav")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Ride
            val isRideSelected = selectedTab == 0
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clickable { onTabSelected(0) }
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .testTag("nav_tab_ride")
            ) {
                InDriveSquigglyRoadIcon(
                    tint = if (isRideSelected) Color.White else Color(0xFF7A8194),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Ride",
                    fontWeight = if (isRideSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (isRideSelected) Color.White else Color(0xFF7A8194)
                )
            }

            // Tab 1: My orders
            val isOrdersSelected = selectedTab == 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clickable { onTabSelected(1) }
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .testTag("nav_tab_my_orders")
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    InDriveMyOrdersIcon(
                        tint = if (isOrdersSelected) Color.White else Color(0xFF7A8194),
                        modifier = Modifier.size(24.dp)
                    )
                    if (activeOrdersCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = InDriveLimeGreen,
                            modifier = Modifier
                                .size(8.dp)
                                .offset(x = 2.dp, y = (-2).dp)
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "My orders",
                    fontWeight = if (isOrdersSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (isOrdersSelected) Color.White else Color(0xFF7A8194)
                )
            }
        }
    }
}

/**
 * Custom vector icon matching the "Ride" tab icon in inDrive (squiggly S-curve road).
 */
@Composable
fun InDriveSquigglyRoadIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.85f)
            cubicTo(
                w * 0.15f, h * 0.55f,
                w * 0.15f, h * 0.25f,
                w * 0.38f, h * 0.25f
            )
            cubicTo(
                w * 0.55f, h * 0.25f,
                w * 0.45f, h * 0.85f,
                w * 0.65f, h * 0.85f
            )
            cubicTo(
                w * 0.85f, h * 0.85f,
                w * 0.85f, h * 0.45f,
                w * 0.75f, h * 0.15f
            )
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Small circle endpoints
        drawCircle(
            color = tint,
            radius = 1.6.dp.toPx(),
            center = Offset(w * 0.25f, h * 0.85f)
        )
        drawCircle(
            color = tint,
            radius = 1.6.dp.toPx(),
            center = Offset(w * 0.75f, h * 0.15f)
        )
    }
}

/**
 * Custom vector icon matching the "My orders" tab icon in inDrive (layered receipt sheets with checkmark).
 */
@Composable
fun InDriveMyOrdersIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokeWidth = 2.dp.toPx()

        // Background offset sheet
        drawRoundRect(
            color = tint.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.32f, h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.65f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // Foreground primary sheet
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.16f, h * 0.24f),
            size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.65f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // Checkmark inside foreground sheet
        val checkPath = Path().apply {
            moveTo(w * 0.28f, h * 0.56f)
            lineTo(w * 0.38f, h * 0.66f)
            lineTo(w * 0.56f, h * 0.44f)
        }
        drawPath(
            path = checkPath,
            color = tint,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
