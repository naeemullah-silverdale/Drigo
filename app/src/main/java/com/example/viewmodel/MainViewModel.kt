package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.RideRequest
import com.example.data.remote.AuthRepository
import com.example.data.remote.FirebaseRepository
import com.example.data.remote.GoogleAuthClient
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class AppScreen {
    WELCOME,
    SIGN_IN,
    SIGN_UP,
    HOME_PLACEHOLDER,
    WALLET,
    DRIVER_REGISTRATION,
    ADMIN_VERIFICATION,
    GOOGLE_DRIVE_DOCUMENTS
}

enum class UserMode {
    PASSENGER,
    DRIVER
}

class MainViewModel(
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.WELCOME)
    val currentScreen = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepo.currentUser)
    val currentUser = _currentUser.asStateFlow()

    // Default mode is PASSENGER for new and logged-in users
    private val _userMode = MutableStateFlow(UserMode.PASSENGER)
    val userMode = _userMode.asStateFlow()

    // Driver online state when in Driver mode
    private val _isDriverOnline = MutableStateFlow(false)
    val isDriverOnline = _isDriverOnline.asStateFlow()

    // Driver verification state
    private val _driverVerification = MutableStateFlow<com.example.data.model.DriverVerification?>(null)
    val driverVerification = _driverVerification.asStateFlow()

    // Unified User Record flow from users/{uid}
    private val _userRecord = MutableStateFlow<com.example.data.model.UserRecord?>(null)
    val userRecord = _userRecord.asStateFlow()

    // Google Drive REST API Cloud Storage state
    val googleDriveManager = com.example.data.remote.GoogleDriveStorageManager()
    
    private val _isGoogleDriveConnected = MutableStateFlow(false)
    val isGoogleDriveConnected = _isGoogleDriveConnected.asStateFlow()

    private val _googleDriveUserEmail = MutableStateFlow<String?>(null)
    val googleDriveUserEmail = _googleDriveUserEmail.asStateFlow()

    private val _googleDriveUserName = MutableStateFlow<String?>(null)
    val googleDriveUserName = _googleDriveUserName.asStateFlow()

    private val _googleDriveFiles = MutableStateFlow<List<com.example.data.model.GoogleDriveFileRecord>>(emptyList())
    val googleDriveFiles = _googleDriveFiles.asStateFlow()

    private val _isGoogleDriveUploading = MutableStateFlow(false)
    val isGoogleDriveUploading = _isGoogleDriveUploading.asStateFlow()

    private val _googleDriveUploadProgress = MutableStateFlow(0f)
    val googleDriveUploadProgress = _googleDriveUploadProgress.asStateFlow()

    private val _googleDriveStatusMessage = MutableStateFlow<String?>(null)
    val googleDriveStatusMessage = _googleDriveStatusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.authStateFlow().collect { user ->
                _currentUser.value = user
                if (user != null) {
                    fetchUserModeFromDb(user.uid)
                    fetchDriverVerificationFromDb(user.uid)
                    fetchUserRecordFromDb(user.uid)
                } else {
                    _userMode.value = UserMode.PASSENGER
                    _driverVerification.value = null
                    _userRecord.value = null
                }
            }
        }
    }

    fun updateDriverVerification(ver: com.example.data.model.DriverVerification) {
        _driverVerification.value = ver
    }

    private var driverVerificationJob: kotlinx.coroutines.Job? = null
    private var userRecordJob: kotlinx.coroutines.Job? = null

    private fun fetchUserRecordFromDb(uid: String) {
        userRecordJob?.cancel()
        userRecordJob = viewModelScope.launch {
            FirebaseRepository.getInstance().listenToUserRecord(uid).collect { record ->
                _userRecord.value = record
                if (record != null) {
                    val driverVer = _driverVerification.value
                    val parsedVerStatus = com.example.data.model.parseDriverVerificationStatus(
                        record.verificationStatus,
                        driverVer?.status,
                        driverVer?.confirmtion ?: false
                    )
                    val parsedDriverAccStatus = com.example.data.model.parseDriverAccountStatus(
                        record.accountStatus,
                        driverVer?.status,
                        parsedVerStatus,
                        record.isOnline
                    )

                    // Realtime Enforcement: Kick driver offline if suspended
                    if (parsedDriverAccStatus == com.example.data.model.DriverAccountStatus.SUSPENDED ||
                        parsedDriverAccStatus == com.example.data.model.DriverAccountStatus.FLAGGED) {
                        if (_isDriverOnline.value) {
                            _isDriverOnline.value = false
                            viewModelScope.launch {
                                FirebaseRepository.getInstance().updateDriverOperationalStatus(uid, "SUSPENDED", false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchDriverVerificationFromDb(uid: String) {
        driverVerificationJob?.cancel()
        driverVerificationJob = viewModelScope.launch {
            FirebaseRepository.getInstance().listenToDriverVerification(uid).collect { ver ->
                _driverVerification.value = ver
                val userRec = _userRecord.value
                val verStatus = com.example.data.model.parseDriverVerificationStatus(
                    userRec?.verificationStatus ?: ver?.verificationStatus,
                    ver?.status,
                    ver?.confirmtion ?: false
                )
                val accStatus = com.example.data.model.parseDriverAccountStatus(
                    userRec?.accountStatus ?: ver?.accountStatus,
                    ver?.status,
                    verStatus,
                    _isDriverOnline.value
                )

                if (verStatus != com.example.data.model.DriverVerificationStatus.APPROVED ||
                    accStatus == com.example.data.model.DriverAccountStatus.SUSPENDED ||
                    accStatus == com.example.data.model.DriverAccountStatus.FLAGGED) {
                    _isDriverOnline.value = false
                }
            }
        }
    }

    private fun fetchUserModeFromDb(uid: String) {
        val db = try {
            FirebaseDatabase.getInstance("https://drigo-8b15c-default-rtdb.firebaseio.com")
        } catch (_: Exception) {
            FirebaseDatabase.getInstance()
        }
        val userRef = db.getReference("users").child(uid).child("mode")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val modeStr = snapshot.getValue(String::class.java)
                if (modeStr == "DRIVER") {
                    _userMode.value = UserMode.DRIVER
                } else {
                    _userMode.value = UserMode.PASSENGER
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _userMode.value = UserMode.PASSENGER
            }
        })
    }

    /**
     * Attempts to switch user mode with verification check:
     * - If switching to DRIVER: checks if driverVerification exists and confirmtion == true.
     *   If not confirmed/registered, navigates to Driver Registration flow.
     * - If switching to PASSENGER: allows immediately.
     */
    fun attemptSwitchUserMode(targetMode: UserMode) {
        if (targetMode == UserMode.DRIVER) {
            val ver = _driverVerification.value
            val userRec = _userRecord.value
            val verStatus = com.example.data.model.parseDriverVerificationStatus(
                userRec?.verificationStatus ?: ver?.verificationStatus,
                ver?.status,
                ver?.confirmtion ?: false
            )
            val accStatus = com.example.data.model.parseDriverAccountStatus(
                userRec?.accountStatus ?: ver?.accountStatus,
                ver?.status,
                verStatus,
                _isDriverOnline.value
            )

            if (accStatus == com.example.data.model.DriverAccountStatus.SUSPENDED) {
                // Switch mode to DRIVER so DriverModeView displays the Account Suspended UI overlay
                setUserMode(UserMode.DRIVER)
                return
            }

            if (verStatus != com.example.data.model.DriverVerificationStatus.APPROVED) {
                // Not approved yet -> navigate to Driver Registration / KYC Status Tracker screen
                _currentScreen.value = AppScreen.DRIVER_REGISTRATION
            } else {
                // Confirmed & Approved! Switch to DRIVER mode
                setUserMode(UserMode.DRIVER)
            }
        } else {
            setUserMode(UserMode.PASSENGER)
        }
    }

    fun setUserMode(mode: UserMode) {
        _userMode.value = mode
        val uid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                FirebaseDatabase.getInstance().getReference("users").child(uid).child("mode")
                    .setValue(mode.name).await()
            } catch (_: Exception) {
                // Ignore sync errors
            }
        }
    }

    fun toggleDriverOnline() {
        val ver = _driverVerification.value
        val userRec = _userRecord.value
        val verStatus = com.example.data.model.parseDriverVerificationStatus(
            userRec?.verificationStatus ?: ver?.verificationStatus,
            ver?.status,
            ver?.confirmtion ?: false
        )
        val accStatus = com.example.data.model.parseDriverAccountStatus(
            userRec?.accountStatus ?: ver?.accountStatus,
            ver?.status,
            verStatus,
            _isDriverOnline.value
        )

        // Rule B.3: Driver can ONLY go Online if verificationStatus == APPROVED and accountStatus != SUSPENDED / FLAGGED
        val canGoOnline = (verStatus == com.example.data.model.DriverVerificationStatus.APPROVED) &&
                (accStatus != com.example.data.model.DriverAccountStatus.SUSPENDED) &&
                (accStatus != com.example.data.model.DriverAccountStatus.FLAGGED)

        if (!canGoOnline) {
            _isDriverOnline.value = false
            return
        }

        val nextOnlineState = !_isDriverOnline.value
        _isDriverOnline.value = nextOnlineState

        val uid = _currentUser.value?.uid ?: return
        viewModelScope.launch {
            val newAccStatus = if (nextOnlineState) "ONLINE" else "ACTIVE"
            FirebaseRepository.getInstance().updateDriverOperationalStatus(uid, newAccStatus, nextOnlineState)
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        val res = authRepo.signIn(email, password)
        return if (res.isSuccess) {
            val user = res.getOrNull()
            _currentUser.value = user
            user?.uid?.let { fetchUserModeFromDb(it) }
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Sign in failed"))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return authRepo.sendPasswordReset(email)
    }

    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        val googleAuthClient = GoogleAuthClient(context)
        val res = googleAuthClient.signInWithGoogle()
        return if (res.isSuccess) {
            val user = res.getOrNull()
            _currentUser.value = user
            user?.uid?.let { fetchUserModeFromDb(it) }
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Google sign in failed"))
        }
    }

    suspend fun signUp(fullName: String, email: String, password: String): Result<Unit> {
        val res = authRepo.signUp(email, password)
        return if (res.isSuccess) {
            val user = res.getOrNull()
            _currentUser.value = user
            _userMode.value = UserMode.PASSENGER // Explicitly default to Passenger for new users
            
            // Set display name on Firebase User
            try {
                user?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                )?.await()

                // Save basic profile to Firebase Realtime Database with default PASSENGER mode
                user?.uid?.let { uid ->
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                    val userData = mapOf(
                        "uid" to uid,
                        "name" to fullName,
                        "email" to email,
                        "mode" to "PASSENGER",
                        "createdAt" to System.currentTimeMillis()
                    )
                    userRef.setValue(userData).await()
                }
            } catch (_: Exception) {
                // Non-blocking profile enrichment
            }

            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Sign up failed"))
        }
    }

    fun signOut() {
        authRepo.signOut()
        _currentUser.value = null
        _userMode.value = UserMode.PASSENGER
        _currentScreen.value = AppScreen.WELCOME
    }

    suspend fun createRideRequest(
        context: Context,
        pickupTitle: String,
        pickupSubtitle: String,
        pickupLat: Double,
        pickupLon: Double,
        destinationTitle: String,
        destinationSubtitle: String,
        destinationLat: Double,
        destinationLon: Double,
        rideCategory: String,
        fare: Int,
        distanceKm: Double,
        durationMinutes: Int
    ): Result<String> {
        val user = _currentUser.value
        val passengerId = user?.uid ?: "rider_${System.currentTimeMillis().toString().takeLast(6)}"
        val passengerName = user?.displayName?.ifBlank { "Drigo Passenger" } ?: (user?.email?.substringBefore("@") ?: "Drigo Passenger")
        val passengerEmail = user?.email ?: ""

        val request = RideRequest(
            id = UUID.randomUUID().toString(),
            passengerId = passengerId,
            passengerName = passengerName,
            passengerEmail = passengerEmail,
            passengerPhotoUrl = "",
            passengerRating = 4.9,
            paymentMethod = "Cash",
            pickupTitle = pickupTitle,
            pickupSubtitle = pickupSubtitle,
            pickupLat = pickupLat,
            pickupLon = pickupLon,
            destinationTitle = destinationTitle,
            destinationSubtitle = destinationSubtitle,
            destinationLat = destinationLat,
            destinationLon = destinationLon,
            rideCategory = rideCategory,
            vehicleType = when {
                rideCategory.contains("Bike", true) || rideCategory.contains("Moto", true) -> "Bike"
                rideCategory.contains("Mini", true) -> "Mini Car"
                rideCategory.contains("Courier", true) || rideCategory.contains("Parcel", true) -> "Courier"
                else -> "Car"
            },
            hasAc = rideCategory.contains("AC", true) || rideCategory.contains("A/C", true),
            estimatedFare = fare,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            status = "SEARCHING_DRIVERS",
            assignedDriverId = "",
            timestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (15 * 60 * 1000L)
        )

        val firebaseRepo = FirebaseRepository.getInstance(context)
        return firebaseRepo.createRideRequest(request)
    }

    // ==========================================
    // GOOGLE DRIVE CLOUD STORAGE INTEGRATION
    // ==========================================

    fun checkAndInitGoogleDrive(context: Context) {
        val account = com.example.data.remote.GoogleDriveAuthHelper.getLastSignedInAccount(context)
        if (account != null && com.example.data.remote.GoogleDriveAuthHelper.hasDrivePermissions(context, account)) {
            setGoogleDriveAccount(context, account)
        } else {
            // Connected to Google Drive via secure Service Account credentials
            _isGoogleDriveConnected.value = true
            _googleDriveUserEmail.value = com.example.data.remote.GoogleDriveCredentials.SERVICE_ACCOUNT_EMAIL
            _googleDriveUserName.value = "Google Drive Cloud Storage"
            _googleDriveStatusMessage.value = "Connected to Google Drive"
            val safeUserId = _currentUser.value?.uid ?: "default_user"
            listenToGoogleDriveFiles(context, safeUserId)
        }
    }

    fun setGoogleDriveAccount(context: Context, account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        googleDriveManager.init(context, account)
        _isGoogleDriveConnected.value = true
        _googleDriveUserEmail.value = account.email
        _googleDriveUserName.value = account.displayName ?: account.givenName ?: "Google User"
        _googleDriveStatusMessage.value = "Connected to Google Drive as ${account.email}"
        
        // Listen to files from Firebase for this user
        val safeUserId = _currentUser.value?.uid ?: account.id ?: "default_user"
        listenToGoogleDriveFiles(context, safeUserId)
    }

    fun disconnectGoogleDrive(context: Context) {
        com.example.data.remote.GoogleDriveAuthHelper.signOut(context)
        googleDriveManager.disconnect()
        _isGoogleDriveConnected.value = false
        _googleDriveUserEmail.value = null
        _googleDriveUserName.value = null
        _googleDriveStatusMessage.value = "Disconnected from Google Drive"
    }

    private var googleDriveFilesJob: kotlinx.coroutines.Job? = null

    private fun listenToGoogleDriveFiles(context: Context, userId: String) {
        googleDriveFilesJob?.cancel()
        googleDriveFilesJob = viewModelScope.launch {
            val firebaseRepo = FirebaseRepository.getInstance(context)
            firebaseRepo.listenToGoogleDriveFiles(userId).collect { list ->
                _googleDriveFiles.value = list
            }
        }
    }

    /**
     * Upload an image directly to user's Google Drive via REST API v3,
     * and save its public URL and metadata into Firebase Firestore & RTDB.
     */
    suspend fun uploadImageToDriveAndSaveToFirebase(
        context: Context,
        imageUri: android.net.Uri,
        customFileName: String? = null,
        docType: String = "GENERAL_IMAGE",
        category: String = "documents",
        notes: String = ""
    ): Result<com.example.data.model.GoogleDriveFileRecord> {
        _isGoogleDriveUploading.value = true
        _googleDriveUploadProgress.value = 0.05f
        _googleDriveStatusMessage.value = "Uploading image to Google Drive..."

        val safeUserId = _currentUser.value?.uid ?: _googleDriveUserEmail.value ?: "driver_${System.currentTimeMillis()}"

        val uploadResult = googleDriveManager.uploadImageToDrive(
            imageUri = imageUri,
            customFileName = customFileName,
            docType = docType,
            category = category,
            userId = safeUserId,
            notes = notes,
            onProgress = { progress ->
                _googleDriveUploadProgress.value = progress
            }
        )

        _isGoogleDriveUploading.value = false

        if (uploadResult.isSuccess) {
            val record = uploadResult.getOrThrow()
            _googleDriveUploadProgress.value = 1.0f
            _googleDriveStatusMessage.value = "Image uploaded to Drive! Saving link in Firebase..."

            // Save metadata and file URL to Firebase for later retrieval
            val firebaseRepo = FirebaseRepository.getInstance(context)
            firebaseRepo.saveGoogleDriveFileRecord(record)

            _googleDriveStatusMessage.value = "Successfully uploaded and saved to Firebase!"
            return Result.success(record)
        } else {
            val err = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
            _googleDriveStatusMessage.value = "Upload error: $err"
            return Result.failure(Exception(err))
        }
    }

    /**
     * Delete a Google Drive file and its Firebase metadata
     */
    suspend fun deleteGoogleDriveFile(context: Context, fileId: String): Result<Unit> {
        val safeUserId = _currentUser.value?.uid ?: _googleDriveUserEmail.value ?: "default_user"
        
        // 1. Delete from Drive
        val driveRes = googleDriveManager.deleteFile(fileId)
        
        // 2. Delete record from Firebase
        val firebaseRepo = FirebaseRepository.getInstance(context)
        firebaseRepo.deleteGoogleDriveFileRecord(safeUserId, fileId)

        return driveRes
    }
}
