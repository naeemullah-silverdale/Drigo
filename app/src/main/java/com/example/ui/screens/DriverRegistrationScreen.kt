package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.DriverDocumentItem
import com.example.data.model.DriverVerification
import com.example.data.remote.DriverDocumentStorageManager
import com.example.data.remote.FirebaseRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

// Magenta Brand Colors
val DriverMagentaBg = Color(0xFF8B004F)
val DriverBrightFuchsia = Color(0xFFFF00CC)
val DriverDarkFuchsia = Color(0xFF6B003D)
val DriverCardBg = Color(0x33FFFFFF)

enum class DriverRegStep {
    PROFILE_AND_IDENTITY,
    VEHICLE_DETAILS,
    DRIVING_LICENSE,
    CONFIRMATION_PENDING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRegistrationScreen(
    user: FirebaseUser?,
    existingVerification: DriverVerification? = null,
    onBackToPassenger: () -> Unit,
    onVerificationCompleted: (DriverVerification) -> Unit,
    onConfirmedAndSwitchToDriver: () -> Unit,
    onNavigateToAdminPortal: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentStep by remember {
        mutableStateOf(
            if (existingVerification != null && (existingVerification.status == "PENDING" || existingVerification.status == "UNDER_REVIEW" || existingVerification.status == "REJECTED" || existingVerification.confirmtion)) {
                DriverRegStep.CONFIRMATION_PENDING
            } else {
                DriverRegStep.PROFILE_AND_IDENTITY
            }
        )
    }

    // Step 1: Profile & Identity
    var driverPhotoDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_DRIVER_PHOTO }
                ?: if (existingVerification?.driverPhotoUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_DRIVER_PHOTO,
                        title = "Driver Profile Photo",
                        category = "profile",
                        storagePath = "drivers/${existingVerification.uid}/profile/driver_profile_photo.jpg",
                        fileUrl = existingVerification.driverPhotoUri,
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }
    var cnicFrontDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_CNIC_FRONT }
                ?: if (existingVerification?.cnicFrontUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_CNIC_FRONT,
                        title = "CNIC / National ID (Front)",
                        category = "identity",
                        storagePath = "drivers/${existingVerification.uid}/identity/cnic_front.jpg",
                        fileUrl = existingVerification.cnicFrontUri,
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }
    var cnicBackDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_CNIC_BACK }
                ?: if (existingVerification?.cnicBackUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_CNIC_BACK,
                        title = "CNIC / National ID (Back)",
                        category = "identity",
                        storagePath = "drivers/${existingVerification.uid}/identity/cnic_back.jpg",
                        fileUrl = existingVerification.cnicBackUri,
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }

    // Step 2: Vehicle Details & Multi-Angle Photos
    var vehicleFrontDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_VEHICLE_FRONT }
                ?: if (existingVerification?.vehicleFrontUri?.isNotBlank() == true || existingVerification?.vehiclePictureUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_VEHICLE_FRONT,
                        title = "Vehicle (Front View)",
                        category = "vehicle",
                        storagePath = "drivers/${existingVerification.uid}/vehicle/vehicle_front.jpg",
                        fileUrl = existingVerification.vehicleFrontUri.ifBlank { existingVerification.vehiclePictureUri },
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }
    var vehicleBackDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_VEHICLE_BACK }
                ?: if (existingVerification?.vehicleBackUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_VEHICLE_BACK,
                        title = "Vehicle (Back View)",
                        category = "vehicle",
                        storagePath = "drivers/${existingVerification.uid}/vehicle/vehicle_back.jpg",
                        fileUrl = existingVerification.vehicleBackUri,
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }
    var vehicleSideDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_VEHICLE_SIDE }
                ?: if (existingVerification?.vehicleSideUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_VEHICLE_SIDE,
                        title = "Vehicle (Side View)",
                        category = "vehicle",
                        storagePath = "drivers/${existingVerification.uid}/vehicle/vehicle_side.jpg",
                        fileUrl = existingVerification.vehicleSideUri,
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }
    var vehicleRegDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_VEHICLE_REGISTRATION }
                ?: if (existingVerification?.vehicleRegistrationDocUri?.isNotBlank() == true || existingVerification?.vehicleCardDocFrontUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_VEHICLE_REGISTRATION,
                        title = "Vehicle Registration Document",
                        category = "documents",
                        storagePath = "drivers/${existingVerification.uid}/documents/vehicle_registration.jpg",
                        fileUrl = existingVerification.vehicleRegistrationDocUri.ifBlank { existingVerification.vehicleCardDocFrontUri },
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }
    var vehicleCompany by remember { mutableStateOf(existingVerification?.vehicleCompany ?: "") }
    var vehicleModel by remember { mutableStateOf(existingVerification?.vehicleModel ?: "") }
    var vehicleNumber by remember { mutableStateOf(existingVerification?.vehicleNumber ?: "") }

    // Step 3: Driving License & Additional Doc
    var licenseFrontDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_LICENSE_FRONT }
                ?: if (existingVerification?.drivingLicenseFrontUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_LICENSE_FRONT,
                        title = "Driving License (Front)",
                        category = "license",
                        storagePath = "drivers/${existingVerification.uid}/license/license_front.jpg",
                        fileUrl = existingVerification.drivingLicenseFrontUri,
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }
    var licenseBackDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_LICENSE_BACK }
                ?: if (existingVerification?.drivingLicenseBackUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_LICENSE_BACK,
                        title = "Driving License (Back)",
                        category = "license",
                        storagePath = "drivers/${existingVerification.uid}/license/license_back.jpg",
                        fileUrl = existingVerification.drivingLicenseBackUri,
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }
    var additionalDoc by remember {
        mutableStateOf(
            existingVerification?.documents?.find { it.docType == DriverDocumentStorageManager.DOC_ADDITIONAL_DOC }
                ?: if (existingVerification?.additionalDocUri?.isNotBlank() == true) {
                    DriverDocumentItem(
                        docType = DriverDocumentStorageManager.DOC_ADDITIONAL_DOC,
                        title = "Additional Verification Document",
                        category = "documents",
                        storagePath = "drivers/${existingVerification.uid}/documents/additional_doc.jpg",
                        fileUrl = existingVerification.additionalDocUri,
                        isRequired = false,
                        driverId = existingVerification.uid
                    )
                } else null
        )
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var verificationState by remember { mutableStateOf(existingVerification) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf<String?>(null) }

    // Listen to real-time verification status for current user
    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        val repo = FirebaseRepository.getInstance(context)
        repo.listenToDriverVerification(uid).collect { ver ->
            if (ver != null) {
                verificationState = ver
                if (ver.confirmtion) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Your driver profile has been approved!")
                    }
                }
            }
        }
    }

    // Required documents check (9 required: Profile, CNIC Front, CNIC Back, Vehicle Front, Vehicle Back, Vehicle Side, Registration Doc, License Front, License Back)
    val requiredUploadedCount = listOfNotNull(
        driverPhotoDoc,
        cnicFrontDoc,
        cnicBackDoc,
        vehicleFrontDoc,
        vehicleBackDoc,
        vehicleSideDoc,
        vehicleRegDoc,
        licenseFrontDoc,
        licenseBackDoc
    ).count { it.fileUrl.isNotBlank() }

    val allRequiredCompleted = requiredUploadedCount >= 9

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DriverMagentaBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DriverMagentaBg)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            when (currentStep) {
                                DriverRegStep.PROFILE_AND_IDENTITY -> onBackToPassenger()
                                DriverRegStep.VEHICLE_DETAILS -> currentStep = DriverRegStep.PROFILE_AND_IDENTITY
                                DriverRegStep.DRIVING_LICENSE -> currentStep = DriverRegStep.VEHICLE_DETAILS
                                DriverRegStep.CONFIRMATION_PENDING -> onBackToPassenger()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    if (currentStep != DriverRegStep.CONFIRMATION_PENDING) {
                        val stepIndex = when (currentStep) {
                            DriverRegStep.PROFILE_AND_IDENTITY -> 1
                            DriverRegStep.VEHICLE_DETAILS -> 2
                            DriverRegStep.DRIVING_LICENSE -> 3
                            DriverRegStep.CONFIRMATION_PENDING -> 3
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Step $stepIndex of 3",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$requiredUploadedCount/9 Required Uploaded",
                                fontSize = 11.sp,
                                color = if (allRequiredCompleted) Color(0xFF00E676) else Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = "Verification Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "DriverRegistrationStepAnimation"
                ) { step ->
                    when (step) {
                        DriverRegStep.PROFILE_AND_IDENTITY -> {
                            ProfileAndIdentityStep(
                                userId = user?.uid ?: "driver_guest",
                                driverPhotoDoc = driverPhotoDoc,
                                cnicFrontDoc = cnicFrontDoc,
                                cnicBackDoc = cnicBackDoc,
                                onDriverPhotoChange = { driverPhotoDoc = it },
                                onCnicFrontChange = { cnicFrontDoc = it },
                                onCnicBackChange = { cnicBackDoc = it },
                                onPreview = { url, title ->
                                    previewImageUrl = url
                                    previewImageTitle = title
                                },
                                onNext = {
                                    currentStep = DriverRegStep.VEHICLE_DETAILS
                                }
                            )
                        }

                        DriverRegStep.VEHICLE_DETAILS -> {
                            VehicleDetailsStep(
                                userId = user?.uid ?: "driver_guest",
                                vehicleFrontDoc = vehicleFrontDoc,
                                vehicleBackDoc = vehicleBackDoc,
                                vehicleSideDoc = vehicleSideDoc,
                                vehicleRegDoc = vehicleRegDoc,
                                vehicleCompany = vehicleCompany,
                                vehicleModel = vehicleModel,
                                vehicleNumber = vehicleNumber,
                                onVehicleFrontChange = { vehicleFrontDoc = it },
                                onVehicleBackChange = { vehicleBackDoc = it },
                                onVehicleSideChange = { vehicleSideDoc = it },
                                onVehicleRegDocChange = { vehicleRegDoc = it },
                                onVehicleCompanyChange = { vehicleCompany = it },
                                onVehicleModelChange = { vehicleModel = it },
                                onVehicleNumberChange = { vehicleNumber = it },
                                onPreview = { url, title ->
                                    previewImageUrl = url
                                    previewImageTitle = title
                                },
                                onNext = {
                                    if (vehicleCompany.isBlank()) vehicleCompany = "Toyota"
                                    if (vehicleModel.isBlank()) vehicleModel = "Corolla"
                                    if (vehicleNumber.isBlank()) vehicleNumber = "LEA-4521"
                                    currentStep = DriverRegStep.DRIVING_LICENSE
                                }
                            )
                        }

                        DriverRegStep.DRIVING_LICENSE -> {
                            DrivingLicenseStep(
                                userId = user?.uid ?: "driver_guest",
                                licenseFrontDoc = licenseFrontDoc,
                                licenseBackDoc = licenseBackDoc,
                                additionalDoc = additionalDoc,
                                onLicenseFrontChange = { licenseFrontDoc = it },
                                onLicenseBackChange = { licenseBackDoc = it },
                                onAdditionalDocChange = { additionalDoc = it },
                                onPreview = { url, title ->
                                    previewImageUrl = url
                                    previewImageTitle = title
                                },
                                isSubmitting = isSubmitting,
                                allRequiredCompleted = allRequiredCompleted,
                                requiredUploadedCount = requiredUploadedCount,
                                onSubmit = {
                                    if (!isSubmitting) {
                                        // Verify all 9 required documents are present and uploaded
                                        val requiredDocs = listOfNotNull(
                                            driverPhotoDoc,
                                            cnicFrontDoc,
                                            cnicBackDoc,
                                            vehicleFrontDoc,
                                            vehicleBackDoc,
                                            vehicleSideDoc,
                                            vehicleRegDoc,
                                            licenseFrontDoc,
                                            licenseBackDoc
                                        )

                                        val incompleteDocs = requiredDocs.filter { it.fileUrl.isBlank() }
                                        if (incompleteDocs.isNotEmpty() || requiredDocs.size < 9) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Please upload all 9 required verification documents before submitting ($requiredUploadedCount/9 completed).")
                                            }
                                        } else {
                                            val safeUid = user?.uid ?: "driver_${System.currentTimeMillis()}"

                                            // Collect all documents
                                            val allDocList = listOfNotNull(
                                                driverPhotoDoc,
                                                cnicFrontDoc,
                                                cnicBackDoc,
                                                vehicleFrontDoc,
                                                vehicleBackDoc,
                                                vehicleSideDoc,
                                                vehicleRegDoc,
                                                licenseFrontDoc,
                                                licenseBackDoc,
                                                additionalDoc
                                            )

                                             val verification = DriverVerification(
                                                uid = safeUid,
                                                name = user?.displayName?.ifBlank { "Drigo Driver" }
                                                    ?: (user?.email?.substringBefore("@") ?: "Drigo Driver"),
                                                email = user?.email ?: "",
                                                phone = user?.phoneNumber ?: "+92 300 1234567",
                                                driverPhotoUri = driverPhotoDoc?.fileUrl.orEmpty(),
                                                cnicFrontUri = cnicFrontDoc?.fileUrl.orEmpty(),
                                                cnicBackUri = cnicBackDoc?.fileUrl.orEmpty(),
                                                vehiclePictureUri = vehicleFrontDoc?.fileUrl.orEmpty(),
                                                vehicleFrontUri = vehicleFrontDoc?.fileUrl.orEmpty(),
                                                vehicleBackUri = vehicleBackDoc?.fileUrl.orEmpty(),
                                                vehicleSideUri = vehicleSideDoc?.fileUrl.orEmpty(),
                                                vehicleCardDocFrontUri = vehicleRegDoc?.fileUrl.orEmpty(),
                                                vehicleCardDocBackUri = vehicleRegDoc?.fileUrl.orEmpty(),
                                                vehicleRegistrationDocUri = vehicleRegDoc?.fileUrl.orEmpty(),
                                                vehicleCompany = vehicleCompany.ifBlank { "Toyota" },
                                                vehicleModel = vehicleModel.ifBlank { "Corolla" },
                                                vehicleNumber = vehicleNumber.ifBlank { "LEA-4521" },
                                                drivingLicenseFrontUri = licenseFrontDoc?.fileUrl.orEmpty(),
                                                drivingLicenseBackUri = licenseBackDoc?.fileUrl.orEmpty(),
                                                additionalDocUri = additionalDoc?.fileUrl.orEmpty(),
                                                documents = allDocList,
                                                confirmtion = false,
                                                status = "PENDING",
                                                accountStatus = "PENDING_REVIEW",
                                                verificationStatus = "PENDING",
                                                isVerified = false,
                                                isOnline = false,
                                                submittedAt = System.currentTimeMillis(),
                                                reviewNotes = "Your verification request is under review. Our compliance team is verifying your CNIC, license, and vehicle registration."
                                            )

                                            isSubmitting = true
                                            scope.launch {
                                                try {
                                                    val repo = FirebaseRepository.getInstance(context)
                                                    val res = repo.saveDriverVerification(verification)
                                                    isSubmitting = false
                                                    if (res.isSuccess) {
                                                        verificationState = verification
                                                        onVerificationCompleted(verification)
                                                        currentStep = DriverRegStep.CONFIRMATION_PENDING
                                                        snackbarHostState.showSnackbar("Application submitted successfully! Your documents are under review.")
                                                    } else {
                                                        val err = res.exceptionOrNull()?.message ?: "Save failed"
                                                        snackbarHostState.showSnackbar("Failed to submit: $err. Please try again.")
                                                    }
                                                } catch (e: Exception) {
                                                    isSubmitting = false
                                                    snackbarHostState.showSnackbar("Error during submission: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        DriverRegStep.CONFIRMATION_PENDING -> {
                            val currentVer = verificationState ?: existingVerification ?: DriverVerification(
                                confirmtion = false,
                                vehicleCompany = vehicleCompany,
                                vehicleModel = vehicleModel,
                                vehicleNumber = vehicleNumber
                            )

                            ConfirmationPendingStep(
                                verification = currentVer,
                                onBackToPassenger = onBackToPassenger,
                                onSimulateApproval = {
                                    val uid = user?.uid ?: currentVer.uid
                                    scope.launch {
                                        val repo = FirebaseRepository.getInstance(context)
                                        val approvedDocs = currentVer.documents.map { it.copy(status = "APPROVED") }
                                        val newVer = currentVer.copy(
                                            confirmtion = true,
                                            status = "APPROVED",
                                            documents = approvedDocs,
                                            reviewNotes = "All driver documents verified and approved."
                                        )
                                        repo.saveDriverVerification(newVer)
                                        repo.updateDriverConfirmation(uid, true)
                                        verificationState = newVer
                                        snackbarHostState.showSnackbar("Profile approved! You can now switch to Driver Mode.")
                                    }
                                },
                                onSwitchToDriver = onConfirmedAndSwitchToDriver,
                                onOpenAdminPortal = onNavigateToAdminPortal,
                                onReuploadRejectedDocs = {
                                    currentStep = DriverRegStep.PROFILE_AND_IDENTITY
                                }
                            )
                        }
                    }
                }
            }

            // High Resolution Image Preview Dialog
            if (previewImageUrl != null) {
                Dialog(onDismissRequest = { previewImageUrl = null }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
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
                                    text = previewImageTitle ?: "Document Preview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(onClick = { previewImageUrl = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
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
                                text = "High-Resolution Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 1: Driver Profile Photo + CNIC Front & Back
 */
@Composable
fun ProfileAndIdentityStep(
    userId: String,
    driverPhotoDoc: DriverDocumentItem?,
    cnicFrontDoc: DriverDocumentItem?,
    cnicBackDoc: DriverDocumentItem?,
    onDriverPhotoChange: (DriverDocumentItem?) -> Unit,
    onCnicFrontChange: (DriverDocumentItem?) -> Unit,
    onCnicBackChange: (DriverDocumentItem?) -> Unit,
    onPreview: (String, String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Personal Details & Identity",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Upload your clear profile photograph and CNIC / National ID card",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Driver Profile Photo (Centered Circle / Box)
        Text(
            text = "Driver Profile Photo *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(modifier = Modifier.width(180.dp)) {
            SecureDocumentUploadCard(
                userId = userId,
                docType = DriverDocumentStorageManager.DOC_DRIVER_PHOTO,
                label = "Driver Face Photo",
                docItem = driverPhotoDoc,
                onDocUploaded = onDriverPhotoChange,
                onPreview = onPreview
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CNIC Front & Back in 2 columns
        Text(
            text = "National Identity Card (CNIC) *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SecureDocumentUploadCard(
                    userId = userId,
                    docType = DriverDocumentStorageManager.DOC_CNIC_FRONT,
                    label = "CNIC Front",
                    docItem = cnicFrontDoc,
                    onDocUploaded = onCnicFrontChange,
                    onPreview = onPreview
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SecureDocumentUploadCard(
                    userId = userId,
                    docType = DriverDocumentStorageManager.DOC_CNIC_BACK,
                    label = "CNIC Back",
                    docItem = cnicBackDoc,
                    onDocUploaded = onCnicBackChange,
                    onPreview = onPreview
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DriverBrightFuchsia,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = "NEXT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Step 2: Vehicle Details & 4 Document Cards:
 * - Vehicle Front
 * - Vehicle Back
 * - Vehicle Side
 * - Vehicle Registration Card / Book
 */
@Composable
fun VehicleDetailsStep(
    userId: String,
    vehicleFrontDoc: DriverDocumentItem?,
    vehicleBackDoc: DriverDocumentItem?,
    vehicleSideDoc: DriverDocumentItem?,
    vehicleRegDoc: DriverDocumentItem?,
    vehicleCompany: String,
    vehicleModel: String,
    vehicleNumber: String,
    onVehicleFrontChange: (DriverDocumentItem?) -> Unit,
    onVehicleBackChange: (DriverDocumentItem?) -> Unit,
    onVehicleSideChange: (DriverDocumentItem?) -> Unit,
    onVehicleRegDocChange: (DriverDocumentItem?) -> Unit,
    onVehicleCompanyChange: (String) -> Unit,
    onVehicleModelChange: (String) -> Unit,
    onVehicleNumberChange: (String) -> Unit,
    onPreview: (String, String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Vehicle Details & Photos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Upload multi-angle photos of your vehicle and registration documents",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        // 2x2 Grid for Vehicle Multi-Angle Photos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SecureDocumentUploadCard(
                    userId = userId,
                    docType = DriverDocumentStorageManager.DOC_VEHICLE_FRONT,
                    label = "Car Front View",
                    docItem = vehicleFrontDoc,
                    onDocUploaded = onVehicleFrontChange,
                    onPreview = onPreview,
                    compact = true
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SecureDocumentUploadCard(
                    userId = userId,
                    docType = DriverDocumentStorageManager.DOC_VEHICLE_BACK,
                    label = "Car Back View",
                    docItem = vehicleBackDoc,
                    onDocUploaded = onVehicleBackChange,
                    onPreview = onPreview,
                    compact = true
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SecureDocumentUploadCard(
                    userId = userId,
                    docType = DriverDocumentStorageManager.DOC_VEHICLE_SIDE,
                    label = "Car Side View",
                    docItem = vehicleSideDoc,
                    onDocUploaded = onVehicleSideChange,
                    onPreview = onPreview,
                    compact = true
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SecureDocumentUploadCard(
                    userId = userId,
                    docType = DriverDocumentStorageManager.DOC_VEHICLE_REGISTRATION,
                    label = "Vehicle Registration",
                    docItem = vehicleRegDoc,
                    onDocUploaded = onVehicleRegDocChange,
                    onPreview = onPreview,
                    compact = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Vehicle Text Fields (Rounded Stadium Shape)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PillInputField(
                value = vehicleCompany,
                placeholder = "Vehicle Company (e.g. Toyota, Honda, Suzuki)",
                onValueChange = onVehicleCompanyChange,
                imeAction = ImeAction.Next
            )
            PillInputField(
                value = vehicleModel,
                placeholder = "Vehicle Model (e.g. Corolla, Civic, Alto)",
                onValueChange = onVehicleModelChange,
                imeAction = ImeAction.Next
            )
            PillInputField(
                value = vehicleNumber,
                placeholder = "Vehicle License Plate (e.g. LEA-4521)",
                onValueChange = onVehicleNumberChange,
                imeAction = ImeAction.Done
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DriverBrightFuchsia,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = "NEXT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Step 3: Driving License (Front + Back) & Optional Additional Document
 */
@Composable
fun DrivingLicenseStep(
    userId: String,
    licenseFrontDoc: DriverDocumentItem?,
    licenseBackDoc: DriverDocumentItem?,
    additionalDoc: DriverDocumentItem?,
    onLicenseFrontChange: (DriverDocumentItem?) -> Unit,
    onLicenseBackChange: (DriverDocumentItem?) -> Unit,
    onAdditionalDocChange: (DriverDocumentItem?) -> Unit,
    onPreview: (String, String) -> Unit,
    isSubmitting: Boolean,
    allRequiredCompleted: Boolean,
    requiredUploadedCount: Int,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Driving License & Documents",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Upload valid driving license and any supporting credentials",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // License Front & Back in 2 columns
        Text(
            text = "Driving License *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SecureDocumentUploadCard(
                    userId = userId,
                    docType = DriverDocumentStorageManager.DOC_LICENSE_FRONT,
                    label = "License Front",
                    docItem = licenseFrontDoc,
                    onDocUploaded = onLicenseFrontChange,
                    onPreview = onPreview
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SecureDocumentUploadCard(
                    userId = userId,
                    docType = DriverDocumentStorageManager.DOC_LICENSE_BACK,
                    label = "License Back",
                    docItem = licenseBackDoc,
                    onDocUploaded = onLicenseBackChange,
                    onPreview = onPreview
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Additional / Optional Document
        Text(
            text = "Additional Document (Optional)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(modifier = Modifier.width(180.dp)) {
            SecureDocumentUploadCard(
                userId = userId,
                docType = DriverDocumentStorageManager.DOC_ADDITIONAL_DOC,
                label = "Additional Doc",
                docItem = additionalDoc,
                onDocUploaded = onAdditionalDocChange,
                onPreview = onPreview,
                compact = true
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Submission Requirement Checklist Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (allRequiredCompleted) Color(0xFF00E676).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (allRequiredCompleted) Color(0xFF00E676) else Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (allRequiredCompleted) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (allRequiredCompleted) Color(0xFF00E676) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = if (allRequiredCompleted) "All Required Documents Ready" else "Required Documents Incomplete ($requiredUploadedCount/9)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (allRequiredCompleted)
                            "You can now submit for 24-hour verification review."
                        else
                            "Please upload all 9 required verification photos before submission.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DriverBrightFuchsia,
                    contentColor = Color.White,
                    disabledContainerColor = DriverBrightFuchsia.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUBMITTING...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Text(
                        text = "SUBMIT FOR REVIEW",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Confirmation Pending Screen (24 Hours Review Message + Real-time status display)
 */
@Composable
fun ConfirmationPendingStep(
    verification: DriverVerification,
    onBackToPassenger: () -> Unit,
    onSimulateApproval: () -> Unit,
    onSwitchToDriver: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    onReuploadRejectedDocs: () -> Unit
) {
    val docs = remember(verification) {
        if (verification.documents.isNotEmpty()) {
            verification.documents
        } else {
            val list = mutableListOf<DriverDocumentItem>()
            if (verification.driverPhotoUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_DRIVER_PHOTO, title = "Driver Profile Photo", category = "profile", fileUrl = verification.driverPhotoUri, isRequired = true, status = verification.status))
            if (verification.cnicFrontUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_CNIC_FRONT, title = "CNIC Front", category = "identity", fileUrl = verification.cnicFrontUri, isRequired = true, status = verification.status))
            if (verification.cnicBackUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_CNIC_BACK, title = "CNIC Back", category = "identity", fileUrl = verification.cnicBackUri, isRequired = true, status = verification.status))
            if (verification.vehicleFrontUri.isNotBlank() || verification.vehiclePictureUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_VEHICLE_FRONT, title = "Vehicle Front", category = "vehicle", fileUrl = verification.vehicleFrontUri.ifBlank { verification.vehiclePictureUri }, isRequired = true, status = verification.status))
            if (verification.vehicleBackUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_VEHICLE_BACK, title = "Vehicle Back", category = "vehicle", fileUrl = verification.vehicleBackUri, isRequired = true, status = verification.status))
            if (verification.vehicleSideUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_VEHICLE_SIDE, title = "Vehicle Side", category = "vehicle", fileUrl = verification.vehicleSideUri, isRequired = true, status = verification.status))
            if (verification.vehicleRegistrationDocUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_VEHICLE_REGISTRATION, title = "Vehicle Registration", category = "documents", fileUrl = verification.vehicleRegistrationDocUri, isRequired = true, status = verification.status))
            if (verification.drivingLicenseFrontUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_LICENSE_FRONT, title = "Driving License Front", category = "license", fileUrl = verification.drivingLicenseFrontUri, isRequired = true, status = verification.status))
            if (verification.drivingLicenseBackUri.isNotBlank()) list.add(DriverDocumentItem(docType = DriverDocumentStorageManager.DOC_LICENSE_BACK, title = "Driving License Back", category = "license", fileUrl = verification.drivingLicenseBackUri, isRequired = true, status = verification.status))
            list
        }
    }

    val reqDocs = docs.filter { it.isRequired }
    val pendingCount = if (reqDocs.isNotEmpty()) reqDocs.count { it.status == "PENDING" || it.status == "UNDER_REVIEW" } else 0
    val rejectedDocs = if (reqDocs.isNotEmpty()) reqDocs.filter { it.status == "REJECTED" } else emptyList()
    val isAllApproved = (verification.verificationStatus == "APPROVED" || verification.accountStatus == "ACTIVE" || verification.accountStatus == "ONLINE" || verification.accountStatus == "ON_TRIP" || verification.isVerified || verification.confirmtion) && rejectedDocs.isEmpty()
    val isRejected = verification.verificationStatus == "REJECTED" || verification.accountStatus == "SUSPENDED" || rejectedDocs.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isAllApproved -> Color(0xFF00E676).copy(alpha = 0.2f)
                isRejected -> Color(0xFFEF4444).copy(alpha = 0.2f)
                else -> Color.White.copy(alpha = 0.15f)
            },
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isAllApproved -> Icons.Default.CheckCircle
                        isRejected -> Icons.Default.Cancel
                        else -> Icons.Default.HourglassTop
                    },
                    contentDescription = null,
                    tint = when {
                        isAllApproved -> Color(0xFF00E676)
                        isRejected -> Color(0xFFEF4444)
                        else -> Color.White
                    },
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                isAllApproved -> "Driver Profile Approved!"
                isRejected -> "Action Required"
                verification.status == "UNDER_REVIEW" -> "Review In Progress"
                else -> "Verification Submitted"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        isAllApproved ->
                            "Congratulations! Your registration has been verified by admin."
                        isRejected -> {
                            val mainReason = verification.rejectionReason.ifBlank { "One or more documents were rejected by admin." }
                            "Action Required: $mainReason" + if (rejectedDocs.isNotEmpty()) {
                                "\n\n" + rejectedDocs.joinToString("\n") { "• ${it.title}: ${it.rejectionReason.ifBlank { "Invalid or unclear document" }}" }
                            } else ""
                        }
                        else ->
                            "Your documents are currently under review by our compliance team."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val statusLabel = when {
                    isAllApproved -> "STATUS: APPROVED"
                    isRejected -> "STATUS: ACTION REQUIRED (${rejectedDocs.size} REJECTED)"
                    else -> "STATUS: KYC DOCS ($pendingCount PENDING)"
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isAllApproved -> Color(0xFF00E676).copy(alpha = 0.25f)
                        isRejected -> Color(0xFFEF4444).copy(alpha = 0.25f)
                        else -> Color(0xFFFFB300).copy(alpha = 0.25f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isAllApproved -> Color(0xFF00E676)
                            isRejected -> Color(0xFFEF4444)
                            else -> Color(0xFFFFB300)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isAllApproved -> Icons.Default.Check
                                isRejected -> Icons.Default.Close
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = null,
                            tint = when {
                                isAllApproved -> Color(0xFF00E676)
                                isRejected -> Color(0xFFEF4444)
                                else -> Color(0xFFFFB300)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Individual KYC Document Status List
        if (docs.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Document Verification Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    docs.forEach { doc ->
                        val docStatusColor = when (doc.status) {
                            "APPROVED" -> Color(0xFF00E676)
                            "REJECTED" -> Color(0xFFEF4444)
                            else -> Color(0xFFFFB300)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                if (doc.rejectionReason.isNotBlank()) {
                                    Text(
                                        text = "Reason: ${doc.rejectionReason}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = docStatusColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, docStatusColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = doc.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = docStatusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (isAllApproved) {
            Button(
                onClick = onSwitchToDriver,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DriverBrightFuchsia,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "SWITCH TO DRIVER MODE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else if (isRejected) {
            Button(
                onClick = onReuploadRejectedDocs,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DriverBrightFuchsia,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RE-UPLOAD REJECTED DOCUMENTS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        } else {
            Button(
                onClick = onBackToPassenger,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DriverMagentaBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Return to Passenger Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Test Helper: Simulate Instant Confirmation
            OutlinedButton(
                onClick = onSimulateApproval,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simulate Instant Approval (Test)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Direct Access to Admin Verification Portal for reviewer inspection
        TextButton(
            onClick = onOpenAdminPortal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Open Admin Verification Portal",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Secure Document Upload Card with:
 * - Direct file picker
 * - Compression and Cloud Storage upload
 * - Real-time progress bar
 * - Thumbnail preview
 * - Replace and View actions
 */
@Composable
fun SecureDocumentUploadCard(
    userId: String,
    docType: String,
    label: String,
    docItem: DriverDocumentItem?,
    onDocUploaded: (DriverDocumentItem?) -> Unit,
    onPreview: (String, String) -> Unit,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            uploadProgress = 0.05f
            uploadError = null
            scope.launch {
                val driveAccount = com.example.data.remote.GoogleDriveAuthHelper.getLastSignedInAccount(context)
                val driveManager = com.example.data.remote.GoogleDriveStorageManager(context).apply {
                    if (driveAccount != null && com.example.data.remote.GoogleDriveAuthHelper.hasDrivePermissions(context, driveAccount)) {
                        init(context, driveAccount)
                    }
                }

                val res = DriverDocumentStorageManager.uploadDocument(
                    context = context,
                    driverId = userId,
                    docType = docType,
                    uri = uri,
                    googleDriveManager = driveManager,
                    oldDocItem = docItem,
                    onProgress = { uploadProgress = it }
                )
                isUploading = false
                if (res.isSuccess) {
                    val uploadedDoc = res.getOrNull()
                    onDocUploaded(uploadedDoc)
                    if (uploadedDoc != null) {
                        try {
                            com.example.data.remote.FirebaseRepository.getInstance(context)
                                .saveDriverDocumentMetadata(userId, uploadedDoc)
                        } catch (_: Exception) {}
                    }
                } else {
                    uploadError = res.exceptionOrNull()?.message ?: "Upload failed"
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(if (compact) 14.dp else 18.dp))
                .background(DriverDarkFuchsia)
                .border(
                    width = 2.dp,
                    color = if (docItem?.fileUrl?.isNotBlank() == true) Color(0xFF00E676) else Color.White,
                    shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
                )
                .clickable {
                    launcher.launch("image/*")
                },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.size(36.dp),
                        color = DriverBrightFuchsia,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(uploadProgress * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (docItem != null && docItem.fileUrl.isNotBlank()) {
                AsyncImage(
                    model = docItem.fileUrl,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Top-right success check badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(20.dp)
                        .background(Color(0xFF00E676), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Uploaded",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                // Bottom preview / zoom button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .clickable {
                            onPreview(docItem.fileUrl, label)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Preview",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add $label",
                        tint = Color.White,
                        modifier = Modifier.size(if (compact) 32.dp else 42.dp)
                    )
                    Text(
                        text = "Tap to upload",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (uploadError != null) {
            Text(
                text = uploadError ?: "",
                fontSize = 9.sp,
                color = Color(0xFFFF5252),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Reusable Pill Input Field:
 * - Rounded stadium shape
 * - White outline border
 * - White placeholder & text
 */
@Composable
fun PillInputField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = imeAction
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}
