package com.cosmicforge.pos.di

import com.cosmicforge.pos.core.security.*
import com.cosmicforge.pos.data.database.dao.SecurityAuditDao
import com.cosmicforge.pos.data.database.dao.UserDao
import com.cosmicforge.pos.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for security and authentication dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideDeviceInfoProvider(): DeviceInfoProvider {
        return DeviceInfoProvider()
    }
    
    @Provides
    @Singleton
    fun provideAuditLogger(
        securityAuditDao: SecurityAuditDao,
        deviceInfoProvider: DeviceInfoProvider
    ): AuditLogger {
        return AuditLogger(securityAuditDao, deviceInfoProvider)
    }
    
    @Provides
    @Singleton
    fun provideRBACManager(): RBACManager {
        return RBACManager()
    }
    
    @Provides
    @Singleton
    fun provideAuthenticationManager(
        userDao: UserDao,
        auditLogger: AuditLogger,
        rbacManager: RBACManager
    ): AuthenticationManager {
        return AuthenticationManager(userDao, auditLogger, rbacManager)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        userDao: UserDao,
        authenticationManager: AuthenticationManager,
        auditLogger: AuditLogger
    ): AuthRepository {
        return AuthRepository(userDao, authenticationManager, auditLogger)
    }
}
