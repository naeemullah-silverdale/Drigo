package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

enum class IntercityWaypointStatus {
    COMPLETED,
    ACTIVE_NEXT,
    UPCOMING
}

data class IntercityMapWaypoint(
    val stopNumber: Int,
    val title: String,
    val subtitle: String = "",
    val point: GeoPoint,
    val status: IntercityWaypointStatus = IntercityWaypointStatus.UPCOMING,
    val isPickup: Boolean = true
)

/**
 * High-performance, dedicated OpenStreetMap composable for City-to-City corridor transit.
 * Displays corridor highway polylines, numbered waypoints, vehicle tracking, and pickup pins.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun IntercityOsmMapView(
    modifier: Modifier = Modifier,
    routePoints: List<GeoPoint> = emptyList(),
    waypoints: List<IntercityMapWaypoint> = emptyList(),
    driverCarLocation: GeoPoint? = null,
    driverCarBearing: Float = 45f,
    driverCarTitle: String? = null, // e.g., "Ali: 115 km/h"
    userPickupLocation: GeoPoint? = null,
    userPickupTitle: String? = null, // e.g., "YOU: Faizabad (12 min ETA)"
    recenterTrigger: Int = 0,
    centerLat: Double = 32.8,
    centerLon: Double = 73.5,
    initialZoom: Double = 8.5,
    onMapClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var routeCasingPolyline by remember { mutableStateOf<Polyline?>(null) }
    val waypointMarkers = remember { mutableStateListOf<Marker>() }
    var carMarker by remember { mutableStateOf<Marker?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }
    var eventsOverlay by remember { mutableStateOf<MapEventsOverlay?>(null) }

    val isAppDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val darkFilter = remember(isAppDark) {
        if (isAppDark) {
            val matrix = android.graphics.ColorMatrix(floatArrayOf(
                -0.78f,  0.00f,  0.00f, 0.00f, 215f,
                 0.00f, -0.78f,  0.00f, 0.00f, 215f,
                 0.00f,  0.00f, -0.78f, 0.00f, 215f,
                 0.00f,  0.00f,  0.00f, 1.00f,   0f
            ))
            android.graphics.ColorMatrixColorFilter(matrix)
        } else null
    }

    // Manage MapView Lifecycle cleanly
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    // Update Route Polylines
    LaunchedEffect(routePoints) {
        mapViewRef?.let { map ->
            routeCasingPolyline?.let { map.overlays.remove(it) }
            routePolyline?.let { map.overlays.remove(it) }
            routeCasingPolyline = null
            routePolyline = null

            if (routePoints.size >= 2) {
                // Background dark casing
                val casing = Polyline(map).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#1B5E20") // Deep Forest Green
                    outlinePaint.strokeWidth = 14f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(routePoints)
                }
                map.overlays.add(0, casing)
                routeCasingPolyline = casing

                // Core vibrant green route
                val core = Polyline(map).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#00E676") // Radiant Mint Green
                    outlinePaint.strokeWidth = 9f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(routePoints)
                }
                map.overlays.add(1, core)
                routePolyline = core

                map.invalidate()
            }
        }
    }

    // Update Waypoint Markers
    LaunchedEffect(waypoints) {
        mapViewRef?.let { map ->
            waypointMarkers.forEach { map.overlays.remove(it) }
            waypointMarkers.clear()

            waypoints.forEach { wp ->
                val iconDrawable = createWaypointPillDrawable(context, wp)
                val marker = Marker(map).apply {
                    position = wp.point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = iconDrawable
                    title = wp.title
                    infoWindow = null
                }
                map.overlays.add(marker)
                waypointMarkers.add(marker)
            }
            map.invalidate()
        }
    }

    // Update Driver Car Marker
    LaunchedEffect(driverCarLocation, driverCarBearing, driverCarTitle) {
        mapViewRef?.let { map ->
            if (driverCarLocation != null) {
                val carDrawable = createCorridorCarDrawable(
                    context = context,
                    bearing = driverCarBearing,
                    title = driverCarTitle ?: "Ali: 115 km/h"
                )
                if (carMarker == null) {
                    val m = Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = carDrawable
                        infoWindow = null
                    }
                    map.overlays.add(m)
                    carMarker = m
                } else {
                    carMarker?.icon = carDrawable
                }
                carMarker?.position = driverCarLocation
                map.invalidate()
            } else {
                carMarker?.let { map.overlays.remove(it) }
                carMarker = null
                map.invalidate()
            }
        }
    }

    // Update User Pickup Marker
    LaunchedEffect(userPickupLocation, userPickupTitle) {
        mapViewRef?.let { map ->
            if (userPickupLocation != null) {
                val userDrawable = createUserPickupPillDrawable(
                    context = context,
                    title = userPickupTitle ?: "YOU: Faizabad (12 min ETA)"
                )
                if (userMarker == null) {
                    val m = Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = userDrawable
                        infoWindow = null
                    }
                    map.overlays.add(m)
                    userMarker = m
                } else {
                    userMarker?.icon = userDrawable
                }
                userMarker?.position = userPickupLocation
                map.invalidate()
            } else {
                userMarker?.let { map.overlays.remove(it) }
                userMarker = null
                map.invalidate()
            }
        }
    }

    // Recenter / Fit View
    LaunchedEffect(recenterTrigger, routePoints) {
        mapViewRef?.let { map ->
            if (routePoints.size >= 2) {
                var minLat = 90.0
                var maxLat = -90.0
                var minLon = 180.0
                var maxLon = -180.0
                routePoints.forEach { pt ->
                    if (pt.latitude < minLat) minLat = pt.latitude
                    if (pt.latitude > maxLat) maxLat = pt.latitude
                    if (pt.longitude < minLon) minLon = pt.longitude
                    if (pt.longitude > maxLon) maxLon = pt.longitude
                }
                val box = BoundingBox(maxLat + 0.15, maxLon + 0.15, minLat - 0.15, minLon - 0.15)
                map.zoomToBoundingBox(box, true, 80)
            } else {
                map.controller.setZoom(initialZoom)
                map.controller.animateTo(GeoPoint(centerLat, centerLon))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    isTilesScaledToDpi = true
                    overlayManager.tilesOverlay.setColorFilter(darkFilter)

                    controller.setZoom(initialZoom)
                    controller.setCenter(GeoPoint(centerLat, centerLon))
                    mapViewRef = this

                    val overlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            onMapClick?.invoke()
                            return false
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    })
                    overlays.add(0, overlay)
                    eventsOverlay = overlay
                }
            },
            update = { map ->
                map.overlayManager.tilesOverlay.setColorFilter(darkFilter)
                if (eventsOverlay != null && !map.overlays.contains(eventsOverlay)) {
                    map.overlays.add(0, eventsOverlay)
                }
                map.invalidate()
            }
        )
    }
}

/**
 * Creates custom pill drawables for numbered waypoints along the corridor.
 * Matches Screenshot 2 (e.g., "1: G-9 Boarded", "2: NEXT: Faizabad", "3: M-2 Toll Plaza").
 */
