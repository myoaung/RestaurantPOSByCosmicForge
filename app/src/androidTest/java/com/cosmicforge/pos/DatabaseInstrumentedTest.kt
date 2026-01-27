package com.cosmicforge.pos.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cosmicforge.pos.data.database.dao.UserDao
import com.cosmicforge.pos.data.database.entities.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * Instrumented test for database operations
 */
@RunWith(AndroidJUnit4::class)
class DatabaseInstrumentedTest {
    
    private lateinit var database: CosmicForgeDatabase
    private lateinit var userDao: UserDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CosmicForgeDatabase::class.java
        ).build()
        
        userDao = database.userDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun testInsertAndRetrieveUser() = runBlocking {
        // Create a test user
        val user = UserEntity(
            userName = "Test Owner",
            pinHash = hashPin("1234"),
            roleLevel = UserEntity.ROLE_OWNER,
            stationId = null
        )
        
        // Insert user
        val userId = userDao.insertUser(user)
        assertTrue("User ID should be generated", userId > 0)
        
        // Retrieve user
        val retrievedUser = userDao.getUserById(userId)
        assertNotNull("User should be retrieved", retrievedUser)
        assertEquals("User names should match", "Test Owner", retrievedUser?.userName)
        assertEquals("Role should be Owner", UserEntity.ROLE_OWNER, retrievedUser?.roleLevel)
    }
    
    @Test
    fun testGetUsersByRole() = runBlocking {
        // Insert multiple users with different roles
        userDao.insertUser(
            UserEntity(
                userName = "Owner 1",
                pinHash = hashPin("1111"),
                roleLevel = UserEntity.ROLE_OWNER
            )
        )
        userDao.insertUser(
            UserEntity(
                userName = "Waiter 1",
                pinHash = hashPin("2222"),
                roleLevel = UserEntity.ROLE_WAITER
            )
        )
        userDao.insertUser(
            UserEntity(
                userName = "Waiter 2",
                pinHash = hashPin("3333"),
                roleLevel = UserEntity.ROLE_WAITER
            )
        )
        
        // Get waiters
        val waiters = userDao.getUsersByRole(UserEntity.ROLE_WAITER).first()
        assertEquals("Should have 2 waiters", 2, waiters.size)
    }
    
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
