package com.example.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

/**
 * Helper for Google Sign-In with Google Drive REST API v3 scope (drive.file)
 * using the configured OAuth 2.0 Client ID.
 */
object GoogleDriveAuthHelper {

    val DRIVE_SCOPE = Scope(DriveScopes.DRIVE_FILE)
    val DRIVE_FULL_SCOPE = Scope(DriveScopes.DRIVE)

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(DRIVE_SCOPE, DRIVE_FULL_SCOPE)
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(context: Context): Intent {
        return getGoogleSignInClient(context).signInIntent
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account
    }

    fun hasDrivePermission(account: GoogleSignInAccount?): Boolean {
        return account != null && (GoogleSignIn.hasPermissions(account, DRIVE_SCOPE) || GoogleSignIn.hasPermissions(account, DRIVE_FULL_SCOPE))
    }

    fun hasDrivePermissions(context: Context, account: GoogleSignInAccount?): Boolean {
        return account != null && (GoogleSignIn.hasPermissions(account, DRIVE_SCOPE) || GoogleSignIn.hasPermissions(account, DRIVE_FULL_SCOPE))
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        getGoogleSignInClient(context).signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
