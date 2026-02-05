package com.cosmicforge.rms

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class with Hilt support
 */
@HiltAndroidApp
class CosmicForgeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize app-level components
        // SQLCipher initialization
        net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
        
        // Initialize 30-day retention worker (Phase 3)
        // Runs daily at 3 AM to cleanup old processed_messages
        com.cosmicforge.rms.core.workers.ProcessedMessagesCleanupWorker.schedule(this)
    }
}
