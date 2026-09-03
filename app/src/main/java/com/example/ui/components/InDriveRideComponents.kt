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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DrigoBrandPurple

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

                        // Quick Fare Negotiation Chips: [-50], [Reset Base], [+50], [+100]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val chips = listOf(
                                "-50" to (customFare - 50).coerceAtLeast(50),
                                "PKR ${option.baseFare}" to option.baseFare,
                                "+50" to (customFare + 50),
                                "+100" to (customFare + 100)
                            )
                            chips.forEach { (label, targetFare) ->
                                Surface(
                                    onClick = { onSetFare(targetFare) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (customFare == targetFare) InDriveLimeGreen.copy(alpha = 0.25f) else Color(0xFF262A35),
                                    border = BorderStroke(
                                        1.dp,
                                        if (customFare == targetFare) InDriveLimeGreen else Color(0xFF353B4B)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (customFare == targetFare) InDriveLimeGreen else Color.White
                                        )
                                    }
                                }
                            }
                        }
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
                Text(
                    text = pickupTitle.ifBlank { "Choose pickup location" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPickupClick() }
                )

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
                Text(
                    text = "${destinationTitle.ifBlank { "Where to?" }} ~$durationMinutes min.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDestinationClick() }
                )

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
