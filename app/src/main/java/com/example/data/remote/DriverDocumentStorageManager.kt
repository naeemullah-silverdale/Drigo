package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.DriverDocumentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Scalable and secure Driver Document Storage Manager using Google Drive as the EXCLUSIVE
 * physical file repository.
 *
 * Supported Credentials:
 * 1. Automatic Google Service Account with OAuth2 JWT Assertion (Zero-friction, instant upload)
 * 2. User Google Account with OAuth 2.0 Client ID (305850616039-shh7n4onls6c0bahv9jt0on6u3mr6op2)
 *
 * Folder Hierarchy in Google Drive:
 *   Drigo_Rideshare_Documents/
 *     Drivers/
 *       {driverId}/
 *         Profile/
 *         CNIC/
 *         Driving_License/
 *         Vehicle/
 *         Registration/
 *         Other/
 */
object DriverDocumentStorageManager {
    private const val TAG = "DriverDocStorage"

    // Supported Document Types
    const val DOC_CNIC_FRONT = "CNIC_FRONT"
    const val DOC_CNIC_BACK = "CNIC_BACK"
    const val DOC_LICENSE_FRONT = "LICENSE_FRONT"
    const val DOC_LICENSE_BACK = "LICENSE_BACK"
    const val DOC_DRIVER_PHOTO = "DRIVER_PHOTO"
    const val DOC_VEHICLE_FRONT = "VEHICLE_FRONT"
    const val DOC_VEHICLE_BACK = "VEHICLE_BACK"
    const val DOC_VEHICLE_SIDE = "VEHICLE_SIDE"
    const val DOC_VEHICLE_REGISTRATION = "VEHICLE_REGISTRATION"
    const val DOC_ADDITIONAL_DOC = "ADDITIONAL_DOC"

    fun getCategoryForDocType(docType: String): String {
        return when (docType) {
            DOC_DRIVER_PHOTO -> "profile"
            DOC_CNIC_FRONT, DOC_CNIC_BACK -> "cnic"
            DOC_LICENSE_FRONT, DOC_LICENSE_BACK -> "license"
            DOC_VEHICLE_FRONT, DOC_VEHICLE_BACK, DOC_VEHICLE_SIDE -> "vehicle"
            DOC_VEHICLE_REGISTRATION -> "registration"
            DOC_ADDITIONAL_DOC -> "other"
            else -> "other"
        }
    }

    fun getSubfolderNameForDocType(docType: String): String {
        return when (docType) {
            DOC_DRIVER_PHOTO -> "Profile"
            DOC_CNIC_FRONT, DOC_CNIC_BACK -> "CNIC"
            DOC_LICENSE_FRONT, DOC_LICENSE_BACK -> "Driving_License"
            DOC_VEHICLE_FRONT, DOC_VEHICLE_BACK, DOC_VEHICLE_SIDE -> "Vehicle"
            DOC_VEHICLE_REGISTRATION -> "Registration"
            else -> "Other"
        }
    }

    fun getFileNameForDocType(docType: String): String {
        return when (docType) {
            DOC_DRIVER_PHOTO -> "driver_profile_photo.jpg"
            DOC_CNIC_FRONT -> "cnic_front.jpg"
            DOC_CNIC_BACK -> "cnic_back.jpg"
            DOC_LICENSE_FRONT -> "license_front.jpg"
            DOC_LICENSE_BACK -> "license_back.jpg"
            DOC_VEHICLE_FRONT -> "vehicle_front.jpg"
            DOC_VEHICLE_BACK -> "vehicle_back.jpg"
            DOC_VEHICLE_SIDE -> "vehicle_side.jpg"
            DOC_VEHICLE_REGISTRATION -> "vehicle_registration.jpg"
            DOC_ADDITIONAL_DOC -> "additional_verification_doc.jpg"
            else -> "${docType.lowercase(Locale.US)}.jpg"
        }
    }

    fun getTitleForDocType(docType: String): String {
        return when (docType) {
            DOC_DRIVER_PHOTO -> "Driver Profile Photo"
            DOC_CNIC_FRONT -> "CNIC / National ID (Front)"
            DOC_CNIC_BACK -> "CNIC / National ID (Back)"
            DOC_LICENSE_FRONT -> "Driving License (Front)"
            DOC_LICENSE_BACK -> "Driving License (Back)"
            DOC_VEHICLE_FRONT -> "Vehicle (Front View)"
            DOC_VEHICLE_BACK -> "Vehicle (Back View)"
            DOC_VEHICLE_SIDE -> "Vehicle (Side View)"
            DOC_VEHICLE_REGISTRATION -> "Vehicle Registration Card / Book"
            DOC_ADDITIONAL_DOC -> "Additional Verification Document"
            else -> docType.replace("_", " ")
        }
    }

