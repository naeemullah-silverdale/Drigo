package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.PlannedDeparture
import com.example.data.model.PlannedDepartureBooking
import com.example.data.model.PlannedDepartureOffer
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.drigoColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class CityToCityStep {
    WHAT_RIDE,   // Step 1: What ride do you need? (Private ride, Shared ride, Parcel delivery)
    WHEN_START,  // Step 2: When to start the ride (Now vs Later)
    CUSTOMIZE    // Step 3: Specify passengers, fare, date/time, comments & Find a driver
}

enum class CityRideType(val title: String, val subtitle: String) {
    PRIVATE("Private ride", "Whole cabin for you"),
    SHARED("Shared ride", "Share the ride with other passengers. Pay only for your seat"),
    PARCEL("Parcel delivery", "Door-to-door, between cities")
}

@Composable
fun CityToCityPassengerFlow(
    distanceKm: Double,
    currentStep: CityToCityStep,
    selectedRideType: CityRideType,
    selectedTimingIsNow: Boolean,
    scheduledDateTimeText: String,
    passengerCount: Int,
    customFare: Int,
    comments: String,
    onStepChange: (CityToCityStep) -> Unit,
    onRideTypeChange: (CityRideType) -> Unit,
    onTimingChange: (Boolean) -> Unit,
    onScheduledDateTimeChange: (String) -> Unit,
    onPassengerCountChange: (Int) -> Unit,
    onDecreaseFare: () -> Unit,
    onIncreaseFare: () -> Unit,
    onCommentsChange: (String) -> Unit,
    onPaymentMethodClick: () -> Unit,
    onFindDriverClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDateTimeDialog by remember { mutableStateOf(false) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var showMorePassengersDialog by remember { mutableStateOf(false) }
    var showAvailableDeparturesSheet by remember { mutableStateOf(false) }

    // Dynamic fares for intercity
    val privateFare = (1200 + (distanceKm * 42)).toInt()
    val sharedSeatFare = (450 + (distanceKm * 18)).toInt()
    val parcelFare = (600 + (distanceKm * 20)).toInt()

    val isDark = MaterialTheme.drigoColors.isDark
    val sheetColor = if (isDark) Color(0xFF16181D) else MaterialTheme.colorScheme.surface
    val cardBg = if (isDark) Color(0xFF1B1D23) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val cardSelectedBg = if (isDark) Color(0xFF252834) else InDriveLimeGreen.copy(alpha = 0.12f)
    val borderDefault = if (isDark) Color(0xFF2C303B) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant
    val chipBg = if (isDark) Color(0xFF2A2D37) else MaterialTheme.colorScheme.surfaceVariant
    val fareBoxBg = if (isDark) Color(0xFF1D2027) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val counterBtnBg = if (isDark) Color(0xFF2C303B) else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = sheetColor,
        border = BorderStroke(1.dp, borderDefault),
        shadowElevation = 24.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("city_to_city_passenger_flow_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "city_to_city_steps_animation"
            ) { step ->
                when (step) {
                    CityToCityStep.WHAT_RIDE -> {
                        // ================= SCREENSHOT 1: What ride do you need? =================
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "What ride do you need?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                fontSize = 21.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // 1. Private ride Card
                            val isPrivate = selectedRideType == CityRideType.PRIVATE
                            Surface(
                                onClick = { onRideTypeChange(CityRideType.PRIVATE) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isPrivate) cardSelectedBg else cardBg,
                                border = BorderStroke(
                                    if (isPrivate) 1.5.dp else 1.dp,
                                    if (isPrivate) InDriveLimeGreen else borderDefault
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_option_private")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(50.dp, 34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CityPrivateCarGraphic(modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Private ride",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Whole cabin for you",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary,
                                            fontSize = 11.5.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "~PKR ${"%,d".format(privateFare)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 2. Shared ride Card
                            val isShared = selectedRideType == CityRideType.SHARED
                            Surface(
                                onClick = { onRideTypeChange(CityRideType.SHARED) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isShared) cardSelectedBg else cardBg,
                                border = BorderStroke(
                                    if (isShared) 1.5.dp else 1.dp,
                                    if (isShared) InDriveLimeGreen else borderDefault
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_option_shared")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(50.dp, 34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CitySharedCarGraphic(modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Shared ride",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Share the ride with other passengers. Pay only for your seat",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary,
                                            fontSize = 11.5.sp,
                                            lineHeight = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "~PKR ${"%,d".format(sharedSeatFare)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            fontSize = 15.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "for 1 seat",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // 3. Parcel delivery Card
                            val isParcel = selectedRideType == CityRideType.PARCEL
                            Surface(
                                onClick = { onRideTypeChange(CityRideType.PARCEL) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isParcel) cardSelectedBg else cardBg,
                                border = BorderStroke(
                                    if (isParcel) 1.5.dp else 1.dp,
                                    if (isParcel) InDriveLimeGreen else borderDefault
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_option_parcel")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(50.dp, 34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CityParcelDeliveryGraphic(modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Parcel delivery",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Door-to-door, between cities",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary,
                                            fontSize = 11.5.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "~PKR ${"%,d".format(parcelFare)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Browse Captain Scheduled Departures Button
                            Surface(
                                onClick = { showAvailableDeparturesSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF1E222D) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color(0xFF2979FF).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("browse_scheduled_rides_btn")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF2979FF).copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.CalendarMonth,
                                                contentDescription = null,
                                                tint = Color(0xFF2979FF),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Browse Scheduled Departures",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Book 1 seat or entire car from Captains",
                                            fontSize = 11.sp,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00E676).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF00E676),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // "Next" Button
                            Button(
                                onClick = { onStepChange(CityToCityStep.WHEN_START) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InDriveLimeGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("city_step1_next_btn")
                            ) {
                                Text(
                                    text = "Next",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    CityToCityStep.WHEN_START -> {
                        // ================= SCREENSHOT 2: When to start the ride =================
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "When to start the ride",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                fontSize = 21.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Option 1: "Now"
                            Surface(
                                onClick = { onTimingChange(true) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedTimingIsNow) cardSelectedBg else cardBg,
                                border = BorderStroke(
                                    if (selectedTimingIsNow) 1.5.dp else 1.dp,
                                    if (selectedTimingIsNow) InDriveLimeGreen else borderDefault
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_time_now_option")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = "Now",
                                            tint = textPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = "Now",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            fontSize = 16.sp
                                        )
                                    }

                                    // Radio Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedTimingIsNow) InDriveLimeGreen else Color.Transparent
                                            )
                                            .border(
                                                2.dp,
                                                if (selectedTimingIsNow) InDriveLimeGreen else borderDefault,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedTimingIsNow) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                            )
                                        }
                                    }
                                }
                            }

                            // Option 2: "Later"
                            Surface(
                                onClick = {
                                    onTimingChange(false)
                                    showDateTimeDialog = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (!selectedTimingIsNow) cardSelectedBg else cardBg,
                                border = BorderStroke(
                                    if (!selectedTimingIsNow) 1.5.dp else 1.dp,
                                    if (!selectedTimingIsNow) InDriveLimeGreen else borderDefault
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_time_later_option")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarMonth,
                                            contentDescription = "Later",
                                            tint = textPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Later",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = if (!selectedTimingIsNow && scheduledDateTimeText.isNotBlank())
                                                    scheduledDateTimeText
                                                else
                                                    "Select date and time",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (!selectedTimingIsNow) InDriveLimeGreen else textSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Radio Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (!selectedTimingIsNow) InDriveLimeGreen else Color.Transparent
                                            )
                                            .border(
                                                2.dp,
                                                if (!selectedTimingIsNow) InDriveLimeGreen else borderDefault,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!selectedTimingIsNow) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // "Next" Button
                            Button(
                                onClick = { onStepChange(CityToCityStep.CUSTOMIZE) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InDriveLimeGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("city_step2_next_btn")
                            ) {
                                Text(
                                    text = "Next",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    CityToCityStep.CUSTOMIZE -> {
                        // ================= SCREENSHOT 3: Customize & Find a Driver =================
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            // Header Row: Ride Type Title on left + Vehicle Graphic on right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedRideType.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary,
                                        fontSize = 21.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (selectedRideType == CityRideType.PARCEL)
                                            "Specify parcel details and your fare"
                                        else
                                            "Specify number of passengers and your fare",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textSecondary,
                                        fontSize = 12.5.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(64.dp, 38.dp)
                                        .padding(start = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (selectedRideType) {
                                        CityRideType.PRIVATE -> CityPrivateCarGraphic(modifier = Modifier.fillMaxSize())
                                        CityRideType.SHARED -> CitySharedCarGraphic(modifier = Modifier.fillMaxSize())
                                        CityRideType.PARCEL -> CityParcelDeliveryGraphic(modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }

                            // Passengers Count Selector Row (if not Parcel)
                            if (selectedRideType != CityRideType.PARCEL) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val passengerOptions = listOf(1, 2, 3, 4)
                                    passengerOptions.forEach { count ->
                                        val isSelected = passengerCount == count
                                        Surface(
                                            onClick = { onPassengerCountChange(count) },
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) textPrimary else chipBg,
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("passenger_chip_$count")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (count == 1) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = if (isSelected) (if (isDark) Color.Black else Color.White) else textPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = "$count",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) (if (isDark) Color.Black else Color.White) else textPrimary
                                                )
                                            }
                                        }
                                    }

                                    // "More v" Option
                                    val isMoreSelected = passengerCount > 4
                                    Surface(
                                        onClick = { showMorePassengersDialog = true },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isMoreSelected) textPrimary else chipBg,
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("passenger_chip_more")
                                        ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isMoreSelected) "$passengerCount Pax" else "More",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = if (isMoreSelected) (if (isDark) Color.Black else Color.White) else textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "More passengers",
                                                tint = if (isMoreSelected) (if (isDark) Color.Black else Color.White) else textPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Fare Container Card with [-] PKR 8,100 [+]
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = fareBoxBg,
                                border = BorderStroke(1.dp, borderDefault),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_fare_box")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Minus Button
                                        Surface(
                                            onClick = onDecreaseFare,
                                            shape = CircleShape,
                                            color = counterBtnBg,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .testTag("city_fare_minus")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "—",
                                                    color = textPrimary,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }

                                        // Large Bold Fare Text
                                        Text(
                                            text = "PKR ${"%,d".format(customFare)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = textPrimary,
                                            fontSize = 22.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Plus Button
                                        Surface(
                                            onClick = onIncreaseFare,
                                            shape = CircleShape,
                                            color = counterBtnBg,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .testTag("city_fare_plus")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Increase Fare",
                                                    tint = textPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Recommended fare",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InDriveLimeGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Card Option 1: Date and time
                            Surface(
                                onClick = { showDateTimeDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, borderDefault),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_date_time_row")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Date and time",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = if (selectedTimingIsNow) "Now" else scheduledDateTimeText.ifBlank { "Select date & time" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            fontSize = 14.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Edit time",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Card Option 2: Comments
                            Surface(
                                onClick = { showCommentsDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, borderDefault),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_comments_row")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Comments",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = comments.ifBlank { "Add comments for driver (optional)" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (comments.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                            color = if (comments.isNotBlank()) textPrimary else textSecondary,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Edit comments",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bottom Action Area: Payment Mode Square + Big Lime "Find a driver" Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Cash Button
                                Surface(
                                    onClick = onPaymentMethodClick,
                                    shape = RoundedCornerShape(12.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, borderDefault),
                                    modifier = Modifier
                                        .size(50.dp)
                                        .testTag("city_payment_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        // Banknote icon
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFF4CAF50),
                                            modifier = Modifier.size(26.dp, 16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(7.dp)
                                                ) {}
                                            }
                                        }
                                    }
                                }

                                // Unified Brand "Find a driver" Button
                                Button(
                                    onClick = onFindDriverClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = InDriveLimeGreen,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .testTag("city_find_driver_btn")
                                ) {
                                    Text(
                                        text = "Find a driver",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheet 0: Available Scheduled Departures from Captains
    if (showAvailableDeparturesSheet) {
        PassengerScheduledDeparturesSheet(
            onDismiss = { showAvailableDeparturesSheet = false }
        )
    }

    // Dialog 1: Date & Time Picker
    if (showDateTimeDialog) {
        val calendar = remember { Calendar.getInstance() }
        var selectedDayOffset by remember { mutableIntStateOf(0) }
        var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
        var selectedMinute by remember { mutableIntStateOf(15) }

        Dialog(onDismissRequest = { showDateTimeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E2026),
                border = BorderStroke(1.dp, Color(0xFF353945)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Select Date & Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Day options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Today", "Tomorrow", "In 2 days").forEachIndexed { idx, label ->
                            val isSel = selectedDayOffset == idx
                            Surface(
                                onClick = { selectedDayOffset = idx },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) InDriveLimeGreen else Color(0xFF2C303B),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSel) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Time slots quick selector
                    Text(
                        text = "Time of Departure",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A6B5)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("08:00 AM", "12:15 PM", "04:30 PM", "08:00 PM").forEach { slot ->
                            Surface(
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, selectedDayOffset)
                                    }
                                    val dayFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                                    val formatted = "${dayFormat.format(cal.time)} $slot"
                                    onScheduledDateTimeChange(formatted)
                                    onTimingChange(false)
                                    showDateTimeDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF282B34),
                                border = BorderStroke(1.dp, Color(0xFF383C48)),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = slot,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDateTimeDialog = false }) {
                            Text("Close", color = InDriveLimeGreen)
                        }
                    }
                }
            }
        }
    }

    // Dialog 2: Comments / Special Instructions
    if (showCommentsDialog) {
        var commentDraft by remember { mutableStateOf(comments) }
        val focusManager = LocalFocusManager.current

        Dialog(onDismissRequest = { showCommentsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E2026),
                border = BorderStroke(1.dp, Color(0xFF353945)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Comments for Driver",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = { Text("e.g. 2 large luggage bags, near Shell pump", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = InDriveLimeGreen,
                            unfocusedBorderColor = Color(0xFF383C48),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick suggestion chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("🧳 Heavy Luggage", "👶 Traveling with child", "⚡ Express travel", "📦 Fragile Parcel").forEach { suggestion ->
                            Surface(
                                onClick = {
                                    commentDraft = if (commentDraft.isBlank()) suggestion else "$commentDraft, $suggestion"
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2A2D37)
                            ) {
                                Text(
                                    text = suggestion,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCommentsDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onCommentsChange(commentDraft.trim())
                                showCommentsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InDriveLimeGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Dialog 3: More Passengers Selector
    if (showMorePassengersDialog) {
        Dialog(onDismissRequest = { showMorePassengersDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E2026),
                border = BorderStroke(1.dp, Color(0xFF353945)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Select Passengers Count",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    listOf(5, 6, 7, 8).forEach { count ->
                        Surface(
                            onClick = {
                                onPassengerCountChange(count)
                                showMorePassengersDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (passengerCount == count) InDriveLimeGreen else Color(0xFF282B34),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$count Passengers (Van / SUV required)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (passengerCount == count) Color.Black else Color.White,
                                    fontSize = 14.sp
                                )
                                if (passengerCount == count) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black
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

/**
 * Custom vector graphic for City-to-City Private Ride: White sleek sedan
 */
@Composable
fun CityPrivateCarGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val carBottom = h * 0.88f
        val carLeft = w * 0.06f
        val carRight = w * 0.94f
        val carW = carRight - carLeft
        val carH = carBottom - h * 0.22f

        val hoodTop = h * 0.50f
        val roofTop = h * 0.26f

        // Dark windows/cabin
        val cabinPath = Path().apply {
            moveTo(carLeft + carW * 0.20f, hoodTop)
            lineTo(carLeft + carW * 0.38f, roofTop)
            lineTo(carLeft + carW * 0.72f, roofTop)
            lineTo(carLeft + carW * 0.88f, hoodTop)
            close()
        }
        drawPath(cabinPath, color = Color(0xFF1E2028))

        // White metallic body
        val bodyPath = Path().apply {
            moveTo(carLeft, hoodTop + carH * 0.2f)
            lineTo(carLeft + carW * 0.12f, hoodTop)
            lineTo(carLeft + carW * 0.92f, hoodTop)
            lineTo(carRight, hoodTop + carH * 0.25f)
            lineTo(carRight, carBottom - carH * 0.10f)
            lineTo(carLeft, carBottom - carH * 0.10f)
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
            topLeft = Offset(carLeft + 1.dp.toPx(), hoodTop + carH * 0.08f),
            size = Size(carW * 0.14f, carH * 0.18f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Wheels
        val wheelRadius = carH * 0.24f
        val wheelY = carBottom - carH * 0.04f
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = Offset(carLeft + carW * 0.28f, wheelY))
        drawCircle(Color(0xFFECEFF1), radius = wheelRadius * 0.5f, center = Offset(carLeft + carW * 0.28f, wheelY))
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = Offset(carLeft + carW * 0.76f, wheelY))
        drawCircle(Color(0xFFECEFF1), radius = wheelRadius * 0.5f, center = Offset(carLeft + carW * 0.76f, wheelY))
    }
}

/**
 * Custom vector graphic for City-to-City Shared Ride: 2 passengers next to car
 */
@Composable
fun CitySharedCarGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left side: 2 Passengers (head + body)
        val p1X = w * 0.14f
        val p1Y = h * 0.38f
        // Passenger 1 (Orange/coral top)
        drawCircle(Color(0xFFFFCC80), radius = 5.dp.toPx(), center = Offset(p1X, p1Y))
        drawRoundRect(
            color = Color(0xFFFF7043),
            topLeft = Offset(p1X - 6.dp.toPx(), p1Y + 5.dp.toPx()),
            size = Size(12.dp.toPx(), 14.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Passenger 2 (Cyan/blue top)
        val p2X = w * 0.28f
        val p2Y = h * 0.35f
        drawCircle(Color(0xFFFFE082), radius = 5.dp.toPx(), center = Offset(p2X, p2Y))
        drawRoundRect(
            color = Color(0xFF29B6F6),
            topLeft = Offset(p2X - 6.dp.toPx(), p2Y + 5.dp.toPx()),
            size = Size(12.dp.toPx(), 15.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Right side: Compact car
        val carLeft = w * 0.40f
        val carRight = w * 0.96f
        val carW = carRight - carLeft
        val carBottom = h * 0.88f
        val hoodTop = h * 0.52f
        val roofTop = h * 0.30f
        val carH = carBottom - h * 0.25f

        // Cabin
        val cabinPath = Path().apply {
            moveTo(carLeft + carW * 0.20f, hoodTop)
            lineTo(carLeft + carW * 0.38f, roofTop)
            lineTo(carLeft + carW * 0.74f, roofTop)
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
            lineTo(carRight, carBottom - carH * 0.10f)
            lineTo(carLeft, carBottom - carH * 0.10f)
            close()
        }
        drawPath(
            bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFB0BEC5)),
                startY = hoodTop,
                endY = carBottom
            )
        )

        // Wheels
        val wheelRadius = carH * 0.24f
        val wheelY = carBottom - carH * 0.04f
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = Offset(carLeft + carW * 0.30f, wheelY))
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = Offset(carLeft + carW * 0.76f, wheelY))
    }
}

/**
 * Custom vector graphic for City-to-City Parcel Delivery: Delivery boxes + Car
 */
@Composable
fun CityParcelDeliveryGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left side: Delivery packages/boxes (Green bag + Orange box)
        val boxLeft = w * 0.06f
        val boxTop = h * 0.44f
        // Green eco bag
        drawRoundRect(
            color = Color(0xFF43A047),
            topLeft = Offset(boxLeft, boxTop + 4.dp.toPx()),
            size = Size(14.dp.toPx(), 16.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Orange parcel box
        val b2Left = boxLeft + 10.dp.toPx()
        val b2Top = boxTop
        drawRoundRect(
            color = Color(0xFFFF9800),
            topLeft = Offset(b2Left, b2Top),
            size = Size(16.dp.toPx(), 18.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Tape line
        drawLine(
            color = Color(0xFFE65100),
            start = Offset(b2Left, b2Top + 9.dp.toPx()),
            end = Offset(b2Left + 16.dp.toPx(), b2Top + 9.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        // Right side: Transport Car
        val carLeft = w * 0.40f
        val carRight = w * 0.96f
        val carW = carRight - carLeft
        val carBottom = h * 0.88f
        val hoodTop = h * 0.52f
        val roofTop = h * 0.30f
        val carH = carBottom - h * 0.25f

        // Cabin
        val cabinPath = Path().apply {
            moveTo(carLeft + carW * 0.20f, hoodTop)
            lineTo(carLeft + carW * 0.38f, roofTop)
            lineTo(carLeft + carW * 0.74f, roofTop)
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
            lineTo(carRight, carBottom - carH * 0.10f)
            lineTo(carLeft, carBottom - carH * 0.10f)
            close()
        }
        drawPath(
            bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFB0BEC5)),
                startY = hoodTop,
                endY = carBottom
            )
        )

        // Wheels
        val wheelRadius = carH * 0.24f
        val wheelY = carBottom - carH * 0.04f
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = Offset(carLeft + carW * 0.30f, wheelY))
        drawCircle(Color(0xFF212121), radius = wheelRadius, center = Offset(carLeft + carW * 0.76f, wheelY))
    }
}

/**
 * Bottom Sheet for Passengers to browse and book scheduled departures posted by drivers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerScheduledDeparturesSheet(
    onDismiss: () -> Unit,
    initialFromCity: String = "Islamabad",
    initialToCity: String = "Lahore"
) {
    val isDark = MaterialTheme.drigoColors.isDark
    val sheetBg = if (isDark) Color(0xFF14161C) else MaterialTheme.colorScheme.surface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBg,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.96f)
    ) {
        CityToCityPassengerDeparturesContent(
            onBackClick = onDismiss,
            onSosClick = { },
            initialFromCity = initialFromCity,
            initialToCity = initialToCity,
            isSheetMode = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyPassengerScheduledDeparturesSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { FirebaseRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val allDepartures by repo.observeAllPlannedDepartures().collectAsState(initial = emptyList())

    val isDark = MaterialTheme.drigoColors.isDark
    val sheetBg = if (isDark) Color(0xFF14161C) else MaterialTheme.colorScheme.surface
    val cardBg = if (isDark) Color(0xFF1C1F28) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val borderCol = if (isDark) Color(0xFF2C303B) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant
    val searchBg = if (isDark) Color(0xFF1E222D) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    var selectedDepartureForBooking by remember { mutableStateOf<PlannedDeparture?>(null) }
    var selectedDepartureForOffer by remember { mutableStateOf<PlannedDeparture?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredDepartures = remember(allDepartures, searchQuery) {
        allDepartures.filter { dep ->
            dep.status.equals("ACTIVE", ignoreCase = true) &&
                    (searchQuery.isBlank() ||
                            dep.corridorName.contains(searchQuery, ignoreCase = true) ||
                            dep.pickupCity.contains(searchQuery, ignoreCase = true) ||
                            dep.dropoffCity.contains(searchQuery, ignoreCase = true) ||
                            dep.pickupHub.contains(searchQuery, ignoreCase = true) ||
                            dep.dropoffHub.contains(searchQuery, ignoreCase = true) ||
                            dep.driverName.contains(searchQuery, ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
            }
        },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scheduled Departures",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Book seat, full car, or make fare offer to Captains",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF252834) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search city, hub, corridor...", color = textSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = searchBg,
                    unfocusedContainerColor = searchBg,
                    focusedBorderColor = InDriveLimeGreen,
                    unfocusedBorderColor = borderCol,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            if (filteredDepartures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = textSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No scheduled departures found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Captains frequently post new trips. You can also request an instant ride in City to City.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDepartures, key = { it.id }) { dep ->
                        PassengerDepartureCard(
                            departure = dep,
                            onBookClick = { selectedDepartureForBooking = dep },
                            onOfferClick = { selectedDepartureForOffer = dep }
                        )
                    }
                }
            }
        }
    }

    // Booking Dialog for selected departure
    if (selectedDepartureForBooking != null) {
        val dep = selectedDepartureForBooking!!
        var isFullCarBuyout by remember { mutableStateOf(false) }
        var seatsToBook by remember { mutableIntStateOf(1) }
        var passengerName by remember { mutableStateOf("") }
        var passengerPhone by remember { mutableStateOf("") }
        var passengerNote by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        val remainingSeats = (dep.totalSeats - dep.bookedSeatsCount).coerceAtLeast(1)
        val calculatedFare = if (isFullCarBuyout) {
            dep.fullCarFare
        } else {
            dep.farePerSeat * seatsToBook
        }

        val dialogBg = if (isDark) Color(0xFF1E212B) else MaterialTheme.colorScheme.surface
        val dialogCardBg = if (isDark) Color(0xFF14161C) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

        Dialog(onDismissRequest = { if (!isSubmitting) selectedDepartureForBooking = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = dialogBg,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Book Scheduled Ride",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${dep.pickupCity} ➔ ${dep.dropoffCity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InDriveLimeGreen,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { if (!isSubmitting) selectedDepartureForBooking = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textPrimary)
                        }
                    }

                    // Captain & Trip Info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = dialogCardBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2979FF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Captain: ${dep.driverName} (${dep.driverRating} ★)",
                                    fontSize = 13.sp,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${dep.driverVehicle} • ${dep.driverPlateNumber}",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = InDriveLimeGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${dep.departureDateText} at ${dep.departureTimeText}",
                                    fontSize = 12.5.sp,
                                    color = InDriveLimeGreen,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Pickup: ${dep.pickupHub} • Drop: ${dep.dropoffHub}",
                                fontSize = 11.5.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Booking Type: Seat vs Whole Car
                    if (dep.allowFullCarBuyout) {
                        Text(text = "Choose Booking Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { isFullCarBuyout = false },
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isFullCarBuyout) InDriveLimeGreen else if (isDark) Color(0xFF252834) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Per Seat (PKR ${dep.farePerSeat})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isFullCarBuyout) Color.Black else textPrimary
                                    )
                                }
                            }

                            Surface(
                                onClick = { isFullCarBuyout = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isFullCarBuyout) InDriveLimeGreen else if (isDark) Color(0xFF252834) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Full Car (PKR ${dep.fullCarFare})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFullCarBuyout) Color.Black else textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Number of seats selector if not full car
                    if (!isFullCarBuyout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Number of Seats:", fontSize = 13.sp, color = textPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..remainingSeats.coerceAtMost(4)).forEach { num ->
                                    Surface(
                                        onClick = { seatsToBook = num },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (seatsToBook == num) InDriveLimeGreen else if (isDark) Color(0xFF252834) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(start = 6.dp).size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$num",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (seatsToBook == num) Color.Black else textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Passenger Details Input
                    OutlinedTextField(
                        value = passengerName,
                        onValueChange = { passengerName = it },
                        label = { Text("Your Name", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Usman Ali") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = dialogCardBg,
                            unfocusedContainerColor = dialogCardBg,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passengerPhone,
                        onValueChange = { passengerPhone = it },
                        label = { Text("Your Phone Number", fontSize = 12.sp) },
                        placeholder = { Text("0300-1234567") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = dialogCardBg,
                            unfocusedContainerColor = dialogCardBg,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passengerNote,
                        onValueChange = { passengerNote = it },
                        label = { Text("Note/Luggage Info (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 1 medium suitcase") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = dialogCardBg,
                            unfocusedContainerColor = dialogCardBg,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Total Fare Summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(dialogCardBg, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total Payable:", fontSize = 13.sp, color = textSecondary)
                        Text(
                            text = "PKR ${"%,d".format(calculatedFare)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = InDriveLimeGreen
                        )
                    }

                    // Confirm Booking Button
                    Button(
                        onClick = {
                            if (passengerName.isBlank() || passengerPhone.isBlank()) {
                                Toast.makeText(context, "Please enter your name and phone number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            scope.launch {
                                val result = repo.bookPlannedDepartureSeat(
                                    departureId = dep.id,
                                    passengerId = "pass_${System.currentTimeMillis().toString().takeLast(6)}",
                                    passengerName = passengerName.trim(),
                                    passengerPhone = passengerPhone.trim(),
                                    seatsBooked = if (isFullCarBuyout) dep.totalSeats else seatsToBook,
                                    isFullCar = isFullCarBuyout,
                                    totalFarePkr = calculatedFare
                                )
                                isSubmitting = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Ride booked successfully! Captain notified.", Toast.LENGTH_LONG).show()
                                    selectedDepartureForBooking = null
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Could not complete booking. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InDriveLimeGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(text = "Confirm & Book Ride", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // Fare Offer Dialog for selected departure
    if (selectedDepartureForOffer != null) {
        PassengerMakeOfferDialog(
            departure = selectedDepartureForOffer!!,
            onDismiss = { selectedDepartureForOffer = null }
        )
    }
}

@Composable
private fun PassengerMakeOfferDialog(
    departure: PlannedDeparture,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { FirebaseRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.drigoColors.isDark

    var offeredFare by remember { mutableIntStateOf(departure.farePerSeat) }
    var requestedSeats by remember { mutableIntStateOf(1) }
    var passengerName by remember { mutableStateOf("") }
    var passengerPhone by remember { mutableStateOf("") }
    var pickupPoint by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val remainingSeats = (departure.totalSeats - departure.bookedSeatsCount).coerceAtLeast(1)
    val diff = offeredFare - departure.farePerSeat

    val dialogBg = if (isDark) Color(0xFF1E212B) else MaterialTheme.colorScheme.surface
    val cardBg = if (isDark) Color(0xFF14161C) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isDark) Color(0xFF353945) else MaterialTheme.colorScheme.outlineVariant

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = dialogBg,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Make Fare Offer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${departure.pickupCity} ➔ ${departure.dropoffCity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InDriveLimeGreen,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { if (!isSubmitting) onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textPrimary)
                    }
                }

                // Captain details card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Captain ${departure.driverName} • ${departure.driverVehicle}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Standard asking fare: PKR ${"%,d".format(departure.farePerSeat)} per seat",
                            fontSize = 11.5.sp,
                            color = textSecondary
                        )
                    }
                }

                // Offered Fare Stepper
                Text(
                    text = "Your Offered Fare Per Seat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                onClick = { offeredFare = (offeredFare - 100).coerceAtLeast(100) },
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF2C303B) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "—", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                                }
                            }

                            Text(
                                text = "PKR ${"%,d".format(offeredFare)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                onClick = { offeredFare += 100 },
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF2C303B) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = textPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Difference tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                diff < 0 -> Color(0xFFFFB300).copy(alpha = 0.15f)
                                diff > 0 -> InDriveLimeGreen.copy(alpha = 0.15f)
                                else -> Color(0xFF2979FF).copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = when {
                                    diff < 0 -> "PKR ${"%,d".format(-diff)} below asking fare"
                                    diff > 0 -> "PKR ${"%,d".format(diff)} above asking fare"
                                    else -> "At standard asking fare"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    diff < 0 -> Color(0xFFFFB300)
                                    diff > 0 -> InDriveLimeGreen
                                    else -> Color(0xFF2979FF)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Seats needed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Number of Seats:", fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (1..remainingSeats.coerceAtMost(4)).forEach { num ->
                            Surface(
                                onClick = { requestedSeats = num },
                                shape = RoundedCornerShape(8.dp),
                                color = if (requestedSeats == num) InDriveLimeGreen else if (isDark) Color(0xFF252834) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(start = 6.dp).size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$num",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (requestedSeats == num) Color.Black else textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Passenger Name & Phone
                OutlinedTextField(
                    value = passengerName,
                    onValueChange = { passengerName = it },
                    label = { Text("Your Name", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Tariq Mehmood") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = passengerPhone,
                    onValueChange = { passengerPhone = it },
                    label = { Text("Phone Number", fontSize = 12.sp) },
                    placeholder = { Text("0300-1234567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pickupPoint,
                    onValueChange = { pickupPoint = it },
                    label = { Text("Preferred Pickup Point (Optional)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Near Thokar Niaz Baig") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Total Calculation
                val totalOffered = offeredFare * requestedSeats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Total Offered Fare:", fontSize = 13.sp, color = textSecondary)
                    Text(
                        text = "PKR ${"%,d".format(totalOffered)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InDriveLimeGreen
                    )
                }

                // Submit Button
                Button(
                    onClick = {
                        if (passengerName.isBlank() || passengerPhone.isBlank()) {
                            Toast.makeText(context, "Please enter your name and phone number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        scope.launch {
                            val result = repo.submitDepartureOffer(
                                departureId = departure.id,
                                passengerId = "pass_${System.currentTimeMillis().toString().takeLast(6)}",
                                passengerName = passengerName.trim(),
                                passengerPhone = passengerPhone.trim(),
                                requestedSeats = requestedSeats,
                                offeredFare = offeredFare,
                                standardAsking = departure.farePerSeat,
                                pickupPoint = pickupPoint.trim(),
                                luggageDetails = ""
                            )
                            isSubmitting = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Offer of PKR $offeredFare sent to Captain!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Could not send offer. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InDriveLimeGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = "Send Offer to Captain", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PassengerDepartureCard(
    departure: PlannedDeparture,
    onBookClick: () -> Unit,
    onOfferClick: () -> Unit
) {
    val isDark = MaterialTheme.drigoColors.isDark
    val cardBg = if (isDark) Color(0xFF1C1F28) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val borderCol = if (isDark) Color(0xFF2C303B) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color(0xFFA0A6B5) else MaterialTheme.colorScheme.onSurfaceVariant

    val remainingSeats = (departure.totalSeats - departure.bookedSeatsCount).coerceAtLeast(0)
    val isFull = remainingSeats <= 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Corridor & Departure Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = departure.pickupCity,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = InDriveLimeGreen,
                        modifier = Modifier.padding(horizontal = 4.dp).size(15.dp)
                    )
                    Text(
                        text = departure.dropoffCity,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = InDriveLimeGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${departure.departureDateText} • ${departure.departureTimeText}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = InDriveLimeGreen,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }
            }

            // Hubs & Captain
            Text(
                text = "📍 ${departure.pickupHub} ➔ ${departure.dropoffHub}",
                fontSize = 12.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Captain ${departure.driverName} • ${departure.driverVehicle}",
                    fontSize = 11.5.sp,
                    color = textSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$remainingSeats seats left",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingSeats > 1) InDriveLimeGreen else Color(0xFFFFB300)
                )
            }

            HorizontalDivider(color = borderCol.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Pricing & Booking Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PKR ${"%,d".format(departure.farePerSeat)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (departure.allowFullCarBuyout) "or PKR ${"%,d".format(departure.fullCarFare)} full car" else "per seat",
                        fontSize = 10.5.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onOfferClick,
                        enabled = !isFull,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = InDriveLimeGreen
                        ),
                        border = BorderStroke(1.dp, InDriveLimeGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "Offer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onBookClick,
                        enabled = !isFull,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFull) Color(0xFF374151) else InDriveLimeGreen,
                            contentColor = if (isFull) Color(0xFF9CA3AF) else Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isFull) "Full" else "Book",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
