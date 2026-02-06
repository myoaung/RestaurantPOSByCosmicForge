package com.cosmicforge.rms.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for Cosmic Forge RMS
 * Migration 9→10: Complete HR Management System
 */

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create rewards table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS rewards (
                reward_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                month_year TEXT NOT NULL,
                user_id INTEGER NOT NULL,
                performance_score REAL NOT NULL,
                total_revenue REAL NOT NULL,
                order_count INTEGER NOT NULL,
                void_count INTEGER NOT NULL,
                is_paid INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
        
        // Create blacklist table with unique NRC constraint
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS blacklist (
                blacklist_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nrc_number TEXT NOT NULL,
                staff_name TEXT NOT NULL,
                reason_for_blacklist TEXT NOT NULL,
                blacklisted_at INTEGER NOT NULL,
                blacklisted_by INTEGER NOT NULL
            )
        """.trimIndent())
        
        database.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_blacklist_nrc_number 
            ON blacklist(nrc_number)
        """.trimIndent())
        
        // Create leave_requests table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS leave_requests (
                request_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id INTEGER NOT NULL,
                leave_type TEXT NOT NULL,
                start_date INTEGER NOT NULL,
                end_date INTEGER NOT NULL,
                reason TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'Pending',
                reviewed_by INTEGER,
                reviewed_at INTEGER,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
