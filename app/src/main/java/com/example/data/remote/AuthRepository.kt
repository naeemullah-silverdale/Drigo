package com.example.data.remote

import android.app.Activity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser?> {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        if (cleanEmail.isBlank()) {
            return Result.failure(Exception("Please enter your email address."))
        }
        if (cleanPassword.isBlank()) {
            return Result.failure(Exception("Please enter your password."))
        }

        return try {
            val result = auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await()
            val user = result.user
            user?.let { syncUserToRealtimeDatabase(it, "email_password") }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthErrorMessage(e)))
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser?> {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        if (cleanEmail.isBlank()) {
            return Result.failure(Exception("Please enter your email address."))
        }
        if (cleanPassword.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters."))
        }

        return try {
            val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
            val user = result.user
            user?.let { syncUserToRealtimeDatabase(it, "email_password") }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthErrorMessage(e)))
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser?> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user
            user?.let { syncUserToRealtimeDatabase(it, "anonymous") }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthErrorMessage(e)))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            return Result.failure(Exception("Please enter your email address."))
        }
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthErrorMessage(e)))
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser?> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            user?.let { syncUserToRealtimeDatabase(it, "google") }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthErrorMessage(e)))
        }
    }

    private suspend fun syncUserToRealtimeDatabase(user: FirebaseUser, provider: String) {
        try {
            val uid = user.uid
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            val userData = mutableMapOf<String, Any>(
                "uid" to uid,
                "name" to (user.displayName ?: "Drigo User"),
                "email" to (user.email ?: ""),
                "provider" to provider,
                "lastLoginAt" to System.currentTimeMillis()
            )
            user.photoUrl?.let { userData["photoUrl"] = it.toString() }
            userRef.updateChildren(userData).await()
        } catch (_: Exception) {
            // Non-blocking sync
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun sendPhoneOtpCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        
        if (forceResendingToken != null) {
            builder.setForceResendingToken(forceResendingToken)
        }
        
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    suspend fun verifyOtpAndLinkPhone(verificationId: String, code: String, phoneNumber: String): Result<Unit> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val user = auth.currentUser
            if (user != null) {
                try {
                    user.updatePhoneNumber(credential).await()
                } catch (_: Exception) {
                    try {
                        user.linkWithCredential(credential).await()
                    } catch (_: Exception) {
                        // Ignore if already linked
                    }
                }
                syncPhoneVerificationToDb(user.uid, phoneNumber)
                Result.success(Unit)
            } else {
                val result = auth.signInWithCredential(credential).await()
                val newUser = result.user
                if (newUser != null) {
                    syncUserToRealtimeDatabase(newUser, "phone")
                    syncPhoneVerificationToDb(newUser.uid, phoneNumber)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthErrorMessage(e)))
        }
    }

    private suspend fun syncPhoneVerificationToDb(uid: String, phoneNumber: String) {
        try {
            val db = try {
                FirebaseDatabase.getInstance("https://drigo-8b15c-default-rtdb.firebaseio.com")
            } catch (_: Exception) {
                FirebaseDatabase.getInstance()
            }
            val userRef = db.getReference("users").child(uid)
            val updateData = mapOf<String, Any>(
                "phone" to phoneNumber,
                "phoneNumber" to phoneNumber,
                "phoneVerified" to true,
                "isPhoneVerified" to true,
                "phoneVerifiedAt" to System.currentTimeMillis()
            )
            userRef.updateChildren(updateData).await()
        } catch (_: Exception) {
            // Non-blocking
        }
    }

    private fun formatAuthErrorMessage(e: Exception): String {
        val rawMsg = e.localizedMessage ?: e.message ?: ""
        val errorCode = (e as? FirebaseAuthException)?.errorCode ?: ""

        // Check for invalid credential / wrong password / credential expired / user not found in v2
        if (errorCode == "ERROR_INVALID_CREDENTIAL" ||
            errorCode == "INVALID_LOGIN_CREDENTIALS" ||
            errorCode == "ERROR_WRONG_PASSWORD" ||
            rawMsg.contains("incorrect, malformed or has expired", ignoreCase = true) ||
            rawMsg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            rawMsg.contains("invalid credential", ignoreCase = true) ||
            rawMsg.contains("RecaptchaAction", ignoreCase = true) ||
            e is FirebaseAuthInvalidCredentialsException
        ) {
            return "Incorrect email or password, or this account does not exist yet. Please verify your details or sign up."
        }

        if (errorCode == "ERROR_USER_NOT_FOUND" ||
            e is FirebaseAuthInvalidUserException ||
            rawMsg.contains("no user record", ignoreCase = true) ||
            rawMsg.contains("user-not-found", ignoreCase = true)
        ) {
            return "No account found with this email. Please sign up to create your Drigo account."
        }

        if (errorCode == "ERROR_EMAIL_ALREADY_IN_USE" ||
            e is FirebaseAuthUserCollisionException ||
            rawMsg.contains("email-already-in-use", ignoreCase = true)
        ) {
            return "An account with this email already exists. Please sign in instead."
        }

        if (errorCode == "ERROR_WEAK_PASSWORD" ||
            e is FirebaseAuthWeakPasswordException ||
            rawMsg.contains("weak-password", ignoreCase = true)
        ) {
            return "The password is too weak. Please use at least 6 characters."
        }

        if (errorCode == "ERROR_TOO_MANY_REQUESTS" ||
            rawMsg.contains("too-many-requests", ignoreCase = true)
        ) {
            return "Too many unsuccessful attempts. Please wait a few moments and try again."
        }

        if (errorCode == "ERROR_USER_DISABLED" ||
            rawMsg.contains("user-disabled", ignoreCase = true)
        ) {
            return "This account has been disabled. Please contact Drigo support."
        }

        if (errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
            rawMsg.contains("operation-not-allowed", ignoreCase = true)
        ) {
            return "This sign-in method is temporarily unavailable. Please try Google sign-in or continue as guest."
        }

        if (e is FirebaseNetworkException || rawMsg.contains("network", ignoreCase = true)) {
            return "Network error. Please check your internet connection and try again."
        }

        return if (rawMsg.isNotBlank()) rawMsg else "Authentication failed. Please try again."
    }
}
