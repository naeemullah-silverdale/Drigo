package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.RouteResult
import com.example.ui.theme.DrigoBrandPurple
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs
import kotlin.math.max

enum class MapSelectionMode {
    NONE,
    PICKUP,
    DESTINATION
}

enum class MapTileLayer {
    STREET,
    DETAILED_TOPO,
    SATELLITE
}

// Cached marker drawables to prevent Bitmap allocation on every frame / gesture
private var cachedUserLocationMarkerDrawable: Drawable? = null
private var cachedDestinationMarkerDrawable: Drawable? = null
private var cachedPickupMarkerDrawable: Drawable? = null
private var cachedDriverCarMarkerDrawable: Drawable? = null

/**
 * Data model for displaying available nearby drivers on the passenger map with custom fare badges.
 */
data class NearbyDriverMarkerData(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val fareText: String, // e.g. "380 Rs"
    val isPrimaryOption: Boolean = false, // Lime green car & badge vs Dark charcoal
    val bearing: Float = 35f,
    val driverName: String = "",
    val driverOffer: com.example.data.model.DriverOffer? = null
)

@SuppressLint("ClickableViewAccessibility")
@Composable
fun RealOsmMapView(
    modifier: Modifier = Modifier,
    currentLatitude: Double?,
    currentLongitude: Double?,
    fromLocation: com.example.data.AppLocation? = null,
    toLocation: com.example.data.AppLocation? = null,
    routeResult: RouteResult? = null,
    driverCarLocation: GeoPoint? = null,
    driverCarBearing: Float? = null,
    driverCarTitle: String? = null,
    passengerRequestsOnMap: List<com.example.data.model.RideRequest> = emptyList(),
    onPassengerRequestMarkerClick: ((com.example.data.model.RideRequest) -> Unit)? = null,
    nearbyDriverMarkers: List<NearbyDriverMarkerData> = emptyList(),
    onNearbyDriverMarkerClick: ((NearbyDriverMarkerData) -> Unit)? = null,
    recenterTrigger: Int = 0,
    showCenterPickupPin: Boolean = false,
    showMapLayerSwitcher: Boolean = true,
    mapSelectionMode: MapSelectionMode = MapSelectionMode.NONE,
    onMapTapped: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onCancelMapSelection: () -> Unit = {},
    onWhereFromClick: () -> Unit = {},
    onMapCenterChanged: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onMapInteractionChange: (isInteracting: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize osmdroid configuration for low-end device optimization
    LaunchedEffect(Unit) {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        config.cacheMapTileCount = 12.toShort()
        config.cacheMapTileOvershoot = 0.toShort()
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var locationMarker by remember { mutableStateOf<Marker?>(null) }
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }
    var pickupMarker by remember { mutableStateOf<Marker?>(null) }
    var driverCarMarker by remember { mutableStateOf<Marker?>(null) }
    var requestMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var nearbyDriverMapMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var routeCasingPolyline by remember { mutableStateOf<Polyline?>(null) }

    var selectedLayer by remember { mutableStateOf(MapTileLayer.STREET) }
    var showLayerMenu by remember { mutableStateOf(false) }
    var isInteractingState by remember { mutableStateOf(false) }

    // Reference variables for interaction state without forcing Compose recomposition loops
    val interactionHandler = remember { Handler(Looper.getMainLooper()) }
    val isInteractingRef = remember { booleanArrayOf(false) }
    val lastCenterReported = remember { doubleArrayOf(0.0, 0.0) }

    // Stable callback references
    val currentOnInteractionChange by rememberUpdatedState(onMapInteractionChange)
    val currentOnMapCenterChanged by rememberUpdatedState(onMapCenterChanged)
    val currentOnMapTapped by rememberUpdatedState(onMapTapped)

    // Debounced runnable to report center change after dragging ceases
    val reportCenterRunnable = remember {
        Runnable {
            mapViewRef?.let { map ->
                if (routeResult == null) {
                    val center = map.mapCenter
                    if (center is GeoPoint) {
                        val lat = center.latitude
                        val lng = center.longitude
                        if (abs(lat - lastCenterReported[0]) > 0.0001 || abs(lng - lastCenterReported[1]) > 0.0001) {
                            lastCenterReported[0] = lat
                            lastCenterReported[1] = lng
                            currentOnMapCenterChanged(lat, lng)
                        }
                    }
                }
            }
        }
    }

    // Debounced runnable to restore UI after user stops interacting
    val restoreInteractionRunnable = remember {
        Runnable {
            if (isInteractingRef[0]) {
                isInteractingRef[0] = false
                isInteractingState = false
                currentOnInteractionChange(false)
                reportCenterRunnable.run()
            }
        }
    }

    // Function to trigger interaction start exactly once
    val startInteractionOnce: () -> Unit = remember {
        {
            interactionHandler.removeCallbacks(restoreInteractionRunnable)
            if (!isInteractingRef[0]) {
                isInteractingRef[0] = true
                isInteractingState = true
                currentOnInteractionChange(true)
            }
        }
    }

    // Function to schedule restoration with debounce
    val scheduleRestoration: (delayMs: Long) -> Unit = remember {
        { delayMs ->
            interactionHandler.removeCallbacks(restoreInteractionRunnable)
            interactionHandler.postDelayed(restoreInteractionRunnable, delayMs)
        }
    }

    // Fallback coordinates
    val defaultLat = 34.0151
    val defaultLng = 71.5249

    val activeLat = currentLatitude ?: defaultLat
    val activeLng = currentLongitude ?: defaultLng

    // Track route signature so camera is adjusted only once per new route
    var lastBoundedRouteSignature by remember { mutableStateOf<String?>(null) }

    // Recenter only when trigger changes explicitly and no route is active
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && routeResult == null) {
            mapViewRef?.let { map ->
                val targetPoint = driverCarLocation ?: GeoPoint(activeLat, activeLng)
                map.controller.animateTo(targetPoint, 18.0, 500L)
                locationMarker?.position = targetPoint
                driverCarMarker?.position = targetPoint
                map.invalidate()
            }
        }
    }

    // 1. Manage Pickup Marker for fromLocation (Distinct Emerald/Green Pin)
    LaunchedEffect(fromLocation) {
        mapViewRef?.let { map ->
            if (fromLocation != null && (fromLocation.latitude != 0.0 || fromLocation.longitude != 0.0)) {
                val pickupPt = GeoPoint(fromLocation.latitude, fromLocation.longitude)
                val pickupDrawable = cachedPickupMarkerDrawable ?: createPickupMarkerDrawable(context).also {
                    cachedPickupMarkerDrawable = it
                }
                if (pickupMarker == null) {
                    val m = Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = pickupDrawable
                        infoWindow = null
                    }
                    map.overlays.add(m)
                    pickupMarker = m
                }
                pickupMarker?.position = pickupPt
                pickupMarker?.title = "Pickup: ${fromLocation.title}"
                if (toLocation == null && routeResult == null) {
                    map.controller.animateTo(pickupPt)
                }
                map.invalidate()
            } else {
                pickupMarker?.let { map.overlays.remove(it) }
                pickupMarker = null
                map.invalidate()
            }
        }
    }

    // 2. Manage Destination Marker for toLocation (Clear Crimson Pin with Flag Glyph)
    LaunchedEffect(toLocation) {
        mapViewRef?.let { map ->
            if (toLocation != null) {
                val destPt = GeoPoint(toLocation.latitude, toLocation.longitude)
                val destDrawable = cachedDestinationMarkerDrawable ?: createDestinationMarkerDrawable(context).also {
                    cachedDestinationMarkerDrawable = it
                }
                if (destinationMarker == null) {
                    val m = Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = destDrawable
                        infoWindow = null
                    }
                    map.overlays.add(m)
                    destinationMarker = m
                }
                destinationMarker?.position = destPt
                destinationMarker?.title = "Destination: ${toLocation.title}"
                map.invalidate()
            } else {
                destinationMarker?.let { map.overlays.remove(it) }
                destinationMarker = null
                map.invalidate()
            }
        }
    }

    // 2.5. Manage Live Driver Car Marker on Map
    LaunchedEffect(driverCarLocation, driverCarBearing) {
        mapViewRef?.let { map ->
            if (driverCarLocation != null) {
                // Ensure generic location dot does not overlap driver car marker
                locationMarker?.let { map.overlays.remove(it) }
                locationMarker = null

                val carDrawable = cachedDriverCarMarkerDrawable ?: createDriverCarMarkerDrawable(context).also {
                    cachedDriverCarMarkerDrawable = it
                }
                if (driverCarMarker == null) {
                    val m = Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = carDrawable
                        infoWindow = null
                    }
                    map.overlays.add(m)
                    driverCarMarker = m
                }
                driverCarMarker?.position = driverCarLocation
                driverCarMarker?.title = driverCarTitle ?: "Driver Vehicle"
                driverCarBearing?.let { b ->
                    driverCarMarker?.rotation = -b
                }
                map.invalidate()
            } else {
                driverCarMarker?.let { map.overlays.remove(it) }
                driverCarMarker = null
                map.invalidate()
            }
        }
    }

    // 2.8. Manage Passenger Request Markers on Driver Map
    LaunchedEffect(passengerRequestsOnMap) {
        mapViewRef?.let { map ->
            // Clean old request markers
            requestMarkers.forEach { map.overlays.remove(it) }
            val newMarkers = mutableListOf<Marker>()

            passengerRequestsOnMap.forEach { req ->
                if (req.pickupLat != 0.0 && req.pickupLon != 0.0) {
                    val reqMarker = Marker(map).apply {
                        position = GeoPoint(req.pickupLat, req.pickupLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = createPassengerRequestPinDrawable(context, req.estimatedFare)
                        title = "${req.passengerName} (PKR ${req.estimatedFare})"
                        setOnMarkerClickListener { _, _ ->
                            onPassengerRequestMarkerClick?.invoke(req)
                            true
                        }
                    }
                    map.overlays.add(reqMarker)
                    newMarkers.add(reqMarker)
                }
            }
            requestMarkers = newMarkers
            map.invalidate()
        }
    }

    // 2.9. Manage Available Nearby Driver Markers with Fare Badges on Map (Matching Reference Image)
    LaunchedEffect(nearbyDriverMarkers) {
        mapViewRef?.let { map ->
            // Clean old nearby driver markers
            nearbyDriverMapMarkers.forEach { map.overlays.remove(it) }
            val newMarkers = mutableListOf<Marker>()

            nearbyDriverMarkers.forEach { driver ->
                if (driver.latitude != 0.0 && driver.longitude != 0.0) {
                    val m = Marker(map).apply {
                        position = GeoPoint(driver.latitude, driver.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createDriverWithFareBadgeDrawable(
                            context = context,
                            fareText = driver.fareText,
                            isPrimary = driver.isPrimaryOption,
                            bearing = driver.bearing
                        )
                        title = "${driver.driverName.ifBlank { "Nearby Captain" }} - ${driver.fareText}"
                        infoWindow = null
                        setOnMarkerClickListener { _, _ ->
                            onNearbyDriverMarkerClick?.invoke(driver)
                            true
                        }
                    }
                    map.overlays.add(m)
                    newMarkers.add(m)
                }
            }
            nearbyDriverMapMarkers = newMarkers
            map.invalidate()
        }
    }

    // 3. Manage Route Polyline & Direct Camera Framing
    LaunchedEffect(routeResult) {
        mapViewRef?.let { map ->
            if (routeResult != null && routeResult.points.isNotEmpty()) {
                val startPt = routeResult.startPoint
                val destPt = routeResult.destinationPoint
                val routeSig = "${String.format(java.util.Locale.US, "%.4f", startPt.latitude)}_${String.format(java.util.Locale.US, "%.4f", startPt.longitude)}_${String.format(java.util.Locale.US, "%.4f", destPt.latitude)}_${String.format(java.util.Locale.US, "%.4f", destPt.longitude)}"

                // Remove existing route polylines to prevent duplicates
                routeCasingPolyline?.let { map.overlays.remove(it) }
                routePolyline?.let { map.overlays.remove(it) }

                // Create outer casing polyline
                val casing = Polyline(map).apply {
                    outlinePaint.color = android.graphics.Color.argb(230, 70, 0, 40)
                    outlinePaint.strokeWidth = 18f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                    setPoints(routeResult.points)
                }
                map.overlays.add(0, casing)
                routeCasingPolyline = casing

                // Create main magenta polyline
                val mainLine = Polyline(map).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#9E0059")
                    outlinePaint.strokeWidth = 12f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                    setPoints(routeResult.points)
                }
                map.overlays.add(1, mainLine)
                routePolyline = mainLine

                // Adjust camera bounding box once per new route
                if (lastBoundedRouteSignature != routeSig) {
                    lastBoundedRouteSignature = routeSig

                    val allLats = mutableListOf(startPt.latitude, destPt.latitude)
                    val allLngs = mutableListOf(startPt.longitude, destPt.longitude)
                    routeResult.points.forEach { pt ->
                        allLats.add(pt.latitude)
                        allLngs.add(pt.longitude)
                    }
                    toLocation?.let {
                        if (it.latitude != 0.0 || it.longitude != 0.0) {
                            allLats.add(it.latitude)
                            allLngs.add(it.longitude)
                        }
                    }
                    fromLocation?.let {
                        if (it.latitude != 0.0 || it.longitude != 0.0) {
                            allLats.add(it.latitude)
                            allLngs.add(it.longitude)
                        }
                    }
                    driverCarLocation?.let {
                        if (it.latitude != 0.0 || it.longitude != 0.0) {
                            allLats.add(it.latitude)
                            allLngs.add(it.longitude)
                        }
                    }

                    val minLat = allLats.minOrNull() ?: minOf(startPt.latitude, destPt.latitude)
                    val maxLat = allLats.maxOrNull() ?: maxOf(startPt.latitude, destPt.latitude)
                    val minLng = allLngs.minOrNull() ?: minOf(startPt.longitude, destPt.longitude)
                    val maxLng = allLngs.maxOrNull() ?: maxOf(startPt.longitude, destPt.longitude)

                    val latPadding = max(0.008, (maxLat - minLat) * 0.40)
                    val lngPadding = max(0.008, (maxLng - minLng) * 0.40)

                    val boundingBox = BoundingBox(
                        maxLat + latPadding,
                        maxLng + lngPadding,
                        minLat - latPadding,
                        minLng - lngPadding
                    )

                    map.post {
                        try {
                            map.zoomToBoundingBox(boundingBox, false, 140)
                        } catch (_: Exception) {
                            map.controller.setCenter(GeoPoint((minLat + maxLat) / 2.0, (minLng + maxLng) / 2.0))
                            map.controller.setZoom(13.5)
                        }
                    }
                }
                map.invalidate()
            } else {
                lastBoundedRouteSignature = null
                routeCasingPolyline?.let { map.overlays.remove(it) }
                routePolyline?.let { map.overlays.remove(it) }
                routeCasingPolyline = null
                routePolyline = null
                map.invalidate()
            }
        }
    }

    // Switch Tile Layer on MapView
    LaunchedEffect(selectedLayer) {
        mapViewRef?.let { map ->
            when (selectedLayer) {
                MapTileLayer.STREET -> map.setTileSource(TileSourceFactory.MAPNIK)
                MapTileLayer.DETAILED_TOPO -> map.setTileSource(TileSourceFactory.OpenTopo)
                MapTileLayer.SATELLITE -> map.setTileSource(TileSourceFactory.USGS_SAT)
            }
            map.invalidate()
        }
    }

    // Manage MapView Lifecycle cleanly
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    interactionHandler.removeCallbacksAndMessages(null)
                    mapViewRef?.onDetach()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            interactionHandler.removeCallbacksAndMessages(null)
            mapViewRef?.onDetach()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop
                var downX = 0f
                var downY = 0f
                var isDragEngaged = false

                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    isTilesScaledToDpi = true

                    controller.setZoom(18.0)
                    val initialPoint = GeoPoint(activeLat, activeLng)
                    controller.setCenter(initialPoint)

                    // User GPS Blue Dot Marker
                    val markerDrawable = cachedUserLocationMarkerDrawable ?: createDrigoLocationMarkerDrawable(ctx).also {
                        cachedUserLocationMarkerDrawable = it
                    }

                    val pinMarker = Marker(this).apply {
                        position = initialPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = markerDrawable
                        title = "Your Current GPS Location"
                        infoWindow = null
                    }
                    overlays.add(pinMarker)
                    locationMarker = pinMarker

                    val centerUpdateRunnable = Runnable {
                        mapCenter?.let { center ->
                            currentOnMapCenterChanged(center.latitude, center.longitude)
                        }
                    }

                    // Gesture detector for instant, precise map taps
                    val gestureDetector = android.view.GestureDetector(ctx, object : android.view.GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            val geoPoint = projection?.fromPixels(e.x.toInt(), e.y.toInt()) as? GeoPoint
                            if (geoPoint != null) {
                                currentOnMapTapped(geoPoint.latitude, geoPoint.longitude)
                            }
                            return true
                        }
                    })

                    // Dedicated touch listener with parent disallow intercept
                    setOnTouchListener { v, event ->
                        gestureDetector.onTouchEvent(event)
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                downX = event.x
                                downY = event.y
                                isDragEngaged = false
                                interactionHandler.removeCallbacks(restoreInteractionRunnable)
                                interactionHandler.removeCallbacks(centerUpdateRunnable)
                            }
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                isDragEngaged = true
                                startInteractionOnce()
                            }
                            MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                if (!isDragEngaged) {
                                    val dx = abs(event.x - downX)
                                    val dy = abs(event.y - downY)
                                    if (dx > touchSlop || dy > touchSlop || event.pointerCount > 1) {
                                        isDragEngaged = true
                                        startInteractionOnce()
                                    }
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                if (isDragEngaged || isInteractingRef[0]) {
                                    scheduleRestoration(1500L)
                                }
                                isDragEngaged = false
                                interactionHandler.removeCallbacks(centerUpdateRunnable)
                                interactionHandler.postDelayed(centerUpdateRunnable, 200L)
                            }
                        }
                        false
                    }

                    // MapListener for programmatic or fling scrolls & zooms
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            if (isInteractingRef[0]) {
                                scheduleRestoration(1500L)
                            }
                            interactionHandler.removeCallbacks(centerUpdateRunnable)
                            interactionHandler.postDelayed(centerUpdateRunnable, 300L)
                            return true
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            startInteractionOnce()
                            scheduleRestoration(1500L)
                            interactionHandler.removeCallbacks(centerUpdateRunnable)
                            interactionHandler.postDelayed(centerUpdateRunnable, 300L)
                            return true
                        }
                    })

                    mapViewRef = this
                }
            },
            update = { _ ->
                // Update User's GPS Location Marker
                val pt = GeoPoint(activeLat, activeLng)
                if (locationMarker?.position?.latitude != activeLat || locationMarker?.position?.longitude != activeLng) {
                    locationMarker?.position = pt
                }
            }
        )

        // Center Pickup Pin (optional center drag overlay when no destination is chosen)
        if (showCenterPickupPin && routeResult == null && toLocation == null) {
            CenterPickupLocationPinWithCallout(
                pickupAddress = fromLocation?.title ?: "",
                isInteracting = isInteractingState,
                onWhereFromClick = onWhereFromClick,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Active Map Tap Placement Banner Indicator (When picking From or To on Map)
        AnimatedVisibility(
            visible = mapSelectionMode != MapSelectionMode.NONE,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -30 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -30 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFF1E2026).copy(alpha = 0.96f),
                border = BorderStroke(
                    1.5.dp,
                    if (mapSelectionMode == MapSelectionMode.PICKUP) Color(0xFF4CAF50) else Color(0xFFE53935)
                ),
                shadowElevation = 10.dp,
                modifier = Modifier.testTag("map_pick_mode_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (mapSelectionMode == MapSelectionMode.PICKUP) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (mapSelectionMode == MapSelectionMode.PICKUP) "Tap on map to set Pickup (From)" else "Tap on map to set Destination (To)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = onCancelMapSelection,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Cancel", color = Color(0xFFB0B3BC), fontSize = 12.sp)
                    }
                }
            }
        }

        // Map Layer Switcher Floating Button (Top Right)
        if (showMapLayerSwitcher) {
            Surface(
                onClick = { showLayerMenu = !showLayerMenu },
                shape = CircleShape,
                color = Color(0xFF1E2024).copy(alpha = 0.92f),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 16.dp)
                    .size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map Style & Street View",
                        tint = if (showLayerMenu) DrigoBrandPurple else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Map Layer Selection Dialog / Card
            AnimatedVisibility(
                visible = showLayerMenu,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 106.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E2024).copy(alpha = 0.96f),
                    shadowElevation = 10.dp,
                    modifier = Modifier.width(200.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Map Style",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { showLayerMenu = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        MapLayerOptionItem(
                            title = "Street View",
                            subtitle = "High clarity roads & labels",
                            isSelected = selectedLayer == MapTileLayer.STREET,
                            onClick = {
                                selectedLayer = MapTileLayer.STREET
                                showLayerMenu = false
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        MapLayerOptionItem(
                            title = "Topography",
                            subtitle = "Contours, POIs & terrain",
                            isSelected = selectedLayer == MapTileLayer.DETAILED_TOPO,
                            onClick = {
                                selectedLayer = MapTileLayer.DETAILED_TOPO
                                showLayerMenu = false
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        MapLayerOptionItem(
                            title = "Satellite View",
                            subtitle = "Aerial imagery",
                            isSelected = selectedLayer == MapTileLayer.SATELLITE,
                            onClick = {
                                selectedLayer = MapTileLayer.SATELLITE
                                showLayerMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Center Pickup Pin & "Where from?" Field Component.
 * - Positioned at exact center of map screen.
 * - "Where from?" field is placed directly above the pointer and is fully clickable.
 * - Lifts smoothly during drag/interaction.
 */
@Composable
private fun CenterPickupLocationPinWithCallout(
    pickupAddress: String,
    isInteracting: Boolean,
    onWhereFromClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liftOffset by animateDpAsState(
        targetValue = if (isInteracting) (-12).dp else 0.dp,
        animationSpec = tween(durationMillis = 160),
        label = "pin_lift"
    )
    val shadowScale by animateFloatAsState(
        targetValue = if (isInteracting) 0.60f else 1.0f,
        animationSpec = tween(durationMillis = 160),
        label = "shadow_scale"
    )

    Box(
        modifier = modifier.testTag("center_pickup_pin_container"),
        contentAlignment = Alignment.Center
    ) {
        // Ground shadow anchor point at exact map center
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 6.dp)
                .graphicsLayer {
                    scaleX = shadowScale
                    scaleY = shadowScale
                    alpha = if (isInteracting) 0.25f else 0.45f
                }
                .background(Color.Black, shape = CircleShape)
        )

        // Lifted Structure containing "Where From?" Field and Pickup Pointer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = (-46).dp + liftOffset)
        ) {
            // "Where From?" Clickable Field Card (Directly above pointer)
            Surface(
                onClick = onWhereFromClick,
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E2024).copy(alpha = 0.96f),
                shadowElevation = if (isInteracting) 10.dp else 6.dp,
                border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                modifier = Modifier
                    .widthIn(min = 160.dp, max = 260.dp)
                    .testTag("where_from_field")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(7.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pickup Location",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isInteracting) "Selecting pickup spot..." else pickupAddress.ifBlank { "Tap to set pickup" },
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Small gap between the "Where From?" field and pointer so pointer remains clearly visible
            Spacer(modifier = Modifier.height(6.dp))

            // Pickup Pin Head (Green round badge with passenger icon & thick white border)
            Surface(
                shape = CircleShape,
                color = Color(0xFF2E7D32),
                shadowElevation = if (isInteracting) 8.dp else 4.dp,
                border = BorderStroke(2.5.dp, Color.White),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Pickup Passenger",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Pin Stem / Pointer Tip (Green)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(8.dp)
                    .background(Color(0xFF2E7D32))
            )

            // Pin Needle Base Point (Green)
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color(0xFF2E7D32), shape = CircleShape)
            )
        }
    }
}

@Composable
private fun MapLayerOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) DrigoBrandPurple.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
        border = if (isSelected) BorderStroke(1.5.dp, DrigoBrandPurple) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Creates the glowing blue Drigo Location Marker icon for User's actual GPS position
 */
private fun createDrigoLocationMarkerDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (48 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Outer subtle radar pulse ring
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 66, 133, 244)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 22f * density, glowPaint)

    // Outer white rim
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 14f * density, ringPaint)

    // Blue Main Circle (#4285F4)
    val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#4285F4")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 11f * density, bluePaint)

    // Black center point
    val blackDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 4f * density, blackDot)

    // White core highlight
    val whiteCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 1.5f * density, whiteCore)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates distinct Destination Marker (Red/Crimson pin with clear flag icon)
 */
