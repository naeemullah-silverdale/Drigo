package com.example.data.remote

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

/**
 * Google Drive API & OAuth Credentials Configuration.
 * Contains both OAuth 2.0 Client credentials and Service Account credentials for seamless,
 * zero-friction Google Drive storage without requiring manual user sign-in.
 */
object GoogleDriveCredentials {
    private const val TAG = "GoogleDriveCredentials"

    // OAuth 2.0 Client ID for interactive Google Sign-In
    const val OAUTH_CLIENT_ID = "305850616039-shh7n4onls6c0bahv9jt0on6u3mr6op2.apps.googleusercontent.com"
    const val PROJECT_ID = "gen-lang-client-0204828489"

    // Service Account Credentials for automatic app-level Google Drive Storage
    const val SERVICE_ACCOUNT_EMAIL = "ais-gemini-key-e61db9c7865e4e0@305850616039.iam.gserviceaccount.com"
    const val PRIVATE_KEY_ID = "5960777f6f00faf2364e9ab7130e84f8d9c3f37e"
    const val TOKEN_URI = "https://oauth2.googleapis.com/token"

    private const val PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDG/iygrKWQuwo0\n" +
            "CFBrp9q9j/2w77BjKM1sMC/Sfkk1/XzFRZ0loj3KnvRLNPcynd4+FNSyUQtVuwYj\n" +
            "M/OM1+AG9AiP+vEv0vFGy52GreAYjQwlNPrrSbmSI/vwY1nM8Jlep61SoG6bA7qe\n" +
            "5uhwtOLeA7b9fkSoxCEJrzQRvfBQIAmo57a/M2D/fEHLSpS55VZhHMLw+KrXMTml\n" +
            "D77+KuOqj0gdTxTVzodRHITN7vMhjg7lMO3cVNkvrICbt7ZBeqyruMS5wu0Wko4f\n" +
            "auPBfq3OK0QtzD/rAu54IWIvC+Ri0xd33hPgA0Zw5XmsAiGG0OP7i5tjv/x1HIQh\n" +
            "1d0l0eCdAgMBAAECggEADu7pKMpRXHSk4IsVL0G3/nd3/tdl9x217h4JoLQdyLb0\n" +
            "A5aPTfvdw5Rn2d2o2rwSfw8vd7hG76aViykYEw7qPtYBGdOnq5fwHQjPvVkpaD5B\n" +
            "gJNNbJXU9BzOMy00LSyLHe7bE3MltG1fC74MCH/n8ehOdbnhliwDHLJwvQ9yIYO8\n" +
            "Guwq4p+UguTLv5+vF/v6aSsaZzDz/gjUqfpxV5EIOXN1S/sGc2nfQVVK9xIaj1Lj\n" +
            "ODutVYsnhH7iRY36LIlA9tp/GIWM0VD09IxocqqIBP5xvcbR7UhYtPz3KLbT6MzC\n" +
            "DGSp+wd6Jw4B7sgX0mqo7+1/MTNJA78vJVnm5N5DYQKBgQDh1RtZjvKPsHSH+ZSk\n" +
            "EMGuGzoF3XKSfwecwPqAWVNUV15QJEcqHMq8lucu23gIDlVY9JvPP0ANz6tecUoS\n" +
            "sTwZpxOSJejrQTD8PhZQ7pymC5bg6c07jVvL6893g54TGnHDEXXLyRB4QsLndIWk\n" +
            "PBEcS8sMrIC9C278OUsfivugcQKBgQDhkzjtpLid5DZbYkMr8Gc+Vvd4CAuQYqs8\n" +
            "4eiaKMzZGctnB1q+ZSABAcBOuBE+0lvg0Mr2uvfilRXNL32JnWidZ9UP26QUwK9U\n" +
            "d6MsD4zu4MEYxsg2kIcvLef7vfOTaZJp80iSOkAi60iLLvCTwHubuWBk3U6OoB9b\n" +
            "ToSRgu/Y7QKBgQCqWpRoCYIGTEiLbgTnglBn4tfxJqxPwA1g/N26Rieq3sEhWUxp\n" +
            "gxCYFIlN+nZgNVfCY7rJKmBKStZtxq3mzdWVUEheHcTG+gVWsgspf+WhB+sHvLZ5\n" +
            "btfJSxfUgu+SX4dja99dG04WOd5GtMo/KqSJoM8Jv3LZqC9PinSV1cScsQKBgQCT\n" +
            "NzTZCz6ddPnwq/FwtKV8rt80y/NYGLzur/TQ3z3JlicEJ22WgoLfhVgPR9uhZ0Il\n" +
            "N76uRZlPm8KttYe1jzBeQgxwHAU6sgH7G1U3hTKSF8aNt/WkfrBrHweeaPqVQzoV\n" +
            "wXukSzXeVDV0+H5zvxQsrk/1o+UH+gJc4aJtIMoNhQKBgGEWztAKhOeDDVtixJ0B\n" +
            "L24RM7uVisq6CAQuDEf3W9KHh1bxqpoJFWRcG5U/mP93HsGl28bTY+B+8C/G22vZ\n" +
            "pTqJRDUdPLvh+0MBO5PHKbm6r7qdz18ZtO4c8sVdSna5RoqBMG1DnIZjW6C+5JYW\na6b6WlUHzjQNortMTdrizEss\n" +
            "-----END PRIVATE KEY-----\n"

