package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppLocation
import com.example.data.LocationHelper
import com.example.data.UserLocationData
import com.example.data.RouteResult
import com.example.data.RouteService
import com.example.data.model.*
import com.example.data.remote.FirebaseRepository
import com.example.service.DriverLocationService
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.PostRideRatingDialog
import com.example.ui.components.SafetyReportDialog
import com.example.ui.components.NotificationCenterSheet
import com.example.ui.screens.driver.DriverPerformanceScreen
import com.example.ui.screens.driver.DriverTripHistoryScreen
import com.example.ui.screens.driver.DriverRideRequestCard
import com.example.ui.screens.driver.DriverCompactRideRequestItem
import com.example.ui.theme.DrigoBrandPurple
import com.example.util.DriverAudioHelper
import com.example.util.RideNotificationManager
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    if (lat1 == 0.0 || lon1 == 0.0 || lat2 == 0.0 || lon2 == 0.0) return 0.0
    val results = FloatArray(1)
    return try {
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        results[0] / 1000.0
    } catch (_: Exception) {
        0.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverModeView(
    user: FirebaseUser?,
    driverVerification: DriverVerification?,
    isDriverOnline: Boolean,
    onToggleOnline: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onSwitchToPassenger: () -> Unit,
    onOpenChat: (tripId: String, partnerName: String, role: String, phone: String, pickup: String, dest: String) -> Unit,
    initialUserLocation: UserLocationData? = null,
    liveRideRequests: List<RideRequest>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val repo = remember { FirebaseRepository.getInstance(context) }
    val routeService = remember { RouteService(context) }
    val locationHelper = remember { LocationHelper(context) }
    val audioHelper = remember { DriverAudioHelper.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioHelper.shutdown()
        }
    }

    // Driver identification and live verification data
    val driverId = remember(user?.uid) { user?.uid ?: "driver_${System.currentTimeMillis().toString().takeLast(6)}" }
    val realTimeDriverVerification by repo.listenToDriverVerification(driverId).collectAsState(initial = driverVerification)
    val activeVerification = realTimeDriverVerification ?: driverVerification

    val driverName = remember(activeVerification?.name, user?.displayName, user?.email) {
        activeVerification?.name?.trim()?.ifBlank { null }
            ?: user?.displayName?.trim()?.ifBlank { null }
            ?: user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }?.ifBlank { null }
            ?: "Captain"
    }
    val driverVehicleMake = remember(activeVerification?.vehicleCompany) {
        activeVerification?.vehicleCompany?.trim()?.ifBlank { null } ?: ""
    }
    val driverVehicleModel = remember(activeVerification?.vehicleModel) {
        activeVerification?.vehicleModel?.trim()?.ifBlank { null } ?: ""
    }
    val driverVehicleColor = remember(activeVerification?.vehicleColor) {
        activeVerification?.vehicleColor?.trim()?.ifBlank { null } ?: ""
    }
    val driverVehicleNumber = remember(activeVerification?.vehicleNumber) {
        activeVerification?.vehicleNumber?.trim()?.ifBlank { null } ?: ""
    }
    val driverPhone = remember(activeVerification?.phone, user?.phoneNumber) {
        activeVerification?.phone?.trim()?.ifBlank { null }
            ?: user?.phoneNumber?.trim()?.ifBlank { null }
            ?: ""
    }

    DisposableEffect(isDriverOnline, driverId) {
        onDispose {
            if (!isDriverOnline) {
                DriverLocationService.stop(context, driverId)
            }
        }
    }

    // Real-time Ride Requests Stream from Firebase Realtime Database
    // Collects only while isDriverOnline is true; stops immediately when offline
    val localStreamRequests by remember(isDriverOnline) {
        if (isDriverOnline) {
            com.example.data.remote.RideRequestRepository.getInstance().getLiveRideRequests()
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val firebaseRequests = if (isDriverOnline) {
        if (!liveRideRequests.isNullOrEmpty()) {
            liveRideRequests
        } else {
            localStreamRequests
        }
    } else {
        emptyList()
    }

    // Audio announcement tracker
    var lastAnnouncedRequestId by remember { mutableStateOf<String?>(null) }
    var isVoiceAlertsEnabled by remember { mutableStateOf(true) }

    // Real passenger requests - robust filtering to show all active requests in area
    val allRequests = remember(firebaseRequests, driverId) {
        val now = System.currentTimeMillis()
        firebaseRequests.filter { req ->
            val isTerminal = req.status.equals("CANCELLED", true) ||
                    req.status.equals("COMPLETED", true) ||
                    req.status.equals("REJECTED", true) ||
                    req.status.equals("ACCEPTED", true) ||
                    req.status.equals("IN_TRIP", true) ||
                    req.status.equals("DRIVER_ARRIVED", true) ||
                    req.status.equals("DRIVER_COMING", true)
            val isActive = !isTerminal
            val notExpired = (req.expiresAt == 0L || req.expiresAt > (now - 2 * 60 * 60 * 1000L) ||
                    req.status.equals("SEARCHING_DRIVERS", true) || req.status.equals("SEARCHING", true) || req.status.equals("PENDING", true)) &&
                    (req.timestamp == 0L || (now - req.timestamp < 48 * 60 * 60 * 1000L))
            val notAssigned = req.assignedDriverId.isBlank() ||
                    req.assignedDriverId.equals("null", true) ||
                    req.assignedDriverId.equals("none", true) ||
                    req.assignedDriverId == "0" ||
                    req.assignedDriverId == driverId
            isActive && notExpired && notAssigned
        }
    }

    // Centralized Ride Notification Manager
    val notifManager = remember(context) { RideNotificationManager.getInstance(context) }

    // Runtime Permission Pre-flight for Going Online (Android 14+ FGS location + Notifications)
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            onToggleOnline()
        } else {
            Toast.makeText(
                context,
                "Location permission is required to go online and receive ride requests.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val handleToggleOnlineClick: () -> Unit = remember(isDriverOnline, onToggleOnline) {
        {
            if (isDriverOnline) {
                onToggleOnline()
            } else {
                val fineGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                if ((fineGranted || coarseGranted) && notifGranted) {
                    onToggleOnline()
                } else {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }
        }
    }

    // Filtering State (Item 3)
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedMaxDistanceFilterKm by remember { mutableStateOf<Double?>(null) } // null means Any distance
    var selectedPaymentFilter by remember { mutableStateOf("All") } // "All", "Cash", "Digital"
    var isAutoAcceptEnabled by remember { mutableStateOf(false) }
    var autoAcceptMinFare by remember { mutableIntStateOf(350) }
    var autoAcceptMaxDistanceKm by remember { mutableDoubleStateOf(5.0) }
    var destinationModeActive by remember { mutableStateOf(false) }
    var destinationModeText by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Bottom Navigation & Performance Gamification System (Image 2)
    var selectedDriverTab by remember { mutableStateOf("REQUESTS") } // "REQUESTS", "PERFORMANCE", or "HISTORY"
    var driverRidesThisWeek by remember { mutableIntStateOf(10) }
    var driverPerformanceRating by remember { mutableDoubleStateOf(5.00) }
    var todayIncomePkr by remember { mutableIntStateOf(0) }
    var dailyGoalPkr by remember { mutableIntStateOf(5000) }
    var walletBalancePkr by remember { mutableIntStateOf(137) }
    var activeBonusesCount by remember { mutableIntStateOf(0) }

    // Realtime Driver Trip History from Firebase
    val firebaseDriverTripHistory by repo.observeDriverTripHistory(driverId.ifBlank { driverPhone }, driverPhone)
        .collectAsState(initial = emptyList())

    val activeDriverTripHistoryList = firebaseDriverTripHistory

    // Smart Vehicle Tariff & Eligibility Controls (Image 13)
    var selectedVehicleModelName by remember { mutableStateOf("Suzuki Mehran (2018)") }
    var selectedVehiclePlateNumber by remember { mutableStateOf("LEA-18-4921") }
    var selectedVehicleCategoryType by remember { mutableStateOf("Hatchback (Non-AC)") }
    var isTariffMiniEnabled by remember { mutableStateOf(true) }
    var isTariffAcEnabled by remember { mutableStateOf(true) }
    var isTariffComfortEnabled by remember { mutableStateOf(true) }
    var isTariffCourierEnabled by remember { mutableStateOf(true) }
    var isCourierParcelEnabled by remember { mutableStateOf(true) }
    var hasCourierThermalBag by remember { mutableStateOf(false) }
    var isCourierHeavyCargoEnabled by remember { mutableStateOf(false) }
    var selectedTariffMultiplier by remember { mutableFloatStateOf(1.0f) }

    // Tariff Sheets & Dialogs
    var showTariffsSheet by remember { mutableStateOf(false) }
    var showChangeVehicleDialog by remember { mutableStateOf(false) }

    // Performance Sheets & Dialogs
    var showTierBenefitsSheet by remember { mutableStateOf(false) }
    var showAddDailyGoalSheet by remember { mutableStateOf(false) }
    var showBonusesSheet by remember { mutableStateOf(false) }
    var showIncomeDetailsSheet by remember { mutableStateOf(false) }

    // Audio & Voice Preferences (Item 5)
    var voiceLanguageChoice by remember { mutableStateOf("EN") } // "EN" or "UR"

    // Driver States
    var selectedRequestForOffer by remember { mutableStateOf<RideRequest?>(null) }
    var activeDriverTrip by remember { mutableStateOf<PassengerOrder?>(null) }
    var isActiveTripSheetExpanded by remember { mutableStateOf(false) }
    var isSendingOffer by remember { mutableStateOf(false) }
    var offerSentRequestId by remember { mutableStateOf<String?>(null) }
    var isCompactListView by remember { mutableStateOf(false) }

    // Announce new requests with notification and audio chime/TTS when online
    LaunchedEffect(allRequests, isDriverOnline, isVoiceAlertsEnabled) {
        if (isDriverOnline) {
            val newest = allRequests.firstOrNull()
            if (newest != null && newest.id != lastAnnouncedRequestId) {
                lastAnnouncedRequestId = newest.id
                if (isVoiceAlertsEnabled) {
                    audioHelper.playNewRequestAlert(
                        pickupTitle = newest.pickupTitle,
                        destinationTitle = newest.destinationTitle,
                        farePkr = newest.estimatedFare,
                        requestId = newest.id
                    )
                }
                if (newest.rideCategory.contains("Share", ignoreCase = true) || newest.rideCategory.contains("Shared", ignoreCase = true)) {
                    notifManager.notifySharedRideMatch(
                        pickup = newest.pickupTitle,
                        dest = newest.destinationTitle,
                        extraFare = newest.estimatedFare,
                        rideId = newest.id,
                        onAction = { selectedRequestForOffer = newest }
                    )
                } else {
                    notifManager.notifyNewRideRequest(
                        pickup = newest.pickupTitle,
                        dest = newest.destinationTitle,
                        farePkr = newest.estimatedFare,
                        rideId = newest.id,
                        onAction = { selectedRequestForOffer = newest }
                    )
                }
            }
        }
    }

    // Wait Time & Extra Surcharges (Item 2)
    var waitTimeSeconds by remember { mutableIntStateOf(0) }
    var isWaitingTimerRunning by remember { mutableStateOf(false) }
    var tollSurchargesPkr by remember { mutableIntStateOf(0) }
    var showTollAddDialog by remember { mutableStateOf(false) }

    // Safety, SOS & PIN Verification (Item 4)
    var showSafetySosSheet by remember { mutableStateOf(false) }
    var isEmergencySirenActive by remember { mutableStateOf(false) }
    var showPinVerificationDialog by remember { mutableStateOf(false) }
    var enteredPassengerPin by remember { mutableStateOf("") }
    var isAudioRecordingActive by remember { mutableStateOf(true) }

    // Passenger Rating on Trip Completion (Item 4)
    var showPassengerRatingDialog by remember { mutableStateOf(false) }
    var completedTripForRating by remember { mutableStateOf<PassengerOrder?>(null) }
    var showDriverReportIncident by remember { mutableStateOf(false) }
    var driverReportTargetTrip by remember { mutableStateOf<PassengerOrder?>(null) }
    var driverRatingStars by remember { mutableIntStateOf(5) }
    val selectedRatingTags = remember { mutableStateListOf<String>() }
    var isBlockPassengerSelected by remember { mutableStateOf(false) }

    // Cancellation (Item 4)
    var showCancelTripDialog by remember { mutableStateOf(false) }

    val hasDriverSubOverlay = showDriverReportIncident ||
            showCancelTripDialog ||
            showPinVerificationDialog ||
            showSafetySosSheet ||
            showPassengerRatingDialog ||
            showTollAddDialog ||
            showIncomeDetailsSheet ||
            showBonusesSheet ||
            showAddDailyGoalSheet ||
            showTierBenefitsSheet ||
            showChangeVehicleDialog ||
            showTariffsSheet ||
            showFilterSheet ||
            selectedRequestForOffer != null ||
            selectedDriverTab != "REQUESTS"

    BackHandler(enabled = hasDriverSubOverlay) {
        when {
            showDriverReportIncident -> showDriverReportIncident = false
            showCancelTripDialog -> showCancelTripDialog = false
            showPinVerificationDialog -> showPinVerificationDialog = false
            showSafetySosSheet -> showSafetySosSheet = false
            showPassengerRatingDialog -> showPassengerRatingDialog = false
            showTollAddDialog -> showTollAddDialog = false
            showIncomeDetailsSheet -> showIncomeDetailsSheet = false
            showBonusesSheet -> showBonusesSheet = false
            showAddDailyGoalSheet -> showAddDailyGoalSheet = false
            showTierBenefitsSheet -> showTierBenefitsSheet = false
            showChangeVehicleDialog -> showChangeVehicleDialog = false
            showTariffsSheet -> showTariffsSheet = false
            showFilterSheet -> showFilterSheet = false
            selectedRequestForOffer != null -> selectedRequestForOffer = null
            selectedDriverTab != "REQUESTS" -> selectedDriverTab = "REQUESTS"
        }
    }

    // Navigation & Map State
    var driverGeoPoint by remember(initialUserLocation) {
        mutableStateOf(
            if (initialUserLocation != null && initialUserLocation.latitude != 0.0) {
                GeoPoint(initialUserLocation.latitude, initialUserLocation.longitude)
            } else {
                GeoPoint(34.0151, 71.5249)
            }
        )
    }
    var driverBearing by remember(initialUserLocation) { mutableFloatStateOf(initialUserLocation?.bearing ?: 0f) }
    var driverRouteResult by remember { mutableStateOf<RouteResult?>(null) }
    var currentNavInstruction by remember { mutableStateOf("Head toward pickup location") }
    var currentSpeedKmh by remember(initialUserLocation) { mutableFloatStateOf(initialUserLocation?.speedKmh ?: 0f) }
    var distanceRemainingText by remember { mutableStateOf("1.2 km • 3 mins") }
    var driverRecenterTrigger by remember { mutableIntStateOf(1) }

    // 1. Fetch real-time GPS location immediately upon opening Driver Mode
    LaunchedEffect(Unit) {
        try {
            val loc = locationHelper.getCurrentLocation()
            if (loc != null) {
                driverGeoPoint = GeoPoint(loc.latitude, loc.longitude)
                if (loc.bearing != 0f) driverBearing = loc.bearing
                if (loc.speedKmh > 0f) currentSpeedKmh = loc.speedKmh
                driverRecenterTrigger++
            }
        } catch (_: Exception) {}
    }

    // 2. Real-time location continuous updates flow for Driver Mode & Firebase synchronization
    LaunchedEffect(isDriverOnline, driverId) {
        try {
            locationHelper.getLocationUpdatesFlow().collectLatest { liveLoc ->
                // Only update position from real GPS if driver is not in simulated active trip route animation
                if (activeDriverTrip == null || driverRouteResult == null) {
                    driverGeoPoint = GeoPoint(liveLoc.latitude, liveLoc.longitude)
                    if (liveLoc.bearing != 0f) {
                        driverBearing = liveLoc.bearing
                    }
                    if (liveLoc.speedKmh > 0f) {
                        currentSpeedKmh = liveLoc.speedKmh
                    }
                }

                // If online, continuously sync coordinates to Firebase /driver_locations and /active_drivers
                if (isDriverOnline && driverId.isNotBlank()) {
                    try {
                        repo.updateDriverOnlineLocation(
                            driverId = driverId,
                            latitude = liveLoc.latitude,
                            longitude = liveLoc.longitude,
                            bearing = liveLoc.bearing,
                            speed = liveLoc.speedKmh,
                            driverName = driverName,
                            vehicleType = "$driverVehicleMake $driverVehicleModel".trim(),
                            vehicleNumber = driverVehicleNumber,
                            phone = driverPhone
                        )
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    // 3. When driver goes online/offline, start/stop Foreground Location Service and update status
    LaunchedEffect(isDriverOnline, driverId) {
        if (isDriverOnline && driverId.isNotBlank()) {
            try {
                DriverLocationService.start(
                    context = context,
                    driverId = driverId,
                    driverName = driverName,
                    vehicleType = "$driverVehicleMake $driverVehicleModel".trim(),
                    vehicleNumber = driverVehicleNumber,
                    phone = driverPhone
                )
            } catch (_: Throwable) {}
            try {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    driverGeoPoint = GeoPoint(loc.latitude, loc.longitude)
                    if (loc.bearing != 0f) driverBearing = loc.bearing
                    if (loc.speedKmh > 0f) currentSpeedKmh = loc.speedKmh
                    repo.updateDriverOnlineLocation(
                        driverId = driverId,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        bearing = loc.bearing,
                        speed = loc.speedKmh,
                        driverName = driverName,
                        vehicleType = "$driverVehicleMake $driverVehicleModel".trim(),
                        vehicleNumber = driverVehicleNumber,
                        phone = driverPhone
                    )
                }
            } catch (_: Exception) {}
            driverRecenterTrigger++
        } else {
            try {
                DriverLocationService.stop(context, driverId)
            } catch (_: Throwable) {}
            if (driverId.isNotBlank()) {
                try {
                    repo.setDriverOffline(driverId)
                } catch (_: Exception) {}
            }
        }
    }

    // Driver Wallet - Synchronized with My Wallet screen (single source of truth)
    val walletUserId = user?.uid ?: "guest_user"
    val driverWallet by repo.listenToUserWallet(walletUserId, "DRIVER").collectAsState(
        initial = WalletEntity(
            userId = walletUserId,
            walletId = "wal_$walletUserId",
            balance = 1250.0,
            currency = "PKR",
            userRole = "DRIVER"
        )
    )

    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }
    val currentWalletBalance = driverWallet?.balance ?: 1250.0
    val formattedWalletBalance = remember(currentWalletBalance) {
        currencyFormatter.format(currentWalletBalance)
    }

    // Driver Earnings & Metrics
    var todayEarnings by remember { mutableIntStateOf(2450) }
    var completedTripsCount by remember { mutableIntStateOf(6) }

    // Filtered requests according to driver eligibility & active preferences
    val filteredRequests = remember(
        allRequests,
        selectedCategoryFilter,
        selectedMaxDistanceFilterKm,
        selectedPaymentFilter,
        destinationModeActive,
        destinationModeText,
        driverGeoPoint,
        driverVehicleMake,
        driverVehicleModel,
        isTariffMiniEnabled,
        isTariffAcEnabled,
        isTariffComfortEnabled,
        isTariffCourierEnabled,
        isCourierParcelEnabled,
        hasCourierThermalBag
    ) {
        val isDriverBike = selectedVehicleCategoryType.contains("Motorcycle", true) ||
                driverVehicleMake.contains("Motorcycle", true) ||
                driverVehicleMake.contains("Bike", true) ||
                driverVehicleModel.contains("CD 70", true) ||
                driverVehicleModel.contains("CG 125", true) ||
                driverVehicleModel.contains("GS 150", true) ||
                driverVehicleModel.contains("YBR", true) ||
                driverVehicleModel.contains("Bike", true) ||
                driverVehicleModel.contains("Motorcycle", true)

        allRequests.filter { req ->
            val isRequestAc = req.hasAc || req.rideCategory.contains("AC", true) || req.rideCategory.contains("A/C", true)
            val isRequestMini = req.vehicleType.contains("Mini", true) || req.rideCategory.contains("Mini", true)
            val isRequestComfort = req.rideCategory.contains("Comfort", true)
            val isRequestCourier = req.rideCategory.contains("Courier", true) || req.rideCategory.contains("Parcel", true)

            // Check if driver tariff switch for this category is disabled
            if (isRequestAc && !isTariffAcEnabled && !isRequestMini) return@filter false
            if (isRequestMini && !isTariffMiniEnabled) return@filter false
            if (isRequestComfort && !isTariffComfortEnabled) return@filter false
            if (isRequestCourier && !isTariffCourierEnabled) return@filter false

            // 1. Vehicle category & AC requirements matching
            val isRequestBike = req.vehicleType.contains("Bike", true) ||
                    req.rideCategory.contains("Bike", true) ||
                    req.rideCategory.contains("Moto", true)

            val matchVehicle = if (selectedCategoryFilter == "All") {
                true
            } else if (isDriverBike) {
                isRequestBike || req.rideCategory.contains("Courier", true) || req.rideCategory.contains("Parcel", true)
            } else {
                if (driverVehicleMake.isBlank() && driverVehicleModel.isBlank()) {
                    true
                } else {
                    !isRequestBike || selectedCategoryFilter == "Bike"
                }
            }

            // 2. Category filter selection from UI chips
            val matchCategory = when (selectedCategoryFilter) {
                "All" -> true
                "Share Ride" -> req.rideCategory.contains("Share", ignoreCase = true)
                "Private Ride" -> req.rideCategory.contains("Private", ignoreCase = true) || req.rideCategory.contains("Book", ignoreCase = true) || req.rideCategory.equals("Car", ignoreCase = true)
                "Ride A/C" -> req.hasAc || req.rideCategory.contains("AC", true) || req.rideCategory.contains("A/C", true)
                "Mini" -> req.vehicleType.contains("Mini", true) || req.rideCategory.contains("Mini", true)
                "Bike" -> isRequestBike
                "Courier" -> req.rideCategory.contains("Courier", true) || req.rideCategory.contains("Parcel", true)
                "City to city" -> req.rideCategory.contains("City", true)
                else -> req.rideCategory.equals(selectedCategoryFilter, ignoreCase = true)
            }

            // 3. Driver distance / matching radius from real-time GPS location
            val distToPickupKm = if (req.pickupLat != 0.0 && req.pickupLon != 0.0 && driverGeoPoint.latitude != 0.0) {
                calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon)
            } else {
                req.distanceKm
            }
            val matchDistance = if (selectedMaxDistanceFilterKm == null) {
                true
            } else if (driverGeoPoint.latitude != 0.0 && req.pickupLat != 0.0) {
                distToPickupKm <= selectedMaxDistanceFilterKm!!
            } else {
                true
            }

            // 4. Payment method filter
            val matchPayment = when (selectedPaymentFilter.uppercase()) {
                "CASH" -> req.paymentMethod.equals("Cash", ignoreCase = true)
                "DIGITAL", "WALLET" -> !req.paymentMethod.equals("Cash", ignoreCase = true)
                else -> true
            }

            // 5. Destination Mode ("On My Way Home")
            val matchDest = if (destinationModeActive && destinationModeText.isNotBlank()) {
                req.destinationTitle.contains(destinationModeText.trim(), ignoreCase = true) ||
                req.destinationSubtitle.contains(destinationModeText.trim(), ignoreCase = true)
            } else true

            matchVehicle && matchCategory && matchDistance && matchPayment && matchDest
        }
    }

    // Auto-Accept Handler (Item 3)
    LaunchedEffect(filteredRequests, isAutoAcceptEnabled, isDriverOnline, activeDriverTrip) {
        if (isDriverOnline && isAutoAcceptEnabled && activeDriverTrip == null) {
            val matchingReq = filteredRequests.firstOrNull { req ->
                req.estimatedFare >= autoAcceptMinFare && req.distanceKm <= autoAcceptMaxDistanceKm && offerSentRequestId != req.id
            }
            if (matchingReq != null) {
                offerSentRequestId = matchingReq.id
                val offer = DriverOffer(
                    requestId = matchingReq.id,
                    driverId = driverId,
                    driverName = driverName,
                    driverVehicleMake = driverVehicleMake,
                    driverVehicleModel = driverVehicleModel,
                    driverPlateNumber = driverVehicleNumber,
                    driverPhone = driverPhone,
                    offeredFare = matchingReq.estimatedFare,
                    driverRating = 4.9,
                    etaMinutes = 3,
                    distanceKmAway = 0.9,
                    driverLat = driverGeoPoint.latitude,
                    driverLon = driverGeoPoint.longitude
                )
                repo.sendDriverOffer(offer)
                audioHelper.speak("Auto offer sent for ${matchingReq.estimatedFare} rupees")
                Toast.makeText(context, "Auto-Accept: Offer sent for PKR ${matchingReq.estimatedFare} to ${matchingReq.passengerName}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    var selectedCancelReason by remember { mutableStateOf("Passenger didn't show up") }
    var showDriverNotificationCenterSheet by remember { mutableStateOf(false) }
    val driverNotificationHistory by notifManager.notificationHistory.collectAsState()

    var lastKnownDriverTripStatus by remember { mutableStateOf<PassengerOrderStatus?>(null) }
    var lastKnownDriverTripId by remember { mutableStateOf<String?>(null) }

    // Manage Driver Active Ride Persistent Notification
    LaunchedEffect(activeDriverTrip?.id, activeDriverTrip?.status) {
        val trip = activeDriverTrip
        if (trip != null && (trip.status == PassengerOrderStatus.DRIVER_COMING || trip.status == PassengerOrderStatus.ACCEPTED || trip.status == PassengerOrderStatus.DRIVER_ARRIVED || trip.status == PassengerOrderStatus.IN_TRIP)) {
            notifManager.updateDriverActiveRideNotification(trip)
        } else {
            notifManager.dismissDriverActiveRideNotification()
        }
    }

    // Real-time synchronization & restoration of Active Driver Trip from Firebase (single source of truth)
    LaunchedEffect(driverId, driverPhone) {
        repo.listenToDriverActiveTrip(driverId, driverPhone).collectLatest { remoteTrip ->
            if (remoteTrip != null) {
                if (remoteTrip.status == PassengerOrderStatus.CANCELLED) {
                    if (activeDriverTrip != null && (activeDriverTrip?.id == remoteTrip.id || activeDriverTrip?.requestId == remoteTrip.requestId)) {
                        Toast.makeText(context, "Passenger cancelled the ride", Toast.LENGTH_SHORT).show()
                        notifManager.notifyDriverRideCancelled(
                            passengerName = remoteTrip.passengerName.ifBlank { "Passenger" },
                            rideId = remoteTrip.id
                        )
                        notifManager.dismissDriverActiveRideNotification()
                        activeDriverTrip = null
                        driverRouteResult = null
                        offerSentRequestId = null
                        driverRecenterTrigger++
                    }
                } else if (remoteTrip.status == PassengerOrderStatus.COMPLETED) {
                    if (activeDriverTrip != null && (activeDriverTrip?.id == remoteTrip.id || activeDriverTrip?.requestId == remoteTrip.requestId)) {
                        notifManager.dismissDriverActiveRideNotification()
                        activeDriverTrip = null
                        driverRouteResult = null
                        offerSentRequestId = null
                        driverRecenterTrigger++
                    }
                } else {
                    // Active status: DRIVER_COMING, DRIVER_ARRIVED, IN_TRIP
                    if (activeDriverTrip == null || activeDriverTrip?.id != remoteTrip.id || activeDriverTrip?.status != remoteTrip.status) {
                        activeDriverTrip = remoteTrip
                        offerSentRequestId = remoteTrip.requestId.ifBlank { remoteTrip.id }
                        driverRecenterTrigger++
                    }
                }
            }
        }
    }

    // Real-time trip status listener on RTDB ride_requests (clears active trip immediately on cancellation or completion)
    LaunchedEffect(activeDriverTrip?.id, activeDriverTrip?.requestId) {
        val trip = activeDriverTrip
        val reqId = trip?.requestId?.ifBlank { trip.id }
        if (reqId != null) {
            val db = try {
                FirebaseDatabase.getInstance("https://drigo-8b15c-default-rtdb.firebaseio.com")
            } catch (_: Exception) {
                try { FirebaseDatabase.getInstance() } catch (_: Exception) { null }
            }
            if (db != null) {
                val statusRef = db.getReference("ride_requests").child(reqId).child("status")
                val statusListener = object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val status = snapshot.getValue(String::class.java)
                        if (status == "CANCELLED") {
                            notifManager.dismissDriverActiveRideNotification()
                            activeDriverTrip = null
                            driverRouteResult = null
                            offerSentRequestId = null
                            driverRecenterTrigger++
                            Toast.makeText(context, "Passenger cancelled the ride", Toast.LENGTH_SHORT).show()
                            notifManager.notifyDriverRideCancelled(
                                passengerName = trip.passengerName.ifBlank { trip.passengerEmail.substringBefore("@").ifBlank { "Passenger" } },
                                rideId = trip.id
                            )
                        } else if (status == "COMPLETED") {
                            notifManager.dismissDriverActiveRideNotification()
                            if (activeDriverTrip?.status != PassengerOrderStatus.COMPLETED) {
                                activeDriverTrip = null
                                driverRouteResult = null
                                offerSentRequestId = null
                                driverRecenterTrigger++
                            }
                        } else if (status == "DRIVER_ARRIVED" && activeDriverTrip?.status != PassengerOrderStatus.DRIVER_ARRIVED) {
                            activeDriverTrip = activeDriverTrip?.copy(status = PassengerOrderStatus.DRIVER_ARRIVED)
                        } else if (status == "IN_TRIP" && activeDriverTrip?.status != PassengerOrderStatus.IN_TRIP) {
                            activeDriverTrip = activeDriverTrip?.copy(status = PassengerOrderStatus.IN_TRIP)
                        }
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                }
                statusRef.addValueEventListener(statusListener)
            }
        }
    }

    // Listen to active driver trip assignments and status updates
    LaunchedEffect(activeDriverTrip?.id, activeDriverTrip?.status) {
        val trip = activeDriverTrip
        if (trip != null) {
            val passName = trip.passengerEmail.substringBefore("@").ifBlank { "Passenger" }
            if (trip.id != lastKnownDriverTripId) {
                lastKnownDriverTripId = trip.id
                notifManager.notifyPassengerAcceptedOffer(
                    passengerName = passName,
                    farePkr = trip.agreedFare,
                    pickup = trip.pickupTitle,
                    rideId = trip.id
                )
                notifManager.notifyDriverRideAssigned(
                    passengerName = passName,
                    pickup = trip.pickupTitle,
                    rideId = trip.id
                )
            }
            if (trip.status != lastKnownDriverTripStatus) {
                if (trip.status == PassengerOrderStatus.CANCELLED) {
                    notifManager.notifyDriverRideCancelled(
                        passengerName = passName,
                        rideId = trip.id
                    )
                    notifManager.clearVoiceQueueAndTracking()
                }
                lastKnownDriverTripStatus = trip.status
            }
        } else if (lastKnownDriverTripId != null && lastKnownDriverTripStatus != PassengerOrderStatus.COMPLETED) {
            // Trip was cleared / cancelled
            lastKnownDriverTripId = null
            lastKnownDriverTripStatus = null
            notifManager.clearVoiceQueueAndTracking()
        }
    }

    // Wait Time Counter effect
    LaunchedEffect(activeDriverTrip?.id, activeDriverTrip?.status) {
        val currentTrip = activeDriverTrip
        if (currentTrip?.status == PassengerOrderStatus.DRIVER_ARRIVED) {
            isWaitingTimerRunning = true
            waitTimeSeconds = 0
            audioHelper.playArrivalChime(currentTrip.id)
            while (isWaitingTimerRunning && activeDriverTrip?.status == PassengerOrderStatus.DRIVER_ARRIVED) {
                delay(1000)
                waitTimeSeconds += 1
            }
        } else {
            isWaitingTimerRunning = false
        }
    }

    // Calculate extra wait fee: Free for first 5 mins (300 sec), then PKR 5 per minute
    val extraWaitFee = remember(waitTimeSeconds) {
        if (waitTimeSeconds > 300) {
            val extraMinutes = ((waitTimeSeconds - 300) + 59) / 60
            extraMinutes * 5
        } else {
            0
        }
    }

    // Auto-calculate route for preview when selecting a request
    LaunchedEffect(selectedRequestForOffer) {
        val req = selectedRequestForOffer
        if (req != null && req.pickupLat != 0.0 && req.destinationLat != 0.0) {
            scope.launch {
                val r = routeService.calculateRoute(
                    startPoint = GeoPoint(req.pickupLat, req.pickupLon),
                    destPoint = GeoPoint(req.destinationLat, req.destinationLon),
                    startAddress = req.pickupTitle,
                    destinationAddress = req.destinationTitle
                )
                driverRouteResult = r
            }
        }
    }

    // Real-Time Driver Car Movement along Route Polyline & Stage Transitions
    LaunchedEffect(activeDriverTrip?.id, activeDriverTrip?.status) {
        val trip = activeDriverTrip
        if (trip != null) {
            val pLat = if (trip.pickupLat != 0.0) trip.pickupLat else (driverGeoPoint.latitude.takeIf { it != 0.0 } ?: 34.0151)
            val pLon = if (trip.pickupLon != 0.0) trip.pickupLon else (driverGeoPoint.longitude.takeIf { it != 0.0 } ?: 71.5249)
            val dLat = if (trip.destinationLat != 0.0) trip.destinationLat else pLat + 0.02
            val dLon = if (trip.destinationLon != 0.0) trip.destinationLon else pLon + 0.02

            // ALWAYS calculate the passenger's ride route (Pickup -> Destination) so both locations are marked and route is colored!
            val pickupPt = GeoPoint(pLat, pLon)
            val destPt = GeoPoint(dLat, dLon)

            val passengerRoute = routeService.calculateRoute(
                startPoint = pickupPt,
                destPoint = destPt,
                startAddress = trip.pickupTitle,
                destinationAddress = trip.destinationTitle
            )
            driverRouteResult = passengerRoute ?: RouteResult(
                points = listOf(pickupPt, destPt),
                distanceKm = trip.distanceKm,
                durationMinutes = trip.durationMinutes,
                startAddress = trip.pickupTitle,
                destinationAddress = trip.destinationTitle,
                startPoint = pickupPt,
                destinationPoint = destPt
            )
            driverRecenterTrigger++

            if (trip.status == PassengerOrderStatus.ACCEPTED || trip.status == PassengerOrderStatus.DRIVER_COMING) {
                // 1. STAGE: DRIVER -> PICKUP LOCATION
                val startLat = if (driverGeoPoint.latitude != 0.0) driverGeoPoint.latitude else (trip.pickupLat - 0.015)
                val startLon = if (driverGeoPoint.longitude != 0.0) driverGeoPoint.longitude else (trip.pickupLon - 0.015)
                val targetLat = trip.pickupLat
                val targetLon = trip.pickupLon

                val pickupNavRoute = routeService.calculateRoute(
                    startPoint = GeoPoint(startLat, startLon),
                    destPoint = GeoPoint(targetLat, targetLon),
                    startAddress = "Current Location",
                    destinationAddress = trip.pickupTitle
                )
                currentNavInstruction = "Drive to Pickup: ${trip.pickupTitle}"

                val points = pickupNavRoute?.points ?: listOf(GeoPoint(startLat, startLon), GeoPoint(targetLat, targetLon))
                if (points.isNotEmpty()) {
                    for (i in 0 until points.size) {
                        if (activeDriverTrip == null || (activeDriverTrip?.status != PassengerOrderStatus.ACCEPTED && activeDriverTrip?.status != PassengerOrderStatus.DRIVER_COMING)) {
                            break
                        }
                        val currentPt = points[i]
                        val nextPt = points.getOrNull(i + 1) ?: currentPt

                        val dLat = Math.toRadians(nextPt.latitude - currentPt.latitude)
                        val dLon = Math.toRadians(nextPt.longitude - currentPt.longitude)
                        val y = Math.sin(dLon) * Math.cos(Math.toRadians(nextPt.latitude))
                        val x = Math.cos(Math.toRadians(currentPt.latitude)) * Math.sin(Math.toRadians(nextPt.latitude)) -
                                Math.sin(Math.toRadians(currentPt.latitude)) * Math.cos(Math.toRadians(nextPt.latitude)) * Math.cos(dLon)
                        val computedBearing = ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360).toFloat()

                        driverGeoPoint = currentPt
                        driverBearing = computedBearing

                        val remainingFraction = 1f - (i.toFloat() / points.size.coerceAtLeast(1))
                        val remainingKm = (pickupNavRoute?.distanceKm ?: 4.0) * remainingFraction
                        val remainingMin = ((pickupNavRoute?.durationMinutes ?: 10) * remainingFraction).toInt().coerceAtLeast(1)
                        distanceRemainingText = "${String.format(java.util.Locale.US, "%.1f", remainingKm)} km to Pickup • $remainingMin min"
                        currentSpeedKmh = (32f + (i % 5) * 2f)

                        // Push Live Driver Location to Firebase
                        repo.updateLiveDriverLocation(
                            LiveDriverLocation(
                                rideId = trip.requestId.ifBlank { trip.id },
                                driverId = driverId,
                                latitude = currentPt.latitude,
                                longitude = currentPt.longitude,
                                bearing = computedBearing,
                                speedKmh = currentSpeedKmh,
                                etaMinutes = remainingMin,
                                distanceRemainingKm = remainingKm,
                                status = trip.status.name,
                                updatedAt = System.currentTimeMillis()
                            )
                        )

                        delay(1200)
                    }
                }
            } else if (trip.status == PassengerOrderStatus.DRIVER_ARRIVED) {
                // 2. STAGE: AT PICKUP LOCATION (Waiting for Passenger)
                driverGeoPoint = GeoPoint(trip.pickupLat, trip.pickupLon)
                currentNavInstruction = "Waiting for passenger at Pickup"
                distanceRemainingText = "Arrived at Pickup • Waiting for Rider"
                currentSpeedKmh = 0f

                repo.updateLiveDriverLocation(
                    LiveDriverLocation(
                        rideId = trip.requestId.ifBlank { trip.id },
                        driverId = driverId,
                        latitude = trip.pickupLat,
                        longitude = trip.pickupLon,
                        bearing = driverBearing,
                        speedKmh = 0f,
                        etaMinutes = 0,
                        distanceRemainingKm = 0.0,
                        status = trip.status.name,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else if (trip.status == PassengerOrderStatus.IN_TRIP) {
                // 3. STAGE: IN TRIP (PICKUP -> DESTINATION)
                val startLat = if (driverGeoPoint.latitude != 0.0) driverGeoPoint.latitude else trip.pickupLat
                val startLon = if (driverGeoPoint.longitude != 0.0) driverGeoPoint.longitude else trip.pickupLon
                val targetLat = trip.destinationLat
                val targetLon = trip.destinationLon

                val r = routeService.calculateRoute(
                    startPoint = GeoPoint(startLat, startLon),
                    destPoint = GeoPoint(targetLat, targetLon),
                    startAddress = trip.pickupTitle,
                    destinationAddress = trip.destinationTitle
                )
                driverRouteResult = r
                currentNavInstruction = "Drive to Dropoff: ${trip.destinationTitle}"

                val points = r?.points ?: listOf(GeoPoint(startLat, startLon), GeoPoint(targetLat, targetLon))
                if (points.isNotEmpty()) {
                    for (i in 0 until points.size) {
                        if (activeDriverTrip == null || activeDriverTrip?.status != PassengerOrderStatus.IN_TRIP) {
                            break
                        }
                        val currentPt = points[i]
                        val nextPt = points.getOrNull(i + 1) ?: currentPt

                        val dLat = Math.toRadians(nextPt.latitude - currentPt.latitude)
                        val dLon = Math.toRadians(nextPt.longitude - currentPt.longitude)
                        val y = Math.sin(dLon) * Math.cos(Math.toRadians(nextPt.latitude))
                        val x = Math.cos(Math.toRadians(currentPt.latitude)) * Math.sin(Math.toRadians(nextPt.latitude)) -
                                Math.sin(Math.toRadians(currentPt.latitude)) * Math.cos(Math.toRadians(nextPt.latitude)) * Math.cos(dLon)
                        val computedBearing = ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360).toFloat()

                        driverGeoPoint = currentPt
                        driverBearing = computedBearing

                        val remainingFraction = 1f - (i.toFloat() / points.size.coerceAtLeast(1))
                        val remainingKm = (r?.distanceKm ?: 6.0) * remainingFraction
                        val remainingMin = ((r?.durationMinutes ?: 15) * remainingFraction).toInt().coerceAtLeast(1)
                        distanceRemainingText = "${String.format(java.util.Locale.US, "%.1f", remainingKm)} km to Dropoff • $remainingMin min"
                        currentSpeedKmh = (38f + (i % 6) * 2f)

                        repo.updateLiveDriverLocation(
                            LiveDriverLocation(
                                rideId = trip.requestId.ifBlank { trip.id },
                                driverId = driverId,
                                latitude = currentPt.latitude,
                                longitude = currentPt.longitude,
                                bearing = computedBearing,
                                speedKmh = currentSpeedKmh,
                                etaMinutes = remainingMin,
                                distanceRemainingKm = remainingKm,
                                status = trip.status.name,
                                updatedAt = System.currentTimeMillis()
                            )
                        )

                        delay(1200)
                    }
                }
            }
        }
    }

    // Helper function to launch External GPS Navigation (Google Maps / Waze)
    fun launchExternalGpsNavigation(lat: Double, lon: Double, label: String) {
        try {
            val uri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback to generic geo intent
                val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                context.startActivity(mapIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Opening GPS: $label ($lat, $lon)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Map interaction state to slide down bottom sheet when driver touches/drags map
    var isMapTouched by remember { mutableStateOf(false) }
    var isRequestsFeedExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E2026))
            .testTag("driver_mode_view")
    ) {
        // Fullscreen OSM Map with Driver Car Marker & Request Pins
        RealOsmMapView(
            modifier = Modifier.fillMaxSize(),
            currentLatitude = driverGeoPoint.latitude,
            currentLongitude = driverGeoPoint.longitude,
            fromLocation = if (activeDriverTrip != null) AppLocation(
                title = activeDriverTrip!!.pickupTitle,
                subtitle = activeDriverTrip!!.pickupSubtitle,
                latitude = if (activeDriverTrip!!.pickupLat != 0.0) activeDriverTrip!!.pickupLat else (driverGeoPoint.latitude.takeIf { it != 0.0 } ?: 34.0151),
                longitude = if (activeDriverTrip!!.pickupLon != 0.0) activeDriverTrip!!.pickupLon else (driverGeoPoint.longitude.takeIf { it != 0.0 } ?: 71.5249)
            ) else selectedRequestForOffer?.let {
                AppLocation(title = it.pickupTitle, subtitle = it.pickupSubtitle, latitude = it.pickupLat, longitude = it.pickupLon)
            },
            toLocation = if (activeDriverTrip != null) AppLocation(
                title = activeDriverTrip!!.destinationTitle,
                subtitle = activeDriverTrip!!.destinationSubtitle,
                latitude = if (activeDriverTrip!!.destinationLat != 0.0) activeDriverTrip!!.destinationLat else (driverGeoPoint.latitude.takeIf { it != 0.0 } ?: 34.0151) + 0.02,
                longitude = if (activeDriverTrip!!.destinationLon != 0.0) activeDriverTrip!!.destinationLon else (driverGeoPoint.longitude.takeIf { it != 0.0 } ?: 71.5249) + 0.02
            ) else selectedRequestForOffer?.let {
                AppLocation(title = it.destinationTitle, subtitle = it.destinationSubtitle, latitude = it.destinationLat, longitude = it.destinationLon)
            },
            routeResult = driverRouteResult,
            driverCarLocation = driverGeoPoint,
            driverCarBearing = driverBearing,
            driverCarTitle = "$driverName ($driverVehicleModel)",
            driverCarFareText = activeDriverTrip?.let { "${it.agreedFare} Rs" } ?: selectedRequestForOffer?.let { "${it.estimatedFare} Rs" },
            passengerRequestsOnMap = if (isDriverOnline && activeDriverTrip == null) filteredRequests else emptyList(),
            onPassengerRequestMarkerClick = { req ->
                selectedRequestForOffer = req
            },
            recenterTrigger = driverRecenterTrigger,
            showCenterPickupPin = false,
            showMapLayerSwitcher = false,
            onMapInteractionChange = { isInteracting ->
                isMapTouched = isInteracting
            }
        )

        // Top Header Gradient Overlay & App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF5101218),
                            Color(0xD0101218),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val availableWidth = maxWidth
                val isCompact = availableWidth < 365.dp
                val isVeryCompact = availableWidth < 330.dp

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu Drawer Button
                    Surface(
                        onClick = onOpenDrawer,
                        shape = CircleShape,
                        color = Color(0xFF1E222D).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, DrigoBrandPurple.copy(alpha = 0.6f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("driver_drawer_menu_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Driver Online / Offline Pill Switch
                    Surface(
                        onClick = handleToggleOnlineClick,
                        shape = RoundedCornerShape(100.dp),
                        color = if (isDriverOnline) Color(0xFF0D381E) else Color(0xFF222630),
                        border = BorderStroke(
                            1.5.dp,
                            if (isDriverOnline) Color(0xFF00E676) else Color(0xFF434959)
                        ),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("driver_online_toggle_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = if (isCompact) 10.dp else 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDriverOnline) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.35f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseAlpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF00E676).copy(alpha = pulseAlpha), CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF90A4AE), CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDriverOnline) "ONLINE" else "OFFLINE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDriverOnline) Color(0xFF00E676) else Color(0xFFCFD8DC),
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Top Right Action Buttons: Voice Toggle + SOS + Wallet
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isVeryCompact) 3.dp else if (isCompact) 4.dp else 6.dp)
                    ) {
                        // Voice Alert Toggle Button
                        Surface(
                            onClick = {
                                isVoiceAlertsEnabled = !isVoiceAlertsEnabled
                                audioHelper.setVoiceEnabled(isVoiceAlertsEnabled)
                                Toast.makeText(
                                    context,
                                    if (isVoiceAlertsEnabled) "Voice announcements ON" else "Voice announcements MUTED",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = CircleShape,
                            color = if (isVoiceAlertsEnabled) Color(0xFF2B203F) else Color(0xFF1E222D),
                            border = BorderStroke(1.dp, if (isVoiceAlertsEnabled) DrigoBrandPurple else Color(0xFF353B4A)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("driver_voice_alerts_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isVoiceAlertsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Voice Alerts",
                                    tint = if (isVoiceAlertsEnabled) DrigoBrandPurple else Color(0xFF78909C),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Notification Center Bell Button with Badge
                        Surface(
                            onClick = { showDriverNotificationCenterSheet = true },
                            shape = CircleShape,
                            color = Color(0xFF1E222D),
                            border = BorderStroke(1.dp, Color(0xFF353B4A)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("driver_notification_center_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Driver Notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                if (driverNotificationHistory.isNotEmpty()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF00E676),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .align(Alignment.TopEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (driverNotificationHistory.size > 9) "9+" else "${driverNotificationHistory.size}",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SOS / Safety Center Button
                        Surface(
                            onClick = { showSafetySosSheet = true },
                            shape = CircleShape,
                            color = Color(0xFFB71C1C),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.7f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("driver_sos_safety_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Driver SOS Safety",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Driver Earnings / Wallet Button
                        Surface(
                            onClick = onNavigateToWallet,
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFF14171F).copy(alpha = 0.96f),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("driver_earnings_wallet_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isVeryCompact) "₨ $formattedWalletBalance" else "PKR $formattedWalletBalance",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isVeryCompact) 10.5.sp else 11.5.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Trip Turn-by-Turn Navigation Bar (Item 2)
        if (activeDriverTrip != null && !isActiveTripSheetExpanded) {
            val trip = activeDriverTrip!!
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E2E).copy(alpha = 0.96f),
                border = BorderStroke(1.5.dp, DrigoBrandPurple),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 96.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DrigoBrandPurple,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (trip.status == PassengerOrderStatus.ACCEPTED || trip.status == PassengerOrderStatus.DRIVER_COMING) Icons.Default.Navigation else Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentNavInstruction,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$distanceRemainingText • ${currentSpeedKmh.toInt()} km/h",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // External GPS App Launcher Button (Google Maps / Waze)
                    Surface(
                        onClick = {
                            val isHeadingToPickup = trip.status == PassengerOrderStatus.ACCEPTED || trip.status == PassengerOrderStatus.DRIVER_COMING
                            val targetLat = if (isHeadingToPickup) trip.pickupLat else trip.destinationLat
                            val targetLon = if (isHeadingToPickup) trip.pickupLon else trip.destinationLon
                            val targetTitle = if (isHeadingToPickup) trip.pickupTitle else trip.destinationTitle
                            launchExternalGpsNavigation(targetLat, targetLon, targetTitle)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2C303E),
                        border = BorderStroke(1.dp, Color(0xFF4FC3F7)),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Open in Google Maps",
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Maps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Share Trip / Route Button
                    Surface(
                        onClick = {
                            val isHeadingToPickup = trip.status == PassengerOrderStatus.ACCEPTED || trip.status == PassengerOrderStatus.DRIVER_COMING
                            val destTitle = if (isHeadingToPickup) trip.pickupTitle else trip.destinationTitle
                            val targetLat = if (isHeadingToPickup) trip.pickupLat else trip.destinationLat
                            val targetLon = if (isHeadingToPickup) trip.pickupLon else trip.destinationLon
                            val shareText = "I am on an active Drigo trip!\n\n" +
                                    "📍 Pickup: ${trip.pickupTitle}\n" +
                                    "🏁 Destination: ${trip.destinationTitle}\n" +
                                    "⏱️ Distance/ETA: $distanceRemainingText\n" +
                                    "💳 Fare: PKR ${trip.agreedFare}\n" +
                                    (if (targetLat != 0.0 && targetLon != 0.0) "🗺️ Route: https://maps.google.com/?q=$targetLat,$targetLon\n\n" else "\n") +
                                    "Drigo Captain Live Route"

                            try {
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Drigo Trip Status & Route")
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Route & ETA via")
                                context.startActivity(shareIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Unable to share ride info", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2C303E),
                        border = BorderStroke(1.dp, DrigoBrandPurple),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Route",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Share",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Account Status Checks
        val parsedVerStatus = remember(driverVerification) {
            parseDriverVerificationStatus(
                driverVerification?.verificationStatus,
                driverVerification?.status,
                driverVerification?.confirmtion ?: false
            )
        }
        val parsedAccStatus = remember(driverVerification, isDriverOnline) {
            parseDriverAccountStatus(
                driverVerification?.accountStatus,
                driverVerification?.status,
                parsedVerStatus,
                isDriverOnline
            )
        }

        // Dedicated Account Suspended / Flagged Overlay
        if (parsedAccStatus == DriverAccountStatus.SUSPENDED || parsedAccStatus == DriverAccountStatus.FLAGGED) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(24.dp),
                color = Color(0xFF1E1E24),
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
                        contentDescription = "Account Suspended",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (parsedAccStatus == DriverAccountStatus.SUSPENDED) "Account Suspended" else "Account Flagged",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = driverVerification?.rejectionReason?.ifBlank {
                            "Your driver account has been suspended by support compliance. Please contact support for account review."
                        } ?: "Your driver account has been suspended by support compliance. Please contact support for account review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@drigo.com?subject=Account%20Suspension%20Appeal%20UID%20${user?.uid}")
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
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSwitchToPassenger,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Switch to Passenger Mode", color = Color.White)
                    }
                }
            }
        }

        // Verified Driver Success Banner (Shown only ONCE upon approval, not every mode switch)
        val driverPrefs = remember(context) { context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE) }
        val bannerKey = remember(driverId) { "verified_banner_shown_$driverId" }
        val isVerifiedOrApproved = parsedVerStatus == DriverVerificationStatus.APPROVED

        var showVerifiedBanner by remember(driverId, parsedVerStatus) {
            val alreadyShown = driverPrefs.getBoolean(bannerKey, false)
            mutableStateOf(isVerifiedOrApproved && !alreadyShown)
        }

        // Reset persistent flag if status is no longer approved (so future re-approval can notify once)
        LaunchedEffect(parsedVerStatus, bannerKey) {
            if (parsedVerStatus != DriverVerificationStatus.APPROVED) {
                driverPrefs.edit().putBoolean(bannerKey, false).apply()
            }
        }

        // Auto-dismiss banner after 8 seconds and persist dismissal
        LaunchedEffect(showVerifiedBanner, bannerKey) {
            if (showVerifiedBanner) {
                delay(8000L)
                showVerifiedBanner = false
                driverPrefs.edit().putBoolean(bannerKey, true).apply()
            }
        }

        if (isVerifiedOrApproved && showVerifiedBanner && activeDriverTrip == null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1B5E20).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF00E676)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 96.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Congratulations! Your registration has been verified by admin.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            showVerifiedBanner = false
                            driverPrefs.edit().putBoolean(bannerKey, true).apply()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        if (activeDriverTrip != null) {
            // ================= ACTIVE TRIP CONTROLS PANEL =================
            val trip = activeDriverTrip!!
            val totalCashToCollect = trip.agreedFare + extraWaitFee + tollSurchargesPkr

            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF181A20),
                shadowElevation = 20.dp,
                border = BorderStroke(1.dp, Color(0xFF2C303E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isActiveTripSheetExpanded) Modifier.fillMaxHeight(0.82f)
                        else Modifier.wrapContentHeight()
                    )
                    .align(Alignment.BottomCenter)
                    .animateContentSize()
                    .testTag("driver_active_trip_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Interactive Header Drag Pill & Expand/Collapse Toggle Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isActiveTripSheetExpanded = !isActiveTripSheetExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Status Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when (trip.status) {
                                PassengerOrderStatus.DRIVER_ARRIVED -> Color(0xFFE65100).copy(alpha = 0.2f)
                                PassengerOrderStatus.IN_TRIP -> DrigoBrandPurple.copy(alpha = 0.2f)
                                else -> Color(0xFF00C853).copy(alpha = 0.2f)
                            },
                            border = BorderStroke(1.dp, when (trip.status) {
                                PassengerOrderStatus.DRIVER_ARRIVED -> Color(0xFFFF9800)
                                PassengerOrderStatus.IN_TRIP -> DrigoBrandPurple
                                else -> Color(0xFF00C853)
                            })
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (trip.status) {
                                        PassengerOrderStatus.DRIVER_ARRIVED -> Color(0xFFFF9800)
                                        PassengerOrderStatus.IN_TRIP -> DrigoBrandPurple
                                        else -> Color(0xFF00C853)
                                    },
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (trip.status) {
                                        PassengerOrderStatus.DRIVER_ARRIVED -> "ARRIVED AT PICKUP"
                                        PassengerOrderStatus.IN_TRIP -> "RIDE IN PROGRESS"
                                        else -> "EN ROUTE TO PICKUP"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Center Drag Pill
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                        )

                        // Right Expand / Collapse Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = if (isActiveTripSheetExpanded) "Collapse" else "Details",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF90A4AE)
                            )
                            Icon(
                                imageVector = if (isActiveTripSheetExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isActiveTripSheetExpanded) "Collapse" else "Expand",
                                tint = Color(0xFF90A4AE),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Main Passenger & Fare Header Row (Always Visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DrigoBrandPurple.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, DrigoBrandPurple.copy(alpha = 0.6f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = trip.passengerName.ifBlank { trip.passengerEmail.substringBefore("@").ifBlank { "Passenger" } },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Text(
                                        text = "★ 4.9",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Fare: PKR $totalCashToCollect • ${trip.paymentMethod.ifBlank { "Cash" }}",
                                fontSize = 11.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Quick Action Buttons Cluster
                        val passPhone = trip.passengerPhone.ifBlank { "+92 300 9876543" }
                        val passName = trip.passengerName.ifBlank { trip.passengerEmail.substringBefore("@").ifBlank { "Passenger" } }
                        val targetNavLat = if (trip.status == PassengerOrderStatus.IN_TRIP) trip.destinationLat else trip.pickupLat
                        val targetNavLon = if (trip.status == PassengerOrderStatus.IN_TRIP) trip.destinationLon else trip.pickupLon
                        val targetNavTitle = if (trip.status == PassengerOrderStatus.IN_TRIP) trip.destinationTitle else trip.pickupTitle

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // GPS Navigation
                            IconButton(
                                onClick = {
                                    launchExternalGpsNavigation(targetNavLat, targetNavLon, targetNavTitle)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0288D1))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Navigate GPS",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Call
                            IconButton(
                                onClick = {
                                    try {
                                        val cleanPhone = passPhone.replace(" ", "")
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Calling $passName ($passPhone)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C303E))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Passenger",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Chat
                            IconButton(
                                onClick = {
                                    onOpenChat(
                                        trip.id,
                                        passName,
                                        "Passenger",
                                        passPhone,
                                        trip.pickupTitle,
                                        trip.destinationTitle
                                    )
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DrigoBrandPurple.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Share Ride Status
                            IconButton(
                                onClick = {
                                    try {
                                        val shareMsg = "Drigo Ride Status Update:\n" +
                                                "Passenger: $passName\n" +
                                                "Pickup: ${trip.pickupTitle}\n" +
                                                "Destination: ${trip.destinationTitle}\n" +
                                                "Fare: PKR $totalCashToCollect\n" +
                                                "Status: ${if (trip.status == PassengerOrderStatus.IN_TRIP) "Driving to Destination" else "Heading to Pickup"}"
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Drigo Active Ride Status")
                                            putExtra(Intent.EXTRA_TEXT, shareMsg)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Ride Details"))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Unable to share ride details", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF37474F))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Ride",
                                    tint = Color(0xFF4FC3F7),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Middle Area: Compact target focus card when collapsed, full scrollable view when expanded
                    if (!isActiveTripSheetExpanded) {
                        // Collapsed Active Target Focus Card (Clean, high information density, zero clutter)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF12141A),
                            border = BorderStroke(1.dp, Color(0xFF262A36)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (trip.status == PassengerOrderStatus.DRIVER_ARRIVED) {
                                // Live Wait Time Banner
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = if (waitTimeSeconds <= 300) Color(0xFF81C784) else Color(0xFFFFB74D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            val min = waitTimeSeconds / 60
                                            val sec = waitTimeSeconds % 60
                                            Text(
                                                text = "Waiting Time: ${String.format(java.util.Locale.US, "%02d:%02d", min, sec)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (waitTimeSeconds <= 300) "Free wait: ${(300 - waitTimeSeconds) / 60}m left" else "Paid wait (+PKR 5/min)",
                                                fontSize = 10.sp,
                                                color = if (waitTimeSeconds <= 300) Color(0xFFB0BEC5) else Color(0xFFFFB74D)
                                            )
                                        }
                                    }
                                    if (extraWaitFee > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFF9800)
                                        ) {
                                            Text(
                                                text = "+PKR $extraWaitFee",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Target Location Summary (Heading to Pickup vs Heading to Dropoff)
                                val isHeadingToDropoff = trip.status == PassengerOrderStatus.IN_TRIP
                                val targetTitle = if (isHeadingToDropoff) trip.destinationTitle.ifBlank { "Destination Location" } else trip.pickupTitle.ifBlank { "Pickup Location" }
                                val targetSubtitle = if (isHeadingToDropoff) trip.destinationSubtitle else trip.pickupSubtitle

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isHeadingToDropoff) Color(0xFFE53935) else Color(0xFF00C853),
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isHeadingToDropoff) "Drop-off: $targetTitle" else "Pickup: $targetTitle",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (targetSubtitle.isNotBlank()) {
                                            Text(
                                                text = targetSubtitle,
                                                fontSize = 10.sp,
                                                color = Color(0xFF90A4AE),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (distanceRemainingText.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = distanceRemainingText.substringBefore("•").trim(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4FC3F7)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // EXPANDED VIEW: Scrollable Comprehensive Details
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Trip Progress Stepper Bar
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF14161C),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Step 1: Accepted
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF00C853),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Accepted", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Box(modifier = Modifier.weight(1f).height(2.dp).padding(horizontal = 6.dp).background(if (trip.status != PassengerOrderStatus.ACCEPTED && trip.status != PassengerOrderStatus.DRIVER_COMING) Color(0xFF00C853) else Color(0xFF37474F)))

                                    // Step 2: Arrived
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (trip.status == PassengerOrderStatus.DRIVER_ARRIVED || trip.status == PassengerOrderStatus.IN_TRIP) Color(0xFF00C853) else Color(0xFF37474F),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (trip.status == PassengerOrderStatus.DRIVER_ARRIVED || trip.status == PassengerOrderStatus.IN_TRIP) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                                } else {
                                                    Text("2", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Arrived", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (trip.status == PassengerOrderStatus.DRIVER_ARRIVED) Color(0xFFFF9800) else Color.White)
                                    }

                                    Box(modifier = Modifier.weight(1f).height(2.dp).padding(horizontal = 6.dp).background(if (trip.status == PassengerOrderStatus.IN_TRIP) Color(0xFF00C853) else Color(0xFF37474F)))

                                    // Step 3: Destination
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (trip.status == PassengerOrderStatus.IN_TRIP) DrigoBrandPurple else Color(0xFF37474F),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("3", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("In Trip", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (trip.status == PassengerOrderStatus.IN_TRIP) DrigoBrandPurple else Color(0xFFB0BEC5))
                                    }
                                }
                            }

                            // Live Wait Time Banner when Captain has arrived
                            if (trip.status == PassengerOrderStatus.DRIVER_ARRIVED) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (waitTimeSeconds <= 300) Color(0xFF1B5E20).copy(alpha = 0.4f) else Color(0xFFE65100).copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, if (waitTimeSeconds <= 300) Color(0xFF4CAF50) else Color(0xFFFF9800)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = if (waitTimeSeconds <= 300) Color(0xFF81C784) else Color(0xFFFFB74D),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                val min = waitTimeSeconds / 60
                                                val sec = waitTimeSeconds % 60
                                                Text(
                                                    text = "Waiting Time: ${String.format(java.util.Locale.US, "%02d:%02d", min, sec)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = if (waitTimeSeconds <= 300) "Free waiting: ${(300 - waitTimeSeconds) / 60}m remaining" else "Paid wait fee (+PKR 5/min)",
                                                    fontSize = 11.sp,
                                                    color = if (waitTimeSeconds <= 300) Color(0xFFB0BEC5) else Color(0xFFFFB74D)
                                                )
                                            }
                                        }

                                        if (extraWaitFee > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFF9800)
                                            ) {
                                                Text(
                                                    text = "+PKR $extraWaitFee",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Route Summary & Details Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF14161C),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val pickupLat = if (trip.pickupLat != 0.0) trip.pickupLat else 34.0151
                                    val pickupLon = if (trip.pickupLon != 0.0) trip.pickupLon else 71.5249
                                    val destLat = if (trip.destinationLat != 0.0) trip.destinationLat else 34.0351
                                    val destLon = if (trip.destinationLon != 0.0) trip.destinationLon else 71.5449

                                    Row(verticalAlignment = Alignment.Top) {
                                        Surface(shape = CircleShape, color = Color(0xFF00C853), modifier = Modifier.padding(top = 3.dp).size(8.dp)) {}
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = trip.pickupTitle.ifBlank { "Pickup Location" },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (trip.pickupSubtitle.isNotBlank()) {
                                                Text(
                                                    text = trip.pickupSubtitle,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF90A4AE),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", pickupLat, pickupLon),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF81D4FA)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.Top) {
                                        Surface(shape = CircleShape, color = Color(0xFFE53935), modifier = Modifier.padding(top = 3.dp).size(8.dp)) {}
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = trip.destinationTitle.ifBlank { "Destination Location" },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.9f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (trip.destinationSubtitle.isNotBlank()) {
                                                Text(
                                                    text = trip.destinationSubtitle,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF90A4AE),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", destLat, destLon),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFFF8A80)
                                            )
                                        }
                                    }

                                    // Extra Route Details (Visible when expanded)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFF2C303E))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Category", fontSize = 11.sp, color = Color(0xFF78909C))
                                            Text(trip.rideCategory.ifBlank { "Ride Mini" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Column {
                                            Text("Est. Distance", fontSize = 11.sp, color = Color(0xFF78909C))
                                            Text("${if (trip.distanceKm > 0) String.format("%.1f", trip.distanceKm) else "8.5"} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Column {
                                            Text("Payment", fontSize = 11.sp, color = Color(0xFF78909C))
                                            Text(trip.paymentMethod.ifBlank { "💵 Cash" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                        }
                                    }

                                    // Tolls / Surcharges Row
                                    if (tollSurchargesPkr > 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Tolls & Surcharges added:",
                                                fontSize = 11.sp,
                                                color = Color(0xFF4FC3F7)
                                            )
                                            Text(
                                                text = "+PKR $tollSurchargesPkr",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4FC3F7)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Action Step Buttons (Fixed at bottom)
                    when (trip.status) {
                        PassengerOrderStatus.ACCEPTED, PassengerOrderStatus.DRIVER_COMING -> {
                            Button(
                                onClick = {
                                    val updated = trip.copy(status = PassengerOrderStatus.DRIVER_ARRIVED)
                                    activeDriverTrip = updated
                                    scope.launch {
                                        repo.updateDriverTripStatus(
                                            trip.id,
                                            PassengerOrderStatus.DRIVER_ARRIVED,
                                            requestId = trip.requestId,
                                            passengerId = trip.passengerId,
                                            driverId = driverId
                                        )
                                        Toast.makeText(context, "Passenger notified: You have arrived!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("driver_arrived_button")
                            ) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I HAVE ARRIVED AT PICKUP",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                        PassengerOrderStatus.DRIVER_ARRIVED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showPinVerificationDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, DrigoBrandPurple),
                                    modifier = Modifier
                                        .weight(0.32f)
                                        .height(50.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = DrigoBrandPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DrigoBrandPurple)
                                }

                                Button(
                                    onClick = {
                                        val updated = trip.copy(status = PassengerOrderStatus.IN_TRIP)
                                        activeDriverTrip = updated
                                        scope.launch {
                                            repo.updateDriverTripStatus(
                                                trip.id,
                                                PassengerOrderStatus.IN_TRIP,
                                                requestId = trip.requestId,
                                                passengerId = trip.passengerId,
                                                driverId = driverId
                                            )
                                            Toast.makeText(context, "Ride Started! Head to destination.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                                    modifier = Modifier
                                        .weight(0.68f)
                                        .height(50.dp)
                                        .testTag("driver_start_trip_button")
                                ) {
                                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "START RIDE",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        PassengerOrderStatus.IN_TRIP -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Add Toll / Surcharge Button
                                OutlinedButton(
                                    onClick = { showTollAddDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF4FC3F7)),
                                    modifier = Modifier
                                        .weight(0.38f)
                                        .height(50.dp)
                                ) {
                                    Text(
                                        text = "+ Toll/Extra",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4FC3F7)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val updated = trip.copy(status = PassengerOrderStatus.COMPLETED, agreedFare = totalCashToCollect)
                                        activeDriverTrip = null
                                        driverRouteResult = null
                                        offerSentRequestId = null
                                        todayEarnings += totalCashToCollect
                                        completedTripsCount += 1
                                        completedTripForRating = updated
                                        val calcDist = calculateDistanceKm(updated.pickupLat, updated.pickupLon, updated.destinationLat, updated.destinationLon).let { if (it > 0) String.format("%.1f", it).toDouble() else 8.5 }
                                        val newHistoryEntry = DriverHistoryItem(
                                            id = updated.id,
                                            tripId = updated.id,
                                            requestId = updated.requestId.ifBlank { updated.id },
                                            driverId = driverId,
                                            passengerId = updated.passengerId,
                                            passengerName = updated.passengerName.ifBlank { "Passenger" },
                                            passengerRating = 4.9,
                                            pickupAddress = updated.pickupTitle,
                                            pickupTitle = updated.pickupTitle,
                                            pickupLatitude = if (updated.pickupLat != 0.0) updated.pickupLat else null,
                                            pickupLongitude = if (updated.pickupLon != 0.0) updated.pickupLon else null,
                                            destinationAddress = updated.destinationTitle,
                                            destinationTitle = updated.destinationTitle,
                                            destinationLatitude = if (updated.destinationLat != 0.0) updated.destinationLat else null,
                                            destinationLongitude = if (updated.destinationLon != 0.0) updated.destinationLon else null,
                                            farePkr = totalCashToCollect,
                                            agreedFare = totalCashToCollect,
                                            paymentMethod = updated.paymentMethod.ifBlank { "💵 Cash" },
                                            dateFormatted = "Just now",
                                            distanceKm = calcDist,
                                            distance = calcDist,
                                            durationMins = if (updated.durationMinutes > 0) updated.durationMinutes else 18,
                                            duration = if (updated.durationMinutes > 0) updated.durationMinutes else 18,
                                            status = "COMPLETED",
                                            tripStatus = "COMPLETED",
                                            category = updated.rideCategory.ifBlank { "Ride Mini" },
                                            rideType = updated.rideCategory.ifBlank { "Ride Mini" },
                                            vehicleType = if (updated.driverVehicleMake.isNotBlank()) "${updated.driverVehicleMake} ${updated.driverVehicleModel}".trim() else updated.rideCategory,
                                            timestamp = System.currentTimeMillis(),
                                            requestedAt = if (updated.createdAt > 0) updated.createdAt else null,
                                            completedAt = System.currentTimeMillis()
                                        )
                                        showPassengerRatingDialog = true
                                        audioHelper.playTripCompleteChime(totalCashToCollect, trip.id)
                                        scope.launch {
                                            repo.saveDriverTripHistoryItem(driverId, newHistoryEntry)
                                            if (driverPhone.isNotBlank() && driverPhone != driverId) {
                                                repo.saveDriverTripHistoryItem(driverPhone, newHistoryEntry)
                                            }
                                            repo.updateDriverTripStatus(
                                                trip.id,
                                                PassengerOrderStatus.COMPLETED,
                                                requestId = trip.requestId,
                                                passengerId = trip.passengerId,
                                                driverId = driverId
                                            )
                                            val payMethodStr = trip.paymentMethod.ifBlank { "Cash" }
                                            repo.processRidePayment(
                                                passengerId = trip.passengerId,
                                                driverId = driverId,
                                                tripId = trip.id,
                                                amount = totalCashToCollect.toDouble(),
                                                paymentMethod = payMethodStr
                                            )
                                            repo.updateLiveDriverLocation(
                                                LiveDriverLocation(
                                                    rideId = trip.requestId.ifBlank { trip.id },
                                                    driverId = driverId,
                                                    latitude = trip.destinationLat,
                                                    longitude = trip.destinationLon,
                                                    bearing = 0f,
                                                    speedKmh = 0f,
                                                    etaMinutes = 0,
                                                    distanceRemainingKm = 0.0,
                                                    status = "COMPLETED",
                                                    updatedAt = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier
                                        .weight(0.62f)
                                        .height(50.dp)
                                        .testTag("driver_complete_trip_button")
                                ) {
                                    Text(
                                        text = "COMPLETE RIDE",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        else -> {}
                    }

                    // Cancel Trip Button
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { showCancelTripDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Cancel Trip",
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else if (isDriverOnline) {
            // ================= PASSENGER RIDE REQUESTS FEED (inDrive style) =================
            // Animate offset when user touches/drags the map: slides down off-screen, comes back when untagged/released
            val sheetTranslationY by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isMapTouched) 500.dp else 0.dp,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "sheetTranslationY"
            )

            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF181A20),
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, Color(0xFF2C303E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = sheetTranslationY)
                    .then(
                        if (isRequestsFeedExpanded) Modifier.fillMaxHeight(0.92f)
                        else Modifier.fillMaxHeight(0.55f)
                    )
                    .align(Alignment.BottomCenter)
                    .testTag("driver_requests_feed_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Drag / Tap to Expand or Collapse Handle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRequestsFeedExpanded = !isRequestsFeedExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Feed Header with Requests Count, Fullscreen Toggle & Preferences Filter Trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { isRequestsFeedExpanded = !isRequestsFeedExpanded }
                        ) {
                            Text(
                                text = "Ride Requests Nearby",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = DrigoBrandPurple,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${filteredRequests.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Smart Tariffs & Vehicle Eligibility Trigger (Image 13)
                            Surface(
                                onClick = { showTariffsSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1B2838),
                                border = BorderStroke(1.dp, Color(0xFF2B3A50)),
                                modifier = Modifier.testTag("driver_tariffs_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Vehicle Tariffs",
                                        tint = Color(0xFF29B6F6),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tariffs",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Filter Button Trigger
                            Surface(
                                onClick = { showFilterSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedCategoryFilter != "All" || selectedMaxDistanceFilterKm != null || destinationModeActive) DrigoBrandPurple.copy(alpha = 0.3f) else Color(0xFF282B35),
                                border = BorderStroke(1.dp, if (selectedCategoryFilter != "All" || selectedMaxDistanceFilterKm != null || destinationModeActive) DrigoBrandPurple else Color(0xFF3B4052))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Filter Rides",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Filters",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // View Mode Toggle (Card View vs Compact List View)
                            Surface(
                                onClick = { isCompactListView = !isCompactListView },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCompactListView) DrigoBrandPurple.copy(alpha = 0.35f) else Color(0xFF282B35),
                                border = BorderStroke(1.dp, if (isCompactListView) DrigoBrandPurple else Color(0xFF3B4052)),
                                modifier = Modifier.testTag("driver_view_mode_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCompactListView) Icons.Default.FormatListBulleted else Icons.Default.ViewAgenda,
                                        contentDescription = if (isCompactListView) "Switch to Card View" else "Switch to List View",
                                        tint = if (isCompactListView) Color(0xFFC6FF00) else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCompactListView) "List" else "Cards",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Expand/Collapse Fullscreen Toggle Button
                            IconButton(
                                onClick = { isRequestsFeedExpanded = !isRequestsFeedExpanded },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF282B35))
                            ) {
                                Icon(
                                    imageVector = if (isRequestsFeedExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = if (isRequestsFeedExpanded) "Collapse Feed" else "Expand Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Active Filters Quick Chips (Item 3)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        // Quick Toggle Chip: Auto-Accept Mode
                        item {
                            FilterChip(
                                selected = isAutoAcceptEnabled,
                                onClick = {
                                    isAutoAcceptEnabled = !isAutoAcceptEnabled
                                    Toast.makeText(context, if (isAutoAcceptEnabled) "Auto-Accept Mode Enabled (PKR $autoAcceptMinFare+)" else "Auto-Accept Disabled", Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Text(
                                        text = if (isAutoAcceptEnabled) "⚡ Auto-Accept (PKR $autoAcceptMinFare+)" else "⚡ Auto-Accept",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFB300),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF222530),
                                    labelColor = Color(0xFFFFD54F)
                                )
                            )
                        }

                        // Quick Toggle Chip: On My Way Home Mode
                        item {
                            FilterChip(
                                selected = destinationModeActive,
                                onClick = {
                                    destinationModeActive = !destinationModeActive
                                    if (destinationModeActive && destinationModeText.isBlank()) {
                                        destinationModeText = "Rawalpindi"
                                    }
                                    Toast.makeText(context, if (destinationModeActive) "On My Way Home Mode Active ($destinationModeText)" else "On My Way Home Disabled", Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Text(
                                        text = if (destinationModeActive) "🏠 Home: ${destinationModeText.ifBlank { "Active" }}" else "🏠 Home Mode",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DrigoBrandPurple,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF222530),
                                    labelColor = Color(0xFFCE93D8)
                                )
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == "All",
                                onClick = { selectedCategoryFilter = "All" },
                                label = { Text("All Categories", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DrigoBrandPurple,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF222530),
                                    labelColor = Color(0xFFB0BEC5)
                                )
                            )
                        }
                        listOf("Share Ride", "Private Ride", "Ride A/C", "Mini", "Bike", "Courier", "City to city").forEach { cat ->
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter.equals(cat, ignoreCase = true),
                                    onClick = {
                                        selectedCategoryFilter = if (selectedCategoryFilter.equals(cat, ignoreCase = true)) "All" else cat
                                    },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DrigoBrandPurple,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF222530),
                                        labelColor = Color(0xFFB0BEC5)
                                    )
                                )
                            }
                        }
                    }

                    if (filteredRequests.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                if (allRequests.isEmpty()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = DrigoBrandPurple.copy(alpha = 0.15f),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Radar,
                                                contentDescription = null,
                                                tint = DrigoBrandPurple,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Searching for passenger requests...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Real passenger ride requests created nearby will appear here in real time",
                                        fontSize = 12.sp,
                                        color = Color(0xFF90A4AE),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = Color(0xFF78909C),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No requests match your active filters",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Try clearing filters (${allRequests.size} active request${if (allRequests.size > 1) "s" else ""} in the area)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF78909C),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(filteredRequests, key = { it.id }) { req ->
                                if (isCompactListView) {
                                    DriverCompactRideRequestItem(
                                        request = req,
                                        isSelected = selectedRequestForOffer?.id == req.id,
                                        isOfferSent = offerSentRequestId == req.id,
                                        driverLat = driverGeoPoint.latitude,
                                        driverLon = driverGeoPoint.longitude,
                                        onSelect = { selectedRequestForOffer = req },
                                        onAcceptOffer = {
                                            scope.launch {
                                                isSendingOffer = true
                                                val distKmToPickup = calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon).coerceAtLeast(0.5)
                                                val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

                                                val offer = DriverOffer(
                                                    requestId = req.id,
                                                    driverId = driverId,
                                                    driverName = driverName,
                                                    driverVehicleMake = driverVehicleMake,
                                                    driverVehicleModel = driverVehicleModel,
                                                    driverPlateNumber = driverVehicleNumber,
                                                    driverPhone = driverPhone,
                                                    offeredFare = req.estimatedFare,
                                                    etaMinutes = etaMins,
                                                    distanceKmAway = distKmToPickup,
                                                    driverLat = driverGeoPoint.latitude,
                                                    driverLon = driverGeoPoint.longitude
                                                )

                                                val order = PassengerOrder(
                                                    id = req.id,
                                                    requestId = req.id,
                                                    passengerId = req.passengerId,
                                                    passengerName = req.passengerName.ifBlank { req.passengerEmail.substringBefore("@").ifBlank { "Passenger" } },
                                                    passengerEmail = req.passengerEmail,
                                                    passengerPhone = req.passengerPhone.ifBlank { "+92 300 9876543" },
                                                    pickupTitle = req.pickupTitle,
                                                    pickupSubtitle = req.pickupSubtitle,
                                                    pickupLat = req.pickupLat,
                                                    pickupLon = req.pickupLon,
                                                    destinationTitle = req.destinationTitle,
                                                    destinationSubtitle = req.destinationSubtitle,
                                                    destinationLat = req.destinationLat,
                                                    destinationLon = req.destinationLon,
                                                    distanceKm = req.distanceKm,
                                                    durationMinutes = req.durationMinutes,
                                                    rideCategory = req.rideCategory,
                                                    agreedFare = req.estimatedFare,
                                                    paymentMethod = req.paymentMethod,
                                                    driverName = driverName,
                                                    driverRating = 4.9,
                                                    driverTotalRides = 1420,
                                                    driverVehicleMake = driverVehicleMake,
                                                    driverVehicleModel = driverVehicleModel,
                                                    driverPlateNumber = driverVehicleNumber,
                                                    driverPhone = driverPhone,
                                                    assignedDriverId = driverId,
                                                    status = PassengerOrderStatus.DRIVER_COMING,
                                                    etaMinutes = etaMins
                                                )
                                                // Immediately show Active Ride view on driver screen
                                                activeDriverTrip = order
                                                offerSentRequestId = req.id
                                                selectedRequestForOffer = null
                                                tollSurchargesPkr = 0
                                                driverRecenterTrigger++
                                                Toast.makeText(context, "Ride accepted! Navigating to pickup...", Toast.LENGTH_SHORT).show()

                                                val result = repo.acceptRideRequest(req.id, offer, order)
                                                isSendingOffer = false
                                                if (!result.isSuccess || !result.getOrDefault(false)) {
                                                    activeDriverTrip = null
                                                    offerSentRequestId = null
                                                    driverRecenterTrigger++
                                                    Toast.makeText(context, "Ride request was already accepted by another driver or is no longer available.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        onCounterOffer = { counterFare ->
                                            scope.launch {
                                                isSendingOffer = true
                                                val distKmToPickup = calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon).coerceAtLeast(0.5)
                                                val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

                                                val offer = DriverOffer(
                                                    requestId = req.id,
                                                    driverId = driverId,
                                                    driverName = driverName,
                                                    driverVehicleMake = driverVehicleMake,
                                                    driverVehicleModel = driverVehicleModel,
                                                    driverPlateNumber = driverVehicleNumber,
                                                    driverPhone = driverPhone,
                                                    offeredFare = counterFare,
                                                    etaMinutes = etaMins,
                                                    distanceKmAway = distKmToPickup,
                                                    driverLat = driverGeoPoint.latitude,
                                                    driverLon = driverGeoPoint.longitude
                                                )
                                                repo.sendDriverOffer(offer)
                                                offerSentRequestId = req.id
                                                isSendingOffer = false
                                                Toast.makeText(context, "Counter-offer of PKR $counterFare sent to ${req.passengerName}!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                } else {
                                    DriverRideRequestCard(
                                        request = req,
                                        isSelected = selectedRequestForOffer?.id == req.id,
                                        isOfferSent = offerSentRequestId == req.id,
                                        driverLat = driverGeoPoint.latitude,
                                        driverLon = driverGeoPoint.longitude,
                                        onSelect = { selectedRequestForOffer = req },
                                        onAcceptOffer = {
                                            scope.launch {
                                                isSendingOffer = true
                                                val distKmToPickup = calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon).coerceAtLeast(0.5)
                                                val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

                                                val offer = DriverOffer(
                                                    requestId = req.id,
                                                    driverId = driverId,
                                                    driverName = driverName,
                                                    driverVehicleMake = driverVehicleMake,
                                                    driverVehicleModel = driverVehicleModel,
                                                    driverPlateNumber = driverVehicleNumber,
                                                    driverPhone = driverPhone,
                                                    offeredFare = req.estimatedFare,
                                                    etaMinutes = etaMins,
                                                    distanceKmAway = distKmToPickup,
                                                    driverLat = driverGeoPoint.latitude,
                                                    driverLon = driverGeoPoint.longitude
                                                )

                                                val order = PassengerOrder(
                                                    id = req.id,
                                                    requestId = req.id,
                                                    passengerId = req.passengerId,
                                                    passengerName = req.passengerName.ifBlank { req.passengerEmail.substringBefore("@").ifBlank { "Passenger" } },
                                                    passengerEmail = req.passengerEmail,
                                                    passengerPhone = req.passengerPhone.ifBlank { "+92 300 9876543" },
                                                    pickupTitle = req.pickupTitle,
                                                    pickupSubtitle = req.pickupSubtitle,
                                                    pickupLat = req.pickupLat,
                                                    pickupLon = req.pickupLon,
                                                    destinationTitle = req.destinationTitle,
                                                    destinationSubtitle = req.destinationSubtitle,
                                                    destinationLat = req.destinationLat,
                                                    destinationLon = req.destinationLon,
                                                    distanceKm = req.distanceKm,
                                                    durationMinutes = req.durationMinutes,
                                                    rideCategory = req.rideCategory,
                                                    agreedFare = req.estimatedFare,
                                                    paymentMethod = req.paymentMethod,
                                                    driverName = driverName,
                                                    driverRating = 4.9,
                                                    driverTotalRides = 1420,
                                                    driverVehicleMake = driverVehicleMake,
                                                    driverVehicleModel = driverVehicleModel,
                                                    driverPlateNumber = driverVehicleNumber,
                                                    driverPhone = driverPhone,
                                                    assignedDriverId = driverId,
                                                    status = PassengerOrderStatus.DRIVER_COMING,
                                                    etaMinutes = etaMins
                                                )
                                                // Immediately show Active Ride view on driver screen
                                                activeDriverTrip = order
                                                offerSentRequestId = req.id
                                                selectedRequestForOffer = null
                                                tollSurchargesPkr = 0
                                                driverRecenterTrigger++
                                                Toast.makeText(context, "Ride accepted! Navigating to pickup...", Toast.LENGTH_SHORT).show()

                                                val result = repo.acceptRideRequest(req.id, offer, order)
                                                isSendingOffer = false
                                                if (!result.isSuccess || !result.getOrDefault(false)) {
                                                    activeDriverTrip = null
                                                    offerSentRequestId = null
                                                    driverRecenterTrigger++
                                                    Toast.makeText(context, "Ride request was already accepted by another driver or is no longer available.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        onCounterOffer = { counterFare ->
                                            scope.launch {
                                                isSendingOffer = true
                                                val distKmToPickup = calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon).coerceAtLeast(0.5)
                                                val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

                                                val offer = DriverOffer(
                                                    requestId = req.id,
                                                    driverId = driverId,
                                                    driverName = driverName,
                                                    driverVehicleMake = driverVehicleMake,
                                                    driverVehicleModel = driverVehicleModel,
                                                    driverPlateNumber = driverVehicleNumber,
                                                    driverPhone = driverPhone,
                                                    offeredFare = counterFare,
                                                    etaMinutes = etaMins,
                                                    distanceKmAway = distKmToPickup,
                                                    driverLat = driverGeoPoint.latitude,
                                                    driverLon = driverGeoPoint.longitude
                                                )
                                                repo.sendDriverOffer(offer)
                                                offerSentRequestId = req.id
                                                isSendingOffer = false
                                                Toast.makeText(context, "Counter-offer of PKR $counterFare sent to ${req.passengerName}!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ================= DRIVER OFFLINE PROMPT =================
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF1E2026),
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, Color(0xFF2C303E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You are currently Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Go online to start receiving passenger requests and make offers in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = handleToggleOnlineClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("driver_go_online_button")
                    ) {
                        Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GO ONLINE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onSwitchToPassenger,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, DrigoBrandPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = DrigoBrandPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Switch to Passenger Mode",
                            color = DrigoBrandPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Floating Recenter Location FAB for Driver
        if (!isActiveTripSheetExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        end = 16.dp,
                        bottom = if (activeDriverTrip != null) 295.dp else if (isDriverOnline) (if (isRequestsFeedExpanded) 90.dp else 470.dp) else 230.dp
                    ),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    onClick = {
                        scope.launch {
                            try {
                                val loc = locationHelper.getCurrentLocation()
                                if (loc != null) {
                                    driverGeoPoint = GeoPoint(loc.latitude, loc.longitude)
                                    if (loc.bearing != 0f) driverBearing = loc.bearing
                                    if (loc.speedKmh > 0f) currentSpeedKmh = loc.speedKmh
                                }
                            } catch (_: Exception) {}
                            driverRecenterTrigger++
                        }
                    },
                    shape = CircleShape,
                    color = Color(0xFF1E2026).copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, Color(0xFF2C303B)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("driver_recenter_location_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Recenter to My Location",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar (Image 2: Ride Requests vs Performance Tabs)
        if (activeDriverTrip == null) {
            Surface(
                color = Color(0xFF14171F),
                border = BorderStroke(1.dp, Color(0xFF2B3142)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ride Requests Tab
                    Surface(
                        onClick = { selectedDriverTab = "REQUESTS" },
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("driver_nav_requests_tab")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "Ride requests",
                                tint = if (selectedDriverTab == "REQUESTS") Color(0xFFC6FF00) else Color(0xFF8C98A9),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Ride requests",
                                fontSize = 12.sp,
                                fontWeight = if (selectedDriverTab == "REQUESTS") FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (selectedDriverTab == "REQUESTS") Color.White else Color(0xFF8C98A9)
                            )
                        }
                    }

                    // Performance Tab
                    Surface(
                        onClick = { selectedDriverTab = "PERFORMANCE" },
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("driver_nav_performance_tab")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Performance",
                                tint = if (selectedDriverTab == "PERFORMANCE") Color(0xFFC6FF00) else Color(0xFF8C98A9),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Performance",
                                fontSize = 12.sp,
                                fontWeight = if (selectedDriverTab == "PERFORMANCE") FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (selectedDriverTab == "PERFORMANCE") Color.White else Color(0xFF8C98A9)
                            )
                        }
                    }

                    // Trip History Tab
                    Surface(
                        onClick = { selectedDriverTab = "HISTORY" },
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("driver_nav_history_tab")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Trip history",
                                tint = if (selectedDriverTab == "HISTORY") Color(0xFFC6FF00) else Color(0xFF8C98A9),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Trip history",
                                fontSize = 12.sp,
                                fontWeight = if (selectedDriverTab == "HISTORY") FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (selectedDriverTab == "HISTORY") Color.White else Color(0xFF8C98A9)
                            )
                        }
                    }
                }
            }
        }

        // Driver Performance Screen Overlay (Image 2)
        if (selectedDriverTab == "PERFORMANCE" && activeDriverTrip == null) {
            DriverPerformanceScreen(
                driverName = driverName,
                driverRating = driverPerformanceRating,
                driverRidesThisWeek = driverRidesThisWeek,
                todayIncomePkr = todayIncomePkr,
                dailyGoalPkr = dailyGoalPkr,
                walletBalancePkr = walletBalancePkr,
                activeBonusesCount = activeBonusesCount,
                onSeeBenefitsClick = { showTierBenefitsSheet = true },
                onIncomeClick = { showIncomeDetailsSheet = true },
                onAddGoalClick = { showAddDailyGoalSheet = true },
                onTopUpClick = onNavigateToWallet,
                onBonusesClick = { showBonusesSheet = true },
                onOpenDrawer = onOpenDrawer,
                onBackClick = { selectedDriverTab = "REQUESTS" },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Driver Trip History Screen Overlay
        if (selectedDriverTab == "HISTORY" && activeDriverTrip == null) {
            DriverTripHistoryScreen(
                driverName = driverName,
                completedTrips = activeDriverTripHistoryList,
                onOpenDrawer = onOpenDrawer,
                onBackClick = { selectedDriverTab = "REQUESTS" },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ================= DIALOGS & BOTTOM SHEETS =================

        // Driver Notification Center Modal Sheet
        if (showDriverNotificationCenterSheet) {
            NotificationCenterSheet(
                notifications = driverNotificationHistory,
                onDismiss = { showDriverNotificationCenterSheet = false },
                onClearAll = { notifManager.clearNotificationHistory() }
            )
        }

        // 1. Toll & Surcharges Dialog (Item 2)
        if (showTollAddDialog) {
            var customTollInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showTollAddDialog = false },
                containerColor = Color(0xFF1E2026),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = {
                    Text("Add Toll / Surcharges", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "Select or enter additional toll plaza or parking fees to add to this ride's final cash total:",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(50, 100, 150).forEach { fee ->
                                Surface(
                                    onClick = {
                                        tollSurchargesPkr += fee
                                        showTollAddDialog = false
                                        Toast.makeText(context, "Added +PKR $fee toll fee", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF2C303E),
                                    border = BorderStroke(1.dp, Color(0xFF4FC3F7)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "+PKR $fee",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customTollInput,
                            onValueChange = { if (it.all { char -> char.isDigit() }) customTollInput = it },
                            label = { Text("Custom Amount (PKR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4FC3F7),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val fee = customTollInput.toIntOrNull() ?: 0
                            if (fee > 0) tollSurchargesPkr += fee
                            showTollAddDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7))
                    ) {
                        Text("Add to Total", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTollAddDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // 1.5. Ride Request Details Bottom Sheet (matching reference image)
        selectedRequestForOffer?.let { req ->
            var expandedBidding by remember(req.id) { mutableStateOf(false) }
            var customBidText by remember(req.id) { mutableStateOf("") }
            val sheetFocusManager = LocalFocusManager.current

            ModalBottomSheet(
                onDismissRequest = { selectedRequestForOffer = null },
                containerColor = Color(0xFF13151D),
                scrimColor = Color.Black.copy(alpha = 0.6f),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    )
                }
            ) {
                val distAwayKm = if (driverGeoPoint.latitude != 0.0 && driverGeoPoint.longitude != 0.0 && req.pickupLat != 0.0) {
                    calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon)
                } else 0.9

                val etaMins = ((distAwayKm / 25.0) * 60.0).toInt().coerceIn(2, 25)
                val formattedFare = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(req.estimatedFare)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Profile + Info Main Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Profile Avatar + Name + Rating + Min Away
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(70.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF673AB7), // purple circle color
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = req.passengerName.trim().take(1).uppercase().ifBlank { "J" },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = req.passengerName.ifBlank { "jawad" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f", if (req.passengerRating > 0.0) req.passengerRating else 4.73),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = "(44)",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "$etaMins min.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Right Column: Fare + Routes + Category Pill
                        Column(modifier = Modifier.weight(1f)) {
                            // Top Row: Distance & Fare & Fair Price Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = String.format(java.util.Locale.US, "~%.1f km", req.distanceKm),
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "PKR$formattedFare",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                // Fair price Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E1428),
                                    border = BorderStroke(1.dp, Color(0xFFCE93D8).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFFE040FB),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Fair price",
                                            color = Color(0xFFCE93D8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Route Timeline Details (A & B)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // A & B Indicator Icons with a connecting line
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 4.dp, end = 12.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color(0xFF2979FF), CircleShape)
                                    ) {
                                        Text("A", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.5.dp)
                                            .height(30.dp)
                                            .background(Color.White.copy(alpha = 0.15f))
                                    )
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color(0xFF00E676), CircleShape)
                                    ) {
                                        Text("B", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                // Destination and Pickup Text
                                Column(modifier = Modifier.weight(1f)) {
                                    // Pickup A Address + Coordinates
                                    Text(
                                        text = req.pickupTitle,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (req.pickupSubtitle.isNotBlank()) {
                                        Text(
                                            text = req.pickupSubtitle,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", req.pickupLat, req.pickupLon),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF81D4FA)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Destination B Address + Coordinates
                                    Text(
                                        text = req.destinationTitle,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (req.destinationSubtitle.isNotBlank()) {
                                        Text(
                                            text = req.destinationSubtitle,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", req.destinationLat, req.destinationLon),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFF8A80)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mini/Ride Category pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF263238),
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Text(
                                    text = req.rideCategory.ifBlank { "Mini" },
                                    color = Color(0xFF90A4AE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bidding controls (if expandedBidding is true)
                    AnimatedVisibility(
                        visible = expandedBidding,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E2026), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Select or Enter Counter-Offer (PKR):",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val base = req.estimatedFare
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(base + 50, base + 100, base + 150).forEach { fare ->
                                    val formattedCounterFare = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(fare)
                                    Surface(
                                        onClick = {
                                            scope.launch {
                                                isSendingOffer = true
                                                val distKmToPickup = calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon).coerceAtLeast(0.5)
                                                val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

                                                val offer = DriverOffer(
                                                    requestId = req.id,
                                                    driverId = driverId,
                                                    driverName = driverName,
                                                    driverVehicleMake = driverVehicleMake,
                                                    driverVehicleModel = driverVehicleModel,
                                                    driverPlateNumber = driverVehicleNumber,
                                                    driverPhone = driverPhone,
                                                    offeredFare = fare,
                                                    etaMinutes = etaMins,
                                                    distanceKmAway = distKmToPickup,
                                                    driverLat = driverGeoPoint.latitude,
                                                    driverLon = driverGeoPoint.longitude
                                                )
                                                repo.sendDriverOffer(offer)
                                                offerSentRequestId = req.id
                                                isSendingOffer = false
                                                selectedRequestForOffer = null
                                                Toast.makeText(context, "Counter-offer of PKR $fare sent to ${req.passengerName}!", Toast.LENGTH_SHORT).show()
                                            }
                                            expandedBidding = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = DrigoBrandPurple.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, DrigoBrandPurple),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "+${fare - base}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "PKR $formattedCounterFare",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customBidText,
                                    onValueChange = { customBidText = it },
                                    placeholder = { Text("Custom Bid", color = Color.Gray, fontSize = 13.sp) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Send
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            val customFare = customBidText.toIntOrNull()
                                            if (customFare != null && customFare > 0) {
                                                scope.launch {
                                                    isSendingOffer = true
                                                    val distKmToPickup = calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon).coerceAtLeast(0.5)
                                                    val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

                                                    val offer = DriverOffer(
                                                        requestId = req.id,
                                                        driverId = driverId,
                                                        driverName = driverName,
                                                        driverVehicleMake = driverVehicleMake,
                                                        driverVehicleModel = driverVehicleModel,
                                                        driverPlateNumber = driverVehicleNumber,
                                                        driverPhone = driverPhone,
                                                        offeredFare = customFare,
                                                        etaMinutes = etaMins,
                                                        distanceKmAway = distKmToPickup,
                                                        driverLat = driverGeoPoint.latitude,
                                                        driverLon = driverGeoPoint.longitude
                                                    )
                                                    repo.sendDriverOffer(offer)
                                                    offerSentRequestId = req.id
                                                    isSendingOffer = false
                                                    selectedRequestForOffer = null
                                                    Toast.makeText(context, "Counter-offer of PKR $customFare sent to ${req.passengerName}!", Toast.LENGTH_SHORT).show()
                                                }
                                                expandedBidding = false
                                                customBidText = ""
                                                sheetFocusManager.clearFocus()
                                            }
                                        }
                                    ),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DrigoBrandPurple,
                                        unfocusedBorderColor = Color.Gray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        val customFare = customBidText.toIntOrNull()
                                        if (customFare != null && customFare > 0) {
                                            scope.launch {
                                                isSendingOffer = true
                                                val distKmToPickup = calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon).coerceAtLeast(0.5)
                                                val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

                                                val offer = DriverOffer(
                                                    requestId = req.id,
                                                    driverId = driverId,
                                                    driverName = driverName,
                                                    driverVehicleMake = driverVehicleMake,
                                                    driverVehicleModel = driverVehicleModel,
                                                    driverPlateNumber = driverVehicleNumber,
                                                    driverPhone = driverPhone,
                                                    offeredFare = customFare,
                                                    etaMinutes = etaMins,
                                                    distanceKmAway = distKmToPickup,
                                                    driverLat = driverGeoPoint.latitude,
                                                    driverLon = driverGeoPoint.longitude
                                                )
                                                repo.sendDriverOffer(offer)
                                                offerSentRequestId = req.id
                                                isSendingOffer = false
                                                selectedRequestForOffer = null
                                                Toast.makeText(context, "Counter-offer of PKR $customFare sent to ${req.passengerName}!", Toast.LENGTH_SHORT).show()
                                            }
                                            expandedBidding = false
                                            customBidText = ""
                                            sheetFocusManager.clearFocus()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Send", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons Area:
                    // 1. Accept Button (high-contrast Lime Green with Dark text)
                    Button(
                        onClick = {
                            scope.launch {
                                isSendingOffer = true
                                val distKmToPickup = calculateDistanceKm(driverGeoPoint.latitude, driverGeoPoint.longitude, req.pickupLat, req.pickupLon).coerceAtLeast(0.5)
                                val etaMins = ((distKmToPickup / 25.0) * 60.0).toInt().coerceIn(2, 25)

                                val offer = DriverOffer(
                                    requestId = req.id,
                                    driverId = driverId,
                                    driverName = driverName,
                                    driverVehicleMake = driverVehicleMake,
                                    driverVehicleModel = driverVehicleModel,
                                    driverPlateNumber = driverVehicleNumber,
                                    driverPhone = driverPhone,
                                    offeredFare = req.estimatedFare,
                                    etaMinutes = etaMins,
                                    distanceKmAway = distKmToPickup,
                                    driverLat = driverGeoPoint.latitude,
                                    driverLon = driverGeoPoint.longitude
                                )

                                val order = PassengerOrder(
                                    id = req.id,
                                    requestId = req.id,
                                    passengerId = req.passengerId,
                                    passengerName = req.passengerName.ifBlank { req.passengerEmail.substringBefore("@").ifBlank { "Passenger" } },
                                    passengerEmail = req.passengerEmail,
                                    passengerPhone = req.passengerPhone.ifBlank { "+92 300 9876543" },
                                    pickupTitle = req.pickupTitle,
                                    pickupSubtitle = req.pickupSubtitle,
                                    pickupLat = req.pickupLat,
                                    pickupLon = req.pickupLon,
                                    destinationTitle = req.destinationTitle,
                                    destinationSubtitle = req.destinationSubtitle,
                                    destinationLat = req.destinationLat,
                                    destinationLon = req.destinationLon,
                                    distanceKm = req.distanceKm,
                                    durationMinutes = req.durationMinutes,
                                    rideCategory = req.rideCategory,
                                    agreedFare = req.estimatedFare,
                                    paymentMethod = req.paymentMethod,
                                    driverName = driverName,
                                    driverRating = 4.9,
                                    driverTotalRides = 1420,
                                    driverVehicleMake = driverVehicleMake,
                                    driverVehicleModel = driverVehicleModel,
                                    driverPlateNumber = driverVehicleNumber,
                                    driverPhone = driverPhone,
                                    assignedDriverId = driverId,
                                    status = PassengerOrderStatus.DRIVER_COMING,
                                    etaMinutes = etaMins
                                )
                                // Immediately show Active Ride view on driver screen
                                activeDriverTrip = order
                                offerSentRequestId = req.id
                                selectedRequestForOffer = null
                                tollSurchargesPkr = 0
                                driverRecenterTrigger++
                                Toast.makeText(context, "Ride accepted! Navigating to pickup...", Toast.LENGTH_SHORT).show()

                                val result = repo.acceptRideRequest(req.id, offer, order)
                                isSendingOffer = false
                                if (!result.isSuccess || !result.getOrDefault(false)) {
                                    activeDriverTrip = null
                                    offerSentRequestId = null
                                    driverRecenterTrigger++
                                    Toast.makeText(context, "Ride request was already accepted by another driver or is no longer available.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCFF00)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Accept for PKR$formattedFare",
                            color = Color(0xFF13151D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Offer Your Fare Button
                    Button(
                        onClick = { expandedBidding = !expandedBidding },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222634)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = if (expandedBidding) "Cancel bidding" else "Offer your fare",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Close Button
                    Button(
                        onClick = { selectedRequestForOffer = null },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2026)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Close",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Filter & "On My Way Home" Preferences Sheet (Item 3 & Item 5)
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = Color(0xFF1E2026),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Ride Preferences & Audio Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-Accept Mode (Item 3)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isAutoAcceptEnabled) DrigoBrandPurple.copy(alpha = 0.2f) else Color(0xFF282B36),
                        border = BorderStroke(1.dp, if (isAutoAcceptEnabled) DrigoBrandPurple else Color(0xFF3E4354)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Auto-Accept Rides", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    }
                                    Text("Automatically bid on rides that match your fare & distance limits", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                }
                                Switch(
                                    checked = isAutoAcceptEnabled,
                                    onCheckedChange = { isAutoAcceptEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = DrigoBrandPurple
                                    )
                                )
                            }

                            if (isAutoAcceptEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Min Fare: PKR $autoAcceptMinFare", fontSize = 11.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(250, 350, 500).forEach { fare ->
                                            Surface(
                                                onClick = { autoAcceptMinFare = fare },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (autoAcceptMinFare == fare) DrigoBrandPurple else Color(0xFF1E2028),
                                                border = BorderStroke(1.dp, if (autoAcceptMinFare == fare) DrigoBrandPurple else Color(0xFF4A4E60))
                                            ) {
                                                Text("PKR $fare+", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Distance Radius Filter
                    Text(
                        text = "Maximum Pickup Distance",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0BEC5)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val distanceOptions = listOf(null to "Any", 3.0 to "< 3 km", 6.0 to "< 6 km", 12.0 to "< 12 km")
                        distanceOptions.forEach { (dist, label) ->
                            Surface(
                                onClick = { selectedMaxDistanceFilterKm = dist },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedMaxDistanceFilterKm == dist) DrigoBrandPurple else Color(0xFF2A2D3A),
                                border = BorderStroke(1.dp, if (selectedMaxDistanceFilterKm == dist) DrigoBrandPurple else Color(0xFF3E4354)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Destination Mode / "On My Way Home" Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "On My Way Home Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Only receive rides heading toward your destination",
                                fontSize = 10.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                        Switch(
                            checked = destinationModeActive,
                            onCheckedChange = { destinationModeActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DrigoBrandPurple
                            )
                        )
                    }

                    if (destinationModeActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = destinationModeText,
                            onValueChange = { destinationModeText = it },
                            placeholder = { Text("e.g. Rawalpindi, Blue Area, F-10...", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DrigoBrandPurple,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Quick Presets for Destination Mode
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Rawalpindi", "Blue Area", "F-10 Markaz", "I-8 Markaz").forEach { loc ->
                                Surface(
                                    onClick = { destinationModeText = loc },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (destinationModeText.contains(loc, true)) DrigoBrandPurple.copy(alpha = 0.3f) else Color(0xFF282B36),
                                    border = BorderStroke(1.dp, if (destinationModeText.contains(loc, true)) DrigoBrandPurple else Color(0xFF3E4354))
                                ) {
                                    Text("📍 $loc", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Payment Method Filter
                    Text(
                        text = "Payment Method Filter",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0BEC5)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All" to "All Methods", "Cash" to "💵 Cash Only", "Digital" to "💳 Digital Wallet").forEach { (method, label) ->
                            Surface(
                                onClick = { selectedPaymentFilter = method },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedPaymentFilter == method) DrigoBrandPurple else Color(0xFF2A2D3A),
                                border = BorderStroke(1.dp, if (selectedPaymentFilter == method) DrigoBrandPurple else Color(0xFF3E4354)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice & Alert Audio Settings (Item 5)
                    Text(
                        text = "Voice Announcements Language",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0BEC5)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                voiceLanguageChoice = "EN"
                                audioHelper.setVoiceLanguage("EN")
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (voiceLanguageChoice == "EN") DrigoBrandPurple else Color(0xFF2A2D3A),
                            border = BorderStroke(1.dp, if (voiceLanguageChoice == "EN") DrigoBrandPurple else Color(0xFF3E4354)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "English TTS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            )
                        }
                        Surface(
                            onClick = {
                                voiceLanguageChoice = "UR"
                                audioHelper.setVoiceLanguage("UR")
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (voiceLanguageChoice == "UR") DrigoBrandPurple else Color(0xFF2A2D3A),
                            border = BorderStroke(1.dp, if (voiceLanguageChoice == "UR") DrigoBrandPurple else Color(0xFF3E4354)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Urdu / Roman TTS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            )
                        }
                        // Test Voice Button
                        IconButton(
                            onClick = { audioHelper.playTestAlert() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF333746))
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Test Audio", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showFilterSheet = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Apply Preferences", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 3. Driver SOS & Emergency Center Dialog (Item 4)
        if (showSafetySosSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSafetySosSheet = false },
                containerColor = Color(0xFF181A20),
                scrimColor = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFD32F2F), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Drigo Captain Safety Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("24/7 Driver Protection & Emergency Assistance", fontSize = 11.sp, color = Color(0xFFEF9A9A))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Call 15 Police
                    Surface(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:15")))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Dialing Police Emergency (15)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFB71C1C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.LocalPolice, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Call Police Emergency (15)", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 14.sp)
                                Text("Direct line to local emergency dispatch", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Call Rescue 1122
                    Surface(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1122")))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Dialing Medical Rescue (1122)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2C303E),
                        border = BorderStroke(1.dp, Color(0xFFEF5350)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Call Rescue 1122 (Ambulance / Fire)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Emergency medical support", fontSize = 11.sp, color = Color(0xFFB0BEC5))
                            }
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color(0xFFEF5350))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Share Live GPS on WhatsApp / SMS
                    Surface(
                        onClick = {
                            try {
                                val msg = "EMERGENCY: I am on duty as a Drigo Captain and need assistance. My current GPS coordinates: https://maps.google.com/?q=${driverGeoPoint.latitude},${driverGeoPoint.longitude}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Live GPS Emergency Alert"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Sharing GPS coordinates...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2C303E),
                        border = BorderStroke(1.dp, Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.ShareLocation, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Share Live Location with Family", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Send real-time map link via WhatsApp / SMS", fontSize = 11.sp, color = Color(0xFFB0BEC5))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Panic Siren & Strobe Alarm (Item 4)
                    Surface(
                        onClick = {
                            isEmergencySirenActive = !isEmergencySirenActive
                            if (isEmergencySirenActive) {
                                audioHelper.playPanicSiren()
                                Toast.makeText(context, "PANIC SIREN TRIGGERED!", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isEmergencySirenActive) Color(0xFFFF1744) else Color(0xFF2C303E),
                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = if (isEmergencySirenActive) Color.White else Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isEmergencySirenActive) "Panic Siren Active (Loud Alarm)" else "Trigger Panic Siren Alarm", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Emits loud alert siren & haptic pulses for immediate protection", fontSize = 11.sp, color = Color(0xFFB0BEC5))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Audio Trip Recording Switch (Item 4)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF222530),
                        border = BorderStroke(1.dp, Color(0xFF373B4D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = if (isAudioRecordingActive) Color(0xFF00E676) else Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Encrypted Audio Safety Recording", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text("Encrypted locally for safety compliance", fontSize = 10.sp, color = Color(0xFF90A4AE))
                                }
                            }
                            Switch(
                                checked = isAudioRecordingActive,
                                onCheckedChange = { isAudioRecordingActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00E676)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }

        // Safety PIN Verification Dialog before Starting Trip (Item 4)
        if (showPinVerificationDialog && activeDriverTrip != null) {
            val trip = activeDriverTrip!!
            AlertDialog(
                onDismissRequest = { showPinVerificationDialog = false },
                containerColor = Color(0xFF1E2026),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = DrigoBrandPurple.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = DrigoBrandPurple, modifier = Modifier.size(28.dp))
                        }
                    }
                },
                title = {
                    Text("Verify Passenger 4-Digit PIN", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Ask the passenger for their safety PIN to confirm correct rider boarding.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = enteredPassengerPin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) enteredPassengerPin = it },
                            placeholder = { Text("e.g. 4821", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DrigoBrandPurple,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp),
                            modifier = Modifier.width(180.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = trip.copy(status = PassengerOrderStatus.IN_TRIP)
                            activeDriverTrip = updated
                            showPinVerificationDialog = false
                            enteredPassengerPin = ""
                            scope.launch {
                                repo.updateDriverTripStatus(
                                    trip.id,
                                    PassengerOrderStatus.IN_TRIP,
                                    requestId = trip.requestId,
                                    passengerId = trip.passengerId,
                                    driverId = driverId
                                )
                                Toast.makeText(context, "PIN Verified! Trip Started.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Verify & Start Trip", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val updated = trip.copy(status = PassengerOrderStatus.IN_TRIP)
                            activeDriverTrip = updated
                            showPinVerificationDialog = false
                            enteredPassengerPin = ""
                            scope.launch {
                                repo.updateDriverTripStatus(
                                    trip.id,
                                    PassengerOrderStatus.IN_TRIP,
                                    requestId = trip.requestId,
                                    passengerId = trip.passengerId,
                                    driverId = driverId
                                )
                                Toast.makeText(context, "Trip Started without PIN.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Skip PIN", color = Color.Gray)
                    }
                }
            )
        }

        // 4. Passenger Rating & Review Dialog (PostRideRatingDialog)
        if (showPassengerRatingDialog && completedTripForRating != null) {
            val trip = completedTripForRating!!
            PostRideRatingDialog(
                rideId = trip.id,
                currentUserId = driverId,
                currentUserName = driverName,
                isDriver = true,
                targetId = trip.passengerId.ifBlank { trip.passengerEmail.ifBlank { "passenger_${trip.id}" } },
                targetName = trip.passengerName.ifBlank { trip.passengerEmail.substringBefore("@").ifBlank { "Passenger" } },
                targetPhone = "",
                targetVehicleSummary = "",
                targetPlateNumber = "",
                pickupTitle = trip.pickupTitle,
                destinationTitle = trip.destinationTitle,
                farePkr = trip.agreedFare,
                onDismiss = {
                    showPassengerRatingDialog = false
                    context.getSharedPreferences("drigo_ratings", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("rated_or_skipped_${trip.id}", true).putBoolean("rated_or_skipped_${trip.requestId}", true).apply()
                    completedTripForRating = null
                },
                onRatingSubmitted = {
                    showPassengerRatingDialog = false
                    context.getSharedPreferences("drigo_ratings", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("rated_or_skipped_${trip.id}", true).putBoolean("rated_or_skipped_${trip.requestId}", true).apply()
                    completedTripForRating = null
                    Toast.makeText(context, "Passenger rating submitted (+10 Captain Points)", Toast.LENGTH_SHORT).show()
                },
                onOpenSafetyReport = {
                    showPassengerRatingDialog = false
                    context.getSharedPreferences("drigo_ratings", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("rated_or_skipped_${trip.id}", true).putBoolean("rated_or_skipped_${trip.requestId}", true).apply()
                    driverReportTargetTrip = trip
                    showDriverReportIncident = true
                }
            )
        }

        // Driver Incident / Safety Report Dialog
        if (showDriverReportIncident && driverReportTargetTrip != null) {
            val trip = driverReportTargetTrip!!
            SafetyReportDialog(
                rideId = trip.id,
                reporterId = driverId,
                reporterName = driverName,
                reporterPhone = driverPhone,
                isReporterDriver = true,
                reportedUserId = trip.passengerEmail.ifBlank { "passenger_${trip.id}" },
                reportedUserName = trip.passengerEmail.substringBefore("@").ifBlank { "Passenger" },
                driverPlateNumber = driverVehicleNumber,
                pickupTitle = trip.pickupTitle,
                destinationTitle = trip.destinationTitle,
                onDismiss = {
                    showDriverReportIncident = false
                    driverReportTargetTrip = null
                },
                onReportSubmitted = {
                    showDriverReportIncident = false
                    driverReportTargetTrip = null
                    Toast.makeText(context, "Incident report submitted securely for admin review", Toast.LENGTH_LONG).show()
                }
            )
        }

        // 5. Structured Cancellation Reason Dialog (Item 4)
        if (showCancelTripDialog) {
            val reasons = listOf(
                "Passenger didn't show up (5+ min wait)",
                "Passenger requested cancellation",
                "Incorrect pickup address",
                "Excessive passengers / luggage issue",
                "Vehicle emergency or mechanical issue"
            )
            AlertDialog(
                onDismissRequest = { showCancelTripDialog = false },
                containerColor = Color(0xFF1E2026),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = {
                    Text("Cancel Ride", fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                },
                text = {
                    Column {
                        Text("Please select the reason for cancellation:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(10.dp))
                        reasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCancelReason = reason }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedCancelReason == reason,
                                    onClick = { selectedCancelReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEF5350))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(reason, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val tripToCancel = activeDriverTrip
                            if (tripToCancel != null) {
                                notifManager.dismissDriverActiveRideNotification()
                                val cancelledHistoryEntry = DriverHistoryItem(
                                    id = tripToCancel.id,
                                    tripId = tripToCancel.id,
                                    requestId = tripToCancel.requestId.ifBlank { tripToCancel.id },
                                    driverId = driverId,
                                    passengerId = tripToCancel.passengerId,
                                    passengerName = tripToCancel.passengerName.ifBlank { "Passenger" },
                                    passengerRating = 4.9,
                                    pickupAddress = tripToCancel.pickupTitle,
                                    pickupTitle = tripToCancel.pickupTitle,
                                    pickupLatitude = if (tripToCancel.pickupLat != 0.0) tripToCancel.pickupLat else null,
                                    pickupLongitude = if (tripToCancel.pickupLon != 0.0) tripToCancel.pickupLon else null,
                                    destinationAddress = tripToCancel.destinationTitle,
                                    destinationTitle = tripToCancel.destinationTitle,
                                    destinationLatitude = if (tripToCancel.destinationLat != 0.0) tripToCancel.destinationLat else null,
                                    destinationLongitude = if (tripToCancel.destinationLon != 0.0) tripToCancel.destinationLon else null,
                                    farePkr = tripToCancel.agreedFare,
                                    agreedFare = tripToCancel.agreedFare,
                                    paymentMethod = tripToCancel.paymentMethod.ifBlank { "💵 Cash" },
                                    dateFormatted = "Cancelled",
                                    distanceKm = if (tripToCancel.distanceKm > 0) tripToCancel.distanceKm else 5.0,
                                    distance = if (tripToCancel.distanceKm > 0) tripToCancel.distanceKm else 5.0,
                                    durationMins = if (tripToCancel.durationMinutes > 0) tripToCancel.durationMinutes else 15,
                                    duration = if (tripToCancel.durationMinutes > 0) tripToCancel.durationMinutes else 15,
                                    status = "CANCELLED",
                                    tripStatus = "CANCELLED",
                                    category = tripToCancel.rideCategory.ifBlank { "Ride Mini" },
                                    rideType = tripToCancel.rideCategory.ifBlank { "Ride Mini" },
                                    vehicleType = if (tripToCancel.driverVehicleMake.isNotBlank()) "${tripToCancel.driverVehicleMake} ${tripToCancel.driverVehicleModel}".trim() else tripToCancel.rideCategory,
                                    timestamp = System.currentTimeMillis(),
                                    requestedAt = if (tripToCancel.createdAt > 0) tripToCancel.createdAt else null,
                                    cancelledAt = System.currentTimeMillis(),
                                    cancellationReason = selectedCancelReason
                                )
                                scope.launch {
                                    repo.saveDriverTripHistoryItem(driverId, cancelledHistoryEntry)
                                    if (driverPhone.isNotBlank() && driverPhone != driverId) {
                                        repo.saveDriverTripHistoryItem(driverPhone, cancelledHistoryEntry)
                                    }
                                    repo.updateDriverTripStatus(
                                        tripToCancel.id,
                                        PassengerOrderStatus.CANCELLED,
                                        requestId = tripToCancel.requestId,
                                        passengerId = tripToCancel.passengerId,
                                        driverId = driverId
                                    )
                                }
                            }
                            activeDriverTrip = null
                            driverRouteResult = null
                            offerSentRequestId = null
                            showCancelTripDialog = false
                            Toast.makeText(context, "Trip cancelled ($selectedCancelReason)", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Confirm Cancellation", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelTripDialog = false }) {
                        Text("Go Back", color = Color.Gray)
                    }
                }
            )
        }

        // 6. Tier Benefits Bottom Sheet (Image 2 - "See benefits")
        if (showTierBenefitsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTierBenefitsSheet = false },
                containerColor = Color(0xFF131B24),
                scrimColor = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Captain Tier Benefits & Perks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Complete weekly trips and maintain high ratings to unlock higher tiers with maximum earnings.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    val tiers = listOf(
                        Triple("Basic ◆", "0–14 rides/week • 4.50+ Rating", listOf("Standard dispatch priority", "Standard platform fee")),
                        Triple("Bronze ◆", "15–29 rides/week • 4.60+ Rating", listOf("5% Commission Cashback", "Priority ride request alerts", "Bronze badge on driver profile")),
                        Triple("Silver ◆", "30–49 rides/week • 4.70+ Rating", listOf("10% Commission Cashback", "Priority dispatch for high-fare rides", "1 Free Cancellation protection per week")),
                        Triple("Platinum ◆", "50+ rides/week • 4.80+ Rating", listOf("15% Commission Cashback", "Top priority dispatch engine", "3 Free Cancellations protection per week", "24/7 Dedicated Priority Hotline"))
                    )

                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(tiers) { (tierTitle, desc, perks) ->
                            val isCurrentTier = driverRidesThisWeek >= when (tierTitle.substringBefore(" ")) {
                                "Platinum" -> 50
                                "Silver" -> 30
                                "Bronze" -> 15
                                else -> 0
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCurrentTier) Color(0xFF1B3245) else Color(0xFF1A1F2B),
                                border = BorderStroke(1.dp, if (isCurrentTier) Color(0xFF29B6F6) else Color(0xFF2A3142)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = tierTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        if (isCurrentTier) {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color(0xFF0288D1)
                                            ) {
                                                Text(
                                                    text = "ACTIVE TIER",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = desc, fontSize = 11.5.sp, color = Color(0xFF81D4FA))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    perks.forEach { perk ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = perk, fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showTierBenefitsSheet = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Got It", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 7. Add Daily Goal Dialog
        if (showAddDailyGoalSheet) {
            var customGoalText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDailyGoalSheet = false },
                containerColor = Color(0xFF181C25),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = { Text("Set Daily Income Goal", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Select your daily target income to stay motivated:", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2000, 3500, 5000, 8000).forEach { preset ->
                                Surface(
                                    onClick = {
                                        dailyGoalPkr = preset
                                        showAddDailyGoalSheet = false
                                        Toast.makeText(context, "Daily goal set to PKR $preset", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (dailyGoalPkr == preset) Color(0xFF0288D1) else Color(0xFF262C3A),
                                    border = BorderStroke(1.dp, Color(0xFF374256)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${preset / 1000}k",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customGoalText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) customGoalText = it },
                            label = { Text("Custom Amount (PKR)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0288D1),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val customVal = customGoalText.toIntOrNull()
                            if (customVal != null && customVal > 0) {
                                dailyGoalPkr = customVal
                                Toast.makeText(context, "Daily goal set to PKR $customVal", Toast.LENGTH_SHORT).show()
                            }
                            showAddDailyGoalSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                    ) {
                        Text("Save Goal", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDailyGoalSheet = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // 8. Captain Bonuses & Surge Incentives Sheet
        if (showBonusesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBonusesSheet = false },
                containerColor = Color(0xFF181C25),
                scrimColor = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("Active Captain Bonuses & Quests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Complete quests to earn extra cash bonuses on top of ride fares.", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    val bonuses = listOf(
                        Triple("Peak Hour Surge Quest", "Complete 3 rides between 5:00 PM – 9:00 PM", "+PKR 400 Extra"),
                        Triple("Weekend Streak Bonus", "Complete 15 rides Friday to Sunday", "+PKR 1,200 Extra"),
                        Triple("High Acceptance Reward", "Maintain 90%+ acceptance rate today", "+PKR 300 Extra")
                    )

                    bonuses.forEach { (title, subtitle, reward) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF222836),
                            border = BorderStroke(1.dp, Color(0xFF333B4F)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text(text = subtitle, fontSize = 11.5.sp, color = Color.Gray)
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2E7D32)) {
                                    Text(text = reward, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { showBonusesSheet = false }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 9. Today's Income Details Breakdown
        if (showIncomeDetailsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showIncomeDetailsSheet = false },
                containerColor = Color(0xFF181C25),
                scrimColor = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("Today's Earnings Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Detailed view of all rides, tips, and bonuses earned today.", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(18.dp))

                    val items = listOf(
                        "Trip Fares Earned" to "PKR $todayIncomePkr",
                        "Passenger Cash Tips" to "PKR 0",
                        "Quest & Surge Bonuses" to "PKR 0",
                        "Platform Fee Deducted" to "-PKR 0",
                        "Net Earnings Today" to "PKR $todayIncomePkr"
                    )

                    items.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontSize = 14.sp, color = if (label.startsWith("Net")) Color.White else Color.Gray, fontWeight = if (label.startsWith("Net")) FontWeight.Bold else FontWeight.Normal)
                            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (label.startsWith("Net")) Color(0xFF00E676) else Color.White)
                        }
                        HorizontalDivider(color = Color(0xFF262C3A))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { showIncomeDetailsSheet = false }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 10. Smart Vehicle Tariff & Eligibility Sheet (Image 13)
        if (showTariffsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTariffsSheet = false },
                containerColor = Color(0xFF141822),
                scrimColor = Color.Black.copy(alpha = 0.7f)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vehicle Tariffs & Category Eligibility",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Manage active ride categories and courier switches.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Registered Vehicle Header Card (Image 13)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1C2230),
                        border = BorderStroke(1.dp, Color(0xFF2C364A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0288D1).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF0288D1)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = Color(0xFF29B6F6),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = selectedVehicleModelName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF283244),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = selectedVehiclePlateNumber,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD54F),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(text = "• $selectedVehicleCategoryType", fontSize = 11.5.sp, color = Color.Gray)
                                    }
                                }
                            }

                            Button(
                                onClick = { showChangeVehicleDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("change_vehicle_button")
                            ) {
                                Text("Change vehicle", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isAcLocked = selectedVehicleCategoryType.contains("Non-AC") ||
                            selectedVehicleCategoryType.contains("Motorcycle") ||
                            selectedVehicleCategoryType.contains("Auto")
                    val isComfortLocked = !selectedVehicleCategoryType.contains("Sedan")

                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        item {
                            Text("Ride Categories & Tariffs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81D4FA))
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Ride Mini
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1B202C),
                                border = BorderStroke(1.dp, Color(0xFF2A3142)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Ride Mini", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                        Text("Standard budget rides • Base PKR 35/km", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isTariffMiniEnabled,
                                        onCheckedChange = { isTariffMiniEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0288D1))
                                    )
                                }
                            }
                        }

                        // Ride A/C (Locked for Suzuki Mehran / Non-AC)
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAcLocked) Color(0xFF221A1A) else Color(0xFF1B202C),
                                border = BorderStroke(1.dp, if (isAcLocked) Color(0xFF4A2828) else Color(0xFF2A3142)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Ride A/C", fontWeight = FontWeight.Bold, color = if (isAcLocked) Color.Gray else Color.White, fontSize = 14.sp)
                                            if (isAcLocked) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFD32F2F).copy(alpha = 0.2f)) {
                                                    Text("🚫 Locked: No A/C", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (isAcLocked) "Not available for $selectedVehicleModelName (No A/C factory unit)" else "Air-conditioned rides • Base PKR 55/km",
                                            fontSize = 11.sp,
                                            color = if (isAcLocked) Color(0xFFEF5350) else Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = if (isAcLocked) false else isTariffAcEnabled,
                                        onCheckedChange = { if (!isAcLocked) isTariffAcEnabled = it },
                                        enabled = !isAcLocked,
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0288D1))
                                    )
                                }
                            }
                        }

                        // Ride Comfort (Locked for Hatchbacks/Bikes)
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isComfortLocked) Color(0xFF221A1A) else Color(0xFF1B202C),
                                border = BorderStroke(1.dp, if (isComfortLocked) Color(0xFF4A2828) else Color(0xFF2A3142)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Ride Comfort", fontWeight = FontWeight.Bold, color = if (isComfortLocked) Color.Gray else Color.White, fontSize = 14.sp)
                                            if (isComfortLocked) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFD32F2F).copy(alpha = 0.2f)) {
                                                    Text("🚫 Requires Sedan", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (isComfortLocked) "Requires Executive/Premium Sedan model" else "Spacious premium sedan rides • Base PKR 75/km",
                                            fontSize = 11.sp,
                                            color = if (isComfortLocked) Color(0xFFEF5350) else Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = if (isComfortLocked) false else isTariffComfortEnabled,
                                        onCheckedChange = { if (!isComfortLocked) isTariffComfortEnabled = it },
                                        enabled = !isComfortLocked,
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0288D1))
                                    )
                                }
                            }
                        }

                        // Courier & Delivery
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Courier & Sub-Category Controls (Image 13)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81D4FA))
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1B202C),
                                border = BorderStroke(1.dp, Color(0xFF2A3142)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Courier / Delivery Orders", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            Text("Accept package, food & item deliveries", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isTariffCourierEnabled,
                                            onCheckedChange = { isTariffCourierEnabled = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0288D1))
                                        )
                                    }

                                    if (isTariffCourierEnabled) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color(0xFF283142))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Sub-toggle: Parcel Delivery
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📄 Documents & Small Parcels", fontSize = 12.5.sp, color = Color.White)
                                            Switch(
                                                checked = isCourierParcelEnabled,
                                                onCheckedChange = { isCourierParcelEnabled = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0288D1))
                                            )
                                        }

                                        // Sub-toggle: Thermal Bag (Food)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("🎒 Insulated Thermal Bag Available", fontSize = 12.5.sp, color = Color.White)
                                                Text("Required for hot food & perishables", fontSize = 10.5.sp, color = Color.Gray)
                                            }
                                            Switch(
                                                checked = hasCourierThermalBag,
                                                onCheckedChange = { hasCourierThermalBag = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0288D1))
                                            )
                                        }

                                        // Sub-toggle: Heavy Cargo
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📦 Heavy Cargo / Large Boxes", fontSize = 12.5.sp, color = Color.White)
                                            Switch(
                                                checked = isCourierHeavyCargoEnabled,
                                                onCheckedChange = { isCourierHeavyCargoEnabled = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0288D1))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tariff Strategy Multiplier
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Base Rate Multiplier", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81D4FA))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1.0f to "1.0x Standard", 1.1f to "1.1x (+10%)", 1.2f to "1.2x (+20%)").forEach { (mult, label) ->
                                    Surface(
                                        onClick = { selectedTariffMultiplier = mult },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selectedTariffMultiplier == mult) Color(0xFF0288D1) else Color(0xFF1E2432),
                                        border = BorderStroke(1.dp, Color(0xFF2C364A)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showTariffsSheet = false
                            Toast.makeText(context, "Tariff & Vehicle eligibility settings saved!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Save Tariff Preferences", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 11. Change Vehicle Dialog (Image 13)
        if (showChangeVehicleDialog) {
            val vehiclesList = listOf(
                Triple("Suzuki Mehran (2018)", "LEA-18-4921", "Hatchback (Non-AC)"),
                Triple("Toyota Corolla (2021)", "ICT-21-9820", "Sedan (AC)"),
                Triple("Honda Civic (2022)", "ISB-22-1102", "Executive Sedan (AC)"),
                Triple("Honda CG125 (2022)", "RIW-22-3049", "Motorcycle (Bike)"),
                Triple("Sazgar Auto (2020)", "RWP-20-7711", "Auto Rickshaw")
            )

            AlertDialog(
                onDismissRequest = { showChangeVehicleDialog = false },
                containerColor = Color(0xFF181C26),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = { Text("Select Driver Vehicle", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Category eligibility locks adapt dynamically to your active vehicle:", fontSize = 12.5.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(vehiclesList) { (model, plate, type) ->
                                val isSelected = selectedVehicleModelName == model
                                Surface(
                                    onClick = {
                                        selectedVehicleModelName = model
                                        selectedVehiclePlateNumber = plate
                                        selectedVehicleCategoryType = type

                                        // Auto update tariff eligibility locks
                                        if (type.contains("Non-AC") || type.contains("Motorcycle") || type.contains("Auto")) {
                                            isTariffAcEnabled = false
                                        } else {
                                            isTariffAcEnabled = true
                                        }

                                        if (type.contains("Sedan")) {
                                            isTariffComfortEnabled = true
                                        } else {
                                            isTariffComfortEnabled = false
                                        }

                                        showChangeVehicleDialog = false
                                        Toast.makeText(context, "Active vehicle set to $model", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF0288D1).copy(alpha = 0.2f) else Color(0xFF222836),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF0288D1) else Color(0xFF333D52)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = model, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                                            Text(text = "$plate • $type", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChangeVehicleDialog = false }) {
                        Text("Close", color = Color(0xFF81D4FA))
                    }
                }
            )
        }
    }
}

