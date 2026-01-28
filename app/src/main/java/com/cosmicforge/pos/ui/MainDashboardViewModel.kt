package com.cosmicforge.pos.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for MainDashboardScreen
 */
@HiltViewModel
class MainDashboardViewModel @Inject constructor() : ViewModel() {
    
    private val _selectedRoute = MutableStateFlow(NavigationRoute.FLOOR_MAP)
    val selectedRoute: StateFlow<NavigationRoute> = _selectedRoute.asStateFlow()
    
    fun selectRoute(route: NavigationRoute) {
        _selectedRoute.value = route
    }
}
