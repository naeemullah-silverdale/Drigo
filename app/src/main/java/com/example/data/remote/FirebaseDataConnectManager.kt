package com.example.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.dataconnect.ConnectorConfig
import com.google.firebase.dataconnect.FirebaseDataConnect
import com.google.firebase.dataconnect.getInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Thread-safe singleton managing Firebase Data Connect (Cloud SQL / PostgreSQL) integration.
 *
 * Configured Service Connector: us-south1/drigo-8b15c-service/default
 * Exposed State: isConnected, lastSyncTimestamp
 */
class FirebaseDataConnectManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    var dataConnect: FirebaseDataConnect? = null
        private set

    init {
        initializeDataConnect()
    }

    private fun initializeDataConnect() {
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
                val config = ConnectorConfig(
                    connector = CONNECTOR,
                    location = LOCATION,
                    serviceId = SERVICE_ID
                )
                dataConnect = FirebaseDataConnect.getInstance(config)
                _isConnected.value = true
                _lastSyncTimestamp.value = System.currentTimeMillis()
                Log.d(TAG, "FirebaseDataConnect initialized successfully ($LOCATION/$SERVICE_ID/$CONNECTOR)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize FirebaseDataConnect: ${e.message}", e)
                _isConnected.value = false
            }
        }
    }

    fun markSync() {
        _lastSyncTimestamp.value = System.currentTimeMillis()
    }

    companion object {
        private const val TAG = "FirebaseDataConnectMgr"
        private const val LOCATION = "us-south1"
        private const val SERVICE_ID = "drigo-8b15c-service"
        private const val CONNECTOR = "default"

        @Volatile
        private var instance: FirebaseDataConnectManager? = null

        fun getInstance(context: Context): FirebaseDataConnectManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseDataConnectManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
