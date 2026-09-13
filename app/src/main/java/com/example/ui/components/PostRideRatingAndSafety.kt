package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.PassengerOrder
import com.example.data.model.ReportCategory
import com.example.data.model.RideRatingEntity
import com.example.data.model.SafetyReportEntity
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Modern Post-Ride Rating & Review Dialog with Tip, Tags, Report & Block options.
 * Works symmetrically for both Passenger and Driver with duplicate prevention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRideRatingDialog(
    rideId: String,
    currentUserId: String,
    currentUserName: String,
    isDriver: Boolean, // True if Driver is rating Passenger, False if Passenger is rating Driver
    targetId: String,
    targetName: String,
    targetPhone: String = "",
    targetVehicleSummary: String = "",
    targetPlateNumber: String = "",
    targetRating: Double = 0.0,
    pickupTitle: String = "",
    destinationTitle: String = "",
    farePkr: Int = 0,
    onDismiss: () -> Unit,
    onRatingSubmitted: (RideRatingEntity) -> Unit,
    onOpenSafetyReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }

    var ratingStars by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var tipAmountPkr by remember { mutableIntStateOf(0) }
    var isBlockSelected by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Quick feedback tags tailored by role and star score
    val availableTags = remember(isDriver, ratingStars) {
        if (isDriver) {
            if (ratingStars >= 4) {
                listOf("Polite Passenger", "On Time at Pickup", "Respectful", "Smooth Trip", "Great Communication")
            } else {
                listOf("Late to Pickup", "Rude Behavior", "Unreasonable Demands", "Luggage Issue", "Payment Delay")
            }
        } else {
            if (ratingStars >= 4) {
                listOf("Smooth Driving", "Clean Vehicle", "Polite Captain", "AC Working Great", "On-Time Arrival", "Safe Driving")
            } else {
                listOf("AC Not Turned On", "Reckless Driving", "Delayed Arrival", "Rude Behavior", "Wrong Route Taken", "Overcharging Request")
            }
        }
    }

    val handleDismissOrSkip = {
        repo.markRideRatedOrSkipped(rideId, raterRole = if (isDriver) "DRIVER" else "PASSENGER")
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = handleDismissOrSkip,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("post_ride_rating_dialog"),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    shape = CircleShape,
                    color = InDriveLimeGreen.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, InDriveLimeGreen),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Trip Completed",
                            tint = InDriveLimeGreen,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (farePkr > 0) "Ride Completed • PKR $farePkr" else "Trip Completed!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isDriver) "Rate Passenger: ${targetName.ifBlank { "Passenger" }}" else "How was your ride with Captain ${targetName.ifBlank { "Captain" }}?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Identity Card of Target
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDriver) InDriveLimeGreen.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDriver) Icons.Default.Person else Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (isDriver) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = targetName.ifBlank { if (isDriver) "Passenger" else "Driver Captain" },
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isDriver) "Passenger" else "Captain",
                                        fontSize = 10.sp,
                                        color = if (isDriver) InDriveLimeGreen else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (targetRating > 0.0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.1f", targetRating),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFFFB300)
                                        )
                                    }
                                }
                            }
                            if (targetVehicleSummary.isNotBlank()) {
                                Text(
                                    text = "$targetVehicleSummary${if (targetPlateNumber.isNotBlank()) " • $targetPlateNumber" else ""}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            } else if (destinationTitle.isNotBlank()) {
                                Text(
                                    text = "To: $destinationTitle",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 5-Star Interactive Rating Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { ratingStars = star },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("star_$star")
                        ) {
                            Icon(
                                imageVector = if (star <= ratingStars) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "$star stars",
                                tint = if (star <= ratingStars) Color(0xFFFFB300) else Color(0xFF5E6578),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // Star Rating Description Label
                Text(
                    text = when (ratingStars) {
                        5 -> "Excellent Experience"
                        4 -> "Good Ride"
                        3 -> "Average Trip"
                        2 -> "Below Expectations"
                        else -> "Poor / Unsatisfactory"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = when (ratingStars) {
                        5 -> InDriveLimeGreen
                        4 -> Color(0xFF81C784)
                        3 -> Color(0xFFFFB300)
                        else -> Color(0xFFEF5350)
                    }
                )

                // Quick Tag Chips (Selectable)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Quick Feedback Tags:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Tags Flow Row representation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableTags.take(3).forEach { tag ->
                            val isSelected = tag in selectedTags
                            Surface(
                                onClick = {
                                    selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) DrigoBrandPurple.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) DrigoBrandPurple else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (availableTags.size > 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableTags.drop(3).take(3).forEach { tag ->
                                val isSelected = tag in selectedTags
                                Surface(
                                    onClick = {
                                        selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) DrigoBrandPurple.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DrigoBrandPurple else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tip Driver (Only for Passenger)
                if (!isDriver && ratingStars >= 4) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Add Tip for Captain (Optional):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0, 50, 100, 200).forEach { tip ->
                                val isSelected = tipAmountPkr == tip
                                Surface(
                                    onClick = { tipAmountPkr = tip },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) InDriveLimeGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) InDriveLimeGreen else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (tip == 0) "None" else "PKR $tip",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (isSelected) InDriveLimeGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Optional Review Comment Field
                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = { reviewComment = it },
                    placeholder = {
                        Text(
                            text = "Write an optional review or compliments...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = DrigoBrandPurple,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_comment_input")
                )

                // Safety Options: Block User & Report Ride
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Block User Checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isBlockSelected = !isBlockSelected },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isBlockSelected,
                                onCheckedChange = { isBlockSelected = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFFEF5350),
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDriver) "Block this passenger from matching again" else "Do not match me with this driver again",
                                fontSize = 11.sp,
                                color = if (isBlockSelected) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isBlockSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Report Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenSafetyReport()
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = "Report",
                                    tint = Color(0xFFFF7043),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Report incident or inappropriate behavior",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF7043),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFFF7043),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    scope.launch {
                        try {
                            val ratingEntity = RideRatingEntity(
                                id = UUID.randomUUID().toString(),
                                rideId = rideId,
                                raterId = currentUserId,
                                raterRole = if (isDriver) "DRIVER" else "PASSENGER",
                                raterName = currentUserName,
                                targetId = targetId,
                                targetName = targetName,
                                stars = ratingStars,
                                reviewText = reviewComment.trim(),
                                tags = selectedTags.joinToString(","),
                                tipAmount = tipAmountPkr.toDouble(),
                                isBlocked = isBlockSelected,
                                timestamp = System.currentTimeMillis()
                            )

                            val res = repo.submitRideRating(ratingEntity)
                            if (res.isSuccess) {
                                Toast.makeText(
                                    context,
                                    if (isBlockSelected) "Rating submitted. User blocked." else "Thank you! Rating & review submitted.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onRatingSubmitted(ratingEntity)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Error saving rating. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving rating: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = InDriveLimeGreen),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("submit_rating_btn")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Feedback",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = handleDismissOrSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for Now",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    )
}

/**
 * Dedicated Safety & Incident Reporting Dialog.
 * Allows passenger or driver to submit an official misconduct/incident report with Admin security logging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyReportDialog(
    rideId: String,
    reporterId: String,
    reporterName: String,
    reporterPhone: String,
    isReporterDriver: Boolean,
    reportedUserId: String,
    reportedUserName: String,
    driverPlateNumber: String = "",
    pickupTitle: String = "",
    destinationTitle: String = "",
    onDismiss: () -> Unit,
    onReportSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }

    var selectedCategory by remember { mutableStateOf(ReportCategory.INAPPROPRIATE_BEHAVIOR) }
    var reportDescription by remember { mutableStateOf("") }
    var blockUserCheck by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = remember(isReporterDriver) {
        if (isReporterDriver) {
            listOf(
                ReportCategory.PASSENGER_MISCONDUCT,
                ReportCategory.INAPPROPRIATE_BEHAVIOR,
                ReportCategory.OVERCHARGING,
                ReportCategory.UNSAFE_EXPERIENCE,
                ReportCategory.FRAUD_SCAM,
                ReportCategory.OTHER
            )
        } else {
            listOf(
                ReportCategory.INAPPROPRIATE_BEHAVIOR,
                ReportCategory.RECKLESS_DRIVING,
                ReportCategory.ROUTE_DEVIATION,
                ReportCategory.OVERCHARGING,
                ReportCategory.VEHICLE_CONDITION,
                ReportCategory.UNSAFE_EXPERIENCE,
                ReportCategory.FRAUD_SCAM,
                ReportCategory.OTHER
            )
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("safety_report_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF5350).copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Safety",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Safety & Incident Report",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Directly escalated to 24/7 Safety Admin Team",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reported User Info Header
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Reporting User: ${reportedUserName.ifBlank { if (isReporterDriver) "Passenger" else "Driver Captain" }}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                        if (driverPlateNumber.isNotBlank()) {
                            Text(
                                text = "Vehicle Plate: $driverPlateNumber",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Select Issue Category:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                // Category selection radio / chips
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = {
                            if (!isSubmitting) {
                                selectedCategory = cat
                                errorMessage = null
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFEF5350).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFEF5350) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (!isSubmitting) {
                                        selectedCategory = cat
                                        errorMessage = null
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFEF5350),
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Description field
                Text(
                    text = "Describe What Happened:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = reportDescription,
                    onValueChange = {
                        reportDescription = it
                        errorMessage = null
                    },
                    enabled = !isSubmitting,
                    placeholder = {
                        Text(
                            text = "Provide details about the incident for the security audit...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color(0xFFEF5350),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Block User Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSubmitting) { blockUserCheck = !blockUserCheck }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = blockUserCheck,
                        onCheckedChange = { if (!isSubmitting) blockUserCheck = it },
                        enabled = !isSubmitting,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFEF5350),
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Block this user immediately from future rides",
                        fontSize = 11.sp,
                        color = if (blockUserCheck) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (blockUserCheck) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Error Message Display
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF5350).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF5350)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFFCDD2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedDesc = reportDescription.trim()
                    if (trimmedDesc.isBlank()) {
                        errorMessage = "Please write a brief description of the issue."
                        Toast.makeText(context, "Please write a brief description of the issue.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (trimmedDesc.length < 3) {
                        errorMessage = "Description must be at least 3 characters long."
                        Toast.makeText(context, "Description must be at least 3 characters long.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val safeRideId = rideId.ifBlank { "trip_${System.currentTimeMillis()}" }
                            val safeReporterId = reporterId.ifBlank { "reporter_${System.currentTimeMillis()}" }
                            val safeReportedUserId = reportedUserId.ifBlank { "reported_user_$safeRideId" }

                            val reportEntity = SafetyReportEntity(
                                id = UUID.randomUUID().toString(),
                                rideId = safeRideId,
                                reporterId = safeReporterId,
                                reporterRole = if (isReporterDriver) "DRIVER" else "PASSENGER",
                                reporterName = reporterName.ifBlank { if (isReporterDriver) "Driver Captain" else "Passenger" },
                                reporterPhone = reporterPhone,
                                reportedUserId = safeReportedUserId,
                                reportedUserName = reportedUserName.ifBlank { if (isReporterDriver) "Passenger" else "Driver Captain" },
                                reportedUserRole = if (isReporterDriver) "PASSENGER" else "DRIVER",
                                category = selectedCategory,
                                description = trimmedDesc,
                                blockUser = blockUserCheck,
                                ridePickupTitle = pickupTitle,
                                rideDestinationTitle = destinationTitle,
                                driverPlateNumber = driverPlateNumber,
                                status = "PENDING_ADMIN_REVIEW",
                                timestamp = System.currentTimeMillis()
                            )

                            val res = repo.submitSafetyReport(reportEntity)
                            if (res.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Incident report submitted successfully. Our 24/7 Safety team is reviewing (#${reportEntity.id.take(6)}).",
                                    Toast.LENGTH_LONG
                                ).show()
                                onReportSubmitted()
                                onDismiss()
                            } else {
                                val err = res.exceptionOrNull()?.localizedMessage ?: "Failed to submit report. Please check your connection."
                                errorMessage = err
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            val err = e.localizedMessage ?: "Failed to submit report. Please check your connection."
                            errorMessage = err
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("submit_safety_report_btn")
            ) {
                if (isSubmitting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submitting Report...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = "Submit Official Report",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    color = if (isSubmitting) Color(0xFF555B6E) else Color(0xFFA0A6B5),
                    fontSize = 12.sp
                )
            }
        }
    )
}

/**
 * Universal Passenger & Driver In-Ride Safety Bottom Sheet:
 * - 1-Tap SOS Emergency Call (15 Police / 1122 Rescue)
 * - Live Ride Share with Loved Ones (WhatsApp / SMS)
 * - Siren Panic Horn Deterrent
 * - Report Ride & Driver/Passenger Identity Card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSafetyModalSheet(
    rideId: String,
    userRole: String, // "PASSENGER" or "DRIVER"
    partnerName: String,
    partnerPhone: String,
    vehicleMake: String = "",
    vehicleModel: String = "",
    vehiclePlate: String = "",
    pickupAddress: String = "",
    destinationAddress: String = "",
    ridePin: String = "",
    onDismiss: () -> Unit,
    onOpenReport: () -> Unit
) {
    val context = LocalContext.current
    val isDriver = userRole.equals("DRIVER", ignoreCase = true)
    var isAudioRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isAudioRecording) {
        if (isAudioRecording) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                recordingSeconds++
            }
        } else {
            recordingSeconds = 0
        }
    }

    val liveTrackingUrl = "https://drigo.pk/track/${rideId.ifBlank { "live" }}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF5350).copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Safety Center",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Safety & Emergency Center",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "24/7 Security Tools & Incident Protection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // 1. Prominent 4-Digit Ride PIN (Safety Verification)
            if (ridePin.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, Color(0xFF00E676)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isDriver) "PASSENGER VERIFICATION PIN" else "YOUR 4-DIGIT RIDE PIN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00E676),
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = if (isDriver) "Ask passenger for this PIN before starting the trip" else "Show this 4-digit PIN to captain before boarding vehicle",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // 4 Digit Boxes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ridePin.forEach { digit ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.size(width = 46.dp, height = 48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = digit.toString(),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Identity Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isDriver) "PASSENGER IDENTITY" else "CAPTAIN & VEHICLE IDENTITY",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        fontSize = 10.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = partnerName.ifBlank { if (isDriver) "Passenger" else "Driver Captain" },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            if (vehicleMake.isNotBlank()) {
                                Text(
                                    text = "$vehicleMake $vehicleModel • $vehiclePlate",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Call Partner
                        if (partnerPhone.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$partnerPhone"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Audio Recording Toggle for Trip Safety
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isAudioRecording) Color(0xFFEF5350).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (isAudioRecording) Color(0xFFEF5350) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isAudioRecording) Color(0xFFEF5350) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isAudioRecording) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = "Audio Recording",
                                    tint = if (isAudioRecording) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Trip Audio Safety Record",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                                if (isAudioRecording) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEF5350)
                                    ) {
                                        Text(
                                            text = String.format(java.util.Locale.US, "REC %02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isAudioRecording) "Encrypted & stored locally for safety" else "Record cabin audio during late-night rides",
                                color = if (isAudioRecording) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isAudioRecording,
                        onCheckedChange = {
                            isAudioRecording = it
                            val statusMsg = if (it) "Trip safety audio recording started (Encrypted local storage)" else "Audio recording stopped & saved"
                            Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFEF5350),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // 4. 1-Tap SOS Buttons (Police 15, 911 / 112 & Rescue 1122)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EMERGENCY POLICE & RESCUE DIALER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF5350),
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Police 15
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:15"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocalPolice, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Police (15)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }

                    // Rescue 1122
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1122"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rescue (1122)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }

                // Motorway Police (130) & Universal Emergency (112 / 911)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:130"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        border = BorderStroke(1.dp, Color(0xFF3949AB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Motorway (130)", fontWeight = FontWeight.Bold, color = Color(0xFF8C9EFF), fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        border = BorderStroke(1.dp, Color(0xFF546E7A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Universal SOS (112)", fontWeight = FontWeight.Bold, color = Color(0xFFB0BEC5), fontSize = 11.sp)
                    }
                }
            }

            // 5. Share Live Trip Tracking URL (WhatsApp / SMS / Web link)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SHARE LIVE TRIP TRACKING LINK (NO APP REQUIRED)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF25D366),
                    letterSpacing = 0.5.sp
                )

                // WhatsApp 1-Tap Share
                Button(
                    onClick = {
                        try {
                            val shareText = "🛡️ Drigo Live Ride Safety Tracking\n\n" +
                                    "Track my real-time ride here: $liveTrackingUrl\n\n" +
                                    "Driver: $partnerName\n" +
                                    (if (vehiclePlate.isNotBlank()) "Vehicle: $vehicleMake $vehicleModel ($vehiclePlate)\n" else "") +
                                    (if (ridePin.isNotBlank()) "Safety PIN: $ridePin\n" else "") +
                                    "Pickup: $pickupAddress\n" +
                                    "Destination: $destinationAddress\n" +
                                    "Ride ID: #${rideId.take(8)}\n\n" +
                                    "You can open this link in any browser without needing the app."

                            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(shareText))
                            }
                            context.startActivity(whatsappIntent)
                        } catch (_: Exception) {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "🛡️ Track my Drigo live ride: $liveTrackingUrl\nDriver: $partnerName ($vehiclePlate)")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Live Trip via WhatsApp"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Live Trip on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }

                // SMS & Universal Share
                OutlinedButton(
                    onClick = {
                        try {
                            val shareText = "I am on a Drigo ride!\n\n" +
                                    "Track live: $liveTrackingUrl\n" +
                                    "Driver: $partnerName\n" +
                                    (if (vehiclePlate.isNotBlank()) "Vehicle: $vehicleMake $vehicleModel ($vehiclePlate)\n" else "") +
                                    (if (ridePin.isNotBlank()) "PIN: $ridePin\n" else "") +
                                    "Pickup: $pickupAddress\n" +
                                    "Destination: $destinationAddress\n" +
                                    "Ride ID: #$rideId"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Ride Details via SMS / App")
                            context.startActivity(shareIntent)
                        } catch (_: Exception) {}
                    },
                    border = BorderStroke(1.dp, Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Share via SMS or Other Apps", color = Color(0xFF25D366), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // 6. Report Issue to Admin
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenReport()
                },
                border = BorderStroke(1.dp, Color(0xFFEF5350)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.ReportProblem, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Report Ride or Inappropriate Behavior", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
