package com.example.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            user?.let { syncUserToRealtimeDatabase(it, "email_password") }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthErrorMessage(e)))
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            user?.let { syncUserToRealtimeDatabase(it, "email_password") }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthErrorMessage(e)))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
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

    private fun formatAuthErrorMessage(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password. Please verify and try again."
            is FirebaseAuthInvalidUserException -> "No account found with this email. Please sign up first."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please sign in instead."
            is FirebaseAuthWeakPasswordException -> "The password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException -> "Network error. Please check your internet connection and try again."
            else -> e.localizedMessage ?: "Authentication failed. Please try again."
        }
    }
}
