package com.example.ui.screens.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverPerformanceScreen(
    driverName: String,
    driverRating: Double,
    driverRidesThisWeek: Int,
    todayIncomePkr: Int,
    dailyGoalPkr: Int,
    walletBalancePkr: Int,
    activeBonusesCount: Int,
    onSeeBenefitsClick: () -> Unit,
    onIncomeClick: () -> Unit,
    onAddGoalClick: () -> Unit,
    onTopUpClick: () -> Unit,
    onBonusesClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTierName = when {
        driverRidesThisWeek >= 50 && driverRating >= 4.80 -> "Platinum"
        driverRidesThisWeek >= 30 && driverRating >= 4.70 -> "Silver"
        driverRidesThisWeek >= 15 && driverRating >= 4.60 -> "Bronze"
        else -> "Basic"
    }

    val nextTierName = when (currentTierName) {
        "Basic" -> "Bronze"
        "Bronze" -> "Silver"
        "Silver" -> "Platinum"
        else -> "Platinum"
    }

    val nextTierRequiredRides = when (currentTierName) {
        "Basic" -> 15
        "Bronze" -> 30
        "Silver" -> 50
        else -> 50
    }

    val ridesRemaining = (nextTierRequiredRides - driverRidesThisWeek).coerceAtLeast(0)
    val progressFraction = (driverRidesThisWeek.toFloat() / nextTierRequiredRides.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1218))
            .padding(bottom = 70.dp)
    ) {
        // Top Header Card (Teal/Navy gradient matching Image 2)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF133043))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Menu Drawer Button
                Surface(
                    onClick = onOpenDrawer,
                    shape = CircleShape,
                    color = Color(0xFF102534),
                    border = BorderStroke(1.dp, Color(0xFF23445A)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = Color(0xFF102534),
                    border = BorderStroke(1.dp, Color(0xFF23445A)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar + Rating + Tier Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E3A4E),
                        border = BorderStroke(2.dp, Color(0xFF00E676)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = driverName.take(1).uppercase().ifBlank { "C" },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1B232A),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.offset(y = 4.dp, x = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = String.format(Locale.US, "%.2f", driverRating), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "$currentTierName ◆", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(text = "Your tier this week", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress to Next Tier Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F2231),
                border = BorderStroke(1.dp, Color(0xFF1A394E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (ridesRemaining > 0) "$ridesRemaining rides to $nextTierName" else "Top Tier Reached ($currentTierName)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Slider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1A3346))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = progressFraction)
                                .background(Brush.horizontalGradient(colors = listOf(Color(0xFF0288D1), Color(0xFF29B6F6))))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Keep 4.75+ rating", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // "See benefits" Button
            Button(
                onClick = onSeeBenefitsClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF102534)),
                border = BorderStroke(1.dp, Color(0xFF23445A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("see_benefits_button")
            ) {
                Text(text = "See benefits", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Body Section
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))

                // Section: Today's income >
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIncomeClick() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Today's income >", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(text = "PKR $todayIncomePkr", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

                Spacer(modifier = Modifier.height(12.dp))

                // Daily goal button
                if (dailyGoalPkr > 0) {
                    Surface(
                        onClick = onAddGoalClick,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1C222D),
                        border = BorderStroke(1.dp, Color(0xFF2A3142)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Daily Goal: PKR $dailyGoalPkr", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                val pct = ((todayIncomePkr.toFloat() / dailyGoalPkr.toFloat()).coerceIn(0f, 1f) * 100).toInt()
                                Text(text = "Progress: $pct% (PKR ${(dailyGoalPkr - todayIncomePkr).coerceAtLeast(0)} left)", fontSize = 12.sp, color = Color(0xFF00E676))
                            }
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Goal", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    Surface(
                        onClick = onAddGoalClick,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF181C25),
                        border = BorderStroke(1.dp, Color(0xFF2C3242)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Add daily goal", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wallet Balance Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181C25),
                    border = BorderStroke(1.dp, Color(0xFF272D3B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFF263238), modifier = Modifier.size(42.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(text = "PKR $walletBalancePkr", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "Wallet balance", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Button(
                            onClick = onTopUpClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C3A)),
                            modifier = Modifier.testTag("top_up_button")
                        ) {
                            Text(text = "Top up", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bonuses Card
                Surface(
                    onClick = onBonusesClick,
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181C25),
                    border = BorderStroke(1.dp, Color(0xFF272D3B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFF332A15), modifier = Modifier.size(42.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(text = "$activeBonusesCount Bonuses >", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
