package com.example.ui.components

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    // Dynamic fares for intercity
    val privateFare = (1200 + (distanceKm * 42)).toInt()
    val sharedSeatFare = (450 + (distanceKm * 18)).toInt()
    val parcelFare = (600 + (distanceKm * 20)).toInt()

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFF16181D),
        border = BorderStroke(1.dp, Color(0xFF2C303B)),
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
                                color = Color.White,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // 1. Private ride Card
                            val isPrivate = selectedRideType == CityRideType.PRIVATE
                            Surface(
                                onClick = { onRideTypeChange(CityRideType.PRIVATE) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isPrivate) Color(0xFF252834) else Color(0xFF1B1D23),
                                border = BorderStroke(
                                    if (isPrivate) 1.5.dp else 1.dp,
                                    if (isPrivate) InDriveLimeGreen else Color(0xFF2C303B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_option_private")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp, 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CityPrivateCarGraphic(modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Private ride",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Whole cabin for you",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFA0A6B5),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = "~PKR ${"%,d".format(privateFare)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            // 2. Shared ride Card
                            val isShared = selectedRideType == CityRideType.SHARED
                            Surface(
                                onClick = { onRideTypeChange(CityRideType.SHARED) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isShared) Color(0xFF252834) else Color(0xFF1B1D23),
                                border = BorderStroke(
                                    if (isShared) 1.5.dp else 1.dp,
                                    if (isShared) InDriveLimeGreen else Color(0xFF2C303B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_option_shared")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp, 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CitySharedCarGraphic(modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Shared ride",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Share the ride with other passengers. Pay only for your seat",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFA0A6B5),
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "~PKR ${"%,d".format(sharedSeatFare)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "for 1 seat",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFA0A6B5),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // 3. Parcel delivery Card
                            val isParcel = selectedRideType == CityRideType.PARCEL
                            Surface(
                                onClick = { onRideTypeChange(CityRideType.PARCEL) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isParcel) Color(0xFF252834) else Color(0xFF1B1D23),
                                border = BorderStroke(
                                    if (isParcel) 1.5.dp else 1.dp,
                                    if (isParcel) InDriveLimeGreen else Color(0xFF2C303B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_option_parcel")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp, 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CityParcelDeliveryGraphic(modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Parcel delivery",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Door-to-door, between cities",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFA0A6B5),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = "~PKR ${"%,d".format(parcelFare)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

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
                                    .height(52.dp)
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
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "When to start the ride",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Option 1: "Now"
                            Surface(
                                onClick = { onTimingChange(true) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedTimingIsNow) Color(0xFF252834) else Color(0xFF1B1D23),
                                border = BorderStroke(
                                    if (selectedTimingIsNow) 1.5.dp else 1.dp,
                                    if (selectedTimingIsNow) InDriveLimeGreen else Color(0xFF2C303B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_time_now_option")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = "Now",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "Now",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 17.sp
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
                                                if (selectedTimingIsNow) InDriveLimeGreen else Color(0xFF555B6D),
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
                                color = if (!selectedTimingIsNow) Color(0xFF252834) else Color(0xFF1B1D23),
                                border = BorderStroke(
                                    if (!selectedTimingIsNow) 1.5.dp else 1.dp,
                                    if (!selectedTimingIsNow) InDriveLimeGreen else Color(0xFF2C303B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_time_later_option")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 18.dp),
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
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Later",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 17.sp
                                            )
                                            Text(
                                                text = if (!selectedTimingIsNow && scheduledDateTimeText.isNotBlank())
                                                    scheduledDateTimeText
                                                else
                                                    "Select date and time",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (!selectedTimingIsNow) InDriveLimeGreen else Color(0xFFA0A6B5),
                                                fontSize = 13.sp
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
                                                if (!selectedTimingIsNow) InDriveLimeGreen else Color(0xFF555B6D),
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

                            Spacer(modifier = Modifier.height(10.dp))

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
                                    .height(52.dp)
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        color = Color.White,
                                        fontSize = 22.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (selectedRideType == CityRideType.PARCEL)
                                            "Specify parcel details and your fare"
                                        else
                                            "Specify number of passengers and your fare",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFA0A6B5),
                                        fontSize = 13.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(68.dp, 40.dp)
                                        .padding(start = 8.dp),
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val passengerOptions = listOf(1, 2, 3, 4)
                                    passengerOptions.forEach { count ->
                                        val isSelected = passengerCount == count
                                        Surface(
                                            onClick = { onPassengerCountChange(count) },
                                            shape = RoundedCornerShape(24.dp),
                                            color = if (isSelected) Color.White else Color(0xFF2A2D37),
                                            modifier = Modifier
                                                .height(44.dp)
                                                .testTag("passenger_chip_$count")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (count == 1) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.Black else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = "$count",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (isSelected) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }

                                    // "More v" Option
                                    val isMoreSelected = passengerCount > 4
                                    Surface(
                                        onClick = { showMorePassengersDialog = true },
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (isMoreSelected) Color.White else Color(0xFF2A2D37),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("passenger_chip_more")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isMoreSelected) "$passengerCount Pax" else "More",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isMoreSelected) Color.Black else Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "More passengers",
                                                tint = if (isMoreSelected) Color.Black else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Fare Container Card with [-] PKR 8,100 [+]
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF1D2027),
                                border = BorderStroke(1.dp, Color(0xFF333744)),
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
                                            color = Color(0xFF2C303B),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .testTag("city_fare_minus")
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

                                        // Large Bold Fare Text
                                        Text(
                                            text = "PKR ${"%,d".format(customFare)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = 24.sp
                                        )

                                        // Plus Button
                                        Surface(
                                            onClick = onIncreaseFare,
                                            shape = CircleShape,
                                            color = Color(0xFF2C303B),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .testTag("city_fare_plus")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Increase Fare",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
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
                                color = Color(0xFF1D2027),
                                border = BorderStroke(1.dp, Color(0xFF2C303B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_date_time_row")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Date and time",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFA0A6B5),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = if (selectedTimingIsNow) "Now" else scheduledDateTimeText.ifBlank { "Select date & time" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Edit time",
                                        tint = Color(0xFFA0A6B5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Card Option 2: Comments
                            Surface(
                                onClick = { showCommentsDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1D2027),
                                border = BorderStroke(1.dp, Color(0xFF2C303B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("city_comments_row")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Comments",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFA0A6B5),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = comments.ifBlank { "Add comments for driver (optional)" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (comments.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                            color = if (comments.isNotBlank()) Color.White else Color(0xFFA0A6B5),
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Edit comments",
                                        tint = Color(0xFFA0A6B5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bottom Action Area: Payment Mode Square + Big Lime "Find a driver" Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Cash Button
                                Surface(
                                    onClick = onPaymentMethodClick,
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF22252C),
                                    border = BorderStroke(1.dp, Color(0xFF323642)),
                                    modifier = Modifier
                                        .size(52.dp)
                                        .testTag("city_payment_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        // Banknote icon
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFF4CAF50),
                                            modifier = Modifier.size(28.dp, 18.dp)
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

                                // Unified Brand Fuchsia "Find a driver" Button
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
                                        fontSize = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
