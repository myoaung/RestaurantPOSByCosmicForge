package com.cosmicforge.pos.core.sms

import android.telephony.SmsManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMS Manager with Myanmar Unicode support
 */
@Singleton
class SMSManager @Inject constructor(
    private val templateRepository: SMSTemplateRepository
) {
    
    /**
     * Send reservation confirmation SMS
     */
    suspend fun sendReservationSMS(
        phoneNumber: String,
        shopName: String,
        customerName: String,
        tableNumber: String,
        dateTime: String
    ): SMSResult {
        val template = templateRepository.getTemplate(SMSTemplateEntity.TYPE_RESERVATION)
            ?: return SMSResult.Failure("Template not found")
        
        val message = templateRepository.replaceVariables(
            template.templateText,
            mapOf(
                "{shop_name}" to shopName,
                "{customer_name}" to customerName,
                "{table}" to tableNumber,
                "{date_time}" to dateTime
            )
        )
        
        return sendSMS(phoneNumber, message)
    }
    
    /**
     * Send thank you SMS after order
     */
    suspend fun sendThankYouSMS(
        phoneNumber: String,
        shopName: String,
        customerName: String,
        orderNumber: String,
        totalAmount: Double
    ): SMSResult {
        val template = templateRepository.getTemplate(SMSTemplateEntity.TYPE_THANK_YOU)
            ?: return SMSResult.Failure("Template not found")
        
        val message = templateRepository.replaceVariables(
            template.templateText,
            mapOf(
                "{shop_name}" to shopName,
                "{customer_name}" to customerName,
                "{order_number}" to orderNumber,
                "{total}" to totalAmount.toInt().toString()
            )
        )
        
        return sendSMS(phoneNumber, message)
    }
    
    /**
     * Send order ready notification
     */
    suspend fun sendOrderReadySMS(
        phoneNumber: String,
        shopName: String,
        customerName: String,
        orderNumber: String
    ): SMSResult {
        val template = templateRepository.getTemplate(SMSTemplateEntity.TYPE_ORDER_READY)
            ?: return SMSResult.Failure("Template not found")
        
        val message = templateRepository.replaceVariables(
            template.templateText,
            mapOf(
                "{shop_name}" to shopName,
                "{customer_name}" to customerName,
                "{order_number}" to orderNumber
            )
        )
        
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
