package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SMS template entity for customizable messages
 */
@Entity(tableName = "sms_templates")
data class SMSTemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "template_type")
    val templateType: String, // RESERVATION, THANK_YOU, ORDER_READY
    
    @ColumnInfo(name = "template_text")
    val templateText: String,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_by")
    val updatedBy: String? = null
) {
    companion object {
        const val TYPE_RESERVATION = "RESERVATION"
        const val TYPE_THANK_YOU = "THANK_YOU"
        const val TYPE_ORDER_READY = "ORDER_READY"
        
        /**
         * Default templates with Myanmar Unicode
         */
        fun getDefaultTemplates(): List<SMSTemplateEntity> {
            return listOf(
                SMSTemplateEntity(
                    templateType = TYPE_RESERVATION,
                    templateText = """
                        🍽️ {shop_name}
                        
                        ကြိုဆိုပါတယ် {customer_name}!
                        
                        စားပွဲနံပါတ်: {table}
                        ရက်စွဲ: {date_time}
                        
                        ကျေးဇူးတင်ပါတယ်။
                    """.trimIndent()
                ),
                SMSTemplateEntity(
                    templateType = TYPE_THANK_YOU,
                    templateText = """
                        🙏 ကျေးဇူးတင်ပါတယ်!
                        
                        {customer_name}
                        
                        အော်ဒါနံပါတ်: {order_number}
                        စုစုပေါင်း: {total} ကျပ်
                        
                        နောက်တစ်ကြိမ် ထပ်လာရောက်ပါနော်။
                        
                        {shop_name}
                    """.trimIndent()
                ),
                SMSTemplateEntity(
                    templateType = TYPE_ORDER_READY,
                    templateText = """
                        ✅ အော်ဒါအဆင်သင့်ဖြစ်ပါပြီ
                        
                        {customer_name}
                        အော်ဒါ: {order_number}
                        
                        လာယူနိုင်ပါပြီခင်ဗျာ။
                        
                        {shop_name}
                    """.trimIndent()
                )
            )
        }
        
        /**
         * Available variables for each template type
         */
        fun getAvailableVariables(templateType: String): List<String> {
            return when (templateType) {
                TYPE_RESERVATION -> listOf(
                    "{shop_name}",
                    "{customer_name}",
                    "{table}",
                    "{date_time}"
                )
                TYPE_THANK_YOU -> listOf(
                    "{shop_name}",
                    "{customer_name}",
                    "{order_number}",
                    "{total}"
                )
                TYPE_ORDER_READY -> listOf(
                    "{shop_name}",
                    "{customer_name}",
                    "{order_number}"
                )
                else -> emptyList()
            }
        }
    }
}
