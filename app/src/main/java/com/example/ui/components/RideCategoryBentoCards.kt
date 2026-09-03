package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DrigoBrandPurple

@Composable
fun RideCategoryBentoCards(
    selectedCategory: String?,
    onShareRideClick: () -> Unit,
    onSendParcelClick: () -> Unit,
    onRequestCarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isShareSelected = selectedCategory == "Share Ride"
    val isParcelSelected = selectedCategory == "Parcel" || selectedCategory == "Couriers"
    val isCarSelected = selectedCategory == "Book Car" || selectedCategory == "Book a Car" ||
            selectedCategory == "Private AC" || selectedCategory == "Ride A/C" ||
            (selectedCategory == null)

    val cardBg = Color(0xFF282A33)
    val cardBorderDefault = Color(0xFF353945)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Column: 2 Cards ("Share your ride" & "Send a parcel")
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Top Left Card: Share your ride
            Surface(
                onClick = onShareRideClick,
                shape = RoundedCornerShape(14.dp),
                color = if (isShareSelected) Color(0xFF2E313D) else cardBg,
                border = BorderStroke(
                    if (isShareSelected) 1.8.dp else 1.dp,
                    if (isShareSelected) DrigoBrandPurple else cardBorderDefault
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("bento_share_ride_card")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Share your ride",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 10.dp)
                    )

                    ShareRideGraphic(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth(0.92f)
                            .height(48.dp)
                    )
                }
            }

            // 2. Bottom Left Card: Send a parcel
            Surface(
                onClick = onSendParcelClick,
                shape = RoundedCornerShape(14.dp),
                color = if (isParcelSelected) Color(0xFF2E313D) else cardBg,
                border = BorderStroke(
                    if (isParcelSelected) 1.8.dp else 1.dp,
                    if (isParcelSelected) DrigoBrandPurple else cardBorderDefault
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("bento_send_parcel_card")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Send a parcel",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 10.dp)
                    )

                    SendParcelGraphic(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth(0.92f)
                            .height(48.dp)
                    )
                }
            }
        }

        // Right Column: Tall Card ("Request a car")
        Surface(
            onClick = onRequestCarClick,
            shape = RoundedCornerShape(14.dp),
            color = if (isCarSelected) Color(0xFF2E313D) else cardBg,
            border = BorderStroke(
                if (isCarSelected) 1.8.dp else 1.dp,
                if (isCarSelected) DrigoBrandPurple else cardBorderDefault
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("bento_request_car_card")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Request a car",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                )

                RequestCarGraphic(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(76.dp)
                )
            }
        }
    }
}

/**
 * Graphic for "Share your ride": White/Silver car with 2 passengers (Woman in coral, Man in blue)
 */
@Composable
private fun ShareRideGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Draw Sleek Car
        val carLeft = w * 0.05f
        val carRight = w * 0.65f
        val carBottom = h * 0.92f

        drawCarBody(
            left = carLeft,
            top = h * 0.35f,
            right = carRight,
            bottom = carBottom
        )

        // 2. Female Passenger (Coral top, brunette hair)
        val p1CenterX = w * 0.68f
        val p1HeadY = h * 0.42f
        val p1HeadR = h * 0.16f

        // Head
        drawCircle(
            color = Color(0xFFFFCC80),
            radius = p1HeadR,
            center = Offset(p1CenterX, p1HeadY)
        )
        // Hair
        drawArc(
            color = Color(0xFF4E342E),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(p1CenterX - p1HeadR, p1HeadY - p1HeadR),
            size = Size(p1HeadR * 2, p1HeadR * 2)
        )
        // Body (Coral dress/top)
        val p1BodyPath = Path().apply {
            moveTo(p1CenterX - p1HeadR * 1.3f, carBottom)
            lineTo(p1CenterX - p1HeadR * 0.6f, p1HeadY + p1HeadR * 0.7f)
            lineTo(p1CenterX + p1HeadR * 0.6f, p1HeadY + p1HeadR * 0.7f)
            lineTo(p1CenterX + p1HeadR * 1.3f, carBottom)
            close()
        }
        drawPath(p1BodyPath, color = Color(0xFFFF6F59))

        // 3. Male Passenger (Blue shirt, dark hair)
        val p2CenterX = w * 0.86f
        val p2HeadY = h * 0.38f
        val p2HeadR = h * 0.17f

        // Head
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = p2HeadR,
            center = Offset(p2CenterX, p2HeadY)
        )
        // Hair
        drawArc(
            color = Color(0xFF1E2026),
            startAngle = 160f,
            sweepAngle = 200f,
            useCenter = true,
            topLeft = Offset(p2CenterX - p2HeadR * 1.05f, p2HeadY - p2HeadR * 1.05f),
            size = Size(p2HeadR * 2.1f, p2HeadR * 1.6f)
        )
        // Body (Blue shirt)
        val p2BodyPath = Path().apply {
            moveTo(p2CenterX - p2HeadR * 1.4f, carBottom)
            lineTo(p2CenterX - p2HeadR * 0.7f, p2HeadY + p2HeadR * 0.7f)
            lineTo(p2CenterX + p2HeadR * 0.7f, p2HeadY + p2HeadR * 0.7f)
            lineTo(p2CenterX + p2HeadR * 1.4f, carBottom)
            close()
        }
        drawPath(p2BodyPath, color = Color(0xFF448AFF))
    }
}

