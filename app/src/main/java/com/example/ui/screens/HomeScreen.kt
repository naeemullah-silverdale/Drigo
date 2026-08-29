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
import com.example.data.remote.FirebaseRepository
import com.example.ui.components.InDriveFixedBottomBar
import com.example.ui.components.InDriveRideOption
import com.example.ui.components.InDriveRideOptionsList
import com.example.ui.components.InDriveRouteTopCard
import com.example.ui.components.MapSelectionMode
import com.example.ui.components.PickupDestinationBottomCard
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.RideCategoryBentoCards
import com.example.ui.components.RideChatSheet
import com.example.ui.components.RouteTopLocationsPanel
import com.example.ui.theme.DrigoBrandPurple
import com.example.viewmodel.UserMode
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
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
            val cat = selectedRideCategory ?: "Private AC"
            val dist = activeRoute?.distanceKm ?: 5.0
            val calculatedBaseFare = when (cat) {
                "Ride A/C", "Private AC" -> 160 + (dist * 45).toInt()
                "Mini" -> 120 + (dist * 35).toInt()
                "Moto" -> 60 + (dist * 18).toInt()
                "Ride", "Private Non-AC" -> 110 + (dist * 35).toInt()
                "City to City", "City to city" -> 450 + (dist * 45).toInt()
                "Couriers", "Parcel", "Parcel Delivery" -> 70 + (dist * 22).toInt()
                "Freight" -> 300 + (dist * 60).toInt()
                "Share Ride" -> 80 + (dist * 25).toInt()
                "Book Car", "Book a Car" -> 160 + (dist * 45).toInt()
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
                snackbarHostState.showSnackbar("Ride Request #$shortId saved to Firebase! Searching for drivers...")
            } else {
                snackbarHostState.showSnackbar("Ride Request created locally. Connecting to Firebase...")
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(310.dp)
            ) {
                // Header in Brand Magenta (#9E0059)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DrigoBrandPurple)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Column {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (userMode == UserMode.DRIVER) Icons.Default.Badge else Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = user?.displayName?.ifBlank { if (userMode == UserMode.DRIVER) "Drigo Driver" else "Drigo Rider" }
                                ?: (user?.email?.substringBefore("@") ?: "Drigo User"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Switch Card in Drawer
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, DrigoBrandPurple.copy(alpha = 0.4f)),
                    color = DrigoBrandPurple.copy(alpha = 0.06f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userMode == UserMode.PASSENGER) "Switch to Driver" else "Switch to Passenger",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple
                            )
                            Text(
                                text = if (userMode == UserMode.PASSENGER) "Accept rides & earn" else "Book rides & delivery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
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

                Spacer(modifier = Modifier.height(8.dp))

                if (userMode == UserMode.PASSENGER) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = DrigoBrandPurple) },
                        label = { Text("Book a Ride", fontWeight = FontWeight.SemiBold) },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("Share Ride") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedRideCategory = "Share Ride"
                            showBookingDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                        label = { Text("Parcel Delivery") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedRideCategory = "Parcel"
                            showBookingDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                } else {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = DrigoBrandPurple) },
                        label = { Text("Driver Dashboard", fontWeight = FontWeight.SemiBold) },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AltRoute, contentDescription = null) },
                        label = { Text("Active Requests") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 12.dp)
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

                Spacer(modifier = Modifier.weight(1f))

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
    ) {
        if (userMode == UserMode.PASSENGER) {
            // ================= PASSENGER MODE =================
            BoxWithConstraints(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF282B33))
                    .testTag("passenger_home_screen")
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
                AnimatedVisibility(
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
                AnimatedVisibility(
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
                                            activeRoute = null
                                            selectedDestinationLocation = null
                                            recenterTrigger++
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

                        val defaultPeekHeight = if (activeRoute != null) 450.dp.coerceAtMost(screenMaxHeight * 0.74f) else 340.dp
                        val targetHeight = if (isRideSheetExpanded) (screenMaxHeight * 0.92f) else defaultPeekHeight
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
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 8.dp)
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

                                if (activeRoute != null) {
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
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp),
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
                                            }
                                        )
                                    }
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
                                if (activeRoute != null) {
                                    if (activeRideRequestId == null) {
                                        // inDrive Fixed Bottom Bar (Attachment 3)
                                        InDriveFixedBottomBar(
                                            currentFare = effectiveCustomFare,
                                            autoAcceptOffer = autoAcceptOffer,
                                            onAutoAcceptChange = { autoAcceptOffer = it },
                                            onFindDriversClick = {
                                                showBookingDialog = true
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
                                    }
                                } else {
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
            // ================= DRIVER MODE =================
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF282B33))
                    .testTag("driver_home_screen")
            ) {
                RealOsmMapView(
                    modifier = Modifier.fillMaxSize(),
                    currentLatitude = userLocationData?.latitude,
                    currentLongitude = userLocationData?.longitude,
                    fromLocation = selectedPickupLocation,
                    toLocation = null,
                    routeResult = null,
                    recenterTrigger = recenterTrigger,
                    showCenterPickupPin = false
                )

                // Top Magenta Header Accent Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(DrigoBrandPurple)
                        .align(Alignment.TopCenter)
                )

                // Menu button
                Surface(
                    onClick = { scope.launch { drawerState.open() } },
                    shape = CircleShape,
                    color = DrigoBrandPurple,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 56.dp)
                        .size(46.dp)
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

                // Driver Mode Active Status Banner
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isDriverOnline) Color(0xFF2E7D32) else Color(0xFF424754),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 58.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDriverOnline) Color(0xFF81C784) else Color(0xFFB0BEC5),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDriverOnline) "YOU'RE ONLINE" else "YOU'RE OFFLINE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Driver Top Wallet Quick Button
                Surface(
                    onClick = onNavigateToWallet,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E2026).copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, Color(0xFF00A859)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 56.dp)
                        .height(44.dp)
                        .testTag("driver_top_wallet_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Driver Wallet",
                            tint = Color(0xFF00A859),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wallet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }

                // Driver Bottom Action Controls Card
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp,
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
                        if (isDriverOnline) {
                            val activeTrip = activeDriverTrip ?: driverRideRequests.firstOrNull()
                            if (activeTrip != null) {
                                // Active Passenger Request Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF3E5F5),
                                    border = BorderStroke(1.5.dp, DrigoBrandPurple),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("driver_active_request_card")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF2E7D32)
                                            ) {
                                                Text(
                                                    text = "PASSENGER RIDE REQUEST",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "Rs. ${activeTrip.estimatedFare}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DrigoBrandPurple
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = DrigoBrandPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = activeTrip.passengerName.ifBlank { "Passenger (Naeem Ullah)" },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF212121)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📍 Pickup: ${activeTrip.pickupTitle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF424242),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "🎯 Dropoff: ${activeTrip.destinationTitle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF424242),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Driver Chat Action Button
                                        Button(
                                            onClick = {
                                                chatTripId = activeTrip.id
                                                chatPartnerName = activeTrip.passengerName.ifBlank { "Naeem Ullah" }
                                                chatPartnerRole = "Passenger"
                                                chatPartnerPhone = "+92 300 9876543"
                                                chatPickupTitle = activeTrip.pickupTitle
                                                chatDestinationTitle = activeTrip.destinationTitle
                                                showChatSheet = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                                .testTag("driver_chat_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = "Chat with Passenger",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Chat with Passenger",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                Text(
                                    text = "Searching for passenger requests near ${selectedPickupLocation.title}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        } else {
                            Text(
                                text = "Go online to receive ride bookings in your area",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Button(
                            onClick = onToggleDriverOnline,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDriverOnline) Color(0xFFD32F2F) else DrigoBrandPurple
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isDriverOnline) "Go Offline" else "Go Online (Driver)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { onSwitchUserMode(UserMode.PASSENGER) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, DrigoBrandPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = DrigoBrandPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Switch back to Passenger Mode",
                                color = DrigoBrandPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
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
}
