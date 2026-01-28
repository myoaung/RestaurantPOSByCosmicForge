package com.cosmicforge.pos.di

import android.content.Context
import com.cosmicforge.pos.core.network.*
import com.cosmicforge.pos.core.sync.SyncEngine
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for network and sync dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideWiFiDirectManager(
        @ApplicationContext context: Context
    ): WiFiDirectManager {
        return WiFiDirectManager(context)
    }
    
    @Provides
    @Singleton
    fun provideNSDManager(
        @ApplicationContext context: Context
    ): NSDManager {
        return NSDManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSocketManager(
        gson: Gson
    ): SocketManager {
        return SocketManager(gson)
    }
    
    @Provides
    @Singleton
    fun provideMeshNetworkManager(
        @ApplicationContext context: Context,
        wifiDirectManager: WiFiDirectManager,
        nsdManager: NSDManager,
        socketManager: SocketManager
    ): MeshNetworkManager {
        return MeshNetworkManager(
            context = context,
            wifiDirectManager = wifiDirectManager,
            nsdManager = nsdManager,
            socketManager = socketManager
        )
    }
    
    @Provides
    @Singleton
    fun provideSyncEngine(
        meshNetwork: MeshNetworkManager,
        orderDao: com.cosmicforge.pos.data.database.dao.OrderDao,
        orderDetailDao: com.cosmicforge.pos.data.database.dao.OrderDetailDao,
        tableDao: com.cosmicforge.pos.data.database.dao.TableDao,
        gson: Gson
    ): SyncEngine {
        return SyncEngine(
            meshNetwork = meshNetwork,
            orderDao = orderDao,
            orderDetailDao = orderDetailDao,
            tableDao = tableDao,
            gson = gson
        )
    }
}
