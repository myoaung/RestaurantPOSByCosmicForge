package com.cosmicforge.pos

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
    }
}
