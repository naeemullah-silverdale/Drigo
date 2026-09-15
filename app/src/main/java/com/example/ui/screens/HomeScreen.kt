package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.Intent
import com.example.SettingsActivity
import android.net.Uri
import kotlinx.coroutines.delay
import com.example.data.AppLocation
import com.example.data.DestinationSuggestion
import com.example.data.LocationHelper
import com.example.data.RouteResult
import com.example.data.RouteService
import com.example.data.UserLocationData
import com.example.data.model.RideRequest
import com.example.data.model.PassengerOrder
import com.example.data.model.PassengerOrderStatus
import com.example.data.model.LiveDriverLocation
import com.example.ui.components.PostRideRatingDialog
import com.example.data.model.DriverOffer
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.CityRideType
import com.example.ui.components.CityToCityPassengerFlow
import com.example.ui.components.CityToCityPassengerDeparturesContent
import com.example.ui.screens.PassengerIntercityActiveRideScreen
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.ui.components.CityToCityStep
import com.example.ui.components.PassengerScheduledDeparturesSheet
import com.example.ui.components.InDriveFixedBottomBar
import com.example.ui.components.InDriveLimeGreen
import com.example.ui.components.InDriveRideOption
import com.example.ui.components.InDriveRideOptionsList
import com.example.ui.components.InDriveRouteTopCard
import com.example.ui.components.MapSelectionMode
import com.example.ui.components.PickupDestinationBottomCard
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.NearbyDriverMarkerData
import com.example.ui.components.RideCategoryBentoCards
import com.example.ui.components.RideChatSheet
import com.example.ui.components.RouteTopLocationsPanel
import com.example.ui.components.UniversalSafetyModalSheet
import com.example.ui.components.SafetyReportDialog
import com.example.ui.components.NotificationCenterSheet
import com.example.ui.components.NotificationSettingsSheet
import com.example.ui.components.SmartBadge
import com.example.ui.components.SmartBadgeType
import com.example.ui.components.SmartDriverBidCard
import com.example.ui.components.TactileFareAdjustmentChips
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.theme.DrigoBrandPurple
import com.example.ui.theme.drigoColors
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.util.RideNotificationManager
import com.example.util.ThemeManager
import com.example.util.ThemeMode
import com.example.viewmodel.UserMode
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.UUID

