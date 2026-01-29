package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.SMSTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SMSTemplateDao {
    
    @Query("SELECT * FROM sms_templates WHERE template_type = :templateType")
    suspend fun getTemplate(templateType: String): SMSTemplateEntity?
    
    @Query("SELECT * FROM sms_templates")
    fun getAllTemplates(): Flow<List<SMSTemplateEntity>>
    
    @Query("SELECT * FROM sms_templates")
    suspend fun getAllTemplatesSnapshot(): List<SMSTemplateEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SMSTemplateEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<SMSTemplateEntity>)
    
    @Update
    suspend fun updateTemplate(template: SMSTemplateEntity)
    
    @Query("DELETE FROM sms_templates WHERE template_type = :templateType")
    suspend fun deleteTemplate(templateType: String)
}
