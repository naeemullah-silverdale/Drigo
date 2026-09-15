package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Reusable M3 Shimmer gradient brush modifier for smooth loading states on low-end and high-end Android devices.
 */
fun Modifier.drigoShimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "DrigoShimmerTransition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DrigoShimmerAnimation"
    )

    val isDark = isSystemInDarkTheme()
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF222736),
            Color(0xFF333B52),
            Color(0xFF222736)
        )
    } else {
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF8FAFC),
            Color(0xFFE2E8F0)
        )
    }

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = translateAnimation - 600f, y = translateAnimation - 600f),
            end = Offset(x = translateAnimation, y = translateAnimation)
        )
    )
}

/**
 * Shimmer placeholder skeleton for Planned Departure cards on Driver screen.
 */
@Composable
fun PlannedDepartureCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .drigoShimmer()
                )
                Box(
                    modifier = Modifier
                        .size(width = 75.dp, height = 18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .drigoShimmer()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .drigoShimmer()
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .drigoShimmer()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .drigoShimmer()
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .drigoShimmer()
                )
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .drigoShimmer()
                )
            }
        }
    }
}

/**
 * Shimmer placeholder skeleton for Passenger Departure list items.
 */
@Composable
fun PassengerDepartureCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .drigoShimmer()
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .drigoShimmer()
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 70.dp, height = 10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .drigoShimmer()
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .drigoShimmer()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .drigoShimmer()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .drigoShimmer()
                )
                Box(
                    modifier = Modifier
                        .size(width = 95.dp, height = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .drigoShimmer()
                )
            }
        }
    }
}
