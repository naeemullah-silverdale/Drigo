package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.example.data.model.DriverOffer
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.CityRideType
import com.example.ui.components.CityToCityPassengerFlow
import com.example.ui.components.CityToCityStep
import com.example.ui.components.InDriveFixedBottomBar
import com.example.ui.components.InDriveLimeGreen
import com.example.ui.components.InDriveRideOption
import com.example.ui.components.InDriveRideOptionsList
import com.example.ui.components.InDriveRouteTopCard
import com.example.ui.components.MapSelectionMode
import com.example.ui.components.PickupDestinationBottomCard
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.RideCategoryBentoCards
import com.example.ui.components.RideChatSheet
import com.example.ui.components.RouteTopLocationsPanel
import com.example.ui.components.UniversalSafetyModalSheet
import com.example.ui.components.SafetyReportDialog
import com.example.ui.theme.DrigoBrandPurple
import com.example.util.RideNotificationManager
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
    driverVerification: com.example.data.model.DriverVerification? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val routeService = remember { RouteService(context) }
    val locationHelper = remember { LocationHelper(context) }

    // Persistent Isolated Locations & Route States
    var selectedPickupLocation by remember {
        mutableStateOf(
            AppLocation(
                title = "Street Number 9, Shero Jahngi, Peshawar",
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

    // Passenger Bottom Tab Navigation & Orders State (Screenshots & My orders tab)
    var passengerNavTab by remember { mutableIntStateOf(0) } // 0 = Ride, 1 = My orders
    var passengerOrders by remember { mutableStateOf<List<PassengerOrder>>(emptyList()) }
    var incomingDriverOffer by remember { mutableStateOf<PassengerOrder?>(null) }

    // Active order tracking for passenger map (Live Car visualization)
    val activePassengerOrder = passengerOrders.firstOrNull {
        it.status == PassengerOrderStatus.ACCEPTED ||
        it.status == PassengerOrderStatus.DRIVER_ARRIVED ||
        it.status == PassengerOrderStatus.IN_TRIP
    }
    var passengerMapDriverLoc by remember { mutableStateOf<LiveDriverLocation?>(null) }

    val repo = remember { FirebaseRepository.getInstance(context) }
    val userRecord by repo.listenToUserRecord(user?.uid ?: "").collectAsState(initial = null)
    val passengerAccStatus = remember(userRecord) {
        com.example.data.model.parsePassengerAccountStatus(userRecord?.accountStatus, null)
    }

    LaunchedEffect(activePassengerOrder?.requestId, activePassengerOrder?.id) {
        val reqId = activePassengerOrder?.requestId?.ifBlank { activePassengerOrder?.id }
        if (reqId != null) {
            val repo = FirebaseRepository.getInstance(context)
            repo.listenToLiveDriverLocation(reqId).collectLatest { loc ->
                if (loc != null) {
                    passengerMapDriverLoc = loc
                }
            }
        } else {
            passengerMapDriverLoc = null
        }
    }

    // Live driver approaching simulation towards pickup for active accepted captain
    LaunchedEffect(activePassengerOrder?.id, activePassengerOrder?.status) {
        val order = activePassengerOrder
        if (order != null && order.status == PassengerOrderStatus.ACCEPTED) {
            val pickupPt = GeoPoint(order.pickupLat, order.pickupLon)
            val driverStartLat = if (passengerMapDriverLoc != null) {
                passengerMapDriverLoc!!.latitude
            } else if (order.pickupLat != 0.0) {
                order.pickupLat + 0.009
            } else {
                selectedPickupLocation.latitude + 0.009
            }

            val driverStartLon = if (passengerMapDriverLoc != null) {
                passengerMapDriverLoc!!.longitude
            } else if (order.pickupLon != 0.0) {
                order.pickupLon + 0.009
            } else {
                selectedPickupLocation.longitude + 0.009
            }
            val driverStartPt = GeoPoint(driverStartLat, driverStartLon)

            // Calculate route from driver position to passenger pickup location
            val routeToPickup = routeService.calculateRoute(
                startPoint = driverStartPt,
                destPoint = pickupPt,
                startAddress = "Captain Location",
                destinationAddress = order.pickupTitle
            )
            activeRoute = routeToPickup

            val points = routeToPickup.points
            if (points.isNotEmpty()) {
                val totalSteps = points.size
                for (i in 0 until totalSteps) {
                    val currentOrder = passengerOrders.firstOrNull { it.id == order.id }
                    if (currentOrder == null || currentOrder.status != PassengerOrderStatus.ACCEPTED) break

                    val currentPt = points[i]
                    val nextPt = if (i < totalSteps - 1) points[i + 1] else currentPt

                    val dLat = nextPt.latitude - currentPt.latitude
                    val dLon = nextPt.longitude - currentPt.longitude
                    val computedBearing = ((Math.toDegrees(Math.atan2(dLon, dLat)) + 360) % 360).toFloat()

                    val remainingFraction = 1f - (i.toFloat() / totalSteps.coerceAtLeast(1))
                    val remainingKm = (routeToPickup.distanceKm * remainingFraction).coerceAtLeast(0.1)
                    val remainingMin = maxOf(1, (routeToPickup.durationMinutes * remainingFraction).toInt())

                    val liveLoc = LiveDriverLocation(
                        rideId = order.requestId.ifBlank { order.id },
                        driverId = "driver_${order.id}",
                        latitude = currentPt.latitude,
                        longitude = currentPt.longitude,
                        bearing = computedBearing,
                        speedKmh = 35f,
                        etaMinutes = remainingMin,
                        distanceRemainingKm = remainingKm,
                        status = PassengerOrderStatus.ACCEPTED.name,
                        updatedAt = System.currentTimeMillis()
                    )
                    passengerMapDriverLoc = liveLoc

                    try {
                        val repo = FirebaseRepository.getInstance(context)
                        repo.updateLiveDriverLocation(liveLoc)
                    } catch (_: Exception) {}

                    kotlinx.coroutines.delay(1200)
                }

                val currentOrder = passengerOrders.firstOrNull { it.id == order.id }
                if (currentOrder != null && currentOrder.status == PassengerOrderStatus.ACCEPTED) {
                    val arrivedOrder = order.copy(status = PassengerOrderStatus.DRIVER_ARRIVED)
                    passengerOrders = passengerOrders.map { if (it.id == order.id) arrivedOrder else it }
                    try {
                        val repo = FirebaseRepository.getInstance(context)
                        repo.savePassengerOrder(arrivedOrder)
                        repo.updateRideRequestStatus(order.requestId, "DRIVER_ARRIVED")
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Notification Manager Instance
    val notifManager = remember(context) { RideNotificationManager.getInstance(context) }
    var lastKnownPassengerOrderStatus by remember { mutableStateOf<PassengerOrderStatus?>(null) }

    // Listen to active passenger order status changes and dispatch notifications
    LaunchedEffect(activePassengerOrder?.id, activePassengerOrder?.status) {
        val order = activePassengerOrder
        if (order != null && order.status != lastKnownPassengerOrderStatus) {
            when (order.status) {
                PassengerOrderStatus.ACCEPTED -> {
                    if (lastKnownPassengerOrderStatus != null) {
                        notifManager.notifyDriverAccepted(
                            driverName = order.driverName.ifBlank { "Captain" },
                            farePkr = order.agreedFare,
                            vehicleModel = "${order.driverVehicleColor} ${order.driverVehicleMake} ${order.driverVehicleModel}".trim(),
                            rideId = order.id
                        )
                    }
                }
                PassengerOrderStatus.DRIVER_ARRIVED -> {
                    notifManager.notifyDriverArrived(
                        driverName = order.driverName.ifBlank { "Captain" },
                        plateNumber = order.driverPlateNumber,
                        rideId = order.id
                    )
                }
                PassengerOrderStatus.IN_TRIP -> {
                    notifManager.notifyRideStarted(
                        destination = order.destinationTitle,
                        rideId = order.id
                    )
                }
                PassengerOrderStatus.COMPLETED -> {
                    notifManager.notifyRideCompleted(
                        farePkr = order.agreedFare,
                        rideId = order.id
                    )
                }
                PassengerOrderStatus.CANCELLED -> {
                    notifManager.notifyPassengerRideCancelled(
                        reason = "Trip ended",
                        rideId = order.id
                    )
                }
                else -> Unit
            }
            lastKnownPassengerOrderStatus = order.status
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
                        driverRating = 4.9,
                        driverTotalRides = 1420,
                        driverVehicleMake = latestOffer.driverVehicleMake,
                        driverVehicleModel = latestOffer.driverVehicleModel,
                        driverVehicleColor = "White",
                        driverPlateNumber = latestOffer.driverPlateNumber,
                        driverPhone = latestOffer.driverPhone,
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
        }
    }

    // Listen to passenger orders from Firebase Realtime Database & Firestore
    LaunchedEffect(user?.uid, user?.email) {
        val uid = user?.uid ?: ""
        val email = user?.email ?: ""
        val repo = FirebaseRepository.getInstance(context)
        repo.listenToPassengerOrders(uid, email).collectLatest { cloudOrders ->
            if (cloudOrders.isNotEmpty()) {
                val cloudIds = cloudOrders.map { it.id }.toSet()
                val localOnly = passengerOrders.filter { it.id !in cloudIds }
                passengerOrders = localOnly + cloudOrders
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
    var activeDriverTrip by remember { mutableStateOf<RideRequest?>(null) }

    // Listen for live driver requests when driver is online
    LaunchedEffect(isDriverOnline) {
        if (isDriverOnline) {
            val repo = FirebaseRepository.getInstance(context)
            repo.listenToRideRequests().collectLatest { reqList ->
                driverRideRequests = reqList
                if (activeDriverTrip == null && reqList.isNotEmpty()) {
                    activeDriverTrip = reqList.first()
                }
            }
        }
    }

    // Function to submit ride request entry to Firebase Database
    fun submitRideRequest() {
        scope.launch {
            isBookingInProgress = true
            val dest = selectedDestinationLocation ?: AppLocation(
                title = "Selected Destination",
                subtitle = "",
                latitude = selectedPickupLocation.latitude + 0.015,
                longitude = selectedPickupLocation.longitude + 0.015
            )
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
                pickupTitle = selectedPickupLocation.title,
                pickupSubtitle = selectedPickupLocation.subtitle,
                pickupLat = selectedPickupLocation.latitude,
                pickupLon = selectedPickupLocation.longitude,
                destinationTitle = dest.title,
                destinationSubtitle = dest.subtitle,
                destinationLat = dest.latitude,
                destinationLon = dest.longitude,
                rideCategory = cat,
                estimatedFare = finalFare,
                distanceKm = dist,
                durationMinutes = duration,
                status = "SEARCHING_DRIVERS",
                timestamp = System.currentTimeMillis()
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
                    }
                }
                MapSelectionMode.DESTINATION -> {
                    selectedDestinationLocation = tappedLocation
                    mapSelectionMode = MapSelectionMode.NONE
                    calculateAndSetRoute(selectedPickupLocation, tappedLocation)
                }
                MapSelectionMode.NONE -> {
                    // Tapping map directly without mode active sets/updates destination and keeps pickup preserved
                    selectedDestinationLocation = tappedLocation
                    calculateAndSetRoute(selectedPickupLocation, tappedLocation)
                }
            }
        }
    }

    fun swapLocations() {
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
                            selected = true,
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AltRoute, contentDescription = null) },
                            label = { Text("Active Requests", fontSize = 14.sp) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(horizontal = 10.dp)
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
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text("Trip History") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 12.dp)
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
                    .background(Color(0xFF14161B))
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
                                .background(Color(0xFF282B33))
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
                                driverCarTitle = activePassengerOrder?.let { "${it.driverName} (${it.driverPlateNumber})" } ?: "Captain Farhan (LEA-4521)",
                                recenterTrigger = recenterTrigger,
                                showCenterPickupPin = activeRoute == null,
                                mapSelectionMode = mapSelectionMode,
                                onMapTapped = { lat, lng -> onMapTapped(lat, lng) },
                                onCancelMapSelection = { mapSelectionMode = MapSelectionMode.NONE },
                                onWhereFromClick = { 
                                    cardInitialEditPickup = true
                                    showPickupDestinationCard = true 
                                },
                                onMapCenterChanged = { lat, lng ->
                                    if (activeRoute == null) {
                                        scope.launch {
                                            val (addrLine, areaName, city) = locationHelper.reverseGeocode(lat, lng)
                                            selectedPickupLocation = AppLocation(
                                                title = addrLine,
                                                subtitle = if (areaName.isNotBlank() && areaName != addrLine) "$areaName, $city" else city,
                                                latitude = lat,
                                                longitude = lng
                                            )
                                            isPickupExplicitlySet = true
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
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 50.dp, start = 14.dp, end = 14.dp)
                    )
                } else if (selectedDestinationLocation != null) {
                    // Top Bar with Hamburger Menu + Direct "From" & "To" Location Selector Card (visible when destination chosen)
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
                                cardInitialEditPickup = false
                                showPickupDestinationCard = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E2026).copy(alpha = 0.96f),
                            border = BorderStroke(1.2.dp, DrigoBrandPurple.copy(alpha = 0.6f)),
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
                                            cardInitialEditPickup = true
                                            showPickupDestinationCard = true
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "From: ${selectedPickupLocation.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = Color(0xFF353945), thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(4.dp))

                                // To Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            cardInitialEditPickup = false
                                            showPickupDestinationCard = true
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE53935),
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "To: ${selectedDestinationLocation!!.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                }
                            }
                        }
                    }
                } else {
                    // Initial State: Hamburger Menu button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 50.dp, start = 14.dp)
                    ) {
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
                        color = Color(0xFF1E2024).copy(alpha = 0.94f),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("recenter_location_btn_map_active")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Recenter to Current GPS Location",
                                tint = Color.White,
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
                                        color = Color(0xFF1E2026).copy(alpha = 0.96f),
                                        border = BorderStroke(1.dp, Color(0xFF2C303B)),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("floating_map_back_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = Color.White,
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
                                    color = Color(0xFF1E2026).copy(alpha = 0.96f),
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
                                    color = Color(0xFF1E2026).copy(alpha = 0.96f),
                                    border = BorderStroke(1.dp, Color(0xFF2C303B)),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("recenter_location_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = "Recenter to Current GPS Location",
                                            tint = Color.White,
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

                            // Dark Sleek Bottom Sheet matching Wallet AddMoneyBottomSheet styling
                            Surface(
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                color = Color(0xFF1E2026),
                                border = BorderStroke(1.dp, Color(0xFF2C303B)),
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
                                                .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
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
                                                scope.launch {
                                                    val repo = FirebaseRepository.getInstance(context)
                                                    repo.cancelPassengerOrder(order.id, order.requestId, user?.uid ?: "")
                                                    passengerOrders = passengerOrders.map { if (it.id == order.id) it.copy(status = PassengerOrderStatus.CANCELLED) else it }
                                                    passengerMapDriverLoc = null
                                                    activeRideRequestId = null
                                                    activeRoute = null
                                                    selectedDestinationLocation = null
                                                    snackbarHostState.showSnackbar("Ride cancelled")
                                                }
                                            }
                                        )
                                    } else if (activeRoute != null) {
                                        // ===== ROUTE ACTIVE: inDrive Full Ride Selection UI (Attachments 1, 2, 3) =====

                                        // Subtitle: "No traffic, lower prices"
                                        Text(
                                            text = "No traffic, lower prices",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFA0A6B5),
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
                                                    color = if (isSelected) Color(0xFF2C303B) else Color.Transparent,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSelected) DrigoBrandPurple else Color(0xFF333846)
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
                                                            color = if (isSelected) Color.White else Color(0xFFA0A6B5)
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
                                            // "Where To?" Search Entry Card
                                            Surface(
                                                onClick = {
                                                    cardInitialEditPickup = false
                                                    showPickupDestinationCard = true
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xFF232730),
                                                border = BorderStroke(1.dp, Color(0xFF333846)),
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
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            // 3-Card Bento Layout in Initial Discovery View
                                            RideCategoryBentoCards(
                                                selectedCategory = selectedRideCategory,
                                                onShareRideClick = {
                                                    selectedRideCategory = "Share Ride"
                                                    selectedTopCategory = "city"
                                                    cardInitialEditPickup = false
                                                    showPickupDestinationCard = true
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

                // Floating Chat Action button on Map when a ride is active
                if (activeRideRequestId != null) {
                    FloatingActionButton(
                        onClick = {
                            chatTripId = activeRideRequestId!!
                            chatPartnerName = "Captain Farhan"
                            chatPartnerRole = "Driver"
                            chatPartnerPhone = "+92 300 1234567"
                            chatPickupTitle = selectedPickupLocation.title
                            chatDestinationTitle = selectedDestinationLocation?.title ?: "Destination"
                            showChatSheet = true
                        },
                        containerColor = DrigoBrandPurple,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 110.dp, end = 16.dp)
                            .testTag("floating_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat with Driver",
                            modifier = Modifier.size(24.dp)
                        )
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
                    val repo = FirebaseRepository.getInstance(context)
                    scope.launch {
                        repo.cancelPassengerOrder(order.id, order.requestId, user?.uid ?: "")
                        passengerOrders = passengerOrders.map {
                            if (it.id == order.id) it.copy(status = PassengerOrderStatus.CANCELLED) else it
                        }
                        activeRideRequestId = null
                        snackbarHostState.showSnackbar("Ride order cancelled")
                    }
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
                modifier = modifier
            )
        }
    }

    // "Where From?" & "Where To?" Bottom Card
    if (showPickupDestinationCard) {
        PickupDestinationBottomCard(
            pickupLocation = selectedPickupLocation,
            destinationLocation = selectedDestinationLocation,
            initialEditingPickup = cardInitialEditPickup,
            initialWhereToText = if (cardInitialEditPickup) "" else (selectedDestinationLocation?.title ?: ""),
            onDismiss = { showPickupDestinationCard = false },
            onDestinationSelected = { destination ->
                selectedDestinationLocation = destination
                showPickupDestinationCard = false
                calculateAndSetRoute(selectedPickupLocation, destination)
            },
            onPickupSelected = { newPickup ->
                selectedPickupLocation = newPickup
                isPickupExplicitlySet = true
                showPickupDestinationCard = false
                if (selectedDestinationLocation != null) {
                    calculateAndSetRoute(newPickup, selectedDestinationLocation!!)
                }
            },
            onPickOnMap = { isPickup ->
                showPickupDestinationCard = false
                mapSelectionMode = if (isPickup) MapSelectionMode.PICKUP else MapSelectionMode.DESTINATION
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
            partnerName = if (userMode == UserMode.DRIVER) "Passenger" else (activeOrder?.driverName?.ifBlank { "Captain Farhan" } ?: "Driver Captain"),
            partnerPhone = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverPhone ?: "+92 300 1234567"),
            vehicleMake = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverVehicleMake ?: "Toyota"),
            vehicleModel = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverVehicleModel ?: "Corolla"),
            vehiclePlate = if (userMode == UserMode.DRIVER) "" else (activeOrder?.driverPlateNumber ?: "LEA-2024"),
            pickupAddress = activeOrder?.pickupTitle?.ifBlank { selectedPickupLocation.title } ?: selectedPickupLocation.title,
            destinationAddress = activeOrder?.destinationTitle?.ifBlank { selectedDestinationLocation?.title ?: "Not Set" } ?: (selectedDestinationLocation?.title ?: "Not Set"),
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

    // Incoming Driver Offer Dialog / Modal
    val currentOffer = incomingDriverOffer
    if (currentOffer != null && activePassengerOrder == null) {
        DriverOfferDialog(
            offer = currentOffer,
            onAccept = {
                val acceptedOrder = currentOffer.copy(status = PassengerOrderStatus.ACCEPTED)
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
                    status = PassengerOrderStatus.ACCEPTED.name,
                    updatedAt = System.currentTimeMillis()
                )
                passengerMapDriverLoc = initialDriverLoc

                // 3. Immediately set passenger active order
                passengerOrders = listOf(acceptedOrder) + passengerOrders.filter { it.id != acceptedOrder.id && it.requestId != acceptedOrder.requestId }

                // 4. Update backend in background
                val repo = FirebaseRepository.getInstance(context)
                scope.launch {
                    repo.savePassengerOrder(acceptedOrder)
                    repo.updateRideRequestStatus(acceptedOrder.requestId, "ACCEPTED")
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
}

/**
 * Dialog displaying incoming Driver Offer with Accept and Decline actions.
 * Matches inDrive offer acceptance workflow.
 */
@Composable
fun DriverOfferDialog(
    offer: PassengerOrder,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = Color(0xFF1B1D24),
        titleContentColor = Color.White,
        textContentColor = Color.White,
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
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = InDriveLimeGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, InDriveLimeGreen.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "~${offer.etaMinutes} min away",
                        color = Color.White,
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
                // Driver card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF222631),
                    border = BorderStroke(1.dp, Color(0xFF333A4C)),
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
                                    tint = Color.White,
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
                                    color = Color.White,
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
                                color = Color(0xFFA0A6B5),
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
                                color = Color(0xFFA0A6B5),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Fixed Cash / Wallet",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "PKR ${"%,d".format(offer.agreedFare)}",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    }
                }

                Text(
                    text = "Accepting confirms this captain and starts live map navigation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A6B5),
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
                border = BorderStroke(1.dp, Color(0xFF3B4152)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFA0A6B5)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("decline_driver_offer_btn")
            ) {
                Text(
                    text = "Decline Offer",
                    color = Color(0xFFA0A6B5),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    )
}

/**
 * Modern Active Captain Assigned Card for the passenger bottom sheet.
 * Displays live ETA, distance, captain info, vehicle plate, fare, and quick actions (Call, Chat, SOS, Cancel).
 */
@Composable
fun ActiveCaptainAssignedCard(
    order: PassengerOrder,
    liveLocation: LiveDriverLocation?,
    onCallCaptain: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSafety: () -> Unit,
    onCancelRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val etaMins = liveLocation?.etaMinutes ?: order.etaMinutes
    val distKm = liveLocation?.distanceRemainingKm ?: 1.2
    val isArrived = order.status == PassengerOrderStatus.DRIVER_ARRIVED || (liveLocation?.status == PassengerOrderStatus.DRIVER_ARRIVED.name)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Live Status & ETA Header Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isArrived) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFF1E293B),
            border = BorderStroke(1.dp, if (isArrived) Color(0xFF00E676) else InDriveLimeGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isArrived) Color(0xFF00E676) else InDriveLimeGreen,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArrived) "CAPTAIN ARRIVED" else "CAPTAIN COMING",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isArrived) Color(0xFF00E676) else InDriveLimeGreen,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222631)
                ) {
                    Text(
                        text = if (isArrived) "At Pickup Location" else "~$etaMins min (${String.format(Locale.US, "%.1f", distKm)} km)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. Captain Profile & Vehicle Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF222631),
            border = BorderStroke(1.dp, Color(0xFF333A4C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DrigoBrandPurple.copy(alpha = 0.3f),
                        border = BorderStroke(2.dp, InDriveLimeGreen),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.driverName.ifBlank { "Captain" },
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
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
                                        text = "${order.driverRating}",
                                        color = Color(0xFFFFB300),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${order.driverVehicleColor} ${order.driverVehicleMake} ${order.driverVehicleModel}".trim().ifBlank { "Toyota Corolla White" },
                            color = Color(0xFFA0A6B5),
                            fontSize = 12.sp
                        )
                    }

                    // License Plate Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF181A20),
                        border = BorderStroke(1.dp, Color(0xFF3E4455))
                    ) {
                        Text(
                            text = order.driverPlateNumber.ifBlank { "LEA-4521" },
                            fontWeight = FontWeight.ExtraBold,
                            color = InDriveLimeGreen,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF333A4C).copy(alpha = 0.6f))

                // Agreed Fare & Pickup Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Agreed Fare (Cash / Wallet)",
                            color = Color(0xFFA0A6B5),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "PKR ${"%,d".format(order.agreedFare)}",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2C303B),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = order.rideCategory,
                            color = Color(0xFFA0A6B5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 3. Quick Action Buttons Row: Call, Chat, Safety, Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Call Button
            Button(
                onClick = onCallCaptain,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C303B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("call_captain_btn"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Chat Button
            Button(
                onClick = onOpenChat,
                colors = ButtonDefaults.buttonColors(containerColor = InDriveLimeGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("chat_captain_btn"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Safety / SOS Button
            IconButton(
                onClick = onOpenSafety,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF2C303B), RoundedCornerShape(14.dp))
                    .testTag("safety_captain_btn")
            ) {
                Icon(Icons.Default.Security, contentDescription = "Safety", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            }

            // Cancel Button
            IconButton(
                onClick = onCancelRide,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF2C303B), RoundedCornerShape(14.dp))
                    .testTag("cancel_captain_ride_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel Ride", tint = Color(0xFFA0A6B5), modifier = Modifier.size(20.dp))
            }
        }
    }
}