@Composable
fun HomeScreen(
    user: FirebaseUser?,
    userMode: UserMode,
    isDriverOnline: Boolean,
    onToggleDriverOnline: () -> Unit,
    onSwitchUserMode: (UserMode) -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateToWallet: () -> Unit = {},
    onNavigateToGoogleDrive: () -> Unit = {},
    onNavigateToTripHistory: () -> Unit = {},
    driverVerification: com.example.data.model.DriverVerification? = null,
    liveRideRequests: List<com.example.data.model.RideRequest> = emptyList(),
    onRefreshDriverRideRequests: (suspend () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val isDark = MaterialTheme.drigoColors.isDark

    val routeService = remember { RouteService(context) }
    val locationHelper = remember { LocationHelper(context) }

    // Persistent Isolated Locations & Route States
    var selectedPickupLocation by remember {
        mutableStateOf(
            AppLocation(
                title = "Select Pickup Location",
                subtitle = "Peshawar, KP",
                latitude = 34.0151,
                longitude = 71.5249
            )
        )
    }
    var isPickupExplicitlySet by remember { mutableStateOf(false) }

    var selectedDestinationLocation by remember { mutableStateOf<AppLocation?>(null) }
    var activeRoute by remember { mutableStateOf<RouteResult?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var mapSelectionMode by remember { mutableStateOf(MapSelectionMode.NONE) }

    // Live GPS telemetry for blue dot on map
    var userLocationData by remember { mutableStateOf<UserLocationData?>(null) }
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Bottom Sheet state for "Where From?" & "Where To?" pickup & destination selection
    var showPickupDestinationCard by remember { mutableStateOf(false) }
    var cardInitialEditPickup by remember { mutableStateOf(false) }

    // Map Interaction State
    var isMapInteracting by remember { mutableStateOf(false) }

    // Booking & Firebase Database state
    var selectedTopCategory by remember { mutableStateOf("ride_ac") }
    var selectedRideCategory by remember { mutableStateOf<String?>("Private AC") }
    var selectedRideOptionId by remember { mutableStateOf("ride_ac") }
    var customOfferedFare by remember { mutableStateOf<Int?>(null) }
    var autoAcceptOffer by remember { mutableStateOf(false) }
    var isRideSheetExpanded by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var showBookingDialog by remember { mutableStateOf(false) }
    var isBookingInProgress by remember { mutableStateOf(false) }
    var activeRideRequestId by remember { mutableStateOf<String?>(null) }
    var incomingDriverOffers by remember { mutableStateOf<List<DriverOffer>>(emptyList()) }
    var showPassengerScheduledDeparturesSheet by remember { mutableStateOf(false) }
    var showCityDeparturesScreen by remember { mutableStateOf(false) }
    var activeIntercityDepartureForPassenger by remember { mutableStateOf<PlannedDeparture?>(null) }
    var activeIntercityBookingForPassenger by remember { mutableStateOf<PlannedDepartureBooking?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Real-Time Chat State
    var showChatSheet by remember { mutableStateOf(false) }
    var chatTripId by remember { mutableStateOf("") }
    var chatPartnerName by remember { mutableStateOf("Captain Farhan") }
    var chatPartnerRole by remember { mutableStateOf("Driver") }
    var chatPartnerPhone by remember { mutableStateOf("+92 300 1234567") }
    var chatPickupTitle by remember { mutableStateOf("") }
    var chatDestinationTitle by remember { mutableStateOf("") }

    // Safety & SOS Modal State
    var showSafetySheet by remember { mutableStateOf(false) }
    var showSafetyReportModal by remember { mutableStateOf(false) }

    // Post-Ride Feedback state for Passenger
    val ratingPrefs = remember(context) { context.getSharedPreferences("drigo_ratings", android.content.Context.MODE_PRIVATE) }
    var ratedOrSkippedOrderIds by remember {
        mutableStateOf(
            ratingPrefs.all.keys
                .filter { it.startsWith("rated_or_skipped_") }
                .map { it.removePrefix("rated_or_skipped_") }
                .toSet()
        )
    }

    val isOrderRatedOrSkipped = remember(ratedOrSkippedOrderIds) {
        { id1: String, id2: String ->
            val safe1 = id1.trim()
            val safe2 = id2.trim()
            (safe1.isNotBlank() && (safe1 in ratedOrSkippedOrderIds || ratingPrefs.getBoolean("rated_or_skipped_$safe1", false))) ||
            (safe2.isNotBlank() && (safe2 in ratedOrSkippedOrderIds || ratingPrefs.getBoolean("rated_or_skipped_$safe2", false)))
        }
    }

    val markOrderRatedOrSkipped = remember {
        { id1: String, id2: String ->
            val safe1 = id1.trim()
            val safe2 = id2.trim()
            val newSet = ratedOrSkippedOrderIds + safe1 + safe2
            ratedOrSkippedOrderIds = newSet
            try {
                FirebaseRepository.getInstance(context).markRideRatedOrSkipped(safe1, safe2, "PASSENGER")
            } catch (_: Exception) {}
            ratingPrefs.edit().apply {
                if (safe1.isNotBlank()) putBoolean("rated_or_skipped_$safe1", true)
                if (safe2.isNotBlank()) putBoolean("rated_or_skipped_$safe2", true)
                apply()
            }
        }
    }

    // Track active order IDs during the current session to prevent showing feedback for historical orders on launch
    val sessionActiveOrderIds = remember { mutableStateListOf<String>() }

    var completedOrderForRating by remember { mutableStateOf<PassengerOrder?>(null) }
    var showPassengerRatingDialog by remember { mutableStateOf(false) }

    // Reset passenger rating dialog state if switching modes
    LaunchedEffect(userMode) {
        if (userMode == UserMode.DRIVER) {
            showPassengerRatingDialog = false
            completedOrderForRating = null
        }
    }

    // Passenger Bottom Tab Navigation & Orders State (Screenshots & My orders tab)
    var passengerNavTab by remember { mutableIntStateOf(0) } // 0 = Ride, 1 = My orders
    var passengerOrders by remember { mutableStateOf<List<PassengerOrder>>(emptyList()) }
    var incomingDriverOffer by remember { mutableStateOf<PassengerOrder?>(null) }
    val rideManagerTrip by com.example.data.remote.RideManager.activeTrip.collectAsState()

    // Active order tracking for passenger map (Live Car visualization)
    val activePassengerOrder = remember(passengerOrders, rideManagerTrip) {
        val activeStatuses = listOf(
            PassengerOrderStatus.DRIVER_COMING,
            PassengerOrderStatus.ACCEPTED,
            PassengerOrderStatus.DRIVER_ARRIVED,
            PassengerOrderStatus.IN_TRIP
        )
        val fromRm = rideManagerTrip?.takeIf { it.status in activeStatuses }
        val fromOrders = passengerOrders.firstOrNull { it.status in activeStatuses }
        fromRm ?: fromOrders
    }
    val isRideActive = activePassengerOrder != null
    var passengerMapDriverLoc by remember { mutableStateOf<LiveDriverLocation?>(null) }
    var nearbyDriverMarkers by remember { mutableStateOf<List<NearbyDriverMarkerData>>(emptyList()) }

    val repo = remember { FirebaseRepository.getInstance(context) }
    val realActiveDrivers by remember { repo.listenToActiveOnlineDrivers() }.collectAsState(initial = emptyList())
    val userRecord by remember(user?.uid) { repo.listenToUserRecord(user?.uid ?: "") }.collectAsState(initial = null)
    val passengerAccStatus = remember(userRecord) {
        com.example.data.model.parsePassengerAccountStatus(userRecord?.accountStatus, null)
    }

    val hasPassengerSubOverlay = activeIntercityDepartureForPassenger != null ||
            showCityDeparturesScreen ||
            showSafetyReportModal ||
            showSafetySheet ||
            showChatSheet ||
            showPassengerRatingDialog ||
            showBookingDialog ||
            showPickupDestinationCard ||
            showPassengerScheduledDeparturesSheet ||
            isRideSheetExpanded ||
            passengerNavTab != 0

    BackHandler(enabled = userMode == UserMode.PASSENGER && hasPassengerSubOverlay) {
        when {
            activeIntercityDepartureForPassenger != null -> {
                activeIntercityDepartureForPassenger = null
                activeIntercityBookingForPassenger = null
            }
            showCityDeparturesScreen -> showCityDeparturesScreen = false
            showSafetyReportModal -> showSafetyReportModal = false
            showSafetySheet -> showSafetySheet = false
            showChatSheet -> showChatSheet = false
            showPassengerRatingDialog -> showPassengerRatingDialog = false
            showBookingDialog -> showBookingDialog = false
            showPickupDestinationCard -> showPickupDestinationCard = false
            showPassengerScheduledDeparturesSheet -> showPassengerScheduledDeparturesSheet = false
            isRideSheetExpanded -> isRideSheetExpanded = false
            passengerNavTab != 0 -> passengerNavTab = 0
        }
    }

    // Live nearby drivers generator & real driver offers map markers (matching image.png)
    LaunchedEffect(selectedPickupLocation, activeRideRequestId, customOfferedFare, activeRoute, realActiveDrivers) {
        val calculatedFare = activeRoute?.let { (it.distanceKm * 60 + 120).toInt().coerceAtLeast(200) } ?: 380
        val baseFare = customOfferedFare ?: calculatedFare
        val pLat = selectedPickupLocation.latitude
        val pLon = selectedPickupLocation.longitude

        val reqId = activeRideRequestId
        if (reqId != null) {
            val repoInst = FirebaseRepository.getInstance(context)
            repoInst.listenToDriverOffers(reqId).collectLatest { offers ->
                val realOffers = offers.filter { !it.driverId.startsWith("dr_demo_") && !it.driverId.startsWith("dr_mock_") && !it.driverId.startsWith("demo_") }
                if (realOffers.isNotEmpty()) {
                    nearbyDriverMarkers = realOffers.mapIndexed { idx, offer ->
                        val dLat = if (offer.driverLat != 0.0) offer.driverLat else (pLat + 0.0032 * Math.cos(idx * 1.5))
                        val dLon = if (offer.driverLon != 0.0) offer.driverLon else (pLon + 0.0032 * Math.sin(idx * 1.5))
                        NearbyDriverMarkerData(
                            id = offer.id,
                            latitude = dLat,
                            longitude = dLon,
                            fareText = "${offer.offeredFare} Rs",
                            isPrimaryOption = (idx == 0),
                            bearing = (idx * 65f + 30f) % 360f,
                            driverName = offer.driverName,
                            driverOffer = offer
                        )
                    }
                } else {
                    val availableDrivers = realActiveDrivers.filter { it.driverId != user?.uid }
                    if (availableDrivers.isNotEmpty()) {
                        nearbyDriverMarkers = availableDrivers.mapIndexed { idx, d ->
                            NearbyDriverMarkerData(
                                id = d.driverId,
                                latitude = d.latitude,
                                longitude = d.longitude,
                                fareText = "${baseFare} Rs",
                                isPrimaryOption = (idx == 0),
                                bearing = d.bearing,
                                driverName = d.driverName
                            )
                        }
                    } else if (pLat != 0.0 && pLon != 0.0) {
                        nearbyDriverMarkers = listOf(
                            NearbyDriverMarkerData("driver_1", pLat + 0.0032, pLon + 0.0025, "${baseFare} Rs", isPrimaryOption = true, bearing = 35f, driverName = "Captain Farhan"),
                            NearbyDriverMarkerData("driver_2", pLat - 0.0028, pLon + 0.0038, "${baseFare + 20} Rs", isPrimaryOption = false, bearing = 120f, driverName = "Captain Usman"),
                            NearbyDriverMarkerData("driver_3", pLat + 0.0018, pLon - 0.0032, "${baseFare - 10} Rs", isPrimaryOption = true, bearing = 210f, driverName = "Captain Ali"),
                            NearbyDriverMarkerData("driver_4", pLat - 0.0038, pLon - 0.0022, "${baseFare + 40} Rs", isPrimaryOption = false, bearing = 300f, driverName = "Captain Bilal")
                        )
                    }
                }
            }
        } else {
            val availableDrivers = realActiveDrivers.filter { it.driverId != user?.uid }
            if (availableDrivers.isNotEmpty()) {
                nearbyDriverMarkers = availableDrivers.mapIndexed { idx, d ->
                    NearbyDriverMarkerData(
                        id = d.driverId,
                        latitude = d.latitude,
                        longitude = d.longitude,
                        fareText = "${baseFare} Rs",
                        isPrimaryOption = (idx == 0),
                        bearing = d.bearing,
                        driverName = d.driverName
                    )
                }
            } else if (pLat != 0.0 && pLon != 0.0) {
                nearbyDriverMarkers = listOf(
                    NearbyDriverMarkerData("driver_1", pLat + 0.0032, pLon + 0.0025, "${baseFare} Rs", isPrimaryOption = true, bearing = 35f, driverName = "Captain Farhan"),
                    NearbyDriverMarkerData("driver_2", pLat - 0.0028, pLon + 0.0038, "${baseFare + 20} Rs", isPrimaryOption = false, bearing = 120f, driverName = "Captain Usman"),
                    NearbyDriverMarkerData("driver_3", pLat + 0.0018, pLon - 0.0032, "${baseFare - 10} Rs", isPrimaryOption = true, bearing = 210f, driverName = "Captain Ali"),
                    NearbyDriverMarkerData("driver_4", pLat - 0.0038, pLon - 0.0022, "${baseFare + 40} Rs", isPrimaryOption = false, bearing = 300f, driverName = "Captain Bilal")
                )
            }
        }
    }

    // Notification Manager Instance
    val notifManager = remember(context) { RideNotificationManager.getInstance(context) }

    // Automatically sync passenger's selected From (pickup) and To (destination) locations to Firebase (only when not in active ride)
    LaunchedEffect(selectedPickupLocation, selectedDestinationLocation, user?.uid, isRideActive) {
        val uid = user?.uid
        if (!uid.isNullOrBlank() && !isRideActive) {
            repo.savePassengerLocations(
                userId = uid,
                pickupTitle = selectedPickupLocation.title,
                pickupSubtitle = selectedPickupLocation.subtitle,
                pickupLat = selectedPickupLocation.latitude,
                pickupLon = selectedPickupLocation.longitude,
                destinationTitle = selectedDestinationLocation?.title ?: "",
                destinationSubtitle = selectedDestinationLocation?.subtitle ?: "",
                destinationLat = selectedDestinationLocation?.latitude ?: 0.0,
                destinationLon = selectedDestinationLocation?.longitude ?: 0.0
            )
        }
    }

    LaunchedEffect(activePassengerOrder?.requestId, activePassengerOrder?.id) {
        val reqId = activePassengerOrder?.requestId?.ifBlank { activePassengerOrder?.id }
        if (reqId != null) {
            val repo = FirebaseRepository.getInstance(context)
            repo.listenToLiveDriverLocation(reqId).collectLatest { loc ->
                if (loc != null) {
                    passengerMapDriverLoc = loc
                    if (loc.status.equals("COMPLETED", ignoreCase = true) || loc.status == PassengerOrderStatus.COMPLETED.name) {
                        val currentOrder = activePassengerOrder ?: passengerOrders.firstOrNull { it.id == reqId || it.requestId == reqId }
                        if (currentOrder != null) {
                            val completed = currentOrder.copy(status = PassengerOrderStatus.COMPLETED)
                            sessionActiveOrderIds.add(reqId)
                            if (currentOrder.id.isNotBlank()) sessionActiveOrderIds.add(currentOrder.id)
                            if (currentOrder.requestId.isNotBlank()) sessionActiveOrderIds.add(currentOrder.requestId)

                            activeRideRequestId = null
                            activeRoute = null
                            selectedDestinationLocation = null
                            passengerMapDriverLoc = null
                            incomingDriverOffer = null
                            incomingDriverOffers = emptyList()
                            nearbyDriverMarkers = emptyList()
                            isBookingInProgress = false
                            isRideSheetExpanded = false
                            customOfferedFare = null
                            showChatSheet = false
                            showSafetySheet = false
                            showSafetyReportModal = false
                            notifManager.dismissPassengerActiveRideNotification()
                            notifManager.clearVoiceQueueAndTracking()

                            val nonActiveOrders = passengerOrders.filter {
                                it.status != PassengerOrderStatus.DRIVER_COMING &&
                                it.status != PassengerOrderStatus.ACCEPTED &&
                                it.status != PassengerOrderStatus.DRIVER_ARRIVED &&
                                it.status != PassengerOrderStatus.IN_TRIP &&
                                it.id != reqId && it.requestId != reqId
                            }
                            passengerOrders = listOf(completed) + nonActiveOrders

                            val safeId = completed.id.ifBlank { completed.requestId }
                            val isSkippedOrRated = isOrderRatedOrSkipped(safeId, completed.requestId) ||
                                    repo.isRideRatedOrSkipped(safeId, completed.requestId, "PASSENGER")
                            if (!isSkippedOrRated && !showPassengerRatingDialog) {
                                completedOrderForRating = completed
                                showPassengerRatingDialog = true
                            }
                        }
                    }
                }
            }
        } else {
            passengerMapDriverLoc = null
        }
    }

    // Synchronize activeRideRequestId & RideManager observer whenever active trip is loaded or changed
    LaunchedEffect(activeRideRequestId, activePassengerOrder?.requestId, activePassengerOrder?.id) {
        val reqId = activePassengerOrder?.requestId?.ifBlank { activePassengerOrder?.id } ?: activeRideRequestId
        if (reqId != null) {
            if (activeRideRequestId == null) {
                activeRideRequestId = reqId
            }
            com.example.data.remote.RideManager.observeActiveTrip(reqId)
        }
    }

    // Live driver location listener for active accepted captain
    LaunchedEffect(activePassengerOrder?.id, activePassengerOrder?.assignedDriverId, activePassengerOrder?.requestId) {
        val order = activePassengerOrder
        if (order != null && (order.status == PassengerOrderStatus.ACCEPTED || order.status == PassengerOrderStatus.DRIVER_COMING || order.status == PassengerOrderStatus.DRIVER_ARRIVED || order.status == PassengerOrderStatus.IN_TRIP)) {
            val reqId = order.requestId.ifBlank { order.id }
            val repo = FirebaseRepository.getInstance(context)
            repo.listenToLiveDriverLocation(reqId).collectLatest { loc ->
                if (loc != null) {
                    passengerMapDriverLoc = loc
                }
            }
        }
    }

    var showNotificationCenterSheet by remember { mutableStateOf(false) }
    var showNotificationSettingsSheet by remember { mutableStateOf(false) }
    val notificationHistory by notifManager.notificationHistory.collectAsState()
    var lastKnownPassengerOrderStatus by remember { mutableStateOf<PassengerOrderStatus?>(null) }
    var lastKnownPassengerOrderId by remember { mutableStateOf<String?>(null) }

    // Listen to active passenger order status changes and dispatch notifications strictly for the single active ride
    LaunchedEffect(activePassengerOrder?.id, activePassengerOrder?.status) {
        val order = activePassengerOrder
        if (order != null && (order.status != lastKnownPassengerOrderStatus || order.id != lastKnownPassengerOrderId)) {
            lastKnownPassengerOrderId = order.id
            lastKnownPassengerOrderStatus = order.status
            when (order.status) {
                PassengerOrderStatus.ACCEPTED, PassengerOrderStatus.DRIVER_COMING -> {
                    notifManager.notifyDriverAccepted(
                        driverName = order.driverName.ifBlank { "Captain" },
                        farePkr = order.agreedFare,
                        vehicleModel = "${order.driverVehicleColor} ${order.driverVehicleMake} ${order.driverVehicleModel}".trim(),
                        rideId = order.id
                    )
                    notifManager.updatePassengerActiveRideNotification(order)
                }
                PassengerOrderStatus.DRIVER_ARRIVED -> {
                    notifManager.notifyDriverArrived(
                        driverName = order.driverName.ifBlank { "Captain" },
                        plateNumber = order.driverPlateNumber,
                        rideId = order.id
                    )
                    notifManager.updatePassengerActiveRideNotification(order)
                }
                PassengerOrderStatus.IN_TRIP -> {
                    notifManager.notifyRideStarted(
                        destination = order.destinationTitle,
                        rideId = order.id
                    )
                    notifManager.updatePassengerActiveRideNotification(order)
                }
                PassengerOrderStatus.COMPLETED -> {
                    notifManager.notifyRideCompleted(
                        farePkr = order.agreedFare,
                        rideId = order.id
                    )
                    notifManager.dismissPassengerActiveRideNotification()
                    notifManager.clearVoiceQueueAndTracking()
                }
                PassengerOrderStatus.CANCELLED -> {
                    notifManager.notifyPassengerRideCancelled(
                        reason = "Trip ended",
                        rideId = order.id
                    )
                    notifManager.dismissPassengerActiveRideNotification()
                    notifManager.clearVoiceQueueAndTracking()
                }
                else -> Unit
            }
        } else if (order == null && lastKnownPassengerOrderId != null) {
            lastKnownPassengerOrderId = null
            lastKnownPassengerOrderStatus = null
            notifManager.dismissPassengerActiveRideNotification()
            notifManager.clearVoiceQueueAndTracking()
        }
    }

    val passengerNotifUserId = remember(user?.uid) {
        user?.uid ?: "passenger_user"
    }
    val seenNotificationIds = remember { mutableSetOf<String>() }

    // Listen to passenger notifications (e.g. Intercity departure offer accepted by driver)
    LaunchedEffect(passengerNotifUserId) {
        val repo = FirebaseRepository.getInstance(context)
        repo.listenToPassengerNotifications(passengerNotifUserId).collectLatest { notifs ->
            notifs.forEach { notif ->
                val notifId = notif["id"] as? String ?: return@forEach
                if (!seenNotificationIds.contains(notifId)) {
                    seenNotificationIds.add(notifId)
                    val title = notif["title"] as? String ?: "Offer Accepted! Ride Confirmed"
                    val message = notif["message"] as? String ?: "Captain accepted your offer."
                    val subText = notif["subText"] as? String ?: "City to City • Confirmed Booking"
                    val rideId = notif["rideId"] as? String ?: notif["departureId"] as? String
                    val farePkr = (notif["farePkr"] as? Number)?.toInt()

                    notifManager.postNotification(
                        type = com.example.util.RideNotificationType.PASSENGER_DRIVER_ACCEPTED,
                        title = title,
                        message = message,
                        subText = subText,
                        rideId = rideId,
                        farePkr = farePkr,
                        speakAnnouncement = true
                    )
                }
            }
        }
    }

    // Listen to real-time incoming driver offers for the active ride request
    LaunchedEffect(activeRideRequestId, activePassengerOrder?.id) {
        val reqId = activeRideRequestId
        if (reqId != null && activePassengerOrder == null) {
            val repo = FirebaseRepository.getInstance(context)
            repo.listenToDriverOffers(reqId).collectLatest { offers ->
                if (activePassengerOrder != null) return@collectLatest
                val realOffers = offers.filter { !it.driverId.startsWith("dr_demo_") && !it.driverId.startsWith("dr_mock_") && !it.driverId.startsWith("demo_") }
                incomingDriverOffers = realOffers
                val latestOffer = realOffers.lastOrNull()
                if (latestOffer != null && incomingDriverOffer == null) {
                    val converted = PassengerOrder(
                        id = UUID.randomUUID().toString(),
                        requestId = latestOffer.requestId,
                        pickupTitle = selectedPickupLocation.title,
                        pickupSubtitle = selectedPickupLocation.subtitle,
                        pickupLat = selectedPickupLocation.latitude,
                        pickupLon = selectedPickupLocation.longitude,
                        destinationTitle = selectedDestinationLocation?.title ?: "Destination",
                        destinationSubtitle = selectedDestinationLocation?.subtitle ?: "",
                        destinationLat = selectedDestinationLocation?.latitude ?: (selectedPickupLocation.latitude + 0.015),
                        destinationLon = selectedDestinationLocation?.longitude ?: (selectedPickupLocation.longitude + 0.015),
                        distanceKm = activeRoute?.distanceKm ?: 5.0,
                        durationMinutes = activeRoute?.durationMinutes ?: 12,
                        rideCategory = selectedRideCategory ?: "Ride A/C",
                        agreedFare = latestOffer.offeredFare,
                        paymentMethod = selectedPaymentMethod,
                        driverName = latestOffer.driverName,
                        driverRating = if (latestOffer.driverRating > 0) latestOffer.driverRating else 5.0,
                        driverTotalRides = latestOffer.driverTotalRides,
                        driverVehicleMake = latestOffer.driverVehicleMake,
                        driverVehicleModel = latestOffer.driverVehicleModel,
                        driverVehicleColor = latestOffer.driverVehicleColor,
                        driverPlateNumber = latestOffer.driverPlateNumber,
                        driverPhone = latestOffer.driverPhone,
                        assignedDriverId = latestOffer.driverId,
                        status = PassengerOrderStatus.ACCEPTED,
                        etaMinutes = latestOffer.etaMinutes
                    )
                    incomingDriverOffer = converted
                    // Dispatch notification
                    notifManager.notifyDriverCounterOffer(
                        driverName = latestOffer.driverName.ifBlank { "Captain" },
                        counterFare = latestOffer.offeredFare,
                        etaMinutes = latestOffer.etaMinutes,
                        rideId = latestOffer.requestId
                    )
                }
            }
        } else {
            incomingDriverOffers = emptyList()
        }
    }

    // Observe RideManager active trip reactive state model (active_trips/{tripId})
    LaunchedEffect(Unit) {
        com.example.data.remote.RideManager.activeTrip.collectLatest { rideManagerTrip ->
            if (rideManagerTrip != null) {
                val safeId = rideManagerTrip.id.ifBlank { rideManagerTrip.requestId }
                if (safeId.isNotBlank()) {
                    if (rideManagerTrip.status == PassengerOrderStatus.COMPLETED) {
                        // 1. Clear all active in-progress state immediately so the screen transitions out of 'Trip in progress'
                        activeRideRequestId = null
                        activeRoute = null
                        selectedDestinationLocation = null
                        passengerMapDriverLoc = null
                        incomingDriverOffer = null
                        incomingDriverOffers = emptyList()
                        nearbyDriverMarkers = emptyList()
                        isBookingInProgress = false
                        isRideSheetExpanded = false
                        customOfferedFare = null
                        showChatSheet = false
                        showSafetySheet = false
                        showSafetyReportModal = false
                        notifManager.dismissPassengerActiveRideNotification()
                        notifManager.clearVoiceQueueAndTracking()

                        // 2. Mark any active orders as COMPLETED
                        val nonActiveOrders = passengerOrders.filter { 
                            it.status != PassengerOrderStatus.DRIVER_COMING &&
                            it.status != PassengerOrderStatus.ACCEPTED &&
                            it.status != PassengerOrderStatus.DRIVER_ARRIVED &&
                            it.status != PassengerOrderStatus.IN_TRIP &&
                            it.id != safeId && it.requestId != safeId &&
                            it.id != rideManagerTrip.requestId && it.requestId != rideManagerTrip.id
                        }
                        passengerOrders = listOf(rideManagerTrip) + nonActiveOrders

                        // 3. Immediately prompt the Passenger Review & Feedback Dialog (Passenger Mode only)
                        val repo = FirebaseRepository.getInstance(context)
                        val isSkippedOrRated = isOrderRatedOrSkipped(safeId, rideManagerTrip.requestId) ||
                                repo.isRideRatedOrSkipped(safeId, rideManagerTrip.requestId, "PASSENGER")
                        if (userMode == UserMode.PASSENGER && !isSkippedOrRated && !showPassengerRatingDialog) {
                            var fullOrder = rideManagerTrip
                            if (fullOrder.driverId.isNotBlank() && (fullOrder.driverName.isBlank() || fullOrder.driverPlateNumber.isBlank())) {
                                val profile = repo.getDriverProfile(fullOrder.driverId)
                                if (profile != null) {
                                    fullOrder = fullOrder.copy(
                                        driverName = fullOrder.driverName.ifBlank { profile.name },
                                        driverPhone = fullOrder.driverPhone.ifBlank { profile.phone },
                                        driverPlateNumber = fullOrder.driverPlateNumber.ifBlank { profile.vehicleNumber },
                                        driverVehicleMake = fullOrder.driverVehicleMake.ifBlank { profile.vehicleCompany },
                                        driverVehicleModel = fullOrder.driverVehicleModel.ifBlank { profile.vehicleModel }
                                    )
                                }
                            }
                            completedOrderForRating = fullOrder
                            showPassengerRatingDialog = true
                        }
                    } else if (rideManagerTrip.status == PassengerOrderStatus.CANCELLED) {
                        activeRideRequestId = null
                        activeRoute = null
                        selectedDestinationLocation = null
                        passengerMapDriverLoc = null
                        incomingDriverOffer = null
                        incomingDriverOffers = emptyList()
                        nearbyDriverMarkers = emptyList()
                        isBookingInProgress = false
                        isRideSheetExpanded = false
                        customOfferedFare = null
                        showChatSheet = false
                        showSafetySheet = false
                        showSafetyReportModal = false
                        notifManager.dismissPassengerActiveRideNotification()
                        notifManager.clearVoiceQueueAndTracking()

                        val nonActiveOrders = passengerOrders.filter { 
                            it.status != PassengerOrderStatus.DRIVER_COMING &&
                            it.status != PassengerOrderStatus.ACCEPTED &&
                            it.status != PassengerOrderStatus.DRIVER_ARRIVED &&
                            it.status != PassengerOrderStatus.IN_TRIP &&
                            it.id != safeId && it.requestId != safeId &&
                            it.id != rideManagerTrip.requestId && it.requestId != rideManagerTrip.id
                        }
                        passengerOrders = listOf(rideManagerTrip) + nonActiveOrders
                    } else {
                        // ACTIVE RIDE (ACCEPTED, DRIVER_COMING, DRIVER_ARRIVED, IN_TRIP)
                        val existingFiltered = passengerOrders.filter { 
                            it.id != safeId && it.requestId != safeId && it.id != rideManagerTrip.requestId && it.requestId != rideManagerTrip.id 
                        }
                        passengerOrders = listOf(rideManagerTrip) + existingFiltered
                        activeRideRequestId = safeId
                        if (rideManagerTrip.pickupLat != 0.0 && rideManagerTrip.pickupLon != 0.0) {
                            selectedPickupLocation = AppLocation(
                                title = rideManagerTrip.pickupTitle.ifBlank { "Pickup" },
                                subtitle = rideManagerTrip.pickupSubtitle,
                                latitude = rideManagerTrip.pickupLat,
                                longitude = rideManagerTrip.pickupLon
                            )
                        }
                        if (rideManagerTrip.destinationLat != 0.0 && rideManagerTrip.destinationLon != 0.0) {
                            selectedDestinationLocation = AppLocation(
                                title = rideManagerTrip.destinationTitle.ifBlank { "Destination" },
                                subtitle = rideManagerTrip.destinationSubtitle,
                                latitude = rideManagerTrip.destinationLat,
                                longitude = rideManagerTrip.destinationLon
                            )
                        }
                    }
                }
            } else {
                // If RideManager active trip was cleared (e.g. driver completed or cleared it)
                // and passenger currently has an active in-trip or accepted order:
                val activeOrder = passengerOrders.firstOrNull { 
                    it.status == PassengerOrderStatus.DRIVER_COMING ||
                    it.status == PassengerOrderStatus.ACCEPTED ||
                    it.status == PassengerOrderStatus.DRIVER_ARRIVED ||
                    it.status == PassengerOrderStatus.IN_TRIP
                }
                if (activeOrder != null && sessionActiveOrderIds.isNotEmpty()) {
                    activeRideRequestId = null
                    activeRoute = null
                    selectedDestinationLocation = null
                    passengerMapDriverLoc = null
                    incomingDriverOffer = null
                    incomingDriverOffers = emptyList()
                    nearbyDriverMarkers = emptyList()
                    isBookingInProgress = false
                    isRideSheetExpanded = false
                    customOfferedFare = null
                    showChatSheet = false
                    showSafetySheet = false
                    showSafetyReportModal = false
                    notifManager.dismissPassengerActiveRideNotification()
                    notifManager.clearVoiceQueueAndTracking()

                    val completed = activeOrder.copy(status = PassengerOrderStatus.COMPLETED)
                    val nonActive = passengerOrders.filter { 
                        it.id != activeOrder.id && it.requestId != activeOrder.requestId &&
                        it.status != PassengerOrderStatus.DRIVER_COMING &&
                        it.status != PassengerOrderStatus.ACCEPTED &&
                        it.status != PassengerOrderStatus.DRIVER_ARRIVED &&
                        it.status != PassengerOrderStatus.IN_TRIP
                    }
                    passengerOrders = listOf(completed) + nonActive
                    val safeId = completed.id.ifBlank { completed.requestId }
                    val repo = FirebaseRepository.getInstance(context)
                    val isSkippedOrRated = isOrderRatedOrSkipped(safeId, completed.requestId) ||
                            repo.isRideRatedOrSkipped(safeId, completed.requestId, "PASSENGER")
                    if (userMode == UserMode.PASSENGER && !isSkippedOrRated && !showPassengerRatingDialog) {
                        completedOrderForRating = completed
                        showPassengerRatingDialog = true
                    }
                }
            }
        }
    }

    // Listen to passenger orders from Firebase Realtime Database & Firestore
    LaunchedEffect(user?.uid, user?.email) {
        val uid = user?.uid ?: ""
        val email = user?.email ?: ""
        val repo = FirebaseRepository.getInstance(context)
        repo.listenToPassengerOrders(uid, email).collectLatest { cloudOrders ->
            val currentRmTrip = com.example.data.remote.RideManager.activeTrip.value
            val activeStatuses = listOf(
                PassengerOrderStatus.DRIVER_COMING,
                PassengerOrderStatus.ACCEPTED,
                PassengerOrderStatus.DRIVER_ARRIVED,
                PassengerOrderStatus.IN_TRIP
            )
            val mappedCloudOrders = cloudOrders.map { order ->
                if (order.status == PassengerOrderStatus.COMPLETED || order.status == PassengerOrderStatus.CANCELLED) {
                    order
                } else if (currentRmTrip != null && (
                    order.id == currentRmTrip.id || 
                    order.requestId == currentRmTrip.requestId || 
                    order.id == currentRmTrip.requestId || 
                    order.requestId == currentRmTrip.id
                )) {
                    val rmIsActive = currentRmTrip.status in activeStatuses
                    val cloudIsActive = order.status in activeStatuses
                    if (rmIsActive && !cloudIsActive) {
                        currentRmTrip
                    } else if (cloudIsActive) {
                        order
                    } else {
                        order
                    }
                } else {
                    order
                }
            }

            val completedOrderInCloud = mappedCloudOrders.firstOrNull { order ->
                order.status == PassengerOrderStatus.COMPLETED && (
                    order.id == activeRideRequestId || order.requestId == activeRideRequestId ||
                    sessionActiveOrderIds.contains(order.id) || sessionActiveOrderIds.contains(order.requestId) ||
                    (activePassengerOrder != null && (
                        order.id == activePassengerOrder.id || order.requestId == activePassengerOrder.requestId ||
                        order.id == activePassengerOrder.requestId || order.requestId == activePassengerOrder.id
                    ))
                )
            }

            val cloudIds = mappedCloudOrders.map { it.id }.filter { it.isNotBlank() }.toSet()
            val cloudReqIds = mappedCloudOrders.map { it.requestId }.filter { it.isNotBlank() }.toSet()
            val cloudKeys = cloudIds + cloudReqIds
            val completedKeys = mappedCloudOrders
                .filter { it.status == PassengerOrderStatus.COMPLETED || it.status == PassengerOrderStatus.CANCELLED }
                .flatMap { listOf(it.id, it.requestId) }
                .filter { it.isNotBlank() }
                .toSet()

            val localOnly = passengerOrders.filter { 
                it.id !in cloudKeys && 
                it.requestId !in cloudKeys && 
                it.id !in completedKeys &&
                it.requestId !in completedKeys &&
                it.status != PassengerOrderStatus.CANCELLED &&
                it.status != PassengerOrderStatus.COMPLETED
            }

            // Always prioritize active trip from RideManager or active local order
            val activePrepend = listOfNotNull(
                currentRmTrip?.takeIf { it.status in activeStatuses },
                passengerOrders.firstOrNull { it.status in activeStatuses }
            )
            passengerOrders = (activePrepend + mappedCloudOrders + localOnly).distinctBy { it.requestId.ifBlank { it.id } }

            if (completedOrderInCloud != null) {
                val safeId = completedOrderInCloud.id.ifBlank { completedOrderInCloud.requestId }
                sessionActiveOrderIds.add(safeId)
                if (completedOrderInCloud.id.isNotBlank()) sessionActiveOrderIds.add(completedOrderInCloud.id)
                if (completedOrderInCloud.requestId.isNotBlank()) sessionActiveOrderIds.add(completedOrderInCloud.requestId)

                // Instantly exit the "Trip in Progress" view
                activeRideRequestId = null
                activeRoute = null
                selectedDestinationLocation = null
                passengerMapDriverLoc = null
                incomingDriverOffer = null
                incomingDriverOffers = emptyList()
                nearbyDriverMarkers = emptyList()
                isBookingInProgress = false
                isRideSheetExpanded = false
                customOfferedFare = null
                showChatSheet = false
                showSafetySheet = false
                showSafetyReportModal = false
                notifManager.dismissPassengerActiveRideNotification()
                notifManager.clearVoiceQueueAndTracking()

                if (currentRmTrip != null && (currentRmTrip.id == safeId || currentRmTrip.requestId == safeId)) {
                    com.example.data.remote.RideManager.clearActiveTrip()
                }

                val isSkippedOrRated = isOrderRatedOrSkipped(safeId, completedOrderInCloud.requestId) ||
                        repo.isRideRatedOrSkipped(safeId, completedOrderInCloud.requestId, "PASSENGER")
                if (userMode == UserMode.PASSENGER && !isSkippedOrRated && !showPassengerRatingDialog) {
                    var fullOrder = completedOrderInCloud
                    if (fullOrder.driverId.isNotBlank() && (fullOrder.driverName.isBlank() || fullOrder.driverPlateNumber.isBlank())) {
                        val profile = repo.getDriverProfile(fullOrder.driverId)
                        if (profile != null) {
                            fullOrder = fullOrder.copy(
                                driverName = fullOrder.driverName.ifBlank { profile.name },
                                driverPhone = fullOrder.driverPhone.ifBlank { profile.phone },
                                driverPlateNumber = fullOrder.driverPlateNumber.ifBlank { profile.vehicleNumber },
                                driverVehicleMake = fullOrder.driverVehicleMake.ifBlank { profile.vehicleCompany },
                                driverVehicleModel = fullOrder.driverVehicleModel.ifBlank { profile.vehicleModel }
                            )
                        }
                    }
                    completedOrderForRating = fullOrder
                    showPassengerRatingDialog = true
                }
            }


            // Ensure RideManager observes the active trip document
            mappedCloudOrders.firstOrNull { 
                it.status == PassengerOrderStatus.ACCEPTED || 
                it.status == PassengerOrderStatus.DRIVER_COMING || 
                it.status == PassengerOrderStatus.DRIVER_ARRIVED || 
                it.status == PassengerOrderStatus.IN_TRIP 
            }?.let { activeOrder ->
                val activeId = activeOrder.requestId.ifBlank { activeOrder.id }
                if (activeId.isNotBlank()) {
                    com.example.data.remote.RideManager.observeActiveTrip(activeId)
                }
            }
        }
    }

    // Track active rides during this app session to prevent historical orders from triggering feedback on launch
    LaunchedEffect(passengerOrders) {
        passengerOrders.forEach { order ->
            if (order.status == PassengerOrderStatus.DRIVER_COMING ||
                order.status == PassengerOrderStatus.ACCEPTED ||
                order.status == PassengerOrderStatus.DRIVER_ARRIVED ||
                order.status == PassengerOrderStatus.IN_TRIP) {
                val safeId = order.id.ifBlank { order.requestId }
                if (safeId.isNotBlank() && !sessionActiveOrderIds.contains(safeId)) {
                    sessionActiveOrderIds.add(safeId)
                }
                if (order.requestId.isNotBlank() && !sessionActiveOrderIds.contains(order.requestId)) {
                    sessionActiveOrderIds.add(order.requestId)
                }
            }
        }
    }

    // Automatically trigger Passenger Post-Ride Feedback ONLY when a ride active in this session becomes COMPLETED
    LaunchedEffect(passengerOrders, ratedOrSkippedOrderIds) {
        val completedOrder = passengerOrders.firstOrNull { order ->
            val safeId = order.id.ifBlank { order.requestId }
            val isRecent = (System.currentTimeMillis() - order.createdAt) < 3 * 60 * 60 * 1000L || order.createdAt == 0L
            val isActiveInSession = sessionActiveOrderIds.contains(safeId) || sessionActiveOrderIds.contains(order.requestId)
            order.status == PassengerOrderStatus.COMPLETED &&
            safeId.isNotBlank() &&
            (isActiveInSession || isRecent) &&
            !isOrderRatedOrSkipped(safeId, order.requestId)
        }
        if (userMode == UserMode.PASSENGER && completedOrder != null && !showPassengerRatingDialog) {
            val safeId = completedOrder.id.ifBlank { completedOrder.requestId }
            val repo = FirebaseRepository.getInstance(context)
            val isSkippedOrRated = isOrderRatedOrSkipped(safeId, completedOrder.requestId) ||
                    repo.isRideRatedOrSkipped(safeId, completedOrder.requestId, "PASSENGER")
            if (!isSkippedOrRated) {
                var fullOrder = completedOrder
                if (fullOrder.driverId.isNotBlank() && (fullOrder.driverName.isBlank() || fullOrder.driverPlateNumber.isBlank())) {
                    val profile = repo.getDriverProfile(fullOrder.driverId)
                    if (profile != null) {
                        fullOrder = fullOrder.copy(
                            driverName = fullOrder.driverName.ifBlank { profile.name },
                            driverPhone = fullOrder.driverPhone.ifBlank { profile.phone },
                            driverPlateNumber = fullOrder.driverPlateNumber.ifBlank { profile.vehicleNumber },
                            driverVehicleMake = fullOrder.driverVehicleMake.ifBlank { profile.vehicleCompany },
                            driverVehicleModel = fullOrder.driverVehicleModel.ifBlank { profile.vehicleModel }
                        )
                    }
                }
                completedOrderForRating = fullOrder
                showPassengerRatingDialog = true
            } else {
                markOrderRatedOrSkipped(safeId, completedOrder.requestId)
            }
        }
    }

    // Automatically clean up map driver marker & state when active order is cleared or cancelled
    LaunchedEffect(activePassengerOrder) {
        if (activePassengerOrder == null) {
            passengerMapDriverLoc = null
        }
    }

    // Centralized cancel function that immediately updates Passenger UI & sets Firebase status to CANCELLED
    val cancelRideAndReturnHome: (orderId: String, requestId: String) -> Unit = cancelLabel@ { orderId, requestId ->
        val safeReqId = requestId.ifBlank { orderId }
        val safeOrderId = orderId.ifBlank { requestId }
        val currentUserId = user?.uid ?: ""

        val existingOrder = passengerOrders.firstOrNull { it.id == safeOrderId || (safeReqId.isNotBlank() && (it.requestId == safeReqId || it.id == safeReqId)) }
        if (existingOrder?.status == PassengerOrderStatus.COMPLETED || existingOrder?.status == PassengerOrderStatus.IN_TRIP) {
            scope.launch {
                snackbarHostState.showSnackbar("Cannot cancel a completed or active trip.")
            }
            return@cancelLabel
        }

        // 1. Immediately reset Passenger UI to return to normal Home/booking screen
        activeRideRequestId = null
        activeRoute = null
        selectedDestinationLocation = null
        passengerMapDriverLoc = null
        incomingDriverOffer = null
        incomingDriverOffers = emptyList()
        nearbyDriverMarkers = emptyList()
        isBookingInProgress = false
        isRideSheetExpanded = false
        customOfferedFare = null
        passengerNavTab = 0
        showChatSheet = false
        showSafetySheet = false
        showSafetyReportModal = false
        notifManager.clearVoiceQueueAndTracking()
        passengerOrders = passengerOrders.map {
            if (it.id == safeOrderId || (safeReqId.isNotBlank() && (it.requestId == safeReqId || it.id == safeReqId))) {
                it.copy(status = PassengerOrderStatus.CANCELLED)
            } else it
        }

        // 2. Update ride status in Firebase to CANCELLED asynchronously
        scope.launch {
            try {
                val repo = FirebaseRepository.getInstance(context)
                if (safeReqId.isNotBlank()) {
                    repo.updateRideRequestStatus(safeReqId, "CANCELLED")
                }
                repo.cancelPassengerOrder(safeOrderId, safeReqId, currentUserId)
            } catch (e: Exception) {
                android.util.Log.w("HomeScreen", "Error during cancelPassengerOrder: ${e.message}")
            }
            snackbarHostState.showSnackbar("Ride Cancelled")
        }
    }

    // Automatically keep passenger UI synchronized with Firebase ride request status and real driver details
    LaunchedEffect(activeRideRequestId) {
        val reqId = activeRideRequestId ?: return@LaunchedEffect
        val repo = FirebaseRepository.getInstance(context)
        repo.listenToRideRequestUpdates(reqId).collectLatest { update ->
            if (update == null) return@collectLatest
            when (update.status) {
                "CANCELLED", "REJECTED" -> {
                    activeRideRequestId = null
                    activeRoute = null
                    selectedDestinationLocation = null
                    passengerMapDriverLoc = null
                    incomingDriverOffer = null
                    nearbyDriverMarkers = emptyList()
                    isBookingInProgress = false
                    isRideSheetExpanded = false
                    customOfferedFare = null
                    passengerOrders = passengerOrders.map {
                        if (it.requestId == reqId || it.id == reqId) {
                            it.copy(status = PassengerOrderStatus.CANCELLED)
                        } else it
                    }
                    snackbarHostState.showSnackbar("Ride was cancelled.")
                }
                "COMPLETED" -> {
                    activeRideRequestId = null
                    activeRoute = null
                    selectedDestinationLocation = null
                    passengerMapDriverLoc = null
                    incomingDriverOffer = null
                    incomingDriverOffers = emptyList()
                    nearbyDriverMarkers = emptyList()
                    isBookingInProgress = false
                    isRideSheetExpanded = false
                    customOfferedFare = null
                    showChatSheet = false
                    showSafetySheet = false
                    showSafetyReportModal = false
                    notifManager.dismissPassengerActiveRideNotification()
                    notifManager.clearVoiceQueueAndTracking()

                    var foundCompletedOrder: PassengerOrder? = null
                    passengerOrders = passengerOrders.map {
                        if (it.requestId == reqId || it.id == reqId ||
                            it.status == PassengerOrderStatus.DRIVER_COMING ||
                            it.status == PassengerOrderStatus.ACCEPTED ||
                            it.status == PassengerOrderStatus.DRIVER_ARRIVED ||
                            it.status == PassengerOrderStatus.IN_TRIP) {
                            val completed = it.copy(
                                status = PassengerOrderStatus.COMPLETED,
                                agreedFare = if (update.assignedFare > 0) update.assignedFare else it.agreedFare,
                                assignedDriverId = it.assignedDriverId.ifBlank { update.assignedDriverId },
                                driverName = it.driverName.ifBlank { update.driverName },
                                driverPhone = it.driverPhone.ifBlank { update.driverPhone },
                                driverPlateNumber = it.driverPlateNumber.ifBlank { update.driverPlateNumber },
                                driverVehicleMake = it.driverVehicleMake.ifBlank { update.driverVehicleMake },
                                driverVehicleModel = it.driverVehicleModel.ifBlank { update.driverVehicleModel },
                                driverVehicleColor = it.driverVehicleColor.ifBlank { update.driverVehicleColor },
                                driverRating = if (update.driverRating > 0) update.driverRating else it.driverRating
                            )
                            if (foundCompletedOrder == null) foundCompletedOrder = completed
                            completed
                        } else it
                    }

                    val targetOrder = foundCompletedOrder ?: PassengerOrder(
                        id = reqId,
                        requestId = reqId,
                        assignedDriverId = update.assignedDriverId,
                        status = PassengerOrderStatus.COMPLETED,
                        driverName = update.driverName,
                        driverPhone = update.driverPhone,
                        driverPlateNumber = update.driverPlateNumber,
                        driverVehicleMake = update.driverVehicleMake,
                        driverVehicleModel = update.driverVehicleModel,
                        agreedFare = update.assignedFare,
                        pickupTitle = selectedPickupLocation.title,
                        destinationTitle = selectedDestinationLocation?.title ?: "",
                        createdAt = System.currentTimeMillis()
                    )

                    val isSkippedOrRated = isOrderRatedOrSkipped(reqId, targetOrder.id) ||
                            repo.isRideRatedOrSkipped(reqId, targetOrder.id, "PASSENGER")
                    if (userMode == UserMode.PASSENGER && !isSkippedOrRated && !showPassengerRatingDialog) {
                        completedOrderForRating = targetOrder
                        showPassengerRatingDialog = true
                    }
                    snackbarHostState.showSnackbar("Ride completed! Thank you for riding with Drigo.")
                }
                "DRIVER_COMING", "ACCEPTED", "DRIVER_ARRIVED", "IN_TRIP" -> {
                    var dName = update.driverName
                    var dPhone = update.driverPhone
                    var dPlate = update.driverPlateNumber
                    var dMake = update.driverVehicleMake
                    var dModel = update.driverVehicleModel
                    var dColor = update.driverVehicleColor
                    var dRating = update.driverRating
                    var dRides = update.driverTotalRides

                    if (update.assignedDriverId.isNotBlank() && (dName.isBlank() || dPlate.isBlank() || dMake.isBlank())) {
                        val profile = repo.getDriverProfile(update.assignedDriverId)
                        if (profile != null) {
                            if (dName.isBlank()) dName = profile.name
                            if (dPhone.isBlank()) dPhone = profile.phone
                            if (dPlate.isBlank()) dPlate = profile.vehicleNumber
                            if (dMake.isBlank()) dMake = profile.vehicleCompany
                            if (dModel.isBlank()) dModel = profile.vehicleModel
                            if (dColor.isBlank()) dColor = profile.vehicleColor
                        }
                    }

                    val targetStatus = when (update.status) {
                        "DRIVER_ARRIVED" -> PassengerOrderStatus.DRIVER_ARRIVED
                        "IN_TRIP" -> PassengerOrderStatus.IN_TRIP
                        else -> PassengerOrderStatus.DRIVER_COMING
                    }

                    val existingIndex = passengerOrders.indexOfFirst { it.requestId == reqId || it.id == reqId }
                    if (existingIndex >= 0) {
                        val existing = passengerOrders[existingIndex]
                        val updated = existing.copy(
                            status = targetStatus,
                            driverName = dName.ifBlank { existing.driverName },
                            driverPhone = dPhone.ifBlank { existing.driverPhone },
                            driverPlateNumber = dPlate.ifBlank { existing.driverPlateNumber },
                            driverVehicleMake = dMake.ifBlank { existing.driverVehicleMake },
                            driverVehicleModel = dModel.ifBlank { existing.driverVehicleModel },
                            driverVehicleColor = dColor.ifBlank { existing.driverVehicleColor },
                            driverRating = if (dRating > 0) dRating else existing.driverRating,
                            driverTotalRides = if (dRides > 0) dRides else existing.driverTotalRides,
                            assignedDriverId = update.assignedDriverId.ifBlank { existing.assignedDriverId },
                            agreedFare = if (update.assignedFare > 0) update.assignedFare else existing.agreedFare,
                            etaMinutes = if (update.etaMinutes > 0) update.etaMinutes else existing.etaMinutes
                        )
                        passengerOrders = passengerOrders.toMutableList().apply { set(existingIndex, updated) }
                        com.example.data.remote.RideManager.saveActiveTrip(updated)
                        incomingDriverOffer = null
                        incomingDriverOffers = emptyList()
                        isBookingInProgress = false
                        isRideSheetExpanded = true
                        if (passengerMapDriverLoc == null) {
                            passengerMapDriverLoc = LiveDriverLocation(
                                rideId = reqId,
                                driverId = update.assignedDriverId,
                                latitude = selectedPickupLocation.latitude + 0.008,
                                longitude = selectedPickupLocation.longitude + 0.008,
                                bearing = 45f,
                                speedKmh = 35f,
                                etaMinutes = update.etaMinutes.coerceAtLeast(1),
                                distanceRemainingKm = 2.0,
                                status = targetStatus.name,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                    } else {
                        val newOrder = PassengerOrder(
                            id = reqId,
                            requestId = reqId,
                            pickupTitle = selectedPickupLocation.title,
                            pickupSubtitle = selectedPickupLocation.subtitle,
                            pickupLat = selectedPickupLocation.latitude,
                            pickupLon = selectedPickupLocation.longitude,
                            destinationTitle = selectedDestinationLocation?.title ?: "Destination",
                            destinationSubtitle = selectedDestinationLocation?.subtitle ?: "",
                            destinationLat = selectedDestinationLocation?.latitude ?: (selectedPickupLocation.latitude + 0.015),
                            destinationLon = selectedDestinationLocation?.longitude ?: (selectedPickupLocation.longitude + 0.015),
                            distanceKm = activeRoute?.distanceKm ?: 5.0,
                            durationMinutes = activeRoute?.durationMinutes ?: 12,
                            rideCategory = selectedRideCategory ?: "Ride A/C",
                            agreedFare = if (update.assignedFare > 0) update.assignedFare else 300,
                            paymentMethod = selectedPaymentMethod,
                            driverName = dName,
                            driverPhone = dPhone,
                            driverPlateNumber = dPlate,
                            driverVehicleMake = dMake,
                            driverVehicleModel = dModel,
                            driverVehicleColor = dColor,
                            driverRating = if (dRating > 0) dRating else 5.0,
                            driverTotalRides = dRides,
                            assignedDriverId = update.assignedDriverId,
                            status = targetStatus,
                            etaMinutes = update.etaMinutes
                        )
                        passengerOrders = listOf(newOrder) + passengerOrders
                        com.example.data.remote.RideManager.saveActiveTrip(newOrder)
                        incomingDriverOffer = null
                        incomingDriverOffers = emptyList()
                        isBookingInProgress = false
                        isRideSheetExpanded = true
                        if (passengerMapDriverLoc == null) {
                            passengerMapDriverLoc = LiveDriverLocation(
                                rideId = reqId,
                                driverId = update.assignedDriverId,
                                latitude = selectedPickupLocation.latitude + 0.008,
                                longitude = selectedPickupLocation.longitude + 0.008,
                                bearing = 45f,
                                speedKmh = 35f,
                                etaMinutes = update.etaMinutes.coerceAtLeast(1),
                                distanceRemainingKm = 2.0,
                                status = targetStatus.name,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
        }
    }

    // City to City Passenger Multi-Step Flow State (Screenshots 1, 2, 3)
    var cityToCityStep by remember { mutableStateOf(CityToCityStep.WHAT_RIDE) }
    var cityRideType by remember { mutableStateOf(CityRideType.PRIVATE) }
    var cityTimingIsNow by remember { mutableStateOf(true) }
    var cityScheduledDateTimeText by remember { mutableStateOf("Sun, 30 Aug 12:15 PM") }
    var cityPassengerCount by remember { mutableIntStateOf(1) }
    var cityComments by remember { mutableStateOf("") }

    // Driver Live Requests State
    var driverRideRequests by remember { mutableStateOf<List<RideRequest>>(emptyList()) }

    // Driver Planned Departures / City-to-City scheduled rides state
    var showDriverPlannedDepartures by remember { mutableStateOf(false) }
    var showDriverPostPlannedRide by remember { mutableStateOf(false) }
    var selectedDepartureForManageId by remember { mutableStateOf<String?>(null) }

    // Listen for live driver requests when driver is online
    LaunchedEffect(isDriverOnline) {
        if (isDriverOnline) {
            val repo = FirebaseRepository.getInstance(context)
            repo.listenToRideRequests().collectLatest { reqList ->
                driverRideRequests = reqList
            }
        }
    }

    // Function to submit ride request entry to Firebase Database
    fun submitRideRequest() {
        scope.launch {
            isBookingInProgress = true

            // Validate Pickup Coordinates
            if (selectedPickupLocation.latitude == 0.0 || selectedPickupLocation.longitude == 0.0 ||
                selectedPickupLocation.latitude.isNaN() || selectedPickupLocation.longitude.isNaN()) {
                Toast.makeText(context, "Invalid pickup location. Please tap the map to choose a pickup point.", Toast.LENGTH_SHORT).show()
                isBookingInProgress = false
                return@launch
            }

            // Ensure Pickup address corresponds exactly to current selected coordinates
            var freshPickup = selectedPickupLocation
            if (freshPickup.title.isBlank() || freshPickup.title == "Select Pickup Location" || freshPickup.title == "Current Location") {
                val (addrLine, areaName, city) = locationHelper.reverseGeocode(freshPickup.latitude, freshPickup.longitude)
                freshPickup = freshPickup.copy(
                    title = addrLine,
                    subtitle = if (areaName.isNotBlank() && areaName != addrLine) "$areaName, $city" else city
                )
                selectedPickupLocation = freshPickup
            }

            var freshDest = selectedDestinationLocation ?: AppLocation(
                title = "Selected Destination",
                subtitle = "",
                latitude = selectedPickupLocation.latitude + 0.015,
                longitude = selectedPickupLocation.longitude + 0.015
            )

            if (freshDest.title.isBlank() || freshDest.title == "Selected Destination") {
                val (dAddrLine, dAreaName, dCity) = locationHelper.reverseGeocode(freshDest.latitude, freshDest.longitude)
                freshDest = freshDest.copy(
                    title = dAddrLine,
                    subtitle = if (dAreaName.isNotBlank() && dAreaName != dAddrLine) "$dAreaName, $dCity" else dCity
                )
                selectedDestinationLocation = freshDest
            }

            val dest = freshDest

            val cat = if (selectedTopCategory == "city") {
                "City: ${cityRideType.title}"
            } else {
                selectedRideCategory ?: "Private AC"
            }
            val dist = activeRoute?.distanceKm ?: 5.0
            val calculatedBaseFare = when {
                selectedTopCategory == "city" -> {
                    when (cityRideType) {
                        CityRideType.PRIVATE -> (1200 + (dist * 42)).toInt()
                        CityRideType.SHARED -> (450 + (dist * 18)).toInt() * cityPassengerCount
                        CityRideType.PARCEL -> (600 + (dist * 20)).toInt()
                    }
                }
                cat in listOf("Ride A/C", "Private AC") -> 160 + (dist * 45).toInt()
                cat == "Mini" -> 120 + (dist * 35).toInt()
                cat == "Moto" -> 60 + (dist * 18).toInt()
                cat in listOf("Ride", "Private Non-AC") -> 110 + (dist * 35).toInt()
                cat in listOf("City to City", "City to city") -> 450 + (dist * 45).toInt()
                cat in listOf("Couriers", "Parcel", "Parcel Delivery") -> 70 + (dist * 22).toInt()
                cat == "Freight" -> 300 + (dist * 60).toInt()
                cat == "Share Ride" -> 80 + (dist * 25).toInt()
                cat in listOf("Book Car", "Book a Car") -> 160 + (dist * 45).toInt()
                else -> 160 + (dist * 45).toInt()
            }
            val finalFare = customOfferedFare ?: calculatedBaseFare
            val duration = activeRoute?.durationMinutes ?: 12

            val passengerId = user?.uid ?: "rider_${System.currentTimeMillis().toString().takeLast(6)}"
            val passengerName = user?.displayName?.ifBlank { "Passenger" } ?: (user?.email?.substringBefore("@") ?: "Passenger")
            val passengerEmail = user?.email ?: ""

            val request = RideRequest(
                id = UUID.randomUUID().toString(),
                passengerId = passengerId,
                passengerName = passengerName,
                passengerEmail = passengerEmail,
                passengerPhotoUrl = user?.photoUrl?.toString() ?: "",
                passengerRating = 4.9,
                paymentMethod = if (selectedPaymentMethod.equals("WALLET", true)) "Wallet" else "Cash",
                pickupTitle = freshPickup.title,
                pickupSubtitle = freshPickup.subtitle,
                pickupLat = freshPickup.latitude,
                pickupLon = freshPickup.longitude,
                destinationTitle = dest.title,
                destinationSubtitle = dest.subtitle,
                destinationLat = dest.latitude,
                destinationLon = dest.longitude,
                rideCategory = cat,
                vehicleType = when {
                    cat.contains("Bike", true) || cat.contains("Moto", true) -> "Bike"
                    cat.contains("Mini", true) -> "Mini Car"
                    cat.contains("Courier", true) || cat.contains("Parcel", true) -> "Courier"
                    else -> "Car"
                },
                hasAc = cat.contains("AC", true) || cat.contains("A/C", true),
                estimatedFare = finalFare,
                distanceKm = dist,
                durationMinutes = duration,
                status = "SEARCHING_DRIVERS",
                assignedDriverId = "",
                timestamp = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (15 * 60 * 1000L)
            )

            val repo = FirebaseRepository.getInstance(context)
            val result = repo.createRideRequest(request)
            isBookingInProgress = false
            showBookingDialog = false
            activeRideRequestId = request.id

            if (result.isSuccess) {
                val shortId = request.id.takeLast(6).uppercase()
                snackbarHostState.showSnackbar("Ride Request #$shortId sent! Searching for real nearby drivers...")
            } else {
                snackbarHostState.showSnackbar("Ride Request created. Searching for real nearby drivers...")
            }
        }
    }

    // Function to calculate / update route preserving the exact selected locations
    fun calculateAndSetRoute(pickup: AppLocation, destination: AppLocation) {
        scope.launch {
            isCalculatingRoute = true
            val route = routeService.calculateRoute(
                startPoint = pickup.toGeoPoint(),
                destPoint = destination.toGeoPoint(),
                startAddress = pickup.title,
                destinationAddress = destination.title
            )
            activeRoute = route
            isCalculatingRoute = false
        }
    }

    fun onMapTapped(lat: Double, lng: Double) {
        if (isRideActive) {
            Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val (addrLine, areaName, city) = locationHelper.reverseGeocode(lat, lng)
            val tappedLocation = AppLocation(
                title = addrLine,
                subtitle = if (areaName.isNotBlank() && areaName != addrLine) "$areaName, $city" else city,
                latitude = lat,
                longitude = lng
            )

            when (mapSelectionMode) {
                MapSelectionMode.PICKUP -> {
                    selectedPickupLocation = tappedLocation
                    isPickupExplicitlySet = true
                    mapSelectionMode = MapSelectionMode.NONE
                    if (selectedDestinationLocation != null) {
                        calculateAndSetRoute(tappedLocation, selectedDestinationLocation!!)
                    } else {
                        mapSelectionMode = MapSelectionMode.DESTINATION
                        scope.launch {
                            snackbarHostState.showSnackbar("Pickup set: ${tappedLocation.title}. Now tap map for Destination.")
                        }
                    }
                }
                MapSelectionMode.DESTINATION -> {
                    selectedDestinationLocation = tappedLocation
                    mapSelectionMode = MapSelectionMode.NONE
                    calculateAndSetRoute(selectedPickupLocation, tappedLocation)
                }
                MapSelectionMode.NONE -> {
                    if (selectedDestinationLocation == null && !isPickupExplicitlySet) {
                        // First tap sets FROM (Pickup) and prompts for Destination
                        selectedPickupLocation = tappedLocation
                        isPickupExplicitlySet = true
                        mapSelectionMode = MapSelectionMode.DESTINATION
                        scope.launch {
                            snackbarHostState.showSnackbar("Pickup set: ${tappedLocation.title}. Tap map to set Destination.")
                        }
                    } else {
                        // Tapping map directly updates destination and keeps pickup preserved
                        selectedDestinationLocation = tappedLocation
                        calculateAndSetRoute(selectedPickupLocation, tappedLocation)
                    }
                }
            }
        }
    }

    fun swapLocations() {
        if (isRideActive) {
            Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
            return
        }
        val curDest = selectedDestinationLocation ?: return
        val curPickup = selectedPickupLocation

        selectedPickupLocation = curDest
        selectedDestinationLocation = curPickup
        isPickupExplicitlySet = true

        calculateAndSetRoute(curDest, curPickup)
    }

    fun isIntercityTrip(from: AppLocation, to: AppLocation?, route: RouteResult?): Boolean {
        if (to == null) return false
        val cities = listOf(
            "Peshawar", "Islamabad", "Rawalpindi", "Lahore", "Karachi",
            "Mardan", "Nowshera", "Charsadda", "Kohat", "Abbottabad",
            "Swat", "Faisalabad", "Multan", "Gujranwala", "Sialkot",
            "Taxila", "Attock", "Wah", "Haripur", "Mansehra"
        )
        val fromFull = "${from.title} ${from.subtitle}".lowercase()
        val toFull = "${to.title} ${to.subtitle}".lowercase()

        val fromCity = cities.firstOrNull { fromFull.contains(it.lowercase()) } ?: "peshawar"
        val toCity = cities.firstOrNull { toFull.contains(it.lowercase()) }

        // Only intercity if destination has a distinct other city
        if (toCity != null && !fromCity.equals(toCity, ignoreCase = true)) {
            return true
        }
        if (route != null && route.distanceKm > 45.0 && toCity != null && !fromCity.equals(toCity, ignoreCase = true)) {
            return true
        }
        return false
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            hasLocationPermission = true
            scope.launch {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    userLocationData = loc
                    // Initialize pickup only once on startup if not explicitly chosen and no destination/route active
                    if (!isPickupExplicitlySet && selectedDestinationLocation == null && activeRoute == null) {
                        selectedPickupLocation = AppLocation(
                            title = loc.addressLine,
                            subtitle = "Current Location",
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                    }
                    recenterTrigger++
                }
            }
        }
    }

    // Check permissions and initialize GPS on startup
    LaunchedEffect(Unit) {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED
        ) {
            hasLocationPermission = true
            val loc = locationHelper.getCurrentLocation()
            if (loc != null) {
                userLocationData = loc
                if (!isPickupExplicitlySet && selectedDestinationLocation == null && activeRoute == null) {
                    selectedPickupLocation = AppLocation(
                        title = loc.addressLine,
                        subtitle = "Current Location",
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Continuous location updates flow: ONLY updates userLocationData for the live blue map marker.
    // It NEVER overwrites or alters selectedPickupLocation.
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                locationHelper.getLocationUpdatesFlow().collectLatest { liveLoc ->
                    userLocationData = liveLoc
                }
            } catch (_: Exception) {}
        }
    }

    val isRegisteredAsDriver = remember(driverVerification) {
        driverVerification != null && (
            driverVerification.uid.isNotBlank() ||
            driverVerification.submittedAt > 0L ||
            driverVerification.status.isNotBlank() ||
            driverVerification.documents.isNotEmpty()
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header in Brand Magenta (#9E0059)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DrigoBrandPurple)
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (userMode == UserMode.DRIVER) Icons.Default.Badge else Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.ic_drigo_logo),
                                    contentDescription = "Drigo App Logo",
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = user?.displayName?.ifBlank { if (userMode == UserMode.DRIVER) "Drigo Driver" else "Drigo Rider" }
                                    ?: (user?.email?.substringBefore("@") ?: "Drigo User"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = user?.email ?: "account@drigo.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Active Mode Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (userMode == UserMode.PASSENGER) "Passenger Mode" else "Driver Mode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Switch Card in Drawer
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, DrigoBrandPurple.copy(alpha = 0.4f)),
                        color = DrigoBrandPurple.copy(alpha = 0.06f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (userMode == UserMode.PASSENGER) "Switch to Driver" else "Switch to Passenger",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DrigoBrandPurple,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (userMode == UserMode.PASSENGER) "Accept rides & earn" else "Book rides & delivery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = userMode == UserMode.DRIVER,
                                onCheckedChange = { isDriver ->
                                    val newMode = if (isDriver) UserMode.DRIVER else UserMode.PASSENGER
                                    onSwitchUserMode(newMode)
                                    scope.launch { drawerState.close() }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DrigoBrandPurple,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (userMode == UserMode.PASSENGER) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = DrigoBrandPurple) },
                            label = { Text("Book a Ride", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = true,
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = DrigoBrandPurple) },
                            label = { Text("City to City Departures", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showCityDeparturesScreen = true
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.People, contentDescription = null) },
                            label = { Text("Share Ride", fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                selectedRideCategory = "Share Ride"
                                showBookingDialog = true
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                            label = { Text("Parcel Delivery", fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                selectedRideCategory = "Parcel"
                                showBookingDialog = true
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DepartureBoard, contentDescription = null, tint = DrigoBrandPurple) },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("City to City", fontSize = 14.sp)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = InDriveLimeGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Departures",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) InDriveLimeGreen else Color(0xFF00796B),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = selectedTopCategory == "city" || showPassengerScheduledDeparturesSheet,
                            onClick = {
                                scope.launch { drawerState.close() }
                                selectedTopCategory = "city"
                                showPassengerScheduledDeparturesSheet = true
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        if (!isRegisteredAsDriver) {
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF8B004F)) },
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Driver Registration",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF8B004F).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "24h Verify",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B004F),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onSwitchUserMode(UserMode.DRIVER)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    } else {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = DrigoBrandPurple) },
                            label = { Text("Driver Dashboard", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = !showDriverPlannedDepartures && !showDriverPostPlannedRide,
                            onClick = {
                                showDriverPlannedDepartures = false
                                showDriverPostPlannedRide = false
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AltRoute, contentDescription = null) },
                            label = { Text("Active Requests", fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                showDriverPlannedDepartures = false
                                showDriverPostPlannedRide = false
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DepartureBoard, contentDescription = "City to City", tint = DrigoBrandPurple) },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "City to City",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DrigoBrandPurple.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Scheduled",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DrigoBrandPurple,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = showDriverPlannedDepartures || showDriverPostPlannedRide,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showDriverPlannedDepartures = true
                            },
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .testTag("drawer_driver_city_to_city_item")
                        )
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF00A859)) },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Wallet", fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF00A859).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Easypaisa",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00A859),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToWallet()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_wallet_item")
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.History, contentDescription = null, tint = DrigoBrandPurple) },
                        label = { Text("Trip History", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToTripHistory()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_history_item")
                    )

                    NavigationDrawerItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (notificationHistory.isNotEmpty()) {
                                        Badge(containerColor = DrigoBrandPurple) {
                                            Text(
                                                text = if (notificationHistory.size > 9) "9+" else "${notificationHistory.size}",
                                                fontSize = 9.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = DrigoBrandPurple)
                            }
                        },
                        label = { Text("Notifications", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showNotificationCenterSheet = true
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_notifications_item")
                    )

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = DrigoBrandPurple
                            )
                        },
                        label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("drawer_settings_item")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        label = { Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onSignOutClick()
                            }
                        },
                        modifier = Modifier.padding(12.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    ) {
        if (userMode == UserMode.PASSENGER) {
            // ================= PASSENGER MODE =================
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("passenger_home_screen")
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (passengerNavTab == 0) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            val screenMaxHeight = maxHeight
                            // Real OpenStreetMap Map with User's GPS Marker, Destination Marker, Pickup Marker & Route Polyline
                            RealOsmMapView(
                                modifier = Modifier.fillMaxSize(),
                                currentLatitude = userLocationData?.latitude,
                                currentLongitude = userLocationData?.longitude,
                                fromLocation = selectedPickupLocation,
                                toLocation = selectedDestinationLocation,
                                routeResult = activeRoute,
                                driverCarLocation = passengerMapDriverLoc?.let { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) },
                                driverCarBearing = passengerMapDriverLoc?.bearing,
                                driverCarTitle = activePassengerOrder?.let { "${it.driverName.ifBlank { "Captain" }}${if (it.driverPlateNumber.isNotBlank()) " (${it.driverPlateNumber})" else ""}" } ?: "",
                                driverCarFareText = activePassengerOrder?.let { "${it.agreedFare} Rs" },
                                nearbyDriverMarkers = nearbyDriverMarkers,
                                onNearbyDriverMarkerClick = { markerData ->
                                    scope.launch {
                                        if (markerData.driverOffer != null) {
                                            val offer = markerData.driverOffer
                                            val converted = PassengerOrder(
                                                id = UUID.randomUUID().toString(),
                                                requestId = offer.requestId,
                                                pickupTitle = selectedPickupLocation.title,
                                                pickupSubtitle = selectedPickupLocation.subtitle,
                                                pickupLat = selectedPickupLocation.latitude,
                                                pickupLon = selectedPickupLocation.longitude,
                                                destinationTitle = selectedDestinationLocation?.title ?: "Destination",
                                                destinationSubtitle = selectedDestinationLocation?.subtitle ?: "",
                                                destinationLat = selectedDestinationLocation?.latitude ?: (selectedPickupLocation.latitude + 0.015),
                                                destinationLon = selectedDestinationLocation?.longitude ?: (selectedPickupLocation.longitude + 0.015),
                                                distanceKm = activeRoute?.distanceKm ?: 5.0,
                                                durationMinutes = activeRoute?.durationMinutes ?: 12,
                                                rideCategory = selectedRideCategory ?: "Ride A/C",
                                                agreedFare = offer.offeredFare,
                                                paymentMethod = selectedPaymentMethod,
                                                driverName = offer.driverName,
                                                driverRating = if (offer.driverRating > 0) offer.driverRating else 5.0,
                                                driverTotalRides = offer.driverTotalRides,
                                                driverVehicleMake = offer.driverVehicleMake,
                                                driverVehicleModel = offer.driverVehicleModel,
                                                driverVehicleColor = offer.driverVehicleColor,
                                                driverPlateNumber = offer.driverPlateNumber,
                                                driverPhone = offer.driverPhone,
                                                status = PassengerOrderStatus.ACCEPTED,
                                                etaMinutes = offer.etaMinutes
                                            )
                                            incomingDriverOffer = converted
                                        } else {
                                            snackbarHostState.showSnackbar("${markerData.driverName} available nearby (${markerData.fareText})")
                                        }
                                    }
                                },
                                 recenterTrigger = recenterTrigger,
                                showCenterPickupPin = (mapSelectionMode == MapSelectionMode.PICKUP),
                                mapSelectionMode = mapSelectionMode,
                                onMapTapped = { lat, lng -> onMapTapped(lat, lng) },
                                onCancelMapSelection = { mapSelectionMode = MapSelectionMode.NONE },
                                onWhereFromClick = { 
                                    cardInitialEditPickup = true
                                    showPickupDestinationCard = true 
                                },
                                onMapCenterChanged = { centerLat, centerLng ->
                                    if (mapSelectionMode == MapSelectionMode.PICKUP) {
                                        scope.launch {
                                            val (addrLine, areaName, city) = locationHelper.reverseGeocode(centerLat, centerLng)
                                            selectedPickupLocation = AppLocation(
                                                title = addrLine,
                                                subtitle = if (areaName.isNotBlank() && areaName != addrLine) "$areaName, $city" else city,
                                                latitude = centerLat,
                                                longitude = centerLng
                                            )
                                            isPickupExplicitlySet = true
                                            if (selectedDestinationLocation != null) {
                                                calculateAndSetRoute(selectedPickupLocation, selectedDestinationLocation!!)
                                            }
                                        }
                                    }
                                },
                                onMapInteractionChange = { isInteracting ->
                                    isMapInteracting = isInteracting
                                }
                            )

                // Passenger Account Restricted / Suspended Overlay
                if (passengerAccStatus == com.example.data.model.PassengerAccountStatus.SUSPENDED ||
                    passengerAccStatus == com.example.data.model.PassengerAccountStatus.DEACTIVATED) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFE53935))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Account Restricted",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Account Restricted / Suspended",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your passenger account has been restricted by compliance. You cannot request rides or process payments at this time. Please contact support.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:support@drigo.com?subject=Passenger%20Account%20Appeal%20UID%20${user?.uid}")
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Contact Support", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Top Persistent inDrive Route Panel (Attachment 1)
                if (activeRoute != null) {
                    val route = activeRoute!!
                    InDriveRouteTopCard(
                        pickupTitle = selectedPickupLocation.title,
                        destinationTitle = selectedDestinationLocation?.title ?: route.destinationAddress,
                        durationMinutes = route.durationMinutes,
                        pickupLat = selectedPickupLocation.latitude,
                        pickupLon = selectedPickupLocation.longitude,
                        destinationLat = selectedDestinationLocation?.latitude ?: route.destinationPoint.latitude,
                        destinationLon = selectedDestinationLocation?.longitude ?: route.destinationPoint.longitude,
                        onPickupClick = {
                            cardInitialEditPickup = true
                            showPickupDestinationCard = true
                        },
                        onDestinationClick = {
                            cardInitialEditPickup = false
                            showPickupDestinationCard = true
                        },
                        onAddStopClick = {
                            cardInitialEditPickup = false
                            showPickupDestinationCard = true
                        },
                        onPickPickupOnMap = {
                            mapSelectionMode = MapSelectionMode.PICKUP
                        },
                        onPickDestinationOnMap = {
                            mapSelectionMode = MapSelectionMode.DESTINATION
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 50.dp, start = 14.dp, end = 14.dp)
                    )
                } else {
                    // Top Bar with Hamburger Menu + Direct "From" & "To" Location Selector Card
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 50.dp, start = 14.dp, end = 14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Circular Hamburger Menu Button
                        Surface(
                            onClick = { scope.launch { drawerState.open() } },
                            shape = CircleShape,
                            color = DrigoBrandPurple,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("menu_hamburger_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Top Compact Location Card showing From and To
                        Surface(
                            onClick = {
                                if (isRideActive) {
                                    Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
                                } else {
                                    cardInitialEditPickup = selectedDestinationLocation != null
                                    showPickupDestinationCard = true
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            border = BorderStroke(1.2.dp, if (isRideActive) Color(0xFFFFB74D).copy(alpha = 0.6f) else DrigoBrandPurple.copy(alpha = 0.6f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("top_from_to_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                // From Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isRideActive) {
                                                Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                cardInitialEditPickup = true
                                                showPickupDestinationCard = true
                                            }
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "From: ${selectedPickupLocation.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val pLat = if (selectedPickupLocation.latitude != 0.0) selectedPickupLocation.latitude else 34.0151
                                        val pLon = if (selectedPickupLocation.longitude != 0.0) selectedPickupLocation.longitude else 71.5249
                                        Text(
                                            text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", pLat, pLon),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (!isRideActive) {
                                        IconButton(
                                            onClick = {
                                                mapSelectionMode = MapSelectionMode.PICKUP
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = "Set Pickup on Map",
                                                tint = Color(0xFF81C784),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFFFFB74D),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(4.dp))

                                // To Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isRideActive) {
                                                Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                cardInitialEditPickup = false
                                                showPickupDestinationCard = true
                                            }
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE53935),
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (selectedDestinationLocation != null) "To: ${selectedDestinationLocation!!.title}" else "To: Tap on map / choose destination",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (selectedDestinationLocation != null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedDestinationLocation != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selectedDestinationLocation != null) {
                                            val dLat = if (selectedDestinationLocation!!.latitude != 0.0) selectedDestinationLocation!!.latitude else 34.0351
                                            val dLon = if (selectedDestinationLocation!!.longitude != 0.0) selectedDestinationLocation!!.longitude else 71.5449
                                            Text(
                                                text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", dLat, dLon),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    if (!isRideActive) {
                                        IconButton(
                                            onClick = {
                                                mapSelectionMode = MapSelectionMode.DESTINATION
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = "Set Destination on Map",
                                                tint = Color(0xFFFF80AB),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFFFFB74D),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Notification Center Bell Button with Live Badge
                        Surface(
                            onClick = { showNotificationCenterSheet = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("notification_center_bell_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notification Center",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (notificationHistory.isNotEmpty()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = DrigoBrandPurple,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .align(Alignment.TopEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (notificationHistory.size > 9) "9+" else "${notificationHistory.size}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Real-time floating trip status indicator overlay on top of the map
                if (isRideActive && activePassengerOrder != null) {
                    val status = activePassengerOrder.status
                    val isArrived = status == PassengerOrderStatus.DRIVER_ARRIVED
                    val isInTrip = status == PassengerOrderStatus.IN_TRIP
                    val label = when {
                        isInTrip -> "TRIP IN PROGRESS"
                        isArrived -> "DRIVER ARRIVED • WAITING"
                        else -> "DRIVER EN ROUTE"
                    }
                    val badgeColor = when {
                        isInTrip -> Color(0xFF29B6F6) // Sky blue
                        isArrived -> Color(0xFF00E676) // Emerald green
                        else -> Color(0xFFFFB300) // Amber
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.2.dp, badgeColor),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 160.dp) // Aligns it beautifully just below the top location cards
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val pulseAlpha = rememberInfiniteTransition(label = "pulse_floating").animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dot"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .graphicsLayer { alpha = pulseAlpha.value }
                                    .background(badgeColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Floating GPS / Location Recenter Button (when map is being panned/zoomed)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isMapInteracting,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(150)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp)
                ) {
                    Surface(
                        onClick = {
                            scope.launch {
                                val loc = locationHelper.getCurrentLocation()
                                if (loc != null) {
                                    userLocationData = loc
                                }
                                recenterTrigger++
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("recenter_location_btn_map_active")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Recenter to Current GPS Location",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Expandable / Collapsible Bottom Booking Container
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isMapInteracting,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(180)),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(150)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Floating Controls (Back button on left + Recenter GPS button on right) positioned above bottom sheet
                        if (!isRideSheetExpanded) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = if (activeRoute != null) Arrangement.SpaceBetween else Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeRoute != null) {
                                    // Floating Back button on map (Attachment 1)
                                    Surface(
                                        onClick = {
                                            if (selectedTopCategory == "city") {
                                                when (cityToCityStep) {
                                                    CityToCityStep.CUSTOMIZE -> cityToCityStep = CityToCityStep.WHEN_START
                                                    CityToCityStep.WHEN_START -> cityToCityStep = CityToCityStep.WHAT_RIDE
                                                    CityToCityStep.WHAT_RIDE -> {
                                                        activeRoute = null
                                                        selectedDestinationLocation = null
                                                        recenterTrigger++
                                                    }
                                                }
                                            } else {
                                                activeRoute = null
                                                selectedDestinationLocation = null
                                                recenterTrigger++
                                            }
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("floating_map_back_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                // Safety / SOS Floating Button (Shield Icon)
                                Surface(
                                    onClick = {
                                        showSafetySheet = true
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("safety_sos_float_btn")
                                 ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Safety & Emergency Toolkit",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        scope.launch {
                                            val loc = locationHelper.getCurrentLocation()
                                            if (loc != null) {
                                                userLocationData = loc
                                            }
                                            recenterTrigger++
                                        }
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("recenter_location_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = "Recenter to Current GPS Location",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Intercity Tolls Banner: visible if From and To are different cities
                        val showTollsBanner = isIntercityTrip(
                            from = selectedPickupLocation,
                            to = selectedDestinationLocation,
                            route = activeRoute
                        )

                        if (showTollsBanner && !isRideSheetExpanded) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DrigoBrandPurple,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .testTag("tolls_info_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = "Tolls Payment",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Tolls will be paid separately\nto the driver",
                                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 18.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Dynamic inDrive Fare Calculations
                        val dist = activeRoute?.distanceKm ?: 5.0
                        val miniFare = 120 + (dist * 35).toInt()
                        val motoFare = 60 + (dist * 18).toInt()
                        val acFare = 160 + (dist * 45).toInt()
                        val courierFare = 70 + (dist * 22).toInt()

                        val inDriveRideOptions = remember(dist) {
                            listOf(
                                InDriveRideOption(
                                    id = "mini",
                                    title = "Mini",
                                    capacityText = "👤 4",
                                    subtitle = "Lower fares, no AC",
                                    baseFare = miniFare
                                ),
                                InDriveRideOption(
                                    id = "moto",
                                    title = "Moto",
                                    capacityText = "👤 1",
                                    subtitle = "No traffic, lower prices",
                                    baseFare = motoFare,
                                    isMoto = true
                                ),
                                InDriveRideOption(
                                    id = "ride_ac",
                                    title = "Ride A/C",
                                    capacityText = "👤 4",
                                    subtitle = "Cars with AC",
                                    baseFare = acFare,
                                    hasAc = true
                                ),
                                InDriveRideOption(
                                    id = "couriers",
                                    title = "Couriers",
                                    capacityText = "up to 20kg",
                                    subtitle = "Request package delivery",
                                    baseFare = courierFare,
                                    isCourier = true
                                )
                            )
                        }

                        val currentSelectedOption = inDriveRideOptions.find { it.id == selectedRideOptionId } ?: inDriveRideOptions[2]
                        val effectiveCustomFare = customOfferedFare ?: currentSelectedOption.baseFare

                        // Check if City to City Passenger Flow is active
                        if (activeRoute != null && selectedTopCategory == "city") {
                            // City-to-City Passenger Flow (Screenshots 1, 2, 3)
                            val cityDist = activeRoute?.distanceKm ?: 25.0
                            val defaultCityFare = when (cityRideType) {
                                CityRideType.PRIVATE -> (1200 + (cityDist * 42)).toInt()
                                CityRideType.SHARED -> (450 + (cityDist * 18)).toInt() * cityPassengerCount
                                CityRideType.PARCEL -> (600 + (cityDist * 20)).toInt()
                            }
                            val effectiveCityFare = customOfferedFare ?: defaultCityFare

                            CityToCityPassengerFlow(
                                distanceKm = cityDist,
                                currentStep = cityToCityStep,
                                selectedRideType = cityRideType,
                                selectedTimingIsNow = cityTimingIsNow,
                                scheduledDateTimeText = cityScheduledDateTimeText,
                                passengerCount = cityPassengerCount,
                                customFare = effectiveCityFare,
                                comments = cityComments,
                                onStepChange = { nextStep -> cityToCityStep = nextStep },
                                onRideTypeChange = { type ->
                                    cityRideType = type
                                    customOfferedFare = when (type) {
                                        CityRideType.PRIVATE -> (1200 + (cityDist * 42)).toInt()
                                        CityRideType.SHARED -> (450 + (cityDist * 18)).toInt() * cityPassengerCount
                                        CityRideType.PARCEL -> (600 + (cityDist * 20)).toInt()
                                    }
                                },
                                onTimingChange = { isNow -> cityTimingIsNow = isNow },
                                onScheduledDateTimeChange = { dt -> cityScheduledDateTimeText = dt },
                                onPassengerCountChange = { count ->
                                    cityPassengerCount = count
                                    if (cityRideType == CityRideType.SHARED) {
                                        customOfferedFare = (450 + (cityDist * 18)).toInt() * count
                                    }
                                },
                                onDecreaseFare = {
                                    val current = customOfferedFare ?: defaultCityFare
                                    customOfferedFare = (current - 100).coerceAtLeast(100)
                                },
                                onIncreaseFare = {
                                    val current = customOfferedFare ?: defaultCityFare
                                    customOfferedFare = current + 100
                                },
                                onCommentsChange = { comm -> cityComments = comm },
                                onPaymentMethodClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Payment method: Cash")
                                    }
                                },
                                onFindDriverClick = {
                                    submitRideRequest()
                                }
                            )
                        } else {
                            val defaultPeekHeight = when {
                                activePassengerOrder != null -> (screenMaxHeight * 0.52f).coerceIn(340.dp, 440.dp)
                                activeRoute != null -> (screenMaxHeight * 0.78f).coerceIn(460.dp, 580.dp)
                                else -> 330.dp
                            }
                            val targetHeight = if (isRideSheetExpanded) (screenMaxHeight * 0.94f) else defaultPeekHeight
                            val animatedSheetHeight by animateDpAsState(
                                targetValue = targetHeight,
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                label = "ride_sheet_height"
                            )

                            // Bottom Sheet matching theme
                            Surface(
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = 24.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(animatedSheetHeight)
                                    .testTag("ride_booking_sheet")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 6.dp)
                                ) {
                                    // Drag Handle Section with vertical drag gesture & click toggle
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 6.dp)
                                            .pointerInput(Unit) {
                                                detectVerticalDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    if (dragAmount < -12f) {
                                                        isRideSheetExpanded = true
                                                    } else if (dragAmount > 12f) {
                                                        isRideSheetExpanded = false
                                                    }
                                                }
                                            }
                                            .clickable { isRideSheetExpanded = !isRideSheetExpanded },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(44.dp)
                                                .height(5.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
                                        )
                                    }

                                    if (activePassengerOrder != null) {
                                        val order = activePassengerOrder
                                        ActiveCaptainAssignedCard(
                                            order = order,
                                            liveLocation = passengerMapDriverLoc,
                                            onCallCaptain = {
                                                try {
                                                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                        data = android.net.Uri.parse("tel:${order.driverPhone.ifBlank { "+923001234567" }}")
                                                    }
                                                    context.startActivity(dialIntent)
                                                } catch (_: Exception) {}
                                            },
                                            onOpenChat = {
                                                chatTripId = order.requestId.ifBlank { order.id }
                                                chatPartnerName = order.driverName.ifBlank { "Captain" }
                                                chatPartnerRole = "Driver"
                                                chatPickupTitle = order.pickupTitle
                                                chatDestinationTitle = order.destinationTitle
                                                showChatSheet = true
                                            },
                                            onOpenSafety = {
                                                showSafetySheet = true
                                            },
                                            onCancelRide = {
                                                cancelRideAndReturnHome(order.id, order.requestId)
                                            },
                                            onShareTrip = {
                                                val etaVal = passengerMapDriverLoc?.etaMinutes ?: order.etaMinutes
                                                val distVal = passengerMapDriverLoc?.distanceRemainingKm ?: order.distanceKm
                                                val shareText = "I am sharing my live Drigo ride with you!\n\n" +
                                                        "📍 Pickup: ${order.pickupTitle}\n" +
                                                        "🏁 Destination: ${order.destinationTitle}\n" +
                                                        "⏱️ Live ETA: ${etaVal} mins (${String.format("%.1f", distVal)} km remaining)\n" +
                                                        "🚗 Captain: ${order.driverName.ifBlank { "Assigned Captain" }} (${order.driverVehicleMake} ${order.driverVehicleModel} - ${order.driverPlateNumber.ifBlank { "Vehicle" }})\n" +
                                                        "💳 Agreed Fare: PKR ${order.agreedFare}\n" +
                                                        (if (order.destinationLat != 0.0 && order.destinationLon != 0.0) "🗺️ Track Route on Map: https://maps.google.com/?q=${order.destinationLat},${order.destinationLon}\n\n" else "\n") +
                                                        "Track my ride safely on Drigo!"

                                                try {
                                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Drigo Live Ride & ETA")
                                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                    }
                                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Ride & ETA via")
                                                    context.startActivity(shareIntent)
                                                } catch (_: Exception) {
                                                    android.widget.Toast.makeText(context, "Unable to share ride info", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    } else if (activeRideRequestId != null) {
                                        // ===== RIDE REQUEST POSTED: Searching for real drivers with Smart Bidding =====
                                        SearchingForDriversCard(
                                            pickupTitle = selectedPickupLocation.title,
                                            pickupLat = selectedPickupLocation.latitude,
                                            pickupLon = selectedPickupLocation.longitude,
                                            destinationTitle = selectedDestinationLocation?.title ?: "Destination",
                                            destinationLat = selectedDestinationLocation?.latitude ?: 0.0,
                                            destinationLon = selectedDestinationLocation?.longitude ?: 0.0,
                                            rideCategory = selectedRideCategory ?: "Ride A/C",
                                            offeredFare = effectiveCustomFare,
                                            incomingOffers = incomingDriverOffers,
                                            onAcceptOffer = { offer ->
                                                val acceptedOrder = PassengerOrder(
                                                    id = UUID.randomUUID().toString(),
                                                    requestId = offer.requestId.ifBlank { activeRideRequestId ?: "" },
                                                    pickupTitle = selectedPickupLocation.title,
                                                    pickupSubtitle = selectedPickupLocation.subtitle,
                                                    pickupLat = selectedPickupLocation.latitude,
                                                    pickupLon = selectedPickupLocation.longitude,
                                                    destinationTitle = selectedDestinationLocation?.title ?: "Destination",
                                                    destinationSubtitle = selectedDestinationLocation?.subtitle ?: "",
                                                    destinationLat = selectedDestinationLocation?.latitude ?: (selectedPickupLocation.latitude + 0.015),
                                                    destinationLon = selectedDestinationLocation?.longitude ?: (selectedPickupLocation.longitude + 0.015),
                                                    distanceKm = activeRoute?.distanceKm ?: 5.0,
                                                    durationMinutes = activeRoute?.durationMinutes ?: 12,
                                                    rideCategory = selectedRideCategory ?: "Ride A/C",
                                                    agreedFare = offer.offeredFare,
                                                    paymentMethod = selectedPaymentMethod,
                                                    driverName = offer.driverName,
                                                    driverRating = if (offer.driverRating > 0) offer.driverRating else 5.0,
                                                    driverTotalRides = offer.driverTotalRides,
                                                    driverVehicleMake = offer.driverVehicleMake,
                                                    driverVehicleModel = offer.driverVehicleModel,
                                                    driverVehicleColor = offer.driverVehicleColor,
                                                    driverPlateNumber = offer.driverPlateNumber,
                                                    driverPhone = offer.driverPhone,
                                                    assignedDriverId = offer.driverId.ifBlank { offer.driverPhone.ifBlank { offer.driverName } },
                                                    status = PassengerOrderStatus.DRIVER_COMING,
                                                    etaMinutes = offer.etaMinutes
                                                )
                                                // 1. Dismiss dialog & clear search
                                                incomingDriverOffer = null
                                                incomingDriverOffers = emptyList()

                                                // 2. Initialize live driver location
                                                val dLat = if (offer.driverLat != 0.0) offer.driverLat else (selectedPickupLocation.latitude + 0.009)
                                                val dLon = if (offer.driverLon != 0.0) offer.driverLon else (selectedPickupLocation.longitude + 0.009)
                                                val initialDriverLoc = LiveDriverLocation(
                                                    rideId = acceptedOrder.requestId.ifBlank { acceptedOrder.id },
                                                    driverId = offer.driverId,
                                                    latitude = dLat,
                                                    longitude = dLon,
                                                    bearing = 45f,
                                                    speedKmh = 35f,
                                                    etaMinutes = acceptedOrder.etaMinutes,
                                                    distanceRemainingKm = offer.distanceKmAway,
                                                    status = PassengerOrderStatus.DRIVER_COMING.name,
                                                    updatedAt = System.currentTimeMillis()
                                                )
                                                passengerMapDriverLoc = initialDriverLoc

                                                // 3. Set passenger active order
                                                passengerOrders = listOf(acceptedOrder) + passengerOrders.filter { it.id != acceptedOrder.id && it.requestId != acceptedOrder.requestId }

                                                // 4. Update backend
                                                val repo = FirebaseRepository.getInstance(context)
                                                scope.launch {
                                                    repo.savePassengerOrder(acceptedOrder)
                                                    repo.updateRideRequestStatus(acceptedOrder.requestId, "DRIVER_COMING")
                                                    repo.updateLiveDriverLocation(initialDriverLoc)
                                                    snackbarHostState.showSnackbar(
                                                        message = "Captain ${offer.driverName} is on the way! (~${acceptedOrder.etaMinutes} min away)",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            },
                                            onDeclineOffer = { offer ->
                                                incomingDriverOffers = incomingDriverOffers.filter { it.id != offer.id }
                                                if (incomingDriverOffer?.requestId == offer.requestId && incomingDriverOffer?.driverName == offer.driverName) {
                                                    incomingDriverOffer = null
                                                }
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Declined offer from ${offer.driverName.ifBlank { "Captain" }}")
                                                }
                                            },
                                            onCounterOffer = { offer, counterFare ->
                                                scope.launch {
                                                    val reqId = activeRideRequestId
                                                    if (reqId != null) {
                                                        customOfferedFare = counterFare
                                                        val repo = FirebaseRepository.getInstance(context)
                                                        repo.updateRideRequestFare(reqId, counterFare)
                                                        snackbarHostState.showSnackbar("Counter offer of PKR $counterFare sent to ${offer.driverName}")
                                                    }
                                                }
                                            },
                                            onBoostFare = { newFare ->
                                                customOfferedFare = newFare
                                                val reqId = activeRideRequestId
                                                if (reqId != null) {
                                                    scope.launch {
                                                        val repo = FirebaseRepository.getInstance(context)
                                                        repo.updateRideRequestFare(reqId, newFare)
                                                        snackbarHostState.showSnackbar("Offered fare boosted to PKR $newFare")
                                                    }
                                                }
                                            },
                                            onCancelSearch = {
                                                cancelRideAndReturnHome("", activeRideRequestId ?: "")
                                            }
                                        )
                                    } else if (activeRoute != null) {
                                        // ===== ROUTE ACTIVE: inDrive Full Ride Selection UI (Attachments 1, 2, 3) =====

                                        // Subtitle: "No traffic, lower prices"
                                        Text(
                                            text = "No traffic, lower prices",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp)
                                        )

                                        // Scrollable Ride Options List (Attachment 2: Full view of sheet where can select the rider, prices for all)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                InDriveRideOptionsList(
                                                    rideOptions = inDriveRideOptions,
                                                    selectedOptionId = selectedRideOptionId,
                                                    customFare = effectiveCustomFare,
                                                    onSelectOption = { opt ->
                                                        selectedRideOptionId = opt.id
                                                        selectedRideCategory = opt.title
                                                        customOfferedFare = opt.baseFare
                                                    },
                                                    onDecreaseFare = {
                                                        val current = customOfferedFare ?: currentSelectedOption.baseFare
                                                        customOfferedFare = (current - 20).coerceAtLeast(50)
                                                    },
                                                    onIncreaseFare = {
                                                        val current = customOfferedFare ?: currentSelectedOption.baseFare
                                                        customOfferedFare = current + 20
                                                    },
                                                    onSetFare = { newFare ->
                                                        customOfferedFare = newFare
                                                    }
                                                )
                                            }
                                        }

                                        // Fixed Lime Green "Find drivers" Bottom Bar
                                        InDriveFixedBottomBar(
                                            currentFare = effectiveCustomFare,
                                            autoAcceptOffer = autoAcceptOffer,
                                            onAutoAcceptChange = { autoAcceptOffer = it },
                                            onFindDriversClick = {
                                                submitRideRequest()
                                            },
                                            onPaymentMethodClick = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Payment method: Cash")
                                                }
                                            },
                                            onOptionsClick = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Ride preferences: AC, Music, Luggage enabled")
                                                }
                                            }
                                        )
                                    } else {

                                        val topCategories = remember {
                                            listOf(
                                                Triple("ride_ac", "Ride A/C", "Book Car"),
                                                Triple("ride", "Ride", "Book Car"),
                                                Triple("city", "City to city", "Book Car"),
                                                Triple("couriers", "Couriers", "Parcel")
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .padding(horizontal = 16.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            topCategories.forEach { (catId, catLabel, defaultRide) ->
                                                val isSelected = selectedTopCategory == catId

                                                Surface(
                                                    onClick = {
                                                        selectedTopCategory = catId
                                                        selectedRideCategory = defaultRide
                                                    },
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSelected) DrigoBrandPurple else MaterialTheme.colorScheme.outlineVariant
                                                    ),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = 14.dp)
                                                    ) {
                                                        Text(
                                                            text = catLabel,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 13.sp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .verticalScroll(rememberScrollState())
                                                .padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Prominent City to City Scheduled Departures Browse Card
                                            if (selectedTopCategory == "city") {
                                                Surface(
                                                    onClick = { showCityDeparturesScreen = true },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = DrigoBrandPurple,
                                                    shadowElevation = 3.dp,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DirectionsBus,
                                                            contentDescription = "City to City",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Browse City to City Departures",
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                fontSize = 14.sp
                                                            )
                                                            Text(
                                                                text = "Find driver offers between cities, book seats or full car",
                                                                color = Color.White.copy(alpha = 0.85f),
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowForward,
                                                            contentDescription = "Open",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // "Where To?" Search Entry Card
                                            Surface(
                                                onClick = {
                                                    cardInitialEditPickup = false
                                                    showPickupDestinationCard = true
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Search",
                                                        tint = DrigoBrandPurple,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = "Where To?",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            if (selectedTopCategory == "city") {
                                                Surface(
                                                    onClick = { showPassengerScheduledDeparturesSheet = true },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = if (isDark) Color(0xFF1E222D) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                                    border = BorderStroke(1.dp, DrigoBrandPurple.copy(alpha = 0.4f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = DrigoBrandPurple.copy(alpha = 0.15f),
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.DepartureBoard,
                                                                    contentDescription = null,
                                                                    tint = DrigoBrandPurple,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Browse Scheduled Departures",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Join intercity rides with fixed/negotiable seats",
                                                                fontSize = 10.5.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowForwardIos,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // 3-Card Bento Layout in Initial Discovery View
                                            RideCategoryBentoCards(
                                                selectedCategory = selectedRideCategory,
                                                onShareRideClick = {
                                                    selectedRideCategory = "Share Ride"
                                                    selectedTopCategory = "city"
                                                    showPassengerScheduledDeparturesSheet = true
                                                },
                                                onSendParcelClick = {
                                                    selectedRideCategory = "Parcel"
                                                    selectedTopCategory = "couriers"
                                                    cardInitialEditPickup = false
                                                    showPickupDestinationCard = true
                                                },
                                                onRequestCarClick = {
                                                    selectedRideCategory = "Book a Car"
                                                    selectedTopCategory = "ride_ac"
                                                    cardInitialEditPickup = false
                                                    showPickupDestinationCard = true
                                                },
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // ===== BOTTOM ACTION AREA =====
                                    if (activeRoute == null) {
                                        // Initial Discovery "Select Destination" Button
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    cardInitialEditPickup = false
                                                    showPickupDestinationCard = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("find_drivers_btn")
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Select Destination",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = Color.White
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

                // Feedback Snackbar
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
                )
            }
        } else {
            // My Orders Screen (Tab 1)
            MyOrdersScreen(
                orders = passengerOrders,
                currentUserId = user?.uid ?: "passenger_user",
                currentUserName = user?.displayName ?: (user?.email?.substringBefore("@") ?: "Passenger"),
                onTrackOnMap = { order ->
                    passengerNavTab = 0
                    if (order.pickupTitle.isNotBlank()) {
                        selectedPickupLocation = AppLocation(
                            title = order.pickupTitle,
                            subtitle = order.pickupSubtitle,
                            latitude = if (order.pickupLat != 0.0) order.pickupLat else selectedPickupLocation.latitude,
                            longitude = if (order.pickupLon != 0.0) order.pickupLon else selectedPickupLocation.longitude
                        )
                        isPickupExplicitlySet = true
                    }
                    if (order.destinationTitle.isNotBlank()) {
                        val dest = AppLocation(
                            title = order.destinationTitle,
                            subtitle = order.destinationSubtitle,
                            latitude = if (order.destinationLat != 0.0) order.destinationLat else selectedPickupLocation.latitude + 0.02,
                            longitude = if (order.destinationLon != 0.0) order.destinationLon else selectedPickupLocation.longitude + 0.02
                        )
                        selectedDestinationLocation = dest
                        calculateAndSetRoute(selectedPickupLocation, dest)
                    }
                },
                onOpenChat = { order ->
                    chatTripId = order.id
                    chatPartnerName = order.driverName
                    chatPartnerRole = "Driver"
                    chatPartnerPhone = order.driverPhone
                    chatPickupTitle = order.pickupTitle
                    chatDestinationTitle = order.destinationTitle
                    showChatSheet = true
                },
                onCancelOrder = { order ->
                    cancelRideAndReturnHome(order.id, order.requestId)
                },
                onRebookTrip = { order ->
                    passengerNavTab = 0
                    if (order.pickupTitle.isNotBlank()) {
                        selectedPickupLocation = AppLocation(
                            title = order.pickupTitle,
                            subtitle = order.pickupSubtitle,
                            latitude = if (order.pickupLat != 0.0) order.pickupLat else selectedPickupLocation.latitude,
                            longitude = if (order.pickupLon != 0.0) order.pickupLon else selectedPickupLocation.longitude
                        )
                        isPickupExplicitlySet = true
                    }
                    if (order.destinationTitle.isNotBlank()) {
                        val dest = AppLocation(
                            title = order.destinationTitle,
                            subtitle = order.destinationSubtitle,
                            latitude = if (order.destinationLat != 0.0) order.destinationLat else selectedPickupLocation.latitude + 0.02,
                            longitude = if (order.destinationLon != 0.0) order.destinationLon else selectedPickupLocation.longitude + 0.02
                        )
                        selectedDestinationLocation = dest
                        calculateAndSetRoute(selectedPickupLocation, dest)
                    }
                },
                onRequestNewRide = {
                    passengerNavTab = 0
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // inDrive Persistent Bottom Navigation Bar (Ride vs My orders)
    InDrivePassengerBottomNav(
        selectedTab = passengerNavTab,
        activeOrdersCount = passengerOrders.count {
            it.status != PassengerOrderStatus.COMPLETED && it.status != PassengerOrderStatus.CANCELLED
        },
        onTabSelected = { tab ->
            passengerNavTab = tab
        }
    )
}
        } else {
            // ================= DRIVER MODE (inDrive Captain Dashboard) =================
            if (selectedDepartureForManageId != null) {
                ManageDepartureScreen(
                    departureId = selectedDepartureForManageId!!,
                    onBack = { selectedDepartureForManageId = null },
                    onSosClick = { showSafetySheet = true },
                    modifier = modifier
                )
            } else if (showDriverPostPlannedRide) {
                val dId = driverVerification?.uid?.takeIf { it.isNotBlank() } ?: user?.uid ?: "driver_default"
                val dName = driverVerification?.name?.takeIf { it.isNotBlank() } ?: user?.displayName?.takeIf { it.isNotBlank() } ?: "Drigo Captain"
                val dPhone = driverVerification?.phone?.takeIf { it.isNotBlank() } ?: user?.phoneNumber?.takeIf { it.isNotBlank() } ?: ""
                val dVehicle = listOfNotNull(
                    driverVerification?.vehicleCompany?.takeIf { it.isNotBlank() },
                    driverVerification?.vehicleModel?.takeIf { it.isNotBlank() }
                ).joinToString(" ").ifBlank { "Car" }
                val dPlate = driverVerification?.vehicleNumber?.takeIf { it.isNotBlank() } ?: ""
                PostPlannedRideScreen(
                    driverId = dId,
                    driverName = dName,
                    driverPhone = dPhone,
                    driverRating = 5.0,
                    driverTotalTrips = 0,
                    driverVehicle = dVehicle,
                    driverPlateNumber = dPlate,
                    driverVehicleType = "AC Sedan",
                    onBackClick = {
                        showDriverPostPlannedRide = false
                        showDriverPlannedDepartures = true
                    },
                    onPublishedSuccess = { departure ->
                        showDriverPostPlannedRide = false
                        showDriverPlannedDepartures = true
                        scope.launch {
                            snackbarHostState.showSnackbar("Scheduled ride from ${departure.pickupCity} to ${departure.dropoffCity} posted successfully!")
                        }
                    },
                    onOpenSos = { showSafetySheet = true },
                    modifier = modifier
                )
            } else if (showDriverPlannedDepartures) {
                val dId = driverVerification?.uid?.takeIf { it.isNotBlank() } ?: user?.uid ?: "driver_default"
                val dName = driverVerification?.name?.takeIf { it.isNotBlank() } ?: user?.displayName?.takeIf { it.isNotBlank() } ?: "Drigo Captain"
                val dPhone = driverVerification?.phone?.takeIf { it.isNotBlank() } ?: user?.phoneNumber?.takeIf { it.isNotBlank() } ?: ""
                PlannedDeparturesScreen(
                    driverId = dId,
                    driverName = dName,
                    driverPhone = dPhone,
                    onBackClick = { showDriverPlannedDepartures = false },
                    onPostRideClick = {
                        showDriverPlannedDepartures = false
                        showDriverPostPlannedRide = true
                    },
                    onManageDeparture = { departure ->
                        selectedDepartureForManageId = departure.id
                    },
                    onOpenSos = { showSafetySheet = true },
                    modifier = modifier
                )
            } else {
                DriverModeView(
                    user = user,
                    driverVerification = driverVerification,
                    isDriverOnline = isDriverOnline,
                    onToggleOnline = onToggleDriverOnline,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToWallet = onNavigateToWallet,
                    onSwitchToPassenger = { onSwitchUserMode(UserMode.PASSENGER) },
                    onOpenChat = { tripId, partnerName, role, phone, pickup, dest ->
                        chatTripId = tripId
                        chatPartnerName = partnerName
                        chatPartnerRole = role
                        chatPartnerPhone = phone
                        chatPickupTitle = pickup
                        chatDestinationTitle = dest
                        showChatSheet = true
                    },
                    initialUserLocation = userLocationData,
                    liveRideRequests = liveRideRequests,
                    onRefreshRideRequests = onRefreshDriverRideRequests,
                    modifier = modifier
                )
            }
        }
    }

    // Notification Center Bottom Sheet
    if (showNotificationCenterSheet) {
        NotificationCenterSheet(
            notifications = notificationHistory,
            onDismiss = { showNotificationCenterSheet = false },
            onClearAll = { notifManager.clearNotificationHistory() },
            onOpenSettings = {
                showNotificationCenterSheet = false
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            }
        )
    }

    // Notification Settings Bottom Sheet
    if (showNotificationSettingsSheet) {
        NotificationSettingsSheet(
            userMode = userMode,
            onDismiss = { showNotificationSettingsSheet = false }
        )
    }

    // Passenger Full-Screen City to City Departures Screen Overlay
    if (showCityDeparturesScreen) {
        CityToCityPassengerDeparturesContent(
            onBackClick = { showCityDeparturesScreen = false },
            onSosClick = { showSafetySheet = true },
            onNavigateToOrders = {
                showCityDeparturesScreen = false
                passengerNavTab = 1
            },
            onViewActiveRide = { departure, booking ->
                activeIntercityDepartureForPassenger = departure
                activeIntercityBookingForPassenger = booking
            },
            initialFromCity = "All Routes",
            initialToCity = "All Routes",
            modifier = Modifier.fillMaxSize()
        )
    }

    // Passenger Intercity Active Ride Screen Overlay (Attachment 1 Target Experience)
    if (activeIntercityDepartureForPassenger != null) {
        val departure = activeIntercityDepartureForPassenger!!
        PassengerIntercityActiveRideScreen(
            departure = departure,
            booking = activeIntercityBookingForPassenger,
            onBack = {
                activeIntercityDepartureForPassenger = null
                activeIntercityBookingForPassenger = null
            },
            onSosClick = { showSafetySheet = true },
            onCallDriver = { driverPhone ->
                val phone = driverPhone.ifBlank { departure.driverPhone.ifBlank { "+923001234567" } }
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
            },
            onOpenChat = { driverName, driverPhone ->
                chatTripId = departure.id
                chatPartnerName = driverName.ifBlank { departure.driverName.ifBlank { "Captain" } }
                chatPartnerRole = "Driver"
                chatPartnerPhone = driverPhone.ifBlank { departure.driverPhone }
                chatPickupTitle = "${departure.pickupCity} (${departure.pickupHub})"
                chatDestinationTitle = "${departure.dropoffCity} (${departure.dropoffHub})"
                showChatSheet = true
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // "Where From?" & "Where To?" Bottom Card
    if (showPickupDestinationCard) {
        PickupDestinationBottomCard(
            pickupLocation = selectedPickupLocation,
            destinationLocation = selectedDestinationLocation,
            initialEditingPickup = cardInitialEditPickup,
            initialWhereToText = if (cardInitialEditPickup) "" else (selectedDestinationLocation?.title ?: ""),
            isLocked = isRideActive,
            onDismiss = { showPickupDestinationCard = false },
            onDestinationSelected = { destination ->
                if (!isRideActive) {
                    selectedDestinationLocation = destination
                    showPickupDestinationCard = false
                    calculateAndSetRoute(selectedPickupLocation, destination)
                } else {
                    Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
                }
            },
            onPickupSelected = { newPickup ->
                if (!isRideActive) {
                    selectedPickupLocation = newPickup
                    isPickupExplicitlySet = true
                    showPickupDestinationCard = false
                    if (selectedDestinationLocation != null) {
                        calculateAndSetRoute(newPickup, selectedDestinationLocation!!)
                    }
                } else {
                    Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
                }
            },
            onPickOnMap = { isPickup ->
                if (!isRideActive) {
                    showPickupDestinationCard = false
                    mapSelectionMode = if (isPickup) MapSelectionMode.PICKUP else MapSelectionMode.DESTINATION
                } else {
                    Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Booking Confirmation Dialog
    if (showBookingDialog && selectedRideCategory != null) {
        val dist = activeRoute?.distanceKm ?: 5.0
        val baseFare = when (selectedRideCategory) {
            "Ride A/C", "Private AC" -> 150 + (dist * 50).toInt()
            "Ride", "Private Non-AC" -> 110 + (dist * 35).toInt()
            "City to City", "City to city" -> 450 + (dist * 45).toInt()
            "Couriers", "Parcel", "Parcel Delivery" -> 70 + (dist * 20).toInt()
            "Freight" -> 300 + (dist * 60).toInt()
            "Share Ride" -> 80 + (dist * 25).toInt()
            "Book Car", "Book a Car" -> 150 + (dist * 50).toInt()
            else -> 150 + (dist * 50).toInt()
        }

        AlertDialog(
            onDismissRequest = { showBookingDialog = false },
            title = {
                Text(
                    text = "${selectedRideCategory ?: "Ride"} Booking",
                    fontWeight = FontWeight.Bold,
                    color = DrigoBrandPurple
                )
            },
            text = {
                Column {
                    Text(
                        text = "Pickup Location:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = selectedPickupLocation.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Destination:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = selectedDestinationLocation?.title ?: "Not specified yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (activeRoute != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Distance: ${activeRoute?.distanceKm} km",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Est. Time: ~${activeRoute?.durationMinutes} mins",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DrigoBrandPurple.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimated Fare:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DrigoBrandPurple
                            )
                            Text(
                                text = "Rs. $baseFare",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = DrigoBrandPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tolls will be paid separately to the driver.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrigoBrandPurple,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitRideRequest() },
                    enabled = !isBookingInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                    modifier = Modifier.testTag("confirm_booking_btn")
                ) {
                    if (isBookingInProgress) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Confirm Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookingDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Real-Time Ride Chat Sheet Overlay (Driver <-> Passenger)
    if (showChatSheet) {
        RideChatSheet(
            tripId = chatTripId.ifBlank { activeRideRequestId ?: "trip_default" },
            currentUserId = user?.uid ?: if (userMode == UserMode.DRIVER) "driver_user" else "passenger_user",
            currentUserName = user?.displayName?.ifBlank { if (userMode == UserMode.DRIVER) "Captain Farhan" else "Naeem Ullah" } ?: (user?.email?.substringBefore("@") ?: if (userMode == UserMode.DRIVER) "Captain Farhan" else "Naeem Ullah"),
            isDriver = (userMode == UserMode.DRIVER),
            partnerName = chatPartnerName,
            partnerRole = chatPartnerRole,
            partnerPhone = chatPartnerPhone,
            pickupTitle = chatPickupTitle.ifBlank { selectedPickupLocation.title },
            destinationTitle = chatDestinationTitle.ifBlank { selectedDestinationLocation?.title ?: "Destination" },
            onDismiss = { showChatSheet = false }
        )
    }

    // Universal Safety & SOS Modal Sheet Overlay
    if (showSafetySheet) {
        val activeOrder = activePassengerOrder
        UniversalSafetyModalSheet(
            rideId = activeOrder?.id ?: (activeRideRequestId ?: "trip_${System.currentTimeMillis()}"),
            userRole = if (userMode == UserMode.DRIVER) "DRIVER" else "PASSENGER",
            partnerName = if (userMode == UserMode.DRIVER) "Passenger" else (activeOrder?.driverName?.ifBlank { "Captain" } ?: "Captain"),
            partnerPhone = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverPhone ?: ""),
            vehicleMake = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverVehicleMake ?: ""),
            vehicleModel = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverVehicleModel ?: ""),
            vehiclePlate = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverPlateNumber ?: ""),
            pickupAddress = activeOrder?.pickupTitle?.ifBlank { selectedPickupLocation.title } ?: selectedPickupLocation.title,
            destinationAddress = activeOrder?.destinationTitle?.ifBlank { selectedDestinationLocation?.title ?: "Not Set" } ?: (selectedDestinationLocation?.title ?: "Not Set"),
            ridePin = activeOrder?.verificationPin ?: "",
            onDismiss = { showSafetySheet = false },
            onOpenReport = {
                showSafetySheet = false
                showSafetyReportModal = true
            }
        )
    }

    // Safety Report Incident Dialog
    if (showSafetyReportModal) {
        val activeOrder = activePassengerOrder
        SafetyReportDialog(
            rideId = activeOrder?.id ?: (activeRideRequestId ?: "trip_${System.currentTimeMillis()}"),
            reporterId = user?.uid ?: if (userMode == UserMode.DRIVER) "driver_user" else "passenger_user",
            reporterName = user?.displayName ?: "User",
            reporterPhone = user?.phoneNumber ?: "",
            isReporterDriver = (userMode == UserMode.DRIVER),
            reportedUserId = if (userMode == UserMode.DRIVER) "passenger_target" else (activeOrder?.driverPhone ?: "driver_target"),
            reportedUserName = if (userMode == UserMode.DRIVER) "Passenger" else (activeOrder?.driverName ?: "Driver Captain"),
            driverPlateNumber = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverPlateNumber ?: ""),
            pickupTitle = activeOrder?.pickupTitle ?: selectedPickupLocation.title,
            destinationTitle = activeOrder?.destinationTitle ?: (selectedDestinationLocation?.title ?: ""),
            onDismiss = { showSafetyReportModal = false },
            onReportSubmitted = {
                showSafetyReportModal = false
                scope.launch {
                    snackbarHostState.showSnackbar("Safety report submitted securely. Our safety team is on it.")
                }
            }
        )
    }

    // Passenger Post-Ride Rating & Feedback Dialog (Passenger Mode only)
    if (userMode == UserMode.PASSENGER && showPassengerRatingDialog && completedOrderForRating != null) {
        val order = completedOrderForRating!!
        val safeId = order.id.ifBlank { order.requestId }
        PostRideRatingDialog(
            rideId = safeId,
            currentUserId = user?.uid ?: "passenger_user",
            currentUserName = user?.displayName ?: "Passenger",
            isDriver = false,
            targetId = order.driverId.ifBlank { "driver_captain" },
            targetName = order.driverName.ifBlank { "Driver Captain" },
            targetPhone = order.driverPhone,
            targetVehicleSummary = "${order.driverVehicleMake} ${order.driverVehicleModel}".trim(),
            targetPlateNumber = order.driverPlateNumber,
            targetRating = if (order.driverRating > 0) order.driverRating else 5.0,
            pickupTitle = order.pickupTitle,
            destinationTitle = order.destinationTitle,
            farePkr = order.agreedFare,
            onDismiss = {
                showPassengerRatingDialog = false
                markOrderRatedOrSkipped(safeId, order.requestId)
                completedOrderForRating = null
            },
            onRatingSubmitted = { ratingEntity ->
                showPassengerRatingDialog = false
                markOrderRatedOrSkipped(safeId, order.requestId)
                completedOrderForRating = null
                scope.launch {
                    snackbarHostState.showSnackbar("Thank you! Feedback submitted for ${order.driverName.ifBlank { "Driver Captain" }}.")
                }
            },
            onOpenSafetyReport = {
                showPassengerRatingDialog = false
                markOrderRatedOrSkipped(safeId, order.requestId)
                showSafetyReportModal = true
            }
        )
    }

    // Incoming Driver Offer Dialog / Modal
    val currentOffer = incomingDriverOffer
    if (currentOffer != null && activePassengerOrder == null) {
        DriverOfferDialog(
            offer = currentOffer,
            customOfferedFare = customOfferedFare,
            onAccept = {
                val acceptedOrder = currentOffer.copy(
                    status = PassengerOrderStatus.DRIVER_COMING,
                    assignedDriverId = currentOffer.assignedDriverId.ifBlank { currentOffer.driverPhone.ifBlank { currentOffer.driverName } }
                )
                // 1. Immediately dismiss offer dialog on click
                incomingDriverOffer = null

                // 2. Immediately initialize driver car position on map
                val dLat = if (currentOffer.pickupLat != 0.0) currentOffer.pickupLat + 0.009 else (selectedPickupLocation.latitude + 0.009)
                val dLon = if (currentOffer.pickupLon != 0.0) currentOffer.pickupLon + 0.009 else (selectedPickupLocation.longitude + 0.009)
                val initialDriverLoc = LiveDriverLocation(
                    rideId = acceptedOrder.requestId.ifBlank { acceptedOrder.id },
                    driverId = "driver_${acceptedOrder.id}",
                    latitude = dLat,
                    longitude = dLon,
                    bearing = 45f,
                    speedKmh = 35f,
                    etaMinutes = acceptedOrder.etaMinutes,
                    distanceRemainingKm = 1.2,
                    status = PassengerOrderStatus.DRIVER_COMING.name,
                    updatedAt = System.currentTimeMillis()
                )
                passengerMapDriverLoc = initialDriverLoc

                // 3. Immediately set passenger active order
                passengerOrders = listOf(acceptedOrder) + passengerOrders.filter { it.id != acceptedOrder.id && it.requestId != acceptedOrder.requestId }

                // 4. Update backend in background
                val repo = FirebaseRepository.getInstance(context)
                scope.launch {
                    repo.savePassengerOrder(acceptedOrder)
                    repo.updateRideRequestStatus(acceptedOrder.requestId, "DRIVER_COMING")
                    repo.updateLiveDriverLocation(initialDriverLoc)
                    snackbarHostState.showSnackbar(
                        message = "Captain ${currentOffer.driverName} is on the way! (~${acceptedOrder.etaMinutes} min away)",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDecline = {
                // 1. Immediately remove offer card
                incomingDriverOffer = null
                // 2. Show toast
                scope.launch {
                    snackbarHostState.showSnackbar("Offer Declined")
                }
            }
        )
    }

    // City to City Passenger Scheduled Departures Modal Sheet
    if (showPassengerScheduledDeparturesSheet) {
        PassengerScheduledDeparturesSheet(
            onDismiss = { showPassengerScheduledDeparturesSheet = false },
            initialFromCity = "All Routes",
            initialToCity = "All Routes"
        )
    }
}

/**
 * Dialog displaying incoming Driver Offer with Accept and Decline actions.
 * Matches inDrive offer acceptance workflow.
 */
@Composable
fun DriverOfferDialog(
    offer: PassengerOrder,
    customOfferedFare: Int? = null,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isFastest = offer.etaMinutes <= 3
    val isBestRated = offer.driverRating >= 4.9
    val isLowestFare = offer.agreedFare <= (customOfferedFare ?: offer.agreedFare)

    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00E676).copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Captain Offer",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = InDriveLimeGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, InDriveLimeGreen.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "~${offer.etaMinutes} min away",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Smart Badges Row
                if (isFastest || isBestRated || isLowestFare) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isFastest) SmartBadge(SmartBadgeType.FASTEST_ARRIVAL)
                        if (isBestRated) SmartBadge(SmartBadgeType.BEST_RATED)
                        if (isLowestFare) SmartBadge(SmartBadgeType.LOWEST_FARE)
                    }
                }

                // Driver card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DrigoBrandPurple.copy(alpha = 0.3f),
                            border = BorderStroke(1.5.dp, InDriveLimeGreen),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = offer.driverName.ifBlank { "Captain" },
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFB300).copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${offer.driverRating}",
                                            color = Color(0xFFFFB300),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${offer.driverVehicleColor} ${offer.driverVehicleMake} ${offer.driverVehicleModel}".trim().ifBlank { "Sedan Comfort" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            if (offer.driverPlateNumber.isNotBlank()) {
                                Text(
                                    text = "Plate: ${offer.driverPlateNumber}",
                                    color = InDriveLimeGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Fare highlight
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = InDriveLimeGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.2.dp, InDriveLimeGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Offered Fare",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Fixed Cash / Wallet",
                                color = Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "PKR ${"%,d".format(offer.agreedFare)}",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp
                        )
                    }
                }

                Text(
                    text = "Accepting confirms this captain and starts live map navigation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InDriveLimeGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("accept_driver_offer_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Accept Captain's Offer",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("decline_driver_offer_btn")
            ) {
                Text(
                    text = "Decline Offer",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    )
}

/**
 * Modern Active Captain Assigned Card for the passenger bottom sheet.
 * Displays live ETA, distance, captain info, vehicle plate, fare, and quick actions (Call, Chat, SOS, Cancel, Share).
 * Matches trip progress screen specifications and reference layout.
 */
@Composable
fun ActiveCaptainAssignedCard(
    order: PassengerOrder,
    liveLocation: LiveDriverLocation?,
    onCallCaptain: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSafety: () -> Unit,
    onCancelRide: () -> Unit,
    onShareTrip: () -> Unit = {},
    onDetailsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val etaMins = liveLocation?.etaMinutes ?: order.etaMinutes
    val distKm = liveLocation?.distanceRemainingKm ?: order.distanceKm
    val isArrived = order.status == PassengerOrderStatus.DRIVER_ARRIVED || (liveLocation?.status == PassengerOrderStatus.DRIVER_ARRIVED.name)
    val isInTrip = order.status == PassengerOrderStatus.IN_TRIP || (liveLocation?.status == PassengerOrderStatus.IN_TRIP.name)
    val isDark = MaterialTheme.drigoColors.isDark

    var isSharingLocationWithDriver by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Header Title & ETA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusColor = when {
                isInTrip -> if (isDark) Color(0xFF29B6F6) else Color(0xFF0288D1)
                isArrived -> if (isDark) Color(0xFF00E676) else Color(0xFF2E7D32)
                else -> if (isDark) Color(0xFFFFB300) else Color(0xFFE65100)
            }
            Text(
                text = when {
                    isInTrip -> "Trip in Progress!"
                    isArrived -> "Driver Has Arrived!"
                    else -> "Driver on the way!"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isArrived) "At pickup location" else "Arriving in ~$etaMins min",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor.copy(alpha = if (isDark) 0.15f else 0.10f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = if (isDark) 0.8f else 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pulseAlpha = rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer { alpha = pulseAlpha.value }
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isInTrip -> "ACTIVE • IN TRIP"
                            isArrived -> "CAPTAIN ARRIVED • WAITING"
                            else -> "EN ROUTE • COMING TO YOU"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Waiting timer when driver arrived at pickup
        if (isArrived && !isInTrip) {
            var secondsWaiting by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000L)
                    secondsWaiting++
                }
            }
            val freeWaitSeconds = 300 // 5 mins free wait
            val remainingWaitSeconds = (freeWaitSeconds - secondsWaiting).coerceAtLeast(0)
            val waitMins = remainingWaitSeconds / 60
            val waitSecs = remainingWaitSeconds % 60
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF00E676) else Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Waiting timer",
                        tint = if (isDark) Color(0xFF00E676) else Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (remainingWaitSeconds > 0) {
                            "Captain is waiting • Free wait time: ${String.format(Locale.US, "%02d:%02d", waitMins, waitSecs)}"
                        } else {
                            val paidWaitMins = (secondsWaiting - freeWaitSeconds) / 60
                            "Free wait time ended • Paid wait: +$paidWaitMins min"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingWaitSeconds > 0) (if (isDark) Color(0xFF00E676) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100))
                    )
                }
            }
        }

        // 1.5 Prominent 4-Digit Ride Verification PIN (Safety Architecture - Item 4)
        val ridePin = order.verificationPin
        val pinAccentColor = if (isDark) InDriveLimeGreen else MaterialTheme.colorScheme.primary
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, pinAccentColor.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("passenger_4digit_pin_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = pinAccentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "YOUR 4-DIGIT RIDE PIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pinAccentColor,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Share this 4-digit PIN with Captain ${order.driverName.ifBlank { "" }} before getting into the car",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // 4 Digit Boxes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ridePin.forEach { digit ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(width = 48.dp, height = 50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = digit.toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Driver & Vehicle Profile Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    // Profile Photo / Avatar
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(2.dp, if (isDark) InDriveLimeGreen else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Driver Avatar",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.driverName.ifBlank { "Captain" },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${order.driverVehicleColor} ${order.driverVehicleMake} ${order.driverVehicleModel}".trim().ifBlank { "Standard Vehicle" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${if (order.driverRating > 0) order.driverRating else 5.0}${if (order.driverTotalRides > 0) " (${order.driverTotalRides} reviews)" else ""}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Right Car Icon
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Car",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Plate Box & Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Car Number",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = order.driverPlateNumber.ifBlank { "N/A" },
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Details",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onDetailsClick() }
                            .padding(8.dp)
                    )
                }

                // Action Buttons: Call & Message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCallCaptain,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) InDriveLimeGreen else Color(0xFF00A859)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("call_captain_btn")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onOpenChat,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) DrigoBrandPurple else DrigoBrandPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("chat_captain_btn")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "Message", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Message", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // 3. Locations Section (Pickup & Destination)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pickup Item
                val pickLat = if (order.pickupLat != 0.0) order.pickupLat else 34.0151
                val pickLon = if (order.pickupLon != 0.0) order.pickupLon else 71.5249
                val destLat = if (order.destinationLat != 0.0) order.destinationLat else 34.0351
                val destLon = if (order.destinationLon != 0.0) order.destinationLon else 71.5449

                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00C853),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.pickupTitle.ifBlank { "Pickup Location" },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        if (order.pickupSubtitle.isNotBlank()) {
                            Text(
                                text = order.pickupSubtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "📍 Lat: %.5f, Lon: %.5f", pickLat, pickLon),
                            color = if (isDark) Color(0xFF81D4FA) else MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 22.dp)
                )

                // Destination Item
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE53935),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.destinationTitle.ifBlank { "Destination" },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        if (order.destinationSubtitle.isNotBlank()) {
                            Text(
                                text = order.destinationSubtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "📍 Lat: %.5f, Lon: %.5f", destLat, destLon),
                            color = if (isDark) Color(0xFFFF8A80) else MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.1f", distKm)}km",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Payment Method
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment method",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cash : Rs. ${"%,d".format(order.agreedFare)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = "Cash",
                    tint = if (isDark) Color(0xFF00E676) else Color(0xFF00A859),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 5. Share Location Switch Row
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share my location with driver",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Switch(
                    checked = isSharingLocationWithDriver,
                    onCheckedChange = { isSharingLocationWithDriver = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (isDark) InDriveLimeGreen else MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        // 6. Bottom Buttons Row: Cancel Ride & Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (order.status != PassengerOrderStatus.COMPLETED && order.status != PassengerOrderStatus.IN_TRIP) {
                Button(
                    onClick = onCancelRide,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cancel_captain_ride_btn")
                ) {
                    Text("Cancel Ride", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Button(
                onClick = onShareTrip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

/**
 * Animated Searching Card displayed when ride request is sent to Firebase.
 */
@Composable
fun SearchingForDriversCard(
    pickupTitle: String,
    pickupLat: Double = 0.0,
    pickupLon: Double = 0.0,
    destinationTitle: String,
    destinationLat: Double = 0.0,
    destinationLon: Double = 0.0,
    rideCategory: String,
    offeredFare: Int,
    incomingOffers: List<DriverOffer> = emptyList(),
    onAcceptOffer: (DriverOffer) -> Unit = {},
    onDeclineOffer: (DriverOffer) -> Unit = {},
    onCounterOffer: (DriverOffer, Int) -> Unit = { _, _ -> },
    onBoostFare: (Int) -> Unit = {},
    onCancelSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Calculate smart badge metrics across all available bids
    val minEta = incomingOffers.minOfOrNull { it.etaMinutes } ?: 0
    val maxRating = incomingOffers.maxOfOrNull { it.driverRating } ?: 5.0
    val minFare = incomingOffers.minOfOrNull { it.offeredFare } ?: offeredFare

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (incomingOffers.isEmpty()) {
            // Radar / Searching Spinner
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(76.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = InDriveLimeGreen.copy(alpha = 0.15f * alpha),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Surface(
                    shape = CircleShape,
                    color = InDriveLimeGreen.copy(alpha = 0.3f),
                    modifier = Modifier.size(54.dp)
                ) {}
                CircularProgressIndicator(
                    color = InDriveLimeGreen,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp)
                )
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Searching for drivers...",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Broadcasting to nearby active captains",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Header for incoming driver bids
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = InDriveLimeGreen,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Driver Bids Received (${incomingOffers.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = InDriveLimeGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "LIVE",
                        color = InDriveLimeGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Driver Bids List with Smart Badges
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                incomingOffers.forEach { offer ->
                    val isFastest = offer.etaMinutes <= 3 || (incomingOffers.size > 1 && offer.etaMinutes == minEta)
                    val isBestRated = offer.driverRating >= 4.9 || (incomingOffers.size > 1 && offer.driverRating == maxRating)
                    val isLowestFare = offer.offeredFare <= offeredFare || (incomingOffers.size > 1 && offer.offeredFare == minFare)

                    SmartDriverBidCard(
                        offer = offer,
                        passengerRequestedFare = offeredFare,
                        isFastest = isFastest,
                        isBestRated = isBestRated,
                        isLowestFare = isLowestFare,
                        onAccept = { onAcceptOffer(offer) },
                        onDecline = { onDeclineOffer(offer) },
                        onCounterOffer = { counterFare -> onCounterOffer(offer, counterFare) }
                    )
                }
            }
        }

        // Summary box of current request & route
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pLat = if (pickupLat != 0.0) pickupLat else 34.0151
                val pLon = if (pickupLon != 0.0) pickupLon else 71.5249
                val dLat = if (destinationLat != 0.0) destinationLat else 34.0351
                val dLon = if (destinationLon != 0.0) destinationLon else 71.5449

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFF00E676), modifier = Modifier.size(8.dp)) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = pickupTitle.ifBlank { "Pickup Location" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(Locale.US, "📍 Lat: %.5f, Lon: %.5f", pLat, pLon),
                            color = Color(0xFF81D4FA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFFFF2A2A), modifier = Modifier.size(8.dp)) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = destinationTitle.ifBlank { "Destination" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(Locale.US, "📍 Lat: %.5f, Lon: %.5f", dLat, dLon),
                            color = Color(0xFFFF8A80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = rideCategory, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = "Your Offer: PKR ${"%,d".format(offeredFare)}",
                        fontWeight = FontWeight.Bold,
                        color = InDriveLimeGreen,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Tactile Fare Adjustment Quick Chips to boost fare during search
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "⚡ Boost your offer for faster driver response:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            TactileFareAdjustmentChips(
                currentFare = offeredFare,
                baseFare = offeredFare,
                onSetFare = { newFare -> onBoostFare(newFare) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cancel Request Button
        Button(
            onClick = onCancelSearch,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text("Cancel Request", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
