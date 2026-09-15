package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.data.model.DriverDocumentItem
import com.example.data.model.DriverVehicle
import com.example.data.model.DriverVerification
import com.example.data.model.VehicleCatalog
import com.example.data.remote.DriverDocumentStorageManager
import com.example.data.remote.FirebaseRepository
import com.example.ui.theme.DrigoBrandFuchsia
import com.example.ui.theme.DrigoBrandMagentaBg
import com.example.ui.theme.DrigoBrandPurple
import com.example.ui.theme.InDriveLimeGreen
import com.example.ui.theme.drigoColors
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

enum class DriverRegStep {
    PROFILE_AND_IDENTITY,
    VEHICLE_DETAILS,
    DRIVING_LICENSE,
    CONFIRMATION_PENDING
}

/**
 * Redesigned Driver Registration & KYC Screen.
 * Provides a modern, guided 3-step onboarding experience with responsive document upload cards,
 * clear progress indicators, helpful document camera instructions, and real-time status updates.
 */
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
    val isDark = isSystemInDarkTheme() || MaterialTheme.drigoColors.isDark

    // Theme adaptive background colors
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2E001A),
                Color(0xFF1F0012),
                Color(0xFF14000B)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF70003E),
                Color(0xFF4D002A),
                Color(0xFF2D0018)
            )
        )
    }

    var currentStep by remember {
        mutableStateOf(
            if (existingVerification != null && (existingVerification.status == "PENDING" || existingVerification.status == "UNDER_REVIEW" || existingVerification.status == "REJECTED" || existingVerification.confirmtion)) {
                DriverRegStep.CONFIRMATION_PENDING
            } else {
                DriverRegStep.PROFILE_AND_IDENTITY
            }
        )
    }

    BackHandler {
        when (currentStep) {
            DriverRegStep.PROFILE_AND_IDENTITY -> onBackToPassenger()
            DriverRegStep.VEHICLE_DETAILS -> currentStep = DriverRegStep.PROFILE_AND_IDENTITY
            DriverRegStep.DRIVING_LICENSE -> currentStep = DriverRegStep.VEHICLE_DETAILS
            DriverRegStep.CONFIRMATION_PENDING -> onBackToPassenger()
        }
    }

    // Step 1 State: Profile & Identity Documents
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

    // Step 2 State: Vehicle Details & Multi-Angle Photos
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

    // Step 3 State: Driving License & Additional Doc
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

    // Real-time verification listener
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

    // Check completed required uploads (9 required docs)
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
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .testTag("driver_registration_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Action Bar
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
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("driver_registration_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = InDriveLimeGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Driver Onboarding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onNavigateToAdminPortal,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("driver_registration_admin_portal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Portal",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stepper Header & Visual Pipeline
                if (currentStep != DriverRegStep.CONFIRMATION_PENDING) {
                    DriverRegistrationStepperHeader(
                        currentStep = currentStep,
                        requiredUploadedCount = requiredUploadedCount,
                        allRequiredCompleted = allRequiredCompleted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Step Content with Smooth Animated Transition
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
                                onBackToPassenger = onBackToPassenger,
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
                                onBack = {
                                    currentStep = DriverRegStep.PROFILE_AND_IDENTITY
                                },
                                onNext = {
                                    if (vehicleCompany.isBlank()) vehicleCompany = "Toyota"
                                    if (vehicleModel.isBlank()) vehicleModel = "Corolla"
                                    if (vehicleNumber.isBlank()) vehicleNumber = "LEA-4521"

                                    // Persist vehicle details into dedicated 'vehicle' table
                                    val currentDriverId = user?.uid ?: "driver_guest"
                                    val vehicleId = "veh_${currentDriverId.trim().replace("-", "")}"
                                    val driverVehicle = DriverVehicle(
                                        vehicleId = vehicleId,
                                        driverId = currentDriverId,
                                        driverName = user?.displayName.orEmpty().ifBlank { "Driver" },
                                        driverPhone = user?.phoneNumber.orEmpty(),
                                        company = vehicleCompany,
                                        model = vehicleModel,
                                        plateNumber = vehicleNumber,
                                        category = "Car",
                                        frontPhotoUrl = vehicleFrontDoc?.fileUrl.orEmpty(),
                                        backPhotoUrl = vehicleBackDoc?.fileUrl.orEmpty(),
                                        sidePhotoUrl = vehicleSideDoc?.fileUrl.orEmpty(),
                                        registrationDocUrl = vehicleRegDoc?.fileUrl.orEmpty(),
                                        verificationStatus = "PENDING",
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    scope.launch {
                                        try {
                                            val repo = FirebaseRepository.getInstance(context)
                                            repo.saveVehicle(driverVehicle)
                                        } catch (_: Exception) {}
                                    }

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
                                onBack = {
                                    currentStep = DriverRegStep.VEHICLE_DETAILS
                                },
                                onSubmit = {
                                    if (!isSubmitting) {
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
                                                snackbarHostState.showSnackbar("Please upload all 9 required verification documents ($requiredUploadedCount/9 completed).")
                                            }
                                        } else {
                                            val safeUid = user?.uid ?: "driver_${System.currentTimeMillis()}"

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

                                                    val cleanDriverId = (user?.uid ?: verification.uid).trim().replace("-", "")
                                                    val vehicleId = "veh_$cleanDriverId"
                                                    val finalVehicle = DriverVehicle(
                                                        vehicleId = vehicleId,
                                                        driverId = user?.uid ?: verification.uid,
                                                        driverName = verification.name,
                                                        driverPhone = verification.phone,
                                                        company = verification.vehicleCompany,
                                                        model = verification.vehicleModel,
                                                        plateNumber = verification.vehicleNumber,
                                                        category = verification.vehicleCategory.ifBlank { "Car" },
                                                        frontPhotoUrl = verification.vehicleFrontUri.ifBlank { verification.vehiclePictureUri },
                                                        backPhotoUrl = verification.vehicleBackUri,
                                                        sidePhotoUrl = verification.vehicleSideUri,
                                                        registrationDocUrl = verification.vehicleRegistrationDocUri.ifBlank {
                                                            verification.vehicleCardDocFrontUri.ifBlank { verification.vehicleCardDocBackUri }
                                                        },
                                                        verificationStatus = "PENDING",
                                                        createdAt = verification.submittedAt,
                                                        updatedAt = System.currentTimeMillis()
                                                    )
                                                    repo.saveVehicle(finalVehicle)

                                                    isSubmitting = false
                                                    if (res.isSuccess) {
                                                        verificationState = verification
                                                        onVerificationCompleted(verification)
                                                        currentStep = DriverRegStep.CONFIRMATION_PENDING
                                                        snackbarHostState.showSnackbar("Application submitted successfully!")
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
                                onSwitchToDriver = onConfirmedAndSwitchToDriver,
                                onReuploadRejectedDocs = {
                                    currentStep = DriverRegStep.PROFILE_AND_IDENTITY
                                }
                            )
                        }
                    }
                }
            }

            // High Resolution Document Zoom Dialog
            if (previewImageUrl != null) {
                Dialog(onDismissRequest = { previewImageUrl = null }) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .testTag("driver_registration_image_preview_dialog")
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
                                IconButton(onClick = { previewImageUrl = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .clip(RoundedCornerShape(14.dp))
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
                                text = "Encrypted High-Resolution Document Preview",
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
 * Modern Stepper Progress Header displaying 3 onboarding steps & live upload counters.
 */
@Composable
fun DriverRegistrationStepperHeader(
    currentStep: DriverRegStep,
    requiredUploadedCount: Int,
    allRequiredCompleted: Boolean
) {
    val stepIndex = when (currentStep) {
        DriverRegStep.PROFILE_AND_IDENTITY -> 1
        DriverRegStep.VEHICLE_DETAILS -> 2
        DriverRegStep.DRIVING_LICENSE -> 3
        DriverRegStep.CONFIRMATION_PENDING -> 3
    }

    val progressFraction = when (currentStep) {
        DriverRegStep.PROFILE_AND_IDENTITY -> 0.33f
        DriverRegStep.VEHICLE_DETAILS -> 0.66f
        DriverRegStep.DRIVING_LICENSE -> 1.0f
        DriverRegStep.CONFIRMATION_PENDING -> 1.0f
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_registration_stepper_header")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pipeline Step Nodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StepNode(
                    stepNumber = 1,
                    title = "Identity",
                    icon = Icons.Default.Badge,
                    isActive = stepIndex == 1,
                    isCompleted = stepIndex > 1
                )
                StepConnector(isCompleted = stepIndex > 1, modifier = Modifier.weight(1f))
                StepNode(
                    stepNumber = 2,
                    title = "Vehicle",
                    icon = Icons.Default.DirectionsCar,
                    isActive = stepIndex == 2,
                    isCompleted = stepIndex > 2
                )
                StepConnector(isCompleted = stepIndex > 2, modifier = Modifier.weight(1f))
                StepNode(
                    stepNumber = 3,
                    title = "License",
                    icon = Icons.Default.AssignmentInd,
                    isActive = stepIndex == 3,
                    isCompleted = allRequiredCompleted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linear Progress Bar & Required Badge Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Step $stepIndex of 3",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$requiredUploadedCount / 9 Docs Uploaded",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (allRequiredCompleted) InDriveLimeGreen else Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (allRequiredCompleted) InDriveLimeGreen else DrigoBrandFuchsia,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepNode(
    stepNumber: Int,
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isCompleted -> InDriveLimeGreen
                isActive -> DrigoBrandFuchsia
                else -> Color.White.copy(alpha = 0.2f)
            },
            border = if (isActive) BorderStroke(2.dp, Color.White) else null,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive || isCompleted) Color.White else Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun StepConnector(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(if (isCompleted) InDriveLimeGreen else Color.White.copy(alpha = 0.25f))
    )
}

/**
 * Step 1: Profile Photo & CNIC Identity Documents
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
    onBackToPassenger: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Guided Section Header Card
        StepGuideBanner(
            title = "Personal Identity & Profile",
            description = "Upload a clear face photograph and official National Identity Card (CNIC) photos for identity verification.",
            icon = Icons.Default.Badge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Driver Profile Photo Section
        SecureDocumentUploadCard(
            userId = userId,
            docType = DriverDocumentStorageManager.DOC_DRIVER_PHOTO,
            title = "Driver Profile Photo",
            subtitle = "Recent front-facing face photo without glasses or hat",
            isRequired = true,
            icon = Icons.Default.AccountBox,
            docItem = driverPhotoDoc,
            onDocUploaded = onDriverPhotoChange,
            onPreview = onPreview,
            compact = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CNIC Front & Back Cards
        Text(
            text = "National Identity Card (CNIC)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 340.dp
            if (isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecureDocumentUploadCard(
                        userId = userId,
                        docType = DriverDocumentStorageManager.DOC_CNIC_FRONT,
                        title = "CNIC Front Side",
                        subtitle = "Clear photo showing full front details",
                        isRequired = true,
                        icon = Icons.Default.CreditCard,
                        docItem = cnicFrontDoc,
                        onDocUploaded = onCnicFrontChange,
                        onPreview = onPreview,
                        compact = true
                    )
                    SecureDocumentUploadCard(
                        userId = userId,
                        docType = DriverDocumentStorageManager.DOC_CNIC_BACK,
                        title = "CNIC Back Side",
                        subtitle = "Clear photo showing back details",
                        isRequired = true,
                        icon = Icons.Default.CreditCard,
                        docItem = cnicBackDoc,
                        onDocUploaded = onCnicBackChange,
                        onPreview = onPreview,
                        compact = true
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SecureDocumentUploadCard(
                            userId = userId,
                            docType = DriverDocumentStorageManager.DOC_CNIC_FRONT,
                            title = "CNIC Front",
                            subtitle = "Front details & CNIC #",
                            isRequired = true,
                            icon = Icons.Default.CreditCard,
                            docItem = cnicFrontDoc,
                            onDocUploaded = onCnicFrontChange,
                            onPreview = onPreview,
                            compact = true
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SecureDocumentUploadCard(
                            userId = userId,
                            docType = DriverDocumentStorageManager.DOC_CNIC_BACK,
                            title = "CNIC Back",
                            subtitle = "Back details & address",
                            isRequired = true,
                            icon = Icons.Default.CreditCard,
                            docItem = cnicBackDoc,
                            onDocUploaded = onCnicBackChange,
                            onPreview = onPreview,
                            compact = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Navigation Footer Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBackToPassenger,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Exit", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DrigoBrandFuchsia,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("driver_registration_step1_next_button")
            ) {
                Text(
                    text = "CONTINUE TO VEHICLE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Step 2: Vehicle Inspection Photos & Text Details
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
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var showCompanyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    if (showCompanyDialog) {
        VehicleCompanySelectionDialog(
            selectedCompany = vehicleCompany,
            onCompanySelected = { newCompany ->
                onVehicleCompanyChange(newCompany)
                onVehicleModelChange("")
            },
            onDismiss = { showCompanyDialog = false }
        )
    }

    if (showModelDialog) {
        VehicleModelSelectionDialog(
            selectedCompany = vehicleCompany,
            selectedModel = vehicleModel,
            onModelSelected = { newModel ->
                onVehicleModelChange(newModel)
            },
            onDismiss = { showModelDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepGuideBanner(
            title = "Vehicle Details & Photos",
            description = "Upload 4-angle vehicle photographs along with official vehicle registration details.",
            icon = Icons.Default.DirectionsCar
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Multi-angle vehicle upload cards
        Text(
            text = "Vehicle Inspection Photos",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SecureDocumentUploadCard(
                        userId = userId,
                        docType = DriverDocumentStorageManager.DOC_VEHICLE_FRONT,
                        title = "Car Front View",
                        subtitle = "Clear view with license plate",
                        isRequired = true,
                        icon = Icons.Default.DirectionsCar,
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
                        title = "Car Back View",
                        subtitle = "Rear view with license plate",
                        isRequired = true,
                        icon = Icons.Default.DirectionsCar,
                        docItem = vehicleBackDoc,
                        onDocUploaded = onVehicleBackChange,
                        onPreview = onPreview,
                        compact = true
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SecureDocumentUploadCard(
                        userId = userId,
                        docType = DriverDocumentStorageManager.DOC_VEHICLE_SIDE,
                        title = "Car Side View",
                        subtitle = "Side profile of vehicle",
                        isRequired = true,
                        icon = Icons.Default.DirectionsCar,
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
                        title = "Registration Doc",
                        subtitle = "Registration card / book",
                        isRequired = true,
                        icon = Icons.Default.Description,
                        docItem = vehicleRegDoc,
                        onDocUploaded = onVehicleRegDocChange,
                        onPreview = onPreview,
                        compact = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Vehicle Information Input Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Vehicle Credentials",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                SelectableDriverInputField(
                    value = vehicleCompany,
                    placeholder = "Choose Company (e.g. Toyota, Nissan)",
                    label = "Vehicle Make / Manufacturer",
                    leadingIcon = Icons.Default.DirectionsCar,
                    onClick = { showCompanyDialog = true }
                )

                SelectableDriverInputField(
                    value = vehicleModel,
                    placeholder = if (vehicleCompany.isBlank()) "Select company first" else "Choose Model (e.g. Corolla, Civic)",
                    label = "Vehicle Model & Year",
                    leadingIcon = Icons.Default.Build,
                    onClick = { showModelDialog = true }
                )

                DriverInputField(
                    value = vehicleNumber,
                    placeholder = "License Plate (e.g. LEA-4521)",
                    label = "Registration / Plate Number",
                    leadingIcon = Icons.Default.Pin,
                    onValueChange = onVehicleNumberChange,
                    imeAction = ImeAction.Done
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Navigation Footer Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Previous", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DrigoBrandFuchsia,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("driver_registration_step2_next_button")
            ) {
                Text(
                    text = "CONTINUE TO LICENSE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Step 3: Driving License & Submission Step
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
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepGuideBanner(
            title = "Driving License & Verification",
            description = "Upload your valid driving license (Front & Back) and any optional supporting credentials.",
            icon = Icons.Default.AssignmentInd
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Driving License Cards
        Text(
            text = "Driving License Credentials",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 340.dp
            if (isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecureDocumentUploadCard(
                        userId = userId,
                        docType = DriverDocumentStorageManager.DOC_LICENSE_FRONT,
                        title = "Driving License Front",
                        subtitle = "License # and expiry date",
                        isRequired = true,
                        icon = Icons.Default.Badge,
                        docItem = licenseFrontDoc,
                        onDocUploaded = onLicenseFrontChange,
                        onPreview = onPreview,
                        compact = true
                    )
                    SecureDocumentUploadCard(
                        userId = userId,
                        docType = DriverDocumentStorageManager.DOC_LICENSE_BACK,
                        title = "Driving License Back",
                        subtitle = "Back categories and endorsement",
                        isRequired = true,
                        icon = Icons.Default.Badge,
                        docItem = licenseBackDoc,
                        onDocUploaded = onLicenseBackChange,
                        onPreview = onPreview,
                        compact = true
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SecureDocumentUploadCard(
                            userId = userId,
                            docType = DriverDocumentStorageManager.DOC_LICENSE_FRONT,
                            title = "License Front",
                            subtitle = "Front license card",
                            isRequired = true,
                            icon = Icons.Default.Badge,
                            docItem = licenseFrontDoc,
                            onDocUploaded = onLicenseFrontChange,
                            onPreview = onPreview,
                            compact = true
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SecureDocumentUploadCard(
                            userId = userId,
                            docType = DriverDocumentStorageManager.DOC_LICENSE_BACK,
                            title = "License Back",
                            subtitle = "Back license details",
                            isRequired = true,
                            icon = Icons.Default.Badge,
                            docItem = licenseBackDoc,
                            onDocUploaded = onLicenseBackChange,
                            onPreview = onPreview,
                            compact = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Additional Supporting Document (Optional)
        SecureDocumentUploadCard(
            userId = userId,
            docType = DriverDocumentStorageManager.DOC_ADDITIONAL_DOC,
            title = "Additional Supporting Document",
            subtitle = "Optional (e.g. Police Character Certificate, Route Permit, Utility Bill)",
            isRequired = false,
            icon = Icons.Default.FolderSpecial,
            docItem = additionalDoc,
            onDocUploaded = onAdditionalDocChange,
            onPreview = onPreview,
            compact = false
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Submission Requirement Status Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (allRequiredCompleted) InDriveLimeGreen.copy(alpha = 0.18f) else Color(0xFFFFB300).copy(alpha = 0.18f),
            border = BorderStroke(1.dp, if (allRequiredCompleted) InDriveLimeGreen else Color(0xFFFFB300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (allRequiredCompleted) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (allRequiredCompleted) InDriveLimeGreen else Color(0xFFFFB300),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = if (allRequiredCompleted) "All Required Documents Ready!" else "Requirements Incomplete ($requiredUploadedCount / 9)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (allRequiredCompleted)
                            "Your application is ready for compliance review."
                        else
                            "Please complete uploading all 9 required photos before submitting.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Navigation Footer Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Previous", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DrigoBrandFuchsia,
                    contentColor = Color.White,
                    disabledContainerColor = DrigoBrandFuchsia.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("driver_registration_submit_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUBMITTING...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Text(
                        text = "SUBMIT FOR REVIEW",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Step 4: Confirmation & Real-time Verification Status Dashboard
 */
@Composable
fun ConfirmationPendingStep(
    verification: DriverVerification,
    onBackToPassenger: () -> Unit,
    onSwitchToDriver: () -> Unit,
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
        // Hero Status Circle Banner
        Surface(
            shape = CircleShape,
            color = when {
                isAllApproved -> InDriveLimeGreen.copy(alpha = 0.2f)
                isRejected -> Color(0xFFEF4444).copy(alpha = 0.2f)
                else -> Color.White.copy(alpha = 0.15f)
            },
            border = BorderStroke(
                2.dp,
                when {
                    isAllApproved -> InDriveLimeGreen
                    isRejected -> Color(0xFFEF4444)
                    else -> Color(0xFFFFB300)
                }
            ),
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
                        isAllApproved -> InDriveLimeGreen
                        isRejected -> Color(0xFFEF4444)
                        else -> Color(0xFFFFB300)
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
                            "Congratulations! Your registration has been verified by the compliance team."
                        isRejected -> {
                            val mainReason = verification.rejectionReason.ifBlank { "One or more documents require re-uploading." }
                            "Action Required: $mainReason" + if (rejectedDocs.isNotEmpty()) {
                                "\n\n" + rejectedDocs.joinToString("\n") { "• ${it.title}: ${it.rejectionReason.ifBlank { "Invalid or unclear document" }}" }
                            } else ""
                        }
                        else ->
                            "Your documents are under review by our compliance team. Reviews take under 24 hours."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                val statusLabel = when {
                    isAllApproved -> "STATUS: APPROVED"
                    isRejected -> "STATUS: ACTION REQUIRED (${rejectedDocs.size} REJECTED)"
                    else -> "STATUS: KYC DOCS ($pendingCount PENDING)"
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isAllApproved -> InDriveLimeGreen.copy(alpha = 0.25f)
                        isRejected -> Color(0xFFEF4444).copy(alpha = 0.25f)
                        else -> Color(0xFFFFB300).copy(alpha = 0.25f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isAllApproved -> InDriveLimeGreen
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
                                isAllApproved -> InDriveLimeGreen
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

        Spacer(modifier = Modifier.height(18.dp))

        // Individual KYC Document Status Breakdown List
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
                            "APPROVED" -> InDriveLimeGreen
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
                    containerColor = DrigoBrandFuchsia,
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
                    containerColor = DrigoBrandFuchsia,
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
                    contentColor = DrigoBrandMagentaBg
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
        }
    }
}

/**
 * Step Instruction Banner with Icon
 */
@Composable
private fun StepGuideBanner(
    title: String,
    description: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = DrigoBrandFuchsia.copy(alpha = 0.25f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Redesigned Secure Document Upload Card with:
 * - Direct photo picker & Google Drive / Cloud Storage upload
 * - Real-time animated upload progress bar & status text
 * - Image thumbnail preview with Crop Fill
 * - Quick Zoom/Preview & Replace action triggers
 * - Clear required/optional badges & camera guidelines
 */
@Composable
fun SecureDocumentUploadCard(
    userId: String,
    docType: String,
    title: String,
    subtitle: String,
    isRequired: Boolean,
    icon: ImageVector,
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
                            FirebaseRepository.getInstance(context)
                                .saveDriverDocumentMetadata(userId, uploadedDoc)
                        } catch (_: Exception) {}
                    }
                } else {
                    uploadError = res.exceptionOrNull()?.message ?: "Upload failed"
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(
            width = if (docItem?.fileUrl?.isNotBlank() == true) 1.5.dp else 1.dp,
            color = when {
                uploadError != null -> Color(0xFFEF4444)
                docItem?.fileUrl?.isNotBlank() == true -> InDriveLimeGreen
                else -> Color.White.copy(alpha = 0.3f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doc_upload_card_$docType")
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Document Header: Title & Required/Optional Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = if (compact) 12.sp else 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isRequired) DrigoBrandFuchsia.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isRequired) DrigoBrandFuchsia else Color.White.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (isRequired) "REQUIRED *" else "OPTIONAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (subtitle.isNotBlank() && !compact) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Interactive Upload Image Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 110.dp else 140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(
                        width = 1.dp,
                        color = if (docItem?.fileUrl?.isNotBlank() == true) InDriveLimeGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.size(36.dp),
                            color = DrigoBrandFuchsia,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (docItem != null && docItem.fileUrl.isNotBlank()) {
                    AsyncImage(
                        model = docItem.fileUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Top-right Uploaded Success Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(InDriveLimeGreen, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Uploaded",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Uploaded",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Bottom Action Bar: Preview & Replace
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.clickable { onPreview(docItem.fileUrl, title) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Zoom",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Preview",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DrigoBrandFuchsia.copy(alpha = 0.85f),
                                modifier = Modifier.clickable { launcher.launch("image/*") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Replace",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Replace",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Add $title",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(if (compact) 26.dp else 34.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to upload photo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Camera or Gallery",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            if (uploadError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uploadError ?: "",
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Reusable Driver Input Field with Leading Icon & Clear Styling
 */
@Composable
fun DriverInputField(
    value: String,
    placeholder: String,
    label: String,
    leadingIcon: ImageVector,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DrigoBrandFuchsia,
                unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = imeAction
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Clickable input field with dropdown arrow for company and model selection.
 */
@Composable
fun SelectableDriverInputField(
    value: String,
    placeholder: String,
    label: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = value.ifBlank { placeholder },
                        fontSize = 14.sp,
                        color = if (value.isNotBlank()) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (value.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Searchable Vehicle Manufacturer / Company Picker Dialog
 */
@Composable
fun VehicleCompanySelectionDialog(
    selectedCompany: String,
    onCompanySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    var customCompanyText by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery) {
        VehicleCatalog.searchManufacturers(searchQuery)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1028),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(4.dp)
                .testTag("vehicle_company_selection_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = DrigoBrandFuchsia,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Select Vehicle Company",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search brand... e.g. Toyota, Nissan", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DrigoBrandFuchsia,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (showCustomInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Type Custom Manufacturer:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customCompanyText,
                            onValueChange = { customCompanyText = it },
                            placeholder = { Text("Enter brand name", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DrigoBrandFuchsia,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCustomInput = false }) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customCompanyText.isNotBlank()) {
                                        onCompanySelected(customCompanyText.trim())
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandFuchsia)
                            ) {
                                Text("Use Brand", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList) { company ->
                            val isSelected = company.equals(selectedCompany, ignoreCase = true)
                            val isCustomOption = company == "Other / Custom Manufacturer"

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) DrigoBrandFuchsia.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                border = if (isSelected) BorderStroke(1.dp, DrigoBrandFuchsia) else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isCustomOption) {
                                            showCustomInput = true
                                        } else {
                                            onCompanySelected(company)
                                            onDismiss()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCustomOption) Icons.Default.Edit else Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = if (isSelected || isCustomOption) InDriveLimeGreen else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = company,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = InDriveLimeGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Searchable Vehicle Model Picker Dialog
 */
@Composable
fun VehicleModelSelectionDialog(
    selectedCompany: String,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    var customModelText by remember { mutableStateOf("") }

    val effectiveCompany = selectedCompany.ifBlank { "Toyota" }
    val filteredList = remember(effectiveCompany, searchQuery) {
        VehicleCatalog.searchModels(effectiveCompany, searchQuery)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1028),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(4.dp)
                .testTag("vehicle_model_selection_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = DrigoBrandFuchsia,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Select Vehicle Model",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "For $effectiveCompany",
                                style = MaterialTheme.typography.labelSmall,
                                color = InDriveLimeGreen
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search model... e.g. Corolla, Civic, Alto", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DrigoBrandFuchsia,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (showCustomInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Type Custom Model / Year:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customModelText,
                            onValueChange = { customModelText = it },
                            placeholder = { Text("e.g. Corolla 2023 / Custom Model", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DrigoBrandFuchsia,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCustomInput = false }) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customModelText.isNotBlank()) {
                                        onModelSelected(customModelText.trim())
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DrigoBrandFuchsia)
                            ) {
                                Text("Use Model", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList) { modelName ->
                            val isSelected = modelName.equals(selectedModel, ignoreCase = true)
                            val isCustomOption = modelName == "Other / Custom Model"

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) DrigoBrandFuchsia.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                border = if (isSelected) BorderStroke(1.dp, DrigoBrandFuchsia) else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isCustomOption) {
                                            showCustomInput = true
                                        } else {
                                            onModelSelected(modelName)
                                            onDismiss()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCustomOption) Icons.Default.Edit else Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = if (isSelected || isCustomOption) InDriveLimeGreen else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = modelName,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = InDriveLimeGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
