package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.DriverDocumentItem
import com.example.data.model.DriverVerification
import com.example.data.model.VerificationStatus
import com.example.data.remote.DriverDocumentStorageManager
import com.example.data.remote.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val AdminNavyBg = Color(0xFF0F172A)
val AdminCardBg = Color(0xFF1E293B)
val AdminSurfaceBorder = Color(0xFF334155)
val AdminAccentTeal = Color(0xFF06B6D4)
val StatusApprovedGreen = Color(0xFF10B981)
val StatusPendingAmber = Color(0xFFF59E0B)
val StatusRejectedRed = Color(0xFFEF4444)
val StatusReviewBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repo = remember(context) { FirebaseRepository.getInstance(context) }

    var allVerifications by remember { mutableStateOf<List<DriverVerification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf<String>("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDriver by remember { mutableStateOf<DriverVerification?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf<String?>(null) }

    // Dialog state for driver rejection reason
    var showRejectDriverDialog by remember { mutableStateOf(false) }
    var driverRejectionReason by remember { mutableStateOf("") }

    // Dialog state for individual document rejection
    var docToReject by remember { mutableStateOf<DriverDocumentItem?>(null) }
    var docRejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repo.listenToAllDriverVerifications().collect { list ->
            allVerifications = list
            isLoading = false
            // Update selectedDriver if it's currently open
            if (selectedDriver != null) {
                selectedDriver = list.find { it.uid == selectedDriver?.uid } ?: selectedDriver
            }
        }
    }

    val filteredList = remember(allVerifications, selectedFilter, searchQuery) {
        allVerifications.filter { ver ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "PENDING" -> ver.status == "PENDING" || ver.status == "PENDING_VERIFICATION"
                "UNDER_REVIEW" -> ver.status == "UNDER_REVIEW"
                "APPROVED" -> ver.status == "APPROVED" || ver.confirmtion
                "REJECTED" -> ver.status == "REJECTED"
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    ver.name.contains(searchQuery, ignoreCase = true) ||
                    ver.phone.contains(searchQuery, ignoreCase = true) ||
                    ver.email.contains(searchQuery, ignoreCase = true) ||
                    ver.vehicleNumber.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedDriver != null) "Driver Document Review" else "Admin Verification Portal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (selectedDriver != null) selectedDriver?.name.orEmpty() else "Secure Document Vault & KYC Approval",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminAccentTeal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedDriver != null) {
                            selectedDriver = null
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AdminAccentTeal.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AdminAccentTeal.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AdminAccentTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ADMIN ACCESS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminAccentTeal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminNavyBg
                )
            )
        },
        containerColor = AdminNavyBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AdminNavyBg)
        ) {
            if (selectedDriver != null) {
                // Driver Document Detail Screen
                DriverDetailReviewView(
                    driver = selectedDriver!!,
                    onPreviewImage = { url, title ->
                        previewImageUrl = url
                        previewImageTitle = title
                    },
                    onApproveDocument = { doc ->
                        scope.launch {
                            val updatedDocs = selectedDriver!!.documents.map {
                                if (it.docType == doc.docType) it.copy(status = "APPROVED", rejectionReason = "") else it
                            }
                            val res = repo.reviewDriverVerification(
                                uid = selectedDriver!!.uid,
                                status = if (selectedDriver!!.status == "REJECTED") "UNDER_REVIEW" else selectedDriver!!.status,
                                updatedDocs = updatedDocs
                            )
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("${doc.title} marked as APPROVED")
                            }
                        }
                    },
                    onRejectDocumentClick = { doc ->
                        docToReject = doc
                        docRejectionReason = ""
                    },
                    onApproveAllAndConfirm = {
                        scope.launch {
                            val approvedDocs = selectedDriver!!.documents.map {
                                it.copy(status = "APPROVED", rejectionReason = "")
                            }
                            val res = repo.reviewDriverVerification(
                                uid = selectedDriver!!.uid,
                                status = "APPROVED",
                                reviewNotes = "All verification documents approved by Admin.",
                                rejectionReason = "",
                                updatedDocs = approvedDocs
                            )
                            if (res.isSuccess) {
                                repo.updateDriverConfirmation(selectedDriver!!.uid, true)
                                snackbarHostState.showSnackbar("Driver ${selectedDriver!!.name} APPROVED successfully!")
                            } else {
                                snackbarHostState.showSnackbar("Error: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    },
                    onMarkUnderReview = {
                        scope.launch {
                            repo.reviewDriverVerification(
                                uid = selectedDriver!!.uid,
                                status = "UNDER_REVIEW",
                                reviewNotes = "Document inspection currently in progress by compliance team."
                            )
                            snackbarHostState.showSnackbar("Marked as UNDER_REVIEW")
                        }
                    },
                    onRejectDriverClick = {
                        showRejectDriverDialog = true
                        driverRejectionReason = ""
                    }
                )
            } else {
                // List of Drivers Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, phone, plate #...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AdminAccentTeal) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AdminAccentTeal,
                            unfocusedBorderColor = AdminSurfaceBorder,
                            focusedContainerColor = AdminCardBg,
                            unfocusedContainerColor = AdminCardBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Tabs
                    val filters = listOf("ALL", "PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            val count = when (filter) {
                                "ALL" -> allVerifications.size
                                "PENDING" -> allVerifications.count { it.status == "PENDING" || it.status == "PENDING_VERIFICATION" }
                                "UNDER_REVIEW" -> allVerifications.count { it.status == "UNDER_REVIEW" }
                                "APPROVED" -> allVerifications.count { it.status == "APPROVED" || it.confirmtion }
                                "REJECTED" -> allVerifications.count { it.status == "REJECTED" }
                                else -> 0
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = "${filter.replace("_", " ")} ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AdminAccentTeal,
                                    selectedLabelColor = Color.Black,
                                    containerColor = AdminCardBg,
                                    labelColor = Color.White.copy(alpha = 0.85f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) AdminAccentTeal else AdminSurfaceBorder,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AdminAccentTeal)
                        }
                    } else if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No driver verification requests found",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredList, key = { it.uid }) { driver ->
                                DriverVerificationCard(
                                    driver = driver,
                                    onClick = { selectedDriver = driver }
                                )
                            }
                        }
                    }
                }
            }

            // Image Preview Modal Dialog
            if (previewImageUrl != null) {
                Dialog(onDismissRequest = { previewImageUrl = null }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AdminCardBg,
                        border = BorderStroke(1.dp, AdminSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = previewImageTitle ?: "Document Inspection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!previewImageUrl.isNullOrBlank()) {
                                        IconButton(onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(previewImageUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Open in Drive",
                                                tint = AdminAccentTeal
                                            )
                                        }
                                    }
                                    IconButton(onClick = { previewImageUrl = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = previewImageUrl,
                                    contentDescription = previewImageTitle,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "High-Resolution KYC Inspection View",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminAccentTeal
                            )
                        }
                    }
                }
            }

            // Reject Driver Overall Dialog
            if (showRejectDriverDialog) {
                AlertDialog(
                    onDismissRequest = { showRejectDriverDialog = false },
                    title = { Text("Reject Driver Verification", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "Provide a rejection reason so the driver knows which documents need to be replaced:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = driverRejectionReason,
                                onValueChange = { driverRejectionReason = it },
                                placeholder = { Text("e.g. CNIC photo is blurry, License is expired...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = StatusRejectedRed,
                                    unfocusedBorderColor = AdminSurfaceBorder,
                                    focusedContainerColor = AdminNavyBg,
                                    unfocusedContainerColor = AdminNavyBg
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val reason = driverRejectionReason.ifBlank { "Documents could not be verified. Please re-upload clear photos." }
                                showRejectDriverDialog = false
                                scope.launch {
                                    repo.reviewDriverVerification(
                                        uid = selectedDriver!!.uid,
                                        status = "REJECTED",
                                        rejectionReason = reason,
                                        reviewNotes = "Application rejected by admin: $reason"
                                    )
                                    repo.updateDriverConfirmation(selectedDriver!!.uid, false)
                                    snackbarHostState.showSnackbar("Driver registration marked as REJECTED.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed)
                        ) {
                            Text("Reject Driver", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRejectDriverDialog = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    containerColor = AdminCardBg
                )
            }

            // Reject Individual Document Dialog
            if (docToReject != null) {
                AlertDialog(
                    onDismissRequest = { docToReject = null },
                    title = { Text("Reject ${docToReject?.title}", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = "Enter the specific reason for rejecting this document:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = docRejectionReason,
                                onValueChange = { docRejectionReason = it },
                                placeholder = { Text("e.g. Blurry photo, corner cut off, expired...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = StatusRejectedRed,
                                    unfocusedBorderColor = AdminSurfaceBorder,
                                    focusedContainerColor = AdminNavyBg,
                                    unfocusedContainerColor = AdminNavyBg
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val target = docToReject!!
                                val reason = docRejectionReason.ifBlank { "Invalid or unreadable document photo." }
                                docToReject = null
                                scope.launch {
                                    val updatedDocs = selectedDriver!!.documents.map {
                                        if (it.docType == target.docType) it.copy(status = "REJECTED", rejectionReason = reason) else it
                                    }
                                    val res = repo.reviewDriverVerification(
                                        uid = selectedDriver!!.uid,
                                        status = "UNDER_REVIEW",
                                        reviewNotes = "Document ${target.title} rejected: $reason",
                                        updatedDocs = updatedDocs
                                    )
                                    if (res.isSuccess) {
                                        snackbarHostState.showSnackbar("${target.title} marked as REJECTED")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed)
                        ) {
                            Text("Confirm Rejection", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { docToReject = null }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    containerColor = AdminCardBg
                )
            }
        }
    }
}

@Composable
fun DriverVerificationCard(
    driver: DriverVerification,
    onClick: () -> Unit
) {
    val statusColor = when (driver.status) {
        "APPROVED" -> StatusApprovedGreen
        "REJECTED" -> StatusRejectedRed
        "UNDER_REVIEW" -> StatusReviewBlue
        else -> StatusPendingAmber
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(driver.submittedAt) {
        if (driver.submittedAt > 0) dateFormatter.format(Date(driver.submittedAt)) else "Recently"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AdminCardBg,
        border = BorderStroke(1.dp, AdminSurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AdminSurfaceBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        if (driver.driverPhotoUri.isNotBlank()) {
                            AsyncImage(
                                model = driver.driverPhotoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = driver.name.ifBlank { "Driver Applicant" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${driver.phone.ifBlank { "No phone" }} • ${driver.vehicleCompany} ${driver.vehicleModel} (${driver.vehicleNumber})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = driver.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AdminSurfaceBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Submitted: $formattedDate",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Inspect Documents",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminAccentTeal
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AdminAccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DriverDetailReviewView(
    driver: DriverVerification,
    onPreviewImage: (String, String) -> Unit,
    onApproveDocument: (DriverDocumentItem) -> Unit,
    onRejectDocumentClick: (DriverDocumentItem) -> Unit,
    onApproveAllAndConfirm: () -> Unit,
    onMarkUnderReview: () -> Unit,
    onRejectDriverClick: () -> Unit
) {
    // Merge structured documents list or build from individual URI fields
    val allDocs = remember(driver) {
        if (driver.documents.isNotEmpty()) {
            driver.documents
        } else {
            val list = mutableListOf<DriverDocumentItem>()
            if (driver.driverPhotoUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_DRIVER_PHOTO,
                    title = "Driver Profile Photo",
                    category = "profile",
                    storagePath = "drivers/${driver.uid}/profile/driver_profile_photo.jpg",
                    fileUrl = driver.driverPhotoUri,
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.cnicFrontUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_CNIC_FRONT,
                    title = "CNIC / National ID (Front)",
                    category = "identity",
                    storagePath = "drivers/${driver.uid}/identity/cnic_front.jpg",
                    fileUrl = driver.cnicFrontUri,
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.cnicBackUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_CNIC_BACK,
                    title = "CNIC / National ID (Back)",
                    category = "identity",
                    storagePath = "drivers/${driver.uid}/identity/cnic_back.jpg",
                    fileUrl = driver.cnicBackUri,
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.drivingLicenseFrontUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_LICENSE_FRONT,
                    title = "Driving License (Front)",
                    category = "license",
                    storagePath = "drivers/${driver.uid}/license/license_front.jpg",
                    fileUrl = driver.drivingLicenseFrontUri,
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.drivingLicenseBackUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_LICENSE_BACK,
                    title = "Driving License (Back)",
                    category = "license",
                    storagePath = "drivers/${driver.uid}/license/license_back.jpg",
                    fileUrl = driver.drivingLicenseBackUri,
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.vehicleFrontUri.isNotBlank() || driver.vehiclePictureUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_VEHICLE_FRONT,
                    title = "Vehicle (Front View)",
                    category = "vehicle",
                    storagePath = "drivers/${driver.uid}/vehicle/vehicle_front.jpg",
                    fileUrl = driver.vehicleFrontUri.ifBlank { driver.vehiclePictureUri },
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.vehicleBackUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_VEHICLE_BACK,
                    title = "Vehicle (Back View)",
                    category = "vehicle",
                    storagePath = "drivers/${driver.uid}/vehicle/vehicle_back.jpg",
                    fileUrl = driver.vehicleBackUri,
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.vehicleSideUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_VEHICLE_SIDE,
                    title = "Vehicle (Side View)",
                    category = "vehicle",
                    storagePath = "drivers/${driver.uid}/vehicle/vehicle_side.jpg",
                    fileUrl = driver.vehicleSideUri,
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.vehicleRegistrationDocUri.isNotBlank() || driver.vehicleCardDocFrontUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_VEHICLE_REGISTRATION,
                    title = "Vehicle Registration Document",
                    category = "documents",
                    storagePath = "drivers/${driver.uid}/documents/vehicle_registration.jpg",
                    fileUrl = driver.vehicleRegistrationDocUri.ifBlank { driver.vehicleCardDocFrontUri },
                    isRequired = true,
                    status = driver.status
                ))
            }
            if (driver.additionalDocUri.isNotBlank()) {
                list.add(DriverDocumentItem(
                    docType = DriverDocumentStorageManager.DOC_ADDITIONAL_DOC,
                    title = "Additional Verification Document",
                    category = "documents",
                    storagePath = "drivers/${driver.uid}/documents/additional_doc.jpg",
                    fileUrl = driver.additionalDocUri,
                    isRequired = false,
                    status = driver.status
                ))
            }
            list
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Driver Overview Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AdminCardBg,
            border = BorderStroke(1.dp, AdminSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = driver.name.ifBlank { "Driver Applicant" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "UID: ${driver.uid.take(16)}...",
                            fontSize = 11.sp,
                            color = AdminAccentTeal
                        )
                    }

                    val statusColor = when (driver.status) {
                        "APPROVED" -> StatusApprovedGreen
                        "REJECTED" -> StatusRejectedRed
                        "UNDER_REVIEW" -> StatusReviewBlue
                        else -> StatusPendingAmber
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, statusColor)
                    ) {
                        Text(
                            text = driver.status,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = AdminSurfaceBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Phone Number", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(text = driver.phone.ifBlank { "N/A" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Vehicle Info", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(text = "${driver.vehicleCompany} ${driver.vehicleModel}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Email Address", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(text = driver.email.ifBlank { "N/A" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Registration Plate", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(text = driver.vehicleNumber.ifBlank { "N/A" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                if (driver.rejectionReason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusRejectedRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, StatusRejectedRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRejectedRed, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Rejection Reason: ${driver.rejectionReason}",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onApproveAllAndConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Approve Driver", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onMarkUnderReview,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StatusReviewBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusReviewBlue),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = StatusReviewBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Under Review", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = onRejectDriverClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reject", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Submitted Verification Documents (${allDocs.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Inspect high-resolution copies, verify security criteria, and approve or reject each file.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Grid of Document Review Cards
        allDocs.forEach { doc ->
            AdminDocumentItemCard(
                doc = doc,
                onPreview = { onPreviewImage(doc.fileUrl, doc.title) },
                onApprove = { onApproveDocument(doc) },
                onReject = { onRejectDocumentClick(doc) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun AdminDocumentItemCard(
    doc: DriverDocumentItem,
    onPreview: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val docStatusColor = when (doc.status) {
        "APPROVED" -> StatusApprovedGreen
        "REJECTED" -> StatusRejectedRed
        "UNDER_REVIEW" -> StatusReviewBlue
        else -> StatusPendingAmber
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AdminCardBg,
        border = BorderStroke(1.dp, AdminSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (doc.isRequired) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AdminAccentTeal.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "REQUIRED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdminAccentTeal,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Storage: ${doc.storagePath}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = docStatusColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, docStatusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = doc.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = docStatusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thumbnail Image & Meta Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .border(1.dp, AdminSurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onPreview),
                    contentAlignment = Alignment.Center
                ) {
                    if (doc.fileUrl.isNotBlank()) {
                        AsyncImage(
                            model = doc.fileUrl,
                            contentDescription = doc.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Format: ${doc.fileType}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(text = "Size: ${if (doc.fileSize > 0) "${doc.fileSize / 1024} KB" else "Processed"}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(text = "Category: ${doc.category}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    if (doc.googleDriveFileId.isNotBlank()) {
                        Text(
                            text = "Drive ID: ${doc.googleDriveFileId.take(14)}...",
                            fontSize = 10.sp,
                            color = AdminAccentTeal
                        )
                    }
                    
                    if (doc.rejectionReason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reason: ${doc.rejectionReason}",
                            fontSize = 11.sp,
                            color = StatusRejectedRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Individual Document Approve / Reject / Drive buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onApprove,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StatusApprovedGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusApprovedGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onReject,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StatusRejectedRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        val driveUrl = doc.googleDriveWebViewLink.ifBlank { doc.fileUrl }
                        if (driveUrl.isNotBlank()) {
                            val context = LocalContext.current
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(driveUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open in Drive",
                                    tint = AdminAccentTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
