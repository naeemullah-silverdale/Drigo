package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.MapSelectionMode
import com.example.ui.components.PickupDestinationBottomCard
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.RideChatSheet
import com.example.ui.components.RouteTopLocationsPanel
import com.example.ui.theme.DrigoBrandPurple
import com.example.viewmodel.UserMode
import com.google.firebase.auth.FirebaseUser
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
    var selectedRideCategory by remember { mutableStateOf<String?>("Share Ride") }
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
            val cat = selectedRideCategory ?: "Share Ride"
            val dist = activeRoute?.distanceKm ?: 5.0
            val fare = when (cat) {
                "Share Ride" -> 80 + (dist * 25).toInt()
                "Book Car" -> 150 + (dist * 50).toInt()
                else -> 70 + (dist * 20).toInt()
            }
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
                estimatedFare = fare,
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
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF282B33))
                    .testTag("passenger_home_screen")
            ) {
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

                // Top Persistent Route Locations Panel OR Top Location Selector Card (ONLY visible when destination is chosen or route is active)
                if (activeRoute != null) {
                    val route = activeRoute!!
                    RouteTopLocationsPanel(
                        pickupAddress = selectedPickupLocation.title,
                        destinationAddress = selectedDestinationLocation?.title ?: route.destinationAddress,
                        distanceKm = route.distanceKm,
                        durationMinutes = route.durationMinutes,
                        isCalculating = isCalculatingRoute,
                        onEditPickupClick = {
                            cardInitialEditPickup = true
                            showPickupDestinationCard = true
                        },
                        onEditDestinationClick = {
                            cardInitialEditPickup = false
                            showPickupDestinationCard = true
                        },
                        onClearRouteClick = {
                            activeRoute = null
                            selectedDestinationLocation = null
                            recenterTrigger++
                        },
                        onSwapLocationsClick = {
                            swapLocations()
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
                    // Initial State: Only Hamburger Menu button visible, NO From/To location card
                    Surface(
                        onClick = { scope.launch { drawerState.open() } },
                        shape = CircleShape,
                        color = DrigoBrandPurple,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 50.dp, start = 14.dp)
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
                        // Floating Recenter Button positioned strictly above the tolls banner / bottom card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.End
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

                        // Intercity Tolls Banner: ONLY visible if From and To are different cities
                        val showTollsBanner = isIntercityTrip(
                            from = selectedPickupLocation,
                            to = selectedDestinationLocation,
                            route = activeRoute
                        )

                        if (showTollsBanner) {
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
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Tolls will be paid separately\nto the driver",
                                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 18.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // White Bottom Card for Booking & Ride Options
                        Surface(
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            color = Color.White,
                            shadowElevation = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ride_booking_sheet")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                // Dynamic Calculated Fares based on distance
                                val dist = activeRoute?.distanceKm ?: 5.0
                                val shareFare = 80 + (dist * 25).toInt()
                                val carFare = 150 + (dist * 50).toInt()
                                val parcelFare = 70 + (dist * 20).toInt()

                                if (activeRoute == null) {
                                    // "Where To?" Search Box with Magenta Outline
                                    Surface(
                                        onClick = { 
                                            cardInitialEditPickup = false
                                            showPickupDestinationCard = true 
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(2.dp, DrigoBrandPurple),
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("where_to_search_box")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = DrigoBrandPurple,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Where To?",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = DrigoBrandPurple,
                                                fontSize = 17.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                // Grid of 3 Ride Service Cards (Share Ride, Parcel, Book Car)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (activeRoute != null) 140.dp else 165.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Left Column: 2 Cards (Share Ride & Parcel)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // "Share Ride" Card
                                        val isShareSelected = selectedRideCategory == "Share Ride" || (selectedRideCategory == null && activeRoute != null)
                                        Surface(
                                            onClick = {
                                                selectedRideCategory = "Share Ride"
                                                if (activeRoute == null) {
                                                    cardInitialEditPickup = false
                                                    showPickupDestinationCard = true
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                if (isShareSelected) 2.2.dp else 1.2.dp,
                                                if (isShareSelected) DrigoBrandPurple else Color(0xFFDCDFE6)
                                            ),
                                            color = if (isShareSelected) Color(0xFFFAF2F8) else Color.White,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .testTag("share_ride_card")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Share Ride",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DrigoBrandPurple,
                                                        fontSize = 14.sp
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.People,
                                                        contentDescription = null,
                                                        tint = DrigoBrandPurple,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    Text(
                                                        text = "Smart Match",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp
                                                    )
                                                    Text(
                                                        text = "Rs. $shareFare",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DrigoBrandPurple
                                                    )
                                                }
                                            }
                                        }

                                        // "Parcel" Card
                                        val isParcelSelected = selectedRideCategory == "Parcel"
                                        Surface(
                                            onClick = {
                                                selectedRideCategory = "Parcel"
                                                if (activeRoute == null) {
                                                    cardInitialEditPickup = false
                                                    showPickupDestinationCard = true
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                if (isParcelSelected) 2.2.dp else 1.2.dp,
                                                if (isParcelSelected) DrigoBrandPurple else Color(0xFFDCDFE6)
                                            ),
                                            color = if (isParcelSelected) Color(0xFFFAF2F8) else Color.White,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .testTag("parcel_card")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Parcel",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DrigoBrandPurple,
                                                        fontSize = 14.sp
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.LocalShipping,
                                                        contentDescription = null,
                                                        tint = DrigoBrandPurple,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    Text(
                                                        text = "Door Delivery",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp
                                                    )
                                                    Text(
                                                        text = "Rs. $parcelFare",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DrigoBrandPurple
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Right Column: Tall "Book Car" Card
                                    val isCarSelected = selectedRideCategory == "Book Car"
                                    Surface(
                                        onClick = {
                                            selectedRideCategory = "Book Car"
                                            if (activeRoute == null) {
                                                cardInitialEditPickup = false
                                                showPickupDestinationCard = true
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            if (isCarSelected) 2.2.dp else 1.2.dp,
                                            if (isCarSelected) DrigoBrandPurple else Color(0xFFDCDFE6)
                                        ),
                                        color = if (isCarSelected) Color(0xFFFAF2F8) else Color.White,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .testTag("book_car_card")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Book Car",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DrigoBrandPurple,
                                                    fontSize = 15.sp
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsCar,
                                                    contentDescription = null,
                                                    tint = DrigoBrandPurple,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = "Dedicated AC ride with premium driver",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = DrigoBrandPurple.copy(alpha = 0.12f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "Rs. $carFare",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DrigoBrandPurple,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (activeRoute != null) {
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Payment Method & Options Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF0F1F5),
                                            modifier = Modifier.clickable {}
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Payments,
                                                    contentDescription = "Payment Method",
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Cash",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF212529)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DrigoBrandPurple.copy(alpha = 0.1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalOffer,
                                                    contentDescription = "Discount",
                                                    tint = DrigoBrandPurple,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Standard Rates",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DrigoBrandPurple
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val currentCategory = selectedRideCategory ?: "Share Ride"
                                    val currentFare = when (currentCategory) {
                                        "Share Ride" -> shareFare
                                        "Book Car" -> carFare
                                        else -> parcelFare
                                    }

                                    // Check if there is an active confirmed ride booking
                                    if (activeRideRequestId != null) {
                                        // --- ACTIVE CONFIRMED BOOKING & DRIVER CARD ---
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFFF3E5F5),
                                            border = BorderStroke(1.5.dp, DrigoBrandPurple),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("active_ride_card")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
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
                                                            text = "CONFIRMED • DRIVER ASSIGNED",
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "ID: #${activeRideRequestId?.takeLast(6)?.uppercase()}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = DrigoBrandPurple,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Driver & Vehicle Details
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = DrigoBrandPurple,
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Person,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "Captain Farhan",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF212121),
                                                                fontSize = 15.sp
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "★ 4.9",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFF57F17),
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                        Text(
                                                            text = "Toyota Corolla Altis • LEB-492 (White)",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF616161),
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    Text(
                                                        text = "Rs. $currentFare",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DrigoBrandPurple
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Action Buttons (Chat & Call)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            chatTripId = activeRideRequestId!!
                                                            chatPartnerName = "Captain Farhan"
                                                            chatPartnerRole = "Driver"
                                                            chatPartnerPhone = "+92 300 1234567"
                                                            chatPickupTitle = selectedPickupLocation.title
                                                            chatDestinationTitle = selectedDestinationLocation?.title ?: "Destination"
                                                            showChatSheet = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(44.dp)
                                                            .testTag("passenger_chat_btn")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Chat,
                                                            contentDescription = "Chat",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Chat with Driver",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            chatTripId = activeRideRequestId!!
                                                            chatPartnerName = "Captain Farhan"
                                                            chatPartnerRole = "Driver"
                                                            chatPartnerPhone = "+92 300 1234567"
                                                            chatPickupTitle = selectedPickupLocation.title
                                                            chatDestinationTitle = selectedDestinationLocation?.title ?: "Destination"
                                                            showChatSheet = true
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = BorderStroke(1.2.dp, DrigoBrandPurple),
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .testTag("passenger_call_btn"),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Call,
                                                            contentDescription = "Call Driver",
                                                            tint = DrigoBrandPurple,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                TextButton(
                                                    onClick = {
                                                        activeRideRequestId = null
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Ride booking cancelled.")
                                                        }
                                                    },
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                                ) {
                                                    Text(
                                                        text = "Cancel Booking",
                                                        color = Color(0xFFD32F2F),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // "Find Drivers" Action Button
                                        Button(
                                            onClick = {
                                                if (selectedRideCategory == null) {
                                                    selectedRideCategory = "Share Ride"
                                                }
                                                showBookingDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("find_drivers_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.DirectionsCar,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Find Drivers ($currentCategory)",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                }
                                                Text(
                                                    text = "Rs. $currentFare",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // Bottom Magenta Accent Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(DrigoBrandPurple)
                        )
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
            "Share Ride" -> 80 + (dist * 25).toInt()
            "Book Car" -> 150 + (dist * 50).toInt()
            else -> 70 + (dist * 20).toInt()
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
