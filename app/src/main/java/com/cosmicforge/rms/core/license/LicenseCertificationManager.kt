package com.cosmicforge.rms.core.license

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * License Certification Manager
 * 
 * Generates and manages production license certificates
 * Triggered upon first successful production sync
 */
@Singleton
class LicenseCertificationManager @Inject constructor(
    private val context: Context
) {
    
    private val Context.licenseDataStore by preferencesDataStore(name = "license_preferences")
    
    companion object {
        private val CERTIFICATE_ISSUED_KEY = stringPreferencesKey("certificate_issued_date")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val LICENSE_KEY = stringPreferencesKey("license_key")
        
        private const val TAG = "LicenseCertificationMgr"
    }
    
    /**
     * Check if license certificate has been issued
     */
    suspend fun isCertificateIssued(): Boolean {
        return context.licenseDataStore.data.map { prefs ->
            prefs[CERTIFICATE_ISSUED_KEY] != null
        }.first()
    }
    
    /**
     * Generate license certificate upon first production sync
     */
    suspend fun generateCertificate(deviceId: String): LicenseCertificate {
        val issuedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val licenseKey = generateLicenseKey(deviceId, issuedDate)
        
        // Store certificate data
        context.licenseDataStore.edit { prefs ->
            prefs[CERTIFICATE_ISSUED_KEY] = issuedDate
            prefs[DEVICE_ID_KEY] = deviceId
            prefs[LICENSE_KEY] = licenseKey
        }
        
        android.util.Log.d(TAG, "✅ License certificate generated: $licenseKey")
        
        return LicenseCertificate(
            deviceId = deviceId,
            issueDate = issuedDate,
            licenseKey = licenseKey,
            productName = "Cosmic Forge POS v9.0",
            licensedTo = "Restaurant Owner",
            validUntil = "2036-02-15" // 10 years from deployment
        )
    }
    
    /**
     * Get existing certificate
     */
    suspend fun getCertificate(): LicenseCertificate? {
        val prefs = context.licenseDataStore.data.first()
        val issuedDate = prefs[CERTIFICATE_ISSUED_KEY] ?: return null
        val deviceId = prefs[DEVICE_ID_KEY] ?: return null
        val licenseKey = prefs[LICENSE_KEY] ?: return null
        
        return LicenseCertificate(
            deviceId = deviceId,
            issueDate = issuedDate,
            licenseKey = licenseKey,
            productName = "Cosmic Forge POS v9.0",
            licensedTo = "Restaurant Owner",
            validUntil = "2036-02-15"
        )
    }
    
    /**
     * Generate unique license key
     */
    private fun generateLicenseKey(deviceId: String, issuedDate: String): String {
        val combined = "$deviceId-$issuedDate-COSMIC-FORGE"
        val hash = combined.hashCode().toString(16).uppercase()
        return "CF-${hash.take(4)}-${hash.takeLast(4)}-POS9"
    }
    
    /**
     * Observe certificate status
     */
    fun observeCertificateStatus(): Flow<Boolean> {
        return context.licenseDataStore.data.map { prefs ->
            prefs[CERTIFICATE_ISSUED_KEY] != null
        }
    }
}

/**
 * License Certificate Data Structure
 */
data class LicenseCertificate(
    val deviceId: String,
    val issueDate: String,
    val licenseKey: String,
    val productName: String,
    val licensedTo: String,
    val validUntil: String
) {
    /**
     * Generate printable certificate text
     */
    fun toCertificateText(): String {
        return """
            ╔════════════════════════════════════════════════════════════╗
            ║        COSMIC FORGE POS - LICENSE CERTIFICATE             ║
            ╠════════════════════════════════════════════════════════════╣
            ║                                                            ║
            ║  Product: $productName                         ║
            ║  Licensed To: $licensedTo                          ║
            ║                                                            ║
            ║  License Key: $licenseKey                  ║
            ║  Device ID: ${deviceId.take(20)}...                    ║
            ║                                                            ║
            ║  Issue Date: $issueDate                        ║
            ║  Valid Until: $validUntil                          ║
            ║                                                            ║
            ║  This certificate confirms that this device is            ║
            ║  authorized to run Cosmic Forge POS software.             ║
            ║                                                            ║
            ║  🛸 Antigravity Protocol Certified                         ║
            ║                                                            ║
            ╚════════════════════════════════════════════════════════════╝
        """.trimIndent()
    }
}