    // In-memory token cache
    private var cachedAccessToken: String? = null
    private var tokenExpiryTimeMs: Long = 0L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var parsedPrivateKey: PrivateKey? = null

    private fun getPrivateKey(): PrivateKey {
        parsedPrivateKey?.let { return it }
        val cleanKey = PRIVATE_KEY_PEM
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\r", "")
            .replace("\n", "")
            .trim()

        val keyBytes = Base64.decode(cleanKey, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val key = keyFactory.generatePrivate(keySpec)
        parsedPrivateKey = key
        return key
    }

    private fun createSignedJwt(): String {
        val privateKey = getPrivateKey()
        val nowSeconds = System.currentTimeMillis() / 1000
        val expSeconds = nowSeconds + 3600

        val headerJson = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
            put("kid", PRIVATE_KEY_ID)
        }.toString()

        val claimJson = JSONObject().apply {
            put("iss", SERVICE_ACCOUNT_EMAIL)
            put("scope", "https://www.googleapis.com/auth/drive https://www.googleapis.com/auth/drive.file")
            put("aud", TOKEN_URI)
            put("exp", expSeconds)
            put("iat", nowSeconds)
        }.toString()

        val base64Header = Base64.encodeToString(
            headerJson.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val base64Claim = Base64.encodeToString(
            claimJson.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val contentToSign = "$base64Header.$base64Claim"

        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(contentToSign.toByteArray(Charsets.UTF_8))
        }.sign()

        val base64Signature = Base64.encodeToString(
            signature,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        return "$contentToSign.$base64Signature"
    }

    /**
     * Obtains a valid Google Drive OAuth2 Access Token using the Service Account credentials.
     * Caches the token for 50 minutes.
     */
    suspend fun getServiceAccountAccessToken(forceRefresh: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedAccessToken != null && now < tokenExpiryTimeMs - (5 * 60 * 1000)) {
            return@withContext Result.success(cachedAccessToken!!)
        }

        try {
            val signedJwt = createSignedJwt()
            val requestBody = FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .add("assertion", signedJwt)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URI)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to get access token: HTTP ${response.code} - $bodyString")
                return@withContext Result.failure(
                    Exception("Failed to authenticate with Google Drive: HTTP ${response.code} ($bodyString)")
                )
            }

            val json = JSONObject(bodyString)
            val accessToken = json.getString("access_token")
            val expiresInSeconds = json.optLong("expires_in", 3600L)

            cachedAccessToken = accessToken
            tokenExpiryTimeMs = System.currentTimeMillis() + (expiresInSeconds * 1000)

            Log.d(TAG, "Successfully acquired Google Drive access token for $SERVICE_ACCOUNT_EMAIL")
            Result.success(accessToken)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching service account access token: ${e.message}", e)
            Result.failure(e)
        }
    }
}
