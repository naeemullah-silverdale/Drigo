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
    WALLET
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

    init {
        viewModelScope.launch {
            authRepo.authStateFlow().collect { user ->
                _currentUser.value = user
                if (user != null) {
                    fetchUserModeFromDb(user.uid)
                } else {
                    _userMode.value = UserMode.PASSENGER
                }
            }
        }
    }

    private fun fetchUserModeFromDb(uid: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("mode")
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
        _isDriverOnline.value = !_isDriverOnline.value
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
            pickupTitle = pickupTitle,
            pickupSubtitle = pickupSubtitle,
            pickupLat = pickupLat,
            pickupLon = pickupLon,
            destinationTitle = destinationTitle,
            destinationSubtitle = destinationSubtitle,
            destinationLat = destinationLat,
            destinationLon = destinationLon,
            rideCategory = rideCategory,
            estimatedFare = fare,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            status = "SEARCHING_DRIVERS",
            timestamp = System.currentTimeMillis()
        )

        val firebaseRepo = FirebaseRepository.getInstance(context)
        return firebaseRepo.createRideRequest(request)
    }
}
