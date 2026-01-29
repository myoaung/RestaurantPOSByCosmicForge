package com.cosmicforge.rms.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.data.database.entities.SMSTemplateEntity
import com.cosmicforge.rms.data.repository.SMSTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SMS template editor
 */
@HiltViewModel
class SMSTemplateViewModel @Inject constructor(
    private val templateRepository: SMSTemplateRepository
) : ViewModel() {
    
    private val _templates = MutableStateFlow<List<SMSTemplateEntity>>(emptyList())
    val templates: StateFlow<List<SMSTemplateEntity>> = _templates.asStateFlow()
    
    private val _selectedTemplate = MutableStateFlow<SMSTemplateEntity?>(null)
    val selectedTemplate: StateFlow<SMSTemplateEntity?> = _selectedTemplate.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    init {
        loadTemplates()
        initializeDefaults()
    }
    
    /**
     * Load all templates
     */
    private fun loadTemplates() {
        viewModelScope.launch {
            templateRepository.getAllTemplates().collect { templates ->
                _templates.value = templates
            }
        }
    }
    
    /**
     * Initialize default templates if needed
     */
    private fun initializeDefaults() {
        viewModelScope.launch {
            templateRepository.initializeDefaultTemplates()
        }
    }
    
    /**
     * Select template for editing
     */
    fun selectTemplate(templateType: String) {
        viewModelScope.launch {
            _selectedTemplate.value = templateRepository.getTemplate(templateType)
        }
    }
    
    /**
     * Update template
     */
    fun updateTemplate(
        templateType: String,
        newText: String,
        updatedBy: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            
            try {
                // Validate required variables are present
                val requiredVars = SMSTemplateEntity.getAvailableVariables(templateType)
                val missingVars = requiredVars.filter { !newText.contains(it) }
                
                if (missingVars.isNotEmpty()) {
                    onError("Missing variables: ${missingVars.joinToString(", ")}")
                    _isSaving.value = false
                    return@launch
                }
                
                templateRepository.updateTemplate(templateType, newText, updatedBy)
                onSuccess()
                
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save template")
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    /**
     * Reset template to default
     */
    fun resetToDefault(templateType: String) {
        viewModelScope.launch {
            templateRepository.resetToDefault(templateType)
            selectTemplate(templateType)
        }
    }
    
    /**
     * Get available variables for template type
     */
    fun getAvailableVariables(templateType: String): List<String> {
        return SMSTemplateEntity.getAvailableVariables(templateType)
    }
    
    /**
     * Get template display name
     */
    fun getTemplateDisplayName(templateType: String): String {
        return when (templateType) {
            SMSTemplateEntity.TYPE_RESERVATION -> "Reservation Confirmation"
            SMSTemplateEntity.TYPE_THANK_YOU -> "Thank You Message"
            SMSTemplateEntity.TYPE_ORDER_READY -> "Order Ready"
            else -> templateType
        }
    }
}