private fun createDestinationMarkerDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (38 * density).toInt().coerceAtLeast(1)
    val heightPx = (50 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = widthPx / 2f
    val pinHeadRadius = 14f * density
    val pinHeadCy = 16f * density

    // Ground shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(90, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawOval(cx - 7f * density, heightPx - 5f * density, cx + 7f * density, heightPx - 1f * density, shadowPaint)

    // Pin stem triangle down to bottom
    val path = android.graphics.Path().apply {
        moveTo(cx - 10f * density, pinHeadCy + 4f * density)
        lineTo(cx, heightPx - 4f * density)
        lineTo(cx + 10f * density, pinHeadCy + 4f * density)
        close()
    }
    val crimsonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E53935") // Crimson Red
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, crimsonPaint)

    // Pin head circle
    canvas.drawCircle(cx, pinHeadCy, pinHeadRadius, crimsonPaint)

    // White border ring
    val whiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    canvas.drawCircle(cx, pinHeadCy, pinHeadRadius - 1.2f * density, whiteRing)

    // White circular interior background
    val whiteInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, pinHeadCy, 9.5f * density, whiteInner)

    // Draw Flag Icon inside head:
    // Flagpole
    val polePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#B71C1C")
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    val poleX = cx - 3.5f * density
    val poleTop = pinHeadCy - 6.5f * density
    val poleBottom = pinHeadCy + 6.5f * density
    canvas.drawLine(poleX, poleTop, poleX, poleBottom, polePaint)

    // Flag banner (triangle)
    val flagPath = android.graphics.Path().apply {
        moveTo(poleX, poleTop)
        lineTo(poleX + 7.5f * density, pinHeadCy - 3.2f * density)
        lineTo(poleX, pinHeadCy)
        close()
    }
    val flagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E53935")
        style = Paint.Style.FILL
    }
    canvas.drawPath(flagPath, flagPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates distinct Pickup Marker (Emerald Green pin with clear person/pickup icon)
 */
private fun createPickupMarkerDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (38 * density).toInt().coerceAtLeast(1)
    val heightPx = (50 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = widthPx / 2f
    val pinHeadRadius = 14f * density
    val pinHeadCy = 16f * density

    // Ground shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(90, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawOval(cx - 7f * density, heightPx - 5f * density, cx + 7f * density, heightPx - 1f * density, shadowPaint)

    // Pin stem triangle down to bottom
    val path = android.graphics.Path().apply {
        moveTo(cx - 10f * density, pinHeadCy + 4f * density)
        lineTo(cx, heightPx - 4f * density)
        lineTo(cx + 10f * density, pinHeadCy + 4f * density)
        close()
    }
    val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2E7D32") // Forest Green
        style = Paint.Style.FILL
    }
    canvas.drawPath(path, greenPaint)

    // Pin head circle
    canvas.drawCircle(cx, pinHeadCy, pinHeadRadius, greenPaint)

    // White border ring
    val whiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    canvas.drawCircle(cx, pinHeadCy, pinHeadRadius - 1.2f * density, whiteRing)

    // White circular interior background
    val whiteInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, pinHeadCy, 9.5f * density, whiteInner)

    // Person / Passenger Icon inside
    val personPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2E7D32")
        style = Paint.Style.FILL
    }
    // Head circle
    canvas.drawCircle(cx, pinHeadCy - 3f * density, 2.8f * density, personPaint)

    // Body arc
    val bodyRect = android.graphics.RectF(
        cx - 5f * density,
        pinHeadCy + 0.5f * density,
        cx + 5f * density,
        pinHeadCy + 7f * density
    )
    canvas.drawArc(bodyRect, 180f, 180f, true, personPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates an inDrive-style modern stylized Top-Down / Isometric Vehicle Car Marker
 * with distinct body, roof, windshield, headlights, wheels, and glowing shadow
 */
private fun createDriverCarMarkerDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (44 * density).toInt().coerceAtLeast(1)
    val heightPx = (44 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = widthPx / 2f
    val cy = heightPx / 2f

    // Radar pulse / glow behind car
    val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(55, 158, 0, 89) // Brand Purple Glow
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 20f * density, pulsePaint)

    // Outer soft circular background
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(235, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 15.5f * density, haloPaint)

    // Border ring on halo
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#9E0059")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(cx, cy, 15.5f * density, ringPaint)

    // Top-down Car Body (Pointing UP towards 12 o'clock so rotation aligns with bearing)
    val carW = 12f * density
    val carH = 22f * density
    val left = cx - (carW / 2f)
    val top = cy - (carH / 2f)
    val right = cx + (carW / 2f)
    val bottom = cy + (carH / 2f)

    // Wheels (4 black rounded rectangles on sides)
    val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#212121")
        style = Paint.Style.FILL
    }
    val wheelW = 2.5f * density
    val wheelH = 4.5f * density
    // Front-left
    canvas.drawRoundRect(android.graphics.RectF(left - 1.2f * density, top + 2.5f * density, left + 1.3f * density, top + 2.5f * density + wheelH), 1f * density, 1f * density, wheelPaint)
    // Front-right
    canvas.drawRoundRect(android.graphics.RectF(right - 1.3f * density, top + 2.5f * density, right + 1.2f * density, top + 2.5f * density + wheelH), 1f * density, 1f * density, wheelPaint)
    // Rear-left
    canvas.drawRoundRect(android.graphics.RectF(left - 1.2f * density, bottom - 2.5f * density - wheelH, left + 1.3f * density, bottom - 2.5f * density), 1f * density, 1f * density, wheelPaint)
    // Rear-right
    canvas.drawRoundRect(android.graphics.RectF(right - 1.3f * density, bottom - 2.5f * density - wheelH, right + 1.2f * density, bottom - 2.5f * density), 1f * density, 1f * density, wheelPaint)

    // Main Car Body (Magenta / Brand Purple or Crisp Dark Metal)
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#9E0059")
        style = Paint.Style.FILL
    }
    val carBodyRect = android.graphics.RectF(left, top, right, bottom)
    canvas.drawRoundRect(carBodyRect, 4f * density, 4f * density, bodyPaint)

    // Front Windshield (curved dark glass at top)
    val windshieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1A237E")
        style = Paint.Style.FILL
    }
    val windshieldRect = android.graphics.RectF(left + 2f * density, top + 4f * density, right - 2f * density, top + 8.5f * density)
    canvas.drawRoundRect(windshieldRect, 1.5f * density, 1.5f * density, windshieldPaint)

    // Rear Windshield
    val rearGlassRect = android.graphics.RectF(left + 2f * density, bottom - 6.5f * density, right - 2f * density, bottom - 3.5f * density)
    canvas.drawRoundRect(rearGlassRect, 1.2f * density, 1.2f * density, windshieldPaint)

    // Car Roof (White / Silver highlight)
    val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val roofRect = android.graphics.RectF(left + 2.5f * density, top + 9f * density, right - 2.5f * density, bottom - 7f * density)
    canvas.drawRoundRect(roofRect, 1.5f * density, 1.5f * density, roofPaint)

    // Headlights (Twin bright amber / yellow beams)
    val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFD600")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(left + 2.2f * density, top + 1f * density, 1.2f * density, headlightPaint)
    canvas.drawCircle(right - 2.2f * density, top + 1f * density, 1.2f * density, headlightPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates passenger ride request marker with fare pill for Driver map
 */
private fun createPassengerRequestPinDrawable(context: Context, farePkr: Int): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (64 * density).toInt().coerceAtLeast(1)
    val heightPx = (52 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = widthPx / 2f

    // Fare Pill at Top
    val pillH = 20f * density
    val pillW = 58f * density
    val pillLeft = cx - (pillW / 2f)
    val pillTop = 2f * density
    val pillRight = cx + (pillW / 2f)
    val pillBottom = pillTop + pillH

    // Pill background (Dark with Green/Brand border)
    val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1E1E2E")
        style = Paint.Style.FILL
    }
    val pillRect = android.graphics.RectF(pillLeft, pillTop, pillRight, pillBottom)
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBg)

    val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#00E676") // Bright Green
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
    }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBorder)

    // Fare Text
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 10f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Rs. $farePkr", cx, pillTop + 14f * density, textPaint)

    // Pin Head circle below pill
    val pinCy = pillBottom + 11f * density
    val pinRadius = 9f * density

    val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#00C853")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, pinCy, pinRadius, greenPaint)

    val whiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(cx, pinCy, pinRadius - 1f * density, whiteRing)

    // Needle pointing down to ground
    val path = android.graphics.Path().apply {
        moveTo(cx - 5f * density, pinCy + 5f * density)
        lineTo(cx, heightPx - 2f * density)
        lineTo(cx + 5f * density, pinCy + 5f * density)
        close()
    }
    canvas.drawPath(path, greenPaint)

    // Person dot in center
    val personDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, pinCy, 3.5f * density, personDot)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates a custom Map Marker Drawable for Nearby Available Drivers with Attached Fare Badge.
 * Styled directly after the reference image: top-down car icon with a rounded pill badge attached
 * showing fare prices (e.g. "380 Rs" in lime green pill, "400 Rs" in dark charcoal pill).
 */