private fun createWaypointPillDrawable(
    context: Context,
    waypoint: IntercityMapWaypoint
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val text = waypoint.title.ifBlank { "Stop ${waypoint.stopNumber}" }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val textWidth = textPaint.measureText(text)
    val pillH = 26f * density
    val pillW = (textWidth + 24f * density).coerceAtLeast(60f * density)
    val pinPinHeight = 8f * density
    val totalH = pillH + pinPinHeight + 4f * density
    val totalW = pillW + 8f * density

    val bitmap = Bitmap.createBitmap(totalW.toInt().coerceAtLeast(1), totalH.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = totalW / 2f
    val pillTop = 2f * density
    val pillBottom = pillTop + pillH
    val pillRect = RectF(cx - pillW / 2f, pillTop, cx + pillW / 2f, pillBottom)

    val isNext = waypoint.status == IntercityWaypointStatus.ACTIVE_NEXT
    val isDone = waypoint.status == IntercityWaypointStatus.COMPLETED

    // Background color
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            isNext -> android.graphics.Color.parseColor("#00E676") // Radiant Green
            isDone -> android.graphics.Color.parseColor("#1B5E20") // Dark Green Done
            else -> android.graphics.Color.WHITE
        }
        style = Paint.Style.FILL
    }

    // Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(pillRect.left + 1.5f * density, pillRect.top + 2.5f * density, pillRect.right + 1.5f * density, pillRect.bottom + 2.5f * density),
        pillH / 2f, pillH / 2f, shadowPaint
    )

    // Pill
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, bgPaint)

    // Border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            isNext -> android.graphics.Color.WHITE
            isDone -> android.graphics.Color.parseColor("#00E676")
            else -> android.graphics.Color.parseColor("#B0BEC5")
        }
        style = Paint.Style.STROKE
        strokeWidth = if (isNext) 2f * density else 1.2f * density
    }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, borderPaint)

    // Downward Pin Pointer
    val pointerPath = android.graphics.Path().apply {
        moveTo(cx - 5f * density, pillBottom - 1f)
        lineTo(cx, pillBottom + pinPinHeight)
        lineTo(cx + 5f * density, pillBottom - 1f)
        close()
    }
    canvas.drawPath(pointerPath, bgPaint)
    canvas.drawPath(pointerPath, borderPaint)

    // Text
    textPaint.color = when {
        isNext -> android.graphics.Color.parseColor("#003300")
        isDone -> android.graphics.Color.WHITE
        else -> android.graphics.Color.parseColor("#263238")
    }
    val fontMetrics = textPaint.fontMetrics
    val textY = pillRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(text, cx, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates vehicle marker with attached live speed badge (e.g. "Ali: 115 km/h").
 * Matches Screenshot 1.
 */
private fun createCorridorCarDrawable(
    context: Context,
    bearing: Float,
    title: String
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.WHITE
    }

    val textWidth = textPaint.measureText(title)
    val pillH = 22f * density
    val pillW = (textWidth + 20f * density).coerceAtLeast(64f * density)

    val carRadius = 14f * density
    val widthPx = (pillW + 40f * density).toInt().coerceAtLeast(1)
    val heightPx = (carRadius * 2 + pillH + 20f * density).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = widthPx / 2f
    val carCy = heightPx - carRadius - 4f * density

    // Draw Attached Pill Badge
    val pillLeft = cx - pillW / 2f
    val pillTop = carCy - carRadius - pillH - 4f * density
    val pillRect = RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH)

    val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1B5E20") // Dark green highway pill
        style = Paint.Style.FILL
    }
    val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#00E676")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBg)
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBorder)

    val fontMetrics = textPaint.fontMetrics
    val textY = pillRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(title, cx, textY, textPaint)

    // Draw Car Circle with bearing arrow
    val carRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val carCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#00C853")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, carCy, carRadius, carRing)
    canvas.drawCircle(cx, carCy, carRadius - 2f * density, carCore)

    // Inner directional chevron
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        strokeWidth = 2.5f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    canvas.save()
    canvas.rotate(bearing, cx, carCy)
    val path = android.graphics.Path().apply {
        moveTo(cx - 4f * density, carCy + 3f * density)
        lineTo(cx, carCy - 4f * density)
        lineTo(cx + 4f * density, carCy + 3f * density)
    }
    canvas.drawPath(path, arrowPaint)
    canvas.restore()

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates user pickup location badge (e.g. "YOU: Faizabad (12 min ETA)").
 * Matches Screenshot 1.
 */
