package com.cosmicforge.pos.core.sms

import android.telephony.SmsManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMS Manager with Myanmar Unicode support
 */
@Singleton
class SMSManager @Inject constructor() {
    
    /**
     * Send reservation confirmation SMS
     */
    fun sendReservationSMS(
        phoneNumber: String,
        customerName: String,
        tableNumber: String,
        dateTime: String
    ): SMSResult {
        val message = buildString {
            appendLine("🍽️ Cosmic Forge POS")
            appendLine()
            appendLine("ကြိုဆိုပါတယ် $customerName!")
            appendLine()
            appendLine("စားပွဲနံပါတ်: $tableNumber")
            appendLine("ရက်စွဲ: $dateTime")
            appendLine()
            appendLine("ကျေးဇူးတင်ပါတယ်။")
        }
        
        return sendSMS(phoneNumber, message)
    }
    
    /**
     * Send thank you SMS after order
     */
    fun sendThankYouSMS(
        phoneNumber: String,
        customerName: String,
        orderNumber: String,
        totalAmount: Double
    ): SMSResult {
        val message = buildString {
            appendLine("🙏 ကျေးဇူးတင်ပါတယ်!")
            appendLine()
            appendLine("$customerName")
            appendLine()
            appendLine("အော်ဒါနံပါတ်: $orderNumber")
            appendLine("စုစုပေါင်း: ${totalAmount.toInt()} ကျပ်")
            appendLine()
            appendLine("နောက်တစ်ကြိမ် ထပ်လာရောက်ပါနော်။")
            appendLine()
            appendLine("Cosmic Forge POS")
        }
        
        return sendSMS(phoneNumber, message)
    }
    
    /**
     * Send order ready notification
     */
    fun sendOrderReadySMS(
        phoneNumber: String,
        customerName: String,
        orderNumber: String
    ): SMSResult {
        val message = buildString {
            appendLine("✅ အော်ဒါအဆင်သင့်ဖြစ်ပါပြီ")
            appendLine()
            appendLine("$customerName")
            appendLine("အော်ဒါ: $orderNumber")
            appendLine()
            appendLine("လာယူနိုင်ပါပြီခင်ဗျာ။")
            appendLine()
            appendLine("Cosmic Forge POS")
        }
        
        return sendSMS(phoneNumber, message)
    }
    
    /**
     * Send generic SMS with Myanmar Unicode support
     */
    private fun sendSMS(phoneNumber: String, message: String): SMSResult {
        return try {
            // Validate phone number
            if (!isValidMyanmarPhone(phoneNumber)) {
                return SMSResult.Failure("Invalid Myanmar phone number")
            }
            
            val smsManager = SmsManager.getDefault()
            
            // Split message if too long (Myanmar Unicode can be larger)
            val parts = smsManager.divideMessage(message)
            
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )
            }
            
            Log.d(TAG, "SMS sent successfully to $phoneNumber")
            SMSResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            SMSResult.Failure(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Validate Myanmar phone number
     * Formats: 09XXXXXXXXX or +959XXXXXXXXX
     */
    private fun isValidMyanmarPhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[\\s-]"), "")
        return cleanPhone.matches(Regex("^(09|\\+?959)\\d{7,9}$"))
    }
    
    /**
     * Format Myanmar phone for SMS
     */
    fun formatMyanmarPhone(phone: String): String {
        val cleanPhone = phone.replace(Regex("[\\s-]"), "")
        return when {
            cleanPhone.startsWith("+959") -> cleanPhone
            cleanPhone.startsWith("09") -> "+959${cleanPhone.substring(2)}"
            else -> cleanPhone
        }
    }
    
    companion object {
        private const val TAG = "SMSManager"
    }
}

/**
 * SMS send result
 */
sealed class SMSResult {
    object Success : SMSResult()
    data class Failure(val reason: String) : SMSResult()
}
