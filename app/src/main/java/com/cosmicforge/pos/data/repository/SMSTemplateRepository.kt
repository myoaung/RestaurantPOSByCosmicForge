package com.cosmicforge.pos.data.repository

import com.cosmicforge.pos.data.database.dao.SMSTemplateDao
import com.cosmicforge.pos.data.database.entities.SMSTemplateEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for SMS template management
 */
@Singleton
class SMSTemplateRepository @Inject constructor(
    private val smsTemplateDao: SMSTemplateDao
) {
    
    /**
     * Get all templates
     */
    fun getAllTemplates(): Flow<List<SMSTemplateEntity>> {
        return smsTemplateDao.getAllTemplates()
    }
    
    /**
     * Get template by type
     */
    suspend fun getTemplate(templateType: String): SMSTemplateEntity? {
        return smsTemplateDao.getTemplate(templateType)
    }
    
    /**
     * Update template
     */
    suspend fun updateTemplate(
        templateType: String,
        newText: String,
        updatedBy: String
    ) {
        val template = SMSTemplateEntity(
            templateType = templateType,
            templateText = newText,
            updatedAt = System.currentTimeMillis(),
            updatedBy = updatedBy
        )
        smsTemplateDao.updateTemplate(template)
    }
    
    /**
     * Initialize default templates if not exist
     */
    suspend fun initializeDefaultTemplates() {
        val existing = smsTemplateDao.getAllTemplatesSnapshot()
        if (existing.isEmpty()) {
            smsTemplateDao.insertTemplates(SMSTemplateEntity.getDefaultTemplates())
        }
    }
    
    /**
     * Reset template to default
     */
    suspend fun resetToDefault(templateType: String) {
        val defaultTemplate = SMSTemplateEntity.getDefaultTemplates()
            .find { it.templateType == templateType }
        
        defaultTemplate?.let {
            smsTemplateDao.insertTemplate(it)
        }
    }
    
    /**
     * Replace variables in template
     */
    fun replaceVariables(
        template: String,
        variables: Map<String, String>
    ): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace(key, value)
        }
        return result
    }
}
