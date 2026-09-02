package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Direct Google Apps Script Web App Uploader client.
 * Uploads Base64-encoded image payloads directly to the Google Apps Script Web App endpoint,
 * which saves the files into the "Driver Registrations" folder in Google Drive without storage quota errors.
 */
object GoogleAppsScriptUploader {
    private const val TAG = "AppsScriptUploader"

    // Google Apps Script Web App Execution Endpoint
    const val APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzOXnsW6737pOCNEdVM7PLGw67Npn2GNYq6cHFjM_9DJXwr-a1gLV43_d-tdz85vjuZxA/exec"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Compresses the image URI into a JPEG byte array to optimize upload payload size.
     */
    fun compressImageToBytes(context: Context, uri: Uri, maxDimension: Int = 1280, quality: Int = 85): ByteArray? {
        var inputStream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open stream for URI: $uri", e)
            null
        } ?: return null

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var sampleSize = 1
            while ((options.outWidth / sampleSize) > maxDimension || (options.outHeight / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()

            if (bitmap != null) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                val bytes = baos.toByteArray()
                bitmap.recycle()
                bytes
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
            try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Constructs a POST request targeting the Google Apps Script endpoint with Base64 payload,
     * executes asynchronously on Dispatchers.IO, and parses the returned JSON fileUrl response.
     */
    suspend fun uploadImageToAppsScript(
        context: Context,
        uri: Uri,
        filename: String,
        onProgress: (Float) -> Unit = {}
    ): Result<com.example.data.model.GoogleDriveFileRecord> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.10f)
            Log.d(TAG, "Preparing image upload for filename: $filename")

            // 1. Read and compress image bytes
            val bytes = compressImageToBytes(context, uri)
                ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

            if (bytes == null || bytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Unable to read image bytes from URI"))
            }

            onProgress(0.30f)

            // 2. Encode to Base64
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
            onProgress(0.45f)

            // 3. Construct JSON Payload
            val jsonPayload = JSONObject().apply {
                put("image", base64Image)
                put("filename", filename)
            }.toString()

            onProgress(0.55f)

            // 4. Build HTTP POST Request with Content-Type: application/json
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(APPS_SCRIPT_URL)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            onProgress(0.70f)

            // 5. Execute HTTP POST call asynchronously on Dispatchers.IO
            Log.d(TAG, "Executing POST request to Google Apps Script URL: $APPS_SCRIPT_URL")
            val response = httpClient.newCall(request).execute()

            onProgress(0.85f)

            // Read raw response body as string
            val responseBody = response.body?.string()?.trim() ?: ""

            // Log raw response body before attempting to parse into JSONObject
            Log.d(TAG, "Raw response body from Google Apps Script [HTTP ${response.code}]: $responseBody")

            if (responseBody.isBlank()) {
                val errorMsg = "Empty response received from Google Apps Script endpoint [HTTP ${response.code}]"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Check if response is HTML (e.g., <!DOCTYPE html> or <html>) instead of JSON
            if (responseBody.startsWith("<!DOCTYPE", ignoreCase = true) ||
                responseBody.startsWith("<html", ignoreCase = true) ||
                responseBody.contains("<head>", ignoreCase = true)
            ) {
                val isGoogleAuthRedirect = responseBody.contains("Google Accounts", ignoreCase = true) ||
                        responseBody.contains("ServiceLogin", ignoreCase = true) ||
                        responseBody.contains("Sign in", ignoreCase = true)

                val errorMessage = if (isGoogleAuthRedirect) {
                    "Google Apps Script returned an authentication redirect page. Please open your Google Apps Script project, click Deploy > Manage deployments > Edit, set 'Who has access' to 'Anyone', and click Deploy."
                } else {
                    "Google Apps Script returned an HTML page (DOCTYPE) instead of JSON. Please verify 'Who has access' is set to 'Anyone' and 'Execute as' is set to 'Me' in your Apps Script deployment settings."
                }
                Log.e(TAG, "HTML DOCTYPE response detected! Raw content: $responseBody")
                return@withContext Result.failure(Exception(errorMessage))
            }

            // 6. Parse JSON Response
            val jsonResponse = try {
                JSONObject(responseBody)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse response into JSONObject. Raw response body was: $responseBody", e)
                return@withContext Result.failure(Exception("Failed to parse Google Apps Script JSON response. Please ensure 'Who has access' is set to 'Anyone' in your Apps Script deployment."))
            }
            val status = jsonResponse.optString("status", "")

            if (status.equals("success", ignoreCase = true)) {
                val fileId = jsonResponse.optString("fileId", "")
                val fileUrl = jsonResponse.optString("fileUrl", "")

                if (fileUrl.isBlank()) {
                    return@withContext Result.failure(Exception("Apps Script returned success status but missing fileUrl"))
                }

                val record = com.example.data.model.GoogleDriveFileRecord(
                    fileId = fileId.ifBlank { "gas_${System.currentTimeMillis()}" },
                    fileName = filename,
                    mimeType = "image/jpeg",
                    webViewLink = fileUrl,
                    directDownloadUrl = fileUrl,
                    fileSize = bytes.size.toLong(),
                    uploadedAt = System.currentTimeMillis(),
                    docType = "DRIVER_DOCUMENT",
                    category = "Driver Registrations",
                    userId = "driver",
                    notes = "Uploaded via Google Apps Script Web App"
                )

                onProgress(1.0f)
                Log.d(TAG, "Successfully uploaded document via Apps Script: fileUrl=$fileUrl, fileId=$fileId")
                Result.success(record)
            } else {
                val errorMsg = jsonResponse.optString("message", "Google Apps Script error")
                Log.e(TAG, "Google Apps Script returned error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload image to Google Apps Script: ${e.message}", e)
            Result.failure(e)
        }
    }
}