/**
 * Graphic for "Send a parcel": White/Silver car with yellow postal envelope & green parcel bag
 */
@Composable
private fun SendParcelGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val carLeft = w * 0.05f
        val carRight = w * 0.65f
        val carBottom = h * 0.92f

        // 1. Draw Sleek Car
        drawCarBody(
            left = carLeft,
            top = h * 0.35f,
            right = carRight,
            bottom = carBottom
        )

        // 2. Green Delivery / Shopping Bag
        val bagLeft = w * 0.68f
        val bagTop = h * 0.40f
        val bagWidth = w * 0.28f
        val bagHeight = carBottom - bagTop

        drawRoundRect(
            color = Color(0xFF2E7D32),
            topLeft = Offset(bagLeft, bagTop),
            size = Size(bagWidth, bagHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Bag Handle
        drawArc(
            color = Color(0xFF81C784),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(bagLeft + bagWidth * 0.25f, bagTop - 5.dp.toPx()),
            size = Size(bagWidth * 0.5f, 10.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Yellow Postal Envelope / Parcel in front
        val envLeft = w * 0.58f
        val envTop = h * 0.56f
        val envWidth = w * 0.26f
        val envHeight = carBottom - envTop

        drawRoundRect(
            color = Color(0xFFFFCA28),
            topLeft = Offset(envLeft, envTop),
            size = Size(envWidth, envHeight),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Envelope Flap Lines
        val flapPath = Path().apply {
            moveTo(envLeft, envTop)
            lineTo(envLeft + envWidth * 0.5f, envTop + envHeight * 0.55f)
            lineTo(envLeft + envWidth, envTop)
        }
        drawPath(
            flapPath,
            color = Color(0xFFFFA000),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Mini label
        drawRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(envLeft + envWidth * 0.65f, envTop + envHeight * 0.65f),
            size = Size(envWidth * 0.25f, envHeight * 0.22f)
        )
    }
}

/**
 * Graphic for "Request a car": Blue road direction signpost, sleek modern car, and green luggage suitcase
 */
@Composable
private fun RequestCarGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bottomY = h * 0.94f

        // 1. Blue Signpost on the left
        val poleX = w * 0.16f
        val poleTopY = h * 0.22f

        // Pole
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(poleX, poleTopY),
            end = Offset(poleX, bottomY),
            strokeWidth = 2.5.dp.toPx()
        )
        // Arrow Sign (Blue pointing left)
        val signLeft = w * 0.04f
        val signRight = w * 0.30f
        val signTop = poleTopY + 3.dp.toPx()
        val signHeight = 14.dp.toPx()

        val signPath = Path().apply {
            moveTo(signLeft + 6.dp.toPx(), signTop)
            lineTo(signRight, signTop)
            lineTo(signRight, signTop + signHeight)
            lineTo(signLeft + 6.dp.toPx(), signTop + signHeight)
            lineTo(signLeft, signTop + signHeight / 2)
            close()
        }
        drawPath(signPath, color = Color(0xFF64B5F6))

        // 2. Modern Silver Car in center
        val carLeft = w * 0.18f
        val carRight = w * 0.74f
        drawCarBody(
            left = carLeft,
            top = h * 0.32f,
            right = carRight,
            bottom = bottomY
        )

        // 3. Green Luggage / Suitcase on the right
        val bagLeft = w * 0.70f
        val bagTop = h * 0.28f
        val bagWidth = w * 0.26f
        val bagHeight = bottomY - bagTop

        // Telescopic Handle
        val handleW = bagWidth * 0.45f
        val handleLeft = bagLeft + (bagWidth - handleW) / 2
        val handleTop = bagTop - 9.dp.toPx()
        drawRoundRect(
            color = Color(0xFF37474F),
            topLeft = Offset(handleLeft, handleTop),
            size = Size(handleW, 10.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Suitcase Body (Lime Green)
        drawRoundRect(
            color = Color(0xFF76FF03),
            topLeft = Offset(bagLeft, bagTop),
            size = Size(bagWidth, bagHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Vertical Grooves on Suitcase
        val grooveColor = Color(0xFF558B2F)
        val step = bagWidth / 4
        for (i in 1..3) {
            val gx = bagLeft + step * i
            drawLine(
                color = grooveColor,
                start = Offset(gx, bagTop + 4.dp.toPx()),
                end = Offset(gx, bottomY - 4.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // Mini luggage wheels
        drawCircle(
            color = Color(0xFF263238),
            radius = 2.5.dp.toPx(),
            center = Offset(bagLeft + bagWidth * 0.22f, bottomY)
        )
        drawCircle(
            color = Color(0xFF263238),
            radius = 2.5.dp.toPx(),
            center = Offset(bagLeft + bagWidth * 0.78f, bottomY)
        )
    }
}

/**
 * Helper to draw a sleek modern silver car body with wheels & headlights
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCarBody(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
) {
    val carW = right - left
    val carH = bottom - top

    val roofTop = top + carH * 0.05f
    val hoodTop = top + carH * 0.45f
    val bodyBottom = bottom - carH * 0.12f

    // 1. Car Cabin & Windows (Dark tint)
    val cabinPath = Path().apply {
        moveTo(left + carW * 0.15f, hoodTop)
        lineTo(left + carW * 0.32f, roofTop)
        lineTo(left + carW * 0.75f, roofTop)
        lineTo(left + carW * 0.92f, hoodTop)
        close()
    }
    drawPath(cabinPath, color = Color(0xFF1E2028))

    // Windshield reflection
    val glassPath = Path().apply {
        moveTo(left + carW * 0.22f, hoodTop - 1.dp.toPx())
        lineTo(left + carW * 0.35f, roofTop + 2.dp.toPx())
        lineTo(left + carW * 0.52f, roofTop + 2.dp.toPx())
        lineTo(left + carW * 0.50f, hoodTop - 1.dp.toPx())
        close()
    }
    drawPath(glassPath, color = Color(0xFF90A4AE).copy(alpha = 0.5f))

    // 2. Car Lower Body (Sleek metallic silver/white)
    val bodyPath = Path().apply {
        moveTo(left, hoodTop + carH * 0.2f)
        lineTo(left + carW * 0.12f, hoodTop)
        lineTo(left + carW * 0.95f, hoodTop)
        lineTo(right, hoodTop + carH * 0.25f)
        lineTo(right, bodyBottom)
        lineTo(left, bodyBottom)
        close()
    }
    drawPath(
        bodyPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC), Color(0xFF90A4AE)),
            startY = hoodTop,
            endY = bodyBottom
        )
    )

    // Front Headlight Glow
    drawRoundRect(
        color = Color(0xFFFFF59D),
        topLeft = Offset(left + 2.dp.toPx(), hoodTop + carH * 0.08f),
        size = Size(carW * 0.14f, carH * 0.16f),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Front Grill bar
    drawLine(
        color = Color(0xFF37474F),
        start = Offset(left + 2.dp.toPx(), hoodTop + carH * 0.26f),
        end = Offset(left + carW * 0.20f, hoodTop + carH * 0.26f),
        strokeWidth = 1.5.dp.toPx()
    )

    // 3. Wheels
    val wheelRadius = carH * 0.24f
    val frontWheelX = left + carW * 0.26f
    val rearWheelX = left + carW * 0.78f

    // Front Wheel
    drawCircle(
        color = Color(0xFF1E2028),
        radius = wheelRadius,
        center = Offset(frontWheelX, bodyBottom)
    )
    drawCircle(
        color = Color(0xFFB0BEC5),
        radius = wheelRadius * 0.55f,
        center = Offset(frontWheelX, bodyBottom)
    )

    // Rear Wheel
    drawCircle(
        color = Color(0xFF1E2028),
        radius = wheelRadius,
        center = Offset(rearWheelX, bodyBottom)
    )
    drawCircle(
        color = Color(0xFFB0BEC5),
        radius = wheelRadius * 0.55f,
        center = Offset(rearWheelX, bodyBottom)
    )
}
