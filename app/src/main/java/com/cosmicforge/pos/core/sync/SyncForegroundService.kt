package com.cosmicforge.pos.core.sync

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cosmicforge.pos.MainActivity
import com.cosmicforge.pos.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service to keep P2P sync running in background
 */
@AndroidEntryPoint
class SyncForegroundService : Service() {
    
    @Inject
    lateinit var syncEngine: SyncEngine
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Sync service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Sync service started")
        
        when (intent?.action) {
            ACTION_START_SYNC -> {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, createNotification(), getForegroundServiceType())
                syncEngine.initialize()
                syncEngine.startSync()
            }
            ACTION_STOP_SYNC -> {
                syncEngine.stopSync()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Sync service destroyed")
        syncEngine.cleanup()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "P2P Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps devices synchronized"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cosmic Forge POS")
            .setContentText("Syncing with other devices...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun getForegroundServiceType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }
    }
    
    companion object {
        private const val TAG = "SyncForegroundService"
        private const val CHANNEL_ID = "sync_service_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_SYNC = "com.cosmicforge.pos.ACTION_START_SYNC"
        const val ACTION_STOP_SYNC = "com.cosmicforge.pos.ACTION_STOP_SYNC"
    }
}
