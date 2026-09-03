package com.example

import android.app.Application
import android.util.Log
import com.example.data.remote.FirebaseRepository
import com.example.util.DriverAudioHelper
import com.example.util.RideNotificationManager
import org.osmdroid.config.Configuration

class DrigoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            // 1. Initialize OsmDroid Configuration
            Configuration.getInstance().apply {
                load(this@DrigoApplication, getSharedPreferences("drigo_osm_prefs", MODE_PRIVATE))
                userAgentValue = packageName
            }
        } catch (t: Throwable) {
            Log.e("DrigoApp", "OsmDroid init error: ${t.message}")
        }

        try {
            // 2. Initialize FirebaseRepository with valid Application Context
            FirebaseRepository.getInstance(this)
        } catch (t: Throwable) {
            Log.e("DrigoApp", "FirebaseRepository init error: ${t.message}")
        }

        try {
            // 3. Initialize RideNotificationManager & Notification Channels
            RideNotificationManager.getInstance(this)
        } catch (t: Throwable) {
            Log.e("DrigoApp", "RideNotificationManager init error: ${t.message}")
        }

        try {
            // 4. Initialize DriverAudioHelper
            DriverAudioHelper.getInstance(this)
        } catch (t: Throwable) {
            Log.e("DrigoApp", "DriverAudioHelper init error: ${t.message}")
        }
    }
}
