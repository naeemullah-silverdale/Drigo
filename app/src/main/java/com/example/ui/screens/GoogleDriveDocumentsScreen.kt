package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GoogleDriveFileRecord
import com.example.data.remote.GoogleDriveAuthHelper
import com.example.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Google Drive Theme Colors
private val DriveBlue = Color(0xFF1A73E8)
private val DriveGreen = Color(0xFF0F9D58)
private val DriveYellow = Color(0xFFF4B400)
private val DriveRed = Color(0xFFDB4437)
private val DriveDarkSurface = Color(0xFF1E293B)
private val DriveCardBg = Color(0xFF273549)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveDocumentsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDriveConnected by viewModel.isGoogleDriveConnected.collectAsState()
    val driveEmail by viewModel.googleDriveUserEmail.collectAsState()
    val driveName by viewModel.googleDriveUserName.collectAsState()
    val driveFiles by viewModel.googleDriveFiles.collectAsState()
    val isUploading by viewModel.isGoogleDriveUploading.collectAsState()
    val uploadProgress by viewModel.googleDriveUploadProgress.collectAsState()
    val statusMessage by viewModel.googleDriveStatusMessage.collectAsState()

    var selectedCategory by remember { mutableStateOf("identity") }
    var customDocNote by remember { mutableStateOf("") }
    var selectedFileForPreview by remember { mutableStateOf<GoogleDriveFileRecord?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<GoogleDriveFileRecord?>(null) }

    // Google Sign In Launcher with Drive.File Scope
    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.setGoogleDriveAccount(context, account)
                Toast.makeText(context, "Connected to Google Drive: ${account.email}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Google Drive connection failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = viewModel.uploadImageToDriveAndSaveToFirebase(
                    context = context,
                    imageUri = uri,
                    customFileName = "Drigo_${selectedCategory.uppercase()}_${System.currentTimeMillis()}.jpg",
                    docType = selectedCategory.uppercase(),
                    category = selectedCategory,
                    notes = customDocNote.ifBlank { "Uploaded via Drigo Google Drive Storage" }
                )
                if (res.isSuccess) {
                    Toast.makeText(context, "Uploaded to Google Drive & saved to Firebase!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAndInitGoogleDrive(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = DriveBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = DriveBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Drive Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Direct REST API v3 • Firebase Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("drive_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isDriveConnected) {
                        IconButton(
                            onClick = {
                                viewModel.disconnectGoogleDrive(context)
                                Toast.makeText(context, "Disconnected Google Drive", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Disconnect Drive",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Account Connection Status Card
            item {
                AccountStatusCard(
                    isConnected = isDriveConnected,
                    email = driveEmail,
                    displayName = driveName,
                    filesCount = driveFiles.size,
                    onConnectClick = {
                        val client = GoogleDriveAuthHelper.getGoogleSignInClient(context)
                        driveSignInLauncher.launch(client.signInIntent)
                    }
                )
            }

            // Upload New Image to Drive Card
            item {
                UploadToDriveCard(
                    isConnected = isDriveConnected,
                    isUploading = isUploading,
                    uploadProgress = uploadProgress,
                    statusMessage = statusMessage,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    onSelectImageClick = {
                        if (!isDriveConnected) {
                            val client = GoogleDriveAuthHelper.getGoogleSignInClient(context)
                            driveSignInLauncher.launch(client.signInIntent)
                        } else {
                            photoPickerLauncher.launch("image/*")
                        }
                    }
                )
            }

            // Stored Files in Firebase Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = DriveBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saved in Firebase (${driveFiles.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Real-time sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = DriveGreen
                    )
                }
            }

            if (driveFiles.isEmpty()) {
                item {
                    EmptyDriveFilesCard(
                        isConnected = isDriveConnected,
                        onUploadClick = {
                            if (!isDriveConnected) {
                                val client = GoogleDriveAuthHelper.getGoogleSignInClient(context)
                                driveSignInLauncher.launch(client.signInIntent)
                            } else {
                                photoPickerLauncher.launch("image/*")
                            }
                        }
                    )
                }
            } else {
                items(driveFiles, key = { it.fileId }) { fileRecord ->
                    DriveFileItemCard(
                        record = fileRecord,
                        onOpenLink = {
                            val targetUrl = fileRecord.webViewLink.ifBlank { fileRecord.directDownloadUrl }
                            if (targetUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeleteClick = {
                            showDeleteConfirmDialog = fileRecord
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteConfirmDialog?.let { record ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Document?") },
            text = {
                Text("This will remove '${record.fileName}' from Google Drive and delete its record from Firebase.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteGoogleDriveFile(context, record.fileId)
                            Toast.makeText(context, "Deleted from Google Drive and Firebase", Toast.LENGTH_SHORT).show()
                            showDeleteConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AccountStatusCard(
    isConnected: Boolean,
    email: String?,
    displayName: String?,
    filesCount: Int,
    onConnectClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isConnected) DriveGreen.copy(alpha = 0.15f) else DriveYellow.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = if (isConnected) DriveGreen else DriveYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isConnected) "Google Drive Connected" else "Google Drive Not Linked",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConnected) (email ?: "Ready for uploads") else "Requires drive.file OAuth permission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isConnected) {
                    Button(
                        onClick = onConnectClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DriveBlue,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Connect", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Folder: Drigo_Rideshare_Documents",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$filesCount files saved",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DriveBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadToDriveCard(
    isConnected: Boolean,
    isUploading: Boolean,
    uploadProgress: Float,
    statusMessage: String?,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onSelectImageClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Upload Image to Google Drive",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Images are uploaded directly to Drive and URLs are stored in Firebase",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selector Chips
            Text(
                text = "Document Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf(
                    "identity" to "ID Card",
                    "license" to "License",
                    "vehicle" to "Vehicle",
                    "profile" to "Profile"
                )
                categories.forEach { (catKey, catLabel) ->
                    val isSelected = selectedCategory == catKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryChange(catKey) },
                        label = { Text(catLabel, fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isUploading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = DriveBlue,
                        trackColor = DriveBlue.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Uploading: ${(uploadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!statusMessage.isNullOrBlank()) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Button(
                    onClick = onSelectImageClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DriveBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Select & Upload Image" else "Connect Drive & Upload",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveFileItemCard(
    record: GoogleDriveFileRecord,
    onOpenLink: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateStr = remember(record.uploadedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(record.uploadedAt))
    }

    val sizeStr = remember(record.fileSize) {
        when {
            record.fileSize > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", record.fileSize / (1024.0 * 1024.0))
            record.fileSize > 1024 -> "${record.fileSize / 1024} KB"
            else -> "${record.fileSize} B"
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                if (record.thumbnailLink.isNotBlank() || record.directDownloadUrl.isNotBlank()) {
                    AsyncImage(
                        model = record.thumbnailLink.ifBlank { record.directDownloadUrl },
                        contentDescription = record.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = DriveBlue,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DriveBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = record.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = DriveBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sizeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = record.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions
            Row {
                IconButton(
                    onClick = onOpenLink,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in Google Drive",
                        tint = DriveBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete File",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDriveFilesCard(
    isConnected: Boolean,
    onUploadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = DriveBlue.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = DriveBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No Google Drive Files Yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Upload driver verification documents or receipts to Google Drive. Their URLs will automatically sync with Firebase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onUploadClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isConnected) "Upload First Image" else "Connect Google Drive")
            }
        }
    }
}
