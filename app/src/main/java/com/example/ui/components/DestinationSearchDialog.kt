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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DestinationSuggestion
import com.example.data.RouteService
import com.example.ui.theme.DrigoBrandPurple
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DestinationSearchDialog(
    initialQuery: String = "",
    currentLat: Double = 34.0151,
    currentLng: Double = 71.5249,
    onDismiss: () -> Unit,
    onDestinationSelected: (suggestion: DestinationSuggestion) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routeService = remember { RouteService(context) }

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var suggestions by remember { mutableStateOf(routeService.popularSuggestions) }
    var isLoading by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Live search query debounce
    fun performSearch(query: String) {
        searchQuery = query
        searchJob?.cancel()
        searchJob = scope.launch {
            if (query.isBlank()) {
                suggestions = routeService.popularSuggestions
                isLoading = false
            } else {
                isLoading = true
                delay(300L) // Debounce for smooth typing
                val results = routeService.searchDestinations(query, currentLat, currentLng)
                suggestions = results
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("destination_search_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = DrigoBrandPurple,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Where To?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DrigoBrandPurple
                            )
                            Text(
                                text = "Search or select destination",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_search_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Input Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { performSearch(it) },
                    placeholder = { Text("Search location, landmark or area...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DrigoBrandPurple
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { performSearch("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DrigoBrandPurple,
                        unfocusedBorderColor = DrigoBrandPurple.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destination_search_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header for suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Popular Destinations in Peshawar" else "Search Results",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = DrigoBrandPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of Destinations
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions, key = { "${it.title}_${it.latitude}_${it.longitude}" }) { suggestion ->
                        SuggestionItemCard(
                            suggestion = suggestion,
                            onClick = {
                                onDestinationSelected(suggestion)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Confirm custom text destination button if typed query
                if (searchQuery.isNotBlank()) {
                    Button(
                        onClick = {
                            val first = suggestions.firstOrNull()
                            if (first != null) {
                                onDestinationSelected(first)
                            } else {
                                onDestinationSelected(
                                    DestinationSuggestion(
                                        title = searchQuery.trim(),
                                        subtitle = "Custom destination",
                                        latitude = currentLat + 0.02,
                                        longitude = currentLng + 0.02
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_destination_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Set as Destination & View Route",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItemCard(
    suggestion: DestinationSuggestion,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF7F8FA),
        border = BorderStroke(1.dp, Color(0xFFE4E7EB)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("destination_item_${suggestion.title.replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = DrigoBrandPurple.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DrigoBrandPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E2024),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = suggestion.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
