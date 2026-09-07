package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverOffer
import com.example.ui.theme.DrigoBrandPurple
import java.util.Locale

// Colors matching unified brand theme
val InDriveLimeGreen = Color(0xFFFF00CC) // Unified Brand Fuchsia for primary action CTA
val InDriveDarkBg = Color(0xFF191B20)
val InDriveCardBg = Color(0xFF22252C)
val InDriveSelectedCardBg = Color(0xFF2E192D)
val InDriveTextPrimary = Color.White
val InDriveTextSecondary = Color(0xFFA0A6B5)
val InDriveBorder = Color(0xFF4A1E44)

data class InDriveRideOption(
    val id: String,
    val title: String,
    val capacityText: String,
    val subtitle: String,
    val baseFare: Int,
    val hasAc: Boolean = false,
    val isCourier: Boolean = false,
    val isMoto: Boolean = false
)

/**
 * Full inDrive-style ride options list with prices for all categories,
 * an expanded interactive card for the selected ride with +/- fare counters,
 * and informational notes.
 */
@Composable
fun InDriveRideOptionsList(
    rideOptions: List<InDriveRideOption>,
    selectedOptionId: String,
    customFare: Int,
    onSelectOption: (InDriveRideOption) -> Unit,
    onDecreaseFare: () -> Unit,
    onIncreaseFare: () -> Unit,
    onSetFare: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rideOptions.forEach { option ->
            val isSelected = option.id == selectedOptionId

            if (isSelected) {
                // Selected Expanded Card with Fare Negotiation (- / +)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = InDriveSelectedCardBg,
                    border = BorderStroke(1.5.dp, InDriveLimeGreen.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("ride_option_${option.id}_selected")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Top info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Vehicle Graphic with Snowflake if AC
                            Box(
                                modifier = Modifier.size(54.dp, 38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    option.isMoto -> MotoVehicleGraphic(modifier = Modifier.fillMaxSize())
                                    option.isCourier -> CourierVehicleGraphic(modifier = Modifier.fillMaxSize())
                                    else -> CarVehicleGraphic(
                                        hasAc = option.hasAc,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = InDriveTextPrimary,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Info",
                                        tint = InDriveTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (option.capacityText.isNotBlank()) {
                                        Text(
                                            text = option.capacityText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = InDriveTextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InDriveTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            // Base Price badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1D2027),
                                border = BorderStroke(1.dp, Color(0xFF333744)),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = "Base PKR ${option.baseFare}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA0A6B5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fare Negotiation Box with [-] PKR amount [+]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1D2027),
                            border = BorderStroke(1.dp, Color(0xFF333744)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Decrement Button
                                Surface(
                                    onClick = onDecreaseFare,
                                    shape = CircleShape,
                                    color = Color(0xFF2C303B),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("fare_minus_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "—",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                // Center Fare Display
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "PKR $customFare",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 19.sp
                                    )
                                    Text(
                                        text = if (customFare >= option.baseFare) "Competitive fare • Fast pickup" else "Recommended PKR ${option.baseFare}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (customFare >= option.baseFare) Color(0xFF00E676) else InDriveTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Increment Button
                                Surface(
                                    onClick = onIncreaseFare,
                                    shape = CircleShape,
                                    color = Color(0xFF2C303B),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("fare_plus_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase Fare",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Fare Negotiation Chips: [-50 PKR], [Reset Base], [+50 PKR], [+100 PKR]
                        TactileFareAdjustmentChips(
                            currentFare = customFare,
                            baseFare = option.baseFare,
                            onSetFare = onSetFare
                        )
                    }
                }
            } else {
                // Unselected Ride Card with Price (~PKR xxx)
                Surface(
                    onClick = { onSelectOption(option) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ride_option_${option.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vehicle Graphic
                        Box(
                            modifier = Modifier.size(48.dp, 34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                option.isMoto -> MotoVehicleGraphic(modifier = Modifier.fillMaxSize())
                                option.isCourier -> CourierVehicleGraphic(modifier = Modifier.fillMaxSize())
                                else -> CarVehicleGraphic(
                                    hasAc = option.hasAc,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = InDriveTextPrimary,
                                    fontSize = 15.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (option.capacityText.isNotBlank()) {
                                    Text(
                                        text = option.capacityText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InDriveTextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = InDriveTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // Approximate Price (~PKR xxx)
                        Text(
                            text = "~PKR ${option.baseFare}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = InDriveTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Tax & Tolls Notice
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF20232B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = InDriveTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Fare doesn't include state entry tax, tolls, or parking fees",
                    style = MaterialTheme.typography.bodySmall,
                    color = InDriveTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * The fixed bottom element matching inDrive (Attachment 1, 2, 3):
 * Fixed at the bottom of the screen:
 * 1. "Auto-accept offer of PKR xxx" toggle switch row with send icon.
 * 2. Cash banknote icon + Bright Lime-Green "Find drivers" button + Tune/Filter icon.
 */
@Composable
fun InDriveFixedBottomBar(
    currentFare: Int,
    autoAcceptOffer: Boolean,
    onAutoAcceptChange: (Boolean) -> Unit,
    onFindDriversClick: () -> Unit,
    onPaymentMethodClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = InDriveDarkBg,
        modifier = modifier
            .fillMaxWidth()
            .testTag("indrive_fixed_bottom_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Row 1: Auto-accept offer toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Auto-accept offer of PKR $currentFare",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = autoAcceptOffer,
                    onCheckedChange = onAutoAcceptChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = InDriveLimeGreen,
                        uncheckedThumbColor = Color(0xFFA0A6B5),
                        uncheckedTrackColor = Color(0xFF333742)
                    ),
                    modifier = Modifier.testTag("auto_accept_switch")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Cash icon + Bright Lime "Find drivers" button + Tune options icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cash Banknote Icon
                Surface(
                    onClick = onPaymentMethodClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("payment_method_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Green banknote graphic
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(26.dp, 16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        }
                    }
                }

                // Unified Brand Fuchsia "Find drivers" Button
                Button(
                    onClick = onFindDriversClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InDriveLimeGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("find_drivers_btn")
                ) {
                    Text(
                        text = "Find drivers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                // Tune / Filter Options Icon
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("filter_options_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Preferences",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top inDrive-style Route Panel (Attachment 1)
 */
@Composable
fun InDriveRouteTopCard(
    pickupTitle: String,
    destinationTitle: String,
    durationMinutes: Int,
    pickupLat: Double = 0.0,
    pickupLon: Double = 0.0,
    destinationLat: Double = 0.0,
    destinationLon: Double = 0.0,
    onPickupClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onAddStopClick: () -> Unit,
    onPickPickupOnMap: (() -> Unit)? = null,
    onPickDestinationOnMap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1B1D23).copy(alpha = 0.98f),
        border = BorderStroke(1.dp, Color(0xFF333742)),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("indrive_top_route_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Pickup Location
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // White figure walking / pickup icon
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Pickup Location",
                    tint = Color(0xFF81C784),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onPickupClick() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPickupClick() }
                ) {
                    Text(
                        text = pickupTitle.ifBlank { "Choose pickup location" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val pLat = if (pickupLat != 0.0) pickupLat else 34.0151
                    val pLon = if (pickupLon != 0.0) pickupLon else 71.5249
                    Text(
                        text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", pLat, pLon),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF81D4FA)
                    )
                }

                if (onPickPickupOnMap != null) {
                    IconButton(
                        onClick = onPickPickupOnMap,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Select Pickup on Map",
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Row 2: Destination Location + Duration + Add Stop (+) Button
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // White Flag icon
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Destination Location",
                    tint = Color(0xFFFF80AB),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDestinationClick() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDestinationClick() }
                ) {
                    Text(
                        text = "${destinationTitle.ifBlank { "Where to?" }} ~$durationMinutes min.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dLat = if (destinationLat != 0.0) destinationLat else 34.0351
                    val dLon = if (destinationLon != 0.0) destinationLon else 71.5449
                    Text(
                        text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", dLat, dLon),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF8A80)
                    )
                }

                if (onPickDestinationOnMap != null) {
                    IconButton(
                        onClick = onPickDestinationOnMap,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Select Destination on Map",
                            tint = Color(0xFFFF80AB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Plus Icon button on right
                IconButton(
                    onClick = onAddStopClick,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add stop",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Custom Vector Graphics for Car, Moto, and Couriers
 */

@Composable
fun CarVehicleGraphic(
    hasAc: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val carBottom = h * 0.88f
        val carLeft = w * 0.08f
        val carRight = w * 0.92f
        val carW = carRight - carLeft
        val carH = carBottom - h * 0.25f

        val hoodTop = h * 0.52f
        val roofTop = h * 0.28f

        // Cabin/Windows
        val cabinPath = Path().apply {
            moveTo(carLeft + carW * 0.18f, hoodTop)
            lineTo(carLeft + carW * 0.36f, roofTop)
            lineTo(carLeft + carW * 0.72f, roofTop)
            lineTo(carLeft + carW * 0.88f, hoodTop)
            close()
        }
        drawPath(cabinPath, color = Color(0xFF1E2028))

        // Body
        val bodyPath = Path().apply {
            moveTo(carLeft, hoodTop + carH * 0.2f)
            lineTo(carLeft + carW * 0.12f, hoodTop)
            lineTo(carLeft + carW * 0.92f, hoodTop)
            lineTo(carRight, hoodTop + carH * 0.25f)
            lineTo(carRight, carBottom - carH * 0.12f)
            lineTo(carLeft, carBottom - carH * 0.12f)
            close()
        }
        drawPath(
            bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC)),
                startY = hoodTop,
                endY = carBottom
            )
        )

        // Headlight
        drawRoundRect(
            color = Color(0xFFFFF59D),
            topLeft = Offset(carLeft + 1.dp.toPx(), hoodTop + carH * 0.1f),
            size = Size(carW * 0.14f, carH * 0.18f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Wheels
        val wheelRadius = carH * 0.24f
        val wheelY = carBottom - carH * 0.06f
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = Offset(carLeft + carW * 0.28f, wheelY))
        drawCircle(Color(0xFFB0BEC5), radius = wheelRadius * 0.5f, center = Offset(carLeft + carW * 0.28f, wheelY))
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = Offset(carLeft + carW * 0.76f, wheelY))
        drawCircle(Color(0xFFB0BEC5), radius = wheelRadius * 0.5f, center = Offset(carLeft + carW * 0.76f, wheelY))

        // Snowflake badge for AC
        if (hasAc) {
            val badgeCenter = Offset(carLeft + 3.dp.toPx(), h * 0.38f)
            drawCircle(
                color = Color(0xFF29B6F6),
                radius = 8.dp.toPx(),
                center = badgeCenter
            )
            // Snowflake lines
            for (angle in 0..120 step 60) {
                val rad = Math.toRadians(angle.toDouble())
                val dx = (Math.cos(rad) * 5.dp.toPx()).toFloat()
                val dy = (Math.sin(rad) * 5.dp.toPx()).toFloat()
                drawLine(
                    color = Color.White,
                    start = Offset(badgeCenter.x - dx, badgeCenter.y - dy),
                    end = Offset(badgeCenter.x + dx, badgeCenter.y + dy),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun MotoVehicleGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bottomY = h * 0.88f

        val wheelRadius = h * 0.22f
        val frontWheelCenter = Offset(w * 0.22f, bottomY - wheelRadius * 0.5f)
        val rearWheelCenter = Offset(w * 0.78f, bottomY - wheelRadius * 0.5f)

        // Wheels
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = frontWheelCenter, style = Stroke(3.dp.toPx()))
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = rearWheelCenter, style = Stroke(3.dp.toPx()))

        // Green body/chassis
        val bikeBody = Path().apply {
            moveTo(frontWheelCenter.x, frontWheelCenter.y)
            lineTo(w * 0.40f, h * 0.32f) // Handlebars
            lineTo(w * 0.60f, h * 0.42f) // Seat
            lineTo(rearWheelCenter.x, rearWheelCenter.y)
            lineTo(w * 0.48f, h * 0.65f) // Engine
            close()
        }
        drawPath(bikeBody, color = Color(0xFF00E676))

        // Handlebars & Headlight
        drawLine(
            color = Color(0xFF37474F),
            start = Offset(w * 0.38f, h * 0.30f),
            end = Offset(w * 0.46f, h * 0.36f),
            strokeWidth = 3.dp.toPx()
        )
        drawCircle(Color(0xFFFFF59D), radius = 3.dp.toPx(), center = Offset(w * 0.34f, h * 0.38f))
    }
}

@Composable
fun CourierVehicleGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bottomY = h * 0.88f

        val wheelRadius = h * 0.20f
        val frontWheelCenter = Offset(w * 0.22f, bottomY - wheelRadius * 0.5f)
        val rearWheelCenter = Offset(w * 0.78f, bottomY - wheelRadius * 0.5f)

        // Wheels
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = frontWheelCenter, style = Stroke(3.dp.toPx()))
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = rearWheelCenter, style = Stroke(3.dp.toPx()))

        // Green Scooter Body
        val scooterBody = Path().apply {
            moveTo(frontWheelCenter.x, frontWheelCenter.y)
            lineTo(w * 0.35f, h * 0.36f)
            lineTo(w * 0.52f, h * 0.68f) // Floorboard
            lineTo(w * 0.68f, h * 0.50f) // Seat base
            lineTo(rearWheelCenter.x, rearWheelCenter.y)
            close()
        }
        drawPath(scooterBody, color = Color(0xFF00C853))

        // Orange / Brown Delivery Cargo Box on Back Rack
        val boxLeft = w * 0.60f
        val boxTop = h * 0.26f
        val boxW = w * 0.28f
        val boxH = h * 0.32f
        drawRoundRect(
            color = Color(0xFFFF9800),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Box Tape stripe
        drawLine(
            color = Color(0xFFE65100),
            start = Offset(boxLeft, boxTop + boxH * 0.5f),
            end = Offset(boxLeft + boxW, boxTop + boxH * 0.5f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Tactile, fast-tap fare adjustment chips for peer-to-peer negotiation.
 * Delivers instant haptic feedback without requiring keyboard input.
 */
@Composable
fun TactileFareAdjustmentChips(
    currentFare: Int,
    baseFare: Int = currentFare,
    onSetFare: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val chips = listOf(
        "-50" to (currentFare - 50).coerceAtLeast(50),
        "Reset Base" to baseFare,
        "+50" to (currentFare + 50),
        "+100" to (currentFare + 100)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { (label, targetFare) ->
            val isSelected = currentFare == targetFare && label != "Reset Base"
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSetFare(targetFare)
                },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) InDriveLimeGreen.copy(alpha = 0.22f) else Color(0xFF242833),
                border = BorderStroke(
                    1.2.dp,
                    if (isSelected) InDriveLimeGreen else Color(0xFF373E4F)
                ),
                shadowElevation = if (isSelected) 3.dp else 1.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("fare_chip_${label.replace(" ", "_")}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (label == "Reset Base") "PKR $baseFare" else "$label PKR",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) InDriveLimeGreen else Color.White
                    )
                }
            }
        }
    }
}

enum class SmartBadgeType {
    FASTEST_ARRIVAL,
    BEST_RATED,
    LOWEST_FARE
}

private data class BadgeStyle(
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun SmartBadge(
    type: SmartBadgeType,
    label: String = when (type) {
        SmartBadgeType.FASTEST_ARRIVAL -> "Fastest Arrival"
        SmartBadgeType.BEST_RATED -> "Best Rated"
        SmartBadgeType.LOWEST_FARE -> "Lowest Fare"
    },
    modifier: Modifier = Modifier
) {
    val style = when (type) {
        SmartBadgeType.FASTEST_ARRIVAL -> BadgeStyle(
            Color(0xFF00E676).copy(alpha = 0.16f),
            Color(0xFF00E676),
            Color(0xFF00E676),
            Icons.Default.Bolt
        )
        SmartBadgeType.BEST_RATED -> BadgeStyle(
            Color(0xFFFFB300).copy(alpha = 0.18f),
            Color(0xFFFFB300),
            Color(0xFFFFD54F),
            Icons.Default.Star
        )
        SmartBadgeType.LOWEST_FARE -> BadgeStyle(
            Color(0xFF00B0FF).copy(alpha = 0.16f),
            Color(0xFF00B0FF),
            Color(0xFF80D8FF),
            Icons.Default.LocalOffer
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = style.bgColor,
        border = BorderStroke(1.dp, style.borderColor.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.textColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = style.textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * Smart Visual Badged Driver Bid Card.
 * Solves choice paralysis when multiple drivers send bids simultaneously:
 * - "Fastest Arrival" (ETA < 3 min)
 * - "Best Rated" (⭐ 4.9+)
 * - "Lowest Fare" (Exact passenger price match)
 */
@Composable
fun SmartDriverBidCard(
    offer: DriverOffer,
    passengerRequestedFare: Int,
    isFastest: Boolean,
    isBestRated: Boolean,
    isLowestFare: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCounterOffer: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showCounterRow by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E212B),
        border = BorderStroke(
            1.5.dp,
            if (isFastest || isLowestFare) Color(0xFF00E676).copy(alpha = 0.6f) else Color(0xFF333A4C)
        ),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("driver_bid_card_${offer.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Smart Badges Row (if any badges apply)
            if (isFastest || isBestRated || isLowestFare) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFastest) {
                        SmartBadge(
                            type = SmartBadgeType.FASTEST_ARRIVAL,
                            label = "Fastest (~${offer.etaMinutes}m)"
                        )
                    }
                    if (isBestRated) {
                        SmartBadge(
                            type = SmartBadgeType.BEST_RATED,
                            label = "Best Rated (${String.format(Locale.US, "%.1f", offer.driverRating)}★)"
                        )
                    }
                    if (isLowestFare) {
                        SmartBadge(
                            type = SmartBadgeType.LOWEST_FARE,
                            label = if (offer.offeredFare == passengerRequestedFare) "Exact Match" else "Lowest Fare"
                        )
                    }
                }
            }

            // Driver Profile & Vehicle Info + Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Driver Avatar
                Surface(
                    shape = CircleShape,
                    color = DrigoBrandPurple.copy(alpha = 0.25f),
                    border = BorderStroke(1.5.dp, InDriveLimeGreen),
                    modifier = Modifier.size(46.dp)
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

                // Driver Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = offer.driverName.ifBlank { "Captain" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Rating Pill
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
                                    text = String.format(Locale.US, "%.1f", offer.driverRating),
                                    color = Color(0xFFFFB300),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${offer.driverVehicleColor} ${offer.driverVehicleMake} ${offer.driverVehicleModel}".trim().ifBlank { "Sedan Comfort" },
                        color = Color(0xFFA0A6B5),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (offer.driverPlateNumber.isNotBlank()) {
                            Text(
                                text = offer.driverPlateNumber,
                                color = Color(0xFF00E676),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "• ~${offer.etaMinutes} min (${String.format(Locale.US, "%.1f", offer.distanceKmAway)} km)",
                            color = Color(0xFF81D4FA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Fare Display & Difference
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PKR ${offer.offeredFare}",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                    val fareDiff = offer.offeredFare - passengerRequestedFare
                    Text(
                        text = when {
                            fareDiff == 0 -> "Exact match"
                            fareDiff > 0 -> "+PKR $fareDiff"
                            else -> "-PKR ${-fareDiff}"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            fareDiff <= 0 -> Color(0xFF00E676)
                            fareDiff <= 50 -> Color(0xFFFFD54F)
                            else -> Color(0xFFFF8A80)
                        }
                    )
                }
            }

            // Quick Counter Row (if expanded)
            AnimatedVisibility(visible = showCounterRow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Quick Counter to Captain:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0A6B5)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val counterChips = listOf(
                            "-50" to (offer.offeredFare - 50).coerceAtLeast(50),
                            "Your PKR $passengerRequestedFare" to passengerRequestedFare,
                            "+50" to (offer.offeredFare + 50)
                        )
                        counterChips.forEach { (label, targetFare) ->
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCounterOffer?.invoke(targetFare)
                                    showCounterRow = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF282D3B),
                                border = BorderStroke(1.dp, Color(0xFF424B60)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Accept & Decline (+ Counter toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Button
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDecline()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF3E465A)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A6B5)),
                    modifier = Modifier
                        .weight(0.32f)
                        .height(44.dp)
                        .testTag("decline_bid_${offer.id}")
                ) {
                    Text(text = "Decline", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (onCounterOffer != null && !showCounterRow) {
                    OutlinedButton(
                        onClick = { showCounterRow = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                        modifier = Modifier
                            .weight(0.30f)
                            .height(44.dp)
                    ) {
                        Text(text = "Counter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Primary Accept Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAccept()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InDriveLimeGreen),
                    modifier = Modifier
                        .weight(if (onCounterOffer != null && !showCounterRow) 0.38f else 0.68f)
                        .height(44.dp)
                        .testTag("accept_bid_${offer.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Accept",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

