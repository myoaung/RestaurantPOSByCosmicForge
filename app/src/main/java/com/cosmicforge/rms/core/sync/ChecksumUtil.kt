package com.cosmicforge.rms.core.sync

import java.security.MessageDigest

/**
 * Checksum Utility for Data Integrity
 * 
 * Uses SHA-256 hashing to ensure sync data hasn't been corrupted
 * during transmission from offline to online state.
 * 
 * Stone Tier: The "Double-Check" Hash requirement
 */
object ChecksumUtil {
    
    /**
     * Generate SHA-256 checksum for data
     * 
     * @param data The data to hash (typically JSON payload)
     * @return Hexadecimal string representation of the hash
     */
    fun generateChecksum(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Validate checksum against data
     * 
     * @param data The data to validate
     * @param checksum The expected checksum
     * @return true if checksum matches, false if corrupted
     */
    fun validateChecksum(data: String, checksum: String): Boolean {
        val calculatedChecksum = generateChecksum(data)
        return calculatedChecksum.equals(checksum, ignoreCase = true)
    }
    
    /**
     * Generate checksum for a batch of sync messages
     * Useful for validating entire sync batches
     */
    fun generateBatchChecksum(payloads: List<String>): String {
        val combinedData = payloads.joinToString(separator = "|")
        return generateChecksum(combinedData)
    }
}