private fun createDriverWithFareBadgeDrawable(
    context: Context,
    fareText: String,
    isPrimary: Boolean,
    bearing: Float
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (110 * density).toInt().coerceAtLeast(1)
    val heightPx = (80 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Center point of the car graphic
    val carCx = 38f * density
    val carCy = 48f * density

    // Clean text measure for pill badge
    val displayText = fareText.ifBlank { "380 Rs" }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 12f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val textWidth = textPaint.measureText(displayText)
    val pillH = 22f * density
    val pillW = (textWidth + 18f * density).coerceAtLeast(50f * density)

    val pillLeft = carCx + 8f * density
    val pillTop = carCy - 34f * density
    val pillRight = pillLeft + pillW
    val pillBottom = pillTop + pillH

    val pillRect = android.graphics.RectF(pillLeft, pillTop, pillRight, pillBottom)

    // 1. Soft Shadow under Pill Badge
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(90, 0, 0, 0)
        style = Paint.Style.FILL
    }
    val shadowRect = android.graphics.RectF(pillLeft + 1.5f * density, pillTop + 2.5f * density, pillRight + 1.5f * density, pillBottom + 2.5f * density)
    canvas.drawRoundRect(shadowRect, pillH / 2f, pillH / 2f, shadowPaint)

    // 2. Pill Background (Primary = Lime Green like #9CDB43 / #A2E048, Secondary = Dark Charcoal #282C37)
    val pillBgColor = if (isPrimary) android.graphics.Color.parseColor("#9CDB43") else android.graphics.Color.parseColor("#282C37")
    val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillBgColor
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBgPaint)

    // 3. Pill Border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isPrimary) android.graphics.Color.parseColor("#82C42C") else android.graphics.Color.parseColor("#424A5C")
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, borderPaint)

    // 4. Draw Fare Text inside Pill
    val textX = pillLeft + (pillW / 2f)
    val fontMetrics = textPaint.fontMetrics
    val textY = pillTop + (pillH / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
    canvas.drawText(displayText, textX, textY, textPaint)

    // 5. Connecting stem to car
    val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillBgColor
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
    }
    canvas.drawLine(carCx + 4f * density, carCy - 12f * density, pillLeft + 4f * density, pillBottom - 2f * density, stemPaint)

    // 6. Draw Top-Down Car Graphic with Rotation
    canvas.save()
    canvas.rotate(bearing, carCx, carCy)

    val carW = 15f * density
    val carH = 28f * density
    val left = carCx - (carW / 2f)
    val top = carCy - (carH / 2f)
    val right = carCx + (carW / 2f)
    val bottom = carCy + (carH / 2f)

    // Wheels
    val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#121417")
        style = Paint.Style.FILL
    }
    val wheelW = 3f * density
    val wheelH = 5.5f * density
    canvas.drawRoundRect(android.graphics.RectF(left - 1.5f * density, top + 3.5f * density, left + 1.5f * density, top + 3.5f * density + wheelH), 1.2f * density, 1.2f * density, wheelPaint)
    canvas.drawRoundRect(android.graphics.RectF(right - 1.5f * density, top + 3.5f * density, right + 1.5f * density, top + 3.5f * density + wheelH), 1.2f * density, 1.2f * density, wheelPaint)
    canvas.drawRoundRect(android.graphics.RectF(left - 1.5f * density, bottom - 3.5f * density - wheelH, left + 1.5f * density, bottom - 3.5f * density), 1.2f * density, 1.2f * density, wheelPaint)
    canvas.drawRoundRect(android.graphics.RectF(right - 1.5f * density, bottom - 3.5f * density - wheelH, right + 1.5f * density, bottom - 3.5f * density), 1.2f * density, 1.2f * density, wheelPaint)

    // Car Body
    val bodyColor = if (isPrimary) android.graphics.Color.parseColor("#9CDB43") else android.graphics.Color.parseColor("#383E4C")
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bodyColor
        style = Paint.Style.FILL
    }
    val bodyRect = android.graphics.RectF(left, top, right, bottom)
    canvas.drawRoundRect(bodyRect, 6f * density, 6f * density, bodyPaint)

    // Body Outline
    val bodyOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(100, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    canvas.drawRoundRect(bodyRect, 6f * density, 6f * density, bodyOutlinePaint)

    // Front Windshield
    val windshieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1B222A")
        style = Paint.Style.FILL
    }
    val windshieldRect = android.graphics.RectF(left + 2f * density, top + 5.5f * density, right - 2f * density, top + 11f * density)
    canvas.drawRoundRect(windshieldRect, 2f * density, 2f * density, windshieldPaint)

    // Rear Glass
    val rearGlassRect = android.graphics.RectF(left + 2.2f * density, bottom - 8.5f * density, right - 2.2f * density, bottom - 4.5f * density)
    canvas.drawRoundRect(rearGlassRect, 1.5f * density, 1.5f * density, windshieldPaint)

    // Car Roof
    val roofColor = if (isPrimary) android.graphics.Color.parseColor("#C8E6C9") else android.graphics.Color.parseColor("#606B7D")
    val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = roofColor
        style = Paint.Style.FILL
    }
    val roofRect = android.graphics.RectF(left + 2.5f * density, top + 11.5f * density, right - 2.5f * density, bottom - 9f * density)
    canvas.drawRoundRect(roofRect, 2f * density, 2f * density, roofPaint)

    // Headlights (Front - Bright Amber/White)
    val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFF176")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(left + 2.5f * density, top + 1.8f * density, 1.3f * density, headlightPaint)
    canvas.drawCircle(right - 2.5f * density, top + 1.8f * density, 1.3f * density, headlightPaint)

    // Taillights (Rear - Red)
    val taillightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FF2A2A")
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(android.graphics.RectF(left + 1.2f * density, bottom - 1.8f * density, left + 4.5f * density, bottom), 1f * density, 1f * density, taillightPaint)
    canvas.drawRoundRect(android.graphics.RectF(right - 4.5f * density, bottom - 1.8f * density, right - 1.2f * density, bottom), 1f * density, 1f * density, taillightPaint)

    canvas.restore()

    return BitmapDrawable(context.resources, bitmap)
}

