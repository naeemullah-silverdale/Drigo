package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InDriveLimeGreen

/**
 * DriverLocationOffScreen
 * Matches the official inDrive / Drigo "Turn your location on" design:
 * - Rendered when device GPS/Location service is turned off or location permission is missing.
 * - Displays the signature green car & skyline illustration with the location pin.
 * - Explains clearly: "Your location info is needed to find ride requests in your current area".
 * - "Go to settings" action button opens Android Location Settings to switch on GPS.
 */
@Composable
fun DriverLocationOffScreen(
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF13151B))
            .testTag("driver_location_off_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stylized vector illustration matching the screenshot
            LocationOffCarIllustration(
                modifier = Modifier
                    .size(240.dp)
                    .padding(bottom = 16.dp)
            )

            // Primary Title
            Text(
                text = "Turn your location on",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle explanation
            Text(
                text = "Your location info is needed to find ride requests in your current area",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFA0A6B5),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // "Go to settings" Button
            Button(
                onClick = onGoToSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InDriveLimeGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("driver_go_to_settings_button")
            ) {
                Text(
                    text = "Go to settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * Custom vector drawing of the inDrive green car, skyline buildings, roadway, and location pin.
 * Rendered using Canvas DrawScope for ultra-fast, smooth rendering across low-end and high-end hardware.
 */
@Composable
fun LocationOffCarIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. City Skyline (Stylized Building Outlines in Background)
        // Building 1 (Left tall building)
        drawRect(
            color = Color(0xFF262C38),
            topLeft = Offset(w * 0.16f, h * 0.28f),
            size = Size(w * 0.20f, h * 0.35f),
            style = Fill
        )
        drawRect(
            color = Color(0xFF374151),
            topLeft = Offset(w * 0.16f, h * 0.28f),
            size = Size(w * 0.20f, h * 0.35f),
            style = Stroke(width = 2f)
        )
        // Antenna spire on left building
        drawLine(
            color = Color(0xFF6B7280),
            start = Offset(w * 0.26f, h * 0.28f),
            end = Offset(w * 0.26f, h * 0.20f),
            strokeWidth = 2.5f
        )

        // Building 2 (Center-left stepped tower)
        drawRect(
            color = Color(0xFF1E232F),
            topLeft = Offset(w * 0.30f, h * 0.33f),
            size = Size(w * 0.24f, h * 0.30f),
            style = Fill
        )
        drawRect(
            color = Color(0xFF4B5563),
            topLeft = Offset(w * 0.30f, h * 0.33f),
            size = Size(w * 0.24f, h * 0.30f),
            style = Stroke(width = 2f)
        )

        // Building 3 (Right building with white facade)
        drawRect(
            color = Color(0xFFECEFF1),
            topLeft = Offset(w * 0.64f, h * 0.30f),
            size = Size(w * 0.18f, h * 0.38f),
            style = Fill
        )
        drawRect(
            color = Color(0xFF374151),
            topLeft = Offset(w * 0.64f, h * 0.30f),
            size = Size(w * 0.18f, h * 0.38f),
            style = Stroke(width = 2.5f)
        )
        // Window slots on right white building
        drawRect(
            color = Color(0xFF90A4AE),
            topLeft = Offset(w * 0.66f, h * 0.34f),
            size = Size(w * 0.04f, h * 0.03f)
        )
        drawRect(
            color = Color(0xFF90A4AE),
            topLeft = Offset(w * 0.72f, h * 0.34f),
            size = Size(w * 0.04f, h * 0.03f)
        )
        drawRect(
            color = Color(0xFF90A4AE),
            topLeft = Offset(w * 0.66f, h * 0.40f),
            size = Size(w * 0.04f, h * 0.03f)
        )
        drawRect(
            color = Color(0xFF90A4AE),
            topLeft = Offset(w * 0.72f, h * 0.40f),
            size = Size(w * 0.04f, h * 0.03f)
        )

        // 2. Perspective Road Leading Into Foreground
        val roadPath = Path().apply {
            moveTo(w * 0.65f, h * 0.48f)
            lineTo(w * 0.74f, h * 0.48f)
            lineTo(w * 0.72f, h * 0.85f)
            lineTo(w * 0.50f, h * 0.85f)
            close()
        }
        drawPath(
            path = roadPath,
            color = Color(0xFFB0BEC5)
        )
        drawPath(
            path = roadPath,
            color = Color(0xFF1E222D),
            style = Stroke(width = 3.5f)
        )

        // Road curb / diagonal divider
        drawLine(
            color = Color(0xFF1E222D),
            start = Offset(w * 0.65f, h * 0.48f),
            end = Offset(w * 0.54f, h * 0.85f),
            strokeWidth = 4f
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.66f, h * 0.49f),
            end = Offset(w * 0.56f, h * 0.85f),
            strokeWidth = 2.5f
        )

        // 3. Green Car (Stylized InDrive Sedan in 3/4 angle)
        // Wheels
        drawRoundRect(
            color = Color(0xFF11141B),
            topLeft = Offset(w * 0.28f, h * 0.65f),
            size = Size(w * 0.08f, h * 0.12f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = Color(0xFF11141B),
            topLeft = Offset(w * 0.49f, h * 0.63f),
            size = Size(w * 0.07f, h * 0.13f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Hubcaps
        drawCircle(
            color = Color(0xFFCFD8DC),
            radius = w * 0.02f,
            center = Offset(w * 0.515f, h * 0.70f)
        )

        // Car Main Body (Bright Lime Green)
        val carBodyColor = InDriveLimeGreen
        val carBodyPath = Path().apply {
            moveTo(w * 0.28f, h * 0.59f)
            // Front hood
            cubicTo(w * 0.31f, h * 0.57f, w * 0.36f, h * 0.56f, w * 0.43f, h * 0.55f)
            // Windshield slant
            lineTo(w * 0.48f, h * 0.47f)
            // Roof
            lineTo(w * 0.58f, h * 0.48f)
            // Rear window & trunk
            cubicTo(w * 0.60f, h * 0.50f, w * 0.61f, h * 0.54f, w * 0.60f, h * 0.58f)
            // Rear side door
            lineTo(w * 0.57f, h * 0.67f)
            // Bottom sill
            lineTo(w * 0.30f, h * 0.67f)
            // Front bumper curve
            cubicTo(w * 0.26f, h * 0.66f, w * 0.26f, h * 0.61f, w * 0.28f, h * 0.59f)
            close()
        }
        drawPath(path = carBodyPath, color = carBodyColor)
        drawPath(path = carBodyPath, color = Color(0xFF1E222D), style = Stroke(width = 3.5f))

        // Windshield (Dark Tinted Glass)
        val windshieldPath = Path().apply {
            moveTo(w * 0.38f, h * 0.56f)
            lineTo(w * 0.47f, h * 0.48f)
            lineTo(w * 0.52f, h * 0.485f)
            lineTo(w * 0.44f, h * 0.56f)
            close()
        }
        drawPath(path = windshieldPath, color = Color(0xFF1E222D))

        // Side Passenger Windows
        val sideWindowPath = Path().apply {
            moveTo(w * 0.528f, h * 0.49f)
            lineTo(w * 0.575f, h * 0.495f)
            lineTo(w * 0.565f, h * 0.56f)
            lineTo(w * 0.455f, h * 0.56f)
            close()
        }
        drawPath(path = sideWindowPath, color = Color(0xFF1E222D))

        // Door Cut Line
        drawLine(
            color = Color(0xFF1E222D),
            start = Offset(w * 0.51f, h * 0.49f),
            end = Offset(w * 0.51f, h * 0.66f),
            strokeWidth = 2.5f
        )

        // Front Headlights (Bright White/Cyan)
        val leftHeadlight = Path().apply {
            moveTo(w * 0.27f, h * 0.61f)
            lineTo(w * 0.31f, h * 0.60f)
            lineTo(w * 0.31f, h * 0.63f)
            lineTo(w * 0.28f, h * 0.635f)
            close()
        }
        drawPath(path = leftHeadlight, color = Color.White)
        drawPath(path = leftHeadlight, color = Color(0xFF1E222D), style = Stroke(width = 1.5f))

        val rightHeadlight = Path().apply {
            moveTo(w * 0.42f, h * 0.59f)
            lineTo(w * 0.48f, h * 0.595f)
            lineTo(w * 0.47f, h * 0.63f)
            lineTo(w * 0.41f, h * 0.625f)
            close()
        }
        drawPath(path = rightHeadlight, color = Color.White)
        drawPath(path = rightHeadlight, color = Color(0xFF1E222D), style = Stroke(width = 1.5f))

        // Front Bumper Air Intake / Grille
        drawRoundRect(
            color = Color(0xFF1E222D),
            topLeft = Offset(w * 0.30f, h * 0.64f),
            size = Size(w * 0.12f, h * 0.022f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // 4. Large Lime-Green Location Marker Pin Floating Above Car
        val pinCenter = Offset(w * 0.51f, h * 0.32f)
        val pinRadius = w * 0.065f

        val pinPath = Path().apply {
            // Circular head
            moveTo(pinCenter.x - pinRadius, pinCenter.y)
            cubicTo(
                pinCenter.x - pinRadius, pinCenter.y - pinRadius * 1.1f,
                pinCenter.x + pinRadius, pinCenter.y - pinRadius * 1.1f,
                pinCenter.x + pinRadius, pinCenter.y
            )
            // Tapered pointer pointing down
            cubicTo(
                pinCenter.x + pinRadius, pinCenter.y + pinRadius * 0.6f,
                pinCenter.x + pinRadius * 0.3f, pinCenter.y + pinRadius * 1.3f,
                pinCenter.x, pinCenter.y + pinRadius * 1.9f
            )
            cubicTo(
                pinCenter.x - pinRadius * 0.3f, pinCenter.y + pinRadius * 1.3f,
                pinCenter.x - pinRadius, pinCenter.y + pinRadius * 0.6f,
                pinCenter.x - pinRadius, pinCenter.y
            )
            close()
        }
        drawPath(path = pinPath, color = InDriveLimeGreen)
        drawPath(path = pinPath, color = Color(0xFF1E222D), style = Stroke(width = 3.5f))

        // Center white circle in location pin
        drawCircle(
            color = Color.White,
            radius = pinRadius * 0.44f,
            center = pinCenter
        )
    }
}
