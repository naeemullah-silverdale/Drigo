package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Audio and Text-to-Speech Assistant for Driver Mode.
 * Announces incoming ride requests, turn-by-turn navigation alerts, and safety chimes hands-free.
 * Engineered for maximum stability on low-end budget Android devices (e.g. Samsung Galaxy A12).
 */
class DriverAudioHelper private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isVoiceEnabled = true
    private var voiceLanguage = "EN" // "EN" or "UR"
    private var toneGenerator: ToneGenerator? = null

    companion object {
        private const val TAG = "DriverAudioHelper"

        @Volatile
        private var INSTANCE: DriverAudioHelper? = null

        fun getInstance(context: Context): DriverAudioHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DriverAudioHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (t: Throwable) {
            Log.e(TAG, "Error initializing TTS: ${t.message}", t)
            tts = null
        }

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (t: Throwable) {
            Log.e(TAG, "Error initializing ToneGenerator: ${t.message}", t)
            toneGenerator = null
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
            tts?.setSpeechRate(1.02f)
            tts?.setPitch(1.0f)
        } else {
            isTtsReady = false
        }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        isVoiceEnabled = enabled
    }

    fun isVoiceEnabled(): Boolean = isVoiceEnabled

    fun setVoiceLanguage(lang: String) {
        voiceLanguage = if (lang.equals("UR", ignoreCase = true)) "UR" else "EN"
    }

    fun getVoiceLanguage(): String = voiceLanguage

    // State Tracking for Active Ride Updates (Strict single active ride deduping)
    private var lastAnnouncedRideId: String? = null
    private var lastAnnouncedStatus: String? = null

    /**
     * Announces a critical status update exclusively for the single active ride.
     * Prevents repetitive announcements on UI recomposition or DB re-fetches.
     * Automatically clears queue and resets state if ride completes or cancels.
     */
    fun announceActiveRideStatus(
        rideId: String,
        status: String,
        customMessage: String? = null,
        farePkr: Int? = null,
        pickupTitle: String? = null,
        destTitle: String? = null
    ) {
        if (rideId.isBlank()) return

        val normalizedStatus = status.uppercase().trim()
        val isTerminal = normalizedStatus in setOf("COMPLETED", "CANCELLED", "REJECTED")

        // If status transition has already been announced for this specific ride, skip (dedupe)
        if (lastAnnouncedRideId == rideId && lastAnnouncedStatus == normalizedStatus && !isTerminal) {
            return
        }

        // Update tracking state
        lastAnnouncedRideId = rideId
        lastAnnouncedStatus = normalizedStatus

        if (!isVoiceEnabled) {
            if (isTerminal) clearQueueAndResetTracking()
            return
        }

        // Prepare context-aware speech text based on status & language
        val speechText = customMessage ?: when (normalizedStatus) {
            "REQUESTED", "NEW_REQUEST" -> {
                val cleanPickup = pickupTitle?.substringBefore(",")?.take(22) ?: "pickup"
                val cleanDest = destTitle?.substringBefore(",")?.take(22) ?: "destination"
                val fare = farePkr ?: 0
                if (voiceLanguage == "UR") {
                    "Nayi ride request. $cleanPickup say $cleanDest. Kiraya $fare rupay."
                } else {
                    "New ride request received. From $cleanPickup to $cleanDest. Fare $fare rupees."
                }
            }
            "ACCEPTED", "HEADING_TO_PICKUP", "CONFIRMED" -> {
                if (voiceLanguage == "UR") "Ride tayar hai. Pickup point ki taraf rawana hon."
                else "Ride confirmed. Heading to pickup point."
            }
            "ARRIVED", "DRIVER_ARRIVED" -> {
                if (voiceLanguage == "UR") "Aap pickup point per pohanch gaye hain. Waiting time shuru."
                else "You have arrived at pickup. Free waiting time started."
            }
            "IN_TRANSIT", "IN_TRIP" -> {
                if (voiceLanguage == "UR") "Safar shuru ho gaya hai. Manzil ki taraf rawana hon."
                else "Trip started. Proceed safely to destination."
            }
            "COMPLETED" -> {
                val fare = farePkr ?: 0
                if (fare > 0) {
                    if (voiceLanguage == "UR") "Safar mukammal. Sawari say $fare rupay wasool karein."
                    else "Ride completed. Please collect $fare rupees from passenger."
                } else {
                    if (voiceLanguage == "UR") "Safar mukammal ho gaya."
                    else "Ride completed."
                }
            }
            "CANCELLED" -> {
                if (voiceLanguage == "UR") "Ride mansookh ho gayi."
                else "Ride cancelled."
            }
            else -> customMessage ?: normalizedStatus
        }

        // Sound chime / vibration feedback
        when (normalizedStatus) {
            "REQUESTED", "NEW_REQUEST" -> {
                try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250) } catch (_: Exception) {}
                vibrateShort()
            }
            "ARRIVED", "DRIVER_ARRIVED" -> {
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350) } catch (_: Exception) {}
                vibrateShort()
            }
            "COMPLETED" -> {
                try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 400) } catch (_: Exception) {}
                vibrateShort()
            }
            "CANCELLED" -> {
                vibrateShort()
            }
        }

        // Speak via TTS
        if (isTtsReady && tts != null && speechText.isNotBlank()) {
            val queueMode = if (isTerminal) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_FLUSH
            tts?.speak(speechText, queueMode, null, "RIDE_${rideId}_${normalizedStatus}_${System.currentTimeMillis()}")
        }

        // If trip is completed or cancelled, reset tracking and clear any pending announcements
        if (isTerminal) {
            clearQueueAndResetTracking()
        }
    }

    /**
     * Clears any spoken announcement queue and resets active ride tracking state.
     */
    fun clearQueueAndResetTracking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        lastAnnouncedRideId = null
        lastAnnouncedStatus = null
    }

    fun playNewRequestAlert(pickupTitle: String, destinationTitle: String, farePkr: Int, requestId: String = "") {
        if (requestId.isNotBlank()) {
            announceActiveRideStatus(
                rideId = requestId,
                status = "REQUESTED",
                farePkr = farePkr,
                pickupTitle = pickupTitle,
                destTitle = destinationTitle
            )
            return
        }

        if (!isVoiceEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
        } catch (_: Exception) {}
        vibrateShort()

        if (isTtsReady && tts != null) {
            val cleanPickup = pickupTitle.substringBefore(",").take(22)
            val cleanDest = destinationTitle.substringBefore(",").take(22)
            val speechText = if (voiceLanguage == "UR") {
                "Nayi ride request. $cleanPickup say $cleanDest. Kiraya $farePkr rupay."
            } else {
                "New ride request received. From $cleanPickup to $cleanDest. Fare $farePkr rupees."
            }
            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "NEW_REQUEST_${System.currentTimeMillis()}")
        }
    }

    fun playPanicSiren() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
            vibrateLong()
        } catch (_: Exception) {}
    }

    fun playTestAlert() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            vibrateShort()
            if (isTtsReady && tts != null) {
                val testMsg = if (voiceLanguage == "UR") "Drigo audio test kamyab raha." else "Drigo audio alert system is working perfectly."
                tts?.speak(testMsg, TextToSpeech.QUEUE_FLUSH, null, "TEST_ALERT")
            }
        } catch (_: Exception) {}
    }

    fun speak(text: String) {
        if (!isVoiceEnabled) return
        if (isTtsReady && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "MSG_${System.currentTimeMillis()}")
        }
    }

    fun announceRideStatus(statusText: String) {
        speak(statusText)
    }

    fun speakCustom(message: String) {
        speak(message)
    }

    fun playArrivalChime(rideId: String = "") {
        if (rideId.isNotBlank()) {
            announceActiveRideStatus(
                rideId = rideId,
                status = "ARRIVED"
            )
            return
        }
        if (!isVoiceEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
            vibrateShort()
            if (isTtsReady) {
                val msg = if (voiceLanguage == "UR") "Aap pickup point per pohanch gaye hain. Waiting time shuru." else "You have arrived at pickup. Free waiting time started."
                tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "ARRIVED")
            }
        } catch (_: Exception) {}
    }

    fun playTripCompleteChime(fare: Int, rideId: String = "") {
        if (rideId.isNotBlank()) {
            announceActiveRideStatus(
                rideId = rideId,
                status = "COMPLETED",
                farePkr = fare
            )
            return
        }
        if (!isVoiceEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 400)
            vibrateShort()
            if (isTtsReady) {
                val msg = if (fare > 0) {
                    if (voiceLanguage == "UR") "Safar mukammal. Sawari say $fare rupay wasool karein." else "Ride completed. Please collect $fare rupees from passenger."
                } else {
                    if (voiceLanguage == "UR") "Safar mukammal ho gaya." else "Ride completed."
                }
                tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "COMPLETED")
            }
        } catch (_: Exception) {}
        clearQueueAndResetTracking()
    }

    private fun vibrateLong() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(800)
                }
            }
        } catch (_: Exception) {}
    }

    private fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(150)
                }
            }
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            toneGenerator?.release()
        } catch (_: Exception) {}
    }
}
