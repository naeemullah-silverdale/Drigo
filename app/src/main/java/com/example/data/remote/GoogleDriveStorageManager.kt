package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.GoogleDriveFileRecord
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Google Drive REST API v3 Manager for direct file uploads to Google Drive.
 * Supports:
 * 1. Automatic Service Account authentication (zero manual login needed)
 * 2. User interactive Google Sign-In with OAuth 2.0 Client
 * 3. Hierarchical folder management:
 *    Drigo_Rideshare_Documents/
 *      Drivers/
 *        {driverId}/
 *          Profile/
 *          CNIC/
 *          Driving_License/
 *          Vehicle/
 *          Registration/
 *          Other/
 * 4. Image compression (RGB_565, max dimension 1280px) for low-end 2GB/3GB RAM phones
 * 5. Automatic shareable permissions so links can be viewed in-app and by Admins
 */
class GoogleDriveStorageManager(private var appContext: Context? = null) {

    companion object {
        private const val TAG = "GoogleDriveManager"
        const val DEFAULT_ROOT_FOLDER = "Drigo_Rideshare_Documents"
        const val DRIVERS_FOLDER = "Drivers"
    }

    private var driveService: Drive? = null
    private var currentAccount: GoogleSignInAccount? = null
    private val folderCache = ConcurrentHashMap<String, String>()

    val isInitialized: Boolean
        get() = driveService != null

    val userEmail: String?
        get() = currentAccount?.email ?: GoogleDriveCredentials.SERVICE_ACCOUNT_EMAIL

    val displayName: String?
        get() = currentAccount?.displayName ?: "Drigo Drive Storage"

