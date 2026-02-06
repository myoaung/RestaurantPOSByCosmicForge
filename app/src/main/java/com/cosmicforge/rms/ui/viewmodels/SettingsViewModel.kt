package com.cosmicforge.rms.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for application settings
 * v10.4.2: Owner Controls
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    val isGamificationEnabled: Flow<Boolean> = settingsRepository.isGamificationEnabled
    val isPublicRecognitionEnabled: Flow<Boolean> = settingsRepository.isPublicRecognitionEnabled
    
    fun toggleGamification(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGamificationEnabled(enabled)
        }
    }
    
    fun togglePublicRecognition(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPublicRecognitionEnabled(enabled)
        }
    }
}