private fun createUserPickupPillDrawable(
    context: Context,
    title: String
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.WHITE
    }

    val textWidth = textPaint.measureText(title)
    val pillH = 26f * density
    val pillW = (textWidth + 22f * density).coerceAtLeast(80f * density)
    val pinPinHeight = 8f * density
    val totalH = pillH + pinPinHeight + 4f * density
    val totalW = pillW + 8f * density

    val bitmap = Bitmap.createBitmap(totalW.toInt().coerceAtLeast(1), totalH.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = totalW / 2f
    val pillTop = 2f * density
    val pillBottom = pillTop + pillH
    val pillRect = RectF(cx - pillW / 2f, pillTop, cx + pillW / 2f, pillBottom)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0288D1") // Vivid Blue Pin
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    // Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(pillRect.left + 1.5f * density, pillRect.top + 2.5f * density, pillRect.right + 1.5f * density, pillRect.bottom + 2.5f * density),
        pillH / 2f, pillH / 2f, shadowPaint
    )

    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, bgPaint)
    canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, borderPaint)

    // Needle pointer
    val pointerPath = android.graphics.Path().apply {
        moveTo(cx - 5f * density, pillBottom - 1f)
        lineTo(cx, pillBottom + pinPinHeight)
        lineTo(cx + 5f * density, pillBottom - 1f)
        close()
    }
    canvas.drawPath(pointerPath, bgPaint)
    canvas.drawPath(pointerPath, borderPaint)

    val fontMetrics = textPaint.fontMetrics
    val textY = pillRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(title, cx, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