    fun isRequiredDoc(docType: String): Boolean {
        return docType != DOC_ADDITIONAL_DOC
    }

    /**
     * Sanitizes inputs to prevent directory traversal.
     */
    fun sanitizeDriverId(driverId: String): String {
        return driverId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64).ifBlank { "driver_unknown" }
    }

    /**
     * Uploads the document exclusively to Google Drive.
     * Automatically authenticates via Google Service Account or user Google Sign-In account.
     * Deletes the old file in Google Drive if oldDocItem is supplied (document replacement).
     */
    suspend fun uploadDocument(
        context: Context,
        driverId: String,
        docType: String,
        uri: Uri,
        googleDriveManager: GoogleDriveStorageManager? = null,
        oldDocItem: DriverDocumentItem? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<DriverDocumentItem> = withContext(Dispatchers.IO) {
        try {
            val safeDriverId = sanitizeDriverId(driverId)
            val category = getCategoryForDocType(docType)
            val subfolder = getSubfolderNameForDocType(docType)
            val fileName = getFileNameForDocType(docType)

            onProgress(0.05f)

            // 1. First try uploading via Google Apps Script endpoint
            val fullFileName = "${safeDriverId}_${fileName}"
            val scriptResult = GoogleAppsScriptUploader.uploadImageToAppsScript(
                context = context,
                uri = uri,
                filename = fullFileName,
                onProgress = onProgress
            )

            val record = if (scriptResult.isSuccess) {
                scriptResult.getOrThrow()
            } else {
                // Fallback to activeDriveManager if Apps Script endpoint fails
                Log.w(TAG, "Google Apps Script upload failed: ${scriptResult.exceptionOrNull()?.message}. Attempting fallback...")
                val activeDriveManager = if (googleDriveManager != null) {
                    googleDriveManager
                } else {
                    val account = GoogleDriveAuthHelper.getLastSignedInAccount(context)
                    if (account != null && GoogleDriveAuthHelper.hasDrivePermissions(context, account)) {
                        GoogleDriveStorageManager(context).apply {
                            init(context, account)
                        }
                    } else {
                        GoogleDriveStorageManager(context)
                    }
                }
                val oldFileId = oldDocItem?.googleDriveFileId?.ifBlank { null }
                val driveResult = activeDriveManager.uploadFileToDrive(
                    fileUri = uri,
                    customFileName = fullFileName,
                    docType = docType,
                    category = category,
                    driverId = safeDriverId,
                    oldDriveFileId = oldFileId,
                    notes = "Driver verification: ${getTitleForDocType(docType)}",
                    onProgress = onProgress
                )
                if (driveResult.isFailure) {
                    val err = scriptResult.exceptionOrNull() ?: driveResult.exceptionOrNull() ?: Exception("Failed to upload document")
                    Log.e(TAG, "All upload attempts failed: ${err.message}", err)
                    return@withContext Result.failure(err)
                }
                driveResult.getOrThrow()
            }

            val storagePath = "Driver Registrations/$safeDriverId/$subfolder/${record.fileName}"

            val docItem = DriverDocumentItem(
                docType = docType,
                title = getTitleForDocType(docType),
                category = category,
                storagePath = storagePath,
                fileUrl = record.webViewLink.ifBlank { record.directDownloadUrl },
                fileType = record.mimeType,
                fileSize = record.fileSize,
                uploadedAt = record.uploadedAt,
                driverId = safeDriverId,
                isRequired = isRequiredDoc(docType),
                status = "PENDING",
                storageProvider = "GOOGLE_APPS_SCRIPT",
                googleDriveFileId = record.fileId,
                googleDriveWebViewLink = record.webViewLink,
                driveFolderId = record.driveFolderId,
                fileName = record.fileName
            )

            onProgress(1.0f)
            Log.d(TAG, "Document successfully uploaded to Google Drive: $storagePath (File ID: ${record.fileId})")
            Result.success(docItem)
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadDocument: ${e.message}", e)
            Result.failure(e)
        }
    }
}
