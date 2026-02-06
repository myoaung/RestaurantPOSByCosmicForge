package com.cosmicforge.rms.utils

import java.util.concurrent.TimeUnit

/**
 * Staff management utilities for HR operations
 * v9.7: NRC validation and tenure calculations
 */
object StaffUtils {
    
    /**
     * Validate Myanmar NRC number format
     * Format: 12/TaKaNa(N)123456
     */
    fun validateNRC(nrc: String?): Boolean {
        if (nrc.isNullOrBlank()) return true // Optional field
        
        // Pattern: 1-2 digits / District name (N or C) / 6 digits
        val pattern = "^\\d{1,2}/[A-Za-z]+\\([NC]\\)\\d{6}$".toRegex()
        return pattern.matches(nrc)
    }
    
    /**
     * Calculate staff tenure from join date
     * Returns Pair(years, months)
     */
    fun calculateTenure(joinDateTimestamp: Long): Pair<Int, Int> {
        val currentTime = System.currentTimeMillis()
        val diffMillis = currentTime - joinDateTimestamp
        val totalMonths = TimeUnit.MILLISECONDS.toDays(diffMillis) / 30
        
        val years = (totalMonths / 12).toInt()
        val months = (totalMonths % 12).toInt()
        
        return Pair(years, months)
    }
    
    /**
     * Format tenure for display
     */
    fun formatTenure(tenure: Pair<Int, Int>): String {
        return "${tenure.first} years, ${tenure.second} months"
    }
    
    /**
     * Calculate tenure in days (for detailed calculations)
     */
    fun getTenureDays(joinDateTimestamp: Long): Long {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - joinDateTimestamp)
    }
}