    /**
     * Initializes the Drive REST API v3 service from a signed-in Google account.
     */
    fun init(context: Context, account: GoogleSignInAccount) {
        this.appContext = context.applicationContext
        this.currentAccount = account
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context.applicationContext,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE)
            ).apply {
                selectedAccount = account.account
            }

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Drigo Rideshare")
                .build()

            Log.d(TAG, "Google Drive service initialized with user account: ${account.email}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Drive service with user account: ${e.message}", e)
            driveService = null
        }
    }

    /**
     * Initializes or gets the Drive REST API v3 service using Service Account or user credentials.
     */
    suspend fun getOrInitDriveService(): Drive? = withContext(Dispatchers.IO) {
        if (driveService != null) return@withContext driveService

        // 1. Try initializing with signed-in user Google account if available
        val ctx = appContext
        if (ctx != null) {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(ctx)
            if (account != null) {
                init(ctx, account)
                if (driveService != null) {
                    Log.d(TAG, "Google Drive service initialized using signed-in account: ${account.email}")
                    return@withContext driveService
                }
            }
        }

        // 2. Try service account authentication as fallback
        val tokenRes = GoogleDriveCredentials.getServiceAccountAccessToken()
        if (tokenRes.isSuccess) {
            val token = tokenRes.getOrThrow()
            val httpRequestInitializer = HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $token"
                request.connectTimeout = 30000
                request.readTimeout = 30000
            }

            val service = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                httpRequestInitializer
            )
                .setApplicationName("Drigo Rideshare")
                .build()

            driveService = service
            Log.d(TAG, "Google Drive service initialized automatically via Service Account")
            return@withContext service
        } else {
            Log.e(TAG, "Failed to initialize service account token: ${tokenRes.exceptionOrNull()?.message}")
            return@withContext null
        }
    }

    fun initializeService(account: GoogleSignInAccount) {
        val ctx = appContext ?: return
        init(ctx, account)
    }

    fun disconnect() {
        driveService = null
        currentAccount = null
        folderCache.clear()
    }

    /**
     * Gets or creates a subfolder under an optional parent folder ID in Google Drive.
     * Caches folder IDs to avoid redundant network round-trips.
     */
    suspend fun getOrCreateSubfolder(
        parentFolderId: String?,
        folderName: String
    ): String? = withContext(Dispatchers.IO) {
        val service = getOrInitDriveService() ?: return@withContext null
        val cacheKey = "${parentFolderId ?: "root"}/$folderName"
        folderCache[cacheKey]?.let { return@withContext it }

        try {
            val parentQuery = if (parentFolderId.isNullOrBlank()) "'root' in parents" else "'$parentFolderId' in parents"
            val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and $parentQuery and trashed = false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val existingFolder = result.files.firstOrNull()
            if (existingFolder != null) {
                folderCache[cacheKey] = existingFolder.id
                return@withContext existingFolder.id
            }

            // Create folder if not found
            val folderMetadata = File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                if (!parentFolderId.isNullOrBlank()) {
                    parents = listOf(parentFolderId)
                }
            }

            val createdFolder = service.files().create(folderMetadata)
                .setFields("id, name")
                .execute()

            // Make folder accessible
            try {
                val perm = Permission().apply {
                    type = "anyone"
                    role = "reader"
                }
                service.permissions().create(createdFolder.id, perm).execute()
            } catch (_: Exception) {}

            Log.d(TAG, "Created folder '$folderName' with ID: ${createdFolder.id}")
            folderCache[cacheKey] = createdFolder.id
            createdFolder.id
        } catch (e: Exception) {
            Log.e(TAG, "Error finding/creating folder '$folderName': ${e.message}", e)
            null
        }
    }

    /**
     * Creates and resolves the hierarchical folder path:
     * Drigo_Rideshare_Documents / Drivers / {driverId} / {categoryFolder}
     */
    suspend fun getOrCreateDriverFolderHierarchy(
        driverId: String,
        category: String
    ): String? = withContext(Dispatchers.IO) {
        val safeDriverId = driverId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64).ifBlank { "driver_unknown" }
        val rootId = getOrCreateSubfolder(null, DEFAULT_ROOT_FOLDER) ?: return@withContext null
        val driversId = getOrCreateSubfolder(rootId, DRIVERS_FOLDER) ?: return@withContext rootId
        val driverFolderId = getOrCreateSubfolder(driversId, safeDriverId) ?: return@withContext driversId

        val subfolderName = when (category.lowercase()) {
            "profile" -> "Profile"
            "cnic", "identity" -> "CNIC"
            "driving_license", "license" -> "Driving_License"
            "vehicle" -> "Vehicle"
            "registration" -> "Registration"
            else -> "Other"
        }

        getOrCreateSubfolder(driverFolderId, subfolderName) ?: driverFolderId
    }

    /**
     * Memory-efficient downsampling and compression for images to prevent OOM on 2GB-4GB RAM phones.
     */
    private fun processAndCompressImage(context: Context, uri: Uri, maxDimension: Int = 1280): ByteArray? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var sampleSize = 1
            while ((options.outWidth / sampleSize) > maxDimension || (options.outHeight / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Low RAM footprint
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
                val bytes = outputStream.toByteArray()
                bitmap.recycle()
                return bytes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image: ${e.message}", e)
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Detects MIME type safely from Context ContentResolver or filename extension.
     */
    private fun detectMimeType(context: Context, uri: Uri, fileName: String): String {
        val typeFromResolver = try {
            context.contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        if (!typeFromResolver.isNullOrBlank()) return typeFromResolver

        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".pdf") -> "application/pdf"
            lowerName.endsWith(".png") -> "image/png"
            lowerName.endsWith(".webp") -> "image/webp"
            lowerName.endsWith(".mp4") -> "video/mp4"
            lowerName.endsWith(".mov") -> "video/quicktime"
            lowerName.endsWith(".avi") -> "video/x-msvideo"
            lowerName.endsWith(".doc") || lowerName.endsWith(".docx") -> "application/msword"
            else -> "image/jpeg"
        }
    }

    /**
     * Uploads any file Uri (Images, PDFs, Videos, Docs) directly to Google Drive via multipart REST API v3 request.
     * Places the file in the designated driver category subfolder.
     * Cleans up oldDriveFileId if replacing an existing file.
     * Generates persistent webViewLinks and direct download URLs.
     */
    suspend fun uploadFileToDrive(
        fileUri: Uri,
        customFileName: String? = null,
        docType: String = "GENERAL_IMAGE",
        category: String = "documents",
        driverId: String = "current_user",
        oldDriveFileId: String? = null,
        notes: String = "",
        onProgress: (Float) -> Unit = {}
    ): Result<GoogleDriveFileRecord> = withContext(Dispatchers.IO) {
        val service = getOrInitDriveService() ?: return@withContext Result.failure(
            IllegalStateException("Google Drive service could not be initialized.")
        )
        val ctx = appContext ?: return@withContext Result.failure(
            IllegalStateException("Context not initialized for Google Drive storage.")
        )

        try {
            onProgress(0.10f)

            // 1. Delete old file from Google Drive if this is a document replacement
            if (!oldDriveFileId.isNullOrBlank()) {
                try {
                    deleteFile(oldDriveFileId)
                    Log.d(TAG, "Replaced and removed old Drive file: $oldDriveFileId")
                } catch (de: Exception) {
                    Log.w(TAG, "Old file deletion non-blocking notice: ${de.message}")
                }
            }

            onProgress(0.20f)

            // 2. Resolve or create hierarchical driver folder in Google Drive
            val folderId = getOrCreateDriverFolderHierarchy(driverId, category)
            onProgress(0.35f)

            // 3. Resolve file name & MIME type
            val originalName = getFileNameFromUri(ctx, fileUri)
            val resolvedFileName = customFileName
                ?: originalName
                ?: "drigo_${docType.lowercase()}_${System.currentTimeMillis()}.jpg"

            val mimeType = detectMimeType(ctx, fileUri, resolvedFileName)

            // 4. Read bytes: If image, compress safely to save RAM/bandwidth; otherwise read stream directly
            val fileBytes = if (mimeType.startsWith("image/")) {
                val compressed = processAndCompressImage(ctx, fileUri)
                compressed ?: (ctx.contentResolver.openInputStream(fileUri)?.use { it.readBytes() } ?: ByteArray(0))
            } else {
                ctx.contentResolver.openInputStream(fileUri)?.use { it.readBytes() } ?: ByteArray(0)
            }

            if (fileBytes.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Could not read file data from URI: $fileUri")
                )
            }

            onProgress(0.55f)

            // 5. Build Google Drive File metadata
            val fileMetadata = File().apply {
                name = resolvedFileName
                this.mimeType = mimeType
                description = "Uploaded via Drigo Rideshare for Driver: $driverId, DocType: $docType, Category: $category"
                if (!folderId.isNullOrBlank()) {
                    parents = listOf(folderId)
                }
            }

            // 6. Content body
            val mediaContent = ByteArrayContent(mimeType, fileBytes)

            onProgress(0.75f)

            // 7. Execute Drive v3 multipart upload request
            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name, mimeType, webViewLink, webContentLink, thumbnailLink, size, parents")
                .execute()

            onProgress(0.85f)

            // 8. Make file readable so it can be previewed
            try {
                val perm = Permission().apply {
                    type = "anyone"
                    role = "reader"
                }
                service.permissions().create(uploadedFile.id, perm).execute()
            } catch (pe: Exception) {
                Log.w(TAG, "Non-blocking permission set notice: ${pe.message}")
            }

            onProgress(0.95f)

            val fileId = uploadedFile.id
            val webViewLink = uploadedFile.webViewLink ?: "https://drive.google.com/file/d/$fileId/view?usp=drivesdk"
            val webContentLink = uploadedFile.webContentLink ?: "https://drive.google.com/uc?export=download&id=$fileId"
            val directDownloadUrl = "https://lh3.googleusercontent.com/d/$fileId"
            val thumbnailLink = uploadedFile.thumbnailLink ?: directDownloadUrl

            val record = GoogleDriveFileRecord(
                fileId = fileId,
                fileName = uploadedFile.name ?: resolvedFileName,
                mimeType = uploadedFile.mimeType ?: mimeType,
                driveFolderId = folderId ?: "",
                webViewLink = webViewLink,
                webContentLink = webContentLink,
                directDownloadUrl = directDownloadUrl,
                thumbnailLink = thumbnailLink,
                fileSize = uploadedFile.getSize() ?: fileBytes.size.toLong(),
                userId = driverId,
                userEmail = userEmail ?: "",
                docType = docType,
                category = category,
                uploadedAt = System.currentTimeMillis(),
                notes = if (notes.isNotBlank()) notes else "Uploaded to Drive folder: $folderId"
            )

            onProgress(1.0f)
            Log.d(TAG, "Successfully uploaded to Google Drive! File ID: $fileId, Link: $webViewLink, Folder: $folderId")
            Result.success(record)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            val userFriendlyError = when {
                msg.contains("storageQuotaExceeded", ignoreCase = true) || msg.contains("Service Accounts do not have storage quota", ignoreCase = true) ->
                    "Google Drive storage connection needed. Please tap 'Connect Google Account' above to upload files."
                msg.contains("Failed to connect", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) || msg.contains("Unable to resolve host", ignoreCase = true) ->
                    "Network connection issue. Please check your internet connection and try again."
                else -> e.message ?: "Upload to Google Drive failed"
            }
            Log.e(TAG, "Error during Google Drive upload: ${e.message}", e)
            Result.failure(Exception(userFriendlyError, e))
        }
    }

    /**
     * Backward-compatible image upload method.
     */
    suspend fun uploadImageToDrive(
        imageUri: Uri,
        customFileName: String? = null,
        docType: String = "GENERAL_IMAGE",
        category: String = "documents",
        userId: String = "current_user",
        notes: String = "",
        folderName: String = DEFAULT_ROOT_FOLDER,
        onProgress: (Float) -> Unit = {}
    ): Result<GoogleDriveFileRecord> {
        return uploadFileToDrive(
            fileUri = imageUri,
            customFileName = customFileName,
            docType = docType,
            category = category,
            driverId = userId,
            oldDriveFileId = null,
            notes = notes,
            onProgress = onProgress
        )
    }

    /**
     * Deletes a file from Google Drive by its file ID.
     */
    suspend fun deleteFile(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val service = getOrInitDriveService() ?: return@withContext Result.failure(
            IllegalStateException("Google Drive service is not connected.")
        )
        try {
            service.files().delete(fileId).execute()
            Log.d(TAG, "Deleted file from Drive: $fileId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file $fileId: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }
}
