package com.example.ui.components

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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupDestinationBottomCard(
    pickupLocation: AppLocation,
    destinationLocation: AppLocation? = null,
    initialEditingPickup: Boolean = false,
    initialWhereToText: String = "",
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

    var isEditingPickup by remember { mutableStateOf(initialEditingPickup) }
    var editablePickupText by remember { mutableStateOf(pickupLocation.title) }

    var searchFieldText by remember {
        mutableStateOf(if (initialEditingPickup) "" else (initialWhereToText.ifBlank { destinationLocation?.title ?: "" }))
    }
    var suggestions by remember { mutableStateOf(routeService.popularSuggestions) }
    var isLoading by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var isGeocodingPickup by remember { mutableStateOf(false) }

    fun performSearch(query: String) {
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
        if (initialWhereToText.isNotBlank()) {
            performSearch(initialWhereToText)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF22242B),
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = Color(0xFF616470)
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
                        color = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                        modifier = Modifier.size(12.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isEditingPickup) "Edit Pickup Location" else "Where To?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isEditingPickup) Color(0xFF81C784) else DrigoBrandPurple,
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
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
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
                    color = Color(0xFFB0B3BC),
                    fontSize = 13.sp
                )
                if (!isEditingPickup) {
                    TextButton(
                        onClick = {
                            editablePickupText = pickupLocation.title
                            isEditingPickup = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Change Pickup", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pickup Location Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF2C2F38),
                border = BorderStroke(1.2.dp, if (isEditingPickup) Color(0xFF4CAF50) else Color(0xFF4A4E5C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pickup_location_card")
            ) {
                if (isEditingPickup) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = editablePickupText,
                            onValueChange = { editablePickupText = it },
                            label = { Text("Pickup Address", color = Color(0xFFB0B3BC)) },
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color(0xFF616470),
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
                                Text("Cancel", color = Color(0xFFB0B3BC))
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
                                    Text("Set Pickup")
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
                            Text(
                                text = pickupLocation.title.ifBlank { "Locating pickup point in Peshawar..." },
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Normal
                            )
                        }
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
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(16.dp)
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
                placeholder = {
                    Text(
                        text = if (isEditingPickup) "Search & pick new pickup location..." else "Search destination (e.g. Saddar, Hayatabad...)",
                        color = Color(0xFF8E92A0),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
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
                    } else if (searchFieldText.isNotEmpty()) {
                        IconButton(onClick = { performSearch("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF2C2F38),
                    unfocusedContainerColor = Color(0xFF2C2F38),
                    focusedBorderColor = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple,
                    unfocusedBorderColor = Color(0xFF4A4E5C),
                    cursorColor = if (isEditingPickup) Color(0xFF4CAF50) else DrigoBrandPurple
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
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
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Direct "Choose on Map" action card
            Surface(
                onClick = {
                    keyboardController?.hide()
                    onPickOnMap(isEditingPickup)
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2C2F38),
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
                                tint = if (isEditingPickup) Color(0xFF81C784) else Color(0xFFFF80AB),
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
                            color = Color.White
                        )
                        Text(
                            text = "Tap any location point directly on the map",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (isEditingPickup) Color(0xFF81C784) else Color(0xFFFF80AB),
                        modifier = Modifier.size(18.dp)
                    )
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
                    text = if (searchFieldText.isBlank()) "Popular Places in Peshawar" else "Search Results",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB0B3BC)
                )
                Text(
                    text = "${suggestions.size} places",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF757885)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-Time Suggestion List
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
                            keyboardController?.hide()
                            if (isEditingPickup) {
                                onPickupSelected(item.toAppLocation())
                                isEditingPickup = false
                            } else {
                                onDestinationSelected(item.toAppLocation())
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2C2F38),
                        border = BorderStroke(1.dp, Color(0xFF3B3E4A)),
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
                                color = if (isEditingPickup) Color(0xFF4CAF50).copy(alpha = 0.2f) else DrigoBrandPurple.copy(alpha = 0.2f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isEditingPickup) Color(0xFF81C784) else DrigoBrandPurple,
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
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9E9E9E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.NorthEast,
                                contentDescription = "Select",
                                tint = Color(0xFF757885),
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
