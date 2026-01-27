package com.cosmicforge.pos.di

import android.content.Context
import androidx.room.Room
import com.cosmicforge.pos.data.database.CosmicForgeDatabase
import com.cosmicforge.pos.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Hilt module for database dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): CosmicForgeDatabase {
        // Generate encryption key from device-specific info
        // In production, this should be generated securely and stored in Android Keystore
        val passphrase = SQLiteDatabase.getBytes("CosmicForge2024!".toCharArray())
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            CosmicForgeDatabase::class.java,
            CosmicForgeDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory) // Enable SQLCipher encryption
            .fallbackToDestructiveMigration() // For development only
            .build()
    }
    
    @Provides
    fun provideShopConfigDao(database: CosmicForgeDatabase): ShopConfigDao {
        return database.shopConfigDao()
    }
    
    @Provides
    fun provideUserDao(database: CosmicForgeDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideTableDao(database: CosmicForgeDatabase): TableDao {
        return database.tableDao()
    }
    
    @Provides
    fun provideMenuItemDao(database: CosmicForgeDatabase): MenuItemDao {
        return database.menuItemDao()
    }
    
    @Provides
    fun provideOrderDao(database: CosmicForgeDatabase): OrderDao {
        return database.orderDao()
    }
    
    @Provides
    fun provideOrderDetailDao(database: CosmicForgeDatabase): OrderDetailDao {
        return database.orderDetailDao()
    }
    
    @Provides
    fun provideSecurityAuditDao(database: CosmicForgeDatabase): SecurityAuditDao {
        return database.securityAuditDao()
    }
    
    @Provides
    fun provideSMSTemplateDao(database: CosmicForgeDatabase): SMSTemplateDao {
        return database.smsTemplateDao()
    }
}
