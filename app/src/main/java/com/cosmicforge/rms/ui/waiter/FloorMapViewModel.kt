@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.cosmicforge.rms.ui.waiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.data.database.entities.TableEntity
import com.cosmicforge.rms.data.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for floor map with real-time table status
 */
@HiltViewModel
class FloorMapViewModel @Inject constructor(
    private val tableRepository: TableRepository
) : ViewModel() {
    
    private val _selectedFloor = MutableStateFlow(1)
    val selectedFloor: StateFlow<Int> = _selectedFloor.asStateFlow()
    
    private val _selectedTable = MutableStateFlow<TableEntity?>(null)
    val selectedTable: StateFlow<TableEntity?> = _selectedTable.asStateFlow()
    
    // Real-time table updates from sync engine
    val tablesOnFloor: StateFlow<List<TableEntity>> = selectedFloor
        .flatMapLatest { floor ->
            tableRepository.getTablesByFloor(floor)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val tableStats: StateFlow<TableStats> = tablesOnFloor
        .map { tables ->
            TableStats(
                total = tables.size,
                free = tables.count { it.status == TableEntity.STATUS_FREE },
                inUse = tables.count { it.status == TableEntity.STATUS_IN_USE },
                dirty = tables.count { it.status == TableEntity.STATUS_DIRTY },
                reserved = tables.count { it.status == TableEntity.STATUS_RESERVED }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TableStats()
        )
    
    /**
     * Select floor
     */
    fun selectFloor(floor: Int) {
        _selectedFloor.value = floor
    }
    
    /**
     * Select table
     */
    fun selectTable(table: TableEntity) {
        _selectedTable.value = table
    }
    
    /**
     * Clear table selection
     */
    fun clearSelection() {
        _selectedTable.value = null
    }
    
    /**
     * Open table for new order
     * Instantly broadcasts 'IN_USE' status to all tablets
     */
    fun openTable(tableId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            tableRepository.updateTableStatus(tableId, TableEntity.STATUS_IN_USE)
            onSuccess()
        }
    }
    
    /**
     * Mark table as dirty
     */
    fun markTableDirty(tableId: String) {
        viewModelScope.launch {
            tableRepository.clearTable(tableId)
        }
    }
    
    /**
     * Mark table as clean
     */
    fun markTableClean(tableId: String) {
        viewModelScope.launch {
            tableRepository.markTableClean(tableId)
        }
    }
    
    /**
     * Reserve table
     */
    fun reserveTable(tableId: String, reservationName: String) {
        viewModelScope.launch {
            tableRepository.reserveTable(tableId, reservationName)
        }
    }
}

/**
 * Table statistics
 */
data class TableStats(
    val total: Int = 0,
    val free: Int = 0,
    val inUse: Int = 0,
    val dirty: Int = 0,
    val reserved: Int = 0
)
