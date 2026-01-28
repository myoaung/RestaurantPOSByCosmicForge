package com.cosmicforge.pos.di

import com.cosmicforge.pos.core.sync.SyncEngine
import com.cosmicforge.pos.data.database.dao.*
import com.cosmicforge.pos.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repositories
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideTableRepository(
        tableDao: TableDao,
        syncEngine: SyncEngine
    ): TableRepository {
        return TableRepository(tableDao, syncEngine)
    }
    
    @Provides
    @Singleton
    fun provideMenuRepository(
        menuItemDao: MenuItemDao
    ): MenuRepository {
        return MenuRepository(menuItemDao)
    }
    
    @Provides
    @Singleton
    fun provideOrderRepository(
        orderDao: OrderDao,
        orderDetailDao: OrderDetailDao,
        syncEngine: SyncEngine
    ): OrderRepository {
        return OrderRepository(orderDao, orderDetailDao, syncEngine)
    }
    
    @Provides
    @Singleton
    fun provideSMSTemplateRepository(
        smsTemplateDao: SMSTemplateDao
    ): SMSTemplateRepository {
        return SMSTemplateRepository(smsTemplateDao)
    }
}
