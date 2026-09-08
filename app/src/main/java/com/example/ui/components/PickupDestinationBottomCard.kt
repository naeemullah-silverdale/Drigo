package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLocation
import com.example.data.DestinationSuggestion
import com.example.data.LocationHelper
import com.example.data.RouteService
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bottom Card for picking and editing Pickup and Destination locations:
 * - "● Where From?" / "● Where To?"
 * - Preserves pickup state and allows direct editing
 * - Provides search with autocomplete suggestions
 * - Locks input and shows notifications when a ride is in progress
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupDestinationBottomCard(
    pickupLocation: AppLocation,
    destinationLocation: AppLocation? = null,
    initialEditingPickup: Boolean = false,
    initialWhereToText: String = "",
    isLocked: Boolean = false,
    onDismiss: () -> Unit,
    onDestinationSelected: (destination: AppLocation) -> Unit,
    onPickupSelected: (pickup: AppLocation) -> Unit,
    onPickOnMap: (isPickup: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routeService = remember { RouteService(context) }
    val locationHelper = remember { LocationHelper(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    fun showLockedToast() {
        Toast.makeText(context, "Location cannot be modified once a ride is in progress.", Toast.LENGTH_SHORT).show()
    }

    var isEditingPickup by remember { mutableStateOf(if (isLocked) false else initialEditingPickup) }
    var editablePickupText by remember { mutableStateOf(pickupLocation.title) }

    var searchFieldText by remember {
        mutableStateOf(if (initialEditingPickup) "" else (initialWhereToText.ifBlank { destinationLocation?.title ?: "" }))
    }
    var suggestions by remember { mutableStateOf(routeService.popularSuggestions) }
    var isLoading by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var isGeocodingPickup by remember { mutableStateOf(false) }

    fun performSearch(query: String) {
        if (isLocked) {
            showLockedToast()
            return
        }
        searchFieldText = query
        searchJob?.cancel()
        searchJob = scope.launch {
            if (query.isBlank()) {
                suggestions = routeService.popularSuggestions
                isLoading = false
            } else {
                isLoading = true
                delay(250L) // Fast debounce for smooth typing
                val results = routeService.searchDestinations(query, pickupLocation.latitude, pickupLocation.longitude)
                suggestions = results
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (initialWhereToText.isNotBlank() && !isLocked) {
            performSearch(initialWhereToText)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        },
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        modifier = Modifier.testTag("pickup_destination_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isLocked) Color(0xFFFFB74D) else if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                        modifier = Modifier.size(12.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isLocked) "Ride in Progress (Locked)" else if (isEditingPickup) "Edit Pickup Location" else "Where To?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) Color(0xFFFFB74D) else if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                        fontSize = 22.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("close_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE65100).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pickup & Destination are locked while trip is active",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFCC80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle: "Current pickup location:"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pickup location (fixed):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                if (!isEditingPickup && !isLocked) {
                    TextButton(
                        onClick = {
                            editablePickupText = pickupLocation.title
                            isEditingPickup = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Change Pickup", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pickup Location Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.2.dp, if (isEditingPickup) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pickup_location_card")
                    .clickable(enabled = isLocked) {
                        showLockedToast()
                    }
            ) {
                if (isEditingPickup && !isLocked) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = editablePickupText,
                            onValueChange = { editablePickupText = it },
                            label = { Text("Pickup Address", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                cursorColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isEditingPickup = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (editablePickupText.isNotBlank()) {
                                        scope.launch {
                                            isGeocodingPickup = true
                                            val coords = locationHelper.geocodeAddress(editablePickupText)
                                            val lat = coords?.first ?: pickupLocation.latitude
                                            val lng = coords?.second ?: pickupLocation.longitude
                                            val newPickup = AppLocation(
                                                title = editablePickupText.trim(),
                                                subtitle = "Custom pickup in Peshawar",
                                                latitude = lat,
                                                longitude = lng
                                            )
                                            isGeocodingPickup = false
                                            onPickupSelected(newPickup)
                                            isEditingPickup = false
                                        }
                                    }
                                },
                                enabled = !isGeocodingPickup,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isGeocodingPickup) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Set Pickup", color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pickupLocation.title.ifBlank { "Locating pickup point in Peshawar..." },
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                if (pickupLocation.subtitle.isNotBlank()) {
                                    Text(
                                        text = pickupLocation.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val pLat = if (pickupLocation.latitude != 0.0) pickupLocation.latitude else 34.0151
                                val pLon = if (pickupLocation.longitude != 0.0) pickupLocation.longitude else 71.5249
                                Text(
                                    text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", pLat, pLon),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0288D1)
                                )
                            }
                        }
                        if (!isLocked) {
                            IconButton(
                                onClick = {
                                    editablePickupText = pickupLocation.title
                                    isEditingPickup = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Pickup Address",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search input field for destination (or pickup if editing pickup mode)
            OutlinedTextField(
                value = searchFieldText,
                onValueChange = { performSearch(it) },
                readOnly = isLocked,
                enabled = !isLocked,
                placeholder = {
                    Text(
                        text = if (isLocked) "Destination locked during active ride" else if (isEditingPickup) "Search & pick new pickup location..." else "Search destination (e.g. Saddar, Hayatabad...)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isLocked) Color(0xFFFFB74D) else if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple
                        )
                    } else if (searchFieldText.isNotEmpty() && !isLocked) {
                        IconButton(onClick = { performSearch("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedBorderColor = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    cursorColor = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (isLocked) {
                            showLockedToast()
                            return@KeyboardActions
                        }
                        keyboardController?.hide()
                        if (searchFieldText.isNotBlank()) {
                            val first = suggestions.firstOrNull()
                            if (first != null) {
                                if (isEditingPickup) {
                                    onPickupSelected(first.toAppLocation())
                                    isEditingPickup = false
                                } else {
                                    onDestinationSelected(first.toAppLocation())
                                }
                            } else {
                                val customLoc = AppLocation(
                                    title = searchFieldText.trim(),
                                    subtitle = "Custom location in Peshawar",
                                    latitude = pickupLocation.latitude + 0.012,
                                    longitude = pickupLocation.longitude + 0.012
                                )
                                if (isEditingPickup) {
                                    onPickupSelected(customLoc)
                                    isEditingPickup = false
                                } else {
                                    onDestinationSelected(customLoc)
                                }
                            }
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("where_to_bottom_card_input")
                    .clickable(enabled = isLocked) {
                        showLockedToast()
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Direct "Choose on Map" action card (hidden or disabled if locked)
            if (!isLocked) {
                Surface(
                    onClick = {
                        keyboardController?.hide()
                        onPickOnMap(isEditingPickup)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.2.dp, if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("choose_on_map_action_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isEditingPickup) Color(0xFF4CAF50).copy(alpha = 0.2f) else DrigoBrandPurple.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isEditingPickup) Icons.Default.Place else Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEditingPickup) "Set Pickup (From) on Map" else "Set Destination (To) on Map",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap any location point directly on the map",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Suggestions Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLocked) "Locations (Read-Only)" else if (searchFieldText.isBlank()) "Popular Places in Peshawar" else "Search Results",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isLocked) {
                    Text(
                        text = "${suggestions.size} places",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-Time Suggestion List (disabled / read-only with toast if locked)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 230.dp)
                    .testTag("suggestions_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions, key = { "${it.title}_${it.latitude}_${it.longitude}" }) { item ->
                    Surface(
                        onClick = {
                            if (isLocked) {
                                showLockedToast()
                                return@Surface
                            }
                            keyboardController?.hide()
                            if (isEditingPickup) {
                                onPickupSelected(item.toAppLocation())
                                isEditingPickup = false
                            } else {
                                onDestinationSelected(item.toAppLocation())
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dest_suggestion_${item.title.replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isLocked) MaterialTheme.colorScheme.surfaceVariant else if (isEditingPickup) Color(0xFF4CAF50).copy(alpha = 0.2f) else DrigoBrandPurple.copy(alpha = 0.2f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "📍 Lat: %.5f, Lon: %.5f", item.latitude, item.longitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0288D1),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.NorthEast,
                                contentDescription = if (isLocked) "Locked" else "Select",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
