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
 */
class DriverAudioHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isVoiceEnabled = true
    private var voiceLanguage = "EN" // "EN" or "UR"
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 90)
        } catch (e: Exception) {
            Log.e("DriverAudioHelper", "Error initializing TTS/Audio: ${e.message}")
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

    fun playNewRequestAlert(pickupTitle: String, destinationTitle: String, farePkr: Int) {
        if (!isVoiceEnabled) return

        // 1. Play Tone Chime
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
        } catch (_: Exception) {}

        // 2. Vibrate phone
        vibrateShort()

        // 3. Spoken Voice Announcement (English or Roman Urdu)
        if (isTtsReady && tts != null) {
            val cleanPickup = pickupTitle.substringBefore(",").take(22)
            val cleanDest = destinationTitle.substringBefore(",").take(22)
            val speechText = if (voiceLanguage == "UR") {
                "Nayi ride request. $cleanPickup say $cleanDest. Kiraya $farePkr rupay."
            } else {
                "New ride request. From $cleanPickup to $cleanDest. Fare $farePkr rupees."
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

    fun playArrivalChime() {
        if (!isVoiceEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
            vibrateShort()
            if (isTtsReady) {
                tts?.speak("You have arrived at the pickup point. Free waiting time started.", TextToSpeech.QUEUE_FLUSH, null, "ARRIVED")
            }
        } catch (_: Exception) {}
    }

    fun playTripCompleteChime(fare: Int) {
        if (!isVoiceEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 400)
            vibrateShort()
            if (isTtsReady) {
                tts?.speak("Trip completed. Please collect $fare rupees cash from the passenger.", TextToSpeech.QUEUE_FLUSH, null, "COMPLETED")
            }
        } catch (_: Exception) {}
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
