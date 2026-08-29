package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.DrigoBrandPurple
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val EasypaisaGreen = Color(0xFF00A859)
private val EasypaisaDarkGreen = Color(0xFF007A41)
private val EasypaisaLightBg = Color(0xFFE8F7F0)
private val DarkCardBg = Color(0xFF1E2026)
private val DarkSurfaceBg = Color(0xFF13151A)
private val BorderColor = Color(0xFF2C303B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    user: FirebaseUser?,
    userRole: String, // "PASSENGER" or "DRIVER"
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirebaseRepository.getInstance(context) }
    val userId = user?.uid ?: "guest_user"

    var wallet by remember {
        mutableStateOf(
            WalletEntity(
                userId = userId,
                walletId = "wal_$userId",
                balance = 1250.0,
                currency = "PKR",
                userRole = userRole
            )
        )
    }
    var transactions by remember { mutableStateOf<List<WalletTransactionEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    // Dialog States
    var showAddMoneySheet by remember { mutableStateOf(false) }
    var activePaymentRequest by remember { mutableStateOf<EasypaisaPaymentRequest?>(null) }
    var showEasypaisaGatewayDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to real-time wallet & transactions from backend
    LaunchedEffect(userId, userRole) {
        scope.launch {
            repo.listenToUserWallet(userId, userRole).collectLatest { w ->
                if (w != null) {
                    wallet = w
                }
            }
        }
        scope.launch {
            repo.listenToUserTransactions(userId).collectLatest { list ->
                transactions = list
            }
        }
    }

    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
    }

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "Top-ups" -> transactions.filter { it.type == TransactionType.TOP_UP }
            "Rides" -> transactions.filter { it.type == TransactionType.RIDE_PAYMENT }
            "Refunds" -> transactions.filter { it.type == TransactionType.REFUND }
            else -> transactions
        }
    }

    val totalTopUp = remember(transactions) {
        transactions.filter { it.type == TransactionType.TOP_UP && it.status == TransactionStatus.SUCCESS }
            .sumOf { it.amount }
    }
    val totalSpent = remember(transactions) {
        transactions.filter { it.type == TransactionType.RIDE_PAYMENT && it.status == TransactionStatus.SUCCESS }
            .sumOf { it.amount }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("wallet_screen"),
        containerColor = DarkSurfaceBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Wallet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (userRole == "DRIVER") DrigoBrandPurple.copy(alpha = 0.25f) else Color(0xFF00BCD4).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, if (userRole == "DRIVER") DrigoBrandPurple else Color(0xFF00BCD4))
                            ) {
                                Text(
                                    text = if (userRole == "DRIVER") "DRIVER ACCOUNT" else "PASSENGER ACCOUNT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (userRole == "DRIVER") Color(0xFFD1C4E9) else Color(0xFF80DEEA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Easypaisa Direct",
                                style = MaterialTheme.typography.labelSmall,
                                color = EasypaisaGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("wallet_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Wallet balance synced with backend ledger.")
                            }
                        },
                        modifier = Modifier.testTag("wallet_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Refresh Balance",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurfaceBg
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Balance Overview Card
            item {
                WalletBalanceCard(
                    balance = wallet.balance,
                    currency = wallet.currency,
                    userRole = userRole,
                    totalAdded = totalTopUp,
                    totalSpent = totalSpent,
                    currencyFormatter = currencyFormatter,
                    onAddMoneyClick = { showAddMoneySheet = true }
                )
            }

            // 2. Easypaisa Payment Method Banner
            item {
                EasypaisaProviderBanner(
                    onAddMoneyClick = { showAddMoneySheet = true }
                )
            }

            // 3. Transaction History Section Title & Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${transactions.size} records",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("All", "Top-ups", "Rides", "Refunds")
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = filter,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DrigoBrandPurple,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkCardBg,
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) DrigoBrandPurple else BorderColor,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("filter_chip_$filter")
                            )
                        }
                    }
                }
            }

            // 4. Transaction List Items
            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyTransactionsPlaceholder(
                        filter = selectedFilter,
                        onAddMoneyClick = { showAddMoneySheet = true }
                    )
                }
            } else {
                items(filteredTransactions, key = { it.transactionId }) { txn ->
                    TransactionItemCard(
                        transaction = txn,
                        currencyFormatter = currencyFormatter
                    )
                }
            }
        }
    }

    // --- Bottom Sheet 1: Add Money Input ---
    if (showAddMoneySheet) {
        AddMoneyBottomSheet(
            userRole = userRole,
            isLoading = isLoading,
            onDismiss = { showAddMoneySheet = false },
            onContinue = { amount, mobileNumber ->
                scope.launch {
                    isLoading = true
                    val result = repo.initiateEasypaisaTopUp(
                        userId = userId,
                        userRole = userRole,
                        amount = amount,
                        mobileNumber = mobileNumber
                    )
                    isLoading = false
                    if (result.isSuccess) {
                        activePaymentRequest = result.getOrNull()
                        showAddMoneySheet = false
                        showEasypaisaGatewayDialog = true
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Failed to initiate top-up"
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }
        )
    }

    // --- Dialog 2: Easypaisa Payment Verification Gateway ---
    if (showEasypaisaGatewayDialog && activePaymentRequest != null) {
        EasypaisaGatewayDialog(
            paymentRequest = activePaymentRequest!!,
            currencyFormatter = currencyFormatter,
            onDismiss = {
                scope.launch {
                    repo.cancelEasypaisaPayment(activePaymentRequest!!.transactionId, "Cancelled by user")
                    showEasypaisaGatewayDialog = false
                    activePaymentRequest = null
                    snackbarHostState.showSnackbar("Easypaisa payment cancelled.")
                }
            },
            onVerifySuccess = { otpOrPin, onFinish ->
                scope.launch {
                    val res = repo.verifyAndProcessEasypaisaPayment(
                        userId = userId,
                        userRole = userRole,
                        orderId = activePaymentRequest!!.orderId,
                        transactionId = activePaymentRequest!!.transactionId,
                        otpOrPin = otpOrPin
                    )
                    if (res.isSuccess) {
                        onFinish(true, res.getOrNull()?.responseMessage ?: "Payment successful!")
                        delay(1200)
                        showEasypaisaGatewayDialog = false
                        activePaymentRequest = null
                        snackbarHostState.showSnackbar("Wallet successfully credited via Easypaisa!")
                    } else {
                        val err = res.exceptionOrNull()?.message ?: "Payment verification failed"
                        onFinish(false, err)
                    }
                }
            }
        )
    }
}

@Composable
private fun WalletBalanceCard(
    balance: Double,
    currency: String,
    userRole: String,
    totalAdded: Double,
    totalSpent: Double,
    currencyFormatter: NumberFormat,
    onAddMoneyClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, DrigoBrandPurple.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wallet_balance_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E1B4E),
                            Color(0xFF191B22)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB39DDB),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$currency ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = currencyFormatter.format(balance),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 34.sp
                            )
                        }
                    }

                    // Role Badge
                    Surface(
                        shape = CircleShape,
                        color = EasypaisaGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, EasypaisaGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EasypaisaGreen,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF3B2F5E), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Stats breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Added",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = "+ $currency ${currencyFormatter.format(totalAdded)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (userRole == "DRIVER") "Total Earned" else "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = "- $currency ${currencyFormatter.format(totalSpent)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8A80)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Add Money Primary Button
                Button(
                    onClick = onAddMoneyClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EasypaisaGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("add_money_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Money (Easypaisa)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EasypaisaProviderBanner(
    onAddMoneyClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EasypaisaGreen,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "EP",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Easypaisa Instant Top-up",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Fast 0% fee deposit via your mobile account",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = onAddMoneyClick,
                modifier = Modifier
                    .background(Color(0xFF2A2D36), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Add Money",
                    tint = EasypaisaGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionItemCard(
    transaction: WalletTransactionEntity,
    currencyFormatter: NumberFormat
) {
    val isCredit = transaction.type == TransactionType.TOP_UP || transaction.type == TransactionType.REFUND
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }
    val formattedDate = remember(transaction.createdAt) {
        dateFormatter.format(Date(transaction.createdAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.transactionId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = when (transaction.type) {
                    TransactionType.TOP_UP -> EasypaisaGreen.copy(alpha = 0.15f)
                    TransactionType.RIDE_PAYMENT -> DrigoBrandPurple.copy(alpha = 0.2f)
                    TransactionType.REFUND -> Color(0xFF00BCD4).copy(alpha = 0.2f)
                    TransactionType.ADJUSTMENT -> Color.Gray.copy(alpha = 0.2f)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.TOP_UP -> Icons.Default.ArrowDownward
                            TransactionType.RIDE_PAYMENT -> Icons.Default.DirectionsCar
                            TransactionType.REFUND -> Icons.Default.Replay
                            TransactionType.ADJUSTMENT -> Icons.Default.Tune
                        },
                        contentDescription = null,
                        tint = when (transaction.type) {
                            TransactionType.TOP_UP -> EasypaisaGreen
                            TransactionType.RIDE_PAYMENT -> Color(0xFFCE93D8)
                            TransactionType.REFUND -> Color(0xFF4DD0E1)
                            TransactionType.ADJUSTMENT -> Color.LightGray
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (transaction.type) {
                        TransactionType.TOP_UP -> "Easypaisa Top-up"
                        TransactionType.RIDE_PAYMENT -> "Ride Payment"
                        TransactionType.REFUND -> "Ride Fare Refund"
                        TransactionType.ADJUSTMENT -> "Wallet Adjustment"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (transaction.notes.isNotBlank()) {
                    Text(
                        text = transaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    if (transaction.referenceId.isNotBlank()) {
                        Text(
                            text = "• ${transaction.referenceId.takeLast(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount & Status Badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isCredit) "+ " else "- ") + "PKR ${currencyFormatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCredit) Color(0xFF81C784) else Color(0xFFFF8A80)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (transaction.status) {
                        TransactionStatus.SUCCESS -> Color(0xFF1B5E20).copy(alpha = 0.35f)
                        TransactionStatus.PENDING -> Color(0xFFE65100).copy(alpha = 0.35f)
                        TransactionStatus.FAILED -> Color(0xFFB71C1C).copy(alpha = 0.35f)
                        TransactionStatus.CANCELLED -> Color(0xFF424242).copy(alpha = 0.35f)
                    }
                ) {
                    Text(
                        text = transaction.status.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.status) {
                            TransactionStatus.SUCCESS -> Color(0xFFA5D6A7)
                            TransactionStatus.PENDING -> Color(0xFFFFCC80)
                            TransactionStatus.FAILED -> Color(0xFFEF9A9A)
                            TransactionStatus.CANCELLED -> Color.LightGray
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsPlaceholder(
    filter: String,
    onAddMoneyClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = DrigoBrandPurple.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = DrigoBrandPurple,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "No $filter Transactions Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Top up your wallet using Easypaisa to start booking rides seamlessly.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onAddMoneyClick,
                colors = ButtonDefaults.buttonColors(containerColor = EasypaisaGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Money via Easypaisa", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMoneyBottomSheet(
    userRole: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onContinue: (amount: Double, mobileNumber: String) -> Unit
) {
    var amountInput by remember { mutableStateOf("1000") }
    var mobileInput by remember { mutableStateOf("03001234567") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetAmounts = listOf(500, 1000, 2000, 5000)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E2026),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Money",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Top up your Drigo $userRole Wallet",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EasypaisaGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, EasypaisaGreen)
                ) {
                    Text(
                        text = "Easypaisa",
                        fontWeight = FontWeight.Bold,
                        color = EasypaisaGreen,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Amount Input Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Amount (PKR)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.LightGray
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it.filter { ch -> ch.isDigit() }
                        errorMessage = null
                    },
                    prefix = { Text("PKR  ", fontWeight = FontWeight.Bold, color = EasypaisaGreen) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EasypaisaGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF13151A),
                        unfocusedContainerColor = Color(0xFF13151A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_money_amount_input")
                )
            }

            // Quick Preset Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetAmounts) { amt ->
                    val isSelected = amountInput == amt.toString()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EasypaisaGreen.copy(alpha = 0.25f) else Color(0xFF2A2D36),
                        border = BorderStroke(1.dp, if (isSelected) EasypaisaGreen else BorderColor),
                        modifier = Modifier.clickable { amountInput = amt.toString() }
                    ) {
                        Text(
                            text = "+ PKR $amt",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Mobile Account Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Easypaisa Mobile Number",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.LightGray
                )
                OutlinedTextField(
                    value = mobileInput,
                    onValueChange = {
                        mobileInput = it
                        errorMessage = null
                    },
                    leadingIcon = {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = EasypaisaGreen)
                    },
                    placeholder = { Text("0300 1234567", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EasypaisaGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF13151A),
                        unfocusedContainerColor = Color(0xFF13151A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("easypaisa_mobile_input")
                )
            }

            // Payment Method Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF13151A),
                    border = BorderStroke(1.2.dp, EasypaisaGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = true,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = EasypaisaGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Easypaisa Mobile Account",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Direct gateway verification with 0% fee",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Submit Button
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    if (amt < 50.0) {
                        errorMessage = "Minimum top-up amount is PKR 50"
                        return@Button
                    }
                    if (mobileInput.length < 10) {
                        errorMessage = "Please enter a valid mobile number"
                        return@Button
                    }
                    onContinue(amt, mobileInput)
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EasypaisaGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_easypaisa_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EasypaisaGatewayDialog(
    paymentRequest: EasypaisaPaymentRequest,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onVerifySuccess: (otpOrPin: String, onFinish: (Boolean, String) -> Unit) -> Unit
) {
    var otpInput by remember { mutableStateOf("7890") }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationStep by remember { mutableStateOf("Ready to authorize") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessPhase by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isVerifying, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D24)),
            border = BorderStroke(1.5.dp, EasypaisaGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("easypaisa_gateway_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EasypaisaGreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "EP",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Easypaisa Checkout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Secure Payment Gateway",
                                style = MaterialTheme.typography.labelSmall,
                                color = EasypaisaGreen
                            )
                        }
                    }

                    if (!isVerifying && !isSuccessPhase) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Order summary container
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF12141A),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Order Reference", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = paymentRequest.orderId.takeLast(12),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mobile Account", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = paymentRequest.mobileNumber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }
                        HorizontalDivider(color = Color(0xFF252833))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Top-up Amount", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = "PKR ${currencyFormatter.format(paymentRequest.amount)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = EasypaisaGreen
                            )
                        }
                    }
                }

                if (!isSuccessPhase) {
                    // Security PIN input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Enter 4-digit Easypaisa PIN / OTP",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray
                        )
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    otpInput = it.filter { ch -> ch.isDigit() }
                                    errorMessage = null
                                }
                            },
                            singleLine = true,
                            enabled = !isVerifying,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EasypaisaGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF12141A),
                                unfocusedContainerColor = Color(0xFF12141A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("easypaisa_pin_input")
                        )
                        Text(
                            text = "Sandbox test default: 7890. Verified on backend.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    if (isVerifying) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = EasypaisaGreen,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = verificationStep,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isVerifying,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (otpInput.length < 4) {
                                    errorMessage = "Please enter a 4-digit PIN"
                                    return@Button
                                }
                                isVerifying = true
                                verificationStep = "Contacting Easypaisa Server..."
                                onVerifySuccess(otpInput) { success, msg ->
                                    isVerifying = false
                                    if (success) {
                                        isSuccessPhase = true
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            enabled = !isVerifying,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EasypaisaGreen),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("easypaisa_pay_btn")
                        ) {
                            Text(
                                text = "Approve & Pay",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Success View
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EasypaisaGreen.copy(alpha = 0.2f),
                            border = BorderStroke(2.dp, EasypaisaGreen),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EasypaisaGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Payment Verified!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "PKR ${currencyFormatter.format(paymentRequest.amount)} has been credited to your wallet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
